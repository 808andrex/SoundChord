package com.soundchord.features.radio.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.icons.BitChordIcons
import com.soundchord.features.R
import com.soundchord.features.radio.data.RadioStation
import com.soundchord.features.radio.playback.toSong

private data class CountryOption(val code: String, val label: Int)
private data class GenreOption(val tag: String, val label: Int)

private val radioGenres = listOf(
    GenreOption("pop", R.string.radio_genre_pop), GenreOption("rock", R.string.radio_genre_rock),
    GenreOption("jazz", R.string.radio_genre_jazz), GenreOption("classical", R.string.radio_genre_classical),
    GenreOption("electronic", R.string.radio_genre_electronic), GenreOption("hip hop", R.string.radio_genre_hip_hop),
    GenreOption("lofi", R.string.radio_genre_lofi), GenreOption("news", R.string.radio_genre_news),
    GenreOption("talk", R.string.radio_genre_talk), GenreOption("ambient", R.string.radio_genre_ambient),
    GenreOption("country", R.string.radio_genre_country), GenreOption("bollywood", R.string.radio_genre_bollywood),
    GenreOption("chillout", R.string.radio_genre_chillout), GenreOption("dance", R.string.radio_genre_dance),
    GenreOption("80s", R.string.radio_genre_80s), GenreOption("90s", R.string.radio_genre_90s),
)

private val radioCountries = listOf(
    CountryOption("AR", R.string.radio_country_ar), CountryOption("BR", R.string.radio_country_br),
    CountryOption("CO", R.string.radio_country_co), CountryOption("EC", R.string.radio_country_ec),
    CountryOption("FR", R.string.radio_country_fr), CountryOption("DE", R.string.radio_country_de),
    CountryOption("IN", R.string.radio_country_in), CountryOption("IT", R.string.radio_country_it),
    CountryOption("JP", R.string.radio_country_jp), CountryOption("MX", R.string.radio_country_mx),
    CountryOption("ES", R.string.radio_country_es), CountryOption("GB", R.string.radio_country_gb),
    CountryOption("US", R.string.radio_country_us),
)

private val cardGradients = listOf(
    listOf(Color(0xFF79566F), Color(0xFF493E53)), listOf(Color(0xFF48586D), Color(0xFF303B4A)),
    listOf(Color(0xFF71684F), Color(0xFF494538)), listOf(Color(0xFF397074), Color(0xFF294B50)),
    listOf(Color(0xFF596957), Color(0xFF3B493C)), listOf(Color(0xFF755B70), Color(0xFF4D3D4D)),
)

