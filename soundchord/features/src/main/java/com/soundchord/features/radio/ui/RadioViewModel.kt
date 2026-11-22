package com.soundchord.features.radio.ui

import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soundchord.features.radio.data.PreferencesRadioFavoritesStore
import com.soundchord.features.radio.data.RadioBrowserRepository
import com.soundchord.features.radio.data.RadioRepository
import com.soundchord.features.radio.data.RadioStation
import com.soundchord.features.radio.data.RadioTag
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.KeyedMix
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

sealed interface RadioUiState {
    data object Loading : RadioUiState
    data class Success(val stations: List<RadioStation>) : RadioUiState
    data class Error(val message: String) : RadioUiState
}

enum class NearbyStationsState { PermissionRequired, LocationUnavailable, Loading, Ready, Error }

class RadioViewModel(
    private val repository: RadioRepository = RadioBrowserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RadioUiState>(RadioUiState.Loading)
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _countryCode = MutableStateFlow<String?>(null)
    val countryCode: StateFlow<String?> = _countryCode.asStateFlow()

    private val _genre = MutableStateFlow<String?>(null)
    val genre: StateFlow<String?> = _genre.asStateFlow()

    private val _language = MutableStateFlow<String?>(null)
    val language: StateFlow<String?> = _language.asStateFlow()

    private val _showFavorites = MutableStateFlow(false)
    val showFavorites: StateFlow<Boolean> = _showFavorites.asStateFlow()

    private val _popularGenres = MutableStateFlow<List<RadioTag>>(emptyList())
    val popularGenres: StateFlow<List<RadioTag>> = _popularGenres.asStateFlow()

    private val _favoriteStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val favoriteStations: StateFlow<List<RadioStation>> = _favoriteStations.asStateFlow()
    val favoriteStationUuids = favoriteStations.map { stations -> stations.mapTo(hashSetOf()) { it.uuid } }

    private val _currentlyPlayingStation = MutableStateFlow<RadioStation?>(null)
    val currentlyPlayingStation: StateFlow<RadioStation?> = _currentlyPlayingStation.asStateFlow()

    private val _personalizedMixes = MutableStateFlow<com.music.bitchord.data.model.UiState<List<KeyedMix>>>(
        com.music.bitchord.data.model.UiState.Loading,
    )
    val personalizedMixes: StateFlow<com.music.bitchord.data.model.UiState<List<KeyedMix>>> = _personalizedMixes.asStateFlow()
    private var mixesLoaded = false
    private var mixesJob: Job? = null
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _nearbyStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val nearbyStations: StateFlow<List<RadioStation>> = _nearbyStations.asStateFlow()
    private val _nearbyStationsState = MutableStateFlow(NearbyStationsState.PermissionRequired)
    val nearbyStationsState: StateFlow<NearbyStationsState> = _nearbyStationsState.asStateFlow()

    private var requestJob: Job? = null
    private val favoriteMutation = Mutex()

    init {
        viewModelScope.launch {
            repository.favoriteStations.collect { stations ->
                favoriteMutation.withLock { _favoriteStations.value = stations }
            }
        }
        observeFilters()
        loadPopularGenres()
    }

    fun fetchTopStations() {
        _searchQuery.value = ""
        _countryCode.value = null
        _genre.value = null
        _language.value = null
    }

    fun fetchStationsByCountry(countryCode: String) {
        _countryCode.value = countryCode.trim().takeIf(String::isNotEmpty)
    }

    fun onCountryCodeChanged(countryCode: String?) {
        _countryCode.value = countryCode?.trim()?.takeIf(String::isNotEmpty)
    }

    fun onGenreChanged(genre: String?) {
        _genre.value = genre?.trim()?.takeIf(String::isNotEmpty)
    }

    fun onLanguageChanged(language: String?) {
        _language.value = language?.trim()?.takeIf(String::isNotEmpty)
    }

    fun showFavorites(show: Boolean) {
        _showFavorites.value = show
        if (show) requestJob?.cancel() else loadStations(
            _searchQuery.value, _countryCode.value, _genre.value, _language.value,
        )
    }

    fun setCurrentlyPlayingStation(station: RadioStation?) {
        _currentlyPlayingStation.value = station
    }

    fun loadPersonalizedMixes(force: Boolean = false) {
        if (!force && (mixesLoaded || _personalizedMixes.value is com.music.bitchord.data.model.UiState.Success || mixesJob?.isActive == true)) return
        mixesJob?.cancel()
        mixesJob = viewModelScope.launch {
            _personalizedMixes.value = com.music.bitchord.data.model.UiState.Loading
            YtMusicRepository.mixedForYouKeyed()
                .onSuccess {
                    mixesLoaded = true
                    val withLiked = if (it.any { mix -> mix.item.browseId == "VLLM" || mix.item.browseId == "LM" }) it else {
                        it + KeyedMix(
                            item = com.music.bitchord.data.model.ShelfItem(
                                title = "Liked music", subtitle = "Your liked songs", thumbnailUrl = null,
                                videoId = null, browseId = YtMusicRepository.LIKED_MUSIC,
                            ),
                            englishTitle = "Liked music",
                        )
                    }
                    _personalizedMixes.value = com.music.bitchord.data.model.UiState.Success(withLiked)
                }
                .onFailure {
                    _personalizedMixes.value = com.music.bitchord.data.model.UiState.Error(
                        it.localizedMessage ?: "No se pudieron cargar tus mixes",
                    )
                }
        }
    }

    fun refreshRadioData(context: Context, locationAllowed: Boolean) {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        mixesLoaded = false
        mixesJob?.cancel()
        viewModelScope.launch {
            try {
                val filters = RadioFilters(_searchQuery.value, _countryCode.value, _genre.value, _language.value)
                val jobs = buildList {
                    add(launch {
                        YtMusicRepository.mixedForYouKeyed().onSuccess { mixes ->
                            mixesLoaded = true
                            val withLiked = if (mixes.any { it.item.browseId == "VLLM" || it.item.browseId == "LM" }) mixes else mixes +
                                KeyedMix(
                                    com.music.bitchord.data.model.ShelfItem("Liked music", "Your liked songs", null, null, YtMusicRepository.LIKED_MUSIC),
                                    "Liked music",
                                )
                            _personalizedMixes.value = com.music.bitchord.data.model.UiState.Success(withLiked)
                        }.onFailure { _personalizedMixes.value = com.music.bitchord.data.model.UiState.Error(it.localizedMessage ?: "No se pudieron cargar tus mixes") }
                    })
                    add(launch {
                        if (!_showFavorites.value) {
                            _uiState.value = RadioUiState.Loading
                            runCatching {
                                repository.getStations(filters.query, filters.countryCode, filters.genre, filters.language, 60)
                            }.onSuccess { _uiState.value = RadioUiState.Success(it) }
                                .onFailure { _uiState.value = RadioUiState.Error(it.localizedMessage ?: "No se pudieron cargar las emisoras") }
                        }
                    })
                    if (locationAllowed) add(launch { loadNearbyStationsAndWait(context) })
                    add(launch { loadPopularGenresAndWait() })
                }
                jobs.forEach { it.join() }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun loadPopularGenresAndWait() {
        runCatching { repository.getPopularTags(limit = 30) }.onSuccess { tags ->
            _popularGenres.value = tags.filter { it.name.isNotBlank() && it.stationCount > 0 }
                .distinctBy { it.name.trim().lowercase() }.sortedByDescending { it.stationCount }
        }
    }

    private suspend fun loadNearbyStationsAndWait(context: Context) {
        _nearbyStationsState.value = NearbyStationsState.Loading
        try {
            val countryCode = withContext(Dispatchers.IO) {
                val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val location = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
                    .firstNotNullOfOrNull { provider -> runCatching {
                        if (manager.isProviderEnabled(provider)) manager.getLastKnownLocation(provider) else null
                    }.getOrNull() } ?: return@withContext null
                if (!Geocoder.isPresent()) return@withContext null
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()?.countryCode
            }?.takeIf(String::isNotBlank)
            if (countryCode == null) {
                _nearbyStationsState.value = NearbyStationsState.LocationUnavailable
            } else {
                _nearbyStations.value = repository.getStations("", countryCode, null, null, limit = 20)
                _nearbyStationsState.value = NearbyStationsState.Ready
            }
        } catch (_: Throwable) {
            _nearbyStationsState.value = NearbyStationsState.Error
        }
    }

    fun isFavorite(stationUuid: String): Boolean =
        _favoriteStations.value.any { it.uuid == stationUuid }

    fun toggleFavorite(station: RadioStation) {
        viewModelScope.launch {
            favoriteMutation.withLock {
                val previous = _favoriteStations.value
                val wasFavorite = previous.any { it.uuid == station.uuid }
                _favoriteStations.value = if (wasFavorite) {
                    previous.filterNot { it.uuid == station.uuid }
                } else {
                    previous.filterNot { it.uuid == station.uuid } + station
                }
                runCatching {
                    if (wasFavorite) repository.removeFavorite(station.uuid)
                    else repository.addFavorite(station)
                }.onFailure {
                    _favoriteStations.value = previous
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private fun loadPopularGenres() {
        viewModelScope.launch {
            runCatching { repository.getPopularTags(limit = 30) }
                .onSuccess { tags ->
                    _popularGenres.value = tags
                        .filter { it.name.isNotBlank() && it.stationCount > 0 }
                        .distinctBy { it.name.trim().lowercase() }
                        .sortedByDescending { it.stationCount }
                }
        }
    }

    fun onNearbyPermissionDenied() {
        _nearbyStationsState.value = NearbyStationsState.PermissionRequired
    }

    fun loadNearbyStations(context: Context, force: Boolean = false) {
        if (!force && (_nearbyStationsState.value == NearbyStationsState.Ready ||
                _nearbyStationsState.value == NearbyStationsState.Loading)) return
        viewModelScope.launch {
            _nearbyStationsState.value = NearbyStationsState.Loading
            runCatching {
                val countryCode = withContext(Dispatchers.IO) {
                    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val location = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
                        .firstNotNullOfOrNull { provider ->
                            runCatching {
                                if (manager.isProviderEnabled(provider)) manager.getLastKnownLocation(provider) else null
                            }.getOrNull()
                        } ?: return@withContext null
                    if (!Geocoder.isPresent()) return@withContext null
                    @Suppress("DEPRECATION")
                    Geocoder(context, Locale.getDefault())
                        .getFromLocation(location.latitude, location.longitude, 1)
                        ?.firstOrNull()?.countryCode
                }?.takeIf(String::isNotBlank) ?: return@runCatching null
                repository.getStations("", countryCode, null, null, limit = 20)
            }.onSuccess { stations ->
                if (stations == null) {
                    _nearbyStationsState.value = NearbyStationsState.LocationUnavailable
                } else {
                    _nearbyStations.value = stations
                    _nearbyStationsState.value = NearbyStationsState.Ready
                }
            }.onFailure {
                _nearbyStationsState.value = NearbyStationsState.Error
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeFilters() {
        viewModelScope.launch {
            combine(
                _searchQuery.debounce(350).distinctUntilChanged(),
                _countryCode,
                _genre,
                _language,
            ) { query, country, selectedGenre, selectedLanguage ->
                RadioFilters(query, country, selectedGenre, selectedLanguage)
            }
                .collect { filters ->
                    if (!_showFavorites.value) {
                        loadStations(filters.query, filters.countryCode, filters.genre, filters.language)
                    }
                }
        }
    }

    private fun loadStations(
        query: String,
        country: String?,
        genre: String? = _genre.value,
        language: String? = _language.value,
    ) {
        if (_showFavorites.value) return
        requestJob?.cancel()
        requestJob = viewModelScope.launch {
            _uiState.value = RadioUiState.Loading
            try {
                val stations = repository.getStations(query, country, genre, language, limit = 60)
                _uiState.value = RadioUiState.Success(stations)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _uiState.value = RadioUiState.Error(
                    error.localizedMessage ?: "No se pudieron cargar las emisoras"
                )
            }
        }
    }

    private data class RadioFilters(
        val query: String,
        val countryCode: String?,
        val genre: String?,
        val language: String?,
    )

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(RadioViewModel::class.java))
            val repository = RadioBrowserRepository(
                favoritesStore = PreferencesRadioFavoritesStore(appContext),
            )
            return RadioViewModel(repository) as T
        }
    }
}
