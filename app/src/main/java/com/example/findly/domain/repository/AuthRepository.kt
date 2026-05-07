package com.example.findly.domain.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: FirebaseUser?
    val authStateFlow: Flow<FirebaseUser?>
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser>
    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<FirebaseUser>
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser>
    suspend fun signInAnonymously(): Result<FirebaseUser>

    suspend fun updateProfilePhoto(photoUrl: String): Result<Unit>

    suspend fun removeProfilePhoto(): Result<Unit>
    fun signOut()
}