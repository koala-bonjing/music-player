package com.sonny.heartymusic.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.sonny.heartymusic.ui.theme.HeroGradientEnd
import com.sonny.heartymusic.ui.theme.HeroGradientMiddle
import com.sonny.heartymusic.ui.theme.HeroGradientStart
import com.sonny.heartymusic.ui.theme.MoodCafeBorder
import com.sonny.heartymusic.ui.theme.MoodCafeFrom
import com.sonny.heartymusic.ui.theme.MoodCafeIcon
import com.sonny.heartymusic.ui.theme.MoodCafeTo
import com.sonny.heartymusic.ui.theme.MoodNightBorder
import com.sonny.heartymusic.ui.theme.MoodNightFrom
import com.sonny.heartymusic.ui.theme.MoodNightIcon
import com.sonny.heartymusic.ui.theme.MoodNightTo
import com.sonny.heartymusic.ui.theme.MoodRainFrom
import com.sonny.heartymusic.ui.theme.MoodRainIcon
import com.sonny.heartymusic.ui.theme.MoodRainTo
import com.sonny.heartymusic.ui.theme.MoodStudyBorder
import com.sonny.heartymusic.ui.theme.MoodStudyFrom
import com.sonny.heartymusic.ui.theme.MoodStudyIcon
import com.sonny.heartymusic.ui.theme.MoodStudyTo

data class QuickPick(
    val title: String,
    val artist: String,
    val coverUrl: String,
    val query: String
)

data class MadeForYouMix(
    val title: String,
    val artists: String,
    val tag: String,
    val coverUrl: String,
    val query: String
)

data class CozyMood(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val borderColor: Color,
    val query: String
)

