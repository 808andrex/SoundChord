package com.soundchord.features.recognition.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.soundchord.features.recognition.audio.AudioResampler
import com.soundchord.features.recognition.audio.DecodedAudio
import com.soundchord.features.recognition.audio.VibraSignature
import com.soundchord.features.recognition.model.RecognitionFailureReason
import com.soundchord.features.recognition.model.RecognitionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.ByteOrder

object MusicRecognitionService {
    private const val SAMPLE_RATE = 44_100
    private const val CAPTURE_MS = 6_000L
    private const val EXTENSION_MS = 4_000L
    private val _status = MutableStateFlow<RecognitionStatus>(RecognitionStatus.Ready)
    val status = _status.asStateFlow()

    fun hasPermission(context: Context): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    suspend fun recognize(context: Context): RecognitionStatus = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) {
            return@withContext RecognitionStatus.Error(
                "Microphone permission not granted", RecognitionFailureReason.MICROPHONE_PERMISSION,
            ).also { _status.value = it }
        }
        try {
            _status.value = RecognitionStatus.Listening
            val firstSample = recordAudio(CAPTURE_MS)
            currentCoroutineContext().ensureActive()
            _status.value = RecognitionStatus.Processing
            var result = recognizeAudio(firstSample)
            if (result.isFailure && result.exceptionOrNull()?.message?.contains("no match", true) == true) {
                _status.value = RecognitionStatus.Listening
                val extension = recordAudio(EXTENSION_MS)
                currentCoroutineContext().ensureActive()
                _status.value = RecognitionStatus.Processing
                result = recognizeAudio(firstSample + extension)
            }
            _status.value = result.fold(
                onSuccess = { RecognitionStatus.Success(it) },
                onFailure = { error ->
                    val message = error.message ?: "Recognition failed"
                    when {
                        message.contains("no match", true) -> RecognitionStatus.NoMatch()
                        error.isNetworkFailure() -> RecognitionStatus.Error(message, RecognitionFailureReason.NETWORK)
                        else -> RecognitionStatus.Error(message, RecognitionFailureReason.RECOGNITION)
                    }
                },
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val reason = when {
                error is SecurityException -> RecognitionFailureReason.MICROPHONE_PERMISSION
                error.isNetworkFailure() -> RecognitionFailureReason.NETWORK
                _status.value == RecognitionStatus.Listening -> RecognitionFailureReason.MICROPHONE_CAPTURE
                else -> RecognitionFailureReason.RECOGNITION
            }
            _status.value = RecognitionStatus.Error(error.message ?: "Recognition failed", reason)
        }
        _status.value
    }

    @SuppressLint("MissingPermission")
    private suspend fun recordAudio(durationMs: Long): ByteArray = withContext(Dispatchers.IO) {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
        )
        check(minBuffer > 0) { "Invalid audio buffer size" }
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, minBuffer,
        )
        check(recorder.state == AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            "Microphone could not be initialized"
        }
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(minBuffer)
        val start = android.os.SystemClock.elapsedRealtime()
        try {
            recorder.startRecording()
            while (android.os.SystemClock.elapsedRealtime() - start < durationMs && currentCoroutineContext().isActive) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) output.write(buffer, 0, read)
                else if (read < 0) error("Microphone recording failed ($read)")
            }
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
        }
        currentCoroutineContext().ensureActive()
        output.toByteArray().also { require(it.isNotEmpty()) { "No audio was recorded" } }
    }

    private suspend fun recognizeAudio(pcm: ByteArray) = try {
        val resampled = AudioResampler.resample(
            DecodedAudio(pcm, 1, SAMPLE_RATE, AudioFormat.ENCODING_PCM_16BIT),
            VibraSignature.REQUIRED_SAMPLE_RATE,
        ).getOrThrow()
        require(
            resampled.channelCount == 1 && resampled.sampleRate == VibraSignature.REQUIRED_SAMPLE_RATE &&
                resampled.pcmEncoding == AudioFormat.ENCODING_PCM_16BIT &&
                ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN && resampled.data.isNotEmpty() &&
                resampled.data.size % 2 == 0
        ) { "Invalid audio format for fingerprint generation" }
        val durationMs = (resampled.data.size / 2) * 1000L / VibraSignature.REQUIRED_SAMPLE_RATE
        ShazamApi.recognize(VibraSignature.fromI16(resampled.data), durationMs)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    fun reset() { _status.value = RecognitionStatus.Ready }

    private fun Throwable.isNetworkFailure(): Boolean = generateSequence(this) { it.cause }.any {
        it is IOException || it is SocketTimeoutException || it is UnknownHostException || it is ConnectException
    }
}
