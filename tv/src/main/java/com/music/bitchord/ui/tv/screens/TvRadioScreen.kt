package com.music.bitchord.ui.tv.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.session.MediaController
import com.music.bitchord.R
import com.music.bitchord.data.model.PlaybackSourceType
import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.beginRadioQueue
import com.music.bitchord.playback.commitRadioQueue
import com.music.bitchord.playback.playSongs
import com.music.bitchord.ui.tv.components.TvArtwork
import com.music.bitchord.ui.tv.components.TvButton
import com.music.bitchord.ui.tv.components.TvChromeScrollEffect
import com.music.bitchord.ui.tv.components.TvListRow
import com.music.bitchord.ui.tv.components.TvLockup
import com.music.bitchord.ui.tv.components.TvShelf
import com.music.bitchord.ui.tv.components.TvShelfTitle
import com.music.bitchord.ui.tv.components.tvClick
import com.music.bitchord.ui.tv.components.tvInitialFocus
import com.music.bitchord.ui.tv.components.tvLift
import com.music.bitchord.ui.tv.theme.TvDimensions
import com.music.bitchord.ui.tv.theme.TvGlass
import com.music.bitchord.ui.tv.theme.TvType
import com.soundchord.features.radio.data.RadioStation
import com.soundchord.features.radio.data.RadioTag
import com.soundchord.features.radio.playback.toSong
import com.soundchord.features.radio.ui.NearbyStationsState
import com.soundchord.features.radio.ui.PersonalStationQueueState
import com.soundchord.features.radio.ui.PersonalStationsViewModel
import com.soundchord.features.radio.ui.RecentStationsState
import com.soundchord.features.radio.ui.RadioUiState
import com.soundchord.features.radio.ui.RadioViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private data class RadioCountry(val code: String, val label: Int)

private val radioCountries = listOf(
    RadioCountry("EC", com.soundchord.features.R.string.radio_country_ec),
    RadioCountry("US", com.soundchord.features.R.string.radio_country_us),
    RadioCountry("MX", com.soundchord.features.R.string.radio_country_mx),
    RadioCountry("CO", com.soundchord.features.R.string.radio_country_co),
    RadioCountry("AR", com.soundchord.features.R.string.radio_country_ar),
    RadioCountry("ES", com.soundchord.features.R.string.radio_country_es),
    RadioCountry("BR", com.soundchord.features.R.string.radio_country_br),
    RadioCountry("GB", com.soundchord.features.R.string.radio_country_gb),
    RadioCountry("FR", com.soundchord.features.R.string.radio_country_fr),
    RadioCountry("DE", com.soundchord.features.R.string.radio_country_de),
    RadioCountry("IT", com.soundchord.features.R.string.radio_country_it),
    RadioCountry("JP", com.soundchord.features.R.string.radio_country_jp),
    RadioCountry("IN", com.soundchord.features.R.string.radio_country_in),
)

