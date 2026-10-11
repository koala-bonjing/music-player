package com.sonny.heartymusic.data.repository

import com.sonny.heartymusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class MusicRepository {

    suspend fun searchSongs(query: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatchingWithRetry {
            val searchExtractor = ServiceList.YouTube.getSearchExtractor(query)
            searchExtractor.fetchPage()
            val page = searchExtractor.initialPage
            page.items
                .filterIsInstance<StreamInfoItem>()
                .map { item ->
                    val videoId = extractVideoId(item.url) ?: item.url
                    val thumbUrl = item.thumbnails.maxByOrNull { it.height }?.url
                        ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                    Song(
                        id = videoId,
                        title = item.name.orEmpty().ifBlank { "Untitled" },
                        artist = item.uploaderName.orEmpty().ifBlank { "Unknown Artist" },
                        durationSeconds = item.duration.coerceAtLeast(0L),
                        thumbnailUrl = thumbUrl
                    )
                }
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
        } catch (firstError: Exception) {
            try {
                // Retry once as mandated by rules
                Result.success(block())
            } catch (secondError: Exception) {
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
}
