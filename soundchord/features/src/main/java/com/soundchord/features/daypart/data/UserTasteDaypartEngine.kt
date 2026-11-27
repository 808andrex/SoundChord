package com.soundchord.features.daypart.data

import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.soundchord.features.daypart.model.Daypart
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

enum class UserGenre(val displayName: String, val searchTerms: String) {
    URBANO("Urbano Latino", "urbano latino reggaeton perreo"),
    RNB("R&B", "r&b soul contemporáneo"),
    POP("Pop", "pop hits éxitos"),
    HIPHOP("Hip-Hop", "hip hop rap trap"),
    ELECTRONIC("Electrónica", "electrónica dance edm"),
    INDIE_ROCK("Indie & Rock", "indie rock alternativo"),
    LATIN("Música Latina", "latino salsa bachata tropical"),
    DEFAULT("Pop & R&B", "pop r&b contemporáneo");

    companion object {
        fun fromArtist(artistName: String): UserGenre? {
            val a = artistName.lowercase(Locale.ROOT)
            return when {
                URBANO_ARTISTS.any { a.contains(it) } -> URBANO
                RNB_ARTISTS.any { a.contains(it) } -> RNB
                HIPHOP_ARTISTS.any { a.contains(it) } -> HIPHOP
                POP_ARTISTS.any { a.contains(it) } -> POP
                ELECTRONIC_ARTISTS.any { a.contains(it) } -> ELECTRONIC
                ROCK_ARTISTS.any { a.contains(it) } -> INDIE_ROCK
                else -> null
            }
        }

        private val URBANO_ARTISTS = listOf(
            "bad bunny", "rauw alejandro", "feid", "karol g", "myke towers", "anuel",
            "mora", "eladio carri", "quevedo", "j balvin", "ozuna", "maluma",
            "ryan castro", "young miko", "blessd", "chencho", "nicky jam", "wisin",
            "yandel", "don omar", "arcangel", "sech", "manuel turizo", "camilo",
            "sebastian yatra", "jhayco", "trueno", "duki", "tiago pzk", "lit killah",
        )

        private val RNB_ARTISTS = listOf(
            "the weeknd", "sza", "frank ocean", "daniel caesar", "brent faiyaz", "giveon",
            "steve lacy", "bryson tiller", "kehlani", "summer walker", "jhene aiko",
            "jhené aiko", "miguel", "khalid", "h.e.r.", "partynextdoor", "dvsn", "tory lanez",
            "chris brown", "snoh aalegra", "alicia keys", "usher", "leon bridges",
        )

        private val HIPHOP_ARTISTS = listOf(
            "drake", "kendrick lamar", "travis scott", "kanye west", "eminem", "j. cole",
            "21 savage", "future", "metro boomin", "lil baby", "post malone", "asap rocky",
            "a\$ap rocky", "gunna", "lil uzi vert", "playboi carti", "jack harlow", "doja cat",
            "tyler, the creator", "mac miller", "juice wrld", "xxxtentacion", "cardi b",
        )

        private val POP_ARTISTS = listOf(
            "taylor swift", "ariana grande", "dua lipa", "olivia rodrigo", "billie eilish",
            "harry styles", "sabrina carpenter", "bruno mars", "ed sheeran", "justin bieber",
            "selena gomez", "shawn mendes", "katy perry", "beyonce", "beyoncé", "rihanna",
            "lady gaga", "miley cyrus", "charli xcx", "chappell roan", "camila cabello",
            "tate mcrae", "lorde", "troye sivan", "shakira", "rosalia", "rosalía",
        )

        private val ELECTRONIC_ARTISTS = listOf(
            "bizarrap", "calvin harris", "david guetta", "avicii", "tiësto", "tiesto",
            "fred again", "skrillex", "daft punk", "peggy gou", "martin garrix", "marshmello",
            "alok", "kygo", "the chainsmokers", "disclosure", "swedish house mafia",
        )

        private val ROCK_ARTISTS = listOf(
            "arctic monkeys", "coldplay", "imagine dragons", "the neighbourhood", "lana del rey",
            "cigarettes after sex", "mac demarco", "the strokes", "radiohead", "queen",
            "nirvana", "linkin park", "red hot chili peppers", "gorillaz", "tame impala",
            "paramore", "green day", "oasis", "twenty one pilots", "muse",
        )
    }
}

