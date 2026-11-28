@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.music.bitchord.ui.app

import com.music.bitchord.TAB_EXPLORE
import com.music.bitchord.TAB_HOME
import com.music.bitchord.TAB_LIBRARY
import com.music.bitchord.TAB_RADIO
import com.music.bitchord.TAB_SEARCH

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import com.music.bitchord.ui.components.LocalCoverTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import com.soundchord.features.editorial.data.AlbumRelatedRepository
import com.soundchord.features.daypart.data.DaypartRepository
import com.soundchord.features.daypart.data.DaypartDatabaseCache
import com.soundchord.features.stations.data.StationsForYouRepository
import com.soundchord.features.playlists.data.PlaylistsForYouRepository
import com.music.bitchord.data.TopPicks
import com.music.bitchord.ui.screens.NativeHomeShelvesHolder
import com.music.bitchord.data.model.ArtistRef
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music.bitchord.R
import com.music.bitchord.auth.WebSessionMode
import com.music.bitchord.data.backup.RemoteDevice
import com.music.bitchord.ui.screens.DriveBackupScreen
import com.music.bitchord.ui.screens.DriveDevicesScreen
import com.music.bitchord.data.innertube.InnertubeParser
import com.music.bitchord.data.OfflinePlaylists
import com.music.bitchord.data.sources.SourceRegistry
import com.music.bitchord.data.LocalGroup
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.model.Account
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.DetailPage
import com.music.bitchord.data.model.EntityType
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.LibraryPage
import com.music.bitchord.data.model.LibraryState
import com.music.bitchord.data.model.MoodGenre
import com.music.bitchord.data.model.MoodGenreSection
import com.music.bitchord.data.model.PlaybackSourceType
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchHistoryEntity
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.TopPick
import com.music.bitchord.data.model.TopPickKind
import com.music.bitchord.data.model.TopPickMood
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.UserPlaylist
import com.music.bitchord.data.scrobbling.LastFM
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.AudioQuality
import com.music.bitchord.data.settings.BitPerfectMode
import com.music.bitchord.data.settings.LibraryViewType
import com.music.bitchord.data.settings.LibrarySort
import com.music.bitchord.data.settings.LocalMusicSort
import com.music.bitchord.data.settings.SongSort
import com.music.bitchord.data.sources.SourceConfig
import com.music.bitchord.data.spotify.SPOTIFY_PAGE_PREFIX
import com.music.bitchord.download.Downloads
import com.music.bitchord.download.SavedCollection
import com.music.bitchord.playback.AudioCache
import com.music.bitchord.playback.PlayerState
import com.music.bitchord.playback.QueueCoordinator
import com.music.bitchord.playback.QueueShuffle
import com.music.bitchord.playback.QueueSource
import com.music.bitchord.playback.cast.CastController
import com.music.bitchord.playback.contextEntryKey
import com.music.bitchord.ui.screens.LocalFolderState
import com.music.bitchord.ui.screens.LocalFolderTab
import com.music.bitchord.ui.screens.LocalFolder
import com.music.bitchord.ui.components.LocalPlayingBarsMotion
import com.music.bitchord.ui.APPLE_MUSIC_PAGE_PREFIX
import com.music.bitchord.ui.MainViewModel
import com.music.bitchord.ui.SearchSource
import com.music.bitchord.ui.components.BrowseTarget
import com.music.bitchord.ui.components.DownloadManagerPage
import com.music.bitchord.ui.components.backdrop.backdrops.LayerBackdrop
import com.music.bitchord.ui.components.backdrop.backdrops.layerBackdrop
import com.music.bitchord.ui.components.ReorderPlaylistPage
import com.music.bitchord.ui.components.StationMenuTarget
import com.music.bitchord.ui.navigation.AppDestination
import com.music.bitchord.ui.navigation.AppNavigator
import com.music.bitchord.ui.navigation.AppRoute
import com.music.bitchord.ui.screens.RADIO_FAVORITES_BROWSE_ID
import com.music.bitchord.ui.navigation.RouteEntry
import com.music.bitchord.ui.player.AddSongsRequest
import com.music.bitchord.ui.replay.ReplayHeroCard
import com.music.bitchord.data.stats.ReplayPeriod
import com.music.bitchord.ui.replay.ReplayState
import com.music.bitchord.ui.replay.ReplayStoryPage
import com.music.bitchord.ui.replay.ReplayScreen
import com.music.bitchord.ui.screens.AboutScreen
import com.music.bitchord.ui.screens.AccountAndScrobblingScreen
import com.music.bitchord.ui.screens.CACHE_FOLDER_BROWSE_ID
import com.music.bitchord.ui.screens.DetailScreen
import com.music.bitchord.ui.screens.DiscordDialog
import com.music.bitchord.ui.screens.DiscordScreen
import com.music.bitchord.ui.screens.EqualizerPresetPage
import com.music.bitchord.ui.screens.EqualizerScreen
import com.soundchord.features.explore.ExploreScreen
import com.music.bitchord.ui.screens.HistoryScreen
import com.music.bitchord.ui.screens.HomeScreen
import com.music.bitchord.ui.screens.LibraryGridPage
import com.music.bitchord.ui.screens.LibraryLink
import com.music.bitchord.ui.screens.LibraryReplayEntry
import com.music.bitchord.ui.screens.LibraryScreen
import com.music.bitchord.ui.screens.ListenTogetherScreen
import com.music.bitchord.ui.screens.MoodGenrePlaylistsScreen
import com.music.bitchord.ui.screens.MoodGridPage
import com.soundchord.features.radio.ui.RadioHistoryScreen
import com.soundchord.features.radio.ui.RadioFavoritesScreen
import com.soundchord.features.radio.data.RadioHistoryStore
import com.soundchord.features.radio.playback.toSong
import com.music.bitchord.ui.screens.RecognitionHistoryScreen
import com.music.bitchord.ui.screens.SearchScreen
import com.music.bitchord.ui.screens.SegmentedControl
import com.music.bitchord.ui.screens.SettingsCategory
import com.music.bitchord.ui.screens.SettingsChoicePage
import com.music.bitchord.ui.screens.SettingsScreen
import com.music.bitchord.ui.screens.SourcesScreen
import com.music.bitchord.ui.screens.SpotifyLibraryScreen
import com.music.bitchord.ui.screens.WearOsScreen
import com.soundchord.features.radio.data.RadioFavoritesStore
import com.soundchord.features.radio.data.RadioStation
import com.soundchord.features.radio.ui.RadioScreen
import com.soundchord.features.recognition.RecognitionScreen
import com.soundchord.features.recognition.data.RecognitionHistoryEntry
import com.soundchord.features.recognition.data.RecognitionHistoryStore
import com.soundchord.features.recognition.model.RecognitionResult
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch



private fun String?.isDeviceFolder(): Boolean =
    this != null && startsWith("local:") &&
        !startsWith(Downloads.PLAYLIST_PREFIX) && !startsWith(com.music.bitchord.data.LocalGroups.PREFIX) &&
        !startsWith(OfflinePlaylists.PREFIX)

@Immutable
data class AppNavigationScreenStates(
    val selectedTab: Int,
    val selectedMoodGenre: MoodGenre?,
    val visibleEntry: RouteEntry?,
    val detail: DetailPage?,
    val detailStack: List<DetailPage>,
    val signedIn: Boolean,
    val account: Account?,
    val selectedChannelName: String?,
    val player: PlayerState,
    val listPadding: PaddingValues,
    val artworkMotion: Boolean,
    val windowWidth: Dp,
    val radioSubpageBack: (() -> Unit)?,
    val recognitionOpen: Boolean,
    val showDiwali: Boolean,
    val diwaliFestive: Boolean,
    val searchSource: SearchSource,
    val searchFocusRequested: Boolean,
    val searchScrollReset: Int,
    val query: String,
    val filter: SearchFilter,
    val results: UiState<List<SearchResult>>?,
    val searchLoadingMore: Boolean,
    val searchHistory: List<SearchHistoryEntity>,
    val searchSuggestions: List<String>,
    val libraryResults: UiState<List<Song>>?,
    val addonResults: UiState<List<Song>>?,
    val addonCeiling: AudioQuality,
    val releaseLibrary: Map<String, LibraryState>,
    val songSort: SongSort,
    val savedCollections: Map<String, SavedCollection>,
    val downloadedReleases: List<SavedCollection>,
    val offlinePlaylistGroups: List<LocalGroup>,
    val localPlaylistItems: List<ShelfItem>,
    val webdavUrl: String,
    val activeJamInviteCode: String?,
    val activeJamInviteServer: String?,
    val homeState: UiState<List<HomeShelf>>,
    val visibleHomeState: UiState<List<HomeShelf>>,
    val homeLoadingMore: Boolean,
    val homeRecentlyPlayedLoading: Boolean,
    val hideRecents: Boolean,
    val topPicks: List<TopPick>?,
    val topPicksLoading: Boolean,
    val hideTopPicks: Boolean,
    val hideHomeMoods: Boolean,
    val exploreState: UiState<List<MoodGenreSection>>,
    val moodGenreShelves: UiState<List<HomeShelf>>,
    val libraryState: UiState<LibraryPage>,
    val historyState: UiState<List<Song>>,
    val radioHistory: List<RadioStation>,
    val radioFavorites: List<RadioStation>,
    val recognitionHistory: List<RecognitionHistoryEntry>,
    val replay: ReplayState,
    val replayCards: List<ReplayHeroCard>,
    val replayLandingPage: ReplayStoryPage,
    val detailActiveShelf: HomeShelf?,
    val detailSearching: Boolean,
    val isLocalDetail: Boolean,
    val refreshing: Set<MainViewModel.Feed>,
    val showRecognitionHistoryClearConfirmation: Boolean,
    val showRadioHistoryClearConfirmation: Boolean,
    val reorderTarget: UserPlaylist?,
    val reorderEntries: UiState<List<Song>>,
    val reorderSaving: Boolean,
    val reduceDynamicBlur: Boolean,
    val radioLabel: String,
    val historyLabel: String,
    val replayLabel: String,
    val searchLabel: String,
)

