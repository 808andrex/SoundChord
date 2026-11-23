package com.music.bitchord.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.util.LruCache
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.music.bitchord.data.model.NOTIFICATION_ART_PX
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.artworkAt
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Adaptive palette and theme colors for home-screen widgets (Apple Music / iOS style).
 */
internal data class WidgetTheme(
    val bgStart: Int,
    val bgEnd: Int,
    val titleColor: Int,
    val artistColor: Int,
    val iconColor: Int,
    val logoColor: Int,
    val favoriteActiveColor: Int,
    val favoriteInactiveColor: Int,
    val isNight: Boolean,
)

/**
 * The rendered assets for an active or idle home-screen widget.
 */
internal data class WidgetRenderResult(
    val background: Bitmap,
    val art: Bitmap,
    val theme: WidgetTheme,
)

/**
 * Compositor and asset generator for SoundChord home-screen widgets.
 *
 * Generates:
 *  - A dynamic, vibrant gradient background reflecting the artwork's dominant color.
 *  - Square artwork cropped and rounded with smooth corners.
 *  - High-contrast text and control colors for 100% legibility.
 */
internal object MediaWidgetArt {

    /**
     * Renders background, artwork and adaptive theme for a widget instance.
     */
    suspend fun renderWidget(
        context: Context,
        artworkUrl: String?,
        widthPx: Int,
        heightPx: Int,
        wide: Boolean,
        cornerRadiusPx: Float,
        density: Float,
        key: String?,
        isNight: Boolean,
    ): WidgetRenderResult {
        peekWidget(key, widthPx, heightPx, wide, isNight)?.let { return it }

        val cacheKey = key?.let { widgetKey(it, widthPx, heightPx, wide, isNight) }
        val rawCover = if (isCoolingOff(key)) null else loadArtwork(context, artworkUrl, NOTIFICATION_ART_PX)
        val failed = rawCover == null && !artworkUrl.isNullOrBlank()
        if (failed && key != null) failures[key] = SystemClock.elapsedRealtime()

        val theme = extractTheme(rawCover, isNight)
        val background = createBackgroundBitmap(widthPx, heightPx, cornerRadiusPx, theme.bgStart, theme.bgEnd)

        val artSizePx = if (wide) {
            (heightPx - (24 * density)).roundToInt().coerceAtLeast(40)
        } else {
            (70 * density).roundToInt().coerceAtLeast(40)
        }
        val artCornerRadiusPx = if (wide) (15 * density) else (13 * density)
        val art = createRoundedArtBitmap(rawCover, artSizePx, artCornerRadiusPx, isNight)

        val result = WidgetRenderResult(background, art, theme)
        if (!failed && cacheKey != null) {
            widgetCache.put(cacheKey, result)
        }
        return result
    }

    /**
     * Checks if a rendered result exists in cache.
     */
    fun peekWidget(
        key: String?,
        widthPx: Int,
        heightPx: Int,
        wide: Boolean,
        isNight: Boolean,
    ): WidgetRenderResult? {
        val k = key ?: return null
        return widgetCache[widgetKey(k, widthPx, heightPx, wide, isNight)]
            ?.takeIf { !it.background.isRecycled && !it.art.isRecycled }
    }

    /**
     * Neutral elegant fallback when nothing has played yet.
     */
    fun fallbackResult(
        widthPx: Int,
        heightPx: Int,
        wide: Boolean,
        cornerRadiusPx: Float,
        density: Float,
        isNight: Boolean,
    ): WidgetRenderResult {
        val theme = fallbackTheme(isNight)
        val background = createBackgroundBitmap(widthPx, heightPx, cornerRadiusPx, theme.bgStart, theme.bgEnd)
        val artSizePx = if (wide) {
            (heightPx - (24 * density)).roundToInt().coerceAtLeast(40)
        } else {
            (70 * density).roundToInt().coerceAtLeast(40)
        }
        val artCornerRadiusPx = if (wide) (15 * density) else (13 * density)
        val art = createRoundedArtBitmap(null, artSizePx, artCornerRadiusPx, isNight)
        return WidgetRenderResult(background, art, theme)
    }

