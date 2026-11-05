package com.example.moodwave.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FacebookStyleEmoji(
    emoji: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
    isAnimated: Boolean = true
) {
    if (!isAnimated || emoji.isBlank()) {
        Text(text = emoji, fontSize = fontSize, modifier = modifier)
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "fbEmojiTransition")

    // Facebook-style vertical bounce
    val bounceOffsetY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounceOffsetY"
    )

    // Facebook-style rocking rotation tilt (-7 deg to +7 deg)
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotationAngle"
    )

    // Subtle scale expansion pulse
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scalePulse"
    )

    Text(
        text = emoji,
        fontSize = fontSize,
        modifier = modifier
            .offset(y = bounceOffsetY.dp)
            .graphicsLayer(
                rotationZ = rotationAngle,
                scaleX = scalePulse,
                scaleY = scalePulse
            )
    )
}
