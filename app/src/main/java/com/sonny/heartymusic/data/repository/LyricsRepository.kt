package com.sonny.heartymusic.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class LyricLine(
    val timeMs: Long,
    val text: String
)

class LyricsRepository(
    private val client: OkHttpClient
) {
    suspend fun getLyrics(title: String, artist: String, durationSeconds: Long = 0L): List<LyricLine> = withContext(Dispatchers.IO) {
        val (cleanTitle, cleanArtist) = sanitizeQuery(title, artist)

        val urlBuilder = "https://lrclib.net/api/get".toHttpUrlOrNull()?.newBuilder()
            ?: return@withContext emptyList()

        urlBuilder.addQueryParameter("track_name", cleanTitle)
        if (cleanArtist.isNotBlank()) {
            urlBuilder.addQueryParameter("artist_name", cleanArtist)
        }
        if (durationSeconds > 0) {
            urlBuilder.addQueryParameter("duration", durationSeconds.toString())
        }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("User-Agent", "HeartyMusic/1.0 (Android)")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)

                val synced = json.optString("syncedLyrics", "")
                if (synced.isNotBlank()) {
                    return@withContext parseSyncedLyrics(synced)
                }

                val plain = json.optString("plainLyrics", "")
                if (plain.isNotBlank()) {
                    return@withContext parsePlainLyrics(plain, durationSeconds)
                }
            }
        } catch (_: Exception) {
            // Graceful fallback on network or parse error
        }
        emptyList()
    }

    private fun parseSyncedLyrics(synced: String): List<LyricLine> {
        val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")
        val list = mutableListOf<LyricLine>()
        synced.lineSequence().forEach { line ->
            val match = regex.matchEntire(line.trim())
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val fractionStr = match.groupValues[3]
                val ms = if (fractionStr.length == 2) {
                    (fractionStr.toLongOrNull() ?: 0L) * 10
                } else {
                    fractionStr.toLongOrNull() ?: 0L
                }
                val text = match.groupValues[4].trim()
                if (text.isNotBlank()) {
                    list.add(LyricLine(min * 60000L + sec * 1000L + ms, text))
                }
            }
        }
        return list
    }

    private fun parsePlainLyrics(plain: String, durationSeconds: Long): List<LyricLine> {
        val lines = plain.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val stepMs = if (durationSeconds > 0) (durationSeconds * 1000L) / lines.size else 4000L
        return lines.mapIndexed { index, line ->
            LyricLine(index * stepMs, line)
        }
    }

    private fun sanitizeQuery(title: String, artist: String): Pair<String, String> {
        var cleanTitle = title
        var cleanArtist = artist

        if (cleanTitle.contains(" - ")) {
            val parts = cleanTitle.split(" - ", limit = 2)
            if (cleanArtist.isBlank() || cleanArtist.contains("Topic", ignoreCase = true) || cleanArtist.contains("VEVO", ignoreCase = true)) {
                cleanArtist = parts[0].trim()
            }
            cleanTitle = parts[1].trim()
        }

        val cleanupRegex = Regex("""(?i)\(official[^\)]*\)|\[official[^\]]*\]|\(lyrics?\)|\[lyrics?\]|\(audio\)|\(video\)|\(feat[^\)]*\)""")
        cleanTitle = cleanTitle.replace(cleanupRegex, "").trim()

        return Pair(cleanTitle, cleanArtist)
    }
}
