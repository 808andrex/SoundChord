package com.music.bitchord.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.widget.RemoteViews
import com.music.bitchord.MainActivity
import com.music.bitchord.R
import com.music.bitchord.playback.PlayerDeepLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/**
 * SoundChord home-screen widgets (Apple Music / iOS style).
 *
 * Two providers:
 *  - [MediaWidgetSquare] for 2×2 square widget.
 *  - [MediaWidgetWide] for 4×2 rectangular widget.
 *
 * Features:
 *  - Soft pastel dynamic gradient background based on album art palette.
 *  - Square artwork with smooth rounded corners.
 *  - Interactive Favorite button and transport controls responding instantly.
 *  - Deep link directly to the player when tapping cover or background.
 */
abstract class MediaWidget : AppWidgetProvider() {

    protected abstract val fallbackWidthDp: Int

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        renderAsync(context, ids)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        id: Int,
        newOptions: Bundle?,
    ) {
        renderAsync(context, intArrayOf(id))
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        for (id in ids) lastRender.remove(id)
    }

    override fun onDisabled(context: Context) {
        MediaWidgetArt.clear()
        lastRender.clear()
    }

    private fun renderAsync(context: Context, ids: IntArray) {
        val pending = goAsync()
        val app = context.applicationContext
        val fallback = fallbackWidthDp
        scope.launch {
            try {
                render(app, ids, fallback, SystemClock.elapsedRealtime() + BROADCAST_BUDGET_MS)
            } finally {
                runCatching { pending.finish() }
            }
        }
    }

    companion object {

        const val WIDE_LAYOUT_MIN_DP = 215

        fun refresh(context: Context) {
            val app = context.applicationContext
            scope.launch {
                val manager = runCatching { AppWidgetManager.getInstance(app) }.getOrNull() ?: return@launch
                for ((provider, fallbackWidthDp) in PROVIDERS) {
                    val ids = runCatching {
                        manager.getAppWidgetIds(ComponentName(app, provider))
                    }.getOrNull()
                    if (ids == null || ids.isEmpty()) continue
                    render(app, ids, fallbackWidthDp, deadline = null)
                }
            }
        }

        private suspend fun render(
            context: Context,
            ids: IntArray,
            fallbackWidthDp: Int,
            deadline: Long?,
        ) {
            val manager = runCatching { AppWidgetManager.getInstance(context) }.getOrNull() ?: return
            val snapshot = MediaWidgetSnapshot.load(context)
            val key = snapshot.artworkUrl ?: KEY_NO_ARTWORK
            for (id in ids) {
                val budget = when (deadline) {
                    null -> RENDER_TIMEOUT_MS
                    else -> minOf(RENDER_TIMEOUT_MS, deadline - SystemClock.elapsedRealtime())
                }
                if (budget <= 0L) return
                withTimeoutOrNull(budget) { render(context, manager, id, snapshot, key, fallbackWidthDp) }
            }
        }

        private suspend fun render(
            context: Context,
            manager: AppWidgetManager,
            id: Int,
            snapshot: MediaWidgetSnapshot,
            key: String,
            fallbackWidthDp: Int,
        ) {
            val size = measure(context, manager, id, fallbackWidthDp)
            val isNight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val cached = MediaWidgetArt.peekWidget(key, size.widthPx, size.heightPx, size.wide, isNight)

            if (cached != null) {
                lastRender[id] = cached
                manager.push(id, views(context, snapshot, size, cached))
                return
            }

            // Push last known or fallback immediately so buttons are responsive
            val immediate = lastRender[id] ?: MediaWidgetArt.fallbackResult(
                widthPx = size.widthPx,
                heightPx = size.heightPx,
                wide = size.wide,
                cornerRadiusPx = size.cornerRadiusPx,
                density = size.density,
                isNight = isNight,
            )
            manager.push(id, views(context, snapshot, size, immediate))

            val result = MediaWidgetArt.renderWidget(
                context = context,
                artworkUrl = snapshot.artworkUrl,
                widthPx = size.widthPx,
                heightPx = size.heightPx,
                wide = size.wide,
                cornerRadiusPx = size.cornerRadiusPx,
                density = size.density,
                key = key,
                isNight = isNight,
            )
            lastRender[id] = result
            manager.push(id, views(context, snapshot, size, result))
        }

        // ---- views ----

        private fun views(
            context: Context,
            snapshot: MediaWidgetSnapshot,
            size: WidgetSize,
            result: WidgetRenderResult,
        ): RemoteViews {
            val layout = if (size.wide) R.layout.widget_media_wide else R.layout.widget_media_compact
            val views = RemoteViews(context.packageName, layout)

            // Background & Artwork
            views.setImageViewBitmap(R.id.widget_background, result.background)
            views.setImageViewBitmap(R.id.widget_art, result.art)

            // Adaptive theme colors
            val theme = result.theme
            views.setTextColor(R.id.widget_title, theme.titleColor)
            views.setTextColor(R.id.widget_artist, theme.artistColor)
            views.setInt(R.id.widget_logo, "setColorFilter", theme.logoColor)
            views.setInt(R.id.widget_previous, "setColorFilter", theme.iconColor)
            views.setInt(R.id.widget_toggle, "setColorFilter", theme.iconColor)
            views.setInt(R.id.widget_next, "setColorFilter", theme.iconColor)

            // Text content
            views.setTextViewText(
                R.id.widget_title,
                if (snapshot.hasTrack) {
                    snapshot.title
                } else {
                    context.getString(R.string.widget_nothing_played)
                },
            )
            views.setTextViewText(
                R.id.widget_artist,
                if (snapshot.hasTrack) {
                    snapshot.artist
                } else {
                    context.getString(R.string.app_name)
                },
            )

            // Play/Pause transport toggle
            views.setImageViewResource(
                R.id.widget_toggle,
                if (snapshot.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            )
            views.setContentDescription(
                R.id.widget_toggle,
                context.getString(
                    if (snapshot.isPlaying) R.string.widget_pause else R.string.widget_play,
                ),
            )

            // Deep-link to player when tapping background, artwork or text info
            val open = openPlayer(context)
            views.setOnClickPendingIntent(R.id.widget_root, open)
            views.setOnClickPendingIntent(R.id.widget_background, open)
            views.setOnClickPendingIntent(R.id.widget_art, open)
            views.setOnClickPendingIntent(R.id.widget_info_container, open)

            if (snapshot.hasTrack) {
                views.setImageAlpha(R.id.widget_toggle, ALPHA_ENABLED)
                views.setImageAlpha(
                    R.id.widget_previous,
                    if (snapshot.hasPrevious) ALPHA_ENABLED else ALPHA_DISABLED,
                )
                views.setImageAlpha(
                    R.id.widget_next,
                    if (snapshot.hasNext) ALPHA_ENABLED else ALPHA_DISABLED,
                )

                views.setOnClickPendingIntent(
                    R.id.widget_toggle,
                    MediaWidgetActions.pendingIntent(context, MediaWidgetActions.ACTION_TOGGLE),
                )
                views.setOnClickPendingIntent(
                    R.id.widget_previous,
                    MediaWidgetActions.pendingIntent(context, MediaWidgetActions.ACTION_PREVIOUS),
                )
                views.setOnClickPendingIntent(
                    R.id.widget_next,
                    MediaWidgetActions.pendingIntent(context, MediaWidgetActions.ACTION_NEXT),
                )
            } else {
                for (button in TRANSPORT) {
                    views.setImageAlpha(button, ALPHA_DISABLED)
                    views.setOnClickPendingIntent(button, open)
                }
            }
            return views
        }

        private fun RemoteViews.setImageAlpha(viewId: Int, alpha: Int) =
            setInt(viewId, "setImageAlpha", alpha)

        private fun openPlayer(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(PlayerDeepLink.EXTRA_OPEN_PLAYER, true)
            return PendingIntent.getActivity(
                context,
                REQUEST_OPEN_PLAYER,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun AppWidgetManager.push(id: Int, views: RemoteViews) {
            runCatching { updateAppWidget(id, views) }
        }

        // ---- measuring ----

        class WidgetSize(
            val wide: Boolean,
            val widthPx: Int,
            val heightPx: Int,
            val cornerRadiusPx: Float,
            val density: Float,
        )

        private fun measure(
            context: Context,
            manager: AppWidgetManager,
            id: Int,
            fallbackWidthDp: Int,
        ): WidgetSize {
            val options = runCatching { manager.getAppWidgetOptions(id) }.getOrNull()
            val landscape =
                context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val widthDp = options?.getInt(
                if (landscape) {
                    AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
                } else {
                    AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
                },
            )?.takeIf { it > 0 } ?: fallbackWidthDp
            val heightDp = options?.getInt(
                if (landscape) {
                    AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
                } else {
                    AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
                },
            )?.takeIf { it > 0 } ?: FALLBACK_HEIGHT_DP

            val wide = widthDp >= WIDE_LAYOUT_MIN_DP
            val resources = context.resources
            val density = resources.displayMetrics.density
            val rawWidth = widthDp * density
            val rawHeight = heightDp * density
            val longest = maxOf(rawWidth, rawHeight)
            val scale = if (longest > MAX_BITMAP_PX) MAX_BITMAP_PX / longest else 1f

            val cornerRadiusPx = resources.getDimension(R.dimen.widget_corner_radius) * scale
            return WidgetSize(
                wide = wide,
                widthPx = (rawWidth * scale).roundToInt().coerceAtLeast(1),
                heightPx = (rawHeight * scale).roundToInt().coerceAtLeast(1),
                cornerRadiusPx = cornerRadiusPx,
                density = density * scale,
            )
        }

        // ---- constants ----

        private val PROVIDERS = listOf(
            MediaWidgetSquare::class.java to SQUARE_WIDTH_DP,
            MediaWidgetWide::class.java to WIDE_WIDTH_DP,
        )

        private val TRANSPORT =
            intArrayOf(R.id.widget_previous, R.id.widget_toggle, R.id.widget_next)

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        private const val RENDER_TIMEOUT_MS = 8_000L
        private const val BROADCAST_BUDGET_MS = 8_000L

        private val lastRender = ConcurrentHashMap<Int, WidgetRenderResult>()

        private const val MAX_BITMAP_PX = 1_200f
        private const val ALPHA_ENABLED = 255
        private const val ALPHA_DISABLED = 90
        private const val KEY_NO_ARTWORK = "no-artwork"
        private const val REQUEST_OPEN_PLAYER = 0
    }
}

private const val SQUARE_WIDTH_DP = 110
private const val WIDE_WIDTH_DP = 250
private const val FALLBACK_HEIGHT_DP = 110

class MediaWidgetSquare : MediaWidget() {
    override val fallbackWidthDp = SQUARE_WIDTH_DP
}

class MediaWidgetWide : MediaWidget() {
    override val fallbackWidthDp = WIDE_WIDTH_DP
}
