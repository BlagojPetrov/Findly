package com.example.findly.domain.repository

interface FcmRepository {
    suspend fun saveTokenForUser(userId: String): Result<Unit>
    suspend fun deleteTokenForUser(userId: String): Result<Unit>
}