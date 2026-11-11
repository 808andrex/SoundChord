package com.music.bitchord.ui.tv.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.MoodGenre
import com.music.bitchord.data.model.MoodGenreSection
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.ShelfLayout
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UiState
import com.music.bitchord.playback.playSongs
import com.music.bitchord.ui.MainViewModel
import com.music.bitchord.ui.tv.components.TvActivityIndicator
import com.music.bitchord.ui.tv.components.TvButton
import com.music.bitchord.ui.tv.components.TvChromeScrollEffect
import com.music.bitchord.ui.tv.components.TvErrorState
import com.music.bitchord.ui.tv.components.TvMoodCard
import com.music.bitchord.ui.tv.components.TvShelf
import com.music.bitchord.ui.tv.components.TvShelfPlaceholder
import com.music.bitchord.ui.tv.components.TvLockup
import com.music.bitchord.ui.tv.components.tvInitialFocus
import com.music.bitchord.ui.tv.dialogs.TvSongMenu
import com.music.bitchord.ui.tv.theme.TvDimensions
import com.music.bitchord.ui.tv.theme.TvGlass
import com.music.bitchord.ui.tv.theme.TvType
import com.soundchord.features.R as FeaturesR
import com.soundchord.features.explore.ExploreShelfCategory
import com.soundchord.features.explore.exploreCategory
import com.soundchord.features.explore.exploreContentShelves
import com.soundchord.features.explore.isReleaseShelf
import kotlinx.coroutines.launch

/** Explore reuses the feature feed and category state with TV shelves and D-pad focus. */
@Composable
fun TvExploreScreen(
    viewModel: MainViewModel,
    mediaController: MediaController?,
    onNavigateToDetail: (browseId: String, title: String, subtitle: String, thumbnailUrl: String?, type: BrowseType) -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected by viewModel.selectedMoodGenre.collectAsState()
    val moodState by viewModel.explore.collectAsState()
    val homeState by viewModel.home.collectAsState()
    val loadingMore by viewModel.homeLoadingMore.collectAsState()
    LaunchedEffect(viewModel) { viewModel.onExploreShown() }
    val selectedMood = selected

    val scope = rememberCoroutineScope()
    var menuSong by remember { mutableStateOf<Song?>(null) }
    val playSong: (Song) -> Unit = { song ->
        scope.launch { mediaController?.playSongs(listOf(song), 0) }
        onNavigateToNowPlaying()
    }
    val shelves = (homeState as? UiState.Success)?.data.orEmpty().exploreContentShelves()

    if (selectedMood == null) {
        TvExploreFeed(
            moodState = moodState,
            homeState = homeState,
            shelves = shelves,
            loadingMore = loadingMore,
            onCategoryClick = viewModel::openMoodGenre,
            onShelfItemClick = { _, item ->
                openShelfItem(item, playSong, onNavigateToDetail)
            },
            onShelfMore = { shelf ->
                shelf.moreBrowseId?.let { browseId ->
                    onNavigateToDetail(
                        browseId,
                        shelf.title,
                        shelf.subtitle,
                        shelf.items.firstOrNull()?.thumbnailUrl,
                        browseTypeOf(browseId),
                    )
                }
            },
            onRetryCategories = viewModel::loadExplore,
            onRetryShelves = viewModel::loadHome,
            onLoadMore = viewModel::loadMoreHome,
            modifier = modifier,
        )
    } else {
        BackHandler { viewModel.closeMoodGenre() }
        TvMoodPage(
            mood = selectedMood,
            viewModel = viewModel,
            mediaController = mediaController,
            onNavigateToDetail = onNavigateToDetail,
            onNavigateToNowPlaying = onNavigateToNowPlaying,
            modifier = modifier,
        )
    }

    menuSong?.let { song ->
        TvSongMenu(
            song = song,
            viewModel = viewModel,
            mediaController = mediaController,
            onNavigateToDetail = onNavigateToDetail,
            onDismiss = { menuSong = null },
        )
    }
}

