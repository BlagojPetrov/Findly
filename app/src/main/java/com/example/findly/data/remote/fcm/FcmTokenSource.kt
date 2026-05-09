package com.example.findly.data.remote.fcm

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class FcmTokenSource {

    private val db = FirebaseFirestore.getInstance()
    private val messaging = FirebaseMessaging.getInstance()

    suspend fun fetchCurrentToken(): String {
        return messaging.token.await()
    }

    suspend fun saveTokenToFirestore(userId: String, token: String) {
        db.collection("users")
            .document(userId)
            .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    suspend fun deleteTokenFromFirestore(userId: String) {
        db.collection("users")
            .document(userId)
            .update("fcmToken", null)
            .await()
    }

    suspend fun deleteInstanceToken() {
        messaging.deleteToken().await()
    }
}