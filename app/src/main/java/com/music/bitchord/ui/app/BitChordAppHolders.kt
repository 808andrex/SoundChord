package com.music.bitchord.ui.app

import android.content.Context
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import com.music.bitchord.auth.GoogleAccountSession
import com.music.bitchord.auth.WebSessionMode
import com.music.bitchord.data.AppUpdateChecker
import com.music.bitchord.data.LocalGroup
import com.music.bitchord.data.OfflinePlaylist
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.listentogether.ServerConnectionState
import com.music.bitchord.data.lyrics.LyricLine
import com.music.bitchord.data.lyrics.LyricsSource
import com.music.bitchord.data.backup.RemoteDevice
import com.music.bitchord.data.model.Account
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.DetailPage
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.LibraryPage
import com.music.bitchord.data.model.LibraryState
import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.model.MoodGenre
import com.music.bitchord.data.model.MoodGenreSection
import com.music.bitchord.data.model.PlaybackSourceType
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchHistoryEntity
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.TopPick
import com.music.bitchord.data.model.TopPickMood
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.UserPlaylist
import com.music.bitchord.data.settings.AudioQuality
import com.music.bitchord.data.settings.BitPerfectMode
import com.music.bitchord.data.settings.LibrarySort
import com.music.bitchord.data.settings.LibraryViewType
import com.music.bitchord.data.settings.LocalMusicSort
import com.music.bitchord.data.settings.SongSort
import com.music.bitchord.data.sources.SourceConfig
import com.music.bitchord.data.sources.SourceRegistry
import com.music.bitchord.download.DownloadTarget
import com.music.bitchord.download.SavedCollection
import com.music.bitchord.playback.PlayerState
import com.music.bitchord.playback.QueueSource
import com.soundchord.features.radio.data.PreferencesRadioFavoritesStore
import com.soundchord.features.radio.data.RadioFavoritesStore
import com.soundchord.features.radio.data.RadioHistoryStore
import com.soundchord.features.radio.data.RadioStation
import com.soundchord.features.recognition.data.RecognitionHistoryEntry
import com.soundchord.features.recognition.model.RecognitionResult
import com.music.bitchord.ui.LyricsProviderState
import com.music.bitchord.ui.MainViewModel
import com.music.bitchord.ui.SearchSource
import com.music.bitchord.ui.components.BottomTab
import com.music.bitchord.ui.components.BrowseTarget
import com.music.bitchord.ui.components.HeldItem
import com.music.bitchord.ui.components.QueueActionNotice
import com.music.bitchord.ui.components.StationMenuTarget
import com.music.bitchord.ui.components.backdrop.backdrops.LayerBackdrop
import com.music.bitchord.ui.components.backdrop.backdrops.SnapshotBackdrop
import com.music.bitchord.ui.components.floatingtabbar.FloatingTabBarScrollConnection
import com.music.bitchord.ui.navigation.AppRoute
import com.music.bitchord.ui.navigation.RouteEntry
import com.music.bitchord.ui.player.PlayerDock
import com.music.bitchord.ui.player.PlayerSheetMotion
import com.music.bitchord.ui.replay.ReplayHeroCard
import com.music.bitchord.data.stats.ReplayPeriod
import com.music.bitchord.ui.replay.ReplayState
import com.music.bitchord.ui.replay.ReplayStoryPage
import com.music.bitchord.ui.screens.DiscordDialog
import com.music.bitchord.ui.screens.DriveBackupAlerts
import com.music.bitchord.ui.screens.LibraryLink
import com.music.bitchord.ui.screens.LocalFolderState
import com.music.bitchord.ui.theme.ArtworkPalette
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Immutable
internal class BitChordNavState(
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
    val glassActive: Boolean,
    val glassSamplesBackdrop: Boolean,
    val detailListStates: MutableMap<String, LazyListState>,
    val localFolderStates: MutableMap<String, LocalFolderState>,
    val pageStates: SaveableStateHolder,
    val pageKeysSeen: MutableSet<String>,
    val radioHistoryStore: RadioHistoryStore,
    val radioFavoritesStore: PreferencesRadioFavoritesStore,
    val radioFavoriteScope: CoroutineScope,
    val homeListState: LazyListState,
    val exploreListState: LazyListState,
    val moodGenreListState: LazyListState,
    val libraryListState: LazyListState,
    val historyListState: LazyListState,
    val libraryShowAllGridState: LazyGridState,
    val moodsGridState: LazyGridState,
    val searchListState: LazyListState,
    val radioListState: LazyListState,
    val route: AppRoute?,
    val detailPalette: ArtworkPalette,
    val detailScrolled: Boolean,
    val isReleaseDetail: Boolean,
    val topDetail: DetailPage?,
    val detailSongSorts: Map<String, SongSort>,
    val downloadedMusicSort: LocalMusicSort,
    val localMusicSort: LocalMusicSort,
    val downloadedMusicViewType: LibraryViewType,
    val localMusicViewType: LibraryViewType,
    val activeAccountId: String?,
    val googleAccounts: List<GoogleAccountSession>,
    val currentFeed: MainViewModel.Feed?,
    val currentPull: PullToRefreshState?,
    val homePull: PullToRefreshState,
    val explorePull: PullToRefreshState,
    val libraryPull: PullToRefreshState,
    val radioPull: PullToRefreshState,
    val historyLabel: String,
    val radioLabel: String,
    val replayLabel: String,
    val searchLabel: String,
    val replayListState: LazyListState,
    val progressiveBlur: Boolean,
    val tabs: List<BottomTab>,
    val replayScrolled: Boolean,
    val scrolled: Boolean,
    val partyServerStatus: ServerConnectionState,
    val updateNotice: AppUpdateChecker.UpdateInfo?,
    val recognitionFabVisible: Boolean,
    val librarySort: LibrarySort,
    val reduceDynamicBlur: Boolean,
)

