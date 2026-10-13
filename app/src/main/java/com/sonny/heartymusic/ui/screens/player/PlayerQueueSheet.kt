package com.sonny.heartymusic.ui.screens.player

import android.widget.Toast
import androidx.activity.compose.BackHandler
import java.util.concurrent.TimeUnit
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonny.heartymusic.data.model.Song
import com.sonny.heartymusic.data.repository.LyricLine
import com.sonny.heartymusic.player.PlayerState
import com.sonny.heartymusic.player.QueueState
import com.sonny.heartymusic.ui.theme.CozyAccent
import com.sonny.heartymusic.ui.theme.CozyAccentLight
import com.sonny.heartymusic.ui.theme.CozyAccentSoft
import com.sonny.heartymusic.ui.theme.CozyCardBorder
import com.sonny.heartymusic.ui.theme.CozyMuted
import com.sonny.heartymusic.ui.theme.CozyText

private enum class QueueTab { QUEUE, LYRICS, VIBES }

/**
 * Swipe-up Queue & Up Next view inspired by YouTube / Spotify cozy player designs.
 *
 * Features:
 * - Dynamic queue list with tap-to-play, reorder drag indicator, and contextual options.
 * - Live animated Now Playing card with active equalizer bars and scrub timeline.
 * - Autoplay vibes switch with automated recommendation queuing.
 * - Integrated Lyrics and Vibes mode tabs.
 * - Anchored bottom mini-player bar for uninterrupted playback control.
 */
