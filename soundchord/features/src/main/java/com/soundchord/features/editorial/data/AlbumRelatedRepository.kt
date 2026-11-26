package com.soundchord.features.editorial.data

import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.AlbumRelatedSections
import com.music.bitchord.data.model.ArtistPage
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Editorial and catalogue recommendations repository for Album detail pages.
 * Resolves style Apple Music + YouTube Music sections asynchronously in parallel:
 * 1. Music Videos (16:9 official videos for tracks on the album)
 * 2. More by <Artist> (other releases by the primary artist, excluding current album)
 * 3. Appears On (curated official editorial playlists strictly specific to the artist)
 * 4. You Might Also Like (similar albums from related artists)
 *
 * Uses composite cache key (albumId + artistName) in memory and SQLite DB.
 * Discards any playlists not relevant to the specific artist.
 */
object AlbumRelatedRepository {

    private val cache = ConcurrentHashMap<String, AlbumRelatedSections>()

    fun getCached(key: String): AlbumRelatedSections? = cache[key]

    fun putInMemoryCache(key: String, sections: AlbumRelatedSections) {
        cache[key] = sections
    }

    fun invalidate(key: String) {
        cache.remove(key)
        AlbumRelatedDatabaseCache.delete(key)
    }

    fun clearCache() {
        cache.clear()
    }

    fun cleanThumbnailUrl(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        var url = rawUrl.trim()
        if (url.startsWith("//")) {
            url = "https:$url"
        } else if (url.startsWith("http://")) {
            url = "https://${url.removePrefix("http://")}"
        }
        if (!url.startsWith("https://")) return null
        if (url.contains("{") || url.contains("}")) return null

        // Fix sddefault.jpg 404s on older YouTube videos/thumbnails
        if (url.contains("/sddefault.jpg")) {
            url = url.replace("/sddefault.jpg", "/hqdefault.jpg")
        }

        // For Google/YouTube content servers, normalize/upgrade size to 600px square
        if (url.contains("googleusercontent.com") || url.contains("ggpht.com")) {
            url = url.replace(Regex("=w\\d+-h\\d+[^?#]*"), "=s600-c")
                .replace(Regex("=s\\d+[^?#]*"), "=s600-c")
        }

        return url
    }

    fun isValidArtwork(url: String?): Boolean {
        return !cleanThumbnailUrl(url).isNullOrBlank()
    }

    fun extractPrimaryArtist(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var str = raw.trim()
        str = str.split("•", "·").firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        str = str.split(Regex("(?i),?\\s*\\b(?:álbum|album|ep|single|sencillo)\\b")).firstOrNull()?.trim().orEmpty()

        if (str.startsWith("Tyler, The Creator", ignoreCase = true) || str.startsWith("Tyler, the Creator", ignoreCase = true)) {
            return "Tyler, the Creator"
        }
        if (str.startsWith("Earth, Wind & Fire", ignoreCase = true)) {
            return "Earth, Wind & Fire"
        }

        val parts = str.split(Regex("(?i)\\s*(?:,|&|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bx\\b|\\bwith\\b)\\s*"))
        val primary = parts.firstOrNull { it.isNotBlank() }?.trim().orEmpty()

        if (primary.equals("álbum", ignoreCase = true) || primary.equals("album", ignoreCase = true) ||
            primary.equals("ep", ignoreCase = true) || primary.equals("single", ignoreCase = true) ||
            primary.equals("sencillo", ignoreCase = true)
        ) {
            return ""
        }
        return primary.replace(Regex("(?i)\\b(?:álbum|album)\\b"), "").trim()
    }

    fun extractBaseTrackTitle(title: String): String {
        val withoutDecorations = title
            .replace(Regex("(?i)\\b(official music video|official video|video oficial|videoclip|music video|visualizer|visualiser|lyric video|lyrics|audio)\\b"), "")
            .replace(Regex("[\\(\\[\\{].*?[\\)\\]\\}]"), "")
            .trim()
        return normalize(withoutDecorations)
    }