@Immutable
internal class BitChordPlayerUiState(
    val playerRaised: Boolean,
    val playerSong: Song?,
    val showNowPlaying: Boolean,
    val reducePlayerMotion: Boolean,
    val reduceDynamicBlur: Boolean,
    val playerLiveCanvas: Boolean,
    val player: PlayerState,
    val playPauseBusy: Boolean,
    val switchingAudioVersion: Boolean,
    val optimisticVersionSong: Song?,
    val convertedAudioId: String?,
    val convertedVideoId: String?,
    val keepVideoId: String?,
    val activeRadioSeed: Pair<String, String>?,
    val partyState: ListenTogether.State,
    val account: Account?,
    val songActions: Song?,
    val songMenuOrigin: HeldItem?,
    val songMenuAnchor: Rect?,
    val menuFromPlayer: Boolean,
    val controlsLocked: Boolean,
    val shuffleEnabled: Boolean,
    val autoplayEnabled: Boolean,
    val signedIn: Boolean,
    val likeStatuses: Map<String, LikeStatus>,
    val query: String,
    val filter: SearchFilter,
    val results: UiState<List<SearchResult>>?,
    val lyrics: List<LyricLine>?,
    val lyricsSource: LyricsSource?,
    val lyricsProviderStates: Map<LyricsSource, LyricsProviderState>,
    val lyricsChecked: Boolean,
    val showLyricsOffset: Boolean,
    val queueLabel: String,
    val queueNotice: QueueActionNotice?,
    val convertedFromVideo: Song?,
    val convertedFromAudio: Song?,
    val playlists: List<UserPlaylist>,
    val playlistsLoading: Boolean,
    val offlinePlaylists: List<OfflinePlaylist>,
    val addonCeiling: AudioQuality,
    val detail: DetailPage?,
    val versionContext: Context,
    val coveredByPlayer: Boolean,
    val activeDock: PlayerDock?,
    val playerBackdrop: SnapshotBackdrop,
    val livePlayerBackdrop: LayerBackdrop,
)

