package com.soundchord.features.radio.ui

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UiState
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.soundchord.features.radio.data.Radio
import com.soundchord.features.radio.data.ApproxLocation
import com.soundchord.features.R
import com.soundchord.features.icons.SoundChordIcons

import java.util.Locale

private data class RadioGenre(val tag: String)

private val RADIO_GENRES = Radio.GENRES.map { tag ->
    RadioGenre(tag)
}

private val RADIO_COUNTRIES = listOf("AR", "BR", "CO", "EC", "FR", "DE", "IN", "IT", "JP", "MX", "ES", "GB", "US")

@Composable
fun RadioScreen(
    listState: LazyListState,
    contentPadding: PaddingValues,
    stations: UiState<List<Song>>,
    onPersonalStationClick: (Song) -> Unit,
    onStationPlay: (Song) -> Unit,
    onStationClick: (Radio.Station) -> Unit,
    onCategoryClick: (kind: String, key: String, title: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    val favourites by Radio.favourites.collectAsState()
    val context = LocalContext.current
    var locationRefresh by remember { mutableIntStateOf(0) }
    var locationPermissionDenied by remember { mutableStateOf(false) }
    val locationSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        locationRefresh++
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        locationPermissionDenied = !granted
        if (granted && !ApproxLocation.enabled(context)) {
            locationSettings.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        if (granted) locationRefresh++
    }
    var nearby by remember { mutableStateOf<List<Radio.Nearby>>(emptyList()) }
    var localFallback by remember { mutableStateOf<List<Radio.Station>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { Radio.ensureLoaded() }
    }

    LaunchedEffect(Unit) {
        when {
            !ApproxLocation.granted(context) -> locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            !ApproxLocation.enabled(context) -> locationSettings.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    LaunchedEffect(context, locationRefresh) {
        loading = true
        val location = runCatching { ApproxLocation.get(context) }.getOrNull()
        val place = location?.let { loc ->
            runCatching { ApproxLocation.place(context, loc.latitude, loc.longitude) }.getOrNull()
        }
        val cityAndRegion = if (location != null && !place?.countryCode.isNullOrBlank()) {
            runCatching { Radio.inArea(place!!.countryCode!!, place.city, place.state) }.getOrDefault(emptyList())
        } else emptyList()
        nearby = if (cityAndRegion.isNotEmpty()) {
            emptyList()
        } else {
            location?.let { loc ->
                runCatching { Radio.nearby(loc.latitude, loc.longitude) }.getOrDefault(emptyList())
            }.orEmpty()
        }
        val country = place?.countryCode?.takeIf { it.isNotBlank() } ?: Locale.getDefault().country
        localFallback = if (location == null && country.isNotBlank()) {
            runCatching { Radio.inCountry(country, 30) }.getOrDefault(emptyList())
        } else cityAndRegion
        loading = false
    }

    val gridColumns = 2

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        item(key = "radio-title") {
            Text(
                text = stringResource(R.string.radio_title),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(
                    start = PAGE_GUTTER,
                    end = PAGE_GUTTER,
                    top = 8.dp,
                    bottom = 18.dp,
                ),
            )
        }

        if (favourites.isNotEmpty()) {
            item(key = "favourites-heading") { SectionHeading(stringResource(R.string.radio_favourites)) }
            item(key = "favourites-cards") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(favourites, key = { "favourite-${it.uuid}" }) { station ->
                        FeaturedStationCard(station, onClick = { onStationClick(station) })
                    }
                }
            }
        }

        item(key = "personal-stations-heading") {
            SectionHeading(stringResource(R.string.radio_personal_stations))
        }
        when (stations) {
            UiState.Loading -> item(key = "personal-stations-loading") {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> item(key = "personal-stations-error") {
                Text(stations.message, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 12.dp))
            }
            is UiState.Success -> if (stations.data.isEmpty()) {
                item(key = "personal-stations-empty") {
                    Text(stringResource(R.string.radio_personal_station_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 12.dp))
                }
            } else {
                item(key = "personal-stations-cards") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(stations.data, key = { "personal-${it.videoId}" }) { seed ->
                            PersonalStationCard(
                                seed = seed,
                                onClick = { onPersonalStationClick(seed) },
                                onPlay = { onStationPlay(seed) },
                            )
                        }
                    }
                }
            }
        }

        item(key = "local-heading") { SectionHeading(stringResource(R.string.radio_nearby)) }
        if (nearby.isNotEmpty()) {
            items(nearby.take(12), key = { "nearby-${it.station.uuid}" }) { item ->
                PopularStationRow(item.station, onClick = { onStationClick(item.station) }, detail = Radio.formatDistance(item.distanceKm))
            }
        } else if (localFallback.isNotEmpty()) {
            items(localFallback.take(12), key = { "local-${it.uuid}" }) { station ->
                PopularStationRow(station, onClick = { onStationClick(station) })
            }
        } else if (loading) {
            item(key = "local-loading") {
                Box(Modifier.fillMaxWidth().height(112.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            item(key = "local-empty") {
                Text(
                    when {
                        !ApproxLocation.granted(context) -> stringResource(R.string.radio_location_permission_needed)
                        !ApproxLocation.enabled(context) -> stringResource(R.string.radio_location_enabled_needed)
                        locationPermissionDenied -> stringResource(R.string.radio_location_permission_needed)
                        else -> stringResource(R.string.radio_no_local_stations)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 10.dp),
                )
                if (!ApproxLocation.granted(context)) {
                    Text(
                        stringResource(R.string.radio_grant_location),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
                            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                    )
                } else if (!ApproxLocation.enabled(context)) {
                    Text(
                        stringResource(R.string.radio_enable_location),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { locationSettings.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                    )
                }
            }
        }

        item(key = "genres-heading") { SectionHeading(stringResource(R.string.radio_genres)) }
        items(RADIO_GENRES.chunked(gridColumns), key = { "genre-${it.first().tag}" }) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { genre ->
                    val genreTitle = stringResource(genreLabelResource(genre.tag))
                    GenreCard(
                        title = genreTitle,
                        subtitle = stringResource(R.string.radio_stations_for, genreTitle),
                        selected = false,
                        onClick = { onCategoryClick("genre", genre.tag, genreTitle) },
                        paletteKey = genre.tag,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(gridColumns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        item(key = "countries-heading") { SectionHeading(stringResource(R.string.radio_countries)) }
        items(RADIO_COUNTRIES.chunked(gridColumns), key = { row -> "country-${row.joinToString()}" }) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { country ->
                    val countryName = stringResource(countryLabelResource(country))
                    GenreCard(
                        title = countryName,
                        subtitle = stringResource(R.string.radio_stations_for, countryName),
                        selected = false,
                        onClick = { onCategoryClick("country", country, countryName) },
                        paletteKey = country,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(gridColumns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        item(key = "radio-bottom-space") { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun PersonalStationCard(seed: Song, onClick: () -> Unit, onPlay: () -> Unit) {
    Box(
        Modifier.width(292.dp)
            .height(238.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = seed.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1.23f),
        )
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            0f to Color.Black.copy(alpha = .12f),
            .36f to Color.Transparent,
            1f to Color.Black.copy(alpha = .88f),
        )))
        Text(
            text = stringResource(R.string.station_custom_label).uppercase(),
            color = Color.White.copy(alpha = .86f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 56.dp, bottom = 16.dp)) {
            Text(
                text = stringResource(R.string.station_title_format, seed.title),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(seed.artist, color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.bodyMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(
            onClick = onPlay,
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                .clip(CircleShape).background(Color.Black.copy(alpha = .52f)),
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.play),
                tint = Color.White, modifier = Modifier.size(26.dp))
        }
    }
}

private fun countryLabelResource(code: String): Int = when (code) {
    "EC" -> R.string.radio_country_ec
    "US" -> R.string.radio_country_us
    "MX" -> R.string.radio_country_mx
    "CO" -> R.string.radio_country_co
    "AR" -> R.string.radio_country_ar
    "ES" -> R.string.radio_country_es
    "BR" -> R.string.radio_country_br
    "GB" -> R.string.radio_country_gb
    "FR" -> R.string.radio_country_fr
    "DE" -> R.string.radio_country_de
    "IT" -> R.string.radio_country_it
    "JP" -> R.string.radio_country_jp
    else -> R.string.radio_country_in
}

private fun genreLabelResource(tag: String): Int = when (tag) {
    "pop" -> R.string.radio_genre_pop
    "rock" -> R.string.radio_genre_rock
    "jazz" -> R.string.radio_genre_jazz
    "classical" -> R.string.radio_genre_classical
    "electronic" -> R.string.radio_genre_electronic
    "hiphop" -> R.string.radio_genre_hip_hop
    "lofi" -> R.string.radio_genre_lofi
    "news" -> R.string.radio_genre_news
    "talk" -> R.string.radio_genre_talk
    "ambient" -> R.string.radio_genre_ambient
    "country" -> R.string.radio_genre_country
    "bollywood" -> R.string.radio_genre_bollywood
    "chillout" -> R.string.radio_genre_chillout
    "dance" -> R.string.radio_genre_dance
    "80s" -> R.string.radio_genre_80s
    else -> R.string.radio_genre_90s
}

private fun radioCardColors(key: String): Pair<Color, Color> = when (key) {
    "EC", "CO" -> Color(0xFFB59A55) to Color(0xFF645B40)
    "ES" -> Color(0xFF9A514A) to Color(0xFF593E3D)
    "AR" -> Color(0xFF7FA8B9) to Color(0xFF4F6B7A)
    "MX", "BR" -> Color(0xFF62836A) to Color(0xFF3F5B4B)
    "US", "GB", "FR" -> Color(0xFF526782) to Color(0xFF343F54)
    "DE" -> Color(0xFF77705C) to Color(0xFF45423B)
    "IT" -> Color(0xFF66816D) to Color(0xFF43574C)
    "JP" -> Color(0xFF8A6666) to Color(0xFF51484A)
    "IN" -> Color(0xFF95724F) to Color(0xFF526A5D)
    "rock" -> Color(0xFF626878) to Color(0xFF3A414D)
    "jazz", "classical" -> Color(0xFF8B7958) to Color(0xFF544F49)
    "electronic", "dance" -> Color(0xFF557C7A) to Color(0xFF354D56)
    "pop", "hiphop" -> Color(0xFF8A5E70) to Color(0xFF514457)
    "news", "talk" -> Color(0xFF667A8C) to Color(0xFF414C5A)
    "ambient", "lofi", "chillout" -> Color(0xFF748375) to Color(0xFF47554D)
    else -> Color(0xFF79745F) to Color(0xFF49483F)
}

@Composable
private fun SectionHeading(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = PAGE_GUTTER, end = PAGE_GUTTER, top = 18.dp, bottom = 12.dp),
    )
}

@Composable
private fun FeaturedStationCard(station: Radio.Station, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(196.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.16f)) {
            StationArtwork(station, Modifier.matchParentSize())
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF453A))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(stringResource(R.string.radio_live), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            station.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 9.dp),
        )
        Text(
            station.nearbyDetails(null).ifBlank { station.country ?: stringResource(R.string.radio_live_broadcast) },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GenreCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    paletteKey: String = title,
    modifier: Modifier = Modifier,
) {
    val colors = radioCardColors(paletteKey)
    Column(
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(listOf(colors.first, colors.second)),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun PopularStationRow(station: Radio.Station, onClick: () -> Unit, detail: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PAGE_GUTTER, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StationArtwork(station, Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(station.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail ?: station.nearbyDetails(null).ifBlank { station.country ?: stringResource(R.string.radio_live_broadcast) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(SoundChordIcons.TabRadio, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun StationArtwork(station: Radio.Station, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!station.favicon.isNullOrBlank()) {
            AsyncImage(
                model = station.favicon,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Icon(SoundChordIcons.TabRadio, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        }
    }
}
