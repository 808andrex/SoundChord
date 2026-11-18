package com.soundchord.features.radio.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Device-local history of recently played Radio Browser stations. */
object RadioHistoryStore {
    private const val PREFS = "soundchord_radio_history"
    private const val KEY_ENTRIES = "entries"
    private const val MAX_ENTRIES = 25
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(RadioStation.serializer())
    private var preferences: SharedPreferences? = null
    private val mutableStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val stations = mutableStations.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        mutableStations.value = read()
    }

    @Synchronized
    fun record(context: Context, station: RadioStation) {
        if (preferences == null) init(context)
        save((listOf(station) + mutableStations.value.filterNot { it.uuid == station.uuid }).take(MAX_ENTRIES))
    }

    @Synchronized
    fun clear() = save(emptyList())

    private fun read(): List<RadioStation> = runCatching {
        json.decodeFromString(serializer, preferences?.getString(KEY_ENTRIES, null) ?: "[]")
            .distinctBy(RadioStation::uuid).take(MAX_ENTRIES)
    }.getOrDefault(emptyList())

    private fun save(value: List<RadioStation>) {
        mutableStations.value = value
        preferences?.edit()?.putString(KEY_ENTRIES, json.encodeToString(serializer, value))?.apply()
    }
}
