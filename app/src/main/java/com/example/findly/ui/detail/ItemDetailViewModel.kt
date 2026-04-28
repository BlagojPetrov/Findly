package com.example.findly.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.findly.data.repository.FakeItemRepository
import com.example.findly.domain.model.Item
import com.example.findly.domain.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ItemDetailViewModel(
    private val itemId: String,
    private val repository: ItemRepository = FakeItemRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState

    init {
        loadItem()
    }

    private fun loadItem() {
        viewModelScope.launch {
            val item = repository.getItemById(itemId)
            _uiState.value = if (item != null) {
                DetailUiState.Success(item)
            } else {
                DetailUiState.Error("Item not found")
            }
        }
    }

    // Factory needed because ViewModel takes a constructor argument (itemId)
    class Factory(private val itemId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ItemDetailViewModel(itemId) as T
        }
    }
}

sealed class DetailUiState {
    data object Loading : DetailUiState()
    data class Success(val item: Item) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}