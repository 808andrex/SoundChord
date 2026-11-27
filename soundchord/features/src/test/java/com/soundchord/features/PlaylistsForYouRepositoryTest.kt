package com.soundchord.features

import com.soundchord.features.playlists.data.PlaylistsForYouRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistsForYouRepositoryTest {

    @Test
    fun testValidAppleMusicEditorialPlaylistsAccepted() {
        val validArt = "https://is1-ssl.mzstatic.com/image/thumb/Features123/600x600jpg.jpg"

        // Official flagship playlists
        assertTrue(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "¡Dale Play!",
                subtitle = "Apple Music",
                idOrBrowseId = "pl.12345",
                thumbnailUrl = validArt,
                userRealArtists = emptyList(),
                curatorName = "Apple Music",
            )
        )

        assertTrue(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "Fuego",
                subtitle = "Apple Music Urbano Latino",
                idOrBrowseId = "apple:playlist:pl.67890",
                thumbnailUrl = validArt,
                userRealArtists = emptyList(),
                curatorName = "Apple Music Urbano Latino",
            )
        )

        assertTrue(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "Éxitos de hoy",
                subtitle = "Playlist destacada · Apple Music",
                idOrBrowseId = "am:pl.abcde",
                thumbnailUrl = validArt,
                userRealArtists = emptyList(),
                curatorName = "Apple Music",
            )
        )
    }

    @Test
    fun testYouTubeMusicPlaylistsStrictlyExcluded() {
        val validArt = "https://is1-ssl.mzstatic.com/image/thumb/Features123/600x600jpg.jpg"

        // YouTube Music IDs (VL, RD, PL, MP, UC)
        assertFalse(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "Latin Hits 2025",
                subtitle = "Apple Music",
                idOrBrowseId = "VLPL_youtube_123",
                thumbnailUrl = validArt,
                userRealArtists = emptyList(),
            )
        )

        assertFalse(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "Myke Towers Mix",
                subtitle = "Apple Music",
                idOrBrowseId = "RDAMVM12345",
                thumbnailUrl = validArt,
                userRealArtists = listOf("Myke Towers"),
            )
        )

        assertFalse(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "Greatest Hits",
                subtitle = "Apple Music",
                idOrBrowseId = "PL1234567890",
                thumbnailUrl = validArt,
                userRealArtists = emptyList(),
            )
        )
    }

    @Test
    fun testPodcastsAndTalkShowsStrictlyExcluded() {
        val validArt = "https://is1-ssl.mzstatic.com/image/thumb/Features123/600x600jpg.jpg"

        // The Wild Project
        assertFalse(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "The Wild Project #240",
                subtitle = "Apple Music",
                idOrBrowseId = "pl.podcast1",
                thumbnailUrl = validArt,
                userRealArtists = emptyList(),
            )
        )

        assertFalse(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "Lo mejor de Jordi Wild",
                subtitle = "Jordi Wild",
                idOrBrowseId = "pl.podcast2",
                thumbnailUrl = validArt,
                userRealArtists = emptyList(),
                curatorName = "Jordi Wild",
            )
        )

        // Quantum FM / Quantum Fracture
        assertFalse(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "Quantum FM Podcast",
                subtitle = "Apple Music",
                idOrBrowseId = "pl.podcast3",
                thumbnailUrl = validArt,
                userRealArtists = emptyList(),
            )
        )

        // Doctor Fisión / Science talks
        assertFalse(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "Doctor Fisión - Curiosidades del Universo",
                subtitle = "Apple Music",
                idOrBrowseId = "pl.podcast4",
                thumbnailUrl = validArt,
                userRealArtists = emptyList(),
            )
        )

        // General talk show / podcast episodes
        assertFalse(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "Episodio 12: Entrevista exclusiva",
                subtitle = "Apple Music",
                idOrBrowseId = "pl.podcast5",
                thumbnailUrl = validArt,
                userRealArtists = emptyList(),
            )
        )
    }

    @Test
    fun testNonAppleCuratorStrictlyExcluded() {
        val validArt = "https://is1-ssl.mzstatic.com/image/thumb/Features123/600x600jpg.jpg"

        assertFalse(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "Top Urban",
                subtitle = "YouTube Music Community",
                idOrBrowseId = "pl.123",
                thumbnailUrl = validArt,
                userRealArtists = emptyList(),
                curatorName = "YouTube Music Community",
            )
        )

        assertFalse(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "Charla Semanal",
                subtitle = "The Wild Project",
                idOrBrowseId = "pl.123",
                thumbnailUrl = validArt,
                userRealArtists = emptyList(),
                curatorName = "The Wild Project",
            )
        )
    }

    @Test
    fun testArtworkValidation() {
        // Null or blank
        assertFalse(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "¡Dale Play!",
                subtitle = "Apple Music",
                idOrBrowseId = "pl.123",
                thumbnailUrl = null,
                userRealArtists = emptyList(),
            )
        )
        assertFalse(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "¡Dale Play!",
                subtitle = "Apple Music",
                idOrBrowseId = "pl.123",
                thumbnailUrl = "",
                userRealArtists = emptyList(),
            )
        )

        // Non-https
        assertFalse(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "¡Dale Play!",
                subtitle = "Apple Music",
                idOrBrowseId = "pl.123",
                thumbnailUrl = "http://example.com/art.jpg",
                userRealArtists = emptyList(),
            )
        )

        // Unresolved template tokens
        assertFalse(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "¡Dale Play!",
                subtitle = "Apple Music",
                idOrBrowseId = "pl.123",
                thumbnailUrl = "https://example.com/{w}x{h}.jpg",
                userRealArtists = emptyList(),
            )
        )
    }

    @Test
    fun testArtistEssentialsOnlyForRealUserArtists() {
        val validArt = "https://is1-ssl.mzstatic.com/image/thumb/Features123/600x600jpg.jpg"

        // Artist NOT in user's real listening history -> Excluded
        assertFalse(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "Taylor Swift Essentials",
                subtitle = "Apple Music Pop",
                idOrBrowseId = "pl.ts123",
                thumbnailUrl = validArt,
                userRealArtists = listOf("Bad Bunny", "Feid", "Mora"),
            )
        )

        // Artist IN user's real listening history -> Accepted
        assertTrue(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "Bad Bunny Essentials",
                subtitle = "Apple Music Urbano Latino",
                idOrBrowseId = "pl.bb123",
                thumbnailUrl = validArt,
                userRealArtists = listOf("Bad Bunny", "Feid", "Mora"),
            )
        )
    }

    @Test
    fun testFitnessAndJunkTermsExcluded() {
        val validArt = "https://is1-ssl.mzstatic.com/image/thumb/Features123/600x600jpg.jpg"

        assertFalse(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "Apple Music Fitness Cardio",
                subtitle = "Apple Music",
                idOrBrowseId = "pl.fit1",
                thumbnailUrl = validArt,
                userRealArtists = emptyList(),
            )
        )

        assertFalse(
            PlaylistsForYouRepository.isStrictlyValidAppleMusicPlaylist(
                title = "Top Hits Karaoke Version",
                subtitle = "Apple Music",
                idOrBrowseId = "pl.junk1",
                thumbnailUrl = validArt,
                userRealArtists = emptyList(),
            )
        )
    }
}
