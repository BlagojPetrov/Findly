package com.example.findly.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.findly.data.local.FindlyDatabase
import com.example.findly.data.remote.firestore.FirestoreMessageSource
import com.example.findly.data.repository.FirestoreItemRepository
import com.example.findly.data.repository.MessageRepositoryImpl
import com.example.findly.data.repository.SavedRepositoryImpl
import com.example.findly.domain.model.Item
import com.example.findly.domain.model.ItemType
import com.example.findly.domain.model.MatchedItem
import com.example.findly.domain.repository.ItemRepository
import com.example.findly.domain.repository.MessageRepository
import com.example.findly.domain.repository.SavedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ItemDetailViewModel(
    application: Application,
    private val itemId: String,
    private val itemRepository: ItemRepository = FirestoreItemRepository(),
    private val savedRepository: SavedRepository = SavedRepositoryImpl(
        FindlyDatabase.getInstance(application).savedItemDao()
    ),
    private val messageRepository: MessageRepository = MessageRepositoryImpl(
        FirestoreMessageSource()
    )
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved

    private val _matchesState = MutableStateFlow<MatchesUiState>(MatchesUiState.Loading)
    val matchesState: StateFlow<MatchesUiState> = _matchesState

    private val currentUserId: String =
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "guest"

    val isOwnItem: StateFlow<Boolean> = _uiState
        .map { state ->
            state is DetailUiState.Success && state.item.userId == currentUserId
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    init {
        loadItem()
    }

    private fun loadItem() {
        viewModelScope.launch {
            val item = itemRepository.getItemById(itemId)
            if (item != null) {
                savedRepository.isItemSaved(itemId, currentUserId)
                    .onEach { _isSaved.value = it }
                    .launchIn(viewModelScope)
                _uiState.value = DetailUiState.Success(item)
                loadMatches(item)
            } else {
                savedRepository.removeSavedItem(itemId, currentUserId)
                _uiState.value = DetailUiState.Error("Item not found")
                _matchesState.value = MatchesUiState.Empty
            }
        }
    }

    private suspend fun loadMatches(item: Item) {
        _matchesState.value = MatchesUiState.Loading
        try {
            val oppositeType = if (item.type == ItemType.LOST) ItemType.FOUND else ItemType.LOST
            val candidates = itemRepository.getItemsByCategory(
                category = item.category.name,
                type = oppositeType
            ).filter { it.id != item.id && it.userId != item.userId }

            val itemWords = tokenize(item.title + " " + item.description)

            val matches = candidates
                .map { candidate ->
                    val candidateWords = tokenize(candidate.title + " " + candidate.description)
                    val score = jaccardSimilarity(itemWords, candidateWords)
                    MatchedItem(item = candidate, score = score)
                }
                .filter { it.score >= MATCH_THRESHOLD }
                .sortedByDescending { it.score }
                .take(MAX_MATCHES)

            _matchesState.value = if (matches.isEmpty()) {
                MatchesUiState.Empty
            } else {
                MatchesUiState.Success(matches)
            }
        } catch (e: Exception) {
            _matchesState.value = MatchesUiState.Empty
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

    fun getOrCreateConversation(
        onSuccess: (conversationId: String, otherUserDisplayName: String, itemTitle: String, otherUserPhotoUrl: String?) -> Unit,
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
                onSuccess(result.getOrThrow(), item.userDisplayName, item.title, item.userPhotoUrl)
            } else {
                onError(result.exceptionOrNull()?.message ?: "Failed to start conversation")
            }
        }
    }

    private fun tokenize(text: String): Set<String> {
        return text
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .split("\\s+".toRegex())
            .filter { it.length > 1 && it !in STOP_WORDS }
            .toSet()
    }

    private fun jaccardSimilarity(setA: Set<String>, setB: Set<String>): Float {
        if (setA.isEmpty() && setB.isEmpty()) return 0f
        val intersection = setA.intersect(setB).size
        val union = setA.union(setB).size
        return intersection.toFloat() / union.toFloat()
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

    companion object {
        private const val MATCH_THRESHOLD = 0.1f
        private const val MAX_MATCHES = 5
        private val STOP_WORDS = setOf(
            "a", "an", "the", "and", "or", "but", "in", "on",
            "at", "to", "for", "of", "with", "my", "i", "it",
            "is", "was", "lost", "found", "item"
        )
    }
}

sealed class DetailUiState {
    data object Loading : DetailUiState()
    data class Success(val item: Item) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

sealed class MatchesUiState {
    data object Loading : MatchesUiState()
    data object Empty : MatchesUiState()
    data class Success(val matches: List<MatchedItem>) : MatchesUiState()
}