@Composable
fun TvRadioScreen(
    mediaController: MediaController?,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    radioViewModel: RadioViewModel? = null,
) {
    val context = LocalContext.current
    val vm: RadioViewModel = radioViewModel ?: viewModel(
        factory = remember(context) { RadioViewModel.Factory(context) },
    )
    val stationsState by vm.uiState.collectAsState()
    val favorites by vm.favoriteStations.collectAsState()
    val genres by vm.popularGenres.collectAsState()
    val nearby by vm.nearbyStations.collectAsState()
    val nearbyState by vm.nearbyStationsState.collectAsState()
    val country by vm.countryCode.collectAsState()
    val genre by vm.genre.collectAsState()
    val playing by vm.currentlyPlayingStation.collectAsState()
    val personalStationsViewModel: PersonalStationsViewModel = viewModel()
    val personalStations by personalStationsViewModel.recentStations.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedCategoryTitle by remember { mutableStateOf<String?>(null) }
    var selectedPersonalSeed by remember { mutableStateOf<Song?>(null) }
    val favoritesLabel = stringResource(com.soundchord.features.R.string.radio_favourites)
    val popularLabel = stringResource(R.string.tv_radio_popular)
    val nearbyLabel = stringResource(com.soundchord.features.R.string.radio_nearby)
    val genresLabel = stringResource(com.soundchord.features.R.string.radio_genres)
    val countriesLabel = stringResource(com.soundchord.features.R.string.radio_countries)
    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) vm.loadNearbyStations(context) else vm.onNearbyPermissionDenied()
    }

    LaunchedEffect(Unit) {
        personalStationsViewModel.loadRecentStations()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            vm.loadNearbyStations(context)
        } else {
            vm.onNearbyPermissionDenied()
        }
        vm.fetchTopStations()
    }

    LaunchedEffect(selectedPersonalSeed) {
        val seed = selectedPersonalSeed ?: return@LaunchedEffect
        personalStationsViewModel.loadStation(seed)
        val queueState = personalStationsViewModel.stationQueue.first {
            it !is PersonalStationQueueState.Loading
        }
        if (queueState is PersonalStationQueueState.Success && queueState.songs.isNotEmpty()) {
            val seedForQueue = seed.copy(
                radioName = seed.title,
                playbackSource = seed.title,
                playbackSourceType = PlaybackSourceType.SHARED_LINK,
                playbackSourceId = seed.videoId,
                isLiveRadioStream = false,
            )
            val queue = (listOf(seedForQueue) + queueState.songs).map { song ->
                song.copy(
                    radioName = seed.title,
                    playbackSource = seed.title,
                    playbackSourceType = PlaybackSourceType.SHARED_LINK,
                    playbackSourceId = seed.videoId,
                    isLiveRadioStream = false,
                )
            }
            mediaController?.beginRadioQueue()
            mediaController?.playSongs(queue, 0)
            mediaController?.commitRadioQueue()
            if (mediaController != null) onNavigateToNowPlaying()
        }
        selectedPersonalSeed = null
    }

    BackHandler(enabled = selectedCategoryTitle != null) {
        vm.onGenreChanged(null)
        vm.onCountryCodeChanged(null)
        selectedCategoryTitle = null
    }

    val playStation: (RadioStation) -> Unit = { station ->
        vm.setCurrentlyPlayingStation(station)
        val radioTrack = station.toSong(context)
        scope.launch { mediaController?.playSongs(listOf(radioTrack), 0) }
        onNavigateToNowPlaying()
    }

    val visibleStations = (stationsState as? RadioUiState.Success)?.stations.orEmpty()
    val selectedTitle = when {
        genre != null -> genre.orEmpty()
        country != null -> radioCountries.firstOrNull { it.code == country }?.let { context.getString(it.label) }.orEmpty()
        else -> ""
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    TvChromeScrollEffect(listState)
    LaunchedEffect(selectedCategoryTitle) {
        if (selectedCategoryTitle != null) listState.scrollToItem(0)
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = TvDimensions.ContentTop, bottom = 45.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        if (selectedCategoryTitle != null) {
            item(key = "radio_category_header") {
                Column(Modifier.fillMaxWidth().padding(horizontal = TvDimensions.SafeMarginHorizontal)) {
                    TvButton(
                        text = stringResource(com.soundchord.features.R.string.radio_back),
                        onClick = {
                            vm.onGenreChanged(null)
                            vm.onCountryCodeChanged(null)
                            selectedCategoryTitle = null
                        },
                        modifier = Modifier.tvInitialFocus(),
                    )
                    TvShelfTitle(title = selectedCategoryTitle.orEmpty(), modifier = Modifier.padding(top = 18.dp))
                }
            }
            when (stationsState) {
                RadioUiState.Loading -> item(key = "radio_category_loading") {
                    TvRadioMessage(stringResource(R.string.tv_radio_loading))
                }
                is RadioUiState.Error -> item(key = "radio_category_error") {
                    TvRadioMessage((stationsState as RadioUiState.Error).message)
                }
                is RadioUiState.Success -> if (visibleStations.isEmpty()) {
                    item(key = "radio_category_empty") {
                        TvRadioMessage(stringResource(com.soundchord.features.R.string.radio_category_empty))
                    }
                } else items(visibleStations, key = { "radio_category_${it.uuid}" }) { station ->
                    TvRadioStationListRow(
                        station = station,
                        isFavorite = vm.isFavorite(station.uuid),
                        onFavorite = { vm.toggleFavorite(station) },
                        onPlay = { playStation(station) },
                        modifier = Modifier.padding(horizontal = TvDimensions.SafeMarginHorizontal),
                        initialFocus = station == visibleStations.firstOrNull(),
                    )
                }
            }
        } else {
            if (favorites.isNotEmpty()) {
                item(key = "radio_favorites") {
                    RadioStationShelf(
                        favoritesLabel, favorites, playing?.uuid, playStation,
                        favoriteUuids = favorites.mapTo(hashSetOf()) { it.uuid },
                        onFavorite = vm::toggleFavorite,
                    )
                }
            }

            item(key = "radio_personal_stations") {
                when (val state = personalStations) {
                    RecentStationsState.Loading -> Column {
                        TvShelfTitle(title = stringResource(com.soundchord.features.R.string.radio_personal_stations))
                        TvRadioMessage(stringResource(R.string.tv_radio_loading))
                    }
                    RecentStationsState.Error -> Column {
                        TvShelfTitle(title = stringResource(com.soundchord.features.R.string.radio_personal_stations))
                        TvRadioMessage(stringResource(com.soundchord.features.R.string.radio_personal_station_empty))
                    }
                    is RecentStationsState.Success -> if (state.songs.isEmpty()) {
                        Column {
                            TvShelfTitle(title = stringResource(com.soundchord.features.R.string.radio_personal_stations))
                            TvRadioMessage(stringResource(com.soundchord.features.R.string.radio_personal_station_empty))
                        }
                    } else {
                        TvShelf(
                            title = stringResource(com.soundchord.features.R.string.radio_personal_stations),
                            items = state.songs,
                            key = { it.videoId },
                        ) { seed ->
                            TvCustomStationCard(
                                seed = seed,
                                initialFocus = state.songs.firstOrNull() == seed,
                                onPlay = { selectedPersonalSeed = seed },
                            )
                        }
                    }
                }
            }

            item(key = "radio_nearby") {
                when (nearbyState) {
                    NearbyStationsState.Ready -> if (nearby.isNotEmpty()) {
                        RadioStationShelf(
                            nearbyLabel,
                            nearby,
                            playing?.uuid,
                            playStation,
                            initialFocus = (personalStations as? RecentStationsState.Success)?.songs.isNullOrEmpty(),
                            favoriteUuids = favorites.mapTo(hashSetOf()) { it.uuid },
                            onFavorite = vm::toggleFavorite,
                        )
                    } else {
                        Column {
                            TvShelfTitle(title = nearbyLabel)
                            TvRadioMessage(stringResource(com.soundchord.features.R.string.radio_no_local_stations))
                        }
                    }
                    NearbyStationsState.PermissionRequired -> Column(Modifier.padding(horizontal = TvDimensions.SafeMarginHorizontal)) {
                        TvShelfTitle(title = nearbyLabel, modifier = Modifier.padding(bottom = 9.dp))
                        Text(
                            stringResource(com.soundchord.features.R.string.radio_location_permission_needed),
                            color = TvGlass.TextSecondary,
                        )
                        TvButton(
                            text = stringResource(com.soundchord.features.R.string.radio_grant_location),
                            onClick = { locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                    NearbyStationsState.LocationUnavailable -> Column {
                        TvShelfTitle(title = nearbyLabel)
                        TvRadioMessage(stringResource(com.soundchord.features.R.string.radio_no_local_stations))
                    }
                    NearbyStationsState.Loading -> Column {
                        TvShelfTitle(title = nearbyLabel)
                        TvRadioMessage(stringResource(R.string.tv_radio_loading))
                    }
                    NearbyStationsState.Error -> Column {
                        TvShelfTitle(title = nearbyLabel)
                        TvRadioMessage(stringResource(R.string.tv_radio_nearby_error))
                    }
                }
            }

            item(key = "radio_popular") {
                when (stationsState) {
                    RadioUiState.Loading -> TvRadioMessage(stringResource(R.string.tv_radio_loading))
                    is RadioUiState.Error -> TvRadioMessage((stationsState as RadioUiState.Error).message)
                    is RadioUiState.Success -> if (visibleStations.isEmpty()) {
                        TvRadioMessage(stringResource(com.soundchord.features.R.string.radio_search_empty))
                    } else {
                        RadioStationShelf(
                            title = if (selectedTitle.isBlank()) popularLabel else stringResource(R.string.tv_radio_stations_for, selectedTitle),
                            stations = visibleStations,
                            playingUuid = playing?.uuid,
                            onPlay = playStation,
                            favoriteUuids = favorites.mapTo(hashSetOf()) { it.uuid },
                            onFavorite = vm::toggleFavorite,
                        )
                    }
                }
            }

            item(key = "radio_genres_grid") {
                RadioTagCarousel(
                    title = genresLabel,
                    genres = genres,
                    selected = genre,
                    onSelect = { tag ->
                        vm.onGenreChanged(tag.name)
                        vm.onCountryCodeChanged(null)
                        selectedCategoryTitle = tag.name
                    },
                )
            }

            item(key = "radio_countries_grid") {
                RadioCountryCarousel(
                    title = countriesLabel,
                    selectedCode = country,
                    onSelect = { selected ->
                        vm.onCountryCodeChanged(selected.code)
                        vm.onGenreChanged(null)
                        selectedCategoryTitle = context.getString(selected.label)
                    },
                )
            }
        }
    }
}

@Composable
private fun RadioStationShelf(
    title: String,
    stations: List<RadioStation>,
    playingUuid: String?,
    onPlay: (RadioStation) -> Unit,
    initialFocus: Boolean = false,
    favoriteUuids: Set<String> = emptySet(),
    onFavorite: (RadioStation) -> Unit = {},
) {
    TvShelf(title = title, items = stations, key = { it.uuid }) { station ->
        TvLockup(
            title = station.name,
            subtitle = listOfNotNull(station.country, station.codec?.uppercase()?.takeIf(String::isNotBlank))
                .joinToString(" · ").ifBlank { station.tags.orEmpty() },
            artworkUrl = station.favicon,
            width = 188.dp,
            aspectRatio = 16f / 9f,
            onClick = { onPlay(station) },
            modifier = Modifier.tvInitialFocus(initialFocus && station == stations.firstOrNull()),
            overlay = {
                if (station.uuid == playingUuid) {
                    Text(stringResource(com.soundchord.features.R.string.radio_live), modifier = Modifier.align(Alignment.TopStart).padding(8.dp), color = androidx.compose.ui.graphics.Color.White)
                }
                IconButton(
                    onClick = { onFavorite(station) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                ) {
                    Icon(
                        imageVector = if (station.uuid in favoriteUuids) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = stringResource(if (station.uuid in favoriteUuids) com.soundchord.features.R.string.radio_remove_favorite else com.soundchord.features.R.string.radio_add_favorite),
                        tint = if (station.uuid in favoriteUuids) Color(0xFFFFD54F) else Color.White,
                    )
                }
            },
        )
    }
}

@Composable
internal fun TvRadioStationListRow(
    station: RadioStation,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    initialFocus: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TvArtwork(
            url = station.favicon,
            glyph = Icons.Rounded.Radio,
            modifier = Modifier.size(46.dp),
            shape = RoundedCornerShape(10.dp),
        )
        TvListRow(
            title = station.name,
            subtitle = listOfNotNull(station.country, station.codec?.uppercase(), station.tags)
                .filter(String::isNotBlank).joinToString(" · "),
            value = station.bitrate.takeIf { it > 0 }?.let {
                stringResource(com.soundchord.features.R.string.radio_bitrate_format, it)
            },
            modifier = Modifier.weight(1f).tvInitialFocus(initialFocus),
            onClick = onPlay,
        )
        IconButton(onClick = onFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = stringResource(if (isFavorite) com.soundchord.features.R.string.radio_remove_favorite else com.soundchord.features.R.string.radio_add_favorite),
                tint = if (isFavorite) Color(0xFFFFD54F) else TvGlass.TextSecondary,
            )
        }
    }
}

@Composable
private fun TvCustomStationCard(
    seed: Song,
    initialFocus: Boolean,
    onPlay: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .width(254.dp)
            .height(318.dp)
            .tvLift(interaction, shape, focusedScale = 1.045f, elevation = 18.dp)
            .tvClick(interaction, onClick = onPlay)
            .tvInitialFocus(initialFocus),
    ) {
        TvArtwork(url = seed.thumbnailUrl, modifier = Modifier.fillMaxSize(), shape = shape)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.42f),
                    0.42f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.88f),
                ),
            ),
        )
        Text(
            text = stringResource(com.soundchord.features.R.string.station_custom_label).uppercase(),
            style = TvType.Caption.copy(fontWeight = FontWeight.Bold),
            color = Color.White.copy(alpha = 0.94f),
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 17.dp, end = 64.dp, bottom = 19.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = stringResource(com.soundchord.features.R.string.station_title_format, seed.title),
                style = TvType.Title.copy(fontSize = 20.sp),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = seed.artist,
                style = TvType.Body,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier.align(Alignment.BottomEnd).padding(13.dp).size(44.dp)
                .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.58f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.play), tint = Color.White, modifier = Modifier.size(30.dp))
        }
    }
}

