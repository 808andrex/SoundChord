package com.soundchord.features.radio.ui

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.music.bitchord.ui.components.coverSharedElement
import com.music.bitchord.ui.components.rememberCoverTap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import androidx.core.content.ContextCompat
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.KeyedMix
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.ui.components.LocalLiquidGlassEnabled
import com.music.bitchord.ui.components.ShelfRow
import com.music.bitchord.ui.icons.BitChordIcons
import com.soundchord.features.R
import com.soundchord.features.radio.data.RadioStation
import com.soundchord.features.radio.data.RadioHistoryStore
import com.soundchord.features.radio.playback.toSong

private data class CountryOption(val code: String, val label: Int, val colors: List<Color>)
private data class GenreOption(val tag: String, val label: Int, val colors: List<Color>)
private data class CategoryOption(val label: Int, val colors: List<Color>)

private val radioGenres = listOf(
    GenreOption("pop", R.string.radio_genre_pop, listOf(Color(0xFFFF4D7D), Color(0xFFB90055))),
    GenreOption("rock", R.string.radio_genre_rock, listOf(Color(0xFFFF6B35), Color(0xFF9E251E))),
    GenreOption("jazz", R.string.radio_genre_jazz, listOf(Color(0xFF9B59FF), Color(0xFF4B2AA8))),
    GenreOption("classical", R.string.radio_genre_classical, listOf(Color(0xFF418BFF), Color(0xFF263D9B))),
    GenreOption("electronic", R.string.radio_genre_electronic, listOf(Color(0xFFEF3DDB), Color(0xFF7028D9))),
    GenreOption("hip hop", R.string.radio_genre_hip_hop, listOf(Color(0xFFFFB52E), Color(0xFFC33A16))),
    GenreOption("lofi", R.string.radio_genre_lofi, listOf(Color(0xFF26C6C8), Color(0xFF176B9A))),
    GenreOption("news", R.string.radio_genre_news, listOf(Color(0xFF398BFF), Color(0xFF154A9B))),
    GenreOption("talk", R.string.radio_genre_talk, listOf(Color(0xFFFF884D), Color(0xFFB83D51))),
    GenreOption("ambient", R.string.radio_genre_ambient, listOf(Color(0xFF32D6B0), Color(0xFF197E86))),
    GenreOption("country", R.string.radio_genre_country, listOf(Color(0xFF7FBD38), Color(0xFF286B37))),
    GenreOption("bollywood", R.string.radio_genre_bollywood, listOf(Color(0xFFFF9F1C), Color(0xFFD62F6B))),
    GenreOption("chillout", R.string.radio_genre_chillout, listOf(Color(0xFF36C7E8), Color(0xFF275FAE))),
    GenreOption("dance", R.string.radio_genre_dance, listOf(Color(0xFFFF3C91), Color(0xFF762BCE))),
    GenreOption("80s", R.string.radio_genre_80s, listOf(Color(0xFF19C5E8), Color(0xFFB535D1))),
    GenreOption("90s", R.string.radio_genre_90s, listOf(Color(0xFF387BFF), Color(0xFF19A9B7))),
)

