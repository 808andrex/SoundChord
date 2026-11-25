package com.soundchord.features.playlistimport.data

import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.sources.TrackMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * Result of importing and resolving an Apple Music playlist.
 */
data class AppleMusicImportResult(
    val playlist: AppleMusicPlaylist,
    val matchedSongs: List<Song>,
    val unmatchedTracks: List<AppleMusicTrack>,
)

object AppleMusicPlaylistRepository {

    private val knownLanguages = listOf(
        "Tamil", "Telugu", "Kannada", "Malayalam", "Hindi",
        "English", "Punjabi", "Marathi", "Bengali", "Bhojpuri", "Gujarati",
    )

    /**
     * Parses an Apple Music link or ID and fetches the remote playlist with all tracks.
     */
    suspend fun fetchPlaylist(urlOrId: String): Result<AppleMusicPlaylist> = withContext(Dispatchers.IO) {
        runCatching {
            val parsed = AppleMusicPlaylistClient.parsePlaylistUrl(urlOrId)
                ?: error("Invalid Apple Music playlist URL or ID")
            AppleMusicPlaylistClient.fetchPlaylist(parsed.first, parsed.second)
        }
    }

    /**
     * Resolves a single Apple Music track to a YouTube Music Song using [TrackMatcher].
     */
    suspend fun resolveTrack(
        track: AppleMusicTrack,
        language: String? = null,
    ): Song? {
        val cleanLang = language?.trim()?.takeIf {
            !it.equals("No Preference", ignoreCase = true) &&
                !it.equals("Multilingual", ignoreCase = true) &&
                it.isNotBlank()
        }

        val query = if (cleanLang != null) {
            "${track.title} ${track.artist} $cleanLang".trim()
        } else {
            "${track.title} ${track.artist}".trim()
        }

        val searchResult = YtMusicRepository.search(query, SearchFilter.SONGS).getOrNull()
        val candidates = searchResult?.filterIsInstance<SearchResult.Track>()
            ?.map { it.song }.orEmpty()

        val target = TrackMatcher.Target(
            title = track.title,
            artist = track.artist,
            durationSec = if (track.durationMs > 0) (track.durationMs / 1000).toInt() else null,
            album = track.album,
        )

        val bestMatch = TrackMatcher.best(candidates, target) ?: candidates.firstOrNull()

        return if (cleanLang != null && candidates.isNotEmpty()) {
            val conflictingLangs = knownLanguages.filter { !it.equals(cleanLang, ignoreCase = true) }
            val nonConflictingCandidates = candidates.filter { candidate ->
                val meta = "${candidate.title} ${candidate.artist} ${candidate.albumName.orEmpty()}".lowercase()
                !conflictingLangs.any {
                    meta.contains("(${it.lowercase()})") ||
                        meta.contains("${it.lowercase()} version") ||
                        meta.contains("${it.lowercase()} song")
                }
            }
            TrackMatcher.best(nonConflictingCandidates, target)
                ?: nonConflictingCandidates.firstOrNull()
                ?: bestMatch
        } else {
            bestMatch
        }
    }

    /**
     * Resolves tracks progressively:
     * 1. Resolves first [initialBatchSize] (default 20) with [concurrency] (default 8)
     *    and fires [onInitialBatchReady] immediately in ~1-1.5s so UI renders top tracks instantly.
     * 2. Continues resolving remaining tracks in background, firing [onProgressiveUpdate]
     *    as new tracks or batches finish.
     */
    suspend fun resolveAndMatchTracksProgressive(
        tracks: List<AppleMusicTrack>,
        language: String? = null,
        initialBatchSize: Int = 20,
        concurrency: Int = 8,
        onInitialBatchReady: (List<Song>) -> Unit = {},
        onProgressiveUpdate: (List<Song>) -> Unit = {},
    ): Pair<List<Song>, List<AppleMusicTrack>> = coroutineScope {
        val semaphore = Semaphore(concurrency)
        val initialTracks = tracks.take(initialBatchSize)
        val remainingTracks = tracks.drop(initialBatchSize)

        // 1. Resolve first batch
        val initialDeferred = initialTracks.map { track ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    val song = resolveTrack(track, language)
                    Pair(song, if (song == null) track else null)
                }
            }
        }
        val initialResults = initialDeferred.awaitAll()
        val initialMatchedSongs = initialResults.mapNotNull { it.first }
        val allMatchedSongs = java.util.Collections.synchronizedList(initialMatchedSongs.toMutableList())
        val allUnmatched = java.util.Collections.synchronizedList(initialResults.mapNotNull { it.second }.toMutableList())

        if (initialMatchedSongs.isNotEmpty()) {
            onInitialBatchReady(initialMatchedSongs.toList())
        }

        // 2. Resolve remaining tracks in parallel with Semaphore(8)
        if (remainingTracks.isNotEmpty()) {
            val remainingDeferred = remainingTracks.map { track ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        val song = resolveTrack(track, language)
                        if (song != null) {
                            allMatchedSongs.add(song)
                        } else {
                            allUnmatched.add(track)
                        }
                    }
                }
            }

            var lastUpdateCount = allMatchedSongs.size
            for (def in remainingDeferred) {
                def.await()
                val currentSize = allMatchedSongs.size
                if (currentSize - lastUpdateCount >= 5) {
                    lastUpdateCount = currentSize
                    onProgressiveUpdate(allMatchedSongs.toList())
                }
            }
            onProgressiveUpdate(allMatchedSongs.toList())
        }

        Pair(allMatchedSongs.toList(), allUnmatched.toList())
    }

    /**
     * Resolves Apple Music tracks to YouTube Music Song objects using [TrackMatcher].
     * Uses bounded concurrency with Semaphore(8) to prevent network saturation.
     * Reports real-time progress via [onProgress].
     */
    suspend fun resolveAndMatchTracks(
        tracks: List<AppleMusicTrack>,
        language: String? = null,
        onProgress: (completed: Int, total: Int) -> Unit,
    ): Pair<List<Song>, List<AppleMusicTrack>> = coroutineScope {
        val total = tracks.size
        val completedCount = AtomicInteger(0)
        val semaphore = Semaphore(8)

        val deferredResults = tracks.map { track ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    val song = resolveTrack(track, language)
                    val done = completedCount.incrementAndGet()
                    onProgress(done, total)

                    if (song != null) {
                        Pair(song, null)
                    } else {
                        Pair(null, track)
                    }
                }
            }
        }

        val results = deferredResults.awaitAll()
        val songs = results.mapNotNull { it.first }
        val unmatched = results.mapNotNull { it.second }

        Pair(songs, unmatched)
    }

    /**
     * Complete pipeline: downloads the Apple Music playlist and resolves all tracks.
     */
    suspend fun importPlaylist(
        urlOrId: String,
        language: String? = null,
        onProgress: (completed: Int, total: Int) -> Unit,
    ): Result<AppleMusicImportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val playlist = fetchPlaylist(urlOrId).getOrThrow()
            val (matchedSongs, unmatchedTracks) = resolveAndMatchTracks(playlist.tracks, language, onProgress)
            AppleMusicImportResult(
                playlist = playlist,
                matchedSongs = matchedSongs,
                unmatchedTracks = unmatchedTracks,
            )
        }
    }
}
