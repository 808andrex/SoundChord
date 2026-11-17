package com.soundchord.features.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.music.bitchord.ui.utils.containSheetGestures
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.soundchord.features.R
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.components.liveMenuFrost
import com.music.bitchord.ui.player.*
import dev.chrisbanes.haze.HazeState

private val OUTPUT_OPEN_SPRING = spring<Float>(dampingRatio = 1f, stiffness = 400f, visibilityThreshold = 0.001f)
private val OUTPUT_CLOSE_SPRING = spring<Float>(dampingRatio = 1f, stiffness = 520f, visibilityThreshold = 0.001f)

/** Floating player output picker. The caller mounts this only while it is open. */
@Composable
fun SoundChordAudioOutputSheet(
    hazeState: HazeState,
    song: Song,
    artworkUrl: String?,
    accountName: String?,
    nestedOverlayOpen: Boolean,
    onDismiss: () -> Unit,
    onOpenPipeline: () -> Unit,
    onOpenCast: () -> Unit,
) {
    val host = PlayerPlatform.host
    val outputs = host.rememberAudioOutputs()
    val cast by host.castState.collectAsStateWithLifecycle()
    val watch by host.watchOutput.collectAsStateWithLifecycle()
    val format by host.outputFormat.collectAsStateWithLifecycle()
    val volume by host.volume.level.collectAsStateWithLifecycle()
    val visibility = remember { MutableTransitionState(false) }
    var hasOpened by remember { mutableStateOf(false) }
    val close = { visibility.targetState = false }
    val cardShape = RoundedCornerShape(32.dp)
    val frost = LocalPlayerFrost.current
    LaunchedEffect(Unit) { host.refreshWatchOutput() }
    LaunchedEffect(Unit) { visibility.targetState = true }
    LaunchedEffect(visibility.currentState) { if (visibility.currentState) hasOpened = true }
    PlayerBackHandler(enabled = visibility.targetState && !nestedOverlayOpen, onBack = close)
    LaunchedEffect(visibility.isIdle, visibility.currentState) {
        if (hasOpened && visibility.isIdle && !visibility.currentState) onDismiss()
    }
    val scrimAlpha by animateFloatAsState(
        targetValue = if (visibility.targetState) .24f else 0f,
        animationSpec = OUTPUT_OPEN_SPRING,
        label = "audioOutputScrim",
    )

    Box(Modifier.fillMaxSize().containSheetGestures().statusBarsPadding().navigationBarsPadding(), contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha))
            .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, onClick = close))
        AnimatedVisibility(
            visibleState = visibility,
            enter = scaleIn(animationSpec = OUTPUT_OPEN_SPRING, transformOrigin = TransformOrigin(.5f, 1f)) + fadeIn(OUTPUT_OPEN_SPRING),
            exit = scaleOut(animationSpec = OUTPUT_CLOSE_SPRING, transformOrigin = TransformOrigin(.5f, 1f)) + fadeOut(OUTPUT_CLOSE_SPRING),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.widthIn(max = 420.dp).fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 112.dp)
                    .heightIn(max = 560.dp)
                    .clip(cardShape)
                    .then(
                        if (frost != null) with(frost) {
                            Modifier.frostPanel(cardShape, Color(0x991E1824))
                        } else Modifier.liveMenuFrost(hazeState, Color(0xCC1E1824))
                    )
                    .border(.5.dp, Color.White.copy(alpha = .12f), cardShape)
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        onClick = {},
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.audio_output_title), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(artworkUrl, null, Modifier.size(48.dp).clip(RoundedCornerShape(9.dp)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, color = Color.White.copy(alpha = .58f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                val active = outputs.firstOrNull { it.isActive }
                if (active != null) {
                    ActiveVolumePill(
                        icon = iconForOutput(active),
                        title = outputName(active, accountName),
                        level = volume,
                        onVolumeChange = host.volume::set,
                    )
                }
                outputs.filterNot { it.isActive }.forEach { device ->
                    OutputDeviceRow(outputName(device, accountName), iconForOutput(device), onClick = { host.selectAudioOutput(device.id) })
                }
                if (watch.available || watch.active) OutputDeviceRow(watch.name ?: stringResource(R.string.output_watch), Icons.Rounded.Watch, onClick = host::selectWatchOutput)
                if (cast.supported) OutputDeviceRow(cast.connectedName ?: stringResource(R.string.output_cast), if (cast.connectedName == null) Icons.Rounded.Cast else Icons.Rounded.CastConnected, subtitle = stringResource(R.string.output_cast_subtitle), onClick = onOpenCast)
                Spacer(Modifier.height(2.dp))
                Box(Modifier.fillMaxWidth().height(.5.dp).background(Color.White.copy(alpha = .12f)))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable(onClick = onOpenPipeline).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.GraphicEq, null, Modifier.size(24.dp), tint = Color.White.copy(alpha = .65f)); Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) { Text(stringResource(R.string.output_audio_pipeline), color = Color.White.copy(alpha = .88f), fontSize = 14.sp); Text(format.summary, color = Color.White.copy(alpha = .55f), fontSize = 12.sp) }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.White.copy(alpha = .45f))
                }
            }
        }
    }
}

