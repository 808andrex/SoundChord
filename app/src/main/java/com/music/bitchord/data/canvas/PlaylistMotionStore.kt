package com.music.bitchord.data.canvas

import android.content.Context
import android.content.SharedPreferences
import com.soundchord.features.playlistimport.data.AppleMusicPlaylistClient
import com.soundchord.features.playlistimport.data.AppleMusicPlaylistRepository
import com.music.bitchord.data.spotify.LocalPlaylistStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.json.JSONObject

/**
 * Single entry for a playlist in [PlaylistMotionStore].
 */
data class PlaylistMotionEntry(
    val motionVideoUrl: String? = null,
    val coverUrl: String? = null,
    val applePlaylistId: String? = null,
    val storefront: String? = null,
    val description: String? = null,
    val author: String? = null,
)

/**
 * Serializable representation of a playlist motion entry for backups (Google Drive / local JSON).
 */
@Serializable
data class PlaylistMotionBackup(
    val browseId: String,
    val applePlaylistId: String? = null,
    val storefront: String? = null,
    val coverUrl: String? = null,
    val motionVideoUrl: String? = null,
    val description: String? = null,
    val author: String? = null,
)

/**
 * Stores animated cover art (HLS .m3u8 video URLs), static Apple Music artwork,
 * and canonical Apple Music playlist IDs associated with playlists.
 *
 * Used both for local playlists and for playlists synchronized with YouTube Music
 * (keyed by canonical playlistId or browseId).
 */
object PlaylistMotionStore {

    private const val PREF_NAME = "bitchord_playlist_motion"
    private lateinit var prefs: SharedPreferences

