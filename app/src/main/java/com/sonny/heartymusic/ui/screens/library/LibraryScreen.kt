package com.sonny.heartymusic.ui.screens.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonny.heartymusic.data.local.entity.PlaylistEntity
import com.sonny.heartymusic.data.model.Song
import com.sonny.heartymusic.ui.screens.home.BIBBLE_AVATAR_URL
import com.sonny.heartymusic.ui.screens.home.getDynamicGreeting
import com.sonny.heartymusic.ui.theme.CozyAccent
import com.sonny.heartymusic.ui.theme.CozyAccentSoft
import com.sonny.heartymusic.ui.theme.CozyBg
import com.sonny.heartymusic.ui.theme.CozyCard
import com.sonny.heartymusic.ui.theme.CozyCardBorder
import com.sonny.heartymusic.ui.theme.CozyMuted
import com.sonny.heartymusic.ui.theme.CozyText

private val filterPills = listOf("Playlists", "Artists", "Albums", "Downloaded", "Podcasts")
private val playlistEmojis = listOf("💌", "🌸", "🌧️", "✨", "☁️", "☕", "🎀", "🧸")

@Composable
fun LibraryScreen(
    favorites: List<Song> = emptyList(),
    playlists: List<PlaylistEntity> = emptyList(),
    onCreatePlaylist: (String, String) -> Unit = { _, _ -> },
    onPlaySong: (Song) -> Unit = {},
    getPlaylistSongs: (String) -> Flow<List<Song>> = { emptyFlow() },
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("Playlists") }
    var isCreatingPlaylist by remember { mutableStateOf(false) }
    var viewingLikedSongs by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }

    val dynamicGreeting = remember { getDynamicGreeting() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CozyBg)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bibble Avatar with status badge
                Box(
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(2.dp, CozyCardBorder, CircleShape)
                    ) {
                        AsyncImage(
                            model = BIBBLE_AVATAR_URL,
                            contentDescription = "Bibble Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(CozyAccent)
                            .border(2.dp, CozyBg, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }

                Column {
                    Text(
                        text = dynamicGreeting,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CozyAccent,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "Your Library",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CozyText
                    )
                }
            }

            // Top action buttons: Search & Add Playlist
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .shadow(elevation = 2.dp, shape = CircleShape, spotColor = Color(0x12000000))
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, CozyCardBorder, CircleShape)
                        .clickable { /* Search in library */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search in library",
                        tint = CozyMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .shadow(elevation = 3.dp, shape = CircleShape, spotColor = CozyAccent.copy(alpha = 0.4f))
                        .clip(CircleShape)
                        .background(CozyAccent)
                        .clickable { isCreatingPlaylist = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add playlist",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterPills.forEach { pill ->
                val isSelected = pill == selectedFilter
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) CozyAccent else Color.White)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) CozyAccent else CozyCardBorder,
                            shape = CircleShape
                        )
                        .clickable { selectedFilter = pill }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = pill,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color.White else CozyMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sorting Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Sort,
                    contentDescription = null,
                    tint = CozyAccent,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Recents",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CozyText
                )
            }

            if (viewingLikedSongs || selectedPlaylist != null) {
                TextButton(
                    onClick = {
                        viewingLikedSongs = false
                        selectedPlaylist = null
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                            tint = CozyAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Back to Library",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CozyAccent
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content Area
        if (viewingLikedSongs) {
            LikedSongsListView(
                favorites = favorites,
                onSongClick = onPlaySong
            )
        } else if (selectedPlaylist != null) {
            val playlistSongs by getPlaylistSongs(selectedPlaylist!!.id).collectAsState(initial = emptyList())
            PlaylistDetailView(
                playlist = selectedPlaylist!!,
                songs = playlistSongs,
                onSongClick = onPlaySong
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // Pinned Item: Liked Songs Card
                item {
                    PinnedLikedSongsCard(
                        count = favorites.size,
                        onClick = { viewingLikedSongs = true }
                    )
                }

                // Custom Playlists from Room DB
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistItemRow(
                        playlist = playlist,
                        onClick = {
                            selectedPlaylist = playlist
                        }
                    )
                }

                // Curated default playlists if user hasn't made many yet
                if (playlists.isEmpty()) {
                    item {
                        DefaultPlaylistRow(
                            emoji = "🌧️",
                            title = "Rainy Day Lo-Fi",
                            subtitle = "Playlist • By Hearty • 28 songs"
                        )
                    }
                    item {
                        DefaultPlaylistRow(
                            emoji = "💌",
                            title = "Sweetheart Serenade",
                            subtitle = "Playlist • 15 songs"
                        )
                    }
                    item {
                        DefaultPlaylistRow(
                            emoji = "☁️",
                            title = "Pink Cloud Dreams",
                            subtitle = "Album • Laufey"
                        )
                    }
                }

                // Cozy Offline Sanctuary Card
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    CozySanctuaryCard()
                }
            }
        }
    }

    // Create Playlist Dialog
    if (isCreatingPlaylist) {
        CreatePlaylistDialog(
            onDismiss = { isCreatingPlaylist = false },
            onConfirm = { title, emoji ->
                onCreatePlaylist(title, emoji)
                isCreatingPlaylist = false
            }
        )
    }
}