    suspend fun getAlbumRelatedSections(
        browseId: String,
        albumTitle: String,
        artistName: String,
        artistBrowseId: String?,
        songs: List<Song>,
        onVideosLoaded: ((List<ShelfItem>) -> Unit)? = null,
        onMoreByArtistLoaded: ((List<ShelfItem>) -> Unit)? = null,
        onAppearsOnLoaded: ((List<ShelfItem>) -> Unit)? = null,
        onYouMightLikeLoaded: ((List<ShelfItem>) -> Unit)? = null,
    ): AlbumRelatedSections = withContext(Dispatchers.IO) {
        val cleanAlbumTitle = albumTitle.trim()
        val effectiveSongs = if (songs.isEmpty() && browseId.isNotBlank()) {
            runCatching {
                YtMusicRepository.browseSongs(browseId).getOrNull()?.songs.orEmpty()
            }.getOrDefault(emptyList())
        } else {
            songs
        }

        val rawArtist = artistName.trim().ifBlank {
            effectiveSongs.firstOrNull()?.artist.orEmpty().trim()
        }
        val primaryArtist = extractPrimaryArtist(rawArtist).ifBlank {
            extractPrimaryArtist(effectiveSongs.firstOrNull()?.artist)
        }.ifBlank { rawArtist }

        val cacheKey = if (primaryArtist.isNotBlank()) "$browseId:${normalize(primaryArtist)}" else browseId

        // 1. In-memory cache check (must have videos if loaded)
        cache[cacheKey]?.let { cached ->
            if (cached.isLoaded && cached.musicVideos.isNotEmpty()) {
                onVideosLoaded?.invoke(cached.musicVideos)
                onMoreByArtistLoaded?.invoke(cached.moreByArtist)
                onAppearsOnLoaded?.invoke(cached.appearsOn)
                onYouMightLikeLoaded?.invoke(cached.youMightAlsoLike)
                return@withContext cached
            }
        }

        // 2. Persistent SQLite DB cache check
        val dbCached = AlbumRelatedDatabaseCache.get(cacheKey)
        if (dbCached != null && dbCached.isLoaded && dbCached.musicVideos.isNotEmpty()) {
            cache[cacheKey] = dbCached
            onVideosLoaded?.invoke(dbCached.musicVideos)
            onMoreByArtistLoaded?.invoke(dbCached.moreByArtist)
            onAppearsOnLoaded?.invoke(dbCached.appearsOn)
            onYouMightLikeLoaded?.invoke(dbCached.youMightAlsoLike)
            return@withContext dbCached
        }

        // 3. Resolve artist browseId if not provided
        val resolvedArtistId = artistBrowseId?.takeIf { it.isNotBlank() }
            ?: effectiveSongs.firstOrNull { !it.artistId.isNullOrBlank() }?.artistId
            ?: effectiveSongs.flatMap { it.artists }.firstOrNull { !it.browseId.isNullOrBlank() }?.browseId
            ?: if (primaryArtist.isNotBlank()) {
                runCatching {
                    YtMusicRepository.search(primaryArtist, SearchFilter.ARTISTS)
                        .getOrNull()
                        ?.filterIsInstance<SearchResult.Browse>()
                        ?.firstOrNull { it.item.type == BrowseType.ARTIST }
                        ?.item?.browseId
                }.getOrNull()
            } else null

        // 4. Fetch artist page from YouTube Music to seed discography and related artists
        val artistPage: ArtistPage? = if (!resolvedArtistId.isNullOrBlank()) {
            runCatching {
                YtMusicRepository.artistPage(resolvedArtistId).getOrNull()
            }.getOrNull()
        } else null

        // 5. Resolve all 4 sections in parallel with independent progressive emissions
        var resolvedVideos = emptyList<ShelfItem>()
        var resolvedMore = emptyList<ShelfItem>()
        var resolvedAppears = emptyList<ShelfItem>()
        var resolvedLike = emptyList<ShelfItem>()

        coroutineScope {
            launch {
                val videos = runCatching {
                    fetchMusicVideos(primaryArtist, cleanAlbumTitle, effectiveSongs, artistPage)
                }.getOrDefault(emptyList())
                resolvedVideos = videos
                if (videos.isNotEmpty()) {
                    onVideosLoaded?.invoke(videos)
                }
            }

            launch {
                val more = runCatching {
                    fetchMoreByArtist(browseId, cleanAlbumTitle, primaryArtist, artistPage)
                }.getOrDefault(emptyList())
                resolvedMore = more
                if (more.isNotEmpty()) {
                    onMoreByArtistLoaded?.invoke(more)
                }
            }

            launch {
                val appears = runCatching {
                    fetchAppearsOn(primaryArtist, effectiveSongs, artistPage)
                }.getOrDefault(emptyList())
                resolvedAppears = appears
                if (appears.isNotEmpty()) {
                    onAppearsOnLoaded?.invoke(appears)
                }
            }

            launch {
                val like = runCatching {
                    fetchYouMightAlsoLike(browseId, cleanAlbumTitle, primaryArtist, artistPage)
                }.getOrDefault(emptyList())
                resolvedLike = like
                if (like.isNotEmpty()) {
                    onYouMightLikeLoaded?.invoke(like)
                }
            }
        }

        val completed = AlbumRelatedSections(
            musicVideos = resolvedVideos,
            moreByArtist = resolvedMore,
            appearsOn = resolvedAppears,
            youMightAlsoLike = resolvedLike,
            isLoaded = true,
        )
        cache[cacheKey] = completed
        AlbumRelatedDatabaseCache.put(cacheKey, completed)
        completed
    }

