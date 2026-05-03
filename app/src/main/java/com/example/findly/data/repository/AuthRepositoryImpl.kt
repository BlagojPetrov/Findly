package com.example.findly.data.repository

import com.example.findly.data.remote.auth.AuthDataSource
import com.example.findly.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl(
    private val dataSource: AuthDataSource = AuthDataSource()
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = dataSource.currentUser

    override val authStateFlow: Flow<FirebaseUser?>
        get() = dataSource.authStateFlow

    override suspend fun signInWithEmail(email: String, password: String) =
        dataSource.signInWithEmail(email, password)

    override suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String
    ) = dataSource.registerWithEmail(email, password, displayName)

    override suspend fun signInWithGoogle(idToken: String) =
        dataSource.signInWithGoogle(idToken)

    override suspend fun signInAnonymously() =
        dataSource.signInAnonymously()

    override suspend fun updateProfilePhoto(photoUrl: String) =
        dataSource.updateProfilePhoto(photoUrl)

    override suspend fun removeProfilePhoto() =
        dataSource.removeProfilePhoto()
    override fun signOut() = dataSource.signOut()
}