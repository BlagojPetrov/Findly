package com.example.findly.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findly.domain.model.Message
import com.example.findly.domain.repository.AuthRepository
import com.example.findly.domain.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ChatViewModel(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState

    private val _sendState = MutableStateFlow<SendMessageState>(SendMessageState.Idle)
    val sendState: StateFlow<SendMessageState> = _sendState

    val currentUserId: String get() = authRepository.currentUser?.uid ?: ""
    val currentUserDisplayName: String get() = authRepository.currentUser?.displayName ?: ""

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            messageRepository.observeMessages(conversationId)
                .catch { e ->
                    _uiState.value = ChatUiState.Error(e.message ?: "Unknown error")
                }
                .collect { messages ->
                    _uiState.value = if (messages.isEmpty()) {
                        ChatUiState.Empty
                    } else {
                        ChatUiState.Success(messages)
                    }
                }
        }
    }

    fun sendMessage(conversationId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val userId = authRepository.currentUser?.uid ?: return
        val displayName = authRepository.currentUser?.displayName ?: ""

        viewModelScope.launch {
            _sendState.value = SendMessageState.Sending
            val result = messageRepository.sendMessage(
                conversationId = conversationId,
                senderId = userId,
                senderDisplayName = displayName,
                text = trimmed
            )
            _sendState.value = if (result.isSuccess) {
                SendMessageState.Sent
            } else {
                SendMessageState.Error(result.exceptionOrNull()?.message ?: "Failed to send")
            }
        }
    }

    fun markAsRead(conversationId: String) {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            messageRepository.markMessagesAsRead(conversationId, userId)
        }
    }

    fun resetSendState() {
        _sendState.value = SendMessageState.Idle
    }
}

sealed class ChatUiState {
    object Loading : ChatUiState()
    object Empty : ChatUiState()
    data class Success(val messages: List<Message>) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

sealed class SendMessageState {
    object Idle : SendMessageState()
    object Sending : SendMessageState()
    object Sent : SendMessageState()
    data class Error(val message: String) : SendMessageState()
}