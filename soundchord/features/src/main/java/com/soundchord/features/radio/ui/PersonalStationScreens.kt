package com.soundchord.features.radio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.Song
import com.soundchord.features.R

@Composable
internal fun PersonalStationsSection(
    state: RecentStationsState,
    onOpen: (Song) -> Unit,
    onPlay: (Song) -> Unit,
    onRetry: () -> Unit,
) {
    Column {
        Text(
            stringResource(R.string.radio_personal_stations),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(Modifier.height(12.dp))
        when (state) {
            RecentStationsState.Loading -> Box(
                Modifier.fillMaxWidth().height(210.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            RecentStationsState.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.station_load_error), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onRetry) { Text(stringResource(R.string.station_retry)) }
            }

            is RecentStationsState.Success -> if (state.songs.isEmpty()) {
                Text(
                    stringResource(R.string.radio_personal_station_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(state.songs, key = { "personal_station_${it.videoId}" }) { seed ->
                        PersonalStationCard(seed, onOpen = { onOpen(seed) }, onPlay = { onPlay(seed) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalStationCard(seed: Song, onOpen: () -> Unit, onPlay: () -> Unit) {
    Box(
        Modifier
            .width(300.dp)
            .height(330.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF202124))
            .clickable(onClick = onOpen),
    ) {
        seed.thumbnailUrl?.let { artwork ->
            AsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } ?: Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = .6f),
            modifier = Modifier.align(Alignment.Center).size(76.dp),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = .25f),
                    .42f to Color.Transparent,
                    1f to Color.Black.copy(alpha = .88f),
                ),
            ),
        )
        Text(
            stringResource(R.string.station_custom_label).uppercase(),
            modifier = Modifier.align(Alignment.TopStart).padding(18.dp),
            color = Color.White.copy(alpha = .92f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 18.dp, end = 76.dp, bottom = 20.dp),
        ) {
            Text(
                stringResource(R.string.station_title_format, seed.title),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                seed.artist,
                color = Color.White.copy(alpha = .82f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Surface(
            onClick = onPlay,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(58.dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = .6f),
            contentColor = Color.White,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PlayArrow, stringResource(R.string.play), modifier = Modifier.size(34.dp))
            }
        }
    }
}

@Composable
internal fun PersonalStationDetailScreen(
    seed: Song,
    queueState: PersonalStationQueueState,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onPlayQueueSong: (Song) -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.radio_back),
                    tint = Color.White,
                )
            }
            Text(
                stringResource(R.string.station_title_format, seed.title),
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "station_seed") { PersonalStationHero(seed, onPlay) }
            item(key = "station_up_next_title") {
                Text(
                    stringResource(R.string.station_up_next),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
            when (queueState) {
                PersonalStationQueueState.Loading -> item {
                    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                PersonalStationQueueState.Error -> item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.station_load_error), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = onRetry) { Text(stringResource(R.string.station_retry)) }
                    }
                }

                is PersonalStationQueueState.Success -> if (queueState.songs.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.station_empty_suggestions),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else items(queueState.songs, key = { "station_queue_${seed.videoId}_${it.videoId}" }) { song ->
                    StationQueueRow(song, onClick = { onPlayQueueSong(song) })
                }
            }
        }
    }
}

@Composable
private fun PersonalStationHero(seed: Song, onPlay: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF151618))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(112.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF292A2D)),
            contentAlignment = Alignment.Center,
        ) {
            seed.thumbnailUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                ?: Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = .6f))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                stringResource(R.string.station_song_label).uppercase(),
                color = Color.White.copy(alpha = .8f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                seed.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                seed.artist,
                color = Color.White.copy(alpha = .72f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onPlay, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Rounded.PlayArrow, stringResource(R.string.play), tint = Color.White, modifier = Modifier.size(38.dp))
        }
    }
}

@Composable
private fun StationQueueRow(song: Song, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(70.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF242528)),
            contentAlignment = Alignment.Center,
        ) {
            song.thumbnailUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                ?: Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = .6f))
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(song.title, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, color = Color.White.copy(alpha = .68f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        song.durationText?.let { Text(it, color = Color.White.copy(alpha = .56f), style = MaterialTheme.typography.bodySmall) }
    }
}
