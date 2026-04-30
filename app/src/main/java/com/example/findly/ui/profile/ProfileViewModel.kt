package com.example.findly.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.findly.data.local.FindlyDatabase
import com.example.findly.data.repository.FakeItemRepository
import com.example.findly.data.repository.SavedRepositoryImpl
import com.example.findly.domain.model.Item
import com.example.findly.domain.repository.ItemRepository
import com.example.findly.domain.repository.SavedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val itemRepository: ItemRepository = FakeItemRepository.getInstance()
    private val savedRepository: SavedRepository = SavedRepositoryImpl(
        FindlyDatabase.getInstance(application).savedItemDao()
    )

    // Hardcoded for now — replaced with real auth data after Firebase integration
    val currentUserId = "current_user"

    val uiState: StateFlow<ProfileUiState> = combine(
        itemRepository.getUserItems(currentUserId),
        savedRepository.getSavedItems()
    ) { myItems, savedItems ->
        ProfileUiState.Success(
            displayName = "Guest User",
            email       = null,
            photoUrl    = null,
            myItems     = myItems,
            savedCount  = savedItems.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState.Loading
    )
}

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(
        val displayName: String,
        val email: String?,
        val photoUrl: String?,
        val myItems: List<Item>,
        val savedCount: Int
    ) : ProfileUiState()
}