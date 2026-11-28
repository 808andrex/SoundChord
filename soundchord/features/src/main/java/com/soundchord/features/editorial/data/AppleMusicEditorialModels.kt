package com.soundchord.features.editorial.data

import kotlinx.serialization.Serializable

enum class EditorialItemKind {
    PLAYLIST,
    ALBUM,
}

@Serializable
data class AppleMusicEditorialItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val wideImageUrl: String? = null,
    val badge: String? = null,
    val kind: EditorialItemKind = EditorialItemKind.PLAYLIST,
    val isExplicit: Boolean = false,
    val storefront: String = "us",
    val trackCount: Int? = null,
)

@Serializable
data class AppleMusicEditorialSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val thumbnailUrl: String? = null,
    val isExplicit: Boolean = false,
    val durationMs: Long = 0L,
    val isrc: String? = null,
)

@Serializable
data class AppleMusicEditorialFeed(
    val heroItems: List<AppleMusicEditorialItem> = emptyList(),
    val newSongs: List<AppleMusicEditorialSong> = emptyList(),
    val updatedPlaylists: List<AppleMusicEditorialItem> = emptyList(),
    val dailyTop100: List<AppleMusicEditorialItem> = emptyList(),
    val cityCharts: List<AppleMusicEditorialItem> = emptyList(),
    val djMixes: List<AppleMusicEditorialItem> = emptyList(),
    val comingSoon: List<AppleMusicEditorialItem> = emptyList(),
    val trendingSongs: List<AppleMusicEditorialSong> = emptyList(),
    val momentSongs: List<AppleMusicEditorialSong> = emptyList(),
    val decadeHits: List<AppleMusicEditorialItem> = emptyList(),
    val roadTrip: List<AppleMusicEditorialItem> = emptyList(),
    val everyoneIsListening: List<AppleMusicEditorialItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
)

@Serializable
data class AppleMusicHomeDiscoveryFeed(
    val workoutPlaylists: List<AppleMusicEditorialItem> = emptyList(),
    val chillPlaylists: List<AppleMusicEditorialItem> = emptyList(),
    val essentialsPlaylists: List<AppleMusicEditorialItem> = emptyList(),
    val focusPlaylists: List<AppleMusicEditorialItem> = emptyList(),
    val partyPlaylists: List<AppleMusicEditorialItem> = emptyList(),
    val feelGoodPlaylists: List<AppleMusicEditorialItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
)
