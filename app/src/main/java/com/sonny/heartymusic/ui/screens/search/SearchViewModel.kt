package com.sonny.heartymusic.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonny.heartymusic.data.model.Song
import com.sonny.heartymusic.data.repository.MusicRepository
import com.sonny.heartymusic.player.MusicPlayerManager
import com.sonny.heartymusic.player.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val musicRepository: MusicRepository,
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val playerState: StateFlow<PlayerState> = playerManager.playerState

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun search() {
        val currentQuery = _query.value.trim()
        if (currentQuery.isBlank()) return

        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            musicRepository.searchSongs(currentQuery)
                .onSuccess { songs ->
                    _uiState.value = if (songs.isEmpty()) {
                        SearchUiState.Error("No tracks found for \"$currentQuery\"")
                    } else {
                        SearchUiState.Success(songs)
                    }
                }
                .onFailure { error ->
                    _uiState.value = SearchUiState.Error(
                        error.localizedMessage ?: "Unable to fetch search results. Check your connection."
                    )
                }
        }
    }

    fun retry() {
        search()
    }

    fun playSong(song: Song) {
        val currentList = (_uiState.value as? SearchUiState.Success)?.songs
        playerManager.play(song, currentList)
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }
}
