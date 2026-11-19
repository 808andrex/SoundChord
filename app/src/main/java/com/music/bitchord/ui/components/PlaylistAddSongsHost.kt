package com.music.bitchord.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.music.bitchord.R
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.DetailPage
import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.model.LibraryPage
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UiState
import com.music.bitchord.ui.MainViewModel
import com.music.bitchord.ui.player.AddSongsRequest
import com.music.bitchord.playback.loadPlaylistRecommendations
import dev.chrisbanes.haze.HazeState

/** Hosts the collection-aware add dialog outside the player's window. */
@Composable
internal fun PlaylistAddSongsHost(
    viewModel: MainViewModel,
    detailStack: List<DetailPage>,
    library: UiState<LibraryPage>,
    likeStatuses: Map<String, LikeStatus>,
    query: String,
    results: UiState<List<SearchResult>>?,
    filter: SearchFilter,
    hazeState: HazeState,
    onNotice: (String) -> Unit,
) {
    val context = LocalContext.current
    val requestOpen by AddSongsRequest.open.collectAsStateWithLifecycle()
    val playlist by AddSongsRequest.playlist.collectAsStateWithLifecycle()
    val likedMusic by AddSongsRequest.likedMusic.collectAsStateWithLifecycle()
    val open = requestOpen && (playlist != null || likedMusic)
    val typeahead by viewModel.typeaheadResults.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val home by viewModel.home.collectAsStateWithLifecycle()
    var suggestions by remember { mutableStateOf<List<Song>?>(null) }
    var previousFilter by remember { mutableStateOf<SearchFilter?>(null) }

    val browseId = if (likedMusic) YtMusicRepository.LIKED_MUSIC else playlist?.browseId
    val page = detailStack.firstOrNull { it.browseId == browseId }
    val pageSongs = (page?.songs as? UiState.Success)?.data.orEmpty()
    val existingIds = remember(pageSongs) { pageSongs.mapTo(HashSet()) { it.videoId } }
    val likedIds = if (likedMusic) likeStatuses.filterValues { it == LikeStatus.LIKE }.keys else emptySet()
    val fallbackCandidates = remember(open, browseId, likedMusic, pageSongs, library, history, home, likedIds) {
        val likedSongs = (library as? UiState.Success)?.data?.likedSongs.orEmpty()
        val recentSongs = (history as? UiState.Success)?.data.orEmpty()
        val homeTracks = (home as? UiState.Success)?.data.orEmpty()
            .flatMap { it.items }
            .mapNotNull { item ->
                item.videoId?.takeIf(String::isNotBlank)?.let { id ->
                    Song(id, item.title, item.subtitle, item.thumbnailUrl)
                }
            }
        (recentSongs + likedSongs + homeTracks)
            .filterNot { it.videoId in existingIds || (likedMusic && it.videoId in likedIds) }
            .distinctBy { it.videoId }
            .take(30)
    }

    // If library/history/home finish loading after the sheet opened, put their
    // tracks in the blank-query list without waiting for another playlist fetch.
    LaunchedEffect(open, playlist?.playlistId, likedMusic, fallbackCandidates, query) {
        if (open && query.isBlank() && suggestions.isNullOrEmpty() && fallbackCandidates.isNotEmpty()) {
            suggestions = fallbackCandidates
        }
    }

    LaunchedEffect(open, playlist?.playlistId, likedMusic) {
        if (!open) return@LaunchedEffect
        viewModel.onQueryChange("")
        previousFilter = filter
        viewModel.onFilterChange(SearchFilter.ALL)
        suggestions = null

        // Show useful rows while the playlist lookup/recommendation request runs.
        suggestions = fallbackCandidates.takeIf { it.isNotEmpty() }
        val collection = when {
            likedMusic -> {
                (pageSongs + (library as? UiState.Success)?.data?.likedSongs.orEmpty()).distinctBy { it.videoId }
            }
            browseId != null -> YtMusicRepository.allSongs(browseId).getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?: pageSongs
            else -> emptyList()
        }
        val excludedIds = existingIds + collection.mapTo(HashSet()) { it.videoId } + likedIds
        val recommended = loadPlaylistRecommendations(
            collection = collection,
            preferred = if (likedMusic) emptyList() else page?.suggestedSongs.orEmpty(),
            excludedVideoIds = excludedIds,
        )
        suggestions = (recommended + fallbackCandidates)
            .filterNot { it.videoId in excludedIds }
            .distinctBy { it.videoId }
            .take(30)
    }

    AddSongsDialog(
        open = open,
        title = if (open) context.getString(R.string.add_songs_to_playlist) else null,
        query = query,
        onQueryChange = viewModel::onQueryChange,
        onSubmit = viewModel::submitSearch,
        results = results,
        typeahead = typeahead,
        suggestions = suggestions?.filterNot { song ->
            likedMusic && likeStatuses[song.videoId] == LikeStatus.LIKE
        },
        filter = filter,
        onFilterChange = viewModel::onFilterChange,
        onLiveSearch = viewModel::searchQuiet,
        backdrop = null,
        hazeState = hazeState,
        onAdd = { song ->
            if (playlist != null) {
                playlist?.let { target ->
                    viewModel.addToPlaylists(listOf(target), song) { added, _, failed ->
                        if (added == 0 && failed > 0) onNotice(context.getString(R.string.failed))
                    }
                }
            } else if (likedMusic) {
                viewModel.addSongToLikedMusic(song) { added ->
                    if (added) onNotice(context.getString(R.string.song_added_to_liked_music))
                }
            }
        },
        onDismiss = {
            AddSongsRequest.hide()
            viewModel.onQueryChange("")
            previousFilter?.let(viewModel::onFilterChange)
            previousFilter = null
        },
    )
}
