package com.sonny.heartymusic.ui.components.bibble

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lightweight Canvas-based particle rendering for Bibble's animation states:
 * - Sparkles / stars for ENTERING & HAPPY
 * - Floating hearts for HEART
 * - Dreamy Zzz's for SLEEPY
 * - Orbiting glowing music note for DISCOVERING
 */
@Composable
fun BibbleParticleCanvas(
    state: BibbleAnimationState,
    modifier: Modifier = Modifier,
    isReducedMotion: Boolean = false
) {
    if (state == BibbleAnimationState.HIDDEN || state == BibbleAnimationState.LEAVING) return

    val infiniteTransition = rememberInfiniteTransition(label = "BibbleParticles")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isReducedMotion) 6000 else 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ParticlePhase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        when (state) {
            BibbleAnimationState.HEART -> {
                drawFloatingHearts(phase, width, height, isReducedMotion)
            }
            BibbleAnimationState.SLEEPY -> {
                drawSleepyBubbles(phase, width, height, isReducedMotion)
            }
            BibbleAnimationState.HAPPY,
            BibbleAnimationState.ENTERING -> {
                drawSparklesAndStars(phase, width, height, isReducedMotion)
            }
            BibbleAnimationState.DISCOVERING -> {
                drawOrbitingMusicNote(phase, width, height, isReducedMotion)
            }
            BibbleAnimationState.DANCING -> {
                drawDancingNotes(phase, width, height, isReducedMotion)
            }
            else -> {}
        }
    }
}

/**
 * Floating pastel pink and lavender hearts drifting upward.
 */
private fun DrawScope.drawFloatingHearts(phase: Float, width: Float, height: Float, reducedMotion: Boolean) {
    val heartColors = listOf(
        Color(0xFFFF8DA1),
        Color(0xFFFFB6C1),
        Color(0xFFE898FF),
        Color(0xFFFF69B4)
    )

    val count = if (reducedMotion) 3 else 6
    for (i in 0 until count) {
        val seed = (i * 0.17f)
        val progress = (phase + seed) % 1f
        val y = height * (0.85f - progress * 0.75f)
        val sway = if (reducedMotion) 0f else sin((progress * 2 * PI + i).toFloat()) * 20f
        val x = (width * 0.2f) + (i * (width * 0.6f / count)) + sway
        val alpha = if (progress < 0.2f) progress / 0.2f else (1f - progress).coerceAtLeast(0f)
        val size = (12f + (i % 3) * 6f)

        drawHeart(
            center = Offset(x, y),
            size = size,
            color = heartColors[i % heartColors.size].copy(alpha = alpha * 0.85f)
        )
    }
}

private fun DrawScope.drawHeart(center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y + size * 0.4f)
        cubicTo(
            center.x - size, center.y - size * 0.4f,
            center.x - size * 0.5f, center.y - size,
            center.x, center.y - size * 0.35f
        )
        cubicTo(
            center.x + size * 0.5f, center.y - size,
            center.x + size, center.y - size * 0.4f,
            center.x, center.y + size * 0.4f
        )
        close()
    }
    drawPath(path = path, color = color, style = Fill)
}

/**
 * Dreamy pastel "Z" bubbles rising softly.
 */
private fun DrawScope.drawSleepyBubbles(phase: Float, width: Float, height: Float, reducedMotion: Boolean) {
    val count = if (reducedMotion) 2 else 4
    for (i in 0 until count) {
        val seed = (i * 0.25f)
        val progress = (phase + seed) % 1f
        val y = height * (0.65f - progress * 0.55f)
        val x = width * 0.62f + (i * 14f) + (if (reducedMotion) 0f else sin(progress * PI.toFloat()) * 12f)
        val alpha = if (progress < 0.15f) progress / 0.15f else (1f - progress).coerceAtLeast(0f)
        val radius = 4f + i * 3f

        // Soft cloud circles
        drawCircle(
            color = Color(0xFFD4C2FC).copy(alpha = alpha * 0.7f),
            radius = radius,
            center = Offset(x, y)
        )
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.9f),
            radius = radius * 0.6f,
            center = Offset(x - radius * 0.3f, y - radius * 0.3f)
        )
    }
}