    private data class ScoredVideo(
        val item: ShelfItem,
        val score: Int,
    )

    /**
     * Section 1: Music Videos (16:9).
     * Official music videos strictly for tracks on the album.
     * Includes collaborations with other artists (e.g. Playboi Carti, Giorgio Moroder).
     * Excludes karaoke, slowed, reverb, fan edits, static audios (e.g. "(Audio)", "Official Audio"),
     * and unofficial channels like "Vevo Play", deduplicating strictly by track title.
     */
    private suspend fun fetchMusicVideos(
        primaryArtist: String,
        albumTitle: String,
        songs: List<Song>,
        artistPage: ArtistPage?,
    ): List<ShelfItem> {
        val candidatesByTrack = LinkedHashMap<String, ScoredVideo>()

        val isAlbumLive = albumTitle.contains("live", ignoreCase = true) ||
            albumTitle.contains("en vivo", ignoreCase = true) ||
            songs.any { it.title.contains("live", ignoreCase = true) || it.title.contains("en vivo", ignoreCase = true) }

        val isAlbumRemix = albumTitle.contains("remix", ignoreCase = true) ||
            songs.any { it.title.contains("remix", ignoreCase = true) }

        fun findMatchingSong(videoTitle: String): Song? {
            val normVTitle = normalize(videoTitle)
            return songs.firstOrNull { song ->
                val base = extractBaseTrackTitle(song.title)
                base.isNotBlank() && (normVTitle.contains(base) || base.contains(normVTitle))
            }
        }

        fun considerVideo(
            rawTitle: String,
            rawAuthor: String,
            videoId: String,
            thumbnailUrl: String?,
            isVideoType: Boolean = true,
            bonusScore: Int = 0,
        ) {
            if (videoId.isBlank()) return
            val cleanArt = cleanThumbnailUrl(thumbnailUrl) ?: return

            val matchedSong = findMatchingSong(rawTitle)
            val trackIsLive = isAlbumLive || (matchedSong != null && (matchedSong.title.contains("live", ignoreCase = true) || matchedSong.title.contains("en vivo", ignoreCase = true)))
            val trackIsRemix = isAlbumRemix || (matchedSong != null && matchedSong.title.contains("remix", ignoreCase = true))

            if (isJunkOrUnofficialVideo(rawTitle, trackIsLive, trackIsRemix)) return

            val trackArtists = if (matchedSong != null) {
                (listOf(primaryArtist, matchedSong.artist) +
                    matchedSong.artists.map { it.name } +
                    matchedSong.artist.split(Regex("(?i)\\s*(?:,|&|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bx\\b|\\bwith\\b)\\s*"))
                ).map { it.trim() }.filter { it.isNotBlank() }.distinct()
            } else {
                listOf(primaryArtist)
            }

            if (!isOfficialArtistVideoAuthor(rawAuthor, primaryArtist, trackArtists)) return

            val trackKey = if (matchedSong != null) {
                extractBaseTrackTitle(matchedSong.title)
            } else {
                extractBaseTrackTitle(rawTitle)
            }
            if (trackKey.isBlank()) return

            val score = videoQualityScore(rawTitle, isVideoType) + bonusScore + (if (matchedSong != null) 200 else 50)
            val existing = candidatesByTrack[trackKey]
            if (existing == null || score > existing.score) {
                val item = ShelfItem(
                    title = rawTitle,
                    subtitle = rawAuthor.ifBlank { primaryArtist },
                    thumbnailUrl = cleanArt,
                    videoId = videoId,
                    browseId = null,
                    isVideo = true,
                )
                candidatesByTrack[trackKey] = ScoredVideo(item, score)
            }
        }

        // 1a. Search album videos query on YouTube Music
        if (primaryArtist.isNotBlank() && albumTitle.isNotBlank()) {
            val query = "$primaryArtist $albumTitle"
            val videoResults = runCatching {
                YtMusicRepository.search(query, SearchFilter.VIDEOS).getOrNull()
                    ?.filterIsInstance<SearchResult.Track>()
                    ?.map { it.song }
                    .orEmpty()
            }.getOrDefault(emptyList())

            for (song in videoResults) {
                considerVideo(
                    rawTitle = song.title,
                    rawAuthor = song.artist,
                    videoId = song.videoId,
                    thumbnailUrl = song.thumbnailUrl,
                    isVideoType = true,
                    bonusScore = 50,
                )
            }
        }

        // 1b. Match videos from artist page's Videos shelf
        val videoShelves = artistPage?.sections?.filter { shelf ->
            shelf.title.contains("Video", ignoreCase = true) ||
                shelf.title.contains("Vídeo", ignoreCase = true) ||
                shelf.items.any { it.isVideo }
        }.orEmpty()
        val artistVideos = videoShelves.flatMap { it.items }.filter { it.isVideo || it.videoId != null }

        for (v in artistVideos) {
            val videoId = v.videoId ?: continue
            considerVideo(
                rawTitle = v.title,
                rawAuthor = v.subtitle,
                videoId = videoId,
                thumbnailUrl = v.thumbnailUrl,
                isVideoType = true,
                bonusScore = 100,
            )
        }

        // 1c. Search individual tracks from the album in parallel to cover collaborations and popular songs
        // (e.g. Timeless with Playboi Carti, Big Sleep with Giorgio Moroder, Cry For Me, Open Hearts, etc.)
        if (songs.isNotEmpty()) {
            val candidateSongs = songs.take(14).filter { song ->
                val trackKey = extractBaseTrackTitle(song.title)
                val existing = candidatesByTrack[trackKey]
                existing == null || existing.score < 280
            }

            if (candidateSongs.isNotEmpty()) {
                coroutineScope {
                    candidateSongs.map { song ->
                        async {
                            val cleanTitle = extractBaseTrackTitle(song.title)
                            val songArtist = if (song.artist.isNotBlank()) song.artist else primaryArtist
                            val query = "$songArtist $cleanTitle"
                            val results = runCatching {
                                YtMusicRepository.search(query, SearchFilter.VIDEOS).getOrNull()
                                    ?.filterIsInstance<SearchResult.Track>()
                                    ?.map { it.song }
                                    .orEmpty()
                            }.getOrDefault(emptyList())
                            song to results
                        }
                    }.awaitAll()
                }.forEach { (song, searchResults) ->
                    for (result in searchResults.take(4)) {
                        considerVideo(
                            rawTitle = result.title,
                            rawAuthor = result.artist,
                            videoId = result.videoId,
                            thumbnailUrl = result.thumbnailUrl,
                            isVideoType = true,
                            bonusScore = 70,
                        )
                    }
                }
            }
        }

        return candidatesByTrack.values
            .sortedByDescending { it.score }
            .map { it.item }
            .take(12)
    }

