package com.music.bitchord.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.soundchord.features.recognition.data.RecognitionHistoryEntry
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RecognitionHistoryScreen(
    entries: List<RecognitionHistoryEntry>,
    onPlay: (RecognitionHistoryEntry) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    showClearConfirmation: Boolean,
    onDismissClearConfirmation: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val locale = Locale.getDefault()
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    val timestampFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", locale)
    val zone = ZoneId.systemDefault()
    val matching = entries.asSequence()
        .filter { query.isBlank() || it.title.contains(query, true) || it.artist.contains(query, true) }
        .sortedByDescending { it.timestamp }
        .toList()
    val groups = matching.groupBy { entry ->
        YearMonth.from(Instant.ofEpochMilli(entry.timestamp).atZone(zone))
    }.toSortedMap(compareByDescending<YearMonth> { it })

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "recognition-history:search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                placeholder = { Text(stringResource(R.string.search)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                ),
            )
        }

        if (entries.isEmpty()) {
            item(key = "recognition-history:empty") {
                EmptyRecognitionHistory(
                    icon = Icons.Rounded.History,
                    message = stringResource(R.string.recognition_history_empty),
                )
            }
        } else if (matching.isEmpty()) {
            item(key = "recognition-history:no-matches") {
                EmptyRecognitionHistory(
                    icon = Icons.Rounded.Search,
                    message = stringResource(R.string.recognition_history_no_matches),
                )
            }
        } else {
            groups.forEach { (month, monthEntries) ->
                item(key = "recognition-history:month:$month") {
                    Text(
                        text = month.atDay(1).atStartOfDay(zone).format(monthFormatter).lowercase(locale),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(start = 22.dp, top = 14.dp, bottom = 2.dp),
                    )
                }
                items(monthEntries, key = { it.id }) { entry ->
                    RecognitionHistoryRow(
                        entry = entry,
                        timestamp = Instant.ofEpochMilli(entry.timestamp).atZone(zone)
                            .format(timestampFormatter).lowercase(locale),
                        onClick = { onPlay(entry) },
                        onRemove = { onRemove(entry.id) },
                    )
                }
            }
        }
        item(key = "recognition-history:bottom-space") { Spacer(Modifier.height(12.dp)) }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissClearConfirmation,
            title = { Text(stringResource(R.string.recognition_history_clear_title)) },
            text = { Text(stringResource(R.string.recognition_history_clear_message)) },
            confirmButton = {
                TextButton(onClick = { onClear(); onDismissClearConfirmation() }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissClearConfirmation) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun RecognitionHistoryRow(
    entry: RecognitionHistoryEntry,
    timestamp: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = entry.artworkUrl,
            contentDescription = entry.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
        )
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)) {
            Text(
                text = entry.title,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.artist,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = timestamp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyRecognitionHistory(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}
