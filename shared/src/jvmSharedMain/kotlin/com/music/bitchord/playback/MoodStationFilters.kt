package com.music.bitchord.playback

import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.TopPickMood
import java.util.Locale

/**
 * Filter policies for Mood stations (Ticket #63):
 * 1. Strict official catalogue song filter (Song only, 1:1 square artwork, no UGC, lyric videos, intros).
 * 2. Cultural & linguistic affinity filter: blocks completely foreign regional genres/languages
 *    (such as Bollywood, Punjabi, Hindi, Desi, etc.) unless the user actively listens to them.
 */
object MoodStationFilters {

    /**
     * Checks if a song belongs to an active Mood station.
     */
    fun isMoodStationSong(song: Song): Boolean {
        val id = song.playbackSourceId ?: return false
        return TopPickMood.entries.any { it.name.equals(id, ignoreCase = true) }
    }

    /**
     * Checks if a song belongs to an active "Estación de [Usuario]" (Personal Station).
     */
    fun isPersonalStationSong(song: Song): Boolean {
        val id = song.playbackSourceId ?: return false
        return id.startsWith("station:personal", ignoreCase = true) || id.equals("personal_station", ignoreCase = true)
    }

    /**
     * Checks if a song belongs to an active "Estación por descubrir" (Discovery Station).
     */
    fun isDiscoveryStationSong(song: Song): Boolean {
        val id = song.playbackSourceId ?: return false
        return id.startsWith("station:discovery", ignoreCase = true) || id.equals("discovery_station", ignoreCase = true)
    }

    /**
     * Checks if a song belongs to any station requiring strict official song filtering and metadata sanitization.
     */
    fun isFilteredPersonalOrDiscoveryStation(song: Song): Boolean {
        return isPersonalStationSong(song) || isDiscoveryStationSong(song)
    }

    /**
     * Strict filter: only official catalogue tracks.
     * Rejects videos, UGC uploads, lyric videos, spoken intros, and widescreen 16:9 covers.
     */
    fun isOfficialSong(song: Song): Boolean {
        // 1. Explicit video flags
        if (song.isVideo || song.isVideoOrigin) return false

        val title = song.title.trim()
        val artist = song.artist.trim()
        if (title.isBlank() || artist.isBlank()) return false
        if (artist.equals("Unknown artist", ignoreCase = true) || artist.equals("Various Artists", ignoreCase = true)) {
            return false
        }

        // 2. UGC, lyric video, video upload or commentary markers in title/artist
        if (isUgcOrVideoText(title) || isUgcOrVideoText(artist)) return false

        // 3. Portada cuadrada 1:1 (reject widescreen 16:9 uncropped thumbnails)
        if (!isSquareArtwork(song.thumbnailUrl)) return false

        // 4. Duración típica de canción oficial (filtrar skits, intros habladas cortas o grabaciones no musicales)
        if (!isMusicDuration(song.durationText)) return false

        return true
    }

    /**
     * Validates whether [url] points to a square 1:1 cover image.
     * Rejects raw widescreen 16:9 YouTube thumbnails (hqdefault, mqdefault, sddefault, maxresdefault,
     * or explicit non-square dimensions like 640x360 or 120x90).
     */
    fun isSquareArtwork(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase(Locale.ROOT)

        // Raw YouTube thumbnail names are 16:9 uncropped unless square crop parameters are present
        val isWidescreenName = lower.contains("hqdefault") ||
            lower.contains("mqdefault") ||
            lower.contains("sddefault") ||
            lower.contains("maxresdefault") ||
            lower.contains("hq720")

        if (isWidescreenName) {
            val hasSquareCrop = lower.contains("-p-") ||
                Regex("""=w(\d+)-h\1\b""").containsMatchIn(lower) ||
                Regex("""=s(\d+)-c\b""").containsMatchIn(lower)
            if (!hasSquareCrop) return false
        }

        // Dimensions pattern: =wXXX-hYYY or /wXXX-hYYY or -wXXX-hYYY
        val dimMatch = Regex("""[=/-]w(\d+)[-_]h(\d+)""").find(lower)
        if (dimMatch != null) {
            val (wStr, hStr) = dimMatch.destructured
            val w = wStr.toDoubleOrNull() ?: 1.0
            val h = hStr.toDoubleOrNull() ?: 1.0
            if (h > 0.0) {
                val ratio = w / h
                if (ratio !in 0.88..1.12) return false
            }
        }

        return true
    }

