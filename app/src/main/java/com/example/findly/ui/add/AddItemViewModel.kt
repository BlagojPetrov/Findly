package com.example.findly.ui.add

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.findly.data.remote.storage.StorageDataSource
import com.example.findly.data.repository.FirestoreItemRepository
import com.example.findly.domain.model.Category
import com.example.findly.domain.model.Item
import com.example.findly.domain.model.ItemType
import com.example.findly.domain.repository.ItemRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddItemViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ItemRepository = FirestoreItemRepository()
    private val storageDataSource = StorageDataSource()

    private val _uiState = MutableStateFlow<AddItemUiState>(AddItemUiState.Idle)
    val uiState: StateFlow<AddItemUiState> = _uiState

    // Holds the selected image URI before upload
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri

    fun onImageSelected(uri: Uri) {
        _selectedImageUri.value = uri
    }

    fun clearSelectedImage() {
        _selectedImageUri.value = null
    }

    fun submitItem(
        type: ItemType,
        title: String,
        description: String,
        category: Category,
        locationName: String
    ) {
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

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val userId = firebaseUser?.uid ?: "guest"
        val userDisplayName = firebaseUser?.displayName
            ?: firebaseUser?.email?.substringBefore("@")
            ?: "Anonymous"

        viewModelScope.launch {
            try {
                // Upload image first if one is selected
                val imageUrl = _selectedImageUri.value?.let { uri ->
                    storageDataSource.uploadItemImage(uri)
                        .getOrNull()
                }

                val newItem = Item(
                    id              = "",
                    type            = type,
                    title           = title.trim(),
                    description     = description.trim(),
                    category        = category,
                    imageUrl        = imageUrl,
                    locationName    = locationName.trim(),
                    userId          = userId,
                    userDisplayName = userDisplayName,
                    timestamp       = System.currentTimeMillis()
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