    /**
     * Extracts dynamic vibrant gradient and high-contrast text/icon colors from cover art.
     */
    private fun extractTheme(rawCover: Bitmap?, isNight: Boolean): WidgetTheme {
        if (rawCover == null) return fallbackTheme(isNight)

        val palette = Palette.from(rawCover)
            .maximumColorCount(24)
            .clearFilters()
            .generate()

        val swatches = palette.swatches
        if (swatches.isEmpty()) return fallbackTheme(isNight)

        val hslTemp = FloatArray(3)

        // Calculate weighted average lightness across the entire cover to determine
        // whether the cover is predominantly white/light or black/dark
        val totalPop = swatches.sumOf { it.population }.coerceAtLeast(1).toFloat()
        val avgLightness = swatches.sumOf { swatch ->
            ColorUtils.colorToHSL(swatch.rgb, hslTemp)
            (hslTemp[2] * swatch.population).toDouble()
        }.toFloat() / totalPop

        // Find chromatic candidates with noticeable saturation (not blown out to black, white or grey)
        val chromaticCandidates = swatches.filter { swatch ->
            ColorUtils.colorToHSL(swatch.rgb, hslTemp)
            hslTemp[1] >= 0.15f && hslTemp[2] in 0.08f..0.92f
        }

        // Score by saturation * sqrt(population) so iconic vivid album colors (like Channel Orange)
        // are prioritized over dark letterboxing or shadows
        val chosenSwatch = if (chromaticCandidates.isNotEmpty()) {
            chromaticCandidates.maxByOrNull { swatch ->
                ColorUtils.colorToHSL(swatch.rgb, hslTemp)
                hslTemp[1] * sqrt(swatch.population.toFloat())
            } ?: palette.dominantSwatch ?: swatches.first()
        } else {
            palette.dominantSwatch ?: swatches.maxByOrNull { it.population } ?: swatches.first()
        }

        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(chosenSwatch.rgb, hsl)
        val hue = hsl[0]
        val saturation = hsl[1]
        val isChromatic = saturation >= 0.15f && chromaticCandidates.isNotEmpty()

        return if (isNight) {
            if (isChromatic) {
                // Vibrant chromatic album (e.g. Channel Orange):
                // Rich, warm medium-tone gradient with high saturation.
                // Lightness capped between 22% and 35% so white text (#FFFFFF) has > 5.5:1 contrast.
                val satStart = saturation.coerceIn(0.60f, 0.90f)
                val satEnd = (saturation * 1.10f).coerceIn(0.65f, 0.95f)
                val bgStart = ColorUtils.HSLToColor(floatArrayOf(hue, satStart, 0.35f))
                val bgEnd = ColorUtils.HSLToColor(floatArrayOf(hue, satEnd, 0.23f))
                WidgetTheme(
                    bgStart = bgStart,
                    bgEnd = bgEnd,
                    titleColor = 0xFFFFFFFF.toInt(),
                    artistColor = 0xD9FFFFFF.toInt(), // 85% white
                    iconColor = 0xFFFFFFFF.toInt(),
                    logoColor = 0xFFFFFFFF.toInt(),
                    favoriteActiveColor = 0xFFFF453A.toInt(),
                    favoriteInactiveColor = 0x80FFFFFF.toInt(),
                    isNight = true,
                )
            } else {
                // Truly monochrome or low-saturation album (e.g. The Weeknd's Trilogy, Drake's IYRTITL, Mac Miller's GO:OD AM):
                // Never use blue or purple fallback. Extract the true neutral tone of the cover:
                // Smoothly map from dark charcoal (#323236 -> #1E1E22) for black covers (avgLightness <= 0.20)
                // to smoke grey / medium graphite (#4A4A4E -> #2E2E32) for white/light covers (avgLightness >= 0.80).
                val t = ((avgLightness - 0.20f) / 0.60f).coerceIn(0f, 1f)
                val startR = (0x32 + (0x4A - 0x32) * t).roundToInt()
                val startG = (0x32 + (0x4A - 0x32) * t).roundToInt()
                val startB = (0x36 + (0x4E - 0x36) * t).roundToInt()

                val endR = (0x1E + (0x2E - 0x1E) * t).roundToInt()
                val endG = (0x1E + (0x2E - 0x1E) * t).roundToInt()
                val endB = (0x22 + (0x32 - 0x22) * t).roundToInt()

                val bgStart = (0xFF shl 24) or (startR shl 16) or (startG shl 8) or startB
                val bgEnd = (0xFF shl 24) or (endR shl 16) or (endG shl 8) or endB

                WidgetTheme(
                    bgStart = bgStart,
                    bgEnd = bgEnd,
                    titleColor = 0xFFFFFFFF.toInt(),
                    artistColor = 0xD9FFFFFF.toInt(),
                    iconColor = 0xFFFFFFFF.toInt(),
                    logoColor = 0xFFFFFFFF.toInt(),
                    favoriteActiveColor = 0xFFFF453A.toInt(),
                    favoriteInactiveColor = 0x80FFFFFF.toInt(),
                    isNight = true,
                )
            }
        } else {
            // Light theme
            if (isChromatic) {
                val satStart = (saturation * 0.35f).coerceIn(0.12f, 0.35f)
                val satEnd = (saturation * 0.70f).coerceIn(0.25f, 0.60f)
                val bgStart = ColorUtils.HSLToColor(floatArrayOf(hue, satStart, 0.96f))
                val bgEnd = ColorUtils.HSLToColor(floatArrayOf(hue, satEnd, 0.86f))
                WidgetTheme(
                    bgStart = bgStart,
                    bgEnd = bgEnd,
                    titleColor = 0xFF1C1C1E.toInt(),
                    artistColor = 0xFF545458.toInt(),
                    iconColor = 0xFF1C1C1E.toInt(),
                    logoColor = 0xFF1C1C1E.toInt(),
                    favoriteActiveColor = 0xFFFA2D48.toInt(),
                    favoriteInactiveColor = 0x801C1C1E.toInt(),
                    isNight = false,
                )
            } else {
                val t = ((avgLightness - 0.20f) / 0.60f).coerceIn(0f, 1f)
                val bgStart = if (t >= 0.5f) 0xFFF2F2F5.toInt() else 0xFFE5E5EA.toInt()
                val bgEnd = if (t >= 0.5f) 0xFFE2E2E6.toInt() else 0xFFD1D1D6.toInt()
                WidgetTheme(
                    bgStart = bgStart,
                    bgEnd = bgEnd,
                    titleColor = 0xFF1C1C1E.toInt(),
                    artistColor = 0xFF545458.toInt(),
                    iconColor = 0xFF1C1C1E.toInt(),
                    logoColor = 0xFF1C1C1E.toInt(),
                    favoriteActiveColor = 0xFFFA2D48.toInt(),
                    favoriteInactiveColor = 0x801C1C1E.toInt(),
                    isNight = false,
                )
            }
        }
    }