@Composable
private fun ActiveVolumePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    level: Float,
    onVolumeChange: (Float) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var dragLevel by remember { mutableFloatStateOf(level.coerceIn(0f, 1f)) }
    LaunchedEffect(level) { if (!dragging) dragLevel = level.coerceIn(0f, 1f) }

    Box(
        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(50))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { point ->
                        dragging = true
                        dragLevel = (point.x / size.width).coerceIn(0f, 1f)
                        onVolumeChange(dragLevel)
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                ) { change, _ ->
                    change.consume()
                    dragLevel = (change.position.x / size.width).coerceIn(0f, 1f)
                    onVolumeChange(dragLevel)
                }
            },
    ) {
        Box(Modifier.matchParentSize().background(Color.White.copy(alpha = .24f))) {
            VolumePillContent(icon, title, dragLevel, Color.White)
        }
        Box(
            Modifier.matchParentSize()
                .drawWithContent { clipRect(right = size.width * dragLevel) { this@drawWithContent.drawContent() } }
                .background(Color.White),
        ) {
            VolumePillContent(icon, title, dragLevel, Color(0xFF1C1C1E))
        }
    }
}

@Composable
private fun VolumePillContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    level: Float,
    tint: Color,
) {
    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(22.dp), tint = tint)
        Spacer(Modifier.width(12.dp))
        Text(title, Modifier.weight(1f), color = tint, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        VolumeGlyph(level, tint, Modifier.size(24.dp))
    }
}

@Composable
private fun VolumeGlyph(level: Float, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val unit = size.minDimension / 24f
        val speaker = Path().apply {
            moveTo(3f * unit, 9f * unit); lineTo(7f * unit, 9f * unit)
            lineTo(12f * unit, 5f * unit); lineTo(12f * unit, 19f * unit)
            lineTo(7f * unit, 15f * unit); lineTo(3f * unit, 15f * unit); close()
        }
        drawPath(speaker, tint)
        repeat(3) { index ->
            val visibility = (level * 3f - index).coerceIn(0f, 1f)
            if (visibility > 0f) {
                val radius = (5f + index * 3f) * unit
                drawArc(
                    color = tint.copy(alpha = visibility),
                    startAngle = -40f * visibility,
                    sweepAngle = 80f * visibility,
                    useCenter = false,
                    topLeft = Offset(11f * unit - radius, 12f * unit - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 1.9f * unit, cap = StrokeCap.Round),
                )
            }
        }
    }
}

@Composable
private fun outputName(device: AudioOutputDevice, accountName: String?): String {
    val firstName = accountName?.trim()?.split(Regex("\\s+"))?.firstOrNull()?.takeIf(String::isNotBlank)
    return when {
        device.name.isNotBlank() -> device.name
        device.kind == AudioOutputKind.WIRED -> stringResource(R.string.output_wired_headphones)
        device.kind == AudioOutputKind.USB -> stringResource(R.string.output_usb_audio)
        device.kind == AudioOutputKind.HDMI -> stringResource(R.string.output_hdmi)
        firstName != null -> stringResource(R.string.output_personal_phone, firstName)
        else -> stringResource(R.string.output_this_phone)
    }
}

@Composable
private fun OutputDeviceRow(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, subtitle: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(24.dp), tint = Color.White.copy(alpha = .72f)); Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); if (subtitle != null) Text(subtitle, color = Color.White.copy(alpha = .55f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        if (subtitle != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.White.copy(alpha = .45f))
    }
}

private fun iconForOutput(device: AudioOutputDevice) = when (device.kind) {
    AudioOutputKind.PHONE -> Icons.Rounded.PhoneAndroid
    AudioOutputKind.WIRED, AudioOutputKind.USB -> Icons.Rounded.Headphones
    AudioOutputKind.BLUETOOTH -> if (device.form == BluetoothForm.SPEAKER) Icons.Rounded.Speaker else Icons.Rounded.Bluetooth
    AudioOutputKind.HDMI -> Icons.Rounded.Tv
    AudioOutputKind.OTHER -> Icons.Rounded.Speaker
}
