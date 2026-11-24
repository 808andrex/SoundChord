package com.music.bitchord.ui.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.TransformOrigin
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.LibrarySort
import com.music.bitchord.data.settings.LibraryViewType
import com.music.bitchord.data.settings.LocalMusicSort
import com.music.bitchord.data.settings.SongSort
import com.music.bitchord.ui.icons.BitChordIcons
import com.music.bitchord.ui.components.optimizedHazeEffect
import com.music.bitchord.ui.screens.localizedLabel
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.zIndex
import androidx.media3.session.MediaController
import com.music.bitchord.DETAIL_TITLE_DROP
import com.music.bitchord.TAB_EXPLORE
import com.music.bitchord.TAB_HOME
import com.music.bitchord.TAB_LIBRARY
import com.music.bitchord.TAB_RADIO
import com.music.bitchord.TAB_SEARCH
import com.music.bitchord.TOP_FADE_RAMP
import com.music.bitchord.TOP_FADE_START
import com.soundchord.features.recognition.data.MusicRecognitionService
import com.music.bitchord.data.AppUpdateChecker
import com.music.bitchord.data.listentogether.ServerConnectionState
import com.music.bitchord.data.model.Account
import com.music.bitchord.ui.components.BrowseTarget
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.DetailPage
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.MoodGenre
import com.music.bitchord.ui.SearchSource
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UiState
import com.music.bitchord.ui.components.QueueActionNotice
import com.music.bitchord.sharedui.resources.Res as SharedRes
import com.music.bitchord.sharedui.resources.search_addons_hint
import com.music.bitchord.sharedui.resources.search_hint
import com.music.bitchord.sharedui.resources.search_library_hint
import com.music.bitchord.ui.MainViewModel
import com.music.bitchord.ui.components.BottomFadeScrim
import com.music.bitchord.ui.player.BottomPlayerBar
import com.music.bitchord.ui.components.BottomTab
import com.music.bitchord.ui.components.FloatingBarsTapGuard
import com.music.bitchord.ui.components.floatingtabbar.FloatingTabBarScrollConnection
import com.music.bitchord.ui.components.FrostedTopBar
import com.soundchord.features.recognition.RecognitionFab
import com.music.bitchord.ui.components.SearchField
import com.music.bitchord.ui.components.TopBarAccountButton
import com.music.bitchord.ui.components.TopBarDownloadButton
import com.music.bitchord.ui.components.TopFadeBlur
import com.music.bitchord.ui.components.menuAnchor
import com.music.bitchord.ui.navigation.AppNavigator
import com.music.bitchord.ui.navigation.AppRoute
import com.music.bitchord.ui.player.PlayerDock
import com.music.bitchord.ui.player.PlayerSheetMotion
import com.music.bitchord.ui.player.rememberBottomPlayerBarActions
import com.music.bitchord.ui.player.rememberBottomPlayerBarState
import com.music.bitchord.ui.theme.ArtworkPalette
import com.music.bitchord.ui.screens.sortedForDetail
import com.soundchord.features.icons.SoundChordIcons

internal fun tween(durationMillis: Int) =
    androidx.compose.animation.core.tween<Float>(durationMillis)

@Composable
internal fun LibrarySort.localizedLabel(): String = when (this) {
    LibrarySort.DEFAULT -> stringResource(R.string.sort_default)
    LibrarySort.TITLE_ASC -> stringResource(R.string.sort_title_ascending)
    LibrarySort.TITLE_DESC -> stringResource(R.string.sort_title_descending)
}

@Composable
internal fun SongSort.localizedLabel(): String = when (this) {
    SongSort.DEFAULT -> stringResource(R.string.sort_default)
    SongSort.TRACK_NUMBER -> stringResource(R.string.sort_track_number)
    SongSort.TITLE_ASC -> stringResource(R.string.sort_title_ascending)
    SongSort.TITLE_DESC -> stringResource(R.string.sort_title_descending)
    SongSort.DATE_ADDED_ASC -> stringResource(R.string.sort_date_added_oldest)
    SongSort.DATE_ADDED_DESC -> stringResource(R.string.sort_date_added)
}

