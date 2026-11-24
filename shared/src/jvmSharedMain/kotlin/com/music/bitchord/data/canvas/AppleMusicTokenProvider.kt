package com.music.bitchord.data.canvas

import com.music.bitchord.data.Http
import okhttp3.Request
import java.util.Base64

/**
 * Provides anonymous Bearer tokens for Apple Music's catalog API (AMP API).
 *
 * Scrapes the public Apple Music web player anonymously without requiring
 * user login or paid subscription, caching the token until near expiration.
 */
object AppleMusicTokenProvider {

    private const val WEB_PLAYER = "https://music.apple.com/us/browse"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    private const val TOKEN_RETRY_MS = 30L * 60 * 1000

    private var cachedToken: String? = null
    private var tokenExpiresAtMs = 0L
    private var retryTokenAfterMs = 0L
    private val rejected = mutableSetOf<String>()

    @Synchronized
    fun token(): String? {
        val now = System.currentTimeMillis()
        cachedToken?.let { if (now < tokenExpiresAtMs - 60_000) return it }
        if (now < retryTokenAfterMs) return null

        val html = fetch(WEB_PLAYER)
        val scripts = html?.let {
            Regex("""/assets/index(?:-legacy)?[~-][A-Za-z0-9_-]+\.js""")
                .findAll(it).map { m -> m.value }.distinct().toList()
        }.orEmpty()

        for (path in scripts) {
            val script = fetch("https://music.apple.com$path") ?: continue
            val candidates = Regex("""ey[A-Za-z0-9_-]+\.ey[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+""")
                .findAll(script)
                .map { it.value }
                .distinct()
                .filter { it !in rejected }
                .mapNotNull { jwt -> expiry(jwt)?.let { jwt to it } }
                .filter { it.second > now }
                .toList()
            if (candidates.isEmpty()) continue

            val (jwt, expiresAt) = candidates.firstOrNull { isWebPlayerToken(it.first) }
                ?: candidates.first()
            cachedToken = jwt
            tokenExpiresAtMs = expiresAt
            return jwt
        }

        retryTokenAfterMs = now + TOKEN_RETRY_MS
        return null
    }

    @Synchronized
    fun invalidateToken(bearer: String) {
        rejected += bearer
        if (cachedToken == bearer) {
            cachedToken = null
            tokenExpiresAtMs = 0L
        }
    }

    private fun fetch(url: String): String? = runCatching {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        Http.client.newCall(req).execute().use { res ->
            if (res.isSuccessful) res.body?.string() else null
        }
    }.getOrNull()

    private fun isWebPlayerToken(jwt: String): Boolean = runCatching {
        val parts = jwt.split(".")
        val header = String(Base64.getUrlDecoder().decode(parts[0]), Charsets.UTF_8)
        val payload = String(Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8)
        header.contains("WebPlayKid") || payload.contains("AMPWebPlay")
    }.getOrDefault(false)

    private fun expiry(jwt: String): Long? = runCatching {
        val payload = String(
            Base64.getUrlDecoder().decode(jwt.split(".")[1]),
            Charsets.UTF_8,
        )
        val seconds = Regex("\"exp\"\\s*:\\s*(\\d+)").find(payload)?.groupValues?.get(1)
        seconds?.toLong()?.times(1000)
    }.getOrDefault(null)
}