/**
 * Engine generating 100% personalized dynamic daypart shelves for SoundChord Home.
 * Evaluates the user's real listening history and top artists from Room/YouTube Music,
 * combined with the current time of day and day of week.
 */
object UserTasteDaypartEngine {

    val FITNESS_JUNK_TERMS = listOf(
        "fitness", "cardio", "pure cardio", "puro cardio", "pure gym", "puro gym",
        "pure running", "puro running", "treadmill", "cycling", "spinning", "pilates",
        "hiit", "crossfit", "cinta de correr", "entreno", "entrenamiento",
        "apple music fitness", "apple music entreno", "gym", "workout",
    )

    val EVENT_AND_HOLIDAY_JUNK = listOf(
        "navidad", "navideñ", "christmas", "villancico", "xmas", "holiday",
        "boda", "wedding", "matrimonio", "casamiento",
        "cumpleaños", "birthday", "kids", "niños", "infantil",
    )

    val HEAVY_TEMPO_JUNK = listOf(
        "perreo", "dembow", "guaracha", "bellakeo", "farruko",
        "gym", "workout", "cardio", "entreno", "crossfit",
        "hardcore", "heavy metal", "drill", "pesado",
    )

    fun isFitness(title: String, subtitle: String = ""): Boolean {
        val t = title.lowercase(Locale.ROOT)
        val s = subtitle.lowercase(Locale.ROOT)
        return FITNESS_JUNK_TERMS.any { t.contains(it) || s.contains(it) }
    }

    fun isEventOrHoliday(title: String, subtitle: String = ""): Boolean {
        val t = title.lowercase(Locale.ROOT)
        val s = subtitle.lowercase(Locale.ROOT)
        return EVENT_AND_HOLIDAY_JUNK.any { t.contains(it) || s.contains(it) }
    }

    fun isHeavyTempo(title: String, subtitle: String = ""): Boolean {
        val t = title.lowercase(Locale.ROOT)
        val s = subtitle.lowercase(Locale.ROOT)
        return HEAVY_TEMPO_JUNK.any { t.contains(it) || s.contains(it) }
    }

    fun sanitizeArtist(raw: String): String? {
        var s = raw.trim()
        if (s.isBlank()) return null

        // If string contains bullet points, isolate the artist segment
        if (s.contains("•")) {
            val parts = s.split("•").map { it.trim() }
            val valid = parts.filter { p ->
                !p.equals("Canción", ignoreCase = true) &&
                    !p.equals("Song", ignoreCase = true) &&
                    !p.equals("Video", ignoreCase = true) &&
                    !p.equals("Álbum", ignoreCase = true) &&
                    !p.equals("Album", ignoreCase = true) &&
                    !p.equals("Single", ignoreCase = true) &&
                    !p.equals("EP", ignoreCase = true) &&
                    !p.equals("Apple Music", ignoreCase = true) &&
                    !p.contains("Playlist", ignoreCase = true) &&
                    !p.matches(Regex("""\d{1,2}:\d{2}""")) &&
                    !p.matches(Regex("""\d{4}"""))
            }
            s = valid.firstOrNull { it.isNotBlank() } ?: return null
        }

        // Split collaborations / featuring to keep the primary artist
        val splitRegex = Regex("""(?i)\s*(?:feat\.?|ft\.?|&|,|;| y | with | x )\s*""")
        val primary = s.split(splitRegex).firstOrNull()?.trim() ?: s

        val lower = primary.lowercase(Locale.ROOT)
        if (primary.length in 2..40 &&
            !lower.contains("playlist") &&
            !lower.contains("apple music") &&
            !lower.contains("soundchord") &&
            !lower.contains("various artists") &&
            !lower.contains("varios artistas") &&
            !lower.contains("desconocido") &&
            !lower.contains("desastre") &&
            !lower.contains("running") &&
            !lower.contains("canción") &&
            !lower.contains("song")
        ) {
            return primary
        }
        return null
    }

