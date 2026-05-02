package com.example.findly.data.remote.storage

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StorageDataSource {

    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun uploadItemImage(imageUri: Uri): Result<String> {
        return try {
            val userId: String = auth.currentUser?.uid ?: "anonymous"
            val fileName: String = "items/$userId/${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child(fileName)
            ref.putFile(imageUri).await()
            val downloadUrl: Uri = ref.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}