@Composable
internal fun FrostedRadioFavoritesSortMenu(
    hazeState: HazeState,
    selected: LibrarySort,
    onSelect: (LibrarySort) -> Unit,
    onDismiss: () -> Unit,
) {
    FrostedMenuPanel(hazeState = hazeState, onDismiss = onDismiss) {
        listOf(LibrarySort.DEFAULT, LibrarySort.TITLE_ASC, LibrarySort.TITLE_DESC).forEach { option ->
            FrostedMenuRow(
                label = option.localizedLabel(),
                checked = option == selected,
                onClick = { onSelect(option) },
            )
        }
    }
}

/**
 * The track-list sort menu, styled after the account switcher: a full-screen
 * scrim to catch the dismissal tap, and the options on a frosted panel that
 * blurs the page behind it. Composed here in the main hierarchy rather than
 * as a popup window — which is exactly what lets the haze see the content it
 * is blurring.
 */
@Composable
internal fun FrostedSortMenu(
    hazeState: HazeState,
    selected: SongSort,
    offerTrackNumber: Boolean = false,
    onSelect: (SongSort) -> Unit,
    onFlipDateDirection: () -> Unit,
    onDismiss: () -> Unit,
) {
    FrostedMenuPanel(hazeState = hazeState, onDismiss = onDismiss) {
        val dateActive = selected == SongSort.DATE_ADDED_ASC ||
            selected == SongSort.DATE_ADDED_DESC
        val arrowRotation by animateFloatAsState(
            targetValue = if (selected == SongSort.DATE_ADDED_ASC) 180f else 0f,
            animationSpec = tween(durationMillis = 200),
            label = "dateAddedArrow",
        )
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button) { onFlipDateDirection() }
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.sort_date_added_toggle),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            if (dateActive) {
                Icon(
                    Icons.Rounded.ArrowUpward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.rotate(arrowRotation),
                )
            }
        }
        SongSort.entries
            .filter { it != SongSort.DATE_ADDED_ASC && it != SongSort.DATE_ADDED_DESC }
            .filter { offerTrackNumber || it != SongSort.TRACK_NUMBER }
            .forEach { option ->
                FrostedMenuRow(
                    label = option.localizedLabel(),
                    checked = option == selected,
                    onClick = { onSelect(option) },
                )
            }
    }
}

/**
 * A device folder's menu, off the same button: list or grid for its tabs, then
 * its sort orders — the Files app's arrangement, view first and order under it.
 */
@Composable
internal fun FrostedFolderMenu(
    hazeState: HazeState,
    sort: LocalMusicSort,
    viewType: LibraryViewType,
    onSort: (LocalMusicSort) -> Unit,
    onToggleViewType: () -> Unit,
    onDismiss: () -> Unit,
) {
    FrostedMenuPanel(hazeState = hazeState, onDismiss = onDismiss) {
        val grid = viewType == LibraryViewType.GRID
        FrostedMenuRow(
            label = stringResource(if (grid) R.string.switch_to_list_view else R.string.switch_to_grid_view),
            checked = false,
            onClick = onToggleViewType,
            icon = if (grid) BitChordIcons.ListView else BitChordIcons.GridView,
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )
        LocalMusicSort.entries.forEach { option ->
            FrostedMenuRow(
                label = option.localizedLabel(),
                checked = option == sort,
                onClick = { onSort(option) },
            )
        }
    }
}