    private fun fallbackTheme(isNight: Boolean): WidgetTheme = if (isNight) {
        WidgetTheme(
            bgStart = 0xFF323236.toInt(),
            bgEnd = 0xFF1E1E22.toInt(),
            titleColor = 0xFFFFFFFF.toInt(),
            artistColor = 0xD9FFFFFF.toInt(),
            iconColor = 0xFFFFFFFF.toInt(),
            logoColor = 0xFFFFFFFF.toInt(),
            favoriteActiveColor = 0xFFFF453A.toInt(),
            favoriteInactiveColor = 0x40FFFFFF.toInt(),
            isNight = true,
        )
    } else {
        WidgetTheme(
            bgStart = 0xFFF2F2F5.toInt(),
            bgEnd = 0xFFE2E2E6.toInt(),
            titleColor = 0xFF1C1C1E.toInt(),
            artistColor = 0xFF8E8E93.toInt(),
            iconColor = 0xCC1C1C1E.toInt(),
            logoColor = 0xCC1C1C1E.toInt(),
            favoriteActiveColor = 0xFFFA2D48.toInt(),
            favoriteInactiveColor = 0x401C1C1E.toInt(),
            isNight = false,
        )
    }

    /**
     * Creates a background bitmap filled edge-to-edge with a smooth linear gradient.
     * The widget root container with clipToOutline applies the single uniform rounded outline.
     */
    private fun createBackgroundBitmap(
        widthPx: Int,
        heightPx: Int,
        cornerRadiusPx: Float,
        startColor: Int,
        endColor: Int,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val shader = LinearGradient(
            0f, 0f,
            widthPx.toFloat() * 0.45f, heightPx.toFloat(),
            startColor, endColor,
            Shader.TileMode.CLAMP,
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = shader
        }
        val rect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
        canvas.drawRect(rect, paint)
        return bitmap
    }

    /**
     * Creates a square album cover bitmap with rounded corners and subtle border stroke.
     */
    private fun createRoundedArtBitmap(
        source: Bitmap?,
        sizePx: Int,
        cornerRadiusPx: Float,
        isNight: Boolean,
    ): Bitmap {
        val targetSize = sizePx.coerceAtLeast(1)
        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val rect = RectF(0f, 0f, targetSize.toFloat(), targetSize.toFloat())

        if (source != null) {
            val scale = maxOf(targetSize.toFloat() / source.width, targetSize.toFloat() / source.height)
            val scaledW = source.width * scale
            val scaledH = source.height * scale
            val dx = (targetSize - scaledW) / 2f
            val dy = (targetSize - scaledH) / 2f

            val matrix = Matrix().apply {
                setScale(scale, scale)
                postTranslate(dx, dy)
            }
            val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(matrix)
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                this.shader = shader
            }
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)

