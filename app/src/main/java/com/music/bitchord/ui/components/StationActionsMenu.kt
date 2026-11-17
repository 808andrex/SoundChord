package com.music.bitchord.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.soundchord.features.R as FeaturesR
import com.soundchord.features.radio.data.RadioStation
import dev.chrisbanes.haze.HazeState

/** Station selected by a long-press in Radio, with the source rectangle used by the glass popover. */
data class StationMenuTarget(
    val station: RadioStation,
    val isFavorite: Boolean,
    val anchor: Rect,
    val onToggleFavorite: () -> Unit,
)

/** Context actions for a live station, framed by the same Liquid Glass popover as song actions. */
@Composable
@OptIn(dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi::class)
fun StationActionsMenu(
    target: StationMenuTarget?,
    hazeState: HazeState,
    onDismiss: () -> Unit,
) {
    var infoStation by remember { mutableStateOf<RadioStation?>(null) }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Box(Modifier.fillMaxSize()) {
        IosMenuPopover(
            item = target,
            anchor = target?.anchor,
            onDismiss = onDismiss,
            hazeState = hazeState,
            blurBackground = true,
        ) { menuTarget ->
            val station = menuTarget.station
            var logoFailed by remember(station.uuid) { mutableStateOf(false) }
            val menuColors = iosMenuColors()
            Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Radio, contentDescription = null, modifier = Modifier.size(28.dp))
                        if (!station.favicon.isNullOrBlank() && !logoFailed) {
                            AsyncImage(
                                model = station.favicon,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                onError = { logoFailed = true },
                            )
                        }
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            station.name,
                            color = menuColors.label,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            stationSubtitle(station),
                            color = menuColors.secondaryLabel,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IosMenuSeparator()
                IosMenuQuickActions {
                    IosMenuQuickAction(
                        icon = if (menuTarget.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        label = stringResource(
                            if (menuTarget.isFavorite) FeaturesR.string.radio_remove_favorite else FeaturesR.string.radio_add_favorite,
                        ),
                        onClick = {
                            menuTarget.onToggleFavorite()
                            onDismiss()
                        },
                    )
                    IosMenuQuickAction(
                        icon = Icons.Rounded.Share,
                        label = stringResource(R.string.station_action_share),
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "${station.name}\n${station.streamUrl}")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, station.name))
                            onDismiss()
                        },
                    )
                }
                IosMenuSeparator()
                IosMenuRow(
                    label = stringResource(R.string.station_action_info),
                    icon = Icons.Rounded.Info,
                    onClick = { infoStation = station; onDismiss() },
                )
            }
        }

        infoStation?.let { station ->
            AlertScaffold(hazeState = hazeState, onDismiss = { infoStation = null }) {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 620.dp).verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.station_action_info), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    StationInfoLine(stringResource(R.string.station_action_name), station.name)
                    (station.country?.takeIf(String::isNotBlank) ?: station.countryCode?.takeIf(String::isNotBlank))?.let {
                        StationInfoLine(stringResource(R.string.station_action_country), it)
                    }
                    station.tags?.takeIf(String::isNotBlank)?.let {
                        StationInfoLine(stringResource(R.string.station_action_genres), it)
                    }
                    station.codec?.takeIf(String::isNotBlank)?.let {
                        StationInfoLine(stringResource(R.string.station_action_codec), it.uppercase())
                    }
                    station.bitrate.takeIf { it > 0 }?.let {
                        StationInfoLine(stringResource(R.string.station_action_bitrate), stringResource(FeaturesR.string.radio_bitrate_format, it))
                    }
                    StationInfoLine(stringResource(R.string.station_action_stream_url), station.streamUrl)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { clipboard.setText(AnnotatedString(station.streamUrl)) }) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.station_action_copy))
                        }
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { infoStation = null }) { Text(stringResource(R.string.close)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun stationSubtitle(station: RadioStation): String = listOfNotNull(
    station.country?.takeIf(String::isNotBlank) ?: station.countryCode?.takeIf(String::isNotBlank),
    station.codec?.takeIf(String::isNotBlank)?.uppercase(),
    station.bitrate.takeIf { it > 0 }?.let { stringResource(FeaturesR.string.radio_bitrate_format, it) },
).joinToString(" · ").ifBlank { stringResource(FeaturesR.string.radio_name) }

@Composable
private fun StationInfoLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
