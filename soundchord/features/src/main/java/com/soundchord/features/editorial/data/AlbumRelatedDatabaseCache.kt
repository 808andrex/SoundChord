package com.soundchord.features.editorial.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.music.bitchord.data.model.AlbumRelatedSections
import com.music.bitchord.data.model.ShelfItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CachedShelfItem(
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String? = null,
    val videoId: String? = null,
    val browseId: String? = null,
    val isVideo: Boolean = false,
) {
    fun toShelfItem() = ShelfItem(
        title = title,
        subtitle = subtitle,
        thumbnailUrl = thumbnailUrl,
        videoId = videoId,
        browseId = browseId,
        isVideo = isVideo,
    )

    companion object {
        fun from(item: ShelfItem) = CachedShelfItem(
            title = item.title,
            subtitle = item.subtitle,
            thumbnailUrl = item.thumbnailUrl,
            videoId = item.videoId,
            browseId = item.browseId,
            isVideo = item.isVideo,
        )
    }
}

@Serializable
data class CachedAlbumRelated(
    val musicVideos: List<CachedShelfItem> = emptyList(),
    val moreByArtist: List<CachedShelfItem> = emptyList(),
    val appearsOn: List<CachedShelfItem> = emptyList(),
    val youMightAlsoLike: List<CachedShelfItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
) {
    fun toAlbumRelatedSections() = AlbumRelatedSections(
        musicVideos = musicVideos.map { it.toShelfItem() },
        moreByArtist = moreByArtist.map { it.toShelfItem() },
        appearsOn = appearsOn.map { it.toShelfItem() },
        youMightAlsoLike = youMightAlsoLike.map { it.toShelfItem() },
        isLoaded = true,
    )

    companion object {
        fun from(sections: AlbumRelatedSections) = CachedAlbumRelated(
            musicVideos = sections.musicVideos.map { CachedShelfItem.from(it) },
            moreByArtist = sections.moreByArtist.map { CachedShelfItem.from(it) },
            appearsOn = sections.appearsOn.map { CachedShelfItem.from(it) },
            youMightAlsoLike = sections.youMightAlsoLike.map { CachedShelfItem.from(it) },
            timestamp = System.currentTimeMillis(),
        )
    }
}

/**
 * Persistent SQLite cache for album related sections (style Apple Music).
 * Retains resolved related sections per album browseId across app restarts with a 24-hour TTL.
 */
class AlbumRelatedDatabaseCache private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "album_related_cache.db"
        private const val DB_VERSION = 5
        private const val TABLE_NAME = "album_related_cache"
        private const val COL_ALBUM_ID = "album_id"
        private const val COL_JSON_DATA = "json_data"
        private const val COL_TIMESTAMP = "timestamp"
        private const val TTL_MS = 24L * 60 * 60 * 1000L // 24 hours

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        @Volatile
        private var instance: AlbumRelatedDatabaseCache? = null

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = AlbumRelatedDatabaseCache(context)
                    }
                }
            }
        }

        fun get(albumId: String): AlbumRelatedSections? {
            val dbHelper = instance ?: return null
            return runCatching {
                val db = dbHelper.readableDatabase
                val cursor = db.query(
                    TABLE_NAME,
                    arrayOf(COL_JSON_DATA, COL_TIMESTAMP),
                    "$COL_ALBUM_ID = ?",
                    arrayOf(albumId),
                    null,
                    null,
                    null,
                )
                cursor.use { c ->
                    if (c.moveToFirst()) {
                        val jsonData = c.getString(c.getColumnIndexOrThrow(COL_JSON_DATA))
                        val timestamp = c.getLong(c.getColumnIndexOrThrow(COL_TIMESTAMP))
                        val now = System.currentTimeMillis()
                        if (now - timestamp < TTL_MS) {
                            val cached = json.decodeFromString<CachedAlbumRelated>(jsonData)
                            val sections = cached.toAlbumRelatedSections()
                            if (sections.musicVideos.isEmpty()) {
                                delete(albumId)
                                null
                            } else {
                                sections
                            }
                        } else {
                            delete(albumId)
                            null
                        }
                    } else null
                }
            }.getOrNull()
        }

        fun put(albumId: String, sections: AlbumRelatedSections) {
            val dbHelper = instance ?: return
            if (!sections.isLoaded) return
            runCatching {
                val cached = CachedAlbumRelated.from(sections)
                val encoded = json.encodeToString(CachedAlbumRelated.serializer(), cached)
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put(COL_ALBUM_ID, albumId)
                    put(COL_JSON_DATA, encoded)
                    put(COL_TIMESTAMP, System.currentTimeMillis())
                }
                db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
        }

        fun delete(albumId: String) {
            val dbHelper = instance ?: return
            runCatching {
                dbHelper.writableDatabase.delete(TABLE_NAME, "$COL_ALBUM_ID = ?", arrayOf(albumId))
            }
        }

        fun clearExpired() {
            val dbHelper = instance ?: return
            runCatching {
                val cutoff = System.currentTimeMillis() - TTL_MS
                dbHelper.writableDatabase.delete(TABLE_NAME, "$COL_TIMESTAMP < ?", arrayOf(cutoff.toString()))
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                $COL_ALBUM_ID TEXT PRIMARY KEY,
                $COL_JSON_DATA TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }
}
