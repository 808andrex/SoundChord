package com.soundchord.features.playlists.data

import android.content.Context
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.ArtistRef
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.TopPick
import com.music.bitchord.data.model.TopPickKind
import com.music.bitchord.data.model.WatchTarget
import com.soundchord.features.daypart.data.UserGenre
import com.soundchord.features.daypart.data.UserTasteDaypartEngine
import com.soundchord.features.editorial.data.AppleMusicEditorialItem
import com.soundchord.features.editorial.data.AppleMusicEditorialRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.Locale

/**
 * Repository orchestrating the "Playlists for You" (Playlists para ti) shelf (Ticket #62).
 * Composes exactly 12 compact vertical playlist cards:
 * - Card #1 (Animated Anchor): "Tus favoritos" (Favorites Mix) in violet fluid gradient.
 * - Card #2 (Animated Anchor): "Nueva música" (New Music Mix) in coral fluid gradient.
 * - Cards #3 to #12: Exclusively Apple Music editorial musical playlists selected in two groups:
 *   1. Playlists of the user's REAL genres & artists (extracted strictly from listening history & library).
 *   2. Featured Apple Music editorial playlists of the day/week (Éxitos de hoy, New Music Daily, R&B Now, Fuego, etc.).
 *   Excludes all podcasts, spoken programs, science talks, Bollywood, Fitness, and alien artists.
 */
object PlaylistsForYouRepository {

    private const val TOTAL_DYNAMIC_PLAYLISTS = 10
    private const val APPLE_MUSIC_PAGE_PREFIX = "apple:playlist:"

    private val JUNK_TERMS = listOf(
        "karaoke", "sing king", "instrumental version", "8d audio", "sped up", "slowed",
        "lyrics", "letra", "nightcore", "bass boosted", "tribute",
        "track by track", "video essentials", "audio commentary", "comentario de audio",
        "videoclips", "videoclip", "short", "shorts",
    )

    private val FITNESS_TERMS = listOf(
        "fitness", "cardio", "pure cardio", "puro cardio", "pure gym", "puro gym",
        "pure running", "puro running", "treadmill", "cycling", "spinning", "pilates",
        "hiit", "crossfit", "cinta de correr", "entreno", "entrenamiento",
        "apple music fitness", "apple music entreno", "workout", "work out",
    )

    /**
     * Non-music terms to strictly eliminate podcasts, science channels, educational talks,
     * live chats, audiobooks, and talk shows (e.g. Doctor Fisión, Domingo de Ciencia, Pláticas y Reflexiones).
     */
    private val PODCAST_AND_TALK_TERMS = listOf(
        "podcast", "podcasts", "episodio", "episodios", "episode", "episodes",
        "the wild project", "wild project", "jordi wild",
        "quantum fm", "quantum fracture", "quantumfracture",
        "ciencia", "científico", "científica", "cientifico", "fisión", "fision",
        "doctor fisión", "doctor fision", "dr. fisión", "dr fisión", "dr. fision",
        "pláticas", "platicas", "reflexiones", "otra ronda", "despejados",
        "audiobook", "audiolibro", "audiolibros", "audio commentary", "comentario de audio",
        "directo", "directos", "en directo", "live stream", "livestream",
        "charla", "charlas", "conferencia", "conferencias", "debate", "debates", "noticias", "news",
        "historia", "documental", "documentales", "curiosidades", "misterio", "terror",
        "true crime", "crimen", "investigación", "investigacion",
        "tutorial", "tutoriales", "gameplay", "gaming", "unboxing", "review",
        "reaction", "reacción", "reaccion", "vlog", "vlogs",
        "entrevista", "entrevistas", "interview", "interviews", "conversación", "conversacion",
        "meditación", "meditacion", "meditation", "mindfulness", "ruido blanco", "white noise",
        "sonidos de lluvia", "rain sounds", "dormir", "sleep sounds", "relaxing sounds",
        "hablamos de", "en vivo", "programa de", "programas", "temporada", "season",
        "talk show", "talkshow", "radio show", "radioshow", "tertulia", "tertulias",
        "monólogo", "monologo", "stand up", "stand-up", "comedia en vivo",
        "audiodrama", "audiorelatos", "relatos", "audiocuento", "audiocuentos",
    )

