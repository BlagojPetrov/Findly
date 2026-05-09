package com.example.findly.data.repository

import com.example.findly.data.remote.fcm.FcmTokenSource
import com.example.findly.domain.repository.FcmRepository

class FcmRepositoryImpl(
    private val fcmTokenSource: FcmTokenSource = FcmTokenSource()
) : FcmRepository {

    override suspend fun saveTokenForUser(userId: String): Result<Unit> {
        return try {
            val token = fcmTokenSource.fetchCurrentToken()
            fcmTokenSource.saveTokenToFirestore(userId, token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTokenForUser(userId: String): Result<Unit> {
        return try {
            fcmTokenSource.deleteTokenFromFirestore(userId)
            fcmTokenSource.deleteInstanceToken()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}