private val radioCountries = listOf(
    CountryOption("AR", R.string.radio_country_ar, listOf(Color(0xFF62C9FF), Color(0xFF2387D2))),
    CountryOption("BR", R.string.radio_country_br, listOf(Color(0xFF18B957), Color(0xFF08763D))),
    CountryOption("CO", R.string.radio_country_co, listOf(Color(0xFFFFD329), Color(0xFF2457C5))),
    CountryOption("EC", R.string.radio_country_ec, listOf(Color(0xFFFFD329), Color(0xFF2457C5))),
    CountryOption("FR", R.string.radio_country_fr, listOf(Color(0xFF2863D8), Color(0xFFD6384B))),
    CountryOption("DE", R.string.radio_country_de, listOf(Color(0xFFD73535), Color(0xFFD9A323))),
    CountryOption("IN", R.string.radio_country_in, listOf(Color(0xFFFF962E), Color(0xFF18864A))),
    CountryOption("IT", R.string.radio_country_it, listOf(Color(0xFF1CA66A), Color(0xFFD94245))),
    CountryOption("JP", R.string.radio_country_jp, listOf(Color(0xFFE98591), Color(0xFFC3263D))),
    CountryOption("MX", R.string.radio_country_mx, listOf(Color(0xFF159A5B), Color(0xFFD83C45))),
    CountryOption("ES", R.string.radio_country_es, listOf(Color(0xFFE34035), Color(0xFFE7B52D))),
    CountryOption("GB", R.string.radio_country_gb, listOf(Color(0xFF2552A5), Color(0xFFD83D4B))),
    CountryOption("US", R.string.radio_country_us, listOf(Color(0xFF2855A6), Color(0xFFD83B4A))),
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RadioScreen(
    onPlayStation: (Song) -> Unit,
    currentSong: Song? = null,
    onPlayMix: (browseId: String, title: String, thumbnailUrl: String?) -> Unit = { _, _, _ -> },
    onRadioRefreshStateChanged: (Boolean) -> Unit = {},
    onStartStation: (Song) -> Unit = onPlayStation,
    viewModel: RadioViewModel? = null,
    stationsViewModel: PersonalStationsViewModel? = null,
    searchQuery: String? = null,
    onStationLongPress: (RadioStation, Boolean, Rect, () -> Unit) -> Unit = { _, _, _, _ -> },
    contentPadding: PaddingValues = PaddingValues(),
    onSubpageBackHandlerChanged: ((() -> Unit)?) -> Unit = {},
    pullToRefreshState: androidx.compose.material3.pulltorefresh.PullToRefreshState? = null,
    listState: LazyListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() },
) {
    val context = LocalContext.current
    val vm = viewModel ?: composeViewModel(factory = remember(context) { RadioViewModel.Factory(context) })
    val stationsVm = stationsViewModel ?: composeViewModel<PersonalStationsViewModel>()
    val state by vm.uiState.collectAsState()
    val favoriteUuids by vm.favoriteStationUuids.collectAsState(initial = emptySet())
    val recentStations by stationsVm.recentStations.collectAsState()
    val stationQueue by stationsVm.stationQueue.collectAsState()
    val genres by vm.popularGenres.collectAsState()
    val playing by vm.currentlyPlayingStation.collectAsState()
    val nearby by vm.nearbyStations.collectAsState()
    val nearbyState by vm.nearbyStationsState.collectAsState()
    val country by vm.countryCode.collectAsState()
    val genre by vm.genre.collectAsState()
    val personalizedMixes by vm.personalizedMixes.collectAsState()
    val radioListState = listState
    var showAllNearby by remember { mutableStateOf(false) }
    // Only a user-requested refresh drives the pull/chrome indicator. Initial
    // loads have their own inline placeholders and must not look like a refresh.
    val refreshing = vm.isRefreshing.collectAsState().value
    LaunchedEffect(refreshing) { onRadioRefreshStateChanged(refreshing) }
    LaunchedEffect(searchQuery) { if (searchQuery == null) vm.loadPersonalizedMixes() }
    LaunchedEffect(searchQuery) { if (searchQuery != null) vm.onSearchQueryChanged(searchQuery) }
    LaunchedEffect(currentSong?.videoId) {
        stationsVm.includeCurrentSong(currentSong)
        stationsVm.loadRecentStations()
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) vm.loadNearbyStations(context) else vm.onNearbyPermissionDenied()
    }
    LaunchedEffect(searchQuery) {
        if (searchQuery == null) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                vm.loadNearbyStations(context)
            } else {
                vm.onNearbyPermissionDenied()
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
    }
    var selectedStationSeed by remember { mutableStateOf<Song?>(null) }
    val playStation: (RadioStation) -> Unit = { station ->
        RadioHistoryStore.record(context, station)
        vm.setCurrentlyPlayingStation(station)
        onPlayStation(station.toSong(context))
    }

    val categoryDetailTitle = when {
        genre != null -> localizedGenre(genre!!)
        country != null -> radioCountries.firstOrNull { it.code == country }?.let { stringResource(it.label) }
        else -> null
    }
    val detailTitle = when {
        showAllNearby -> stringResource(R.string.radio_nearby)
        else -> categoryDetailTitle
    }
    val detailStateActive = detailTitle != null
    val subpageListState = rememberSaveable(detailTitle, saver = LazyListState.Saver) { LazyListState() }
    val subpageBack = remember(detailStateActive, showAllNearby, vm) {
        when {
            showAllNearby -> ({ showAllNearby = false })
            detailStateActive -> ({
                vm.onGenreChanged(null)
                vm.onCountryCodeChanged(null)
            })
            else -> null
        }
    }
    SideEffect { onSubpageBackHandlerChanged(subpageBack) }
    BackHandler(enabled = selectedStationSeed != null || detailStateActive) {
        if (selectedStationSeed != null) {
            selectedStationSeed = null
        } else if (showAllNearby) {
            showAllNearby = false
        } else {
            vm.onGenreChanged(null)
            vm.onCountryCodeChanged(null)
        }
    }

    selectedStationSeed?.let { seed ->
        PersonalStationDetailScreen(
            seed = seed,
            queueState = stationQueue,
            onBack = { selectedStationSeed = null },
            onPlay = { onStartStation(seed) },
            onPlayQueueSong = onPlayStation,
            onRetry = { stationsVm.loadStation(seed, force = true) },
        )
        return
    }

    val pullState = pullToRefreshState ?: rememberPullToRefreshState()
    val displayedGenres = remember(genres) {
        val availableTags = genres.map { it.name.lowercase().replace('-', ' ').trim() }.toSet()
        radioGenres.filter { it.tag in availableTags }.ifEmpty { radioGenres }
    }
    val chunkedGenres = remember(displayedGenres) { displayedGenres.chunked(2) }
    val chunkedCountries = remember { radioCountries.chunked(2) }
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            vm.refreshRadioData(context, hasLocation)
            stationsVm.loadRecentStations(force = true)
        },
        state = pullState,
        modifier = if (searchQuery != null) Modifier.fillMaxWidth().heightIn(max = 1600.dp) else Modifier.fillMaxSize(),
        indicator = {},
    ) {
    LazyColumn(
        state = if (detailStateActive) subpageListState else radioListState,
        modifier = if (searchQuery != null) Modifier.fillMaxWidth().heightIn(max = 1600.dp) else Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = if (searchQuery == null) contentPadding.calculateTopPadding() else 8.dp,
            bottom = if (searchQuery == null) contentPadding.calculateBottomPadding() else 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (searchQuery != null && searchQuery.isNotBlank()) {
            item(key = "radio_search_title") {
                Text(stringResource(R.string.radio_stations_for, searchQuery), style = MaterialTheme.typography.titleMedium)
            }
            when (val searchState = state) {
                RadioUiState.Loading -> item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                is RadioUiState.Error -> item { Text(searchState.message, color = MaterialTheme.colorScheme.error) }
                is RadioUiState.Success -> if (searchState.stations.isEmpty()) {
                    item { Text(stringResource(R.string.radio_search_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else itemsIndexed(searchState.stations, key = { _, it -> "radio_search_${it.uuid}" }) { index, station ->
                    StationRow(
                        station = station,
                        isPlaying = station.uuid == playing?.uuid,
                        isFavorite = station.uuid in favoriteUuids,
                        onFavorite = { vm.toggleFavorite(station) },
                        onClick = { playStation(station) },
                        onLongPress = { bounds ->
                            onStationLongPress(station, station.uuid in favoriteUuids, bounds) { vm.toggleFavorite(station) }
                        },
                        showDivider = index < searchState.stations.lastIndex,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        } else if (searchQuery != null) {
            item(key = "radio_search_hint") { Text(stringResource(R.string.radio_search_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (searchQuery != null) {
            // The integrated search results above are the whole page in this mode.
        } else if (detailStateActive) {
            item(key = "detail_header") {
                RadioSubpageTitle(
                    if (showAllNearby) detailTitle.orEmpty()
                    else stringResource(R.string.radio_stations_for, detailTitle.orEmpty()),
                )
            }
            if (showAllNearby) {
                when (nearbyState) {
                    NearbyStationsState.PermissionRequired -> item {
                        Text(stringResource(R.string.radio_location_permission_needed), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    NearbyStationsState.LocationUnavailable -> item {
                        Text(stringResource(R.string.radio_location_enabled_needed), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    NearbyStationsState.Loading -> item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    }
                    NearbyStationsState.Error -> item {
                        Text(stringResource(R.string.radio_no_local_stations), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    NearbyStationsState.Ready -> if (nearby.isEmpty()) {
                        item { Text(stringResource(R.string.radio_no_local_stations), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else itemsIndexed(nearby, key = { _, station -> "nearby_detail_${station.uuid}" }) { index, station ->
                        StationRow(
                            station = station,
                            isPlaying = station.uuid == playing?.uuid,
                            isFavorite = station.uuid in favoriteUuids,
                            onFavorite = { vm.toggleFavorite(station) },
                            onClick = { playStation(station) },
                            onLongPress = { bounds ->
                                onStationLongPress(station, station.uuid in favoriteUuids, bounds) { vm.toggleFavorite(station) }
                            },
                            showDivider = index < nearby.lastIndex,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            } else when (state) {
                RadioUiState.Loading -> item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                is RadioUiState.Error -> item { Text((state as RadioUiState.Error).message, color = MaterialTheme.colorScheme.error) }
                is RadioUiState.Success -> {
                    val stations = (state as RadioUiState.Success).stations
                    if (stations.isEmpty()) item { Text(stringResource(R.string.radio_category_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    itemsIndexed(stations, key = { _, it -> "detail_${it.uuid}" }) { index, station ->
                        StationRow(
                            station = station,
                            isPlaying = station.uuid == playing?.uuid,
                            isFavorite = station.uuid in favoriteUuids,
                            onFavorite = { vm.toggleFavorite(station) },
                            onClick = { playStation(station) },
                            onLongPress = { bounds ->
                                onStationLongPress(station, station.uuid in favoriteUuids, bounds) { vm.toggleFavorite(station) }
                            },
                            showDivider = index < stations.lastIndex,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        } else {
            item(key = "title") {
                Text(
                    text = stringResource(R.string.radio_title),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
                )
            }

            item(key = "quick_mix_grid") {
                PersonalizedMixesSection(
                    state = personalizedMixes,
                    onRetry = { vm.loadPersonalizedMixes(force = true) },
                    onPlayMix = { mix -> mix.browseId?.let { onPlayMix(it, mix.title, mix.thumbnailUrl) } },
                    glass = LocalLiquidGlassEnabled.current,
                    showQuickGrid = true,
                    showPersonalMixRows = false,
                )
            }

            item(key = "personal_stations") {
                PersonalStationsSection(
                    state = recentStations,
                    onOpen = { seed ->
                        selectedStationSeed = seed
                        stationsVm.loadStation(seed)
                    },
                    onPlay = onStartStation,
                    onRetry = { stationsVm.loadRecentStations(force = true) },
                )
            }

            item(key = "nearby_stations") {
                NearbyStationsSection(
                    stations = nearby,
                    state = nearbyState,
                    favoriteUuids = favoriteUuids,
                    playing = playing,
                    onFavorite = vm::toggleFavorite,
                    onPlay = playStation,
                    onLongPress = { station, bounds ->
                        onStationLongPress(station, station.uuid in favoriteUuids, bounds) { vm.toggleFavorite(station) }
                    },
                    onSeeAll = { showAllNearby = true },
                    onRequestPermission = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                    onEnableLocation = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
                )
            }

            item(key = "personalized_mixes") {
                PersonalizedMixesSection(
                    state = personalizedMixes,
                    onRetry = { vm.loadPersonalizedMixes(force = true) },
                    onPlayMix = { mix ->
                        mix.browseId?.let { id -> onPlayMix(id, mix.title, mix.thumbnailUrl) }
                    },
                    glass = LocalLiquidGlassEnabled.current,
                    showQuickGrid = false,
                    showPersonalMixRows = true,
                )
            }

            item(key = "genres_title") { SectionTitle(stringResource(R.string.radio_genres)) }
            if (genres.isEmpty()) item { Text(stringResource(R.string.radio_genres_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (displayedGenres.isNotEmpty()) items(
                items = chunkedGenres,
                key = { row -> "genre_${row.firstOrNull()?.tag.orEmpty()}" },
                contentType = { "genre_row" },
            ) { row ->
                CategoryGridRow(row.map { CategoryOption(it.label, it.colors) }) { index -> vm.onGenreChanged(row[index].tag) }
            }

            item(key = "countries_title") { SectionTitle(stringResource(R.string.radio_countries)) }
            items(
                items = chunkedCountries,
                key = { row -> "country_${row.first().code}" },
                contentType = { "country_row" },
            ) { row ->
                CategoryGridRow(row.map { CategoryOption(it.label, it.colors) }) { index -> vm.onCountryCodeChanged(row[index].code) }
            }
        }
    }
    }
}

private val QUICK_MIX_NAMES = listOf(
    "my supermix", "discover mix", "new release mix", "replay mix", "archive mix", "liked music",
)

private val SIGNATURE_MIX_TITLE_HINTS = listOf(
    listOf("my supermix", "mi supermix"),
    listOf("discover mix", "mix de descubrimiento"),
    listOf("new release mix", "mix de novedades"),
    listOf("replay mix", "mix de temas escuchados", "mix de repetición"),
    listOf("archive mix", "mix de archivo"),
    listOf("liked music", "música que me gusta", "musica que me gusta"),
)

@Composable
private fun PersonalizedMixesSection(
    state: UiState<List<KeyedMix>>,
    onRetry: () -> Unit,
    onPlayMix: (ShelfItem) -> Unit,
    glass: Boolean,
    showQuickGrid: Boolean,
    showPersonalMixRows: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state) {
            UiState.Loading -> {
                if (showQuickGrid) Box(Modifier.fillMaxWidth().height(104.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                if (showQuickGrid) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(state.message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
            }
            is UiState.Success -> {
                val (unique, normalizedQuick) = remember(state.data) {
                    val u = state.data.distinctBy { it.item.browseId ?: it.item.title }
                    val nq = QUICK_MIX_NAMES.mapIndexed { index, _ ->
                        val hints = SIGNATURE_MIX_TITLE_HINTS[index]
                        u.firstOrNull { keyed -> keyed.englishTitle.trim().lowercase() in hints }?.item
                            ?: u.firstOrNull { keyed -> keyed.item.title.trim().lowercase() in hints }?.item
                            ?: u.firstOrNull { keyed ->
                                val title = keyed.item.title.trim().lowercase()
                                when (index) {
                                    0 -> "supermix" in title
                                    1 -> "discover" in title || "descubr" in title
                                    2 -> "release" in title || "novedad" in title || "estreno" in title
                                    3 -> "replay" in title || "escuchad" in title || "repetici" in title
                                    4 -> "archive" in title || "archivo" in title
                                    else -> false
                                }
                            }?.item
                            ?: if (index == 5) u.firstOrNull {
                            it.item.browseId == com.music.bitchord.data.YtMusicRepository.LIKED_MUSIC ||
                                it.item.browseId == "LM" || it.item.browseId == "VLLM"
                        }?.item ?: ShelfItem(
                            title = "Liked music", subtitle = "Your liked songs", thumbnailUrl = null,
                            videoId = null, browseId = com.music.bitchord.data.YtMusicRepository.LIKED_MUSIC,
                        ) else ShelfItem(
                            title = QUICK_MIX_NAMES[index].toQuickMixTitle(),
                            subtitle = "", thumbnailUrl = null, videoId = null,
                            browseId = null,
                        )
                    }
                    u to nq
                }
                val chunkedQuick = remember(normalizedQuick) { normalizedQuick.chunked(3) }
                if (showQuickGrid) BoxWithConstraints(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        chunkedQuick.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { item ->
                                    QuickMixCard(item, glass, Modifier.weight(1f), onClick = { onPlayMix(item) })
                                }
                            }
                        }
                    }
                }
                val groups = remember(unique) { groupPersonalizedMixes(unique) }
                if (showPersonalMixRows && groups.yours.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tus mixes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                        ShelfRow(
                            contentPadding = PaddingValues(end = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(
                                items = groups.yours,
                                key = { it.browseId ?: it.title },
                                contentType = { "mix_card" },
                            ) { item ->
                                MixCarouselCard(item, glass, darkContainer = true, onClick = { onPlayMix(item) })
                            }
                        }
                    }
                }
                if (showPersonalMixRows) {
                    groups.moods.forEach { mood ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(mood.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                            ShelfRow(
                                contentPadding = PaddingValues(end = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(
                                    items = mood.items,
                                    key = { it.browseId ?: it.title },
                                    contentType = { "mix_card" },
                                ) { item ->
                                    MixCarouselCard(item, glass, darkContainer = false, onClick = { onPlayMix(item) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbyStationsSection(
    stations: List<RadioStation>,
    state: NearbyStationsState,
    favoriteUuids: Set<String>,
    playing: RadioStation?,
    onFavorite: (RadioStation) -> Unit,
    onPlay: (RadioStation) -> Unit,
    onLongPress: (RadioStation, Rect) -> Unit,
    onSeeAll: () -> Unit,
    onRequestPermission: () -> Unit,
    onEnableLocation: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(stringResource(R.string.radio_nearby))
            if (state == NearbyStationsState.Ready && stations.isNotEmpty()) {
                TextButton(onClick = onSeeAll) { Text(stringResource(R.string.radio_show_all), color = MaterialTheme.colorScheme.primary) }
            }
        }
        when (state) {
            NearbyStationsState.PermissionRequired -> Column {
                Text(stringResource(R.string.radio_location_permission_needed), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onRequestPermission) { Text(stringResource(R.string.radio_grant_location)) }
            }
            NearbyStationsState.LocationUnavailable -> Column {
                Text(stringResource(R.string.radio_location_enabled_needed), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onEnableLocation) { Text(stringResource(R.string.radio_enable_location)) }
            }
            NearbyStationsState.Loading -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            NearbyStationsState.Error -> Text(stringResource(R.string.radio_no_local_stations), color = MaterialTheme.colorScheme.onSurfaceVariant)
            NearbyStationsState.Ready -> if (stations.isEmpty()) {
                Text(stringResource(R.string.radio_no_local_stations), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                stations.take(8).forEachIndexed { index, station ->
                    StationRow(
                        station = station,
                        isPlaying = station.uuid == playing?.uuid,
                        isFavorite = station.uuid in favoriteUuids,
                        onFavorite = { onFavorite(station) },
                        onClick = { onPlay(station) },
                        onLongPress = { bounds -> onLongPress(station, bounds) },
                        showDivider = index < minOf(stations.size, 8) - 1,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickMixCard(item: ShelfItem, glass: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(if (glass) 18.dp else 12.dp)
    val cardKey = item.browseId ?: item.title
    val cardToken = remember(cardKey) { Any() }
    val onCardClick = rememberCoverTap(cardKey, cardToken, onClick)
    Box(
        modifier.aspectRatio(1f).clip(shape)
            .coverSharedElement(cardKey, shape, cardToken)
            .background(Brush.linearGradient(quickMixGradient(item)))
            .then(if (glass) Modifier else Modifier)
            .combinedClickable(onClick = onCardClick)
    ) {
        item.thumbnailUrl?.let { url ->
            AsyncImage(
                model = url.artworkAt(48), contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.5f; scaleY = 1.5f }.blur(24.dp),
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
        }
        Box(Modifier.fillMaxSize().clip(shape).background(Color.White.copy(alpha = if (glass) .22f else .14f)))
        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            Icon(
                painter = painterResource(R.drawable.ic_soundchord_logo),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(54.dp, 37.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(item.title, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MixCarouselCard(item: ShelfItem, glass: Boolean, darkContainer: Boolean, onClick: () -> Unit) {
    val cardShape = RoundedCornerShape(if (glass) 20.dp else 12.dp)
    val cardKey = item.browseId ?: item.title
    val cardToken = remember(cardKey) { Any() }
    val onCardClick = rememberCoverTap(cardKey, cardToken, onClick)
    Column(
        Modifier.width(if (darkContainer) 280.dp else 150.dp)
            .clip(cardShape)
            .combinedClickable(onClick = onCardClick),
    ) {
        AsyncImage(
            model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .coverSharedElement(cardKey, RoundedCornerShape(12.dp), cardToken),
        )
        Column(
            Modifier.fillMaxWidth()
                .then(if (darkContainer) Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (glass) 0.58f else 1f)) else Modifier)
                .padding(horizontal = if (darkContainer) 11.dp else 0.dp, vertical = 9.dp),
        ) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun String.toQuickMixTitle(): String = when (this) {
    "my supermix" -> "My Supermix"
    "discover mix" -> "Discover Mix"
    "new release mix" -> "New Release Mix"
    "replay mix" -> "Replay Mix"
    "archive mix" -> "Archive Mix"
    else -> "Liked Music"
}

private data class PersonalizedMoodMix(val name: String, val items: List<ShelfItem>)
private data class PersonalizedMixGroups(val yours: List<ShelfItem>, val moods: List<PersonalizedMoodMix>)

private fun groupPersonalizedMixes(mixes: List<KeyedMix>): PersonalizedMixGroups {
    val numberedMix = Regex("^(.+?)\\s*(?:mix|mix de|mix para|mix de los|mix of)?\\s*(\\d+)$", RegexOption.IGNORE_CASE)
    val supermix = Regex("^(.+?)\\s+(?:supermix|mix de descubrimiento|mix de novedades|mix de archivo|mix de temas escuchados)$", RegexOption.IGNORE_CASE)
    val yours = mutableListOf<ShelfItem>()
    val moods = LinkedHashMap<String, Pair<String, MutableList<Pair<Int, ShelfItem>>>>()
    mixes.distinctBy { it.item.browseId ?: it.item.title }.forEach { keyed ->
        val title = keyed.item.title.trim()
        val canonical = keyed.englishTitle.trim().ifBlank { title }
        val lower = canonical.lowercase()
        val localizedTitle = title.lowercase()
        val signatureByTitle = lower in QUICK_MIX_NAMES || SIGNATURE_MIX_TITLE_HINTS.any { hints ->
            hints.any { hint -> lower == hint || localizedTitle == hint ||
                (hint.contains("discover") && (lower.contains("discover") || localizedTitle.contains("descubr"))) ||
                (hint.contains("archive") && (lower.contains("archive") || localizedTitle.contains("archivo"))) ||
                (hint.contains("liked") && (lower.contains("liked") || localizedTitle.contains("gusta")))
            }
        }
        if (signatureByTitle || keyed.item.browseId == "VLLM" || keyed.item.browseId == "LM" ||
            keyed.item.browseId?.startsWith("RD") == true && localizedTitle.contains("discover")
        ) return@forEach
        val superMatch = supermix.matchEntire(canonical) ?: supermix.matchEntire(title)
        val numbered = numberedMix.matchEntire(canonical) ?: numberedMix.matchEntire(title)
        when {
            superMatch != null -> {
                val moodTitle = title
                val mood = moodNameFromTitle(moodTitle, superMatch.groupValues[1].trim())
                moods.getOrPut(mood.lowercase()) { mood to mutableListOf() }.second += 0 to keyed.item
            }
            numbered != null && numbered.groupValues[1].trim().lowercase() !in setOf("my", "mi", "mis") -> {
                val mood = moodNameFromTitle(title, numbered.groupValues[1].trim().removeSuffix("-").trim())
                moods.getOrPut(mood.lowercase()) { mood to mutableListOf() }.second += numbered.groupValues[2].toInt() to keyed.item
            }
            else -> yours += keyed.item
        }
    }
    return PersonalizedMixGroups(
        yours = yours,
        moods = moods.values.map { (name, members) ->
            val ordered = members.sortedBy { it.first }.map { it.second }
            val heading = if (name.equals("My", true)) ordered.firstOrNull()?.title ?: name else {
                name
            }
            PersonalizedMoodMix(heading, ordered)
        },
    )
}

private fun moodNameFromTitle(shownTitle: String, fallback: String): String {
    val title = shownTitle.trim()
    val numbered = Regex("\\s*(?:mix\\s*)?\\d+$", RegexOption.IGNORE_CASE)
    val withoutNumber = title.replace(numbered, "").trim().trimEnd('-', '–', ':')
    if (withoutNumber.isNotBlank()) return withoutNumber
    val supermix = Regex("\\s+supermix$", RegexOption.IGNORE_CASE)
    return title.replace(supermix, "").trim().ifBlank { fallback }
}

private fun quickMixGradient(item: ShelfItem): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFF77766F), Color(0xFF454550)),
        listOf(Color(0xFF262589), Color(0xFF252D50)),
        listOf(Color(0xFFB76E48), Color(0xFF713A55)),
        listOf(Color(0xFF96717D), Color(0xFF604F59)),
        listOf(Color(0xFF155545), Color(0xFF172F2A)),
        listOf(Color(0xFFA56BCB), Color(0xFFC66EAA)),
    )
    val index = QUICK_MIX_NAMES.indexOfFirst { name ->
        item.title.equals(name.toQuickMixTitle(), ignoreCase = true) ||
            (name == "liked music" && (item.browseId == "LM" || item.browseId == "VLLM"))
    }.coerceAtLeast(0)
    return palettes[index]
}

@Composable
private fun RadioSubpageTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
    )
}

@Composable
private fun SectionTitle(title: String, prominent: Boolean = false, modifier: Modifier = Modifier) {
    Text(
        title,
        modifier = modifier,
        style = if (prominent) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun CategoryGridRow(categories: List<CategoryOption>, onClick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        categories.forEachIndexed { index, category ->
            val title = stringResource(category.label)
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "category_card_scale",
            )
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(148.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onClick(index) },
                    ),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Column(
                    Modifier.fillMaxSize().background(Brush.linearGradient(category.colors)).padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.radio_stations_for, title), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .86f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (categories.size == 1 && index == 0) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun StationRow(
    station: RadioStation,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onClick: () -> Unit,
    onLongPress: (Rect) -> Unit,
    showDivider: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var artworkFailed by remember(station.uuid) { mutableStateOf(false) }
    var rowBounds by remember(station.uuid) { mutableStateOf<Rect?>(null) }
    val haptics = LocalHapticFeedback.current
    Column(modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            shape = RoundedCornerShape(18.dp),
            color = if (isPlaying) Color.White.copy(alpha = .16f) else Color.Transparent,
            border = if (isPlaying) BorderStroke(1.dp, Color.White) else null,
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .onGloballyPositioned { rowBounds = it.boundsInWindow() }
                        .combinedClickable(
                            onClick = onClick,
                            onLongClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                rowBounds?.let(onLongPress)
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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
                    Column {
                        Text(station.name, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(stationMetadata(station), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (isPlaying) Text(stringResource(R.string.radio_now_playing), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onFavorite) {
                    Icon(if (isFavorite) BitChordIcons.StarFilled else BitChordIcons.Star, stringResource(if (isFavorite) R.string.radio_remove_favorite else R.string.radio_add_favorite), tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 76.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline,
            )
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