    /**
     * Unwanted regional genres outside the user's music sphere (Bollywood, Indian, Pakistani).
     */
    private val UNWANTED_REGIONAL_TERMS = listOf(
        "bollywood", "indian", "punjabi", "hindi", "desi", "tollywood", "tamil", "telugu",
        "bhangra", "ghazal", "qawwali", "marathi", "bhojpuri", "kannada", "malayalam",
        "mohd. rafi", "mohd rafi", "mohammed rafi", "lata mangeshkar", "arijit singh",
        "alka yagnik", "kishore kumar", "udit narayan", "asha bhosle", "kumar sanu",
    )

    fun init(context: Context) {
        PlaylistsDatabaseCache.init(context)
        AppleMusicEditorialRepository.init(context)
    }

    private fun userKey(identity: String?): String = identity?.ifBlank { "guest" } ?: "guest"

    /**
     * Immediate 0 ms read from SQLite cache for the current 12-hour slot.
     */
    fun getCachedPlaylists(
        identity: String?,
        time: LocalDateTime = LocalDateTime.now(),
    ): List<TopPick>? {
        val slot = PlaylistsDatabaseCache.getSlotKey(time)
        val cached = PlaylistsDatabaseCache.get(userKey(identity), slot) ?: return null
        if (cached.size < 12) return null
        val anchorsValid = cached[0].kind == TopPickKind.FAVORITES_MIX && cached[1].kind == TopPickKind.NEW_RELEASE_MIX
        if (!anchorsValid) return null
        val dynamicValid = cached.drop(2).all { pick ->
            val bId = pick.browseId.orEmpty()
            val isApple = bId.startsWith(APPLE_MUSIC_PAGE_PREFIX) || bId.startsWith("apple:") || bId.startsWith("am:") || bId.startsWith("pl.")
            val isNotYt = !bId.startsWith("VL") && !bId.startsWith("RD") && !bId.startsWith("MP") && !bId.startsWith("PL") && !bId.startsWith("UC")
            val hasValidArtwork = !pick.images.firstOrNull().isNullOrBlank() &&
                pick.images.firstOrNull()?.startsWith("https://") == true &&
                pick.images.firstOrNull()?.contains("{") == false &&
                pick.images.firstOrNull()?.contains("}") == false
            val isNotPodcast = !isNonMusicOrPodcast(pick.title) && !isNonMusicOrPodcast(pick.subtitle)
            isApple && isNotYt && hasValidArtwork && isNotPodcast && pick.kind == TopPickKind.EDITORIAL_PLAYLIST
        }
        return if (dynamicValid) cached else null
    }

    /**
     * Resolves verified Apple Music editorial playlists for Hero "Top selecciones para ti" (Ticket #75).
     * Sourced directly from the verified Playlists for You SQLite cache and Apple Music editorial discovery feed,
     * rotated deterministically by day and 2-hour slot (epochDay + franjaHorariaIndex).
     * Guaranteed to have valid HTTPS artwork and resolvable Apple Music IDs.
     */
    fun getVerifiedEditorialPlaylistsForHero(
        now: LocalDateTime = LocalDateTime.now(),
        limit: Int = 2,
    ): List<TopPick> {
        val verifiedPool = mutableListOf<TopPick>()

        // 1. Gather all verified Apple Music editorial playlists from PlaylistsDatabaseCache
        val cachedFromDb = PlaylistsDatabaseCache.getAllVerifiedPlaylists()
        for (pick in cachedFromDb) {
            val art = pick.images.firstOrNull()
            if (!art.isNullOrBlank() &&
                art.startsWith("https://") &&
                !art.contains("{") &&
                !art.contains("}") &&
                isStrictlyValidAppleMusicPlaylist(pick.title, pick.subtitle, pick.browseId, art, emptyList(), pick.subtitle)
            ) {
                verifiedPool.add(pick)
            }
        }

        // 2. Supplement from pre-cached editorial discovery feed if pool is small
        if (verifiedPool.size < limit * 3) {
            val discoveryItems = AppleMusicEditorialRepository.getCachedDiscoveryPlaylists()
            val sf = AppleMusicEditorialRepository.defaultStorefront()
            for (item in discoveryItems) {
                val art = item.thumbnailUrl
                if (!art.isNullOrBlank() &&
                    art.startsWith("https://") &&
                    !art.contains("{") &&
                    !art.contains("}") &&
                    isStrictlyValidAppleMusicPlaylist(item.title, item.subtitle, item.id, art, emptyList(), item.subtitle)
                ) {
                    val itemSf = item.storefront.ifBlank { sf }
                    val fullId = if (item.id.contains(":")) item.id else "$itemSf:${item.id}"
                    verifiedPool.add(
                        TopPick(
                            kind = TopPickKind.EDITORIAL_PLAYLIST,
                            title = item.title,
                            subtitle = item.subtitle.ifBlank { "Apple Music" },
                            images = listOf(art),
                            browseId = "$APPLE_MUSIC_PAGE_PREFIX$fullId",
                        ),
                    )
                }
            }
        }

        val cleanPool = verifiedPool.distinctBy { it.browseId ?: it.title }
        if (cleanPool.isEmpty()) return emptyList()

        val epochDay = now.toLocalDate().toEpochDay()
        val franjaHorariaIndex = (now.hour / 2).toLong()
        val combinedSeed = epochDay + franjaHorariaIndex

        val rotated = rotatePool(cleanPool, combinedSeed.toInt())
        return rotated.take(limit)
    }

