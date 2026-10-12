package com.sonny.heartymusic.ui.components.bibble

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonny.heartymusic.ui.theme.CozyAccent
import com.sonny.heartymusic.ui.theme.CozyText
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * High-detail interactive Pixel Bibble screen companion.
 *
 * Walks freely across the screen, sways to the music beat,
 * naps when paused, eats snacks, reacts to taps, and can be
 * dragged anywhere on the player interface.
 */
@Composable
fun PixelBibblePet(
    isVisible: Boolean,
    isPlaying: Boolean,
    isFavorited: Boolean,
    songTitle: String = "",
    artist: String = "",
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    // Position coordinates
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Sprite frame state
    var currentFrame by remember { mutableStateOf(PixelBibbleFrame.IDLE) }
    var speechText by remember { mutableStateOf<String?>("Hi cutie! ✨") }
    var speechTimer by remember { mutableStateOf(4) }

    // Rhythmic bounce when dancing
    val danceTransition = rememberInfiniteTransition(label = "PixelDance")
    val danceBounce by danceTransition.animateFloat(
        initialValue = 0f,
        targetValue = -9f,
        animationSpec = infiniteRepeatable(
            animation = tween(340, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PixelDanceBounce"
    )

    val danceTilt by danceTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(680, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PixelDanceTilt"
    )

    // React to song favorite
    LaunchedEffect(isFavorited) {
        if (isFavorited) {
            currentFrame = PixelBibbleFrame.LOVE
            speechText = "Added to Liked Songs! 💖"
            speechTimer = 6
        }
    }

    // Drag release bounce
    val dropScale by animateFloatAsState(
        targetValue = if (isDragging) 1.18f else 1f,
        animationSpec = tween(150),
        label = "DropScale"
    )

    // Autonomous behavior loop
    LaunchedEffect(isPlaying, isDragging, songTitle) {
        if (isDragging) {
            currentFrame = PixelBibbleFrame.DANGLING
            speechText = "Wheee! Put me anywhere! 🌸"
            return@LaunchedEffect
        }

        while (true) {
            if (speechTimer > 0) {
                speechTimer--
                if (speechTimer == 0) speechText = null
            }

            if (!isPlaying) {
                // Paused: Sleep
                currentFrame = PixelBibbleFrame.SLEEP
                if (Random.nextInt(10) < 3 && speechText == null) {
                    speechText = "Sleeping cozy... zzz ☁️"
                    speechTimer = 5
                }
                delay(3000)
            } else {
                // Playing: Pick random fun activities
                val roll = Random.nextInt(100)
                when {
                    roll < 45 -> {
                        // Dance groove
                        for (i in 0 until 6) {
                            currentFrame = if (i % 2 == 0) PixelBibbleFrame.DANCE_1 else PixelBibbleFrame.DANCE_2
                            delay(340)
                        }
                        if (Random.nextInt(10) < 3 && speechText == null) {
                            val phrases = mutableListOf("Groovy! ✨", "Such a vibe! ♡", "Dancing with you! 🎵")
                            if (artist.isNotBlank()) phrases.add("Loving $artist! 🌸")
                            speechText = phrases.random()
                            speechTimer = 4
                        }
                    }
                    roll < 65 -> {
                        // Waddle/Walk
                        val stepDir = if (Random.nextBoolean()) 14f else -14f
                        for (i in 0 until 4) {
                            currentFrame = if (i % 2 == 0) PixelBibbleFrame.WALK_1 else PixelBibbleFrame.WALK_2
                            offsetX = (offsetX + stepDir).coerceIn(-280f, 280f)
                            delay(270)
                        }
                        currentFrame = PixelBibbleFrame.IDLE
                    }
                    roll < 80 -> {
                        // Snack
                        currentFrame = PixelBibbleFrame.SNACK
                        speechText = "Nom nom strawberry! 🍓"
                        speechTimer = 4
                        delay(2400)
                        currentFrame = PixelBibbleFrame.IDLE
                    }
                    roll < 92 -> {
                        // Love
                        currentFrame = PixelBibbleFrame.LOVE
                        speechText = "You're my favorite! 💕"
                        speechTimer = 4
                        delay(2200)
                        currentFrame = PixelBibbleFrame.IDLE
                    }
                    else -> {
                        // Blink
                        currentFrame = PixelBibbleFrame.BLINK
                        delay(350)
                        currentFrame = PixelBibbleFrame.IDLE
                        delay(2000)
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 120.dp, end = 24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .scale(dropScale)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            currentFrame = PixelBibbleFrame.IDLE
                            speechText = "*boop!* ♡"
                            speechTimer = 3
                        },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    )
                }
        ) {
            // Speech Bubble
            AnimatedVisibility(
                visible = speechText != null,
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut()
            ) {
                speechText?.let { text ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .shadow(6.dp, RoundedCornerShape(14.dp), spotColor = Color(0x33B388FF))
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.5.dp, Color(0xFFFFD5E5), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = text,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CozyText,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Pure Pixel Bibble Sprite with gentle ground shadow
            Box(
                modifier = Modifier
                    .offset(
                        y = if (isPlaying && (currentFrame == PixelBibbleFrame.DANCE_1 || currentFrame == PixelBibbleFrame.DANCE_2)) danceBounce.dp else 0.dp
                    )
                    .rotate(
                        if (isPlaying && (currentFrame == PixelBibbleFrame.DANCE_1 || currentFrame == PixelBibbleFrame.DANCE_2)) danceTilt else 0f
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        currentFrame = if (Random.nextBoolean()) PixelBibbleFrame.SURPRISE else PixelBibbleFrame.LOVE
                        speechText = listOf("Hehehe! *giggles* ✨", "Bop! 💖", "Bibble loves you! 🌸", "Cutie! ♡").random()
                        speechTimer = 4
                    },
                contentAlignment = Alignment.Center
            ) {
                // Soft ground shadow beneath Bibble
                Canvas(
                    modifier = Modifier
                        .size(64.dp, 10.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = 2.dp)
                ) {
                    drawOval(
                        color = Color(0x22000000),
                        topLeft = Offset.Zero,
                        size = size
                    )
                }

                // Detailed 57x76 Pixel Bibble Sprite
                PixelBibbleSprite(
                    frame = currentFrame,
                    pixelSize = 1.3.dp
                )

                // Tiny close badge on Bibble's corner
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-4).dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFFFC1E3), CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close Bibble",
                        tint = CozyAccent,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }
}
