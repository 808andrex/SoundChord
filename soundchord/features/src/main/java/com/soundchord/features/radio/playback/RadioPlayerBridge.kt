package com.soundchord.features.radio.playback

import android.content.Context
import com.music.bitchord.data.model.PlaybackSourceType
import com.music.bitchord.data.model.Song
import com.soundchord.features.R
import com.soundchord.features.radio.data.RadioStation

/**
 * Convierte una [RadioStation] en un objeto [Song] configurado para que
 * el reproductor de BitChord/SoundChord identifique el stream como una emisión en directo.
 */
fun RadioStation.toSong(context: Context): Song {
    val liveBadgeText = context.getString(R.string.live_radio)
    val radioSource = context.getString(R.string.radio_playback_source)
    val defaultArtist = country?.takeIf { it.isNotBlank() } ?: "Radio"

    return Song(
        videoId = streamUrl,
        title = name,
        artist = defaultArtist,
        thumbnailUrl = favicon.orEmpty(),
        durationText = liveBadgeText,
        albumName = codec?.uppercase()?.takeIf { it.isNotBlank() } ?: "LIVE",
        radioName = name,
        // Al asignar liveBadgeText a sourceQuality, la UI del reproductor dibuja
        // el badge ("● EN VIVO" / "● LIVE") justo debajo de la barra de tiempo
        sourceQuality = liveBadgeText,
        playbackSource = radioSource,
        playbackSourceType = PlaybackSourceType.EXPLORE
    )
}