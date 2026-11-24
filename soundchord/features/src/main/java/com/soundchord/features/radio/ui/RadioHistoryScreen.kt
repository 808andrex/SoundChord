package com.soundchord.features.radio.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.soundchord.features.R
import com.soundchord.features.radio.data.RadioStation
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun RadioHistoryScreen(
    stations: List<RadioStation>,
    favoriteUuids: Set<String>,
    onPlay: (RadioStation) -> Unit,
    onFavorite: (RadioStation) -> Unit,
    showClearDialog: Boolean,
    onDismissClearDialog: () -> Unit,
    onConfirmClear: () -> Unit,
    dialogSurfaceModifier: Modifier,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val lightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val scrimColor = Color.Black.copy(alpha = if (lightTheme) 0.25f else 0.65f)
    val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    BackHandler(enabled = showClearDialog, onBack = onDismissClearDialog)
    var wasVisible by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose {
            if (wasVisible) onDismissClearDialog()
        }
    }
    LaunchedEffect(showClearDialog) { wasVisible = showClearDialog }
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (stations.isEmpty()) {
                item(key = "radio-history:empty") {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 56.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(Icons.Rounded.Radio, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.radio_history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else items(stations, key = { it.uuid }) { station ->
                RadioHistoryRow(station, station.uuid in favoriteUuids, { onPlay(station) }, { onFavorite(station) })
            }
            item(key = "radio-history:bottom-space") { Spacer(Modifier.height(12.dp)) }
        }
        if (showClearDialog) {
            RadioHistoryClearDialog(
                onDismiss = onDismissClearDialog,
                onConfirm = onConfirmClear,
                dialogSurfaceModifier = dialogSurfaceModifier,
            )
        }
    }
}

@Composable
private fun RadioHistoryClearDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    dialogSurfaceModifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    val pop = remember { Animatable(0.90f) }
    val fade = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { fade.animateTo(1f, tween(160, easing = FastOutSlowInEasing)) }
        launch { pop.animateTo(1f, spring(dampingRatio = 0.76f, stiffness = 420f)) }
    }

    val close: (after: (() -> Unit)?) -> Unit = { after ->
        if (!closing) {
            closing = true
            scope.launch {
                launch { fade.animateTo(0f, tween(140, easing = FastOutLinearInEasing)) }
                pop.animateTo(0.90f, tween(140, easing = FastOutSlowInEasing))
                after?.invoke()
                onDismiss()
            }
        }
    }

    BackHandler(enabled = !closing) { close(null) }

    val lightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val scrimColor = Color.Black.copy(alpha = if (lightTheme) 0.25f else 0.65f)
    val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val shape = RoundedCornerShape(14.dp)
    val surface = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f)
            .drawBehind {
                drawRect(scrimColor.copy(alpha = scrimColor.alpha * fade.value.coerceIn(0f, 1f)))
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { close(null) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(270.dp)
                .graphicsLayer {
                    val s = pop.value.coerceAtLeast(0f)
                    scaleX = s
                    scaleY = s
                    alpha = fade.value.coerceIn(0f, 1f)
                }
                .clip(shape)
                .background(surface)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 19.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.radio_history_clear_title),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.W600,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.radio_history_clear_message),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = dividerColor,
            )

            // Botón superior a ancho completo: Cancelar
            val cancelInteractionSource = remember { MutableInteractionSource() }
            val cancelPressed by cancelInteractionSource.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(
                        if (cancelPressed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f) else Color.Transparent,
                    )
                    .clickable(
                        indication = null,
                        interactionSource = cancelInteractionSource,
                        onClick = { close(null) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.radio_history_cancel),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = dividerColor,
            )

            // Botón inferior a ancho completo: Eliminar (rojo destructivo acento)
            val confirmInteractionSource = remember { MutableInteractionSource() }
            val confirmPressed by confirmInteractionSource.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(
                        if (confirmPressed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f) else Color.Transparent,
                    )
                    .clickable(
                        indication = null,
                        interactionSource = confirmInteractionSource,
                        onClick = {
                            close { onConfirm() }
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.radio_history_delete),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.W600,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun RadioHistoryRow(station: RadioStation, favorite: Boolean, onPlay: () -> Unit, onFavorite: () -> Unit) {
    var artworkFailed by remember(station.uuid) { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .82f))
            .clickable(onClick = onPlay).padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            if (!station.favicon.isNullOrBlank() && !artworkFailed) {
                AsyncImage(station.favicon, station.name, Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop, onError = { artworkFailed = true })
            } else Icon(Icons.Rounded.Radio, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)) {
            Text(station.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val metadata = listOfNotNull(
                station.country,
                station.codec?.uppercase(),
                station.bitrate.takeIf { it > 0 }?.let { stringResource(R.string.radio_bitrate_format, it) },
            ).joinToString(" • ")
            Text(metadata, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onFavorite, modifier = Modifier.size(44.dp)) {
        Icon(if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, stringResource(if (favorite) R.string.radio_remove_favorite else R.string.radio_add_favorite), tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