@Composable
fun PlayerQueueSheet(
    song: Song,
    playerState: PlayerState,
    queueState: QueueState,
    isLoadingMore: Boolean,
    isAutoplay: Boolean,
    isShuffleActive: Boolean,
    isRepeatActive: Boolean,
    favoriteSongIds: Set<String>,
    lyrics: List<LyricLine>,
    activeLyricIndex: Int,
    onDismiss: () -> Unit,
    onPlayQueueItem: (Int) -> Unit,   // absolute index in the player's playlist
    onPlayNext: (Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleAutoplay: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onApplyVibe: (vibeQuery: String, vibeTitle: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(QueueTab.QUEUE) }

    val upNext = queueState.upNext
    val totalDurationMins = remember(queueState.items) {
        queueState.items.sumOf { it.song.durationSeconds } / 60
    }

    BackHandler {
        onDismiss()
    }

    val sheetGradient = Brush.verticalGradient(
        listOf(
            Color(0xFFFFF6F8),
            Color(0xFFFFEEF2),
            Color(0xFFFDE4EC)
        )
    )

    val durationMs = if (playerState.durationMs > 0) playerState.durationMs else (song.durationSeconds * 1000L).coerceAtLeast(1L)
    val progressFraction = (playerState.currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

    // Equalizer dancing bars animation
    val eqTransition = rememberInfiniteTransition(label = "EqWave")
    val eq1 by eqTransition.animateFloat(
        initialValue = 4f,
        targetValue = 13f,
        animationSpec = infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq1"
    )
    val eq2 by eqTransition.animateFloat(
        initialValue = 12f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq2"
    )
    val eq3 by eqTransition.animateFloat(
        initialValue = 6f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq3"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(sheetGradient)
            .statusBarsPadding()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    // Swiping down dismisses sheet
                    if (dragAmount > 22f) {
                        onDismiss()
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 68.dp) // Leave space for anchored mini player
        ) {
            // Drag Handle Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 42.dp, height = 4.5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFB2C9))
                )
            }

            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f))
                        .border(1.dp, Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Close Queue",
                        tint = CozyAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val sourceTitle = playerState.sourceTitle
                    Text(
                        text = if (!sourceTitle.isNullOrBlank()) "PLAYING FROM PLAYLIST ♥" else "PLAYING FROM QUEUE ♥",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CozyAccent,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (!sourceTitle.isNullOrBlank()) "$sourceTitle ♡" else "Sweetheart Serenade ♡",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CozyText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "${queueState.items.size} tracks • ${if (totalDurationMins >= 60) "${totalDurationMins / 60} hr ${totalDurationMins % 60} min" else "$totalDurationMins min"}",
                        fontSize = 11.sp,
                        color = CozyMuted
                    )
                }

                IconButton(
                    onClick = {
                        Toast.makeText(context, "Queue settings ♡", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f))
                        .border(1.dp, Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More options",
                        tint = CozyMuted,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Pill Tabs (Queue, Lyrics, Vibes)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Queue Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .shadow(if (selectedTab == QueueTab.QUEUE) 3.dp else 0.dp, CircleShape, spotColor = CozyAccent)
                        .clip(CircleShape)
                        .background(if (selectedTab == QueueTab.QUEUE) CozyAccent else Color.White.copy(alpha = 0.75f))
                        .border(1.dp, if (selectedTab == QueueTab.QUEUE) Color.Transparent else Color(0xFFFFE0E8), CircleShape)
                        .clickable { selectedTab = QueueTab.QUEUE },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = null,
                            tint = if (selectedTab == QueueTab.QUEUE) Color.White else CozyMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Queue (${upNext.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == QueueTab.QUEUE) Color.White else CozyMuted
                        )
                    }
                }

                // Lyrics Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .shadow(if (selectedTab == QueueTab.LYRICS) 3.dp else 0.dp, CircleShape, spotColor = CozyAccent)
                        .clip(CircleShape)
                        .background(if (selectedTab == QueueTab.LYRICS) CozyAccent else Color.White.copy(alpha = 0.75f))
                        .border(1.dp, if (selectedTab == QueueTab.LYRICS) Color.Transparent else Color(0xFFFFE0E8), CircleShape)
                        .clickable { selectedTab = QueueTab.LYRICS },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Article,
                            contentDescription = null,
                            tint = if (selectedTab == QueueTab.LYRICS) Color.White else CozyMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Lyrics",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == QueueTab.LYRICS) Color.White else CozyMuted
                        )
                    }
                }

                // Vibes Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .shadow(if (selectedTab == QueueTab.VIBES) 3.dp else 0.dp, CircleShape, spotColor = CozyAccent)
                        .clip(CircleShape)
                        .background(if (selectedTab == QueueTab.VIBES) CozyAccent else Color.White.copy(alpha = 0.75f))
                        .border(1.dp, if (selectedTab == QueueTab.VIBES) Color.Transparent else Color(0xFFFFE0E8), CircleShape)
                        .clickable { selectedTab = QueueTab.VIBES },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = if (selectedTab == QueueTab.VIBES) Color.White else CozyMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Vibes",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == QueueTab.VIBES) Color.White else CozyMuted
                        )
                    }
                }
            }

            when (selectedTab) {
                QueueTab.QUEUE -> {
                    // Queue List Content with Now Playing Card
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        // 1. "NOW PLAYING" Interactive Card
                        item {
                            NowPlayingCard(
                                song = song,
                                playerState = playerState,
                                isFavorited = favoriteSongIds.contains(song.id),
                                progressFraction = progressFraction,
                                eq1 = eq1,
                                eq2 = eq2,
                                eq3 = eq3,
                                onTogglePlayPause = onTogglePlayPause,
                                onToggleFavorite = { onToggleFavorite(song) },
                                onSeekTo = onSeekTo
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // 2. Autoplay vibes switch & controls row
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Autoplay switch
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onToggleAutoplay() }
                                ) {
                                    Switch(
                                        checked = isAutoplay,
                                        onCheckedChange = { onToggleAutoplay() },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = CozyAccent,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = Color(0xFFFFCCD9)
                                        ),
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = "Autoplay vibes ✨",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CozyText
                                    )
                                }

                                // Queue control icons (Shuffle, Repeat, Clear Queue)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = onToggleShuffle,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Shuffle,
                                            contentDescription = "Shuffle",
                                            tint = if (isShuffleActive) CozyAccent else CozyMuted,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = onToggleRepeat,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Repeat,
                                            contentDescription = "Repeat",
                                            tint = if (isRepeatActive) CozyAccent else CozyMuted,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    Text(
                                        text = " | ",
                                        color = Color(0xFFFFD5E2),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Clear Queue",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CozyAccent,
                                        modifier = Modifier
                                            .clickable {
                                                onClearQueue()
                                                Toast.makeText(context, "Queue cleared ♡", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // 3. Section Title: "NEXT IN QUEUE"
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "NEXT IN QUEUE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = CozyMuted
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.8f))
                                        .border(1.dp, Color(0xFFFFE0E8), CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${upNext.size} up next",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CozyAccent
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // 4. Queue Items
                        if (upNext.isEmpty() && !isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Queue is empty 🌸",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CozyText
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isAutoplay) "Autoplay will find new cozy songs automatically! ✨" else "Turn on Autoplay to keep the music playing ♡",
                                            fontSize = 11.sp,
                                            color = CozyMuted
                                        )
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = upNext,
                                key = { _, entry -> entry.key },          // unique per row, duplicates can't crash
                                contentType = { _, _ -> "queue_song" }
                            ) { i, entry ->
                                val queueIndex = queueState.currentIndex + 1 + i
                                QueueSongRow(
                                    song = entry.song,
                                    isFavorited = favoriteSongIds.contains(entry.song.id),
                                    onPlay = { onPlayQueueItem(queueIndex) },
                                    onToggleFavorite = { onToggleFavorite(entry.song) },
                                    onPlayNext = if (i > 0) ({ onPlayNext(queueIndex) }) else null,
                                    onRemove = { onRemoveFromQueue(queueIndex) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            if (isLoadingMore) {
                                item(key = "loading_more") { LoadingMoreRow() }
                            }
                        }
                    }
                }

                QueueTab.LYRICS -> {
                    // Synced Lyrics Content
                    val listState = rememberLazyListState()
                    LaunchedEffect(activeLyricIndex) {
                        if (activeLyricIndex >= 0 && lyrics.isNotEmpty()) {
                            listState.animateScrollToItem((activeLyricIndex - 2).coerceAtLeast(0))
                        }
                    }

                    if (lyrics.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Searching cozy lyrics for this song... ✨",
                                fontSize = 13.sp,
                                color = CozyMuted
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            itemsIndexed(lyrics) { idx, line ->
                                val isCurrent = idx == activeLyricIndex
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isCurrent) CozyAccent else Color.Transparent)
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = line.text,
                                        fontSize = if (isCurrent) 16.sp else 14.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCurrent) Color.White else CozyText.copy(alpha = 0.65f),
                                        lineHeight = 22.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }

                QueueTab.VIBES -> {
                    // Vibes Presets
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        item {
                            Text(
                                text = "COZY VIBES PRESETS 🌸",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = CozyMuted
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        val vibesList = listOf(
                            Triple("Hip-Hop & Rap 🔥", "Hard-hitting beats, melodic trap, lyricism", "hip hop rap radio mix songs"),
                            Triple("Pop-Rock Anthems 🎸", "Driving guitars, indie rock, pop punk energy", "pop rock alternative radio mix songs"),
                            Triple("2000s Nostalgia ✨", "Iconic throwback anthems, Y2K pop classics", "2000s throwback nostalgia songs playlist"),
                            Triple("Romantic Lo-Fi 💖", "Soft piano, ambient rain, gentle guitar chords", "lo-fi romantic chill playlist"),
                            Triple("Pink Cloud Dreams ☁️", "Dreamy bedroom pop & ethereal acoustic vibes", "bedroom pop acoustic indie chill"),
                            Triple("Sweetheart R&B 💕", "Silky vocals, neo-soul, late-night grooves", "r&b soul radio mix songs")
                        )

                        itemsIndexed(vibesList) { _, vibe ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color(0x22B388FF))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFFFE8EF), RoundedCornerShape(16.dp))
                                    .clickable {
                                        onApplyVibe(vibe.third, vibe.first)
                                        selectedTab = QueueTab.QUEUE
                                        Toast.makeText(context, "Queue filled with ${vibe.first} ✨", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = vibe.first,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CozyText
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = vibe.second,
                                            fontSize = 11.sp,
                                            color = CozyMuted
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = CozyAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        // Anchored Mini Player Bar at Bottom (matching reference)
        AnchoredQueueMiniPlayer(
            song = song,
            playerState = playerState,
            progressFraction = progressFraction,
            onTogglePlayPause = onTogglePlayPause,
            onPrevious = onPrevious,
            onNext = onNext,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Now Playing card styled exactly like the reference UI:
 * Thumbnail with equalizer bars overlay, song tags, favorite & circular play button.
 */
@Composable
private fun NowPlayingCard(
    song: Song,
    playerState: PlayerState,
    isFavorited: Boolean,
    progressFraction: Float,
    eq1: Float,
    eq2: Float,
    eq3: Float,
    onTogglePlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSeekTo: (Long) -> Unit
) {
    val durationMs = if (playerState.durationMs > 0) playerState.durationMs else (song.durationSeconds * 1000L).coerceAtLeast(1L)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color(0x33FFB2C9))
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFF7F9))
            .border(1.dp, Color(0xFFFFD5E2), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: "● NOW PLAYING" + Output device
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(CozyAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NOW PLAYING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CozyAccent,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFFFE0E8), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Headphones,
                        contentDescription = null,
                        tint = CozyAccent,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Cozy Buds Pro ♡",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CozyText
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Info Row: Artwork + Title/Artist + Heart & Play buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail with equalizer bars overlay
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(3.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFCCD9))
                ) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Equalizer overlay on bottom-right of thumbnail
                    if (playerState.isPlaying) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .background(Color(0x99000000), RoundedCornerShape(3.dp))
                                .padding(horizontal = 3.dp, vertical = 2.dp)
                        ) {
                            Box(modifier = Modifier.width(2.5.dp).height(eq1.dp).background(Color.White, CircleShape))
                            Box(modifier = Modifier.width(2.5.dp).height(eq2.dp).background(Color.White, CircleShape))
                            Box(modifier = Modifier.width(2.5.dp).height(eq3.dp).background(Color.White, CircleShape))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Title + Artist + Tag & Time
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CozyText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFFFDDE6))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "ORIGINAL",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = CozyAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${song.artist} ✨ • Hearty Music",
                        fontSize = 11.sp,
                        color = CozyMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFFFDDE6), CircleShape)
                                .padding(horizontal = 6.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "Romantic Lo-Fi",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CozyAccent
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${formatMs(playerState.currentPositionMs)} / ${formatMs(durationMs)}",
                            fontSize = 10.sp,
                            color = CozyMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Actions: Heart + Circular Play/Pause button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = "Favorite",
                            tint = if (isFavorited) CozyAccent else CozyMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(3.dp, CircleShape, spotColor = CozyAccent)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(CozyAccent, CozyAccentLight)))
                            .clickable(onClick = onTogglePlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            var dragFraction by remember { mutableStateOf<Float?>(null) }

            // Scrubber Progress Bar
            Slider(
                value = dragFraction ?: progressFraction,
                onValueChange = { dragFraction = it },
                onValueChangeFinished = {
                    dragFraction?.let { onSeekTo((it * durationMs).toLong()) }
                    dragFraction = null
                },
                colors = SliderDefaults.colors(
                    thumbColor = CozyAccent,
                    activeTrackColor = CozyAccent,
                    inactiveTrackColor = Color(0xFFFFD5E2)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            )
        }
    }
}

