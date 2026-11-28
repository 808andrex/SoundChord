package com.soundchord.features.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.soundchord.features.R
import com.soundchord.features.editorial.data.AppleMusicEditorialFeed
import com.soundchord.features.editorial.data.AppleMusicEditorialItem
import com.soundchord.features.editorial.data.AppleMusicEditorialRepository
import com.soundchord.features.editorial.data.AppleMusicEditorialSong
import com.soundchord.features.editorial.data.EditorialItemKind
import com.soundchord.features.playlistimport.data.AppleMusicPlaylistRepository
import com.soundchord.features.playlistimport.data.AppleMusicTrack
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.components.trackColumnWidth
import kotlinx.coroutines.launch
import com.music.bitchord.ui.components.coverSharedElement
import com.music.bitchord.ui.components.rememberCoverTap
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.MoodGenre
import com.music.bitchord.data.model.MoodGenreSection
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.ShelfLayout
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.PullToRefresh

/** In-memory fallback and singleton store for Explore row scroll positions across compositions */
object ExploreScrollStateHolder {
    val rowScrollStates = mutableMapOf<String, Pair<Int, Int>>()

    fun getRowScroll(key: String): Pair<Int, Int> = rowScrollStates[key] ?: (0 to 0)

    fun saveRowScroll(key: String, index: Int, offset: Int) {
        rowScrollStates[key] = index to offset
    }
}

@kotlinx.serialization.Serializable
private data class CachedMusicVideoItem(
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String?,
    val videoId: String?,
    val browseId: String?,
    val isVideo: Boolean,
)

@kotlinx.serialization.Serializable
private data class CachedMusicVideoShelf(
    val title: String,
    val subtitle: String,
    val items: List<CachedMusicVideoItem>,
)

internal object ExploreMusicVideosCache {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true }
    var cachedShelves: List<HomeShelf> = emptyList()

    fun getOrRestore(): List<HomeShelf> {
        if (cachedShelves.isNotEmpty()) return cachedShelves
        val rawJson = com.soundchord.features.editorial.data.EditorialDatabaseCache.getVideoShelvesJson() ?: return emptyList()
        return runCatching {
            val decoded = json.decodeFromString<List<CachedMusicVideoShelf>>(rawJson)
            val shelves = decoded.map { shelf ->
                HomeShelf(
                    title = shelf.title,
                    subtitle = shelf.subtitle,
                    layout = ShelfLayout.VIDEO,
                    items = shelf.items.map { item ->
                        ShelfItem(
                            title = item.title,
                            subtitle = item.subtitle,
                            thumbnailUrl = item.thumbnailUrl,
                            videoId = item.videoId,
                            browseId = item.browseId,
                            isVideo = item.isVideo,
                        )
                    }
                )
            }
            cachedShelves = shelves
            shelves
        }.getOrDefault(emptyList())
    }

    fun updateIfNotEmpty(newShelves: List<HomeShelf>) {
        if (newShelves.isNotEmpty() && newShelves.any { it.items.isNotEmpty() }) {
            cachedShelves = newShelves
            runCatching {
                val toSave = newShelves.map { shelf ->
                    CachedMusicVideoShelf(
                        title = shelf.title,
                        subtitle = shelf.subtitle,
                        items = shelf.items.map { item ->
                            CachedMusicVideoItem(
                                title = item.title,
                                subtitle = item.subtitle,
                                thumbnailUrl = item.thumbnailUrl,
                                videoId = item.videoId,
                                browseId = item.browseId,
                                isVideo = item.isVideo,
                            )
                        }
                    )
                }
                com.soundchord.features.editorial.data.EditorialDatabaseCache.putVideoShelves(json.encodeToString(toSave))
            }
        }
    }
}

class ExploreRowScrollController(
    val getScroll: (String) -> Pair<Int, Int> = { ExploreScrollStateHolder.getRowScroll(it) },
    val saveScroll: (String, Int, Int) -> Unit = { key, index, offset ->
        ExploreScrollStateHolder.saveRowScroll(key, index, offset)
    },
)

val LocalExploreRowScrollController = staticCompositionLocalOf { ExploreRowScrollController() }

/**
 * Persists and restores the exact firstVisibleItemIndex and firstVisibleItemScrollOffset
 * of each horizontal carousel across navigation (e.g. entering and returning from DetailScreen).
 */
@Composable
fun rememberPersistentRowState(sectionKey: String): LazyListState {
    val controller = LocalExploreRowScrollController.current
    val (savedIndex, savedOffset) = remember(sectionKey) {
        controller.getScroll(sectionKey)
    }

    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = savedIndex,
        initialFirstVisibleItemScrollOffset = savedOffset,
    )

    LaunchedEffect(state, sectionKey) {
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                controller.saveScroll(sectionKey, index, offset)
            }
    }

    DisposableEffect(state, sectionKey) {
        onDispose {
            controller.saveScroll(sectionKey, state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
        }
    }

    return state
}

