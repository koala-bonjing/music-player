package com.sonny.heartymusic.player

import android.net.Uri
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import com.sonny.heartymusic.data.repository.MusicRepository
import kotlinx.coroutines.runBlocking

class ResolvingAudioSource(
    private val musicRepository: MusicRepository
) : ResolvingDataSource.Resolver {

    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val uri = dataSpec.uri
        if (uri.scheme == "hearty" && uri.host == "track") {
            val videoId = uri.lastPathSegment
                ?: throw IllegalArgumentException("Missing videoId in stream URI: $uri")

            val streamUrl = runBlocking {
                musicRepository.resolveStreamUrl(videoId)
            }.getOrThrow()

            return dataSpec.buildUpon()
                .setUri(Uri.parse(streamUrl))
                .build()
        }
        return dataSpec
    }
}
