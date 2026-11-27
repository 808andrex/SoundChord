package com.soundchord.features.playlists.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.music.bitchord.data.model.TopPick
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

@Serializable
data class CachedPlaylistsPayload(
    val playlists: List<TopPick> = emptyList(),
    val slot: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Persistent SQLite cache for "Playlists for You" (Playlists para ti) - Ticket #62.
 * Caches 12 personalized vertical playlist cards indexed by user identity and 12-hour time slot
 * (AM: 00:00-11:59, PM: 12:00-23:59) with a 12-hour TTL for instant (0 ms) cold-start rendering
 * and automatic rotation every 12 hours based on recent user listening history.
 */
class PlaylistsDatabaseCache private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "playlists_for_you_cache.db"
        private const val DB_VERSION = 4
        private const val TABLE_NAME = "playlists_cache"
        private const val COL_USER_KEY = "user_key"
        private const val COL_SLOT = "cache_slot"
        private const val COL_JSON_DATA = "json_data"
        private const val COL_TIMESTAMP = "timestamp"
        private const val TTL_MS = 12L * 60 * 60 * 1000L // 12 hours TTL

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        @Volatile
        private var instance: PlaylistsDatabaseCache? = null

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = PlaylistsDatabaseCache(context)
                    }
                }
            }
        }

        /**
         * Calculates the unique 12-hour slot key, dividing each day into AM (0:00-11:59)
         * and PM (12:00-23:59) intervals for automated 12-hour rotation.
         */
        fun getSlotKey(time: LocalDateTime = LocalDateTime.now()): String {
            val date = time.toLocalDate().toString()
            val slot = if (time.hour < 12) "AM" else "PM"
            return "${date}_$slot"
        }

        fun get(userKey: String, currentSlot: String = getSlotKey()): List<TopPick>? {
            val dbHelper = instance ?: return null
            return runCatching {
                val db = dbHelper.readableDatabase
                val cursor = db.query(
                    TABLE_NAME,
                    arrayOf(COL_JSON_DATA, COL_TIMESTAMP, COL_SLOT),
                    "$COL_USER_KEY = ? AND $COL_SLOT = ?",
                    arrayOf(userKey, currentSlot),
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
                            val payload = json.decodeFromString<CachedPlaylistsPayload>(jsonData)
                            if (payload.playlists.isNotEmpty()) {
                                payload.playlists
                            } else {
                                delete(userKey, currentSlot)
                                null
                            }
                        } else {
                            delete(userKey, currentSlot)
                            null
                        }
                    } else null
                }
            }.getOrNull()
        }

        fun put(userKey: String, currentSlot: String = getSlotKey(), playlists: List<TopPick>) {
            val dbHelper = instance ?: return
            if (playlists.isEmpty()) return
            runCatching {
                val payload = CachedPlaylistsPayload(
                    playlists = playlists,
                    slot = currentSlot,
                    timestamp = System.currentTimeMillis(),
                )
                val encoded = json.encodeToString(CachedPlaylistsPayload.serializer(), payload)
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put(COL_USER_KEY, userKey)
                    put(COL_SLOT, currentSlot)
                    put(COL_JSON_DATA, encoded)
                    put(COL_TIMESTAMP, payload.timestamp)
                }
                db.insertWithOnConflict(
                    TABLE_NAME,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE,
                )
            }
        }

        fun prunePreviousSlots(currentSlot: String = getSlotKey()) {
            val dbHelper = instance ?: return
            runCatching {
                val db = dbHelper.writableDatabase
                db.delete(TABLE_NAME, "$COL_SLOT != ?", arrayOf(currentSlot))
            }
        }

        fun delete(userKey: String, currentSlot: String = getSlotKey()) {
            val dbHelper = instance ?: return
            runCatching {
                val db = dbHelper.writableDatabase
                db.delete(
                    TABLE_NAME,
                    "$COL_USER_KEY = ? AND $COL_SLOT = ?",
                    arrayOf(userKey, currentSlot),
                )
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                $COL_USER_KEY TEXT NOT NULL,
                $COL_SLOT TEXT NOT NULL,
                $COL_JSON_DATA TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                PRIMARY KEY ($COL_USER_KEY, $COL_SLOT)
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }
}
