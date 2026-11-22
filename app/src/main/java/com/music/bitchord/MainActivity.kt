package com.music.bitchord

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Upgrade
import com.music.bitchord.data.listentogether.ServerConnectionState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soundchord.features.radio.data.PreferencesRadioFavoritesStore
import com.soundchord.features.radio.data.RadioStation
import com.soundchord.features.radio.data.RadioHistoryStore
import com.music.bitchord.auth.DiscordLoginScreen
import com.music.bitchord.auth.SpotifyLoginScreen
import com.music.bitchord.auth.WebSessionMode
import com.music.bitchord.auth.YtMusicLoginScreen
import com.music.bitchord.data.AppUpdateChecker
import com.music.bitchord.data.LocalMediaRepository
import com.music.bitchord.data.listentogether.JamInviteLink
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.listentogether.partyQueueIndexOf
import com.music.bitchord.data.NerdStats
import com.music.bitchord.data.TrackLog
import com.music.bitchord.data.innertube.InnertubeParser
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.model.PlaybackSourceType
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.isUnresolvedSpotify
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.withoutVideos
import com.music.bitchord.data.model.UserPlaylist
import com.music.bitchord.data.model.EntityType
import com.music.bitchord.data.model.SearchHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.music.bitchord.data.model.durationMillis
import com.music.bitchord.data.scrobbling.LastFM
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.BitPerfectMode
import com.music.bitchord.ui.AddonBlockedAlert
import com.music.bitchord.data.settings.LibrarySort
import com.music.bitchord.data.settings.ThemeMode
import com.music.bitchord.ui.components.AccountProfileSelector
import com.music.bitchord.ui.components.DacVolumeHud
import com.music.bitchord.ui.components.DacVolumeKeys
import com.music.bitchord.ui.components.LocalPlayingBarsMotion
import com.music.bitchord.ui.components.handleDacVolumeKey
import com.music.bitchord.ui.components.VolumeHud
import com.music.bitchord.ui.navigation.AppDestination
import com.music.bitchord.ui.navigation.AppRoute
import com.music.bitchord.ui.screens.AboutScreen
import com.music.bitchord.ui.screens.AboutWhatsNewOverlay
import com.music.bitchord.ui.screens.EqualizerPresetPage
import com.music.bitchord.ui.screens.SettingsChoicePage
import com.music.bitchord.ui.components.SpotifyImportAlert
import com.music.bitchord.ui.screens.WearOsScreen
import com.music.bitchord.ui.screens.AccountAndScrobblingScreen
import com.music.bitchord.ui.screens.DiscordDialog
import com.music.bitchord.ui.screens.DiscordDialogHost
import com.music.bitchord.ui.screens.DiscordScreen
import com.music.bitchord.ui.screens.EqualizerScreen
import com.music.bitchord.ui.screens.HistoryScreen
import com.music.bitchord.ui.screens.RecognitionHistoryScreen
import com.music.bitchord.ui.screens.LibraryReplayEntry
import com.music.bitchord.ui.screens.libraryDeviceItems
import com.music.bitchord.ui.screens.SPOTIFY_BROWSE_ID
import com.music.bitchord.ui.screens.libraryLinks
import com.music.bitchord.ui.screens.CACHE_FOLDER_BROWSE_ID
import com.music.bitchord.ui.screens.ListenTogetherScreen
import com.music.bitchord.ui.screens.PartyServerEditor
import com.music.bitchord.ui.screens.SignatureApiEditor
import com.music.bitchord.ui.screens.SettingsScreen
import com.music.bitchord.ui.screens.SourceEditorAlert
import com.music.bitchord.ui.screens.SourcesScreen
import com.music.bitchord.data.sources.TrackMatcher
import com.music.bitchord.ui.screens.SpotifyCanvasAuthScreen
import com.music.bitchord.ui.screens.SpotifyLibraryScreen
import com.music.bitchord.data.spotify.SPOTIFY_PAGE_PREFIX
import com.music.bitchord.playback.AudioCache
import com.music.bitchord.playback.LinkRequest
import com.music.bitchord.playback.MusicLink
import com.music.bitchord.playback.OriginalVersion
import com.music.bitchord.playback.PlayerDeepLink
import com.music.bitchord.playback.QueueBuilder
import com.music.bitchord.playback.QueueCoordinator
import com.music.bitchord.playback.QueueCoordinator.asQueueEntry
import com.music.bitchord.playback.QueueShuffle
import com.music.bitchord.playback.QueueSource
import com.music.bitchord.playback.cast.CastController
import com.music.bitchord.data.model.QueueTier
import com.music.bitchord.playback.autoplayEnabledFor
import com.music.bitchord.playback.autoplaySectionStart
import com.music.bitchord.playback.beginRadioQueue
import com.music.bitchord.playback.commitRadioQueue
import com.music.bitchord.playback.fromAutoplay
import com.music.bitchord.playback.hasYouTubeOriginal
import com.music.bitchord.playback.loadAutoplayTracks
import com.music.bitchord.playback.playSongs
import com.music.bitchord.playback.BoundedQueuePlanner
import com.music.bitchord.playback.playBoundedContext
import com.music.bitchord.playback.contextEntryKey
import com.music.bitchord.playback.toMediaItem
import com.music.bitchord.playback.toSong
import com.music.bitchord.playback.toDirectYouTubeMediaItem
import com.music.bitchord.playback.toggleAutoplay
import com.music.bitchord.playback.toggleShuffle
import com.music.bitchord.playback.upgradeQuality
import com.music.bitchord.playback.revertToOriginal
import com.music.bitchord.playback.swapToVersion
import com.music.bitchord.playback.smart.VersionAudioAligner
import com.music.bitchord.download.DownloadSession
import com.music.bitchord.download.DownloadStore
import com.music.bitchord.download.MediaTagger
import com.music.bitchord.download.DownloadTarget
import com.music.bitchord.download.Downloads
import com.music.bitchord.ui.components.BrowseActionsMenu
import com.music.bitchord.ui.components.BrowseTarget
import com.music.bitchord.ui.components.ConfirmationAlert
import com.music.bitchord.ui.components.InfoAlert
import com.music.bitchord.ui.components.DownloadManagerPage
import com.music.bitchord.ui.components.PlaylistPickerSheet
import com.music.bitchord.ui.components.EditPlaylistSheet
import com.music.bitchord.ui.components.ReorderPlaylistPage
import com.music.bitchord.ui.components.LongPressOrigin
import com.music.bitchord.ui.components.MenuAnchor
import com.music.bitchord.ui.components.menuAnchor
import com.music.bitchord.ui.components.IosMenuPopover
import com.music.bitchord.ui.components.SongActionsMenu
import com.music.bitchord.ui.components.StationActionsMenu
import com.music.bitchord.ui.components.StationMenuTarget
import com.music.bitchord.ui.components.HeldContextMenu
import com.music.bitchord.ui.components.HeldItem
import androidx.media3.session.MediaController
import com.music.bitchord.playback.QualityUpgrade
import com.music.bitchord.playback.rememberMediaController
import com.music.bitchord.playback.rememberPlayerState
import com.music.bitchord.playback.setQueueDragActive
import com.music.bitchord.ui.MainViewModel
import com.music.bitchord.ui.SearchSource
import com.music.bitchord.ui.components.BottomFadeScrim
import com.music.bitchord.ui.components.TopFadeBlur
import com.music.bitchord.ui.components.FloatingBarsTapGuard
import com.music.bitchord.ui.components.BottomTab
import com.music.bitchord.ui.components.FLOATING_BAR_MAX_WIDTH
import com.music.bitchord.ui.components.FloatingBottomBar
import com.music.bitchord.ui.components.GlassNavBar
import com.music.bitchord.ui.components.floatingtabbar.rememberFloatingTabBarScrollConnection
import com.music.bitchord.ui.components.FrostedTopBar
import com.music.bitchord.ui.components.LastfmLoginAlert
import com.music.bitchord.ui.components.LocalAppBackdrop
import com.music.bitchord.ui.components.LocalLiquidGlassEnabled
import com.music.bitchord.ui.components.backdrop.backdrops.LayerBackdrop
import com.music.bitchord.ui.components.backdrop.backdrops.layerBackdrop
import com.music.bitchord.ui.components.backdrop.backdrops.rememberLayerBackdrop
import com.music.bitchord.ui.components.backdrop.backdrops.rememberSnapshotBackdrop
import com.music.bitchord.ui.components.backdrop.backdrops.snapshotBackdropSource
import com.music.bitchord.ui.components.isGlassSupported
import com.music.bitchord.data.sources.SourceConfig
import com.music.bitchord.data.sources.SourceKind
import com.music.bitchord.data.sources.SourceRegistry
import com.music.bitchord.ui.components.ChoiceAlert
import com.music.bitchord.ui.components.ListenBrainzTokenAlert
import com.music.bitchord.ui.components.MiniPlayer
import com.music.bitchord.ui.components.QueueActionNotice
import com.music.bitchord.ui.components.QueueActionNoticeHost
import com.music.bitchord.ui.components.TopBarAccountButton
import com.music.bitchord.ui.components.iosMenuPanel
import com.music.bitchord.ui.screens.SegmentedControl
import com.music.bitchord.ui.components.SearchField
import com.music.bitchord.sharedui.resources.Res as SharedRes
import com.music.bitchord.sharedui.resources.search_hint
import com.music.bitchord.sharedui.resources.search_addons_hint
import com.music.bitchord.sharedui.resources.search_library_hint
import com.music.bitchord.ui.components.TopBarDownloadButton
import com.music.bitchord.ui.components.optimizedHazeEffect
import com.music.bitchord.ui.components.topBarContentPadding
import com.music.bitchord.ui.components.topBarHeight
import com.music.bitchord.ui.components.AppLanguageDialog
import com.music.bitchord.ui.components.TranslationLanguageDialog
import com.music.bitchord.ui.components.LyricsSourcesDialog
import com.music.bitchord.ui.components.ServerEditorHost
import com.music.bitchord.ui.components.UpdateAvailableDialog
import com.music.bitchord.ui.components.WebDavConflictAlert
import com.music.bitchord.ui.components.FieldConfig
import com.music.bitchord.ui.icons.BitChordIcons
import androidx.media3.common.Player
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.TopPicks
import com.music.bitchord.data.model.TopPick
import com.music.bitchord.data.model.TopPickKind
import com.music.bitchord.data.model.TopPickMood
import com.music.bitchord.ui.screens.MoodGridPage
import com.music.bitchord.ui.player.NowPlayingScreen
import com.music.bitchord.ui.player.MixViewRequest
import com.music.bitchord.ui.player.MusicVideoEngine
import com.music.bitchord.ui.player.PlayerSheetMotion
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import com.music.bitchord.ui.components.MiniPlayerPull
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.layout.onSizeChanged
import com.music.bitchord.ui.player.LocalPlayerDock
import com.music.bitchord.ui.player.PlayerDock
import com.music.bitchord.ui.player.AddSongsRequest
import com.music.bitchord.ui.components.AddSongsDialog
import com.music.bitchord.ui.components.PlaylistAddSongsHost
import com.music.bitchord.ui.screens.DetailScreen
import com.music.bitchord.ui.screens.sortedForDetail
import com.music.bitchord.ui.screens.LocalFolder
import com.music.bitchord.ui.screens.LocalFolderState
import com.music.bitchord.ui.screens.LocalFolderTab
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.vector.ImageVector
import com.music.bitchord.ui.screens.localizedLabel
import com.music.bitchord.data.LocalGroups
import com.music.bitchord.data.OfflinePlaylists
import com.music.bitchord.data.toGroup
import com.music.bitchord.data.settings.LibraryViewType
import com.music.bitchord.data.settings.LocalMusicSort
import com.music.bitchord.ui.screens.HomeScreen
import com.music.bitchord.ui.screens.LibraryGridPage
import com.music.bitchord.ui.screens.LibraryScreen
import com.music.bitchord.ui.screens.MoodGenrePlaylistsScreen
import com.music.bitchord.ui.screens.SearchScreen
import com.soundchord.features.explore.ExploreScreen
import com.music.bitchord.data.settings.SongSort
import com.music.bitchord.ui.onboarding.OnboardingScreen
import com.music.bitchord.ui.festive.DiwaliGate
import com.music.bitchord.ui.festive.DiwaliGreetingScreen
import com.music.bitchord.ui.replay.ReplayScreen
import com.music.bitchord.ui.replay.cards
import com.music.bitchord.ui.replay.ReplayShareSheet
import com.music.bitchord.ui.replay.ReplayStories
import com.music.bitchord.ui.replay.ReplayStoryPage
import com.music.bitchord.ui.replay.rememberReplayState
import com.music.bitchord.ui.theme.BitChordTheme
import com.music.bitchord.ui.theme.rememberArtworkPalette
import com.music.bitchord.data.canvas.AppleArtistArtRepository
import com.music.bitchord.data.canvas.keyColors
import com.music.bitchord.ui.theme.SystemBarIcons
import com.music.bitchord.ui.utils.guardSheetFromContentTouches
import com.music.bitchord.ui.utils.rememberIosOverscrollFactory
import com.music.bitchord.ui.performance.resolvePerformanceRefreshRate
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Locale
import com.soundchord.features.icons.SoundChordIcons
import com.soundchord.features.radio.ui.RadioScreen
import com.soundchord.features.radio.ui.RadioHistoryScreen
import com.soundchord.features.radio.playback.toSong
import com.soundchord.features.recognition.RecognitionFab
import com.soundchord.features.recognition.RecognitionScreen
import com.soundchord.features.recognition.data.MusicRecognitionService
import com.soundchord.features.recognition.data.RecognitionHistoryStore
import com.soundchord.features.recognition.data.RecognitionHistoryEntry
import com.soundchord.features.recognition.model.RecognitionResult
import com.music.bitchord.ui.app.AppNavigationHost
import com.music.bitchord.ui.app.AppNavigationListStates
import com.music.bitchord.ui.app.AppNavigationScreenActions
import com.music.bitchord.ui.app.rememberAppNavigationScreenActions
import com.music.bitchord.ui.app.AppNavigationScreenStates
import com.music.bitchord.ui.app.rememberAppNavigationScreenStates
import com.music.bitchord.ui.app.FrostedFolderMenu
import com.music.bitchord.ui.app.FrostedMenuPanel
import com.music.bitchord.ui.app.FrostedMenuRow
import com.music.bitchord.ui.app.FrostedSortMenu
import com.music.bitchord.ui.app.GlobalSheetsHost
import com.music.bitchord.ui.app.GlobalSheetsHostActions
import com.music.bitchord.ui.app.rememberGlobalSheetsHostActions
import com.music.bitchord.ui.app.GlobalSheetsHostState
import com.music.bitchord.ui.app.rememberGlobalSheetsHostState
import com.music.bitchord.ui.app.Isolated
import com.music.bitchord.ui.app.SongMenuActions
import com.music.bitchord.ui.app.SongMenuContent
import com.music.bitchord.ui.app.SongMenuState
import com.music.bitchord.ui.app.rememberSongMenuState
import com.music.bitchord.ui.app.historyDialogSurface
import com.music.bitchord.ui.app.localizedLabel
import com.music.bitchord.ui.app.tween
import com.music.bitchord.ui.app.rememberVersionSwitchController
import com.music.bitchord.ui.app.uploadTracksToWebDav
import com.music.bitchord.ui.app.openByName
import com.music.bitchord.ui.app.playFrom
import com.music.bitchord.ui.app.startRadioStation
import com.music.bitchord.ui.app.openTopPickCard
import com.music.bitchord.ui.app.enqueueSingleTrack
import com.music.bitchord.ui.app.enqueueBatchSongs
import com.music.bitchord.ui.app.shelfSong
import com.music.bitchord.ui.app.withBrowseSongs
import com.music.bitchord.ui.player.BottomPlayerBar
import com.music.bitchord.ui.player.BottomPlayerBarActions
import com.music.bitchord.ui.player.rememberBottomPlayerBarActions
import com.music.bitchord.ui.player.BottomPlayerBarState
import com.music.bitchord.ui.player.rememberBottomPlayerBarState
import com.music.bitchord.ui.player.MainPlayerSheet
import com.music.bitchord.ui.player.MainPlayerSheetActions
import com.music.bitchord.ui.player.rememberMainPlayerSheetActions
import com.music.bitchord.ui.player.MainPlayerSheetState
import com.music.bitchord.ui.player.rememberMainPlayerSheetState
import com.music.bitchord.ui.navigation.AppNavigator
import com.music.bitchord.ui.player.BitChordPlayerUi
import com.music.bitchord.ui.app.BitChordChrome
import com.music.bitchord.ui.app.BitChordChromeState
import com.music.bitchord.ui.app.BitChordChromeActions
import com.music.bitchord.ui.app.BitChordGlobalSheetsUi
import com.music.bitchord.ui.app.BitChordAppState
import com.music.bitchord.ui.app.BitChordAppActions
import com.music.bitchord.ui.app.BitChordNavState
import com.music.bitchord.ui.app.BitChordPlayerUiState
import com.music.bitchord.ui.app.BitChordSheetsUiState
import androidx.compose.runtime.saveable.SaveableStateHolder
import com.music.bitchord.auth.GoogleAccountSession
import com.music.bitchord.ui.components.floatingtabbar.FloatingTabBarScrollConnection
import com.music.bitchord.playback.PlayerState
import com.music.bitchord.data.model.DetailPage
import com.music.bitchord.ui.app.VersionSwitchController
import com.music.bitchord.data.stats.ReplayPeriod
import com.music.bitchord.download.SavedCollection
import com.music.bitchord.data.OfflinePlaylist
import com.music.bitchord.data.lyrics.LyricsSource
import com.music.bitchord.data.listentogether.ParsedJamInvite

/** A full first screen of a native YouTube Music radio before AutoPlay tops it up. */
/** How much of an artist page's header scrolls away before the top fade begins. */
internal const val TOP_FADE_START = 0.55f

/** How much of the header's height the fade takes to reach full strength. */
internal const val TOP_FADE_RAMP = 0.3f

internal const val INITIAL_RADIO_TRACKS = 24

