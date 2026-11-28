package com.soundchord.features.daypart.data

import android.content.Context
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.ShelfLayout
import com.soundchord.features.daypart.model.Daypart
import com.soundchord.features.editorial.data.AppleMusicEditorialRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

/**
 * Repository orchestrating personalized dynamic time-of-day shelves (UserTasteDaypartEngine).
 * Combines the user's real listening history and favorite artists from Room/YouTube Music
 * with the active time-of-day slot and daily Room/SQLite database caching.
 */
object DaypartRepository {

    private val JUNK_TERMS = listOf(
        "karaoke", "sing king", "instrumental version", "8d audio", "sped up", "slowed",
        "lyrics", "letra", "nightcore", "bass boosted", "tribute",
        "track by track", "video essentials", "audio commentary", "comentario de audio",
    )

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        DaypartDatabaseCache.init(context)
        AppleMusicEditorialRepository.init(context)
    }

    private const val CACHE_VERSION = "v3"

    fun resolveAccountHash(identity: String?): String {
        if (identity.isNullOrBlank()) return "guest"
        val clean = identity.filter { it.isLetterOrDigit() }.take(12).lowercase(Locale.ROOT)
        val hash = (identity.hashCode().toLong() and 0xFFFFFFFFL).toString(16)
        return if (clean.isNotBlank()) "${clean}_$hash" else "user_$hash"
    }

    fun getCacheKey(
        daypart: Daypart,
        date: LocalDate = LocalDate.now(),
        genreKey: String = "general",
        artistKey: String = "mix",
        accountHash: String = "guest",
    ): String {
        return "${CACHE_VERSION}_${accountHash}_${daypart.key}_${genreKey}_${artistKey}_$date"
    }

    /**
     * Reads cached shelves immediately from SQLite (0 ms wait).
     */
    fun getCachedShelves(
        daypart: Daypart = Daypart.current(),
        date: LocalDate = LocalDate.now(),
        accountHash: String = "guest",
    ): List<HomeShelf>? {
        val prefix = "${CACHE_VERSION}_${accountHash}_${daypart.key}_"
        return DaypartDatabaseCache.getByPrefix(prefix, date.toString())
            ?: DaypartDatabaseCache.get(getCacheKey(daypart, date, accountHash = accountHash))
            ?: DaypartDatabaseCache.get("${CACHE_VERSION}_${accountHash}_${daypart.key}_$date")
    }

    /**
     * Resolves the 4 personalized shelves for the active daypart slot.
     * Checks SQLite cache first unless [forceRefresh] is true.
     */
    suspend fun getDaypartShelves(
        homeShelves: List<HomeShelf>,
        editorialShelves: List<HomeShelf>,
        daypart: Daypart = Daypart.current(),
        userArtists: List<String> = emptyList(),
        userGenreKeywords: List<String> = emptyList(),
        identity: String? = null,
        date: LocalDate = LocalDate.now(),
        dayOfWeek: DayOfWeek = date.dayOfWeek,
        forceRefresh: Boolean = false,
    ): List<HomeShelf> = withContext(Dispatchers.IO) {
        val accountHash = resolveAccountHash(identity)
        val topArtists = UserTasteDaypartEngine.extractUserArtists(homeShelves, userArtists)
        val featuredArtist = UserTasteDaypartEngine.selectFeaturedArtist(topArtists, daypart, date) ?: "Tus artistas"
        val genreConfig = UserTasteDaypartEngine.selectGenreForDaypart(topArtists, daypart, date, userGenreKeywords)
        val primaryGenre = UserTasteDaypartEngine.inferPrimaryGenre(topArtists, daypart, userGenreKeywords)

        val artistKey = featuredArtist.filter { it.isLetter() }.take(8).lowercase(Locale.ROOT).ifBlank { "mix" }
        val genreKey = genreConfig.title.filter { it.isLetter() }.take(8).lowercase(Locale.ROOT).ifBlank { "genre" }
        val cacheKey = getCacheKey(daypart, date, genreKey, artistKey, accountHash)

        if (!forceRefresh) {
            val cached = DaypartDatabaseCache.get(cacheKey)
            if (!cached.isNullOrEmpty()) return@withContext cached
        }

        // Prune stale cache entries from previous dates
        DaypartDatabaseCache.prunePreviousDates(date.toString())

        val resolved = resolvePersonalizedDaypartShelves(
            daypart = daypart,
            genreConfig = genreConfig,
            primaryGenre = primaryGenre,
            featuredArtist = featuredArtist,
            homeShelves = homeShelves,
            editorialShelves = editorialShelves,
            userGenreKeywords = userGenreKeywords,
            date = date,
            dayOfWeek = dayOfWeek,
        )

        if (resolved.isNotEmpty()) {
            DaypartDatabaseCache.put(cacheKey, resolved)
            DaypartDatabaseCache.put(getCacheKey(daypart, date, accountHash = accountHash), resolved)
        }
        resolved
    }

    private suspend fun resolvePersonalizedDaypartShelves(
        daypart: Daypart,
        genreConfig: DaypartGenreConfig,
        primaryGenre: UserGenre,
        featuredArtist: String,
        homeShelves: List<HomeShelf>,
        editorialShelves: List<HomeShelf>,
        userGenreKeywords: List<String>,
        date: LocalDate,
        dayOfWeek: DayOfWeek,
    ): List<HomeShelf> = coroutineScope {
        val seedBase = date.toEpochDay() * 100L
        val isWeekend = dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY

        // 1. Estante de Género + Hora del Día (Pool amplio editorial que combina género de la franja + subgéneros afines)
        val q1 = async {
            val queries = buildGenreDaypartQueries(genreConfig.genre, daypart)
            searchMultipleQueries(
                queries = queries,
                limitPerQuery = 8,
                targetCount = 12,
                genre = genreConfig.genre,
                excludedCurators = genreConfig.excludedCuratorKeywords,
            )
        }

        // 2. Estante Basado en Artista Favorito ("Porque escuchas a [Artista]")
        val shelf2Title = if (featuredArtist != "Tus artistas") "Porque escuchas a $featuredArtist" else "Para fans de ${primaryGenre.displayName}"
        val shelf2Sub = if (featuredArtist != "Tus artistas") "Playlists inspiradas en su estilo y artistas similares" else "Playlists según tus gustos musicales"
        val q2 = async { searchArtistPlaylists(featuredArtist, primaryGenre, userGenreKeywords) }

        // 3. Estante Dinámico según Franja Horaria (Ticket #77: Múltiples consultas editoriales combinadas con géneros de la cuenta activa)
        val (shelf3Title, shelf3Queries) = when (daypart) {
            Daypart.MORNING -> {
                val base = listOf("focus", "concentración", "lofi focus", "beats concentración", "instrumental focus", "piano relax")
                val genreCrossed = if (primaryGenre.isLatin) {
                    listOf("${primaryGenre.displayName} suave", "guitarra acústica relax", "música instrumental latina")
                } else {
                    listOf("${primaryGenre.displayName} focus", "acoustic focus")
                }
                Pair("Para concentrarte", base + genreCrossed)
            }
            Daypart.AFTERNOON -> {
                val base = listOf("chill", "tardeo", "acústico", "relax", "tarde", "lounge", "chillout", "desconectar")
                val genreCrossed = when {
                    primaryGenre == UserGenre.CUMBIA || primaryGenre == UserGenre.LATIN || primaryGenre == UserGenre.SALSA || primaryGenre == UserGenre.VALLENATO -> {
                        listOf("cumbia chill", "tropical relax", "acústico latino", "boleros relax", "latin chill", "tardeo latino")
                    }
                    primaryGenre == UserGenre.RNB -> listOf("r&b chill", "soul lounge", "r&b acústico", "chillout soul")
                    primaryGenre == UserGenre.POP || primaryGenre == UserGenre.LATIN_POP -> listOf("pop acústico", "pop chill", "pop relax", "baladas acústicas")
                    primaryGenre == UserGenre.INDIE_ROCK || primaryGenre == UserGenre.ROCK_EN_ESPANOL -> listOf("indie chill", "rock acústico", "rock suave")
                    primaryGenre == UserGenre.URBANO -> listOf("urbano chill", "reggaeton acústico", "suave latino", "tardeo")
                    else -> listOf("chillout", "acústico relax", "lounge tardes")
                }
                Pair("Tardeo y chill", base + genreCrossed)
            }
            Daypart.EVENING -> {
                val base = listOf("relax", "calma", "chill noche", "suave", "acústico", "nocturno", "ambient", "lounge")
                val genreCrossed = if (primaryGenre.isLatin) {
                    listOf("boleros suaves", "baladas románticas noche", "guitarra clásica relax", "salsa sensual noche")
                } else {
                    listOf("${primaryGenre.displayName} relax", "${primaryGenre.displayName} noche", "soul nocturno")
                }
                Pair("Para relajarte", base + genreCrossed)
            }
            Daypart.NIGHT -> {
                val base = listOf("madrugada", "nocturno", "dark chill", "beats nocturnos", "mellow", "calma", "late night", "desvelo")
                val genreCrossed = if (primaryGenre.isLatin) {
                    listOf("boleros de medianoche", "baladas nocturnas", "nostalgia madrugada", "clásicos nocturnos")
                } else {
                    listOf("${primaryGenre.displayName} nocturno", "atmospheric chill", "late night soul")
                }
                Pair("Sesión de madrugada", base + genreCrossed)
            }
        }
        val q3 = async {
            val queried = searchMultipleQueries(shelf3Queries, limitPerQuery = 6, targetCount = 12, excludeHeavy = true)
            blendWithFallbacks(queried, editorialShelves, "chill", "relax", "focus", "lofi", "tardeo", "suave", excludeHeavy = true)
        }

        // 4. Estante Contextual de Energía / Descanso (Múltiples consultas cruzadas)
        val (shelf4Title, shelf4Sub, shelf4Queries, shelf4ExcludeHeavy) = when {
            daypart == Daypart.MORNING -> Tuple4(
                "Para empezar con ritmo",
                "Playlists con energía dentro de tus géneros favoritos",
                listOf("${primaryGenre.displayName} ritmo", "${primaryGenre.displayName} energía", "${primaryGenre.displayName} despertar", "éxitos con energía", "mañana con ritmo"),
                false,
            )
            daypart == Daypart.AFTERNOON -> Tuple4(
                "Tendencias en tu estilo",
                "Los éxitos del momento dentro de lo que más escuchas",
                listOf("${primaryGenre.displayName} éxitos", "${primaryGenre.displayName} hits", "${primaryGenre.displayName} tendencias", "éxitos de hoy", "tendencias del momento"),
                false,
            )
            isWeekend && daypart == Daypart.EVENING -> Tuple4(
                "Vibra de fin de semana",
                "Playlists de previa, fiesta y noche de fin de semana",
                listOf("${primaryGenre.displayName} fiesta", "${primaryGenre.displayName} fin de semana", "previa y fiesta", "fiesta fin de semana", "party hits"),
                false,
            )
            else -> Tuple4(
                "Para bajar el ritmo",
                "Sonidos tranquilos, acústicos y chill para desconectar",
                listOf("${primaryGenre.displayName} suave", "${primaryGenre.displayName} chill", "${primaryGenre.displayName} relax", "acústico suave", "baladas relax"),
                true,
            )
        }
        val q4 = async {
            val queried = searchMultipleQueries(shelf4Queries, limitPerQuery = 6, targetCount = 12, excludeHeavy = shelf4ExcludeHeavy)
            blendWithFallbacks(queried, editorialShelves, "ritmo", "tendencias", "chill", "suave", "fiesta", excludeHeavy = shelf4ExcludeHeavy)
        }

        val items1 = selectDailyPlaylists(
            blendGenrePool(q1.await(), genreConfig),
            seed = seedBase + 1L,
            targetCount = 12,
        )

        val items2 = selectDailyPlaylists(
            q2.await(),
            seed = seedBase + 2L,
            targetCount = 12,
        )

        val items3 = selectDailyPlaylists(
            q3.await(),
            seed = seedBase + 3L,
            targetCount = 12, // Entre 10 y 12 playlists válidas para Ticket #77
        )

        val items4 = selectDailyPlaylists(
            q4.await(),
            seed = seedBase + 4L,
            targetCount = 12,
        )

        listOfNotNull(
            buildShelf(genreConfig.title, "Playlists según tus gustos para este momento", items1),
            buildShelf(shelf2Title, shelf2Sub, items2),
            buildShelf(shelf3Title, "", items3), // Subtítulo vacío para encabezado limpio
            buildShelf(shelf4Title, shelf4Sub, items4),
        )
    }

    /**
     * Resolves dynamically personalized "Imprescindibles" shelf based on the user's top artists.
     * Guarantees between 10 and 12 distinct Essentials playlists.
     */
    suspend fun getPersonalizedEssentialsShelf(
        homeShelves: List<HomeShelf>,
        editorialShelves: List<HomeShelf>,
        userArtists: List<String>,
        userGenreKeywords: List<String> = emptyList(),
        identity: String? = null,
        date: LocalDate = LocalDate.now(),
        forceRefresh: Boolean = false,
    ): HomeShelf? = withContext(Dispatchers.IO) {
        val accountHash = resolveAccountHash(identity)
        val cacheKey = "${CACHE_VERSION}_essentials_${accountHash}_$date"
        if (!forceRefresh) {
            val cached = DaypartDatabaseCache.get(cacheKey)
            if (!cached.isNullOrEmpty()) {
                val cachedShelf = cached.firstOrNull()
                // Require between 10 and 12 items so user is never stuck with 1 card
                if (cachedShelf != null && cachedShelf.items.size in 10..12) {
                    return@withContext cachedShelf
                } else if (cachedShelf != null && cachedShelf.items.size > 12) {
                    return@withContext cachedShelf.copy(items = cachedShelf.items.take(12))
                }
            }
        }

        // 1. Extract up to 25 real artists from user history & rotate order daily
        val topArtists = UserTasteDaypartEngine.extractUserArtists(homeShelves, userArtists, limit = 25)
        val rotatedArtists = UserTasteDaypartEngine.rotateArtistsDaily(topArtists, date)

        val isLatinUser = topArtists.any { a -> UserGenre.fromArtist(a)?.isLatin == true } ||
            userGenreKeywords.any { kw -> UserGenre.detectGenreFromKeyword(kw)?.isLatin == true } ||
            UserTasteDaypartEngine.rankUserGenres(topArtists, userGenreKeywords).any { it.isLatin }

        val candidateItems = mutableListOf<ShelfItem>()
        val seenArtists = mutableSetOf<String>()
        val seenBrowseIds = mutableSetOf<String>()
        val seenTitles = mutableSetOf<String>()

        fun addCandidate(item: ShelfItem): Boolean {
            val browseId = item.browseId?.trim().orEmpty()
            if (browseId.isNotBlank() && seenBrowseIds.contains(browseId)) return false

            val normTitle = item.title.trim().lowercase(Locale.ROOT)
            if (seenTitles.contains(normTitle)) return false

            val t = item.title.lowercase(Locale.ROOT)
            val s = item.subtitle.lowercase(Locale.ROOT)
            if (t.contains("video essentials") || item.isVideo) return false
            if (UserTasteDaypartEngine.isFitness(item.title, item.subtitle)) return false
            if (UserTasteDaypartEngine.isEventOrHoliday(item.title, item.subtitle)) return false
            if (isLatinUser && (isGenericEnglishPop(t, s) || isNonLatinArtist(t, s))) return false

            val artistKey = UserTasteDaypartEngine.extractArtistKey(item.title, item.subtitle)
            if (artistKey.isNotBlank() && seenArtists.contains(artistKey)) return false

            candidateItems.add(item)
            if (browseId.isNotBlank()) seenBrowseIds.add(browseId)
            seenTitles.add(normTitle)
            if (artistKey.isNotBlank()) seenArtists.add(artistKey)
            return true
        }

        // 2. Query in parallel with concurrency semaphore (Semaphore = 4)
        val gate = Semaphore(4)
        val chunkSize = 6

        for (chunk in rotatedArtists.chunked(chunkSize)) {
            if (candidateItems.size >= 12) break
            coroutineScope {
                val deferred = chunk.map { artist ->
                    async {
                        gate.withPermit {
                            searchArtistEssentials(artist)
                        }
                    }
                }
                for (item in deferred.awaitAll().filterNotNull()) {
                    addCandidate(item)
                    if (candidateItems.size >= 12) break
                }
            }
        }

        // 3. Smart genre affinity fill if user history yielded fewer than 10 Essentials playlists
        // Searches within the REAL genres the active account listens to (strictly prohibited Rock/Pop fallback for Latin listeners)
        if (candidateItems.size < 12) {
            val similarArtistsPool = mutableListOf<String>()
            if (isLatinUser) {
                // If user listens to cumbia / salsa / tropical / boleros / popular:
                // Complete exclusively with Essentials of verified Latin/Tropical artists with Apple Music playlists
                val latinFlagship = listOf(
                    "Marc Anthony", "Juan Luis Guerra", "Carlos Vives", "Los Ángeles Azules",
                    "Gilberto Santa Rosa", "Romeo Santos", "Rubén Blades", "Joe Arroyo",
                    "Celia Cruz", "Héctor Lavoe", "Willie Colón", "El Gran Combo de Puerto Rico",
                    "Grupo Niche", "Fania All Stars", "Aventura", "Elvis Crespo", "Eddy Herrera",
                    "Pastor López", "Sonora Dinamita", "Diomedes Díaz", "Silvestre Dangond",
                    "Binomio de Oro de América", "Jorge Celedón", "Darío Gómez", "Julio Jaramillo", "Alci Acosta",
                )
                val rotatedLatin = UserTasteDaypartEngine.rotateArtistsDaily(latinFlagship, date)
                for (artist in rotatedLatin) {
                    val lower = artist.lowercase(Locale.ROOT)
                    if (!seenArtists.contains(lower) && !similarArtistsPool.any { it.equals(artist, ignoreCase = true) }) {
                        similarArtistsPool.add(artist)
                    }
                }
            } else {
                val dominantGenres = UserTasteDaypartEngine.rankUserGenres(topArtists, userGenreKeywords)
                    .ifEmpty { listOf(UserGenre.POP, UserGenre.URBANO, UserGenre.RNB, UserGenre.HIPHOP) }
                for (genre in dominantGenres) {
                    val genreArtists = UserGenre.getSimilarArtistsForGenre(genre)
                    val rotatedGenreArtists = UserTasteDaypartEngine.rotateArtistsDaily(genreArtists, date)
                    for (artist in rotatedGenreArtists) {
                        val lower = artist.lowercase(Locale.ROOT)
                        if (!seenArtists.contains(lower) && !similarArtistsPool.any { it.equals(artist, ignoreCase = true) }) {
                            similarArtistsPool.add(artist)
                        }
                    }
                }
            }

            for (chunk in similarArtistsPool.chunked(chunkSize)) {
                if (candidateItems.size >= 12) break
                coroutineScope {
                    val deferred = chunk.map { artist ->
                        async {
                            gate.withPermit {
                                searchArtistEssentials(artist)
                            }
                        }
                    }
                    for (item in deferred.awaitAll().filterNotNull()) {
                        addCandidate(item)
                        if (candidateItems.size >= 12) break
                    }
                }
            }
        }

        // 4. Fallback from editorial shelves if still under 10 (network / catalogue constraints)
        if (candidateItems.size < 10) {
            val fallbackPool = editorialShelves.firstOrNull {
                val t = it.title.lowercase(Locale.ROOT)
                t.contains("imprescindibles") || t.contains("essentials")
            }?.items.orEmpty()

            for (item in fallbackPool) {
                if (candidateItems.size >= 12) break
                if (!isValidCard(item)) continue
                val t = item.title.lowercase(Locale.ROOT)
                val s = item.subtitle.lowercase(Locale.ROOT)
                if (t.contains("video essentials") || item.isVideo) continue
                if (UserTasteDaypartEngine.isFitness(item.title, item.subtitle)) continue
                if (isLatinUser && (isGenericEnglishPop(t, s) || isNonLatinArtist(t, s))) continue
                addCandidate(item)
            }
        }

        if (candidateItems.isEmpty()) return@withContext null

        val finalItems = candidateItems.take(12)

        val shelfTitle = appContext?.getString(com.soundchord.features.R.string.shelf_essentials) ?: "Imprescindibles"

        val shelf = HomeShelf(
            title = shelfTitle,
            subtitle = "", // Encabezado limpio mostrando únicamente el título "Imprescindibles"
            items = finalItems,
            layout = ShelfLayout.CARDS,
        )

        // 5. Daily Room/SQLite cache: persist full batch of 10 to 12 items for instant cold start
        if (finalItems.size >= 10) {
            DaypartDatabaseCache.put(cacheKey, listOf(shelf))
        }
        shelf
    }

    private suspend fun searchArtistEssentials(artist: String): ShelfItem? {
        val cleanArtist = artist.trim()
        val lowerArtist = cleanArtist.lowercase(Locale.ROOT)
        if (lowerArtist.isBlank()) return null

        val queries = listOf("$cleanArtist Essentials", "$cleanArtist imprescindibles")
        for (query in queries) {
            val candidates = searchCuratedPool(query, limit = 6)
            val match = candidates.firstOrNull { item ->
                isValidEssentialsCard(item, lowerArtist)
            }
            if (match != null) return match
        }
        return null
    }

    private fun isValidEssentialsCard(item: ShelfItem, lowerArtist: String): Boolean {
        if (!isValidCard(item)) return false
        val t = item.title.lowercase(Locale.ROOT)
        val s = item.subtitle.lowercase(Locale.ROOT)

        if (t.contains("video essentials") || s.contains("video essentials") || item.isVideo) return false
        if (t.contains("videoclips") || t.contains("video") || s.contains("video")) return false
        if (UserTasteDaypartEngine.isFitness(item.title, item.subtitle)) return false
        if (t.contains("track by track") || t.contains("audio commentary") || t.contains("comentario de audio")) return false

        val isEssentialsTitle = t.contains("essentials") || t.contains("imprescindibles")
        if (!isEssentialsTitle) return false

        val titleArtist = UserTasteDaypartEngine.extractArtistKey(item.title, item.subtitle)
        val artistMatches = t.contains(lowerArtist) || s.contains(lowerArtist) ||
            (titleArtist.isNotBlank() && (lowerArtist.contains(titleArtist) || titleArtist.contains(lowerArtist)))

        return artistMatches
    }

    private fun isGenericEnglishPop(title: String, subtitle: String): Boolean {
        val t = title.lowercase(Locale.ROOT)
        val s = subtitle.lowercase(Locale.ROOT)
        val popEnglishArtists = listOf(
            "justin bieber", "backstreet boys", "britney spears", "nsync", "taylor swift",
            "ariana grande", "katy perry", "shawn mendes", "miley cyrus", "ed sheeran",
            "harry styles", "sabrina carpenter", "chappell roan", "charli xcx", "selena gomez",
            "jonas brothers", "one direction", "maroon 5", "demi lovato", "billie eilish",
        )
        return popEnglishArtists.any { t.contains(it) || s.contains(it) }
    }

    private fun isNonLatinArtist(title: String, subtitle: String): Boolean {
        val t = title.lowercase(Locale.ROOT)
        val s = subtitle.lowercase(Locale.ROOT)
        val nonLatinArtists = listOf(
            "linkin park", "red hot chili peppers", "gorillaz", "coldplay", "arctic monkeys",
            "queen", "nirvana", "the strokes", "radiohead", "green day", "tame impala",
            "imagine dragons", "oasis", "paramore", "the neighbourhood", "cigarettes after sex",
            "metallica", "ac/dc", "the beatles", "pink floyd", "led zeppelin", "guns n' roses",
            "justin bieber", "backstreet boys", "britney spears", "nsync", "taylor swift",
            "ariana grande", "katy perry", "shawn mendes", "miley cyrus", "ed sheeran",
            "billie eilish", "dua lipa", "olivia rodrigo", "harry styles", "sabrina carpenter",
            "chappell roan", "charli xcx", "the weeknd", "drake", "travis scott", "eminem",
            "kendrick lamar", "kanye west", "post malone", "jack harlow", "21 savage",
            "calvin harris", "david guetta", "avicii", "tiësto", "skrillex", "marshmello",
        )
        return nonLatinArtists.any { t.contains(it) || s.contains(it) }
    }

    private fun deduplicateItems(items: List<ShelfItem>): List<ShelfItem> {
        val seenIds = mutableSetOf<String>()
        val seenTitles = mutableSetOf<String>()
        val result = mutableListOf<ShelfItem>()
        for (item in items) {
            val id = item.browseId?.trim()?.takeIf { it.isNotBlank() }
            val normTitle = item.title.trim().lowercase(Locale.ROOT)
            if (id != null && seenIds.contains(id)) continue
            if (seenTitles.contains(normTitle)) continue
            if (id != null) seenIds.add(id)
            seenTitles.add(normTitle)
            result.add(item)
        }
        return result
    }

    private fun buildGenreDaypartQueries(genre: UserGenre, daypart: Daypart): List<String> = when (genre) {
        UserGenre.CUMBIA, UserGenre.LATIN -> listOf(
            "cumbia", "salsa", "tropical", "merengue", "vallenato", "bachata",
            "fiesta latina", "música latina", "éxitos tropicales", "cumbia colombiana",
            "salsa clásica", "cumbia sonidera", "fiesta tropical", "¡Dale Play!",
        )
        UserGenre.SALSA -> listOf(
            "salsa", "salsa clásica", "salsa dura", "salsa sensual", "tropical",
            "fania", "fiesta latina", "música latina", "salsa romántica",
        )
        UserGenre.VALLENATO -> listOf(
            "vallenato", "vallenatos clásicos", "vallenato de oro", "parranda",
            "parranda vallenata", "tropical", "música latina",
        )
        UserGenre.REGIONAL_POPULAR -> listOf(
            "música popular", "popular colombiana", "despecho", "cantina",
            "rancheras", "regional", "éxitos de cantina", "regional mexicano",
        )
        UserGenre.BOLERO_BALADA -> listOf(
            "boleros", "boleros del recuerdo", "baladas románticas", "baladas del recuerdo",
            "clásicos en español", "nostalgia", "boleros de oro",
        )
        UserGenre.ROCK_EN_ESPANOL -> listOf(
            "rock en español", "rock en tu idioma", "rock latino", "alternativo en español",
            "rock argentino", "rock mexicano",
        )
        UserGenre.LATIN_POP -> listOf(
            "pop latino", "baladas pop", "pop en español", "éxitos pop", "latin pop", "¡Dale Play!",
        )
        UserGenre.URBANO -> listOf(
            "urbano latino", "reggaeton", "trap latino", "fuego", "dale reggaetón", "vibras", "perreo",
        )
        UserGenre.RNB -> listOf(
            "r&b now", "r&b", "soul", "after hours", "slow grooves", "neo-soul", "soul lounge",
        )
        UserGenre.HIPHOP -> listOf(
            "rap life", "hip-hop hits", "trap kingz", "hip hop", "the plug", "beats",
        )
        UserGenre.INDIE_ROCK -> listOf(
            "alt ctrl", "indie anthems", "modern rock", "indie rock", "rock classics",
        )
        UserGenre.ELECTRONIC -> listOf(
            "dancexl", "club room", "electronic chill", "dance hits", "edm", "electronic",
        )
        UserGenre.POP -> listOf(
            "today's hits", "a-list pop", "pop hits", "pure pop", "chart hits", "pop deluxe",
        )
        UserGenre.DEFAULT -> {
            if (genre.isLatin) {
                listOf("cumbia", "salsa", "tropical", "fiesta latina", "música latina", "cumbia colombiana", "éxitos tropicales")
            } else {
                listOf("today's hits", "a-list pop", "r&b now", "chart hits", "pop hits")
            }
        }
    }

    private suspend fun searchArtistPlaylists(
        artist: String?,
        genre: UserGenre,
        userKeywords: List<String> = emptyList(),
    ): List<ShelfItem> = coroutineScope {
        if (artist.isNullOrBlank() || artist == "Tus artistas") {
            val terms = if (genre.isLatin) listOf("${genre.displayName} hits", "${genre.displayName} esenciales", "fiesta latina")
            else listOf("${genre.searchTerms} esenciales imprescindibles", "${genre.displayName} hits")
            return@coroutineScope searchMultipleQueries(terms, targetCount = 10)
        }

        val cleanArtist = artist.trim()
        val lowerArtist = cleanArtist.lowercase(Locale.ROOT)
        val artistGenre = UserTasteDaypartEngine.detectArtistGenre(cleanArtist, userKeywords) ?: genre

        // 1. Direct query for the seed artist in Apple Music
        val p1 = searchCuratedPool(cleanArtist, 15)
        val p2 = searchCuratedPool("$cleanArtist Essentials", 10)
        val directMatches = (p1 + p2).filter { item ->
            isValidCard(item) && (
                item.title.lowercase(Locale.ROOT).contains(lowerArtist) ||
                item.subtitle.lowercase(Locale.ROOT).contains(lowerArtist) ||
                isValidEssentialsCard(item, lowerArtist)
            )
        }.distinctBy { it.browseId ?: it.videoId ?: it.title }

        val gathered = directMatches.toMutableList()

        // 2. If under 8 items, fill with editorial playlists and Essentials of SAME GENRE and similar style artists
        if (gathered.size < 8) {
            val genreQueries = when (artistGenre) {
                UserGenre.CUMBIA -> listOf(
                    "Cumbia colombiana", "Cumbia sonidera", "Fiesta Tropical", "Cumbias del recuerdo",
                    "Los Ángeles Azules Essentials", "Pastor López Essentials", "Sonora Dinamita Essentials",
                    "Lisandro Meza Essentials", "Grupo 5 Essentials", "Salsa clásica",
                )
                UserGenre.SALSA -> listOf(
                    "Salsa clásica", "Salsa sensual", "Salsa dura", "Fania All Stars Essentials",
                    "Grupo Niche Essentials", "Joe Arroyo Essentials", "Héctor Lavoe Essentials",
                    "Willie Colón Essentials", "El Gran Combo Essentials", "Celia Cruz Essentials",
                )
                UserGenre.VALLENATO -> listOf(
                    "Vallenato clásico", "Vallenato de oro", "Parranda vallenata",
                    "Diomedes Díaz Essentials", "Binomio de Oro Essentials", "Silvestre Dangond Essentials",
                    "Jorge Celedón Essentials", "Los Inquietos del Vallenato Essentials",
                )
                UserGenre.REGIONAL_POPULAR -> listOf(
                    "Música Popular colombiana", "Éxitos de cantina", "Despecho", "Rancheras clásicas",
                    "Darío Gómez Essentials", "Yeison Jiménez Essentials", "Jessi Uribe Essentials",
                    "Vicente Fernández Essentials", "Carin León Essentials", "Christian Nodal Essentials",
                )
                UserGenre.BOLERO_BALADA -> listOf(
                    "Boleros del recuerdo", "Baladas de oro", "Boleros románticos",
                    "Julio Jaramillo Essentials", "Los Panchos Essentials", "Armando Manzanero Essentials",
                    "José José Essentials", "Camilo Sesto Essentials", "Juan Gabriel Essentials",
                )
                UserGenre.ROCK_EN_ESPANOL -> listOf(
                    "Rock en español", "Rock en tu idioma", "Rock latino",
                    "Soda Stereo Essentials", "Café Tacvba Essentials", "Hombres G Essentials",
                    "Los Fabulosos Cadillacs Essentials", "Maná Essentials", "Andrés Calamaro Essentials",
                )
                UserGenre.LATIN_POP -> listOf(
                    "Pop latino", "Baladas pop en español", "Hits latinos",
                    "Shakira Essentials", "Chayanne Essentials", "Luis Fonsi Essentials",
                    "Enrique Iglesias Essentials", "Alejandro Sanz Essentials", "Morat Essentials",
                )
                UserGenre.LATIN -> listOf(
                    "¡Dale Play!", "Fiesta Tropical", "Merengue clásico", "Bachata romántica",
                    "Juan Luis Guerra Essentials", "Carlos Vives Essentials", "Celia Cruz Essentials",
                    "Marc Anthony Essentials", "Elvis Crespo Essentials", "Eddy Herrera Essentials",
                )
                UserGenre.URBANO -> listOf(
                    "Fuego", "Dale Reggaetón", "Trap Kingz", "Vibras",
                    "Bad Bunny Essentials", "Feid Essentials", "Karol G Essentials",
                    "Rauw Alejandro Essentials", "J Balvin Essentials", "Daddy Yankee Essentials",
                )
                UserGenre.RNB -> listOf(
                    "R&B Now", "After Hours", "Soul Lounge", "Slow Grooves",
                    "The Weeknd Essentials", "SZA Essentials", "Frank Ocean Essentials",
                    "Brent Faiyaz Essentials", "Daniel Caesar Essentials",
                )
                UserGenre.HIPHOP -> listOf(
                    "Rap Life", "Trap Kingz", "Hip-Hop Hits",
                    "Drake Essentials", "Kendrick Lamar Essentials", "Travis Scott Essentials",
                    "J. Cole Essentials", "Eminem Essentials",
                )
                UserGenre.INDIE_ROCK -> listOf(
                    "Indie Anthems", "Alt CTRL", "Modern Rock",
                    "Arctic Monkeys Essentials", "Coldplay Essentials", "The Strokes Essentials",
                    "Radiohead Essentials", "Tame Impala Essentials",
                )
                UserGenre.ELECTRONIC -> listOf(
                    "danceXL", "Club Room", "Electronic Chill",
                    "Calvin Harris Essentials", "David Guetta Essentials", "Avicii Essentials",
                    "Tiësto Essentials", "Daft Punk Essentials",
                )
                UserGenre.POP -> listOf(
                    "A-List Pop", "Pop Hits", "Today's Hits",
                    "Dua Lipa Essentials", "Ariana Grande Essentials", "Olivia Rodrigo Essentials",
                    "Bruno Mars Essentials", "Billie Eilish Essentials",
                )
                UserGenre.DEFAULT -> {
                    if (artistGenre.isLatin) {
                        listOf(
                            "Cumbia colombiana", "Salsa clásica", "Fiesta Tropical",
                            "Los Ángeles Azules Essentials", "Pastor López Essentials", "Sonora Dinamita Essentials",
                        )
                    } else {
                        listOf(
                            "A-List Pop", "R&B Now", "Indie Anthems",
                            "The Weeknd Essentials", "Dua Lipa Essentials", "Coldplay Essentials",
                        )
                    }
                }
            }

            val gate = Semaphore(4)
            for (query in genreQueries) {
                if (gathered.size >= 12) break
                val results = gate.withPermit { searchCuratedPool(query, limit = 6) }
                for (item in results) {
                    if (gathered.size >= 12) break
                    if (!isValidCard(item)) continue
                    val t = item.title.lowercase(Locale.ROOT)
                    val s = item.subtitle.lowercase(Locale.ROOT)
                    // Strict protection: FORBIDDEN fallback to generic English pop or Rock if artist is Latin!
                    if (artistGenre.isLatin && (isGenericEnglishPop(t, s) || isNonLatinArtist(t, s))) continue
                    val normTitle = item.title.trim().lowercase(Locale.ROOT)
                    if (gathered.none { it.browseId == item.browseId || it.title.trim().lowercase(Locale.ROOT) == normTitle }) {
                        gathered.add(item)
                    }
                }
            }
        }

        deduplicateItems(gathered).take(12)
    }

    private suspend fun searchMultipleQueries(
        queries: List<String>,
        limitPerQuery: Int = 8,
        targetCount: Int = 12,
        excludeHeavy: Boolean = false,
        genre: UserGenre? = null,
        excludedCurators: List<String> = emptyList(),
    ): List<ShelfItem> = coroutineScope {
        val gate = Semaphore(4)
        val deferred = queries.map { q ->
            async {
                gate.withPermit {
                    searchCuratedPool(
                        query = q,
                        limit = limitPerQuery,
                        excludeHeavy = excludeHeavy,
                        genre = genre,
                        excludedCurators = excludedCurators,
                    )
                }
            }
        }
        val gathered = deduplicateItems(
            deferred.awaitAll().flatten().filter { isValidCard(it) }
        )

        if (gathered.size >= targetCount) {
            gathered.take(targetCount)
        } else {
            gathered
        }
    }

    private suspend fun searchCuratedPool(
        query: String,
        limit: Int = 25,
        excludeHeavy: Boolean = false,
        genre: UserGenre? = null,
        excludedCurators: List<String> = emptyList(),
    ): List<ShelfItem> {
        return runCatching {
            AppleMusicEditorialRepository.searchPlaylists(query, limit = limit, excludeFitness = true)
                .filter { item ->
                    val curator = item.subtitle.lowercase(Locale.ROOT)
                    val title = item.title.lowercase(Locale.ROOT)
                    AppleMusicEditorialRepository.isValidItem(item) &&
                        (item.trackCount == null || item.trackCount > 0) &&
                        !UserTasteDaypartEngine.isFitness(item.title, item.subtitle) &&
                        !UserTasteDaypartEngine.isEventOrHoliday(item.title, item.subtitle) &&
                        (!excludeHeavy || !UserTasteDaypartEngine.isHeavyTempo(item.title, item.subtitle)) &&
                        !UserTasteDaypartEngine.isGreyOrExternalCurator(curator) &&
                        !UserTasteDaypartEngine.isGreyOrExternalCurator(title) &&
                        excludedCurators.none { curator.contains(it) || title.contains(it) } &&
                        (genre == null || UserTasteDaypartEngine.matchesGenre(genre, title, curator)) &&
                        !JUNK_TERMS.any { title.contains(it) || curator.contains(it) }
                }
                .map { item ->
                    val curator = item.subtitle.trim().ifBlank { "Apple Music" }
                    ShelfItem(
                        title = item.title,
                        subtitle = "Playlist • $curator",
                        thumbnailUrl = item.thumbnailUrl,
                        videoId = null,
                        browseId = "apple:playlist:${item.storefront.ifBlank { "us" }}:${item.id}",
                    )
                }
        }.getOrNull().orEmpty()
    }

    private fun blendGenrePool(
        primary: List<ShelfItem>,
        config: DaypartGenreConfig,
    ): List<ShelfItem> {
        val filtered = primary.filter { item ->
            isValidCard(item) &&
                config.excludedCuratorKeywords.none { item.subtitle.contains(it, ignoreCase = true) || item.title.contains(it, ignoreCase = true) } &&
                UserTasteDaypartEngine.matchesGenre(config.genre, item.title, item.subtitle)
        }
        return if (filtered.size >= 8) filtered else primary.filter { isValidCard(it) }
    }

    private fun selectDailyPlaylists(
        candidates: List<ShelfItem>,
        seed: Long,
        targetCount: Int = 12,
    ): List<ShelfItem> {
        if (candidates.isEmpty()) return emptyList()
        val clean = deduplicateItems(
            candidates.filter {
                !UserTasteDaypartEngine.isFitness(it.title, it.subtitle) &&
                !UserTasteDaypartEngine.isEventOrHoliday(it.title, it.subtitle)
            }
        )
        if (clean.size <= targetCount) return clean
        val rng = kotlin.random.Random(seed)
        return clean.shuffled(rng).take(targetCount)
    }

    private fun isValidCard(item: ShelfItem): Boolean {
        if (!AppleMusicEditorialRepository.isValidArtworkUrl(item.thumbnailUrl)) return false
        val titleLower = item.title.lowercase(Locale.ROOT)
        val subLower = item.subtitle.lowercase(Locale.ROOT)
        if (UserTasteDaypartEngine.isFitness(titleLower, subLower)) return false
        if (UserTasteDaypartEngine.isEventOrHoliday(titleLower, subLower)) return false
        if (JUNK_TERMS.any { titleLower.contains(it) }) return false
        return true
    }

    private fun blendWithFallbacks(
        primary: List<ShelfItem>,
        sources: List<HomeShelf>,
        vararg keywords: String,
        excludeHeavy: Boolean = false,
    ): List<ShelfItem> {
        val pool = primary.filter { isValidCard(it) && (!excludeHeavy || !UserTasteDaypartEngine.isHeavyTempo(it.title, it.subtitle)) }.toMutableList()
        if (pool.size < 25) {
            val matches = sources.flatMap { it.items }
                .filter { item ->
                    isValidCard(item) &&
                        (!excludeHeavy || !UserTasteDaypartEngine.isHeavyTempo(item.title, item.subtitle)) &&
                        keywords.any { k -> item.title.lowercase(Locale.ROOT).contains(k) || item.subtitle.lowercase(Locale.ROOT).contains(k) } &&
                        pool.none { it.browseId == item.browseId || it.title.equals(item.title, ignoreCase = true) }
                }
            pool.addAll(matches)
        }
        if (pool.size < 12) {
            val general = sources.flatMap { it.items }
                .filter {
                    isValidCard(it) &&
                        (!excludeHeavy || !UserTasteDaypartEngine.isHeavyTempo(it.title, it.subtitle)) &&
                        pool.none { p -> p.browseId == it.browseId || p.title.equals(it.title, ignoreCase = true) }
                }
            pool.addAll(general.take(15 - pool.size))
        }
        return deduplicateItems(pool)
    }

    private fun buildShelf(title: String, subtitle: String, items: List<ShelfItem>): HomeShelf? {
        if (items.isEmpty()) return null
        return HomeShelf(
            title = title,
            subtitle = subtitle,
            items = items,
            layout = ShelfLayout.CARDS,
        )
    }

    private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
