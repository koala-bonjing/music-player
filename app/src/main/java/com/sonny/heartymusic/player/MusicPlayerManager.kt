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
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.sonny.heartymusic.data.model.Song
import com.sonny.heartymusic.data.repository.LibraryRepository
import com.sonny.heartymusic.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val KEY_DURATION = "durationSeconds"
private const val KEY_QID = "qid"

/** Refill the queue with radio songs when fewer than this many songs are left. */
private const val REFILL_THRESHOLD = 3
private const val RADIO_BATCH = 15

/** Remember the last N played ids so autoplay doesn't serve the same songs again. */
private const val RECENT_LIMIT = 40

/** Autoplay only picks real songs: skips Shorts, live streams (0s) and 1-hour compilations. */
private val PLAYABLE_SECONDS = 60L..720L

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val error: String? = null,
    val sourceTitle: String? = null
)

/**
 * One row in the queue. [key] is unique per row (not per song), so the same song can be in
 * the queue twice and LazyColumn keys never collide.
 */
data class QueueEntry(val key: Long, val song: Song)

/**
 * Snapshot of the player's playlist. Built once per change, not once per recomposition.
 * [upNext] = everything after the current song, in play order.
 */
data class QueueState(
    val items: List<QueueEntry> = emptyList(),
    val currentIndex: Int = -1
) {
    val upNext: List<QueueEntry> =
        if (currentIndex in items.indices) items.subList(currentIndex + 1, items.size) else emptyList()
}

/**
 * Mental model: the queue lives INSIDE the Media3 player (a real playlist of MediaItems).
 * This class only edits it and mirrors it into [queueState] for the UI. That's why
 * notification / lock-screen / Bluetooth next-previous now work and songs switch without gaps.
 */