    fun extractUserArtists(homeShelves: List<HomeShelf>, extraArtists: List<String> = emptyList()): List<String> {
        val artists = mutableListOf<String>()

        extraArtists.forEach { raw ->
            val clean = sanitizeArtist(raw)
            if (clean != null) artists.add(clean)
        }

        val listenAgain = homeShelves.firstOrNull { s ->
            val label = s.title.trim().lowercase(Locale.ROOT)
            label.contains("listen again") || label.contains("vuelve a escuchar") || label.contains("escuchar de nuevo")
        }
        listenAgain?.items?.forEach { item ->
            // Subtitle holds the artist info (never take item.title which is track title)
            val clean = sanitizeArtist(item.subtitle)
            if (clean != null) {
                artists.add(clean)
            }
        }

        homeShelves.forEach { shelf ->
            val title = shelf.title.trim()
            if (title.contains("Mix", ignoreCase = true)) {
                val candidate = title
                    .replace("Mix de ", "", ignoreCase = true)
                    .replace("Mix de", "", ignoreCase = true)
                    .replace(" Mix", "", ignoreCase = true)
                    .replace("Mix", "", ignoreCase = true)
                    .trim()
                if (candidate.isNotBlank() && candidate.length > 2 &&
                    !candidate.startsWith("Mi ", ignoreCase = true) &&
                    !candidate.startsWith("My ", ignoreCase = true)
                ) {
                    val clean = sanitizeArtist(candidate)
                    if (clean != null) {
                        artists.add(clean)
                    }
                }
            }
        }

        return artists
            .filter { it.length > 1 }
            .groupBy { it.lowercase(Locale.ROOT) }
            .entries
            .sortedByDescending { it.value.size }
            .map { it.value.first() }
            .take(15)
    }

    fun selectFeaturedArtist(topArtists: List<String>, daypart: Daypart, date: LocalDate): String? {
        if (topArtists.isEmpty()) return null
        val daypartIndex = daypart.ordinal // 0: MORNING, 1: AFTERNOON, 2: EVENING, 3: NIGHT
        val dayEpoch = date.toEpochDay()
        val artistIndex = (((dayEpoch * 4 + daypartIndex) % topArtists.size).toInt() + topArtists.size) % topArtists.size
        return topArtists[artistIndex]
    }

    fun rankUserGenres(topArtists: List<String>): List<UserGenre> {
        val scores = mutableMapOf<UserGenre, Int>()
        topArtists.forEach { artist ->
            val genre = UserGenre.fromArtist(artist)
            if (genre != null) {
                scores[genre] = (scores[genre] ?: 0) + 1
            }
        }
        return scores.entries.sortedByDescending { it.value }.map { it.key }
    }

    fun inferPrimaryGenre(topArtists: List<String>, daypart: Daypart): UserGenre {
        val ranked = rankUserGenres(topArtists)
        return ranked.firstOrNull() ?: when (daypart) {
            Daypart.MORNING -> UserGenre.POP
            Daypart.AFTERNOON -> UserGenre.URBANO
            Daypart.EVENING -> UserGenre.RNB
            Daypart.NIGHT -> UserGenre.RNB
        }
    }

