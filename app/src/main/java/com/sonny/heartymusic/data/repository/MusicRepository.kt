package com.sonny.heartymusic.data.repository

import com.sonny.heartymusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class MusicRepository {

    suspend fun searchSongs(query: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatchingWithRetry {
            val searchExtractor = ServiceList.YouTube.getSearchExtractor(query)
            searchExtractor.fetchPage()
            val page = searchExtractor.initialPage
            page.items
                .filterIsInstance<StreamInfoItem>()
                .map { it.toSong() }
        }
    }

    /**
     * "Radio" for a song = YouTube's own auto-generated mix (list=RD<videoId>).
     * It picks songs by listening patterns, so it's far better than guessing a genre from keywords.
     */
    suspend fun getRadio(seed: Song): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://www.youtube.com/watch?v=${seed.id}&list=RD${seed.id}"
            PlaylistInfo.getInfo(ServiceList.YouTube, url)
                .relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { it.toSong() }
        }.recoverCatching {
            // Mix unavailable: fall back to a plain artist search
            searchSongs("${seed.artist} songs").getOrThrow()
        }
    }

    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        try {
            ServiceList.YouTube.getSuggestionExtractor().suggestionList(trimmed)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun resolveStreamUrl(videoId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatchingWithRetry {
            val watchUrl = "https://www.youtube.com/watch?v=$videoId"
            val streamExtractor = ServiceList.YouTube.getStreamExtractor(watchUrl)
            streamExtractor.fetchPage()

            val audioStreams = streamExtractor.audioStreams
            val bestAudio = audioStreams.maxByOrNull { it.averageBitrate }
                ?: audioStreams.firstOrNull()
                ?: throw IllegalStateException("No audio streams available for $videoId")

            bestAudio.content ?: throw IllegalStateException("Audio stream URL was empty")
        }
    }

    private inline fun <T> runCatchingWithRetry(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (firstError: Throwable) {
            try {
                // Retry once as mandated by rules
                Result.success(block())
            } catch (secondError: Throwable) {
                Result.failure(secondError)
            }
        }
    }

    private fun extractVideoId(url: String): String? {
        if (url.contains("v=")) {
            val id = url.substringAfter("v=").substringBefore("&")
            if (id.isNotBlank()) return id
        }
        if (url.contains("youtu.be/")) {
            val id = url.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
            if (id.isNotBlank()) return id
        }
        return null
    }

    private fun StreamInfoItem.toSong(): Song {
        val videoId = extractVideoId(url) ?: url
        val thumbUrl = thumbnails.maxByOrNull { it.height }?.url
            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        return Song(
            id = videoId,
            title = name.orEmpty().ifBlank { "Untitled" },
            artist = uploaderName.orEmpty().ifBlank { "Unknown Artist" },
            durationSeconds = duration.coerceAtLeast(0L),
            thumbnailUrl = thumbUrl
        )
    }
}
