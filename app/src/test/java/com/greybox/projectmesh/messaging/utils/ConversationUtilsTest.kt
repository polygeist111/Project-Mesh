package com.greybox.projectmesh.messaging.utils

import org.junit.Assert.*
import org.junit.Test

class ConversationUtilsTest {

    @Test
    fun checkCreateConversationId() {
        val id1 = ConversationUtils.createConversationId("a", "b")
        val id2 = ConversationUtils.createConversationId("b", "a")

        assertEquals("a-b", id1)
        assertEquals(id1, id2)
    }

    @Test
    fun checkIdenticalUuids() {
        val id = ConversationUtils.createConversationId("same", "same")
        assertEquals("same-same", id)
    }

    @Test
    fun checkSpecialCase() {
        val id = ConversationUtils.createConversationId("anything", "test-device-uuid")
        assertEquals("local-user-test-device-uuid", id)
    }

    @Test
    fun checkSecondSpecialCase() {
        val id = ConversationUtils.createConversationId("anything", "offline-test-device-uuid")
        assertEquals("local-user-offline-test-device-uuid", id)
    }
}
