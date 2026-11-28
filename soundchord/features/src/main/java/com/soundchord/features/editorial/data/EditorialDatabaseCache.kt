package com.soundchord.features.editorial.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.music.bitchord.data.model.TopPick
import com.music.bitchord.data.model.TopPickKind
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CachedHeroPlaylistTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long = 0L,
    val coverUrl: String? = null,
)

@Serializable
data class CachedHeroPlaylist(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val storefront: String,
    val browseId: String,
    val trackCount: Int? = null,
    val tracks: List<CachedHeroPlaylistTrack> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
) {
    fun toTopPick(): TopPick = TopPick(
        kind = TopPickKind.EDITORIAL_PLAYLIST,
        title = title,
        subtitle = subtitle.ifBlank { "Apple Music" },
        images = listOf(thumbnailUrl),
        browseId = browseId,
    )

    fun toEditorialItem(): AppleMusicEditorialItem = AppleMusicEditorialItem(
        id = id,
        title = title,
        subtitle = subtitle,
        thumbnailUrl = thumbnailUrl,
        wideImageUrl = thumbnailUrl,
        badge = "PLAYLIST ACTUALIZADA",
        kind = EditorialItemKind.PLAYLIST,
        storefront = storefront,
        trackCount = trackCount,
    )

    companion object {
        fun from(item: AppleMusicEditorialItem): CachedHeroPlaylist? {
            val art = item.thumbnailUrl ?: return null
            if (item.id.isBlank() || !AppleMusicEditorialRepository.isValidArtworkUrl(art)) return null
            if (item.trackCount != null && item.trackCount <= 0) return null
            val sf = item.storefront.ifBlank { "us" }
            return CachedHeroPlaylist(
                id = item.id,
                title = item.title,
                subtitle = item.subtitle,
                thumbnailUrl = art,
                storefront = sf,
                browseId = "apple:playlist:$sf:${item.id}",
                trackCount = item.trackCount,
            )
        }

        fun from(pick: TopPick, storefront: String = "us"): CachedHeroPlaylist? {
            val art = pick.images.firstOrNull() ?: return null
            if (!AppleMusicEditorialRepository.isValidArtworkUrl(art)) return null
            val rawBrowseId = pick.browseId.orEmpty()
            if (!rawBrowseId.startsWith("apple:playlist:")) return null
            val id = if (rawBrowseId.contains(":")) rawBrowseId.substringAfterLast(":") else rawBrowseId
            if (id.isBlank()) return null
            val sf = if (rawBrowseId.contains(":")) {
                val parts = rawBrowseId.removePrefix("apple:playlist:").split(":")
                if (parts.size >= 2 && parts[0].length == 2) parts[0] else storefront
            } else storefront
            return CachedHeroPlaylist(
                id = id,
                title = pick.title,
                subtitle = pick.subtitle,
                thumbnailUrl = art,
                storefront = sf,
                browseId = if (rawBrowseId.isNotBlank()) rawBrowseId else "apple:playlist:$sf:$id",
                trackCount = null,
            )
        }

        fun from(playlist: com.soundchord.features.playlistimport.data.AppleMusicPlaylist): CachedHeroPlaylist? {
            val art = playlist.coverUrl ?: return null
            if (!AppleMusicEditorialRepository.isValidArtworkUrl(art)) return null
            if (playlist.tracks.size < 5) return null // Strict: discard any playlist with < 5 tracks
            val sf = playlist.storefront.ifBlank { "us" }
            return CachedHeroPlaylist(
                id = playlist.id,
                title = playlist.name,
                subtitle = playlist.curator.ifBlank { "Apple Music" },
                thumbnailUrl = art,
                storefront = sf,
                browseId = "apple:playlist:$sf:${playlist.id}",
                trackCount = playlist.tracks.size,
                tracks = playlist.tracks.map {
                    CachedHeroPlaylistTrack(
                        id = it.id,
                        title = it.title,
                        artist = it.artist,
                        album = it.album,
                        durationMs = it.durationMs,
                        coverUrl = it.coverUrl,
                    )
                },
            )
        }
    }
}

