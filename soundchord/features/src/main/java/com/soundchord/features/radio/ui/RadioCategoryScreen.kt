package com.soundchord.features.radio.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.soundchord.features.R
import com.soundchord.features.radio.data.Radio
import com.music.bitchord.ui.components.PAGE_GUTTER

@Composable
fun RadioCategoryScreen(
    kind: String,
    key: String,
    title: String,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onStationClick: (Radio.Station) -> Unit,
    modifier: Modifier = Modifier,
) {
    var stations by remember(kind, key) { mutableStateOf<List<Radio.Station>?>(null) }
    LaunchedEffect(kind, key) {
        stations = runCatching {
            when (kind) {
                "genre" -> Radio.byTag(key, 50)
                "country" -> Radio.inCountry(key, 50)
                else -> emptyList()
            }
        }.getOrDefault(emptyList())
    }
    LazyColumn(state = listState, contentPadding = contentPadding, modifier = modifier) {
        item(key = "category-heading") {
            Text(
                stringResource(R.string.radio_stations_for, title),
                modifier = Modifier.padding(start = PAGE_GUTTER, end = PAGE_GUTTER, top = 18.dp, bottom = 14.dp),
            )
        }
        when {
            stations == null -> item(key = "category-loading") { CircularProgressIndicator(Modifier.padding(PAGE_GUTTER)) }
            stations!!.isEmpty() -> item(key = "category-empty") {
                Text(stringResource(R.string.radio_category_empty), modifier = Modifier.padding(PAGE_GUTTER))
            }
            else -> items(stations!!, key = { "station-${it.uuid}" }) { station ->
                RadioStationRow(station, onClick = { onStationClick(station) })
            }
        }
    }
}

@Composable
private fun RadioStationRow(station: Radio.Station, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = PAGE_GUTTER, vertical = 7.dp)
            .clickableNoRipple(onClick),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Box(Modifier.size(54.dp)) {
            coil3.compose.AsyncImage(
                model = station.favicon,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        }
        androidx.compose.foundation.layout.Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
            Text(
                text = station.details.ifBlank { station.nearbyDetails(null) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    clickable(onClick = onClick)
