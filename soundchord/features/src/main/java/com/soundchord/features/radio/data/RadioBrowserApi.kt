package com.soundchord.features.radio.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object RadioBrowserApi {
    private const val BASE_URL = "https://de1.api.radio-browser.info/json"

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun getTopStations(limit: Int = 30): List<RadioStation> =
        client.get("$BASE_URL/stations/topclick/$limit").body()

    suspend fun getStationsByCountry(countryCode: String, limit: Int = 30): List<RadioStation> =
        client.get("$BASE_URL/stations/bycountrycodeexact/$countryCode") {
            parameter("limit", limit)
            parameter("order", "votes")
            parameter("reverse", true)
        }.body()

    suspend fun searchStations(
        query: String,
        countryCode: String? = null,
        genre: String? = null,
        language: String? = null,
        limit: Int = 30
    ): List<RadioStation> = client.get("$BASE_URL/stations/search") {
        parameter("name", query)
        countryCode?.takeIf(String::isNotBlank)?.let { parameter("countrycode", it) }
        genre?.takeIf(String::isNotBlank)?.let { parameter("tag", it) }
        language?.takeIf(String::isNotBlank)?.let { parameter("language", it) }
        parameter("limit", limit)
    }.body()

    suspend fun getPopularTags(limit: Int = 30): List<RadioTag> =
        client.get("$BASE_URL/tags") {
            parameter("order", "stationcount")
            parameter("reverse", true)
            parameter("limit", limit)
        }.body()
}

