package com.music.bitchord.data.canvas

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Stores animated cover art (HLS .m3u8 video URLs) associated with playlists.
 *
 * Used both for local playlists and for playlists synchronized with YouTube Music
 * (keyed by canonical playlistId or browseId).
 */
object PlaylistMotionStore {

    private const val PREF_NAME = "bitchord_playlist_motion"
    private lateinit var prefs: SharedPreferences

    private val _motionMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val motionMap: StateFlow<Map<String, String>> = _motionMap.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        val appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val loaded = mutableMapOf<String, String>()
        for ((key, value) in prefs.all) {
            if (value is String && value.isNotBlank()) {
                loaded[key] = value
            }
        }
        _motionMap.value = loaded
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
     * Returns the motion artwork URL (.m3u8) for a given playlist if one exists.
     */
    fun getMotionVideoUrl(playlistIdOrBrowseId: String): String? {
        val key = normalizeId(playlistIdOrBrowseId)
        return _motionMap.value[key] ?: if (this::prefs.isInitialized) prefs.getString(key, null) else null
    }

    /**
     * Associates a motion artwork URL (.m3u8) with a playlist.
     */
    fun setMotionVideoUrl(playlistIdOrBrowseId: String, motionVideoUrl: String) {
        val key = normalizeId(playlistIdOrBrowseId)
        if (this::prefs.isInitialized) {
            prefs.edit().putString(key, motionVideoUrl).apply()
        }
        _motionMap.update { current ->
            current + (key to motionVideoUrl)
        }
    }

    /**
     * Removes the motion artwork association for a playlist.
     */
    fun removeMotionVideoUrl(playlistIdOrBrowseId: String) {
        val key = normalizeId(playlistIdOrBrowseId)
        if (this::prefs.isInitialized) {
            prefs.edit().remove(key).apply()
        }
        _motionMap.update { current ->
            current - key
        }
    }
}
