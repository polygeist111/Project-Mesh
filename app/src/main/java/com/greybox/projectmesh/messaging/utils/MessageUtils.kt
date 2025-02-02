package com.greybox.projectmesh.messaging.utils

object MessageUtils {
    fun formatTimestamp(timestamp: Long): String {
        //Adding timestamp formatting logic
        return java.text.SimpleDateFormat("HH:mm").format(timestamp)
    }

    fun generateChatId(sender: String, receiver: String): String {
        //Create a consistent chat ID for two users
        return listOf(sender, receiver).sorted().joinToString("-")
    }
}