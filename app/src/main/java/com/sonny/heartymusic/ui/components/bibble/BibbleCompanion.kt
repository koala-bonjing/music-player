package com.sonny.heartymusic.ui.components.bibble

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonny.heartymusic.ui.theme.CozyAccent
import com.sonny.heartymusic.ui.theme.CozyMuted
import com.sonny.heartymusic.ui.theme.CozyText

/**
 * The Interactive Bibble Companion Overlay.
 *
 * Displays Bibble, reactions, speech bubble, and quick interactive action chips.
 * Positioned cleanly above player controls without blocking audio playback.
 */
@Composable
fun BibbleCompanion(
    controller: BibbleController,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isReducedMotion = remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }

    AnimatedVisibility(
        visible = controller.isVisible,
        enter = slideInVertically(initialOffsetY = { it / 3 }) + fadeIn() + scaleIn(initialScale = 0.88f),
        exit = slideOutVertically(targetOffsetY = { it / 3 }) + fadeOut() + scaleOut(targetScale = 0.88f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = Color(0x33B388FF)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFFFAF8).copy(alpha = 0.96f),
                            Color(0xFFFBF4FF).copy(alpha = 0.96f),
                            Color(0xFFF3F8FF).copy(alpha = 0.96f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color(0xFFFFD5E5).copy(alpha = 0.8f),
                            Color(0xFFD4C2FC).copy(alpha = 0.8f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFFFE0EB))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Fairytopia ✨",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CozyAccent
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bibble Companion",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CozyText
                        )
                    }

                    IconButton(
                        onClick = { controller.dismiss() },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss Bibble",
                            tint = CozyMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Character Animation Area with physics, particles, and speech bubble
                BibbleAnimationView(
                    state = controller.state,
                    speech = controller.speech,
                    onBibbleTap = { controller.onBibbleTapped(isPlaying) },
                    isReducedMotion = isReducedMotion
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Action Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BibbleChip(
                        label = "Dance 🎵",
                        isSelected = controller.state == BibbleAnimationState.DANCING,
                        onClick = { controller.dispatchAction(BibbleUserAction.DANCE, isPlaying) }
                    )
                    BibbleChip(
                        label = "Love 💕",
                        isSelected = controller.state == BibbleAnimationState.HEART,
                        onClick = { controller.dispatchAction(BibbleUserAction.LOVE, isPlaying) }
                    )
                    BibbleChip(
                        label = "Catch Note ✨",
                        isSelected = controller.state == BibbleAnimationState.DISCOVERING,
                        onClick = { controller.dispatchAction(BibbleUserAction.CATCH_NOTE, isPlaying) }
                    )
                    BibbleChip(
                        label = "Nap ☁️",
                        isSelected = controller.state == BibbleAnimationState.SLEEPY,
                        onClick = { controller.dispatchAction(BibbleUserAction.NAP, isPlaying) }
                    )
                }
            }
        }
    }
}

/**
 * Interactive pill button for quick companion actions.
 */
@Composable
private fun BibbleChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (isSelected) {
                    Brush.linearGradient(listOf(CozyAccent, Color(0xFFC77DFF)))
                } else {
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.9f)))
                }
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else Color(0xFFFFD1DC),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else CozyText
        )
    }
}
