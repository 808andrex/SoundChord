package com.soundchord.features.radio.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Data boundary for radio station discovery. */
interface RadioRepository {
    val favoriteStations: Flow<List<RadioStation>>
    suspend fun getStations(
        query: String,
        countryCode: String?,
        genre: String?,
        language: String?,
        limit: Int,
    ): List<RadioStation>
    suspend fun getPopularTags(limit: Int = 30): List<RadioTag>
    suspend fun addFavorite(station: RadioStation)
    suspend fun removeFavorite(stationUuid: String)
    suspend fun isFavorite(stationUuid: String): Boolean
}

class RadioBrowserRepository(
    private val api: RadioBrowserDataSource = RadioBrowserDataSource.Default,
    private val favoritesStore: RadioFavoritesStore = RadioFavoritesStore.None,
) : RadioRepository {
    override val favoriteStations = favoritesStore.stations

    override suspend fun getStations(
        query: String,
        countryCode: String?,
        genre: String?,
        language: String?,
        limit: Int,
    ): List<RadioStation> = when {
        query.isNotBlank() || !genre.isNullOrBlank() || !language.isNullOrBlank() ->
            api.searchStations(query.trim(), countryCode, genre, language, limit)
        !countryCode.isNullOrBlank() -> api.getStationsByCountry(countryCode, limit)
        else -> api.getTopStations(limit)
    }

    override suspend fun getPopularTags(limit: Int) = api.getPopularTags(limit)
    override suspend fun addFavorite(station: RadioStation) = favoritesStore.add(station)
    override suspend fun removeFavorite(stationUuid: String) = favoritesStore.remove(stationUuid)
    override suspend fun isFavorite(stationUuid: String) =
        favoriteStations.first().any { it.uuid == stationUuid }
}

/** Adapter that keeps the repository replaceable in ViewModel tests. */
interface RadioBrowserDataSource {
    suspend fun getTopStations(limit: Int): List<RadioStation>
    suspend fun getStationsByCountry(countryCode: String, limit: Int): List<RadioStation>
    suspend fun searchStations(
        query: String,
        countryCode: String?,
        genre: String?,
        language: String?,
        limit: Int,
    ): List<RadioStation>
    suspend fun getPopularTags(limit: Int): List<RadioTag>

    data object Default : RadioBrowserDataSource {
        override suspend fun getTopStations(limit: Int) = RadioBrowserApi.getTopStations(limit)
        override suspend fun getStationsByCountry(countryCode: String, limit: Int) =
            RadioBrowserApi.getStationsByCountry(countryCode, limit)
        override suspend fun searchStations(
            query: String,
            countryCode: String?,
            genre: String?,
            language: String?,
            limit: Int,
        ) = RadioBrowserApi.searchStations(query, countryCode, genre, language, limit)
        override suspend fun getPopularTags(limit: Int) = RadioBrowserApi.getPopularTags(limit)
    }
}

/** Local persistence boundary, independently replaceable in repository tests. */
interface RadioFavoritesStore {
    val stations: Flow<List<RadioStation>>
    suspend fun add(station: RadioStation)
    suspend fun remove(stationUuid: String)

    data object None : RadioFavoritesStore {
        override val stations: Flow<List<RadioStation>> = kotlinx.coroutines.flow.flowOf(emptyList())
        override suspend fun add(station: RadioStation) = Unit
        override suspend fun remove(stationUuid: String) = Unit
    }
}
