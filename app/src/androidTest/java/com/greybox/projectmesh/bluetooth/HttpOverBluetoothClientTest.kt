package com.greybox.projectmesh.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import rawhttp.core.RawHttp
import rawhttp.core.body.StringBody
import rawhttp.core.RawHttpRequest
import java.util.UUID

/**
 * These are deliberately lightweight, environment-guarded tests to give the team a place to extend.
 * Many emulators/devices have Bluetooth disabled or unavailable; we skip when not supported.
 *
 * What we cover now:
 *  - can construct the client
 *  - (if adapter is present & enabled) we can exercise the "busy" path by returning UUID_BUSY
 *
 * Later improvements (when you add a mocking lib or DI seam for adapter/socket):
 *  - simulate adapter disabled -> expect 503 "Bluetooth not enabled"
 *  - simulate socket errors -> expect 500 "Internal Server Error"
 *  - end-to-end allocations with a fake allocation client/server on a device lab
 */
@RunWith(AndroidJUnit4::class)
class HttpOverBluetoothClientTest {

    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val rawHttp = RawHttp()

    // Minimal fake allocation client that always yields "busy" UUID.
    // (Meshrabiya defines UUID_BUSY; your production client returns it when the server is busy.)
    private class FakeBusyUuidAllocator(
        val uuidToReturn: UUID
    ) : UuidAllocationClient(
        appContext = ApplicationProvider.getApplicationContext(),
        onLog = /* pass a real logger later */ object : com.ustadmobile.meshrabiya.log.MNetLogger {},
        clientNodeAddr = 0
    ) {
        override suspend fun requestUuidAllocation(remoteAddress: String, uuidMask: UUID): UUID {
            return uuidToReturn
        }
    }

    @Test
    fun constructClient_whenBluetoothEnvPresent_orSkip() {
        val manager = appContext.getSystemService(BluetoothManager::class.java)
        // If the environment has no Bluetooth, skip (gives us a green skip instead of red fail)
        assumeTrue("BluetoothManager not available on this device/emulator", manager != null)

        val client = HttpOverBluetoothClient(
            appContext = appContext,
            rawHttp = rawHttp,
            logger = object : com.ustadmobile.meshrabiya.log.MNetLogger {}, // no-op stub logger
            clientNodeAddr = 1
        )

        // Just ensure it's constructed; add real assertions later
        assertTrue(client is HttpOverBluetoothClient)
    }

    @Test
    fun sendRequest_returnsBusyOrSkips_whenAdapterEnabled() = kotlinx.coroutines.runBlocking {
        val manager = appContext.getSystemService(BluetoothManager::class.java)
        val adapter: BluetoothAdapter? = manager?.adapter

        // If adapter is missing or disabled, skip (can't reliably force-enable in tests)
        assumeTrue("Bluetooth adapter not available", adapter != null)
        assumeTrue("Bluetooth adapter disabled", adapter!!.isEnabled)

        val busyUuid = com.ustadmobile.meshrabiya.MeshrabiyaConstants.UUID_BUSY

        val client = HttpOverBluetoothClient(
            appContext = appContext,
            rawHttp = rawHttp,
            logger = object : com.ustadmobile.meshrabiya.log.MNetLogger {}, // no-op
            clientNodeAddr = 1,
            uuidAllocationClient = FakeBusyUuidAllocator(busyUuid)
        )

        val request: RawHttpRequest = rawHttp.parseRequest(
            "GET /hello HTTP/1.1\r\nHost: example\r\n\r\n"
        )

        val response = client.sendRequest(
            remoteAddress = "00:11:22:33:44:55", // dummy; socket won't be used on "busy"
            uuidMask = UUID.fromString("00112233-4455-6677-8899-aabbccdd0000"),
            request = request
        )
        // On "busy", client returns 503 Service Unavailable
        assertTrue(response.response.statusCode == 503)
        response.close()
    }
}