@Immutable
internal class BitChordSheetsUiState(
    val pageBackdrop: SnapshotBackdrop,
    val reduceDynamicBlur: Boolean,
    val playerRaised: Boolean,
    val songActions: Song?,
    val songMenuOrigin: HeldItem?,
    val songMenuAnchor: Rect?,
    val menuFromPlayer: Boolean,
    val browseActions: BrowseTarget?,
    val browseMenuOrigin: HeldItem?,
    val browseMenuAnchor: Rect?,
    val browseRenameInSheet: Boolean,
    val stationActions: StationMenuTarget?,
    val replay: ReplayState,
    val replayStory: ReplayStoryPage?,
    val showReplayShare: Boolean,
    val replaySharePage: ReplayStoryPage?,
    val account: Account?,
    val playlistTarget: Song?,
    val creatingPlaylist: Boolean,
    val playlists: List<UserPlaylist>,
    val playlistsLoading: Boolean,
    val offlinePlaylistTarget: Song?,
    val creatingOfflinePlaylist: Boolean,
    val offlinePlaylists: List<OfflinePlaylist>,
    val showSpotifyImportDialog: Boolean,
    val signedIn: Boolean,
    val editPlaylistTarget: UserPlaylist?,
    val detailStack: List<DetailPage>,
    val libraryState: UiState<LibraryPage>,
    val likeStatuses: Map<String, LikeStatus>,
    val query: String,
    val results: UiState<List<SearchResult>>?,
    val filter: SearchFilter,
    val onboardingShown: MutableTransitionState<Boolean>,
    val webSession: WebSessionMode?,
    val googleAccounts: List<GoogleAccountSession>,
    val activeAccountId: String?,
    val activeProfileId: String?,
    val showDiwali: Boolean,
    val showUpdateDialog: Boolean,
    val onboardingComplete: Boolean,
    val updateAvailable: AppUpdateChecker.UpdateInfo?,
    val showLyricsSources: Boolean,
    val songSortMenuOpen: Boolean,
    val songSort: SongSort,
    val detail: DetailPage?,
    val folderMenuOpen: Boolean,
    val showAccountSelector: Boolean,
    val showAppLanguage: Boolean,
    val showTranslationLanguage: Boolean,
    val showListenBrainzLogin: Boolean,
    val listenBrainzToken: String,
    val showLastfmLogin: Boolean,
    val showWebDavEditor: Boolean,
    val showSmbEditor: Boolean,
    val showDiscordLogin: Boolean,
    val showSpotifyLogin: Boolean,
    val showSpotifyCanvasAuth: Boolean,
    val discordDialog: DiscordDialog?,
    val editingPartyServer: Boolean,
    val editingSignatureApi: Boolean,
    val editingSource: SourceConfig?,
    val choosingSourceQuality: SourceConfig?,
    val explainingOutdatedAddon: SourceConfig?,
    val confirmJioSaavn: Boolean,
    val confirmBitPerfect: BitPerfectMode?,
    val confirmFixedVolume: Boolean,
    val driveAlerts: DriveBackupAlerts,
    val blockedAddonSong: Song?,
    val addonLeftOutOfDownload: Pair<String, SourceRegistry.DownloadExclusion>?,
    val showAboutWhatsNew: Boolean,
    val recognitionOpen: Boolean,
)

@Immutable
internal class BitChordAppState(
    val nav: BitChordNavState,
    val player: BitChordPlayerUiState,
    val sheets: BitChordSheetsUiState,
)

