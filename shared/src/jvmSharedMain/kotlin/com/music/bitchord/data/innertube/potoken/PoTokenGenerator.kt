package com.music.bitchord.data.innertube.potoken

import com.metrolist.innertubex.extraction.TokenProvider
import com.metrolist.innertubex.extraction.TokenProviderCapabilities
import com.metrolist.innertubex.extraction.strategy.PoTokenProviderKind
import com.music.bitchord.data.TrackLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap

/**
 * One loaded BotGuard: a browser page that has run the challenge, holds an
 * integrity token, and mints PoTokens from it until it expires or dies. The
 * phone's is an Android WebView, the desktop's a JavaFX one; both drive the
 * same [PO_TOKEN_HTML].
 */
interface PoTokenMinter {
    /** Past the integrity token's lifetime, less a margin. */
    val isExpired: Boolean

    /** The page crashed, threw after start-up, or a mint timed out. */
    val isDead: Boolean

    /** A PoToken bound to [identifier] — a visitor data or a video id. */
    suspend fun generatePoToken(identifier: String): String

    /** Tears the page down, on whatever thread the browser needs. */
    fun close()
}

/**
 * BotGuard PoTokens for YouTube's web clients, minted in a browser page.
 *
 * Keeps one loaded page per session (visitor data): the session's streaming
 * token is minted once when the page is made, and each track's player token is
 * minted from the same page. A page that has expired, died or belongs to
 * another session is replaced; a mint that fails on an old page gets one retry
 * on a fresh one.
 *
 * @param createMinter loads a new BotGuard page; the platform's browser.
 * @param available whether this platform can host a page at all.
 */