    /**
     * Section 2: More by <Artist>.
     * Other albums, EPs and singles by the main artist, excluding currently viewed album.
     */
    private suspend fun fetchMoreByArtist(
        currentBrowseId: String,
        currentAlbumTitle: String,
        primaryArtist: String,
        artistPage: ArtistPage?,
    ): List<ShelfItem> {
        val candidates = mutableListOf<ShelfItem>()

        // 2a. YouTube Music discography from artist landing page
        val releaseShelves = artistPage?.sections?.filter { shelf ->
            shelf.title.contains("Album", ignoreCase = true) ||
                shelf.title.contains("Álbum", ignoreCase = true) ||
                shelf.title.contains("Single", ignoreCase = true) ||
                shelf.title.contains("Sencillo", ignoreCase = true) ||
                shelf.title.contains("EP", ignoreCase = true)
        }.orEmpty()
        candidates.addAll(releaseShelves.flatMap { it.items }.filter { it.browseId != null && !it.isVideo })

        // 2b. Apple Music albums for this artist
        if (primaryArtist.isNotBlank()) {
            val appleAlbums = runCatching {
                AppleMusicEditorialRepository.searchAlbums(primaryArtist, limit = 12)
            }.getOrDefault(emptyList()).mapNotNull { item ->
                val cleanArt = cleanThumbnailUrl(item.thumbnailUrl) ?: return@mapNotNull null
                ShelfItem(
                    title = item.title,
                    subtitle = item.subtitle,
                    thumbnailUrl = cleanArt,
                    videoId = null,
                    browseId = "apple:album:${item.storefront}:${item.id}",
                    isVideo = false,
                )
            }
            candidates.addAll(appleAlbums)
        }

        // 2c. Fallback search on YouTube Music if scarce
        if (candidates.size < 5 && primaryArtist.isNotBlank()) {
            val ytmAlbums = runCatching {
                YtMusicRepository.search(primaryArtist, SearchFilter.ALBUMS).getOrNull()
                    ?.filterIsInstance<SearchResult.Browse>()
                    ?.map { it.item }
                    ?.filter { it.type == BrowseType.ALBUM && it.browseId != null }
                    ?.mapNotNull {
                        val cleanArt = cleanThumbnailUrl(it.thumbnailUrl) ?: return@mapNotNull null
                        ShelfItem(
                            title = it.title,
                            subtitle = it.subtitle.ifBlank { primaryArtist },
                            thumbnailUrl = cleanArt,
                            videoId = null,
                            browseId = it.browseId,
                            isVideo = false,
                        )
                    }.orEmpty()
            }.getOrDefault(emptyList())
            candidates.addAll(ytmAlbums)
        }

        // 2d. Strict exclusion of currently viewed album
        val normCurrentTitle = normalize(currentAlbumTitle)
        val normCurrentId = currentBrowseId.removePrefix("VL")

        val seenTitles = mutableSetOf<String>()
        val seenIds = mutableSetOf<String>()
        val filtered = mutableListOf<ShelfItem>()

        for (item in candidates) {
            val browseId = item.browseId ?: continue
            val normId = browseId.removePrefix("VL")
            if (normId.equals(normCurrentId, ignoreCase = true) || browseId.equals(currentBrowseId, ignoreCase = true)) continue

            val normTitle = normalize(item.title)
            if (normTitle.isEmpty()) continue
            if (normTitle.equals(normCurrentTitle, ignoreCase = true) || item.title.equals(currentAlbumTitle, ignoreCase = true)) continue
            if (normCurrentTitle.length >= 6 && normTitle == normCurrentTitle) continue

            val cleanArt = cleanThumbnailUrl(item.thumbnailUrl) ?: continue

            if (seenIds.add(normId) && seenTitles.add(normTitle)) {
                filtered.add(item.copy(thumbnailUrl = cleanArt))
            }
        }

        return filtered.take(15)
    }