    suspend fun getPlaylistsForYou(
        homeShelves: List<HomeShelf>,
        editorialShelves: List<HomeShelf> = emptyList(),
        identity: String?,
        topPicks: List<TopPick>? = null,
        localArtists: List<ArtistRef> = emptyList(),
        localSongs: List<Song> = emptyList(),
        userArtists: List<String> = emptyList(),
        forceRefresh: Boolean = false,
        now: LocalDateTime = LocalDateTime.now(),
    ): List<TopPick> = withContext(Dispatchers.IO) {
        val key = userKey(identity)
        val currentSlot = PlaylistsDatabaseCache.getSlotKey(now)

        if (!forceRefresh) {
            val cached = getCachedPlaylists(identity, now)
            if (!cached.isNullOrEmpty() && cached.size >= 12) {
                return@withContext cached
            }
        }

        // 12-hour rotation: prune previous time slots
        PlaylistsDatabaseCache.prunePreviousSlots(currentSlot)

        val resolved = coroutineScope {
            // 1. Gather account mixes and real listening history
            val mixesDeferred = async { findMixes(homeShelves) }
            val historyDeferred = async { runCatching { YtMusicRepository.recentHistory().getOrNull() }.getOrNull().orEmpty() }

            val mixes = mixesDeferred.await()
            val history = historyDeferred.await()

            // Extract real artists strictly from user history, library songs, and local onboarding artists
            val historyArtists = history.mapNotNull { song ->
                song.artist.takeIf { it.isNotBlank() }
            }.flatMap { rawArtist ->
                rawArtist.split(",", "&", "ft.", "feat.", " y ", " e ", "/", " x ", " X ").map { it.trim() }
            }.filter { candidate ->
                candidate.isNotBlank() && candidate.length > 1 &&
                    !isUnwantedRegional(candidate) && !isNonMusicOrPodcast(candidate)
            }.distinct()

            val libraryArtists = (localSongs.map { it.artist } + localArtists.map { it.name })
                .flatMap { rawArtist ->
                    rawArtist.split(",", "&", "ft.", "feat.", " y ", " e ", "/", " x ", " X ").map { it.trim() }
                }.filter { candidate ->
                    candidate.isNotBlank() && candidate.length > 1 &&
                        !isUnwantedRegional(candidate) && !isNonMusicOrPodcast(candidate)
                }.distinct()

            // User's actual confirmed artists (incorporating userArtists resolved by UserArtistPoolResolver)
            val userRealArtists = (userArtists + historyArtists + libraryArtists).filter { candidate ->
                candidate.isNotBlank() && candidate.length > 1 &&
                    !isUnwantedRegional(candidate) && !isNonMusicOrPodcast(candidate)
            }.distinct()

            // Infer active listening genres from what the listener actually played
            val inferredGenres = userRealArtists.mapNotNull { UserGenre.fromArtist(it) }.distinct()
            val primaryGenre = inferredGenres.firstOrNull() ?: UserGenre.DEFAULT

            // 2. Card #1: Favorites Mix Anchor
            val card1 = resolveFavoritesMix(topPicks, mixes, history, localSongs, userRealArtists)

            // 3. Card #2: New Music Mix Anchor
            val card2 = resolveNewReleaseMix(topPicks, mixes, history, userRealArtists)

            // 4. Cards #3 to #12: Exclusively Apple Music editorial musical playlists
            val dynamicPlaylists = resolveDynamicAppleMusicPlaylists(
                editorialShelves = editorialShelves,
                userGenre = primaryGenre,
                inferredGenres = inferredGenres,
                userRealArtists = userRealArtists,
                now = now,
            )

            listOf(card1, card2) + dynamicPlaylists.take(TOTAL_DYNAMIC_PLAYLISTS)
        }

        if (resolved.size >= 10) {
            PlaylistsDatabaseCache.put(key, currentSlot, resolved)
        }

        resolved
    }

