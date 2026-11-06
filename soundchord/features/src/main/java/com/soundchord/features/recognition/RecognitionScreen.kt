package com.soundchord.features.recognition

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.soundchord.features.R
import com.soundchord.features.recognition.data.MusicRecognitionService
import com.soundchord.features.recognition.model.RecognitionFailureReason
import com.soundchord.features.recognition.model.RecognitionResult
import com.soundchord.features.recognition.model.RecognitionStatus

private val RecognitionRed = Color(0xFFFF3158)

@Composable
fun RecognitionScreen(
    onPlayResult: (RecognitionResult) -> Unit,
    modifier: Modifier = Modifier,
    recognitionViewModel: RecognitionViewModel = viewModel(),
) {
    DisposableEffect(recognitionViewModel) { onDispose { recognitionViewModel.cancel() } }
    val status by recognitionViewModel.status.collectAsStateWithLifecycle()
    var permissionDenied by remember { mutableStateOf(false) }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) recognitionViewModel.start()
    }
    val listening = status is RecognitionStatus.Listening
    val transition = rememberInfiniteTransition(label = "recognition-pulse")
    val pulse by transition.animateFloat(1f, 1.75f, infiniteRepeatable(tween(1100), RepeatMode.Restart), label = "pulse")
    val pulseAlpha by transition.animateFloat(.42f, 0f, infiniteRepeatable(tween(1100), RepeatMode.Restart), label = "pulse-alpha")

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Spacer(Modifier.height(48.dp))
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            when (val current = status) {
                is RecognitionStatus.Success -> RecognitionResultCard(
                    result = current.result,
                    onPlay = { onPlayResult(current.result) },
                    onRetry = {
                        recognitionViewModel.cancel()
                        requestPermission.launch(Manifest.permission.RECORD_AUDIO)
                    },
                )
                else -> {
                    val (headline, supporting) = when (current) {
                        RecognitionStatus.Ready -> "" to if (permissionDenied) stringResource(R.string.recognition_permission_needed) else stringResource(R.string.recognition_tap_to_identify)
                        RecognitionStatus.Listening -> stringResource(R.string.recognition_listening) to stringResource(R.string.recognition_listening_hint)
                        RecognitionStatus.Processing -> stringResource(R.string.recognition_processing) to stringResource(R.string.recognition_processing_hint)
                        is RecognitionStatus.NoMatch -> stringResource(R.string.recognition_no_match) to stringResource(R.string.recognition_clear_audio_hint)
                        is RecognitionStatus.Error -> when (current.reason) {
                            RecognitionFailureReason.MICROPHONE_PERMISSION -> stringResource(R.string.recognition_permission_error) to stringResource(R.string.recognition_permission_error_hint)
                            RecognitionFailureReason.MICROPHONE_CAPTURE -> stringResource(R.string.recognition_capture_error) to stringResource(R.string.recognition_capture_error_hint)
                            RecognitionFailureReason.NETWORK -> stringResource(R.string.recognition_network_error) to stringResource(R.string.recognition_network_error_hint)
                            RecognitionFailureReason.RECOGNITION -> stringResource(R.string.recognition_failed) to stringResource(R.string.recognition_clear_audio_hint)
                        }
                        is RecognitionStatus.Success -> "" to ""
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        if (current !is RecognitionStatus.Ready) {
                            Text(headline, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(22.dp))
                        }
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
                            if (listening) Box(Modifier.size(150.dp).scale(pulse).alpha(pulseAlpha).background(RecognitionRed, CircleShape))
                            Box(
                                Modifier.size(150.dp).background(RecognitionRed, CircleShape)
                                    .clickable(enabled = !listening && current !is RecognitionStatus.Processing) {
                                        if (current is RecognitionStatus.NoMatch || current is RecognitionStatus.Error) recognitionViewModel.cancel()
                                        requestPermission.launch(Manifest.permission.RECORD_AUDIO)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(if (current is RecognitionStatus.Processing) Icons.Rounded.MusicNote else Icons.Rounded.Mic, null, tint = Color.White, modifier = Modifier.size(58.dp))
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(supporting, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        if (listening) {
                            Spacer(Modifier.height(26.dp))
                            OutlinedButton(onClick = recognitionViewModel::cancel) { Text(stringResource(R.string.recognition_cancel)) }
                        }
                        if (current is RecognitionStatus.NoMatch || current is RecognitionStatus.Error) {
                            Spacer(Modifier.height(22.dp))
                            Button(onClick = { recognitionViewModel.cancel(); requestPermission.launch(Manifest.permission.RECORD_AUDIO) }) {
                                Text(stringResource(R.string.recognition_listen_again))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecognitionResultCard(result: RecognitionResult, onPlay: () -> Unit, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 20.dp),
    ) {
        AsyncImage(
            model = result.coverArtHqUrl ?: result.coverArtUrl,
            contentDescription = stringResource(R.string.recognition_album_art_description, result.title),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(220.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface),
        )
        Spacer(Modifier.height(18.dp))
        Text(result.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(result.artist, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        result.album?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.height(18.dp))
        Button(onClick = onPlay, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = RecognitionRed)) {
            Icon(Icons.Rounded.PlayArrow, null)
            Text(stringResource(R.string.recognition_play_soundchord), modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Refresh, null)
            Text(stringResource(R.string.recognition_listen_again), modifier = Modifier.padding(start = 8.dp))
        }
    }
}
