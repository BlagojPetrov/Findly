package com.example.findly.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.findly.data.local.FindlyDatabase
import com.example.findly.data.repository.FirestoreItemRepository
import com.example.findly.data.repository.SavedRepositoryImpl
import com.example.findly.domain.model.Item
import com.example.findly.domain.repository.ItemRepository
import com.example.findly.domain.repository.SavedRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val itemRepository: ItemRepository = FirestoreItemRepository()
    private val savedRepository: SavedRepository = SavedRepositoryImpl(
        FindlyDatabase.getInstance(application).savedItemDao()
    )

    // ← this was missing from your file
    private val firebaseUser = FirebaseAuth.getInstance().currentUser

    val currentUserId: String = firebaseUser?.uid ?: "guest"

    val uiState: StateFlow<ProfileUiState> = combine(
        itemRepository.getUserItems(currentUserId),
        savedRepository.getSavedItems(currentUserId)
    ) { myItems, savedItems ->
        ProfileUiState.Success(
            displayName = firebaseUser?.displayName
                ?: firebaseUser?.email?.substringBefore("@")
                ?: "Guest User",
            email       = firebaseUser?.email,
            photoUrl    = firebaseUser?.photoUrl?.toString(),
            isAnonymous = firebaseUser?.isAnonymous ?: true,
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
        val isAnonymous: Boolean,
        val myItems: List<Item>,
        val savedCount: Int
    ) : ProfileUiState()
}