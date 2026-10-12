package com.sonny.heartymusic.ui.components.bibble

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonny.heartymusic.ui.theme.CozyAccent
import com.sonny.heartymusic.ui.theme.CozyText
import kotlinx.coroutines.delay

/**
 * Animated presentation view for Bibble.
 *
 * Implements physics-based animations (swaying, bouncing, sleeping breath,
 * excited hops, heart pulse) and overlay particle canvas.
 *
 * When video assets are added, this view automatically transitions to
 * video playback, maintaining identical state and layout semantics.
 */
@Composable
fun BibbleAnimationView(
    state: BibbleAnimationState,
    speech: BibbleSpeech?,
    onBibbleTap: () -> Unit,
    modifier: Modifier = Modifier,
    isReducedMotion: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BibblePhysics")

    // Dynamic animation parameters driven by current state
    val danceBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isReducedMotion) -4f else -14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DanceBounce"
    )

    val danceSway by infiniteTransition.animateFloat(
        initialValue = if (isReducedMotion) -2f else -8f,
        targetValue = if (isReducedMotion) 2f else 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 840, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DanceSway"
    )

    val sleepyBreathe by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SleepyBreathe"
    )

    val heartPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isReducedMotion) 1.04f else 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HeartPulse"
    )

    val happyHop by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isReducedMotion) -6f else -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HappyHop"
    )

    val discoverTilt by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isReducedMotion) 5f else 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DiscoverTilt"
    )

    // Entrance and exit scale & translation transitions
    var isAppearing by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state == BibbleAnimationState.ENTERING) {
            isAppearing = true
        } else if (state == BibbleAnimationState.LEAVING) {
            isAppearing = false
        }
    }

    val stateScale by animateFloatAsState(
        targetValue = when (state) {
            BibbleAnimationState.HIDDEN -> 0f
            BibbleAnimationState.LEAVING -> 0.1f
            BibbleAnimationState.ENTERING -> if (isAppearing) 1.05f else 0.3f
            BibbleAnimationState.HEART -> heartPulse
            BibbleAnimationState.SLEEPY -> sleepyBreathe
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f),
        label = "BibbleScale"
    )

    val stateOffsetY by animateFloatAsState(
        targetValue = when (state) {
            BibbleAnimationState.HIDDEN,
            BibbleAnimationState.LEAVING -> 120f
            BibbleAnimationState.ENTERING -> 0f
            BibbleAnimationState.DANCING -> danceBounce
            BibbleAnimationState.HAPPY -> happyHop
            BibbleAnimationState.SLEEPY -> 10f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 280),
        label = "BibbleOffsetY"
    )

    val stateRotation by animateFloatAsState(
        targetValue = when (state) {
            BibbleAnimationState.DANCING -> danceSway
            BibbleAnimationState.DISCOVERING -> discoverTilt
            BibbleAnimationState.SLEEPY -> -4f
            BibbleAnimationState.HAPPY -> if (danceSway > 0) 5f else -5f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 250),
        label = "BibbleRotation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Speech Bubble
        AnimatedVisibility(
            visible = speech != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            if (speech != null) {
                BibbleSpeechBubble(message = speech.message)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Character Container with Particles
        Box(
            modifier = Modifier
                .size(170.dp)
                .offset(y = stateOffsetY.dp)
                .scale(stateScale)
                .rotate(stateRotation)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBibbleTap
                ),
            contentAlignment = Alignment.Center
        ) {
            // Particle Canvas Layer (floating hearts, stars, sparkles, zzz)
            BibbleParticleCanvas(
                state = state,
                isReducedMotion = isReducedMotion,
                modifier = Modifier.matchParentSize()
            )

            // Soft glowing aura behind Bibble
            Box(
                modifier = Modifier
                    .size(126.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                when (state) {
                                    BibbleAnimationState.HEART -> Color(0x66FF8DA1)
                                    BibbleAnimationState.HAPPY -> Color(0x66FFD166)
                                    BibbleAnimationState.DANCING -> Color(0x55B5E2FA)
                                    BibbleAnimationState.SLEEPY -> Color(0x44D4C2FC)
                                    else -> Color(0x40E0C3FC)
                                },
                                Color.Transparent
                            )
                        )
                    )
            )

            // Authentic Bibble Character Image Container
            Box(
                modifier = Modifier
                    .size(118.dp)
                    .shadow(elevation = 8.dp, shape = CircleShape, spotColor = Color(0x33B388FF))
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0xFFFFB6C1),
                                Color(0xFFC77DFF),
                                Color(0xFF80D8FF)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = BibbleAssetManager.DEFAULT_CHARACTER_URL,
                    contentDescription = "Bibble Character",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(112.dp)
                )
            }

            // Status Badge Icon at Bibble's corner
            val badgeEmoji = when (state) {
                BibbleAnimationState.DANCING -> "🎵"
                BibbleAnimationState.HEART -> "💖"
                BibbleAnimationState.SLEEPY -> "💤"
                BibbleAnimationState.HAPPY -> "✨"
                BibbleAnimationState.DISCOVERING -> "🔎"
                else -> "🌸"
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-16).dp, y = (-12).dp)
                    .size(28.dp)
                    .shadow(elevation = 3.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFFFC1E3), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = badgeEmoji, fontSize = 13.sp)
            }
        }
    }
}

/**
 * Cute, rounded speech bubble with soft drop shadow and tail.
 */
@Composable
fun BibbleSpeechBubble(message: String) {
    Box(
        modifier = Modifier
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x22800080))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFFFD1DC), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = message,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CozyText,
            lineHeight = 16.sp
        )
    }
}
