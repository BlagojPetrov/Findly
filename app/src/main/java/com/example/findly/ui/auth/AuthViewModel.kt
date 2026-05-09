package com.example.findly.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findly.data.repository.AuthRepositoryImpl
import com.example.findly.data.repository.FcmRepositoryImpl
import com.example.findly.domain.repository.AuthRepository
import com.example.findly.domain.repository.FcmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository: AuthRepository = AuthRepositoryImpl()
    private val fcmRepository: FcmRepository = FcmRepositoryImpl()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun signInWithEmail(email: String, password: String) {
        if (!validateInput(email, password)) return
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.signInWithEmail(email, password)
                .onSuccess { user ->
                    fcmRepository.saveTokenForUser(user.uid)
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Sign in failed") }
        }
    }

    fun registerWithEmail(email: String, password: String, confirmPassword: String, displayName: String) {
        if (displayName.isBlank()) {
            _uiState.value = AuthUiState.Error("Display name cannot be empty")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = AuthUiState.Error("Passwords do not match")
            return
        }
        if (!validateInput(email, password)) return
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.registerWithEmail(email, password, displayName)
                .onSuccess { user ->
                    fcmRepository.saveTokenForUser(user.uid)
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Registration failed") }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.signInWithGoogle(idToken)
                .onSuccess { user ->
                    fcmRepository.saveTokenForUser(user.uid)
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Google sign in failed") }
        }
    }

    fun signInAnonymously() {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.signInAnonymously()
                .onSuccess { _uiState.value = AuthUiState.Success }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Anonymous sign in failed") }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = AuthUiState.Error("Enter a valid email address")
            return false
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
            return false
        }
        return true
    }
}

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}