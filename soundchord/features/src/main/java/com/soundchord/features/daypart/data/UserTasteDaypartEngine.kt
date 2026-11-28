package com.soundchord.features.daypart.data

import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.soundchord.features.daypart.model.Daypart
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

enum class UserGenre(val displayName: String, val searchTerms: String) {
    CUMBIA("Cumbia & Tropical", "cumbia cumbia colombiana cumbia sonidera fiesta tropical"),
    SALSA("Salsa", "salsa salsa clásica salsa dura salsa romántica"),
    VALLENATO("Vallenato", "vallenato vallenatos clásicos vallenato de oro parranda"),
    REGIONAL_POPULAR("Música Popular & Regional", "música popular despecho rancheras cantina regional"),
    BOLERO_BALADA("Boleros & Baladas", "boleros del recuerdo baladas románticas clásicos"),
    ROCK_EN_ESPANOL("Rock en Español", "rock en español rock latino alternativo en español"),
    LATIN_POP("Pop Latino", "pop latino baladas pop en español hits latinos"),
    LATIN("Música Latina", "latino salsa bachata tropical merengue"),
    URBANO("Urbano Latino", "urbano latino reggaeton perreo"),
    RNB("R&B", "r&b soul contemporáneo"),
    POP("Pop", "pop hits éxitos"),
    HIPHOP("Hip-Hop", "hip hop rap trap"),
    ELECTRONIC("Electrónica", "electrónica dance edm"),
    INDIE_ROCK("Indie & Rock", "indie rock alternativo"),
    DEFAULT("Pop & R&B", "pop r&b contemporáneo");

    val isLatin: Boolean
        get() = when (this) {
            CUMBIA, SALSA, VALLENATO, REGIONAL_POPULAR, BOLERO_BALADA,
            ROCK_EN_ESPANOL, LATIN_POP, LATIN, URBANO -> true
            else -> false
        }

