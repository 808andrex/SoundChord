package com.soundchord.features.stations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import com.soundchord.features.R
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UiState
import com.music.bitchord.ui.components.PAGE_GUTTER
@Composable
fun StationDetailScreen(
    seed: Song,
    queue: UiState<List<Song>>,
    contentPadding: PaddingValues,
    onPlaySeed: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(seed.videoId) { onRetry() }
    LazyColumn(contentPadding = contentPadding, modifier = modifier.fillMaxSize()) {
        item {
            Text(
                stringResource(R.string.station_title_format, seed.title),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = PAGE_GUTTER, end = PAGE_GUTTER, top = 12.dp, bottom = 20.dp),
            )
        }
        item { StationSeedCard(seed, onPlaySeed) }
        item {
            Text(
                stringResource(R.string.station_up_next),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = PAGE_GUTTER, end = PAGE_GUTTER, top = 28.dp, bottom = 12.dp),
            )
        }
        when (queue) {
            UiState.Loading -> item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            is UiState.Error -> item { StationMessage(queue.message, "Reintentar", onRetry) }
            is UiState.Success -> if (queue.data.isEmpty()) {
                item { StationMessage(stringResource(R.string.station_empty_suggestions), stringResource(R.string.station_retry), onRetry) }
            } else {
                items(queue.data, key = Song::videoId) { song -> StationQueueRow(song, onClick = { onPlaySong(song) }) }
            }
        }
    }
}

@Composable
private fun StationSeedCard(seed: Song, onPlay: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = PAGE_GUTTER).clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f)).clickable(onClick = onPlay).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(seed.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(104.dp).clip(RoundedCornerShape(18.dp)))
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(stringResource(R.string.station_song_label).uppercase(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(seed.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
            Text(seed.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onPlay) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.play), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun StationQueueRow(song: Song, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = PAGE_GUTTER, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(song.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            Text(song.title, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        song.durationText?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun StationMessage(message: String, action: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = PAGE_GUTTER, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Text(action, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onAction).padding(8.dp))
    }
}