/** The scrim and frosted panel both top-bar menus are drawn on. */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
internal fun FrostedMenuPanel(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val reduceMotion by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val shape = MaterialTheme.shapes.extraLarge
    val scrimColor = MaterialTheme.colorScheme.scrim

    val pop = remember { Animatable(if (reduceMotion) 1f else 0.35f) }
    val fade = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            fade.animateTo(1f, tween(120))
            pop.snapTo(1f)
        } else {
            launch { fade.animateTo(1f, tween(160, easing = FastOutSlowInEasing)) }
            launch { pop.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 380f)) }
        }
    }

    val close: () -> Unit = {
        if (!closing) {
            closing = true
            scope.launch {
                if (reduceMotion) {
                    fade.animateTo(0f, tween(100))
                } else {
                    launch { fade.animateTo(0f, tween(180, easing = FastOutLinearInEasing)) }
                    pop.animateTo(0.6f, tween(200, easing = FastOutSlowInEasing))
                }
                onDismiss()
            }
        }
    }

    BackHandler(enabled = !closing, onBack = close)

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind { drawRect(scrimColor.copy(alpha = 0.48f * fade.value.coerceIn(0f, 1f))) }
            .clickable(onClick = close),
        contentAlignment = Alignment.TopEnd,
    ) {
        Surface(
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = shape,
            modifier = Modifier
                .padding(top = 56.dp, end = 20.dp)
                .width(IntrinsicSize.Max)
                .graphicsLayer {
                    val s = if (reduceMotion) 1f else pop.value.coerceAtLeast(0f)
                    scaleX = s
                    scaleY = s
                    alpha = fade.value.coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(1f, 0f)
                }
                .clip(shape)
                .then(
                    if (reduceDynamicBlur) {
                        Modifier.background(MaterialTheme.colorScheme.surface)
                    } else {
                        Modifier.optimizedHazeEffect(
                            state = hazeState,
                            style = HazeMaterials.thin(MaterialTheme.colorScheme.surface),
                        )
                    },
                )
                .clickable(onClick = {}),
        ) {
            Column(Modifier.padding(vertical = 8.dp), content = content)
        }
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
internal fun historyDialogSurface(hazeState: HazeState, reduceDynamicBlur: Boolean): Modifier {
    val shape = RoundedCornerShape(14.dp)
    return Modifier
        .clip(shape)
        .then(
            if (reduceDynamicBlur) {
                Modifier.background(MaterialTheme.colorScheme.surface)
            } else {
                Modifier.hazeEffect(
                    state = hazeState,
                    style = HazeMaterials.regular(MaterialTheme.colorScheme.surface),
                )
            },
        )
}

/** One option on a frosted menu: its label, and a tick or [icon] at the end. */
@Composable
internal fun FrostedMenuRow(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (checked) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        } else if (icon != null) {
            Spacer(Modifier.width(16.dp))
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * [content] in a recompose scope of its own, so state read inside it
 * recomposes it alone rather than everything it is written in.
 */
@Composable
internal fun Isolated(content: @Composable () -> Unit) = content()

@OptIn(ExperimentalMaterial3Api::class)
@Immutable
internal data class BitChordChromeState(
    val detail: DetailPage?,
    val isLocalDetail: Boolean,
    val isReleaseDetail: Boolean,
    val detailPalette: ArtworkPalette,
    val glassActive: Boolean,
    val progressiveBlur: Boolean,
    val reduceDynamicBlur: Boolean,
    val hazeState: HazeState,
    val detailListStates: MutableMap<String, LazyListState>,
    val homeListState: LazyListState,
    val route: AppRoute?,
    val selectedTab: Int,
    val tabs: List<BottomTab>,
    val recognitionOpen: Boolean,
    val detailActiveShelf: HomeShelf?,
    val selectedMoodGenre: MoodGenre?,
    val replayScrolled: Boolean,
    val detailScrolled: Boolean,
    val scrolled: Boolean,
    val currentFeed: MainViewModel.Feed?,
    val refreshing: Set<Any?>,
    val currentPull: PullToRefreshState?,
    val radioSubpageBack: (() -> Unit)?,
    val searchFocusRequested: Boolean,
    val query: String,
    val searchSource: SearchSource,
    val partyServerStatus: ServerConnectionState,
    val updateNotice: AppUpdateChecker.UpdateInfo?,
    val signedIn: Boolean,
    val recognitionHistory: List<Any>,
    val radioHistory: List<Any>,
    val account: Account?,
    val detailSearching: Boolean,
    val recognitionFabVisible: Boolean,
    val playerSong: Song?,
    val isPlaying: Boolean,
    val playPauseBusy: Boolean,
    val controlsLocked: Boolean,
    val reducePlayerMotion: Boolean,
    val queueNotice: QueueActionNotice?,
    val navBarScroll: FloatingTabBarScrollConnection,
    val activeDock: PlayerDock?,
    val playerSheetMotion: PlayerSheetMotion,
    val librarySort: LibrarySort,
    val songSort: SongSort,
    val navigator: AppNavigator,
)

@Immutable
internal data class BitChordChromeActions(
    val onGoBack: () -> Unit,
    val onResetRecognition: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onSubmitSearch: () -> Unit,
    val onSearchFocusHandled: () -> Unit,
    val onShowUpdateDialog: () -> Unit,
    val onPushRoute: (AppRoute) -> Unit,
    val onLoadHistory: () -> Unit,
    val onSetLibrarySort: (LibrarySort) -> Unit,
    val onToggleDetailSearching: () -> Unit,
    val onOpenFolderMenu: () -> Unit,
    val onOpenSongSortMenu: () -> Unit,
    val onOpenBrowseMenu: (BrowseTarget) -> Unit,
    val onDownloadIdFor: (String?) -> String?,
    val onShareArtist: (DetailPage) -> Unit,
    val onShowRecognitionHistoryClearConfirmation: () -> Unit,
    val onShowRadioHistoryClearConfirmation: () -> Unit,
    val onAccountClick: () -> Unit,
    val onSwipeProfile: (Boolean) -> Boolean,
    val onRecognitionOpen: () -> Unit,
    val onTabSelected: (Int) -> Unit,
    val onTogglePlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onExpandPlayer: () -> Unit,
    val onShowHostOnlyNotice: () -> Unit,
    val onActiveTabTap: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BitChordChrome(
    state: BitChordChromeState,
    actions: BitChordChromeActions,
    modifier: Modifier = Modifier,
) {
    var librarySortMenuOpen by remember { mutableStateOf(false) }
    val searchFieldFocus = remember { FocusRequester() }

    val isDetailVisible = state.detail != null &&
        (state.detail.type == BrowseType.ALBUM ||
            state.detail.type == BrowseType.PLAYLIST ||
            state.detail.type == BrowseType.ARTIST) &&
        !state.isLocalDetail
    val chromePageColor = if (isDetailVisible) {
        state.detailPalette.background
    } else {
        MaterialTheme.colorScheme.background
    }

    val artistListState = state.detail
        ?.takeIf { isDetailVisible && it.type == BrowseType.ARTIST }
        ?.let { state.detailListStates.getOrPut(it.browseId) { LazyListState() } }
    val homeHeadingVisible = state.route == null && state.detail == null && state.selectedTab == TAB_HOME
    val headerListState = artistListState ?: state.homeListState.takeIf { homeHeadingVisible }
    val fadeStart = if (artistListState != null) TOP_FADE_START else 0f
    val fadeRamp = if (artistListState != null) TOP_FADE_RAMP else 1f
    val topFadeAlpha by remember(headerListState, fadeStart, fadeRamp) {
        derivedStateOf {
            val list = headerListState ?: return@derivedStateOf 1f
            if (list.firstVisibleItemIndex > 0) return@derivedStateOf 1f
            val header = list.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == 0 }?.size?.toFloat()
                ?: return@derivedStateOf 0f
            ((list.firstVisibleItemScrollOffset - header * fadeStart) /
                (header * fadeRamp)).coerceIn(0f, 1f)
        }
    }

    Box(modifier) {
        if (state.reduceDynamicBlur || !state.glassActive || !state.progressiveBlur) {
            BottomFadeScrim(
                pageColor = chromePageColor,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .rotate(180f)
                    .graphicsLayer { alpha = topFadeAlpha },
            )
        } else {
            TopFadeBlur(
                hazeState = state.hazeState,
                pageColor = chromePageColor,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer { alpha = topFadeAlpha },
            )
        }

        FrostedTopBar(
            title = if (state.recognitionOpen) {
                stringResource(com.soundchord.features.R.string.recognition_title)
            } else when (val r = state.route) {
                AppRoute.Spotify -> stringResource(R.string.spotify)
                AppRoute.Downloads -> stringResource(R.string.downloads)
                AppRoute.Discord -> "Discord"
                AppRoute.History -> stringResource(R.string.history)
                AppRoute.RecognitionHistory -> stringResource(R.string.recognition_history_title)
                AppRoute.RadioHistory -> stringResource(com.soundchord.features.R.string.radio_history_title)
                AppRoute.RadioFavorites -> stringResource(com.soundchord.features.R.string.radio_favourites)
                is AppRoute.LibraryShowAll -> r.shelf.title
                is AppRoute.ArtistShelf -> r.shelf.title
                AppRoute.Moods -> stringResource(R.string.find_your_mood)
                AppRoute.AccountScrobbling -> stringResource(R.string.account_scrobbling)
                AppRoute.Sources -> stringResource(R.string.sources)
                AppRoute.ListenTogether -> stringResource(R.string.listen_together)
                AppRoute.WearOs -> stringResource(R.string.wear_os_page_title)
                AppRoute.DriveBackup -> stringResource(R.string.drive_backup_title)
                AppRoute.DriveDevices -> stringResource(R.string.drive_devices_title)
                AppRoute.Equalizer -> stringResource(R.string.equalizer)
                AppRoute.Settings -> stringResource(R.string.settings)
                AppRoute.About -> stringResource(R.string.settings_category_about)
                is AppRoute.SettingsSection -> stringResource(r.category.title)
                is AppRoute.SettingsChoice -> stringResource(r.choice.title)
                AppRoute.EqualizerPresets -> stringResource(R.string.equalizer_preset)
                AppRoute.Replay -> stringResource(R.string.replay)
                null -> when {
                    state.detail != null && state.detailActiveShelf != null -> state.detailActiveShelf.title
                    state.detail != null -> state.detail.title
                    state.selectedMoodGenre != null -> state.selectedMoodGenre.title
                    else -> state.tabs[state.selectedTab].let {
                        if (it.label == "Play") stringResource(R.string.listen_now) else it.label
                    }
                }
            },
            transparentBackdrop = true,
            opaqueBackdrop = state.route == AppRoute.Settings || state.route == AppRoute.About || state.route is AppRoute.SettingsSection,
            artworkPageChrome = true,
            backButtonHazeState = state.hazeState,
            centeredTitle = state.recognitionOpen || state.route == AppRoute.History ||
                state.route == AppRoute.RecognitionHistory || state.route == AppRoute.RadioHistory,
            trailingTitle = if (state.detail != null && state.detailActiveShelf != null) state.detail.title else null,
            scrolled = {
                when {
                    state.recognitionOpen -> true
                    state.route == AppRoute.Replay -> state.replayScrolled
                    state.route != null -> true
                    state.detail != null && state.detailActiveShelf != null -> true
                    state.detail != null -> state.detailScrolled
                    state.selectedMoodGenre != null -> true
                    else -> state.scrolled || state.selectedTab == TAB_SEARCH
                }
            },
            refreshing = state.currentFeed != null && state.currentFeed in state.refreshing,
            pullFraction = { state.currentPull?.distanceFraction ?: 0f },
            onBack = when {
                state.recognitionOpen -> ({ actions.onResetRecognition() })
                state.selectedTab == TAB_RADIO && state.route == null && state.radioSubpageBack != null -> state.radioSubpageBack
                state.route != null || state.detail != null || state.selectedMoodGenre != null -> actions.onGoBack
                else -> null
            },
            modifier = Modifier.align(Alignment.TopCenter).zIndex(1f),
            accessory = if (
                state.selectedTab == TAB_SEARCH && state.route == null && state.detail == null && state.selectedMoodGenre == null
            ) {
                {
                    val keyboard = LocalSoftwareKeyboardController.current
                    LaunchedEffect(state.searchFocusRequested) {
                        if (state.searchFocusRequested) {
                            searchFieldFocus.requestFocus()
                            keyboard?.show()
                            actions.onSearchFocusHandled()
                        }
                    }
                    SearchField(
                        query = state.query,
                        onQueryChange = actions.onQueryChange,
                        onSubmit = actions.onSubmitSearch,
                        focusRequester = searchFieldFocus,
                        placeholder = org.jetbrains.compose.resources.stringResource(
                            if (state.searchSource == SearchSource.LIBRARY) {
                                SharedRes.string.search_library_hint
                            } else if (state.searchSource == SearchSource.ADDONS) {
                                SharedRes.string.search_addons_hint
                            } else {
                                SharedRes.string.search_hint
                            },
                        ),
                    )
                }
            } else null,
            actions = {
                if (state.route == AppRoute.ListenTogether) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        when (val serverState = state.partyServerStatus) {
                            is ServerConnectionState.CustomFallback -> {
                                Icon(
                                    Icons.Rounded.CloudOff,
                                    contentDescription = stringResource(R.string.listen_together_top_bar_fallback, stringResource(R.string.listen_together_ping, serverState.latencyMs)),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.listen_together_ping, serverState.latencyMs),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 1,
                                )
                            }
                            is ServerConnectionState.CustomOnline -> {
                                Text(
                                    text = stringResource(R.string.listen_together_ping, serverState.latencyMs),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                            }
                            is ServerConnectionState.DefaultOnline -> {
                                Text(
                                    text = stringResource(R.string.listen_together_ping, serverState.latencyMs),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                            }
                            ServerConnectionState.Checking -> {
                                Text(
                                    text = stringResource(R.string.listen_together_server_checking),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                            ServerConnectionState.Offline -> {
                                Text(
                                    text = stringResource(R.string.listen_together_server_offline_dash),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
                if (!state.recognitionOpen && state.route == null && state.detail == null && state.selectedTab == TAB_HOME) {
                    state.updateNotice?.let { update ->
                        IconButton(onClick = actions.onShowUpdateDialog) {
                            Icon(
                                Icons.Rounded.Upgrade,
                                contentDescription = stringResource(R.string.update_available, update.version),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                if (!(state.route != null && AppRoute.Settings in state.navigator)) {
                    if (state.recognitionOpen) {
                        IconButton(
                            onClick = {
                                actions.onResetRecognition()
                                actions.onPushRoute(AppRoute.RecognitionHistory)
                            },
                        ) {
                            Icon(
                                Icons.Rounded.History,
                                contentDescription = stringResource(R.string.recognition_history_title),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    if (state.route == null && state.detail == null && state.selectedTab == TAB_LIBRARY) {
                        IconButton(
                            onClick = {
                                actions.onPushRoute(AppRoute.History)
                                actions.onLoadHistory()
                            },
                        ) {
                            Icon(
                                Icons.Rounded.History,
                                contentDescription = stringResource(R.string.listening_history),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    if (state.route is AppRoute.LibraryShowAll) {
                        Box {
                            IconButton(onClick = { librarySortMenuOpen = true }) {
                                Icon(
                                    Icons.Rounded.Sort,
                                    contentDescription = stringResource(R.string.sort_library),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            DropdownMenu(
                                expanded = librarySortMenuOpen,
                                onDismissRequest = { librarySortMenuOpen = false },
                            ) {
                                LibrarySort.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.localizedLabel()) },
                                        trailingIcon = if (option == state.librarySort) {
                                            { Icon(Icons.Rounded.Check, contentDescription = null) }
                                        } else null,
                                        onClick = {
                                            actions.onSetLibrarySort(option)
                                            librarySortMenuOpen = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                    if (state.route == AppRoute.RadioFavorites) {
                        IconButton(onClick = actions.onToggleDetailSearching) {
                            Icon(
                                if (state.detailSearching) Icons.Rounded.Close else BitChordIcons.Search,
                                contentDescription = stringResource(
                                    if (state.detailSearching) R.string.close_search else R.string.search_this_list,
                                ),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(onClick = actions.onOpenFolderMenu) {
                            Icon(
                                Icons.Rounded.Sort,
                                contentDescription = stringResource(com.soundchord.features.R.string.radio_sort_stations),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    if ((state.isReleaseDetail || state.isLocalDetail) && state.detail != null) {
                        val releaseSongs = (state.detail.songs as? UiState.Success)?.data.orEmpty()
                        if (releaseSongs.isNotEmpty() || (state.detail.type == BrowseType.PLAYLIST && state.signedIn)) {
                            IconButton(onClick = actions.onToggleDetailSearching) {
                                Icon(
                                    if (state.detailSearching) Icons.Rounded.Close else BitChordIcons.Search,
                                    contentDescription = stringResource(
                                        if (state.detailSearching) R.string.close_search else R.string.search_this_list,
                                    ),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        IconButton(
                            onClick = { if (state.isLocalDetail) actions.onOpenFolderMenu() else actions.onOpenSongSortMenu() },
                        ) {
                            Icon(
                                Icons.Rounded.Sort,
                                contentDescription = stringResource(
                                    if (state.isLocalDetail) R.string.sort_music else R.string.sort_songs,
                                ),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        if (state.isReleaseDetail && (releaseSongs.isNotEmpty() || state.detail.type == BrowseType.PLAYLIST)) {
                            IconButton(
                                modifier = Modifier.menuAnchor(),
                                onClick = {
                                    actions.onOpenBrowseMenu(BrowseTarget(
                                        browseId = state.detail.browseId,
                                        title = state.detail.title,
                                        subtitle = state.detail.subtitle,
                                        thumbnailUrl = state.detail.thumbnailUrl,
                                        type = state.detail.type,
                                        songs = releaseSongs.sortedForDetail(state.songSort).map { song ->
                                            if (state.detail.type == BrowseType.ALBUM) {
                                                song.copy(albumName = song.albumName ?: state.detail.title)
                                            } else {
                                                song
                                            }
                                        },
                                        fromCard = false,
                                        downloadId = actions.onDownloadIdFor(state.detail.browseId),
                                    ))
                                },
                            ) {
                                Icon(
                                    Icons.Rounded.MoreHoriz,
                                    contentDescription = stringResource(R.string.more),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                    if (state.detail != null && !state.isLocalDetail && state.detail.type == BrowseType.ARTIST &&
                        state.detailActiveShelf == null && state.detail.browseId.startsWith("UC")
                    ) {
                        IconButton(
                            onClick = { actions.onShareArtist(state.detail) },
                        ) {
                            Icon(
                                Icons.Rounded.IosShare,
                                contentDescription = stringResource(R.string.share),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    if (state.route == AppRoute.RecognitionHistory) {
                        IconButton(
                            onClick = actions.onShowRecognitionHistoryClearConfirmation,
                            enabled = state.recognitionHistory.isNotEmpty(),
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    if (state.route == AppRoute.RadioHistory) {
                        IconButton(
                            onClick = actions.onShowRadioHistoryClearConfirmation,
                            enabled = state.radioHistory.isNotEmpty(),
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = stringResource(com.soundchord.features.R.string.radio_history_clear),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    if (!state.recognitionOpen && state.route == null && state.detail == null && state.selectedTab == TAB_RADIO) {
                        IconButton(onClick = { actions.onPushRoute(AppRoute.RadioHistory) }) {
                            Icon(Icons.Rounded.History, contentDescription = stringResource(com.soundchord.features.R.string.radio_history_title), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    if (state.route != AppRoute.Downloads && !state.recognitionOpen) {
                        TopBarDownloadButton(onClick = { actions.onPushRoute(AppRoute.Downloads) })
                    }
                    if (!state.isReleaseDetail && state.route != AppRoute.RecognitionHistory && state.route != AppRoute.RadioHistory) {
                        TopBarAccountButton(
                            account = state.account,
                            onClick = actions.onAccountClick,
                            onSwipeProfile = actions.onSwipeProfile,
                        )
                    }
                }
            },
        )

        val floatingBarsHeight = remember { mutableIntStateOf(0) }
        val recognitionFabBottom = with(LocalDensity.current) {
            floatingBarsHeight.intValue.toDp() + 14.dp
        }

        if (!state.recognitionOpen && state.route == null && state.detail == null && state.selectedMoodGenre == null && state.selectedTab == TAB_HOME) {
            RecognitionFab(
                visible = state.recognitionFabVisible,
                onClick = actions.onRecognitionOpen,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .zIndex(2f)
                    .padding(end = 24.dp, bottom = recognitionFabBottom),
            )
        }

        BottomFadeScrim(
            withMiniPlayer = state.playerSong != null,
            pageColor = chromePageColor,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        FloatingBarsTapGuard(
            barsHeight = { floatingBarsHeight.intValue },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        BottomPlayerBar(
            state = rememberBottomPlayerBarState(
                glassActive = state.glassActive,
                queueNotice = state.queueNotice,
                tabs = state.tabs,
                selectedIndex = state.selectedTab,
                song = state.playerSong,
                isPlaying = state.isPlaying,
                isLoading = state.playPauseBusy,
                controlsLocked = state.controlsLocked,
                reducePlayerMotion = state.reducePlayerMotion,
            ),
            actions = rememberBottomPlayerBarActions(
                onTabSelected = actions.onTabSelected,
                onPlayPause = actions.onTogglePlayPause,
                onNext = actions.onNext,
                onPrevious = actions.onPrevious,
                onExpand = actions.onExpandPlayer,
                onBlockedControl = actions.onShowHostOnlyNotice,
                onHeightChanged = { floatingBarsHeight.intValue = it },
                onActiveTabTap = actions.onActiveTabTap,
            ),
            navBarScroll = state.navBarScroll,
            dock = state.activeDock,
            pull = state.playerSheetMotion.takeIf { !state.reducePlayerMotion },
            hazeState = state.hazeState,
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(1f),
        )
    }
}

