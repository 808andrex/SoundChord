package com.music.bitchord.data.spotify

import android.content.Context
import android.net.Uri
import android.content.SharedPreferences
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.PlaylistCovers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class LocalPlaylist(
    val id: String,
    val title: String,
    val songs: List<Song>,
    val customCoverUri: String? = null,
) {
    val browseId: String get() = "local:playlist:$id"
}

object LocalPlaylistStore {
    private const val PREF_NAME = "bitchord_local_playlists"
    private const val KEY_PLAYLISTS = "playlists_json"

    private lateinit var prefs: SharedPreferences
    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var coverObserverStarted = false

    private val _playlists = MutableStateFlow<List<LocalPlaylist>>(emptyList())
    val playlists = _playlists.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        PlaylistCovers.init(appContext)
        prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        load()
        if (!coverObserverStarted) {
            coverObserverStarted = true
            scope.launch {
                PlaylistCovers.covers.collect { covers ->
                    val updated = _playlists.value.map { playlist ->
                        playlist.copy(customCoverUri = covers[PlaylistCovers.normalizePlaylistId(playlist.id)]?.toString())
                    }
                    if (updated != _playlists.value) {
                        _playlists.value = updated
                        persist(updated)
                    }
                }
            }
        }
    }

    private fun load() {
        if (!this::prefs.isInitialized) return
        val raw = prefs.getString(KEY_PLAYLISTS, null) ?: return
        runCatching {
            val list = mutableListOf<LocalPlaylist>()
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val title = obj.getString("title")
                val songsArray = obj.getJSONArray("songs")
                val songs = mutableListOf<Song>()
                for (j in 0 until songsArray.length()) {
                    val sObj = songsArray.getJSONObject(j)
                    val song = Song(
                        videoId = sObj.optString("videoId", ""),
                        title = sObj.optString("title", ""),
                        artist = sObj.optString("artist", ""),
                        thumbnailUrl = sObj.optString("thumbnailUrl", null).takeIf { !it.isNullOrBlank() && it != "null" },
                        durationText = sObj.optString("durationText", null).takeIf { !it.isNullOrBlank() && it != "null" },
                        artistId = sObj.optString("artistId", null).takeIf { !it.isNullOrBlank() && it != "null" },
                        albumId = sObj.optString("albumId", null).takeIf { !it.isNullOrBlank() && it != "null" },
                        albumName = sObj.optString("albumName", null).takeIf { !it.isNullOrBlank() && it != "null" },
                        isVideo = sObj.optBoolean("isVideo", false),
                    )
                    songs.add(song)
                }
                val storedCover = obj.optString("customCoverUri", null)?.takeIf { !it.isNullOrBlank() && it != "null" }
                val cover = PlaylistCovers.getCoverUri(appContext, id)?.toString() ?: storedCover
                list.add(LocalPlaylist(id, title, songs, cover))
            }
            _playlists.value = list
        }
    }

    suspend fun savePlaylist(title: String, songs: List<Song>, coverUri: Uri? = null): LocalPlaylist {
        val id = "sp_local_" + java.util.UUID.randomUUID().toString()
        val playlist = LocalPlaylist(id = id, title = title, songs = songs)
        val updated = listOf(playlist) + _playlists.value.filterNot { it.id == id }
        _playlists.value = updated
        persist(updated)
        if (coverUri != null && this::appContext.isInitialized) {
            val saved = com.music.bitchord.data.PlaylistCovers.save(appContext, id, coverUri)?.toString()
            val withCover = _playlists.value.map { item ->
                if (item.id == id) item.copy(customCoverUri = saved) else item
            }
            _playlists.value = withCover
            persist(withCover)
            return withCover.first { it.id == id }
        }
        return playlist
    }

    fun deletePlaylist(id: String) {
        if (this::appContext.isInitialized) PlaylistCovers.delete(appContext, id)
        val updated = _playlists.value.filterNot { it.id == id || it.browseId == id || it.id == id.removePrefix("local:playlist:") }
        _playlists.value = updated
        persist(updated)
    }

    fun renamePlaylist(id: String, newTitle: String) {
        val cleanId = id.removePrefix("local:playlist:").removePrefix("VL")
        val updated = _playlists.value.map {
            if (it.id == cleanId || it.id == id || it.browseId == id) {
                it.copy(title = newTitle)
            } else it
        }
        _playlists.value = updated
        persist(updated)
    }

    /**
     * Takes one row of [song] out of the playlist at [browseId]. The first
     * copy, when the playlist holds it twice: rows here carry no per-row id
     * to say which copy was picked, and they play the same either way.
     */
    fun removeSong(browseId: String, song: Song) {
        val updated = _playlists.value.map { playlist ->
            if (playlist.browseId != browseId) return@map playlist
            val index = playlist.songs.indexOfFirst { it.videoId == song.videoId }
            if (index < 0) playlist else playlist.copy(songs = playlist.songs.filterIndexed { i, _ -> i != index })
        }
        _playlists.value = updated
        persist(updated)
    }

    fun getPlaylist(id: String): LocalPlaylist? {
        val cleanId = id.removePrefix("local:playlist:").removePrefix("VL")
        return _playlists.value.firstOrNull { it.id == cleanId || it.id == id || it.browseId == id }
    }

    fun refreshCovers() {
        if (!this::prefs.isInitialized) return
        val covers = PlaylistCovers.covers.value
        val updated = _playlists.value.map { playlist ->
            val coverUri = covers[PlaylistCovers.normalizePlaylistId(playlist.id)]?.toString()
                ?: PlaylistCovers.getCoverUri(appContext, playlist.id)?.toString()
                ?: playlist.customCoverUri
            playlist.copy(customCoverUri = coverUri)
        }
        if (updated != _playlists.value) {
            _playlists.value = updated
            persist(updated)
        }
    }

    fun exportPlaylistsJson(): String? = if (this::prefs.isInitialized) prefs.getString(KEY_PLAYLISTS, null) else null

    fun importPlaylistsJson(raw: String?) {
        if (raw.isNullOrBlank() || !this::prefs.isInitialized) return
        runCatching {
            val incoming = mutableListOf<LocalPlaylist>()
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val title = obj.getString("title")
                val songsArray = obj.getJSONArray("songs")
                val songs = mutableListOf<Song>()
                for (j in 0 until songsArray.length()) {
                    val sObj = songsArray.getJSONObject(j)
                    val song = Song(
                        videoId = sObj.optString("videoId", ""),
                        title = sObj.optString("title", ""),
                        artist = sObj.optString("artist", ""),
                        thumbnailUrl = sObj.optString("thumbnailUrl", null).takeIf { !it.isNullOrBlank() && it != "null" },
                        durationText = sObj.optString("durationText", null).takeIf { !it.isNullOrBlank() && it != "null" },
                        artistId = sObj.optString("artistId", null).takeIf { !it.isNullOrBlank() && it != "null" },
                        albumId = sObj.optString("albumId", null).takeIf { !it.isNullOrBlank() && it != "null" },
                        albumName = sObj.optString("albumName", null).takeIf { !it.isNullOrBlank() && it != "null" },
                        isVideo = sObj.optBoolean("isVideo", false),
                    )
                    songs.add(song)
                }
                val storedCover = obj.optString("customCoverUri", null)?.takeIf { !it.isNullOrBlank() && it != "null" }
                val cover = PlaylistCovers.getCoverUri(appContext, id)?.toString() ?: storedCover
                incoming.add(LocalPlaylist(id, title, songs, cover))
            }
            val existingIds = _playlists.value.map { it.id }.toSet()
            val newPlaylists = incoming.filter { it.id !in existingIds }
            val merged = _playlists.value + newPlaylists
            _playlists.value = merged
            persist(merged)
        }
    }

    private fun persist(list: List<LocalPlaylist>) {
        if (!this::prefs.isInitialized) return
        runCatching {
            val jsonArray = JSONArray()
            for (playlist in list) {
                val pObj = JSONObject()
                pObj.put("id", playlist.id)
                pObj.put("title", playlist.title)
                pObj.put("customCoverUri", playlist.customCoverUri)
                val songsArray = JSONArray()
                for (song in playlist.songs) {
                    val sObj = JSONObject()
                    sObj.put("videoId", song.videoId)
                    sObj.put("title", song.title)
                    sObj.put("artist", song.artist)
                    sObj.put("thumbnailUrl", song.thumbnailUrl)
                    sObj.put("durationText", song.durationText)
                    sObj.put("artistId", song.artistId)
                    sObj.put("albumId", song.albumId)
                    sObj.put("albumName", song.albumName)
                    sObj.put("isVideo", song.isVideo)
                    songsArray.put(sObj)
                }
                pObj.put("songs", songsArray)
                jsonArray.put(pObj)
            }
            prefs.edit().putString(KEY_PLAYLISTS, jsonArray.toString()).apply()
        }
    }
}