@Composable
internal fun rememberAppNavigationScreenStates(
    selectedTab: Int,
    selectedMoodGenre: MoodGenre?,
    visibleEntry: RouteEntry?,
    detail: DetailPage?,
    detailStack: List<DetailPage>,
    signedIn: Boolean,
    account: Account?,
    selectedChannelName: String?,
    player: PlayerState,
    listPadding: PaddingValues,
    artworkMotion: Boolean,
    windowWidth: Dp,
    radioSubpageBack: (() -> Unit)?,
    recognitionOpen: Boolean,
    showDiwali: Boolean,
    diwaliFestive: Boolean,
    searchSource: SearchSource,
    searchFocusRequested: Boolean,
    searchScrollReset: Int,
    query: String,
    filter: SearchFilter,
    results: UiState<List<SearchResult>>?,
    searchLoadingMore: Boolean,
    searchHistory: List<SearchHistoryEntity>,
    searchSuggestions: List<String>,
    libraryResults: UiState<List<Song>>?,
    addonResults: UiState<List<Song>>?,
    addonCeiling: AudioQuality,
    releaseLibrary: Map<String, LibraryState>,
    songSort: SongSort,
    savedCollections: Map<String, SavedCollection>,
    downloadedReleases: List<SavedCollection>,
    offlinePlaylistGroups: List<LocalGroup>,
    localPlaylistItems: List<ShelfItem>,
    webdavUrl: String,
    activeJamInviteCode: String?,
    activeJamInviteServer: String?,
    homeState: UiState<List<HomeShelf>>,
    visibleHomeState: UiState<List<HomeShelf>>,
    homeLoadingMore: Boolean,
    homeRecentlyPlayedLoading: Boolean,
    hideRecents: Boolean,
    topPicks: List<TopPick>?,
    topPicksLoading: Boolean,
    hideTopPicks: Boolean,
    hideHomeMoods: Boolean,
    exploreState: UiState<List<MoodGenreSection>>,
    moodGenreShelves: UiState<List<HomeShelf>>,
    libraryState: UiState<LibraryPage>,
    historyState: UiState<List<Song>>,
    radioHistory: List<RadioStation>,
    radioFavorites: List<RadioStation>,
    recognitionHistory: List<RecognitionHistoryEntry>,
    replay: ReplayState,
    replayCards: List<ReplayHeroCard>,
    replayLandingPage: ReplayStoryPage,
    detailActiveShelf: HomeShelf?,
    detailSearching: Boolean,
    isLocalDetail: Boolean,
    refreshing: Set<MainViewModel.Feed>,
    showRecognitionHistoryClearConfirmation: Boolean,
    showRadioHistoryClearConfirmation: Boolean,
    reorderTarget: UserPlaylist?,
    reorderEntries: UiState<List<Song>>,
    reorderSaving: Boolean,
    reduceDynamicBlur: Boolean,
    radioLabel: String,
    historyLabel: String,
    replayLabel: String,
    searchLabel: String,
): AppNavigationScreenStates = AppNavigationScreenStates(
    selectedTab = selectedTab,
    selectedMoodGenre = selectedMoodGenre,
    visibleEntry = visibleEntry,
    detail = detail,
    detailStack = detailStack,
    signedIn = signedIn,
    account = account,
    selectedChannelName = selectedChannelName,
    player = player,
    listPadding = listPadding,
    artworkMotion = artworkMotion,
    windowWidth = windowWidth,
    radioSubpageBack = radioSubpageBack,
    recognitionOpen = recognitionOpen,
    showDiwali = showDiwali,
    diwaliFestive = diwaliFestive,
    searchSource = searchSource,
    searchFocusRequested = searchFocusRequested,
    searchScrollReset = searchScrollReset,
    query = query,
    filter = filter,
    results = results,
    searchLoadingMore = searchLoadingMore,
    searchHistory = searchHistory,
    searchSuggestions = searchSuggestions,
    libraryResults = libraryResults,
    addonResults = addonResults,
    addonCeiling = addonCeiling,
    releaseLibrary = releaseLibrary,
    songSort = songSort,
    savedCollections = savedCollections,
    downloadedReleases = downloadedReleases,
    offlinePlaylistGroups = offlinePlaylistGroups,
    localPlaylistItems = localPlaylistItems,
    webdavUrl = webdavUrl,
    activeJamInviteCode = activeJamInviteCode,
    activeJamInviteServer = activeJamInviteServer,
    homeState = homeState,
    visibleHomeState = visibleHomeState,
    homeLoadingMore = homeLoadingMore,
    homeRecentlyPlayedLoading = homeRecentlyPlayedLoading,
    hideRecents = hideRecents,
    topPicks = topPicks,
    topPicksLoading = topPicksLoading,
    hideTopPicks = hideTopPicks,
    hideHomeMoods = hideHomeMoods,
    exploreState = exploreState,
    moodGenreShelves = moodGenreShelves,
    libraryState = libraryState,
    historyState = historyState,
    radioHistory = radioHistory,
    radioFavorites = radioFavorites,
    recognitionHistory = recognitionHistory,
    replay = replay,
    replayCards = replayCards,
    replayLandingPage = replayLandingPage,
    detailActiveShelf = detailActiveShelf,
    detailSearching = detailSearching,
    isLocalDetail = isLocalDetail,
    refreshing = refreshing,
    showRecognitionHistoryClearConfirmation = showRecognitionHistoryClearConfirmation,
    showRadioHistoryClearConfirmation = showRadioHistoryClearConfirmation,
    reorderTarget = reorderTarget,
    reorderEntries = reorderEntries,
    reorderSaving = reorderSaving,
    reduceDynamicBlur = reduceDynamicBlur,
    radioLabel = radioLabel,
    historyLabel = historyLabel,
    replayLabel = replayLabel,
    searchLabel = searchLabel,
)

@Immutable
data class AppNavigationScreenActions(
    val onSelectTab: (Int) -> Unit,
    val onRadioSubpageBackChanged: ((() -> Unit)?) -> Unit,
    val onSearchSourceChange: (SearchSource) -> Unit,
    val onSearchFocusHandled: () -> Unit,
    val onSongSwipe: (Song) -> Unit,
    val onOpenSongMenu: (Song) -> Unit,
    val onOpenBrowseMenu: (BrowseTarget) -> Unit,
    val onLibraryItemClick: (ShelfItem) -> Unit,
    val onBrowseLongPress: (ShelfItem) -> Unit,
    val onShelfLongPress: (ShelfItem) -> Unit,
    val onPlayRadio: (Song, QueueSource) -> Unit,
    val onPlayFrom: (List<Song>, Int, QueueSource, Boolean, String?) -> Unit,
    val onPlayRecognizedSong: (RecognitionResult) -> Unit,
    val onPlayRecognitionHistoryEntry: (RecognitionHistoryEntry) -> Unit,
    val onStartRadio: (Song) -> Unit,
    val onOpenTopPick: (TopPick, String) -> Unit,
    val onPlayMood: (TopPickMood, String) -> Unit,
    val onDownloadFromCache: ((List<Song>) -> Unit)?,
    val onRemoveFromCache: ((List<Song>) -> Unit)?,
    val onUploadToWebDav: (List<Song>) -> Unit,
    val onShowQueueNotice: (String) -> Unit,
    val onSetReplayPeriod: (ReplayPeriod) -> Unit,
    val onOpenReplayStory: (ReplayStoryPage) -> Unit,
    val onShareReplay: () -> Unit,
    val onOpenReplayLandingPage: (ReplayStoryPage) -> Unit,
    val onStationActions: (StationMenuTarget) -> Unit,
    val onBlockedAddonSong: (Song) -> Unit,
    val onCreatingPlaylist: () -> Unit,
    val onCreatingOfflinePlaylist: () -> Unit,
    val onShowSpotifyImport: () -> Unit,
    val onOpenWebSession: (WebSessionMode) -> Unit,
    val onShowDiwali: () -> Unit,
    val onShowUpdateDialog: () -> Unit,
    val onShowAboutWhatsNew: () -> Unit,
    val onShowLyricsSources: () -> Unit,
    val onShowTranslationLanguage: () -> Unit,
    val onShowAppLanguage: () -> Unit,
    val onShowListenBrainzLogin: () -> Unit,
    val onShowLastfmLogin: () -> Unit,
    val onShowDiscordLogin: () -> Unit,
    val onShowSpotifyLogin: () -> Unit,
    val onShowSpotifyCanvasAuth: () -> Unit,
    val onShowWebDavEditor: () -> Unit,
    val onShowSmbEditor: () -> Unit,
    val onConfirmJioSaavn: () -> Unit,
    val onConfirmBitPerfect: (BitPerfectMode) -> Unit,
    val onConfirmFixedVolume: () -> Unit,
    val onConfirmDriveDisconnect: () -> Unit,
    val onConfirmDriveRemove: (RemoteDevice) -> Unit,
    val onConfirmDriveRestore: (RemoteDevice) -> Unit,
    val onConfirmDriveRename: (String) -> Unit,
    val onEditingPartyServer: () -> Unit,
    val onEditingSignatureApi: () -> Unit,
    val onDiscordDialog: (DiscordDialog) -> Unit,
    val onEditSource: (SourceConfig) -> Unit,
    val onChooseSourceQuality: (SourceConfig) -> Unit,
    val onExplainOutdatedAddon: (SourceConfig) -> Unit,
    val onShowAccountSelector: () -> Unit,
    val onClearRecognitionHistoryConfirmation: (Boolean) -> Unit,
    val onClearRadioHistoryConfirmation: (Boolean) -> Unit,
    val onDetailSearchingChange: (Boolean) -> Unit,
    val onReorderTargetChange: (UserPlaylist?) -> Unit,
    val onReorderSavingChange: (Boolean) -> Unit,
    val onFolderSortOf: (String) -> LocalMusicSort,
    val onFolderViewTypeOf: (String) -> LibraryViewType,
    val onLibraryLinks: @Composable () -> List<LibraryLink>,
    val onLibraryDeviceItems: @Composable (List<SavedCollection>) -> List<ShelfItem>,
    val onTopBarHeight: @Composable () -> Dp,
    val onRefusedByHost: () -> Boolean,
    val onOpenByName: (String?, String, String?, BrowseType, String?) -> Unit,
)

