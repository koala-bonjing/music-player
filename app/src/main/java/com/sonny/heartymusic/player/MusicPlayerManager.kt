package com.sonny.heartymusic.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.sonny.heartymusic.data.model.Song
import com.sonny.heartymusic.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val error: String? = null
)

class MusicPlayerManager(
    private val context: Context,
    @Suppress("UNUSED_PARAMETER") private val musicRepository: MusicRepository? = null
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressTrackerJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var pendingSongToPlay: Song? = null

    init {
        connectToMediaService()
    }

    private fun connectToMediaService() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, HeartyMediaService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future

        future.addListener({
            try {
                val controller = future.get()
                mediaController = controller
                setupController(controller)
            } catch (e: Exception) {
                _playerState.update {
                    it.copy(error = "Media service connection error: ${e.message}")
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupController(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startTrackingProgress()
                } else {
                    stopTrackingProgress()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _playerState.update { it.copy(isLoading = true, error = null) }
                    }
                    Player.STATE_READY -> {
                        _playerState.update {
                            it.copy(
                                isLoading = false,
                                durationMs = controller.duration.coerceAtLeast(0L),
                                error = null
                            )
                        }
                    }
                    Player.STATE_ENDED -> {
                        _playerState.update { it.copy(isPlaying = false, isLoading = false) }
                        stopTrackingProgress()
                    }
                    Player.STATE_IDLE -> {
                        _playerState.update { it.copy(isLoading = false) }
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem != null) {
                    val song = Song(
                        id = mediaItem.mediaId,
                        title = mediaItem.mediaMetadata.title?.toString().orEmpty(),
                        artist = mediaItem.mediaMetadata.artist?.toString().orEmpty(),
                        thumbnailUrl = mediaItem.mediaMetadata.artworkUri?.toString().orEmpty(),
                        durationSeconds = mediaItem.mediaMetadata.extras?.getLong("durationSeconds") ?: 0L
                    )
                    _playerState.update { it.copy(currentSong = song) }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _playerState.update {
                    it.copy(
                        isLoading = false,
                        isPlaying = false,
                        error = error.localizedMessage ?: "Playback error encountered"
                    )
                }
            }
        })

        // Restore active playback state if the service was already running in the background
        controller.currentMediaItem?.let { item ->
            val restoredSong = Song(
                id = item.mediaId,
                title = item.mediaMetadata.title?.toString().orEmpty(),
                artist = item.mediaMetadata.artist?.toString().orEmpty(),
                thumbnailUrl = item.mediaMetadata.artworkUri?.toString().orEmpty(),
                durationSeconds = item.mediaMetadata.extras?.getLong("durationSeconds") ?: 0L
            )
            _playerState.update {
                it.copy(
                    currentSong = restoredSong,
                    isPlaying = controller.isPlaying,
                    isLoading = controller.playbackState == Player.STATE_BUFFERING,
                    currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
                    durationMs = controller.duration.coerceAtLeast(0L)
                )
            }
            if (controller.isPlaying) {
                startTrackingProgress()
            }
        }

        // If a song play request arrived before controller was connected
        pendingSongToPlay?.let { song ->
            pendingSongToPlay = null
            play(song)
        }
    }

    fun play(song: Song) {
        _playerState.update {
            it.copy(
                currentSong = song,
                isLoading = true,
                error = null,
                currentPositionMs = 0L,
                durationMs = song.durationSeconds * 1000L
            )
        }

        val controller = mediaController
        if (controller == null) {
            pendingSongToPlay = song
            return
        }

        val extras = Bundle().apply {
            putLong("durationSeconds", song.durationSeconds)
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setArtworkUri(Uri.parse(song.thumbnailUrl))
            .setExtras(extras)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse("hearty://track/${song.id}"))
            .setMediaId(song.id)
            .setMediaMetadata(metadata)
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    fun release() {
        stopTrackingProgress()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
    }

    private fun startTrackingProgress() {
        progressTrackerJob?.cancel()
        progressTrackerJob = scope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null) {
                    val currentPos = controller.currentPosition.coerceAtLeast(0L)
                    val duration = controller.duration.coerceAtLeast(0L)
                    _playerState.update {
                        it.copy(
                            currentPositionMs = currentPos,
                            durationMs = if (duration > 0L) duration else it.durationMs
                        )
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopTrackingProgress() {
        progressTrackerJob?.cancel()
        progressTrackerJob = null
    }
}
