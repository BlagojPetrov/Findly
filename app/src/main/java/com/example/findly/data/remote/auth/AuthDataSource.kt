package com.example.findly.data.remote.auth

import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthDataSource {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user!!
            if (!user.isEmailVerified) {
                auth.signOut()
                return Result.failure(EmailNotVerifiedException())
            }
            saveUserToFirestore(user)
            Result.success(user)
        } catch (e: EmailNotVerifiedException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user!!
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            user.updateProfile(profileUpdates).await()
            user.sendEmailVerification().await()
            saveUserToFirestore(user)
            auth.signOut()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user!!
            saveUserToFirestore(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithFacebook(
        token: String,
        name: String?,
        photoUrl: String?
    ): Result<FirebaseUser> {
        return try {
            val credential = FacebookAuthProvider.getCredential(token)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user!!

            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name ?: "")
                .setPhotoUri(photoUrl?.let { android.net.Uri.parse(it) })
                .build()

            user.updateProfile(profileUpdates).await()

            auth.currentUser?.reload()?.await()

            saveUserToFirestore(auth.currentUser!!)

            Result.success(user)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val user = auth.currentUser
                ?: return Result.failure(Exception("No user signed in"))
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reloadUser(): Result<Unit> {
        return try {
            auth.currentUser?.reload()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified == true
    }

    suspend fun updateProfilePhoto(photoUrl: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Not signed in"))
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setPhotoUri(android.net.Uri.parse(photoUrl))
                .build()
            user.updateProfile(profileUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeProfilePhoto(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Not signed in"))
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setPhotoUri(null)
                .build()
            user.updateProfile(profileUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveUserToFirestore(user: FirebaseUser) {
        val userDoc = db.collection("users").document(user.uid)
        val snapshot = userDoc.get().await()
        if (!snapshot.exists()) {
            userDoc.set(
                mapOf(
                    "uid" to user.uid,
                    "displayName" to (user.displayName ?: ""),
                    "email" to (user.email ?: ""),
                    "photoUrl" to (user.photoUrl?.toString() ?: ""),
                    "fcmToken" to ""
                )
            ).await()
        }
    }

    fun signOut() = auth.signOut()
}

class EmailNotVerifiedException : Exception("Email not verified")