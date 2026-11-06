package com.soundchord.features.radio.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RecentStationsState {
    data object Loading : RecentStationsState
    data class Success(val songs: List<Song>) : RecentStationsState
    data object Error : RecentStationsState
}

sealed interface PersonalStationQueueState {
    data object Loading : PersonalStationQueueState
    data class Success(val songs: List<Song>) : PersonalStationQueueState
    data object Error : PersonalStationQueueState
}

/** Loads recent songs and the YouTube Music radio queue for a chosen seed. */
class PersonalStationsViewModel : ViewModel() {
    private val _recentStations = MutableStateFlow<RecentStationsState>(RecentStationsState.Loading)
    val recentStations: StateFlow<RecentStationsState> = _recentStations.asStateFlow()

    private val _stationQueue = MutableStateFlow<PersonalStationQueueState>(PersonalStationQueueState.Loading)
    val stationQueue: StateFlow<PersonalStationQueueState> = _stationQueue.asStateFlow()

    private var recentSongs = emptyList<Song>()
    private var recentLoadFailed = false
    private var currentSong: Song? = null
    private var recentLoadStarted = false
    private var recentJob: Job? = null
    private var queueSeedId: String? = null
    private var queueJob: Job? = null

    fun includeCurrentSong(song: Song?) {
        currentSong = song?.takeUnless {
            it.isLiveRadioStream || it.playbackSourceId?.startsWith("radio:") == true
        }
        if (recentLoadStarted) publishRecentStations()
    }

    fun loadRecentStations(force: Boolean = false) {
        if (recentLoadStarted && !force) return
        recentJob?.cancel()
        recentLoadStarted = true
        _recentStations.value = RecentStationsState.Loading
        recentJob = viewModelScope.launch {
            try {
                val result = YtMusicRepository.recents()
                recentSongs = result.getOrDefault(emptyList())
                recentLoadFailed = result.isFailure
                publishRecentStations()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                recentLoadFailed = true
                publishRecentStations()
            }
        }
    }

    fun loadStation(seed: Song, force: Boolean = false) {
        if (queueSeedId == seed.videoId && !force) return
        queueSeedId = seed.videoId
        queueJob?.cancel()
        queueJob = viewModelScope.launch {
            _stationQueue.value = PersonalStationQueueState.Loading
            try {
                YtMusicRepository.radio(seed.videoId, preferVideo = seed.isVideo)
                    .onSuccess { songs ->
                        if (queueSeedId == seed.videoId) {
                            _stationQueue.value = PersonalStationQueueState.Success(
                                songs.filterNot { it.videoId == seed.videoId }
                                    .distinctBy(Song::videoId)
                                    .take(40),
                            )
                        }
                    }
                    .onFailure {
                        if (queueSeedId == seed.videoId) _stationQueue.value = PersonalStationQueueState.Error
                    }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                if (queueSeedId == seed.videoId) _stationQueue.value = PersonalStationQueueState.Error
            }
        }
    }

    private fun publishRecentStations() {
        val songs = buildList {
            currentSong?.let(::add)
            addAll(recentSongs)
        }.distinctBy(Song::videoId).take(20)

        _recentStations.value = when {
            songs.isNotEmpty() -> RecentStationsState.Success(songs)
            recentLoadFailed -> RecentStationsState.Error
            else -> RecentStationsState.Success(emptyList())
        }
    }
}