    private val _entryMap = MutableStateFlow<Map<String, PlaylistMotionEntry>>(emptyMap())
    val entryMap: StateFlow<Map<String, PlaylistMotionEntry>> = _entryMap.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        val appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val loaded = mutableMapOf<String, PlaylistMotionEntry>()
        for ((key, value) in prefs.all) {
            if (value is String && value.isNotBlank()) {
                val entry = parseEntry(value)
                if (entry != null) {
                    loaded[key] = entry
                }
            }
        }
        _entryMap.value = loaded
    }

    private fun parseEntry(raw: String): PlaylistMotionEntry? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        return if (trimmed.startsWith("{")) {
            runCatching {
                val obj = JSONObject(trimmed)
                PlaylistMotionEntry(
                    motionVideoUrl = obj.optString("motionVideoUrl", null)?.takeIf { it.isNotBlank() && it != "null" },
                    coverUrl = obj.optString("coverUrl", null)?.takeIf { it.isNotBlank() && it != "null" },
                    applePlaylistId = obj.optString("applePlaylistId", null)?.takeIf { it.isNotBlank() && it != "null" },
                    storefront = obj.optString("storefront", null)?.takeIf { it.isNotBlank() && it != "null" },
                    description = obj.optString("description", null)?.takeIf { it.isNotBlank() && it != "null" },
                    author = obj.optString("author", null)?.takeIf { it.isNotBlank() && it != "null" },
                )
            }.getOrNull()
        } else {
            // Backwards compatibility with plain .m3u8 URL strings stored previously
            PlaylistMotionEntry(motionVideoUrl = trimmed)
        }
    }

    private fun serializeEntry(entry: PlaylistMotionEntry): String {
        val obj = JSONObject()
        entry.motionVideoUrl?.let { obj.put("motionVideoUrl", it) }
        entry.coverUrl?.let { obj.put("coverUrl", it) }
        entry.applePlaylistId?.let { obj.put("applePlaylistId", it) }
        entry.storefront?.let { obj.put("storefront", it) }
        entry.description?.let { obj.put("description", it) }
        entry.author?.let { obj.put("author", it) }
        return obj.toString()
    }

    /**
     * Normalizes playlist IDs and browse IDs to a consistent key.
     * E.g. "VLPLxxxx" -> "PLxxxx", "local:playlist:sp_local_xxx" -> "sp_local_xxx"
     */
    fun normalizeId(id: String): String {
        return id.trim()
            .removePrefix("VL")
            .removePrefix("local:playlist:")
    }

    /**
     * Returns the full motion entry for a given playlist if one exists.
     */
    fun getEntry(playlistIdOrBrowseId: String): PlaylistMotionEntry? {
        val key = normalizeId(playlistIdOrBrowseId)
        val inMemory = _entryMap.value[key]
        if (inMemory != null) return inMemory
        if (this::prefs.isInitialized) {
            val raw = prefs.getString(key, null)
            if (raw != null) {
                val parsed = parseEntry(raw)
                if (parsed != null) {
                    _entryMap.update { it + (key to parsed) }
                    return parsed
                }
            }
        }
        return null
    }

    /**
     * Returns the motion artwork URL (.m3u8) for a given playlist if one exists.
     */
    fun getMotionVideoUrl(playlistIdOrBrowseId: String): String? {
        return getEntry(playlistIdOrBrowseId)?.motionVideoUrl
    }

    /**
     * Returns the static square cover URL for a given playlist if one exists.
     */
    fun getCoverUrl(playlistIdOrBrowseId: String): String? {
        return getEntry(playlistIdOrBrowseId)?.coverUrl
    }

    /**
     * Returns the Apple Music playlist ID for a given playlist if one exists.
     */
    fun getApplePlaylistId(playlistIdOrBrowseId: String): String? {
        return getEntry(playlistIdOrBrowseId)?.applePlaylistId
    }

    /**
     * Returns the storefront code (e.g. "us", "es", "mx") for a given Apple Music playlist if one exists.
     */
    fun getStorefront(playlistIdOrBrowseId: String): String? {
        return getEntry(playlistIdOrBrowseId)?.storefront
    }

    /**
     * Returns the editorial description for a given playlist if one exists.
     */
    fun getDescription(playlistIdOrBrowseId: String): String? {
        return getEntry(playlistIdOrBrowseId)?.description
    }

    /**
     * Returns the original author or curator name for a given playlist if one exists.
     */
    fun getAuthor(playlistIdOrBrowseId: String): String? {
        return getEntry(playlistIdOrBrowseId)?.author
    }

    /**
     * Associates an author/curator name with a playlist.
     */
    fun setAuthor(playlistIdOrBrowseId: String, author: String) {
        setEntry(playlistIdOrBrowseId, author = author)
    }

    /**
     * Stores or updates a full entry for a playlist.
     */
    fun setEntry(
        playlistIdOrBrowseId: String,
        motionVideoUrl: String? = null,
        coverUrl: String? = null,
        applePlaylistId: String? = null,
        storefront: String? = null,
        description: String? = null,
        author: String? = null,
    ) {
        val key = normalizeId(playlistIdOrBrowseId)
        val existing = getEntry(key)
        val entry = PlaylistMotionEntry(
            motionVideoUrl = motionVideoUrl ?: existing?.motionVideoUrl,
            coverUrl = coverUrl ?: existing?.coverUrl,
            applePlaylistId = applePlaylistId ?: existing?.applePlaylistId,
            storefront = storefront ?: existing?.storefront,
            description = description ?: existing?.description,
            author = author ?: existing?.author,
        )
        if (entry.motionVideoUrl == null && entry.coverUrl == null && entry.applePlaylistId == null && entry.description == null && entry.author == null) {
            removeMotionVideoUrl(key)
            return
        }
        val serialized = serializeEntry(entry)
        if (this::prefs.isInitialized) {
            prefs.edit().putString(key, serialized).apply()
        }
        _entryMap.update { it + (key to entry) }
    }

    /**
     * Associates a motion artwork URL (.m3u8) with a playlist.
     */
    fun setMotionVideoUrl(playlistIdOrBrowseId: String, motionVideoUrl: String) {
        setEntry(playlistIdOrBrowseId, motionVideoUrl = motionVideoUrl)
    }

    /**
     * Associates a static cover URL with a playlist.
     */
    fun setCoverUrl(playlistIdOrBrowseId: String, coverUrl: String) {
        setEntry(playlistIdOrBrowseId, coverUrl = coverUrl)
    }

    /**
     * Associates an editorial description with a playlist.
     */
    fun setDescription(playlistIdOrBrowseId: String, description: String) {
        setEntry(playlistIdOrBrowseId, description = description)
    }

    /**
     * Removes the motion artwork association for a playlist.
     */
    fun removeMotionVideoUrl(playlistIdOrBrowseId: String) {
        val key = normalizeId(playlistIdOrBrowseId)
        if (this::prefs.isInitialized) {
            prefs.edit().remove(key).apply()
        }
        _entryMap.update { it - key }
    }

    /**
     * Exports all stored playlist motion entries for backup.
     */
    fun exportMotionList(): List<PlaylistMotionBackup> {
        val result = mutableListOf<PlaylistMotionBackup>()
        for ((key, entry) in _entryMap.value) {
            if (entry.motionVideoUrl != null || entry.coverUrl != null || entry.applePlaylistId != null || entry.description != null || entry.author != null) {
                result.add(
                    PlaylistMotionBackup(
                        browseId = key,
                        applePlaylistId = entry.applePlaylistId,
                        storefront = entry.storefront,
                        coverUrl = entry.coverUrl,
                        motionVideoUrl = entry.motionVideoUrl,
                        description = entry.description,
                        author = entry.author,
                    )
                )
            }
        }
        return result
    }

    /**
     * Restores backup entries into the store.
     */
    fun importMotionList(list: List<PlaylistMotionBackup>) {
        for (backup in list) {
            setEntry(
                playlistIdOrBrowseId = backup.browseId,
                motionVideoUrl = backup.motionVideoUrl,
                coverUrl = backup.coverUrl,
                applePlaylistId = backup.applePlaylistId,
                storefront = backup.storefront,
                description = backup.description,
                author = backup.author,
            )
        }
    }

    /**
     * Attempts to refresh the motion video URL from Apple Music using [applePlaylistId]
     * if the video URL fails or has expired.
     */
    suspend fun refreshMotionUrl(playlistIdOrBrowseId: String): String? {
        val key = normalizeId(playlistIdOrBrowseId)
        val entry = getEntry(key) ?: return null
        val appleId = entry.applePlaylistId ?: return entry.motionVideoUrl
        val sf = entry.storefront?.takeIf { it.isNotBlank() } ?: "us"
        return runCatching {
            val refreshed = AppleMusicPlaylistClient.fetchPlaylist(storefront = sf, playlistId = appleId)
            val newMotion = refreshed.motionVideoUrl ?: entry.motionVideoUrl
            val newCover = refreshed.coverUrl ?: entry.coverUrl
            val newDesc = refreshed.description.takeIf { it.isNotBlank() } ?: entry.description
            setEntry(
                playlistIdOrBrowseId = key,
                motionVideoUrl = newMotion,
                coverUrl = newCover,
                applePlaylistId = appleId,
                storefront = sf,
                description = newDesc,
            )
            newMotion
        }.getOrNull()
    }

    /**
     * Automatically queries Apple Music in the background for all imported playlists to recover
     * and persist up-to-date coverUrl, motionVideoUrl, and editorial description.
     */
    fun refreshAllImportedPlaylists(scope: kotlinx.coroutines.CoroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)) {
        scope.launch {
            val entries = _entryMap.value.filter { it.value.applePlaylistId != null }
            for ((key, entry) in entries) {
                val appleId = entry.applePlaylistId ?: continue
                val sf = entry.storefront?.takeIf { it.isNotBlank() } ?: "us"
                runCatching {
                    val refreshed = AppleMusicPlaylistClient.fetchPlaylist(storefront = sf, playlistId = appleId)
                    setEntry(
                        playlistIdOrBrowseId = key,
                        motionVideoUrl = refreshed.motionVideoUrl ?: entry.motionVideoUrl,
                        coverUrl = refreshed.coverUrl ?: entry.coverUrl,
                        applePlaylistId = appleId,
                        storefront = sf,
                        description = refreshed.description.takeIf { it.isNotBlank() } ?: entry.description,
                    )
                    val local = LocalPlaylistStore.getPlaylist(key)
                        ?: LocalPlaylistStore.playlists.value.firstOrNull { it.applePlaylistId == appleId }
                    if (local != null) {
                        val matchedSongs = if (refreshed.tracks.isNotEmpty()) {
                            runCatching {
                                val (songs, _) = AppleMusicPlaylistRepository.resolveAndMatchTracks(refreshed.tracks) { _, _ -> }
                                songs
                            }.getOrNull()
                        } else null

                        LocalPlaylistStore.updatePlaylistFromAppleMusic(
                            id = local.id,
                            title = refreshed.name.takeIf { it.isNotBlank() },
                            songs = matchedSongs?.takeIf { it.isNotEmpty() },
                            coverUrl = refreshed.coverUrl,
                            motionVideoUrl = refreshed.motionVideoUrl,
                            description = refreshed.description.takeIf { it.isNotBlank() },
                        )
                    }
                }
            }
        }
    }
}
