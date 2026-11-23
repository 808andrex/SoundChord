package com.music.bitchord.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.music.bitchord.data.spotify.LocalPlaylistStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream
import java.io.File

@Serializable
data class PlaylistCoverBackup(
    val playlistId: String,
    val imageBase64: String,
    val mimeType: String = "image/jpeg",
)

/** Local custom playlist artwork. YouTube Music does not accept custom covers from this client. */
object PlaylistCovers {
    private const val DIR = "playlist_covers"
    private const val SIZE = 800
    private val _covers = MutableStateFlow<Map<String, Uri>>(emptyMap())

    /** Covers keyed by the canonical playlist id (without the YouTube `VL` prefix). */
    val covers: StateFlow<Map<String, Uri>> = _covers.asStateFlow()

    /** Scans existing covers once per app start so consumers can observe them immediately. */
    @Synchronized
    fun init(context: Context) {
        val directory = File(context.applicationContext.filesDir, DIR)
        val existing = directory.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension.equals("jpg", ignoreCase = true) }
            .associate { file ->
                normalizePlaylistId(file.nameWithoutExtension) to versionedUri(file)
            }
        _covers.value = existing
    }

    /** Normalizes server browse ids and raw playlist ids to the same cover key. */
    internal fun normalizePlaylistId(id: String): String = id.trim().removePrefix("VL")

    private fun canonicalId(playlistId: String): String {
        val localId = playlistId.trim().removePrefix("local:playlist:")
        return normalizePlaylistId(localId)
    }

    private fun file(context: Context, playlistId: String) =
        File(File(context.filesDir, DIR), "${canonicalId(playlistId).replace(Regex("[^A-Za-z0-9_-]"), "_")}.jpg")

    private fun versionedUri(file: File): Uri = Uri.fromFile(file)
        .buildUpon()
        .appendQueryParameter("v", "${file.lastModified()}-${file.length()}")
        .build()

    fun getCoverUri(context: Context, playlistId: String): Uri? {
        val normalizedId = normalizePlaylistId(canonicalId(playlistId))
        return file(context, normalizedId).takeIf { it.isFile }?.let(::versionedUri)
    }

    fun delete(context: Context, playlistId: String) {
        val normalizedId = normalizePlaylistId(canonicalId(playlistId))
        file(context, normalizedId).delete()
        _covers.update { it - normalizedId }
    }

    /** Center-crops to a square, scales to at most 800px, and atomically replaces the cover. */
    suspend fun save(context: Context, playlistId: String, source: Uri): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            var sample = 1
            while (bounds.outWidth / sample > SIZE * 2 || bounds.outHeight / sample > SIZE * 2) sample *= 2
            val decoded = resolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null
            val side = minOf(decoded.width, decoded.height)
            val square = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side)
            val scaled = if (side > SIZE) Bitmap.createScaledBitmap(square, SIZE, SIZE, true) else square
            val output = file(context, playlistId)
            output.parentFile?.mkdirs()
            val temporary = File(output.parentFile, "${output.name}.tmp")
            temporary.outputStream().use { stream ->
                check(scaled.compress(Bitmap.CompressFormat.JPEG, 90, stream))
            }
            if (!temporary.renameTo(output)) {
                temporary.copyTo(output, overwrite = true)
                temporary.delete()
            }
            check(output.isFile)
            val normalizedId = normalizePlaylistId(canonicalId(playlistId))
            val savedUri = getCoverUri(context, normalizedId)
            if (savedUri != null) _covers.update { it + (normalizedId to savedUri) }
            savedUri
        }.getOrNull()
    }

    private const val BACKUP_MAX_DIMENSION = 512
    private const val BACKUP_JPEG_QUALITY = 80

    /**
     * Exports all local playlist custom covers, scaled down to at most 512x512 JPEG,
     * base64-encoded to keep backups lightweight.
     */
    suspend fun exportCovers(context: Context): List<PlaylistCoverBackup> = withContext(Dispatchers.IO) {
        val directory = File(context.applicationContext.filesDir, DIR)
        val files = directory.listFiles()
            ?.filter { it.isFile && it.extension.equals("jpg", ignoreCase = true) }
            .orEmpty()
        files.mapNotNull { file ->
            runCatching {
                val playlistId = normalizePlaylistId(file.nameWithoutExtension)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@mapNotNull null
                var sample = 1
                while (bounds.outWidth / sample > BACKUP_MAX_DIMENSION * 2 ||
                    bounds.outHeight / sample > BACKUP_MAX_DIMENSION * 2
                ) {
                    sample *= 2
                }
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                val decoded = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return@mapNotNull null
                val maxDim = maxOf(decoded.width, decoded.height)
                val scaled = if (maxDim > BACKUP_MAX_DIMENSION) {
                    val scale = BACKUP_MAX_DIMENSION.toFloat() / maxDim
                    val w = (decoded.width * scale).toInt().coerceAtLeast(1)
                    val h = (decoded.height * scale).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(decoded, w, h, true)
                } else {
                    decoded
                }
                val stream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, BACKUP_JPEG_QUALITY, stream)
                val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                PlaylistCoverBackup(
                    playlistId = playlistId,
                    imageBase64 = base64,
                    mimeType = "image/jpeg",
                )
            }.getOrNull()
        }
    }

    /**
     * Restores custom covers from backup, saving them onto disk and notifying listeners.
     */
    suspend fun importCovers(context: Context, backups: List<PlaylistCoverBackup>) = withContext(Dispatchers.IO) {
        if (backups.isEmpty()) return@withContext
        val directory = File(context.applicationContext.filesDir, DIR).apply { mkdirs() }
        for (backup in backups) {
            runCatching {
                val bytes = Base64.decode(backup.imageBase64, Base64.DEFAULT)
                if (bytes.isEmpty()) return@runCatching
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching
                val targetFile = file(context, backup.playlistId)
                targetFile.parentFile?.mkdirs()
                val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
                tempFile.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }
            }
        }
        init(context)
        LocalPlaylistStore.refreshCovers()
    }
}