    /**
     * Section 3: Appears On.
     * Curated official editorial playlists combining:
     * 1. Artist editorial playlists (Apple Music Essentials, Imprescindibles, Influencias, Set List, and YTM official).
     * 2. Popular editorial hit playlists (Apple Music Today's Hits / Exitos de hoy, New Music Daily, Pop Hits, etc.)
     *    resolved using the album's lead singles + primary artist.
     * Strictly filters out personal/user playlists to eliminate grey cards.
     */
    private suspend fun fetchAppearsOn(
        primaryArtist: String,
        songs: List<Song>,
        artistPage: ArtistPage?,
    ): List<ShelfItem> {
        if (primaryArtist.isBlank()) return emptyList()

        val combined = mutableListOf<ShelfItem>()
        val seenBrowseIds = mutableSetOf<String>()
        val seenTitles = mutableSetOf<String>()

        fun addIfDistinct(item: ShelfItem) {
            val bId = item.browseId.orEmpty()
            val normT = normalize(item.title)
            if (bId.isNotBlank() && normT.isNotBlank() && seenBrowseIds.add(bId) && seenTitles.add(normT)) {
                combined.add(item)
            }
        }

        // 3a. Apple Music artist editorial playlists (Essentials, Imprescindibles, Influencias, Set List)
        val artistQueries = listOf(
            "$primaryArtist Essentials",
            "$primaryArtist Imprescindibles",
            "$primaryArtist Influencias",
            "$primaryArtist Set List",
            primaryArtist,
        ).distinct()

        val artistEditorialPlaylists = mutableListOf<ShelfItem>()
        for (q in artistQueries) {
            val results = runCatching {
                AppleMusicEditorialRepository.searchPlaylists(q, limit = 8)
            }.getOrDefault(emptyList())

            for (item in results) {
                if (isJunkPlaylist(item.title)) continue
                if (!isRelevantToArtist(item.title, item.description, primaryArtist)) continue
                val cleanArt = cleanThumbnailUrl(item.thumbnailUrl) ?: continue

                artistEditorialPlaylists.add(
                    ShelfItem(
                        title = item.title,
                        subtitle = item.subtitle.ifBlank { "Apple Music" },
                        thumbnailUrl = cleanArt,
                        videoId = null,
                        browseId = "apple:playlist:${item.storefront}:${item.id}",
                        isVideo = false,
                    )
                )
            }
            if (artistEditorialPlaylists.size >= 8) break
        }

        // 3b. Popular editorial hit playlists from lead singles
        val hitEditorialPlaylists = mutableListOf<ShelfItem>()
        val leadSingles = songs.take(3)
        if (leadSingles.isNotEmpty()) {
            for (single in leadSingles) {
                val cleanSingle = extractBaseTrackTitle(single.title)
                if (cleanSingle.isBlank()) continue

                // Apple Music playlist search for this single + artist
                val appleHits = runCatching {
                    AppleMusicEditorialRepository.searchPlaylists("$cleanSingle $primaryArtist playlist", limit = 4)
                }.getOrDefault(emptyList())

                for (item in appleHits) {
                    if (isJunkPlaylist(item.title)) continue
                    val cleanArt = cleanThumbnailUrl(item.thumbnailUrl) ?: continue
                    hitEditorialPlaylists.add(
                        ShelfItem(
                            title = item.title,
                            subtitle = item.subtitle.ifBlank { "Apple Music" },
                            thumbnailUrl = cleanArt,
                            videoId = null,
                            browseId = "apple:playlist:${item.storefront}:${item.id}",
                            isVideo = false,
                        )
                    )
                }

                // YouTube Music official playlist search for this single + artist
                val ytmHits = runCatching {
                    YtMusicRepository.search("$cleanSingle $primaryArtist", SearchFilter.PLAYLISTS).getOrNull()
                        ?.filterIsInstance<SearchResult.Browse>()
                        ?.map { it.item }
                        ?.filter { it.type == BrowseType.PLAYLIST && it.browseId != null }
                        .orEmpty()
                }.getOrDefault(emptyList())

                for (item in ytmHits) {
                    val bId = item.browseId ?: continue
                    if (isJunkPlaylist(item.title) || isUserPlaylist(item.title, item.subtitle)) continue
                    if (!isOfficialYtMusicPlaylist(item.title, item.subtitle, primaryArtist)) continue
                    val cleanArt = cleanThumbnailUrl(item.thumbnailUrl) ?: continue

                    hitEditorialPlaylists.add(
                        ShelfItem(
                            title = item.title,
                            subtitle = item.subtitle.ifBlank { "YouTube Music" },
                            thumbnailUrl = cleanArt,
                            videoId = null,
                            browseId = bId,
                            isVideo = false,
                        )
                    )
                }
            }
        }

        // 3c. YouTube Music verified playlists from artist landing page (Featured on shelf)
        val featuredShelf = artistPage?.sections?.firstOrNull {
            it.title.contains("Featured on", ignoreCase = true) ||
                it.title.contains("Aparece en", ignoreCase = true) ||
                it.title.contains("Presente en", ignoreCase = true)
        }
        val ytmArtistPlaylists = (featuredShelf?.items ?: emptyList()).mapNotNull { item ->
            val bId = item.browseId ?: return@mapNotNull null
            if (item.isVideo || isJunkPlaylist(item.title) || isUserPlaylist(item.title, item.subtitle)) {
                return@mapNotNull null
            }
            if (!isOfficialYtMusicPlaylist(item, primaryArtist)) {
                return@mapNotNull null
            }
            val cleanArt = cleanThumbnailUrl(item.thumbnailUrl) ?: return@mapNotNull null
            ShelfItem(
                title = item.title,
                subtitle = item.subtitle.ifBlank { "YouTube Music" },
                thumbnailUrl = cleanArt,
                videoId = null,
                browseId = bId,
                isVideo = false,
            )
        }

        // 3d. Interleave artist editorial playlists with hit editorial playlists
        var artistIdx = 0
        var hitsIdx = 0
        var ytmIdx = 0

        while (combined.size < 12 && (artistIdx < artistEditorialPlaylists.size || hitsIdx < hitEditorialPlaylists.size || ytmIdx < ytmArtistPlaylists.size)) {
            if (artistIdx < artistEditorialPlaylists.size) {
                addIfDistinct(artistEditorialPlaylists[artistIdx++])
            }
            if (hitsIdx < hitEditorialPlaylists.size) {
                addIfDistinct(hitEditorialPlaylists[hitsIdx++])
            }
            if (ytmIdx < ytmArtistPlaylists.size) {
                addIfDistinct(ytmArtistPlaylists[ytmIdx++])
            }
            if (artistIdx < artistEditorialPlaylists.size) {
                addIfDistinct(artistEditorialPlaylists[artistIdx++])
            }
        }

        return combined.take(12)
    }

