package com.example.findly.ui.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.findly.data.local.FindlyDatabase
import com.example.findly.data.remote.storage.StorageDataSource
import com.example.findly.data.repository.AuthRepositoryImpl
import com.example.findly.data.repository.FirestoreItemRepository
import com.example.findly.data.repository.SavedRepositoryImpl
import com.example.findly.domain.model.Item
import com.example.findly.domain.repository.AuthRepository
import com.example.findly.domain.repository.ItemRepository
import com.example.findly.domain.repository.SavedRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val itemRepository: ItemRepository = FirestoreItemRepository()
    private val savedRepository: SavedRepository = SavedRepositoryImpl(
        FindlyDatabase.getInstance(application).savedItemDao()
    )
    private val authRepository: AuthRepository = AuthRepositoryImpl()
    private val storageDataSource = StorageDataSource()

    private val firebaseUser get() = FirebaseAuth.getInstance().currentUser

    val currentUserId: String get() = firebaseUser?.uid ?: "guest"

    private val _photoUpdateState = MutableStateFlow<PhotoUpdateState>(PhotoUpdateState.Idle)
    val photoUpdateState: StateFlow<PhotoUpdateState> = _photoUpdateState

    // Triggers profile recomposition after photo update
    private val _refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<ProfileUiState> = combine(
        itemRepository.getUserItems(currentUserId),
        savedRepository.getSavedItems(currentUserId),
        _refreshTrigger
    ) { myItems, savedItems, _ ->
        val user = FirebaseAuth.getInstance().currentUser
        ProfileUiState.Success(
            displayName = user?.displayName
                ?: user?.email?.substringBefore("@")
                ?: "Guest User",
            email       = user?.email,
            photoUrl    = user?.photoUrl?.toString(),
            isAnonymous = user?.isAnonymous ?: true,
            myItems     = myItems,
            savedCount  = savedItems.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState.Loading
    )

    fun uploadProfilePhoto(uri: Uri) {
        _photoUpdateState.value = PhotoUpdateState.Loading
        viewModelScope.launch {
            // Step 1 — upload to Storage
            val uploadResult = storageDataSource.uploadProfilePhoto(uri)
            if (uploadResult.isFailure) {
                _photoUpdateState.value = PhotoUpdateState.Error(
                    uploadResult.exceptionOrNull()?.message ?: "Upload failed"
                )
                return@launch
            }

            val photoUrl = uploadResult.getOrNull()!!

            // Step 2 — update Firebase Auth profile
            val updateResult = authRepository.updateProfilePhoto(photoUrl)
            if (updateResult.isFailure) {
                _photoUpdateState.value = PhotoUpdateState.Error(
                    updateResult.exceptionOrNull()?.message ?: "Profile update failed"
                )
                return@launch
            }

            // Step 3 — trigger UI refresh
            _refreshTrigger.value++
            _photoUpdateState.value = PhotoUpdateState.Success
        }
    }

    fun resetPhotoState() {
        _photoUpdateState.value = PhotoUpdateState.Idle
    }

    fun removeProfilePhoto() {
        _photoUpdateState.value = PhotoUpdateState.Loading
        viewModelScope.launch {
            val result = authRepository.removeProfilePhoto()
            if (result.isSuccess) {
                _refreshTrigger.value++
                _photoUpdateState.value = PhotoUpdateState.Success
            } else {
                _photoUpdateState.value = PhotoUpdateState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to remove photo"
                )
            }
        }
    }
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

sealed class PhotoUpdateState {
    data object Idle : PhotoUpdateState()
    data object Loading : PhotoUpdateState()
    data object Success : PhotoUpdateState()
    data class Error(val message: String) : PhotoUpdateState()
}