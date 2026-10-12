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

    fun getSongsForPlaylist(playlistId: String): Flow<List<Song>> =
        dao.getSongsForPlaylist(playlistId).map { list ->
            list.map { it.toSong() }
        }

    suspend fun addSongToPlaylist(playlistId: String, song: Song) {
        dao.addSongToPlaylist(
            com.sonny.heartymusic.data.local.entity.PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = song.id,
                title = song.title,
                artist = song.artist,
                thumbnailUrl = song.thumbnailUrl,
                durationSeconds = song.durationSeconds
            )
        )
        dao.updatePlaylistSongCount(playlistId)
    }

    suspend fun createPlaylistAndAddSong(title: String, coverEmoji: String = "💌", song: Song): String {
        val id = UUID.randomUUID().toString()
        dao.insertPlaylist(
            PlaylistEntity(
                id = id,
                title = title,
                description = "Created by you",
                coverEmoji = coverEmoji,
                songCount = 1
            )
        )
        dao.addSongToPlaylist(
            com.sonny.heartymusic.data.local.entity.PlaylistSongCrossRef(
                playlistId = id,
                songId = song.id,
                title = song.title,
                artist = song.artist,
                thumbnailUrl = song.thumbnailUrl,
                durationSeconds = song.durationSeconds
            )
        )
        return id
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        dao.removeSongFromPlaylist(playlistId, songId)
        dao.updatePlaylistSongCount(playlistId)
    }

    suspend fun deletePlaylist(playlistId: String) {
        dao.deleteSongsForPlaylist(playlistId)
        dao.deletePlaylist(playlistId)
    }
}

fun com.sonny.heartymusic.data.local.entity.PlaylistSongCrossRef.toSong() = Song(
    id = songId,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds
)

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