@Composable
private fun TvExploreFeed(
    moodState: UiState<List<MoodGenreSection>>,
    homeState: UiState<List<HomeShelf>>,
    shelves: List<HomeShelf>,
    loadingMore: Boolean,
    onCategoryClick: (MoodGenre) -> Unit,
    onShelfItemClick: (HomeShelf, ShelfItem) -> Unit,
    onShelfMore: (HomeShelf) -> Unit,
    onRetryCategories: () -> Unit,
    onRetryShelves: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    TvChromeScrollEffect(listState)
    val nearEnd by remember(listState) {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd, shelves.size) { if (nearEnd) onLoadMore() }
    val featured = remember(shelves) {
        shelves.flatMap { shelf ->
            shelf.items.filter { it.browseId != null && it.thumbnailUrl != null }
                .map { shelf to it }
        }.distinctBy { (_, item) -> item.browseId }.take(8)
    }
    val groupedShelves = remember(shelves) {
        ExploreShelfCategory.entries.mapNotNull { category ->
            category to shelves.filter { it.exploreCategory() == category }
        }.filter { (_, matching) -> matching.isNotEmpty() }
    }
    val unclassified = remember(shelves) {
        shelves.filter { it.exploreCategory() == null && !it.isReleaseShelf() }
    }
    val hasMoodCategories = (moodState as? UiState.Success)?.data?.any { it.items.isNotEmpty() } == true

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = TvDimensions.ContentTop + 3.dp,
            bottom = 38.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (featured.isNotEmpty()) {
            item(key = "explore-featured") {
                TvShelf(
                    title = stringResource(FeaturesR.string.explore_featured),
                    items = featured,
                    key = { (_, item) -> "${item.browseId}:${item.title}" },
                ) { (shelf, item) ->
                    val index = featured.indexOfFirst { it.second.browseId == item.browseId }
                    TvLockup(
                        title = item.title,
                        subtitle = item.subtitle.ifBlank { shelf.title },
                        artworkUrl = item.thumbnailUrl,
                        width = 276.dp,
                        aspectRatio = 16f / 9f,
                        modifier = Modifier.tvInitialFocus(index == 0),
                        onClick = { onShelfItemClick(shelf, item) },
                    )
                }
            }
        }

        item(key = "explore-moods") {
            TvExploreMoodRail(
                state = moodState,
                initialFocus = featured.isEmpty(),
                onCategoryClick = onCategoryClick,
                onRetry = onRetryCategories,
            )
        }

        when (homeState) {
            UiState.Loading -> item(key = "explore-shelves-loading") {
                TvShelfPlaceholder(width = 164.dp, count = 6)
            }
            is UiState.Error -> item(key = "explore-shelves-error") {
                TvErrorState(
                    message = homeState.message,
                    onRetry = onRetryShelves,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
            is UiState.Success -> Unit
        }

        groupedShelves.forEach { (category, matchingShelves) ->
            val rowItems = matchingShelves.flatMap { shelf -> shelf.items.map { shelf to it } }
            val moreShelf = matchingShelves.firstOrNull { it.moreBrowseId != null }
            item(key = "explore-group-${category.name}") {
                TvExploreShelfRow(
                    title = stringResource(category.titleRes),
                    shelves = matchingShelves,
                    items = rowItems,
                    moreShelf = moreShelf,
                    onShelfItemClick = onShelfItemClick,
                    onShelfMore = onShelfMore,
                    initialFocus = featured.isEmpty() && !hasMoodCategories,
                )
            }
        }

        unclassified.forEach { shelf ->
            item(key = "explore-shelf-${shelf.title}") {
                TvExploreShelfRow(
                    title = shelf.title,
                    shelves = listOf(shelf),
                    items = shelf.items.map { shelf to it },
                    moreShelf = shelf.takeIf { it.moreBrowseId != null },
                    onShelfItemClick = onShelfItemClick,
                    onShelfMore = onShelfMore,
                    initialFocus = featured.isEmpty() && !hasMoodCategories && groupedShelves.isEmpty(),
                )
            }
        }

        if (loadingMore) {
            item(key = "explore-load-more") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.Center,
                ) { TvActivityIndicator() }
            }
        }
    }
}