    /**
     * Section 4: You Might Also Like.
     * Similar albums from related artists (Apple Music + YouTube Music combined).
     */
    private suspend fun fetchYouMightAlsoLike(
        currentBrowseId: String,
        currentAlbumTitle: String,
        primaryArtist: String,
        artistPage: ArtistPage?,
    ): List<ShelfItem> {
        val candidates = mutableListOf<ShelfItem>()

        // 4a. Identify similar artists
        val similarShelf = artistPage?.sections?.firstOrNull {
            it.title.contains("Fans might also like", ignoreCase = true) ||
                it.title.contains("Similar artists", ignoreCase = true) ||
                it.title.contains("Artistas similares", ignoreCase = true) ||
                it.title.contains("Otros artistas", ignoreCase = true)
        }
        val similarArtists = similarShelf?.items?.map { it.title.trim() }?.filter { it.isNotBlank() }?.take(4).orEmpty()

        // 4b. Fetch top albums for similar artists on Apple Music
        for (relArtist in similarArtists) {
            val appleAlbums = runCatching {
                AppleMusicEditorialRepository.searchAlbums(relArtist, limit = 2)
            }.getOrDefault(emptyList()).mapNotNull { item ->
                val cleanArt = cleanThumbnailUrl(item.thumbnailUrl) ?: return@mapNotNull null
                ShelfItem(
                    title = item.title,
                    subtitle = item.subtitle,
                    thumbnailUrl = cleanArt,
                    videoId = null,
                    browseId = "apple:album:${item.storefront}:${item.id}",
                    isVideo = false,
                )
            }
            candidates.addAll(appleAlbums)
        }

        // 4c. YouTube Music albums for similar artists
        if (candidates.size < 6 && similarArtists.isNotEmpty()) {
            for (relArtist in similarArtists.take(2)) {
                val ytmAlbums = runCatching {
                    YtMusicRepository.search(relArtist, SearchFilter.ALBUMS).getOrNull()
                        ?.filterIsInstance<SearchResult.Browse>()
                        ?.map { it.item }
                        ?.filter { it.type == BrowseType.ALBUM && it.browseId != null }
                        ?.take(2)
                        ?.mapNotNull {
                            val cleanArt = cleanThumbnailUrl(it.thumbnailUrl) ?: return@mapNotNull null
                            ShelfItem(
                                title = it.title,
                                subtitle = it.subtitle.ifBlank { relArtist },
                                thumbnailUrl = cleanArt,
                                videoId = null,
                                browseId = it.browseId,
                                isVideo = false,
                            )
                        }.orEmpty()
                }.getOrDefault(emptyList())
                candidates.addAll(ytmAlbums)
            }
        }

        // 4d. Strict exclusion of currently viewed album and deduplication
        val normCurrentTitle = normalize(currentAlbumTitle)
        val normCurrentId = currentBrowseId.removePrefix("VL")

        val seenTitles = mutableSetOf<String>()
        val seenIds = mutableSetOf<String>()
        val filtered = mutableListOf<ShelfItem>()

        for (item in candidates) {
            val browseId = item.browseId ?: continue
            val normId = browseId.removePrefix("VL")
            if (normId.equals(normCurrentId, ignoreCase = true) || browseId.equals(currentBrowseId, ignoreCase = true)) continue

            val normTitle = normalize(item.title)
            if (normTitle.isEmpty()) continue
            if (normTitle.equals(normCurrentTitle, ignoreCase = true) || item.title.equals(currentAlbumTitle, ignoreCase = true)) continue

            val cleanArt = cleanThumbnailUrl(item.thumbnailUrl) ?: continue

            if (seenIds.add(normId) && seenTitles.add(normTitle)) {
                filtered.add(item.copy(thumbnailUrl = cleanArt))
            }
        }

        return filtered.take(12)
    }

