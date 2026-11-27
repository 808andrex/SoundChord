package com.soundchord.features.daypart.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.ShelfLayout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CachedDaypartShelfItem(
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
        fun from(item: ShelfItem) = CachedDaypartShelfItem(
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
data class CachedDaypartShelf(
    val title: String,
    val subtitle: String = "",
    val items: List<CachedDaypartShelfItem> = emptyList(),
    val layoutName: String = "CARDS",
) {
    fun toHomeShelf() = HomeShelf(
        title = title,
        subtitle = subtitle,
        items = items.map { it.toShelfItem() },
        layout = runCatching { ShelfLayout.valueOf(layoutName) }.getOrDefault(ShelfLayout.CARDS),
    )

    companion object {
        fun from(shelf: HomeShelf) = CachedDaypartShelf(
            title = shelf.title,
            subtitle = shelf.subtitle,
            items = shelf.items.map { CachedDaypartShelfItem.from(it) },
            layoutName = shelf.layout.name,
        )
    }
}

@Serializable
data class CachedDaypartPayload(
    val shelves: List<CachedDaypartShelf> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Persistent SQLite database cache for dynamic time-of-day shelves (Dayparting).
 * Caches shelves indexed by daypart key (morning, afternoon, evening, night)
 * with an 8-hour TTL for instant (0 ms) cold-start rendering.
 */
class DaypartDatabaseCache private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "daypart_cache.db"
        private const val DB_VERSION = 1
        private const val TABLE_NAME = "daypart_cache"
        private const val COL_DAYPART_KEY = "daypart_key"
        private const val COL_JSON_DATA = "json_data"
        private const val COL_TIMESTAMP = "timestamp"
        private const val TTL_MS = 8L * 60 * 60 * 1000L // 8 hours TTL

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        @Volatile
        private var instance: DaypartDatabaseCache? = null

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = DaypartDatabaseCache(context)
                    }
                }
            }
        }

        fun get(daypartKey: String): List<HomeShelf>? {
            val dbHelper = instance ?: return null
            return runCatching {
                val db = dbHelper.readableDatabase
                val cursor = db.query(
                    TABLE_NAME,
                    arrayOf(COL_JSON_DATA, COL_TIMESTAMP),
                    "$COL_DAYPART_KEY = ?",
                    arrayOf(daypartKey),
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
                            val payload = json.decodeFromString<CachedDaypartPayload>(jsonData)
                            val shelves = payload.shelves.map { it.toHomeShelf() }
                            if (shelves.isEmpty() || shelves.all { it.items.isEmpty() }) {
                                delete(daypartKey)
                                null
                            } else {
                                shelves
                            }
                        } else {
                            delete(daypartKey)
                            null
                        }
                    } else null
                }
            }.getOrNull()
        }

        fun getByPrefix(prefix: String, dateSuffix: String): List<HomeShelf>? {
            val dbHelper = instance ?: return null
            return runCatching {
                val db = dbHelper.readableDatabase
                val cursor = db.query(
                    TABLE_NAME,
                    arrayOf(COL_DAYPART_KEY, COL_JSON_DATA, COL_TIMESTAMP),
                    "$COL_DAYPART_KEY LIKE ? AND $COL_DAYPART_KEY LIKE ?",
                    arrayOf("$prefix%", "%$dateSuffix"),
                    null,
                    null,
                    "$COL_TIMESTAMP DESC",
                    "1",
                )
                cursor.use { c ->
                    if (c.moveToFirst()) {
                        val key = c.getString(c.getColumnIndexOrThrow(COL_DAYPART_KEY))
                        val jsonData = c.getString(c.getColumnIndexOrThrow(COL_JSON_DATA))
                        val timestamp = c.getLong(c.getColumnIndexOrThrow(COL_TIMESTAMP))
                        val now = System.currentTimeMillis()
                        if (now - timestamp < TTL_MS) {
                            val payload = json.decodeFromString<CachedDaypartPayload>(jsonData)
                            val shelves = payload.shelves.map { it.toHomeShelf() }
                            if (shelves.isNotEmpty() && shelves.any { it.items.isNotEmpty() }) {
                                shelves
                            } else null
                        } else {
                            delete(key)
                            null
                        }
                    } else null
                }
            }.getOrNull()
        }

        fun put(daypartKey: String, shelves: List<HomeShelf>) {
            val dbHelper = instance ?: return
            if (shelves.isEmpty()) return
            runCatching {
                val payload = CachedDaypartPayload(
                    shelves = shelves.map { CachedDaypartShelf.from(it) },
                    timestamp = System.currentTimeMillis(),
                )
                val encoded = json.encodeToString(CachedDaypartPayload.serializer(), payload)
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put(COL_DAYPART_KEY, daypartKey)
                    put(COL_JSON_DATA, encoded)
                    put(COL_TIMESTAMP, System.currentTimeMillis())
                }
                db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
        }

        fun prunePreviousDates(currentDate: String) {
            val dbHelper = instance ?: return
            runCatching {
                val db = dbHelper.writableDatabase
                db.delete(TABLE_NAME, "$COL_DAYPART_KEY LIKE '%_%' AND $COL_DAYPART_KEY NOT LIKE ?", arrayOf("%_$currentDate"))
            }
        }

        fun delete(daypartKey: String) {
            val dbHelper = instance ?: return
            runCatching {
                dbHelper.writableDatabase.delete(TABLE_NAME, "$COL_DAYPART_KEY = ?", arrayOf(daypartKey))
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
                $COL_DAYPART_KEY TEXT PRIMARY KEY,
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
