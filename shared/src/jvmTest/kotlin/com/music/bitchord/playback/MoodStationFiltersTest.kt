package com.music.bitchord.playback

import com.music.bitchord.data.model.Song
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoodStationFiltersTest {

    private fun sampleSong(
        id: String = "test1234",
        title: String = "Midnight City",
        artist: String = "M83",
        thumb: String? = "https://lh3.googleusercontent.com/sample=w540-h540-l90-rj",
        isVideo: Boolean = false,
        duration: String? = "4:03",
    ) = Song(
        videoId = id,
        title = title,
        artist = artist,
        thumbnailUrl = thumb,
        isVideo = isVideo,
        durationText = duration,
    )

    @Test
    fun officialSongFilterAcceptsSquareCatalogueTracks() {
        val song = sampleSong()
        assertTrue(MoodStationFilters.isOfficialSong(song))
    }

    @Test
    fun officialSongFilterRejectsVideoFlaggedTracks() {
        val videoTrack = sampleSong(isVideo = true)
        assertFalse(MoodStationFilters.isOfficialSong(videoTrack))

        val videoOriginTrack = sampleSong().copy(isVideoOrigin = true)
        assertFalse(MoodStationFilters.isOfficialSong(videoOriginTrack))
    }

    @Test
    fun officialSongFilterRejectsWidescreenArtwork() {
        // Raw uncropped 16:9 thumbnail
        val rawHq = sampleSong(thumb = "https://i.ytimg.com/vi/abc/hqdefault.jpg")
        assertFalse(MoodStationFilters.isOfficialSong(rawHq))

        // Explicit 16:9 dimension
        val wide169 = sampleSong(thumb = "https://img.youtube.com/vi/abc/w640-h360.jpg")
        assertFalse(MoodStationFilters.isOfficialSong(wide169))

        // Cropped square thumbnail is accepted
        val croppedSquare = sampleSong(thumb = "https://i.ytimg.com/vi/abc/hqdefault.jpg?sqp=-p-&w=540")
        assertTrue(MoodStationFilters.isOfficialSong(croppedSquare))

        val dimSquare = sampleSong(thumb = "https://img.youtube.com/vi/abc/w500-h500.jpg")
        assertTrue(MoodStationFilters.isOfficialSong(dimSquare))
    }

    @Test
    fun officialSongFilterRejectsUgcAndLyricVideos() {
        assertFalse(MoodStationFilters.isOfficialSong(sampleSong(title = "Starboy (Official Music Video)")))
        assertFalse(MoodStationFilters.isOfficialSong(sampleSong(title = "Blinding Lights [Lyric Video]")))
        assertFalse(MoodStationFilters.isOfficialSong(sampleSong(title = "Provenza (Video Oficial)")))
        assertFalse(MoodStationFilters.isOfficialSong(sampleSong(title = "Desamor - Letra / Lyrics")))
        assertFalse(MoodStationFilters.isOfficialSong(sampleSong(title = "Track (Sped Up + Reverb)")))
        assertFalse(MoodStationFilters.isOfficialSong(sampleSong(title = "Track (Slowed & Reverb)")))
        assertFalse(MoodStationFilters.isOfficialSong(sampleSong(title = "Track (Visualizer)")))
        assertFalse(MoodStationFilters.isOfficialSong(sampleSong(title = "Album Track (Audio Commentary)")))
        assertFalse(MoodStationFilters.isOfficialSong(sampleSong(title = "Track (Spoken Intro)")))
        assertFalse(MoodStationFilters.isOfficialSong(sampleSong(title = "Track (Karaoke Version)")))
    }

    @Test
    fun officialSongFilterRejectsExtremeDurations() {
        // Skit / Spoken intro < 45s
        assertFalse(MoodStationFilters.isOfficialSong(sampleSong(duration = "0:25")))
        // Normal song 3:30
        assertTrue(MoodStationFilters.isOfficialSong(sampleSong(duration = "3:30")))
        // Long podcast / live set > 12m
        assertFalse(MoodStationFilters.isOfficialSong(sampleSong(duration = "15:45")))
    }

    @Test
    fun culturalProfileBlocksAlienRegionalShelvesAndTracksWhenNoAffinity() {
        // User listens to Latin and Western pop/rock
        val profile = MoodStationFilters.buildCulturalProfile(
            artists = listOf("The Weeknd", "Bad Bunny", "Coldplay", "Dua Lipa"),
            genres = listOf("pop", "latin", "reggaeton", "alternative"),
            songs = listOf(sampleSong(title = "Blinding Lights", artist = "The Weeknd")),
        )

        assertFalse(profile.hasDesiAffinity)
        assertFalse(profile.hasKpopAffinity)
        assertFalse(profile.hasArabicAffinity)

        // Shelves from alien scenes are blocked
        assertFalse(MoodStationFilters.isCulturallyAllowedShelf("Romantic Bollywood", profile))
        assertFalse(MoodStationFilters.isCulturallyAllowedShelf("Hindi Chill Hits", profile))
        assertFalse(MoodStationFilters.isCulturallyAllowedShelf("Punjabi Sad Songs", profile))
        assertFalse(MoodStationFilters.isCulturallyAllowedShelf("Top 50 K-Pop Workout", profile))

        // Matching / universal shelves are allowed
        assertTrue(MoodStationFilters.isCulturallyAllowedShelf("Chill Hits", profile))
        assertTrue(MoodStationFilters.isCulturallyAllowedShelf("Acoustic Unwind", profile))
        assertTrue(MoodStationFilters.isCulturallyAllowedShelf("Latin Vibes", profile))
        assertTrue(MoodStationFilters.isCulturallyAllowedShelf("Pop Romance", profile))

        // Alien tracks are blocked
        assertFalse(MoodStationFilters.hasCulturalAffinity("Arijit Singh", "Kesariya", profile))
        assertFalse(MoodStationFilters.hasCulturalAffinity("Diljit Dosanjh", "Lover", profile))
        // Indic script track is blocked
        assertFalse(MoodStationFilters.hasCulturalAffinity("कलाकार", "गीत", profile))

        // Culturally affine tracks are allowed
        assertTrue(MoodStationFilters.hasCulturalAffinity("Coldplay", "Yellow", profile))
        assertTrue(MoodStationFilters.hasCulturalAffinity("Bad Bunny", "Ojitos Lindos", profile))
    }

    @Test
    fun culturalProfileAllowsRegionalTracksWhenUserHasAffinity() {
        // User listens to Arijit Singh / Bollywood
        val profile = MoodStationFilters.buildCulturalProfile(
            artists = listOf("Arijit Singh", "Pritam", "Ed Sheeran"),
            genres = listOf("bollywood", "pop"),
            songs = emptyList(),
        )

        assertTrue(profile.hasDesiAffinity)
        assertTrue(MoodStationFilters.isCulturallyAllowedShelf("Romantic Bollywood", profile))
        assertTrue(MoodStationFilters.hasCulturalAffinity("Arijit Singh", "Kesariya", profile))
    }

    @Test
    fun isMoodStationSongIdentifiesMoodQueues() {
        val moodSong = sampleSong().copy(playbackSourceId = "ENERGY")
        assertTrue(MoodStationFilters.isMoodStationSong(moodSong))

        val heartbreakSong = sampleSong().copy(playbackSourceId = "HEARTBREAK")
        assertTrue(MoodStationFilters.isMoodStationSong(heartbreakSong))

        val regularSong = sampleSong().copy(playbackSourceId = "VLPL123456789")
        assertFalse(MoodStationFilters.isMoodStationSong(regularSong))

        val noSourceSong = sampleSong().copy(playbackSourceId = null)
        assertFalse(MoodStationFilters.isMoodStationSong(noSourceSong))
    }

    @Test
    fun officialSongFilterRejectsVevoVideoMarkers() {
        assertFalse(MoodStationFilters.isOfficialSong(sampleSong(artist = "TheWeekndVEVO")))
        assertFalse(MoodStationFilters.isOfficialSong(sampleSong(artist = "DuaLipaVEVO")))
        assertFalse(MoodStationFilters.isOfficialSong(sampleSong(title = "Blinding Lights (VEVO Official)")))
        assertFalse(MoodStationFilters.isOfficialSong(sampleSong(title = "Levitating [VEVO]")))
    }

    @Test
    fun metadataSanitizationCleansChannelPrefixesAndVideoSuffixes() {
        val dirty1 = sampleSong(
            title = "The Weeknd - Blinding Lights (Official Video)",
            artist = "TheWeekndVEVO",
        )
        val clean1 = MoodStationFilters.sanitizeSongMetadata(dirty1)
        kotlin.test.assertEquals("Blinding Lights", clean1.title)
        kotlin.test.assertEquals("TheWeeknd", clean1.artist)

        val dirty2 = sampleSong(
            title = "Coldplay - Yellow [Official Music Video]",
            artist = "Coldplay - Topic",
        )
        val clean2 = MoodStationFilters.sanitizeSongMetadata(dirty2)
        kotlin.test.assertEquals("Yellow", clean2.title)
        kotlin.test.assertEquals("Coldplay", clean2.artist)

        val dirty3 = sampleSong(
            title = "Dua Lipa - Levitating | Official Video",
            artist = "Dua Lipa",
        )
        val clean3 = MoodStationFilters.sanitizeSongMetadata(dirty3)
        kotlin.test.assertEquals("Levitating", clean3.title)
        kotlin.test.assertEquals("Dua Lipa", clean3.artist)

        val dirty4 = sampleSong(
            title = "Starboy (Audio)",
            artist = "The Weeknd - VEVO",
        )
        val clean4 = MoodStationFilters.sanitizeSongMetadata(dirty4)
        kotlin.test.assertEquals("Starboy", clean4.title)
        kotlin.test.assertEquals("The Weeknd", clean4.artist)
    }

    @Test
    fun stationIdentificationDetectsPersonalAndDiscoveryQueues() {
        val personalSong = sampleSong().copy(playbackSourceId = "station:personal:RDTMAK55")
        assertTrue(MoodStationFilters.isPersonalStationSong(personalSong))
        assertFalse(MoodStationFilters.isDiscoveryStationSong(personalSong))
        assertTrue(MoodStationFilters.isFilteredPersonalOrDiscoveryStation(personalSong))

        val discoverySong = sampleSong().copy(playbackSourceId = "station:discovery:RDTMAK12")
        assertFalse(MoodStationFilters.isPersonalStationSong(discoverySong))
        assertTrue(MoodStationFilters.isDiscoveryStationSong(discoverySong))
        assertTrue(MoodStationFilters.isFilteredPersonalOrDiscoveryStation(discoverySong))

        val regularSong = sampleSong().copy(playbackSourceId = "VLPL123456789")
        assertFalse(MoodStationFilters.isPersonalStationSong(regularSong))
        assertFalse(MoodStationFilters.isDiscoveryStationSong(regularSong))
        assertFalse(MoodStationFilters.isFilteredPersonalOrDiscoveryStation(regularSong))
    }
}
