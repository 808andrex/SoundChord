package com.soundchord.features.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
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
import coil3.compose.AsyncImage
import com.soundchord.features.R
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.MoodGenre
import com.music.bitchord.data.model.MoodGenreSection
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.ShelfLayout
import com.music.bitchord.data.model.UiState
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.PullToRefresh

/** Modular Explore feed: app data and navigation are supplied through models and callbacks. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    moodState: UiState<List<MoodGenreSection>>,
    shelves: List<HomeShelf>,
    listState: LazyListState,
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
) {
    val contentShelves = shelves.filter { it.items.isNotEmpty() && !it.isExploreExcluded() }
    val categories = ExploreShelfCategory.entries
        .mapNotNull { category -> category to contentShelves.filter { it.exploreCategory() == category } }
        .filter { (_, matching) -> matching.isNotEmpty() }
    val unclassified = contentShelves.filter { it.exploreCategory() == null && !it.isReleaseShelf() }

    PullToRefresh(
        refreshing = refreshing,
        onRefresh = onRefresh,
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

            val heroShelves = contentShelves
            if (heroShelves.any { shelf -> shelf.items.any { it.browseId != null && it.thumbnailUrl != null } }) {
                item(key = "explore-hero") {
                    ExploreHeroCarousel(heroShelves, onShelfItemClick)
                }
            }

            item(key = "explore-moods") {
                MoodGenreSection(
                    state = moodState,
                    onCategoryClick = onCategoryClick,
                    onRetry = onRetry,
                )
            }

            categories.forEach { (category, matchingShelves) ->
                item(key = "explore-shelf-group-${category.name}") {
                    ExploreShelfSection(
                        title = stringResource(category.titleRes),
                        shelves = matchingShelves,
                        onItemClick = onShelfItemClick,
                        onMore = onShelfMore,
                    )
                }
            }

            unclassified.forEachIndexed { index, shelf ->
                item(key = "explore-shelf-${shelf.title}-$index") {
                    ExploreShelfSection(
                        title = shelf.title,
                        shelves = listOf(shelf),
                        onItemClick = onShelfItemClick,
                        onMore = onShelfMore,
                    )
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
) {
    val cards = shelves.flatMap { shelf ->
        shelf.items.filter { it.browseId != null && it.thumbnailUrl != null }
            .map { shelf to it }
    }.distinctBy { (_, item) -> item.browseId }
        .take(8)
    if (cards.isEmpty()) return

    LazyRow(
        contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 24.dp),
    ) {
        items(cards, key = { (_, item) -> "explore-hero-${item.browseId}" }) { (shelf, item) ->
            Box(
                modifier = Modifier
                    .fillParentMaxWidth(.88f)
                    .height(220.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onItemClick(shelf, item) },
            ) {
                AsyncImage(
                    model = item.thumbnailUrl,
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
) {
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
                val moods = state.data.flatMap(MoodGenreSection::items)
                    .distinctBy { it.browseId to it.params }
                if (moods.isEmpty()) {
                    Text(
                        text = stringResource(R.string.explore_empty_moods),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 12.dp),
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 22.dp),
                    ) {
                        items(moods, key = { "${it.browseId}|${it.params}" }) { mood ->
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
                model = artwork,
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
        shelves.forEach { shelf ->
            if (shelves.size > 1 && shelf.title != title) {
                Text(
                    text = shelf.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = PAGE_GUTTER, end = PAGE_GUTTER, bottom = 8.dp),
                )
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(shelf.items, key = { "${shelf.title}:${it.videoId}:${it.browseId}:${it.title}" }) { item ->
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
    Column(
        Modifier.width(cardWidth).clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(if (video) 16f / 9f else 1f)
                .clip(RoundedCornerShape(18.dp)).background(Color(0xFF222222)),
        ) {
            item.thumbnailUrl?.let { artwork ->
                AsyncImage(
                    model = artwork,
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
    MUSIC_VIDEOS(R.string.explore_music_videos),
    TRENDING(R.string.explore_trending),
    REMIXES(R.string.explore_remixes),
    QUICK_SELECTIONS(R.string.explore_quick_selections),
    FROM_LIBRARY(R.string.explore_from_library),
    COMMUNITY(R.string.explore_community),
    VIDEOS_FOR_YOU(R.string.explore_videos_for_you),
    NEW_DISCOVERIES(R.string.explore_new_discoveries),
    DAILY_DISCOVERY(R.string.explore_daily_discovery),
    GOOD_MORNING(R.string.explore_good_morning),
}

private fun HomeShelf.exploreCategory(): ExploreShelfCategory? {
    val label = title.lowercase()
    return when {
        listOf("buen día", "buen dia", "good morning").any(label::contains) -> ExploreShelfCategory.GOOD_MORNING
        listOf("daily discovery", "descubrimiento diario", "tu descubrimiento diario").any(label::contains) -> ExploreShelfCategory.DAILY_DISCOVERY
        listOf("versiones", "remix", "remixes").any(label::contains) -> ExploreShelfCategory.REMIXES
        listOf("quick selection", "quick picks", "selecciones rápidas", "selecciones rapidas").any(label::contains) -> ExploreShelfCategory.QUICK_SELECTIONS
        listOf("tendencia", "trending", "popular").any(label::contains) -> ExploreShelfCategory.TRENDING
        listOf("biblioteca", "library", "your music").any(label::contains) -> ExploreShelfCategory.FROM_LIBRARY
        listOf("comunidad", "community").any(label::contains) -> ExploreShelfCategory.COMMUNITY
        listOf("nuevos descubrimientos", "new discoveries", "discoveries", "descubrimientos").any(label::contains) -> ExploreShelfCategory.NEW_DISCOVERIES
        listOf("videos musicales para ti", "vídeos musicales para ti", "music videos for you").any(label::contains) -> ExploreShelfCategory.VIDEOS_FOR_YOU
        listOf("vídeo", "video", "music videos", "videos musicales").any(label::contains) -> ExploreShelfCategory.MUSIC_VIDEOS
        else -> null
    }
}

private fun HomeShelf.isExploreExcluded(): Boolean {
    val label = title.trim().lowercase()
    return label in setOf(
        "recents", "recientes", "recently played", "recently played songs", "vuelve a escucharlo", "listen again",
        "favoritos olvidados", "forgotten favorites", "mixes personalizados", "mix personalizado", "mi mix",
        "your mixes", "personalized mixes", "vídeos musicales nuevos", "videos musicales nuevos", "new music videos",
    )
}

private fun HomeShelf.isReleaseShelf(): Boolean {
    val label = title.lowercase()
    return listOf("release", "álbum", "album", "single").any(label::contains)
}
