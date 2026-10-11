package com.sonny.heartymusic.ui.screens.search

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonny.heartymusic.data.model.Song
import com.sonny.heartymusic.ui.theme.CozyAccent
import com.sonny.heartymusic.ui.theme.CozyAccentSoft
import com.sonny.heartymusic.ui.theme.CozyBg
import com.sonny.heartymusic.ui.theme.CozyCard
import com.sonny.heartymusic.ui.theme.CozyCardBorder
import com.sonny.heartymusic.ui.theme.CozyCardHover
import com.sonny.heartymusic.ui.theme.CozyMuted
import com.sonny.heartymusic.ui.theme.CozyText

private val searchCategoryChips = listOf("Top Results", "Artists", "Songs", "Playlists")

private data class CozyVibeCard(
    val title: String,
    val subtitle: String,
    val query: String,
    val gradientColors: List<Color>,
    val textColor: Color
)

private val cozyVibesList = listOf(
    CozyVibeCard(
        title = "Lo-Fi Beats & Rain",
        subtitle = "Soft Study",
        query = "lofi beats rain study",
        gradientColors = listOf(Color(0xFFFBCFE8), Color(0xFFFDA4AF)),
        textColor = Color(0xFF881337)
    ),
    CozyVibeCard(
        title = "Pastel Pop",
        subtitle = "Upbeat Smiles",
        query = "pastel pop upbeat songs",
        gradientColors = listOf(Color(0xFFFEF3C7), Color(0xFFFED7AA)),
        textColor = Color(0xFF78350F)
    ),
    CozyVibeCard(
        title = "Acoustic & Tea",
        subtitle = "Gentle Guitars",
        query = "acoustic tea gentle guitar cozy",
        gradientColors = listOf(Color(0xFFF3E8FF), Color(0xFFFBCFE8)),
        textColor = Color(0xFF581C87)
    ),
    CozyVibeCard(
        title = "Sleep & Clouds",
        subtitle = "Deep Rest",
        query = "deep sleep ambient clouds peaceful",
        gradientColors = listOf(Color(0xFFDBEAFE), Color(0xFFC7D2FE)),
        textColor = Color(0xFF1E3A8A)
    )
)

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier
) {
    val query by viewModel.query.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val focusManager = LocalFocusManager.current
    var selectedCategory by remember { mutableStateOf("Top Results") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CozyBg)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        SearchInputBar(
            query = query,
            onQueryChange = viewModel::onQueryChange,
            onSearch = {
                focusManager.clearFocus()
                viewModel.search()
            },
            onClear = {
                viewModel.onQueryChange("")
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Category Chips matching Stitch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            searchCategoryChips.forEach { cat ->
                val isSelected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) CozyAccent else Color.White)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) CozyAccent else CozyCardBorder,
                            shape = CircleShape
                        )
                        .clickable {
                            selectedCategory = cat
                            if (query.isNotBlank() && !isSelected) {
                                viewModel.search()
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color.White else CozyMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (val state = uiState) {
            is SearchUiState.Idle -> {
                SearchIdleView(
                    onVibeClick = { vibeQuery ->
                        viewModel.onQueryChange(vibeQuery)
                        viewModel.search()
                    }
                )
            }
            is SearchUiState.Loading -> {
                SearchLoadingView()
            }
            is SearchUiState.Error -> {
                SearchErrorView(
                    errorMessage = state.message,
                    onRetry = viewModel::retry
                )
            }
            is SearchUiState.Success -> {
                SearchResultList(
                    songs = state.songs,
                    currentlyPlayingId = playerState.currentSong?.id,
                    isPlaying = playerState.isPlaying,
                    onSongClick = viewModel::playSong
                )
            }
        }
    }
}

@Composable
private fun SearchInputBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "Search songs, artists, vibes...",
                style = MaterialTheme.typography.bodyMedium,
                color = CozyMuted,
                fontSize = 13.5.sp
            )
        },
        leadingIcon = {
            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = CozyAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(CozyCard)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = "Clear search",
                        tint = CozyAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.9f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.75f),
            focusedBorderColor = CozyAccent,
            unfocusedBorderColor = CozyCardBorder,
            focusedTextColor = CozyText,
            unfocusedTextColor = CozyText
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() })
    )
}

@Composable
private fun SearchResultList(
    songs: List<Song>,
    currentlyPlayingId: String?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SONGS & COVERS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CozyMuted,
                letterSpacing = 0.8.sp
            )
            Text(
                text = "${songs.size} results",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = CozyAccent
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            itemsIndexed(
                items = songs,
                key = { index, song -> "${song.id}_$index" }
            ) { _, song ->
                SongListItem(
                    song = song,
                    isCurrentSong = song.id == currentlyPlayingId,
                    isPlaying = isPlaying && song.id == currentlyPlayingId,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun SongListItem(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentSong) CozyCardHover else CozyCard.copy(alpha = 0.75f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(
                    if (isCurrentSong) CozyAccent else CozyCardBorder.copy(alpha = 0.5f),
                    if (isCurrentSong) CozyAccent else CozyCardBorder.copy(alpha = 0.5f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CozyAccent.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = CozyAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = CozyText,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = CozyMuted,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = song.formattedDuration,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = CozyText.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun SearchIdleView(
    onVibeClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Explore Cozy Vibes ♡",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CozyText
            )
            Text(
                text = "Tap to search",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = CozyAccent
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        val pairs = cozyVibesList.chunked(2)
        pairs.forEach { rowPairs ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowPairs.forEach { vibe ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onVibeClick(vibe.query) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(vibe.gradientColors))
                                .padding(12.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = vibe.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = vibe.textColor
                                )
                                Text(
                                    text = vibe.subtitle,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = vibe.textColor.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SearchLoadingView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = CozyAccent,
            strokeWidth = 3.dp,
            modifier = Modifier.size(42.dp)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Searching cozy tunes...",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = CozyMuted
        )
    }
}

@Composable
private fun SearchErrorView(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CozyCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(CozyCardBorder, CozyCardBorder))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = CozyAccent,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = CozyText
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onRetry,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CozyAccent,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Retry", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
