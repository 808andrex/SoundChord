package com.music.bitchord.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.BrowseItem
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.settings.AppSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    data class ArtistSuggestion(val artist: BrowseItem, val relatedToArtistName: String? = null)

    private val _artists = MutableStateFlow<List<ArtistSuggestion>>(emptyList())
    val artists: StateFlow<List<ArtistSuggestion>> = _artists.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ArtistSuggestion>>(emptyList())
    val searchResults: StateFlow<List<ArtistSuggestion>> = _searchResults.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()

    private val _artistsLoading = MutableStateFlow(true)
    val artistsLoading: StateFlow<Boolean> = _artistsLoading.asStateFlow()

    private val _selected = MutableStateFlow(AppSettings.onboardingArtists.value)
    val selected: StateFlow<List<AppSettings.OnboardingArtist>> = _selected.asStateFlow()

    private val countryCode = Locale.getDefault().country
    val countryName: String = countryCode.takeIf(String::isNotBlank)
        ?.let { runCatching { Locale("", it).displayCountry }.getOrNull() }
        ?.takeIf(String::isNotBlank).orEmpty()

    private val relatedJobs = mutableMapOf<String, Job>()

    init {
        loadPopularArtists()
        observeSearch()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _searchLoading.value = query.isNotBlank()
    }

    fun toggleArtist(suggestion: ArtistSuggestion) {
        val artist = suggestion.artist
        val current = _selected.value.toMutableList()
        val existing = current.indexOfFirst { it.browseId == artist.browseId }
        if (existing >= 0) {
            current.removeAt(existing)
            relatedJobs.remove(artist.browseId)?.cancel()
        } else {
            current += AppSettings.OnboardingArtist(artist.browseId, artist.title, artist.thumbnailUrl)
            if (_artists.value.none { it.artist.browseId == artist.browseId }) {
                _artists.value = _artists.value + suggestion
            }
            loadRelatedArtists(artist)
            _searchQuery.value = ""
        }
        _selected.value = current
        AppSettings.setOnboardingArtists(current)
    }

    private fun loadPopularArtists() {
        viewModelScope.launch {
            try {
                val region = Locale.getDefault().country.takeIf(String::isNotBlank)
                val regional = region?.let { YtMusicRepository.chartArtists(it).getOrNull().orEmpty() }.orEmpty()
                val charts = regional.ifEmpty { YtMusicRepository.chartArtists(null).getOrNull().orEmpty() }
                val candidates = charts.ifEmpty {
                    searchArtistFallback("popular artists").ifEmpty { searchArtistFallback("top artists") }
                }
                if (candidates.isNotEmpty()) {
                    _artists.value = candidates.distinctBy { it.browseId }.take(60).map(::ArtistSuggestion)
                }
            } finally {
                _artistsLoading.value = false
            }
        }
    }

    private suspend fun searchArtistFallback(query: String): List<BrowseItem> =
        YtMusicRepository.search(query, SearchFilter.ARTISTS).getOrNull().orEmpty()
            .mapNotNull { (it as? com.music.bitchord.data.model.SearchResult.Browse)?.item }
            .filter { it.type == BrowseType.ARTIST }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        viewModelScope.launch {
            _searchQuery.debounce(350).distinctUntilChanged().collectLatest { text ->
                val query = text.trim()
                if (query.isEmpty()) {
                    _searchResults.value = emptyList()
                    _searchLoading.value = false
                    return@collectLatest
                }
                _searchLoading.value = true
                try {
                    _searchResults.value = YtMusicRepository.search(query, SearchFilter.ARTISTS)
                        .getOrNull().orEmpty()
                        .mapNotNull { (it as? com.music.bitchord.data.model.SearchResult.Browse)?.item }
                        .filter { it.type == BrowseType.ARTIST }
                        .distinctBy { it.browseId }
                        .map(::ArtistSuggestion)
                } finally {
                    _searchLoading.value = false
                }
            }
        }
    }

    private fun loadRelatedArtists(artist: BrowseItem) {
        relatedJobs.remove(artist.browseId)?.cancel()
        relatedJobs[artist.browseId] = viewModelScope.launch {
            try {
                val page = YtMusicRepository.artistLanding(artist.browseId).getOrNull() ?: return@launch
                val related = page.sections
                    .firstOrNull { shelf -> shelf.items.isNotEmpty() && shelf.items.all { it.browseId?.startsWith("UC") == true } }
                    ?.items.orEmpty()
                    .filter { it.browseId != artist.browseId && it.browseId?.startsWith("UC") == true }
                    .map { ArtistSuggestion(BrowseItem(it.browseId!!, it.title, it.subtitle, it.thumbnailUrl, BrowseType.ARTIST), artist.title) }
                    .distinctBy { it.artist.browseId }
                if (related.isNotEmpty()) {
                    val current = _artists.value.toMutableList()
                    val anchorIndex = current.indexOfFirst { it.artist.browseId == artist.browseId }
                    if (anchorIndex >= 0) {
                        val existing = current.mapTo(hashSetOf()) { it.artist.browseId }
                        val additions = related.filterNot { it.artist.browseId in existing }
                        current.addAll(anchorIndex + 1, additions)
                        _artists.value = current.take(60)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(OnboardingViewModel::class.java))
            return OnboardingViewModel(application) as T
        }
    }
}