@Immutable
internal class BitChordAppActions(
    val play: (List<Song>, Int) -> Unit,
    val playFrom: (List<Song>, Int, QueueSource, Boolean, String?) -> Unit,
    val playRadio: (Song, QueueSource) -> Unit,
    val playRecognizedSong: (RecognitionResult) -> Unit,
    val playRecognitionHistoryEntry: (RecognitionHistoryEntry) -> Unit,
    val startRadio: (Song) -> Unit,
    val openTopPick: (TopPick, String) -> Unit,
    val playMood: (TopPickMood, String) -> Unit,
    val addToQueue: (Song) -> Unit,
    val playNext: (Song) -> Unit,
    val onSongSwipe: (Song) -> Unit,
    val openByName: (String?, String, String?, BrowseType, String?) -> Unit,
    val queueSongs: (List<Song>, Boolean) -> Unit,
    val addSongsToQueue: (List<Song>) -> Unit,
    val playSongsNext: (List<Song>) -> Unit,
    val onBrowseLongPress: (ShelfItem) -> Unit,
    val onShelfLongPress: (ShelfItem) -> Unit,
    val withBrowseSongs: (BrowseTarget, (List<Song>) -> Unit) -> Unit,
    val onSearchSourceChange: (SearchSource) -> Unit,
    val onLibraryItemClick: (ShelfItem) -> Unit,
    val startDownload: (List<Song>, DownloadTarget?) -> Unit,
    val downloadFromCache: (List<Song>) -> Unit,
    val removeFromCache: (List<Song>) -> Unit,
    val downloadSong: (Song) -> Unit,
    val openSongMenu: (Song) -> Unit,
    val openPlayerSongMenu: (Song) -> Unit,
    val openBrowseMenu: (BrowseTarget) -> Unit,
    val folderSortOf: (String) -> LocalMusicSort,
    val folderViewTypeOf: (String) -> LibraryViewType,
    val likeStatusOf: (Song) -> LikeStatus,
    val toggleLikeOf: (Song) -> Unit,
    val downloadIdFor: (String?) -> String?,
    val showQueueNotice: (String) -> Unit,
    val showHostOnlyNotice: () -> Unit,
    val refusedByHost: () -> Boolean,
    val togglePlayPause: () -> Unit,
    val uploadToWebDav: (List<Song>) -> Unit,
    val dismissPlayer: () -> Unit,
    val switchToMusicOnly: (Song, Boolean) -> Unit,
    val switchToVideo: (Song, Boolean) -> Unit,
    // Setters
    val onSelectTab: (Int) -> Unit,
    val onRecognitionOpenChange: (Boolean) -> Unit,
    val onResetRecognition: () -> Unit,
    val onShowRecognitionHistoryClearConfirmationChange: (Boolean) -> Unit,
    val onShowRadioHistoryClearConfirmationChange: (Boolean) -> Unit,
    val onRadioSubpageBackChange: ((() -> Unit)?) -> Unit,
    val onReplayStoryChange: (ReplayStoryPage?) -> Unit,
    val onShowReplayShareChange: (Boolean) -> Unit,
    val onReplaySharePageChange: (ReplayStoryPage?) -> Unit,
    val onReplayLandingPageChange: (ReplayStoryPage) -> Unit,
    val onShowSpotifyCanvasAuthChange: (Boolean) -> Unit,
    val onEditingSourceChange: (SourceConfig?) -> Unit,
    val onChoosingSourceQualityChange: (SourceConfig?) -> Unit,
    val onExplainingOutdatedAddonChange: (SourceConfig?) -> Unit,
    val onConfirmJioSaavnChange: (Boolean) -> Unit,
    val onConfirmBitPerfectChange: (BitPerfectMode?) -> Unit,
    val onConfirmFixedVolumeChange: (Boolean) -> Unit,
    val onConfirmDriveDisconnect: () -> Unit,
    val onConfirmDriveRemove: (RemoteDevice) -> Unit,
    val onConfirmDriveRestore: (RemoteDevice) -> Unit,
    val onConfirmDriveRename: (String) -> Unit,
    val onBlockedAddonSongChange: (Song?) -> Unit,
    val onAddonLeftOutOfDownloadChange: (Pair<String, SourceRegistry.DownloadExclusion>?) -> Unit,
    val onEditingPartyServerChange: (Boolean) -> Unit,
    val onEditingSignatureApiChange: (Boolean) -> Unit,
    val onShowLyricsSourcesChange: (Boolean) -> Unit,
    val onShowAppLanguageChange: (Boolean) -> Unit,
    val onShowTranslationLanguageChange: (Boolean) -> Unit,
    val onShowAccountSelectorChange: (Boolean) -> Unit,
    val onShowListenBrainzLoginChange: (Boolean) -> Unit,
    val onShowLastfmLoginChange: (Boolean) -> Unit,
    val onShowDiscordLoginChange: (Boolean) -> Unit,
    val onShowSpotifyLoginChange: (Boolean) -> Unit,
    val onShowWebDavEditorChange: (Boolean) -> Unit,
    val onShowSmbEditorChange: (Boolean) -> Unit,
    val onDiscordDialogChange: (DiscordDialog?) -> Unit,
    val onShowLyricsOffsetChange: (Boolean) -> Unit,
    val onStationActionsChange: (StationMenuTarget?) -> Unit,
    val onSongActionsChange: (Song?) -> Unit,
    val onSongMenuOriginChange: (HeldItem?) -> Unit,
    val onSongMenuAnchorChange: (Rect?) -> Unit,
    val onMenuFromPlayerChange: (Boolean) -> Unit,
    val onMenuFromHistoryChange: (Boolean) -> Unit,
    val onBrowseActionsChange: (BrowseTarget?) -> Unit,
    val onBrowseMenuOriginChange: (HeldItem?) -> Unit,
    val onBrowseMenuAnchorChange: (Rect?) -> Unit,
    val onBrowseRenameInSheetChange: (Boolean) -> Unit,
    val onEditPlaylistTargetChange: (UserPlaylist?) -> Unit,
    val onPlaylistTargetChange: (Song?) -> Unit,
    val onCreatingPlaylistChange: (Boolean) -> Unit,
    val onOfflinePlaylistTargetChange: (Song?) -> Unit,
    val onCreatingOfflinePlaylistChange: (Boolean) -> Unit,
    val onShowSpotifyImportDialogChange: (Boolean) -> Unit,
    val onWebSessionChange: (WebSessionMode?) -> Unit,
    val onShowAboutWhatsNewChange: (Boolean) -> Unit,
    val onShowUpdateDialogChange: (Boolean) -> Unit,
    val onShowDiwaliChange: (Boolean) -> Unit,
    val onSongSortMenuOpenChange: (Boolean) -> Unit,
    val onFolderMenuOpenChange: (Boolean) -> Unit,
    val onDetailSearchingChange: (Boolean) -> Unit,
    val onShowNowPlayingChange: (Boolean) -> Unit,
    val onPlayerLiveCanvasChange: (Boolean) -> Unit,
    val onActiveRadioSeedChange: (Pair<String, String>?) -> Unit,
    val onReorderTargetChange: (UserPlaylist?) -> Unit,
    val onReorderEntriesChange: (UiState<List<Song>>) -> Unit,
    val onReorderSavingChange: (Boolean) -> Unit,
    val onSearchFocusRequestedChange: (Boolean) -> Unit,
    val onOptimisticVersionSongChange: (Song?) -> Unit,
    val onConvertedFromVideoChange: (Song?, String?) -> Unit,
    val onConvertedFromAudioChange: (Song?, String?) -> Unit,
    val onKeepVideoIdChange: (String?) -> Unit,
    val onActiveTabTap: () -> Unit = {},
)

