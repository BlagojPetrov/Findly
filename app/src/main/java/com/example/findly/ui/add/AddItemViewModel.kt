package com.example.findly.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findly.data.repository.FakeItemRepository
import com.example.findly.domain.model.Category
import com.example.findly.domain.model.Item
import com.example.findly.domain.model.ItemType
import com.example.findly.domain.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AddItemViewModel : ViewModel() {

    private val repository: ItemRepository = FakeItemRepository.getInstance()

    private val _uiState = MutableStateFlow<AddItemUiState>(AddItemUiState.Idle)
    val uiState: StateFlow<AddItemUiState> = _uiState

    fun submitItem(
        type: ItemType,
        title: String,
        description: String,
        category: Category,
        locationName: String
    ) {
        // Basic validation
        if (title.isBlank()) {
            _uiState.value = AddItemUiState.Error("Title cannot be empty")
            return
        }
        if (description.isBlank()) {
            _uiState.value = AddItemUiState.Error("Description cannot be empty")
            return
        }
        if (locationName.isBlank()) {
            _uiState.value = AddItemUiState.Error("Location cannot be empty")
            return
        }

        _uiState.value = AddItemUiState.Loading

        viewModelScope.launch {
            try {
                val newItem = Item(
                    id = UUID.randomUUID().toString(),
                    type = type,
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    imageUrl = null,
                    locationName = locationName.trim(),
                    userId = "current_user",        // replaced with real auth later
                    userDisplayName = "You",        // replaced with real auth later
                    timestamp = System.currentTimeMillis()
                )
                repository.createItem(newItem)
                _uiState.value = AddItemUiState.Success
            } catch (e: Exception) {
                _uiState.value = AddItemUiState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun resetState() {
        _uiState.value = AddItemUiState.Idle
    }
}

sealed class AddItemUiState {
    data object Idle : AddItemUiState()
    data object Loading : AddItemUiState()
    data object Success : AddItemUiState()
    data class Error(val message: String) : AddItemUiState()
}