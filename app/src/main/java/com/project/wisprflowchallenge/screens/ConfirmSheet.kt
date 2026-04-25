package com.project.wisprflowchallenge.screens

// ui/ConfirmSheet.kt

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.wisprflowchallenge.model.CalendarEvent
import java.text.SimpleDateFormat
import java.util.*

/**
 * Shows a summary of the parsed event so the user can review before saving.
 * This is the most important UX step — always confirm before writing to calendar!
 *
 * The user can see exactly what the LLM understood and cancel if it got
 * something wrong.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmSheet(
    event: CalendarEvent,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEEE, MMMM d 'at' h:mm a", Locale.US)
    val startStr  = dateFormat.format(Date(event.startMillis))

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Add to calendar?",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(16.dp))

            // Show what the LLM parsed
            EventDetailRow(label = "Event",    value = event.title)
            EventDetailRow(label = "When",     value = startStr)
            if (event.location.isNotBlank())
                EventDetailRow(label = "Where", value = event.location)
            if (event.description.isNotBlank())
                EventDetailRow(label = "Notes", value = event.description)

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EventDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.3f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.7f)
        )
    }
}