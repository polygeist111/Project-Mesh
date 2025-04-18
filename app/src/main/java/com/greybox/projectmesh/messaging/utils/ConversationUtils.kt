package com.greybox.projectmesh.messaging.utils

import com.greybox.projectmesh.testing.TestDeviceService

/**
 * Centralized utility class for conversation-related operations.
 * This ensures consistent conversation ID generation across the application.
 */

object ConversationUtils {
    /**
     * Creates a consistent conversation ID from two UUIDs.
     * This method ensures that the same conversation ID is generated
     * regardless of which user initiates the conversation.
     *
     * @param uuid1 The UUID of the first user
     * @param uuid2 The UUID of the second user
     * @return A standardized conversation ID
     */

    fun createConversationId(uuid1: String, uuid2: String): String {
        //special cases for test devices
        if (uuid2 == "test-device-uuid") {
            return "local-user-test-device-uuid"
        }
        if (uuid2 == "offline-test-device-uuid") {
            return "local-user-offline-test-device-uuid"
        }

        //sort UUIDs to ensure the same ID is generated regardless of order
        return listOf(uuid1, uuid2).sorted().joinToString("-")
    }
}