package com.soundchord.features.recognition.data

import android.content.Context
import android.content.SharedPreferences
import com.soundchord.features.recognition.model.RecognitionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class RecognitionHistoryEntry(
    val id: String,
    val videoId: String?,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val timestamp: Long,
)

/** Recognition records stay on this device and are independent of account history. */
object RecognitionHistoryStore {
    private const val PREFS = "soundchord_recognition_history"
    private const val KEY_ENTRIES = "entries"
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(RecognitionHistoryEntry.serializer())
    private var preferences: SharedPreferences? = null
    private val mutableEntries = MutableStateFlow<List<RecognitionHistoryEntry>>(emptyList())

    val entries = mutableEntries.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        mutableEntries.value = read()
    }

    @Synchronized
    fun record(context: Context, result: RecognitionResult) {
        if (preferences == null) init(context)
        val entry = RecognitionHistoryEntry(
            id = UUID.randomUUID().toString(),
            videoId = result.youtubeVideoId,
            title = result.title,
            artist = result.artist,
            artworkUrl = result.coverArtHqUrl ?: result.coverArtUrl,
            timestamp = System.currentTimeMillis(),
        )
        save(listOf(entry) + mutableEntries.value)
    }

    @Synchronized
    fun remove(id: String) = save(mutableEntries.value.filterNot { it.id == id })

    @Synchronized
    fun clear() = save(emptyList())

    private fun read(): List<RecognitionHistoryEntry> = runCatching {
        json.decodeFromString(serializer, preferences?.getString(KEY_ENTRIES, null) ?: "[]")
    }.getOrDefault(emptyList())

    private fun save(value: List<RecognitionHistoryEntry>) {
        mutableEntries.value = value
        preferences?.edit()?.putString(KEY_ENTRIES, json.encodeToString(serializer, value))?.apply()
    }
}
