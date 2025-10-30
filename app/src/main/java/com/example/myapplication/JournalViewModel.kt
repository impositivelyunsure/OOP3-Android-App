package com.example.myapplication

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class SortMethod { BUBBLE, INSERTION, SELECTION }

class JournalViewModel : ViewModel() {

    private val _items = MutableStateFlow<List<JournalEntry>>(emptyList())
    val items: StateFlow<List<JournalEntry>> = _items

    private val _searchResults = MutableStateFlow<List<JournalEntry>>(emptyList())
    val searchResults: StateFlow<List<JournalEntry>> = _searchResults

    var currentSearch: SearchMethod = SearchMethod.HASHMAP
        private set

    var currentSort: SortMethod = SortMethod.BUBBLE
        private set

    fun add(entry: JournalEntry) {
        _items.update { it + entry }
        //sort(currentSort)
    }

    fun remove(id: Long) {
        _items.update { it.filterNot { e -> e.id == id } }
    }

    fun sort(method: SortMethod) {
        currentSort = method
        val mutable = _items.value.toMutableList()
        SortUtils.sortByEmotion(mutable, method)
        _items.value = mutable
    }

    fun setSearchMethod(m: SearchMethod) { currentSearch = m }

    fun searchByEmotion(emotion: Emotion) {
        _searchResults.value = SearchUtils.searchByEmotion(_items.value, emotion, currentSearch)
    }
}
