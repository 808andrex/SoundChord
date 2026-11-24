package com.music.bitchord.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Scope allowing internal components of [FloatingCard] to trigger its closing motion. */
class FloatingCardScope internal constructor(private val closeWith: () -> Unit) {
    fun close() = closeWith()
}

val LocalFloatingCard = compositionLocalOf<FloatingCardScope?> { null }

/**
 * A modal card anchored to the bottom edge of the screen, featuring frosted glass
 * liquid styling, iOS-like spring physics, and full touch dismissibility.
 *
 * Supports closing via:
 * - Dragging the top handle or header down.
 * - Tapping the dimmed scrim backdrop outside.
 * - Pressing system back.
 * - Tapping the circular close (X) button.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun FloatingCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    subtitle: String? = null,
    dismissible: Boolean = true,
    hazeState: HazeState? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp),
    actions: (@Composable () -> Unit)? = null,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val reduceMotion by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val dark = MaterialTheme.colorScheme.surface.luminance() <= 0.5f

    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var settling by remember { mutableStateOf<Job?>(null) }

    val anim = remember { Animatable(if (reduceMotion) 1f else 0f) }

    val close: () -> Unit = {
        if (!closing) {
            closing = true
            scope.launch {
                if (reduceMotion) {
                    anim.snapTo(0f)
                } else {
                    anim.animateTo(0f, tween(160, easing = FastOutLinearInEasing))
                }
                onDismiss()
            }
        }
    }

    val cardScope = remember { FloatingCardScope(close) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            anim.snapTo(1f)
        } else {
            anim.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 420f))
        }
    }

    BackHandler(enabled = dismissible && !closing, onBack = close)

    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.onSurface,
        LocalFloatingCard provides cardScope,
    ) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .zIndex(100f),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val maxAvailableHeight = maxHeight
            val density = LocalDensity.current
            val dismissThresholdPx = with(density) { 110.dp.toPx() }
            val shape = RoundedCornerShape(32.dp)

            // Scrim backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = anim.value.coerceIn(0f, 1f)
                    }
                    .background(Color.Black.copy(alpha = if (dark) 0.65f else 0.28f))
                    .pointerInput(dismissible) {
                        detectTapGestures {
                            if (dismissible) close()
                        }
                    },
            )

            val barsTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val barsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            val canBlur = !reduceDynamicBlur && hazeState != null
            val glassSurface = if (dark) {
                Color(0xFF141418).copy(alpha = 0.82f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
            }

            Column(
                modifier = Modifier
                    .padding(top = barsTop, bottom = barsBottom)
                    .imePadding()
                    .padding(12.dp)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .heightIn(max = maxAvailableHeight * 0.90f)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        translationY = (1f - anim.value) * size.height * 0.45f + dragY
                        val s = if (reduceMotion) 1f else 0.92f + 0.08f * anim.value
                        scaleX = s
                        scaleY = s
                        alpha = anim.value.coerceIn(0f, 1f)
                    }
                    .then(
                        if (!canBlur) {
                            Modifier.shadow(
                                elevation = 24.dp,
                                shape = shape,
                                ambientColor = MaterialTheme.colorScheme.scrim,
                                spotColor = MaterialTheme.colorScheme.scrim,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .clip(shape)
                    .then(
                        if (canBlur) {
                            Modifier
                                .optimizedHazeEffect(
                                    state = hazeState,
                                    style = HazeMaterials.regular(glassSurface),
                                )
                                .background(
                                    Brush.verticalGradient(
                                        0f to Color.White.copy(alpha = if (dark) 0.08f else 0.18f),
                                        0.35f to Color.White.copy(alpha = 0f),
                                    ),
                                )
                                .border(
                                    1.dp,
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = if (dark) 0.22f else 0.50f),
                                            Color.White.copy(alpha = 0.04f),
                                        ),
                                    ),
                                    shape,
                                )
                        } else {
                            Modifier
                                .background(if (dark) Color(0xFF16161B) else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    shape,
                                )
                        },
                    )
                    .pointerInput(Unit) { detectTapGestures { /* consume clicks on card */ } }
                    .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 14.dp),
            ) {
                // Header & Drag Handle (supports swipe-down to dismiss)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .draggable(
                            orientation = Orientation.Vertical,
                            enabled = dismissible,
                            state = rememberDraggableState { delta ->
                                dragY = (dragY + delta).coerceAtLeast(0f)
                            },
                            onDragStarted = {
                                settling?.cancel()
                            },
                            onDragStopped = { velocity ->
                                if (dragY > dismissThresholdPx || velocity > 1400f) {
                                    close()
                                } else {
                                    settling = scope.launch {
                                        animate(
                                            initialValue = dragY,
                                            targetValue = 0f,
                                            initialVelocity = velocity,
                                            animationSpec = spring(dampingRatio = 0.76f, stiffness = 500f),
                                        ) { v, _ -> dragY = v }
                                    }
                                }
                            },
                        ),
                ) {
                    // Grab handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f)),
                        )
                    }

                    // Title and Close Button Row
                    if (title != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (icon != null) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (subtitle != null) {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            if (dismissible) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                        .clickable(
                                            onClickLabel = stringResource(R.string.close),
                                            role = Role.Button,
                                            onClick = close,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.close),
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.height(4.dp))
                    }

                    if (header != null) header()
                }

                // Scrollable/Flexible Content area
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .padding(contentPadding)
                        .graphicsLayer {
                            val rise = ((anim.value - 0.2f) / 0.8f).coerceIn(0f, 1f)
                            alpha = rise
                            translationY = (1f - rise) * 16.dp.toPx()
                        },
                    content = content,
                )

                // Fixed Actions Row (outside scrollable area)
                if (actions != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 4.dp, top = 18.dp),
                    ) {
                        actions()
                    }
                }
            }
        }
    }
}
