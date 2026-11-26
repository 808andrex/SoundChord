package com.soundchord.features

import com.music.bitchord.data.model.AlbumRelatedSections
import com.music.bitchord.data.model.ShelfItem
import com.soundchord.features.editorial.data.AlbumRelatedRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumRelatedRepositoryTest {

    @Test
    fun testAlbumRelatedSectionsDefaults() {
        val defaultSections = AlbumRelatedSections()
        assertTrue(defaultSections.musicVideos.isEmpty())
        assertTrue(defaultSections.moreByArtist.isEmpty())
        assertTrue(defaultSections.appearsOn.isEmpty())
        assertTrue(defaultSections.youMightAlsoLike.isEmpty())
        assertFalse(defaultSections.isLoaded)
    }

    @Test
    fun testAlbumRelatedSectionsWithData() {
        val video = ShelfItem(
            title = "Test Video",
            subtitle = "Test Artist",
            thumbnailUrl = "https://example.com/thumb.jpg",
            videoId = "video123",
            browseId = null,
            isVideo = true,
        )
        val album = ShelfItem(
            title = "Other Album",
            subtitle = "Test Artist",
            thumbnailUrl = "https://example.com/album.jpg",
            videoId = null,
            browseId = "MPREb_123",
            isVideo = false,
        )

        val sections = AlbumRelatedSections(
            musicVideos = listOf(video),
            moreByArtist = listOf(album),
            appearsOn = emptyList(),
            youMightAlsoLike = emptyList(),
            isLoaded = true,
        )

        assertEquals(1, sections.musicVideos.size)
        assertTrue(sections.musicVideos.first().isVideo)
        assertEquals(1, sections.moreByArtist.size)
        assertFalse(sections.moreByArtist.first().isVideo)
        assertTrue(sections.appearsOn.isEmpty())
        assertTrue(sections.youMightAlsoLike.isEmpty())
        assertTrue(sections.isLoaded)
    }

    @Test
    fun testCacheOperations() {
        AlbumRelatedRepository.clearCache()
        assertNull(AlbumRelatedRepository.getCached("album_test_id:bad bunny"))

        val dummy = AlbumRelatedSections(isLoaded = true)
        AlbumRelatedRepository.putInMemoryCache("album_test_id:bad bunny", dummy)
        assertNotNull(AlbumRelatedRepository.getCached("album_test_id:bad bunny"))

        // Check invalidate
        AlbumRelatedRepository.invalidate("album_test_id:bad bunny")
        assertNull(AlbumRelatedRepository.getCached("album_test_id:bad bunny"))
    }

    @Test
    fun testCleanThumbnailUrl() {
        // Protocol relative
        assertEquals("https://example.com/art.jpg", AlbumRelatedRepository.cleanThumbnailUrl("//example.com/art.jpg"))
        // http to https
        assertEquals("https://example.com/art.jpg", AlbumRelatedRepository.cleanThumbnailUrl("http://example.com/art.jpg"))
        // sddefault to hqdefault
        assertEquals("https://i.ytimg.com/vi/123/hqdefault.jpg", AlbumRelatedRepository.cleanThumbnailUrl("https://i.ytimg.com/vi/123/sddefault.jpg"))
        // Google content server image resizing
        assertEquals("https://lh3.googleusercontent.com/test=s600-c", AlbumRelatedRepository.cleanThumbnailUrl("https://lh3.googleusercontent.com/test=w120-h120-l90-rj"))
        assertEquals("https://yt3.ggpht.com/avatar=s600-c", AlbumRelatedRepository.cleanThumbnailUrl("https://yt3.ggpht.com/avatar=s60"))
        // Invalid or null
        assertNull(AlbumRelatedRepository.cleanThumbnailUrl(null))
        assertNull(AlbumRelatedRepository.cleanThumbnailUrl(""))
        assertNull(AlbumRelatedRepository.cleanThumbnailUrl("ftp://foo.com/bar.jpg"))
        assertNull(AlbumRelatedRepository.cleanThumbnailUrl("https://foo.com/{w}x{h}.jpg"))
    }

    @Test
    fun testExtractPrimaryArtist() {
        assertEquals("The Weeknd", AlbumRelatedRepository.extractPrimaryArtist("The Weeknd, Justice"))
        assertEquals("The Weeknd", AlbumRelatedRepository.extractPrimaryArtist("The Weeknd, Justice • 2025"))
        assertEquals("The Weeknd", AlbumRelatedRepository.extractPrimaryArtist("The Weeknd, Álbum"))
        assertEquals("The Weeknd", AlbumRelatedRepository.extractPrimaryArtist("The Weeknd, Album"))
        assertEquals("Bad Bunny", AlbumRelatedRepository.extractPrimaryArtist("Bad Bunny & Chencho Corleone"))
        assertEquals("Metro Boomin", AlbumRelatedRepository.extractPrimaryArtist("Metro Boomin feat. Future"))
        assertEquals("Playboi Carti", AlbumRelatedRepository.extractPrimaryArtist("Playboi Carti & The Weeknd"))
        assertEquals("Tyler, the Creator", AlbumRelatedRepository.extractPrimaryArtist("Tyler, The Creator"))
        assertEquals("Earth, Wind & Fire", AlbumRelatedRepository.extractPrimaryArtist("Earth, Wind & Fire"))
        assertEquals("Childish Gambino", AlbumRelatedRepository.extractPrimaryArtist("Childish Gambino"))
    }

    @Test
    fun testExtractBaseTrackTitle() {
        assertEquals("timeless", AlbumRelatedRepository.extractBaseTrackTitle("Timeless (with Playboi Carti) [Official Music Video]"))
        assertEquals("dancing in the flames", AlbumRelatedRepository.extractBaseTrackTitle("Dancing In The Flames (Official Video)"))
        assertEquals("big sleep", AlbumRelatedRepository.extractBaseTrackTitle("Big Sleep [with Giorgio Moroder]"))
        assertEquals("sao paulo", AlbumRelatedRepository.extractBaseTrackTitle("São Paulo (feat. Anitta)"))
        assertEquals("open hearts", AlbumRelatedRepository.extractBaseTrackTitle("Open Hearts"))
    }
}