@Stable
internal class BitChordSheetStateHolder {
    var webSession by mutableStateOf<WebSessionMode?>(null)
    var replayLandingPage by mutableStateOf(ReplayStoryPage.INTRO)
    var replayStory by mutableStateOf<ReplayStoryPage?>(null)
    var showReplayShare by mutableStateOf(false)
    var replaySharePage by mutableStateOf<ReplayStoryPage?>(null)
    var showSpotifyCanvasAuth by mutableStateOf(false)
    var editingSource by mutableStateOf<SourceConfig?>(null)
    var choosingSourceQuality by mutableStateOf<SourceConfig?>(null)
    var explainingOutdatedAddon by mutableStateOf<SourceConfig?>(null)
    var confirmJioSaavn by mutableStateOf(false)
    var confirmBitPerfect by mutableStateOf<BitPerfectMode?>(null)
    var confirmFixedVolume by mutableStateOf(false)
    val driveAlerts = DriveBackupAlerts()
    var blockedAddonSong by mutableStateOf<Song?>(null)
    var addonLeftOutOfDownload by mutableStateOf<Pair<String, SourceRegistry.DownloadExclusion>?>(null)
    var editingPartyServer by mutableStateOf(false)
    var editingSignatureApi by mutableStateOf(false)
    var showLyricsSources by mutableStateOf(false)
    var showAppLanguage by mutableStateOf(false)
    var showTranslationLanguage by mutableStateOf(false)
    var showAccountSelector by mutableStateOf(false)
    var showListenBrainzLogin by mutableStateOf(false)
    var showLastfmLogin by mutableStateOf(false)
    var showWebDavEditor by mutableStateOf(false)
    var showSmbEditor by mutableStateOf(false)
    var showDiscordLogin by mutableStateOf(false)
    var showSpotifyLogin by mutableStateOf(false)
    var discordDialog by mutableStateOf<DiscordDialog?>(null)
    var songActions by mutableStateOf<Song?>(null)
    var stationActions by mutableStateOf<StationMenuTarget?>(null)
    var showLyricsOffset by mutableStateOf(false)
    var menuFromPlayer by mutableStateOf(false)
    var menuFromHistory by mutableStateOf(false)
    var songMenuOrigin by mutableStateOf<HeldItem?>(null)
    var songMenuAnchor by mutableStateOf<Rect?>(null)
    var linksLoading by mutableStateOf(false)
    var playlistTarget by mutableStateOf<Song?>(null)
    var creatingPlaylist by mutableStateOf(false)
    var offlinePlaylistTarget by mutableStateOf<Song?>(null)
    var creatingOfflinePlaylist by mutableStateOf(false)
    var showSpotifyImportDialog by mutableStateOf(false)
    var browseActions by mutableStateOf<BrowseTarget?>(null)
    var browseMenuOrigin by mutableStateOf<HeldItem?>(null)
    var browseMenuAnchor by mutableStateOf<Rect?>(null)
    var browseRenameInSheet by mutableStateOf(false)
    var editPlaylistTarget by mutableStateOf<UserPlaylist?>(null)
    var reorderTarget by mutableStateOf<UserPlaylist?>(null)
    var reorderEntries by mutableStateOf<UiState<List<Song>>>(UiState.Loading)
    var reorderSaving by mutableStateOf(false)
    var songSortMenuOpen by mutableStateOf(false)
    var folderMenuOpen by mutableStateOf(false)
    var showUpdateDialog by mutableStateOf(false)
    var updateDialogShown by mutableStateOf(false)
    var showAboutWhatsNew by mutableStateOf(false)
    var showDiwali by mutableStateOf(false)
    var diwaliAutoShown by mutableStateOf(false)
    var recognitionOpen by mutableStateOf(false)
    var showRecognitionHistoryClearConfirmation by mutableStateOf(false)
    var showRadioHistoryClearConfirmation by mutableStateOf(false)
    var radioSubpageBack by mutableStateOf<(() -> Unit)?>(null)
    var recognitionFabVisible by mutableStateOf(true)
    var activeJamInviteCode by mutableStateOf<String?>(null)
    var activeJamInviteServer by mutableStateOf<String?>(null)
    var activeRadioSeed by mutableStateOf<Pair<String, String>?>(null)
    var searchFocusRequested by mutableStateOf(false)
}

