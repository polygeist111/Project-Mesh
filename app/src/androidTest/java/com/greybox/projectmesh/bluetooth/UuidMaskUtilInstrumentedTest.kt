package com.greybox.projectmesh.bluetooth

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class UuidMaskUtilInstrumentedTest {

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
        val mask2 = UUID.fromString("00112233-4455-6677-8899-aabbccde0000")
        val candidate = uuidForMaskAndPort(mask1, 42)
        assertTrue(!candidate.matchesMask(mask2))
    }
}
