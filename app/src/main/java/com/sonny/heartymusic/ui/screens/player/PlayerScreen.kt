package com.sonny.heartymusic.ui.screens.player

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.sonny.heartymusic.data.local.entity.PlaylistEntity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonny.heartymusic.data.model.Song
import com.sonny.heartymusic.data.repository.LyricLine
import com.sonny.heartymusic.data.repository.LyricsRepository
import com.sonny.heartymusic.player.PlayerState
import com.sonny.heartymusic.ui.theme.CozyAccent
import com.sonny.heartymusic.ui.theme.CozyAccentLight
import com.sonny.heartymusic.ui.theme.CozyAccentSoft
import com.sonny.heartymusic.ui.theme.CozyCard
import com.sonny.heartymusic.ui.theme.CozyCardBorder
import com.sonny.heartymusic.ui.theme.CozyMuted
import com.sonny.heartymusic.ui.theme.CozyText
import java.util.concurrent.TimeUnit

@Composable
fun PlayerScreen(
    song: Song,
    playerState: PlayerState,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onDismiss: () -> Unit,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    isShuffleActive: Boolean = false,
    isRepeatActive: Boolean = false,
    isFavorited: Boolean = false,
    lyricsRepository: LyricsRepository? = null,
    playlists: List<PlaylistEntity> = emptyList(),
    onAddToPlaylist: (playlistId: String, song: Song) -> Unit = { _, _ -> },
    onCreatePlaylistAndAddSong: (title: String, emoji: String, song: Song) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLyricsExpanded by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    var lyrics by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var isLoadingLyrics by remember { mutableStateOf(false) }

    LaunchedEffect(song.id, lyricsRepository) {
        if (lyricsRepository != null) {
            isLoadingLyrics = true
            lyrics = lyricsRepository.getLyrics(song.title, song.artist, song.durationSeconds)
            isLoadingLyrics = false
        }
    }

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var pendingSeekMs by remember { mutableStateOf<Long?>(null) }

    val durationMs = if (playerState.durationMs > 0) playerState.durationMs else (song.durationSeconds * 1000L).coerceAtLeast(1L)

    LaunchedEffect(playerState.currentPositionMs, playerState.isLoading) {
        pendingSeekMs?.let { target ->
            if (!playerState.isLoading && Math.abs(playerState.currentPositionMs - target) < 1500L) {
                pendingSeekMs = null
            }
        }
    }

    val currentPositionMs = when {
        isDraggingSlider -> (sliderPosition * durationMs).toLong()
        pendingSeekMs != null -> pendingSeekMs!!
        else -> playerState.currentPositionMs
    }

    // Lead time offset (+450ms) so lyrics highlight right on vocal attack instead of trailing half a second late
    val syncedPositionMs = currentPositionMs + 450L
    val activeLyricIndex = remember(syncedPositionMs, lyrics) {
        if (lyrics.isEmpty()) -1 else lyrics.indexOfLast { it.timeMs <= syncedPositionMs }
    }

    val animatedTransition = rememberInfiniteTransition(label = "playerPing")
    val pingScale by animatedTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pingScale"
    )

    val backgroundBrush = Brush.verticalGradient(
        listOf(
            Color(0xFFFFF5F7),
            Color(0xFFFFF0F3),
            Color(0xFFFCE4EC)
        )
    )

    BackHandler(enabled = isLyricsExpanded) {
        isLyricsExpanded = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.75f))
                        .border(1.dp, Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Minimize",
                        tint = CozyAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                val sourceTitle = playerState.sourceTitle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (!sourceTitle.isNullOrBlank()) "PLAYING FROM PLAYLIST" else "NOW PLAYING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CozyAccent,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (!sourceTitle.isNullOrBlank()) sourceTitle else "Hearty Music",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CozyText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (!sourceTitle.isNullOrBlank()) "♡" else "✨",
                            fontSize = if (!sourceTitle.isNullOrBlank()) 12.sp else 11.sp,
                            color = CozyAccent
                        )
                    }
                }

                IconButton(
                    onClick = {
                        Toast.makeText(context, "${song.title} by ${song.artist}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.75f))
                        .border(1.dp, Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More",
                        tint = CozyText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Artwork Container
            Box(
                modifier = Modifier
                    .size(270.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top-left Ambient Flower Pill
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🌸", fontSize = 13.sp)
                }

                // Bottom-right Quality Hi-Fi Badge
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .scale(pingScale)
                                .clip(CircleShape)
                                .background(CozyAccent)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Cozy Hi-Fi ✨",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CozyText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Track Metadata Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = song.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CozyText,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .basicMarquee()
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(CozyCard)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ORIGINAL",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = CozyAccent
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song.artist,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CozyMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "✨", fontSize = 11.sp, color = CozyAccent)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // + Add button (exact match to reference: white circle, subtle shadow, dark icon)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(elevation = 3.dp, shape = CircleShape, spotColor = Color(0x18000000))
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, CozyCardBorder.copy(alpha = 0.7f), CircleShape)
                            .clickable {
                                showAddToPlaylistDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add to playlist",
                            tint = CozyText,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Heart favorite button (exact match to reference: solid pink circle with white heart when active)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(
                                elevation = if (isFavorited) 4.dp else 2.dp,
                                shape = CircleShape,
                                spotColor = if (isFavorited) CozyAccent.copy(alpha = 0.5f) else Color(0x15000000)
                            )
                            .clip(CircleShape)
                            .background(if (isFavorited) CozyAccent else Color.White)
                            .then(
                                if (!isFavorited) Modifier.border(1.dp, CozyCardBorder.copy(alpha = 0.7f), CircleShape)
                                else Modifier
                            )
                            .clickable {
                                val willBeFavorited = !isFavorited
                                onToggleFavorite()
                                Toast.makeText(
                                    context,
                                    if (willBeFavorited) "Added to Liked Songs ♡" else "Removed from Liked Songs",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = "Favorite",
                            tint = if (isFavorited) Color.White else CozyMuted.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Genre & Info Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f))
                        .border(1.dp, CozyCardBorder, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(text = "Romantic Lo-Fi", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = CozyMuted)
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f))
                        .border(1.dp, CozyCardBorder, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(text = "R&B Soul", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = CozyMuted)
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CozyAccentSoft)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(text = "104 BPM", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = CozyAccent)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Playback Scrubber Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                val progressFraction = if (durationMs > 0) {
                    (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Slider(
                    value = if (isDraggingSlider) sliderPosition else progressFraction,
                    onValueChange = {
                        isDraggingSlider = true
                        sliderPosition = it
                    },
                    onValueChangeFinished = {
                        val targetMs = (sliderPosition * durationMs).toLong()
                        pendingSeekMs = targetMs
                        isDraggingSlider = false
                        onSeekTo(targetMs)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = CozyAccent,
                        activeTrackColor = CozyAccent,
                        inactiveTrackColor = CozyCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatMs(currentPositionMs),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CozyMuted
                    )
                    Text(
                        text = "-${formatMs((durationMs - currentPositionMs).coerceAtLeast(0L))}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CozyMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Playback Controls Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(40.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffleActive) CozyAccent else CozyMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        if (isShuffleActive) {
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(CozyAccent)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = CozyText,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Big Center Play/Pause Pill
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(CozyAccent, CozyAccentLight)))
                        .clickable(onClick = onTogglePlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    if (playerState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = CozyText,
                        modifier = Modifier.size(30.dp)
                    )
                }

                IconButton(
                    onClick = onToggleRepeat,
                    modifier = Modifier.size(40.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (isRepeatActive) CozyAccent else CozyMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        if (isRepeatActive) {
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(CozyAccent)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Output Device & Queue Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.75f))
                        .border(1.dp, Color.White, CircleShape)
                        .clickable {
                            Toast.makeText(context, "Audio playing on this device ♡", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Headphones,
                            contentDescription = null,
                            tint = CozyAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cozy Buds Pro ♡",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CozyText
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Listening to \"${song.title}\" by ${song.artist} on Hearty Music ♡ https://youtu.be/${song.id}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Track"))
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Share, contentDescription = "Share", tint = CozyMuted, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Cozy Queue: ${song.title} playing ♡", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Queue", tint = CozyMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Synced Lyrics Preview Drawer Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFFFDDE4), Color(0xFFFCE4EC))))
                    .border(1.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
                    .clickable { isLyricsExpanded = true }
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LYRICS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CozyText,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.75f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (lyrics.isNotEmpty()) "Synced ✨" else "Live",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CozyAccent
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.85f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Expand", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CozyAccent)
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(imageVector = Icons.Rounded.OpenInFull, contentDescription = null, tint = CozyAccent, modifier = Modifier.size(10.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isLoadingLyrics) {
                        Text(
                            text = "Listening for lyrics... ♡",
                            fontSize = 12.sp,
                            color = CozyMuted,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else if (lyrics.isEmpty()) {
                        Text(
                            text = "Sing along with your heart... ♡",
                            fontSize = 12.sp,
                            color = CozyMuted,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        // Previous line
                        val prevLine = if (activeLyricIndex > 0) lyrics[activeLyricIndex - 1].text else ""
                        if (prevLine.isNotBlank()) {
                            Text(
                                text = prevLine,
                                fontSize = 12.sp,
                                color = CozyMuted.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Current active karaoke line
                        val activeLine = if (activeLyricIndex in lyrics.indices) lyrics[activeLyricIndex].text else lyrics.first().text
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.horizontalGradient(listOf(CozyAccent, CozyAccentLight)))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = activeLine,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = 16.sp
                            )
                        }

                        // Next line
                        val nextLine = if (activeLyricIndex + 1 in lyrics.indices) lyrics[activeLyricIndex + 1].text else ""
                        if (nextLine.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = nextLine,
                                fontSize = 12.sp,
                                color = CozyMuted.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Expandable Full-Screen Karaoke Lyrics View (Spotify Style)
        AnimatedVisibility(
            visible = isLyricsExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            val listState = rememberLazyListState()

            LaunchedEffect(activeLyricIndex) {
                if (activeLyricIndex >= 0 && lyrics.isNotEmpty()) {
                    listState.animateScrollToItem((activeLyricIndex - 2).coerceAtLeast(0))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFF0F3), Color(0xFFFFE0E8), Color(0xFFFCD0DC))))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Header: Responsive & Anchored
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isLyricsExpanded = false },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.85f))
                            .border(1.dp, Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = CozyText,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = song.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CozyText,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Text(
                            text = song.artist,
                            fontSize = 11.sp,
                            color = CozyMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.85f))
                            .border(1.dp, Color.White, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Karaoke ✨",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CozyAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoadingLyrics) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CozyAccent)
                    }
                } else if (lyrics.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No synced lyrics available for this track ♡\nEnjoy the melody!",
                            textAlign = TextAlign.Center,
                            color = CozyMuted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(lyrics) { index, line ->
                            KaraokeLyricItem(
                                line = line,
                                isActive = index == activeLyricIndex,
                                isPast = index < activeLyricIndex,
                                onClick = { onSeekTo(line.timeMs) }
                            )
                        }
                    }
                }
            }
        }

        if (showAddToPlaylistDialog) {
            AddToPlaylistDialog(
                song = song,
                playlists = playlists,
                onSelectPlaylist = { playlist ->
                    onAddToPlaylist(playlist.id, song)
                    Toast.makeText(context, "Added to \"${playlist.title}\" 🌸", Toast.LENGTH_SHORT).show()
                    showAddToPlaylistDialog = false
                },
                onCreatePlaylistAndAdd = { title, emoji ->
                    onCreatePlaylistAndAddSong(title, emoji, song)
                    Toast.makeText(context, "Created \"$title\" and added song ♡", Toast.LENGTH_SHORT).show()
                    showAddToPlaylistDialog = false
                },
                onDismiss = { showAddToPlaylistDialog = false }
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
private fun KaraokeLyricItem(
    line: LyricLine,
    isActive: Boolean,
    isPast: Boolean,
    onClick: () -> Unit
) {
    val bgAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "lyricBgAlpha"
    )
    val targetScale by animateFloatAsState(
        targetValue = if (isActive) 1.03f else 1.0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "lyricScale"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            isActive -> Color.White
            isPast -> CozyMuted.copy(alpha = 0.45f)
            else -> CozyText.copy(alpha = 0.85f)
        },
        animationSpec = tween(durationMillis = 300),
        label = "lyricTextColor"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(targetScale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        CozyAccent.copy(alpha = bgAlpha),
                        CozyAccentLight.copy(alpha = bgAlpha)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = line.text,
            fontSize = if (isActive) 18.sp else 16.sp,
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = textColor,
            lineHeight = if (isActive) 25.sp else 22.sp
        )
    }
}

