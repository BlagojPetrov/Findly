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
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                val userId = firebaseUser?.uid ?: "guest"
                val userDisplayName = firebaseUser?.displayName
                    ?: firebaseUser?.email?.substringBefore("@")
                    ?: "Anonymous"
                val userPhotoUrl = firebaseUser?.photoUrl?.toString()

                // Upload image and wait for URL before proceeding
                var imageUrl: String? = null
                val imageUri = _selectedImageUri.value

                if (imageUri != null) {
                    android.util.Log.d("AddItemVM", "Uploading image: $imageUri")
                    val uploadResult = storageDataSource.uploadItemImage(imageUri)
                    if (uploadResult.isSuccess) {
                        imageUrl = uploadResult.getOrNull()
                        android.util.Log.d("AddItemVM", "Image URL: $imageUrl")
                    } else {
                        android.util.Log.e("AddItemVM", "Upload failed: ${uploadResult.exceptionOrNull()?.message}")
                        _uiState.value = AddItemUiState.Error("Image upload failed. Try again.")
                        return@launch          // ← stop here, don't save to Firestore
                    }
                }

                // Only reaches here after image is uploaded (or no image selected)
                val newItem = Item(
                    id              = "",
                    type            = type,
                    title           = title.trim(),
                    description     = description.trim(),
                    category        = category,
                    imageUrl        = imageUrl,    // ← guaranteed to be the real URL or null
                    locationName    = locationName.trim(),
                    userId          = userId,
                    userDisplayName = userDisplayName,
                    userPhotoUrl    = userPhotoUrl,
                    timestamp       = System.currentTimeMillis()
                )

                android.util.Log.d("AddItemVM", "Saving item with imageUrl: $imageUrl")
                repository.createItem(newItem)
                _uiState.value = AddItemUiState.Success

            } catch (e: Exception) {
                android.util.Log.e("AddItemVM", "Error: ${e.message}", e)
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