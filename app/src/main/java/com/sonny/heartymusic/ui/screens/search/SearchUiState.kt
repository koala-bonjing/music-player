package com.sonny.heartymusic.ui.screens.search

import com.sonny.heartymusic.data.model.Song

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val songs: List<Song>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}