    fun selectGenreForDaypart(
        topArtists: List<String>,
        daypart: Daypart,
        date: LocalDate = LocalDate.now(),
    ): DaypartGenreConfig {
        val ranked = rankUserGenres(topArtists)
        val hasPop = ranked.contains(UserGenre.POP)
        val hasRnb = ranked.contains(UserGenre.RNB)
        val hasUrbano = ranked.contains(UserGenre.URBANO)
        val hasHiphop = ranked.contains(UserGenre.HIPHOP)
        val hasElectronic = ranked.contains(UserGenre.ELECTRONIC)
        val hasRock = ranked.contains(UserGenre.INDIE_ROCK)

        return when (daypart) {
            Daypart.MORNING -> {
                when {
                    hasPop && hasRnb -> DaypartGenreConfig(
                        title = "Pop y R&B para empezar el día",
                        query = "pop r&b suave mañana despertar acoustic hits",
                        genre = UserGenre.POP,
                        excludedCuratorKeywords = listOf("urbano latino", "reggaeton", "perreo", "dembow"),
                    )
                    hasPop -> DaypartGenreConfig(
                        title = "Pop para empezar el día",
                        query = "pop suave mañana despertar acoustic hits",
                        genre = UserGenre.POP,
                        excludedCuratorKeywords = listOf("urbano latino", "reggaeton"),
                    )
                    hasRnb -> DaypartGenreConfig(
                        title = "R&B para la mañana",
                        query = "r&b soul contemporáneo suave despertar acústico",
                        genre = UserGenre.RNB,
                        excludedCuratorKeywords = listOf("urbano latino", "reggaeton"),
                    )
                    hasRock -> DaypartGenreConfig(
                        title = "Indie & Acústico para empezar el día",
                        query = "indie acústico folk suave despertar",
                        genre = UserGenre.INDIE_ROCK,
                        excludedCuratorKeywords = listOf("urbano latino", "reggaeton"),
                    )
                    else -> DaypartGenreConfig(
                        title = "Pop y R&B para empezar el día",
                        query = "pop r&b suave acústico mañana",
                        genre = UserGenre.POP,
                        excludedCuratorKeywords = listOf("urbano latino", "reggaeton"),
                    )
                }
            }
            Daypart.AFTERNOON -> {
                when {
                    hasUrbano -> DaypartGenreConfig(
                        title = "Tarde de Urbano Latino",
                        query = "urbano latino reggaeton tarde éxitos ritmo",
                        genre = UserGenre.URBANO,
                        excludedCuratorKeywords = emptyList(),
                    )
                    hasHiphop -> DaypartGenreConfig(
                        title = "Tarde de Hip-Hop",
                        query = "hip hop rap trap tarde ritmo",
                        genre = UserGenre.HIPHOP,
                        excludedCuratorKeywords = listOf("urbano latino"),
                    )
                    hasPop -> DaypartGenreConfig(
                        title = "Tarde de Pop Hits",
                        query = "pop hits tarde ritmo éxitos",
                        genre = UserGenre.POP,
                        excludedCuratorKeywords = listOf("urbano latino"),
                    )
                    hasElectronic -> DaypartGenreConfig(
                        title = "Tarde de Electrónica",
                        query = "electrónica dance edm tarde ritmo",
                        genre = UserGenre.ELECTRONIC,
                        excludedCuratorKeywords = listOf("urbano latino"),
                    )
                    else -> DaypartGenreConfig(
                        title = "Tarde de Urbano Latino",
                        query = "urbano latino reggaeton tarde éxitos",
                        genre = UserGenre.URBANO,
                        excludedCuratorKeywords = emptyList(),
                    )
                }
            }
            Daypart.EVENING -> {
                when {
                    hasRnb -> DaypartGenreConfig(
                        title = "Noches de R&B y Soul",
                        query = "r&b soul contemporáneo nocturno late night suave calma r&b now",
                        genre = UserGenre.RNB,
                        excludedCuratorKeywords = listOf("urbano latino", "reggaeton", "perreo", "dembow"),
                    )
                    hasRock -> DaypartGenreConfig(
                        title = "Noches de Indie & Chill",
                        query = "indie chill alternativo noche suave calma",
                        genre = UserGenre.INDIE_ROCK,
                        excludedCuratorKeywords = listOf("urbano latino", "reggaeton"),
                    )
                    hasPop -> DaypartGenreConfig(
                        title = "Noches de Pop & Baladas",
                        query = "pop acústico baladas noche suave",
                        genre = UserGenre.POP,
                        excludedCuratorKeywords = listOf("urbano latino", "reggaeton"),
                    )
                    else -> DaypartGenreConfig(
                        title = "Noches de R&B y Soul",
                        query = "r&b soul nocturno late night calma",
                        genre = UserGenre.RNB,
                        excludedCuratorKeywords = listOf("urbano latino", "reggaeton", "perreo"),
                    )
                }
            }
            Daypart.NIGHT -> {
                when {
                    hasHiphop && hasRnb -> DaypartGenreConfig(
                        title = "Hip-Hop y R&B nocturno",
                        query = "r&b now hip hop late night atmospheric mellow beats soul",
                        genre = UserGenre.RNB,
                        excludedCuratorKeywords = listOf("urbano latino", "reggaeton", "perreo", "dembow"),
                    )
                    hasHiphop -> DaypartGenreConfig(
                        title = "Hip-Hop nocturno",
                        query = "hip hop rap late night atmospheric dark chill mellow",
                        genre = UserGenre.HIPHOP,
                        excludedCuratorKeywords = listOf("urbano latino", "reggaeton", "perreo"),
                    )
                    hasRnb -> DaypartGenreConfig(
                        title = "R&B nocturno",
                        query = "r&b soul nocturno late night dark atmospheric r&b now",
                        genre = UserGenre.RNB,
                        excludedCuratorKeywords = listOf("urbano latino", "reggaeton", "perreo", "dembow"),
                    )
                    hasElectronic -> DaypartGenreConfig(
                        title = "Electrónica de madrugada",
                        query = "ambient downtempo electrónica noche dark beats",
                        genre = UserGenre.ELECTRONIC,
                        excludedCuratorKeywords = listOf("urbano latino", "reggaeton"),
                    )
                    else -> DaypartGenreConfig(
                        title = "Hip-Hop y R&B nocturno",
                        query = "r&b now hip hop nocturno atmospheric dark chill",
                        genre = UserGenre.RNB,
                        excludedCuratorKeywords = listOf("urbano latino", "reggaeton", "perreo", "dembow"),
                    )
                }
            }
        }
    }

