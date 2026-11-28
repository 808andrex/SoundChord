package com.music.bitchord.ui.app

import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.LibraryPage
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.TopPick
import com.music.bitchord.data.model.TopPickKind
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.stats.ListeningStats
import com.music.bitchord.data.stats.ReplayPeriod
import com.music.bitchord.download.SavedCollection
import com.soundchord.features.daypart.data.UserTasteDaypartEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Aggregates the user's real listening history and tastes across all local and remote sources:
 * - Local playback history (Room / ListeningStats)
 * - Remote YouTube Music listening history
 * - Liked songs from Library
 * - Saved albums and downloads
 * - Subscriptions and followed artists
 * - Onboarding picks and Home dynamic shelves
 */
object UserArtistPoolResolver {

    suspend fun resolveUserRealArtists(
        historyState: UiState<List<Song>>,
        libraryState: UiState<LibraryPage>,
        savedCollections: Map<String, SavedCollection>,
        topPicks: List<TopPick>?,
        homeShelves: List<HomeShelf>,
    ): List<String> = withContext(Dispatchers.IO) {
        val rawArtists = mutableListOf<String>()

        // 1. Historial local de reproducción (Room / ListeningStats)
        runCatching {
            val allTimeSummary = ListeningStats.summary(ReplayPeriod.ALL_TIME)
            allTimeSummary.artists.forEach { rawArtists.add(it.title) }
            val monthSummary = ListeningStats.summary(ReplayPeriod.THIS_MONTH)
            monthSummary.artists.forEach { rawArtists.add(it.title) }
        }

        // Historial remoto de reproducción (YouTube Music)
        (historyState as? UiState.Success)?.data?.forEach { song ->
            if (song.artist.isNotBlank()) rawArtists.add(song.artist)
        }

        // 2. Canciones con Me gusta
        val libraryData = (libraryState as? UiState.Success)?.data
        libraryData?.likedSongs?.forEach { song ->
            if (song.artist.isNotBlank()) rawArtists.add(song.artist)
        }
        libraryData?.librarySongs?.forEach { song ->
            if (song.artist.isNotBlank()) rawArtists.add(song.artist)
        }

        // 3. Álbumes guardados
        libraryData?.shelves?.forEach { shelf ->
            val title = shelf.title.trim().lowercase(Locale.ROOT)
            if (title == "albums" || title == "álbumes") {
                shelf.items.forEach { item ->
                    if (item.subtitle.isNotBlank()) rawArtists.add(item.subtitle)
                }
            }
        }
        savedCollections.values.forEach { col ->
            if (col.subtitle.isNotBlank()) rawArtists.add(col.subtitle)
        }

        // 4. Suscripciones de YouTube Music y Artistas de la biblioteca
        libraryData?.shelves?.forEach { shelf ->
            val title = shelf.title.trim().lowercase(Locale.ROOT)
            if (title == "subscriptions" || title == "suscripciones" ||
                title == "artists" || title == "artistas"
            ) {
                shelf.items.forEach { item ->
                    if (item.title.isNotBlank()) rawArtists.add(item.title)
                }
            }
        }

        // 5. Onboarding y Top Picks
        AppSettings.onboardingArtists.value.forEach { rawArtists.add(it.name) }
        topPicks?.forEach { pick ->
            val name = when (pick.kind) {
                TopPickKind.FEATURED_ARTIST -> pick.artistName?.takeIf { it.isNotBlank() } ?: pick.title.takeIf { it.isNotBlank() }
                TopPickKind.ARTIST_AND_SIMILAR -> pick.artistName?.takeIf { it.isNotBlank() } ?: pick.subtitle.takeIf { it.isNotBlank() }
                TopPickKind.LATEST_RELEASE -> pick.artistName?.takeIf { it.isNotBlank() } ?: pick.subtitle.takeIf { it.isNotBlank() }
                TopPickKind.NEW_RELEASE_MIX -> pick.artistName?.takeIf { it.isNotBlank() } ?: pick.subtitle.takeIf { it.isNotBlank() }
                TopPickKind.FAVORITES_MIX -> pick.artistName?.takeIf { it.isNotBlank() } ?: pick.subtitle.takeIf { it.isNotBlank() }
                else -> pick.artistName?.takeIf { it.isNotBlank() }
            }
            if (!name.isNullOrBlank()) rawArtists.add(name)
        }

        // Sanitize and extract Top 25 real artists
        UserTasteDaypartEngine.extractUserArtists(homeShelves, rawArtists, limit = 25)
    }

    suspend fun resolveUserGenreKeywords(
        historyState: UiState<List<Song>>,
        libraryState: UiState<LibraryPage>,
        homeShelves: List<HomeShelf>,
    ): List<String> = withContext(Dispatchers.IO) {
        val keywords = mutableSetOf<String>()

        // 1. Historial de reproducción (canciones y álbumes)
        (historyState as? UiState.Success)?.data?.forEach { song ->
            if (song.title.isNotBlank()) keywords.add(song.title)
            song.albumName?.takeIf { it.isNotBlank() }?.let { keywords.add(it) }
        }

        // 2. Canciones de biblioteca y me gusta
        val libraryData = (libraryState as? UiState.Success)?.data
        libraryData?.likedSongs?.forEach { song ->
            if (song.title.isNotBlank()) keywords.add(song.title)
            song.albumName?.takeIf { it.isNotBlank() }?.let { keywords.add(it) }
        }
        libraryData?.librarySongs?.forEach { song ->
            if (song.title.isNotBlank()) keywords.add(song.title)
            song.albumName?.takeIf { it.isNotBlank() }?.let { keywords.add(it) }
        }

        // 3. Estantes dinámicos de Inicio (ej. "Mix de Cumbia", "Mix de Salsa")
        homeShelves.forEach { shelf ->
            if (shelf.title.isNotBlank()) keywords.add(shelf.title)
            shelf.items.forEach { item ->
                if (item.title.isNotBlank()) keywords.add(item.title)
            }
        }

        keywords.toList()
    }
}
