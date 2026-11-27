package com.music.bitchord.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.ic_logo
import com.music.bitchord.ui.screens.LocalArtworkMotion
import com.music.bitchord.ui.screens.LocalArtworkScrolling
import kotlinx.coroutines.flow.collectLatest
import org.intellij.lang.annotations.Language
import org.jetbrains.compose.resources.painterResource
import kotlin.math.cos
import kotlin.math.sin

/** Standard tall Hero card dimensions matching Apple Music style. */
val HERO_CARD_HEIGHT = 370.dp
val HeroCardShape = RoundedCornerShape(24.dp)

/** Favorites Mix fluid palette: rich violet, purple and lilac tones ported from Zenith. */
val FavoritesMixFluidPalette: List<Color> = listOf(
    Color(0xFF5B1FD1),
    Color(0xFF8E2DE2),
    Color(0xFFB65CFF),
    Color(0xFF3A1C71),
    Color(0xFFD27BFF),
)

/** New Music Mix fluid palette: energetic hot pink, crimson, coral and peach (Apple Music aesthetic). */
val NewMusicMixFluidPalette: List<Color> = listOf(
    Color(0xFFFF0055),
    Color(0xFFF00035),
    Color(0xFFFF696F),
    Color(0xFFFF9B91),
    Color(0xFFFFB0B8),
)

/**
 * Reusable animated fluid gradient Hero card for SoundChord's "Top selecciones para ti" row.
 * Respects 24.dp rounded corners, thumbnail borders, SoundChord logo in top-end,
 * and high-contrast editorial typography.
 */
@Composable
fun AnimatedHeroGradientCard(
    title: String,
    eyebrow: String,
    subtitle: String,
    colors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    eyebrowColor: Color = Color.White.copy(alpha = 0.92f),
    animationSpeed: Float = 1.0f,
) {
    Box(
        modifier = modifier
            .height(HERO_CARD_HEIGHT)
            .clip(HeroCardShape)
            .thumbnailBorder(HeroCardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongClick)
            .contextClick(onLongClick),
    ) {
        // Fluid mesh animated background
        OrganicFluidBackground(
            modifier = Modifier.fillMaxSize(),
            colors = colors,
            animationSpeed = animationSpeed,
        )

        // Contrast scrim for caption legibility at bottom, keeping center vibrant
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.50f to Color.Transparent,
                        0.72f to Color.Black.copy(alpha = 0.25f),
                        0.88f to Color.Black.copy(alpha = 0.55f),
                        1.00f to Color.Black.copy(alpha = 0.75f),
                    ),
                ),
        )

        // SoundChord logo on top right of the card
        Icon(
            painter = painterResource(Res.drawable.ic_logo),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp, end = 18.dp)
                .height(18.dp)
                .aspectRatio(730f / 484f),
        )

        // Large prominent title centered horizontally and vertically on the card (Apple Music style)
        val formattedTitle = if (title.contains(" ") && !title.contains("\n")) {
            // Split "Nueva música" or "Favorites Mix" cleanly across 2 lines
            val mid = title.indexOf(' ')
            title.substring(0, mid) + "\n" + title.substring(mid + 1)
        } else {
            title
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 54.dp), // slightly above bottom captions
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = formattedTitle,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 44.sp,
                    lineHeight = 48.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Editorial caption at the bottom (eyebrow "Hecho para ti" + dynamic subtitle with artists)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        ) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp,
                ),
                color = eyebrowColor.copy(alpha = 0.90f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Organic fluid background: continuous flowing mesh gradient inspired by Apple Music and Zenith.
 *
 * Uses AGSL RuntimeShader on Android 13+ (API 33+) with domain warping, anisotropic Gaussian blobs
 * rotating and breathing, sheen and dither.
 * Falls back to optimized multi-point Canvas radial gradients below API 33.
 * Pauses automatically during scrolls (via LocalArtworkScrolling) or when out of screen.
 */
@Composable
fun OrganicFluidBackground(
    modifier: Modifier = Modifier,
    colors: List<Color> = FavoritesMixFluidPalette,
    animationSpeed: Float = 1f,
) {
    val context = LocalContext.current
    val motionAllowed = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val artworkMotion = LocalArtworkMotion.current
    val isScrolling = LocalArtworkScrolling.current
    val effectiveSpeed = if (motionAllowed && artworkMotion) animationSpeed else 0f
    val speedState = rememberUpdatedState(effectiveSpeed)
    val colorsState = rememberUpdatedState(colors.ifEmpty { FavoritesMixFluidPalette })

    val clock = remember { FluidClock(startAt = FluidTimeline.phase) }
    val agsl = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && OrganicFluidSupport.shaderSupported) {
            runCatching { AgslFluid() }.getOrNull()
        } else {
            null
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, clock) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            snapshotFlow { clock.visible && speedState.value > 0f && !isScrolling() }.collectLatest { running ->
                if (!running) return@collectLatest
                while (true) {
                    val frameNanos = withFrameNanos { it }
                    clock.seconds = FluidTimeline.tick(frameNanos, speedState.value)
                }
            }
        }
    }

    val screenW = LocalConfiguration.current.screenWidthDp * LocalDensity.current.density
    val screenH = LocalConfiguration.current.screenHeightDp * LocalDensity.current.density

    Spacer(
        modifier
            .onGloballyPositioned { clock.visible = it.isOnScreen(screenW, screenH) }
            .drawBehind {
                val t = clock.seconds
                if (agsl != null) {
                    agsl.draw(this, t.toFloat(), colorsState.value)
                } else {
                    drawFluidFallback(t.toFloat(), colorsState.value)
                }
            },
    )
}