    fun matchesGenre(genre: UserGenre, title: String, subtitle: String): Boolean {
        val t = title.lowercase(Locale.ROOT)
        val s = subtitle.lowercase(Locale.ROOT)
        return when (genre) {
            UserGenre.RNB -> {
                if (s.contains("urbano latino") || t.contains("reggaeton") || t.contains("perreo") || t.contains("dembow")) return false
                t.contains("r&b") || t.contains("soul") || s.contains("r&b") || s.contains("soul") ||
                    t.contains("the a-list: r&b") || t.contains("r&b now") || t.contains("grooves")
            }
            UserGenre.HIPHOP -> {
                if (s.contains("urbano latino") || t.contains("reggaeton") || t.contains("perreo") || t.contains("dembow")) return false
                t.contains("hip-hop") || t.contains("hip hop") || t.contains("rap") || t.contains("trap") ||
                    s.contains("hip-hop") || s.contains("rap")
            }
            UserGenre.URBANO -> {
                t.contains("urbano") || t.contains("latino") || t.contains("reggaeton") || t.contains("perreo") ||
                    s.contains("urbano") || s.contains("latino")
            }
            UserGenre.POP -> {
                if (s.contains("urbano latino") && !t.contains("pop")) return false
                t.contains("pop") || s.contains("pop") || t.contains("hits") || t.contains("éxitos")
            }
            UserGenre.ELECTRONIC -> {
                t.contains("dance") || t.contains("electronic") || t.contains("electrónica") || t.contains("edm")
            }
            UserGenre.INDIE_ROCK -> {
                t.contains("rock") || t.contains("indie") || t.contains("alternative") || t.contains("alternativo")
            }
            else -> true
        }
    }

    fun buildGenreDaypartTitle(genre: UserGenre, daypart: Daypart): String {
        return when (daypart) {
            Daypart.MORNING -> when (genre) {
                UserGenre.URBANO -> "Urbano para empezar el día"
                UserGenre.RNB -> "R&B para la mañana"
                else -> "${genre.displayName} para la mañana"
            }
            Daypart.AFTERNOON -> "Tarde de ${genre.displayName}"
            Daypart.EVENING, Daypart.NIGHT -> "Noches de ${genre.displayName}"
        }
    }

    fun buildGenreDaypartQuery(genre: UserGenre, daypart: Daypart): String {
        val base = genre.searchTerms
        return when (daypart) {
            Daypart.MORNING -> "$base suave mañana despertar acoustic"
            Daypart.AFTERNOON -> "$base tarde éxitos hits ritmo"
            Daypart.EVENING, Daypart.NIGHT -> "$base nocturno late night soul dark calma"
        }
    }
}

data class DaypartGenreConfig(
    val title: String,
    val query: String,
    val genre: UserGenre,
    val excludedCuratorKeywords: List<String> = emptyList(),
)