class MusicPlayerManager(
    private val context: Context,
    private val musicRepository: MusicRepository? = null,
    private val libraryRepository: LibraryRepository? = null
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressTrackerJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _favoriteSongIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteSongIds: StateFlow<Set<String>> = _favoriteSongIds.asStateFlow()

    private val _isShuffleMode = MutableStateFlow(false)
    val isShuffleMode: StateFlow<Boolean> = _isShuffleMode.asStateFlow()

    private val _isRepeatMode = MutableStateFlow(false)
    val isRepeatMode: StateFlow<Boolean> = _isRepeatMode.asStateFlow()

    private val _isAutoplay = MutableStateFlow(true)
    val isAutoplay: StateFlow<Boolean> = _isAutoplay.asStateFlow()

    private val _queueState = MutableStateFlow(QueueState())
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()

    /** Kept so existing screens that read `queue: List<Song>` still compile. */
    val queue: StateFlow<List<Song>> = _queueState
        .map { state -> state.items.map { it.song } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** True while autoplay / vibe songs are being fetched. Drives the "finding songs" row. */
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var pendingPlay: (() -> Unit)? = null

    private var refillJob: Job? = null
    private val recentIds = ArrayDeque<String>()
    private var lastCountedKey = -1L

    /** Seeded with the clock so keys restored from a still-running service can't collide. */
    private var keyCounter = System.currentTimeMillis()

    /** Up-next order before shuffle was turned on, so turning it off can restore it. */
    private var originalOrder: List<Long>? = null

    init {
        connectToMediaService()
        libraryRepository?.let { repo ->
            scope.launch {
                repo.favoriteIds.collect { ids ->
                    _favoriteSongIds.value = ids
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Connection
    // ---------------------------------------------------------------------------------------

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
                if (isPlaying) startTrackingProgress() else stopTrackingProgress()
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
                        // Only reached when there is NO next item (repeat-one and a filled
                        // queue never end). Autoplay is the last chance to keep music going.
                        _playerState.update { it.copy(isPlaying = false, isLoading = false) }
                        stopTrackingProgress()
                        if (_isAutoplay.value) advanceWithRadio()
                    }
                    Player.STATE_IDLE -> {
                        _playerState.update { it.copy(isLoading = false) }
                    }
                }
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                syncQueue()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncQueue()
                val entry = mediaItem?.toEntry() ?: return
                _playerState.update {
                    it.copy(
                        currentSong = entry.song,
                        currentPositionMs = 0L,
                        durationMs = entry.song.durationSeconds * 1000L
                    )
                }
                rememberPlayed(entry, reason)
                ensureUpcoming()
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

        // Restore state if the service was already playing in the background
        syncQueue()
        controller.currentMediaItem?.let { item ->
            _playerState.update {
                it.copy(
                    currentSong = item.toEntry().song,
                    isPlaying = controller.isPlaying,
                    isLoading = controller.playbackState == Player.STATE_BUFFERING,
                    currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
                    durationMs = controller.duration.coerceAtLeast(0L)
                )
            }
            if (controller.isPlaying) startTrackingProgress()
        }

        // A play request that arrived before the controller was connected
        pendingPlay?.let { action ->
            pendingPlay = null
            action()
        }
    }

    /** Mirror the player's playlist into [queueState]. Called from player callbacks only. */
    private fun syncQueue() {
        val c = mediaController ?: return
        val count = c.mediaItemCount
        val items = ArrayList<QueueEntry>(count)
        for (i in 0 until count) items += c.getMediaItemAt(i).toEntry()
        _queueState.value = QueueState(
            items = items,
            currentIndex = if (count == 0) -1 else c.currentMediaItemIndex
        )
    }

    // ---------------------------------------------------------------------------------------
    // Playing
    // ---------------------------------------------------------------------------------------

    /**
     * - [queue] with 2+ songs  -> plays them as a playlist, starting at [song].
     * - otherwise (search tap) -> queue is just [song]; autoplay fills in similar songs.
     */
    fun play(song: Song, queue: List<Song>? = null, sourceTitle: String? = null) {
        val songs = when {
            queue != null && queue.size > 1 ->
                if (queue.any { it.id == song.id }) queue else listOf(song) + queue
            else -> listOf(song)
        }
        val start = songs.indexOfFirst { it.id == song.id }
        val isSingleSeed = songs.size == 1

        refillJob?.cancel()
        _playerState.update {
            it.copy(
                currentSong = song,
                sourceTitle = sourceTitle
                    ?: if (isSingleSeed && _isAutoplay.value) "${song.artist} Radio ♡" else null,
                isLoading = true,
                error = null,
                currentPositionMs = 0L,
                durationMs = song.durationSeconds * 1000L
            )
        }

        val controller = mediaController
        if (controller == null) {
            pendingPlay = { play(song, queue, sourceTitle) }
            return
        }

        val entries = songs.map { QueueEntry(nextKey(), it) }
        var ordered = entries
        var startIndex = start
        originalOrder = null
        if (_isShuffleMode.value && entries.size > 1) {
            val head = entries[start]
            val rest = entries.filterIndexed { i, _ -> i != start }
            originalOrder = rest.map { it.key }
            ordered = listOf(head) + rest.shuffled()
            startIndex = 0
        }

        controller.setMediaItems(ordered.map { it.toMediaItem() }, startIndex, 0L)
        controller.prepare()
        controller.play()
        // ensureUpcoming() runs from onMediaItemTransition, so autoplay needs no call here.
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    fun seekTo(positionMs: Long) {
        _playerState.update { it.copy(currentPositionMs = positionMs) }
        mediaController?.seekTo(positionMs)
    }

    /** Media3 handles "restart if past 3s, else previous song" itself. */
    fun seekToPrevious() {
        mediaController?.seekToPrevious()
    }

    fun seekToNext() {
        val c = mediaController ?: return
        when {
            c.hasNextMediaItem() -> c.seekToNextMediaItem()
            _isAutoplay.value -> advanceWithRadio()
            else -> c.seekTo(0)
        }
    }

    // ---------------------------------------------------------------------------------------
    // Queue editing (all indexes are absolute positions in the player's playlist)
    // ---------------------------------------------------------------------------------------

    fun skipToQueueItem(index: Int) {
        val c = mediaController ?: return
        if (index in 0 until c.mediaItemCount) {
            c.seekTo(index, 0L)
            c.play()
        }
    }

    /** Kept for old call sites; prefer [skipToQueueItem]. */
    fun playFromQueue(song: Song) {
        val index = _queueState.value.items.indexOfFirst { it.song.id == song.id }
        if (index >= 0) skipToQueueItem(index) else play(song)
    }

    fun removeQueueItem(index: Int) {
        val c = mediaController ?: return
        if (index in 0 until c.mediaItemCount && index != c.currentMediaItemIndex) {
            c.removeMediaItem(index)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val c = mediaController ?: return
        val last = c.mediaItemCount - 1
        if (fromIndex in 0..last && toIndex in 0..last && fromIndex != toIndex) {
            c.moveMediaItem(fromIndex, toIndex)
        }
    }

    /** Move a queued song so it plays right after the current one. */
    fun moveToPlayNext(index: Int) {
        val c = mediaController ?: return
        val target = c.currentMediaItemIndex + 1
        if (index > target && index < c.mediaItemCount) c.moveMediaItem(index, target)
    }

    /** Keeps only the current song. */
    fun clearQueue() {
        val c = mediaController ?: return
        val current = c.currentMediaItemIndex
        if (current + 1 < c.mediaItemCount) c.removeMediaItems(current + 1, c.mediaItemCount)
        if (current > 0) c.removeMediaItems(0, current)
        originalOrder = null
    }

    /**
     * Shuffle reorders the songs after the current one (what you see is what plays).
     * Turning it off restores the original order; songs added meanwhile stay at the end.
     */
    fun toggleShuffle() {
        val next = !_isShuffleMode.value
        _isShuffleMode.value = next
        val c = mediaController ?: return
        val tail = _queueState.value.upNext
        if (tail.size < 2) {
            originalOrder = if (next) tail.map { it.key } else null
            return
        }
        val reordered = if (next) {
            originalOrder = tail.map { it.key }
            tail.shuffled()
        } else {
            val position = originalOrder?.withIndex()?.associate { it.value to it.index }
            originalOrder = null
            if (position == null) tail else tail.sortedBy { position[it.key] ?: Int.MAX_VALUE }
        }
        replaceUpNext(c, reordered)
    }

    fun toggleRepeat() {
        val next = !_isRepeatMode.value
        _isRepeatMode.value = next
        mediaController?.repeatMode = if (next) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun toggleAutoplay() {
        val next = !_isAutoplay.value
        _isAutoplay.value = next
        if (next) ensureUpcoming()
    }

    private fun replaceUpNext(c: MediaController, entries: List<QueueEntry>) {
        val from = c.currentMediaItemIndex + 1
        if (from < c.mediaItemCount) c.removeMediaItems(from, c.mediaItemCount)
        c.addMediaItems(entries.map { it.toMediaItem() })
    }

    // ---------------------------------------------------------------------------------------
    // Autoplay / generated queue
    // ---------------------------------------------------------------------------------------

    /** Top up the queue before it runs dry, so the next song is already waiting. */
    private fun ensureUpcoming() {
        if (!_isAutoplay.value || refillJob?.isActive == true) return
        val state = _queueState.value
        if (state.currentIndex < 0 || state.upNext.size >= REFILL_THRESHOLD) return
        val seed = state.items.last().song
        launchLoading { appendRadio(seed) }
    }

    /** Queue ended (or user hit next at the end): fetch more, then keep playing. */
    private fun advanceWithRadio() {
        val seed = _queueState.value.items.lastOrNull()?.song ?: return
        _playerState.update { it.copy(isLoading = true) }
        launchLoading {
            appendRadio(seed)
            val c = mediaController
            if (c != null && c.hasNextMediaItem()) {
                c.seekToNextMediaItem()
                c.play()
            } else {
                _playerState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Appends songs similar to [seed]. Result is dropped if the queue changed while fetching
     * (user started another song), and anything already queued or recently played is skipped.
     */
    private suspend fun appendRadio(seed: Song) {
        val repo = musicRepository ?: return
        val known = knownIds()
        val fresh = repo.getRadio(seed).getOrNull().orEmpty()
            .asSequence()
            .filter { it.durationSeconds in PLAYABLE_SECONDS }
            .distinctBy { it.id }
            .filter { it.id !in known }
            .take(RADIO_BATCH)
            .toList()
        if (fresh.isEmpty()) return

        val c = mediaController ?: return
        if (_queueState.value.items.lastOrNull()?.song?.id != seed.id) return
        c.addMediaItems(fresh.map { QueueEntry(nextKey(), it).toMediaItem() })
    }

    /** Replace everything after the current song with a vibe preset's songs. */
    fun populateVibeQueue(vibeQuery: String, vibeTitle: String) {
        val current = _playerState.value.currentSong ?: return
        val repo = musicRepository ?: return
        _playerState.update { it.copy(sourceTitle = vibeTitle) }
        launchLoading {
            val fresh = repo.searchSongs(vibeQuery).getOrNull().orEmpty()
                .asSequence()
                .filter { it.durationSeconds in PLAYABLE_SECONDS }
                .distinctBy { it.id }
                .filter { it.id != current.id }
                .take(20)
                .toList()
            val c = mediaController ?: return@launchLoading
            if (fresh.isEmpty() || _playerState.value.currentSong?.id != current.id) return@launchLoading
            clearQueue()
            c.addMediaItems(fresh.map { QueueEntry(nextKey(), it).toMediaItem() })
        }
    }

    /** Old entry point; autoplay now seeds from the end of the queue. */
    @Deprecated("Autoplay refills itself", ReplaceWith("Unit"))
    fun populateGenreQueue(@Suppress("UNUSED_PARAMETER") song: Song) = ensureUpcoming()

    /** Only one load runs at a time; starting a new one cancels the old. */
    private fun launchLoading(block: suspend () -> Unit): Job {
        refillJob?.cancel()
        val job = scope.launch { block() }
        refillJob = job
        _isLoadingMore.value = true
        job.invokeOnCompletion { if (refillJob === job) _isLoadingMore.value = false }
        return job
    }

    private fun knownIds(): Set<String> {
        val ids = HashSet<String>(recentIds)
        _queueState.value.items.forEach { ids += it.song.id }
        return ids
    }

    private fun rememberPlayed(entry: QueueEntry, reason: Int) {
        val isRepeat = reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
        if (entry.key == lastCountedKey && !isRepeat) return
        lastCountedKey = entry.key

        recentIds.remove(entry.song.id)
        recentIds.addLast(entry.song.id)
        while (recentIds.size > RECENT_LIMIT) recentIds.removeFirst()

        scope.launch {
            try {
                libraryRepository?.recordPlay(entry.song)
            } catch (e: Exception) {
                // Ignore recording failure
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Favorites (unchanged)
    // ---------------------------------------------------------------------------------------

    fun toggleFavorite(song: Song) {
        val songId = song.id
        _favoriteSongIds.update { set ->
            if (set.contains(songId)) set - songId else set + songId
        }
        scope.launch {
            try {
                libraryRepository?.toggleFavorite(song)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun toggleFavorite(songId: String) {
        val current = _playerState.value.currentSong
        if (current != null && current.id == songId) {
            toggleFavorite(current)
        } else {
            _favoriteSongIds.update { set ->
                if (set.contains(songId)) set - songId else set + songId
            }
            scope.launch {
                try {
                    val isFav = _favoriteSongIds.value.contains(songId)
                    if (!isFav) {
                        libraryRepository?.removeFavorite(songId)
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Lifecycle / progress
    // ---------------------------------------------------------------------------------------

    fun release() {
        stopTrackingProgress()
        refillJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
    }

    private fun startTrackingProgress() {
        progressTrackerJob?.cancel()
        progressTrackerJob = scope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.playbackState != Player.STATE_BUFFERING) {
                    val currentPos = controller.currentPosition.coerceAtLeast(0L)
                    val duration = controller.duration.coerceAtLeast(0L)
                    _playerState.update {
                        it.copy(
                            currentPositionMs = currentPos,
                            durationMs = if (duration > 0L) duration else it.durationMs
                        )
                    }
                }
                delay(100)
            }
        }
    }

    private fun stopTrackingProgress() {
        progressTrackerJob?.cancel()
        progressTrackerJob = null
    }

    private fun nextKey(): Long = ++keyCounter
}

// -------------------------------------------------------------------------------------------
// Song <-> MediaItem
// -------------------------------------------------------------------------------------------

/**
 * Note: the URI stays hearty://track/<id>. Stream URLs expire, so HeartyMediaService must
 * resolve that URI when the item is opened (ResolvingDataSource), not when it's queued.
 */
private fun QueueEntry.toMediaItem(): MediaItem {
    val extras = Bundle().apply {
        putLong(KEY_DURATION, song.durationSeconds)
        putLong(KEY_QID, key)
    }
    val metadata = MediaMetadata.Builder()
        .setTitle(song.title)
        .setArtist(song.artist)
        .setArtworkUri(Uri.parse(song.thumbnailUrl))
        .setExtras(extras)
        .build()
    return MediaItem.Builder()
        .setUri(Uri.parse("hearty://track/${song.id}"))
        .setMediaId(song.id)
        .setMediaMetadata(metadata)
        .build()
}

private fun MediaItem.toEntry(): QueueEntry {
    val extras = mediaMetadata.extras
    return QueueEntry(
        key = extras?.getLong(KEY_QID) ?: 0L,
        song = Song(
            id = mediaId,
            title = mediaMetadata.title?.toString().orEmpty(),
            artist = mediaMetadata.artist?.toString().orEmpty(),
            thumbnailUrl = mediaMetadata.artworkUri?.toString().orEmpty(),
            durationSeconds = extras?.getLong(KEY_DURATION) ?: 0L
        )
    )
}
