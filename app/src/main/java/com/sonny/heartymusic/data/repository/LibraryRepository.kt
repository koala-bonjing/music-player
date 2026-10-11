package com.sonny.heartymusic.data.repository

import com.sonny.heartymusic.data.local.dao.HeartyDao
import com.sonny.heartymusic.data.local.entity.FavoriteSongEntity
import com.sonny.heartymusic.data.local.entity.PlayHistoryEntity
import com.sonny.heartymusic.data.local.entity.PlaylistEntity
import com.sonny.heartymusic.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID

class LibraryRepository(
    private val dao: HeartyDao
) {
    val favorites: Flow<List<Song>> = dao.getAllFavorites().map { list ->
        list.map { it.toSong() }
    }

    val favoriteIds: Flow<Set<String>> = dao.getAllFavoriteIds().map { it.toSet() }

    val recentHistory: Flow<List<Song>> = dao.getRecentHistory(10).map { list ->
        list.map { it.toSong() }
    }

    val playlists: Flow<List<PlaylistEntity>> = dao.getAllPlaylists()

    suspend fun toggleFavorite(song: Song) {
        val exists = dao.isFavorite(song.id).firstOrNull() ?: false
        if (exists) {
            dao.deleteFavorite(song.id)
        } else {
            dao.insertFavorite(song.toFavoriteEntity())
        }
    }

    suspend fun removeFavorite(songId: String) {
        dao.deleteFavorite(songId)
    }

    suspend fun addFavorite(song: Song) {
        dao.insertFavorite(song.toFavoriteEntity())
    }

    suspend fun recordPlay(song: Song) {
        dao.recordHistory(song.toHistoryEntity())
    }

    suspend fun createPlaylist(title: String, description: String = "", coverEmoji: String = "💌") {
        val id = UUID.randomUUID().toString()
        dao.insertPlaylist(
            PlaylistEntity(
                id = id,
                title = title,
                description = description,
                coverEmoji = coverEmoji
            )
        )
    }

    suspend fun deletePlaylist(playlistId: String) {
        dao.deletePlaylist(playlistId)
    }
}

fun Song.toFavoriteEntity() = FavoriteSongEntity(
    id = id,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds
)

fun Song.toHistoryEntity() = PlayHistoryEntity(
    id = id,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds
)

fun FavoriteSongEntity.toSong() = Song(
    id = id,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds
)

fun PlayHistoryEntity.toSong() = Song(
    id = id,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds
)
