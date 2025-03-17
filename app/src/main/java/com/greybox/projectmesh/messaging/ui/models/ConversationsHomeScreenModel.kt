package com.greybox.projectmesh.messaging.ui.models

import com.greybox.projectmesh.messaging.data.entities.Conversation

data class ConversationsHomeScreenModel (
    val isLoading: Boolean = false,
    val conversations: List<Conversation> = emptyList(),
    val error: String? = null
)