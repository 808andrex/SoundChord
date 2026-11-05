package com.soundchord.features.radio.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.util.Locale

object RadioBrowserApi{
    private const val BASE_URL = "https://de1.api.radio-browser.info/json"

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation){
            json(Json{
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun getTopStations(limit: Int = 30): List<RadioStation>{
        return client.get("$BASE_URL/stations/topclick/$limit").body()
    }

    suspend fun getStationsByCountry(countryCode: String, limit: Int=30): List<RadioStation>{
        return client.get("$BASE_URL/stations/bycountrycodeexact/$countryCode"){
            parameter("limit",limit)
            parameter("order", "votes")
            parameter("reverse", true)
        }.body()
    }

    suspend fun searchStations(query: String, limit: Int = 30): List<RadioStation>{
        return client.get("$BASE_URL/stations/byname/$query"){
            parameter("limit", limit)
        }.body()
    }
}

