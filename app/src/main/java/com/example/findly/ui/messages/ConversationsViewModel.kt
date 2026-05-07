package com.example.findly.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findly.domain.model.Conversation
import com.example.findly.domain.repository.AuthRepository
import com.example.findly.domain.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ConversationsViewModel(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConversationsUiState>(ConversationsUiState.Loading)
    val uiState: StateFlow<ConversationsUiState> = _uiState

    init {
        loadConversations()
    }

    private fun loadConversations() {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            messageRepository.observeConversations(userId)
                .catch { e ->
                    _uiState.value = ConversationsUiState.Error(e.message ?: "Unknown error")
                }
                .collect { conversations ->
                    _uiState.value = if (conversations.isEmpty()) {
                        ConversationsUiState.Empty
                    } else {
                        ConversationsUiState.Success(conversations)
                    }
                }
        }
    }
}

sealed class ConversationsUiState {
    object Loading : ConversationsUiState()
    object Empty : ConversationsUiState()
    data class Success(val conversations: List<Conversation>) : ConversationsUiState()
    data class Error(val message: String) : ConversationsUiState()
}