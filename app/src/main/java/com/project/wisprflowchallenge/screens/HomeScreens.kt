package com.project.wisprflowchallenge.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.wisprflowchallenge.viewmodels.CalendarViewModel

@Composable
fun HomeScreen(viewModel: CalendarViewModel) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {

            // App title / icon shown only when idle
            AnimatedVisibility(
                visible = state is CalendarViewModel.UiState.Idle,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Wispr Flow",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Say something like:\n\"Schedule lunch with Alex tomorrow at noon\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Status text for non-idle states
            AnimatedVisibility(visible = state !is CalendarViewModel.UiState.Idle) {
                Text(
                    text = when (state) {
                        is CalendarViewModel.UiState.Initializing -> "Initializing AI…"
                        is CalendarViewModel.UiState.Downloading  ->
                            "Downloading AI Model (${(state as CalendarViewModel.UiState.Downloading).progress}%)"
                        is CalendarViewModel.UiState.Listening    -> "Listening…"
                        is CalendarViewModel.UiState.Processing   -> "Thinking…"
                        is CalendarViewModel.UiState.Confirming   -> "Review your event"
                        is CalendarViewModel.UiState.Done         -> "✓ Event saved!"
                        is CalendarViewModel.UiState.Error        ->
                            (state as CalendarViewModel.UiState.Error).message
                        is CalendarViewModel.UiState.Unavailable  ->
                            (state as CalendarViewModel.UiState.Unavailable).reason
                        else -> ""
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = when (state) {
                        is CalendarViewModel.UiState.Done  -> MaterialTheme.colorScheme.primary
                        is CalendarViewModel.UiState.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            // Download progress bar
            if (state is CalendarViewModel.UiState.Downloading) {
                LinearProgressIndicator(
                    progress = {
                        (state as CalendarViewModel.UiState.Downloading).progress / 100f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            // Live transcript bubble
            val transcript = when (state) {
                is CalendarViewModel.UiState.Listening ->
                    (state as CalendarViewModel.UiState.Listening).partialText
                is CalendarViewModel.UiState.Processing ->
                    (state as CalendarViewModel.UiState.Processing).transcript
                else -> ""
            }

            AnimatedVisibility(visible = transcript.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = "\"$transcript\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Spinner while processing (and no transcript to show)
            if (state is CalendarViewModel.UiState.Processing && transcript.isEmpty()) {
                CircularProgressIndicator()
            }

            // Mic button
            MicButton(
                isListening = state is CalendarViewModel.UiState.Listening,
                enabled = state is CalendarViewModel.UiState.Idle
                        || state is CalendarViewModel.UiState.Listening,
                onPress   = { viewModel.startListening() },
                onRelease = { viewModel.stopListening() }
            )
        }

        // Confirmation sheet
        if (state is CalendarViewModel.UiState.Confirming) {
            val event = (state as CalendarViewModel.UiState.Confirming).event
            ConfirmSheet(
                event     = event,
                onConfirm = { viewModel.confirmEvent(event) },
                onDismiss = { viewModel.dismiss() }
            )
        }
    }
}