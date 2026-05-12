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

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri

    private val _editItem = MutableStateFlow<Item?>(null)

    private val _imageCleared = MutableStateFlow(false)
    val editItem: StateFlow<Item?> = _editItem

    val isEditMode: Boolean get() = _editItem.value != null

    fun loadItemForEdit(itemId: String) {
        viewModelScope.launch {
            val item = repository.getItemById(itemId)
            _editItem.value = item
        }
    }

    fun onImageSelected(uri: Uri) {
        _selectedImageUri.value = uri
    }

    fun clearSelectedImage() {
        _selectedImageUri.value = null
        if (isEditMode) _imageCleared.value = true
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
        val userPhotoUrl = firebaseUser?.photoUrl?.toString()

        viewModelScope.launch {
            try {
                var imageUrl: String? = _editItem.value?.imageUrl
                val imageUri = _selectedImageUri.value

                when {
                    imageUri != null -> {
                        val uploadResult = storageDataSource.uploadItemImage(imageUri)
                        if (uploadResult.isSuccess) {
                            imageUrl = uploadResult.getOrNull()
                        } else {
                            _uiState.value = AddItemUiState.Error("Image upload failed. Try again.")
                            return@launch
                        }
                    }
                    isEditMode && _imageCleared.value -> {
                        imageUrl = null
                    }
                }

                if (isEditMode) {
                    val existingItem = _editItem.value!!
                    val updatedItem = existingItem.copy(
                        type = type,
                        title = title.trim(),
                        description = description.trim(),
                        category = category,
                        imageUrl = imageUrl,
                        locationName = locationName.trim()
                    )
                    repository.updateItem(updatedItem)
                } else {
                    val newItem = Item(
                        id = "",
                        type = type,
                        title = title.trim(),
                        description = description.trim(),
                        category = category,
                        imageUrl = imageUrl,
                        locationName = locationName.trim(),
                        userId = userId,
                        userDisplayName = userDisplayName,
                        userPhotoUrl = userPhotoUrl,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.createItem(newItem)
                }

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