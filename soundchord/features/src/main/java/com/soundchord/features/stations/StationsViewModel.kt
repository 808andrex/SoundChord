package com.soundchord.features.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Owns station recommendations and the YouTube Music radio queue for one seed. */
class StationsViewModel : ViewModel() {
    private val _stations = MutableStateFlow<UiState<List<Song>>>(UiState.Loading)
    val stations: StateFlow<UiState<List<Song>>> = _stations.asStateFlow()

    private val _upNext = MutableStateFlow<UiState<List<Song>>>(UiState.Loading)
    val upNext: StateFlow<UiState<List<Song>>> = _upNext.asStateFlow()

    private var loaded = false
    private var detailSeedId: String? = null

    fun loadStations(force: Boolean = false) {
        if (loaded && !force) return
        loaded = true
        if (force) _stations.value = UiState.Loading
        viewModelScope.launch {
            _stations.value = YtMusicRepository.recents().fold(
                onSuccess = { songs ->
                    val current = (_stations.value as? UiState.Success)?.data.orEmpty()
                    UiState.Success((current + songs).distinctBy(Song::videoId).take(20))
                },
                onFailure = { UiState.Error(it.message ?: "No se pudieron cargar tus escuchas recientes") },
            )
        }
    }

    fun includeCurrentSong(song: Song?) {
        if (song == null) return
        val current = (_stations.value as? UiState.Success)?.data.orEmpty()
        _stations.value = UiState.Success((listOf(song) + current).distinctBy(Song::videoId).take(20))
    }

    fun loadStation(seed: Song) {
        if (detailSeedId == seed.videoId && _upNext.value !is UiState.Error) return
        detailSeedId = seed.videoId
        _upNext.value = UiState.Loading
        viewModelScope.launch {
            _upNext.value = YtMusicRepository.radio(seed.videoId, preferVideo = seed.isVideo).fold(
                onSuccess = { queue -> UiState.Success(queue.filterNot { it.videoId == seed.videoId }.distinctBy(Song::videoId).take(40)) },
                onFailure = { UiState.Error(it.message ?: "No se pudo cargar la estación") },
            )
        }
    }
}