    private fun resolveFavoritesMix(
        topPicks: List<TopPick>?,
        mixes: Map<MixType, ShelfItem>,
        history: List<Song>,
        localSongs: List<Song>,
        topArtists: List<String>,
    ): TopPick {
        topPicks?.firstOrNull { it.kind == TopPickKind.FAVORITES_MIX }?.let { return it }

        val watch = mixes[MixType.FAVORITES]?.watchTarget()
            ?: mixes[MixType.SUPERMIX]?.watchTarget()
            ?: (localSongs.firstOrNull() ?: history.firstOrNull())?.let { song ->
                WatchTarget(playlistId = "RDAMVM${song.videoId}", videoId = song.videoId)
            }

        val artists = topArtists.filter { it.isNotBlank() }.distinct().take(3)
        val isSpanish = Locale.getDefault().language == "es"
        val prefix = if (isSpanish) "Con" else "Featuring"
        val subtitle = if (artists.isNotEmpty()) "$prefix ${artists.joinToString(", ")}" else ""

        return TopPick(
            kind = TopPickKind.FAVORITES_MIX,
            title = "",
            subtitle = subtitle,
            watch = watch,
        )
    }

    private fun resolveNewReleaseMix(
        topPicks: List<TopPick>?,
        mixes: Map<MixType, ShelfItem>,
        history: List<Song>,
        topArtists: List<String>,
    ): TopPick {
        topPicks?.firstOrNull { it.kind == TopPickKind.NEW_RELEASE_MIX }?.let { return it }

        val watch = mixes[MixType.NEW_RELEASE]?.watchTarget()
            ?: (history.firstOrNull())?.let { song ->
                WatchTarget(playlistId = "RDAMVM${song.videoId}", videoId = song.videoId)
            }

        val artists = topArtists.filter { it.isNotBlank() }.distinct().take(3)
        val isSpanish = Locale.getDefault().language == "es"
        val prefix = if (isSpanish) "Con" else "Featuring"
        val subtitle = if (mixes[MixType.NEW_RELEASE]?.subtitle?.isNotBlank() == true) {
            mixes[MixType.NEW_RELEASE]!!.subtitle
        } else if (artists.isNotEmpty()) {
            "$prefix ${artists.joinToString(", ")}"
        } else {
            ""
        }

        return TopPick(
            kind = TopPickKind.NEW_RELEASE_MIX,
            title = "",
            subtitle = subtitle,
            watch = watch,
        )
    }