internal fun shouldSkipAfterDislike(
    previousStatus: LikeStatus,
    targetVideoId: String,
    currentVideoId: String?,
): Boolean = previousStatus != LikeStatus.DISLIKE && targetVideoId == currentVideoId


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Before the composition, so a cold launch from a widget's artwork has
        // the request already standing by the time BitChordApp first reads it.
        PlayerDeepLink.consume(intent)
        JamInviteLink.consume(intent)
        // Likewise for a link tapped or shared from another app — see [MusicLink].
        MusicLink.consume(intent)
        setContent {
            val theme by AppSettings.themeMode.collectAsStateWithLifecycle()
            val appFont by AppSettings.appFont.collectAsStateWithLifecycle()
            val accentColor by AppSettings.accentColor.collectAsStateWithLifecycle()
            val highPerformance by AppSettings.highPerformanceMode.collectAsStateWithLifecycle()
            val liquidGlassEnabled by AppSettings.liquidGlass.collectAsStateWithLifecycle()
            val iosOverscrollFactory = rememberIosOverscrollFactory()
            val performanceRefreshRate by AppSettings.performanceRefreshRate.collectAsStateWithLifecycle()
            val composeView = LocalView.current
            LaunchedEffect(highPerformance, performanceRefreshRate, composeView) {
                applyPerformanceMode(highPerformance, performanceRefreshRate, composeView)
            }
            val darkTheme = when (theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            BitChordTheme(darkTheme = darkTheme, font = appFont, accentColor = accentColor) {
                // The glass surfaces sample this layer, and a layer records only
                // what is drawn into it — which, for BitChord, is a page that
                // paints no background of its own. Everywhere a page is not
                // showing artwork the recording is transparent, so the glass had
                // nothing to blur there and you saw straight through it to the
                // sharp page underneath: album art came through the bar blurred
                // and text came through it untouched. The window's background is
                // the floor the pages have always been drawn against, so it is
                // laid down here too and the recording is opaque like the screen.
                val windowBackground = MaterialTheme.colorScheme.background
                val paintBackdrop: ContentDrawScope.() -> Unit = remember(windowBackground) {
                    {
                        drawRect(windowBackground)
                        drawContent()
                    }
                }
                val appBackdrop = rememberLayerBackdrop(onDraw = paintBackdrop)
                CompositionLocalProvider(
                    LocalOverscrollFactory provides iosOverscrollFactory,
                    LocalLiquidGlassEnabled provides liquidGlassEnabled,
                    LocalAppBackdrop provides appBackdrop,
                ) {
                // The window's width, measured rather than asked for.
                //
                // `Configuration.screenWidthDp` is the wrong question here: in a
                // freeform or desktop window it can report the display rather
                // than the window it is actually in, and it lands a beat late
                // when that window is dragged. The layout downstream splits in
                // two on the strength of this number and sizes both halves from
                // it, so a stale one is a player pane sized for a window that no
                // longer exists and a page squeezed to a sliver to pay for it.
                // A measured constraint cannot be stale — it is the very width
                // the split is about to be laid out in.
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    BitChordApp(
                        darkTheme = darkTheme,
                        windowWidth = maxWidth,
                        windowHeight = maxHeight,
                        appBackdrop = appBackdrop,
                    )
                }
                }
            }
        }
    }

    /**
     * The volume keys move the DAC's own volume while direct USB output owns
     * it: Android's music stream no longer reaches that DAC, so the system
     * volume would change nothing anyone hears. Handled here rather than left
     * to the media session so the app's own HUD answers instead of Android's
     * remote-volume panel; in the background the session takes them. A sheet
     * in a window of its own asks the same through [DacVolumeKeys].
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        handleDacVolumeKey(event) || super.dispatchKeyEvent(event)

    /**
     * Requests a window refresh rate without forcing a display mode or
     * resolution. Android may still lower it for temperature, battery state or
     * hardware limits, which is why Settings describes this as a preference.
     */
    private fun applyPerformanceMode(enabled: Boolean, refreshRate: Int, composeView: View) {
        val supportedRefreshRate = composeView.display.resolvePerformanceRefreshRate(refreshRate)
        window.attributes = window.attributes.apply {
            preferredRefreshRate = if (enabled) supportedRefreshRate.toFloat() else 0f
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.setFrameRatePowerSavingsBalanced(!enabled)
            composeView.requestedFrameRate = if (enabled) {
                supportedRefreshRate.toFloat()
            } else {
                View.REQUESTED_FRAME_RATE_CATEGORY_DEFAULT
            }
        }
    }

    /**
     * The other half of the relay. This activity is `singleTask`, so once it is
     * running a second tap on the widget does not rebuild anything — it arrives
     * here, and [onCreate] never runs again.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Replaces what getIntent() returns, so the extra this consumes is the
        // one that just arrived and not the one the task was started with.
        setIntent(intent)
        PlayerDeepLink.consume(intent)
        JamInviteLink.consume(intent)
        MusicLink.consume(intent)
    }
}

internal fun handleOpenPlaybackSource(
    displayedSong: Song,
    queueLabel: String,
    context: Context,
    navigator: AppNavigator,
    viewModel: MainViewModel,
    dismissPlayer: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onSetReplayLandingPage: (ReplayStoryPage) -> Unit,
) {
    val sourceType = displayedSong.playbackSourceType ?: PlaybackSourceType.QUEUE
    val sourceTitle = displayedSong.playbackSource
        ?: displayedSong.albumName
        ?: queueLabel
    val sourceId = displayedSong.playbackSourceId

    dismissPlayer()
    navigator.clear()

    when (sourceType) {
        PlaybackSourceType.BROWSE -> {
            val id = sourceId ?: return
            viewModel.closeMoodGenre()
            viewModel.openDetail(id, sourceTitle)
        }
        PlaybackSourceType.HOME -> {
            viewModel.closeMoodGenre()
            onSelectTab(TAB_HOME)
        }
        PlaybackSourceType.SEARCH -> {
            viewModel.closeMoodGenre()
            onSelectTab(TAB_SEARCH)
        }
        PlaybackSourceType.HISTORY -> {
            navigator.push(AppRoute.History)
            viewModel.loadHistory()
        }
        PlaybackSourceType.REPLAY -> {
            onSetReplayLandingPage(ReplayStoryPage.INTRO)
            navigator.push(AppRoute.Replay)
        }
        PlaybackSourceType.EXPLORE -> onSelectTab(TAB_EXPLORE)
        PlaybackSourceType.SHARED_LINK -> {
            val id = sourceId ?: return
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://music.youtube.com/watch?v=$id"),
                ),
            )
        }
        PlaybackSourceType.QUEUE -> Unit
    }
}

@Composable
private fun BitChordBackHandlers(
    canGoBack: Boolean,
    onGoBack: () -> Unit,
    addSongsOpen: Boolean,
    onDismissAddSongs: () -> Unit,
    recognitionOpen: Boolean,
    onResetRecognition: () -> Unit,
    replayDrawable: Boolean,
    replayStory: ReplayStoryPage?,
    showReplayShare: Boolean,
    onDismissReplayStory: () -> Unit,
    onDismissReplayShare: () -> Unit,
    showUpdateDialog: Boolean,
    onDismissUpdateDialog: () -> Unit,
    showListenBrainzLogin: Boolean,
    onDismissListenBrainzLogin: () -> Unit,
    showLastfmLogin: Boolean,
    onDismissLastfmLogin: () -> Unit,
    discordDialog: DiscordDialog?,
    onDismissDiscordDialog: () -> Unit,
    editingSource: SourceConfig?,
    onDismissEditingSource: () -> Unit,
    choosingSourceQuality: SourceConfig?,
    onDismissChoosingSourceQuality: () -> Unit,
    explainingOutdatedAddon: SourceConfig?,
    onDismissExplainingOutdatedAddon: () -> Unit,
    editingPartyServer: Boolean,
    onDismissEditingPartyServer: () -> Unit,
    editingSignatureApi: Boolean,
    onDismissEditingSignatureApi: () -> Unit,
) {
    BackHandler(enabled = addSongsOpen, onBack = onDismissAddSongs)
    BackHandler(enabled = canGoBack, onBack = onGoBack)
    BackHandler(enabled = recognitionOpen, onBack = onResetRecognition)
    BackHandler(enabled = replayDrawable && replayStory != null && !showReplayShare, onBack = onDismissReplayStory)
    BackHandler(enabled = replayDrawable && showReplayShare, onBack = onDismissReplayShare)
    BackHandler(enabled = showUpdateDialog, onBack = onDismissUpdateDialog)
    BackHandler(enabled = showListenBrainzLogin, onBack = onDismissListenBrainzLogin)
    BackHandler(enabled = showLastfmLogin, onBack = onDismissLastfmLogin)
    BackHandler(enabled = discordDialog != null, onBack = onDismissDiscordDialog)
    BackHandler(enabled = editingSource != null, onBack = onDismissEditingSource)
    BackHandler(enabled = choosingSourceQuality != null, onBack = onDismissChoosingSourceQuality)
    BackHandler(enabled = explainingOutdatedAddon != null, onBack = onDismissExplainingOutdatedAddon)
    BackHandler(enabled = editingPartyServer, onBack = onDismissEditingPartyServer)
    BackHandler(enabled = editingSignatureApi, onBack = onDismissEditingSignatureApi)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BitChordApp(
    darkTheme: Boolean,
    windowWidth: Dp,
    windowHeight: Dp,
    appBackdrop: LayerBackdrop,
    viewModel: MainViewModel = viewModel(),
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val hazeState = remember { HazeState() }
    val glassActive = LocalLiquidGlassEnabled.current && isGlassSupported()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val progressiveBlur by AppSettings.progressiveBlur.collectAsStateWithLifecycle()
    val glassSamplesBackdrop = glassActive && !reduceDynamicBlur
    val navBarScroll = rememberFloatingTabBarScrollConnection()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var recognitionOpen by rememberSaveable { mutableStateOf(false) }
    var showRecognitionHistoryClearConfirmation by remember { mutableStateOf(false) }
    var showRadioHistoryClearConfirmation by remember { mutableStateOf(false) }
    var radioSubpageBack by remember { mutableStateOf<(() -> Unit)?>(null) }
    var recognitionFabVisible by rememberSaveable { mutableStateOf(true) }
    var showNowPlaying by remember { mutableStateOf(false) }
    androidx.compose.runtime.SideEffect { VolumeHud.suppressed = showNowPlaying }
    val appForeground = com.music.bitchord.ui.rememberIsForeground()
    val artworkMotion = appForeground && !showNowPlaying
    val playerDock = remember { PlayerDock() }
    val playerSheetScope = rememberCoroutineScope()
    val playerSheetMotion = remember {
        PlayerSheetMotion(
            scope = playerSheetScope,
            dock = playerDock,
            shown = { showNowPlaying },
            setShown = { showNowPlaying = it },
        )
    }
    playerSheetMotion.windowInfo = LocalWindowInfo.current
    val coveredByPlayer by remember {
        derivedStateOf { showNowPlaying && playerDock.attached && playerDock.openFraction() >= 1f }
    }
    val reducePlayerMotion by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val activeDock = playerDock.takeIf { !reducePlayerMotion }
    playerSheetMotion.flingVelocity = with(LocalDensity.current) { PLAYER_PULL_FLING_VELOCITY.toPx() }
    val dismissPlayer: () -> Unit = {
        if (reducePlayerMotion) showNowPlaying = false else playerSheetMotion.close()
    }
    val openPlayerRequested by PlayerDeepLink.pending.collectAsStateWithLifecycle()
    var webSession by remember { mutableStateOf<WebSessionMode?>(null) }
    var replayLandingPage by rememberSaveable { mutableStateOf(ReplayStoryPage.INTRO) }
    var replayStory by rememberSaveable { mutableStateOf<ReplayStoryPage?>(null) }
    var showReplayShare by rememberSaveable { mutableStateOf(false) }
    var replaySharePage by rememberSaveable { mutableStateOf<ReplayStoryPage?>(null) }
    var showSpotifyCanvasAuth by remember { mutableStateOf(false) }

    var editingSource by remember { mutableStateOf<SourceConfig?>(null) }
    var choosingSourceQuality by remember { mutableStateOf<SourceConfig?>(null) }
    var explainingOutdatedAddon by remember { mutableStateOf<SourceConfig?>(null) }
    var confirmJioSaavn by remember { mutableStateOf(false) }
    var confirmBitPerfect by remember { mutableStateOf<BitPerfectMode?>(null) }
    var confirmFixedVolume by remember { mutableStateOf(false) }
    var blockedAddonSong by remember { mutableStateOf<com.music.bitchord.data.model.Song?>(null) }
    var addonLeftOutOfDownload by remember {
        mutableStateOf<Pair<String, SourceRegistry.DownloadExclusion>?>(null)
    }
    var editingPartyServer by remember { mutableStateOf(false) }
    var editingSignatureApi by remember { mutableStateOf(false) }
    var showLyricsSources by remember { mutableStateOf(false) }
    var showAppLanguage by remember { mutableStateOf(false) }
    var showTranslationLanguage by remember { mutableStateOf(false) }
    var showAccountSelector by remember { mutableStateOf(false) }
    var showListenBrainzLogin by remember { mutableStateOf(false) }
    var showLastfmLogin by remember { mutableStateOf(false) }
    var showWebDavEditor by remember { mutableStateOf(false) }
    var showSmbEditor by remember { mutableStateOf(false) }
    var showDiscordLogin by remember { mutableStateOf(false) }
    var showSpotifyLogin by remember { mutableStateOf(false) }
    var discordDialog by remember { mutableStateOf<DiscordDialog?>(null) }
    var songActions by remember { mutableStateOf<Song?>(null) }
    var stationActions by remember { mutableStateOf<StationMenuTarget?>(null) }
    var showLyricsOffset by remember { mutableStateOf(false) }
    var menuFromPlayer by remember { mutableStateOf(false) }
    var menuFromHistory by remember { mutableStateOf(false) }
    var songMenuOrigin by remember { mutableStateOf<HeldItem?>(null) }
    var songMenuAnchor by remember { mutableStateOf<Rect?>(null) }
    val playerBackdrop = rememberSnapshotBackdrop()
    val livePlayerBackdrop = rememberLayerBackdrop()
    var playerLiveCanvas by remember { mutableStateOf(false) }
    val pageBackdrop = rememberSnapshotBackdrop()
    val openSongMenu: (Song) -> Unit = { song ->
        menuFromPlayer = false
        songMenuOrigin = LongPressOrigin.consume()
        songMenuAnchor = MenuAnchor.consume()
        songActions = song
    }
    var linksLoading by remember { mutableStateOf(false) }
    var playlistTarget by remember { mutableStateOf<Song?>(null) }
    var creatingPlaylist by remember { mutableStateOf(false) }
    var offlinePlaylistTarget by remember { mutableStateOf<Song?>(null) }
    var creatingOfflinePlaylist by remember { mutableStateOf(false) }
    var showSpotifyImportDialog by remember { mutableStateOf(false) }
    var browseActions by remember { mutableStateOf<BrowseTarget?>(null) }
    var browseMenuOrigin by remember { mutableStateOf<HeldItem?>(null) }
    var browseMenuAnchor by remember { mutableStateOf<Rect?>(null) }
    var browseRenameInSheet by remember { mutableStateOf(false) }
    var editPlaylistTarget by remember { mutableStateOf<UserPlaylist?>(null) }
    var reorderTarget by remember { mutableStateOf<UserPlaylist?>(null) }
    var reorderEntries by remember { mutableStateOf<UiState<List<Song>>>(UiState.Loading) }
    var reorderSaving by remember { mutableStateOf(false) }
    val openBrowseMenu: (BrowseTarget) -> Unit = { target ->
        browseMenuOrigin = LongPressOrigin.consume()
        browseMenuAnchor = MenuAnchor.consume()
        browseRenameInSheet = false
        browseActions = target
    }
    val autoplay by AppSettings.autoplay.collectAsStateWithLifecycle()
    val partyState by ListenTogether.state.collectAsStateWithLifecycle()
    val autoplayEnabled = autoplayEnabledFor(partyState, autoplay)
    val partyServerStatus by ListenTogether.serverConnectionState.collectAsStateWithLifecycle()
    val listenBrainzToken by AppSettings.listenBrainzToken.collectAsStateWithLifecycle()
    var searchFocusRequested by remember { mutableStateOf(false) }
    var playRequestGeneration by remember { mutableIntStateOf(0) }
    var activeRadioSeed by remember { mutableStateOf<Pair<String, String>?>(null) }

    val onboardingComplete by AppSettings.onboardingComplete.collectAsStateWithLifecycle()
    val onboardingShown = remember { MutableTransitionState(!onboardingComplete) }
    onboardingShown.targetState = !onboardingComplete
    val onboardingVisible = onboardingShown.currentState || onboardingShown.targetState

    val diwaliFestive = remember(appForeground) {
        DiwaliGate.isIndianTimezone() && DiwaliGate.isDiwaliDay()
    }
    var showDiwali by rememberSaveable { mutableStateOf(false) }
    var diwaliAutoShown by rememberSaveable { mutableStateOf(false) }

    val navigator = viewModel.navigator
    val detailStack by viewModel.detailStack.collectAsStateWithLifecycle()
    val topEntry = navigator.visibleEntry(detailStack.size)
    val artistShelfRoute = topEntry?.route as? AppRoute.ArtistShelf
    val visibleEntry = topEntry?.takeIf { artistShelfRoute == null }
    val detailActiveShelf = artistShelfRoute?.shelf
    val route = visibleEntry?.route
    val topDetail = detailStack.lastOrNull()
    val detail = topDetail?.takeIf { visibleEntry == null }

    SystemBarIcons(
        dark = !darkTheme && !showNowPlaying && route != AppRoute.Replay && replayStory == null && !onboardingVisible && !showDiwali,
    )

    val homeState by viewModel.home.collectAsStateWithLifecycle()
    val hideRecents by AppSettings.hideRecents.collectAsStateWithLifecycle()
    val hideHomeVideos by AppSettings.hideHomeVideos.collectAsStateWithLifecycle()
    val hideTopPicks by AppSettings.hideTopPicks.collectAsStateWithLifecycle()
    val hideHomeMoods by AppSettings.hideHomeMoods.collectAsStateWithLifecycle()
    val visibleHomeState = remember(homeState, hideHomeVideos, hideRecents) {
        val loaded = homeState as? UiState.Success
        if (loaded != null && (hideHomeVideos || hideRecents)) {
            var shelves = loaded.data
            if (hideRecents) shelves = shelves.filterNot { it.title == YtMusicRepository.RECENT_TITLE }
            if (hideHomeVideos) shelves = shelves.withoutVideos()
            UiState.Success(shelves)
        } else {
            homeState
        }
    }
    val homeLoadingMore by viewModel.homeLoadingMore.collectAsStateWithLifecycle()
    val homeRecentlyPlayedLoading by viewModel.homeRecentlyPlayedLoading.collectAsStateWithLifecycle()
    val topPicks by viewModel.topPicks.collectAsStateWithLifecycle()
    val topPicksLoading by viewModel.topPicksLoading.collectAsStateWithLifecycle()

    var updateDialogShown by rememberSaveable { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showAboutWhatsNew by remember { mutableStateOf(false) }
    val updateAvailable by viewModel.updateAvailable.collectAsStateWithLifecycle()

    val updateNotice = updateAvailable

    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val exploreState by viewModel.explore.collectAsStateWithLifecycle()
    val selectedMoodGenre by viewModel.selectedMoodGenre.collectAsStateWithLifecycle()
    val moodGenreShelves by viewModel.moodGenreShelves.collectAsStateWithLifecycle()
    val libraryState by viewModel.library.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val searchSource by viewModel.searchSource.collectAsStateWithLifecycle()
    val libraryResults by viewModel.libraryResults.collectAsStateWithLifecycle()
    val addonResults by viewModel.addonResults.collectAsStateWithLifecycle()
    val addonCeiling = AppSettings.ceilingFor(
        wifi = AppSettings.audioQualityWifi.collectAsStateWithLifecycle().value,
        cellular = AppSettings.audioQualityCellular.collectAsStateWithLifecycle().value,
        metered = AppSettings.meteredConnection.collectAsStateWithLifecycle().value,
        override = AppSettings.streamCeilingOverride.collectAsStateWithLifecycle().value,
    )
    val signedIn by viewModel.signedIn.collectAsStateWithLifecycle()
    val incomingJamInvite by JamInviteLink.pending.collectAsStateWithLifecycle()
    val linkRequest by MusicLink.pending.collectAsStateWithLifecycle()
    var activeJamInviteCode by rememberSaveable { mutableStateOf<String?>(null) }
    var activeJamInviteServer by rememberSaveable { mutableStateOf<String?>(null) }

    val account by viewModel.account.collectAsStateWithLifecycle()
    val selectedChannelName by viewModel.selectedChannelName.collectAsStateWithLifecycle()
    val googleAccounts by viewModel.googleAccounts.collectAsStateWithLifecycle()
    val activeAccountId by viewModel.activeAccountId.collectAsStateWithLifecycle()
    val activeProfileId by viewModel.activeProfileId.collectAsStateWithLifecycle()
    val historyState by viewModel.history.collectAsStateWithLifecycle()
    val recognitionHistory by RecognitionHistoryStore.entries.collectAsStateWithLifecycle()
    val radioHistoryStore = remember(context) { RadioHistoryStore.apply { init(context) } }
    val radioHistory by radioHistoryStore.stations.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()
    val lyricsSource by viewModel.lyricsSource.collectAsStateWithLifecycle()
    val lyricsChecked by viewModel.lyricsChecked.collectAsStateWithLifecycle()
    val lyricsProviderStates by viewModel.lyricsProviderStates.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val searchSuggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val searchLoadingMore by viewModel.searchLoadingMore.collectAsStateWithLifecycle()
    val searchScrollReset by viewModel.searchScrollReset.collectAsStateWithLifecycle()
    val releaseLibrary by viewModel.releaseLibrary.collectAsStateWithLifecycle()
    val isLocalDetail = detail?.browseId.isDeviceFolder()
    val isReleaseDetail = detail != null && !isLocalDetail && detail.type != BrowseType.ARTIST
    val detailSongSorts by AppSettings.detailSongSorts.collectAsStateWithLifecycle()
    val songSort = topDetail?.browseId?.let { detailSongSorts[it] } ?: SongSort.DEFAULT
    var songSortMenuOpen by remember { mutableStateOf(false) }
    val downloadedMusicSort by AppSettings.downloadedMusicSort.collectAsStateWithLifecycle()
    val localMusicSort by AppSettings.localMusicSort.collectAsStateWithLifecycle()
    val downloadedMusicViewType by AppSettings.downloadedMusicViewType.collectAsStateWithLifecycle()
    val localMusicViewType by AppSettings.localMusicViewType.collectAsStateWithLifecycle()
    val folderSortOf: (String) -> LocalMusicSort = { id ->
        if (id == "local:downloads") downloadedMusicSort else localMusicSort
    }
    val folderViewTypeOf: (String) -> LibraryViewType = { id ->
        if (id == "local:downloads") downloadedMusicViewType else localMusicViewType
    }
    var folderMenuOpen by remember { mutableStateOf(false) }
    var detailSearching by rememberSaveable(topDetail?.browseId) { mutableStateOf(false) }
    val likeStatuses by viewModel.likeStatuses.collectAsStateWithLifecycle()
    val radioFavoritesStore = remember(context) { PreferencesRadioFavoritesStore(context) }
    val radioFavorites by radioFavoritesStore.stations.collectAsStateWithLifecycle(initialValue = emptyList())
    val radioFavoriteScope = rememberCoroutineScope()
    val offlineLiked by OfflinePlaylists.liked.collectAsStateWithLifecycle()
    val offlinePlaylists by OfflinePlaylists.playlists.collectAsStateWithLifecycle()
    val offlinePlaylistGroups = remember(offlinePlaylists) { offlinePlaylists.map { it.toGroup() } }
    val likeStatusOf: (Song) -> LikeStatus = { song ->
        val uri = song.localUri
        val radioUuid = song.playbackSourceId
            ?.takeIf { it.startsWith("radio:") }
            ?.removePrefix("radio:")
        when {
            !radioUuid.isNullOrBlank() -> if (radioFavorites.any { it.uuid == radioUuid }) LikeStatus.LIKE else LikeStatus.INDIFFERENT
            uri != null -> if (uri in offlineLiked) LikeStatus.LIKE else LikeStatus.INDIFFERENT
            else -> likeStatuses[song.videoId] ?: LikeStatus.INDIFFERENT
        }
    }
    val toggleLikeOf: (Song) -> Unit = { song ->
        val radioUuid = song.playbackSourceId
            ?.takeIf { it.startsWith("radio:") }
            ?.removePrefix("radio:")
        when {
            !radioUuid.isNullOrBlank() -> radioFavoriteScope.launch {
                if (radioFavorites.any { it.uuid == radioUuid }) {
                    radioFavoritesStore.remove(radioUuid)
                } else {
                    radioFavoritesStore.add(
                        RadioStation(
                            uuid = radioUuid,
                            name = song.radioName ?: song.title,
                            streamUrl = song.videoId,
                            favicon = song.thumbnailUrl,
                            country = song.artist.takeUnless { it.equals("Radio", ignoreCase = true) },
                            codec = song.albumName?.takeUnless { it.equals("LIVE", ignoreCase = true) },
                        ),
                    )
                }
            }
            song.localUri != null -> OfflinePlaylists.toggleLike(song)
            else -> viewModel.toggleLike(song.videoId)
        }
    }
    val pinnedToOriginal by OriginalVersion.pinned.collectAsStateWithLifecycle()
    val qualityUpgradesInFlight by NerdStats.racingLossless.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val playlistsLoading by viewModel.playlistsLoading.collectAsStateWithLifecycle()
    val ownedPlaylists by viewModel.playlistOwned.collectAsStateWithLifecycle()

    val savedDownloads by Downloads.saved.collectAsStateWithLifecycle()
    val localMusicFolderUri by AppSettings.localMusicFolderUri.collectAsStateWithLifecycle()
    val filterNonMusicAudio by AppSettings.filterNonMusicAudio.collectAsStateWithLifecycle()
    val webdavUrl by AppSettings.webdavUrl.collectAsStateWithLifecycle()
    val smbHost by AppSettings.smbHost.collectAsStateWithLifecycle()
    val librarySort by AppSettings.librarySort.collectAsStateWithLifecycle()
    val savedCollections by Downloads.collections.collectAsStateWithLifecycle()
    val downloadedReleases = remember(savedCollections, savedDownloads) {
        Downloads.savedReleases()
    }
    val localPlaylists by com.music.bitchord.data.spotify.LocalPlaylistStore.playlists.collectAsStateWithLifecycle()
    val localPlaylistItems = localPlaylists.map { playlist ->
        ShelfItem(
            title = playlist.title,
            subtitle = stringResource(R.string.local_playlist_subtitle, playlist.songs.size),
            thumbnailUrl = playlist.customCoverUri ?: playlist.songs.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl,
            videoId = null,
            browseId = playlist.browseId,
        )
    }
    val downloadIdFor: (String?) -> String? = { id ->
        id?.let { Downloads.recordIdOf(it) ?: it }?.takeIf { it in savedCollections }
    }
    val controller = rememberMediaController()
    val player = rememberPlayerState(controller)
    DisposableEffect(controller) {
        MusicVideoEngine.attach(controller)
        onDispose { MusicVideoEngine.attach(null) }
    }
    val awaitingPartyStart by ListenTogether.awaitingStart.collectAsStateWithLifecycle()
    val playPauseBusy = player.isLoading || awaitingPartyStart
    val controlsLocked = partyState.controlsLocked
    var queueNotice by remember { mutableStateOf<QueueActionNotice?>(null) }
    var queueNoticeId by remember { mutableIntStateOf(0) }
    val showQueueNotice: (String) -> Unit = { message ->
        queueNoticeId += 1
        queueNotice = QueueActionNotice(queueNoticeId, message)
    }
    val hostOnlyMessage = stringResource(R.string.listen_together_host_only_notice)
    val showHostOnlyNotice: () -> Unit = { showQueueNotice(hostOnlyMessage) }

    val refusedByHost: () -> Boolean = {
        val locked = ListenTogether.state.value.controlsLocked
        if (locked) showHostOnlyNotice()
        locked
    }

    val togglePlayPause: () -> Unit = {
        controller?.let { c ->
            val party = ListenTogether.state.value
            if (party.controlsLocked && !party.playback.isPlaying && !c.isPlaying) {
                showHostOnlyNotice()
            } else if (c.isPlaying) {
                c.pause()
            } else {
                c.play()
            }
        }
    }
    val shuffleEnabled by QueueShuffle.enabled.collectAsStateWithLifecycle()
    val versionContext = LocalContext.current
    val versionSwitch = rememberVersionSwitchController()
    var convertedFromVideo by versionSwitch.convertedFromVideoState
    var convertedAudioId by versionSwitch.convertedAudioIdState
    var switchingAudioVersion by versionSwitch.switchingAudioVersionState
    var keepVideoId by versionSwitch.keepVideoIdState
    var convertedFromAudio by versionSwitch.convertedFromAudioState
    var convertedVideoId by versionSwitch.convertedVideoIdState
    var optimisticVersionSong by versionSwitch.optimisticVersionSongState
    val missingVersions = versionSwitch.missingVersions

    val syncedLyricsEnabled by AppSettings.syncedLyrics.collectAsStateWithLifecycle()
    val lyricsSources by AppSettings.lyricsSources.collectAsStateWithLifecycle()
    val lyricsSourceOrder by AppSettings.lyricsSourceOrder.collectAsStateWithLifecycle()
    val prioritizeSyllableSync by AppSettings.prioritizeSyllableSync.collectAsStateWithLifecycle()

    val homeListState = rememberLazyListState()
    val exploreListState = rememberLazyListState()
    val moodGenreListState = rememberLazyListState()
    val libraryListState = rememberLazyListState()
    val historyListState = rememberLazyListState()
    val libraryShowAllGridState = rememberLazyGridState()
    val moodsGridState = rememberLazyGridState()
    val searchListState = rememberLazyListState()
    val radioListState = rememberLazyListState()
    val currentListState = when (selectedTab) {
        TAB_HOME -> homeListState
        TAB_EXPLORE -> if (selectedMoodGenre == null) exploreListState else moodGenreListState
        TAB_RADIO -> radioListState
        TAB_LIBRARY -> libraryListState
        else -> searchListState
    }

    val homePull = rememberPullToRefreshState()
    val explorePull = rememberPullToRefreshState()
    val libraryPull = rememberPullToRefreshState()
    val radioPull = rememberPullToRefreshState()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val currentFeed = when {
        detail != null -> null
        route is AppRoute.LibraryShowAll && route.shelf.title == YtMusicRepository.PLAYLISTS_SHELF ->
            MainViewModel.Feed.LIBRARY
        route != null -> null
        selectedTab == TAB_HOME -> MainViewModel.Feed.HOME
        selectedTab == TAB_EXPLORE -> MainViewModel.Feed.EXPLORE
        selectedTab == TAB_RADIO -> MainViewModel.Feed.RADIO
        selectedTab == TAB_LIBRARY -> MainViewModel.Feed.LIBRARY
        else -> null
    }

    val currentPull = when (currentFeed) {
        MainViewModel.Feed.HOME -> homePull
        MainViewModel.Feed.EXPLORE -> explorePull
        MainViewModel.Feed.LIBRARY -> libraryPull
        MainViewModel.Feed.RADIO -> radioPull
        null -> null
    }
    val scrolled by remember(currentListState) {
        derivedStateOf {
            currentListState.firstVisibleItemIndex > 0 ||
                currentListState.firstVisibleItemScrollOffset > 24
        }
    }

    val replayEntryId = navigator.entries.lastOrNull { it.route == AppRoute.Replay }?.id
    val replayListState = remember(replayEntryId, replayLandingPage) { LazyListState() }
    val replayScrolled by remember(replayListState) {
        derivedStateOf {
            replayListState.firstVisibleItemIndex > 0 ||
                replayListState.firstVisibleItemScrollOffset > 24
        }
    }

    val detailListStates = remember { mutableMapOf<String, LazyListState>() }
    val detailListState = topDetail?.browseId?.let { browseId ->
        detailListStates.getOrPut(browseId) { LazyListState() }
    } ?: remember { LazyListState() }
    val localFolderStates = remember { mutableMapOf<String, LocalFolderState>() }
    val pageStates = rememberSaveableStateHolder()
    val pageKeysSeen = remember { mutableSetOf<String>() }
    val livePageKeys = navigator.entries.map { AppDestination.Route(it).key } +
        detailStack.map { AppDestination.Release(it.browseId).key }
    val detailTitleDrop = with(LocalDensity.current) { DETAIL_TITLE_DROP.toPx() }
    val detailScrolled by remember(detailListState, detailTitleDrop) {
        derivedStateOf {
            detailListState.firstVisibleItemIndex > 0 ||
                detailListState.firstVisibleItemScrollOffset > detailTitleDrop
        }
    }

    val homeLabel = stringResource(R.string.home)
    val playLabel = stringResource(R.string.play)
    val exploreLabel = stringResource(R.string.explore)
    val libraryLabel = stringResource(R.string.library)
    val searchLabel = stringResource(R.string.search)
    val historyLabel = stringResource(R.string.history)
    val replayLabel = stringResource(R.string.replay)
    val queueLabel = stringResource(R.string.queue)
    val sharedLinkLabel = stringResource(R.string.shared_link)
    val openedFileLabel = stringResource(R.string.opened_file)
    val radioLabel = stringResource(com.soundchord.features.R.string.radio_name)
    val tabs = remember(homeLabel, exploreLabel, radioLabel, libraryLabel, searchLabel) {
        listOf(
            BottomTab(homeLabel, BitChordIcons.Home),
            BottomTab(exploreLabel, BitChordIcons.TabExplore),
            BottomTab(radioLabel, SoundChordIcons.TabRadio),
            BottomTab(libraryLabel, BitChordIcons.TabLibrary),
            BottomTab(searchLabel, BitChordIcons.TabSearch),
        )
    }


    val onSelectTab: (Int) -> Unit = { index ->
        if (recognitionOpen) {
            recognitionOpen = false
            MusicRecognitionService.reset()
        }
        navigator.clear()
        viewModel.closeMoodGenre()
        radioSubpageBack = null
        selectedTab = index

        if (index == TAB_SEARCH) {
            viewModel.onQueryChange("")
            searchFocusRequested = true
        }
    }

    val appActions = rememberBitChordAppActions(
        context = context,
        controller = controller,
        viewModel = viewModel,
        navigator = navigator,
        player = player,
        topDetail = topDetail,
        route = route,
        filter = filter,
        query = query,
        results = results,
        likeStatuses = likeStatuses,
        queueLabel = queueLabel,
        searchLabel = searchLabel,
        sharedLinkLabel = sharedLinkLabel,
        openedFileLabel = openedFileLabel,
        versionSwitch = versionSwitch,
        versionContext = versionContext,
        refusedByHost = refusedByHost,
        showQueueNotice = showQueueNotice,
        showHostOnlyNotice = showHostOnlyNotice,
        dismissPlayer = dismissPlayer,
        togglePlayPause = togglePlayPause,
        openSongMenu = openSongMenu,
        openBrowseMenu = openBrowseMenu,
        downloadIdFor = downloadIdFor,
        folderSortOf = folderSortOf,
        folderViewTypeOf = folderViewTypeOf,
        likeStatusOf = likeStatusOf,
        toggleLikeOf = toggleLikeOf,
        activeRadioSeed = activeRadioSeed,
        onActiveRadioSeedChanged = { activeRadioSeed = it },
        onSelectTab = onSelectTab,
        onRecognitionOpenChange = { recognitionOpen = it },
        onShowRecognitionHistoryClearConfirmationChange = { showRecognitionHistoryClearConfirmation = it },
        onShowRadioHistoryClearConfirmationChange = { showRadioHistoryClearConfirmation = it },
        onRadioSubpageBackChange = { radioSubpageBack = it },
        onReplayStoryChange = { replayStory = it },
        onShowReplayShareChange = { showReplayShare = it },
        onReplaySharePageChange = { replaySharePage = it },
        onReplayLandingPageChange = { replayLandingPage = it },
        onShowSpotifyCanvasAuthChange = { showSpotifyCanvasAuth = it },
        onEditingSourceChange = { editingSource = it },
        onChoosingSourceQualityChange = { choosingSourceQuality = it },
        onExplainingOutdatedAddonChange = { explainingOutdatedAddon = it },
        onConfirmJioSaavnChange = { confirmJioSaavn = it },
        onConfirmBitPerfectChange = { confirmBitPerfect = it },
        onConfirmFixedVolumeChange = { confirmFixedVolume = it },
        onBlockedAddonSongChange = { blockedAddonSong = it },
        onAddonLeftOutOfDownloadChange = { addonLeftOutOfDownload = it },
        onEditingPartyServerChange = { editingPartyServer = it },
        onEditingSignatureApiChange = { editingSignatureApi = it },
        onShowLyricsSourcesChange = { showLyricsSources = it },
        onSongSortMenuOpenChange = { songSortMenuOpen = it },
        onFolderMenuOpenChange = { folderMenuOpen = it },
        onDetailSearchingChange = { detailSearching = it },
        onShowAccountSelectorChange = { showAccountSelector = it },
        onShowAppLanguageChange = { showAppLanguage = it },
        onShowTranslationLanguageChange = { showTranslationLanguage = it },
        onShowListenBrainzLoginChange = { showListenBrainzLogin = it },
        onShowLastfmLoginChange = { showLastfmLogin = it },
        onShowWebDavEditorChange = { showWebDavEditor = it },
        onShowSmbEditorChange = { showSmbEditor = it },
        onShowDiscordLoginChange = { showDiscordLogin = it },
        onShowSpotifyLoginChange = { showSpotifyLogin = it },
        onDiscordDialogChange = { discordDialog = it },
        onShowAboutWhatsNewChange = { showAboutWhatsNew = it },
        onPlaylistTargetChange = { playlistTarget = it },
        onCreatingPlaylistChange = { creatingPlaylist = it },
        onOfflinePlaylistTargetChange = { offlinePlaylistTarget = it },
        onCreatingOfflinePlaylistChange = { creatingOfflinePlaylist = it },
        onEditPlaylistTargetChange = { editPlaylistTarget = it },
        onShowSpotifyImportDialogChange = { showSpotifyImportDialog = it },
        onSongActionsChange = { songActions = it },
        onSongMenuOriginChange = { songMenuOrigin = it },
        onSongMenuAnchorChange = { songMenuAnchor = it },
        onMenuFromPlayerChange = { menuFromPlayer = it },
        onMenuFromHistoryChange = { menuFromHistory = it },
        onBrowseActionsChange = { browseActions = it },
        onBrowseMenuOriginChange = { browseMenuOrigin = it },
        onBrowseMenuAnchorChange = { browseMenuAnchor = it },
        onBrowseRenameInSheetChange = { browseRenameInSheet = it },
        onStationActionsChange = { stationActions = it },
        onSearchFocusRequestedChange = { searchFocusRequested = it },
        onReorderTargetChange = { reorderTarget = it },
        onReorderEntriesChange = { reorderEntries = it },
        onReorderSavingChange = { reorderSaving = it },
        onShowNowPlayingChange = { showNowPlaying = it },
        onPlayerLiveCanvasChange = { playerLiveCanvas = it },
        onShowDiwaliChange = { showDiwali = it },
        onShowUpdateDialogChange = { showUpdateDialog = it },
        onWebSessionChange = { webSession = it },
        onActiveJamInviteCodeChange = { activeJamInviteCode = it },
        onActiveJamInviteServerChange = { activeJamInviteServer = it },
        onRecognitionFabVisibleChange = { recognitionFabVisible = it },
        onShowLyricsOffsetChange = { showLyricsOffset = it },
        onOptimisticVersionSongChange = { optimisticVersionSong = it },
        onConvertedFromVideoChange = { song, id ->
            convertedFromVideo = song
            convertedVideoId = id
        },
        onConvertedFromAudioChange = { song, id ->
            convertedFromAudio = song
            convertedAudioId = id
        },
        onKeepVideoIdChange = { keepVideoId = it },
    )

    val listPadding = PaddingValues(
        top = topBarContentPadding(),
        bottom = if (player.song != null) 210.dp else 140.dp,
    )

    val appleArtVersion by AppleArtistArtRepository.updates.collectAsStateWithLifecycle()
    val detailApple = remember(topDetail?.browseId, topDetail?.type, appleArtVersion) {
        topDetail?.takeIf { it.type == BrowseType.ARTIST }
            ?.let { AppleArtistArtRepository.cached(it.browseId) }
    }
    val detailPalette = rememberArtworkPalette(
        imageUrl = detailApple?.heroUrl ?: topDetail?.thumbnailUrl,
        keyColors = detailApple?.keyColors(),
    )

    val replayOpen = route == AppRoute.Replay || replayStory != null || showReplayShare ||
        (selectedTab == TAB_LIBRARY && detail == null && route == null)
    val (replay, setReplayPeriod) = rememberReplayState(replayOpen)
    val replayCards = remember(replay.summary) {
        replay.summary?.takeUnless { it.isEmpty }?.cards(context).orEmpty()
    }

    val playerShowing = showNowPlaying
    var links by remember { mutableStateOf<Song?>(null) }
    val playerSong = player.song?.let { current ->
        val extra = links?.takeIf { it.videoId == current.videoId } ?: return@let current
        current.copy(
            artistId = current.artistId ?: extra.artistId,
            albumId = current.albumId ?: extra.albumId,
            albumName = current.albumName ?: extra.albumName,
        )
    }

    val navState = BitChordNavState(
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
        glassActive = glassActive,
        glassSamplesBackdrop = glassSamplesBackdrop,
        detailListStates = detailListStates,
        localFolderStates = localFolderStates,
        pageStates = pageStates,
        pageKeysSeen = pageKeysSeen,
        radioHistoryStore = radioHistoryStore,
        radioFavoritesStore = radioFavoritesStore,
        radioFavoriteScope = radioFavoriteScope,
        homeListState = homeListState,
        exploreListState = exploreListState,
        moodGenreListState = moodGenreListState,
        libraryListState = libraryListState,
        historyListState = historyListState,
        libraryShowAllGridState = libraryShowAllGridState,
        moodsGridState = moodsGridState,
        searchListState = searchListState,
        radioListState = radioListState,
        route = route,
        detailPalette = detailPalette,
        detailScrolled = detailScrolled,
        isReleaseDetail = isReleaseDetail,
        topDetail = topDetail,
        detailSongSorts = detailSongSorts,
        downloadedMusicSort = downloadedMusicSort,
        localMusicSort = localMusicSort,
        downloadedMusicViewType = downloadedMusicViewType,
        localMusicViewType = localMusicViewType,
        activeAccountId = activeAccountId,
        googleAccounts = googleAccounts,
        currentFeed = currentFeed,
        currentPull = currentPull,
        homePull = homePull,
        explorePull = explorePull,
        libraryPull = libraryPull,
        radioPull = radioPull,
        historyLabel = historyLabel,
        radioLabel = radioLabel,
        replayLabel = replayLabel,
        searchLabel = searchLabel,
        replayListState = replayListState,
        progressiveBlur = progressiveBlur,
        tabs = tabs,
        replayScrolled = replayScrolled,
        scrolled = scrolled,
        partyServerStatus = partyServerStatus,
        updateNotice = updateNotice,
        recognitionFabVisible = recognitionFabVisible,
        librarySort = librarySort,
        reduceDynamicBlur = reduceDynamicBlur,
    )

    val playerUiState = BitChordPlayerUiState(
        playerRaised = playerShowing && playerSong != null,
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
        activeRadioSeed = activeRadioSeed,
        partyState = partyState,
        account = account,
        songActions = songActions,
        songMenuOrigin = songMenuOrigin,
        songMenuAnchor = songMenuAnchor,
        menuFromPlayer = menuFromPlayer,
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
        showLyricsOffset = showLyricsOffset,
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

    val sheetsUiState = BitChordSheetsUiState(
        pageBackdrop = pageBackdrop,
        reduceDynamicBlur = reduceDynamicBlur,
        playerRaised = playerShowing && playerSong != null,
        songActions = songActions,
        songMenuOrigin = songMenuOrigin,
        songMenuAnchor = songMenuAnchor,
        menuFromPlayer = menuFromPlayer,
        browseActions = browseActions,
        browseMenuOrigin = browseMenuOrigin,
        browseMenuAnchor = browseMenuAnchor,
        browseRenameInSheet = browseRenameInSheet,
        stationActions = stationActions,
        replay = replay,
        replayStory = replayStory,
        showReplayShare = showReplayShare,
        replaySharePage = replaySharePage,
        account = account,
        playlistTarget = playlistTarget,
        creatingPlaylist = creatingPlaylist,
        playlists = playlists,
        playlistsLoading = playlistsLoading,
        offlinePlaylistTarget = offlinePlaylistTarget,
        creatingOfflinePlaylist = creatingOfflinePlaylist,
        offlinePlaylists = offlinePlaylists,
        showSpotifyImportDialog = showSpotifyImportDialog,
        signedIn = signedIn,
        editPlaylistTarget = editPlaylistTarget,
        detailStack = detailStack,
        libraryState = libraryState,
        likeStatuses = likeStatuses,
        query = query,
        results = results,
        filter = filter,
        onboardingShown = onboardingShown,
        webSession = webSession,
        googleAccounts = googleAccounts,
        activeAccountId = activeAccountId,
        activeProfileId = activeProfileId,
        showDiwali = showDiwali,
        showUpdateDialog = showUpdateDialog,
        onboardingComplete = onboardingComplete,
        updateAvailable = updateAvailable,
        showLyricsSources = showLyricsSources,
        songSortMenuOpen = songSortMenuOpen,
        songSort = songSort,
        detail = detail,
        folderMenuOpen = folderMenuOpen,
        showAccountSelector = showAccountSelector,
        showAppLanguage = showAppLanguage,
        showTranslationLanguage = showTranslationLanguage,
        showListenBrainzLogin = showListenBrainzLogin,
        listenBrainzToken = listenBrainzToken,
        showLastfmLogin = showLastfmLogin,
        showWebDavEditor = showWebDavEditor,
        showSmbEditor = showSmbEditor,
        showDiscordLogin = showDiscordLogin,
        showSpotifyLogin = showSpotifyLogin,
        showSpotifyCanvasAuth = showSpotifyCanvasAuth,
        discordDialog = discordDialog,
        editingPartyServer = editingPartyServer,
        editingSignatureApi = editingSignatureApi,
        editingSource = editingSource,
        choosingSourceQuality = choosingSourceQuality,
        explainingOutdatedAddon = explainingOutdatedAddon,
        confirmJioSaavn = confirmJioSaavn,
        confirmBitPerfect = confirmBitPerfect,
        confirmFixedVolume = confirmFixedVolume,
        blockedAddonSong = blockedAddonSong,
        addonLeftOutOfDownload = addonLeftOutOfDownload,
        showAboutWhatsNew = showAboutWhatsNew,
        recognitionOpen = recognitionOpen,
    )

    val appState = BitChordAppState(
        nav = navState,
        player = playerUiState,
        sheets = sheetsUiState,
    )


    BitChordAppEffects(
        state = appState,
        actions = appActions,
        navigator = navigator,
        viewModel = viewModel,
        controller = controller,
        versionSwitch = versionSwitch,
        context = context,
        openPlayerRequested = openPlayerRequested,
        diwaliAutoShown = diwaliAutoShown,
        onSetDiwaliAutoShown = { diwaliAutoShown = it },
        updateDialogShown = updateDialogShown,
        onSetUpdateDialogShown = { updateDialogShown = it },
        incomingJamInvite = incomingJamInvite,
        syncedLyricsEnabled = syncedLyricsEnabled,
        lyricsSources = lyricsSources,
        lyricsSourceOrder = lyricsSourceOrder,
        prioritizeSyllableSync = prioritizeSyllableSync,
        linkRequest = linkRequest,
        sharedLinkLabel = sharedLinkLabel,
        searchLabel = searchLabel,
        openedFileLabel = openedFileLabel,
        playerShowing = playerShowing,
        playerSong = playerSong,
        onSetLinks = { links = it },
        onSetLinksLoading = { linksLoading = it },
        onClearQueueNotice = { queueNotice = null },
        appForeground = appForeground,
        savedDownloads = savedDownloads,
        localMusicFolderUri = localMusicFolderUri,
        filterNonMusicAudio = filterNonMusicAudio,
        smbHost = smbHost,
        livePageKeys = livePageKeys,
        onActiveJamInviteCodeChange = { activeJamInviteCode = it },
        onActiveJamInviteServerChange = { activeJamInviteServer = it },
        onRecognitionFabVisibleChange = { recognitionFabVisible = it },
    )
    BitChordAppContent(
        state = appState,
        actions = appActions,
        windowWidth = windowWidth,
        windowHeight = windowHeight,
        appBackdrop = appBackdrop,
        hazeState = hazeState,
        navBarScroll = navBarScroll,
        playerDock = playerDock,
        playerSheetMotion = playerSheetMotion,
        navigator = navigator,
        viewModel = viewModel,
        controller = controller,
    )
}



@Composable
private fun rememberBitChordAppActions(
    context: Context,
    controller: MediaController?,
    viewModel: MainViewModel,
    navigator: AppNavigator,
    player: PlayerState,
    topDetail: DetailPage?,
    route: AppRoute?,
    filter: SearchFilter,
    query: String,
    results: UiState<List<SearchResult>>?,
    likeStatuses: Map<String, LikeStatus>,
    queueLabel: String,
    searchLabel: String,
    sharedLinkLabel: String,
    openedFileLabel: String,
    versionSwitch: VersionSwitchController,
    versionContext: Context,
    refusedByHost: () -> Boolean,
    showQueueNotice: (String) -> Unit,
    showHostOnlyNotice: () -> Unit,
    dismissPlayer: () -> Unit,
    togglePlayPause: () -> Unit,
    openSongMenu: (Song) -> Unit,
    openBrowseMenu: (BrowseTarget) -> Unit,
    downloadIdFor: (String?) -> String?,
    folderSortOf: (String) -> LocalMusicSort,
    folderViewTypeOf: (String) -> LibraryViewType,
    likeStatusOf: (Song) -> LikeStatus,
    toggleLikeOf: (Song) -> Unit,
    activeRadioSeed: Pair<String, String>?,
    onActiveRadioSeedChanged: (Pair<String, String>?) -> Unit,
    onSelectTab: (Int) -> Unit,
    onRecognitionOpenChange: (Boolean) -> Unit,
    onShowRecognitionHistoryClearConfirmationChange: (Boolean) -> Unit,
    onShowRadioHistoryClearConfirmationChange: (Boolean) -> Unit,
    onRadioSubpageBackChange: ((() -> Unit)?) -> Unit,
    onReplayStoryChange: (ReplayStoryPage?) -> Unit,
    onShowReplayShareChange: (Boolean) -> Unit,
    onReplaySharePageChange: (ReplayStoryPage?) -> Unit,
    onReplayLandingPageChange: (ReplayStoryPage) -> Unit,
    onShowSpotifyCanvasAuthChange: (Boolean) -> Unit,
    onEditingSourceChange: (SourceConfig?) -> Unit,
    onChoosingSourceQualityChange: (SourceConfig?) -> Unit,
    onExplainingOutdatedAddonChange: (SourceConfig?) -> Unit,
    onConfirmJioSaavnChange: (Boolean) -> Unit,
    onConfirmBitPerfectChange: (BitPerfectMode?) -> Unit,
    onConfirmFixedVolumeChange: (Boolean) -> Unit,
    onBlockedAddonSongChange: (Song?) -> Unit,
    onAddonLeftOutOfDownloadChange: (Pair<String, SourceRegistry.DownloadExclusion>?) -> Unit,
    onEditingPartyServerChange: (Boolean) -> Unit,
    onEditingSignatureApiChange: (Boolean) -> Unit,
    onShowLyricsSourcesChange: (Boolean) -> Unit,
    onSongSortMenuOpenChange: (Boolean) -> Unit,
    onFolderMenuOpenChange: (Boolean) -> Unit,
    onShowAccountSelectorChange: (Boolean) -> Unit,
    onShowAppLanguageChange: (Boolean) -> Unit,
    onShowTranslationLanguageChange: (Boolean) -> Unit,
    onShowListenBrainzLoginChange: (Boolean) -> Unit,
    onShowLastfmLoginChange: (Boolean) -> Unit,
    onShowWebDavEditorChange: (Boolean) -> Unit,
    onShowSmbEditorChange: (Boolean) -> Unit,
    onShowDiscordLoginChange: (Boolean) -> Unit,
    onShowSpotifyLoginChange: (Boolean) -> Unit,
    onDiscordDialogChange: (DiscordDialog?) -> Unit,
    onShowAboutWhatsNewChange: (Boolean) -> Unit,
    onPlaylistTargetChange: (Song?) -> Unit,
    onCreatingPlaylistChange: (Boolean) -> Unit,
    onOfflinePlaylistTargetChange: (Song?) -> Unit,
    onCreatingOfflinePlaylistChange: (Boolean) -> Unit,
    onEditPlaylistTargetChange: (UserPlaylist?) -> Unit,
    onShowSpotifyImportDialogChange: (Boolean) -> Unit,
    onSongActionsChange: (Song?) -> Unit,
    onSongMenuOriginChange: (HeldItem?) -> Unit,
    onSongMenuAnchorChange: (Rect?) -> Unit,
    onMenuFromPlayerChange: (Boolean) -> Unit,
    onMenuFromHistoryChange: (Boolean) -> Unit,
    onBrowseActionsChange: (BrowseTarget?) -> Unit,
    onBrowseMenuOriginChange: (HeldItem?) -> Unit,
    onBrowseMenuAnchorChange: (Rect?) -> Unit,
    onBrowseRenameInSheetChange: (Boolean) -> Unit,
    onStationActionsChange: (StationMenuTarget?) -> Unit,
    onSearchFocusRequestedChange: (Boolean) -> Unit,
    onDetailSearchingChange: (Boolean) -> Unit,
    onReorderTargetChange: (UserPlaylist?) -> Unit,
    onReorderEntriesChange: (UiState<List<Song>>) -> Unit,
    onReorderSavingChange: (Boolean) -> Unit,
    onShowNowPlayingChange: (Boolean) -> Unit,
    onPlayerLiveCanvasChange: (Boolean) -> Unit,
    onShowDiwaliChange: (Boolean) -> Unit,
    onShowUpdateDialogChange: (Boolean) -> Unit,
    onWebSessionChange: (WebSessionMode?) -> Unit,
    onActiveJamInviteCodeChange: (String?) -> Unit,
    onActiveJamInviteServerChange: (String?) -> Unit,
    onRecognitionFabVisibleChange: (Boolean) -> Unit,
    onShowLyricsOffsetChange: (Boolean) -> Unit,
    onOptimisticVersionSongChange: (Song?) -> Unit,
    onConvertedFromVideoChange: (Song?, String?) -> Unit,
    onConvertedFromAudioChange: (Song?, String?) -> Unit,
    onKeepVideoIdChange: (String?) -> Unit,
): BitChordAppActions {
    val scope = rememberCoroutineScope()
    var playRequestGeneration by remember { mutableIntStateOf(0) }

    fun uploadToWebDav(songs: List<Song>) {
        uploadTracksToWebDav(scope, context, songs, topDetail?.browseId, viewModel, showQueueNotice)
    }

    suspend fun switchToMusicOnly(song: Song, pauseWhileResolving: Boolean) {
        versionSwitch.switchToMusicOnly(controller, song, pauseWhileResolving, versionContext, showQueueNotice)
    }

    suspend fun switchToVideo(song: Song, pauseWhileResolving: Boolean) {
        versionSwitch.switchToVideo(controller, song, pauseWhileResolving, versionContext, showQueueNotice)
    }

    fun playFrom(
        allSongs: List<Song>,
        allIndex: Int,
        source: QueueSource,
        boundedContext: Boolean = false,
        continuationToken: String? = null,
    ) {
        playFrom(
            scope = scope,
            controller = controller,
            playerQueue = player.queue,
            allSongs = allSongs,
            allIndex = allIndex,
            source = source,
            boundedContext = boundedContext,
            continuationToken = continuationToken,
            refusedByHost = refusedByHost,
            onPlayRequested = {
                playRequestGeneration++
                onActiveRadioSeedChanged(null)
            },
        )
    }
    // Kept for entry points whose rows already carry their origin (notably a
    // collection action fetched before this callback). The explicit wrappers
    // below are preferred because a track's album is not necessarily where it
    // was played from.
    val play: (List<Song>, Int) -> Unit = { songs, index ->
        val first = songs.getOrNull(index)
        val source = QueueSource(
            title = first?.playbackSource ?: first?.albumName ?: queueLabel,
            type = first?.playbackSourceType ?: PlaybackSourceType.QUEUE,
            id = first?.playbackSourceId,
        )
        playFrom(songs, index, source)
    }

    /**
     * A song picked on its own — off a home card or a search hit — starts a
     * station rather than queueing the list it was shown in. Searching
     * "Perfect" and tapping the top hit otherwise queues twenty covers and
     * remixes of the same song. Album, artist and playlist pages keep [play],
     * where the surrounding list *is* the thing the user asked for.
     */
    val playRadio: (Song, QueueSource) -> Unit = { song, source ->
        playRequestGeneration++
        onActiveRadioSeedChanged(null)
        scope.launch {
            if (refusedByHost()) return@launch
            val c = controller ?: return@launch
            val currentTimeline = player.queue.takeIf { it.size == c.mediaItemCount }
                ?: (0 until c.mediaItemCount).map { c.getMediaItemAt(it).toSong() }
            val currentIndex = c.currentMediaItemIndex
            val oneOffQueue = QueueCoordinator.buildOneOffQueue(
                currentTimeline = currentTimeline,
                currentIndex = currentIndex,
                tappedSong = song,
                source = source,
            )
            c.playSongs(oneOffQueue, 0)
            // Start radio in the mini-player; the user opens the full view by tapping it.
        }
    }

    val playRecognizedSong: (RecognitionResult) -> Unit = { result ->
        navigator.clear()
        onRecognitionOpenChange(false)
        val videoId = result.youtubeVideoId
        if (!videoId.isNullOrBlank()) {
            onSelectTab(TAB_HOME)
            playRadio(
                Song(
                    videoId = videoId,
                    title = result.title,
                    artist = result.artist,
                    thumbnailUrl = result.coverArtHqUrl ?: result.coverArtUrl,
                    albumName = result.album,
                ),
                QueueSource(searchLabel, PlaybackSourceType.SEARCH),
            )
        } else {
            onSelectTab(TAB_SEARCH)
            viewModel.setSearchSource(SearchSource.YOUTUBE)
            viewModel.onQueryChange(listOf(result.title, result.artist).filter(String::isNotBlank).joinToString(" "))
            viewModel.submitSearch()
        }
    }

    val playRecognitionHistoryEntry: (RecognitionHistoryEntry) -> Unit = { entry ->
        navigator.clear()
        onShowRecognitionHistoryClearConfirmationChange(false)
        val videoId = entry.videoId
        if (!videoId.isNullOrBlank()) {
            onSelectTab(TAB_HOME)
            playRadio(
                Song(
                    videoId = videoId,
                    title = entry.title,
                    artist = entry.artist,
                    thumbnailUrl = entry.artworkUrl,
                ),
                QueueSource(searchLabel, PlaybackSourceType.SEARCH),
            )
        } else {
            onSelectTab(TAB_SEARCH)
            viewModel.setSearchSource(SearchSource.YOUTUBE)
            viewModel.onQueryChange(listOf(entry.title, entry.artist).filter(String::isNotBlank).joinToString(" "))
            viewModel.submitSearch()
        }
    }

    /**
     * Starts the explicit station offered by every song overflow menu.
     *
     * The related tracks come from YouTube Music's own RDAMVM watch queue.
     * Loading happens before the player is touched so a failed request cannot
     * destroy the queue already playing. Once ready, the whole old queue is
     * replaced in one Media3 operation.
     */
    val startRadio: (Song) -> Unit = { song ->
        startRadioStation(
            scope = scope,
            context = context,
            controller = controller,
            song = song,
            refusedByHost = refusedByHost,
            onPlayRequested = { ++playRequestGeneration },
            isCurrentRequest = { it == playRequestGeneration },
            onActiveRadioSeedChanged = onActiveRadioSeedChanged,
        )
    }

    val openTopPick: (TopPick, String) -> Unit = { pick, label ->
        openTopPickCard(
            scope = scope,
            context = context,
            controller = controller,
            viewModel = viewModel,
            pick = pick,
            label = label,
            refusedByHost = refusedByHost,
            onPlayRequested = { ++playRequestGeneration },
            isCurrentRequest = { it == playRequestGeneration },
            onActiveRadioSeedChanged = onActiveRadioSeedChanged,
        )
    }

    val playMood: (TopPickMood, String) -> Unit = { mood, title ->
        val hints = viewModel.topPicks.value?.firstOrNull { it.kind == TopPickKind.MOOD }?.hints
        openTopPick(
            if (hints != null) TopPicks.moodStation(mood, hints) else TopPicks.moodStation(mood),
            title,
        )
    }

    val addToQueue: (Song) -> Unit = { song ->
        enqueueSingleTrack(
            scope = scope,
            context = context,
            controller = controller,
            playerQueue = player.queue,
            song = song,
            next = false,
            queueLabel = queueLabel,
            refusedByHost = refusedByHost,
            showQueueNotice = showQueueNotice,
        )
    }

    val playNext: (Song) -> Unit = { song ->
        enqueueSingleTrack(
            scope = scope,
            context = context,
            controller = controller,
            playerQueue = player.queue,
            song = song,
            next = true,
            queueLabel = queueLabel,
            refusedByHost = refusedByHost,
            showQueueNotice = showQueueNotice,
        )
    }

    val onSongSwipe: (Song) -> Unit = { song ->
        if (AppSettings.swipeToPlayNext.value) playNext(song) else addToQueue(song)
    }

    /**
     * Opens an artist or release page given its browse id — or, failing that,
     * its name.
     *
     * Replay's charts are the reason this exists. An artist there is counted by
     * *name*, because a name is the only thing every track carries: a browse id
     * rides along only when the row that queued the track happened to have one,
     * which for a home-feed card or an AutoPlay suggestion it does not. So half
     * the rows on a chart would have nothing to open, and a row that does
     * nothing when tapped is worse than a row that isn't tappable — it reads as
     * the app having failed rather than as the app not offering.
     *
     * Searching for the name is what the user would do next anyway, and it is
     * what the app already does to find a video's catalogue release (see
     * [YtMusicRepository.resolveAudio]). A search that finds nothing says so,
     * which is at least an answer.
     */
    fun openByName(
        browseId: String?,
        name: String,
        subtitle: String?,
        type: BrowseType,
        artwork: String? = null,
    ) {
        openByName(context, scope, viewModel, browseId, name, subtitle, type, artwork)
    }

    val queueSongs: (List<Song>, Boolean) -> Unit = { songs, next ->
        enqueueBatchSongs(
            scope = scope,
            context = context,
            controller = controller,
            playerQueue = player.queue,
            songs = songs,
            next = next,
            queueLabel = queueLabel,
            refusedByHost = refusedByHost,
            showQueueNotice = showQueueNotice,
            playFallback = play,
        )
    }
    val addSongsToQueue: (List<Song>) -> Unit = { songs -> queueSongs(songs, false) }
    val playSongsNext: (List<Song>) -> Unit = { songs -> queueSongs(songs, true) }

    // ---- Album / playlist menu ----

    /**
     * Holding an album or playlist card, wherever one is drawn.
     *
     * Artists are left out. An artist page is a selection of their work rather
     * than a running order, and "add Radiohead to the queue" has no answer that
     * isn't a guess — so holding an artist card does nothing, as it did before.
     */
    val onBrowseLongPress: (ShelfItem) -> Unit = { item ->
        val id = item.browseId
        val type = id?.let { viewModel.browseTypeOf(it) }
        if (id != null && type != BrowseType.ARTIST) {
            openBrowseMenu(
                BrowseTarget(
                    browseId = id,
                    title = item.title,
                    subtitle = item.subtitle,
                    thumbnailUrl = item.thumbnailUrl,
                    type = type ?: BrowseType.OTHER,
                    downloadId = downloadIdFor(id),
                ),
            )
        }
    }

    /**
     * The track a song card stands for, or null if the card is a collection.
     *
     * The card's own subtitle is billed as "Song • Chelsea Wolfe"; only the
     * credit belongs in the field the player, mini player and everything
     * downstream read.
     */
    val onShelfLongPress: (ShelfItem) -> Unit = { item ->
        val song = shelfSong(item)
        if (song != null) openSongMenu(song) else onBrowseLongPress(item)
    }

    val withBrowseSongs: (BrowseTarget, (List<Song>) -> Unit) -> Unit = { target, action ->
        withBrowseSongs(context, viewModel, target, action)
    }

    // ---- Downloads ----
    // Two permissions, and never both on one device: writing to the shared
    // Music folder needs storage access below API 29 and none at all from
    // 29 on, where MediaStore grants an app its own rows; notifications are
    // only asked for from API 33. So the branches below are mutually exclusive
    // by SDK level, and nothing here can stack two dialogs on each other.
    var downloadPending by remember { mutableStateOf<List<Song>>(emptyList()) }
    /**
     * What the pending batch was asked for as, held alongside it for the same
     * reason: the storage-permission dialog is a round trip through another
     * process, and the release has to survive it or a whole album granted
     * permission arrives as forty loose tracks.
     */
    var downloadPendingFrom by remember { mutableStateOf<DownloadTarget?>(null) }
    val notifyPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Refusing costs the progress notification, not the download. */ }
    val storagePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val songs = downloadPending
        val from = downloadPendingFrom
        downloadPending = emptyList()
        downloadPendingFrom = null
        when {
            songs.isEmpty() -> Unit
            granted -> {
                songs.forEach { Downloads.enqueue(context, it, from?.title) }
                if (from != null) Downloads.markRequested(from.id, songs.map { it.videoId })
            }
            // The one case where refusing is fatal: below API 29 there is no
            // other way to reach the Music folder.
            else -> Toast
                .makeText(context, context.getString(R.string.storage_required_save), Toast.LENGTH_SHORT)
                .show()
        }
    }
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.reloadLocalDetail("local:all")
            viewModel.reloadLocalDetail("local:downloads")
            viewModel.loadLibrarySongs()
        } else {
            Toast.makeText(context, context.getString(R.string.storage_required_read), Toast.LENGTH_SHORT).show()
        }
    }
    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    // The Search tab's Library source reads the Local Music folder, so it asks
    // for the same permission opening that folder does.
    val onSearchSourceChange: (SearchSource) -> Unit = { source ->
        if (source == SearchSource.LIBRARY && !LocalMediaRepository.hasStoragePermission(context)) {
            mediaPermissionLauncher.launch(mediaPermission)
        }
        viewModel.setSearchSource(source)
    }
    // Shared by the Library tab itself and by a shelf's "Show all" page, so a
    // card opens the same way from either.
    val onLibraryItemClick: (ShelfItem) -> Unit = { item ->
        item.browseId?.let { id ->
            if (id == SPOTIFY_BROWSE_ID) {
                navigator.push(AppRoute.Spotify)
                return@let
            }
            if ((id == "local:all" || id == "local:downloads") && !LocalMediaRepository.hasStoragePermission(context)) {
                mediaPermissionLauncher.launch(mediaPermission)
            }
            // Opened from a shelf's "Show all" page, it stacks over the grid
            // exactly as one opened from the Library tab stacks over that, so
            // back from the release lands on the grid rather than skipping
            // past it.
            viewModel.openDetail(
                browseId = id,
                title = item.title,
                subtitle = item.subtitle,
                thumbnailUrl = item.thumbnailUrl,
            )
        }
    }
    // Takes a list so a single tap on an album/playlist header can queue the
    // whole thing — the permission dance only needs to happen once for the
    // batch, not once per track.
    //
    // [from] is what the list *is*, when it is a release rather than a
    // selection: an album or a playlist. It is recorded whole, so the Downloads
    // page can offer the thing that was tapped back rather than the forty rows
    // it decomposed into — see [Downloads.rememberCollection]. Null for a single
    // track, which is not a release however many of them are asked for one at a
    // time.
    val startDownload: (List<Song>, DownloadTarget?) -> Unit = { requested, from ->
        val saved = Downloads.saved.value
        // Already on disk, and already queued or running: neither needs asking
        // again. What's left is what a tap on "Download" actually means.
        //
        // The release's cover is stamped onto any row that hasn't got one, as a
        // last check before the tap becomes a file.
        //
        // An album page bills its artwork once, in the header — its track rows
        // carry no thumbnail at all, see [InnertubeParser.parseResponsiveListItem]
        // — and a row that reaches [MediaTagger.artworkFor] with a null url is a
        // track saved with no cover in the file and none in [SavedSongMetadata]
        // either, so nothing downstream can draw one afterwards.
        // `MainViewModel.withArtwork` normally fills those in as a page loads and
        // covers the usual route here; this is the backstop for a list that
        // reached this function some other way, and it is worth having precisely
        // because the failure is silent and permanent — the file is written
        // without a cover, and re-downloading adopts the untagged copy rather
        // than replacing it.
        val songs = requested
            .filter { it.videoId !in saved }
            .map { song ->
                val cover = from?.thumbnailUrl
                if (song.thumbnailUrl.isNullOrBlank() && !cover.isNullOrBlank()) {
                    song.copy(thumbnailUrl = cover)
                } else {
                    song
                }
            }
        // Asked here as well as inside [Downloads.enqueue] — not instead of it.
        // Enqueue is the invariant and has to refuse whoever calls it, including
        // the storage-permission continuation below, which resumes long enough
        // after this check for the connection to have changed under it. This is
        // the one place that knows the tap was for forty tracks and can say so
        // once, rather than leaving forty identical failed rows to be read.
        // A copy out of the song cache spends no data, so Wi-Fi only does not
        // refuse a batch that has one — [Downloads.enqueue] still refuses the
        // tracks in it that would need the network.
        val blocked = songs.isNotEmpty() && !AppSettings.downloadsAllowedNow &&
            songs.none { Downloads.servableFromCache(it.videoId) }
        if (songs.isNotEmpty() && !blocked) {
            val needsStorage = AppSettings.exportDownloads.value && DownloadStore.needsLegacyPermission() &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) != PackageManager.PERMISSION_GRANTED

            // Asked for here rather than at launch because here is where it means
            // something: a download is the first thing this app does that the user
            // is expected to walk away from.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            if (needsStorage) {
                downloadPending = songs
                downloadPendingFrom = from
                storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                songs.forEach { Downloads.enqueue(context, it, from?.title) }
                if (from != null) Downloads.markRequested(from.id, songs.map { it.videoId })
            }
            // Said once per addon and reason per session, and only for a batch
            // that will reach the sources at all — a copy out of the song cache
            // never asks them. Without it the addon was passed over in silence
            // and the file simply arrived from JioSaavn or YouTube.
            if (!songs.all { Downloads.servableFromCache(it.videoId) }) {
                SourceRegistry.addonsLeftOutOfDownloads()
                    .firstOrNull { (config, why) -> explainedDownloadExclusions.add("${config.id}:$why") }
                    ?.let { (config, why) -> onAddonLeftOutOfDownloadChange(config.displayName to why) }
            }
        }
        // Recorded for everything that was asked for, not just what still has to
        // be fetched: a release whose tracks are already on the device is still
        // that release, and the point of the record is to group them. Skipped
        // only when the whole batch was refused, since then there will be
        // nothing on disk for it to group.
        if (from != null && !blocked) {
            Downloads.rememberCollection(from, requested)
            // A collection cover is not part of any audio file. Cache the
            // header image separately so its Downloads card still has artwork
            // with no connection, then atomically replace the remote URL in
            // the persisted collection record.
            if (!from.thumbnailUrl.isNullOrBlank()) {
                scope.launch(Dispatchers.IO) {
                    MediaTagger.cacheArtwork(context.applicationContext, from.thumbnailUrl)
                        ?.let { Downloads.rememberCollectionArtwork(from.id, it) }
                }
            }
        }
        when {
            // The row's own icon reports a queued download, so a single tap
            // normally needs no toast — but a refused one leaves the row exactly
            // as it was, and a button that visibly does nothing is worse than a
            // long message. So this one is said whatever the count.
            blocked -> Toast.makeText(
                context,
                context.getString(R.string.wifi_only_download_refusal),
                Toast.LENGTH_LONG,
            ).show()
            requested.size > 1 -> {
                val message = if (songs.isEmpty()) {
                    context.getString(R.string.already_downloaded)
                } else {
                    context.resources.getQuantityString(
                        R.plurals.downloading_song_count,
                        songs.size,
                        songs.size,
                    )
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    // The Cached songs folder's download buttons. The song cache already holds
    // these tracks, so they are saved from its copy — no data spent, no
    // connection needed — rather than fetched afresh at the Download quality
    // setting. See [Downloads.preferCachedCopy]. The folder's own second line
    // ("YouTube · 4.2 MB") is the cache's, not the file's, and stays behind.
    val downloadFromCache: (List<Song>) -> Unit = { songs ->
        val cleaned = songs.map { it.copy(downloadFormat = null, localDateAddedSeconds = null) }
        Downloads.preferCachedCopy(cleaned.map { it.videoId })
        startDownload(cleaned, null)
    }
    val removedFromCacheMessage = stringResource(R.string.removed_from_cache)
    // Takes tracks out of the song cache — the folder's selection, or one
    // song's menu. The open folder re-reads itself off [AudioCache.contentsChanged].
    val removeFromCache: (List<Song>) -> Unit = { songs ->
        scope.launch {
            val removed = songs.sumOf { AudioCache.remove(it.videoId) }
            if (removed > 0) showQueueNotice(removedFromCacheMessage)
        }
    }
    /**
     * One track on its own, which is never a release.
     *
     * A track reached through AutoPlay or a radio queue often has no duration
     * string at all — YouTube's watch-queue rows don't always send
     * `lengthText` — while the player itself knows exactly how long the same
     * track runs once it has loaded. Backfilled from there when it's the song
     * on screen, so downloading it isn't handed a duration of zero and
     * silently skipped for lyrics — see [LyricsTag.forTrack].
     */
    val downloadSong: (Song) -> Unit = { song ->
        val withDuration = if (song.durationMillis() <= 0L &&
            player.song?.videoId == song.videoId && player.durationMs > 0L
        ) {
            song.copy(durationText = formatDurationText(player.durationMs))
        } else {
            song
        }
        startDownload(listOf(withDuration), null)
    }

    val appActions = BitChordAppActions(
        play = play,
        playFrom = { songs, index, source, bounded, continuationToken ->
            playFrom(songs, index, source, bounded, continuationToken)
        },
        playRadio = playRadio,
        playRecognizedSong = playRecognizedSong,
        playRecognitionHistoryEntry = playRecognitionHistoryEntry,
        startRadio = startRadio,
        openTopPick = openTopPick,
        playMood = playMood,
        addToQueue = addToQueue,
        playNext = playNext,
        onSongSwipe = onSongSwipe,
        openByName = { browseId, name, subtitle, type, artwork ->
            openByName(browseId, name, subtitle, type, artwork)
        },
        queueSongs = queueSongs,
        addSongsToQueue = addSongsToQueue,
        playSongsNext = playSongsNext,
        onBrowseLongPress = onBrowseLongPress,
        onShelfLongPress = onShelfLongPress,
        withBrowseSongs = withBrowseSongs,
        onSearchSourceChange = onSearchSourceChange,
        onLibraryItemClick = onLibraryItemClick,
        startDownload = startDownload,
        downloadFromCache = downloadFromCache,
        removeFromCache = removeFromCache,
        downloadSong = downloadSong,
        openSongMenu = openSongMenu,
        openBrowseMenu = openBrowseMenu,
        folderSortOf = folderSortOf,
        folderViewTypeOf = folderViewTypeOf,
        likeStatusOf = likeStatusOf,
        toggleLikeOf = toggleLikeOf,
        downloadIdFor = downloadIdFor,
        showQueueNotice = showQueueNotice,
        showHostOnlyNotice = showHostOnlyNotice,
        refusedByHost = refusedByHost,
        togglePlayPause = togglePlayPause,
        uploadToWebDav = { uploadToWebDav(it) },
        dismissPlayer = dismissPlayer,
        switchToMusicOnly = { song, pause ->
            scope.launch { switchToMusicOnly(song, pause) }
        },
        switchToVideo = { song, pause ->
            scope.launch { switchToVideo(song, pause) }
        },
        onSelectTab = onSelectTab,
        onRecognitionOpenChange = onRecognitionOpenChange,
        onResetRecognition = {
            onRecognitionOpenChange(false)
            MusicRecognitionService.reset()
        },
        onShowRecognitionHistoryClearConfirmationChange = onShowRecognitionHistoryClearConfirmationChange,
        onShowRadioHistoryClearConfirmationChange = onShowRadioHistoryClearConfirmationChange,
        onRadioSubpageBackChange = onRadioSubpageBackChange,
        onReplayStoryChange = onReplayStoryChange,
        onShowReplayShareChange = onShowReplayShareChange,
        onReplaySharePageChange = onReplaySharePageChange,
        onReplayLandingPageChange = onReplayLandingPageChange,
        onShowSpotifyCanvasAuthChange = onShowSpotifyCanvasAuthChange,
        onEditingSourceChange = onEditingSourceChange,
        onChoosingSourceQualityChange = onChoosingSourceQualityChange,
        onExplainingOutdatedAddonChange = onExplainingOutdatedAddonChange,
        onConfirmJioSaavnChange = onConfirmJioSaavnChange,
        onConfirmBitPerfectChange = onConfirmBitPerfectChange,
        onConfirmFixedVolumeChange = onConfirmFixedVolumeChange,
        onBlockedAddonSongChange = onBlockedAddonSongChange,
        onAddonLeftOutOfDownloadChange = onAddonLeftOutOfDownloadChange,
        onEditingPartyServerChange = onEditingPartyServerChange,
        onEditingSignatureApiChange = onEditingSignatureApiChange,
        onShowLyricsSourcesChange = onShowLyricsSourcesChange,
        onShowAppLanguageChange = onShowAppLanguageChange,
        onShowTranslationLanguageChange = onShowTranslationLanguageChange,
        onShowAccountSelectorChange = onShowAccountSelectorChange,
        onShowListenBrainzLoginChange = onShowListenBrainzLoginChange,
        onShowLastfmLoginChange = onShowLastfmLoginChange,
        onShowDiscordLoginChange = onShowDiscordLoginChange,
        onShowSpotifyLoginChange = onShowSpotifyLoginChange,
        onShowWebDavEditorChange = onShowWebDavEditorChange,
        onShowSmbEditorChange = onShowSmbEditorChange,
        onDiscordDialogChange = onDiscordDialogChange,
        onShowLyricsOffsetChange = onShowLyricsOffsetChange,
        onStationActionsChange = onStationActionsChange,
        onSongActionsChange = onSongActionsChange,
        onSongMenuOriginChange = onSongMenuOriginChange,
        onSongMenuAnchorChange = onSongMenuAnchorChange,
        onMenuFromPlayerChange = onMenuFromPlayerChange,
        onMenuFromHistoryChange = onMenuFromHistoryChange,
        onBrowseActionsChange = onBrowseActionsChange,
        onBrowseMenuOriginChange = onBrowseMenuOriginChange,
        onBrowseMenuAnchorChange = onBrowseMenuAnchorChange,
        onBrowseRenameInSheetChange = onBrowseRenameInSheetChange,
        onEditPlaylistTargetChange = onEditPlaylistTargetChange,
        onPlaylistTargetChange = onPlaylistTargetChange,
        onCreatingPlaylistChange = onCreatingPlaylistChange,
        onOfflinePlaylistTargetChange = onOfflinePlaylistTargetChange,
        onCreatingOfflinePlaylistChange = onCreatingOfflinePlaylistChange,
        onShowSpotifyImportDialogChange = onShowSpotifyImportDialogChange,
        onWebSessionChange = onWebSessionChange,
        onShowAboutWhatsNewChange = onShowAboutWhatsNewChange,
        onShowUpdateDialogChange = onShowUpdateDialogChange,
        onShowDiwaliChange = onShowDiwaliChange,
        onSongSortMenuOpenChange = onSongSortMenuOpenChange,
        onFolderMenuOpenChange = onFolderMenuOpenChange,
        onDetailSearchingChange = onDetailSearchingChange,
        onShowNowPlayingChange = onShowNowPlayingChange,
        onPlayerLiveCanvasChange = onPlayerLiveCanvasChange,
        onActiveRadioSeedChange = onActiveRadioSeedChanged,
        onReorderTargetChange = onReorderTargetChange,
        onReorderEntriesChange = onReorderEntriesChange,
        onReorderSavingChange = onReorderSavingChange,
        onSearchFocusRequestedChange = onSearchFocusRequestedChange,
        onOptimisticVersionSongChange = onOptimisticVersionSongChange,
        onConvertedFromVideoChange = onConvertedFromVideoChange,
        onConvertedFromAudioChange = onConvertedFromAudioChange,
        onKeepVideoIdChange = onKeepVideoIdChange,
    )


    return appActions
}


@Composable
private fun BitChordAppEffects(
    state: BitChordAppState,
    actions: BitChordAppActions,
    navigator: AppNavigator,
    viewModel: MainViewModel,
    controller: MediaController?,
    versionSwitch: VersionSwitchController,
    context: Context,
    openPlayerRequested: Boolean,
    diwaliAutoShown: Boolean,
    onSetDiwaliAutoShown: (Boolean) -> Unit,
    updateDialogShown: Boolean,
    onSetUpdateDialogShown: (Boolean) -> Unit,
    incomingJamInvite: ParsedJamInvite?,
    syncedLyricsEnabled: Boolean,
    lyricsSources: Set<LyricsSource>,
    lyricsSourceOrder: List<LyricsSource>,
    prioritizeSyllableSync: Boolean,
    linkRequest: LinkRequest?,
    sharedLinkLabel: String,
    searchLabel: String,
    openedFileLabel: String,
    playerShowing: Boolean,
    playerSong: Song?,
    onSetLinks: (Song?) -> Unit,
    onSetLinksLoading: (Boolean) -> Unit,
    onClearQueueNotice: () -> Unit,
    appForeground: Boolean,
    savedDownloads: Map<String, String>,
    localMusicFolderUri: String?,
    filterNonMusicAudio: Boolean,
    smbHost: String?,
    livePageKeys: List<String>,
    onActiveJamInviteCodeChange: (String?) -> Unit,
    onActiveJamInviteServerChange: (String?) -> Unit,
    onRecognitionFabVisibleChange: (Boolean) -> Unit,
) {
    LaunchedEffect(state.nav.selectedTab) {
        if (state.nav.selectedTab == TAB_EXPLORE) viewModel.onExploreShown()
    }
    LaunchedEffect(appForeground) {
        if (appForeground) viewModel.refreshTopPicksIfStale()
    }
    LaunchedEffect(openPlayerRequested) {
        if (openPlayerRequested) {
            actions.onShowNowPlayingChange(true)
            PlayerDeepLink.handled()
        }
    }
    LaunchedEffect(state.nav.diwaliFestive, state.sheets.onboardingComplete) {
        if (state.nav.diwaliFestive && state.sheets.onboardingComplete && !diwaliAutoShown) {
            onSetDiwaliAutoShown(true)
            if (!AppSettings.diwaliGreetingShown.value) actions.onShowDiwaliChange(true)
        }
    }
    LaunchedEffect(state.nav.detail?.browseId) {
        if (state.nav.reorderTarget?.browseId != state.nav.detail?.browseId) actions.onReorderTargetChange(null)
    }
    LaunchedEffect(state.nav.updateNotice) {
        if (state.nav.updateNotice != null && !updateDialogShown) {
            onSetUpdateDialogShown(true)
            actions.onShowUpdateDialogChange(true)
        }
    }
    LaunchedEffect(incomingJamInvite) {
        val invite = incomingJamInvite ?: return@LaunchedEffect
        onActiveJamInviteCodeChange(invite.code)
        onActiveJamInviteServerChange(invite.serverUrl)
        actions.dismissPlayer()
        actions.onReplayStoryChange(null)
        actions.onShowReplayShareChange(false)
        actions.onWebSessionChange(null)
        navigator.reset(AppRoute.Settings, AppRoute.ListenTogether)
        JamInviteLink.handled()
    }
    LaunchedEffect(state.nav.signedIn, state.nav.activeJamInviteCode) {
        if (state.nav.signedIn && state.nav.activeJamInviteCode != null) {
            navigator.pushAll(AppRoute.Settings, AppRoute.ListenTogether)
        }
    }
    LaunchedEffect(state.nav.detail?.browseId, state.nav.signedIn) {
        if (state.nav.signedIn) viewModel.resolvePlaylistOwnership(state.nav.detail?.browseId)
    }
    LaunchedEffect(savedDownloads, state.nav.savedCollections, state.nav.topDetail?.browseId) {
        val openPage = state.nav.topDetail ?: return@LaunchedEffect
        val open = openPage.browseId
        val folder = LocalGroups.folderOf(open) ?: open
        if (openPage.songs !is UiState.Loading &&
            (folder == "local:downloads" || folder == CACHE_FOLDER_BROWSE_ID || Downloads.recordIdOf(open) != null ||
                com.music.bitchord.data.spotify.LocalPlaylistStore.getPlaylist(open) != null)
        ) {
            viewModel.reloadLocalDetail(open)
        }
    }
    LaunchedEffect(state.nav.topDetail?.browseId, state.player.offlinePlaylists) {
        val openPage = state.nav.topDetail ?: return@LaunchedEffect
        if (openPage.songs !is UiState.Loading && OfflinePlaylists.isOfflinePlaylist(openPage.browseId)) {
            viewModel.reloadLocalDetail(openPage.browseId)
        }
    }
    LaunchedEffect(localMusicFolderUri, filterNonMusicAudio) {
        val open = state.nav.topDetail?.browseId
        if (open == "local:all" || LocalGroups.folderOf(open) == "local:all") {
            viewModel.reloadLocalDetail(open!!)
        }
    }
    LaunchedEffect(state.nav.webdavUrl) {
        if (state.nav.topDetail?.browseId == com.music.bitchord.data.webdav.WebDavConfig.BROWSE_ID) {
            viewModel.reloadLocalDetail(com.music.bitchord.data.webdav.WebDavConfig.BROWSE_ID)
        }
    }
    LaunchedEffect(smbHost) {
        if (state.nav.topDetail?.browseId == com.music.bitchord.data.smb.SmbConfig.BROWSE_ID) {
            viewModel.reloadLocalDetail(com.music.bitchord.data.smb.SmbConfig.BROWSE_ID)
        }
    }
    LaunchedEffect(state.player.queueNotice?.id) {
        val shown = state.player.queueNotice ?: return@LaunchedEffect
        delay(3_000)
        if (state.player.queueNotice?.id == shown.id) onClearQueueNotice()
    }
    LaunchedEffect(
        state.player.player.song?.videoId, state.player.player.song?.radioName, state.player.player.song?.playbackSourceId,
        state.player.player.song?.isLiveRadioStream,
        state.player.player.durationMs, syncedLyricsEnabled, lyricsSources,
        lyricsSourceOrder, prioritizeSyllableSync,
    ) {
        state.player.player.song?.takeUnless { it.isLiveRadioStream || it.playbackSourceId?.startsWith("radio:") == true }?.let {
            viewModel.loadLyrics(
                it.videoId,
                it.title,
                it.artist,
                state.player.player.durationMs.takeIf { ms -> ms > 0L } ?: it.durationMillis(),
                it.albumName,
                it.localUri,
            )
        } ?: if (state.player.player.song?.isLiveRadioStream == true || state.player.player.song?.playbackSourceId?.startsWith("radio:") == true) {
            viewModel.clearLyrics()
        } else Unit
    }
    LaunchedEffect(state.nav.homeListState, state.nav.selectedTab, state.nav.recognitionOpen) {
        if (state.nav.selectedTab != TAB_HOME || state.nav.recognitionOpen) {
            onRecognitionFabVisibleChange(false)
            return@LaunchedEffect
        }
        var previousIndex = state.nav.homeListState.firstVisibleItemIndex
        var previousOffset = state.nav.homeListState.firstVisibleItemScrollOffset
        snapshotFlow { state.nav.homeListState.firstVisibleItemIndex to state.nav.homeListState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val previous = previousIndex * 10_000 + previousOffset
                val current = index * 10_000 + offset
                val delta = current - previous
                if (kotlin.math.abs(delta) > 6) onRecognitionFabVisibleChange(delta < 0)
                if (index == 0 && offset == 0) onRecognitionFabVisibleChange(true)
                previousIndex = index
                previousOffset = offset
            }
    }
    LaunchedEffect(state.nav.currentFeed) {
        if (state.nav.currentFeed == MainViewModel.Feed.HOME) viewModel.onHomeShown()
        if (state.nav.currentFeed == MainViewModel.Feed.LIBRARY) viewModel.onLibraryShown()
    }
    LaunchedEffect(state.nav.detailStack.map { it.browseId }) {
        val open = state.nav.detailStack.mapTo(HashSet()) { it.browseId }
        state.nav.detailListStates.keys.retainAll(open)
        state.nav.localFolderStates.keys.retainAll(open)
    }
    LaunchedEffect(livePageKeys) {
        val gone = state.nav.pageKeysSeen.filter { it !in livePageKeys }
        gone.forEach(state.nav.pageStates::removeState)
        state.nav.pageKeysSeen.removeAll(gone.toSet())
    }
    LaunchedEffect(state.player.player.song?.videoId) {
        versionSwitch.onSongChanged(state.player.player.song?.videoId) {
            if (state.player.activeRadioSeed?.first != state.player.player.song?.videoId) actions.onActiveRadioSeedChange(null)
        }
    }
    LaunchedEffect(linkRequest, controller) {
        val request = linkRequest ?: return@LaunchedEffect
        val session = controller ?: return@LaunchedEffect
        when (request) {
            is LinkRequest.Track -> {
                val song = YtMusicRepository.trackLinks(request.videoId).getOrNull()
                if (song == null) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.couldnt_open_link),
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    actions.playRadio(
                        song,
                        QueueSource(sharedLinkLabel, PlaybackSourceType.SHARED_LINK, song.videoId),
                    )
                }
            }
            is LinkRequest.Page -> {
                actions.dismissPlayer()
                viewModel.openDetail(request.browseId, title = "")
            }
            is LinkRequest.Search -> {
                val songs = if (!request.play) null else {
                    YtMusicRepository.search(request.query, SearchFilter.SONGS).getOrNull()
                        ?.filterIsInstance<SearchResult.Track>()
                }
                val top = songs?.firstOrNull()?.song
                if (top != null) {
                    actions.playRadio(top, QueueSource(searchLabel, PlaybackSourceType.SEARCH))
                } else {
                    actions.dismissPlayer()
                    navigator.clear()
                    actions.onSelectTab(TAB_SEARCH)
                    viewModel.searchFor(request.query)
                }
            }
            LinkRequest.Resume -> if (session.mediaItemCount > 0) session.play()
            is LinkRequest.LocalFile -> {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        request.uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                val song = LocalMediaRepository.songFromUri(context, request.uri)
                actions.playFrom(
                    listOf(song),
                    0,
                    QueueSource(openedFileLabel, PlaybackSourceType.SHARED_LINK, song.localUri),
                    false,
                    null,
                )
                actions.onShowNowPlayingChange(true)
            }
        }
        MusicLink.handled()
    }
    LaunchedEffect(state.player.player.song?.videoId, playerShowing) {
        onSetLinks(null)
        onSetLinksLoading(false)
        if (!playerShowing) return@LaunchedEffect
        val current = state.player.player.song ?: return@LaunchedEffect
        if (current.isLiveRadioStream || current.playbackSourceId?.startsWith("radio:") == true) return@LaunchedEffect
        if (current.albumId != null && current.artistId != null) return@LaunchedEffect
        onSetLinksLoading(true)
        onSetLinks(YtMusicRepository.trackLinks(current.videoId).getOrNull())
        onSetLinksLoading(false)
    }
    LaunchedEffect(playerSong) {
        if (playerSong != null && state.sheets.songActions?.videoId == playerSong.videoId) {
            actions.onSongActionsChange(playerSong)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BitChordAppContent(
    state: BitChordAppState,
    actions: BitChordAppActions,
    windowWidth: Dp,
    windowHeight: Dp,
    appBackdrop: LayerBackdrop,
    hazeState: HazeState,
    navBarScroll: FloatingTabBarScrollConnection,
    playerDock: PlayerDock,
    playerSheetMotion: PlayerSheetMotion,
    navigator: AppNavigator,
    viewModel: MainViewModel,
    controller: MediaController?,
) {
    val nav = state.nav
    val playerState = state.player
    val sheets = state.sheets
    val addSongsOpen by AddSongsRequest.open.collectAsStateWithLifecycle()

    val canGoBack = !addSongsOpen && (navigator.entries.isNotEmpty() || nav.detailStack.isNotEmpty() ||
        nav.selectedMoodGenre != null || nav.selectedTab != TAB_HOME)
    val goBack: () -> Unit = {
        if (!navigator.pop() && !viewModel.closeMoodGenre()) actions.onSelectTab(TAB_HOME)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawWithContent {
                if (!playerState.coveredByPlayer) drawContent()
            },
    ) {
        BitChordBackHandlers(
            canGoBack = canGoBack,
            onGoBack = goBack,
            addSongsOpen = addSongsOpen,
            onDismissAddSongs = { AddSongsRequest.hide() },
            recognitionOpen = sheets.recognitionOpen,
            onResetRecognition = actions.onResetRecognition,
            replayDrawable = sheets.replay.summary?.isEmpty == false,
            replayStory = sheets.replayStory,
            showReplayShare = sheets.showReplayShare,
            onDismissReplayStory = { actions.onReplayStoryChange(null) },
            onDismissReplayShare = { actions.onShowReplayShareChange(false) },
            showUpdateDialog = sheets.showUpdateDialog,
            onDismissUpdateDialog = { actions.onShowUpdateDialogChange(false) },
            showListenBrainzLogin = sheets.showListenBrainzLogin,
            onDismissListenBrainzLogin = { actions.onShowListenBrainzLoginChange(false) },
            showLastfmLogin = sheets.showLastfmLogin,
            onDismissLastfmLogin = { actions.onShowLastfmLoginChange(false) },
            discordDialog = sheets.discordDialog,
            onDismissDiscordDialog = { actions.onDiscordDialogChange(null) },
            editingSource = sheets.editingSource,
            onDismissEditingSource = { actions.onEditingSourceChange(null) },
            choosingSourceQuality = sheets.choosingSourceQuality,
            onDismissChoosingSourceQuality = { actions.onChoosingSourceQualityChange(null) },
            explainingOutdatedAddon = sheets.explainingOutdatedAddon,
            onDismissExplainingOutdatedAddon = { actions.onExplainingOutdatedAddonChange(null) },
            editingPartyServer = sheets.editingPartyServer,
            onDismissEditingPartyServer = { actions.onEditingPartyServerChange(false) },
            editingSignatureApi = sheets.editingSignatureApi,
            onDismissEditingSignatureApi = { actions.onEditingSignatureApiChange(false) },
        )

        Row(
            Modifier
                .fillMaxSize()
                .snapshotBackdropSource(sheets.pageBackdrop, floor = MaterialTheme.colorScheme.background),
        ) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                AppNavigationHost(
                    targetState = when {
                        nav.visibleEntry != null -> AppDestination.Route(nav.visibleEntry)
                        nav.detail != null -> AppDestination.Release(nav.detail.browseId)
                        else -> AppDestination.Tab(nav.selectedTab)
                    },
                    navigator = navigator,
                    viewModel = viewModel,
                    hazeState = hazeState,
                    glassActive = nav.glassActive,
                    glassSamplesBackdrop = nav.glassSamplesBackdrop,
                    appBackdrop = appBackdrop,
                    navBarScroll = navBarScroll,
                    detailListStates = nav.detailListStates,
                    localFolderStates = nav.localFolderStates,
                    pageStates = nav.pageStates,
                    pageKeysSeen = nav.pageKeysSeen,
                    radioHistoryStore = nav.radioHistoryStore,
                    radioFavoritesStore = nav.radioFavoritesStore,
                    radioFavoriteScope = nav.radioFavoriteScope,
                    states = rememberAppNavigationScreenStates(
                        selectedTab = nav.selectedTab,
                        selectedMoodGenre = nav.selectedMoodGenre,
                        visibleEntry = nav.visibleEntry,
                        detail = nav.detail,
                        detailStack = nav.detailStack,
                        signedIn = nav.signedIn,
                        account = nav.account,
                        selectedChannelName = nav.selectedChannelName,
                        player = nav.player,
                        listPadding = nav.listPadding,
                        artworkMotion = nav.artworkMotion,
                        windowWidth = windowWidth,
                        radioSubpageBack = nav.radioSubpageBack,
                        recognitionOpen = nav.recognitionOpen,
                        showDiwali = nav.showDiwali,
                        diwaliFestive = nav.diwaliFestive,
                        searchSource = nav.searchSource,
                        searchFocusRequested = nav.searchFocusRequested,
                        searchScrollReset = nav.searchScrollReset,
                        query = nav.query,
                        filter = nav.filter,
                        results = nav.results,
                        searchLoadingMore = nav.searchLoadingMore,
                        searchHistory = nav.searchHistory,
                        searchSuggestions = nav.searchSuggestions,
                        libraryResults = nav.libraryResults,
                        addonResults = nav.addonResults,
                        addonCeiling = nav.addonCeiling,
                        releaseLibrary = nav.releaseLibrary,
                        songSort = nav.songSort,
                        savedCollections = nav.savedCollections,
                        downloadedReleases = nav.downloadedReleases,
                        offlinePlaylistGroups = nav.offlinePlaylistGroups,
                        localPlaylistItems = nav.localPlaylistItems,
                        webdavUrl = nav.webdavUrl,
                        activeJamInviteCode = nav.activeJamInviteCode,
                        activeJamInviteServer = nav.activeJamInviteServer,
                        homeState = nav.homeState,
                        visibleHomeState = nav.visibleHomeState,
                        homeLoadingMore = nav.homeLoadingMore,
                        homeRecentlyPlayedLoading = nav.homeRecentlyPlayedLoading,
                        hideRecents = nav.hideRecents,
                        topPicks = nav.topPicks,
                        topPicksLoading = nav.topPicksLoading,
                        hideTopPicks = nav.hideTopPicks,
                        hideHomeMoods = nav.hideHomeMoods,
                        exploreState = nav.exploreState,
                        moodGenreShelves = nav.moodGenreShelves,
                        libraryState = nav.libraryState,
                        historyState = nav.historyState,
                        radioHistory = nav.radioHistory,
                        radioFavorites = nav.radioFavorites,
                        recognitionHistory = nav.recognitionHistory,
                        replay = nav.replay,
                        replayCards = nav.replayCards,
                        replayLandingPage = nav.replayLandingPage,
                        detailActiveShelf = nav.detailActiveShelf,
                        detailSearching = nav.detailSearching,
                        isLocalDetail = nav.isLocalDetail,
                        refreshing = nav.refreshing,
                        showRecognitionHistoryClearConfirmation = nav.showRecognitionHistoryClearConfirmation,
                        showRadioHistoryClearConfirmation = nav.showRadioHistoryClearConfirmation,
                        reorderTarget = nav.reorderTarget,
                        reorderEntries = nav.reorderEntries,
                        reorderSaving = nav.reorderSaving,
                        reduceDynamicBlur = nav.reduceDynamicBlur,
                        radioLabel = nav.radioLabel,
                        historyLabel = nav.historyLabel,
                        replayLabel = nav.replayLabel,
                        searchLabel = nav.searchLabel,
                    ),
                    lists = AppNavigationListStates(
                        homeListState = nav.homeListState,
                        exploreListState = nav.exploreListState,
                        libraryListState = nav.libraryListState,
                        searchListState = nav.searchListState,
                        historyListState = nav.historyListState,
                        replayListState = nav.replayListState,
                        moodGenreListState = nav.moodGenreListState,
                        libraryShowAllGridState = nav.libraryShowAllGridState,
                        moodsGridState = nav.moodsGridState,
                        homePull = nav.homePull,
                        explorePull = nav.explorePull,
                        libraryPull = nav.libraryPull,
                        radioPull = nav.radioPull,
                    ),
                    actions = rememberAppNavigationScreenActions(
                        onSelectTab = actions.onSelectTab,
                        onRadioSubpageBackChanged = actions.onRadioSubpageBackChange,
                        onSearchSourceChange = actions.onSearchSourceChange,
                        onSearchFocusHandled = { actions.onSearchFocusRequestedChange(false) },
                        onSongSwipe = actions.onSongSwipe,
                        onOpenSongMenu = actions.openSongMenu,
                        onOpenBrowseMenu = actions.openBrowseMenu,
                        onLibraryItemClick = actions.onLibraryItemClick,
                        onBrowseLongPress = actions.onBrowseLongPress,
                        onShelfLongPress = actions.onShelfLongPress,
                        onPlayRadio = actions.playRadio,
                        onPlayFrom = actions.playFrom,
                        onPlayRecognizedSong = actions.playRecognizedSong,
                        onPlayRecognitionHistoryEntry = actions.playRecognitionHistoryEntry,
                        onStartRadio = actions.startRadio,
                        onOpenTopPick = actions.openTopPick,
                        onPlayMood = actions.playMood,
                        onDownloadFromCache = actions.downloadFromCache,
                        onRemoveFromCache = actions.removeFromCache,
                        onUploadToWebDav = actions.uploadToWebDav,
                        onShowQueueNotice = actions.showQueueNotice,
                        onSetReplayPeriod = { },
                        onOpenReplayStory = { actions.onReplayStoryChange(it) },
                        onShareReplay = { actions.onShowReplayShareChange(true) },
                        onOpenReplayLandingPage = { actions.onReplayLandingPageChange(it) },
                        onStationActions = { actions.onStationActionsChange(it) },
                        onBlockedAddonSong = { actions.onBlockedAddonSongChange(it) },
                        onCreatingPlaylist = { actions.onCreatingPlaylistChange(true) },
                        onCreatingOfflinePlaylist = { actions.onCreatingOfflinePlaylistChange(true) },
                        onShowSpotifyImport = { actions.onShowSpotifyImportDialogChange(true) },
                        onOpenWebSession = { actions.onWebSessionChange(it) },
                        onShowDiwali = { actions.onShowDiwaliChange(true) },
                        onShowUpdateDialog = { actions.onShowUpdateDialogChange(true) },
                        onShowAboutWhatsNew = { actions.onShowAboutWhatsNewChange(true) },
                        onShowLyricsSources = { actions.onShowLyricsSourcesChange(true) },
                        onShowTranslationLanguage = { actions.onShowTranslationLanguageChange(true) },
                        onShowAppLanguage = { actions.onShowAppLanguageChange(true) },
                        onShowListenBrainzLogin = { actions.onShowListenBrainzLoginChange(true) },
                        onShowLastfmLogin = { actions.onShowLastfmLoginChange(true) },
                        onShowDiscordLogin = { actions.onShowDiscordLoginChange(true) },
                        onShowSpotifyLogin = { actions.onShowSpotifyLoginChange(true) },
                        onShowSpotifyCanvasAuth = { actions.onShowSpotifyCanvasAuthChange(true) },
                        onShowWebDavEditor = { actions.onShowWebDavEditorChange(true) },
                        onShowSmbEditor = { actions.onShowSmbEditorChange(true) },
                        onConfirmJioSaavn = { actions.onConfirmJioSaavnChange(true) },
                        onConfirmBitPerfect = { actions.onConfirmBitPerfectChange(it) },
                        onConfirmFixedVolume = { actions.onConfirmFixedVolumeChange(true) },
                        onEditingPartyServer = { actions.onEditingPartyServerChange(true) },
                        onEditingSignatureApi = { actions.onEditingSignatureApiChange(true) },
                        onDiscordDialog = { actions.onDiscordDialogChange(it) },
                        onEditSource = { actions.onEditingSourceChange(it) },
                        onChooseSourceQuality = { actions.onChoosingSourceQualityChange(it) },
                        onExplainOutdatedAddon = { actions.onExplainingOutdatedAddonChange(it) },
                        onShowAccountSelector = { actions.onShowAccountSelectorChange(true) },
                        onClearRecognitionHistoryConfirmation = { actions.onShowRecognitionHistoryClearConfirmationChange(it) },
                        onClearRadioHistoryConfirmation = { actions.onShowRadioHistoryClearConfirmationChange(it) },
                        onDetailSearchingChange = actions.onDetailSearchingChange,
                        onReorderTargetChange = actions.onReorderTargetChange,
                        onReorderSavingChange = actions.onReorderSavingChange,
                        onFolderSortOf = actions.folderSortOf,
                        onFolderViewTypeOf = actions.folderViewTypeOf,
                        onLibraryLinks = { libraryLinks() },
                        onLibraryDeviceItems = { releases -> libraryDeviceItems(releases) + nav.localPlaylistItems },
                        onTopBarHeight = { topBarHeight() },
                        onRefusedByHost = actions.refusedByHost,
                        onOpenByName = actions.openByName,
                    ),
                    modifier = Modifier.fillMaxSize().zIndex(0f),
                )

                BitChordChrome(
                    state = BitChordChromeState(
                        detail = nav.detail,
                        isLocalDetail = nav.isLocalDetail,
                        isReleaseDetail = nav.isReleaseDetail,
                        detailPalette = nav.detailPalette,
                        glassActive = nav.glassActive,
                        progressiveBlur = nav.progressiveBlur,
                        reduceDynamicBlur = nav.reduceDynamicBlur,
                        hazeState = hazeState,
                        detailListStates = nav.detailListStates,
                        homeListState = nav.homeListState,
                        route = nav.route,
                        selectedTab = nav.selectedTab,
                        tabs = nav.tabs,
                        recognitionOpen = nav.recognitionOpen,
                        detailActiveShelf = nav.detailActiveShelf,
                        selectedMoodGenre = nav.selectedMoodGenre,
                        replayScrolled = nav.replayScrolled,
                        detailScrolled = nav.detailScrolled,
                        scrolled = nav.scrolled,
                        currentFeed = nav.currentFeed,
                        refreshing = nav.refreshing,
                        currentPull = nav.currentPull,
                        radioSubpageBack = nav.radioSubpageBack,
                        searchFocusRequested = nav.searchFocusRequested,
                        query = nav.query,
                        searchSource = nav.searchSource,
                        partyServerStatus = nav.partyServerStatus,
                        updateNotice = nav.updateNotice,
                        signedIn = nav.signedIn,
                        recognitionHistory = nav.recognitionHistory,
                        radioHistory = nav.radioHistory,
                        account = nav.account,
                        detailSearching = nav.detailSearching,
                        recognitionFabVisible = nav.recognitionFabVisible,
                        playerSong = playerState.playerSong,
                        isPlaying = playerState.player.isPlaying,
                        playPauseBusy = playerState.playPauseBusy,
                        controlsLocked = playerState.controlsLocked,
                        reducePlayerMotion = playerState.reducePlayerMotion,
                        queueNotice = playerState.queueNotice,
                        navBarScroll = navBarScroll,
                        activeDock = playerState.activeDock,
                        playerSheetMotion = playerSheetMotion,
                        librarySort = nav.librarySort,
                        songSort = nav.songSort,
                        navigator = navigator,
                    ),
                    actions = BitChordChromeActions(
                        onGoBack = goBack,
                        onResetRecognition = actions.onResetRecognition,
                        onQueryChange = { viewModel.onQueryChange(it) },
                        onSubmitSearch = { viewModel.submitSearch() },
                        onSearchFocusHandled = { actions.onSearchFocusRequestedChange(false) },
                        onShowUpdateDialog = { actions.onShowUpdateDialogChange(true) },
                        onPushRoute = { navigator.push(it) },
                        onLoadHistory = { viewModel.loadHistory() },
                        onSetLibrarySort = { AppSettings.librarySort.value = it },
                        onToggleDetailSearching = { actions.onDetailSearchingChange(!nav.detailSearching) },
                        onOpenFolderMenu = { actions.onFolderMenuOpenChange(true) },
                        onOpenSongSortMenu = { actions.onSongSortMenuOpenChange(true) },
                        onOpenBrowseMenu = actions.openBrowseMenu,
                        onDownloadIdFor = actions.downloadIdFor,
                        onShareArtist = { },
                        onShowRecognitionHistoryClearConfirmation = { actions.onShowRecognitionHistoryClearConfirmationChange(true) },
                        onShowRadioHistoryClearConfirmation = { actions.onShowRadioHistoryClearConfirmationChange(true) },
                        onAccountClick = { actions.onShowAccountSelectorChange(true) },
                        onSwipeProfile = { forward -> viewModel.stepProfile(forward) },
                        onRecognitionOpen = { actions.onRecognitionOpenChange(true) },
                        onTabSelected = actions.onSelectTab,
                        onTogglePlayPause = actions.togglePlayPause,
                        onNext = { controller?.seekToNextMediaItem() },
                        onPrevious = { controller?.seekToPrevious() },
                        onExpandPlayer = if (playerState.reducePlayerMotion) {
                            { actions.onShowNowPlayingChange(true) }
                        } else {
                            { playerSheetMotion.open() }
                        },
                        onShowHostOnlyNotice = actions.showHostOnlyNotice,
                    ),
                    modifier = Modifier.fillMaxSize().zIndex(1f),
                )
            }
        }

        val playerRaised = playerState.showNowPlaying && playerState.playerSong != null

        val songMenuState = rememberSongMenuState(
            menuFromPlayer = playerState.menuFromPlayer,
            partyState = playerState.partyState,
            signedIn = playerState.signedIn,
            account = playerState.account,
            switchingAudioVersion = playerState.switchingAudioVersion,
            convertedFromVideo = playerState.convertedFromVideo,
            convertedFromAudio = playerState.convertedFromAudio,
            convertedAudioId = playerState.convertedAudioId,
            convertedVideoId = playerState.convertedVideoId,
            keepVideoId = playerState.keepVideoId,
            optimisticVersionSong = playerState.optimisticVersionSong,
            playlists = playerState.playlists,
            playlistsLoading = playerState.playlistsLoading,
            likeStatuses = playerState.likeStatuses,
            offlinePlaylists = playerState.offlinePlaylists,
            addonCeiling = playerState.addonCeiling,
            detail = playerState.detail,
            versionContext = playerState.versionContext,
        )
        val songMenuActions = SongMenuActions(
            onDismissPlayer = actions.dismissPlayer,
            onDismissActions = { actions.onSongActionsChange(null) },
            onPlaySongsNext = actions.playSongsNext,
            onAddSongsToQueue = actions.addSongsToQueue,
            onStartDownload = actions.startDownload,
            onStartPlaylistPicker = { actions.onPlaylistTargetChange(it) },
            onStartOfflinePlaylistPicker = { actions.onOfflinePlaylistTargetChange(it) },
            onShowQueueNotice = actions.showQueueNotice,
            onPlayRadio = actions.playRadio,
            onSwitchToMusicOnly = actions.switchToMusicOnly,
            onSwitchToVideo = actions.switchToVideo,
            onUpdateOptimisticVersion = actions.onOptimisticVersionSongChange,
            onUpdateConvertedVideo = actions.onConvertedFromVideoChange,
            onUpdateConvertedAudio = actions.onConvertedFromAudioChange,
            onUpdateKeepVideoId = actions.onKeepVideoIdChange,
        )

        BitChordPlayerUi(
            state = rememberMainPlayerSheetState(
                playerRaised = playerRaised,
                playerSong = playerState.playerSong,
                showNowPlaying = playerState.showNowPlaying,
                windowWidth = windowWidth,
                windowHeight = windowHeight,
                reducePlayerMotion = playerState.reducePlayerMotion,
                reduceDynamicBlur = playerState.reduceDynamicBlur,
                playerLiveCanvas = playerState.playerLiveCanvas,
                player = playerState.player,
                playPauseBusy = playerState.playPauseBusy,
                switchingAudioVersion = playerState.switchingAudioVersion,
                optimisticVersionSong = playerState.optimisticVersionSong,
                convertedAudioId = playerState.convertedAudioId,
                convertedVideoId = playerState.convertedVideoId,
                keepVideoId = playerState.keepVideoId,
                activeRadioSeed = playerState.activeRadioSeed,
                partyState = playerState.partyState,
                account = playerState.account,
                songActions = playerState.songActions,
                songMenuOrigin = playerState.songMenuOrigin,
                songMenuAnchor = playerState.songMenuAnchor,
                menuFromPlayer = playerState.menuFromPlayer,
                controlsLocked = playerState.controlsLocked,
                shuffleEnabled = playerState.shuffleEnabled,
                autoplayEnabled = playerState.autoplayEnabled,
                signedIn = playerState.signedIn,
                likeStatuses = playerState.likeStatuses,
                query = playerState.query,
                filter = playerState.filter,
                results = playerState.results,
                addSongsTypeahead = viewModel.typeaheadResults.collectAsStateWithLifecycle().value,
                lyrics = playerState.lyrics,
                lyricsSource = playerState.lyricsSource,
                lyricsProviderStates = playerState.lyricsProviderStates,
                lyricsChecked = playerState.lyricsChecked,
                showLyricsOffset = playerState.showLyricsOffset,
                queueLabel = playerState.queueLabel,
            ),
            actions = rememberMainPlayerSheetActions(
                onDismissRequest = { actions.onShowNowPlayingChange(false) },
                onLiveCanvasChanged = actions.onPlayerLiveCanvasChange,
                onDismissSongActions = { actions.onSongActionsChange(null) },
                onTogglePlayPause = actions.togglePlayPause,
                onBlockedControl = actions.showHostOnlyNotice,
                onOpenSongMenu = actions.openSongMenu,
                onDismissPlayer = actions.dismissPlayer,
                onToggleLike = { song -> actions.toggleLikeOf(song) },
                onLikeStatusOf = actions.likeStatusOf,
                onSearchQueryChange = { viewModel.onQueryChange(it) },
                onSubmitSearch = { viewModel.submitSearch() },
                onFilterChange = { viewModel.onFilterChange(it) },
                onLiveSearch = { },
                onAddToQueue = actions.addToQueue,
                onAddToPlaylists = { playlists, song, onResult -> viewModel.addToPlaylists(playlists, song, onResult) },
                onAddSongToLikedMusic = { song, onResult -> viewModel.addSongToLikedMusic(song, onResult) },
                onShowQueueNotice = actions.showQueueNotice,
                onDismissLyricsOffset = { actions.onShowLyricsOffsetChange(false) },
                onOpenPlaybackSource = { song ->
                    handleOpenPlaybackSource(
                        displayedSong = song,
                        queueLabel = playerState.queueLabel,
                        context = playerState.versionContext,
                        navigator = navigator,
                        viewModel = viewModel,
                        dismissPlayer = actions.dismissPlayer,
                        onSelectTab = actions.onSelectTab,
                        onSetReplayLandingPage = actions.onReplayLandingPageChange,
                    )
                },
            ),
            playerSheetMotion = playerSheetMotion,
            playerDock = playerDock,
            activeDock = playerState.activeDock,
            playerBackdrop = playerState.playerBackdrop,
            livePlayerBackdrop = playerState.livePlayerBackdrop,
            controller = controller,
            viewModel = viewModel,
            navigator = navigator,
            songMenuState = songMenuState,
            songMenuActions = songMenuActions,
        )

        BitChordGlobalSheetsUi(
            state = rememberGlobalSheetsHostState(
                route = nav.route,
                hazeState = hazeState,
                pageBackdrop = sheets.pageBackdrop,
                reduceDynamicBlur = sheets.reduceDynamicBlur,
                playerRaised = playerRaised,
                songActions = sheets.songActions,
                songMenuOrigin = sheets.songMenuOrigin,
                songMenuAnchor = sheets.songMenuAnchor,
                menuFromPlayer = sheets.menuFromPlayer,
                browseActions = sheets.browseActions,
                browseMenuOrigin = sheets.browseMenuOrigin,
                browseMenuAnchor = sheets.browseMenuAnchor,
                browseRenameInSheet = sheets.browseRenameInSheet,
                stationActions = sheets.stationActions,
                replay = sheets.replay,
                replayStory = sheets.replayStory,
                showReplayShare = sheets.showReplayShare,
                replaySharePage = sheets.replaySharePage,
                account = sheets.account,
                playlistTarget = sheets.playlistTarget,
                creatingPlaylist = sheets.creatingPlaylist,
                playlists = sheets.playlists,
                playlistsLoading = sheets.playlistsLoading,
                offlinePlaylistTarget = sheets.offlinePlaylistTarget,
                creatingOfflinePlaylist = sheets.creatingOfflinePlaylist,
                offlinePlaylists = sheets.offlinePlaylists,
                showSpotifyImportDialog = sheets.showSpotifyImportDialog,
                signedIn = sheets.signedIn,
                editPlaylistTarget = sheets.editPlaylistTarget,
                detailStack = sheets.detailStack,
                libraryState = sheets.libraryState,
                likeStatuses = sheets.likeStatuses,
                query = sheets.query,
                results = sheets.results,
                filter = sheets.filter,
                onboardingShown = sheets.onboardingShown,
                webSession = sheets.webSession,
                googleAccounts = sheets.googleAccounts,
                activeAccountId = sheets.activeAccountId,
                activeProfileId = sheets.activeProfileId,
                showDiwali = sheets.showDiwali,
                showUpdateDialog = sheets.showUpdateDialog,
                onboardingComplete = sheets.onboardingComplete,
                updateAvailable = sheets.updateAvailable,
                showLyricsSources = sheets.showLyricsSources,
                songSortMenuOpen = sheets.songSortMenuOpen,
                songSort = sheets.songSort,
                detail = sheets.detail,
                folderMenuOpen = sheets.folderMenuOpen,
                showAccountSelector = sheets.showAccountSelector,
                showAppLanguage = sheets.showAppLanguage,
                showTranslationLanguage = sheets.showTranslationLanguage,
                showListenBrainzLogin = sheets.showListenBrainzLogin,
                listenBrainzToken = sheets.listenBrainzToken,
                showLastfmLogin = sheets.showLastfmLogin,
                showWebDavEditor = sheets.showWebDavEditor,
                showSmbEditor = sheets.showSmbEditor,
                showDiscordLogin = sheets.showDiscordLogin,
                showSpotifyLogin = sheets.showSpotifyLogin,
                showSpotifyCanvasAuth = sheets.showSpotifyCanvasAuth,
                discordDialog = sheets.discordDialog,
                editingPartyServer = sheets.editingPartyServer,
                editingSignatureApi = sheets.editingSignatureApi,
                editingSource = sheets.editingSource,
                choosingSourceQuality = sheets.choosingSourceQuality,
                explainingOutdatedAddon = sheets.explainingOutdatedAddon,
                confirmJioSaavn = sheets.confirmJioSaavn,
                confirmBitPerfect = sheets.confirmBitPerfect,
                confirmFixedVolume = sheets.confirmFixedVolume,
                blockedAddonSong = sheets.blockedAddonSong,
                addonLeftOutOfDownload = sheets.addonLeftOutOfDownload,
                showAboutWhatsNew = sheets.showAboutWhatsNew,
                songMenuState = songMenuState,
            ),
            actions = rememberGlobalSheetsHostActions(
                onDismissSongActions = {
                    actions.onSongActionsChange(null)
                    actions.onMenuFromPlayerChange(false)
                    actions.onMenuFromHistoryChange(false)
                },
                onDismissBrowseActions = {
                    actions.onBrowseActionsChange(null)
                    actions.onBrowseMenuOriginChange(null)
                    actions.onBrowseMenuAnchorChange(null)
                    actions.onBrowseRenameInSheetChange(false)
                },
                onDismissStationActions = { actions.onStationActionsChange(null) },
                onDismissReplayStory = { actions.onReplayStoryChange(null) },
                onShareReplayCard = { actions.onReplaySharePageChange(it) },
                onDismissReplayShare = { actions.onShowReplayShareChange(false) },
                onDismissPlaylistPicker = {
                    actions.onPlaylistTargetChange(null)
                    actions.onCreatingPlaylistChange(false)
                },
                onDismissOfflinePlaylistPicker = {
                    actions.onOfflinePlaylistTargetChange(null)
                    actions.onCreatingOfflinePlaylistChange(false)
                },
                onDismissSpotifyImport = { actions.onShowSpotifyImportDialogChange(false) },
                onDismissEditPlaylist = { actions.onEditPlaylistTargetChange(null) },
                onDismissWebSession = { actions.onWebSessionChange(null) },
                onDismissDiwali = { actions.onShowDiwaliChange(false) },
                onDismissUpdateDialog = { actions.onShowUpdateDialogChange(false) },
                onDismissLyricsSources = { actions.onShowLyricsSourcesChange(false) },
                onDismissSongSortMenu = { actions.onSongSortMenuOpenChange(false) },
                onDismissFolderMenu = { actions.onFolderMenuOpenChange(false) },
                onDismissAccountSelector = { actions.onShowAccountSelectorChange(false) },
                onDismissAppLanguage = { actions.onShowAppLanguageChange(false) },
                onDismissTranslationLanguage = { actions.onShowTranslationLanguageChange(false) },
                onDismissListenBrainzLogin = { actions.onShowListenBrainzLoginChange(false) },
                onDismissLastfmLogin = { actions.onShowLastfmLoginChange(false) },
                onDismissWebDavEditor = { actions.onShowWebDavEditorChange(false) },
                onDismissSmbEditor = { actions.onShowSmbEditorChange(false) },
                onDismissDiscordLogin = { actions.onShowDiscordLoginChange(false) },
                onDismissSpotifyLogin = { actions.onShowSpotifyLoginChange(false) },
                onDismissSpotifyCanvasAuth = { actions.onShowSpotifyCanvasAuthChange(false) },
                onDismissDiscordDialog = { actions.onDiscordDialogChange(null) },
                onDismissPartyServer = { actions.onEditingPartyServerChange(false) },
                onDismissSignatureApi = { actions.onEditingSignatureApiChange(false) },
                onDismissEditingSource = { actions.onEditingSourceChange(null) },
                onDismissChoosingSourceQuality = { actions.onChoosingSourceQualityChange(null) },
                onDismissExplainingOutdatedAddon = { actions.onExplainingOutdatedAddonChange(null) },
                onDismissConfirmJioSaavn = { actions.onConfirmJioSaavnChange(false) },
                onDismissConfirmBitPerfect = { actions.onConfirmBitPerfectChange(null) },
                onDismissConfirmFixedVolume = { actions.onConfirmFixedVolumeChange(false) },
                onDismissBlockedAddonSong = { actions.onBlockedAddonSongChange(null) },
                onDismissAddonLeftOutOfDownload = { actions.onAddonLeftOutOfDownloadChange(null) },
                onDismissAboutWhatsNew = { actions.onShowAboutWhatsNewChange(false) },
                onShowQueueNotice = actions.showQueueNotice,
                onSelectTab = actions.onSelectTab,
                onDismissPlayer = actions.dismissPlayer,
                onPlaySongsNext = actions.playSongsNext,
                onAddSongsToQueue = actions.addSongsToQueue,
                onPlayFrom = { songs, index -> actions.play(songs, index) },
                onStartDownload = actions.startDownload,
                onStartPlaylistPicker = { actions.onPlaylistTargetChange(it) },
                onStartOfflinePlaylistPicker = { actions.onOfflinePlaylistTargetChange(it) },
                onFolderSortOf = actions.folderSortOf,
                onFolderViewTypeOf = actions.folderViewTypeOf,
                onBrowseRenameInSheetChange = actions.onBrowseRenameInSheetChange,
                onEditPlaylistTargetChange = actions.onEditPlaylistTargetChange,
                onWebSessionModeChange = actions.onWebSessionChange,
                onCreatingPlaylistChange = actions.onCreatingPlaylistChange,
                onCreatingOfflinePlaylistChange = actions.onCreatingOfflinePlaylistChange,
                onSongMenuActions = songMenuActions,
            ),
            controller = controller,
            viewModel = viewModel,
            navigator = navigator,
        )
    }
}


/**
 * The addon exclusions already explained this session, as `configId:reason` —
 * see [SourceRegistry.addonsLeftOutOfDownloads]. Process-wide rather than
 * remembered by the composition, so turning the phone doesn't say it again.
 */
internal val explainedDownloadExclusions = java.util.Collections.synchronizedSet(mutableSetOf<String>())

/** UIKit's sheet curve: leaves at once, settles long — a sheet let go of, not pushed. */
internal val OnboardingDismissEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

/**
 * Whether this page id is one of the device folders — `local:downloads`,
 * `local:all` and the rest, the pages with Songs / Artists / Albums tabs.
 *
 * Asked rather than `startsWith("local:")` because that prefix covers unlike
 * pages: a folder, one downloaded playlist, and an album or artist grouped out
 * of a folder — the last two plain track listings that want the same chrome
 * every other release page gets. See [Downloads.PLAYLIST_PREFIX] and
 * [LocalGroups.PREFIX] for why they share a namespace at all.
 */
internal fun String?.isDeviceFolder(): Boolean =
    this != null && startsWith("local:") &&
        !startsWith(Downloads.PLAYLIST_PREFIX) && !startsWith(LocalGroups.PREFIX) &&
        !startsWith(OfflinePlaylists.PREFIX)

/**
 * Whether [videoId] is the track playing, and is known to be playing YouTube's
 * own copy — which decides whether the player menu leads with "Revert to
 * original" or with "Upgrade quality".
 *
 * See [QualityUpgrade.isKnownToBePlayingYouTubesOwn] for why "known" is doing
 * real work here: an unresolved track playing off the disk cache is judged by
 * the origin its cache entry was tagged with, and keeps the revert on offer
 * only when the entry has none.
 */
internal fun playingYouTubesOwn(videoId: String, controller: MediaController?): Boolean {
    val item = controller?.currentMediaItem?.takeIf { it.mediaId == videoId } ?: return false
    val uri = item.localConfiguration?.uri
    return QualityUpgrade.isKnownToBePlayingYouTubesOwn(
        videoId,
        uri?.let(QualityUpgrade::cacheTag),
        uri?.let(AudioCache::originOf),
    )
}

/** `M:SS`/`H:MM:SS`, the same shape [String?.durationMillis] parses back. */
internal fun formatDurationText(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(Locale.ROOT, minutes, seconds)
}

/**
 * How far short of the end a seek is allowed to land.
 *
 * Seeking to the final millisecond is indistinguishable from the track running
 * out, so it starts the next song — which is not what anyone dragging to the end
 * of the bar, or tapping the last line of a lyric, is asking for. A second back
 * from the end plays the outro instead.
 */
internal const val SEEK_END_GUARD_MS = 1_000L

/**
 * How far a detail page scrolls before its title moves up into the bar.
 *
 * Roughly the height of the sleeve and the credit stacked above the Play pair,
 * so the two titles hand over as the header one leaves rather than sitting on
 * screen together. The bar cross-fades over 220ms, which absorbs the difference
 * between that estimate and a particular page's real header.
 */
internal val DETAIL_TITLE_DROP = 320.dp

/**
 * How fast, a second, a mini-player pull has to be moving when the finger
 * lifts to decide open or closed on its own, wherever it got to — see
 * [MiniPlayerPull]. A flick is a whole gesture; only a slow pull is judged by
 * the distance it covered.
 */
internal val PLAYER_PULL_FLING_VELOCITY = 400.dp

/** M3's own scrim strength, which the player's dim stands in for. */
internal const val PLAYER_SCRIM_ALPHA = 0.32f

/**
 * [content] in a recompose scope of its own, so state read inside it
 * recomposes it alone rather than everything it is written in.
 */
/** Longer than the ⋯ menu's exit, so its panel keeps its frost on the way out. */
internal const val SNAPSHOT_RELEASE_DELAY_MS = 400L

/**
 * The player sheet's shape: square, as [RectangleShape] was, but reaching a
 * whole sheet's height above the sheet's top edge.
 *
 * The sheet's Surface clips its content to this shape, and on the way down to
 * the mini player the artwork travels ahead of the sheet carrying it — up
 * above the sheet's top edge, where a plain rectangle cut it off mid-air,
 * leaving only its lower part to arrive in the bar. Nothing else is drawn
 * outside the sheet (its background is transparent and it casts no shadow),
 * so at rest this is the rectangle it replaces.
 */
internal object PlayerSheetShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rectangle(Rect(0f, -size.height, size.width, size.height))
}

internal const val TAB_HOME = 0
internal const val TAB_EXPLORE = 1
internal const val TAB_RADIO = 2
internal const val TAB_LIBRARY = 3
internal const val TAB_SEARCH = 4