    companion object {
        fun fromArtist(artistName: String): UserGenre? {
            val a = artistName.lowercase(Locale.ROOT)
            return when {
                CUMBIA_ARTISTS.any { a.contains(it) } -> CUMBIA
                SALSA_ARTISTS.any { a.contains(it) } -> SALSA
                VALLENATO_ARTISTS.any { a.contains(it) } -> VALLENATO
                REGIONAL_POPULAR_ARTISTS.any { a.contains(it) } -> REGIONAL_POPULAR
                BOLERO_BALADA_ARTISTS.any { a.contains(it) } -> BOLERO_BALADA
                ROCK_EN_ESPANOL_ARTISTS.any { a.contains(it) } -> ROCK_EN_ESPANOL
                LATIN_POP_ARTISTS.any { a.contains(it) } -> LATIN_POP
                LATIN_ARTISTS.any { a.contains(it) } -> LATIN
                URBANO_ARTISTS.any { a.contains(it) } -> URBANO
                RNB_ARTISTS.any { a.contains(it) } -> RNB
                HIPHOP_ARTISTS.any { a.contains(it) } -> HIPHOP
                POP_ARTISTS.any { a.contains(it) } -> POP
                ELECTRONIC_ARTISTS.any { a.contains(it) } -> ELECTRONIC
                ROCK_ARTISTS.any { a.contains(it) } -> INDIE_ROCK
                else -> null
            }
        }

        fun detectGenreFromKeyword(keyword: String): UserGenre? {
            val k = keyword.lowercase(Locale.ROOT)
            return when {
                k.contains("cumbia") || k.contains("chicha") || k.contains("sonidera") || k.contains("villera") -> CUMBIA
                k.contains("salsa") || k.contains("guaguanco") || k.contains("timba") || k.contains("son cubano") -> SALSA
                k.contains("vallenato") || k.contains("acordeon") || k.contains("parranda") -> VALLENATO
                k.contains("popular") || k.contains("despecho") || k.contains("cantina") || k.contains("ranchera") ||
                    k.contains("corridos") || k.contains("norteño") || k.contains("banda") || k.contains("pasillo") ||
                    k.contains("sanjuanito") -> REGIONAL_POPULAR
                k.contains("bolero") || k.contains("baladas del recuerdo") || k.contains("balada romántica") -> BOLERO_BALADA
                k.contains("rock en español") || k.contains("rock latino") || k.contains("rock en tu idioma") -> ROCK_EN_ESPANOL
                k.contains("tropical") || k.contains("merengue") || k.contains("bachata") || k.contains("mambo") -> LATIN
                k.contains("reggaeton") || k.contains("urbano") || k.contains("perreo") || k.contains("dembow") -> URBANO
                k.contains("r&b") || k.contains("soul") -> RNB
                k.contains("hip-hop") || k.contains("hip hop") || k.contains("rap") || k.contains("trap") -> HIPHOP
                k.contains("dance") || k.contains("electronic") || k.contains("electrónica") || k.contains("edm") -> ELECTRONIC
                k.contains("indie") || k.contains("rock") || k.contains("alternative") || k.contains("alternativo") -> INDIE_ROCK
                k.contains("pop latino") || k.contains("pop en español") -> LATIN_POP
                k.contains("pop") -> POP
                else -> null
            }
        }

        fun detectArtistGenre(artist: String, contextKeywords: List<String> = emptyList()): UserGenre? {
            val direct = fromArtist(artist)
            if (direct != null) return direct

            for (kw in contextKeywords) {
                val g = detectGenreFromKeyword(kw)
                if (g != null) return g
            }
            return null
        }

        fun getSimilarArtistsForGenre(genre: UserGenre): List<String> = when (genre) {
            UserGenre.CUMBIA -> listOf(
                "Rodolfo Aicardi", "Los Ángeles Azules", "Pastor López", "Sonora Dinamita", "Lisandro Meza",
                "Los Corraleros de Majagual", "Los Hispanos", "Grupo 5", "Agua Marina", "Armonía 10",
                "Corazón Serrano", "Los Graduados", "Los Melódicos", "Gilda", "Los Palmeras", "Chicha Libre",
                "Rumba Habana La Herencia", "Lucho Bermúdez", "Pedro Laza", "Aníbal Velásquez", "Cuarteto Imperial",
                "Marc Anthony", "Juan Luis Guerra", "Carlos Vives", "Gilberto Santa Rosa", "Romeo Santos", "Rubén Blades", "Joe Arroyo",
            )
            UserGenre.SALSA -> listOf(
                "Grupo Niche", "Joe Arroyo", "Frankie Ruiz", "Héctor Lavoe", "Willie Colón", "Marc Anthony",
                "Rubén Blades", "Celia Cruz", "El Gran Combo de Puerto Rico", "Gilberto Santa Rosa", "Jerry Rivera",
                "Oscar D'León", "Maelo Ruiz", "Tito Nieves", "Victor Manuelle", "Eddie Santiago", "Orquesta Guayacán",
                "Fania All Stars", "Andy Montañez", "Ismael Rivera", "Juan Luis Guerra", "Carlos Vives", "Romeo Santos",
            )
            UserGenre.VALLENATO -> listOf(
                "Diomedes Díaz", "Binomio de Oro de América", "Silvestre Dangond", "Jorge Celedón",
                "Los Inquietos del Vallenato", "Los Chiches del Vallenato", "Nelson Velásquez", "Rafael Orozco",
                "Peter Manjarrés", "Kaleth Morales", "Jean Carlos Centeno", "Hebert Vargas", "Poncho Zuleta",
                "Los Diablitos", "Los Gigantes del Vallenato", "Patricia Teherán",
            )
            UserGenre.REGIONAL_POPULAR -> listOf(
                "Darío Gómez", "El Charrito Negro", "Luis Alberto Posada", "Yeison Jiménez", "Jessi Uribe",
                "Jhonny Rivera", "Arelys Henao", "Alzate", "Pipe Bueno", "Carin León", "Christian Nodal",
                "Vicente Fernández", "Alejandro Fernández", "Los Tigres del Norte", "Julio Jaramillo",
                "Alci Acosta", "Olimpo Cárdenas", "Segundo Rosero", "Gerardo Morán",
            )
            UserGenre.BOLERO_BALADA -> listOf(
                "Julio Jaramillo", "Alci Acosta", "Los Panchos", "Armando Manzanero", "José José",
                "Camilo Sesto", "Roberto Carlos", "Rocío Dúrcal", "Juan Gabriel", "Sandro", "Leo Dan",
                "Los Ángeles Negros", "Los Terrícolas", "Los Iracundos", "Nelson Ned",
            )
            UserGenre.ROCK_EN_ESPANOL -> listOf(
                "Soda Stereo", "Gustavo Cerati", "Enanitos Verdes", "Hombres G", "Café Tacvba",
                "Los Fabulosos Cadillacs", "Molotov", "Maná", "Andrés Calamaro", "Charly García",
                "Fito Páez", "Caifanes", "Zoé", "Babasónicos", "Vilma Palma e Vampiros", "Héroes del Silencio",
            )
            UserGenre.LATIN_POP -> listOf(
                "Shakira", "Chayanne", "Ricky Martin", "Luis Fonsi", "Enrique Iglesias", "Ricardo Arjona",
                "David Bisbal", "Alejandro Sanz", "Morat", "Reik", "Camila", "Sin Bandera",
                "Sebastián Yatra", "Camilo", "Ha*Ash", "Jesse & Joy",
            )
            UserGenre.LATIN -> listOf(
                "Juan Luis Guerra", "Marc Anthony", "Romeo Santos", "Aventura", "Carlos Vives",
                "Rubén Blades", "Celia Cruz", "Fonseca", "Eddy Herrera", "Elvis Crespo", "Wilfrido Vargas",
                "Olga Tañón", "Sergio Vargas", "Los Hermanos Rosario", "Rikarena", "Proyecto Uno", "Ilegales",
            )
            UserGenre.URBANO -> listOf(
                "Bad Bunny", "Feid", "Karol G", "Rauw Alejandro", "Myke Towers", "Mora",
                "Eladio Carrión", "Quevedo", "J Balvin", "Ozuna", "Maluma", "Anuel AA",
                "Arcángel", "Young Miko", "Ryan Castro", "Blessd", "Manuel Turizo", "Duki",
                "Trueno", "Chencho Corleone",
            )
            UserGenre.RNB -> listOf(
                "The Weeknd", "SZA", "Frank Ocean", "Daniel Caesar", "Brent Faiyaz", "Giveon",
                "Steve Lacy", "Bryson Tiller", "Kehlani", "Summer Walker", "Jhené Aiko",
                "Khalid", "H.E.R.", "PARTYNEXTDOOR", "Leon Bridges", "Usher", "Alicia Keys",
            )
            UserGenre.HIPHOP -> listOf(
                "Drake", "Kendrick Lamar", "Travis Scott", "Kanye West", "Eminem", "J. Cole",
                "21 Savage", "Future", "Metro Boomin", "Post Malone", "A\$AP Rocky", "Jack Harlow",
                "Tyler, The Creator", "Mac Miller", "Doja Cat",
            )
            UserGenre.POP -> listOf(
                "Taylor Swift", "Ariana Grande", "Dua Lipa", "Olivia Rodrigo", "Billie Eilish",
                "Harry Styles", "Sabrina Carpenter", "Bruno Mars", "Ed Sheeran", "Justin Bieber",
                "Selena Gomez", "Rihanna", "Lady Gaga", "Miley Cyrus", "Shakira", "Rosalía",
            )
            UserGenre.ELECTRONIC -> listOf(
                "Bizarrap", "Calvin Harris", "David Guetta", "Avicii", "Tiësto", "Fred again..",
                "Skrillex", "Daft Punk", "Martin Garrix", "Marshmello", "Kygo", "The Chainsmokers",
            )
            UserGenre.INDIE_ROCK -> listOf(
                "Arctic Monkeys", "Coldplay", "Imagine Dragons", "The Neighbourhood", "Lana Del Rey",
                "Cigarettes After Sex", "The Strokes", "Radiohead", "Queen", "Nirvana", "Linkin Park",
                "Red Hot Chili Peppers", "Gorillaz", "Tame Impala", "Paramore",
            )
            UserGenre.DEFAULT -> listOf(
                "Taylor Swift", "The Weeknd", "Bad Bunny", "Drake", "Billie Eilish",
                "Bruno Mars", "Dua Lipa", "Ariana Grande", "SZA", "Coldplay",
            )
        }

        private val CUMBIA_ARTISTS = listOf(
            "rodolfo aicardi", "rumba habana", "los ángeles azules", "los angeles azules", "pastor lópez", "pastor lopez",
            "sonora dinamita", "la sonora dinamita", "lisandro meza", "los corraleros de majagual", "los hispanos",
            "los graduados", "los melódicos", "los melodicos", "billo's caracas boys", "billos caracas boys",
            "grupo 5", "agua marina", "armonía 10", "armonia 10", "corazón serrano", "corazon serrano", "gilda",
            "damas gratis", "los palmeras", "chicha libre", "cuarteto imperial", "amar azul", "lucho bermúdez",
            "lucho bermudez", "pedro laza", "aníbal velásquez", "anibal velasquez", "los fantasmas del caribe",
            "alquimia", "la sonora matancera", "pequenos musical", "alberto barros",
        )

        private val SALSA_ARTISTS = listOf(
            "grupo niche", "joe arroyo", "frankie ruiz", "héctor lavoe", "hector lavoe", "willie colón", "willie colon",
            "marc anthony", "rubén blades", "ruben blades", "celia cruz", "el gran combo", "gilberto santa rosa",
            "jerry rivera", "oscar d'león", "oscar d leon", "tito nieves", "cheo feliciano", "maelo ruiz",
            "victor manuelle", "eddie santiago", "la sonora ponceña", "la sonora poncena", "orquesta guayacán",
            "orquesta guayacan", "rey ruiz", "tito rojas", "fania all stars", "andy montañez", "andy montanez",
            "ismael rivera", "ismael miranda", "charlie aponte", "pedro arroyo", "david pabon", "la 33",
        )

        private val VALLENATO_ARTISTS = listOf(
            "diomedes díaz", "diomedes diaz", "binomio de oro", "binomio de oro de américa", "silvestre dangond",
            "jorge celedón", "jorge celedon", "los inquietos del vallenato", "los chiches del vallenato",
            "nelson velásquez", "nelson velasquez", "rafael orozco", "peter manjarrés", "peter manjarres",
            "kaleth morales", "hebert vargas", "poncho zuleta", "los diablitos", "los gigantes del vallenato",
            "miguel morales", "jean carlos centeno", "omar geles", "patricia teherán", "patricia teheran",
            "las musas del vallenato", "felipe peláez", "felipe pelaez", "churo diaz", "ivan villazon",
        )

        private val REGIONAL_POPULAR_ARTISTS = listOf(
            "darío gómez", "dario gomez", "el charrito negro", "luis alberto posada", "yeison jiménez", "yeison jimenez",
            "jessi uribe", "jhonny rivera", "arelys henao", "alzate", "pipe bueno", "carin león", "carin leon",
            "christian nodal", "vicente fernández", "vicente fernandez", "alejandro fernández", "alejandro fernandez",
            "pepe aguilar", "antonio aguilar", "marco antonio solís", "marco antonio solis", "los tigres del norte",
            "los bukis", "segundo rosero", "gerardo morán", "gerardo moran", "juanita burbano", "claudio vallejo",
            "paulina tamayo", "fausto miño", "chucho ponce", "hermanos miño naranjo", "gabriel arriaga",
        )

        private val BOLERO_BALADA_ARTISTS = listOf(
            "julio jaramillo", "olimpo cárdenas", "olimpo cardenas", "alci acosta", "los panchos", "armando manzanero",
            "josé josé", "jose jose", "camilo sesto", "roberto carlos", "rocío dúrcal", "rocio durcal",
            "juan gabriel", "sandro", "leo dan", "los ángeles negros", "los angeles negros", "los terrícolas",
            "los terricolas", "los galos", "los iracundos", "los pasteles verdes", "nelson ned", "raphael",
            "nino bravo", "dyango", "lucho gatica", "bienvenido granda",
        )

        private val ROCK_EN_ESPANOL_ARTISTS = listOf(
            "soda stereo", "gustavo cerati", "enanitos verdes", "los enanitos verdes", "hombres g",
            "café tacvba", "cafe tacvba", "los fabulosos cadillacs", "molotov", "maná", "mana",
            "andrés calamaro", "andres calamaro", "charly garcía", "charly garcia", "fito páez", "fito paez",
            "caifanes", "zoé", "zoe", "babasónicos", "babasonicos", "vilma palma", "jarabe de palo",
            "héroes del silencio", "heroes del silencio", "la ley", "bunbury", "ataque 77", "los prisioneros",
            "rata blanca", "cuca", "elefante", "panteon rococo", "auténticos decadentes",
        )

        private val LATIN_POP_ARTISTS = listOf(
            "chayanne", "ricky martin", "luis fonsi", "enrique iglesias", "ricardo arjona", "david bisbal",
            "alejandro sanz", "morat", "reik", "camila", "sin bandera", "ha*ash", "ha ash", "jesse & joy",
            "la oreja de van gogh", "amaia montero", "fey", "paulina rubio", "thalia", "thalía", "lucero",
        )

        private val LATIN_ARTISTS = listOf(
            "juan luis guerra", "romeo santos", "aventura", "carlos vives", "fonseca", "eddy herrera",
            "elvis crespo", "wilfrido vargas", "olga tañón", "olga tanon", "sergio vargas",
            "los hermanos rosario", "rikarena", "proyecto uno", "ilegales", "chichi peralta",
            "kinito méndez", "kinito mendez", "los toros band", "milly quezada", "fernando villalona",
        )

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

    val GREY_AND_EXTERNAL_CURATORS = listOf(
        "vice", "pitchfork",
    )

    fun detectArtistGenre(artist: String, contextKeywords: List<String> = emptyList()): UserGenre? =
        UserGenre.detectArtistGenre(artist, contextKeywords)

    fun detectGenreFromKeyword(keyword: String): UserGenre? =
        UserGenre.detectGenreFromKeyword(keyword)

    fun getSimilarArtistsForGenre(genre: UserGenre): List<String> =
        UserGenre.getSimilarArtistsForGenre(genre)

    fun isGreyOrExternalCurator(curatorName: String): Boolean {
        val c = curatorName.lowercase(Locale.ROOT)
        return GREY_AND_EXTERNAL_CURATORS.any { c.contains(it) }
    }

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

    fun extractUserArtists(
        homeShelves: List<HomeShelf>,
        extraArtists: List<String> = emptyList(),
        limit: Int = 25,
    ): List<String> {
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
            .take(limit)
    }

    /**
     * Rotates the artist pool order daily according to [date] epoch day,
     * ensuring that every day a distinct subset of the user's top artists is prioritized.
     */
    fun rotateArtistsDaily(artists: List<String>, date: LocalDate = LocalDate.now()): List<String> {
        if (artists.isEmpty()) return emptyList()
        val epochDay = date.toEpochDay()
        val offset = (((epochDay % artists.size) + artists.size) % artists.size).toInt()
        return artists.drop(offset) + artists.take(offset)
    }

    /**
     * Normalizes and isolates the artist key from an Essentials / Imprescindibles playlist title.
     * E.g. "Bad Bunny: imprescindibles" -> "bad bunny", "Drake Essentials" -> "drake".
     */
    fun extractArtistKey(title: String, subtitle: String = ""): String {
        val clean = title
            .replace(Regex("""(?i)\s*:\s*(?:essentials|imprescindibles|lo mejor de|lo más nuevo|setlist|deep cuts|next steps).*"""), "")
            .replace(Regex("""(?i)\s+(?:essentials|imprescindibles|lo mejor de|lo más nuevo|setlist|deep cuts|next steps).*"""), "")
            .replace(Regex("""(?i)\s*-\s*(?:essentials|imprescindibles|lo mejor de|lo más nuevo|setlist|deep cuts|next steps).*"""), "")
            .trim()
            .lowercase(Locale.ROOT)
        return clean.ifBlank { subtitle.trim().lowercase(Locale.ROOT) }
    }

    fun selectFeaturedArtist(topArtists: List<String>, daypart: Daypart, date: LocalDate): String? {
        if (topArtists.isEmpty()) return null
        val daypartIndex = daypart.ordinal // 0: MORNING, 1: AFTERNOON, 2: EVENING, 3: NIGHT
        val dayEpoch = date.toEpochDay()
        val artistIndex = (((dayEpoch * 4 + daypartIndex) % topArtists.size).toInt() + topArtists.size) % topArtists.size
        return topArtists[artistIndex]
    }

    fun rankUserGenres(
        topArtists: List<String>,
        contextKeywords: List<String> = emptyList(),
    ): List<UserGenre> {
        val scores = mutableMapOf<UserGenre, Int>()
        topArtists.forEach { artist ->
            val genre = UserGenre.fromArtist(artist)
            if (genre != null) {
                scores[genre] = (scores[genre] ?: 0) + 3
            }
        }
        contextKeywords.forEach { kw ->
            val g = UserGenre.detectGenreFromKeyword(kw)
            if (g != null) {
                scores[g] = (scores[g] ?: 0) + 1
            }
        }
        return scores.entries.sortedByDescending { it.value }.map { it.key }
    }

    fun inferPrimaryGenre(
        topArtists: List<String>,
        daypart: Daypart,
        contextKeywords: List<String> = emptyList(),
    ): UserGenre {
        val ranked = rankUserGenres(topArtists, contextKeywords)
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
        contextKeywords: List<String> = emptyList(),
    ): DaypartGenreConfig {
        val ranked = rankUserGenres(topArtists, contextKeywords)
        val hasCumbia = ranked.contains(UserGenre.CUMBIA)
        val hasSalsa = ranked.contains(UserGenre.SALSA)
        val hasVallenato = ranked.contains(UserGenre.VALLENATO)
        val hasRegional = ranked.contains(UserGenre.REGIONAL_POPULAR)
        val hasBolero = ranked.contains(UserGenre.BOLERO_BALADA)
        val hasRockEspanol = ranked.contains(UserGenre.ROCK_EN_ESPANOL)
        val hasLatinPop = ranked.contains(UserGenre.LATIN_POP)
        val hasLatin = ranked.contains(UserGenre.LATIN)
        val hasPop = ranked.contains(UserGenre.POP)
        val hasRnb = ranked.contains(UserGenre.RNB)
        val hasUrbano = ranked.contains(UserGenre.URBANO)
        val hasHiphop = ranked.contains(UserGenre.HIPHOP)
        val hasElectronic = ranked.contains(UserGenre.ELECTRONIC)
        val hasRock = ranked.contains(UserGenre.INDIE_ROCK)

        return when (daypart) {
            Daypart.MORNING -> {
                when {
                    hasCumbia || hasLatin -> DaypartGenreConfig(
                        title = "Cumbia y Tropical para empezar el día",
                        query = "cumbia suave tropical fiesta mañana acústico",
                        genre = UserGenre.CUMBIA,
                        excludedCuratorKeywords = listOf("perreo", "dembow"),
                    )
                    hasSalsa -> DaypartGenreConfig(
                        title = "Salsa y Sabor para la mañana",
                        query = "salsa suave salsa romántica mañana acústico",
                        genre = UserGenre.SALSA,
                        excludedCuratorKeywords = listOf("perreo", "dembow"),
                    )
                    hasBolero -> DaypartGenreConfig(
                        title = "Boleros para empezar el día",
                        query = "boleros del recuerdo baladas mañana suave",
                        genre = UserGenre.BOLERO_BALADA,
                        excludedCuratorKeywords = listOf("perreo", "dembow"),
                    )
                    hasRegional -> DaypartGenreConfig(
                        title = "Música Popular para la mañana",
                        query = "música popular rancheras mañana",
                        genre = UserGenre.REGIONAL_POPULAR,
                        excludedCuratorKeywords = listOf("perreo", "dembow"),
                    )
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
                    hasCumbia -> DaypartGenreConfig(
                        title = "Tarde de Cumbia y Tropical",
                        query = "cumbia colombiana cumbia sonidera fiesta ritmo tarde",
                        genre = UserGenre.CUMBIA,
                        excludedCuratorKeywords = emptyList(),
                    )
                    hasSalsa -> DaypartGenreConfig(
                        title = "Tarde de Salsa y Sabor",
                        query = "salsa clásica salsa dura tarde ritmo",
                        genre = UserGenre.SALSA,
                        excludedCuratorKeywords = emptyList(),
                    )
                    hasVallenato -> DaypartGenreConfig(
                        title = "Tarde de Parranda Vallenata",
                        query = "vallenato vallenatos clásicos fiesta tarde",
                        genre = UserGenre.VALLENATO,
                        excludedCuratorKeywords = emptyList(),
                    )
                    hasRegional -> DaypartGenreConfig(
                        title = "Tarde de Cantina y Popular",
                        query = "música popular despecho cantina tarde ritmo",
                        genre = UserGenre.REGIONAL_POPULAR,
                        excludedCuratorKeywords = emptyList(),
                    )
                    hasLatin -> DaypartGenreConfig(
                        title = "Tarde de Música Latina",
                        query = "tropical merengue salsa bachata fiesta tarde ritmo",
                        genre = UserGenre.LATIN,
                        excludedCuratorKeywords = emptyList(),
                    )
                    hasRockEspanol -> DaypartGenreConfig(
                        title = "Tarde de Rock en Español",
                        query = "rock en español rock latino tarde ritmo",
                        genre = UserGenre.ROCK_EN_ESPANOL,
                        excludedCuratorKeywords = emptyList(),
                    )
                    hasLatinPop -> DaypartGenreConfig(
                        title = "Tarde de Pop Latino",
                        query = "pop latino baladas pop en español hits tarde",
                        genre = UserGenre.LATIN_POP,
                        excludedCuratorKeywords = emptyList(),
                    )
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
                        title = "Tarde de Música Latina",
                        query = "cumbia salsa tropical latino fiesta tarde éxitos",
                        genre = UserGenre.CUMBIA,
                        excludedCuratorKeywords = emptyList(),
                    )
                }
            }
            Daypart.EVENING -> {
                when {
                    hasBolero -> DaypartGenreConfig(
                        title = "Noches de Boleros y Baladas",
                        query = "boleros del recuerdo baladas románticas noche calma",
                        genre = UserGenre.BOLERO_BALADA,
                        excludedCuratorKeywords = listOf("reggaeton", "perreo", "dembow"),
                    )
                    hasSalsa -> DaypartGenreConfig(
                        title = "Noches de Salsa Romántica",
                        query = "salsa romántica salsa sensual noche calma",
                        genre = UserGenre.SALSA,
                        excludedCuratorKeywords = listOf("perreo", "dembow"),
                    )
                    hasCumbia || hasLatin -> DaypartGenreConfig(
                        title = "Noches de Cumbia y Tropical",
                        query = "cumbia tropical fiesta noche clásicos",
                        genre = UserGenre.CUMBIA,
                        excludedCuratorKeywords = emptyList(),
                    )
                    hasVallenato -> DaypartGenreConfig(
                        title = "Noches de Vallenato Romántico",
                        query = "vallenato romántico noche calma",
                        genre = UserGenre.VALLENATO,
                        excludedCuratorKeywords = emptyList(),
                    )
                    hasRegional -> DaypartGenreConfig(
                        title = "Noches de Música Popular",
                        query = "música popular despecho cantina noche",
                        genre = UserGenre.REGIONAL_POPULAR,
                        excludedCuratorKeywords = emptyList(),
                    )
                    hasRockEspanol -> DaypartGenreConfig(
                        title = "Noches de Rock en Español",
                        query = "rock en español clásicos noche calma",
                        genre = UserGenre.ROCK_EN_ESPANOL,
                        excludedCuratorKeywords = emptyList(),
                    )
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
                        title = "Noches de Música y Calma",
                        query = "boleros baladas acústico suave noche calma",
                        genre = UserGenre.BOLERO_BALADA,
                        excludedCuratorKeywords = listOf("reggaeton", "perreo", "dembow"),
                    )
                }
            }
            Daypart.NIGHT -> {
                when {
                    hasBolero -> DaypartGenreConfig(
                        title = "Boleros y Baladas de medianoche",
                        query = "boleros baladas nostalgia nocturno late night calma",
                        genre = UserGenre.BOLERO_BALADA,
                        excludedCuratorKeywords = listOf("reggaeton", "perreo", "dembow"),
                    )
                    hasCumbia || hasLatin -> DaypartGenreConfig(
                        title = "Cumbia y Tropical de madrugada",
                        query = "cumbia clásicos del recuerdo tropical nostalgia noche",
                        genre = UserGenre.CUMBIA,
                        excludedCuratorKeywords = listOf("perreo", "dembow"),
                    )
                    hasSalsa -> DaypartGenreConfig(
                        title = "Salsa Romántica de madrugada",
                        query = "salsa romántica sensual late night dark",
                        genre = UserGenre.SALSA,
                        excludedCuratorKeywords = listOf("perreo", "dembow"),
                    )
                    hasRegional -> DaypartGenreConfig(
                        title = "Clásicos de cantina nocturnos",
                        query = "música popular clásicos despecho cantina noche",
                        genre = UserGenre.REGIONAL_POPULAR,
                        excludedCuratorKeywords = emptyList(),
                    )
                    hasRockEspanol -> DaypartGenreConfig(
                        title = "Rock en Español de madrugada",
                        query = "rock en español alternativo nocturno late night",
                        genre = UserGenre.ROCK_EN_ESPANOL,
                        excludedCuratorKeywords = emptyList(),
                    )
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
                        title = "Sesión de madrugada",
                        query = "boleros baladas ambient nocturno late night calma",
                        genre = UserGenre.BOLERO_BALADA,
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
            UserGenre.CUMBIA -> {
                t.contains("cumbia") || t.contains("chicha") || t.contains("sonidera") ||
                    t.contains("villera") || t.contains("tropical") || t.contains("salsa") ||
                    t.contains("merengue") || t.contains("vallenato") || t.contains("bachata") ||
                    t.contains("latino") || t.contains("latina") || t.contains("fiesta") ||
                    s.contains("cumbia") || s.contains("tropical") || s.contains("latino") ||
                    s.contains("salsa") || s.contains("vallenato") || s.contains("merengue")
            }
            UserGenre.SALSA -> {
                t.contains("salsa") || t.contains("guaguanco") || t.contains("fania") ||
                    t.contains("tropical") || t.contains("latino") || t.contains("latina") ||
                    s.contains("salsa") || s.contains("tropical") || s.contains("latino")
            }
            UserGenre.VALLENATO -> {
                t.contains("vallenato") || t.contains("parranda") || s.contains("vallenato")
            }
            UserGenre.REGIONAL_POPULAR -> {
                t.contains("popular") || t.contains("despecho") || t.contains("cantina") ||
                    t.contains("ranchera") || t.contains("corridos") || t.contains("norteño") ||
                    t.contains("banda") || s.contains("regional") || s.contains("popular")
            }
            UserGenre.BOLERO_BALADA -> {
                t.contains("bolero") || t.contains("balada") || t.contains("recuerdo") || s.contains("bolero")
            }
            UserGenre.ROCK_EN_ESPANOL -> {
                t.contains("rock en español") || t.contains("rock latino") || t.contains("rock en tu idioma") ||
                    s.contains("rock en español") || s.contains("rock latino")
            }
            UserGenre.LATIN_POP -> {
                t.contains("pop latino") || t.contains("pop en español") || s.contains("pop latino")
            }
            UserGenre.LATIN -> {
                t.contains("latino") || t.contains("tropical") || t.contains("merengue") ||
                    t.contains("bachata") || s.contains("latino") || s.contains("tropical")
            }
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