private object FluidTimeline {
    var phase: Double = 30.0
        private set
    private var lastFrameNanos = 0L

    fun tick(frameNanos: Long, speed: Float): Double {
        if (frameNanos > lastFrameNanos) {
            val dt = if (lastFrameNanos == 0L) 0.0 else ((frameNanos - lastFrameNanos) / 1_000_000_000.0).coerceAtMost(0.1)
            lastFrameNanos = frameNanos
            phase += dt * speed
        }
        return phase
    }
}

private class FluidClock(startAt: Double) {
    var seconds by mutableDoubleStateOf(startAt)
    var visible by mutableStateOf(true)
}

private fun LayoutCoordinates.isOnScreen(screenW: Float, screenH: Float): Boolean {
    if (!isAttached) return false
    val r = boundsInWindow()
    return r.width > 0f && r.height > 0f && r.right > 0f && r.bottom > 0f &&
        r.left < screenW && r.top < screenH
}

private val UNIFORM_NAMES = arrayOf("c0", "c1", "c2", "c3", "c4")

object OrganicFluidSupport {
    val shaderSupported: Boolean by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            runCatching { RuntimeShader(FLUID_AGSL) }.isSuccess
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class AgslFluid {
    private val shader = RuntimeShader(FLUID_AGSL)
    private val brush = ShaderBrush(shader)

    fun draw(scope: DrawScope, time: Float, colors: List<Color>) {
        shader.setFloatUniform("iResolution", scope.size.width, scope.size.height)
        shader.setFloatUniform("iTime", time)
        val n = colors.size
        for (i in 0 until 5) {
            val c = colors[i % n]
            shader.setFloatUniform(UNIFORM_NAMES[i], c.red, c.green, c.blue)
        }
        scope.drawRect(brush = brush)
    }
}

private fun DrawScope.drawFluidFallback(t: Float, colors: List<Color>) {
    val n = colors.size
    fun pal(i: Int) = colors[((i % n) + n) % n]
    drawRect(Brush.verticalGradient(listOf(pal(2), pal(3), pal(4))))
    val w = size.width
    val h = size.height
    val m = maxOf(w, h)
    for (i in 0 until 6) {
        val k = i + 1f
        val c = Offset(
            w * (0.5f + 0.40f * sin(t * (0.06f + 0.017f * k) + k * 2.3f) + 0.12f * sin(t * (0.15f + 0.01f * k) + k * 5.1f)),
            h * (0.5f + 0.40f * cos(t * (0.07f + 0.013f * k) + k * 1.7f) + 0.12f * sin(t * (0.13f + 0.012f * k) + k * 3.3f)),
        )
        val r = m * (0.38f + 0.12f * sin(t * (0.09f + 0.01f * k) + k))
        val color = pal(i + 1)
        val deg = (t * (3f + k) * (if (i % 2 == 0) 1f else -1f)) + k * 40f
        val stretch = 1.5f + 0.5f * sin(t * 0.11f * k)
        rotate(deg, pivot = c) {
            scale(stretch, 1f / stretch, pivot = c) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.92f), color.copy(alpha = 0f)),
                        center = c,
                        radius = r,
                    ),
                    radius = r,
                    center = c,
                )
            }
        }
    }
}

