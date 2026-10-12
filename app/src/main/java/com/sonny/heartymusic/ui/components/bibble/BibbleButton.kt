package com.sonny.heartymusic.ui.components.bibble

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sonny.heartymusic.ui.theme.CozyAccent
import com.sonny.heartymusic.ui.theme.CozyCardBorder

/**
 * A small, charming pixel Bibble trigger button that sits alongside player controls.
 *
 * Sized at 36.dp to match the Add (+) and Favorite (♡) buttons in PlayerScreen.
 */
@Composable
fun BibbleButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else if (isActive) 1.06f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "BibbleButtonScale"
    )

    Box(
        modifier = modifier
            .size(36.dp)
            .scale(scale)
            .shadow(
                elevation = if (isActive) 5.dp else 3.dp,
                shape = CircleShape,
                spotColor = if (isActive) Color(0x44B388FF) else Color(0x18000000)
            )
            .clip(CircleShape)
            .background(
                if (isActive) {
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFFFE4EC),
                            Color(0xFFF3E5F5),
                            Color(0xFFE1F5FE)
                        )
                    )
                } else {
                    Brush.linearGradient(listOf(Color.White, Color.White))
                }
            )
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                brush = if (isActive) {
                    Brush.linearGradient(listOf(CozyAccent, Color(0xFFB388FF)))
                } else {
                    Brush.linearGradient(listOf(CozyCardBorder.copy(alpha = 0.7f), CozyCardBorder.copy(alpha = 0.7f)))
                },
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics {
                contentDescription = "Say hi to Bibble pixel music companion"
            },
        contentAlignment = Alignment.Center
    ) {
        PixelBibbleSprite(
            frame = if (isActive) PixelBibbleFrame.DANCE_1 else PixelBibbleFrame.IDLE,
            pixelSize = 0.32.dp
        )
    }
}
