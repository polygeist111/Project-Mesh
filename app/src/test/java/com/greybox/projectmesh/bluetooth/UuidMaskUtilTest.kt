package com.greybox.projectmesh.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class UuidMaskUtilTest {

    @Test
    fun uuidForMaskAndPort_setsLow16BitsToPort() {
        val mask = UUID.fromString("12345678-1234-5678-9abc-def012340000")
        val port = 0xBEEF

        val result = uuidForMaskAndPort(mask, port)

        // Most-significant bits preserved
        assertEquals(mask.mostSignificantBits, result.mostSignificantBits)

        // Low 16 bits become the port
        assertEquals(port, result.maskedPort())

        // All higher LSBs should match the mask (mirror the implementation's signed shift)
        val resultLsbMasked = result.leastSignificantBits.shr(16).shl(16)
        val maskLsbMasked = mask.leastSignificantBits.shr(16).shl(16)
        assertEquals(maskLsbMasked, resultLsbMasked)
    }

    @Test
    fun maskedPort_returnsLow16Bits() {
        val base = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffff0000")
        val port = 0x1234
        val uuid = uuidForMaskAndPort(base, port)

        assertEquals(port, uuid.maskedPort())
    }
}