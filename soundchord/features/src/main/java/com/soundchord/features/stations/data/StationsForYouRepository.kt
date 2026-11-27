package com.soundchord.features.stations.data

import android.content.Context
import com.music.bitchord.data.AppleArtistPhotos
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.ArtistRef
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.TopPick
import com.music.bitchord.data.model.TopPickKind
import com.music.bitchord.data.model.WatchTarget
import com.music.bitchord.data.model.normalizedArtistName
import com.soundchord.features.daypart.data.UserTasteDaypartEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale

/**
 * Repository orchestrating the "Stations for You" (Emisoras para ti) shelf.
 * Delivers 10 to 12 personalized square station cards:
 * 1. Personal Station (Estación de [Usuario])
 * 2. Discovery Station (Por descubrir)
 * 3. 8 to 10 Custom Artist Stations ([Artista] y artistas similares)
 *
 * Real personalization sourced from YouTube Music (history, subscriptions, library, likes)
 * with intelligent fallback to local Room/ListeningStats, backed by SQLite daily cache.
 */
object StationsForYouRepository {

    private const val MAX_ARTIST_STATIONS = 10
    private const val MIN_ARTIST_STATIONS = 8
    private const val CONCURRENT_LANDING_LIMIT = 5

    fun init(context: Context) {
        StationsDatabaseCache.init(context)
    }

    private fun userKey(identity: String?): String = identity?.ifBlank { "guest" } ?: "guest"

    /**
     * Immediate 0 ms read from SQLite cache.
     */
    fun getCachedStations(identity: String?, date: LocalDate = LocalDate.now()): List<TopPick>? {
        return StationsDatabaseCache.get(userKey(identity), date.toString())
    }

    suspend fun getStationsForYou(
        homeShelves: List<HomeShelf>,
        identity: String?,
        listenerName: String?,
        localArtists: List<ArtistRef> = emptyList(),
        localSongs: List<Song> = emptyList(),
        forceRefresh: Boolean = false,
        date: LocalDate = LocalDate.now(),
    ): List<TopPick> = withContext(Dispatchers.IO) {
        val key = userKey(identity)
        val dateStr = date.toString()

        if (!forceRefresh) {
            val cached = StationsDatabaseCache.get(key, dateStr)
            if (!cached.isNullOrEmpty() && cached.size >= 10) {
                return@withContext cached
            }
        }

        // Daily rotation: remove older dates
        StationsDatabaseCache.prunePreviousDates(dateStr)

        val resolved = coroutineScope {
            // Parallel data gathering from YouTube Music account
            val historyDeferred = async { runCatching { YtMusicRepository.recentHistory().getOrNull() }.getOrNull().orEmpty() }
            val libraryDeferred = async { runCatching { YtMusicRepository.library().getOrNull() }.getOrNull() }
            val mixesDeferred = async { findMixes(homeShelves) }

            val history = historyDeferred.await()
            val libraryPage = libraryDeferred.await()
            val subscriptions = libraryPage?.shelves?.firstOrNull { it.title.equals("Subscriptions", ignoreCase = true) }?.items.orEmpty()
            val libraryArtists = libraryPage?.shelves?.firstOrNull { it.title.equals("Artists", ignoreCase = true) }?.items.orEmpty()
            val likedSongs = libraryPage?.likedSongs.orEmpty()
            val librarySongs = libraryPage?.librarySongs.orEmpty()
            val mixes = mixesDeferred.await()

            // 1. Collect and score artist candidates
            val candidates = rankCandidates(
                history = history,
                subscriptions = subscriptions,
                libraryArtists = libraryArtists,
                likedSongs = likedSongs,
                librarySongs = librarySongs,
                homeShelves = homeShelves,
                localArtists = localArtists,
            )

            // 2. Resolve artist pages and photos
            val limiter = Semaphore(CONCURRENT_LANDING_LIMIT)
            val candidateTargets = candidates.mapNotNull { artist ->
                val browseId = artist.browseId ?: return@mapNotNull null
                artist to browseId
            }.take(16)

            val artistPages = candidateTargets.map { (artist, browseId) ->
                async {
                    limiter.withPermit {
                        val page = YtMusicRepository.artistLanding(browseId).getOrNull()
                        if (page != null) artist to page else null
                    }
                }
            }.awaitAll().filterNotNull()

            // Deduplicate by normalized name
            val distinctArtists = artistPages
                .distinctBy { (ref, page) -> normalizedArtistName(page.name ?: ref.name) }
                .filter { (_, page) -> page.radio != null || page.shuffle != null || page.songs.isNotEmpty() }

            // 3. Resolve Apple Music photos / thumbnails for the top artists and their related artists
            val photoMap = LinkedHashMap<String, String>()
            distinctArtists.forEach { (ref, page) ->
                val artistName = page.name ?: ref.name
                val applePhoto = runCatching {
                    AppleArtistPhotos.photoFor(artistName, { page.songs.map { it.title } })
                }.getOrNull()
                val photoUrl = applePhoto ?: sanitizeAvatarUrl(page.thumbnailUrl)
                if (!photoUrl.isNullOrBlank()) {
                    photoMap[artistName] = photoUrl
                }
                // Also gather related artist photos
                page.sections.firstOrNull { it.items.isNotEmpty() && it.items.all { item -> item.browseId?.startsWith("UC") == true } }
                    ?.items?.take(3)?.forEach { relatedItem ->
                        val thumb = sanitizeAvatarUrl(relatedItem.thumbnailUrl)
                        if (!thumb.isNullOrBlank()) {
                            photoMap.putIfAbsent(relatedItem.title, thumb)
                        }
                    }
            }

            // 4. Build Card 1: Personal Station (Estación de [Usuario])
            val cardPersonal = buildPersonalStation(mixes, history, localSongs)

            // 5. Build Card 2: Discovery Station (Por descubrir)
            val cardDiscovery = buildDiscoveryStation(mixes, distinctArtists.firstOrNull(), history)

            // 6. Build Cards 3 to 12: 8 to 10 Artist Stations ([Artista] y artistas similares)
            val artistCards = distinctArtists.mapNotNull { (ref, page) ->
                val name = page.name ?: ref.name
                val radioTarget = page.radio
                    ?: page.shuffle
                    ?: page.songs.firstOrNull()?.let { WatchTarget(playlistId = "RDAMVM${it.videoId}", videoId = it.videoId) }
                    ?: return@mapNotNull null

                val primaryFace = photoMap[name] ?: sanitizeAvatarUrl(page.thumbnailUrl) ?: return@mapNotNull null
                val relatedFaces = page.sections
                    .firstOrNull { it.items.isNotEmpty() && it.items.all { item -> item.browseId?.startsWith("UC") == true } }
                    ?.items.orEmpty()
                    .filter { it.browseId != ref.browseId && normalizedArtistName(it.title) != normalizedArtistName(name) }
                    .mapNotNull { relatedItem ->
                        val thumb = sanitizeAvatarUrl(relatedItem.thumbnailUrl)
                        photoMap[relatedItem.title] ?: thumb
                    }
                    .filter { it != primaryFace }
                    .distinct()
                    .take(2)

                val images = listOf(primaryFace) + relatedFaces

                TopPick(
                    kind = TopPickKind.ARTIST_AND_SIMILAR,
                    title = name,
                    subtitle = "",
                    artistName = name,
                    artistBrowseId = ref.browseId,
                    images = images,
                    watch = radioTarget,
                )
            }.take(MAX_ARTIST_STATIONS)

            // Combine into the 10-12 stations carousel
            val result = buildList {
                cardPersonal?.let(::add)
                cardDiscovery?.let(::add)
                addAll(artistCards)
            }

            result
        }

        if (resolved.isNotEmpty()) {
            StationsDatabaseCache.put(key, dateStr, resolved)
        }

        resolved
    }