class PoTokenGenerator(
    private val createMinter: suspend () -> PoTokenMinter,
    private val available: () -> Boolean = { true },
) {
    private val TAG = "PoTokenGenerator"

    private var webViewBadImpl = false // whether the platform's browser turned out to be unusable

    private val webPoTokenGenLock = Mutex()
    private var webPoTokenSessionId: String? = null
    private var webPoTokenStreamingPot: String? = null
    private var webPoTokenGenerator: PoTokenMinter? = null
    private data class CachedPoTokens(
        val playerRequest: String,
        val streamingData: String,
        val expiresAtMs: Long,
    )
    private val cachedVideoTokens = ConcurrentHashMap<String, CachedPoTokens>()
    private val pendingVideoTokens = ConcurrentHashMap<String, CompletableDeferred<PoTokenResult?>>()

    suspend fun getWebClientPoToken(videoId: String, sessionId: String): PoTokenResult? {
        val key = "$sessionId\u0000$videoId"
        cachedVideoTokens[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let {
            return PoTokenResult(playerRequestPoToken = it.playerRequest, streamingDataPoToken = it.streamingData)
        }
        val supported = available()
        TrackLog.d(TAG, "WebView state: supported=$supported, badImpl=$webViewBadImpl")
        if (!supported || webViewBadImpl) {
            TrackLog.d(TAG, "WebView not available: supported=$supported, badImpl=$webViewBadImpl")
            return null
        }
        return try {
            withTimeout(POTOKEN_TIMEOUT_MS) {
                cachedOrMintVideoToken(key, videoId, sessionId)
            }
        } catch (e: TimeoutCancellationException) {
            TrackLog.w(TAG, "poToken generation timed out after ${POTOKEN_TIMEOUT_MS}ms; proceeding without PoToken")
            clearGenerator()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: BadWebViewException) {
            TrackLog.e(TAG, "Could not obtain PO token because WebView is unavailable")
            webViewBadImpl = true
            null
        } catch (e: Exception) {
            TrackLog.e(TAG, "PO token generation failed type=${e::class.simpleName ?: "unknown"}")
            throw e
        }
    }

    suspend fun close() {
        clearGenerator()
        cachedVideoTokens.clear()
    }

    private suspend fun cachedOrMintVideoToken(
        key: String,
        videoId: String,
        sessionId: String,
    ): PoTokenResult? {
        cachedVideoTokens[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let {
            return PoTokenResult(playerRequestPoToken = it.playerRequest, streamingDataPoToken = it.streamingData)
        }
        val created = CompletableDeferred<PoTokenResult?>()
        val pending = pendingVideoTokens.putIfAbsent(key, created)
        if (pending != null) return pending.await()
        try {
            val tokens = getWebClientPoToken(videoId, sessionId, forceRecreate = false)
            cachedVideoTokens[key] = CachedPoTokens(
                playerRequest = tokens.playerRequestPoToken,
                streamingData = tokens.streamingDataPoToken,
                expiresAtMs = System.currentTimeMillis() + VIDEO_TOKEN_TTL_MS,
            )
            if (cachedVideoTokens.size > MAX_CACHED_VIDEO_TOKENS) {
                val now = System.currentTimeMillis()
                cachedVideoTokens.entries.removeAll { it.value.expiresAtMs <= now }
                if (cachedVideoTokens.size > MAX_CACHED_VIDEO_TOKENS) cachedVideoTokens.clear()
            }
            created.complete(tokens)
            return tokens
        } catch (error: Throwable) {
            created.completeExceptionally(error)
            throw error
        } finally {
            pendingVideoTokens.remove(key, created)
        }
    }

    private suspend fun clearGenerator() {
        webPoTokenGenLock.withLock {
            try {
                webPoTokenGenerator?.close()
            } catch (error: Exception) {
                TrackLog.e(TAG, "PO token WebView cleanup failed type=${error::class.simpleName ?: "unknown"}")
            }
            webPoTokenGenerator = null
            webPoTokenStreamingPot = null
            webPoTokenSessionId = null
        }
    }

    private companion object {
        const val POTOKEN_TIMEOUT_MS = 8_000L
        const val VIDEO_TOKEN_TTL_MS = 45 * 60 * 1000L
        const val MAX_CACHED_VIDEO_TOKENS = 256
    }

    private suspend fun getWebClientPoToken(videoId: String, sessionId: String, forceRecreate: Boolean): PoTokenResult {
        val (poTokenGenerator, streamingPot, hasBeenRecreated) =
            webPoTokenGenLock.withLock {
                val shouldRecreate =
                    forceRecreate || webPoTokenGenerator == null || webPoTokenGenerator!!.isExpired ||
                        webPoTokenGenerator!!.isDead ||
                        webPoTokenSessionId != sessionId
                if (shouldRecreate) {
                    TrackLog.d(TAG, "Creating new PoTokenWebView (forceRecreate=$forceRecreate)")
                    webPoTokenGenerator?.close()
                    webPoTokenGenerator = null
                    webPoTokenStreamingPot = null
                    webPoTokenSessionId = null
                    val newGenerator = createMinter()
                    val newStreamingPot = try {
                        newGenerator.generatePoToken(sessionId)
                    } catch (t: Throwable) {
                        runCatching { newGenerator.close() }
                        throw t
                    }
                    webPoTokenGenerator = newGenerator
                    webPoTokenStreamingPot = newStreamingPot
                    webPoTokenSessionId = sessionId
                    TrackLog.d(TAG, "Streaming PO token generated")
                }
                Triple(webPoTokenGenerator!!, webPoTokenStreamingPot!!, shouldRecreate)
            }
        val playerPot = try {
            poTokenGenerator.generatePoToken(videoId)
        } catch (throwable: Throwable) {
            if (hasBeenRecreated) {
                throw throwable
            } else {
                TrackLog.e(TAG, "PO-token generation failed; recreating WebView")
                return getWebClientPoToken(videoId = videoId, sessionId = sessionId, forceRecreate = true)
            }
        }
        TrackLog.d(TAG, "PO token generated successfully")
        return PoTokenResult(
            playerRequestPoToken = streamingPot,
            streamingDataPoToken = playerPot,
        )
    }

    /** This generator as InnerTubeX's token provider, for WEB_REMIX and the other BotGuard clients. */
    fun asTokenProvider(): TokenProvider = object : TokenProvider {
        override val capabilities = TokenProviderCapabilities(
            providers = setOf(PoTokenProviderKind.WEB_BOTGUARD),
            usesWebView = true,
        )

        override suspend fun getPoToken(
            videoId: String,
            visitorData: String,
            cookie: String?,
        ): com.metrolist.innertubex.extraction.PoTokenResult? =
            getWebClientPoToken(videoId, visitorData)?.let { token ->
                com.metrolist.innertubex.extraction.PoTokenResult(
                    playerRequestToken = token.playerRequestPoToken,
                    streamingDataToken = token.streamingDataPoToken,
                    visitorData = visitorData,
                )
            }

        override suspend fun close() {
            this@PoTokenGenerator.close()
        }
    }
}