@Language("AGSL")
private const val FLUID_AGSL = """
uniform float2 iResolution;
uniform float iTime;
uniform float3 c0;
uniform float3 c1;
uniform float3 c2;
uniform float3 c3;
uniform float3 c4;

float3 pal(float x) {
    x = mod(x, 5.0);
    float3 a = c0;
    float3 b = c1;
    float f = x;
    if (x >= 4.0) { a = c4; b = c0; f = x - 4.0; }
    else if (x >= 3.0) { a = c3; b = c4; f = x - 3.0; }
    else if (x >= 2.0) { a = c2; b = c3; f = x - 2.0; }
    else if (x >= 1.0) { a = c1; b = c2; f = x - 1.0; }
    return mix(a, b, f * f * (3.0 - 2.0 * f));
}

float2 rot(float2 v, float a) {
    float s = sin(a);
    float c = cos(a);
    return float2(c * v.x - s * v.y, s * v.x + c * v.y);
}

half4 main(float2 fc) {
    float2 p = (fc - 0.5 * iResolution) / min(iResolution.x * 1.35, iResolution.y);
    float t = iTime;

    // Domain warping: three sinusoidal flow octaves
    float2 w = p;
    w += 0.30 * float2(sin(p.y * 2.3 + t * 0.23), cos(p.x * 2.1 - t * 0.19));
    w += 0.17 * float2(sin(w.y * 3.7 - t * 0.31 + 1.3), cos(w.x * 3.3 + t * 0.27 + 2.1));
    w += 0.08 * float2(sin(w.y * 6.1 + t * 0.41 + 0.7), cos(w.x * 5.5 - t * 0.37 + 4.0));

    // Base fluid flow
    float g = clamp(0.5 + w.y * 0.85 + 0.18 * sin(w.x * 2.0 + t * 0.13), 0.0, 1.0);
    float3 col = mix(pal(2.0 + t * 0.012), pal(3.55 + t * 0.012), g);

    // Dynamic rotating and breathing Gaussian ink blobs
    for (int i = 0; i < 6; i++) {
        float k = float(i) + 1.0;
        float h1 = fract(sin(k * 91.7) * 437.5);
        float h2 = fract(sin(k * 37.3) * 951.1);
        float h3 = fract(sin(k * 53.9) * 173.3);
        float h4 = fract(sin(k * 17.1) * 683.7);
        float2 c = float2(
            0.55 * sin(t * (0.06 + 0.08 * h1) + k * 2.3) + 0.20 * sin(t * (0.15 + 0.10 * h2) + k * 5.1),
            0.50 * sin(t * (0.07 + 0.08 * h2) + k * 1.7 + 1.0) + 0.20 * sin(t * (0.13 + 0.10 * h3) + k * 3.3)
        );
        float2 r = float2(
            0.26 + 0.32 * h3 + 0.08 * sin(t * (0.10 + 0.10 * h4) + k),
            0.11 + 0.13 * h4 + 0.05 * sin(t * (0.12 + 0.10 * h1) + k * 2.0)
        );
        float dir = h2 > 0.5 ? 1.0 : -1.0;
        float ang = t * (0.04 + 0.06 * h1) * dir + k * 1.3;
        float2 d = rot(w - c, ang) / r;
        float a = exp(-dot(d, d));
        col = mix(col, pal(k * 0.83 + t * 0.022 + h1 * 2.0), clamp(a, 0.0, 1.0) * 0.9);
    }

    // Soft light sheen
    float ridge = pow(0.5 + 0.5 * sin(w.x * 2.6 + w.y * 1.9 + t * 0.17), 3.0);
    col = mix(col, float3(1.0, 0.86, 0.86), 0.10 * ridge);

    // Subtle dither to avoid banding on wide color gamuts
    col += (fract(sin(dot(fc, float2(12.9898, 78.233))) * 43758.5453) - 0.5) / 255.0;
    return half4(half3(clamp(col, 0.0, 1.0)), 1.0);
}
"""
