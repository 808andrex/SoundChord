@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.music.bitchord.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ambient transition scopes for Apple Music / iOS style shared element transitions.
 * Wrapped in [CoverTransitionBox] or [SharedTransitionLayout] at navigation host level.
 */
val LocalCoverTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalCoverVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Map of actively tapped covers to disambiguate identical albums across multiple cards.
 */
private val tappedCovers = mutableStateMapOf<String, Any>()

/**
 * Call from a card's click before navigation starts.
 */
fun markCoverTapped(browseId: String?, token: Any) {
    if (browseId.isNullOrEmpty()) return
    if (tappedCovers.size > 32 && !tappedCovers.containsKey(browseId)) tappedCovers.clear()
    tappedCovers[browseId] = token
}

/**
 * Marks a cover as the shared element for [browseId].
 * On cards pass its [token]; on the detail page header leave [token] null.
 * Uses unified key format "artwork_$browseId".
 */
@Composable
fun Modifier.coverSharedElement(
    browseId: String?,
    shape: Shape? = null,
    token: Any? = null,
): Modifier {
    val scope = LocalCoverTransitionScope.current ?: return this
    val visibility = LocalCoverVisibilityScope.current ?: return this
    if (browseId.isNullOrEmpty()) return this
    val active = if (token == null) {
        true
    } else {
        val tapped by remember(token, browseId) {
            derivedStateOf { tappedCovers[browseId] === token }
        }
        tapped
    }
    if (!active) return this
    return with(scope) {
        this@coverSharedElement.sharedElement(
            sharedContentState = rememberSharedContentState(key = "artwork_$browseId"),
            animatedVisibilityScope = visibility,
            boundsTransform = { _, _ -> tween(durationMillis = 560, easing = CoverEasing) },
        )
    }
}

/**
 * A card's tap that opens a page: marks the cover, waits one frame so the card
 * has picked up its shared element and been laid out with it, then runs [action].
 */
@Composable
fun rememberCoverTap(browseId: String?, token: Any, action: () -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val latest = rememberUpdatedState(action)
    return {
        if (browseId.isNullOrEmpty()) {
            latest.value()
        } else {
            markCoverTapped(browseId, token)
            scope.launch {
                withFrameNanos { }
                latest.value()
            }
        }
    }
}

/** Long, soft landing: the cover is easy to follow and settles without a bounce. */
val CoverEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

/**
 * Fades and lifts a piece of a page in while its page is entering, after
 * [delayMs], so text and buttons arrive behind the cover rather than with it.
 * Runs on the draw layer (graphicsLayer) only: no recomposition or relayout.
 * On page exit / popBackStack, fades out rapidly (~100ms) so text never floats
 * over the returning screen.
 */
@Composable
fun Modifier.enterReveal(delayMs: Int = 0, rise: Dp = 14.dp): Modifier {
    val visibility = LocalCoverVisibilityScope.current ?: return this
    val progress by visibility.transition.animateFloat(
        transitionSpec = {
            if (targetState == EnterExitState.Visible) {
                tween(durationMillis = 420, delayMillis = delayMs + 260, easing = FastOutSlowInEasing)
            } else {
                tween(durationMillis = 100, easing = LinearEasing)
            }
        },
        label = "enterReveal",
    ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
    return this.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * rise.toPx()
    }
}

/**
 * Rapid exit fade (~100ms) for secondary page content (titles, action capsules, song rows,
 * descriptions, headers) on backstack pop/exit so that no text or buttons float over the
 * returning screen while the shared cover contracts smoothly to its origin card.
 */
@Composable
fun Modifier.secondaryExitFade(durationMillis: Int = 100): Modifier {
    val visibility = LocalCoverVisibilityScope.current ?: return this
    val progress by visibility.transition.animateFloat(
        transitionSpec = {
            if (targetState == EnterExitState.Visible) {
                tween(durationMillis = 200, easing = FastOutSlowInEasing)
            } else {
                tween(durationMillis = durationMillis, easing = LinearEasing)
            }
        },
        label = "secondaryExitFade",
    ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
    return this.graphicsLayer {
        alpha = progress
    }
}

/**
 * Staggered cascade entrance animation (*staggered fade + slide*) for grid items,
 * lists ("Ver todo" / LibraryShowAll), and song rows.
 * Animates vertical lift (12..16 dp) and fadeIn with proportional delay based on index.
 * Fades out rapidly (~100ms) on backstack pop/exit.
 */
@Composable
fun Modifier.staggeredEntrance(
    index: Int,
    baseDelayMs: Int = 40,
    stepMs: Int = 28,
    rise: Dp = 14.dp,
): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val delayTime = (baseDelayMs + (index.coerceAtMost(24) * stepMs)).toLong()
        if (delayTime > 0) delay(delayTime)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = 380f,
            ),
        )
    }
    val visibility = LocalCoverVisibilityScope.current
    val exitAlpha by visibility?.transition?.animateFloat(
        transitionSpec = {
            if (targetState == EnterExitState.Visible) {
                tween(durationMillis = 200)
            } else {
                tween(durationMillis = 100, easing = LinearEasing)
            }
        },
        label = "staggeredExit",
    ) { state -> if (state == EnterExitState.Visible) 1f else 0f } ?: remember { mutableFloatStateOf(1f) }

    return this.graphicsLayer {
        val p = progress.value
        val enterA = p.coerceIn(0f, 1f)
        alpha = enterA * exitAlpha
        if (p < 1f) {
            translationY = (1f - p) * rise.toPx()
        }
    }
}

/** A Box that hosts the shared transition for everything inside it. */
@Composable
fun CoverTransitionBox(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    SharedTransitionLayout(modifier) {
        CompositionLocalProvider(LocalCoverTransitionScope provides this) {
            Box(Modifier.fillMaxSize(), content = content)
        }
    }
}
