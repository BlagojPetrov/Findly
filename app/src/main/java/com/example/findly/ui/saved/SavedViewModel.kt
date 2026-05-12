package com.example.findly.ui.saved

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.findly.data.local.FindlyDatabase
import com.example.findly.data.repository.FirestoreItemRepository
import com.example.findly.data.repository.SavedRepositoryImpl
import com.example.findly.domain.model.Item
import com.example.findly.domain.repository.ItemRepository
import com.example.findly.domain.repository.SavedRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavedViewModel(application: Application) : AndroidViewModel(application) {

    private val currentUserId: String =
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "guest"

    private val repository: SavedRepository = SavedRepositoryImpl(
        FindlyDatabase.getInstance(application).savedItemDao()
    )

    private val itemRepository: ItemRepository = FirestoreItemRepository()

    val uiState: StateFlow<SavedUiState> = repository.getSavedItems(currentUserId)
        .onEach { items -> validateSavedItems(items) }
        .map { items ->
            if (items.isEmpty()) SavedUiState.Empty
            else SavedUiState.Success(items)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SavedUiState.Loading
        )

    private fun validateSavedItems(items: List<Item>) {
        viewModelScope.launch {
            items.forEach { item ->
                val exists = itemRepository.getItemById(item.id)
                if (exists == null) {
                    repository.removeSavedItem(item.id, currentUserId)
                }
            }
        }
    }

    fun removeSavedItem(itemId: String) {
        viewModelScope.launch {
            repository.removeSavedItem(itemId, currentUserId)
        }
    }
}

sealed class SavedUiState {
    data object Loading : SavedUiState()
    data object Empty : SavedUiState()
    data class Success(val items: List<Item>) : SavedUiState()
}