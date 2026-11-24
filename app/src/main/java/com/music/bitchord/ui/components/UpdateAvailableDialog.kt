package com.music.bitchord.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.AppUpdateChecker
import dev.chrisbanes.haze.HazeState

/** UIAlertController's own metrics: 14pt corner, 44pt rows. Kept for shared alert compatibility. */
internal val ALERT_WIDTH = 270.dp
internal val ALERT_CORNER = 14.dp
internal val ACTION_HEIGHT = 44.dp

/** Default scrim shared by the other frosted alerts. */
internal val SCRIM_COLOR = Color.Black.copy(alpha = 0.65f)

/**
 * Redesigned software update dialog for SoundChord.
 *
 * Utilizes the unified [FloatingCard] container, the branded [ReleaseHero]
 * component, and structured release note parsing for Highlights, Visual Improvements,
 * and Bug Fixes, while maintaining full direct download/install capabilities.
 */
@Composable
fun UpdateAvailableDialog(
    version: String,
    notes: String?,
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstall: () -> Unit,
    onOpenReleasePage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by AppUpdateChecker.download.collectAsStateWithLifecycle()

    // Clean up version string (e.g. "bitchord 1.9.1" -> "1.9.1", "BitChord-v1.3.0" -> "1.3.0", "v1.3.0" -> "1.3.0")
    val cleanVersion = remember(version) {
        val trimmed = version.trim()
        val afterApp = if (trimmed.contains("bitchord", ignoreCase = true)) {
            trimmed.substringAfterLast("bitchord", trimmed).substringAfterLast("BitChord", trimmed).trim()
        } else {
            trimmed
        }
        afterApp.removePrefix("-v").removePrefix("-").removePrefix("v").trim()
    }

    val data = remember(notes, cleanVersion) {
        WhatsNewCatalog.parseReleaseNotes(notes, cleanVersion, context)
    }

    FloatingCard(
        onDismiss = onDismiss,
        title = stringResource(R.string.update_dialog_title),
        hazeState = hazeState,
        modifier = modifier,
        actions = {
            val card = LocalFloatingCard.current
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Primary Action Button (full width, accent color)
                Button(
                    onClick = {
                        when (state) {
                            is AppUpdateChecker.DownloadState.Downloading -> onCancelDownload()
                            is AppUpdateChecker.DownloadState.Ready -> onInstall()
                            is AppUpdateChecker.DownloadState.Failed -> onDownload()
                            else -> onDownload()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = CircleShape,
                ) {
                    Text(
                        text = when (state) {
                            is AppUpdateChecker.DownloadState.Downloading -> stringResource(R.string.cancel)
                            is AppUpdateChecker.DownloadState.Ready -> stringResource(R.string.install_now)
                            is AppUpdateChecker.DownloadState.Failed -> stringResource(R.string.try_again)
                            else -> stringResource(R.string.download_now)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Secondary Action Button (Remind me later / Later / Open releases page)
                if (state !is AppUpdateChecker.DownloadState.Downloading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (state is AppUpdateChecker.DownloadState.Failed) {
                                    onOpenReleasePage()
                                } else {
                                    card?.close() ?: onDismiss()
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = when (state) {
                                is AppUpdateChecker.DownloadState.Ready -> stringResource(R.string.later)
                                is AppUpdateChecker.DownloadState.Failed -> stringResource(R.string.open_releases_page)
                                else -> stringResource(R.string.remind_me_later)
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Reused Top Hero Card
            ReleaseHero(
                version = cleanVersion,
                status = stringResource(R.string.update_hero_status),
                versionLabel = stringResource(R.string.update_hero_version_label),
            )

            // Download progress & status banner
            val downloading = state as? AppUpdateChecker.DownloadState.Downloading
            if (downloading != null || state is AppUpdateChecker.DownloadState.Failed) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (downloading != null) {
                                stringResource(R.string.update_downloading_body, cleanVersion)
                            } else {
                                stringResource(R.string.update_failed_body, cleanVersion)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (state is AppUpdateChecker.DownloadState.Failed) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        if (downloading != null && downloading.fraction > 0f) {
                            Text(
                                text = "${(downloading.fraction * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                    ) {
                        if (downloading != null && downloading.fraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(downloading.fraction.coerceIn(0f, 1f))
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                    }

                    if (state is AppUpdateChecker.DownloadState.Failed) {
                        Text(
                            text = (state as AppUpdateChecker.DownloadState.Failed).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // Download ready notice
            if (state is AppUpdateChecker.DownloadState.Ready) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = stringResource(R.string.update_ready_body, cleanVersion),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Highlights Section
            Text(
                text = stringResource(R.string.whats_new_highlights_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = 4.dp, start = 4.dp)
                    .semantics { heading() },
            )

            data.highlights.forEachIndexed { index, note ->
                HighlightCard(note = note, index = index)
            }

            // Visual Improvements Accordion
            if (data.visualImprovements.isNotEmpty()) {
                CategoryAccordion(
                    title = stringResource(R.string.whats_new_visual_improvements_title),
                    countLabel = stringResource(
                        R.string.whats_new_visual_improvements_count,
                        data.visualImprovements.size,
                    ),
                    items = data.visualImprovements,
                )
            }

            // Bug Fixes Accordion
            if (data.fixes.isNotEmpty()) {
                CategoryAccordion(
                    title = stringResource(R.string.whats_new_fixes_title),
                    countLabel = stringResource(
                        R.string.whats_new_fixes_count,
                        data.fixes.size,
                    ),
                    items = data.fixes,
                )
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

/** Full-bleed action row for UIAlertController style dialogs. Kept for shared alert compatibility. */
@Composable
internal fun AlertAction(
    label: String,
    emphasised: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ACTION_HEIGHT)
            .background(
                if (pressed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f) else Color.Transparent,
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
                fontWeight = if (emphasised) FontWeight.W600 else FontWeight.W400,
            ),
            color = (if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                .copy(alpha = if (enabled) 1f else 0.4f),
        )
    }
}

/** Hairline separator. Kept for shared alert compatibility. */
@Composable
internal fun AlertRule(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)),
    )
}
