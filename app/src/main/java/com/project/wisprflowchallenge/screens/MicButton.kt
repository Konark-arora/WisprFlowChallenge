package com.project.wisprflowchallenge.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun MicButton(
    isListening: Boolean,
    enabled: Boolean = true,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {

            // Pulse rings while listening
            if (isListening) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        )
                )
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .scale(pulseScale * 0.85f)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                        )
                )
            }

            // Box instead of FilledIconButton — prevents touch event theft
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        color = when {
                            !enabled    -> MaterialTheme.colorScheme.surfaceVariant
                            isListening -> MaterialTheme.colorScheme.error
                            else        -> MaterialTheme.colorScheme.primary
                        }
                    )
                    // Tap to start, tap again to stop
                    .pointerInput(enabled) {
                        if (enabled) {
                            detectTapGestures(
                                onTap = {
                                    if (isListening) onRelease() else onPress()
                                }
                            )
                        }
                    }
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isListening) "Tap to stop" else "Tap to speak",
                    modifier = Modifier.size(32.dp),
                    tint = when {
                        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                        else     -> MaterialTheme.colorScheme.onPrimary
                    }
                )
            }
        }

        // Label under button
        Text(
            text = when {
                !enabled    -> ""
                isListening -> "Tap to stop"
                else        -> "Tap to speak"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (isListening)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}