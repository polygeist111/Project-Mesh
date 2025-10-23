package com.greybox.projectmesh.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.UUID

class BluetoothUuidsTest {

    @Test
    fun allocationServiceUuid_isStable() {
        val expected = UUID.fromString("d8a8f0e8-1f62-4b1f-9c38-000000000000")
        assertNotNull(BluetoothUuids.ALLOCATION_SERVICE_UUID)
        assertEquals(expected, BluetoothUuids.ALLOCATION_SERVICE_UUID)
    }

    @Test
    fun allocationCharUuid_isStable() {
        val expected = UUID.fromString("5a8dc9d3-c6b1-4eab-8f7d-3a2f6b0c9e4d")
        assertNotNull(BluetoothUuids.ALLOCATION_CHAR_UUID)
        assertEquals(expected, BluetoothUuids.ALLOCATION_CHAR_UUID)
    }
}
