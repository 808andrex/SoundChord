package com.soundchord.features.radio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.soundchord.features.R
import com.soundchord.features.radio.data.RadioStation
import com.music.bitchord.ui.components.SearchField

enum class RadioFavoritesSort { DEFAULT, NAME_ASC, NAME_DESC }

/** Library detail page for the stations stored by PreferencesRadioFavoritesStore. */
@Composable
fun RadioFavoritesScreen(
    stations: List<RadioStation>,
    onPlay: (RadioStation) -> Unit,
    onRemoveFavorite: (RadioStation) -> Unit,
    contentPadding: PaddingValues,
    searching: Boolean = false,
    sort: RadioFavoritesSort = RadioFavoritesSort.DEFAULT,
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(searching) { if (!searching) searchQuery = "" }
    val visibleStations = remember(stations, searchQuery, sort) {
        val filtered = stations.filter { station ->
            searchQuery.isBlank() || listOfNotNull(station.name, station.country, station.codec)
                .any { it.contains(searchQuery.trim(), ignoreCase = true) }
        }
        when (sort) {
            RadioFavoritesSort.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            RadioFavoritesSort.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            RadioFavoritesSort.DEFAULT -> filtered
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(key = "radio_favorites_header") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(0.76f).aspectRatio(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Radio,
                            contentDescription = null,
                            modifier = Modifier.size(112.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(R.string.radio_favourites),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.radio_favorites_count, stations.size),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                if (stations.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(58.dp).clickable { onPlay(stations.first()) },
                        shape = RoundedCornerShape(30.dp),
                        color = androidx.compose.ui.graphics.Color.White,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = androidx.compose.ui.graphics.Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.play),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = androidx.compose.ui.graphics.Color.Black,
                            )
                        }
                    }
                }
                if (searching) {
                    Spacer(Modifier.height(14.dp))
                    SearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSubmit = {},
                        placeholder = stringResource(R.string.radio_search_list),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    )
                }
            }
        }
        if (stations.isEmpty()) {
            item(key = "radio_favorites_empty") {
                Text(
                    stringResource(R.string.radio_favorites_empty),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (visibleStations.isEmpty()) {
            item(key = "radio_favorites_search_empty") {
                Text(
                    stringResource(R.string.radio_search_empty),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            itemsIndexed(visibleStations, key = { _, station -> station.uuid }) { index, station ->
                Column {
                    FavoriteStationRow(
                        station = station,
                        onPlay = { onPlay(station) },
                        onRemove = { onRemoveFavorite(station) },
                    )
                    if (index < visibleStations.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 96.dp, end = 20.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteStationRow(station: RadioStation, onPlay: () -> Unit, onRemove: () -> Unit) {
    var artworkFailed by remember(station.uuid) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!station.favicon.isNullOrBlank() && !artworkFailed) {
                AsyncImage(
                    station.favicon,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    onError = { artworkFailed = true },
                )
            } else {
                Icon(Icons.Rounded.Radio, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(station.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    station.country?.takeIf(String::isNotBlank),
                    station.codec?.uppercase()?.takeIf(String::isNotBlank),
                    station.bitrate.takeIf { it > 0 }?.let { stringResource(R.string.radio_bitrate_format, it) },
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Rounded.Star, stringResource(R.string.radio_remove_favorite), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

