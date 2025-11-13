package com.greybox.projectmesh.extension

import org.junit.Assert.*
import org.junit.Test

class ListExtensionTest {

    @Test
    fun checkUpdateFirstItem() {
        val list = listOf(1, 2, 3, 4)
        val updated = list.updateItem(
            condition = { it % 2 == 0 },
            function = { it * 10 }
        )

        assertEquals(listOf(1, 20, 3, 4), updated)
    }

    @Test
    fun checkOnlyFirstMatch() {
        val list = listOf(2, 4, 6)
        val updated = list.updateItem(
            condition = { it % 2 == 0 },
            function = { it * 10 }
        )

        assertEquals(listOf(20, 4, 6), updated)
    }

    @Test
    fun checkNoMatch() {
        val list = listOf(1, 3, 5)
        val updated = list.updateItem(
            condition = { it % 2 == 0 },
            function = { it * 10 }
        )

        assertSame(list, updated)
        assertEquals(listOf(1, 3, 5), updated)
    }

    @Test
    fun checkEmptyList() {
        val list = emptyList<Int>()
        val updated = list.updateItem(
            condition = { true },
            function = { it * 10 }
        )

        assertSame(list, updated)
        assertTrue(updated.isEmpty())
    }

    @Test
    fun checkCallingFunctionNoMatch() {
        val list = listOf(1, 3, 5)
        var called = false

        val updated = list.updateItem(
            condition = { it % 2 == 0 },
            function = {
                called = true
                it * 10
            }
        )

        assertFalse(called)
        assertSame(list, updated)
    }
}