    private fun isRelevantToArtist(playlistTitle: String, playlistDesc: String?, artistName: String): Boolean {
        val normArtist = normalize(artistName)
        if (normArtist.isBlank() || normArtist.length < 2) return false
        val normTitle = normalize(playlistTitle)
        val normDesc = normalize(playlistDesc.orEmpty())

        // Direct containment in title or description
        if (normTitle.contains(normArtist) || normDesc.contains(normArtist)) {
            return true
        }

        // For multi-word artists (e.g. "Childish Gambino", "Bad Bunny", "Arctic Monkeys")
        // require all meaningful words to appear in title or description
        val artistWords = normArtist.split(" ").filter { it.length > 2 }
        if (artistWords.size >= 2 && artistWords.all { normTitle.contains(it) || normDesc.contains(it) }) {
            return true
        }

        return false
    }

    private fun isUserPlaylist(title: String, subtitle: String): Boolean {
        val sub = subtitle.lowercase(Locale.ROOT)
        val tit = title.lowercase(Locale.ROOT)

        // View counts or song counts in subtitle (typical of user playlists in YouTube Music)
        val viewCountRegex = Regex("""\d+[\s,.]*[kKmMbB]?\s*(visualizaciones|vistas|views|reproducciones)""", RegexOption.IGNORE_CASE)
        val trackCountRegex = Regex("""\d+\s*(canciones|songs|pistas|tracks)""", RegexOption.IGNORE_CASE)
        if (viewCountRegex.containsMatchIn(sub) || trackCountRegex.containsMatchIn(sub)) {
            return true
        }

        // Typical user-playlist titles
        val userKeywords = listOf(
            "full album", "album completo", "best album", "mis canciones", "my playlist",
            "favoritas", "favorites", "tribute", "karaoke", "discografia", "discography"
        )
        if (userKeywords.any { tit.contains(it) }) {
            return true
        }

        return false
    }

    private fun isOfficialYtMusicPlaylist(title: String, subtitle: String, artistName: String): Boolean {
        val sub = subtitle.lowercase(Locale.ROOT).trim()
        val cleanArtist = normalize(artistName)

        val isOfficialCurator = sub.contains("youtube music") ||
            sub.contains("youtube") ||
            sub.contains("apple music") ||
            sub.contains("vevo") ||
            (cleanArtist.isNotEmpty() && normalize(subtitle).contains(cleanArtist))

        return isOfficialCurator && !isUserPlaylist(title, subtitle)
    }

    private fun isOfficialYtMusicPlaylist(item: ShelfItem, artistName: String): Boolean =
        isOfficialYtMusicPlaylist(item.title, item.subtitle, artistName)