@Composable
internal fun rememberAppNavigationScreenActions(
    onSelectTab: (Int) -> Unit,
    onRadioSubpageBackChanged: ((() -> Unit)?) -> Unit,
    onSearchSourceChange: (SearchSource) -> Unit,
    onSearchFocusHandled: () -> Unit,
    onSongSwipe: (Song) -> Unit,
    onOpenSongMenu: (Song) -> Unit,
    onOpenBrowseMenu: (BrowseTarget) -> Unit,
    onLibraryItemClick: (ShelfItem) -> Unit,
    onBrowseLongPress: (ShelfItem) -> Unit,
    onShelfLongPress: (ShelfItem) -> Unit,
    onPlayRadio: (Song, QueueSource) -> Unit,
    onPlayFrom: (List<Song>, Int, QueueSource, Boolean, String?) -> Unit,
    onPlayRecognizedSong: (RecognitionResult) -> Unit,
    onPlayRecognitionHistoryEntry: (RecognitionHistoryEntry) -> Unit,
    onStartRadio: (Song) -> Unit,
    onOpenTopPick: (TopPick, String) -> Unit,
    onPlayMood: (TopPickMood, String) -> Unit,
    onDownloadFromCache: ((List<Song>) -> Unit)?,
    onRemoveFromCache: ((List<Song>) -> Unit)?,
    onUploadToWebDav: (List<Song>) -> Unit,
    onShowQueueNotice: (String) -> Unit,
    onSetReplayPeriod: (ReplayPeriod) -> Unit,
    onOpenReplayStory: (ReplayStoryPage) -> Unit,
    onShareReplay: () -> Unit,
    onOpenReplayLandingPage: (ReplayStoryPage) -> Unit,
    onStationActions: (StationMenuTarget) -> Unit,
    onBlockedAddonSong: (Song) -> Unit,
    onCreatingPlaylist: () -> Unit,
    onCreatingOfflinePlaylist: () -> Unit,
    onShowSpotifyImport: () -> Unit,
    onOpenWebSession: (WebSessionMode) -> Unit,
    onShowDiwali: () -> Unit,
    onShowUpdateDialog: () -> Unit,
    onShowAboutWhatsNew: () -> Unit,
    onShowLyricsSources: () -> Unit,
    onShowTranslationLanguage: () -> Unit,
    onShowAppLanguage: () -> Unit,
    onShowListenBrainzLogin: () -> Unit,
    onShowLastfmLogin: () -> Unit,
    onShowDiscordLogin: () -> Unit,
    onShowSpotifyLogin: () -> Unit,
    onShowSpotifyCanvasAuth: () -> Unit,
    onShowWebDavEditor: () -> Unit,
    onShowSmbEditor: () -> Unit,
    onConfirmJioSaavn: () -> Unit,
    onConfirmBitPerfect: (BitPerfectMode) -> Unit,
    onConfirmFixedVolume: () -> Unit,
    onConfirmDriveDisconnect: () -> Unit,
    onConfirmDriveRemove: (RemoteDevice) -> Unit,
    onConfirmDriveRestore: (RemoteDevice) -> Unit,
    onConfirmDriveRename: (String) -> Unit,
    onEditingPartyServer: () -> Unit,
    onEditingSignatureApi: () -> Unit,
    onDiscordDialog: (DiscordDialog) -> Unit,
    onEditSource: (SourceConfig) -> Unit,
    onChooseSourceQuality: (SourceConfig) -> Unit,
    onExplainOutdatedAddon: (SourceConfig) -> Unit,
    onShowAccountSelector: () -> Unit,
    onClearRecognitionHistoryConfirmation: (Boolean) -> Unit,
    onClearRadioHistoryConfirmation: (Boolean) -> Unit,
    onDetailSearchingChange: (Boolean) -> Unit,
    onReorderTargetChange: (UserPlaylist?) -> Unit,
    onReorderSavingChange: (Boolean) -> Unit,
    onFolderSortOf: (String) -> LocalMusicSort,
    onFolderViewTypeOf: (String) -> LibraryViewType,
    onLibraryLinks: @Composable () -> List<LibraryLink>,
    onLibraryDeviceItems: @Composable (List<SavedCollection>) -> List<ShelfItem>,
    onTopBarHeight: @Composable () -> Dp,
    onRefusedByHost: () -> Boolean,
    onOpenByName: (String?, String, String?, BrowseType, String?) -> Unit,
): AppNavigationScreenActions = AppNavigationScreenActions(
    onSelectTab = onSelectTab,
    onRadioSubpageBackChanged = onRadioSubpageBackChanged,
    onSearchSourceChange = onSearchSourceChange,
    onSearchFocusHandled = onSearchFocusHandled,
    onSongSwipe = onSongSwipe,
    onOpenSongMenu = onOpenSongMenu,
    onOpenBrowseMenu = onOpenBrowseMenu,
    onLibraryItemClick = onLibraryItemClick,
    onBrowseLongPress = onBrowseLongPress,
    onShelfLongPress = onShelfLongPress,
    onPlayRadio = onPlayRadio,
    onPlayFrom = onPlayFrom,
    onPlayRecognizedSong = onPlayRecognizedSong,
    onPlayRecognitionHistoryEntry = onPlayRecognitionHistoryEntry,
    onStartRadio = onStartRadio,
    onOpenTopPick = onOpenTopPick,
    onPlayMood = onPlayMood,
    onDownloadFromCache = onDownloadFromCache,
    onRemoveFromCache = onRemoveFromCache,
    onUploadToWebDav = onUploadToWebDav,
    onShowQueueNotice = onShowQueueNotice,
    onSetReplayPeriod = onSetReplayPeriod,
    onOpenReplayStory = onOpenReplayStory,
    onShareReplay = onShareReplay,
    onOpenReplayLandingPage = onOpenReplayLandingPage,
    onStationActions = onStationActions,
    onBlockedAddonSong = onBlockedAddonSong,
    onCreatingPlaylist = onCreatingPlaylist,
    onCreatingOfflinePlaylist = onCreatingOfflinePlaylist,
    onShowSpotifyImport = onShowSpotifyImport,
    onOpenWebSession = onOpenWebSession,
    onShowDiwali = onShowDiwali,
    onShowUpdateDialog = onShowUpdateDialog,
    onShowAboutWhatsNew = onShowAboutWhatsNew,
    onShowLyricsSources = onShowLyricsSources,
    onShowTranslationLanguage = onShowTranslationLanguage,
    onShowAppLanguage = onShowAppLanguage,
    onShowListenBrainzLogin = onShowListenBrainzLogin,
    onShowLastfmLogin = onShowLastfmLogin,
    onShowDiscordLogin = onShowDiscordLogin,
    onShowSpotifyLogin = onShowSpotifyLogin,
    onShowSpotifyCanvasAuth = onShowSpotifyCanvasAuth,
    onShowWebDavEditor = onShowWebDavEditor,
    onShowSmbEditor = onShowSmbEditor,
    onConfirmJioSaavn = onConfirmJioSaavn,
    onConfirmBitPerfect = onConfirmBitPerfect,
    onConfirmFixedVolume = onConfirmFixedVolume,
    onConfirmDriveDisconnect = onConfirmDriveDisconnect,
    onConfirmDriveRemove = onConfirmDriveRemove,
    onConfirmDriveRestore = onConfirmDriveRestore,
    onConfirmDriveRename = onConfirmDriveRename,
    onEditingPartyServer = onEditingPartyServer,
    onEditingSignatureApi = onEditingSignatureApi,
    onDiscordDialog = onDiscordDialog,
    onEditSource = onEditSource,
    onChooseSourceQuality = onChooseSourceQuality,
    onExplainOutdatedAddon = onExplainOutdatedAddon,
    onShowAccountSelector = onShowAccountSelector,
    onClearRecognitionHistoryConfirmation = onClearRecognitionHistoryConfirmation,
    onClearRadioHistoryConfirmation = onClearRadioHistoryConfirmation,
    onDetailSearchingChange = onDetailSearchingChange,
    onReorderTargetChange = onReorderTargetChange,
    onReorderSavingChange = onReorderSavingChange,
    onFolderSortOf = onFolderSortOf,
    onFolderViewTypeOf = onFolderViewTypeOf,
    onLibraryLinks = onLibraryLinks,
    onLibraryDeviceItems = onLibraryDeviceItems,
    onTopBarHeight = onTopBarHeight,
    onRefusedByHost = onRefusedByHost,
    onOpenByName = onOpenByName,
)

