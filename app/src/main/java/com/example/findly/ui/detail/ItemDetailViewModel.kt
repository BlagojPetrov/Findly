package com.example.findly.ui.detail

import com.example.findly.data.remote.firestore.FirestoreMessageSource
import com.example.findly.data.repository.MessageRepositoryImpl
import com.example.findly.domain.repository.MessageRepository
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.findly.data.local.FindlyDatabase
import com.example.findly.data.repository.FirestoreItemRepository
import com.example.findly.data.repository.SavedRepositoryImpl
import com.example.findly.domain.model.Item
import com.example.findly.domain.repository.ItemRepository
import com.example.findly.domain.repository.SavedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

class ItemDetailViewModel(
    application: Application,
    private val itemId: String,
    private val itemRepository: ItemRepository = FirestoreItemRepository(),
    private val savedRepository: SavedRepository = SavedRepositoryImpl(
        FindlyDatabase.getInstance(application).savedItemDao()
    )
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved

    init {
        loadItem()
    }

    private val currentUserId: String =
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "guest"

    private val messageRepository: MessageRepository = MessageRepositoryImpl(FirestoreMessageSource())

    fun getOrCreateConversation(
        onSuccess: (conversationId: String, otherUserDisplayName: String, itemTitle: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentState = _uiState.value
        if (currentState !is DetailUiState.Success) return

        val item = currentState.item
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            onError("You must be signed in to contact the poster")
            return
        }

        if (item.userId == currentUser.uid) {
            onError("You cannot message yourself")
            return
        }

        viewModelScope.launch {
            val result = messageRepository.getOrCreateConversation(
                itemId = item.id,
                itemTitle = item.title,
                currentUserId = currentUser.uid,
                currentUserDisplayName = currentUser.displayName ?: "Anonymous",
                otherUserId = item.userId,
                otherUserDisplayName = item.userDisplayName,
                otherUserPhotoUrl = item.userPhotoUrl
            )
            if (result.isSuccess) {
                onSuccess(result.getOrThrow(), item.userDisplayName, item.title)
            } else {
                onError(result.exceptionOrNull()?.message ?: "Failed to start conversation")
            }
        }
    }

    val isOwnItem: StateFlow<Boolean> = _uiState
        .map { state ->
            state is DetailUiState.Success && state.item.userId == currentUserId
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    fun deleteItem(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                itemRepository.deleteItem(itemId)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to delete item")
            }
        }
    }

    private fun loadItem() {
        viewModelScope.launch {
            val item = itemRepository.getItemById(itemId)
            _uiState.value = if (item != null) {
                savedRepository.isItemSaved(itemId, currentUserId)
                    .onEach { _isSaved.value = it }
                    .launchIn(viewModelScope)
                DetailUiState.Success(item)
            } else {
                DetailUiState.Error("Item not found")
            }
        }
    }

    fun toggleSaved() {
        val currentState = _uiState.value
        if (currentState !is DetailUiState.Success) return
        viewModelScope.launch {
            if (_isSaved.value) {
                savedRepository.removeSavedItem(itemId, currentUserId)
            } else {
                savedRepository.saveItem(currentState.item, currentUserId)
            }
        }
    }

    class Factory(
        private val application: Application,
        private val itemId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return ItemDetailViewModel(application, itemId) as T
        }
    }
}

sealed class DetailUiState {
    data object Loading : DetailUiState()
    data class Success(val item: Item) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}