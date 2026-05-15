package com.example.findly.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findly.data.remote.auth.EmailNotVerifiedException
import com.example.findly.data.repository.AuthRepositoryImpl
import com.example.findly.data.repository.FcmRepositoryImpl
import com.example.findly.domain.repository.AuthRepository
import com.example.findly.domain.repository.FcmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val repository: AuthRepository = AuthRepositoryImpl()
    private val fcmRepository: FcmRepository = FcmRepositoryImpl()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    private val _forgotPasswordState = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val forgotPasswordState: StateFlow<ForgotPasswordState> = _forgotPasswordState

    private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val verificationState: StateFlow<VerificationState> = _verificationState

    fun signInWithEmail(email: String, password: String) {
        if (!validateInput(email, password)) return
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.signInWithEmail(email, password)
                .onSuccess { user ->
                    fcmRepository.saveTokenForUser(user.uid)
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { error ->
                    if (error is EmailNotVerifiedException) {
                        _uiState.value = AuthUiState.EmailNotVerified
                    } else {
                        _uiState.value = AuthUiState.Error(error.message ?: "Sign in failed")
                    }
                }
        }
    }

    fun registerWithEmail(
        email: String,
        password: String,
        confirmPassword: String,
        displayName: String
    ) {
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
                .onSuccess {
                    _uiState.value = AuthUiState.VerificationEmailSent
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "Registration failed")
                }
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
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "Google sign in failed")
                }
        }
    }

    fun signInWithFacebook(token: String, name: String?, photoUrl: String?) {
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            repository.signInWithFacebook(token, name, photoUrl)
                .onSuccess { user ->

                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest
                        .Builder()
                        .setDisplayName(name ?: "User")
                        .setPhotoUri(
                            photoUrl?.let { android.net.Uri.parse(it) }
                        )
                        .build()

                    try {
                        user.updateProfile(profileUpdates).await()
                        user.reload().await()

                        fcmRepository.saveTokenForUser(user.uid)
                        _uiState.value = AuthUiState.Success

                    } catch (e: Exception) {
                        _uiState.value =
                            AuthUiState.Error(e.message ?: "Profile update failed")
                    }
                }
                .onFailure {
                    _uiState.value =
                        AuthUiState.Error(it.message ?: "Facebook sign in failed")
                }
        }
    }

    fun signInAnonymously() {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.signInAnonymously()
                .onSuccess { _uiState.value = AuthUiState.Success }
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "Anonymous sign in failed")
                }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _forgotPasswordState.value = ForgotPasswordState.Error("Enter a valid email address")
            return
        }
        _forgotPasswordState.value = ForgotPasswordState.Loading
        viewModelScope.launch {
            repository.sendPasswordResetEmail(email)
                .onSuccess {
                    _forgotPasswordState.value = ForgotPasswordState.Success
                }
                .onFailure {
                    _forgotPasswordState.value = ForgotPasswordState.Error(
                        it.message ?: "Failed to send reset email"
                    )
                }
        }
    }

    fun resendVerificationEmail() {
        _verificationState.value = VerificationState.Loading
        viewModelScope.launch {
            repository.sendEmailVerification()
                .onSuccess {
                    _verificationState.value = VerificationState.EmailResent
                }
                .onFailure {
                    _verificationState.value = VerificationState.Error(
                        it.message ?: "Failed to resend verification email"
                    )
                }
        }
    }

    fun checkEmailVerification() {
        _verificationState.value = VerificationState.Loading
        viewModelScope.launch {
            repository.reloadUser()
                .onSuccess {
                    if (repository.isEmailVerified()) {
                        val userId = repository.currentUser?.uid
                        if (userId != null) fcmRepository.saveTokenForUser(userId)
                        repository.clearPendingVerificationUser() // <-- ADD THIS
                        _verificationState.value = VerificationState.Verified
                    } else {
                        _verificationState.value = VerificationState.NotVerified
                    }
                }
                .onFailure {
                    _verificationState.value = VerificationState.Error(
                        it.message ?: "Failed to check verification status"
                    )
                }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun resetForgotPasswordState() {
        _forgotPasswordState.value = ForgotPasswordState.Idle
    }

    fun resetVerificationState() {
        _verificationState.value = VerificationState.Idle
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
    data object EmailNotVerified : AuthUiState()
    data object VerificationEmailSent : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

sealed class ForgotPasswordState {
    data object Idle : ForgotPasswordState()
    data object Loading : ForgotPasswordState()
    data object Success : ForgotPasswordState()
    data class Error(val message: String) : ForgotPasswordState()
}

sealed class VerificationState {
    data object Idle : VerificationState()
    data object Loading : VerificationState()
    data object Verified : VerificationState()
    data object NotVerified : VerificationState()
    data object EmailResent : VerificationState()
    data class Error(val message: String) : VerificationState()
}