@OptIn(ExperimentalMaterial3Api::class)
@Immutable
data class AppNavigationListStates(
    val homeListState: LazyListState,
    val exploreListState: LazyListState,
    val libraryListState: LazyListState,
    val searchListState: LazyListState,
    val historyListState: LazyListState,
    val replayListState: LazyListState,
    val moodGenreListState: LazyListState,
    val libraryShowAllGridState: LazyGridState,
    val moodsGridState: LazyGridState,
    val homePull: PullToRefreshState,
    val explorePull: PullToRefreshState,
    val libraryPull: PullToRefreshState,
    val radioPull: PullToRefreshState,
    val radioListState: LazyListState? = null,
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun AppNavigationHost(
    targetState: AppDestination,
    navigator: AppNavigator,
    viewModel: MainViewModel,
    hazeState: HazeState,
    glassActive: Boolean,
    glassSamplesBackdrop: Boolean,
    appBackdrop: LayerBackdrop,
    navBarScroll: NestedScrollConnection,
    detailListStates: MutableMap<String, LazyListState>,
    localFolderStates: MutableMap<String, LocalFolderState>,
    pageStates: SaveableStateHolder,
    pageKeysSeen: MutableSet<String>,
    radioHistoryStore: RadioHistoryStore,
    radioFavoritesStore: RadioFavoritesStore,
    radioFavoriteScope: CoroutineScope,
    states: AppNavigationScreenStates,
    lists: AppNavigationListStates,
    actions: AppNavigationScreenActions,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    com.soundchord.features.editorial.data.AppleMusicEditorialRepository.init(context)
    DaypartRepository.init(context)
    StationsForYouRepository.init(context)
    PlaylistsForYouRepository.init(context)
    val scope = rememberCoroutineScope()
    val radioSort = AppSettings.librarySort.collectAsStateWithLifecycle().value
    val radioListState = lists.radioListState ?: rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val hideHomeEditorial = AppSettings.hideHomeEditorial.collectAsStateWithLifecycle().value
    var homeEditorialShelves by remember { mutableStateOf<List<HomeShelf>>(emptyList()) }
    var daypartShelves by remember {
        mutableStateOf<List<HomeShelf>>(
            DaypartRepository.getCachedShelves(
                daypart = com.soundchord.features.daypart.model.Daypart.current(),
                accountHash = DaypartRepository.resolveAccountHash(states.account?.name),
            ).orEmpty()
        )
    }
    var personalizedEssentialsShelf by remember { mutableStateOf<HomeShelf?>(null) }
    var stationsForYou by remember {
        mutableStateOf<List<TopPick>>(
            StationsForYouRepository.getCachedStations(identity = states.account?.name).orEmpty()
        )
    }
    var playlistsForYou by remember {
        mutableStateOf<List<TopPick>>(
            PlaylistsForYouRepository.getCachedPlaylists(identity = states.account?.name).orEmpty()
        )
    }
    LaunchedEffect(Unit) {
        com.soundchord.features.editorial.data.AppleMusicEditorialRepository.init(context)
        DaypartRepository.init(context)
        StationsForYouRepository.init(context)
        PlaylistsForYouRepository.init(context)
        TopPicks.editorialHeroResolver = { now, genres ->
            if (AppSettings.hideHomeEditorial.value) {
                emptyList()
            } else {
                com.soundchord.features.editorial.data.AppleMusicEditorialRepository.resolveHeroTopPicks(now, genres)
            }
        }
        homeEditorialShelves = com.soundchord.features.editorial.data.AppleMusicEditorialRepository.getHomeDiscoveryShelves()
        val cachedStations = StationsForYouRepository.getCachedStations(identity = states.account?.name)
        if (!cachedStations.isNullOrEmpty()) {
            stationsForYou = cachedStations
        }
        val cachedPlaylists = PlaylistsForYouRepository.getCachedPlaylists(identity = states.account?.name)
        if (!cachedPlaylists.isNullOrEmpty()) {
            playlistsForYou = cachedPlaylists
        }
        val cachedSongMixes = DaypartDatabaseCache.get("home_song_mixes_shelf")?.firstOrNull()?.takeIf { it.items.size >= 4 }
        if (cachedSongMixes != null) {
            NativeHomeShelvesHolder.lastValidSongMixes = cachedSongMixes
        }
    }
    LaunchedEffect(states.visibleHomeState, homeEditorialShelves, states.topPicks, states.account, states.historyState, states.libraryState) {
        val homeShelves = (states.visibleHomeState as? UiState.Success)?.data.orEmpty()
        val currentDaypart = com.soundchord.features.daypart.model.Daypart.current()
        val userRealArtists = UserArtistPoolResolver.resolveUserRealArtists(
            historyState = states.historyState,
            libraryState = states.libraryState,
            savedCollections = states.savedCollections,
            topPicks = states.topPicks,
            homeShelves = homeShelves,
        )
        val userGenreKeywords = UserArtistPoolResolver.resolveUserGenreKeywords(
            historyState = states.historyState,
            libraryState = states.libraryState,
            homeShelves = homeShelves,
        )
        val resolved = DaypartRepository.getDaypartShelves(
            homeShelves = homeShelves,
            editorialShelves = homeEditorialShelves,
            daypart = currentDaypart,
            userArtists = userRealArtists,
            userGenreKeywords = userGenreKeywords,
            identity = states.account?.name,
        )
        if (resolved.isNotEmpty()) {
            daypartShelves = resolved
        }
        val resolvedEssentials = DaypartRepository.getPersonalizedEssentialsShelf(
            homeShelves = homeShelves,
            editorialShelves = homeEditorialShelves,
            userArtists = userRealArtists,
            userGenreKeywords = userGenreKeywords,
            identity = states.account?.name,
        )
        if (resolvedEssentials != null) {
            personalizedEssentialsShelf = resolvedEssentials
        }
        val resolvedStations = StationsForYouRepository.getStationsForYou(
            homeShelves = homeShelves,
            identity = states.account?.name,
            listenerName = states.account?.name,
            localArtists = AppSettings.onboardingArtists.value.map { ArtistRef(name = it.name, browseId = it.browseId) },
        )
        if (resolvedStations.isNotEmpty()) {
            stationsForYou = resolvedStations
        }
        val resolvedPlaylists = PlaylistsForYouRepository.getPlaylistsForYou(
            homeShelves = homeShelves,
            editorialShelves = homeEditorialShelves,
            identity = states.account?.name,
            topPicks = states.topPicks,
            localArtists = AppSettings.onboardingArtists.value.map { ArtistRef(name = it.name, browseId = it.browseId) },
            userArtists = userRealArtists,
        )
        if (resolvedPlaylists.isNotEmpty()) {
            playlistsForYou = resolvedPlaylists
        }
        val songMixesCandidate = homeShelves.firstOrNull { s ->
            s.layout == com.music.bitchord.data.model.ShelfLayout.LIST ||
                s.title.contains("mix", ignoreCase = true) ||
                s.title.contains("picks", ignoreCase = true)
        }?.let { raw ->
            val clean = raw.items.filter { !it.isVideo && it.videoId != null }
            if (clean.size >= 4) raw.copy(items = clean) else null
        }
        if (songMixesCandidate != null) {
            DaypartDatabaseCache.put("home_song_mixes_shelf", listOf(songMixesCandidate))
            NativeHomeShelvesHolder.lastValidSongMixes = songMixesCandidate
        }
    }

    SharedTransitionLayout {
        CompositionLocalProvider(
            LocalCoverTransitionScope provides this@SharedTransitionLayout,
        ) {
            AnimatedContent(
                targetState = targetState,
                contentKey = { destination ->
                    if (destination is AppDestination.Tab) "tabs" else destination.key
                },
        transitionSpec = {
            val initial = initialState
            val target = targetState
            val tabSwap = initial is AppDestination.Tab && target is AppDestination.Tab
            val libraryTab = AppDestination.Tab(TAB_LIBRARY)
            fun AppDestination.isShowAll() =
                (this as? AppDestination.Route)?.entry?.route is AppRoute.LibraryShowAll
            val libraryShowAllSwap = (initial.isShowAll() && target == libraryTab) ||
                (target.isShowAll() && initial == libraryTab)

            fun AppDestination.isSettingsRoot() =
                (this as? AppDestination.Route)?.entry?.route == AppRoute.Settings

            fun AppDestination.isSettingsSubPage() =
                ((this as? AppDestination.Route)?.entry?.route).let { r ->
                    r is AppRoute.SettingsSection || r is AppRoute.SettingsChoice || r == AppRoute.About ||
                        r is AppRoute.AccountScrobbling || r is AppRoute.Discord || r is AppRoute.DriveBackup ||
                        r is AppRoute.DriveDevices || r is AppRoute.WearOs || r is AppRoute.Equalizer ||
                        r is AppRoute.EqualizerPresets
                }

            val pushingSettings = (initial.isSettingsRoot() && target.isSettingsSubPage()) ||
                (initial.isSettingsSubPage() && (target as? AppDestination.Route)?.entry?.route is AppRoute.SettingsChoice)
            val poppingSettings = (initial.isSettingsSubPage() && target.isSettingsRoot()) ||
                ((initial as? AppDestination.Route)?.entry?.route is AppRoute.SettingsChoice && target.isSettingsSubPage())

            val poppingRelease = initial is AppDestination.Release &&
                (target !is AppDestination.Release || !states.detailStack.any { it.browseId == initial.browseId })
            val pushingRelease = target is AppDestination.Release && !poppingRelease

            if (tabSwap || libraryShowAllSwap) {
                (EnterTransition.None togetherWith ExitTransition.None).apply {
                    targetContentZIndex = 1f
                }
            } else if (pushingRelease) {
                // Entering a playlist/album/artist/mix: the release page arrives fully opaque immediately
                // so the underlying screen never ghosts through behind the buttons. The header
                // cover flies in while title, credits, action capsules and track list stagger in via enterReveal.
                (EnterTransition.None togetherWith fadeOut(tween(160))).apply {
                    targetContentZIndex = 1f
                }
            } else if (poppingRelease) {
                // Leaving a playlist/album/artist/mix back to previous screen (popBackStack):
                // keep the outgoing release on top (targetContentZIndex = 0f) so the cover
                // contracts smoothly back into its card on top of the underlying screen.
                (fadeIn(tween(280)) togetherWith fadeOut(tween(560, easing = FastOutLinearInEasing))).apply {
                    targetContentZIndex = 0f
                }
            } else if (pushingSettings) {
                // iOS-style push animation when navigating into Settings sub-screens
                ((slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it } +
                    fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { -it / 3 } +
                        fadeOut(tween(200)))).apply {
                        targetContentZIndex = 1f
                    }
            } else if (poppingSettings) {
                // iOS-style pop animation when popping back from Settings sub-screens
                ((slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { -it / 3 } +
                    fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { it } +
                        fadeOut(tween(260)))).apply {
                        targetContentZIndex = 1f
                    }
            } else {
                fadeIn(tween(180)) togetherWith fadeOut(tween(180))
            }
        },
        modifier = modifier
            .hazeSource(hazeState)
            .then(
                if (glassActive) {
                    Modifier
                        .then(
                            if (glassSamplesBackdrop) {
                                Modifier.layerBackdrop(appBackdrop)
                            } else {
                                Modifier
                            },
                        )
                        .nestedScroll(navBarScroll)
                } else {
                    Modifier
                },
            ),
        label = "content",
    ) { destination ->
        val key = destination.key
        val pageRoute = (destination as? AppDestination.Route)?.entry?.route
        val live = (destination as? AppDestination.Release)?.let { release ->
            states.detailStack.lastOrNull()?.takeIf { it.browseId == release.browseId }
        }
        val held = remember(key) { mutableStateOf(live) }
        if (live != null) held.value = live
        val page = held.value
        val pageDetailListState = remember(key) {
            detailListStates.getOrPut(key) { LazyListState() }
        }

        val onHomeItemClick: (ShelfItem, String) -> Unit = remember(actions, viewModel) {
            { item: ShelfItem, shelfTitle: String ->
                val song = shelfSong(item)
                val browseId = item.browseId
                when {
                    song != null -> actions.onPlayRadio(
                        song,
                        QueueSource(shelfTitle, PlaybackSourceType.HOME),
                    )
                    browseId != null -> viewModel.openDetail(
                        browseId = browseId,
                        title = item.title,
                        subtitle = item.subtitle,
                        thumbnailUrl = item.thumbnailUrl,
                    )
                }
            }
        }
        val onHomeTopPickLongPress: (TopPick) -> Unit = remember(actions) {
            { pick: TopPick ->
                pick.browseId?.let { browseId ->
                    actions.onShelfLongPress(
                        ShelfItem(
                            title = pick.title,
                            subtitle = pick.artistName.orEmpty(),
                            thumbnailUrl = pick.images.firstOrNull(),
                            videoId = null,
                            browseId = browseId,
                        ),
                    )
                }
            }
        }
        val onExploreShelfItemClick: (HomeShelf, ShelfItem) -> Unit = remember(actions, viewModel) {
            { shelf: HomeShelf, item: ShelfItem ->
                val song = shelfSong(item)
                val browseId = item.browseId
                when {
                    song != null -> actions.onPlayRadio(
                        song,
                        QueueSource(shelf.title, PlaybackSourceType.EXPLORE, shelf.moreBrowseId),
                    )
                    browseId != null -> viewModel.openDetail(
                        browseId = browseId,
                        title = item.title,
                        subtitle = item.subtitle,
                        thumbnailUrl = item.thumbnailUrl,
                    )
                }
            }
        }
        val onRadioPlayMix: (String, String, String?) -> Unit = remember(viewModel) {
            { browseId: String, title: String, thumbnailUrl: String? ->
                viewModel.openDetail(browseId, title, "YouTube Music mix", thumbnailUrl, BrowseType.PLAYLIST)
            }
        }
        val onRadioPlayStation: (Song) -> Unit = remember(actions, states.radioLabel) {
            { song: Song ->
                actions.onPlayRadio(
                    song,
                    QueueSource(states.radioLabel, PlaybackSourceType.QUEUE, id = song.playbackSourceId),
                )
            }
        }
        val onRadioStationLongPress: (RadioStation, Boolean, Rect, () -> Unit) -> Unit = remember(actions) {
            { station: RadioStation, isFavorite: Boolean, anchor: Rect, toggleFavorite: () -> Unit ->
                actions.onStationActions(StationMenuTarget(station, isFavorite, anchor, toggleFavorite))
            }
        }
        val onLibraryShelfClick: (ShelfItem) -> Unit = remember(navigator, actions) {
            { item: ShelfItem ->
                if (item.browseId == RADIO_FAVORITES_BROWSE_ID) navigator.push(AppRoute.RadioFavorites)
                else actions.onLibraryItemClick(item)
            }
        }
        val exploreShelves = remember(states.homeState) {
            (states.homeState as? UiState.Success)?.data.orEmpty()
        }

        val pageContent: @Composable () -> Unit = {
            if (pageRoute == AppRoute.RecognitionHistory) {
                RecognitionHistoryScreen(
                    entries = states.recognitionHistory,
                    onPlay = actions.onPlayRecognitionHistoryEntry,
                    onRemove = RecognitionHistoryStore::remove,
                    onClear = RecognitionHistoryStore::clear,
                    showClearConfirmation = states.showRecognitionHistoryClearConfirmation,
                    onDismissClearConfirmation = {
                        actions.onClearRecognitionHistoryConfirmation(false)
                    },
                    dialogSurfaceModifier = historyDialogSurface(hazeState, states.reduceDynamicBlur),
                    contentPadding = states.listPadding,
                )
            } else if (pageRoute == AppRoute.RadioHistory) {
                RadioHistoryScreen(
                    stations = states.radioHistory,
                    favoriteUuids = states.radioFavorites.mapTo(hashSetOf()) { it.uuid },
                    onPlay = { station ->
                        radioHistoryStore.record(context, station)
                        actions.onPlayRadio(
                            station.toSong(context),
                            QueueSource(states.radioLabel, PlaybackSourceType.QUEUE, id = "radio:${station.uuid}"),
                        )
                    },
                    onFavorite = { station ->
                        radioFavoriteScope.launch {
                            if (states.radioFavorites.any { it.uuid == station.uuid }) radioFavoritesStore.remove(station.uuid)
                            else radioFavoritesStore.add(station)
                        }
                    },
                    showClearDialog = states.showRadioHistoryClearConfirmation,
                    onDismissClearDialog = { actions.onClearRadioHistoryConfirmation(false) },
                    onConfirmClear = {
                        radioHistoryStore.clear()
                        actions.onClearRadioHistoryConfirmation(false)
                    },
                    dialogSurfaceModifier = historyDialogSurface(hazeState, states.reduceDynamicBlur),
                    contentPadding = states.listPadding,
                )
            } else if (pageRoute == AppRoute.RadioFavorites) {
                RadioFavoritesScreen(
                    stations = states.radioFavorites,
                    onPlay = { station ->
                        radioHistoryStore.record(context, station)
                        actions.onPlayRadio(
                            station.toSong(context),
                            QueueSource(states.radioLabel, PlaybackSourceType.QUEUE, id = "radio:${station.uuid}"),
                        )
                    },
                    onRemoveFavorite = { station -> radioFavoriteScope.launch { radioFavoritesStore.remove(station.uuid) } },
                    contentPadding = states.listPadding,
                    searching = states.detailSearching,
                    sort = when (radioSort) {
                        LibrarySort.TITLE_ASC -> com.soundchord.features.radio.ui.RadioFavoritesSort.NAME_ASC
                        LibrarySort.TITLE_DESC -> com.soundchord.features.radio.ui.RadioFavoritesSort.NAME_DESC
                        LibrarySort.DEFAULT -> com.soundchord.features.radio.ui.RadioFavoritesSort.DEFAULT
                    },
                )
            } else if (pageRoute == AppRoute.History) {
                HistoryScreen(
                    state = states.historyState,
                    listState = lists.historyListState,
                    currentSong = states.player.song,
                    isPlaying = states.player.isPlaying,
                    onSongClick = { songs, index ->
                        actions.onPlayFrom(songs, index, QueueSource(states.historyLabel, PlaybackSourceType.HISTORY), false, null)
                    },
                    onSongLongPress = { song ->
                        actions.onOpenSongMenu(song)
                    },
                    onSongSwipe = actions.onSongSwipe,
                    onRetry = viewModel::loadHistory,
                    contentPadding = states.listPadding,
                )
            } else if (pageRoute is AppRoute.LibraryShowAll) {
                val shelf = if (pageRoute.shelf.title == YtMusicRepository.PLAYLISTS_SHELF) {
                    val liveLibrary = (states.libraryState as? UiState.Success)?.data
                    if (liveLibrary == null) pageRoute.shelf
                    else liveLibrary.shelves.firstOrNull {
                        it.title == YtMusicRepository.PLAYLISTS_SHELF
                    } ?: HomeShelf(YtMusicRepository.PLAYLISTS_SHELF, emptyList())
                } else pageRoute.shelf
                shelf.let {
                    LibraryGridPage(
                        shelf = it,
                        gridState = lists.libraryShowAllGridState,
                        onItemClick = actions.onLibraryItemClick,
                        onItemLongPress = actions.onBrowseLongPress,
                        onNewPlaylist = if (shelf.title == YtMusicRepository.PLAYLISTS_SHELF) {
                            { actions.onCreatingPlaylist() }
                        } else {
                            null
                        },
                        refreshing = MainViewModel.Feed.LIBRARY in states.refreshing,
                        onRefresh = { viewModel.refresh(MainViewModel.Feed.LIBRARY) },
                        pullState = lists.libraryPull,
                        contentPadding = states.listPadding,
                    )
                }
            } else if (pageRoute == AppRoute.Moods) {
                MoodGridPage(
                    gridState = lists.moodsGridState,
                    onClick = actions.onPlayMood,
                    contentPadding = states.listPadding,
                )
            } else if (pageRoute == AppRoute.Replay) {
                ReplayScreen(
                    state = states.replay,
                    holder = states.account?.name.orEmpty(),
                    onPeriodChange = actions.onSetReplayPeriod,
                    onOpenStory = actions.onOpenReplayStory,
                    onPlaySong = { song ->
                        actions.onPlayRadio(song, QueueSource(states.replayLabel, PlaybackSourceType.REPLAY))
                    },
                    onOpenArtist = { id, name ->
                        actions.onOpenByName(id, name, null, BrowseType.ARTIST, null)
                    },
                    onOpenAlbum = { id, title, artist, art ->
                        actions.onOpenByName(id, title, artist, BrowseType.ALBUM, art)
                    },
                    onShare = {
                        actions.onShareReplay()
                    },
                    contentPadding = states.listPadding,
                    listState = lists.replayListState,
                    landingPage = states.replayLandingPage,
                )
            } else if (pageRoute == AppRoute.Spotify) {
                SpotifyLibraryScreen(
                    onOpenPlaylist = { playlist ->
                        viewModel.openDetail(
                            browseId = SPOTIFY_PAGE_PREFIX + playlist.id,
                            title = playlist.name,
                            subtitle = playlist.owner ?: context.getString(R.string.spotify),
                            thumbnailUrl = playlist.imageUrl,
                            type = BrowseType.PLAYLIST,
                        )
                    },
                    onOpenLogin = actions.onShowSpotifyLogin,
                    onOpenTokenSetup = actions.onShowSpotifyCanvasAuth,
                    contentPadding = states.listPadding,
                )
            } else if (pageRoute == AppRoute.Discord) {
                DiscordScreen(
                    song = states.player.song,
                    positionMs = states.player.position.positionMs,
                    durationMs = states.player.durationMs,
                    onOpenLogin = actions.onShowDiscordLogin,
                    onOpenDialog = actions.onDiscordDialog,
                    contentPadding = states.listPadding,
                )
            } else if (pageRoute == AppRoute.AccountScrobbling) {
                AccountAndScrobblingScreen(
                    signedIn = states.signedIn,
                    account = states.account,
                    channelName = states.selectedChannelName,
                    onSignIn = {
                        navigator.clear()
                        actions.onOpenWebSession(WebSessionMode.SIGN_IN)
                    },
                    onSwitchChannel = {
                        viewModel.loadChannels()
                        actions.onShowAccountSelector()
                    },
                    onSignOut = { viewModel.signOut() },
                    onOpenListenBrainzLogin = actions.onShowListenBrainzLogin,
                    onOpenLastfmLogin = actions.onShowLastfmLogin,
                    onOpenDiscord = { navigator.push(AppRoute.Discord) },
                    onOpenSpotify = { navigator.push(AppRoute.Spotify) },
                    contentPadding = states.listPadding,
                )
            } else if (pageRoute == AppRoute.Sources) {
                SourcesScreen(
                    contentPadding = states.listPadding,
                    onEditSource = actions.onEditSource,
                    onChooseSourceQuality = actions.onChooseSourceQuality,
                    onExplainOutdatedAddon = actions.onExplainOutdatedAddon,
                    onEditWebDav = actions.onShowWebDavEditor,
                    onEditSmb = actions.onShowSmbEditor,
                    onConfirmJioSaavn = actions.onConfirmJioSaavn,
                )
            } else if (pageRoute == AppRoute.Downloads) {
                DownloadManagerPage(
                    onCleared = { navigator.pop() },
                    contentPadding = states.listPadding,
                )
            } else if (pageRoute == AppRoute.WearOs) {
                WearOsScreen(contentPadding = states.listPadding)
            } else if (pageRoute == AppRoute.DriveBackup) {
                DriveBackupScreen(
                    contentPadding = states.listPadding,
                    onOpenDevices = { navigator.push(AppRoute.DriveDevices) },
                    onConfirmDisconnect = actions.onConfirmDriveDisconnect,
                    onConfirmRemove = actions.onConfirmDriveRemove,
                    onRename = actions.onConfirmDriveRename,
                )
            } else if (pageRoute == AppRoute.DriveDevices) {
                DriveDevicesScreen(
                    contentPadding = states.listPadding,
                    onConfirmRestore = actions.onConfirmDriveRestore,
                    onDone = { navigator.pop() },
                )
            } else if (pageRoute == AppRoute.ListenTogether) {
                ListenTogetherScreen(
                    signedIn = states.signedIn,
                    inviteCode = states.activeJamInviteCode,
                    inviteServer = states.activeJamInviteServer,
                    onInviteHandled = {
                        // Managed by host
                    },
                    onSignIn = {
                        navigator.clear()
                        actions.onOpenWebSession(WebSessionMode.SIGN_IN)
                    },
                    contentPadding = states.listPadding,
                    onEditServer = actions.onEditingPartyServer,
                )
            } else if (pageRoute == AppRoute.Equalizer) {
                EqualizerScreen(
                    contentPadding = states.listPadding,
                    onOpenPresets = { navigator.push(AppRoute.EqualizerPresets) },
                )
            } else if (pageRoute == AppRoute.EqualizerPresets) {
                EqualizerPresetPage(
                    onChosen = { navigator.pop() },
                    contentPadding = states.listPadding,
                )
            } else if (pageRoute == AppRoute.About) {
                AboutScreen(
                    contentPadding = states.listPadding,
                    onShowWhatsNew = actions.onShowAboutWhatsNew,
                    onShowUpdateDialog = actions.onShowUpdateDialog,
                )
            } else if (pageRoute is AppRoute.SettingsChoice) {
                SettingsChoicePage(
                    choice = pageRoute.choice,
                    onChosen = { navigator.pop() },
                    onConfirmBitPerfect = actions.onConfirmBitPerfect,
                    contentPadding = states.listPadding,
                    onSpotifyCanvasAuth = actions.onShowSpotifyCanvasAuth,
                )
            } else if (pageRoute == AppRoute.Settings || pageRoute is AppRoute.SettingsSection) {
                SettingsScreen(
                    section = (pageRoute as? AppRoute.SettingsSection)?.category,
                    onOpenSection = { navigator.push(AppRoute.SettingsSection(it)) },
                    onOpenAbout = { navigator.push(AppRoute.About) },
                    onOpenChoice = { navigator.push(AppRoute.SettingsChoice(it)) },
                    windowWidth = states.windowWidth,
                    signedIn = states.signedIn,
                    account = states.account,
                    onSignIn = {
                        navigator.clear()
                        actions.onOpenWebSession(WebSessionMode.SIGN_IN)
                    },
                    onSignOut = { viewModel.signOut() },
                    onAccountScrobbling = { navigator.push(AppRoute.AccountScrobbling) },
                    onEqualizer = { navigator.push(AppRoute.Equalizer) },
                    onOpenReplay = {
                        actions.onOpenReplayLandingPage(ReplayStoryPage.INTRO)
                        navigator.push(AppRoute.Replay)
                    },
                    onDriveBackup = { navigator.push(AppRoute.DriveBackup) },
                    onLyricsSources = actions.onShowLyricsSources,
                    onTranslationLanguage = actions.onShowTranslationLanguage,
                    onSources = { navigator.push(AppRoute.Sources) },
                    onListenTogether = { navigator.push(AppRoute.ListenTogether) },
                    onAppLanguage = actions.onShowAppLanguage,
                    onEditSignatureApi = actions.onEditingSignatureApi,
                    onConfirmBitPerfect = actions.onConfirmBitPerfect,
                    onConfirmFixedVolume = actions.onConfirmFixedVolume,
                    contentPadding = states.listPadding,
                )
            } else if (page != null) {
                val folder = if (page.browseId.isDeviceFolder()) {
                    val isDownloads = page.browseId == "local:downloads"
                    val isCache = page.browseId == CACHE_FOLDER_BROWSE_ID
                    val isDevice = page.browseId == "local:all"
                    val folderSongs = (page.songs as? UiState.Success)?.data.orEmpty()
                    val downloadCollections = remember(folderSongs, states.savedCollections, isDownloads) {
                        if (isDownloads) Downloads.collectionsAmong(folderSongs) else emptyList()
                    }
                    LocalFolder(
                        state = localFolderStates.getOrPut(page.browseId) { LocalFolderState() },
                        sort = actions.onFolderSortOf(page.browseId),
                        viewType = actions.onFolderViewTypeOf(page.browseId),
                        collections = downloadCollections,
                        selectable = isDownloads || isCache,
                        onOpenGroup = viewModel::openLocalGroup,
                        onGroupLongPress = { group ->
                            actions.onOpenBrowseMenu(
                                BrowseTarget(
                                    browseId = group.browseId,
                                    title = group.title,
                                    subtitle = group.subtitle,
                                    thumbnailUrl = group.thumbnailUrl,
                                    type = group.type,
                                    songs = group.songs,
                                    downloadId = group.downloadId,
                                ),
                            )
                        },
                        onDelete = if (isDownloads) {
                            { selected: List<Song> ->
                                scope.launch {
                                    selected.forEach { song -> Downloads.delete(context, song.videoId) }
                                }
                            }
                        } else {
                            null
                        },
                        onUpload = if (isDownloads &&
                            com.music.bitchord.data.webdav.WebDavConfig.isConfigured(states.webdavUrl)
                        ) {
                            { selected: List<Song> -> actions.onUploadToWebDav(selected) }
                        } else {
                            null
                        },
                        onDownload = if (isCache) actions.onDownloadFromCache else null,
                        onRemoveFromCache = if (isCache) actions.onRemoveFromCache else null,
                        tabs = if (isDevice) LocalFolderTab.DEVICE else LocalFolderTab.BASIC,
                        playlists = if (isDevice) states.offlinePlaylistGroups else emptyList(),
                        onCreatePlaylist = if (isDevice) ({ actions.onCreatingOfflinePlaylist() }) else null,
                    )
                } else {
                    null
                }

                val withAlbum: (Song) -> Song = { song ->
                    if (page.type == BrowseType.ALBUM) {
                        song.copy(
                            albumName = song.albumName ?: page.title,
                            albumId = song.albumId ?: page.browseId,
                        )
                    } else {
                        song
                    }
                }

                fun playDetailSongs(songs: List<Song>, index: Int) {
                    if (ListenTogether.state.value.controlsLocked) {
                        actions.onRefusedByHost()
                        return
                    }
                    val source = QueueSource(
                        page.title,
                        PlaybackSourceType.BROWSE,
                        page.browseId,
                        page.type,
                    )
                    val selected = songs.getOrNull(index)
                    val pageKeys =
                        (page.songs as? UiState.Success)?.data.orEmpty().map { it.contextEntryKey() }
                    val pageSongKeys = pageKeys.toHashSet()
                    val useBounded =
                        (page.type == BrowseType.ALBUM || page.type == BrowseType.PLAYLIST) &&
                            selected?.contextEntryKey()?.let(pageSongKeys::contains) == true &&
                            inPageOrder(songs.map { it.contextEntryKey() }, pageKeys) &&
                            !CastController.state.value.casting
                    val latestPage = if (useBounded) {
                        viewModel.takePlaybackContext(page.browseId)
                    } else {
                        null
                    }
                    val latestSongs = latestPage
                        ?.let { (it.songs as? UiState.Success)?.data.orEmpty() }
                        ?.map(withAlbum)
                        ?.takeIf { it.isNotEmpty() }
                    val queue = latestSongs ?: songs.map(withAlbum)
                    if (queue.isEmpty()) return
                    val queueIndex = selected?.let { tapped ->
                        queue.indexOfFirst { it.contextEntryKey() == tapped.contextEntryKey() }
                    }?.takeIf { it >= 0 } ?: index
                    actions.onPlayFrom(
                        queue,
                        queueIndex.coerceIn(queue.indices),
                        source,
                        useBounded,
                        latestPage?.continuation ?: page.continuation,
                    )
                }

                val reordering = states.reorderTarget?.takeIf { it.browseId == page.browseId }
                val isExternal = viewModel.isExternalPlaylist(page.browseId)
                val editablePlaylist = if (states.signedIn && page.type == BrowseType.PLAYLIST && !isExternal) {
                    viewModel.editablePlaylist(page.browseId)
                } else {
                    null
                }
                val isEditableUserPlaylist = editablePlaylist != null || (page.browseId == "VLLM" && states.signedIn)
                DetailScreen(
                    page = page,
                    currentSong = states.player.song,
                    isPlaying = states.player.isPlaying,
                    listState = pageDetailListState,
                    activeShelf = states.detailActiveShelf,
                    onActiveShelfChange = { shelf ->
                        if (shelf != null) navigator.push(AppRoute.ArtistShelf(page.browseId, shelf))
                        else navigator.pop()
                    },
                    onSongClick = { s, i -> playDetailSongs(s, i) },
                    onPlay = { songs ->
                        if (songs.isNotEmpty()) {
                            QueueShuffle.disableForNextQueue()
                            playDetailSongs(songs, 0)
                        }
                    },
                    onSongLongPress = { actions.onOpenSongMenu(withAlbum(it)) },
                    onSongSwipe = { actions.onSongSwipe(withAlbum(it)) },
                    onShuffle = { songs ->
                        if (songs.isNotEmpty()) {
                            QueueShuffle.enableForNextQueue()
                            playDetailSongs(songs, songs.indices.random())
                        }
                    },
                    onSectionItemClick = { item ->
                        val video = shelfSong(item)?.takeIf { item.browseId == null }
                        if (video != null) {
                            actions.onPlayRadio(video, QueueSource(page.title, PlaybackSourceType.BROWSE, page.browseId))
                        } else {
                            item.browseId?.let { id ->
                                val targetType = if (id.startsWith(APPLE_MUSIC_PAGE_PREFIX) ||
                                    id.startsWith("VL") || id.startsWith("PL") || id.startsWith("RD")
                                ) {
                                    BrowseType.PLAYLIST
                                } else {
                                    BrowseType.ALBUM
                                }
                                viewModel.openDetail(
                                    browseId = id,
                                    title = item.title,
                                    subtitle = item.subtitle,
                                    thumbnailUrl = item.thumbnailUrl,
                                    type = targetType,
                                )
                            }
                        }
                    },
                    onSectionItemLongPress = actions.onShelfLongPress,
                    searching = states.detailSearching && page.browseId == states.detail?.browseId,
                    onSearchingChange = actions.onDetailSearchingChange,
                    onArtistClick = { id, name ->
                        viewModel.openDetail(id, name, "Artist", null, BrowseType.ARTIST)
                    },
                    onAddSuggested = { song -> viewModel.addSuggestedSong(page.browseId, song) },
                    onAddSongs = when {
                        page.browseId == "VLLM" && states.signedIn -> ({ AddSongsRequest.showForLikedMusic() })
                        editablePlaylist != null -> ({ AddSongsRequest.showForPlaylist(editablePlaylist) })
                        else -> null
                    },
                    isEditableUserPlaylist = isEditableUserPlaylist,
                    onToggleLibrary = { viewModel.toggleLibrary(page.browseId) },
                    onToggleSubscription = if (states.signedIn) {
                        { viewModel.toggleSubscription(page.browseId) }
                    } else {
                        null
                    },
                    releaseLibrary = states.releaseLibrary,
                    onLoadReleaseLibrary = viewModel::loadReleaseLibrary,
                    onToggleReleaseLibrary = if (states.signedIn) {
                        viewModel::toggleReleaseLibrary
                    } else {
                        null
                    },
                    songSort = states.songSort,
                    folder = folder,
                    contentPadding = states.listPadding,
                    reorder = if (reordering != null) {
                        { palette ->
                            ReorderPlaylistPage(
                                playlist = reordering,
                                entries = states.reorderEntries,
                                saving = states.reorderSaving,
                                palette = palette,
                                contentPadding = PaddingValues(
                                    bottom = states.listPadding.calculateBottomPadding(),
                                ),
                                onClose = { actions.onReorderTargetChange(null) },
                                onSave = { reordered ->
                                    val original = (states.reorderEntries as? UiState.Success)?.data.orEmpty()
                                    actions.onReorderSavingChange(true)
                                    viewModel.reorderPlaylist(reordering, original, reordered) { saved ->
                                        actions.onReorderSavingChange(false)
                                        actions.onShowQueueNotice(
                                            context.getString(
                                                if (saved) R.string.playlist_reordered else R.string.reorder_failed,
                                            ),
                                        )
                                        if (saved && states.reorderTarget == reordering) actions.onReorderTargetChange(null)
                                    }
                                },
                            )
                        }
                    } else {
                        null
                    },
                    onRetry = { viewModel.reloadDetail(page.browseId) },
                    onLoadRelatedPlaylists = { p ->
                        viewModel.loadPlaylistRecommendations(
                            browseId = p.browseId,
                            title = p.title,
                            songs = (p.songs as? UiState.Success)?.data.orEmpty(),
                            curatorName = p.subtitle,
                        )
                    },
                    onLoadAlbumRelated = { p ->
                        val songs = (p.songs as? UiState.Success)?.data.orEmpty()
                        val rawArtist = songs.firstOrNull { it.artist.isNotBlank() }?.artist ?: p.subtitle
                        val primaryArtist = AlbumRelatedRepository.extractPrimaryArtist(rawArtist)
                        val artistId = songs.firstOrNull { !it.artistId.isNullOrBlank() }?.artistId
                        viewModel.loadAlbumRelatedSections(
                            browseId = p.browseId,
                            title = p.title,
                            artistName = primaryArtist,
                            songs = songs,
                            artistBrowseId = artistId,
                        )
                    },
                )
            } else if (states.recognitionOpen) {
                RecognitionScreen(
                    onPlayResult = actions.onPlayRecognizedSong,
                )
            } else when ((destination as? AppDestination.Tab)?.index ?: states.selectedTab) {
                TAB_HOME -> HomeScreen(
                    state = states.visibleHomeState,
                    listState = lists.homeListState,
                    currentSong = states.player.song,
                    isPlaying = states.player.isPlaying,
                    title = stringResource(R.string.listen_now),
                    festiveLogo = states.diwaliFestive,
                    onFestiveLogoClick = actions.onShowDiwali,
                    signedIn = states.signedIn,
                    onSignIn = { actions.onOpenWebSession(WebSessionMode.SIGN_IN) },
                    onItemClick = onHomeItemClick,
                    onItemLongPress = actions.onShelfLongPress,
                    onRetry = viewModel::loadHome,
                    refreshing = MainViewModel.Feed.HOME in states.refreshing,
                    onRefresh = { viewModel.refresh(MainViewModel.Feed.HOME) },
                    pullState = lists.homePull,
                    contentPadding = states.listPadding,
                    onLoadMore = viewModel::loadMoreHome,
                    loadingMore = states.homeLoadingMore,
                    recentlyPlayedLoading = states.homeRecentlyPlayedLoading && !states.hideRecents,
                    topPicks = states.topPicks,
                    topPicksLoading = states.topPicksLoading,
                    listenerName = states.account?.name,
                    onTopPickClick = actions.onOpenTopPick.takeUnless { states.hideTopPicks },
                    onMoodClick = actions.onPlayMood.takeUnless { states.hideHomeMoods },
                    onMoodsShowAll = { navigator.push(AppRoute.Moods) },
                    artworkMotion = states.artworkMotion && !states.showDiwali,
                    onTopPickLongPress = onHomeTopPickLongPress,
                    editorialShelves = if (hideHomeEditorial) emptyList() else homeEditorialShelves,
                    daypartShelves = daypartShelves,
                    personalizedEssentialsShelf = personalizedEssentialsShelf,
                    stationsForYou = stationsForYou,
                    onStationClick = actions.onOpenTopPick,
                    playlistsForYou = playlistsForYou,
                    onPlaylistClick = actions.onOpenTopPick,
                )
                TAB_EXPLORE -> states.selectedMoodGenre?.let { category ->
                    MoodGenrePlaylistsScreen(
                        title = category.title,
                        state = states.moodGenreShelves,
                        listState = lists.moodGenreListState,
                        onItemClick = { item ->
                            item.videoId?.let { videoId ->
                                actions.onPlayRadio(
                                    Song(
                                        videoId = videoId,
                                        title = item.title,
                                        artist = InnertubeParser.artistFromSubtitle(item.subtitle),
                                        thumbnailUrl = item.thumbnailUrl,
                                        isVideo = item.isVideo,
                                    ),
                                    QueueSource(
                                        category.title,
                                        PlaybackSourceType.EXPLORE,
                                        category.browseId,
                                    ),
                                )
                            } ?: item.browseId?.let { browseId ->
                                viewModel.openDetail(
                                    browseId = browseId,
                                    title = item.title,
                                    subtitle = item.subtitle,
                                    thumbnailUrl = item.thumbnailUrl,
                                )
                            }
                        },
                        onRetry = { viewModel.openMoodGenre(category) },
                        contentPadding = states.listPadding,
                    )
                } ?: ExploreScreen(
                    moodState = states.exploreState,
                    shelves = exploreShelves,
                    listState = lists.exploreListState,
                    onCategoryClick = viewModel::openMoodGenre,
                    onShelfItemClick = onExploreShelfItemClick,
                    onShelfMore = { shelf -> navigator.push(AppRoute.LibraryShowAll(shelf)) },
                    onRetry = viewModel::loadExplore,
                    refreshing = MainViewModel.Feed.EXPLORE in states.refreshing,
                    onRefresh = { viewModel.refresh(MainViewModel.Feed.EXPLORE) },
                    pullState = lists.explorePull,
                    contentPadding = states.listPadding,
                    onPlaySong = { song ->
                        actions.onPlayRadio(
                            song,
                            QueueSource(
                                if (java.util.Locale.getDefault().language == "es") "Canciones nuevas destacadas" else "Top New Songs",
                                PlaybackSourceType.EXPLORE,
                            ),
                        )
                    },
                    onSongMenu = actions.onOpenSongMenu,
                    getRowScrollPosition = viewModel::getExploreRowScroll,
                    onSaveRowScrollPosition = viewModel::saveExploreRowScroll,
                )
                TAB_RADIO -> RadioScreen(
                    listState = radioListState,
                    currentSong = states.player.song,
                    contentPadding = states.listPadding,
                    pullToRefreshState = lists.radioPull,
                    onRadioRefreshStateChanged = { refreshing -> viewModel.setRadioRefreshing(refreshing) },
                    onPlayMix = onRadioPlayMix,
                    onSubpageBackHandlerChanged = actions.onRadioSubpageBackChanged,
                    onPlayStation = onRadioPlayStation,
                    onStartStation = actions.onStartRadio,
                    onStationLongPress = onRadioStationLongPress,
                    onSongLongPress = actions.onOpenSongMenu,
                )
                TAB_SEARCH -> SearchScreen(
                    query = states.query,
                    onQueryChange = viewModel::onQueryChange,
                    filter = states.filter,
                    currentSong = states.player.song,
                    isPlaying = states.player.isPlaying,
                    onFilterChange = viewModel::onFilterChange,
                    results = states.results,
                    appleMusicPlaylists = viewModel.appleMusicPlaylists.collectAsStateWithLifecycle().value,
                    loadingMore = states.searchLoadingMore,
                    onLoadMore = viewModel::loadMoreSearchResults,
                    listState = lists.searchListState,
                    scrollResetTrigger = states.searchScrollReset,
                    focusRequested = states.searchFocusRequested,
                    onFocusHandled = actions.onSearchFocusHandled,
                    onSongClick = { songs, index ->
                        songs.getOrNull(index)?.let { song ->
                            viewModel.recordEntity(SearchHistoryEntity(
                                id = song.videoId,
                                title = song.title,
                                subtitle = song.artist.ifEmpty { "" },
                                artworkUrl = song.thumbnailUrl,
                                entityType = EntityType.TRACK,
                            ))
                            actions.onPlayRadio(song, QueueSource(states.searchLabel, PlaybackSourceType.SEARCH))
                        }
                    },
                    onSongLongPress = actions.onOpenSongMenu,
                    onSongSwipe = actions.onSongSwipe,
                    onTopResultPlay = { song ->
                        viewModel.recordEntity(SearchHistoryEntity(
                            id = song.videoId,
                            title = song.title,
                            subtitle = song.artist.ifEmpty { "" },
                            artworkUrl = song.thumbnailUrl,
                            entityType = EntityType.TRACK,
                        ))
                        actions.onPlayRadio(song, QueueSource(states.searchLabel, PlaybackSourceType.SEARCH))
                    },
                    onTopResultPlaylist = { song ->
                        viewModel.recordEntity(SearchHistoryEntity(
                            id = song.videoId,
                            title = song.title,
                            subtitle = song.artist.ifEmpty { "" },
                            artworkUrl = song.thumbnailUrl,
                            entityType = EntityType.TRACK,
                        ))
                        viewModel.loadPlaylists()
                        actions.onOpenSongMenu(song)
                    },
                    onBrowseClick = { item ->
                        viewModel.recordEntity(SearchHistoryEntity(
                            id = item.browseId ?: "",
                            title = item.title,
                            subtitle = item.subtitle.ifBlank { "" },
                            artworkUrl = item.thumbnailUrl,
                            entityType = when (item.type) {
                                BrowseType.ALBUM -> EntityType.ALBUM
                                BrowseType.ARTIST -> EntityType.ARTIST
                                BrowseType.PLAYLIST -> EntityType.PLAYLIST
                                BrowseType.OTHER -> EntityType.OTHER
                            },
                        ))
                        viewModel.openDetail(
                            browseId = item.browseId,
                            title = item.title,
                            subtitle = item.subtitle,
                            thumbnailUrl = item.thumbnailUrl,
                            type = item.type,
                        )
                    },
                    onBrowseLongPress = { item ->
                        if (item.type != BrowseType.ARTIST) {
                            val recordId = item.browseId?.let { Downloads.recordIdOf(it) ?: it }?.takeIf { it in states.savedCollections }
                            actions.onOpenBrowseMenu(
                                BrowseTarget(
                                    browseId = item.browseId,
                                    title = item.title,
                                    subtitle = item.subtitle,
                                    thumbnailUrl = item.thumbnailUrl,
                                    type = item.type,
                                    downloadId = recordId,
                                ),
                            )
                        }
                    },
                    history = states.searchHistory,
                    suggestions = states.searchSuggestions,
                    typeaheadResults = viewModel.typeaheadResults.collectAsStateWithLifecycle().value,
                    onSubmit = viewModel::submitSearch,
                    onSuggestionClick = viewModel::searchFor,
                    onHistoryClick = { entity ->
                        when (entity.entityType) {
                            EntityType.TRACK -> {
                                actions.onPlayRadio(
                                    Song(
                                        videoId = entity.id,
                                        title = entity.title,
                                        artist = entity.subtitle,
                                        thumbnailUrl = entity.artworkUrl,
                                    ),
                                    QueueSource(entity.title, PlaybackSourceType.SEARCH),
                                )
                            }
                            EntityType.ALBUM, EntityType.ARTIST, EntityType.PLAYLIST, EntityType.OTHER -> {
                                viewModel.openDetail(
                                    browseId = entity.id,
                                    title = entity.title,
                                    subtitle = entity.subtitle,
                                    thumbnailUrl = entity.artworkUrl,
                                )
                            }
                        }
                    },
                    onHistoryRemove = viewModel::removeSearch,
                    onHistoryClear = viewModel::clearSearchHistory,
                    onTypeaheadLongPress = actions.onOpenSongMenu,
                    contentPadding = states.listPadding,
                    topPadding = actions.onTopBarHeight() + 4.dp,
                    showField = false,
                    sourceSwitcher = {
                        SegmentedControl(
                            options = listOf(
                                stringResource(R.string.search_source_youtube),
                                stringResource(R.string.library),
                                stringResource(com.soundchord.features.R.string.radio_name),
                                stringResource(R.string.search_source_addons),
                            ),
                            selectedIndex = states.searchSource.ordinal,
                            onSelect = { index -> actions.onSearchSourceChange(SearchSource.entries[index]) },
                        )
                    },
                    searchingLibrary = states.searchSource == SearchSource.LIBRARY,
                    searchingRadio = states.searchSource == SearchSource.RADIO,
                    radioContent = if (states.searchSource == SearchSource.RADIO) {
                        {
                            RadioScreen(
                                searchQuery = states.query,
                                onPlayStation = { song ->
                                    actions.onPlayRadio(song, QueueSource(states.radioLabel, PlaybackSourceType.QUEUE, id = song.playbackSourceId))
                                },
                                onStationLongPress = { station, isFavorite, anchor, toggleFavorite ->
                                    actions.onStationActions(StationMenuTarget(station, isFavorite, anchor, toggleFavorite))
                                },
                                onSongLongPress = actions.onOpenSongMenu,
                            )
                        }
                    } else null,
                    libraryResults = states.libraryResults,
                    searchingAddons = states.searchSource == SearchSource.ADDONS,
                    addonResults = states.addonResults,
                    isBlocked = { song ->
                        val configId = SourceRegistry.parseTrackKey(song.videoId)?.first
                        !SourceRegistry.permitsForPlayback(configId, states.addonCeiling)
                    },
                    onBlockedSongClick = actions.onBlockedAddonSong,
                    addonHistory = viewModel.addonSearchHistory.collectAsStateWithLifecycle().value,
                    onAddonHistoryClick = { term ->
                        viewModel.onQueryChange(term)
                        viewModel.submitSearch()
                    },
                    onAddonHistoryRemove = viewModel::removeAddonSearch,
                    onAddonHistoryClear = viewModel::clearAddonSearchHistory,
                    onLibrarySongClick = { songs, index ->
                        actions.onPlayFrom(
                            songs,
                            index,
                            if (states.searchSource == SearchSource.ADDONS) {
                                QueueSource(
                                    context.getString(R.string.search_source_addons),
                                    PlaybackSourceType.BROWSE,
                                    "addons:search",
                                )
                            } else {
                                QueueSource(
                                    context.getString(R.string.local_music),
                                    PlaybackSourceType.BROWSE,
                                    "local:all",
                                )
                            },
                            false,
                            null,
                        )
                    },
                )
                else -> LibraryScreen(
                    signedIn = states.signedIn,
                    state = states.libraryState,
                    listState = lists.libraryListState,
                    onShelfItemClick = onLibraryShelfClick,
                    onShelfItemLongPress = actions.onBrowseLongPress,
                    onNewPlaylist = actions.onCreatingPlaylist,
                    onImportSpotifyPlaylist = actions.onShowSpotifyImport,
                    onImportAppleMusicPlaylist = { AppleMusicImportController.showDialog = true },
                    onShowAll = { shelf -> navigator.push(AppRoute.LibraryShowAll(shelf)) },
                    replay = {
                        LibraryReplayEntry(
                            cards = states.replayCards,
                            loading = states.replay.loading,
                            holder = states.account?.name.orEmpty(),
                            memberSince = states.replay.memberSince,
                            onOpenReplay = { page ->
                                actions.onOpenReplayLandingPage(page)
                                navigator.push(AppRoute.Replay)
                            },
                        )
                    },
                    onSignIn = { actions.onOpenWebSession(WebSessionMode.SIGN_IN) },
                    onRetry = viewModel::loadLibrary,
                    refreshing = MainViewModel.Feed.LIBRARY in states.refreshing,
                    onRefresh = { viewModel.refresh(MainViewModel.Feed.LIBRARY) },
                    pullState = lists.libraryPull,
                    contentPadding = states.listPadding,
                    links = actions.onLibraryLinks(),
                    deviceItems = actions.onLibraryDeviceItems(states.downloadedReleases) + states.localPlaylistItems,
                )
            }
        }

        val barsMotion = states.artworkMotion && transition.targetState == EnterExitState.Visible
        CompositionLocalProvider(
            LocalPlayingBarsMotion provides barsMotion,
            com.music.bitchord.ui.components.LocalCoverVisibilityScope provides this@AnimatedContent,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (pageRoute == AppRoute.RadioHistory || pageRoute == AppRoute.RecognitionHistory) {
                            Modifier.hazeSource(hazeState)
                        } else Modifier
                    ),
            ) {
                if (destination is AppDestination.Tab) {
                    if (destination == targetState || (targetState !is AppDestination.Tab && destination.index == states.selectedTab)) {
                        pageContent()
                    }
                } else {
                    pageKeysSeen += key
                    pageStates.SaveableStateProvider(key, pageContent)
                }
            }
        }
    }
        }
    }
}

/**
 * [shown] keeps [page]'s own order: every entry is the page's, in the page's
 * sequence, though a search may have left some out. A sort breaks that, and
 * then the page snapshot can't stand in for the list on screen (#1141).
 */
internal fun inPageOrder(shown: List<String>, page: List<String>): Boolean {
    val rest = page.iterator()
    return shown.all { key -> rest.asSequence().any { it == key } }
}