            // Subtle inner border for contrast against very light or very dark backgrounds
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1f
                color = if (isNight) 0x1AFFFFFF else 0x14000000
            }
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, borderPaint)
        } else {
            // Elegant neutral placeholder
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isNight) 0xFF2C2C2E.toInt() else 0xFFE5E5EA.toInt()
            }
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, bgPaint)

            // Centered musical notes icon
            val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isNight) 0x66FFFFFF else 0x40000000
                style = Paint.Style.FILL
            }
            val r = targetSize * 0.16f
            canvas.drawCircle(targetSize * 0.38f, targetSize * 0.62f, r, iconPaint)
            canvas.drawCircle(targetSize * 0.66f, targetSize * 0.52f, r, iconPaint)
            val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = iconPaint.color
                style = Paint.Style.STROKE
                strokeWidth = r * 0.45f
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(targetSize * 0.38f + r * 0.9f, targetSize * 0.62f, targetSize * 0.38f + r * 0.9f, targetSize * 0.32f, stemPaint)
            canvas.drawLine(targetSize * 0.66f + r * 0.9f, targetSize * 0.52f, targetSize * 0.66f + r * 0.9f, targetSize * 0.24f, stemPaint)
            canvas.drawLine(targetSize * 0.38f + r * 0.9f, targetSize * 0.32f, targetSize * 0.66f + r * 0.9f, targetSize * 0.24f, stemPaint)
        }
        return output
    }

    private fun widgetKey(key: String, widthPx: Int, heightPx: Int, wide: Boolean, isNight: Boolean) =
        "widget|$key|$widthPx|$heightPx|$wide|$isNight"

    private suspend fun loadArtwork(context: Context, url: String?, longestSidePx: Int): Bitmap? {
        if (url.isNullOrBlank()) return null
        val px = artPxFor(longestSidePx)
        val fallbackPx = NOTIFICATION_ART_PX.takeIf { it != px }
        return load(context, url, px) ?: fallbackPx?.let { load(context, url, it) }
    }

    private suspend fun load(context: Context, url: String, px: Int): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(url.artworkAt(px))
            .size(px)
            .allowHardware(false)
            .build()
        return when (val result = SingletonImageLoader.get(context).execute(request)) {
            is SuccessResult -> result.image.toBitmap()
            else -> null
        }
    }

    private fun artPxFor(requestedPx: Int): Int = when {
        requestedPx <= ROW_ART_PX -> ROW_ART_PX
        else -> NOTIFICATION_ART_PX
    }

    private fun isCoolingOff(key: String?): Boolean {
        val failedAt = key?.let { failures[it] } ?: return false
        val since = SystemClock.elapsedRealtime() - failedAt
        if (since in 0 until FAILURE_COOLDOWN_MS) return true
        failures.remove(key)
        return false
    }

    fun clear() {
        widgetCache.evictAll()
        failures.clear()
    }

    // ------------------------------------------------------------------------
    // Backward-compatibility hooks for MediaWidgetPill
    // ------------------------------------------------------------------------

    suspend fun circle(context: Context, artworkUrl: String?, sizePx: Int, key: String): Bitmap? {
        val cover = loadArtwork(context, artworkUrl, sizePx) ?: return null
        val square = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(square)
        val scale = maxOf(sizePx.toFloat() / cover.width, sizePx.toFloat() / cover.height)
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate((sizePx - cover.width * scale) / 2f, (sizePx - cover.height * scale) / 2f)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(cover, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(matrix)
            }
        }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)
        return square
    }

    fun peekCircle(key: String, sizePx: Int): Bitmap? = null

    suspend fun pill(context: Context, artworkUrl: String?, widthPx: Int, heightPx: Int, key: String?): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawRoundRect(RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()), heightPx / 2f, heightPx / 2f, Paint().apply {
            color = 0xFF1B2130.toInt()
        })
        return bmp
    }

    fun peekPill(key: String, widthPx: Int, heightPx: Int): Bitmap? = null

    private val widgetCache = object : LruCache<String, WidgetRenderResult>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: WidgetRenderResult): Int =
            value.background.allocationByteCount + value.art.allocationByteCount
    }

    private val failures = ConcurrentHashMap<String, Long>()
    private const val FAILURE_COOLDOWN_MS = 30_000L
}