/** Modular Explore feed: app data and navigation are supplied through models and callbacks. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    moodState: UiState<List<MoodGenreSection>>,
    shelves: List<HomeShelf>,
    listState: LazyListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() },
    onCategoryClick: (MoodGenre) -> Unit,
    onShelfItemClick: (HomeShelf, ShelfItem) -> Unit,
    onShelfMore: (HomeShelf) -> Unit,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    editorialEnabled: Boolean = true,
    onOpenMoodsGenres: () -> Unit = {},
    onOpenCharts: () -> Unit = {},
    onPlaySong: ((Song) -> Unit)? = null,
    onSongMenu: ((Song) -> Unit)? = null,
    getRowScrollPosition: ((String) -> Pair<Int, Int>)? = null,
    onSaveRowScrollPosition: ((String, Int, Int) -> Unit)? = null,
    onEditorialItemClick: (AppleMusicEditorialItem) -> Unit = { item ->
        val storefront = item.storefront.ifBlank { "us" }
        val browseId = if (item.id.startsWith("MPREb") || item.id.startsWith("VL") || item.id.startsWith("PL") || item.id.startsWith("UC") || item.id.startsWith("apple:")) {
            item.id
        } else {
            when (item.kind) {
                EditorialItemKind.PLAYLIST -> "apple:playlist:$storefront:${item.id}"
                EditorialItemKind.ALBUM -> "apple:album:$storefront:${item.id}"
            }
        }
        onShelfItemClick(
            HomeShelf(title = "Editorial", items = emptyList()),
            ShelfItem(
                title = item.title,
                subtitle = item.subtitle,
                thumbnailUrl = item.thumbnailUrl ?: item.wideImageUrl,
                videoId = null,
                browseId = browseId,
            ),
        )
    },
) {
    val scrollController = remember(getRowScrollPosition, onSaveRowScrollPosition) {
        ExploreRowScrollController(
            getScroll = { key ->
                getRowScrollPosition?.invoke(key) ?: ExploreScrollStateHolder.getRowScroll(key)
            },
            saveScroll = { key, index, offset ->
                ExploreScrollStateHolder.saveRowScroll(key, index, offset)
                onSaveRowScrollPosition?.invoke(key, index, offset)
            },
        )
    }

    var editorialFeed by remember { mutableStateOf(AppleMusicEditorialRepository.getCachedFeedOrSeed()) }
    var resolvingSongId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(editorialEnabled) {
        if (editorialEnabled) {
            val freshFeed = AppleMusicEditorialRepository.getEditorialFeed()
            if (freshFeed.heroItems.isNotEmpty() || freshFeed.newSongs.isNotEmpty()) {
                editorialFeed = freshFeed
            }
        }
    }

    val filteredShelves = remember(shelves) {
        shelves.filter { it.items.isNotEmpty() && !it.isExploreExcluded() }
    }
    val categoryShelvesMap = remember(filteredShelves) {
        ExploreShelfCategory.entries
            .filter {
                it != ExploreShelfCategory.FROM_YOUR_LIBRARY &&
                    it != ExploreShelfCategory.TRENDING &&
                    it != ExploreShelfCategory.PROGRAMS_FOR_YOU
            }
            .associateWith { category ->
                filteredShelves.filter { it.exploreCategory() == category }
            }
            .filterValues { it.isNotEmpty() }
    }

    val rawVideoShelves = categoryShelvesMap[ExploreShelfCategory.MUSIC_VIDEOS]
    LaunchedEffect(rawVideoShelves) {
        if (!rawVideoShelves.isNullOrEmpty()) {
            ExploreMusicVideosCache.updateIfNotEmpty(rawVideoShelves)
        }
    }
    val videoShelves = remember(rawVideoShelves, editorialFeed) {
        val fromLive = rawVideoShelves?.takeIf { it.isNotEmpty() }
        val fromCache = fromLive ?: ExploreMusicVideosCache.getOrRestore().takeIf { it.isNotEmpty() }
        fromCache ?: run {
            // Resilient fallback from editorial songs / artist tracks so section is ALWAYS visible
            val fallbackCandidates = (editorialFeed.momentSongs + editorialFeed.trendingSongs).take(12)
            if (fallbackCandidates.isNotEmpty()) {
                listOf(
                    HomeShelf(
                        title = "Vídeos musicales para ti",
                        subtitle = "",
                        layout = ShelfLayout.VIDEO,
                        items = fallbackCandidates.map { song ->
                            ShelfItem(
                                title = song.title,
                                subtitle = song.artist,
                                thumbnailUrl = song.thumbnailUrl,
                                videoId = song.id,
                                browseId = null,
                                isVideo = true,
                            )
                        }
                    )
                )
            } else emptyList()
        }
    }

    val roadTripItems = remember(editorialFeed.roadTrip) {
        val raw = editorialFeed.roadTrip
        val excluded = listOf("infantil", "kids", "disney", "cocomelon", "essentials", "imprescindibles")
        raw.filter { item ->
            val t = item.title.lowercase()
            !excluded.any { t.contains(it) }
        }
    }

    CompositionLocalProvider(LocalExploreRowScrollController provides scrollController) {
        PullToRefresh(
        refreshing = refreshing,
        onRefresh = {
            onRefresh()
            if (editorialEnabled) {
                scope.launch {
                    editorialFeed = AppleMusicEditorialRepository.getEditorialFeed(forceRefresh = true)
                }
            }
        },
        state = pullState,
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (showTitle) {
                item(key = "explore-title") {
                    Text(
                        text = stringResource(R.string.explore_title),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(
                            start = PAGE_GUTTER,
                            end = PAGE_GUTTER,
                            top = 8.dp,
                            bottom = 14.dp,
                        ),
                    )
                }
            }

            if (editorialEnabled) {
                // --- INICIO / EXPLORAR v1.4 (APPLE MUSIC EDITORIAL) ---

                // 1. Destacado (Hero Cards estilo Apple Music)
                val heroCards = editorialFeed.heroItems
                if (heroCards.isNotEmpty()) {
                    item(key = "explore-editorial-hero") {
                        AppleMusicEditorialHeroCarousel(
                            items = heroCards,
                            onItemClick = onEditorialItemClick,
                            sectionKey = "hero_featured",
                        )
                    }
                } else {
                    // Skeleton placeholder para evitar parpadeo al cargar en frío
                    item(key = "explore-editorial-hero-skeleton") {
                        EditorialHeroSkeleton()
                    }
                }

                val handleSongClick: (AppleMusicEditorialSong) -> Unit = { song ->
                    scope.launch {
                        resolvingSongId = song.id
                        val track = AppleMusicTrack(
                            id = song.id,
                            title = song.title,
                            artist = song.artist,
                            album = song.album,
                            durationMs = song.durationMs,
                            isrc = song.isrc,
                            coverUrl = song.thumbnailUrl,
                        )
                        val resolved = AppleMusicPlaylistRepository.resolveTrack(track)
                        resolvingSongId = null
                        if (resolved != null) {
                            onPlaySong?.invoke(resolved) ?: onShelfItemClick(
                                HomeShelf(title = "Editorial", items = emptyList()),
                                ShelfItem(
                                    title = resolved.title,
                                    subtitle = resolved.artist,
                                    thumbnailUrl = resolved.thumbnailUrl,
                                    videoId = resolved.videoId,
                                    browseId = null,
                                ),
                            )
                        }
                    }
                }

                val handleSongMenuClick: (AppleMusicEditorialSong) -> Unit = { song ->
                    scope.launch {
                        val track = AppleMusicTrack(
                            id = song.id,
                            title = song.title,
                            artist = song.artist,
                            album = song.album,
                            durationMs = song.durationMs,
                            isrc = song.isrc,
                            coverUrl = song.thumbnailUrl,
                        )
                        val resolved = AppleMusicPlaylistRepository.resolveTrack(track)
                        if (resolved != null) {
                            onSongMenu?.invoke(resolved)
                        }
                    }
                }

                // 2. Canciones nuevas destacadas (4 canciones por columna)
                val newSongs = editorialFeed.newSongs
                if (newSongs.isNotEmpty()) {
                    item(key = "explore-editorial-new-songs") {
                        AppleMusicEditorialSongsSection(
                            title = stringResource(R.string.editorial_section_new_songs),
                            songs = newSongs,
                            resolvingSongId = resolvingSongId,
                            onSongClick = handleSongClick,
                            onSongMenuClick = handleSongMenuClick,
                            sectionKey = "featured_new_songs",
                        )
                    }
                }

                // 3. Playlists actualizadas
                val updatedPlaylists = editorialFeed.updatedPlaylists
                if (updatedPlaylists.isNotEmpty()) {
                    item(key = "explore-editorial-updated-playlists") {
                        AppleMusicEditorialShelfSection(
                            title = stringResource(R.string.editorial_section_updated_playlists),
                            items = updatedPlaylists,
                            onItemClick = onEditorialItemClick,
                            sectionKey = "updated_playlists",
                        )
                    }
                }

                // 4. Últimos lanzamientos (categoría NEW_RELEASES, inmediatamente debajo de Playlists actualizadas)
                categoryShelvesMap[ExploreShelfCategory.NEW_RELEASES]?.let { releaseShelves ->
                    val filteredReleases = releaseShelves.filter { shelf ->
                        val l = shelf.title.lowercase()
                        !l.contains("para ti") && !l.contains("for you")
                    }
                    if (filteredReleases.isNotEmpty()) {
                        item(key = "explore-shelf-group-NEW_RELEASES") {
                            ExploreShelfSection(
                                title = stringResource(ExploreShelfCategory.NEW_RELEASES.titleRes),
                                shelves = filteredReleases,
                                onItemClick = onShelfItemClick,
                                onMore = onShelfMore,
                                sectionKey = "cat_NEW_RELEASES",
                            )
                        }
                    }
                }

                // 5. Éxitos por década
                val decadeHits = editorialFeed.decadeHits
                if (decadeHits.isNotEmpty()) {
                    item(key = "explore-editorial-decade-hits") {
                        AppleMusicEditorialShelfSection(
                            title = stringResource(R.string.editorial_section_decade_hits),
                            items = decadeHits,
                            onItemClick = onEditorialItemClick,
                            sectionKey = "decade_hits",
                        )
                    }
                }

                // 6. Últimos DJ Mixes
                val djMixes = editorialFeed.djMixes
                if (djMixes.isNotEmpty()) {
                    item(key = "explore-editorial-dj-mixes") {
                        AppleMusicEditorialShelfSection(
                            title = stringResource(R.string.editorial_section_dj_mixes),
                            items = djMixes,
                            onItemClick = onEditorialItemClick,
                            sectionKey = "latest_dj_mixes",
                        )
                    }
                }

                // 7. Toda la gente está escuchando... > (inmediatamente debajo de Últimos DJ Mixes)
                val everyoneListening = editorialFeed.everyoneIsListening
                if (everyoneListening.isNotEmpty()) {
                    item(key = "explore-editorial-everyone-listening") {
                        AppleMusicEditorialShelfSection(
                            title = stringResource(R.string.editorial_section_everyone_listening),
                            items = everyoneListening,
                            onItemClick = onEditorialItemClick,
                            onHeaderClick = {
                                val shelfTitle = "Toda la gente está escuchando..."
                                val shelfItems = everyoneListening.map { item ->
                                    val storefront = item.storefront.ifBlank { "us" }
                                    val browseId = if (item.id.startsWith("MPREb") || item.id.startsWith("VL") || item.id.startsWith("PL") || item.id.startsWith("UC") || item.id.startsWith("apple:")) {
                                        item.id
                                    } else {
                                        when (item.kind) {
                                            EditorialItemKind.PLAYLIST -> "apple:playlist:$storefront:${item.id}"
                                            EditorialItemKind.ALBUM -> "apple:album:$storefront:${item.id}"
                                        }
                                    }
                                    ShelfItem(
                                        title = item.title,
                                        subtitle = item.subtitle,
                                        thumbnailUrl = item.thumbnailUrl ?: item.wideImageUrl,
                                        videoId = null,
                                        browseId = browseId,
                                    )
                                }
                                onShelfMore(HomeShelf(title = shelfTitle, items = shelfItems))
                            },
                            sectionKey = "everyone_listening",
                        )
                    }
                }

                // 8. Para el camino
                if (roadTripItems.isNotEmpty()) {
                    item(key = "explore-editorial-road-trip") {
                        AppleMusicEditorialShelfSection(
                            title = stringResource(R.string.editorial_section_road_trip),
                            items = roadTripItems,
                            onItemClick = onEditorialItemClick,
                            sectionKey = "road_trip",
                        )
                    }
                }

                // 9. Canciones del momento (4 canciones por columna)
                val momentSongs = editorialFeed.momentSongs
                if (momentSongs.isNotEmpty()) {
                    item(key = "explore-editorial-moment-songs") {
                        AppleMusicEditorialSongsSection(
                            title = stringResource(R.string.editorial_section_moment_songs),
                            songs = momentSongs,
                            resolvingSongId = resolvingSongId,
                            onSongClick = handleSongClick,
                            onSongMenuClick = handleSongMenuClick,
                            sectionKey = "songs_of_the_moment",
                        )
                    }
                }

                // 10. Top 100 diario
                val top100 = editorialFeed.dailyTop100
                if (top100.isNotEmpty()) {
                    item(key = "explore-editorial-top-100") {
                        AppleMusicEditorialShelfSection(
                            title = stringResource(R.string.editorial_section_daily_top_100),
                            items = top100,
                            onItemClick = onEditorialItemClick,
                            sectionKey = "daily_top_100",
                        )
                    }
                }

                // 11. Éxitos por ciudad
                val cityCharts = editorialFeed.cityCharts
                if (cityCharts.isNotEmpty()) {
                    item(key = "explore-editorial-city-charts") {
                        AppleMusicEditorialShelfSection(
                            title = stringResource(R.string.editorial_section_city_charts),
                            items = cityCharts,
                            onItemClick = onEditorialItemClick,
                            sectionKey = "city_charts",
                        )
                    }
                }

                // 12. Vídeos musicales (categoría MUSIC_VIDEOS - siempre visible con respaldo)
                if (videoShelves.isNotEmpty()) {
                    item(key = "explore-shelf-group-MUSIC_VIDEOS") {
                        ExploreShelfSection(
                            title = stringResource(ExploreShelfCategory.MUSIC_VIDEOS.titleRes),
                            shelves = videoShelves,
                            onItemClick = onShelfItemClick,
                            onMore = onShelfMore,
                            sectionKey = "cat_MUSIC_VIDEOS",
                        )
                    }
                }

                // 13. Canciones en tendencia (Trending Songs)
                val trendingSongs = editorialFeed.trendingSongs
                if (trendingSongs.isNotEmpty()) {
                    item(key = "explore-editorial-trending-songs") {
                        AppleMusicTrendingSongsSection(
                            title = stringResource(R.string.editorial_section_trending_songs),
                            songs = trendingSongs,
                            resolvingSongId = resolvingSongId,
                            onSongClick = handleSongClick,
                            sectionKey = "trending_songs",
                        )
                    }
                }

                // 15. Más para explorar (Al final estilo Apple Music)
                item(key = "explore-more-to-explore") {
                    ExploreMoreToExploreSection(
                        onOpenMoodsGenres = onOpenMoodsGenres,
                        onOpenCharts = onOpenCharts,
                    )
                }
            } else {
                // --- EXPLORAR CLÁSICO v1.3 (YOUTUBE MUSIC) ---
                val heroShelves = filteredShelves
                if (heroShelves.any { shelf -> shelf.items.any { it.browseId != null && it.thumbnailUrl != null } }) {
                    item(key = "explore-hero") {
                        ExploreHeroCarousel(
                            shelves = heroShelves,
                            onItemClick = onShelfItemClick,
                            sectionKey = "hero_featured",
                        )
                    }
                }

                // Estado de ánimo y géneros arriba en v1.3
                item(key = "explore-moods") {
                    MoodGenreSection(
                        state = moodState,
                        onCategoryClick = onCategoryClick,
                        onRetry = onRetry,
                        sectionKey = "moods_and_genres",
                    )
                }

                // Todas las categorías clásicas de YouTube Music
                categoryShelvesMap.forEach { (category, catShelves) ->
                    item(key = "explore-shelf-group-${category.name}") {
                        ExploreShelfSection(
                            title = stringResource(category.titleRes),
                            shelves = catShelves,
                            onItemClick = onShelfItemClick,
                            onMore = onShelfMore,
                            sectionKey = "cat_${category.name}",
                        )
                    }
                }
            }
        }
    }
    }
}

/**
 * Dark skeleton placeholder for the editorial hero carousel to prevent layout jumps on cold start.
 */