    /**
     * Resolves cards #3 to #12 exclusively from Apple Music editorial playlists:
     * - Group 1: Editorial playlists of the user's real genres & real artists (only artists in user's history/library).
     * - Group 2: Featured Apple Music editorial playlists of the day/week (Éxitos de hoy, New Music Daily, R&B Now, Fuego, etc.).
     */
    private suspend fun resolveDynamicAppleMusicPlaylists(
        editorialShelves: List<HomeShelf>,
        userGenre: UserGenre,
        inferredGenres: List<UserGenre>,
        userRealArtists: List<String>,
        now: LocalDateTime,
    ): List<TopPick> = coroutineScope {
        val storefront = AppleMusicEditorialRepository.defaultStorefront()
        val isSpanish = Locale.getDefault().language == "es"
        val featuredBadge = if (isSpanish) "Playlist destacada · Apple Music" else "Featured Playlist · Apple Music"

        val featuredPlaylists = mutableListOf<TopPick>()
        val userTastePlaylists = mutableListOf<TopPick>()

        // --------------------------------------------------------------------
        // Group 2: Featured Apple Music Editorial Playlists of the Day/Week
        // --------------------------------------------------------------------
        val flagshipTerms = when (userGenre) {
            UserGenre.CUMBIA -> listOf("Cumbia colombiana", "Fiesta Tropical", "¡Dale Play!", "Éxitos de hoy")
            UserGenre.SALSA -> listOf("Salsa clásica", "Salsa sensual", "¡Dale Play!", "Éxitos de hoy")
            UserGenre.VALLENATO -> listOf("Vallenato clásico", "Vallenato de oro", "¡Dale Play!", "Éxitos de hoy")
            UserGenre.REGIONAL_POPULAR -> listOf("Música Popular", "Éxitos de cantina", "Despecho", "Éxitos de hoy")
            UserGenre.BOLERO_BALADA -> listOf("Boleros del recuerdo", "Baladas románticas", "Éxitos del recuerdo", "Éxitos de hoy")
            UserGenre.ROCK_EN_ESPANOL -> listOf("Rock en español", "Rock en tu idioma", "Rock latino", "Éxitos de hoy")
            UserGenre.LATIN_POP -> listOf("Pop latino", "Baladas pop", "¡Dale Play!", "Éxitos de hoy")
            UserGenre.URBANO -> listOf("Fuego", "Dale Reggaetón", "¡Dale Play!", "Éxitos de hoy")
            UserGenre.RNB -> listOf("R&B Now", "After Hours", "New Music Daily", "Today's Hits")
            UserGenre.HIPHOP -> listOf("Trap Kingz", "Rap Life", "New Music Daily", "Today's Hits")
            UserGenre.POP -> listOf("A-List Pop", "Éxitos de hoy", "New Music Daily", "Today's Hits")
            UserGenre.INDIE_ROCK -> listOf("Indie Anthems", "Alt CTRL", "New Music Daily", "Today's Hits")
            UserGenre.ELECTRONIC -> listOf("danceXL", "Club Room", "New Music Daily", "Today's Hits")
            UserGenre.LATIN -> listOf("¡Dale Play!", "Fuego", "Vibras", "Éxitos de hoy")
            else -> listOf("¡Dale Play!", "Éxitos de hoy", "New Music Daily", "Fuego")
        }

        val featuredDeferred = flagshipTerms.map { term ->
            async {
                runCatching {
                    AppleMusicEditorialRepository.searchPlaylists(
                        query = term,
                        limit = 4,
                        storefront = storefront,
                        excludeFitness = true,
                    )
                }.getOrDefault(emptyList())
            }
        }

        // --------------------------------------------------------------------
        // Group 1: Editorial Playlists of User's REAL Genres & Artists
        // --------------------------------------------------------------------
        // Only query essentials for artists that ACTUALLY exist in the user's real listening history
        val topRealArtists = userRealArtists.take(4)
        val artistDeferred = topRealArtists.map { artist ->
            async {
                runCatching {
                    AppleMusicEditorialRepository.searchPlaylists(
                        query = "$artist essentials",
                        limit = 4,
                        storefront = storefront,
                        excludeFitness = true,
                    )
                }.getOrDefault(emptyList())
            }
        }

        val activeGenres = (inferredGenres.take(2).ifEmpty { listOf(userGenre) }).distinct()
        val genreDeferred = activeGenres.map { genre ->
            val genreQuery = when (genre) {
                UserGenre.CUMBIA -> "Cumbia"
                UserGenre.SALSA -> "Salsa"
                UserGenre.VALLENATO -> "Vallenato"
                UserGenre.REGIONAL_POPULAR -> "Música Popular"
                UserGenre.BOLERO_BALADA -> "Boleros"
                UserGenre.ROCK_EN_ESPANOL -> "Rock en Español"
                UserGenre.LATIN_POP -> "Pop Latino"
                UserGenre.URBANO -> "Urbano Latino"
                UserGenre.RNB -> "R&B Soul"
                UserGenre.HIPHOP -> "Hip-Hop"
                UserGenre.POP -> "Pop Hits"
                UserGenre.ELECTRONIC -> "Dance Electronic"
                UserGenre.INDIE_ROCK -> "Indie Rock"
                UserGenre.LATIN -> "Música Latina"
                else -> "Pop Hits"
            }
            async {
                runCatching {
                    AppleMusicEditorialRepository.searchPlaylists(
                        query = "$genreQuery playlist",
                        limit = 6,
                        storefront = storefront,
                        excludeFitness = true,
                    )
                }.getOrDefault(emptyList())
            }
        }

        val featuredResults = featuredDeferred.flatMap { it.await() }
        val artistResults = artistDeferred.flatMap { it.await() }
        val genreResults = genreDeferred.flatMap { it.await() }

        // Process Group 2: Featured Playlists
        featuredResults.forEach { item ->
            if (isStrictlyValidAppleMusicPlaylist(item.title, item.subtitle, item.id, item.thumbnailUrl, userRealArtists, curatorName = item.subtitle)) {
                val itemSf = item.storefront.ifBlank { storefront }
                val fullId = if (item.id.contains(":")) item.id else "$itemSf:${item.id}"
                featuredPlaylists.add(
                    TopPick(
                        kind = TopPickKind.EDITORIAL_PLAYLIST,
                        title = item.title,
                        subtitle = featuredBadge,
                        images = listOfNotNull(item.thumbnailUrl),
                        browseId = "$APPLE_MUSIC_PAGE_PREFIX$fullId",
                    ),
                )
            }
        }

        // Also add from pre-cached editorial shelves if matching strict criteria
        editorialShelves.flatMap { it.items }.forEach { item ->
            val bId = item.browseId ?: return@forEach
            val isAppleId = bId.startsWith("apple:") || bId.startsWith("am:") || bId.startsWith("pl.")
            val isNotYt = !bId.startsWith("VL") && !bId.startsWith("RD") && !bId.startsWith("MP") && !bId.startsWith("PL") && !bId.startsWith("UC")
            if (isAppleId && isNotYt) {
                if (isStrictlyValidAppleMusicPlaylist(item.title, item.subtitle, bId, item.thumbnailUrl, userRealArtists, curatorName = item.subtitle)) {
                    val normalizedId = if (bId.startsWith(APPLE_MUSIC_PAGE_PREFIX)) bId else if (bId.startsWith("apple:")) bId else "$APPLE_MUSIC_PAGE_PREFIX$bId"
                    featuredPlaylists.add(
                        TopPick(
                            kind = TopPickKind.EDITORIAL_PLAYLIST,
                            title = item.title,
                            subtitle = featuredBadge,
                            images = listOfNotNull(item.thumbnailUrl),
                            browseId = normalizedId,
                        ),
                    )
                }
            }
        }

        // Process Group 1: User's Real Artists (must match real artist name)
        artistResults.forEach { item ->
            val matchesRealArtist = topRealArtists.any { real ->
                item.title.contains(real, ignoreCase = true) || item.subtitle.contains(real, ignoreCase = true)
            }
            if (matchesRealArtist && isStrictlyValidAppleMusicPlaylist(item.title, item.subtitle, item.id, item.thumbnailUrl, userRealArtists, curatorName = item.subtitle)) {
                val itemSf = item.storefront.ifBlank { storefront }
                val fullId = if (item.id.contains(":")) item.id else "$itemSf:${item.id}"
                userTastePlaylists.add(
                    TopPick(
                        kind = TopPickKind.EDITORIAL_PLAYLIST,
                        title = item.title,
                        subtitle = item.subtitle.ifBlank { "Apple Music" },
                        images = listOfNotNull(item.thumbnailUrl),
                        browseId = "$APPLE_MUSIC_PAGE_PREFIX$fullId",
                    ),
                )
            }
        }

        // Process Group 1: User's Real Genres
        genreResults.forEach { item ->
            if (isStrictlyValidAppleMusicPlaylist(item.title, item.subtitle, item.id, item.thumbnailUrl, userRealArtists, curatorName = item.subtitle)) {
                val itemSf = item.storefront.ifBlank { storefront }
                val fullId = if (item.id.contains(":")) item.id else "$itemSf:${item.id}"
                userTastePlaylists.add(
                    TopPick(
                        kind = TopPickKind.EDITORIAL_PLAYLIST,
                        title = item.title,
                        subtitle = item.subtitle.ifBlank { "Apple Music" },
                        images = listOfNotNull(item.thumbnailUrl),
                        browseId = "$APPLE_MUSIC_PAGE_PREFIX$fullId",
                    ),
                )
            }
        }

        // Deduplicate distinct playlists by browseId or title
        val cleanFeatured = featuredPlaylists.distinctBy { it.browseId ?: it.title }
        val cleanUserTaste = userTastePlaylists.distinctBy { it.browseId ?: it.title }

        // 12-hour rotation seed: (dayOfYear * 2) + (0 for AM, 1 for PM)
        val slotSeed = (now.dayOfYear * 2) + (now.hour / 12)
        val rotatedFeatured = rotatePool(cleanFeatured, slotSeed)
        val rotatedUserTaste = rotatePool(cleanUserTaste, slotSeed + 7)

        // Combine both groups: ~5 Featured + ~5 User Taste, alternating smoothly
        val final10 = mutableListOf<TopPick>()
        val featuredIter = rotatedFeatured.iterator()
        val tasteIter = rotatedUserTaste.iterator()

        var step = 0
        while (final10.size < TOTAL_DYNAMIC_PLAYLISTS && (featuredIter.hasNext() || tasteIter.hasNext())) {
            if (step % 2 == 0 && featuredIter.hasNext()) {
                final10.add(featuredIter.next())
            } else if (tasteIter.hasNext()) {
                final10.add(tasteIter.next())
            } else if (featuredIter.hasNext()) {
                final10.add(featuredIter.next())
            }
            step++
        }

        // If shortfall, fill from remaining verified Apple Music items
        if (final10.size < TOTAL_DYNAMIC_PLAYLISTS) {
            val remaining = (rotatedFeatured + rotatedUserTaste).distinctBy { it.browseId ?: it.title }
            for (candidate in remaining) {
                if (final10.none { it.browseId == candidate.browseId }) {
                    final10.add(candidate)
                    if (final10.size >= TOTAL_DYNAMIC_PLAYLISTS) break
                }
            }
        }

        // If still shortfall (rare: fresh install / network glitch), pull from pre-cached Apple Music Discovery items
        if (final10.size < TOTAL_DYNAMIC_PLAYLISTS) {
            val discoveryShelves = AppleMusicEditorialRepository.getHomeDiscoveryShelves()
            discoveryShelves.flatMap { it.items }.forEach { item ->
                if (final10.size >= TOTAL_DYNAMIC_PLAYLISTS) return@forEach
                val bId = item.browseId ?: return@forEach
                val isApple = bId.startsWith("apple:") || bId.startsWith("am:") || bId.startsWith("pl.")
                val isNotYt = !bId.startsWith("VL") && !bId.startsWith("RD") && !bId.startsWith("MP") && !bId.startsWith("PL")
                if (isApple && isNotYt && isStrictlyValidAppleMusicPlaylist(item.title, item.subtitle, bId, item.thumbnailUrl, userRealArtists, curatorName = item.subtitle)) {
                    val normalizedId = if (bId.startsWith(APPLE_MUSIC_PAGE_PREFIX)) bId else if (bId.startsWith("apple:")) bId else "$APPLE_MUSIC_PAGE_PREFIX$bId"
                    if (final10.none { it.browseId == normalizedId }) {
                        final10.add(
                            TopPick(
                                kind = TopPickKind.EDITORIAL_PLAYLIST,
                                title = item.title,
                                subtitle = featuredBadge,
                                images = listOfNotNull(item.thumbnailUrl),
                                browseId = normalizedId,
                            ),
                        )
                    }
                }
            }
        }

        final10.take(TOTAL_DYNAMIC_PLAYLISTS)
    }

