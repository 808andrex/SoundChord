package com.soundchord.features.radio.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soundchord.features.radio.data.PreferencesRadioFavoritesStore
import com.soundchord.features.radio.data.RadioBrowserRepository
import com.soundchord.features.radio.data.RadioRepository
import com.soundchord.features.radio.data.RadioStation
import com.soundchord.features.radio.data.RadioTag
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale

sealed interface RadioUiState {
    data object Loading : RadioUiState
    data class Success(val stations: List<RadioStation>) : RadioUiState
    data class Error(val message: String) : RadioUiState
}

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

    val favoriteStations: StateFlow<List<RadioStation>> = repository.favoriteStations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _currentlyPlayingStation = MutableStateFlow<RadioStation?>(null)
    val currentlyPlayingStation: StateFlow<RadioStation?> = _currentlyPlayingStation.asStateFlow()

    private val _nearbyStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val nearbyStations: StateFlow<List<RadioStation>> = _nearbyStations.asStateFlow()

    private var requestJob: Job? = null

    init {
        observeFilters()
        loadPopularGenres()
        loadNearbyStations()
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

    fun isFavorite(stationUuid: String): Boolean =
        favoriteStations.value.any { it.uuid == stationUuid }

    fun toggleFavorite(station: RadioStation) {
        viewModelScope.launch {
            if (isFavorite(station.uuid)) repository.removeFavorite(station.uuid)
            else repository.addFavorite(station)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private fun loadPopularGenres() {
        viewModelScope.launch {
            runCatching { repository.getPopularTags(limit = 30) }
                .onSuccess { _popularGenres.value = it }
        }
    }

    private fun loadNearbyStations() {
        viewModelScope.launch {
            val countryCode = Locale.getDefault().country.takeIf(String::isNotBlank)
            runCatching { repository.getStations("", countryCode, null, null, limit = 20) }
                .onSuccess { _nearbyStations.value = it }
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
                val stations = repository.getStations(query, country, genre, language, limit = 40)
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