    private fun isOfficialArtistVideoAuthor(
        videoAuthor: String,
        primaryArtist: String,
        trackArtists: List<String> = emptyList(),
    ): Boolean {
        val normAuthor = normalize(videoAuthor)
        if (normAuthor.isBlank()) return true

        // Discard fake / fan VEVO and third-party compilation channels
        if (normAuthor == "vevo play" || normAuthor.contains("vevo play") ||
            normAuthor.contains("vevo official channel") || normAuthor.contains("lyrics channel") ||
            normAuthor.contains("fan page")
        ) {
            return false
        }

        val allAllowed = (listOf(primaryArtist) + trackArtists).filter { it.isNotBlank() }.distinct()

        for (artist in allAllowed) {
            val normArt = normalize(artist)
            if (normArt.length < 2) continue

            // Direct containment
            if (normAuthor.contains(normArt) || normArt.contains(normAuthor)) {
                return true
            }

            // Word-level match (e.g. "Playboi Carti" in "PlayboiCartiVEVO")
            val artWords = normArt.split(" ").filter { it.length > 2 }
            if (artWords.isNotEmpty() && artWords.all { normAuthor.contains(it) }) {
                return true
            }

            // VEVO variant (e.g. "TheWeekndVEVO", "PlayboiCartiVEVO")
            if (normAuthor.contains("vevo") && artWords.any { normAuthor.contains(it) }) {
                return true
            }
        }

        // Bare VEVO channel
        if (normAuthor == "vevo" || normAuthor == "vevo music") {
            return true
        }

        // Official record labels
        val officialLabels = listOf(
            "records", "recordings", "music", "entertainment", "warner", "sony",
            "universal", "interscope", "rca", "columbia", "atlantic", "def jam",
            "republic", "xo"
        )
        if (officialLabels.any { normAuthor.contains(it) }) {
            val primaryWords = normalize(primaryArtist).split(" ").filter { it.length > 2 }
            if (primaryWords.any { normAuthor.contains(it) }) {
                return true
            }
        }

        return false
    }

    private fun isJunkOrUnofficialVideo(videoTitle: String, isLiveAllowed: Boolean, isRemixAllowed: Boolean): Boolean {
        val lower = videoTitle.lowercase(Locale.ROOT)

        // Strictly eliminate static audios
        if (lower.contains("(audio)") || lower.contains("[audio]") ||
            lower.contains("official audio") || lower.contains("audio oficial") ||
            lower.contains(" - topic") || lower.contains("audio stream") || lower.contains("full stream")
        ) {
            return true
        }

        val strictKeywords = listOf(
            "karaoke", "instrumental", "slowed", "reverb", "sped up", "speed up", "speedup",
            "nightcore", "8d audio", "8d music", "8d", "cover", "tribute", "tributo",
            "tutorial", "lesson", "guitar lesson", "bass lesson", "drum cover", "guitar cover",
            "react", "reaction", "reacción", "reaccion", "review", "resena", "reseña",
            "unboxing", "parody", "parodia", "letra", "lyrics", "lyric video", "vocal coach",
            "mashup", "compilation", "recopilacion", "recopilación", "todas las canciones",
            "album completo", "full album"
        )
        if (strictKeywords.any { lower.contains(it) }) {
            return true
        }

        if (!isRemixAllowed && (lower.contains("remix") || lower.contains("edit") || lower.contains("bootleg"))) {
            return true
        }

        if (!isLiveAllowed && (lower.contains("live") || lower.contains("en vivo") || lower.contains("en directo") || lower.contains("concierto") || lower.contains("festival"))) {
            return true
        }

        return false
    }

    private fun videoQualityScore(videoTitle: String, isVideoType: Boolean): Int {
        val lower = videoTitle.lowercase(Locale.ROOT)
        var score = 0
        if (isVideoType) score += 40

        // Prioritize official music videos
        if (lower.contains("official music video") || lower.contains("video musical oficial")) score += 100
        else if (lower.contains("official video") || lower.contains("video oficial")) score += 90
        else if (lower.contains("music video") || lower.contains("videoclip") || lower.contains("video clip")) score += 80
        else if (lower.contains("visualizer") || lower.contains("visualiser") || lower.contains("visual video")) score += 50
        else score += 30

        if (lower.contains("official audio") || lower.contains("audio oficial") || lower.contains("topic")) score -= 60
        return score
    }

    private fun isJunkPlaylist(title: String): Boolean {
        val lower = title.lowercase(Locale.ROOT)
        val junkKeywords = listOf(
            "karaoke", "sing king", "instrumental", "8d audio", "sped up", "slowed",
            "lyrics", "letra", "nightcore", "bass boosted", "tribute", "track by track",
            "video essentials", "audio commentary", "comentario de audio"
        )
        return junkKeywords.any { lower.contains(it) }
    }

    private fun normalize(text: String): String {
        val nfd = Normalizer.normalize(text, Normalizer.Form.NFD)
        return nfd.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
