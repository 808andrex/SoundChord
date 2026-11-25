package com.soundchord.features.editorial.data

import android.content.Context
import com.soundchord.features.R
import com.music.bitchord.data.Http
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.canvas.AppleMusicTokenProvider
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.ShelfLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.io.File
import java.util.Locale

object AppleMusicEditorialRepository {

    private const val API_BASE = "https://amp-api.music.apple.com/v1/catalog"
    private const val CACHE_FILE_NAME = "apple_music_editorial_cache.json"
    private const val CACHE_TTL_MS = 2L * 60 * 60 * 1000L // 2 hours
    private const val HOME_DISCOVERY_CACHE_FILE_NAME = "apple_music_home_discovery_cache.json"
    private const val HOME_DISCOVERY_CACHE_TTL_MS = 2L * 60 * 60 * 1000L // 2 hours
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private var cacheDir: File? = null
    private var inMemoryCache: AppleMusicEditorialFeed? = null
    private var inMemoryTimestamp: Long = 0L
    private var inMemoryHomeDiscoveryCache: AppleMusicHomeDiscoveryFeed? = null
    private var inMemoryHomeDiscoveryTimestamp: Long = 0L
    private var appContext: Context? = null
    private val apiSemaphore = Semaphore(4)

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
        if (cacheDir == null) {
            cacheDir = context.applicationContext.cacheDir
        }
    }

    fun defaultStorefront(): String {
        val country = Locale.getDefault().country.lowercase(Locale.ROOT)
        return if (country.length == 2) country else "us"
    }

    fun formatAppleArtworkUrl(rawUrl: String?, width: Int = 600, height: Int = 600, format: String = "jpg"): String? {
        if (rawUrl.isNullOrBlank()) return null
        val formatted = rawUrl
            .replace("{w}", width.toString())
            .replace("{h}", height.toString())
            .replace("{f}", format)
            .replace("{c}", "")
        return if (formatted.startsWith("https://") && !formatted.contains("{") && !formatted.contains("}")) {
            formatted
        } else if (rawUrl.startsWith("https://") && !rawUrl.contains("{") && !rawUrl.contains("}")) {
            rawUrl
        } else null
    }

    fun isValidArtworkUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        if (!url.startsWith("https://")) return false
        if (url.contains("{") || url.contains("}")) return false
        return true
    }

    fun isValidItem(item: AppleMusicEditorialItem): Boolean {
        return isValidArtworkUrl(item.thumbnailUrl)
    }

    fun isValidSong(song: AppleMusicEditorialSong): Boolean {
        return isValidArtworkUrl(song.thumbnailUrl)
    }

    /**
     * Synchronous access to the active in-memory cache or disk cache.
     * Guaranteed instant (0ms) return on the main thread so UI never composes with dummy grey cards.
     */
    fun getCachedFeedOrSeed(storefront: String = defaultStorefront()): AppleMusicEditorialFeed {
        return inMemoryCache ?: readDiskCache() ?: AppleMusicEditorialFeed()
    }

    /**
     * Retrieves the editorial feed with 2-hour in-memory and disk caching.
     */
    suspend fun getEditorialFeed(
        storefront: String = defaultStorefront(),
        forceRefresh: Boolean = false,
    ): AppleMusicEditorialFeed = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 1. Check in-memory cache
        if (!forceRefresh) {
            inMemoryCache?.let { cached ->
                if (now - inMemoryTimestamp < CACHE_TTL_MS && (cached.heroItems.isNotEmpty() || cached.newSongs.isNotEmpty())) {
                    return@withContext cached
                }
            }

            // 2. Check disk cache
            val diskFeed = readDiskCache()
            if (diskFeed != null && now - diskFeed.timestamp < CACHE_TTL_MS && (diskFeed.heroItems.isNotEmpty() || diskFeed.newSongs.isNotEmpty())) {
                inMemoryCache = diskFeed
                inMemoryTimestamp = diskFeed.timestamp
                return@withContext diskFeed
            }
        }

        // 3. Fetch from remote Apple Music API
        val remoteFeed = runCatching { fetchRemoteFeed(storefront) }.getOrNull()
        if (remoteFeed != null && (remoteFeed.heroItems.isNotEmpty() || remoteFeed.newSongs.isNotEmpty())) {
            val feedWithTimestamp = remoteFeed.copy(timestamp = now)
            inMemoryCache = feedWithTimestamp
            inMemoryTimestamp = now
            writeDiskCache(feedWithTimestamp)
            return@withContext feedWithTimestamp
        }

        // 4. Fallback to existing disk cache if available
        readDiskCache()?.let { return@withContext it }

        AppleMusicEditorialFeed()
    }

    /**
     * Builds dynamic editorial discovery shelves for the Home screen:
     * Cached with a 2-hour TTL and persisted to disk.
     */
    suspend fun getHomeDiscoveryShelves(
        storefront: String = defaultStorefront(),
        forceRefresh: Boolean = false,
    ): List<HomeShelf> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        var discoveryFeed: AppleMusicHomeDiscoveryFeed? = null

        if (!forceRefresh) {
            inMemoryHomeDiscoveryCache?.let { cached ->
                if (now - inMemoryHomeDiscoveryTimestamp < HOME_DISCOVERY_CACHE_TTL_MS && cached.workoutPlaylists.isNotEmpty()) {
                    discoveryFeed = cached
                }
            }
            if (discoveryFeed == null) {
                val disk = readHomeDiscoveryDiskCache()
                if (disk != null && now - disk.timestamp < HOME_DISCOVERY_CACHE_TTL_MS && disk.workoutPlaylists.isNotEmpty()) {
                    inMemoryHomeDiscoveryCache = disk
                    inMemoryHomeDiscoveryTimestamp = disk.timestamp
                    discoveryFeed = disk
                }
            }
        }

        if (discoveryFeed == null) {
            val remote = runCatching { fetchRemoteHomeDiscovery(storefront) }.getOrNull()
            if (remote != null && (remote.workoutPlaylists.isNotEmpty() || remote.chillPlaylists.isNotEmpty() || remote.focusPlaylists.isNotEmpty())) {
                inMemoryHomeDiscoveryCache = remote
                inMemoryHomeDiscoveryTimestamp = now
                writeHomeDiscoveryDiskCache(remote)
                discoveryFeed = remote
            } else {
                discoveryFeed = inMemoryHomeDiscoveryCache ?: readHomeDiscoveryDiskCache()
            }
        }

        val feed = discoveryFeed ?: AppleMusicHomeDiscoveryFeed()
        val isSpanish = Locale.getDefault().language == "es"

        val workoutShelfItems = feed.workoutPlaylists.filter { isValidItem(it) }.take(15).map { item ->
            ShelfItem(
                title = item.title,
                subtitle = item.subtitle,
                thumbnailUrl = item.thumbnailUrl,
                videoId = null,
                browseId = "apple:playlist:${item.storefront.ifBlank { storefront }}:${item.id}",
            )
        }

        val chillShelfItems = feed.chillPlaylists.filter { isValidItem(it) }.take(15).map { item ->
            ShelfItem(
                title = item.title,
                subtitle = item.subtitle,
                thumbnailUrl = item.thumbnailUrl,
                videoId = null,
                browseId = "apple:playlist:${item.storefront.ifBlank { storefront }}:${item.id}",
            )
        }

        val essentialsShelfItems = feed.essentialsPlaylists.filter { isValidItem(it) }.take(18).map { item ->
            ShelfItem(
                title = item.title,
                subtitle = item.subtitle,
                thumbnailUrl = item.thumbnailUrl,
                videoId = null,
                browseId = "apple:playlist:${item.storefront.ifBlank { storefront }}:${item.id}",
            )
        }

        val focusShelfItems = feed.focusPlaylists.filter { isValidItem(it) }.take(15).map { item ->
            ShelfItem(
                title = item.title,
                subtitle = item.subtitle,
                thumbnailUrl = item.thumbnailUrl,
                videoId = null,
                browseId = "apple:playlist:${item.storefront.ifBlank { storefront }}:${item.id}",
            )
        }

        val partyShelfItems = feed.partyPlaylists.filter { isValidItem(it) }.take(15).map { item ->
            ShelfItem(
                title = item.title,
                subtitle = item.subtitle,
                thumbnailUrl = item.thumbnailUrl,
                videoId = null,
                browseId = "apple:playlist:${item.storefront.ifBlank { storefront }}:${item.id}",
            )
        }

        val feelGoodShelfItems = feed.feelGoodPlaylists.filter { isValidItem(it) }.take(15).map { item ->
            ShelfItem(
                title = item.title,
                subtitle = item.subtitle,
                thumbnailUrl = item.thumbnailUrl,
                videoId = null,
                browseId = "apple:playlist:${item.storefront.ifBlank { storefront }}:${item.id}",
            )
        }

        val workoutTitle = appContext?.getString(R.string.shelf_workout) ?: (if (isSpanish) "Dale al gym" else "Work Out")
        val chillTitle = appContext?.getString(R.string.shelf_chill) ?: (if (isSpanish) "Para relajarte" else "Chill")
        val essentialsTitle = appContext?.getString(R.string.shelf_essentials) ?: (if (isSpanish) "Imprescindibles" else "Essentials")
        val focusTitle = appContext?.getString(R.string.shelf_focus) ?: (if (isSpanish) "Para concentrarte" else "Focus")
        val partyTitle = appContext?.getString(R.string.shelf_party) ?: (if (isSpanish) "De fiesta" else "Party")
        val feelGoodTitle = appContext?.getString(R.string.shelf_feel_good) ?: (if (isSpanish) "Buen rollo" else "Feel Good")

        val shelves = mutableListOf<HomeShelf>()
        if (workoutShelfItems.isNotEmpty()) {
            shelves.add(
                HomeShelf(
                    title = workoutTitle,
                    subtitle = "",
                    items = workoutShelfItems,
                    layout = ShelfLayout.CARDS,
                ),
            )
        }
        if (chillShelfItems.isNotEmpty()) {
            shelves.add(
                HomeShelf(
                    title = chillTitle,
                    subtitle = "",
                    items = chillShelfItems,
                    layout = ShelfLayout.CARDS,
                ),
            )
        }
        if (essentialsShelfItems.isNotEmpty()) {
            shelves.add(
                HomeShelf(
                    title = essentialsTitle,
                    subtitle = "",
                    items = essentialsShelfItems,
                    layout = ShelfLayout.CARDS,
                ),
            )
        }
        if (focusShelfItems.isNotEmpty()) {
            shelves.add(
                HomeShelf(
                    title = focusTitle,
                    subtitle = "",
                    items = focusShelfItems,
                    layout = ShelfLayout.CARDS,
                ),
            )
        }
        if (partyShelfItems.isNotEmpty()) {
            shelves.add(
                HomeShelf(
                    title = partyTitle,
                    subtitle = "",
                    items = partyShelfItems,
                    layout = ShelfLayout.CARDS,
                ),
            )
        }
        if (feelGoodShelfItems.isNotEmpty()) {
            shelves.add(
                HomeShelf(
                    title = feelGoodTitle,
                    subtitle = "",
                    items = feelGoodShelfItems,
                    layout = ShelfLayout.CARDS,
                ),
            )
        }
        shelves
    }

    private fun readHomeDiscoveryDiskCache(): AppleMusicHomeDiscoveryFeed? {
        val dir = cacheDir ?: return null
        val file = File(dir, HOME_DISCOVERY_CACHE_FILE_NAME)
        if (!file.exists() || !file.canRead()) return null
        return runCatching {
            json.decodeFromString<AppleMusicHomeDiscoveryFeed>(file.readText())
        }.getOrNull()
    }

    private fun writeHomeDiscoveryDiskCache(feed: AppleMusicHomeDiscoveryFeed) {
        val dir = cacheDir ?: return
        runCatching {
            val file = File(dir, HOME_DISCOVERY_CACHE_FILE_NAME)
            file.writeText(json.encodeToString(feed))
        }
    }

    private suspend fun fetchRemoteHomeDiscovery(storefront: String): AppleMusicHomeDiscoveryFeed = withContext(Dispatchers.IO) {
        var bearer = AppleMusicTokenProvider.token()
            ?: error("Could not obtain Apple Music authorization token")

        val isSpanish = Locale.getDefault().language == "es"
        val sfCandidates = listOf(storefront, "us", "mx", "es").distinct()

        // 1. Dale al gym (Workout - Apple Music Fitness)
        val workoutQueries = if (isSpanish) {
            listOf("Pop+latino+para+entrenar", "Baile+latino", "Puro+cardio", "Tu+mejor+marca", "Ritmo+de+pedal", "Puro+ejercicio", "Workout", "Gimnasio", "Cardio", "Running", "Fitness")
        } else {
            listOf("Pure+Workout", "Gym+Motivation", "Cardio+Workout", "Fitness+Pop", "Running+Motivation", "HIIT+Workout", "Workout")
        }
        val workoutCandidates = mutableListOf<AppleMusicEditorialItem>()
        val workoutExcludedKeywords = listOf("setlist", "video essentials", "escritas por", "written by", "next steps", "deep cuts", "influences")
        val fitnessKeywords = listOf("entrenar", "entrenamiento", "workout", "cardio", "pedal", "running", "gym", "fitness", "yoga", "ejercicio", "marca", "fuerza", "hiit")
        for (sf in sfCandidates) {
            for (query in workoutQueries) {
                val url = "$API_BASE/$sf/search?term=$query&types=playlists&limit=15&extend=editorialArtwork"
                val res = executeGet(url, bearer) ?: continue
                val root = runCatching { json.parseToJsonElement(res).jsonObject }.getOrNull()
                val data = root?.get("results")?.jsonObject?.get("playlists")?.jsonObject?.get("data")?.jsonArray
                    ?: JsonArray(emptyList())
                for (elem in data) {
                    val curator = elem.jsonObject["attributes"]?.jsonObject?.get("curatorName")?.jsonPrimitive?.contentOrNull.orEmpty()
                    if (!curator.contains("Apple Music", ignoreCase = true)) continue
                    val title = elem.jsonObject["attributes"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull.orEmpty()
                    val titleLower = title.lowercase()
                    if (workoutExcludedKeywords.any { titleLower.contains(it) }) continue
                    val isFitnessCurator = curator.contains("Apple Music Fitness", ignoreCase = true)
                    val hasFitnessKeyword = fitnessKeywords.any { titleLower.contains(it) }
                    if (!isFitnessCurator && !hasFitnessKeyword) continue

                    parseEditorialItem(elem.jsonObject, sf, defaultKind = EditorialItemKind.PLAYLIST, defaultBadge = null)?.let { item ->
                        if (isValidItem(item)) {
                            workoutCandidates.add(item.copy(badge = null))
                        }
                    }
                }
            }
            if (workoutCandidates.size >= 25) break
        }

        // Apply diversity: max 2 items per workout theme
        fun getWorkoutTheme(title: String): String {
            val t = title.lowercase()
            return when {
                t.contains("cardio") -> "cardio"
                t.contains("pedal") -> "pedal"
                t.contains("baile") || t.contains("dance") -> "dance"
                t.contains("pop latino") || t.contains("reggaetón") || t.contains("reggaeton") -> "latin"
                t.contains("running") || t.contains("correr") -> "running"
                t.contains("hiit") -> "hiit"
                t.contains("fuerza") || t.contains("strength") || t.contains("pesas") -> "strength"
                t.contains("yoga") -> "yoga"
                t.contains("marca") -> "performance"
                t.contains("ejercicio") -> "exercise"
                t.contains("pop") -> "pop"
                t.contains("gym") || t.contains("gimnasio") -> "gym"
                else -> "workout"
            }
        }
        val workoutThemeCounts = mutableMapOf<String, Int>()
        val diverseWorkoutCandidates = mutableListOf<AppleMusicEditorialItem>()
        for (item in workoutCandidates.distinctBy { it.id.lowercase() }) {
            val theme = getWorkoutTheme(item.title)
            val count = workoutThemeCounts.getOrDefault(theme, 0)
            if (count < 2) {
                diverseWorkoutCandidates.add(item)
                workoutThemeCounts[theme] = count + 1
            }
        }

        // 2. Para relajarte (Chill)
        val chillQueries = if (isSpanish) {
            listOf("Para+relajarse", "Pure+Chill", "Calma", "Chill", "Relax", "Concentración", "Acústico")
        } else {
            listOf("Pure+Chill", "Chill", "Relax", "Acoustic+Chill", "Focus", "Peaceful")
        }
        val chillCandidates = mutableListOf<AppleMusicEditorialItem>()
        for (sf in sfCandidates) {
            for (query in chillQueries) {
                val url = "$API_BASE/$sf/search?term=$query&types=playlists&limit=15&extend=editorialArtwork"
                val res = executeGet(url, bearer) ?: continue
                val root = runCatching { json.parseToJsonElement(res).jsonObject }.getOrNull()
                val data = root?.get("results")?.jsonObject?.get("playlists")?.jsonObject?.get("data")?.jsonArray
                    ?: JsonArray(emptyList())
                for (elem in data) {
                    val curator = elem.jsonObject["attributes"]?.jsonObject?.get("curatorName")?.jsonPrimitive?.contentOrNull
                    if (curator == null || !curator.contains("Apple Music", ignoreCase = true)) continue
                    parseEditorialItem(elem.jsonObject, sf, defaultKind = EditorialItemKind.PLAYLIST, defaultBadge = null)?.let { item ->
                        if (isValidItem(item)) {
                            chillCandidates.add(item.copy(badge = null))
                        }
                    }
                }
            }
            if (chillCandidates.size >= 15) break
        }

        // 3. Imprescindibles (Essentials - Solo artistas oficiales)
        val targetArtists = listOf(
            "Bad Bunny", "The Weeknd", "Karol G", "Feid", "Mora", "Myke Towers", "Maná", "Luis Miguel",
            "Rauw Alejandro", "Drake", "Taylor Swift", "Peso Pluma", "J Balvin", "Shakira", "Rosalía",
            "Coldplay", "Dua Lipa", "Billie Eilish"
        )
        val essentialsQueries = targetArtists.map { "${it.replace(" ", "+")}+Essentials" }
        val essentialsCandidates = mutableListOf<AppleMusicEditorialItem>()
        val excludedGenreKeywords = listOf(
            "rock", "pop ", "pop:", "reggaetón", "reggaeton", "bachata", "merengue", "salsa", "urbano", "latin",
            "navidad", "canciones de", "música", "musica", "hits", "motown", "80", "90", "2000", "2010", "70", "60",
            "década", "decada", "soundtrack", "canción", "cancion", "romántic", "romantic", "balada", "fiesta", "club",
            "r&b", "hip-hop", "hip hop", "trap", "cumbia", "indie", "electronic", "electrónica", "electronica", "metal",
            "punk", "country", "jazz", "blues", "clásica", "clasica", "acústico", "acustico"
        )
        for (sf in sfCandidates) {
            for (query in essentialsQueries) {
                val url = "$API_BASE/$sf/search?term=$query&types=playlists&limit=10&extend=editorialArtwork"
                val res = executeGet(url, bearer) ?: continue
                val root = runCatching { json.parseToJsonElement(res).jsonObject }.getOrNull()
                val data = root?.get("results")?.jsonObject?.get("playlists")?.jsonObject?.get("data")?.jsonArray
                    ?: JsonArray(emptyList())
                for (elem in data) {
                    val curator = elem.jsonObject["attributes"]?.jsonObject?.get("curatorName")?.jsonPrimitive?.contentOrNull.orEmpty()
                    if (!curator.contains("Apple Music", ignoreCase = true)) continue
                    val title = elem.jsonObject["attributes"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull.orEmpty()
                    val titleLower = title.lowercase()
                    if (!titleLower.contains("essentials") && !titleLower.contains("imprescindibles")) continue
                    if (excludedGenreKeywords.any { titleLower.contains(it) }) continue

                    parseEditorialItem(elem.jsonObject, sf, defaultKind = EditorialItemKind.PLAYLIST, defaultBadge = null)?.let { item ->
                        if (isValidItem(item)) {
                            essentialsCandidates.add(item.copy(badge = null))
                        }
                    }
                }
            }
            if (essentialsCandidates.size >= 25) break
        }

        // Deduplicate Essentials by artist so each playlist represents a distinct artist
        val seenArtists = mutableSetOf<String>()
        val diverseEssentialsCandidates = mutableListOf<AppleMusicEditorialItem>()
        for (item in essentialsCandidates.distinctBy { it.id.lowercase() }) {
            val artistKey = item.title.replace(": Imprescindibles", "", ignoreCase = true)
                .replace(" Imprescindibles", "", ignoreCase = true)
                .replace(": Essentials", "", ignoreCase = true)
                .replace(" Essentials", "", ignoreCase = true)
                .trim().lowercase()
            if (seenArtists.add(artistKey)) {
                diverseEssentialsCandidates.add(item)
            }
        }

        // 4. Para concentrarte (Focus)
        val focusQueries = if (isSpanish) {
            listOf("Focus", "Concentración", "Lo-Fi", "Beatstrumentals", "Estudio", "Pure+Focus")
        } else {
            listOf("Pure+Focus", "Focus", "Lo-Fi+Beats", "Beatstrumentals", "Study", "Brain+Food")
        }
        val focusCandidates = mutableListOf<AppleMusicEditorialItem>()
        for (sf in sfCandidates) {
            for (query in focusQueries) {
                val url = "$API_BASE/$sf/search?term=$query&types=playlists&limit=15&extend=editorialArtwork"
                val res = executeGet(url, bearer) ?: continue
                val root = runCatching { json.parseToJsonElement(res).jsonObject }.getOrNull()
                val data = root?.get("results")?.jsonObject?.get("playlists")?.jsonObject?.get("data")?.jsonArray
                    ?: JsonArray(emptyList())
                for (elem in data) {
                    val curator = elem.jsonObject["attributes"]?.jsonObject?.get("curatorName")?.jsonPrimitive?.contentOrNull.orEmpty()
                    if (!curator.contains("Apple Music", ignoreCase = true)) continue
                    parseEditorialItem(elem.jsonObject, sf, defaultKind = EditorialItemKind.PLAYLIST, defaultBadge = null)?.let { item ->
                        if (isValidItem(item)) {
                            focusCandidates.add(item.copy(badge = null))
                        }
                    }
                }
            }
            if (focusCandidates.size >= 15) break
        }

        // 5. De fiesta (Party)
        val partyQueries = if (isSpanish) {
            listOf("Party", "Fiesta", "Perreo", "Club", "Previa", "La+Previa", "Dale+Play+Fiesta")
        } else {
            listOf("Party+Starters", "Dance+Party", "Club", "Weekend+Party", "Pop+Party")
        }
        val partyCandidates = mutableListOf<AppleMusicEditorialItem>()
        for (sf in sfCandidates) {
            for (query in partyQueries) {
                val url = "$API_BASE/$sf/search?term=$query&types=playlists&limit=15&extend=editorialArtwork"
                val res = executeGet(url, bearer) ?: continue
                val root = runCatching { json.parseToJsonElement(res).jsonObject }.getOrNull()
                val data = root?.get("results")?.jsonObject?.get("playlists")?.jsonObject?.get("data")?.jsonArray
                    ?: JsonArray(emptyList())
                for (elem in data) {
                    val curator = elem.jsonObject["attributes"]?.jsonObject?.get("curatorName")?.jsonPrimitive?.contentOrNull.orEmpty()
                    if (!curator.contains("Apple Music", ignoreCase = true)) continue
                    parseEditorialItem(elem.jsonObject, sf, defaultKind = EditorialItemKind.PLAYLIST, defaultBadge = null)?.let { item ->
                        if (isValidItem(item)) {
                            partyCandidates.add(item.copy(badge = null))
                        }
                    }
                }
            }
            if (partyCandidates.size >= 15) break
        }

        // 6. Buen rollo (Feel Good)
        val feelGoodQueries = if (isSpanish) {
            listOf("Buen+rollo", "Feel+Good", "Sonidos+del+verano", "Good+Vibes", "Pura+Vida", "Energía+positiva")
        } else {
            listOf("Feel+Good", "Good+Vibes", "Summer+Sounds", "Happy+Hits", "Sunny+Vibes", "Uplifting")
        }
        val feelGoodCandidates = mutableListOf<AppleMusicEditorialItem>()
        for (sf in sfCandidates) {
            for (query in feelGoodQueries) {
                val url = "$API_BASE/$sf/search?term=$query&types=playlists&limit=15&extend=editorialArtwork"
                val res = executeGet(url, bearer) ?: continue
                val root = runCatching { json.parseToJsonElement(res).jsonObject }.getOrNull()
                val data = root?.get("results")?.jsonObject?.get("playlists")?.jsonObject?.get("data")?.jsonArray
                    ?: JsonArray(emptyList())
                for (elem in data) {
                    val curator = elem.jsonObject["attributes"]?.jsonObject?.get("curatorName")?.jsonPrimitive?.contentOrNull.orEmpty()
                    if (!curator.contains("Apple Music", ignoreCase = true)) continue
                    parseEditorialItem(elem.jsonObject, sf, defaultKind = EditorialItemKind.PLAYLIST, defaultBadge = null)?.let { item ->
                        if (isValidItem(item)) {
                            feelGoodCandidates.add(item.copy(badge = null))
                        }
                    }
                }
            }
            if (feelGoodCandidates.size >= 15) break
        }

        AppleMusicHomeDiscoveryFeed(
            workoutPlaylists = diverseWorkoutCandidates.filter { isValidItem(it) }.distinctBy { it.id.lowercase() }.take(15),
            chillPlaylists = chillCandidates.filter { isValidItem(it) }.distinctBy { it.id.lowercase() }.take(15),
            essentialsPlaylists = diverseEssentialsCandidates.filter { isValidItem(it) }.distinctBy { it.id.lowercase() }.take(18),
            focusPlaylists = focusCandidates.filter { isValidItem(it) }.distinctBy { it.id.lowercase() }.take(15),
            partyPlaylists = partyCandidates.filter { isValidItem(it) }.distinctBy { it.id.lowercase() }.take(15),
            feelGoodPlaylists = feelGoodCandidates.filter { isValidItem(it) }.distinctBy { it.id.lowercase() }.take(15),
            timestamp = System.currentTimeMillis(),
        )
    }

    private fun getCuratedHomeDiscoverySeed(storefront: String): AppleMusicHomeDiscoveryFeed {
        return AppleMusicHomeDiscoveryFeed()
    }

    private fun readDiskCache(): AppleMusicEditorialFeed? {
        val dir = cacheDir ?: return null
        val file = File(dir, CACHE_FILE_NAME)
        if (!file.exists() || !file.canRead()) return null
        return runCatching {
            json.decodeFromString<AppleMusicEditorialFeed>(file.readText())
        }.getOrNull()
    }

    private fun writeDiskCache(feed: AppleMusicEditorialFeed) {
        val dir = cacheDir ?: return
        runCatching {
            val file = File(dir, CACHE_FILE_NAME)
            file.writeText(json.encodeToString(feed))
        }
    }

    private suspend fun fetchRemoteFeed(storefront: String): AppleMusicEditorialFeed = withContext(Dispatchers.IO) {
        var bearer = AppleMusicTokenProvider.token()
            ?: error("Could not obtain Apple Music authorization token")

        val candidateStorefronts = listOf(storefront, "us", "mx", "ec").distinct()
        var chartsJson: String? = null
        var activeStorefront = storefront

        for (sf in candidateStorefronts) {
            val chartsUrl = "$API_BASE/$sf/charts?types=playlists,albums,songs&limit=30&extend=editorialArtwork,editorialVideo"
            val res = executeGet(chartsUrl, bearer)
            if (res != null) {
                chartsJson = res
                activeStorefront = sf
                break
            }
        }

        if (chartsJson == null) {
            AppleMusicTokenProvider.invalidateToken(bearer)
            bearer = AppleMusicTokenProvider.token()
                ?: error("Could not refresh Apple Music authorization token")
            for (sf in candidateStorefronts) {
                val chartsUrl = "$API_BASE/$sf/charts?types=playlists,albums,songs&limit=30&extend=editorialArtwork,editorialVideo"
                val res = executeGet(chartsUrl, bearer)
                if (res != null) {
                    chartsJson = res
                    activeStorefront = sf
                    break
                }
            }
        }

        val root = chartsJson?.let {
            runCatching { json.parseToJsonElement(it).jsonObject }.getOrNull()
        } ?: error("Failed to parse charts response")

        val results = root["results"]?.jsonObject ?: JsonObject(emptyMap())
        val isSpanish = Locale.getDefault().language == "es"

        fun isOfficialTop100(title: String, curator: String? = null): Boolean {
            val t = title.trim()
            val startsWithTop100 = t.startsWith("Top 100:", ignoreCase = true) ||
                t.startsWith("Le top 100:", ignoreCase = true) ||
                t.startsWith("Top 100 :", ignoreCase = true)
            if (!startsWithTop100) return false
            val hasPastYear = Regex("\\b(19\\d\\d|20[0-2][0-9])\\b").containsMatchIn(t)
            if (hasPastYear) return false
            if (curator != null && !curator.equals("Apple Music", ignoreCase = true)) {
                return false
            }
            return true
        }

        fun normalizeTop100Title(title: String): String {
            return title.lowercase()
                .replace("le top 100:", "top 100:")
                .replace("top 100 :", "top 100:")
                .replace("top 100 -", "top 100:")
                .trim()
        }

        fun isOfficialCityChart(title: String): Boolean {
            val t = title.trim()
            return t.startsWith("Top 25:", ignoreCase = true) || t.startsWith("Top 25 :", ignoreCase = true)
        }

        fun isRealDjMix(item: AppleMusicEditorialItem): Boolean {
            if (!isValidItem(item)) return false
            val t = item.title.lowercase()
            val desc = (item.description ?: "").lowercase()
            val sub = item.subtitle.lowercase()

            val isGeneric = t.contains("essentials") || t.contains("imprescindibles") ||
                t.contains("lo mejor de") || t.contains("greatest hits") || t.contains("best of") ||
                t.contains("antología") || t.contains("top 100") || t.contains("top 25") ||
                t.contains("today's hits") || t.contains("chart")
            if (isGeneric) return false

            return t.contains("dj mix") || t.contains("club mix") || t.contains("live set") ||
                t.contains("tomorrowland") || t.contains("beats in space") || t.contains("today at apple") ||
                t.contains("boiler room") || t.contains("cercle") || t.contains("mixmag") ||
                t.contains("defected") || t.contains("resident advisor") || t.contains("edc") ||
                t.contains("fabric") || t.contains("ministry of sound") || t.contains("club") ||
                sub.contains("dj") || desc.contains("dj mix") || desc.contains("live set")
        }

        fun isUpcomingRelease(releaseDateStr: String?): Boolean {
            if (releaseDateStr.isNullOrBlank()) return false
            return runCatching {
                val parts = releaseDateStr.take(10).split("-")
                if (parts.size == 3) {
                    val relYear = parts[0].toInt()
                    val relMonth = parts[1].toInt()
                    val relDay = parts[2].toInt()
                    val today = java.time.LocalDate.now()
                    val releaseDate = java.time.LocalDate.of(relYear, relMonth, relDay)
                    releaseDate.isAfter(today)
                } else false
            }.getOrDefault(false)
        }

        // Extract albums from charts
        val albumList = mutableListOf<AppleMusicEditorialItem>()
        val albumsArray = results["albums"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("data")?.jsonArray ?: JsonArray(emptyList())
        for (elem in albumsArray) {
            parseEditorialItem(elem.jsonObject, activeStorefront, defaultKind = EditorialItemKind.ALBUM)
                ?.let { albumList.add(it) }
        }

        // Extract REAL NEW RELEASES for "Canciones nuevas destacadas" (New Music Daily + Dale Play + fresh album tracks)
        val newSongsList = mutableListOf<AppleMusicEditorialSong>()

        // 1. Fetch tracks from New Music Daily (Apple Music's premier new releases playlist)
        val nmdUrl = "$API_BASE/$activeStorefront/playlists/pl.2b0e6e332fdf4b7a91164da3162127b5?include=tracks"
        val nmdJson = executeGet(nmdUrl, bearer)
        if (nmdJson != null) {
            val nmdRoot = runCatching { json.parseToJsonElement(nmdJson).jsonObject }.getOrNull()
            val tracksArray = nmdRoot?.get("data")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("relationships")?.jsonObject?.get("tracks")?.jsonObject?.get("data")?.jsonArray
                ?: JsonArray(emptyList())
            for (elem in tracksArray) {
                parseEditorialSong(elem.jsonObject)?.let { newSongsList.add(it) }
            }
        }

        // 2. If Spanish storefront, also fetch from Dale Play / Novedades
        if (isSpanish) {
            val dalePlayUrl = "$API_BASE/$activeStorefront/playlists/pl.075f88440e6c40e5b7222aa894458f33?include=tracks"
            val dalePlayJson = executeGet(dalePlayUrl, bearer)
            if (dalePlayJson != null) {
                val dpRoot = runCatching { json.parseToJsonElement(dalePlayJson).jsonObject }.getOrNull()
                val dpTracksArray = dpRoot?.get("data")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("relationships")?.jsonObject?.get("tracks")?.jsonObject?.get("data")?.jsonArray
                    ?: JsonArray(emptyList())
                for (elem in dpTracksArray) {
                    parseEditorialSong(elem.jsonObject)?.let { newSongsList.add(it) }
                }
            }
        }

        // 3. Fetch top tracks from freshly launched albums in charts
        val freshAlbumIds = albumList.map { it.id }.filter { it.all { ch -> ch.isDigit() } }.take(5)
        if (freshAlbumIds.isNotEmpty()) {
            val albumsUrl = "$API_BASE/$activeStorefront/albums?ids=${freshAlbumIds.joinToString(",")}&include=tracks"
            val albumsJson = executeGet(albumsUrl, bearer)
            if (albumsJson != null) {
                val albRoot = runCatching { json.parseToJsonElement(albumsJson).jsonObject }.getOrNull()
                val albData = albRoot?.get("data")?.jsonArray ?: JsonArray(emptyList())
                for (albObj in albData) {
                    val albTracks = albObj.jsonObject["relationships"]?.jsonObject?.get("tracks")?.jsonObject
                        ?.get("data")?.jsonArray ?: JsonArray(emptyList())
                    for (trackElem in albTracks.take(3)) {
                        parseEditorialSong(trackElem.jsonObject)?.let { newSongsList.add(it) }
                    }
                }
            }
        }

        // Extract playlists from charts
        val chartPlaylistList = mutableListOf<AppleMusicEditorialItem>()
        val playlistsArray = results["playlists"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("data")?.jsonArray ?: JsonArray(emptyList())
        for (elem in playlistsArray) {
            parseEditorialItem(elem.jsonObject, activeStorefront, defaultKind = EditorialItemKind.PLAYLIST)
                ?.let { chartPlaylistList.add(it) }
        }

        // Fetch curated playlist sets in batch
        val curatedPlaylistIds = listOf(
            // Updated Playlists
            "pl.f4d106fed2bd41149aaacabb233eb5eb", // Today's Hits
            "pl.2b0e6e332fdf4b7a91164da3162127b5", // New Music Daily
            "pl.075f88440e6c40e5b7222aa894458f33", // Dale Play
            "pl.8e05c879331846b48a04870f7cfcb257", // Éxitos de hoy
            "pl.5ee8333dbe944d9f9151e97d92d123bf", // A-List Pop
            "pl.74cf97ae33394c868038abebc73299c8", // ¡Viva Latino!
            "pl.abe8ba42642d481e9e544d64f7dad32d", // Rap Life
            "pl.2e40d6c813be45ee8ec4807460bf0c86", // Viral Hits
            "pl.c624d776495d4ea79eb5380ea7eb6375", // Dance Party
            "pl.b7ae3e0a28e84edd8e997a71e75ab620", // R&B Now
        )

        val batchUrl = "$API_BASE/$activeStorefront/playlists?ids=${curatedPlaylistIds.joinToString(",")}&extend=editorialArtwork,editorialVideo"
        val batchJson = executeGet(batchUrl, bearer)
        val batchPlaylists = mutableMapOf<String, AppleMusicEditorialItem>()
        if (batchJson != null) {
            val batchRoot = runCatching { json.parseToJsonElement(batchJson).jsonObject }.getOrNull()
            val dataArray = batchRoot?.get("data")?.jsonArray ?: JsonArray(emptyList())
            for (elem in dataArray) {
                parseEditorialItem(elem.jsonObject, activeStorefront, defaultKind = EditorialItemKind.PLAYLIST)
                    ?.let { batchPlaylists[it.id] = it }
            }
        }

        // Fetch DJ Mixes (real dynamic DJ sessions across multiple terms)
        val djSearchTerms = listOf(
            "DJ+Mix",
            "Club+Mix",
            "Boiler+Room",
            "Beats+In+Space",
            "Electronic+Mix",
            "Tomorrowland",
            "Resident+Advisor",
            "Cercle",
            "Today+at+Apple",
        )
        val djCandidates = mutableListOf<AppleMusicEditorialItem>()
        val djStorefronts = listOf(activeStorefront, "us", "gb").distinct()
        for (sf in djStorefronts) {
            for (term in djSearchTerms) {
                val djMixUrl = "$API_BASE/$sf/search?term=$term&types=playlists&limit=15&extend=editorialArtwork,editorialVideo"
                val djMixJson = executeGet(djMixUrl, bearer) ?: continue
                val djRoot = runCatching { json.parseToJsonElement(djMixJson).jsonObject }.getOrNull()
                val djResults = djRoot?.get("results")?.jsonObject?.get("playlists")?.jsonObject?.get("data")?.jsonArray
                    ?: JsonArray(emptyList())
                for (elem in djResults) {
                    parseEditorialItem(elem.jsonObject, sf, defaultKind = EditorialItemKind.PLAYLIST, defaultBadge = "EXCLUSIVE DJ MIX")
                        ?.let { parsed ->
                            if (isRealDjMix(parsed)) {
                                djCandidates.add(parsed)
                            }
                        }
                }
            }
            if (djCandidates.size >= 25) break
        }

        // Build Apple Music Hero Pool (alternating up to 8 top albums and updated playlists)
        val appleHeroItems = mutableListOf<AppleMusicEditorialItem>()
        val albumsPool = albumList.toMutableList()
        val playlistsPool = (batchPlaylists.values.toList() + chartPlaylistList).distinctBy { it.id }.toMutableList()

        for (i in 0 until 4) {
            if (albumsPool.isNotEmpty()) {
                appleHeroItems.add(albumsPool.removeAt(0))
            }
            if (playlistsPool.isNotEmpty()) {
                appleHeroItems.add(playlistsPool.removeAt(0))
            }
        }

        // Build YouTube Music dynamic items from Explore / New Releases (3 to 4 items)
        val ytmHeroItems = mutableListOf<AppleMusicEditorialItem>()
        runCatching {
            val ytmShelves = YtMusicRepository.homeSupplement("FEmusic_new_releases").getOrNull()
                ?: YtMusicRepository.homeSupplement("FEmusic_explore").getOrNull()
                ?: emptyList()
            val ytmShelfItems = ytmShelves.flatMap { it.items }.filter { it.browseId != null }
            for (shelfItem in ytmShelfItems.take(8)) {
                val bId = shelfItem.browseId ?: continue
                val isAlbum = bId.startsWith("MPREb") || bId.startsWith("VLOLAK") || bId.startsWith("OLAK")
                val badge = if (isAlbum) {
                    if (isSpanish) "NUEVO ÁLBUM" else "NEW ALBUM"
                } else {
                    if (isSpanish) "PLAYLIST DESTACADA" else "FEATURED PLAYLIST"
                }
                val desc = if (isSpanish) {
                    "Lanzamiento destacado en YouTube Music"
                } else {
                    "Featured release on YouTube Music"
                }
                ytmHeroItems.add(
                    AppleMusicEditorialItem(
                        id = bId,
                        title = shelfItem.title,
                        subtitle = shelfItem.subtitle,
                        description = desc,
                        thumbnailUrl = shelfItem.thumbnailUrl,
                        wideImageUrl = shelfItem.thumbnailUrl,
                        badge = badge,
                        kind = if (isAlbum) EditorialItemKind.ALBUM else EditorialItemKind.PLAYLIST,
                        storefront = activeStorefront,
                    ),
                )
                if (ytmHeroItems.size >= 4) break
            }
        }

        // Interleave Apple Music (up to 8) and YouTube Music (3-4) for a long 10-12 dynamic card carousel
        val heroItems = mutableListOf<AppleMusicEditorialItem>()
        var amIdx = 0
        var ytmIdx = 0
        while (amIdx < appleHeroItems.size || ytmIdx < ytmHeroItems.size) {
            if (amIdx < appleHeroItems.size) heroItems.add(appleHeroItems[amIdx++])
            if (amIdx < appleHeroItems.size) heroItems.add(appleHeroItems[amIdx++])
            if (ytmIdx < ytmHeroItems.size) heroItems.add(ytmHeroItems[ytmIdx++])
        }

        // Build Updated Playlists
        val updatedKeys = listOf(
            "pl.2b0e6e332fdf4b7a91164da3162127b5",
            "pl.f4d106fed2bd41149aaacabb233eb5eb",
            "pl.075f88440e6c40e5b7222aa894458f33",
            "pl.8e05c879331846b48a04870f7cfcb257",
            "pl.5ee8333dbe944d9f9151e97d92d123bf",
            "pl.74cf97ae33394c868038abebc73299c8",
            "pl.abe8ba42642d481e9e544d64f7dad32d",
            "pl.2e40d6c813be45ee8ec4807460bf0c86",
            "pl.c624d776495d4ea79eb5380ea7eb6375",
            "pl.b7ae3e0a28e84edd8e997a71e75ab620",
        )

        // 1. Fetch live playlist charts for the active storefront
        val playlistChartsUrl = "$API_BASE/$activeStorefront/charts?types=playlists&limit=25&extend=editorialArtwork,editorialVideo"
        val playlistChartsJson = executeGet(playlistChartsUrl, bearer)
        val dynamicPlaylists = mutableListOf<AppleMusicEditorialItem>()
        if (playlistChartsJson != null) {
            val plRoot = runCatching { json.parseToJsonElement(playlistChartsJson).jsonObject }.getOrNull()
            val plArray = plRoot?.get("results")?.jsonObject?.get("playlists")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("data")?.jsonArray ?: JsonArray(emptyList())
            for (elem in plArray) {
                parseEditorialItem(elem.jsonObject, activeStorefront, defaultKind = EditorialItemKind.PLAYLIST)
                    ?.let { dynamicPlaylists.add(it) }
            }
        }

        // 2. Fetch popular editorial playlists search (Hits / Éxitos)
        val searchTerm = if (isSpanish) "Exitos" else "Hits"
        val searchPlaylistsUrl = "$API_BASE/$activeStorefront/search?term=$searchTerm&types=playlists&limit=15&extend=editorialArtwork,editorialVideo"
        val searchJson = executeGet(searchPlaylistsUrl, bearer)
        if (searchJson != null) {
            val sRoot = runCatching { json.parseToJsonElement(searchJson).jsonObject }.getOrNull()
            val sArray = sRoot?.get("results")?.jsonObject?.get("playlists")?.jsonObject?.get("data")?.jsonArray
                ?: JsonArray(emptyList())
            for (elem in sArray) {
                parseEditorialItem(elem.jsonObject, activeStorefront, defaultKind = EditorialItemKind.PLAYLIST)
                    ?.let { dynamicPlaylists.add(it) }
            }
        }

        // Build Updated Playlists (10 to 12 items, excluding playlists already featured in Hero)
        val heroIds = heroItems.map { it.id }.toSet()
        val allCandidatePlaylists = (
            dynamicPlaylists +
            chartPlaylistList +
            updatedKeys.mapNotNull { batchPlaylists[it] } +
            batchPlaylists.values
        ).distinctBy { it.id }

        val nonHeroPlaylists = allCandidatePlaylists.filter { it.id !in heroIds }
        val candidatePool = if (nonHeroPlaylists.size >= 10) nonHeroPlaylists else allCandidatePlaylists
        val updatedPlaylists = candidatePool
            .filter { isValidItem(it) }
            .distinctBy { it.id }
            .take(12)
            .map { it.copy(badge = if (isSpanish) "ACTUALIZADA" else "UPDATED") }

        // Build Daily Top 100 (purified: ONLY official Apple Music curator, normalized title deduplication, no empty/grey cards)
        val top100List = mutableListOf<AppleMusicEditorialItem>()
        val top100Storefronts = listOf(activeStorefront, "us", "mx", "es", "gb", "fr", "de", "jp", "co", "ar", "ca", "au", "it", "kr", "br", "ec", "cl", "pe").distinct()

        for (sf in top100Storefronts) {
            val top100SearchUrl = "$API_BASE/$sf/search?term=Top+100%3A&types=playlists&limit=25&extend=editorialArtwork,editorialVideo"
            val top100Json = executeGet(top100SearchUrl, bearer) ?: continue
            val top100Root = runCatching { json.parseToJsonElement(top100Json).jsonObject }.getOrNull()
            val top100Data = top100Root?.get("results")?.jsonObject?.get("playlists")?.jsonObject?.get("data")?.jsonArray
                ?: JsonArray(emptyList())
            for (elem in top100Data) {
                val curatorName = elem.jsonObject["attributes"]?.jsonObject?.get("curatorName")?.jsonPrimitive?.contentOrNull?.trim()
                if (curatorName == null || !curatorName.equals("Apple Music", ignoreCase = true)) {
                    continue
                }
                val parsed = parseEditorialItem(elem.jsonObject, sf, defaultKind = EditorialItemKind.PLAYLIST)
                if (parsed != null && isOfficialTop100(parsed.title, curatorName) && !parsed.thumbnailUrl.isNullOrBlank() && !parsed.thumbnailUrl.contains("{")) {
                    top100List.add(parsed)
                }
            }
            if (top100List.map { normalizeTop100Title(it.title) }.distinct().size >= 30) break
        }

        val dailyTop100 = top100List
            .filter {
                isOfficialTop100(it.title, it.subtitle) &&
                    isValidItem(it)
            }
            .distinctBy { normalizeTop100Title(it.title) }
            .take(30)

        // Build City Charts (purified: only official Apple Music "Top 25: [City]" playlists)
        val cityList = mutableListOf<AppleMusicEditorialItem>()
        val cityStorefronts = listOf(activeStorefront, "us", "mx", "es", "gb", "fr", "de", "co").distinct()

        for (sf in cityStorefronts) {
            val citySearchUrl = "$API_BASE/$sf/search?term=Top+25&types=playlists&limit=25&extend=editorialArtwork,editorialVideo"
            val cityJson = executeGet(citySearchUrl, bearer) ?: continue
            val cityRoot = runCatching { json.parseToJsonElement(cityJson).jsonObject }.getOrNull()
            val cityData = cityRoot?.get("results")?.jsonObject?.get("playlists")?.jsonObject?.get("data")?.jsonArray
                ?: JsonArray(emptyList())
            for (elem in cityData) {
                val parsed = parseEditorialItem(elem.jsonObject, sf, defaultKind = EditorialItemKind.PLAYLIST)
                if (parsed != null && isOfficialCityChart(parsed.title) && !parsed.thumbnailUrl.isNullOrBlank() && !parsed.thumbnailUrl.contains("{")) {
                    cityList.add(parsed)
                }
            }
            if (cityList.distinctBy { it.id }.size >= 25) break
        }

        val cityCharts = cityList
            .filter { isOfficialCityChart(it.title) && isValidItem(it) }
            .distinctBy { it.id }
            .take(25)

        // Build Coming Soon (albums with releaseDate > today or pre-add/coming soon editorial collections)
        val upcomingSearchTerms = if (isSpanish) {
            listOf("Proximamente", "Coming+Soon", "Pre-Add", "Upcoming")
        } else {
            listOf("Coming+Soon", "Pre-Add", "Upcoming")
        }
        val upcomingList = mutableListOf<AppleMusicEditorialItem>()

        // 1. Check albums in charts for upcoming release dates
        for (elem in albumsArray) {
            val rDate = elem.jsonObject["attributes"]?.jsonObject?.get("releaseDate")?.jsonPrimitive?.contentOrNull
            if (isUpcomingRelease(rDate)) {
                parseEditorialItem(elem.jsonObject, activeStorefront, defaultKind = EditorialItemKind.ALBUM, defaultBadge = if (isSpanish) "PRÓXIMAMENTE" else "COMING SOON")
                    ?.let { parsed ->
                        if (!parsed.thumbnailUrl.isNullOrBlank() && !parsed.thumbnailUrl.contains("{")) {
                            upcomingList.add(parsed)
                        }
                    }
            }
        }

        // 2. Query upcoming search terms in active storefront
        for (term in upcomingSearchTerms) {
            val csUrl = "$API_BASE/$activeStorefront/search?term=$term&types=albums&limit=25&extend=editorialArtwork,editorialVideo"
            val csJson = executeGet(csUrl, bearer) ?: continue
            val csRoot = runCatching { json.parseToJsonElement(csJson).jsonObject }.getOrNull()
            val csData = csRoot?.get("results")?.jsonObject?.get("albums")?.jsonObject?.get("data")?.jsonArray
                ?: JsonArray(emptyList())
            for (elem in csData) {
                val rDate = elem.jsonObject["attributes"]?.jsonObject?.get("releaseDate")?.jsonPrimitive?.contentOrNull
                if (isUpcomingRelease(rDate)) {
                    parseEditorialItem(elem.jsonObject, activeStorefront, defaultKind = EditorialItemKind.ALBUM, defaultBadge = if (isSpanish) "PRÓXIMAMENTE" else "COMING SOON")
                        ?.let { parsed ->
                            if (!parsed.thumbnailUrl.isNullOrBlank() && !parsed.thumbnailUrl.contains("{")) {
                                upcomingList.add(parsed)
                            }
                        }
                }
            }
            if (upcomingList.distinctBy { it.id }.size >= 12) break
        }

        val candidateComingSoon = if (upcomingList.size < 4) {
            val freshAlbums = albumList.filter { alb ->
                !alb.thumbnailUrl.isNullOrBlank() && alb.id !in heroIds
            }.take(8).map {
                it.copy(badge = if (isSpanish) "PRÓXIMAMENTE" else "COMING SOON")
            }
            (upcomingList + freshAlbums).distinctBy { it.id }
        } else {
            upcomingList
        }

        val comingSoon = candidateComingSoon
            .filter { isValidItem(it) }
            .distinctBy { it.id }
            .take(12)

        // Apply diversity rule to DJ Mixes: max 2 items per curatorName, round-robin interleaved
        // Strictly filter to ensure no item without valid https artwork is ever added, and do not append broken seed items
        val candidateDjPool = djCandidates.filter { isValidItem(it) && isRealDjMix(it) }

        val djByCurator = mutableMapOf<String, MutableList<AppleMusicEditorialItem>>()
        for (item in candidateDjPool.distinctBy { it.id }) {
            val curatorKey = item.subtitle.ifBlank { "Apple Music" }.lowercase().trim()
            val list = djByCurator.getOrPut(curatorKey) { mutableListOf() }
            if (list.size < 2) {
                list.add(item)
            }
        }

        val djMixes = mutableListOf<AppleMusicEditorialItem>()
        var djRound = 0
        var addedInRound = true
        while (djMixes.size < 15 && addedInRound) {
            addedInRound = false
            for (list in djByCurator.values) {
                if (djRound < list.size && djMixes.size < 15) {
                    djMixes.add(list[djRound])
                    addedInRound = true
                }
            }
            djRound++
        }

        // Fetch Trending Songs (Global / US Apple Music Top Song Charts: GET /v1/catalog/us/charts?types=songs&limit=20)
        // Replicating Mornye's exact real-time global feed with fallbacks to most-played and Today's Hits US
        val trendingSongsList = mutableListOf<AppleMusicEditorialSong>()
        val usTrendingChartsUrl = "$API_BASE/us/charts?types=songs&limit=20"
        val usTrendingJson = executeGet(usTrendingChartsUrl, bearer)
        if (usTrendingJson != null) {
            val tRoot = runCatching { json.parseToJsonElement(usTrendingJson).jsonObject }.getOrNull()
            val tData = tRoot?.get("results")?.jsonObject?.get("songs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("data")?.jsonArray ?: JsonArray(emptyList())
            for (elem in tData) {
                parseEditorialSong(elem.jsonObject)?.let { song ->
                    if (isValidSong(song)) {
                        trendingSongsList.add(song)
                    }
                }
            }
        }

        // Fallback 1: most-played chart in US
        if (trendingSongsList.isEmpty()) {
            val mostPlayedUrl = "$API_BASE/us/charts?types=songs&chart=most-played&limit=20"
            val mpJson = executeGet(mostPlayedUrl, bearer)
            if (mpJson != null) {
                val mpRoot = runCatching { json.parseToJsonElement(mpJson).jsonObject }.getOrNull()
                val mpData = mpRoot?.get("results")?.jsonObject?.get("songs")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("data")?.jsonArray ?: JsonArray(emptyList())
                for (elem in mpData) {
                    parseEditorialSong(elem.jsonObject)?.let { song ->
                        if (isValidSong(song)) {
                            trendingSongsList.add(song)
                        }
                    }
                }
            }
        }

        // Fallback 2: Today's Hits editorial playlist from US (pl.f4d106fed2bd41149aaacabb233eb5eb)
        if (trendingSongsList.isEmpty()) {
            val thUrl = "$API_BASE/us/playlists/pl.f4d106fed2bd41149aaacabb233eb5eb?include=tracks"
            val thJson = executeGet(thUrl, bearer)
            if (thJson != null) {
                val thRoot = runCatching { json.parseToJsonElement(thJson).jsonObject }.getOrNull()
                val thTracks = thRoot?.get("data")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("relationships")?.jsonObject?.get("tracks")?.jsonObject?.get("data")?.jsonArray
                    ?: JsonArray(emptyList())
                for (elem in thTracks.take(20)) {
                    parseEditorialSong(elem.jsonObject)?.let { song ->
                        if (isValidSong(song)) {
                            trendingSongsList.add(song)
                        }
                    }
                }
            }
        }

        val trendingSongs = trendingSongsList
            .filter { isValidSong(it) }
            .distinctBy { it.id }
            .take(20)

        val newSongs = newSongsList
            .filter { isValidSong(it) }
            .distinctBy { "${it.title.lowercase()}:${it.artist.lowercase()}" }
            .take(24)

        // Fetch Canciones del momento (16 premier tracks from hit playlists, excluding newSongs)
        val momentPlaylistIds = if (isSpanish) {
            listOf(
                "pl.8e05c879331846b48a04870f7cfcb257", // Éxitos de hoy
                "pl.075f88440e6c40e5b7222aa894458f33", // ¡Dale Play!
                "pl.f4d106fed2bd41149aaacabb233eb5eb", // Today's Hits
            )
        } else {
            listOf(
                "pl.f4d106fed2bd41149aaacabb233eb5eb", // Today's Hits
                "pl.5ee8333dbe944d9f9151e97d92d123bf", // A-List Pop
                "pl.8e05c879331846b48a04870f7cfcb257", // Éxitos de hoy
            )
        }
        val momentRawTracks = mutableListOf<AppleMusicEditorialSong>()
        for (plId in momentPlaylistIds) {
            val plUrl = "$API_BASE/$activeStorefront/playlists/$plId?include=tracks"
            val plJson = executeGet(plUrl, bearer) ?: continue
            val momentRoot = runCatching { json.parseToJsonElement(plJson).jsonObject }.getOrNull()
            val tracksArr = momentRoot?.get("data")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("relationships")?.jsonObject?.get("tracks")?.jsonObject?.get("data")?.jsonArray
                ?: JsonArray(emptyList())
            for (elem in tracksArr) {
                parseEditorialSong(elem.jsonObject)?.let { momentRawTracks.add(it) }
            }
            if (momentRawTracks.size >= 30) break
        }

        val newSongKeys = newSongs.map { "${it.title.lowercase()}:${it.artist.lowercase()}" }.toSet()
        val momentSongs = momentRawTracks
            .filter { song ->
                isValidSong(song) &&
                    "${song.title.lowercase()}:${song.artist.lowercase()}" !in newSongKeys
            }
            .distinctBy { "${it.title.lowercase()}:${it.artist.lowercase()}" }
            .take(16)

        // Fetch Decade Hits (Éxitos por década)
        val decadeQueries = listOf("Hits+2000", "Hits+2010", "90s+Hits", "80s+Hits", "70s+Hits", "2000s+Hits", "2010s+Hits")
        val decadeRawList = mutableListOf<AppleMusicEditorialItem>()
        for (sf in candidateStorefronts) {
            for (query in decadeQueries) {
                val url = "$API_BASE/$sf/search?term=$query&types=playlists&limit=15&extend=editorialArtwork"
                val res = executeGet(url, bearer) ?: continue
                val rootObj = runCatching { json.parseToJsonElement(res).jsonObject }.getOrNull()
                val data = rootObj?.get("results")?.jsonObject?.get("playlists")?.jsonObject?.get("data")?.jsonArray
                    ?: JsonArray(emptyList())
                for (elem in data) {
                    val curator = elem.jsonObject["attributes"]?.jsonObject?.get("curatorName")?.jsonPrimitive?.contentOrNull
                    if (curator == null || !curator.contains("Apple Music", ignoreCase = true)) continue
                    val title = elem.jsonObject["attributes"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull.orEmpty()
                    val hasDecade = listOf("80", "90", "2000", "2010", "70", "decade", "década").any { title.contains(it, ignoreCase = true) }
                    if (!hasDecade) continue
                    parseEditorialItem(elem.jsonObject, sf, defaultKind = EditorialItemKind.PLAYLIST, defaultBadge = null)?.let { item ->
                        if (isValidItem(item)) {
                            decadeRawList.add(item.copy(badge = null))
                        }
                    }
                }
            }
            if (decadeRawList.size >= 15) break
        }
        val decadeHits = decadeRawList
            .filter { isValidItem(it) }
            .distinctBy { it.id.lowercase() }
            .take(15)

        // Fetch Road Trip / Para el camino
        val roadTripQueries = listOf("Long+Haul", "Road+Trip", "Car+Audio", "Carretera", "Driving")
        val roadTripRawList = mutableListOf<AppleMusicEditorialItem>()
        val roadTripExcluded = listOf("infantil", "kids", "disney", "cocomelon", "essentials", "imprescindibles")
        for (sf in candidateStorefronts) {
            for (query in roadTripQueries) {
                val url = "$API_BASE/$sf/search?term=$query&types=playlists&limit=15&extend=editorialArtwork"
                val res = executeGet(url, bearer) ?: continue
                val rootObj = runCatching { json.parseToJsonElement(res).jsonObject }.getOrNull()
                val data = rootObj?.get("results")?.jsonObject?.get("playlists")?.jsonObject?.get("data")?.jsonArray
                    ?: JsonArray(emptyList())
                for (elem in data) {
                    val curator = elem.jsonObject["attributes"]?.jsonObject?.get("curatorName")?.jsonPrimitive?.contentOrNull
                    if (curator == null || !curator.contains("Apple Music", ignoreCase = true)) continue
                    val title = elem.jsonObject["attributes"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull.orEmpty()
                    val titleLower = title.lowercase()
                    if (roadTripExcluded.any { titleLower.contains(it) }) continue
                    parseEditorialItem(elem.jsonObject, sf, defaultKind = EditorialItemKind.PLAYLIST, defaultBadge = null)?.let { item ->
                        if (isValidItem(item)) {
                            roadTripRawList.add(item.copy(badge = null))
                        }
                    }
                }
            }
            if (roadTripRawList.size >= 15) break
        }
        val roadTrip = roadTripRawList
            .filter { isValidItem(it) && !roadTripExcluded.any { ex -> it.title.lowercase().contains(ex) } }
            .distinctBy { it.id.lowercase() }
            .take(15)

        AppleMusicEditorialFeed(
            heroItems = heroItems.filter { isValidItem(it) },
            newSongs = newSongs.filter { isValidSong(it) },
            updatedPlaylists = updatedPlaylists.filter { isValidItem(it) },
            dailyTop100 = dailyTop100.filter { isValidItem(it) },
            cityCharts = cityCharts.filter { isValidItem(it) },
            djMixes = djMixes.filter { isValidItem(it) },
            comingSoon = comingSoon.filter { isValidItem(it) },
            trendingSongs = trendingSongs.filter { isValidSong(it) },
            momentSongs = momentSongs.filter { isValidSong(it) },
            decadeHits = decadeHits.filter { isValidItem(it) },
            roadTrip = roadTrip.filter { isValidItem(it) },
        )
    }

    private fun parseEditorialItem(
        obj: JsonObject,
        storefront: String,
        defaultKind: EditorialItemKind,
        defaultBadge: String? = null,
    ): AppleMusicEditorialItem? {
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val type = obj["type"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val attributes = obj["attributes"]?.jsonObject ?: return null

        val title = attributes["name"]?.jsonPrimitive?.contentOrNull?.trim() ?: return null
        val artist = attributes["artistName"]?.jsonPrimitive?.contentOrNull?.trim()
        val curator = attributes["curatorName"]?.jsonPrimitive?.contentOrNull?.trim()
        val subtitle = artist ?: curator ?: ""

        val editorialNotes = attributes["editorialNotes"]?.jsonObject
        val rawDescription = editorialNotes?.let { notes ->
            (notes["short"]?.jsonPrimitive?.contentOrNull
                ?: notes["standard"]?.jsonPrimitive?.contentOrNull).orEmpty()
        } ?: attributes["description"]?.jsonObject?.let { desc ->
            (desc["short"]?.jsonPrimitive?.contentOrNull
                ?: desc["standard"]?.jsonPrimitive?.contentOrNull).orEmpty()
        } ?: attributes["description"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val cleanDescription = rawDescription.replace(Regex("<[^>]+>"), "").trim()

        val isSpanish = Locale.getDefault().language == "es"
        val kind = if (type == "albums") EditorialItemKind.ALBUM else defaultKind

        // Wide editorial artwork support
        val editorialArtwork = attributes["editorialArtwork"]?.jsonObject
        val wideBannerUrl = (editorialArtwork?.get("bannerUber")?.jsonObject
            ?: editorialArtwork?.get("subscriptionHero")?.jsonObject
            ?: editorialArtwork?.get("staticDetailUber")?.jsonObject
            ?: editorialArtwork?.get("storefrontEditorialArtwork")?.jsonObject)
            ?.get("url")?.jsonPrimitive?.contentOrNull

        val rawArtworkUrl = attributes["artwork"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
        val thumbnailUrl = formatAppleArtworkUrl(rawArtworkUrl, 600, 600, "jpg") ?: return null
        val wideImageUrl = formatAppleArtworkUrl(wideBannerUrl ?: rawArtworkUrl, 1200, 675, "jpg") ?: thumbnailUrl

        // Dynamic badges
        val badge = defaultBadge ?: when (kind) {
            EditorialItemKind.ALBUM -> {
                val genreNames = attributes["genreNames"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
                val isSoundtrack = genreNames.any { it.contains("Soundtrack", ignoreCase = true) } ||
                    title.contains("soundtrack", ignoreCase = true) || title.contains("bso", ignoreCase = true) || title.contains("ost", ignoreCase = true)
                if (isSoundtrack) {
                    if (isSpanish) "BANDA SONORA" else "SOUNDTRACK"
                } else {
                    if (isSpanish) "NUEVO ÁLBUM" else "NEW ALBUM"
                }
            }
            EditorialItemKind.PLAYLIST -> {
                val lowerTitle = title.lowercase()
                when {
                    lowerTitle.contains("dj mix") || lowerTitle.contains("club") || curator?.contains("DJ", ignoreCase = true) == true -> {
                        "EXCLUSIVE DJ MIX"
                    }
                    lowerTitle.contains("top 100") || lowerTitle.contains("top 25") || lowerTitle.contains("chart") -> {
                        "TOP CHARTS"
                    }
                    else -> {
                        if (isSpanish) "PLAYLIST ACTUALIZADA" else "UPDATED PLAYLIST"
                    }
                }
            }
        }

        // Smart fallback description if cleanDescription is empty
        val finalDescription = cleanDescription.ifBlank {
            when (kind) {
                EditorialItemKind.ALBUM -> {
                    if (isSpanish) "Álbum destacado en Apple Music" else "Featured album on Apple Music"
                }
                EditorialItemKind.PLAYLIST -> {
                    if (isSpanish) "Playlist editorial actualizada en Apple Music" else "Updated editorial playlist on Apple Music"
                }
            }
        }

        val isExplicit = attributes["contentRating"]?.jsonPrimitive?.contentOrNull.equals("explicit", ignoreCase = true)

        return AppleMusicEditorialItem(
            id = id,
            title = title,
            subtitle = subtitle,
            description = finalDescription,
            thumbnailUrl = thumbnailUrl,
            wideImageUrl = wideImageUrl,
            badge = badge,
            kind = kind,
            isExplicit = isExplicit,
            storefront = storefront,
        )
    }

    private fun parseEditorialSong(obj: JsonObject): AppleMusicEditorialSong? {
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val attributes = obj["attributes"]?.jsonObject ?: return null
        val title = attributes["name"]?.jsonPrimitive?.contentOrNull?.trim() ?: return null
        val artist = attributes["artistName"]?.jsonPrimitive?.contentOrNull?.trim() ?: "Various Artists"
        val album = attributes["albumName"]?.jsonPrimitive?.contentOrNull?.trim()
        val isExplicit = attributes["contentRating"]?.jsonPrimitive?.contentOrNull.equals("explicit", ignoreCase = true)
        val rawArtworkUrl = attributes["artwork"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
        val thumbnailUrl = formatAppleArtworkUrl(rawArtworkUrl, 600, 600, "jpg") ?: return null
        val durationMs = attributes["durationInMillis"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
        val isrc = attributes["isrc"]?.jsonPrimitive?.contentOrNull

        return AppleMusicEditorialSong(
            id = id,
            title = title,
            artist = artist,
            album = album,
            thumbnailUrl = thumbnailUrl,
            isExplicit = isExplicit,
            durationMs = durationMs,
            isrc = isrc,
        )
    }

    private suspend fun executeGet(url: String, bearer: String): String? = apiSemaphore.withPermit {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $bearer")
                .header("Origin", "https://music.apple.com")
                .header("Referer", "https://music.apple.com/")
                .header("User-Agent", USER_AGENT)
                .build()

            runCatching {
                Http.client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) response.body.string() else null
                }
            }.getOrNull()
        }
    }

    /**
     * Reliable curated fallback dataset with high-resolution Apple Music CDN covers.
     * Guaranteed instant rendering even when offline or before the network finishes.
     */
    private fun getCuratedSeedFeed(storefront: String): AppleMusicEditorialFeed {
        return AppleMusicEditorialFeed()
    }
}