@Composable
fun EditorialHeroSkeleton() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(bottom = 28.dp),
        userScrollEnabled = false,
    ) {
        items(2) {
            Column(
                modifier = Modifier.width(320.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9.5f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                )
            }
        }
    }
}

/**
 * Apple Music style "Más para explorar" bottom list.
 */
@Composable
private fun ExploreMoreToExploreSection(
    onOpenMoodsGenres: () -> Unit,
    onOpenCharts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.explore_more_to_explore),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            thickness = 0.5.dp,
        )

        MoreToExploreRow(
            title = stringResource(R.string.explore_moods_genres),
            onClick = onOpenMoodsGenres,
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            thickness = 0.5.dp,
        )

        MoreToExploreRow(
            title = stringResource(R.string.explore_charts_rankings),
            onClick = onOpenCharts,
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            thickness = 0.5.dp,
        )
    }
}

@Composable
private fun MoreToExploreRow(
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Apple Music / Mornye editorial panoramic Hero carousel (Novedades destacadas).
 */
@Composable
fun AppleMusicEditorialHeroCarousel(
    items: List<AppleMusicEditorialItem>,
    onItemClick: (AppleMusicEditorialItem) -> Unit,
    sectionKey: String = "hero_featured",
) {
    if (items.isEmpty()) return

    val rowState = rememberPersistentRowState(sectionKey)

    LazyRow(
        state = rowState,
        contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(bottom = 28.dp),
    ) {
        items(
            items = items,
            key = { "editorial-hero-${it.storefront}-${it.id}" },
            contentType = { "editorial_hero" },
        ) { item ->
            Column(
                modifier = Modifier
                    .fillParentMaxWidth(0.88f)
                    .clickable { onItemClick(item) },
            ) {
                // Top header outside of image container with safe horizontal padding to prevent letter clipping
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                ) {
                    item.badge?.takeIf { it.isNotBlank() }?.let { badge ->
                        Text(
                            text = badge.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }

                // Large panoramic image card with 14.dp rounded corners
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(248.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    AsyncImage(
                        model = item.wideImageUrl ?: item.thumbnailUrl?.artworkAt(CARD_ART_PX * 2),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Dark gradient in lower half
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.45f to Color.Transparent,
                                    0.75f to Color.Black.copy(alpha = 0.55f),
                                    1f to Color.Black.copy(alpha = 0.88f),
                                ),
                            ),
                    )
                    // Superimposed editorial description (max 2 lines)
                    item.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Text(
                            text = desc,
                            color = Color.White.copy(alpha = 0.92f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Normal,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Apple Music style "Top New Songs" / "Canciones nuevas destacadas" section.
 * Renders horizontal columns of 4 songs per chunk with 48.dp square covers,
 * explicit badge, track info, and 3-dot options menu.
 */
@Composable
fun AppleMusicEditorialSongsSection(
    title: String,
    songs: List<AppleMusicEditorialSong>,
    resolvingSongId: String?,
    onSongClick: (AppleMusicEditorialSong) -> Unit,
    onSongMenuClick: (AppleMusicEditorialSong) -> Unit,
    modifier: Modifier = Modifier,
    sectionKey: String = title,
) {
    if (songs.isEmpty()) return

    val rowState = rememberPersistentRowState(sectionKey)

    Column(modifier = modifier.padding(bottom = 28.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
        )

        val chunkedSongs = remember(songs) { songs.chunked(4) }

        BoxWithConstraints {
            val columnWidth = trackColumnWidth(maxWidth)
            LazyRow(
                state = rowState,
                contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = chunkedSongs,
                    key = { chunk -> "song-chunk-${chunk.firstOrNull()?.id ?: 0}" },
                ) { columnSongs ->
                    Column(
                        modifier = Modifier.width(columnWidth),
                    ) {
                        columnSongs.forEachIndexed { index, song ->
                            val isResolving = resolvingSongId == song.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = !isResolving) { onSongClick(song) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (!song.thumbnailUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = song.thumbnailUrl,
                                            contentDescription = song.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                    if (isResolving) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.weight(1f, fill = false),
                                        )
                                        if (song.isExplicit) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(Color(0xFF3A3A3C))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                            ) {
                                                Text(
                                                    text = "E",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                    ),
                                                    color = Color(0xFFAEAEB2),
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                IconButton(
                                    onClick = { onSongMenuClick(song) },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            if (index < columnSongs.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 60.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Apple Music editorial shelf section with 170.dp square cards, badges and descriptions.
 */
@Composable
fun AppleMusicEditorialShelfSection(
    title: String,
    items: List<AppleMusicEditorialItem>,
    onItemClick: (AppleMusicEditorialItem) -> Unit,
    modifier: Modifier = Modifier,
    onHeaderClick: (() -> Unit)? = null,
    sectionKey: String = title,
) {
    val validItems = remember(items) {
        items.filter { !it.thumbnailUrl.isNullOrBlank() && it.thumbnailUrl.startsWith("https://") && !it.thumbnailUrl.contains("{") }
    }
    if (validItems.isEmpty()) return

    val rowState = rememberPersistentRowState(sectionKey)

    Column(modifier = modifier.padding(bottom = 26.dp)) {
        if (onHeaderClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onHeaderClick)
                    .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp),
                )
            }
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
            )
        }

        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(
                items = validItems,
                key = { "editorial-shelf-${it.storefront}-${it.id}" },
            ) { item ->
                AppleMusicEditorialCard(
                    item = item,
                    onClick = { onItemClick(item) },
                )
            }
        }
    }
}

@Composable
fun AppleMusicEditorialCard(
    item: AppleMusicEditorialItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .width(170.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = item.thumbnailUrl ?: item.wideImageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Trending songs horizontal shelf with Mornye-style square cards.
 */
@Composable
fun AppleMusicTrendingSongsSection(
    title: String,
    songs: List<AppleMusicEditorialSong>,
    resolvingSongId: String?,
    onSongClick: (AppleMusicEditorialSong) -> Unit,
    modifier: Modifier = Modifier,
    sectionKey: String = "trending_songs",
) {
    val validSongs = remember(songs) {
        songs.filter { !it.thumbnailUrl.isNullOrBlank() && it.thumbnailUrl.startsWith("https://") && !it.thumbnailUrl.contains("{") }
    }
    if (validSongs.isEmpty()) return

    val rowState = rememberPersistentRowState(sectionKey)

    Column(modifier = modifier.padding(bottom = 26.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
        )

        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(
                items = validSongs,
                key = { "trending-song-${it.id}" },
            ) { song ->
                val isResolving = resolvingSongId == song.id
                val shape = RoundedCornerShape(14.dp)
                Column(
                    modifier = Modifier
                        .width(150.dp)
                        .clickable(enabled = !isResolving) { onSongClick(song) },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!song.thumbnailUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        if (isResolving) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** Featured artwork carousel, using browsable albums, playlists and artist picks. */
@Composable
fun ExploreHeroCarousel(
    shelves: List<HomeShelf>,
    onItemClick: (HomeShelf, ShelfItem) -> Unit,
    sectionKey: String = "hero_featured",
) {
    val cards = remember(shelves) {
        shelves.flatMap { shelf ->
            shelf.items.filter { it.browseId != null && it.thumbnailUrl != null }
                .map { shelf to it }
        }.distinctBy { (_, item) -> item.browseId }
            .take(8)
    }
    if (cards.isEmpty()) return

    val rowState = rememberPersistentRowState(sectionKey)

    LazyRow(
        state = rowState,
        contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 24.dp),
    ) {
        items(
            items = cards,
            key = { (_, item) -> "explore-hero-${item.browseId}" },
            contentType = { "explore_hero" },
        ) { (shelf, item) ->
            Box(
                modifier = Modifier
                    .fillParentMaxWidth(.88f)
                    .height(220.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onItemClick(shelf, item) },
            ) {
                AsyncImage(
                    model = item.thumbnailUrl.artworkAt(CARD_ART_PX * 2),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            .38f to Color.Black.copy(alpha = .12f),
                            1f to Color.Black.copy(alpha = .82f),
                        ),
                    ),
                )
                Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.explore_featured),
                        color = Color.White.copy(alpha = .84f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = item.title,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.subtitle.ifBlank { shelf.title },
                        color = Color.White.copy(alpha = .84f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Mood and genre carousel with retry/loading states owned by the caller. */
@Composable
fun MoodGenreSection(
    state: UiState<List<MoodGenreSection>>,
    onCategoryClick: (MoodGenre) -> Unit,
    onRetry: () -> Unit,
    sectionKey: String = "moods_and_genres",
) {
    val rawMoodAndGenres = (state as? UiState.Success)?.data.orEmpty()
    val rotationSeed = remember { System.currentTimeMillis() / (4 * 3600 * 1000L) }
    val moods = remember(rawMoodAndGenres, rotationSeed) {
        val validMoods = rawMoodAndGenres.flatMap(MoodGenreSection::items)
            .distinctBy { it.browseId to it.params }
            .filter { mood ->
                val title = mood.title.trim().lowercase()
                val thumb = mood.thumbnailUrl.orEmpty().trim()
                (thumb.startsWith("http://") || thumb.startsWith("https://")) &&
                    !title.contains("navidad") &&
                    !title.contains("novena") &&
                    !title.contains("aguinaldo") &&
                    !title.contains("hanukkah") &&
                    !title.contains("janucá") &&
                    !title.contains("regional y popular")
            }
        validMoods.shuffled(java.util.Random(rotationSeed))
    }

    Column {
        Text(
            text = stringResource(R.string.explore_moods_genres),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = PAGE_GUTTER, end = PAGE_GUTTER, top = 12.dp, bottom = 14.dp),
        )
        when (state) {
            UiState.Loading -> LazyRow(
                contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 22.dp),
            ) {
                items(3) { index ->
                    Box(
                        Modifier.width(240.dp).height(170.dp).clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF242424 + index * 0x080808)),
                    )
                }
            }
            is UiState.Error -> Column(Modifier.padding(horizontal = PAGE_GUTTER, vertical = 12.dp)) {
                Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onRetry, modifier = Modifier.padding(top = 10.dp)) {
                    Text(stringResource(R.string.explore_retry))
                }
            }
            is UiState.Success -> {
                if (moods.isEmpty()) {
                    Text(
                        text = stringResource(R.string.explore_empty_moods),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 12.dp),
                    )
                } else {
                    val rowState = rememberPersistentRowState(sectionKey)
                    LazyRow(
                        state = rowState,
                        contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 22.dp),
                    ) {
                        items(
                            items = moods,
                            key = { "${it.browseId}|${it.params}" },
                            contentType = { "mood_genre" },
                        ) { mood ->
                            MoodGenreCard(mood = mood, onClick = { onCategoryClick(mood) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodGenreCard(mood: MoodGenre, onClick: () -> Unit) {
    val vividAccents = listOf(
        Color(0xFFFF3D81), Color(0xFF7C4DFF), Color(0xFF00B8D9),
        Color(0xFFFF8A00), Color(0xFF00A86B), Color(0xFF365DFF),
    )
    val accent = vividAccents[(mood.title.hashCode().and(Int.MAX_VALUE)) % vividAccents.size]
    Box(
        Modifier.width(240.dp).height(170.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(mood.stripeColor?.let { Color(it.toInt()) } ?: Color(0xFF384653))
            .clickable(onClick = onClick),
    ) {
        mood.thumbnailUrl?.let { artwork ->
            AsyncImage(
                model = artwork.artworkAt(CARD_ART_PX),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to accent.copy(alpha = .22f),
                    .55f to accent.copy(alpha = .08f),
                    1f to Color.Transparent,
                ),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Transparent, .48f to Color.Black.copy(alpha = .12f), 1f to Color.Black.copy(alpha = .78f)),
            ),
        )
        Text(
            text = mood.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 14.dp, end = 44.dp, bottom = 14.dp),
        )
        Box(
            Modifier.align(Alignment.TopEnd).padding(10.dp)
                .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = .5f))
                .clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 8.dp),
        ) {
            Text("▶", color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** A localized shelf heading and one or more horizontal shelves of tappable cards. */
@Composable
fun ExploreShelfSection(
    title: String,
    shelves: List<HomeShelf>,
    onItemClick: (HomeShelf, ShelfItem) -> Unit,
    onMore: (HomeShelf) -> Unit,
    sectionKey: String = title,
) {
    if (shelves.isEmpty()) return
    Column(Modifier.padding(bottom = 22.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = PAGE_GUTTER).padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            shelves.firstOrNull { it.moreBrowseId != null }?.let { moreShelf ->
                Text(
                    text = stringResource(R.string.explore_see_more),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clickable { onMore(moreShelf) }.padding(start = 12.dp),
                )
            }
        }
        shelves.forEachIndexed { shelfIndex, shelf ->
            if (shelf.title.isNotBlank() && shelf.title != title) {
                Text(
                    text = shelf.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = PAGE_GUTTER, end = PAGE_GUTTER, bottom = 8.dp),
                )
            }
            val rowState = rememberPersistentRowState("explore_row_${sectionKey}_${shelf.title}_$shelfIndex")
            LazyRow(
                state = rowState,
                contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = shelf.items,
                    key = { "${shelf.title}:${it.videoId}:${it.browseId}:${it.title}" },
                    contentType = { if (shelf.layout == ShelfLayout.VIDEO || it.isVideo) "explore_video_card" else "explore_shelf_card" },
                ) { item ->
                    ExploreShelfCard(
                        item = item,
                        video = shelf.layout == ShelfLayout.VIDEO || item.isVideo,
                        onClick = { onItemClick(shelf, item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreShelfCard(item: ShelfItem, video: Boolean, onClick: () -> Unit) {
    val cardWidth = if (video) 270.dp else 190.dp
    val cardToken = remember { Any() }
    val openCard = rememberCoverTap(item.browseId ?: item.videoId, cardToken, onClick)
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.width(cardWidth).clickable(onClick = openCard),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(if (video) 16f / 9f else 1f)
                .clip(shape)
                .coverSharedElement(item.browseId ?: item.videoId, shape, cardToken)
                .background(Color(0xFF222222)),
        ) {
            item.thumbnailUrl?.let { artwork ->
                AsyncImage(
                    model = artwork.artworkAt(CARD_ART_PX),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (video) {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, .7f to Color.Transparent, 1f to Color.Black.copy(alpha = .25f))))
            }
        }
        Text(
            text = item.title,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private enum class ExploreShelfCategory(val titleRes: Int) {
    NEW_RELEASES(R.string.explore_new_releases),
    MUSIC_VIDEOS(R.string.explore_music_videos),
    PROGRAMS_FOR_YOU(R.string.explore_programs),
    TRENDING(R.string.explore_trending),
    FROM_YOUR_LIBRARY(R.string.explore_from_library),
    REMIXES(R.string.explore_remixes),
    QUICK_SELECTIONS(R.string.explore_quick_selections),
    COMMUNITY(R.string.explore_community),
    NEW_DISCOVERIES(R.string.explore_new_discoveries),
    DAILY_DISCOVERY(R.string.explore_daily_discovery),
    GOOD_MORNING(R.string.explore_good_morning),
}

private fun HomeShelf.exploreCategory(): ExploreShelfCategory? {
    val label = title.lowercase()
    return when {
        listOf(
            "nuevo lanzamiento", "nuevos lanzamientos", "últimos lanzamientos", "ultimos lanzamientos",
            "new release", "new releases", "latest release", "latest releases", "release", "releases",
            "álbumes y sencillos", "albumes y sencillos", "albums & singles", "new albums",
            "álbum", "album", "single",
        ).any(label::contains) -> ExploreShelfCategory.NEW_RELEASES
        listOf(
            "programa", "programas", "shows", "show", "podcast", "podcasts", "episodio", "episode",
        ).any(label::contains) -> ExploreShelfCategory.PROGRAMS_FOR_YOU
        listOf(
            "videos musicales para ti", "vídeos musicales para ti", "music videos for you",
            "vídeo", "video", "music videos", "videos musicales",
        ).any(label::contains) -> ExploreShelfCategory.MUSIC_VIDEOS
        listOf("buen día", "buen dia", "good morning").any(label::contains) -> ExploreShelfCategory.GOOD_MORNING
        listOf("daily discovery", "descubrimiento diario", "tu descubrimiento diario").any(label::contains) -> ExploreShelfCategory.DAILY_DISCOVERY
        listOf("versiones", "remix", "remixes").any(label::contains) -> ExploreShelfCategory.REMIXES
        listOf("quick selection", "quick picks", "selecciones rápidas", "selecciones rapidas").any(label::contains) -> ExploreShelfCategory.QUICK_SELECTIONS
        listOf("tendencia", "trending", "popular").any(label::contains) -> ExploreShelfCategory.TRENDING
        listOf("biblioteca", "library", "your music").any(label::contains) -> ExploreShelfCategory.FROM_YOUR_LIBRARY
        listOf("comunidad", "community").any(label::contains) -> ExploreShelfCategory.COMMUNITY
        listOf("nuevos descubrimientos", "new discoveries", "discoveries", "descubrimientos").any(label::contains) -> ExploreShelfCategory.NEW_DISCOVERIES
        else -> null
    }
}

private fun HomeShelf.isExploreExcluded(): Boolean {
    val label = title.trim().lowercase()
    val isProgramOrPodcast = label.contains("programa") || label.contains("programas") ||
        label.contains("podcast") || label.contains("podcasts") ||
        label.contains("episodio") || label.contains("episode") ||
        label.contains("show") || label.contains("shows")
    val isExcludedTitle = label in setOf(
        "recents", "recientes", "recently played", "recently played songs", "vuelve a escucharlo", "listen again",
        "favoritos olvidados", "forgotten favorites", "mixes personalizados", "mix personalizado", "mi mix",
        "your mixes", "personalized mixes", "vídeos musicales nuevos", "videos musicales nuevos", "new music videos",
        "álbumes para ti", "albumes para ti", "albums for you",
    ) || (label.contains("para ti") && (label.contains("álbum") || label.contains("album")))
      || label.contains("comunidad") || label.contains("community")
      || label.contains("nuevos descubrimientos") || label.contains("new discoveries")
      || label.contains("descubrimiento diario") || label.contains("daily discovery")
      || isProgramOrPodcast
    val cat = exploreCategory()
    val isExcludedCat = cat == ExploreShelfCategory.FROM_YOUR_LIBRARY ||
        cat == ExploreShelfCategory.TRENDING ||
        cat == ExploreShelfCategory.COMMUNITY ||
        cat == ExploreShelfCategory.NEW_DISCOVERIES ||
        cat == ExploreShelfCategory.DAILY_DISCOVERY ||
        cat == ExploreShelfCategory.PROGRAMS_FOR_YOU
    return isExcludedTitle || isExcludedCat
}
