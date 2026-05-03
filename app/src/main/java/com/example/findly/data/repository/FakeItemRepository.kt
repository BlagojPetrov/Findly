package com.example.findly.data.repository

import com.example.findly.domain.model.Category
import com.example.findly.domain.model.Item
import com.example.findly.domain.model.ItemType
import com.example.findly.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeItemRepository private constructor() : ItemRepository {

    private val _items = MutableStateFlow(dummyItems())

    override fun getItemsFeed(): Flow<List<Item>> = _items

    override fun getItemsByType(type: ItemType): Flow<List<Item>> =
        _items.map { list -> list.filter { it.type == type } }

    override fun searchItems(query: String): Flow<List<Item>> =
        _items.map { list ->
            if (query.isBlank()) list
            else list.filter { item ->
                item.title.contains(query, ignoreCase = true) ||
                        item.description.contains(query, ignoreCase = true) ||
                        item.locationName?.contains(query, ignoreCase = true) == true
            }
        }

    override suspend fun getItemById(id: String): Item? {
        return _items.value.find { it.id == id }
    }

    override suspend fun createItem(item: Item) {
        val current = _items.value.toMutableList()
        current.add(0, item)
        _items.value = current
    }

    override fun getUserItems(userId: String): Flow<List<Item>> =
        _items.map { list -> list.filter { it.userId == userId } }

    override suspend fun deleteItem(itemId: String) {
        val current = _items.value.toMutableList()
        current.removeAll { it.id == itemId }
        _items.value = current
    }

    companion object {
        @Volatile
        private var INSTANCE: FakeItemRepository? = null

        fun getInstance(): FakeItemRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FakeItemRepository().also { INSTANCE = it }
            }
        }
    }

    private fun dummyItems() = listOf(
        Item(
            id = "1",
            type = ItemType.LOST,
            title = "Black iPhone 14 Pro",
            description = "Lost near the city park. Has a cracked screen protector and a dark green case.",
            category = Category.ELECTRONICS,
            imageUrl = null,
            locationName = "City Park, Bitola",
            userId = "user_1",
            userDisplayName = "Marko S.",
            timestamp = System.currentTimeMillis() - 3_600_000L  // 1 hour ago
        ),
        Item(
            id = "2",
            type = ItemType.FOUND,
            title = "Set of Keys",
            description = "Found a set of keys with a red keychain near the bus station. About 4 keys total.",
            category = Category.KEYS,
            imageUrl = null,
            locationName = "Bus Station, Bitola",
            userId = "user_2",
            userDisplayName = "Ana K.",
            timestamp = System.currentTimeMillis() - 7_200_000L  // 2 hours ago
        ),
        Item(
            id = "3",
            type = ItemType.LOST,
            title = "Blue Backpack",
            description = "Nike backpack, blue and black. Contains university books and a charger inside.",
            category = Category.CLOTHING,
            imageUrl = null,
            locationName = "UKLO Faculty, Bitola",
            userId = "user_3",
            userDisplayName = "Stefan P.",
            timestamp = System.currentTimeMillis() - 86_400_000L  // 1 day ago
        ),
        Item(
            id = "4",
            type = ItemType.FOUND,
            title = "Student ID Card",
            description = "Found an ID card near the library. Name on card: M. Ilievski.",
            category = Category.DOCUMENTS,
            imageUrl = null,
            locationName = "City Library, Bitola",
            userId = "user_4",
            userDisplayName = "Elena V.",
            timestamp = System.currentTimeMillis() - 10_800_000L  // 3 hours ago
        ),
        Item(
            id = "5",
            type = ItemType.LOST,
            title = "Silver Watch",
            description = "Lost a Casio silver watch. Sentimental value, reward offered.",
            category = Category.ACCESSORIES,
            imageUrl = null,
            locationName = "Shirok Sokak, Bitola",
            userId = "user_5",
            userDisplayName = "Nikola T.",
            timestamp = System.currentTimeMillis() - 172_800_000L  // 2 days ago
        ),
        Item(
            id = "6",
            type = ItemType.FOUND,
            title = "Brown Leather Wallet",
            description = "Found a wallet with some cash and cards inside. Waiting for the owner to claim it.",
            category = Category.WALLET,
            imageUrl = null,
            locationName = "Magnolija Mall, Bitola",
            userId = "user_6",
            userDisplayName = "Jana M.",
            timestamp = System.currentTimeMillis() - 1_800_000L  // 30 min ago
        )
    )
}