package com.example.findly.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findly.data.repository.FakeItemRepository
import com.example.findly.domain.model.Item
import com.example.findly.domain.model.ItemType
import com.example.findly.domain.repository.ItemRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel : ViewModel() {

    // Will be injected via Hilt later — for now we instantiate directly
    private val repository: ItemRepository = FakeItemRepository()

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow<ItemType?>(null)  // null = show all

    val uiState: StateFlow<HomeUiState> = combine(
        _searchQuery.debounce(300),
        _activeFilter
    ) { query, filter -> Pair(query, filter) }
        .flatMapLatest { (query, filter) ->
            val source = when {
                query.isNotBlank() -> repository.searchItems(query)
                filter != null     -> repository.getItemsByType(filter)
                else               -> repository.getItemsFeed()
            }
            source.map<List<Item>, HomeUiState> { HomeUiState.Success(it) }
                .onStart { emit(HomeUiState.Loading) }
                .catch { e -> emit(HomeUiState.Error(e.message ?: "Unknown error")) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChanged(type: ItemType?) {
        _activeFilter.value = type
    }
}

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val items: List<Item>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}