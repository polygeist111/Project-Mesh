package com.greybox.projectmesh.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class UuidMaskUtilTest {

    @Test
    fun uuidForMaskAndPort_setsLow16BitsToPort() {
        val mask = UUID.fromString("12345678-1234-5678-9abc-def012340000")
        val port = 0xBEEF // 48879

        val result = uuidForMaskAndPort(mask, port)

        // Most-significant bits preserved
        assertEquals(mask.mostSignificantBits, result.mostSignificantBits)

        // Low 16 bits become the port
        assertEquals(port, result.maskedPort())

        // All other least-significant bits above the port come from the mask
        val resultLsbMasked = result.leastSignificantBits.ushr(16).shl(16)
        val maskLsbMasked = mask.leastSignificantBits.ushr(16).shl(16)
        assertEquals(maskLsbMasked, resultLsbMasked)
    }

    @Test
    fun maskedPort_returnsLow16Bits() {
        val base = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffff0000")
        val port = 0x1234
        val uuid = uuidForMaskAndPort(base, port)

        assertEquals(port, uuid.maskedPort())
    }

    @Test
    fun matchesMask_trueWhenOnlyPortDiffers() {
        val mask = UUID.fromString("00112233-4455-6677-8899-aabbccdd0000")
        val port = 5
        val candidate = uuidForMaskAndPort(mask, port)

        assertTrue(candidate.matchesMask(mask))
    }

    @Test
    fun matchesMask_falseWhenHighBitsDiffer() {
        val mask1 = UUID.fromString("00112233-4455-6677-8899-aabbccdd0000")
        val mask2 = UUID.fromString("00112233-4455-6677-8899-aabbccde0000") // differ in high bits
        val candidate = uuidForMaskAndPort(mask1, 42)

        // Should not match a different mask
        assertTrue(!candidate.matchesMask(mask2))
    }
}