    private fun rankCandidates(
        history: List<Song>,
        subscriptions: List<ShelfItem>,
        libraryArtists: List<ShelfItem>,
        likedSongs: List<Song>,
        librarySongs: List<Song>,
        homeShelves: List<HomeShelf>,
        localArtists: List<ArtistRef>,
    ): List<ArtistRef> {
        val scores = LinkedHashMap<String, Pair<String, Double>>()

        fun credit(id: String?, name: String?, weight: Double) {
            if (id == null || !id.startsWith("UC") || name.isNullOrBlank()) return
            if (name.equals("Various Artists", ignoreCase = true) || name.equals("Varios Artistas", ignoreCase = true)) return
            val current = scores[id]
            val currentScore = current?.second ?: 0.0
            scores[id] = (name to (currentScore + weight))
        }

        // Subscriptions carry high intent
        subscriptions.forEach { item ->
            credit(item.browseId, item.title, 10.0)
        }

        // Library corpus artists
        libraryArtists.forEach { item ->
            credit(item.browseId, item.title, 6.0)
        }

        // Liked songs & library songs
        (likedSongs + librarySongs).forEach { song ->
            val lead = song.artists.firstOrNull { it.browseId?.startsWith("UC") == true }
            if (lead != null) {
                credit(lead.browseId, lead.name, 4.0)
            } else if (song.artistId?.startsWith("UC") == true) {
                credit(song.artistId, song.artist, 4.0)
            }
        }

        // Recent history weighted by recency
        val totalHistory = history.size.coerceAtLeast(1)
        history.forEachIndexed { index, song ->
            val recency = 1.0 - (index.toDouble() / totalHistory)
            val lead = song.artists.firstOrNull { it.browseId?.startsWith("UC") == true }
            if (lead != null) {
                credit(lead.browseId, lead.name, 2.0 + recency * 3.0)
            } else if (song.artistId?.startsWith("UC") == true) {
                credit(song.artistId, song.artist, 2.0 + recency * 3.0)
            }
        }

        // Home shelves: Listen Again, Mix shelves
        val extractedNames = UserTasteDaypartEngine.extractUserArtists(homeShelves)
        extractedNames.forEach { name ->
            // If already scored under channel, give it an extra bump
            scores.entries.firstOrNull { it.value.first.equals(name, ignoreCase = true) }?.let { entry ->
                scores[entry.key] = (entry.value.first to (entry.value.second + 4.0))
            }
        }

        // Local taste fallback (Room ListeningStats, Onboarding seeds)
        val totalLocal = localArtists.size.coerceAtLeast(1)
        localArtists.forEachIndexed { index, artist ->
            val weight = 4.0 + (totalLocal - index).toDouble() * 0.5
            credit(artist.browseId, artist.name, weight)
        }

        return scores.entries
            .sortedByDescending { it.value.second }
            .map { ArtistRef(name = it.value.first, browseId = it.key) }
    }