@Composable
private fun PinnedLikedSongsCard(
    count: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(CozyCardBorder, CozyCardBorder))
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFFFE5EC),
                            Color(0xFFFFF0F4),
                            Color(0xFFFCE7F3)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Heart icon squircle (refined 44dp with 18dp heart)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp), spotColor = Color(0x25F472B6))
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFF472B6),
                                    Color(0xFFFB7185)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = "Liked Songs",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Liked Songs",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = CozyText
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFFFE4E6))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PIN",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF43F5E),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Playlist • $count ${if (count == 1) "song" else "songs"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = CozyAccent
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = CozyAccent.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PlaylistItemRow(
    playlist: PlaylistEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CozyCard.copy(alpha = 0.65f)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(CozyCardBorder.copy(alpha = 0.4f), CozyCardBorder.copy(alpha = 0.4f)))
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFDE8E9))
                    .border(1.dp, CozyCardBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = playlist.coverEmoji.ifEmpty { "💌" },
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CozyText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (playlist.songCount > 0) "Playlist • ${playlist.songCount} songs" else if (playlist.description.isNotEmpty()) playlist.description else "Playlist • By You",
                    fontSize = 11.5.sp,
                    color = CozyMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = CozyMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun DefaultPlaylistRow(
    emoji: String,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CozyCard.copy(alpha = 0.65f)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(CozyCardBorder.copy(alpha = 0.4f), CozyCardBorder.copy(alpha = 0.4f)))
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFDE8E9))
                    .border(1.dp, CozyCardBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CozyText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = CozyMuted,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = CozyMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun LikedSongsListView(
    favorites: List<Song>,
    onSongClick: (Song) -> Unit
) {
    if (favorites.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CozyCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(CozyCardBorder, CozyCardBorder))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, CozyCardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = CozyAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No Liked Songs Yet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CozyText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap the heart on any song to save it to your personal cozy favorites ♡",
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = CozyMuted
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(favorites, key = { it.id }) { song ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSongClick(song) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CozyCard.copy(alpha = 0.75f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(CozyCardBorder.copy(alpha = 0.4f), CozyCardBorder.copy(alpha = 0.4f)))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CozyText,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            Text(
                                text = song.artist,
                                fontSize = 11.sp,
                                color = CozyMuted,
                                maxLines = 1
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(CozyAccentSoft)
                                .clickable { onSongClick(song) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                tint = CozyAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailView(
    playlist: PlaylistEntity,
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    if (songs.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CozyCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(CozyCardBorder, CozyCardBorder))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFDE8E9))
                        .border(1.dp, CozyCardBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = playlist.coverEmoji.ifEmpty { "💌" }, fontSize = 26.sp)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = playlist.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CozyText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No songs in this playlist yet ♡\nAdd songs by tapping the \"+\" button in the music player!",
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = CozyMuted,
                    lineHeight = 17.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CozyCard.copy(alpha = 0.8f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(CozyCardBorder, CozyCardBorder))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFDE8E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = playlist.coverEmoji.ifEmpty { "💌" }, fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CozyText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${songs.size} cozy tracks",
                                fontSize = 12.sp,
                                color = CozyAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            items(songs, key = { it.id }) { song ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSongClick(song) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CozyCard.copy(alpha = 0.75f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(CozyCardBorder.copy(alpha = 0.4f), CozyCardBorder.copy(alpha = 0.4f)))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                fontSize = 13.sp,
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
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(CozyAccentSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                tint = CozyAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CozySanctuaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F2)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(CozyCardBorder.copy(alpha = 0.6f), CozyCardBorder.copy(alpha = 0.6f)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3E8FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFC084FC),
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Quiet Offline Corner",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CozyText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Downloaded bedtime stories and offline sleep tracks appear here for cozy journeys.",
                fontSize = 11.sp,
                color = CozyMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 15.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, emoji: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("💌") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Cozy Playlist ♡",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = CozyText
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Pick an emoji cover:",
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
                                .size(36.dp)
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
                            Text(text = emoji, fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text(text = "Playlist name...", fontSize = 13.sp, color = CozyMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = CozyAccent,
                        unfocusedBorderColor = CozyCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), selectedEmoji)
                    }
                },
                enabled = title.isNotBlank(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CozyAccent,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Create", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = CozyMuted, fontSize = 12.5.sp)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}