@Composable
internal fun rememberBitChordSheetStateHolder(): BitChordSheetStateHolder = remember { BitChordSheetStateHolder() }

internal fun rememberBitChordSheetsUiState(
    sheetHolder: BitChordSheetStateHolder,
    pageBackdrop: SnapshotBackdrop,
    reduceDynamicBlur: Boolean,
    playerRaised: Boolean,
    replay: ReplayState,
    account: Account?,
    playlists: List<UserPlaylist>,
    playlistsLoading: Boolean,
    offlinePlaylists: List<OfflinePlaylist>,
    signedIn: Boolean,
    detailStack: List<DetailPage>,
    libraryState: UiState<LibraryPage>,
    likeStatuses: Map<String, LikeStatus>,
    query: String,
    results: UiState<List<SearchResult>>?,
    filter: SearchFilter,
    onboardingShown: MutableTransitionState<Boolean>,
    googleAccounts: List<GoogleAccountSession>,
    activeAccountId: String?,
    activeProfileId: String?,
    onboardingComplete: Boolean,
    updateAvailable: AppUpdateChecker.UpdateInfo?,
    songSort: SongSort,
    detail: DetailPage?,
    listenBrainzToken: String,
): BitChordSheetsUiState = BitChordSheetsUiState(
    pageBackdrop = pageBackdrop,
    reduceDynamicBlur = reduceDynamicBlur,
    playerRaised = playerRaised,
    songActions = sheetHolder.songActions,
    songMenuOrigin = sheetHolder.songMenuOrigin,
    songMenuAnchor = sheetHolder.songMenuAnchor,
    menuFromPlayer = sheetHolder.menuFromPlayer,
    browseActions = sheetHolder.browseActions,
    browseMenuOrigin = sheetHolder.browseMenuOrigin,
    browseMenuAnchor = sheetHolder.browseMenuAnchor,
    browseRenameInSheet = sheetHolder.browseRenameInSheet,
    stationActions = sheetHolder.stationActions,
    replay = replay,
    replayStory = sheetHolder.replayStory,
    showReplayShare = sheetHolder.showReplayShare,
    replaySharePage = sheetHolder.replaySharePage,
    account = account,
    playlistTarget = sheetHolder.playlistTarget,
    creatingPlaylist = sheetHolder.creatingPlaylist,
    playlists = playlists,
    playlistsLoading = playlistsLoading,
    offlinePlaylistTarget = sheetHolder.offlinePlaylistTarget,
    creatingOfflinePlaylist = sheetHolder.creatingOfflinePlaylist,
    offlinePlaylists = offlinePlaylists,
    showSpotifyImportDialog = sheetHolder.showSpotifyImportDialog,
    signedIn = signedIn,
    editPlaylistTarget = sheetHolder.editPlaylistTarget,
    detailStack = detailStack,
    libraryState = libraryState,
    likeStatuses = likeStatuses,
    query = query,
    results = results,
    filter = filter,
    onboardingShown = onboardingShown,
    webSession = sheetHolder.webSession,
    googleAccounts = googleAccounts,
    activeAccountId = activeAccountId,
    activeProfileId = activeProfileId,
    showDiwali = sheetHolder.showDiwali,
    showUpdateDialog = sheetHolder.showUpdateDialog,
    onboardingComplete = onboardingComplete,
    updateAvailable = updateAvailable,
    showLyricsSources = sheetHolder.showLyricsSources,
    songSortMenuOpen = sheetHolder.songSortMenuOpen,
    songSort = songSort,
    detail = detail,
    folderMenuOpen = sheetHolder.folderMenuOpen,
    showAccountSelector = sheetHolder.showAccountSelector,
    showAppLanguage = sheetHolder.showAppLanguage,
    showTranslationLanguage = sheetHolder.showTranslationLanguage,
    showListenBrainzLogin = sheetHolder.showListenBrainzLogin,
    listenBrainzToken = listenBrainzToken,
    showLastfmLogin = sheetHolder.showLastfmLogin,
    showWebDavEditor = sheetHolder.showWebDavEditor,
    showSmbEditor = sheetHolder.showSmbEditor,
    showDiscordLogin = sheetHolder.showDiscordLogin,
    showSpotifyLogin = sheetHolder.showSpotifyLogin,
    showSpotifyCanvasAuth = sheetHolder.showSpotifyCanvasAuth,
    discordDialog = sheetHolder.discordDialog,
    editingPartyServer = sheetHolder.editingPartyServer,
    editingSignatureApi = sheetHolder.editingSignatureApi,
    editingSource = sheetHolder.editingSource,
    choosingSourceQuality = sheetHolder.choosingSourceQuality,
    explainingOutdatedAddon = sheetHolder.explainingOutdatedAddon,
    confirmJioSaavn = sheetHolder.confirmJioSaavn,
    confirmBitPerfect = sheetHolder.confirmBitPerfect,
    confirmFixedVolume = sheetHolder.confirmFixedVolume,
    driveAlerts = sheetHolder.driveAlerts,
    blockedAddonSong = sheetHolder.blockedAddonSong,
    addonLeftOutOfDownload = sheetHolder.addonLeftOutOfDownload,
    showAboutWhatsNew = sheetHolder.showAboutWhatsNew,
    recognitionOpen = sheetHolder.recognitionOpen,
)

