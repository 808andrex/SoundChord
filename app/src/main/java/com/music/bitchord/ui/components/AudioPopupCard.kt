package com.music.bitchord.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.utils.containSheetGestures
import com.music.bitchord.ui.player.LocalPlayerFrost
import com.music.bitchord.ui.player.PlayerBackHandler
import dev.chrisbanes.haze.HazeState

private val POPUP_CARD_SHAPE = RoundedCornerShape(ALERT_CORNER)
private val POPUP_SCRIM_COLOR = Color.Black.copy(alpha = 0.4f)
private val POPUP_WIDTH = 320.dp
private val POPUP_CONTENT_MAX_HEIGHT = 420.dp
private val POPUP_OPEN_SPRING = spring<Float>(dampingRatio = 1f, stiffness = 400f, visibilityThreshold = 0.001f)
private val POPUP_CLOSE_SPRING = spring<Float>(dampingRatio = 1f, stiffness = 520f, visibilityThreshold = 0.001f)
internal val LocalAudioPopupDismiss = staticCompositionLocalOf<() -> Unit> { {} }

/**
 * The frosted card the output drawer's popups open into — the one
 * [AudioPipelineDialog] was drawn in, lifted out so the Cast picker is the same
 * object rather than a second one made to look like it.
 *
 * A scrim that dismisses, a card that swallows the tap so touching it does not,
 * a centred title and subtitle, a scrolling body, and a full-width closing
 * action. Fixed to the player's own dark palette rather than the theme-adaptive
 * one the settings dialogs use, since everything on the player is white alphas
 * whatever the app's light/dark theme is.
 *
 * [content] is the scrolling body, handed a [BoxScope] so a body can lay
 * something behind its rows — the pipeline draws its signal line there.
 */
@Composable
internal fun AudioPopupCard(
    hazeState: HazeState,
    title: String,
    subtitle: String,
    doneLabel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val frost = LocalPlayerFrost.current
    val visibility = remember { MutableTransitionState(false) }
    var hasOpened by remember { mutableStateOf(false) }
    val close = { visibility.targetState = false }
    LaunchedEffect(Unit) { visibility.targetState = true }
    LaunchedEffect(visibility.currentState) { if (visibility.currentState) hasOpened = true }
    PlayerBackHandler(enabled = visibility.targetState, onBack = close)
    LaunchedEffect(visibility.isIdle, visibility.currentState) {
        if (hasOpened && visibility.isIdle && !visibility.currentState) onDismiss()
    }
    val scrimAlpha by animateFloatAsState(
        targetValue = if (visibility.targetState) POPUP_SCRIM_COLOR.alpha else 0f,
        animationSpec = POPUP_OPEN_SPRING,
        label = "audioPopupScrim",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .containSheetGestures()
            .background(Color.Black.copy(alpha = scrimAlpha))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = close,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visibleState = visibility,
            enter = scaleIn(
                animationSpec = POPUP_OPEN_SPRING,
                transformOrigin = TransformOrigin(0.5f, 1f),
            ) + fadeIn(POPUP_OPEN_SPRING),
            exit = scaleOut(
                animationSpec = POPUP_CLOSE_SPRING,
                transformOrigin = TransformOrigin(0.5f, 1f),
            ) + fadeOut(POPUP_CLOSE_SPRING),
        ) {
            CompositionLocalProvider(LocalAudioPopupDismiss provides close) {
                Column(
                    modifier = Modifier
                        .width(POPUP_WIDTH)
                        .clip(POPUP_CARD_SHAPE)
                        .then(
                            when {
                                reduceDynamicBlur -> Modifier.background(MenuFrostDark)
                                frost != null -> with(frost) {
                                    Modifier.frostPanel(POPUP_CARD_SHAPE, MenuFrostDark.copy(alpha = MENU_FROST_ALPHA))
                                }
                                else -> Modifier.liveMenuFrost(hazeState, MenuFrostDark)
                            }
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {},
                        ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 19.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.W600,
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = subtitle,
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                            ),
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .heightIn(max = POPUP_CONTENT_MAX_HEIGHT)
                            .verticalScroll(rememberScrollState()),
                        content = content,
                    )

                    PipelineRule(Modifier)
                    PipelineDoneAction(label = doneLabel, onClick = close)
                }
            }
        }
    }
}