    private val UGC_OR_VIDEO_PATTERN = Regex(
        """\b(lyric\s*video|lyrics|lyrical|video\s*l[ií]rico|letra|letras|official\s*video|music\s*video|videoclip|video\s*oficial|audio\s*oficial|official\s*audio|visualizer|visualiser|full\s*video|short\s*version|extended\s*version|skit|spoken\s*intro|intro\s*spoken|intro\s*hablada|interview|entrevista|audio\s*commentary|comentario|behind\s*the\s*scenes|making\s*of|teaser|trailer|clip\s*oficial|karaoke|sing\s*king|instrumental|tribute|tributo|sped\s*up|slowed\s*(?:and|\+)?\s*reverb|nightcore|8d\s*audio|bass\s*boosted|dance\s*performance|dance\s*practice|choreography|fan\s*made|fan\s*video|ugc|vevo|\w+vevo)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val BRACKET_VIDEO_PATTERN = Regex(
        """[(\[](?:[^)\]]*\b(?:video|videoclip|lyric|lyrics|letra|official|4k|hd|audio|visualizer|vevo)\b[^)\]]*)[)\]]""",
        RegexOption.IGNORE_CASE,
    )

    fun isUgcOrVideoText(text: String): Boolean {
        if (UGC_OR_VIDEO_PATTERN.containsMatchIn(text)) return true
        if (BRACKET_VIDEO_PATTERN.containsMatchIn(text)) return true
        return false
    }

    fun isMusicDuration(durationText: String?): Boolean {
        if (durationText.isNullOrBlank()) return true
        val parts = durationText.trim().split(":").mapNotNull { it.toIntOrNull() }
        val totalSeconds = when (parts.size) {
            1 -> parts[0]
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> return true
        }
        return totalSeconds in 45..720
    }

    // ── Metadata Sanitization ────────────────────────────────────────────────

    /**
     * Cleans an artist name: strips " - Topic", "VEVO" channel tags, etc.
     */
    fun cleanArtist(rawArtist: String): String {
        var artist = rawArtist.trim()
        artist = artist.replace(Regex("""\s*[-_]?\s*topic\b""", RegexOption.IGNORE_CASE), "").trim()
        artist = artist.replace(Regex("""\s*[-_]?\s*vevo\b""", RegexOption.IGNORE_CASE), "").trim()
        if (artist.endsWith("VEVO", ignoreCase = true) && artist.length > 4) {
            artist = artist.substring(0, artist.length - 4).trim()
        }
        return artist.ifBlank { rawArtist }
    }

    /**
     * Cleans a track title: removes channel prefixes like "Artist - Title",
     * removes video/lyric/audio tags like "(Official Video)", "[Official Music Video]",
     * and trims trailing dash or pipe noise.
     */
    fun cleanTitle(rawTitle: String, rawArtist: String): String {
        var title = rawTitle.trim()

        // 1. Remove pipe noise: e.g. "Title | Official Music Video" -> "Title"
        if (title.contains(" | ")) {
            val beforePipe = title.substringBefore(" | ").trim()
            val afterPipe = title.substringAfter(" | ").trim()
            if (isUgcOrVideoText(afterPipe) || afterPipe.contains("video", ignoreCase = true) || afterPipe.contains("audio", ignoreCase = true)) {
                title = beforePipe
            }
        }

        // 2. Remove bracket/parenthesis noise: e.g. "(Official Music Video)", "[Official Video]", "(Audio)", "(Video Oficial)"
        val bracketNoise = Regex(
            """\s*[\(\[](?:official\s*(?:music\s*|lyric\s*)?video|video\s*oficial|audio\s*oficial|official\s*audio|official|official\s*hd|audio|video|visualizer|visualiser|4k|hd|videoclip|clip\s*oficial|lyric\s*video|lyrics|letra|letras|extended\s*version|short\s*version)[\)\]]\s*""",
            RegexOption.IGNORE_CASE,
        )
        title = title.replace(bracketNoise, " ").trim()

        // 3. Remove trailing dash noise: e.g. "Title - Official Video"
        val dashNoise = Regex(
            """\s*[-–—]\s*(?:official\s*(?:music\s*|lyric\s*)?video|video\s*oficial|audio\s*oficial|official\s*audio|visualizer|visualiser|videoclip|clip\s*oficial|lyric\s*video|lyrics|letra)\s*$""",
            RegexOption.IGNORE_CASE,
        )
        title = title.replace(dashNoise, "").trim()

        // 4. Remove channel/artist prefix in title: e.g. "Coldplay - Yellow" -> "Yellow"
        val cleanArt = cleanArtist(rawArtist).lowercase(Locale.ROOT)
        val separatorMatch = Regex("""\s*[-–—:]\s*""").find(title)
        if (separatorMatch != null && separatorMatch.range.first > 0) {
            val partBefore = title.substring(0, separatorMatch.range.first).trim()
            val partAfter = title.substring(separatorMatch.range.last + 1).trim()
            val partBeforeLower = partBefore.lowercase(Locale.ROOT)
            if (partBeforeLower == cleanArt ||
                cleanArt.contains(partBeforeLower) ||
                partBeforeLower.contains(cleanArt) ||
                (cleanArt.isNotBlank() && cleanArt.split(" ", ",", "&").any { it.length > 2 && it in partBeforeLower })
            ) {
                if (partAfter.isNotBlank()) {
                    title = partAfter
                }
            }
        }

        // Clean any residual bracket noise that might have been revealed
        title = title.replace(bracketNoise, " ").trim()
        title = title.replace(Regex("""\s+"""), " ").trim()

        return title.ifBlank { rawTitle }
    }

    /**
     * Sanitizes a song's metadata to guarantee it matches an official catalogue track
     * (clean album track title and clean artist name).
     */
    fun sanitizeSongMetadata(song: Song): Song {
        val cleanArt = cleanArtist(song.artist)
        val cleanTit = cleanTitle(song.title, song.artist)
        return song.copy(
            title = cleanTit,
            artist = cleanArt,
        )
    }

    // ── Cultural Affinity & Foreign Music Blocking ───────────────────────────

    data class CulturalProfile(
        val hasDesiAffinity: Boolean = false,
        val hasKpopAffinity: Boolean = false,
        val hasJpopAffinity: Boolean = false,
        val hasArabicAffinity: Boolean = false,
        val hasSertanejoAffinity: Boolean = false,
    )

    private val DESI_KEYWORDS = listOf(
        "bollywood", "desi", "hindi", "punjabi", "indian", "tollywood", "kollywood",
        "bhojpuri", "tamil", "telugu", "malayalam", "kannada", "ghazal", "qawwali",
        "bhangra", "sufi", "marathi", "gujarati", "haryanvi", "bengali", "assamese",
        "arijit", "pritam", "sidhu", "diljit", "badshah", "atif aslam", "neha kakkar",
        "shreya ghoshal", "kumar sanu", "alka yagnik", "lata mangeshkar",
    )

    private val KPOP_KEYWORDS = listOf(
        "k-pop", "kpop", "korean", "bts", "blackpink", "twice", "stray kids",
        "newjeans", "txt", "seventeen", "exo", "aespa", "ive", "le sserafim",
    )

    private val JPOP_KEYWORDS = listOf(
        "j-pop", "jpop", "japanese", "anime", "yoasobi", "ado", "kenshi yonezu", "lisa",
    )

    private val ARABIC_KEYWORDS = listOf(
        "arabic", "khaleeji", "dabke", "rai", "amr diab", "nancy ajram", "elissa", "sherine",
    )

    private val SERTANEJO_KEYWORDS = listOf(
        "sertanejo", "funk rj", "funk sp", "funk brasil", "pagode", "arrocha",
        "marília mendonça", "gusttavo lima", "jorge & mateus", "luan santana",
    )

    fun buildCulturalProfile(
        artists: Collection<String>,
        genres: Collection<String>,
        songs: Collection<Song> = emptyList(),
    ): CulturalProfile {
        val userCorpus = buildString {
            artists.forEach { append(it).append(' ') }
            genres.forEach { append(it).append(' ') }
            songs.forEach {
                append(it.artist).append(' ')
                append(it.title).append(' ')
            }
        }.lowercase(Locale.ROOT)

        val hasDesi = DESI_KEYWORDS.any { it in userCorpus } || containsIndicScript(userCorpus)
        val hasKpop = KPOP_KEYWORDS.any { it in userCorpus } || containsHangulScript(userCorpus)
        val hasJpop = JPOP_KEYWORDS.any { it in userCorpus } || containsKanaScript(userCorpus)
        val hasArabic = ARABIC_KEYWORDS.any { it in userCorpus } || containsArabicScript(userCorpus)
        val hasSertanejo = SERTANEJO_KEYWORDS.any { it in userCorpus }

        return CulturalProfile(
            hasDesiAffinity = hasDesi,
            hasKpopAffinity = hasKpop,
            hasJpopAffinity = hasJpop,
            hasArabicAffinity = hasArabic,
            hasSertanejoAffinity = hasSertanejo,
        )
    }

    /**
     * Determines whether a shelf title from YouTube Music mood categories is acceptable for the user.
     * Blocks shelves of regional categories the user does not listen to (e.g. "Romantic Bollywood", "Hindi Chill").
     */
    fun isCulturallyAllowedShelf(shelfTitle: String, profile: CulturalProfile): Boolean {
        val lower = shelfTitle.lowercase(Locale.ROOT)

        if (!profile.hasDesiAffinity) {
            if (DESI_KEYWORDS.any { it in lower } || containsIndicScript(shelfTitle)) return false
        }
        if (!profile.hasKpopAffinity) {
            if (KPOP_KEYWORDS.any { it in lower } || containsHangulScript(shelfTitle)) return false
        }
        if (!profile.hasJpopAffinity) {
            if (JPOP_KEYWORDS.any { it in lower } || containsKanaScript(shelfTitle)) return false
        }
        if (!profile.hasArabicAffinity) {
            if (ARABIC_KEYWORDS.any { it in lower } || containsArabicScript(shelfTitle)) return false
        }
        if (!profile.hasSertanejoAffinity) {
            if (SERTANEJO_KEYWORDS.any { it in lower }) return false
        }

        return true
    }

    /**
     * Checks if a track fits the user's cultural and linguistic profile.
     * Rejects foreign regional music that the user has no affinity for.
     */
    fun hasCulturalAffinity(song: Song, profile: CulturalProfile): Boolean {
        return hasCulturalAffinity(song.artist, song.title, profile)
    }

    fun hasCulturalAffinity(artist: String, title: String, profile: CulturalProfile): Boolean {
        val combined = "$artist $title".lowercase(Locale.ROOT)

        if (!profile.hasDesiAffinity) {
            if (DESI_KEYWORDS.any { it in combined } || containsIndicScript(artist) || containsIndicScript(title)) {
                return false
            }
        }
        if (!profile.hasKpopAffinity) {
            if (KPOP_KEYWORDS.any { it in combined } || containsHangulScript(artist) || containsHangulScript(title)) {
                return false
            }
        }
        if (!profile.hasJpopAffinity) {
            if (JPOP_KEYWORDS.any { it in combined } || containsKanaScript(artist) || containsKanaScript(title)) {
                return false
            }
        }
        if (!profile.hasArabicAffinity) {
            if (ARABIC_KEYWORDS.any { it in combined } || containsArabicScript(artist) || containsArabicScript(title)) {
                return false
            }
        }
        if (!profile.hasSertanejoAffinity) {
            if (SERTANEJO_KEYWORDS.any { it in combined }) return false
        }

        return true
    }

    // ── Unicode Script Detection ─────────────────────────────────────────────

    private fun containsIndicScript(text: String): Boolean {
        for (i in 0 until text.length) {
            val codePoint = text.codePointAt(i)
            val block = Character.UnicodeBlock.of(codePoint) ?: continue
            if (block == Character.UnicodeBlock.DEVANAGARI ||
                block == Character.UnicodeBlock.DEVANAGARI_EXTENDED ||
                block == Character.UnicodeBlock.GURMUKHI ||
                block == Character.UnicodeBlock.BENGALI ||
                block == Character.UnicodeBlock.TAMIL ||
                block == Character.UnicodeBlock.TELUGU ||
                block == Character.UnicodeBlock.KANNADA ||
                block == Character.UnicodeBlock.MALAYALAM ||
                block == Character.UnicodeBlock.GUJARATI ||
                block == Character.UnicodeBlock.ORIYA
            ) {
                return true
            }
        }
        return false
    }

    private fun containsHangulScript(text: String): Boolean {
        for (i in 0 until text.length) {
            val codePoint = text.codePointAt(i)
            val block = Character.UnicodeBlock.of(codePoint) ?: continue
            if (block == Character.UnicodeBlock.HANGUL_SYLLABLES ||
                block == Character.UnicodeBlock.HANGUL_JAMO ||
                block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
            ) {
                return true
            }
        }
        return false
    }

    private fun containsKanaScript(text: String): Boolean {
        for (i in 0 until text.length) {
            val codePoint = text.codePointAt(i)
            val block = Character.UnicodeBlock.of(codePoint) ?: continue
            if (block == Character.UnicodeBlock.HIRAGANA ||
                block == Character.UnicodeBlock.KATAKANA ||
                block == Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS
            ) {
                return true
            }
        }
        return false
    }

    private fun containsArabicScript(text: String): Boolean {
        for (i in 0 until text.length) {
            val codePoint = text.codePointAt(i)
            val block = Character.UnicodeBlock.of(codePoint) ?: continue
            if (block == Character.UnicodeBlock.ARABIC ||
                block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A ||
                block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B
            ) {
                return true
            }
        }
        return false
    }
}
