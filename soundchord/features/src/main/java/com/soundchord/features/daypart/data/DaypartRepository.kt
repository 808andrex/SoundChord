package com.soundchord.features.daypart.data

import android.content.Context
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.ShelfLayout
import com.soundchord.features.daypart.model.Daypart
import com.soundchord.features.editorial.data.AppleMusicEditorialRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    fun init(context: Context) {
        DaypartDatabaseCache.init(context)
        AppleMusicEditorialRepository.init(context)
    }

    fun getCacheKey(
        daypart: Daypart,
        date: LocalDate = LocalDate.now(),
        genreKey: String = "general",
        artistKey: String = "mix",
    ): String {
        return "${daypart.key}_${genreKey}_${artistKey}_$date"
    }

    /**
     * Reads cached shelves immediately from SQLite (0 ms wait).
     */
    fun getCachedShelves(
        daypart: Daypart = Daypart.current(),
        date: LocalDate = LocalDate.now(),
    ): List<HomeShelf>? {
        val prefix = "${daypart.key}_"
        return DaypartDatabaseCache.getByPrefix(prefix, date.toString())
            ?: DaypartDatabaseCache.get(getCacheKey(daypart, date))
            ?: DaypartDatabaseCache.get("${daypart.key}_$date")
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
        date: LocalDate = LocalDate.now(),
        dayOfWeek: DayOfWeek = date.dayOfWeek,
        forceRefresh: Boolean = false,
    ): List<HomeShelf> = withContext(Dispatchers.IO) {
        val topArtists = UserTasteDaypartEngine.extractUserArtists(homeShelves, userArtists)
        val featuredArtist = UserTasteDaypartEngine.selectFeaturedArtist(topArtists, daypart, date) ?: "Tus artistas"
        val genreConfig = UserTasteDaypartEngine.selectGenreForDaypart(topArtists, daypart, date)
        val primaryGenre = UserTasteDaypartEngine.inferPrimaryGenre(topArtists, daypart)

        val artistKey = featuredArtist.filter { it.isLetter() }.take(8).lowercase(Locale.ROOT).ifBlank { "mix" }
        val genreKey = genreConfig.title.filter { it.isLetter() }.take(8).lowercase(Locale.ROOT).ifBlank { "genre" }
        val cacheKey = getCacheKey(daypart, date, genreKey, artistKey)

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
            date = date,
            dayOfWeek = dayOfWeek,
        )

        if (resolved.isNotEmpty()) {
            DaypartDatabaseCache.put(cacheKey, resolved)
            DaypartDatabaseCache.put(getCacheKey(daypart, date), resolved)
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
        date: LocalDate,
        dayOfWeek: DayOfWeek,
    ): List<HomeShelf> = coroutineScope {
        val seedBase = date.toEpochDay() * 100L
        val isWeekend = dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY

        // 1. Estante de Género + Hora del Día (Filtro estricto curador y género para coincidir 100% con el título)
        val q1 = async {
            searchCuratedPool(
                query = genreConfig.query,
                limit = 25,
                genre = genreConfig.genre,
                excludedCurators = genreConfig.excludedCuratorKeywords,
            )
        }

        // 2. Estante Basado en Artista Favorito ("Porque escuchas a [Artista]")
        val shelf2Title = if (featuredArtist != "Tus artistas") "Porque escuchas a $featuredArtist" else "Para fans de ${primaryGenre.displayName}"
        val shelf2Sub = if (featuredArtist != "Tus artistas") "Playlists inspiradas en su estilo y artistas similares" else "Playlists según tus gustos musicales"
        val q2 = async { searchArtistPlaylists(featuredArtist, primaryGenre) }

        // 3. Estante Dinámico según Franja Horaria (Sin subtítulo gris descriptivo)
        val (shelf3Title, shelf3Query) = when (daypart) {
            Daypart.MORNING -> Pair("Para concentrarte", "lofi focus beats instrumental concentración estudio")
            Daypart.AFTERNOON -> Pair("Tardeo y chill", "indie chill r&b chill pop acústico tardeo relax desconectar")
            Daypart.EVENING -> Pair("Para relajarte", "r&b suave soul nocturno ambient calma chill noche")
            Daypart.NIGHT -> Pair("Sesión de madrugada", "madrugada r&b oscuro beats nocturnos atmospheric soul dark chill")
        }
        val q3 = async { searchCuratedPool(shelf3Query, 25, excludeHeavy = true) }

        // 4. Estante Contextual de Energía / Descanso
        val (shelf4Title, shelf4Sub, shelf4Query, shelf4ExcludeHeavy) = when {
            daypart == Daypart.MORNING -> Tuple4(
                "Para empezar con ritmo",
                "Playlists con energía dentro de tus géneros favoritos",
                "${primaryGenre.searchTerms} energía pop ritmo mañana despertar",
                false,
            )
            daypart == Daypart.AFTERNOON -> Tuple4(
                "Tendencias en tu estilo",
                "Los éxitos del momento dentro de lo que más escuchas",
                "${primaryGenre.searchTerms} tendencias hits virales top del momento",
                false,
            )
            isWeekend && daypart == Daypart.EVENING -> Tuple4(
                "Vibra de fin de semana",
                "Playlists de previa, fiesta y noche de fin de semana",
                "${primaryGenre.searchTerms} fiesta previa party reggaeton dance fin de semana",
                false,
            )
            else -> Tuple4(
                "Para bajar el ritmo",
                "Sonidos tranquilos, acústicos y chill para desconectar",
                "${primaryGenre.searchTerms} suave acústico chill baladas relax descanso soul",
                true,
            )
        }
        val q4 = async { searchCuratedPool(shelf4Query, 25, excludeHeavy = shelf4ExcludeHeavy) }

        val items1 = selectDailyPlaylists(
            blendGenrePool(q1.await(), genreConfig),
            seed = seedBase + 1L,
        )

        val items2 = selectDailyPlaylists(
            q2.await(),
            seed = seedBase + 2L,
        )

        val items3 = selectDailyPlaylists(
            blendWithFallbacks(q3.await(), editorialShelves, "chill", "relax", "focus", "lofi", excludeHeavy = true),
            seed = seedBase + 3L,
        )

        val items4 = selectDailyPlaylists(
            blendWithFallbacks(q4.await(), editorialShelves, "ritmo", "tendencias", "chill", "suave", excludeHeavy = shelf4ExcludeHeavy),
            seed = seedBase + 4L,
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
     */
    suspend fun getPersonalizedEssentialsShelf(
        homeShelves: List<HomeShelf>,
        editorialShelves: List<HomeShelf>,
        userArtists: List<String>,
        date: LocalDate = LocalDate.now(),
        forceRefresh: Boolean = false,
    ): HomeShelf? = withContext(Dispatchers.IO) {
        val cacheKey = "essentials_user_${date}"
        if (!forceRefresh) {
            val cached = DaypartDatabaseCache.get(cacheKey)
            if (!cached.isNullOrEmpty()) return@withContext cached.firstOrNull()
        }

        val topArtists = UserTasteDaypartEngine.extractUserArtists(homeShelves, userArtists)
        val candidateItems = mutableListOf<ShelfItem>()
        val seenArtists = mutableSetOf<String>()

        for (artist in topArtists.take(12)) {
            val cleanArtist = artist.trim()
            val lowerArtist = cleanArtist.lowercase(Locale.ROOT)
            if (seenArtists.contains(lowerArtist)) continue

            val essentials = searchCuratedPool("$cleanArtist Essentials imprescindibles", limit = 10)
                .filter { item ->
                    val t = item.title.lowercase(Locale.ROOT)
                    val s = item.subtitle.lowercase(Locale.ROOT)
                    !t.contains("video essentials") && !s.contains("video") &&
                        (t.contains("essentials") || t.contains("imprescindibles") || t.contains(lowerArtist))
                }
            val best = essentials.firstOrNull()
            if (best != null) {
                candidateItems.add(best)
                seenArtists.add(lowerArtist)
            }
        }

        if (candidateItems.size < 15) {
            val fallbackPool = editorialShelves.firstOrNull {
                val t = it.title.lowercase(Locale.ROOT)
                t.contains("imprescindibles") || t.contains("essentials")
            }?.items.orEmpty()

            for (item in fallbackPool) {
                if (candidateItems.size >= 18) break
                val t = item.title.lowercase(Locale.ROOT)
                if (t.contains("video essentials")) continue
                val artistKey = t.replace("essentials", "").replace("imprescindibles", "").replace(":", "").trim()
                if (artistKey.isNotBlank() && seenArtists.contains(artistKey)) continue
                if (candidateItems.none { it.browseId == item.browseId || it.title.equals(item.title, ignoreCase = true) }) {
                    candidateItems.add(item)
                    if (artistKey.isNotBlank()) seenArtists.add(artistKey)
                }
            }
        }

        if (candidateItems.isEmpty()) return@withContext null

        val seed = date.toEpochDay() * 50L
        val cleanItems = selectDailyPlaylists(candidateItems, seed = seed, targetCount = 18)

        val shelf = HomeShelf(
            title = "Imprescindibles",
            subtitle = "Las canciones esenciales que no pueden faltar",
            items = cleanItems,
            layout = ShelfLayout.CARDS,
        )
        DaypartDatabaseCache.put(cacheKey, listOf(shelf))
        shelf
    }

    private suspend fun searchArtistPlaylists(
        artist: String?,
        genre: UserGenre,
    ): List<ShelfItem> {
        if (artist.isNullOrBlank() || artist == "Tus artistas") {
            return searchCuratedPool("${genre.searchTerms} esenciales imprescindibles", 25)
        }
        val p1 = searchCuratedPool("$artist Essentials imprescindibles", 20)
        val p2 = searchCuratedPool(artist, 20)
        val combined = (p1 + p2).filter { item ->
            val t = item.title.lowercase(Locale.ROOT)
            val s = item.subtitle.lowercase(Locale.ROOT)
            isValidCard(item) && !t.contains("video essentials") && (
                t.contains(artist.lowercase(Locale.ROOT)) ||
                s.contains(artist.lowercase(Locale.ROOT)) ||
                t.contains("essentials") ||
                t.contains("imprescindibles")
            )
        }.distinctBy { it.browseId ?: it.videoId ?: it.title }

        if (combined.isNotEmpty()) return combined

        return searchCuratedPool("${genre.displayName} imprescindibles hits", 20)
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
                        !UserTasteDaypartEngine.isFitness(item.title, item.subtitle) &&
                        !UserTasteDaypartEngine.isEventOrHoliday(item.title, item.subtitle) &&
                        (!excludeHeavy || !UserTasteDaypartEngine.isHeavyTempo(item.title, item.subtitle)) &&
                        excludedCurators.none { curator.contains(it) || title.contains(it) } &&
                        (genre == null || UserTasteDaypartEngine.matchesGenre(genre, title, curator)) &&
                        !JUNK_TERMS.any { title.contains(it) }
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
        return primary.filter { item ->
            isValidCard(item) &&
                config.excludedCuratorKeywords.none { item.subtitle.contains(it, ignoreCase = true) || item.title.contains(it, ignoreCase = true) } &&
                UserTasteDaypartEngine.matchesGenre(config.genre, item.title, item.subtitle)
        }
    }

    private fun selectDailyPlaylists(
        candidates: List<ShelfItem>,
        seed: Long,
        targetCount: Int = 12,
    ): List<ShelfItem> {
        if (candidates.isEmpty()) return emptyList()
        val clean = candidates
            .filter {
                !UserTasteDaypartEngine.isFitness(it.title, it.subtitle) &&
                !UserTasteDaypartEngine.isEventOrHoliday(it.title, it.subtitle)
            }
            .distinctBy { it.browseId ?: it.videoId ?: it.title }
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
        return pool.distinctBy { it.browseId ?: it.videoId ?: it.title }
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