@Composable
private fun TvExploreMoodRail(
    state: UiState<List<MoodGenreSection>>,
    initialFocus: Boolean,
    onCategoryClick: (MoodGenre) -> Unit,
    onRetry: () -> Unit,
) {
    val moods = (state as? UiState.Success)?.data
        ?.flatMap(MoodGenreSection::items)
        ?.distinctBy { it.browseId to it.params }
        .orEmpty()
    Column {
        TvShelf(
            title = stringResource(FeaturesR.string.explore_moods_genres),
            items = moods,
            key = { "${it.browseId}|${it.params}" },
        ) { mood ->
            val index = moods.indexOfFirst { it.browseId == mood.browseId && it.params == mood.params }
            TvMoodCard(
                item = mood,
                onClick = { onCategoryClick(mood) },
                modifier = Modifier.width(176.dp).tvInitialFocus(initialFocus && index == 0),
            )
        }
        when (state) {
            UiState.Loading -> Row(
                modifier = Modifier.padding(start = TvDimensions.SafeMarginHorizontal, top = 6.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) { TvActivityIndicator() }
            is UiState.Error -> Row(
                modifier = Modifier.padding(horizontal = TvDimensions.SafeMarginHorizontal, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(text = state.message, color = TvGlass.TextSecondary, style = TvType.Body)
                TvButton(text = stringResource(FeaturesR.string.explore_retry), onClick = onRetry)
            }
            is UiState.Success -> if (moods.isEmpty()) {
                Text(
                    text = stringResource(FeaturesR.string.explore_empty_moods),
                    style = TvType.Body,
                    color = TvGlass.TextSecondary,
                    modifier = Modifier.padding(horizontal = TvDimensions.SafeMarginHorizontal, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun TvExploreShelfRow(
    title: String,
    shelves: List<HomeShelf>,
    items: List<Pair<HomeShelf, ShelfItem>>,
    moreShelf: HomeShelf?,
    onShelfItemClick: (HomeShelf, ShelfItem) -> Unit,
    onShelfMore: (HomeShelf) -> Unit,
    initialFocus: Boolean,
) {
    if (items.isEmpty()) return
    TvShelf(
        title = title,
        subtitle = shelves.firstOrNull()?.subtitle?.takeIf { shelves.size == 1 && it.isNotBlank() },
        items = items,
        key = { (shelf, item) -> "${shelf.title}:${item.browseId}:${item.videoId}:${item.title}" },
        trailing = {
            moreShelf?.let { shelf ->
                TvButton(
                    text = stringResource(FeaturesR.string.explore_see_more),
                    onClick = { onShelfMore(shelf) },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        },
    ) { (shelf, item) ->
        val video = shelf.layout == ShelfLayout.VIDEO || item.isVideo
        val index = items.indexOfFirst { it.second.videoId == item.videoId && it.second.browseId == item.browseId }
        TvLockup(
            title = item.title,
            subtitle = item.subtitle,
            artworkUrl = item.thumbnailUrl,
            width = if (video) 220.dp else 163.dp,
            aspectRatio = if (video) 16f / 9f else 1f,
            modifier = Modifier.tvInitialFocus(initialFocus && index == 0),
            onClick = { onShelfItemClick(shelf, item) },
        )
    }
}

@Composable
private fun TvMoodPage(
    mood: MoodGenre,
    viewModel: MainViewModel,
    mediaController: MediaController?,
    onNavigateToDetail: (browseId: String, title: String, subtitle: String, thumbnailUrl: String?, type: BrowseType) -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shelves by viewModel.moodGenreShelves.collectAsState()
    val scope = rememberCoroutineScope()
    var menuSong by remember { mutableStateOf<Song?>(null) }

    when (val state = shelves) {
        UiState.Loading -> TvShelvesLoading(modifier)
        is UiState.Error -> TvErrorState(
            message = state.message,
            onRetry = { viewModel.openMoodGenre(mood) },
            modifier = modifier,
        )
        is UiState.Success -> TvShelfFeed(
            shelves = state.data,
            onPlaySong = { song ->
                scope.launch { mediaController?.playSongs(listOf(song), 0) }
                onNavigateToNowPlaying()
            },
            onSongMenu = { menuSong = it },
            onNavigateToDetail = onNavigateToDetail,
            plainCardStyle = true,
            header = {
                Text(
                    text = mood.title,
                    style = TvType.LargeTitle,
                    color = TvGlass.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(
                        start = TvDimensions.SafeMarginHorizontal,
                        end = TvDimensions.SafeMarginHorizontal,
                        bottom = 3.dp,
                    ),
                )
            },
            modifier = modifier,
        )
    }

    menuSong?.let { song ->
        TvSongMenu(
            song = song,
            viewModel = viewModel,
            mediaController = mediaController,
            onNavigateToDetail = onNavigateToDetail,
            onDismiss = { menuSong = null },
        )
    }
}
