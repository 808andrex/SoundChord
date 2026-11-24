package com.soundchord.features.playlistimport.ui

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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.PlaylistPrivacy
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.components.PillTextField
import com.soundchord.features.R
import com.soundchord.features.playlistimport.data.AppleMusicPlaylist
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch

private val ALERT_CORNER = 14.dp
private val ALERT_WIDTH = 290.dp
private val SCRIM_COLOR = Color.Black.copy(alpha = 0.5f)

private val IMPORT_LANGUAGES = listOf(
    "Tamil", "Telugu", "Kannada", "Malayalam", "Hindi",
    "English", "Multilingual", "No Preference", "Manual / Other",
)

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun AppleMusicImportAlert(
    hazeState: HazeState,
    signedIn: Boolean,
    onImported: (playlist: AppleMusicPlaylist, privacy: PlaylistPrivacy, songs: List<Song>) -> Unit,
    onDismiss: () -> Unit,
    viewModel: AppleMusicImportViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }

    val pop = remember { Animatable(0.90f) }
    val fade = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        viewModel.reset()
        launch { fade.animateTo(1f, tween(160, easing = FastOutSlowInEasing)) }
        launch { pop.animateTo(1f, spring(dampingRatio = 0.76f, stiffness = 420f)) }
    }

    val close: () -> Unit = {
        if (!closing) {
            closing = true
            scope.launch {
                launch { fade.animateTo(0f, tween(160, easing = FastOutLinearInEasing)) }
                pop.animateTo(0.90f, tween(160, easing = FastOutSlowInEasing))
                viewModel.reset()
                onDismiss()
            }
        }
    }

    val isWorking = state.step is AppleMusicImportStep.LoadingMetadata || state.step is AppleMusicImportStep.Matching

    BackHandler(enabled = !closing && !isWorking, onBack = close)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(SCRIM_COLOR.copy(alpha = SCRIM_COLOR.alpha * fade.value.coerceIn(0f, 1f)))
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { if (!isWorking) close() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(ALERT_WIDTH)
                .graphicsLayer {
                    val s = pop.value.coerceAtLeast(0f)
                    scaleX = s
                    scaleY = s
                    alpha = fade.value.coerceIn(0f, 1f)
                }
                .clip(RoundedCornerShape(ALERT_CORNER))
                .hazeEffect(
                    state = hazeState,
                    style = HazeMaterials.regular(MaterialTheme.colorScheme.surface),
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
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 19.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.apple_music_import_title),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.W600),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(4.dp))

                when (val step = state.step) {
                    AppleMusicImportStep.Input -> {
                        Text(
                            text = if (state.errorMessage != null) {
                                stringResource(R.string.apple_music_import_invalid)
                            } else {
                                stringResource(
                                    if (signedIn) R.string.apple_music_import_description
                                    else R.string.apple_music_import_description_local,
                                )
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp),
                            color = if (state.errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(14.dp))

                        PillTextField(
                            value = state.link,
                            onValueChange = viewModel::onLinkChanged,
                            placeholder = stringResource(R.string.apple_music_import_hint),
                            enabled = !isWorking,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (state.link.isNotBlank() && !isWorking) viewModel.loadPreview()
                            }),
                        )
                    }

                    AppleMusicImportStep.LoadingMetadata -> {
                        Text(
                            text = stringResource(R.string.apple_music_import_fetching),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    is AppleMusicImportStep.Preview -> {
                        val playlist = step.playlist
                        PlaylistPreviewCard(playlist = playlist)

                        Spacer(Modifier.height(12.dp))

                        if (state.errorMessage != null) {
                            Text(
                                text = stringResource(R.string.apple_music_import_none),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        // Language preference selection
                        Text(
                            text = stringResource(R.string.apple_music_import_language_preference),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            IMPORT_LANGUAGES.forEach { lang ->
                                val selected = lang == state.selectedLanguage
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                        )
                                        .clickable { viewModel.onLanguageChanged(lang) }
                                        .padding(horizontal = 9.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = lang,
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        if (state.selectedLanguage == "Manual / Other") {
                            Spacer(Modifier.height(8.dp))
                            PillTextField(
                                value = state.customLanguageInput,
                                onValueChange = viewModel::onCustomLanguageChanged,
                                placeholder = stringResource(R.string.apple_music_import_language_hint),
                                enabled = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                            )
                        }

                        // Privacy selector if signed in
                        if (signedIn) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.who_can_see_it),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                PlaylistPrivacy.entries.forEach { option ->
                                    val selected = option == state.privacy
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(percent = 50))
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant,
                                            )
                                            .clickable { viewModel.onPrivacyChanged(option) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = option.label,
                                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is AppleMusicImportStep.Matching -> {
                        val progress = if (step.total > 0) step.completed.toFloat() / step.total.toFloat() else 0f
                        Text(
                            text = stringResource(R.string.apple_music_import_matching, step.completed, step.total),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    is AppleMusicImportStep.Completed -> {
                        Text(
                            text = stringResource(R.string.apple_music_import_unmatched, step.unmatchedTracks.size),
                            modifier = Modifier.padding(bottom = 10.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            step.unmatchedTracks.forEach { track ->
                                Text(
                                    text = if (track.artist.isBlank()) track.title else "${track.title} · ${track.artist}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Hairline separator
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
            )

            // Action buttons
            when (val step = state.step) {
                AppleMusicImportStep.Input -> {
                    AlertButton(
                        label = stringResource(R.string.apple_music_import_preview_button),
                        emphasised = true,
                        enabled = state.link.isNotBlank() && !isWorking,
                        onClick = viewModel::loadPreview,
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f))
                    AlertButton(
                        label = stringResource(R.string.cancel),
                        emphasised = false,
                        enabled = !isWorking,
                        onClick = close,
                    )
                }

                AppleMusicImportStep.LoadingMetadata -> {
                    AlertButton(
                        label = stringResource(R.string.cancel),
                        emphasised = false,
                        onClick = viewModel::resetToInput,
                    )
                }

                is AppleMusicImportStep.Preview -> {
                    AlertButton(
                        label = stringResource(R.string.apple_music_import_button),
                        emphasised = true,
                        onClick = {
                            viewModel.startMatching { playlist, privacy, songs ->
                                onImported(playlist, privacy, songs)
                                close()
                            }
                        },
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f))
                    AlertButton(
                        label = stringResource(R.string.cancel),
                        emphasised = false,
                        onClick = viewModel::resetToInput,
                    )
                }

                is AppleMusicImportStep.Matching -> {
                    // Running, no actions to prevent corruption
                }

                is AppleMusicImportStep.Completed -> {
                    AlertButton(
                        label = stringResource(R.string.apple_music_import_save),
                        emphasised = true,
                        onClick = {
                            viewModel.confirmSave { playlist, privacy, songs ->
                                onImported(playlist, privacy, songs)
                                close()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistPreviewCard(playlist: AppleMusicPlaylist) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            if (!playlist.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = playlist.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (playlist.curator.isNotBlank()) {
                Text(
                    text = playlist.curator,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stringResource(R.string.apple_music_import_track_count, playlist.tracks.size),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!playlist.motionVideoUrl.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.apple_music_import_motion_badge),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasised: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                if (pressed && enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f) else Color.Transparent,
            )
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 17.sp,
                fontWeight = if (emphasised) FontWeight.W600 else FontWeight.Normal,
            ),
            color = (if (emphasised) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                .copy(alpha = if (enabled) 1f else 0.4f),
        )
    }
}
