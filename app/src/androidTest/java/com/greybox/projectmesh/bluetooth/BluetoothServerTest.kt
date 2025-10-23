package com.greybox.projectmesh.bluetooth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import rawhttp.core.RawHttp

/**
 * Placeholder: enable and flesh out once BluetoothServer.handleRequest is implemented.
 *
 * Suggested first real test:
 *  - Build a GET request and assert handleRequest returns 200 + expected body.
 */
@RunWith(AndroidJUnit4::class)
class BluetoothServerTest {

    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val rawHttp = RawHttp()

    @Ignore("Enable after implementing BluetoothServer.handleRequest")
    @Test
    fun handleRequest_returnsExpectedResponse() {
        // val server = BluetoothServer(
        //     context = appContext,
        //     rawHttp = rawHttp,
        //     logger = object : com.ustadmobile.meshrabiya.log.MNetLogger {},
        //     maxClients = 1
        // )
        //
        // val req = rawHttp.parseRequest("GET /ping HTTP/1.1\r\nHost: localhost\r\n\r\n")
        // val resp = server.handleRequest("AA:BB:CC:DD:EE:FF", req)
        //
        // assertEquals(200, resp.statusCode)
        // assertTrue(resp.body.isPresent)
        // assertEquals("pong", resp.body.get().asRawString(Charsets.UTF_8))
    }
}
