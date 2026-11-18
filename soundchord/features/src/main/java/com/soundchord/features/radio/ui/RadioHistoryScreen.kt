package com.soundchord.features.radio.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.soundchord.features.R
import com.soundchord.features.radio.data.RadioStation
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi

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
    val deleteColor = Color(0xFFFF453A)
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
        AnimatedVisibility(
            visible = showClearDialog,
            modifier = Modifier.fillMaxSize().zIndex(1f),
            enter = fadeIn(tween(160)) + scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = 600f,
                ),
                initialScale = 0.9f,
            ),
            exit = fadeOut(tween(120)) + scaleOut(
                animationSpec = tween(140),
                targetScale = 0.94f,
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().zIndex(1f),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onDismissClearDialog,
                        ),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 300.dp)
                        .padding(horizontal = 28.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1C1C1E).copy(alpha = 0.92f))
                        .then(dialogSurfaceModifier)
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), RoundedCornerShape(24.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {},
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.radio_history_clear_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(R.string.radio_history_clear_message),
                            modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.72f),
                            textAlign = TextAlign.Center,
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        TextButton(
                            onClick = onDismissClearDialog,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                        ) {
                            Text(
                                stringResource(R.string.radio_history_cancel),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        TextButton(
                            onClick = onConfirmClear,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                        ) {
                            Text(
                                stringResource(R.string.radio_history_delete),
                                color = deleteColor,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                    }
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
            Text(station.name, color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val metadata = listOfNotNull(
                station.country,
                station.codec?.uppercase(),
                station.bitrate.takeIf { it > 0 }?.let { stringResource(R.string.radio_bitrate_format, it) },
            ).joinToString(" • ")
            Text(metadata, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onFavorite, modifier = Modifier.size(44.dp)) {
            Icon(if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, stringResource(if (favorite) R.string.radio_remove_favorite else R.string.radio_add_favorite), tint = if (favorite) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
