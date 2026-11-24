package com.soundchord.features.playlistimport.data

import com.music.bitchord.data.Http
import com.music.bitchord.data.canvas.AppleMusicTokenProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.util.Locale

object AppleMusicPlaylistClient {

    private const val API_BASE = "https://amp-api.music.apple.com/v1/catalog"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val PLAYLIST_URL_REGEX = Regex(
        """^(?:https?://)?music\.apple\.com/([a-z]{2})/playlist/(?:[^/?#]+/)?([a-z0-9.-]+)""",
        RegexOption.IGNORE_CASE,
    )

    private val MOTION_KEYS = listOf(
        "motionDetailTall",
        "motionTallVideo3x4",
        "motionArtworkTall3x4",
        "motionArtistSquare1x1",
        "motionDetailSquare",
        "motionSquareVideo1x1",
        "motionArtworkSquare1x1",
        "motionArtistFullscreen16x9",
        "motionArtistWide16x9",
        "motionArtworkWide16x9",
        "motionDetailWide",
    )

    /**
     * Parses an Apple Music playlist link or raw playlist ID.
     * Returns Pair(storefront, playlistId) or null if invalid.
     */
    fun parsePlaylistUrl(input: String): Pair<String, String>? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val match = PLAYLIST_URL_REGEX.find(trimmed)
        if (match != null) {
            val storefront = match.groupValues[1].lowercase(Locale.ROOT)
            val playlistId = match.groupValues[2]
            return storefront to playlistId
        }

        // Support direct playlist ID (pl.xxxx or pl.u-xxxx)
        if (trimmed.startsWith("pl.") && trimmed.length >= 5) {
            val defaultStorefront = Locale.getDefault().country
                .takeIf { it.length == 2 }?.lowercase(Locale.ROOT) ?: "us"
            return defaultStorefront to trimmed
        }