    private fun buildPersonalStation(
        mixes: Map<MixType, ShelfItem>,
        history: List<Song>,
        localSongs: List<Song>,
    ): TopPick? {
        val watch = mixes[MixType.SUPERMIX]?.watchTarget()
            ?: (localSongs.firstOrNull() ?: history.firstOrNull())?.let { song ->
                WatchTarget(playlistId = "RDAMVM${song.videoId}", videoId = song.videoId)
            }
            ?: return null
        return TopPick(
            kind = TopPickKind.PERSONAL_STATION,
            watch = watch,
        )
    }

    private fun buildDiscoveryStation(
        mixes: Map<MixType, ShelfItem>,
        seedArtist: Pair<ArtistRef, com.music.bitchord.data.model.ArtistPage>?,
        history: List<Song>,
    ): TopPick? {
        mixes[MixType.DISCOVER]?.watchTarget()?.let { watch ->
            return TopPick(kind = TopPickKind.DISCOVERY_STATION, watch = watch)
        }
        val seed = seedArtist ?: return null
        val radio = seed.second.radio ?: seed.second.shuffle ?: return null
        return TopPick(
            kind = TopPickKind.DISCOVERY_STATION,
            artistName = seed.second.name ?: seed.first.name,
            watch = radio,
            exclude = history.take(150).map { it.videoId },
        )
    }

    private enum class MixType { SUPERMIX, DISCOVER }

    private suspend fun findMixes(homeShelves: List<HomeShelf>): Map<MixType, ShelfItem> {
        val onHome = homeShelves.flatMap { it.items }.filter { it.isMix() }
        val mixes = onHome.takeIf { it.isNotEmpty() }
            ?: runCatching { YtMusicRepository.mixedForYou().getOrNull() }.getOrNull().orEmpty().filter { it.isMix() }
        if (mixes.isEmpty()) return emptyMap()
        val found = mutableMapOf<MixType, ShelfItem>()
        mixes.forEach { item ->
            val title = item.title.lowercase(Locale.ROOT)
            val kind = when {
                "supermix" in title -> MixType.SUPERMIX
                "discover" in title -> MixType.DISCOVER
                else -> null
            }
            if (kind != null) found.putIfAbsent(kind, item)
        }
        if (found.isEmpty()) found[MixType.SUPERMIX] = mixes.first()
        return found
    }

    private fun ShelfItem.isMix(): Boolean =
        browseId?.removePrefix("VL")?.startsWith("RDTMAK") == true

    private fun ShelfItem.watchTarget(): WatchTarget? {
        val playlist = browseId?.removePrefix("VL") ?: return null
        return WatchTarget(playlistId = playlist, videoId = videoId)
    }

    /**
     * Sanitizes Google/YouTube channel avatar URLs by stripping white border/padding
     * parameters like `-k-c0x00ffffff-no-rj` or `-c0x00ffffff` and replacing with a clean
     * full square crop (`-p-l90-rj` or `=s512-c-rp-mo`).
     */
    private fun sanitizeAvatarUrl(url: String?): String? {
        if (url == null) return null
        if ("ggpht.com" !in url && "googleusercontent.com" !in url) return url
        var clean = url.replace("-c0x00ffffff", "")
            .replace("-c0xffffffff", "")
            .replace("-k-no-rj", "-no-rj")
            .replace("-k-no", "-no")
            .replace("-k-c0x00ffffff", "")
        if (clean.contains("=w") || clean.contains("=s")) {
            clean = clean.replace(Regex("""=w\d+-h\d+[^/?]*"""), "=w512-h512-p-l90-rj")
                .replace(Regex("""=s\d+[^/?]*"""), "=s512-c-rp-mo")
        }
        return clean
    }
}