internal fun rememberBitChordPlayerUiState(
    sheetHolder: BitChordSheetStateHolder,
    playerRaised: Boolean,
    playerSong: Song?,
    showNowPlaying: Boolean,
    reducePlayerMotion: Boolean,
    reduceDynamicBlur: Boolean,
    playerLiveCanvas: Boolean,
    player: PlayerState,
    playPauseBusy: Boolean,
    switchingAudioVersion: Boolean,
    optimisticVersionSong: Song?,
    convertedAudioId: String?,
    convertedVideoId: String?,
    keepVideoId: String?,
    partyState: ListenTogether.State,
    account: Account?,
    controlsLocked: Boolean,
    shuffleEnabled: Boolean,
    autoplayEnabled: Boolean,
    signedIn: Boolean,
    likeStatuses: Map<String, LikeStatus>,
    query: String,
    filter: SearchFilter,
    results: UiState<List<SearchResult>>?,
    lyrics: List<LyricLine>?,
    lyricsSource: LyricsSource?,
    lyricsProviderStates: Map<LyricsSource, LyricsProviderState>,
    lyricsChecked: Boolean,
    queueLabel: String,
    queueNotice: QueueActionNotice?,
    convertedFromVideo: Song?,
    convertedFromAudio: Song?,
    playlists: List<UserPlaylist>,
    playlistsLoading: Boolean,
    offlinePlaylists: List<OfflinePlaylist>,
    addonCeiling: AudioQuality,
    detail: DetailPage?,
    versionContext: Context,
    coveredByPlayer: Boolean,
    activeDock: PlayerDock?,
    playerBackdrop: SnapshotBackdrop,
    livePlayerBackdrop: LayerBackdrop,
): BitChordPlayerUiState = BitChordPlayerUiState(
    playerRaised = playerRaised,
    playerSong = playerSong,
    showNowPlaying = showNowPlaying,
    reducePlayerMotion = reducePlayerMotion,
    reduceDynamicBlur = reduceDynamicBlur,
    playerLiveCanvas = playerLiveCanvas,
    player = player,
    playPauseBusy = playPauseBusy,
    switchingAudioVersion = switchingAudioVersion,
    optimisticVersionSong = optimisticVersionSong,
    convertedAudioId = convertedAudioId,
    convertedVideoId = convertedVideoId,
    keepVideoId = keepVideoId,
    activeRadioSeed = sheetHolder.activeRadioSeed,
    partyState = partyState,
    account = account,
    songActions = sheetHolder.songActions,
    songMenuOrigin = sheetHolder.songMenuOrigin,
    songMenuAnchor = sheetHolder.songMenuAnchor,
    menuFromPlayer = sheetHolder.menuFromPlayer,
    controlsLocked = controlsLocked,
    shuffleEnabled = shuffleEnabled,
    autoplayEnabled = autoplayEnabled,
    signedIn = signedIn,
    likeStatuses = likeStatuses,
    query = query,
    filter = filter,
    results = results,
    lyrics = lyrics,
    lyricsSource = lyricsSource,
    lyricsProviderStates = lyricsProviderStates,
    lyricsChecked = lyricsChecked,
    showLyricsOffset = sheetHolder.showLyricsOffset,
    queueLabel = queueLabel,
    queueNotice = queueNotice,
    convertedFromVideo = convertedFromVideo,
    convertedFromAudio = convertedFromAudio,
    playlists = playlists,
    playlistsLoading = playlistsLoading,
    offlinePlaylists = offlinePlaylists,
    addonCeiling = addonCeiling,
    detail = detail,
    versionContext = versionContext,
    coveredByPlayer = coveredByPlayer,
    activeDock = activeDock,
    playerBackdrop = playerBackdrop,
    livePlayerBackdrop = livePlayerBackdrop,
)