private val playlistEmojis = listOf("💌", "🌸", "🌧️", "✨", "☁️", "☕", "🎀", "🧸")

@Composable
private fun AddToPlaylistDialog(
    song: Song,
    playlists: List<PlaylistEntity>,
    onSelectPlaylist: (PlaylistEntity) -> Unit,
    onCreatePlaylistAndAdd: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var isCreatingNew by remember { mutableStateOf(playlists.isEmpty()) }
    var newTitle by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("💌") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (isCreatingNew) "New Playlist ♡" else "Add to Playlist",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CozyText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${song.title} • ${song.artist}",
                    fontSize = 12.sp,
                    color = CozyMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (isCreatingNew) {
                    Text(
                        text = "Choose an emoji:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = CozyMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        playlistEmojis.forEach { emoji ->
                            val isSelected = emoji == selectedEmoji
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) CozyAccentSoft else Color(0xFFFFF0F2))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) CozyAccent else CozyCardBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 18.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        placeholder = {
                            Text(
                                text = "Playlist name...",
                                color = CozyMuted,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CozyAccent,
                            unfocusedBorderColor = CozyCardBorder,
                            focusedTextColor = CozyText,
                            unfocusedTextColor = CozyText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Option to create new playlist
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CozyAccentSoft.copy(alpha = 0.5f))
                            .border(1.dp, CozyAccent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .clickable { isCreatingNew = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CozyAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Create New Playlist",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = CozyAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Existing Playlists
                    playlists.forEach { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CozyCard.copy(alpha = 0.7f))
                                .border(1.dp, CozyCardBorder, RoundedCornerShape(14.dp))
                                .clickable { onSelectPlaylist(playlist) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFDE8E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = playlist.coverEmoji.ifEmpty { "💌" },
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.title,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CozyText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${playlist.songCount} songs",
                                    fontSize = 11.5.sp,
                                    color = CozyMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add to this playlist",
                                tint = CozyAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        },
        confirmButton = {
            if (isCreatingNew) {
                Button(
                    onClick = {
                        val title = newTitle.trim()
                        if (title.isNotEmpty()) {
                            onCreatePlaylistAndAdd(title, selectedEmoji)
                        }
                    },
                    enabled = newTitle.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CozyAccent,
                        disabledContainerColor = CozyAccent.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Create & Add", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (isCreatingNew && playlists.isNotEmpty()) {
                        isCreatingNew = false
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(
                    text = if (isCreatingNew && playlists.isNotEmpty()) "Back" else "Cancel",
                    color = CozyMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(22.dp)
    )
}