private val radioCategoryGradients = listOf(
    listOf(Color(0xFFFF4D7D), Color(0xFFB90055)),
    listOf(Color(0xFFFF6B35), Color(0xFF9E251E)),
    listOf(Color(0xFF9B59FF), Color(0xFF4B2AA8)),
    listOf(Color(0xFF418BFF), Color(0xFF263D9B)),
    listOf(Color(0xFFEF3DDB), Color(0xFF7028D9)),
    listOf(Color(0xFF26C6C8), Color(0xFF176B9A)),
    listOf(Color(0xFF18B957), Color(0xFF08763D)),
    listOf(Color(0xFFFFD329), Color(0xFF2457C5)),
    listOf(Color(0xFFE34035), Color(0xFFE7B52D)),
)

@Composable
private fun RadioTagCarousel(
    title: String,
    genres: List<RadioTag>,
    selected: String?,
    onSelect: (RadioTag) -> Unit,
) {
    if (genres.isEmpty()) {
        TvShelfTitle(title = title)
        TvRadioMessage(stringResource(com.soundchord.features.R.string.radio_genres_empty))
        return
    }
    TvShelf(title = title, items = genres, key = { "radio_genre_${it.name}" }) { tag ->
        val titleText = tag.name.replaceFirstChar { it.uppercase() }
        RadioCategoryCard(
            title = titleText,
            subtitle = stringResource(com.soundchord.features.R.string.radio_stations_for, titleText),
            gradient = radioCategoryGradients[(genres.indexOf(tag)) % radioCategoryGradients.size],
            selected = selected.equals(tag.name, ignoreCase = true),
            modifier = Modifier.width(226.dp),
            onClick = { onSelect(tag) },
        )
    }
}

