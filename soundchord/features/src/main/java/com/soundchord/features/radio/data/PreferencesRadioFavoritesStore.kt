package com.soundchord.features.radio.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.radioFavoritesDataStore by preferencesDataStore(name = "radio_favorites")

/** Preferences DataStore persistence for complete favorite station snapshots. */
class PreferencesRadioFavoritesStore(context: Context) : RadioFavoritesStore {
    private val dataStore = context.applicationContext.radioFavoritesDataStore
    private val json = Json { ignoreUnknownKeys = true }
    private val favoritesKey = stringPreferencesKey("stations_json")

    override val stations: Flow<List<RadioStation>> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { preferences ->
            preferences[favoritesKey]?.let { encoded ->
                runCatching { json.decodeFromString(ListSerializer(RadioStation.serializer()), encoded) }
                    .getOrDefault(emptyList())
            }.orEmpty()
        }

    override suspend fun add(station: RadioStation) {
        dataStore.edit { preferences ->
            val current = decode(preferences[favoritesKey])
            preferences[favoritesKey] = json.encodeToString(
                ListSerializer(RadioStation.serializer()),
                (current.filterNot { it.uuid == station.uuid } + station),
            )
        }
    }

    override suspend fun remove(stationUuid: String) {
        dataStore.edit { preferences ->
            val remaining = decode(preferences[favoritesKey]).filterNot { it.uuid == stationUuid }
            preferences[favoritesKey] = json.encodeToString(ListSerializer(RadioStation.serializer()), remaining)
        }
    }

    private fun decode(encoded: String?): List<RadioStation> = encoded?.let {
        runCatching { json.decodeFromString(ListSerializer(RadioStation.serializer()), it) }
            .getOrDefault(emptyList())
    }.orEmpty()
}
