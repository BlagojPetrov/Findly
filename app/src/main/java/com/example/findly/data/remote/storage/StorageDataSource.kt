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

            // Step 1 - upload the file and wait
            val uploadTask = ref.putFile(imageUri).await()
            android.util.Log.d("Storage", "Upload complete: ${uploadTask.metadata?.path}")

            // Step 2 - get download URL and wait
            val downloadUrl: String = ref.downloadUrl.await().toString()
            android.util.Log.d("Storage", "Download URL: $downloadUrl")

            Result.success(downloadUrl)
        } catch (e: Exception) {
            android.util.Log.e("Storage", "Upload failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun uploadProfilePhoto(imageUri: Uri): Result<String> {
        return try {
            val userId: String = auth.currentUser?.uid
                ?: return Result.failure(Exception("Not signed in"))
            val fileName: String = "profiles/$userId/avatar.jpg"
            val ref = storage.reference.child(fileName)

            ref.putFile(imageUri).await()
            val downloadUrl: String = ref.downloadUrl.await().toString()
            android.util.Log.d("Storage", "Profile photo URL: $downloadUrl")

            Result.success(downloadUrl)
        } catch (e: Exception) {
            android.util.Log.e("Storage", "Profile upload failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}