@Composable
private fun RadioCountryCarousel(
    title: String,
    selectedCode: String?,
    onSelect: (RadioCountry) -> Unit,
) {
    TvShelf(title = title, items = radioCountries, key = { "radio_country_${it.code}" }) { country ->
        val countryName = stringResource(country.label)
        RadioCategoryCard(
            title = countryName,
            subtitle = stringResource(com.soundchord.features.R.string.radio_stations_for, countryName),
            gradient = radioCategoryGradients[(radioCountries.indexOf(country) + 2) % radioCategoryGradients.size],
            selected = selectedCode == country.code,
            modifier = Modifier.width(226.dp),
            onClick = { onSelect(country) },
        )
    }
}

@Composable
private fun RadioCategoryCard(
    title: String,
    subtitle: String,
    gradient: List<Color>,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier.height(150.dp)
            .tvLift(interaction, shape, focusedScale = 1.035f, elevation = 15.dp)
            .clip(shape)
            .background(Brush.linearGradient(gradient))
            .border(if (selected) 2.dp else 0.dp, Color.White.copy(alpha = 0.9f), shape)
            .tvClick(interaction, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            title,
            style = TvType.Body.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            subtitle,
            style = TvType.Caption.copy(fontSize = 12.sp),
            color = Color.White.copy(alpha = 0.84f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TvRadioMessage(message: String) {
    Text(
        text = message,
        color = com.music.bitchord.ui.tv.theme.TvGlass.TextSecondary,
        textAlign = TextAlign.Start,
        modifier = Modifier.padding(horizontal = TvDimensions.SafeMarginHorizontal, vertical = 8.dp),
    )
}