        return null
    }

    /**
     * Fetches full playlist metadata and all tracks from Apple Music's AMP API.
     */
    suspend fun fetchPlaylist(storefront: String, playlistId: String): AppleMusicPlaylist =
        withContext(Dispatchers.IO) {
            var bearer = AppleMusicTokenProvider.token()
                ?: error("Could not obtain Apple Music authorization token")

            val initialUrl = "$API_BASE/$storefront/playlists/$playlistId?include=tracks&extend=editorialArtwork,editorialVideo"
            var responseBody = executeGet(initialUrl, bearer)

            // If 401 Unauthorized, invalidate token and retry once
            if (responseBody == null) {
                AppleMusicTokenProvider.invalidateToken(bearer)
                bearer = AppleMusicTokenProvider.token()
                    ?: error("Could not refresh Apple Music authorization token")
                responseBody = executeGet(initialUrl, bearer)
                    ?: error("Failed to load Apple Music playlist (HTTP error)")
            }

            val root = runCatching { json.parseToJsonElement(responseBody).jsonObject }.getOrNull()
                ?: error("Failed to parse Apple Music playlist response")

            val playlistObj = root["data"]?.jsonArray?.firstOrNull()?.jsonObject
                ?: error("Apple Music playlist not found or private")

            val attributes = playlistObj["attributes"]?.jsonObject ?: JsonObject(emptyMap())
            val name = attributes["name"]?.jsonPrimitive?.contentOrNull?.trim()
                ?: "Imported Apple Music Playlist"

            val description = attributes["description"]?.jsonObject?.let { desc ->
                (desc["standard"]?.jsonPrimitive?.contentOrNull
                    ?: desc["short"]?.jsonPrimitive?.contentOrNull).orEmpty()
            } ?: attributes["description"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val cleanDescription = description.replace(Regex("<[^>]+>"), "").trim()

            val curator = attributes["curatorName"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()

            val coverUrl = attributes["artwork"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull?.let { rawUrl ->
                rawUrl.replace("{w}", "1000").replace("{h}", "1000")
            }

            val editorialVideo = attributes["editorialVideo"]?.jsonObject
            val (motionVideoUrl, motionPreviewUrl) = extractMotionArtwork(editorialVideo)

            val allTracks = mutableListOf<AppleMusicTrack>()
            val tracksRelationship = playlistObj["relationships"]?.jsonObject?.get("tracks")?.jsonObject
            val initialTracksArray = tracksRelationship?.get("data")?.jsonArray ?: JsonArray(emptyList())

            for (element in initialTracksArray) {
                parseTrack(element.jsonObject)?.let { allTracks.add(it) }
            }

            // Paginate through remaining tracks if next URL exists
            var nextUrl = tracksRelationship?.get("next")?.jsonPrimitive?.contentOrNull
            while (!nextUrl.isNullOrBlank()) {
                val fullNextUrl = if (nextUrl.startsWith("http")) {
                    nextUrl
                } else {
                    "https://amp-api.music.apple.com$nextUrl"
                }

                val nextBody = executeGet(fullNextUrl, bearer) ?: break
                val nextRoot = runCatching { json.parseToJsonElement(nextBody).jsonObject }.getOrNull() ?: break
                val nextItems = nextRoot["data"]?.jsonArray ?: break
                if (nextItems.isEmpty()) break

                for (element in nextItems) {
                    parseTrack(element.jsonObject)?.let { allTracks.add(it) }
                }

                nextUrl = nextRoot["next"]?.jsonPrimitive?.contentOrNull
            }

            AppleMusicPlaylist(
                id = playlistId,
                name = name,
                description = cleanDescription,
                curator = curator,
                coverUrl = coverUrl,
                motionVideoUrl = motionVideoUrl,
                motionPreviewUrl = motionPreviewUrl,
                tracks = allTracks,
            )
        }

    private fun parseTrack(obj: JsonObject): AppleMusicTrack? {
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val attributes = obj["attributes"]?.jsonObject ?: return null
        val title = attributes["name"]?.jsonPrimitive?.contentOrNull?.trim() ?: return null
        val artist = attributes["artistName"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val album = attributes["albumName"]?.jsonPrimitive?.contentOrNull?.trim()
        val durationMs = attributes["durationInMillis"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
        val isrc = attributes["isrc"]?.jsonPrimitive?.contentOrNull?.trim()
        val cover = attributes["artwork"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull?.let { rawUrl ->
            rawUrl.replace("{w}", "600").replace("{h}", "600")
        }

        return AppleMusicTrack(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            isrc = isrc,
            coverUrl = cover,
        )
    }

    private fun extractMotionArtwork(editorialVideo: JsonObject?): Pair<String?, String?> {
        if (editorialVideo == null) return null to null
        var videoUrl: String? = null
        var previewUrl: String? = null

        for (key in MOTION_KEYS) {
            val entry = editorialVideo[key]?.jsonObject ?: continue
            if (videoUrl == null) {
                videoUrl = entry["video"]?.jsonPrimitive?.contentOrNull
                    ?: entry["videoUrl"]?.jsonPrimitive?.contentOrNull
                    ?: entry["hlsUrl"]?.jsonPrimitive?.contentOrNull
                    ?: entry["url"]?.jsonPrimitive?.contentOrNull
            }
            if (previewUrl == null) {
                val frameUrl = entry["previewFrame"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
                if (frameUrl != null) {
                    val height = if (key.contains("Tall") || key.contains("3x4")) "1440" else "1080"
                    previewUrl = frameUrl.replace("{w}", "1080").replace("{h}", height)
                }
            }
            if (videoUrl != null && previewUrl != null) break
        }

        return videoUrl to previewUrl
    }

    private fun executeGet(url: String, bearer: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $bearer")
            .header("Origin", "https://music.apple.com")
            .header("Referer", "https://music.apple.com/")
            .header("User-Agent", USER_AGENT)
            .build()

        return runCatching {
            Http.client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        }.getOrNull()
    }
}
