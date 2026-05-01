package com.example.findly.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.findly.data.local.FindlyDatabase
import com.example.findly.data.repository.FakeItemRepository
import com.example.findly.data.repository.SavedRepositoryImpl
import com.example.findly.domain.model.Item
import com.example.findly.domain.repository.ItemRepository
import com.example.findly.domain.repository.SavedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ItemDetailViewModel(
    application: Application,
    private val itemId: String,
    private val itemRepository: ItemRepository = FakeItemRepository.getInstance(),
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