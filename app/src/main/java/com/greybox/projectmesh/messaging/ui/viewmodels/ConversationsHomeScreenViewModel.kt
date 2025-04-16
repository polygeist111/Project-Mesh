package com.greybox.projectmesh.messaging.ui.viewmodels

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greybox.projectmesh.GlobalApp
import com.greybox.projectmesh.messaging.repository.ConversationRepository
import com.greybox.projectmesh.messaging.ui.models.ConversationsHomeScreenModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance

class ConversationsHomeScreenViewModel(
    di: DI
) : ViewModel() {

    //keep track of UI state
    private val _uiState = MutableStateFlow(ConversationsHomeScreenModel(isLoading = true))
    val uiState: Flow<ConversationsHomeScreenModel> = _uiState.asStateFlow()

    //get repository instances
    private val conversationRepository: ConversationRepository by di.instance()

    // Get access to SharedPreferences for the local UUID
    private val sharedPrefs: SharedPreferences by di.instance(tag = "settings")

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            try {
                //start with loading state
                _uiState.update { it.copy (isLoading = true, error = null )}

                //get local device id
                val localUuid = sharedPrefs.getString("UUID", null) ?: "local-user"
                Log.d("ConversationsViewModel", "Local UUID: $localUuid")

                //collect conversations from repository
                conversationRepository.getAllConversations().collect { conversations ->
                    Log.d("ConversationsViewModel", "Loaded ${conversations.size} conversations")

                    //filter out conversations with self
                    val filteredConversations = conversations.filter { conversation ->
                        conversation.userUuid != localUuid
                    }

                    conversations.forEach { conversation ->
                        Log.d("ConversationsViewModel", "Conversation: ID=${conversation.id}, UserUUID=${conversation.userUuid}, Name=${conversation.userName}")
                    }

                    Log.d("ConversationsViewModel", "Filtering out conversations with UUID: $localUuid")

                    Log.d("ConversationsViewModel", "After filtering self: ${filteredConversations.size} conversations")


                    //update the UI state with the conversations
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            conversations = filteredConversations,
                            error = null
                        )
                    }
                }
            }catch (e: Exception) {
                Log.e("ConversationsViewModel", "Error loading conversations", e)

                _uiState.update {
                    it.copy (
                        isLoading = false,
                        error = "Failed to load conversations: ${e.message}"
                    )
                }
            }
        }
    }

    //function to refresh conversations manually
    fun refreshConversations(){
        loadConversations()
    }

    //Function to mark a conversation as read
    fun markConversationAsRead(conversationId: String) {
        viewModelScope.launch {
            try {
                conversationRepository.markAsRead(conversationId)
            } catch (e: Exception) {
                Log.e("ConversationsViewModel", "Error marking conversation as read", e)
            }
        }
    }
}