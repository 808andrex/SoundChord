package com.soundchord.features.radio.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

object RadioBrowserApi {
    private val mirrors = listOf(
        "de1.api.radio-browser.info",
        "de2.api.radio-browser.info",
        "fi1.api.radio-browser.info",
        "nl1.api.radio-browser.info",
    )
    @Volatile private var workingMirror: String? = null
    private const val USER_AGENT = "SoundChord/1.2.0"

    private val client = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun getTopStations(limit: Int = 60): List<RadioStation> =
        requestOnMirrors("stations/topclick/$limit") {
            parameter("hidebroken", true)
        }

    suspend fun getStationsByCountry(countryCode: String, limit: Int = 60): List<RadioStation> =
        requestOnMirrors(path = "stations/bycountrycodeexact/$countryCode") {
            parameter("limit", limit)
            parameter("order", "votes")
            parameter("reverse", true)
            parameter("hidebroken", true)
        }

    suspend fun searchStations(
        query: String,
        countryCode: String? = null,
        genre: String? = null,
        language: String? = null,
        limit: Int = 60
    ): List<RadioStation> = requestOnMirrors("stations/search") {
        parameter("name", query.trim())
        countryCode?.takeIf(String::isNotBlank)?.let { parameter("countrycode", it) }
        genre?.takeIf(String::isNotBlank)?.let { parameter("tag", it) }
        language?.takeIf(String::isNotBlank)?.let { parameter("language", it) }
        parameter("limit", limit)
        parameter("hidebroken", true)
        parameter("order", "clickcount")
        parameter("reverse", true)
    }

    suspend fun getPopularTags(limit: Int = 30): List<RadioTag> =
        requestOnMirrors("tags") {
            parameter("order", "stationcount")
            parameter("reverse", true)
            parameter("limit", limit)
        }

    private suspend inline fun <reified T> requestOnMirrors(
        path: String,
        crossinline configure: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val preferred = workingMirror
        val candidates = buildList {
            preferred?.let(::add)
            addAll(mirrors.filterNot { it == preferred }.shuffled())
        }
        var lastFailure: Throwable? = null
        var emptyResponse: T? = null

        for (host in candidates) {
            try {
                val response: T = client.get("https://$host/json/$path") {
                    header(HttpHeaders.UserAgent, USER_AGENT)
                    configure()
                }.body()
                if (response is List<*> && response.isEmpty()) {
                    emptyResponse = response
                    continue
                }
                workingMirror = host
                return response
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                lastFailure = failure
            }
        }

        emptyResponse?.let { return it }
        throw lastFailure ?: IllegalStateException("Radio Browser request failed on every mirror")
    }
}

