package com.greybox.projectmesh.server

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class InputStreamCounterTest {

    @Test
    fun readSingleBytes_countsAllBytes() {
        val data = "hello world".toByteArray()
        val input = ByteArrayInputStream(data)
        val counter = InputStreamCounter(input)

        while (true) {
            val result = counter.read()
            if (result == -1) break
        }

        assertEquals(data.size, counter.bytesRead)
        assertFalse(counter.closed)
    }

    @Test
    fun readIntoBuffer_countsAllBytes() {
        val data = ByteArray(4096) { it.toByte() }
        val input = ByteArrayInputStream(data)
        val counter = InputStreamCounter(input)

        val buffer = ByteArray(1024)
        while (true) {
            val n = counter.read(buffer)
            if (n == -1) break
        }

        assertEquals(data.size, counter.bytesRead)
    }

    @Test
    fun readWithOffset_countsAllBytes() {
        val data = "abcdefghi".toByteArray()
        val input = ByteArrayInputStream(data)
        val counter = InputStreamCounter(input)

        val buffer = ByteArray(10)
        while (true) {
            val n = counter.read(buffer, 1, 4)
            if (n == -1) break
        }

        assertEquals(data.size, counter.bytesRead)
    }

    @Test
    fun bytesRead_doesNotIncreaseAfterEof() {
        val data = "test".toByteArray()
        val input = ByteArrayInputStream(data)
        val counter = InputStreamCounter(input)

        val buffer = ByteArray(2)
        while (counter.read(buffer) != -1) {
        }

        val before = counter.bytesRead
        val eofRead = counter.read(buffer)

        assertEquals(-1, eofRead)
        assertEquals(before, counter.bytesRead)
    }

    @Test
    fun close_setsClosedFlag() {
        val data = "xyz".toByteArray()
        val input = ByteArrayInputStream(data)
        val counter = InputStreamCounter(input)

        assertFalse(counter.closed)

        counter.close()

        assertTrue(counter.closed)
    }
}