    private fun <T> rotatePool(list: List<T>, seed: Int): List<T> {
        if (list.size <= 1) return list
        val offset = (seed * 3) % list.size
        val shifted = list.drop(offset) + list.take(offset)
        return shifted.sortedBy { (it.hashCode() xor (seed * 37)).rem(1000) }
    }

    private fun isUnwantedRegional(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return UNWANTED_REGIONAL_TERMS.any { lower.contains(it) }
    }

    private fun isNonMusicOrPodcast(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return PODCAST_AND_TALK_TERMS.any { lower.contains(it) }
    }

    /**
     * Strict filter ensuring cards are EXCLUSIVELY musical Apple Music editorial playlists.
     * Excludes:
     * - Any non-Apple Music ID (rejects YouTube VL..., RD..., PL..., MP..., UC...)
     * - Any podcast, spoken program, or science talk (e.g. The Wild Project, Quantum FM, Doctor Fisión)
     * - Invalid or unverified curatorName (must be official Apple Music editorial curator)
     * - Invalid, missing, or template artworkUrl
     * - Indian / Bollywood / Punjabi playlists
     * - Apple Music Fitness playlists
     * - Artist Essentials playlists where the artist is NOT in the user's real listening history
     * - Albums, singles, and junk terms
     */
    fun isStrictlyValidAppleMusicPlaylist(
        title: String,
        subtitle: String,
        idOrBrowseId: String?,
        thumbnailUrl: String?,
        userRealArtists: List<String>,
        curatorName: String? = null,
    ): Boolean {
        // 1. Strict artwork validation: must be secure https and without unresolved template braces
        if (thumbnailUrl.isNullOrBlank() || !thumbnailUrl.startsWith("https://") || thumbnailUrl.contains("{") || thumbnailUrl.contains("}")) return false

        // 2. Strict ID validation: Reject any YouTube Music origin and require Apple Music prefix/format
        val rawId = idOrBrowseId?.trim().orEmpty()
        if (rawId.isBlank()) return false
        if (rawId.startsWith("VL") || rawId.startsWith("RD") || rawId.startsWith("PL") || rawId.startsWith("MP") || rawId.startsWith("UC") || rawId.startsWith("OLAK")) {
            return false
        }
        val isAppleId = rawId.startsWith(APPLE_MUSIC_PAGE_PREFIX) || rawId.startsWith("apple:") || rawId.startsWith("am:") || rawId.startsWith("pl.")
        if (!isAppleId) return false

        val t = title.lowercase(Locale.ROOT)
        val s = subtitle.lowercase(Locale.ROOT)
        val c = (curatorName ?: subtitle).lowercase(Locale.ROOT)

        // 3. Exclude podcasts, talk shows, science channels, educational talks, interviews, and spoken audio
        if (isNonMusicOrPodcast(t) || isNonMusicOrPodcast(s) || isNonMusicOrPodcast(c)) return false

        // 4. Strict curator validation: Must be verified Apple Music editorial catalog
        val isAppleCurator = c.contains("apple music") || c == "apple" || c.startsWith("apple")
        if (!isAppleCurator) return false

        // 5. Exclude unwanted Indian / Bollywood content
        if (isUnwantedRegional(t) || isUnwantedRegional(s) || isUnwantedRegional(c)) return false

        // 6. Exclude fitness
        if (FITNESS_TERMS.any { t.contains(it) || s.contains(it) || c.contains(it) }) return false

        // 7. Exclude singles, albums, EPs
        if (t.contains("single") || t.contains("sencillo") || s.contains("single") || s.contains("sencillo")) return false
        if (t.contains("album") || t.contains("álbum") || s.contains("album") || s.contains("álbum")) return false
        if (t.contains(" ep") || s.contains(" ep") || t.endsWith("ep") || s.endsWith("ep")) return false

        // 8. Exclude junk terms
        if (JUNK_TERMS.any { t.contains(it) || s.contains(it) || c.contains(it) }) return false

        // 9. Exclude artist essentials of artists that the user has NEVER listened to
        val isArtistEssentials = t.contains("essentials") || t.contains("imprescindibles") || t.contains("next steps")
        if (isArtistEssentials) {
            val matchesUserHistory = userRealArtists.any { real ->
                t.contains(real.lowercase(Locale.ROOT))
            }
            if (!matchesUserHistory) return false
        }

        return true
    }

    private enum class MixType { SUPERMIX, DISCOVER, NEW_RELEASE, FAVORITES }

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
                "new release" in title || "nueva música" in title || "novedades" in title -> MixType.NEW_RELEASE
                "favorite" in title || "favorit" in title -> MixType.FAVORITES
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
}