private val quickPicks = listOf(
    QuickPick("Warm Hugs & Cocoa", "Acoustic Cozy", "https://images.unsplash.com/photo-1544717305-2782549b5136?w=300&q=80", "Warm Hugs and Cocoa acoustic"),
    QuickPick("Risk It All", "Bruno Mars", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300&q=80", "Bruno Mars Risk It All"),
    QuickPick("Pink Cloud Dreams", "Pastel Beats", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=300&q=80", "Pink Cloud Dreams lofi"),
    QuickPick("Late Night Lo-fi", "Chillhop Music", "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=300&q=80", "Late Night Lofi chill beats"),
    QuickPick("Bubblegum Acoustic", "Sweet Melodies", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300&q=80", "Bubblegum Acoustic sweet pop"),
    QuickPick("Morning Tea & Rain", "Peaceful Guitar", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=300&q=80", "Morning Tea and Rain peaceful piano")
)

private val madeForYouMixes = listOf(
    MadeForYouMix("Daily Cozy Mix 1", "Clairo, Laufey, Bruno Mars, Boy Pablo", "MIX 01", "https://images.unsplash.com/photo-1526478806334-5fd488fcaabc?w=400&q=80", "Clairo Laufey Boy Pablo playlist"),
    MadeForYouMix("Your Heartbeat Hits", "Sweet love ballads & acoustic guitar", "PERSONAL", "https://images.unsplash.com/photo-1518895949257-7621c3c786d7?w=400&q=80", "Sweet acoustic love ballads"),
    MadeForYouMix("Soft Girl Essentials", "Dreamy indie & gentle vocals", "POPULAR", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=400&q=80", "Soft girl aesthetic indie music"),
    MadeForYouMix("Pastel Bedroom Pop", "Soft synth & cozy slippers", "INDIE", "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=400&q=80", "Pastel bedroom pop synth")
)

private val cozyMoods = listOf(
    CozyMood("Rainy Afternoon", "Lo-fi & Gentle Rain", Icons.Rounded.WaterDrop, MoodRainIcon, MoodRainFrom, MoodRainTo, CozyCardBorder, "Rainy Afternoon Lofi with Rain"),
    CozyMood("Warm Cafe Bakery", "Soft Bossa & Jazz", Icons.Rounded.LocalCafe, MoodCafeIcon, MoodCafeFrom, MoodCafeTo, MoodCafeBorder, "Warm Cafe Bakery Bossa Nova Jazz"),
    CozyMood("Starlit Lullaby", "Ambient Sleep Melodies", Icons.Rounded.Bedtime, MoodNightIcon, MoodNightFrom, MoodNightTo, MoodNightBorder, "Starlit Lullaby Ambient Sleep Melodies"),
    CozyMood("Cute Study Desk", "Focus Chill Beats", Icons.Rounded.MenuBook, MoodStudyIcon, MoodStudyFrom, MoodStudyTo, MoodStudyBorder, "Cute Study Desk Focus Beats")
)

private val categories = listOf("All", "Music", "Podcasts", "Lo-Fi Vibes", "Soft Pop")

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToSearchWithQuery: (String) -> Unit = {},
    currentPlayingSong: Song? = null,
    isPlaying: Boolean = false
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CozyBg)
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        TopBar(
            selectedCategory = selectedCategory,
            onCategorySelected = { cat ->
                selectedCategory = cat
                if (cat != "All") {
                    onNavigateToSearchWithQuery(cat)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            HeroMomentBanner(
                onPlaySerenade = {
                    onNavigateToSearchWithQuery("Daily Serenade cozy music")
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            JumpBackInGrid(
                items = quickPicks,
                currentPlayingSong = currentPlayingSong,
                isPlaying = isPlaying,
                onItemClick = { item ->
                    onNavigateToSearchWithQuery(item.query)
                }
            )

            Spacer(modifier = Modifier.height(28.dp))
        }

        MadeForYouSection(
            mixes = madeForYouMixes,
            onMixClick = { mix ->
                onNavigateToSearchWithQuery(mix.query)
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            CozyMoodsSection(
                moods = cozyMoods,
                onMoodClick = { mood ->
                    onNavigateToSearchWithQuery(mood.query)
                }
            )
        }
    }
}

@Composable
private fun TopBar(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CozyBg)
            .padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "GOOD EVENING, CUTIE",
                        color = CozyAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "♡", color = CozyAccent, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Anjel Heart",
                    color = CozyText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { /* notification view */ },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CozyCard)
                        .border(1.dp, CozyCardBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = "Notifications",
                        tint = CozyMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(2.dp, CozyAccent, CircleShape)
                ) {
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&q=80",
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val isSelected = category == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) CozyAccent else CozyCard)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) CozyAccent else CozyCardBorder,
                            shape = CircleShape
                        )
                        .clickable { onCategorySelected(category) }
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = category,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) Color.White else CozyMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMomentBanner(
    onPlaySerenade: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CozyCardBorder, CozyCardBorder)))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(HeroGradientStart, HeroGradientMiddle, HeroGradientEnd)
                    )
                )
                .padding(vertical = 24.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, CozyCardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = CozyAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Hearty Music",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CozyText
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Your soft, quiet personal music corner for peaceful mornings & cozy nights",
                    fontSize = 12.sp,
                    color = CozyMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, CozyCardBorder, CircleShape)
                        .clickable(onClick = onPlaySerenade)
                        .padding(horizontal = 20.dp, vertical = 9.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = CozyAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Play Daily Serenade",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CozyAccent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JumpBackInGrid(
    items: List<QuickPick>,
    currentPlayingSong: Song?,
    isPlaying: Boolean,
    onItemClick: (QuickPick) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Jump Back In",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CozyText
            )
            Text(
                text = "History",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = CozyMuted
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val chunked = items.chunked(2)
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { item ->
                    val isItemActive = isPlaying && currentPlayingSong?.title?.contains(item.title, ignoreCase = true) == true

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onItemClick(item) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CozyCard),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CozyCardBorder, CozyCardBorder)))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.coverUrl,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = item.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CozyText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.artist,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CozyMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isItemActive) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(CozyAccent)
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
private fun MadeForYouSection(
    mixes: List<MadeForYouMix>,
    onMixClick: (MadeForYouMix) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Made Just For You ♡",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CozyText
                )
                Text(
                    text = "Soft mixes aligned with your peaceful tastes",
                    fontSize = 11.sp,
                    color = CozyMuted
                )
            }
            Text(
                text = "See all",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CozyAccent
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(mixes) { mix ->
                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onMixClick(mix) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(CozyCardHover)
                    ) {
                        AsyncImage(
                            model = mix.coverUrl,
                            contentDescription = mix.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .align(Alignment.TopStart)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.9f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = mix.tag,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CozyAccent
                            )
                        }

                        Box(
                            modifier = Modifier
                                .padding(10.dp)
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CozyAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = mix.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CozyText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mix.artists,
                        fontSize = 10.sp,
                        color = CozyMuted,
                        maxLines = 2,
                        lineHeight = 14.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CozyMoodsSection(
    moods: List<CozyMood>,
    onMoodClick: (CozyMood) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Cozy Moods",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = CozyText
        )

        Spacer(modifier = Modifier.height(12.dp))

        val chunked = moods.chunked(2)
        chunked.forEach { rowMoods ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowMoods.forEach { mood ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onMoodClick(mood) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(mood.borderColor, mood.borderColor)))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.horizontalGradient(listOf(mood.gradientStart, mood.gradientEnd)))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mood.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CozyText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = mood.subtitle,
                                    fontSize = 10.sp,
                                    color = CozyMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = mood.icon,
                                    contentDescription = mood.title,
                                    tint = mood.iconColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
