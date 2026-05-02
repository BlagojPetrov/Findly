package com.example.findly.data.remote.firestore

import com.example.findly.data.remote.firestore.dto.ItemDto
import com.example.findly.domain.model.ItemType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreItemSource {

    private val db = FirebaseFirestore.getInstance()
    private val itemsCollection = db.collection("items")

    fun observeItems(): Flow<List<ItemDto>> = callbackFlow {
        val listener = itemsCollection
            .whereEqualTo("status", "ACTIVE")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull {
                    it.toObject(ItemDto::class.java)
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    fun observeItemsByType(type: ItemType): Flow<List<ItemDto>> = callbackFlow {
        val listener = itemsCollection
            .whereEqualTo("status", "ACTIVE")
            .whereEqualTo("type", type.name)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull {
                    it.toObject(ItemDto::class.java)
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    fun observeUserItems(userId: String): Flow<List<ItemDto>> = callbackFlow {
        val listener = itemsCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull {
                    it.toObject(ItemDto::class.java)
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getItemById(id: String): ItemDto? {
        return try {
            itemsCollection.document(id).get().await()
                .toObject(ItemDto::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createItem(dto: ItemDto): String {
        val doc = if (dto.id.isEmpty()) itemsCollection.document()
        else itemsCollection.document(dto.id)
        doc.set(dto).await()
        return doc.id
    }

    fun searchItems(query: String): Flow<List<ItemDto>> = callbackFlow {
        val listener = itemsCollection
            .whereEqualTo("status", "ACTIVE")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents
                    ?.mapNotNull { it.toObject(ItemDto::class.java) }
                    ?.filter { item ->
                        item.title.contains(query, ignoreCase = true) ||
                                item.description.contains(query, ignoreCase = true) ||
                                item.locationName?.contains(query, ignoreCase = true) == true
                    } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }
}