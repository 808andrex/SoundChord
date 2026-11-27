package com.soundchord.features.stations.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.music.bitchord.data.model.TopPick
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CachedStationsPayload(
    val stations: List<TopPick> = emptyList(),
    val date: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Persistent SQLite cache for "Stations for You" (Emisoras para ti).
 * Retains resolved personalized stations per user identity and date with 24-hour TTL
 * for instant (0 ms) cold-start rendering and automatic daily rotation.
 */
class StationsDatabaseCache private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "stations_for_you_cache.db"
        private const val DB_VERSION = 2
        private const val TABLE_NAME = "stations_cache"
        private const val COL_USER_KEY = "user_key"
        private const val COL_DATE = "cache_date"
        private const val COL_JSON_DATA = "json_data"
        private const val COL_TIMESTAMP = "timestamp"
        private const val TTL_MS = 24L * 60 * 60 * 1000L // 24 hours TTL

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        @Volatile
        private var instance: StationsDatabaseCache? = null

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = StationsDatabaseCache(context)
                    }
                }
            }
        }

        fun get(userKey: String, currentDate: String): List<TopPick>? {
            val dbHelper = instance ?: return null
            return runCatching {
                val db = dbHelper.readableDatabase
                val cursor = db.query(
                    TABLE_NAME,
                    arrayOf(COL_JSON_DATA, COL_TIMESTAMP, COL_DATE),
                    "$COL_USER_KEY = ? AND $COL_DATE = ?",
                    arrayOf(userKey, currentDate),
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
                            val payload = json.decodeFromString<CachedStationsPayload>(jsonData)
                            if (payload.stations.isNotEmpty()) {
                                payload.stations
                            } else {
                                delete(userKey, currentDate)
                                null
                            }
                        } else {
                            delete(userKey, currentDate)
                            null
                        }
                    } else null
                }
            }.getOrNull()
        }

        fun put(userKey: String, currentDate: String, stations: List<TopPick>) {
            val dbHelper = instance ?: return
            if (stations.isEmpty()) return
            runCatching {
                val payload = CachedStationsPayload(
                    stations = stations,
                    date = currentDate,
                    timestamp = System.currentTimeMillis(),
                )
                val encoded = json.encodeToString(CachedStationsPayload.serializer(), payload)
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put(COL_USER_KEY, userKey)
                    put(COL_DATE, currentDate)
                    put(COL_JSON_DATA, encoded)
                    put(COL_TIMESTAMP, System.currentTimeMillis())
                }
                db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
        }

        fun delete(userKey: String, currentDate: String) {
            val dbHelper = instance ?: return
            runCatching {
                val db = dbHelper.writableDatabase
                db.delete(TABLE_NAME, "$COL_USER_KEY = ? AND $COL_DATE = ?", arrayOf(userKey, currentDate))
            }
        }

        fun prunePreviousDates(currentDate: String) {
            val dbHelper = instance ?: return
            runCatching {
                val db = dbHelper.writableDatabase
                db.delete(TABLE_NAME, "$COL_DATE != ?", arrayOf(currentDate))
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                $COL_USER_KEY TEXT NOT NULL,
                $COL_DATE TEXT NOT NULL,
                $COL_JSON_DATA TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                PRIMARY KEY ($COL_USER_KEY, $COL_DATE)
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }
}