/**
 * Sparkles and golden/pink star particles for happy or entrance states.
 */
private fun DrawScope.drawSparklesAndStars(phase: Float, width: Float, height: Float, reducedMotion: Boolean) {
    val colors = listOf(
        Color(0xFFFFD166),
        Color(0xFFFF9EAA),
        Color(0xFFB5E2FA),
        Color(0xFFEDC2F8)
    )

    val count = if (reducedMotion) 4 else 8
    for (i in 0 until count) {
        val seed = (i * 0.13f)
        val progress = (phase + seed) % 1f
        val angle = (i * (2 * PI / count) + (if (reducedMotion) 0.0 else phase * PI * 0.5)).toFloat()
        val dist = (width * 0.35f) * (0.3f + progress * 0.7f)
        val cx = width * 0.5f + cos(angle) * dist
        val cy = height * 0.5f + sin(angle) * dist * 0.8f
        val alpha = if (progress < 0.3f) progress / 0.3f else (1f - progress).coerceAtLeast(0f)
        val starSize = 5f + (i % 3) * 3f

        drawStar(
            center = Offset(cx, cy),
            size = starSize,
            color = colors[i % colors.size].copy(alpha = alpha * 0.9f)
        )
    }
}

private fun DrawScope.drawStar(center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - size)
        lineTo(center.x + size * 0.3f, center.y - size * 0.3f)
        lineTo(center.x + size, center.y)
        lineTo(center.x + size * 0.3f, center.y + size * 0.3f)
        lineTo(center.x, center.y + size)
        lineTo(center.x - size * 0.3f, center.y + size * 0.3f)
        lineTo(center.x - size, center.y)
        lineTo(center.x - size * 0.3f, center.y - size * 0.3f)
        close()
    }
    drawPath(path = path, color = color, style = Fill)
}

/**
 * A glowing floating musical note that circles around Bibble.
 */
private fun DrawScope.drawOrbitingMusicNote(phase: Float, width: Float, height: Float, reducedMotion: Boolean) {
    val angle = (phase * 2 * PI).toFloat()
    val rx = width * 0.38f
    val ry = height * 0.28f
    val cx = width * 0.5f + cos(angle) * rx
    val cy = height * 0.42f + sin(angle) * ry

    // Glow halo
    drawCircle(
        color = Color(0xFFFF9EAA).copy(alpha = 0.35f),
        radius = 16f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = Color(0xFFFF4081).copy(alpha = 0.85f),
        radius = 7f,
        center = Offset(cx, cy)
    )
    // Stem of note
    drawLine(
        color = Color(0xFFFF4081),
        start = Offset(cx + 6f, cy),
        end = Offset(cx + 6f, cy - 20f),
        strokeWidth = 3f
    )
    // Flag of note
    drawLine(
        color = Color(0xFFFF4081),
        start = Offset(cx + 6f, cy - 20f),
        end = Offset(cx + 14f, cy - 14f),
        strokeWidth = 3f
    )
}

/**
 * Little ambient musical notes that float gently when Bibble is dancing.
 */
private fun DrawScope.drawDancingNotes(phase: Float, width: Float, height: Float, reducedMotion: Boolean) {
    val count = if (reducedMotion) 2 else 3
    for (i in 0 until count) {
        val seed = (i * 0.33f)
        val progress = (phase + seed) % 1f
        val y = height * (0.8f - progress * 0.7f)
        val side = if (i % 2 == 0) -1f else 1f
        val x = width * 0.5f + (side * (width * 0.35f + (if (reducedMotion) 0f else sin(progress * 4f) * 12f)))
        val alpha = if (progress < 0.2f) progress / 0.2f else (1f - progress).coerceAtLeast(0f)

        // Cute note head
        drawCircle(
            color = Color(0xFFAB7DF6).copy(alpha = alpha * 0.8f),
            radius = 5f,
            center = Offset(x, y)
        )
        drawLine(
            color = Color(0xFFAB7DF6).copy(alpha = alpha * 0.8f),
            start = Offset(x + 4f, y),
            end = Offset(x + 4f, y - 14f),
            strokeWidth = 2.5f
        )
    }
}