/**
 * Persistent SQLite/Room database cache for Apple Music editorial playlists and Hero cards (Ticket #75).
 * Retains resolved valid editorial playlists with guaranteed non-empty tracks and valid artwork.
 * Serves as a resilient zero-failure fallback across time slot changes and network drops.
 */
class EditorialDatabaseCache private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "apple_music_editorial_hero_cache.db"
        private const val DB_VERSION = 2
        private const val TABLE_NAME = "editorial_hero_cache"
        private const val COL_KEY = "cache_key"
        private const val COL_JSON_DATA = "json_data"
        private const val COL_STOREFRONT = "storefront"
        private const val COL_TIMESTAMP = "timestamp"
        private const val TTL_MS = 48L * 60 * 60 * 1000L // 48 hours

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        @Volatile
        private var instance: EditorialDatabaseCache? = null

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = EditorialDatabaseCache(context)
                    }
                }
            }
        }

        fun putEditorialItem(cacheKey: String, item: AppleMusicEditorialItem) {
            val dbHelper = instance ?: return
            val cached = CachedHeroPlaylist.from(item) ?: return
            val encoded = runCatching { json.encodeToString(CachedHeroPlaylist.serializer(), cached) }.getOrNull() ?: return
            runCatching {
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put(COL_KEY, cacheKey)
                    put(COL_JSON_DATA, encoded)
                    put(COL_STOREFRONT, cached.storefront)
                    put(COL_TIMESTAMP, cached.timestamp)
                }
                db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
        }

        fun getEditorialItem(cacheKey: String, maxAgeMs: Long = TTL_MS): AppleMusicEditorialItem? {
            val dbHelper = instance ?: return null
            return runCatching {
                val db = dbHelper.readableDatabase
                val cursor = db.query(
                    TABLE_NAME,
                    arrayOf(COL_JSON_DATA, COL_TIMESTAMP),
                    "$COL_KEY = ?",
                    arrayOf(cacheKey),
                    null,
                    null,
                    null,
                )
                cursor.use { c ->
                    if (c.moveToFirst()) {
                        val jsonData = c.getString(c.getColumnIndexOrThrow(COL_JSON_DATA))
                        val timestamp = c.getLong(c.getColumnIndexOrThrow(COL_TIMESTAMP))
                        val now = System.currentTimeMillis()
                        if (now - timestamp < maxAgeMs) {
                            val payload = json.decodeFromString<CachedHeroPlaylist>(jsonData)
                            payload.toEditorialItem()
                        } else null
                    } else null
                }
            }.getOrNull()
        }

        fun getLatestEditorialItem(): AppleMusicEditorialItem? {
            val dbHelper = instance ?: return null
            return runCatching {
                val db = dbHelper.readableDatabase
                val cursor = db.query(
                    TABLE_NAME,
                    arrayOf(COL_JSON_DATA),
                    null,
                    null,
                    null,
                    null,
                    "$COL_TIMESTAMP DESC",
                    "1",
                )
                cursor.use { c ->
                    if (c.moveToFirst()) {
                        val jsonData = c.getString(c.getColumnIndexOrThrow(COL_JSON_DATA))
                        val payload = json.decodeFromString<CachedHeroPlaylist>(jsonData)
                        payload.toEditorialItem()
                    } else null
                }
            }.getOrNull()
        }

        fun putEditorialSong(cacheKey: String, song: AppleMusicEditorialSong) {
            val dbHelper = instance ?: return
            val encoded = runCatching { json.encodeToString(AppleMusicEditorialSong.serializer(), song) }.getOrNull() ?: return
            runCatching {
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put(COL_KEY, cacheKey)
                    put(COL_JSON_DATA, encoded)
                    put(COL_STOREFRONT, "us")
                    put(COL_TIMESTAMP, System.currentTimeMillis())
                }
                db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
        }

        fun getEditorialSong(cacheKey: String, maxAgeMs: Long = TTL_MS): AppleMusicEditorialSong? {
            val dbHelper = instance ?: return null
            return runCatching {
                val db = dbHelper.readableDatabase
                val cursor = db.query(
                    TABLE_NAME,
                    arrayOf(COL_JSON_DATA, COL_TIMESTAMP),
                    "$COL_KEY = ?",
                    arrayOf(cacheKey),
                    null,
                    null,
                    null,
                )
                cursor.use { c ->
                    if (c.moveToFirst()) {
                        val jsonData = c.getString(c.getColumnIndexOrThrow(COL_JSON_DATA))
                        val timestamp = c.getLong(c.getColumnIndexOrThrow(COL_TIMESTAMP))
                        val now = System.currentTimeMillis()
                        if (now - timestamp < maxAgeMs) {
                            json.decodeFromString<AppleMusicEditorialSong>(jsonData)
                        } else null
                    } else null
                }
            }.getOrNull()
        }

        fun putVideoShelves(shelvesJson: String) {
            val dbHelper = instance ?: return
            runCatching {
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put(COL_KEY, "explore_music_videos_shelves")
                    put(COL_JSON_DATA, shelvesJson)
                    put(COL_STOREFRONT, "us")
                    put(COL_TIMESTAMP, System.currentTimeMillis())
                }
                db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
        }

        fun getVideoShelvesJson(): String? {
            val dbHelper = instance ?: return null
            return runCatching {
                val db = dbHelper.readableDatabase
                val cursor = db.query(
                    TABLE_NAME,
                    arrayOf(COL_JSON_DATA),
                    "$COL_KEY = ?",
                    arrayOf("explore_music_videos_shelves"),
                    null,
                    null,
                    null,
                )
                cursor.use { c ->
                    if (c.moveToFirst()) {
                        c.getString(c.getColumnIndexOrThrow(COL_JSON_DATA))
                    } else null
                }
            }.getOrNull()
        }

        fun putHeroTopPick(cacheKey: String, pick: TopPick, storefront: String = "us") {
            val dbHelper = instance ?: return
            val cached = CachedHeroPlaylist.from(pick, storefront) ?: return
            val encoded = runCatching { json.encodeToString(CachedHeroPlaylist.serializer(), cached) }.getOrNull() ?: return
            runCatching {
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put(COL_KEY, cacheKey)
                    put(COL_JSON_DATA, encoded)
                    put(COL_STOREFRONT, cached.storefront)
                    put(COL_TIMESTAMP, cached.timestamp)
                }
                db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
        }

        fun putVerifiedHeroPlaylist(
            cacheKey: String,
            playlist: com.soundchord.features.playlistimport.data.AppleMusicPlaylist,
        ) {
            val dbHelper = instance ?: return
            val cached = CachedHeroPlaylist.from(playlist) ?: return
            val encoded = runCatching { json.encodeToString(CachedHeroPlaylist.serializer(), cached) }.getOrNull() ?: return
            runCatching {
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put(COL_KEY, cacheKey)
                    put(COL_JSON_DATA, encoded)
                    put(COL_STOREFRONT, cached.storefront)
                    put(COL_TIMESTAMP, cached.timestamp)
                }
                db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
        }

        fun getVerifiedPlaylist(playlistId: String): com.soundchord.features.playlistimport.data.AppleMusicPlaylist? {
            val dbHelper = instance ?: return null
            return runCatching {
                val db = dbHelper.readableDatabase
                val cursor = db.query(
                    TABLE_NAME,
                    arrayOf(COL_JSON_DATA),
                    "$COL_JSON_DATA LIKE ?",
                    arrayOf("%\"id\":\"$playlistId\"%"),
                    null,
                    null,
                    "$COL_TIMESTAMP DESC",
                    "1",
                )
                cursor.use { c ->
                    if (c.moveToFirst()) {
                        val jsonData = c.getString(c.getColumnIndexOrThrow(COL_JSON_DATA))
                        val cached = json.decodeFromString<CachedHeroPlaylist>(jsonData)
                        if (cached.tracks.size >= 5) {
                            com.soundchord.features.playlistimport.data.AppleMusicPlaylist(
                                id = cached.id,
                                name = cached.title,
                                description = "",
                                curator = cached.subtitle,
                                coverUrl = cached.thumbnailUrl,
                                motionVideoUrl = null,
                                motionPreviewUrl = null,
                                tracks = cached.tracks.map {
                                    com.soundchord.features.playlistimport.data.AppleMusicTrack(
                                        id = it.id,
                                        title = it.title,
                                        artist = it.artist,
                                        album = it.album,
                                        durationMs = it.durationMs,
                                        isrc = null,
                                        coverUrl = it.coverUrl,
                                    )
                                },
                                storefront = cached.storefront,
                            )
                        } else null
                    } else null
                }
            }.getOrNull()
        }

        fun getHeroTopPick(cacheKey: String, maxAgeMs: Long = TTL_MS): TopPick? {
            val dbHelper = instance ?: return null
            return runCatching {
                val db = dbHelper.readableDatabase
                val cursor = db.query(
                    TABLE_NAME,
                    arrayOf(COL_JSON_DATA, COL_TIMESTAMP),
                    "$COL_KEY = ?",
                    arrayOf(cacheKey),
                    null,
                    null,
                    null,
                )
                cursor.use { c ->
                    if (c.moveToFirst()) {
                        val jsonData = c.getString(c.getColumnIndexOrThrow(COL_JSON_DATA))
                        val timestamp = c.getLong(c.getColumnIndexOrThrow(COL_TIMESTAMP))
                        val now = System.currentTimeMillis()
                        if (now - timestamp < maxAgeMs) {
                            val payload = json.decodeFromString<CachedHeroPlaylist>(jsonData)
                            if (payload.browseId.startsWith("apple:playlist:") && (payload.trackCount == null || payload.trackCount >= 5)) {
                                payload.toTopPick()
                            } else null
                        } else null
                    } else null
                }
            }.getOrNull()
        }

        fun getLatestHeroTopPick(): TopPick? {
            val dbHelper = instance ?: return null
            return runCatching {
                val db = dbHelper.readableDatabase
                val cursor = db.query(
                    TABLE_NAME,
                    arrayOf(COL_JSON_DATA),
                    null,
                    null,
                    null,
                    null,
                    "$COL_TIMESTAMP DESC",
                    "1",
                )
                cursor.use { c ->
                    if (c.moveToFirst()) {
                        val jsonData = c.getString(c.getColumnIndexOrThrow(COL_JSON_DATA))
                        val payload = json.decodeFromString<CachedHeroPlaylist>(jsonData)
                        if (payload.browseId.startsWith("apple:playlist:") && (payload.trackCount == null || payload.trackCount >= 5)) {
                            payload.toTopPick()
                        } else null
                    } else null
                }
            }.getOrNull()
        }

        fun getRecentHeroTopPicks(limit: Int = 4): List<TopPick> {
            val dbHelper = instance ?: return emptyList()
            return runCatching {
                val db = dbHelper.readableDatabase
                val cursor = db.query(
                    TABLE_NAME,
                    arrayOf(COL_JSON_DATA),
                    null,
                    null,
                    null,
                    null,
                    "$COL_TIMESTAMP DESC",
                    (limit * 2).toString(),
                )
                cursor.use { c ->
                    val list = mutableListOf<TopPick>()
                    while (c.moveToNext()) {
                        val jsonData = c.getString(c.getColumnIndexOrThrow(COL_JSON_DATA))
                        val payload = json.decodeFromString<CachedHeroPlaylist>(jsonData)
                        if (payload.browseId.startsWith("apple:playlist:") && (payload.trackCount == null || payload.trackCount >= 5)) {
                            val pick = payload.toTopPick()
                            if (!pick.images.firstOrNull().isNullOrBlank() && list.none { it.browseId == pick.browseId }) {
                                list.add(pick)
                                if (list.size >= limit) break
                            }
                        }
                    }
                    list
                }
            }.getOrElse { emptyList() }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                $COL_KEY TEXT PRIMARY KEY,
                $COL_JSON_DATA TEXT NOT NULL,
                $COL_STOREFRONT TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_editorial_ts ON $TABLE_NAME($COL_TIMESTAMP)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }
}
