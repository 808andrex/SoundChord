package com.soundchord.features.radio.data

/** Data boundary for radio station discovery. */
interface RadioRepository {
    suspend fun getStations(query: String, countryCode: String?, limit: Int): List<RadioStation>
}

class RadioBrowserRepository(
    private val api: RadioBrowserDataSource = RadioBrowserDataSource.Default
) : RadioRepository {
    override suspend fun getStations(
        query: String,
        countryCode: String?,
        limit: Int
    ): List<RadioStation> = when {
        query.isNotBlank() -> api.searchStations(query.trim(), countryCode, limit)
        !countryCode.isNullOrBlank() -> api.getStationsByCountry(countryCode, limit)
        else -> api.getTopStations(limit)
    }
}

/** Adapter that keeps the repository replaceable in ViewModel tests. */
interface RadioBrowserDataSource {
    suspend fun getTopStations(limit: Int): List<RadioStation>
    suspend fun getStationsByCountry(countryCode: String, limit: Int): List<RadioStation>
    suspend fun searchStations(query: String, countryCode: String?, limit: Int): List<RadioStation>

    data object Default : RadioBrowserDataSource {
        override suspend fun getTopStations(limit: Int) = RadioBrowserApi.getTopStations(limit)
        override suspend fun getStationsByCountry(countryCode: String, limit: Int) =
            RadioBrowserApi.getStationsByCountry(countryCode, limit)
        override suspend fun searchStations(query: String, countryCode: String?, limit: Int) =
            RadioBrowserApi.searchStations(query, countryCode, limit)
    }
}