/**
 * Single song card in the Up Next queue.
 */
@Composable
private fun QueueSongRow(
    song: Song,
    isFavorited: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayNext: (() -> Unit)?,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color(0x18000000))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFFFE8EF), RoundedCornerShape(16.dp))
            .clickable(onClick = onPlay)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFFCCD9))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Song Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CozyText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val mins = song.durationSeconds / 60
                val secs = song.durationSeconds % 60
                Text(
                    text = "${song.artist} • $mins:${secs.toString().padStart(2, '0')}",
                    fontSize = 11.sp,
                    color = CozyMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Heart Favorite
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = "Favorite",
                    tint = if (isFavorited) CozyAccent else CozyMuted.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Context Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Options",
                        tint = CozyMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Play Now ♡") },
                        onClick = {
                            showMenu = false
                            onPlay()
                        }
                    )
                    if (onPlayNext != null) {
                        DropdownMenuItem(
                            text = { Text("Play Next") },
                            onClick = {
                                showMenu = false
                                onPlayNext()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Remove from Queue") },
                        onClick = {
                            showMenu = false
                            onRemove()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Anchored Mini Player bar at the bottom of the Queue Sheet.
 */
@Composable
private fun AnchoredQueueMiniPlayer(
    song: Song,
    playerState: PlayerState,
    progressFraction: Float,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .shadow(10.dp, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFFFD5E2), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
    ) {
        // Red / pink progress line on very top of mini player
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color(0xFFFFCCD9))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressFraction)
                    .height(3.dp)
                    .background(CozyAccent)
            )
        }

        // Mini player content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFFFFCCD9))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${song.title} ♡",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CozyText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val durationMs = if (playerState.durationMs > 0) playerState.durationMs else (song.durationSeconds * 1000L).coerceAtLeast(1L)
                Text(
                    text = "${song.artist} ✨ • ${formatMs(playerState.currentPositionMs)} / ${formatMs(durationMs)}",
                    fontSize = 10.sp,
                    color = CozyMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = CozyText,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .shadow(3.dp, CircleShape, spotColor = CozyAccent)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(CozyAccent, CozyAccentLight)))
                    .clickable(onClick = onTogglePlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = CozyText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun LoadingMoreRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 2.dp,
            color = CozyAccent
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Finding similar songs ✨", fontSize = 11.sp, color = CozyMuted)
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
