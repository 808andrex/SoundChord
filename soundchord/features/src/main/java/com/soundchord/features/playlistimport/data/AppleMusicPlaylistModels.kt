package com.soundchord.features.playlistimport.data

/**
 * Single track metadata extracted from an Apple Music playlist.
 */
data class AppleMusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long,
    val isrc: String? = null,
    val coverUrl: String? = null,
)

/**
 * Metadata and tracklist of an Apple Music playlist, including optional motion artwork.
 */
data class AppleMusicPlaylist(
    val id: String,
    val name: String,
    val description: String = "",
    val curator: String = "",
    val coverUrl: String? = null,
    val motionVideoUrl: String? = null,
    val motionPreviewUrl: String? = null,
    val tracks: List<AppleMusicTrack> = emptyList(),
    val storefront: String = "us",
)