@Composable
fun RadioScreen(onPlayStation: (Song) -> Unit, viewModel: RadioViewModel? = null) {
    val context = LocalContext.current
    val vm = viewModel ?: composeViewModel(factory = remember(context) { RadioViewModel.Factory(context) })
    val state by vm.uiState.collectAsState()
    val favorites by vm.favoriteStations.collectAsState()
    val genres by vm.popularGenres.collectAsState()
    val playing by vm.currentlyPlayingStation.collectAsState()
    val nearby by vm.nearbyStations.collectAsState()
    val country by vm.countryCode.collectAsState()
    val genre by vm.genre.collectAsState()
    var showAllFavorites by remember { mutableStateOf(false) }

    val detailTitle = when {
        genre != null -> localizedGenre(genre!!)
        country != null -> radioCountries.firstOrNull { it.code == country }?.let { stringResource(it.label) }
        else -> null
    }
    val detailStateActive = detailTitle != null
    BackHandler(enabled = showAllFavorites || detailStateActive) {
        if (showAllFavorites) {
            showAllFavorites = false
        } else {
            vm.onGenreChanged(null)
            vm.onCountryCodeChanged(null)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (showAllFavorites) {
            item(key = "favorites_header") {
                RadioSubpageHeader(stringResource(R.string.radio_favourites)) { showAllFavorites = false }
            }
            if (favorites.isEmpty()) {
                item { Text(stringResource(R.string.radio_favorites_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(favorites, key = { "all_favorite_${it.uuid}" }) { station ->
                    StationRow(station, station.uuid == playing?.uuid, vm.isFavorite(station.uuid), { vm.toggleFavorite(station) }, {
                        vm.setCurrentlyPlayingStation(station)
                        onPlayStation(station.toSong(context))
                    })
                }
            }
        } else if (detailStateActive) {
            item(key = "detail_header") {
                RadioSubpageHeader(stringResource(R.string.radio_stations_for, detailTitle.orEmpty())) {
                    vm.onGenreChanged(null)
                    vm.onCountryCodeChanged(null)
                }
            }
            when (state) {
                RadioUiState.Loading -> item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                is RadioUiState.Error -> item { Text((state as RadioUiState.Error).message, color = MaterialTheme.colorScheme.error) }
                is RadioUiState.Success -> {
                    val stations = (state as RadioUiState.Success).stations
                    if (stations.isEmpty()) item { Text(stringResource(R.string.radio_category_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(stations, key = { "detail_${it.uuid}" }) { station ->
                        StationRow(station, station.uuid == playing?.uuid, vm.isFavorite(station.uuid), { vm.toggleFavorite(station) }, {
                            vm.setCurrentlyPlayingStation(station); onPlayStation(station.toSong(context))
                        })
                    }
                }
            }
        } else {
            item(key = "title") { Text(stringResource(R.string.radio_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)) }

            item(key = "favorites") {
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle(stringResource(R.string.radio_favourites), prominent = true, modifier = Modifier.weight(1f))
                        TextButton(onClick = { showAllFavorites = true }) {
                            Text(stringResource(R.string.radio_show_all), color = Color.White)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (favorites.isEmpty()) {
                        Text(stringResource(R.string.radio_favorites_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 20.dp))
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(favorites, key = { "favorite_${it.uuid}" }) { station ->
                                FavoriteStationCard(
                                    station = station,
                                    isFavorite = vm.isFavorite(station.uuid),
                                    onFavorite = { vm.toggleFavorite(station) },
                                    onPlay = { vm.setCurrentlyPlayingStation(station); onPlayStation(station.toSong(context)) },
                                )
                            }
                        }
                    }
                }
            }

            item(key = "nearby_title") { SectionTitle(stringResource(R.string.radio_nearby)) }
            if (nearby.isEmpty()) {
                item { Text(stringResource(R.string.radio_no_local_stations), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(nearby, key = { "nearby_${it.uuid}" }) { station ->
                    StationRow(station, station.uuid == playing?.uuid, vm.isFavorite(station.uuid), { vm.toggleFavorite(station) }, {
                        vm.setCurrentlyPlayingStation(station); onPlayStation(station.toSong(context))
                    })
                }
            }

            item(key = "genres_title") { SectionTitle(stringResource(R.string.radio_genres)) }
            if (genres.isEmpty()) item { Text(stringResource(R.string.radio_genres_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            val availableTags = genres.map { it.name.lowercase().replace('-', ' ').trim() }.toSet()
            val displayedGenres = radioGenres.filter { it.tag in availableTags }.ifEmpty { radioGenres }
            items(displayedGenres.chunked(2), key = { row -> "genre_${row.firstOrNull()?.tag.orEmpty()}" }) { row ->
                CategoryGridRow(row.map { it.label }) { index -> vm.onGenreChanged(row[index].tag) }
            }

            item(key = "countries_title") { SectionTitle(stringResource(R.string.radio_countries)) }
            items(radioCountries.chunked(2), key = { row -> "country_${row.first().code}" }) { row ->
                CategoryGridRow(row.map { it.label }) { index -> vm.onCountryCodeChanged(row[index].code) }
            }
        }
    }
}

@Composable
private fun RadioSubpageHeader(title: String, onBack: () -> Unit) {
    Column(Modifier.statusBarsPadding().padding(top = 8.dp, bottom = 8.dp)) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = .08f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = .22f)),
            onClick = onBack,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.radio_back),
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun SectionTitle(title: String, prominent: Boolean = false, modifier: Modifier = Modifier) {
    Text(
        title,
        modifier = modifier,
        style = if (prominent) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Color.White,
    )
}

@Composable
private fun FavoriteStationCard(station: RadioStation, isFavorite: Boolean, onFavorite: () -> Unit, onPlay: () -> Unit) {
    val gradient = cardGradients[(station.uuid.hashCode().and(Int.MAX_VALUE)) % cardGradients.size]
    var artworkFailed by remember(station.uuid) { mutableStateOf(false) }
    Column(Modifier.width(250.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onPlay),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF101114), gradient.first(), Color(0xFF101114))))) {
                if (!station.favicon.isNullOrBlank() && !artworkFailed) {
                    AsyncImage(station.favicon, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)), contentScale = ContentScale.Crop, onError = { artworkFailed = true })
                } else {
                    Icon(Icons.Rounded.Radio, null, Modifier.align(Alignment.Center).size(100.dp), tint = Color.White.copy(alpha = .88f))
                }
                Surface(Modifier.align(Alignment.BottomStart).padding(14.dp), color = Color(0xFFF44336), shape = CircleShape) {
                    Text(stringResource(R.string.radio_live), modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp), color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onFavorite, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) {
                    Icon(if (isFavorite) BitChordIcons.StarFilled else BitChordIcons.Star, stringResource(if (isFavorite) R.string.radio_remove_favorite else R.string.radio_add_favorite), tint = Color.White)
                }
            }
        }
        Text(
            text = station.name,
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(stationMetadata(station), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CategoryGridRow(labels: List<Int>, onClick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        labels.forEachIndexed { index, labelResource ->
            val title = stringResource(labelResource)
            val colors = cardGradients[(title.hashCode().and(Int.MAX_VALUE)) % cardGradients.size]
            Card(
                modifier = Modifier.weight(1f).height(148.dp).clickable { onClick(index) },
                shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Column(
                    Modifier.fillMaxSize().background(Brush.linearGradient(colors)).padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.radio_stations_for, title), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .86f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (labels.size == 1 && index == 0) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun StationRow(station: RadioStation, isPlaying: Boolean, isFavorite: Boolean, onFavorite: () -> Unit, onClick: () -> Unit) {
    var artworkFailed by remember(station.uuid) { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (isPlaying) Color.White.copy(alpha = .16f) else Color.Transparent,
        border = if (isPlaying) BorderStroke(1.dp, Color.White) else null,
        onClick = onClick,
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(52.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(contentAlignment = Alignment.Center) {
                    if (!station.favicon.isNullOrBlank() && !artworkFailed) {
                        AsyncImage(station.favicon, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop, onError = { artworkFailed = true })
                    } else {
                        Icon(Icons.Rounded.Radio, null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(station.name, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stationMetadata(station), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (isPlaying) Text(stringResource(R.string.radio_now_playing), style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
            IconButton(onClick = onFavorite) {
                Icon(if (isFavorite) BitChordIcons.StarFilled else BitChordIcons.Star, stringResource(if (isFavorite) R.string.radio_remove_favorite else R.string.radio_add_favorite), tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun stationMetadata(station: RadioStation): String = listOfNotNull(
    station.country,
    station.codec?.uppercase(),
    station.bitrate.takeIf { it > 0 }?.let { stringResource(R.string.radio_bitrate_format, it) },
).joinToString(" • ")

@Composable
private fun localizedGenre(tag: String): String = when (tag.lowercase()) {
    "pop" -> stringResource(R.string.radio_genre_pop)
    "rock" -> stringResource(R.string.radio_genre_rock)
    "jazz" -> stringResource(R.string.radio_genre_jazz)
    "classical", "classica", "clásica" -> stringResource(R.string.radio_genre_classical)
    "electronic", "electronic music", "dance" -> stringResource(R.string.radio_genre_electronic)
    "hip hop", "hip-hop", "hiphop" -> stringResource(R.string.radio_genre_hip_hop)
    "lofi", "lo-fi" -> stringResource(R.string.radio_genre_lofi)
    "news" -> stringResource(R.string.radio_genre_news)
    "talk" -> stringResource(R.string.radio_genre_talk)
    "ambient" -> stringResource(R.string.radio_genre_ambient)
    "country" -> stringResource(R.string.radio_genre_country)
    "bollywood" -> stringResource(R.string.radio_genre_bollywood)
    "chillout" -> stringResource(R.string.radio_genre_chillout)
    "80s" -> stringResource(R.string.radio_genre_80s)
    "90s" -> stringResource(R.string.radio_genre_90s)
    else -> tag.replaceFirstChar { it.uppercase() }
}
