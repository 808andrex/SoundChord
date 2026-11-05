package com.soundchord.features.radio.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.bitchord.data.model.Song
import com.soundchord.features.R
import com.soundchord.features.radio.data.RadioStation
import com.soundchord.features.radio.playback.toSong
import com.soundchord.features.icons.SoundChordIcons

@Composable
fun RadioScreen(
    onPlayStation: (Song) -> Unit,
    viewModel: RadioViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Buscador de Radio
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            placeholder = { Text(stringResource(R.string.search_radio_placeholder)) },
            singleLine = true
        )

        // Estado de la Lista
        when (val state = uiState) {
            is RadioUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is RadioUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is RadioUiState.Success -> {
                if (state.stations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.radio_category_empty))
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = state.stations,
                            key = { it.uuid }
                        ) { station ->
                            RadioStationRow(
                                station = station,
                                onClick = {
                                    val song = station.toSong(context)
                                    onPlayStation(song)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RadioStationRow(
    station: RadioStation,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icono / Carátula de la emisora utilizando el ImageVector propio
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = SoundChordIcons.Radio,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            Text(
                text = listOfNotNull(
                    station.country,
                    station.codec?.uppercase(),
                    if (station.bitrate > 0) "${station.bitrate} kbps" else null
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}