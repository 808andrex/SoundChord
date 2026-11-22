package com.music.bitchord.ui.player

import com.music.bitchord.PLAYER_SCRIM_ALPHA
import com.music.bitchord.PlayerSheetShape
import com.music.bitchord.SEEK_END_GUARD_MS
import com.music.bitchord.SNAPSHOT_RELEASE_DELAY_MS

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.music.bitchord.R
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.lyrics.LyricLine
import com.music.bitchord.data.lyrics.LyricsSource
import com.music.bitchord.data.model.Account
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.model.PlaybackSourceType
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.UserPlaylist
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.playback.PlayerState
import com.music.bitchord.playback.QueueCoordinator
import com.music.bitchord.playback.QueueSource
import com.music.bitchord.playback.loadAutoplayTracks
import com.music.bitchord.playback.setQueueDragActive
import com.music.bitchord.playback.toggleAutoplay
import com.music.bitchord.playback.toggleShuffle
import com.music.bitchord.ui.LyricsProviderState
import com.music.bitchord.ui.MainViewModel
import com.music.bitchord.ui.components.AddSongsDialog
import com.music.bitchord.ui.components.BottomTab
import com.music.bitchord.ui.components.DacVolumeKeys
import com.music.bitchord.ui.components.FLOATING_BAR_MAX_WIDTH
import com.music.bitchord.ui.components.FloatingBottomBar
import com.music.bitchord.ui.components.GlassNavBar
import com.music.bitchord.ui.components.HeldItem
import com.music.bitchord.ui.components.IosMenuPopover
import com.music.bitchord.ui.components.MiniPlayer
import com.music.bitchord.ui.components.QueueActionNotice
import com.music.bitchord.ui.components.QueueActionNoticeHost
import com.music.bitchord.ui.components.backdrop.backdrops.LayerBackdrop
import com.music.bitchord.ui.components.backdrop.backdrops.SnapshotBackdrop
import com.music.bitchord.ui.components.backdrop.backdrops.layerBackdrop
import com.music.bitchord.ui.components.backdrop.backdrops.snapshotBackdropSource
import com.music.bitchord.ui.components.floatingtabbar.FloatingTabBarScrollConnection
import com.music.bitchord.ui.app.SongMenuContent
import com.music.bitchord.ui.app.SongMenuState
import com.music.bitchord.ui.app.SongMenuActions
import com.music.bitchord.ui.navigation.AppNavigator
import com.music.bitchord.ui.navigation.AppRoute
import com.music.bitchord.ui.player.AddSongsRequest
import com.music.bitchord.ui.player.LocalPlayerDock
import com.music.bitchord.ui.player.NowPlayingScreen
import com.music.bitchord.ui.player.PlayerDock
import com.music.bitchord.ui.player.PlayerSheetMotion
import com.music.bitchord.ui.replay.ReplayStoryPage
import com.music.bitchord.ui.utils.guardSheetFromContentTouches
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch



@Immutable
data class BottomPlayerBarState(
    val glassActive: Boolean,
    val queueNotice: QueueActionNotice?,
    val tabs: List<BottomTab>,
    val selectedIndex: Int,
    val song: Song?,
    val isPlaying: Boolean,
    val isLoading: Boolean,
    val controlsLocked: Boolean,
    val reducePlayerMotion: Boolean,
)

@Composable
internal fun rememberBottomPlayerBarState(
    glassActive: Boolean,
    queueNotice: QueueActionNotice?,
    tabs: List<BottomTab>,
    selectedIndex: Int,
    song: Song?,
    isPlaying: Boolean,
    isLoading: Boolean,
    controlsLocked: Boolean,
    reducePlayerMotion: Boolean,
): BottomPlayerBarState = remember(
    glassActive,
    queueNotice,
    tabs,
    selectedIndex,
    song,
    isPlaying,
    isLoading,
    controlsLocked,
    reducePlayerMotion,
) {
    BottomPlayerBarState(
        glassActive = glassActive,
        queueNotice = queueNotice,
        tabs = tabs,
        selectedIndex = selectedIndex,
        song = song,
        isPlaying = isPlaying,
        isLoading = isLoading,
        controlsLocked = controlsLocked,
        reducePlayerMotion = reducePlayerMotion,
    )
}

@Immutable
data class BottomPlayerBarActions(
    val onTabSelected: (Int) -> Unit,
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onExpand: () -> Unit,
    val onBlockedControl: () -> Unit,
    val onHeightChanged: (Int) -> Unit,
)

@Composable
internal fun rememberBottomPlayerBarActions(
    onTabSelected: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit,
    onBlockedControl: () -> Unit,
    onHeightChanged: (Int) -> Unit,
): BottomPlayerBarActions = remember(
    onTabSelected,
    onPlayPause,
    onNext,
    onPrevious,
    onExpand,
    onBlockedControl,
    onHeightChanged,
) {
    BottomPlayerBarActions(
        onTabSelected = onTabSelected,
        onPlayPause = onPlayPause,
        onNext = onNext,
        onPrevious = onPrevious,
        onExpand = onExpand,
        onBlockedControl = onBlockedControl,
        onHeightChanged = onHeightChanged,
    )
}

@Composable
internal fun BottomPlayerBar(
    state: BottomPlayerBarState,
    actions: BottomPlayerBarActions,
    navBarScroll: FloatingTabBarScrollConnection,
    dock: PlayerDock?,
    pull: PlayerSheetMotion?,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    if (state.glassActive) {
        Column(
            modifier = modifier
                .widthIn(max = FLOATING_BAR_MAX_WIDTH)
                .fillMaxWidth()
                .onSizeChanged { actions.onHeightChanged(it.height) },
        ) {
            QueueActionNoticeHost(state.queueNotice)
            GlassNavBar(
                tabs = state.tabs,
                selectedIndex = state.selectedIndex,
                onTabSelected = actions.onTabSelected,
                scrollConnection = navBarScroll,
                song = state.song,
                isPlaying = state.isPlaying,
                isLoading = state.isLoading,
                onPlayPause = actions.onPlayPause,
                onNext = actions.onNext,
                onPrevious = actions.onPrevious,
                onExpand = actions.onExpand,
                controlsLocked = state.controlsLocked,
                onBlockedControl = actions.onBlockedControl,
                dock = dock,
                pull = pull,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Column(
            modifier = modifier
                .widthIn(max = FLOATING_BAR_MAX_WIDTH)
                .fillMaxWidth()
                .onSizeChanged { actions.onHeightChanged(it.height) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            QueueActionNoticeHost(state.queueNotice)
            state.song?.let { song ->
                MiniPlayer(
                    song = song,
                    isPlaying = state.isPlaying,
                    isLoading = state.isLoading,
                    hazeState = hazeState,
                    onPlayPause = actions.onPlayPause,
                    onNext = actions.onNext,
                    onPrevious = actions.onPrevious,
                    onExpand = actions.onExpand,
                    controlsLocked = state.controlsLocked,
                    onBlockedControl = actions.onBlockedControl,
                    dock = dock,
                    pull = pull,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            FloatingBottomBar(
                tabs = state.tabs,
                selectedIndex = state.selectedIndex,
                hazeState = hazeState,
                onTabSelected = actions.onTabSelected,
            )
        }
    }
}

@Immutable
data class MainPlayerSheetState(
    val playerRaised: Boolean,
    val playerSong: Song?,
    val showNowPlaying: Boolean,
    val windowWidth: Dp,
    val windowHeight: Dp,
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
    val addSongsTypeahead: List<SearchResult>,
    val lyrics: List<LyricLine>?,
    val lyricsSource: LyricsSource?,
    val lyricsProviderStates: Map<LyricsSource, LyricsProviderState>,
    val lyricsChecked: Boolean,
    val showLyricsOffset: Boolean,
    val queueLabel: String,
)

@Composable
internal fun rememberMainPlayerSheetState(
    playerRaised: Boolean,
    playerSong: Song?,
    showNowPlaying: Boolean,
    windowWidth: Dp,
    windowHeight: Dp,
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
    activeRadioSeed: Pair<String, String>?,
    partyState: ListenTogether.State,
    account: Account?,
    songActions: Song?,
    songMenuOrigin: HeldItem?,
    songMenuAnchor: Rect?,
    menuFromPlayer: Boolean,
    controlsLocked: Boolean,
    shuffleEnabled: Boolean,
    autoplayEnabled: Boolean,
    signedIn: Boolean,
    likeStatuses: Map<String, LikeStatus>,
    query: String,
    filter: SearchFilter,
    results: UiState<List<SearchResult>>?,
    addSongsTypeahead: List<SearchResult>,
    lyrics: List<LyricLine>?,
    lyricsSource: LyricsSource?,
    lyricsProviderStates: Map<LyricsSource, LyricsProviderState>,
    lyricsChecked: Boolean,
    showLyricsOffset: Boolean,
    queueLabel: String,
): MainPlayerSheetState = MainPlayerSheetState(
    playerRaised = playerRaised,
    playerSong = playerSong,
    showNowPlaying = showNowPlaying,
    windowWidth = windowWidth,
    windowHeight = windowHeight,
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
    addSongsTypeahead = addSongsTypeahead,
    lyrics = lyrics,
    lyricsSource = lyricsSource,
    lyricsProviderStates = lyricsProviderStates,
    lyricsChecked = lyricsChecked,
    showLyricsOffset = showLyricsOffset,
    queueLabel = queueLabel,
)

@Immutable
data class MainPlayerSheetActions(
    val onDismissRequest: () -> Unit,
    val onLiveCanvasChanged: (Boolean) -> Unit,
    val onDismissSongActions: () -> Unit,
    val onTogglePlayPause: () -> Unit,
    val onBlockedControl: () -> Unit,
    val onOpenSongMenu: (Song) -> Unit,
    val onDismissPlayer: () -> Unit,
    val onToggleLike: (Song) -> Unit,
    val onLikeStatusOf: (Song) -> LikeStatus,
    val onSearchQueryChange: (String) -> Unit,
    val onSubmitSearch: () -> Unit,
    val onFilterChange: (SearchFilter) -> Unit,
    val onLiveSearch: () -> Unit,
    val onAddToQueue: (Song) -> Unit,
    val onAddToPlaylists: (List<UserPlaylist>, Song, (Int, Int, Int) -> Unit) -> Unit,
    val onAddSongToLikedMusic: (Song, (Boolean) -> Unit) -> Unit,
    val onShowQueueNotice: (String) -> Unit,
    val onDismissLyricsOffset: () -> Unit,
    val onOpenPlaybackSource: (Song) -> Unit,
)

@Composable
internal fun rememberMainPlayerSheetActions(
    onDismissRequest: () -> Unit,
    onLiveCanvasChanged: (Boolean) -> Unit,
    onDismissSongActions: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onBlockedControl: () -> Unit,
    onOpenSongMenu: (Song) -> Unit,
    onDismissPlayer: () -> Unit,
    onToggleLike: (Song) -> Unit,
    onLikeStatusOf: (Song) -> LikeStatus,
    onSearchQueryChange: (String) -> Unit,
    onSubmitSearch: () -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onLiveSearch: () -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylists: (List<UserPlaylist>, Song, (Int, Int, Int) -> Unit) -> Unit,
    onAddSongToLikedMusic: (Song, (Boolean) -> Unit) -> Unit,
    onShowQueueNotice: (String) -> Unit,
    onDismissLyricsOffset: () -> Unit,
    onOpenPlaybackSource: (Song) -> Unit,
): MainPlayerSheetActions = MainPlayerSheetActions(
    onDismissRequest = onDismissRequest,
    onLiveCanvasChanged = onLiveCanvasChanged,
    onDismissSongActions = onDismissSongActions,
    onTogglePlayPause = onTogglePlayPause,
    onBlockedControl = onBlockedControl,
    onOpenSongMenu = onOpenSongMenu,
    onDismissPlayer = onDismissPlayer,
    onToggleLike = onToggleLike,
    onLikeStatusOf = onLikeStatusOf,
    onSearchQueryChange = onSearchQueryChange,
    onSubmitSearch = onSubmitSearch,
    onFilterChange = onFilterChange,
    onLiveSearch = onLiveSearch,
    onAddToQueue = onAddToQueue,
    onAddToPlaylists = onAddToPlaylists,
    onAddSongToLikedMusic = onAddSongToLikedMusic,
    onShowQueueNotice = onShowQueueNotice,
    onDismissLyricsOffset = onDismissLyricsOffset,
    onOpenPlaybackSource = onOpenPlaybackSource,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainPlayerSheet(
    state: MainPlayerSheetState,
    actions: MainPlayerSheetActions,
    playerSheetMotion: PlayerSheetMotion,
    playerDock: PlayerDock,
    activeDock: PlayerDock?,
    playerBackdrop: SnapshotBackdrop,
    livePlayerBackdrop: LayerBackdrop,
    controller: MediaController?,
    viewModel: MainViewModel,
    navigator: AppNavigator,
    songMenuContent: @Composable (Song) -> Unit,
) {
    if (!state.playerRaised || state.playerSong == null) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetDensity = LocalDensity.current

    val confirmSheetValue: (SheetValue) -> Boolean = { value ->
        if (value == SheetValue.Hidden && !state.reducePlayerMotion) {
            playerSheetMotion.close()
            false
        } else {
            true
        }
    }
    val nowPlayingSheetState = rememberSaveable(
        saver = SheetState.Saver(
            skipPartiallyExpanded = true,
            confirmValueChange = confirmSheetValue,
            density = sheetDensity,
            skipHiddenState = false,
        ),
    ) {
        SheetState(
            skipPartiallyExpanded = true,
            density = sheetDensity,
            initialValue = if (playerSheetMotion.holding) SheetValue.Expanded else SheetValue.Hidden,
            confirmValueChange = confirmSheetValue,
        )
    }

    DisposableEffect(nowPlayingSheetState) {
        playerSheetMotion.sheet = nowPlayingSheetState
        playerDock.sheetOffset = playerSheetMotion::position
        onDispose {
            playerDock.sheetOffset = null
            playerSheetMotion.onSheetGone()
        }
    }

    ModalBottomSheet(
        onDismissRequest = actions.onDismissRequest,
        sheetState = nowPlayingSheetState,
        scrimColor = if (state.reducePlayerMotion) BottomSheetDefaults.ScrimColor else Color.Transparent,
        shape = PlayerSheetShape,
        containerColor = Color.Transparent,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        sheetMaxWidth = Dp.Unspecified,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = state.reducePlayerMotion),
    ) {
        BackHandler(enabled = !state.reducePlayerMotion) { playerSheetMotion.close() }
        val sheetWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        DacVolumeKeys()
        DisposableEffect(sheetWindow, state.reducePlayerMotion) {
            if (!state.reducePlayerMotion) playerSheetMotion.attachWindow(sheetWindow)
            onDispose { }
        }

        Box(
            Modifier
                .offset { IntOffset(0, playerSheetMotion.contentOffsetPx()) }
                .drawBehind {
                    val open = playerDock.openFraction()
                    if (!state.reducePlayerMotion && open > 0f && !(open >= 1f && playerDock.attached)) {
                        val top = playerSheetMotion.position().takeUnless { it.isNaN() } ?: 0f
                        drawRect(
                            color = Color.Black.copy(alpha = PLAYER_SCRIM_ALPHA * open),
                            topLeft = Offset(0f, -top.toInt().toFloat()),
                            size = Size(size.width, size.height),
                        )
                    }
                }
                .guardSheetFromContentTouches(nowPlayingSheetState)
                .onSizeChanged {
                    playerDock.sheetTravel = it.height.toFloat()
                    playerSheetMotion.laidOut = true
                },
        ) {
            val addSongsRequested by AddSongsRequest.open.collectAsStateWithLifecycle()
            val addSongsPlaylist by AddSongsRequest.playlist.collectAsStateWithLifecycle()
            val addSongsToLikedMusic by AddSongsRequest.likedMusic.collectAsStateWithLifecycle()
            val addSongsOpen = addSongsRequested && addSongsPlaylist == null && !addSongsToLikedMusic
            var addSongsSuggestions by remember { mutableStateOf<List<Song>?>(null) }
            var addSongsPreviousFilter by remember { mutableStateOf<SearchFilter?>(null) }

            DisposableEffect(Unit) {
                onDispose {
                    if (AddSongsRequest.playlist.value == null && !AddSongsRequest.likedMusic.value) {
                        AddSongsRequest.hide()
                    }
                }
            }

            LaunchedEffect(addSongsOpen) {
                if (!addSongsOpen) return@LaunchedEffect
                if (!state.reduceDynamicBlur && !state.playerLiveCanvas) playerBackdrop.capture()
                actions.onSearchQueryChange("")
                addSongsPreviousFilter = state.filter
                actions.onFilterChange(SearchFilter.ALL)
                addSongsSuggestions = null
                val seed = state.player.song
                addSongsSuggestions = if (seed == null) emptyList() else {
                    loadAutoplayTracks(existing = state.player.queue, seedSong = seed, limit = 8)
                        .getOrElse { emptyList() }
                }
            }

            val playerMenuUp = (state.songActions != null && state.songMenuOrigin == null && state.menuFromPlayer) || addSongsOpen
            LaunchedEffect(playerMenuUp, state.reduceDynamicBlur) {
                if (!playerMenuUp || state.reduceDynamicBlur) {
                    delay(SNAPSHOT_RELEASE_DELAY_MS)
                    playerBackdrop.release()
                }
            }

            Box(
                when {
                    state.reduceDynamicBlur -> Modifier
                    state.playerLiveCanvas -> Modifier.layerBackdrop(livePlayerBackdrop)
                    else -> Modifier.snapshotBackdropSource(playerBackdrop)
                },
            ) {
                CompositionLocalProvider(LocalPlayerDock provides activeDock) {
                    val song = state.playerSong
                    val effectiveSong = state.optimisticVersionSong?.takeIf {
                        it.videoId == state.convertedAudioId || it.videoId == state.convertedVideoId || it.videoId == state.keepVideoId ||
                        it.videoId == YtMusicRepository.cachedAudioVersion(song.videoId)?.videoId ||
                        it.videoId == YtMusicRepository.cachedVideoVersion(song.videoId)?.videoId
                    } ?: song
                    val displayedSong = state.activeRadioSeed
                        ?.takeIf { (videoId, _) -> effectiveSong.radioName == null && videoId == effectiveSong.videoId }
                        ?.let { (videoId, name) ->
                            effectiveSong.copy(
                                radioName = name,
                                playbackSource = name,
                                playbackSourceType = PlaybackSourceType.SHARED_LINK,
                                playbackSourceId = videoId,
                            )
                        }
                        ?: effectiveSong
                    val playedBy = state.partyState
                        .takeIf {
                            it.inParty && it.playback.track?.videoId == displayedSong.videoId
                        }
                        ?.playback
                        ?.let { playback ->
                            playback.startedByName?.takeIf(String::isNotBlank)
                                ?: state.partyState.members.firstOrNull {
                                    it.memberId == playback.startedBy
                                }?.displayName?.takeIf(String::isNotBlank)
                        }

                    NowPlayingScreen(
                        song = displayedSong,
                        radioTrackTitle = state.player.radioTrackTitle,
                        playedBy = playedBy,
                        accountName = state.account?.name,
                        windowWidth = state.windowWidth,
                        windowHeight = state.windowHeight,
                        menuOpen = { state.songActions != null && state.menuFromPlayer && state.songMenuOrigin == null },
                        onLiveCanvasChanged = actions.onLiveCanvasChanged,
                        isPlaying = state.player.isPlaying,
                        isLoading = state.playPauseBusy,
                        position = state.player.position,
                        durationMs = state.player.durationMs,
                        audioVersionSwitching = state.switchingAudioVersion,
                        qualityUpgraded = state.player.isQualityUpgraded,
                        onPlayPause = actions.onTogglePlayPause,
                        onNext = { controller?.seekToNextMediaItem() },
                        onPrevious = { controller?.seekToPrevious() },
                        onBlockedControl = actions.onBlockedControl,
                        onSeekFraction = { fraction ->
                            controller?.let { player ->
                                val duration = player.duration
                                if (duration > 0) {
                                    player.seekTo(
                                        (fraction * duration).toLong()
                                            .coerceIn(0L, (duration - SEEK_END_GUARD_MS).coerceAtLeast(0L)),
                                    )
                                }
                            }
                        },
                        onSeek = { target ->
                            controller?.let { player ->
                                val duration = player.duration
                                player.seekTo(
                                    if (duration > 0) {
                                        target.coerceIn(0L, (duration - SEEK_END_GUARD_MS).coerceAtLeast(0L))
                                    } else {
                                        target.coerceAtLeast(0L)
                                    },
                                )
                            }
                        },
                        queue = state.player.queue,
                        queueIndex = state.player.queueIndex,
                        hasPrevious = state.player.hasPrevious,
                        hasNext = state.player.hasNext,
                        repeatMode = state.player.repeatMode,
                        shuffleEnabled = state.shuffleEnabled,
                        autoplayEnabled = state.autoplayEnabled,
                        signedIn = state.signedIn,
                        likeStatus = actions.onLikeStatusOf(song),
                        onToggleLike = { actions.onToggleLike(song) },
                        onToggleShuffle = { controller?.toggleShuffle() },
                        onCycleRepeat = {
                            controller?.let {
                                val next = when (it.repeatMode) {
                                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                    else -> Player.REPEAT_MODE_OFF
                                }
                                AppSettings.setRepeatMode(next)
                                it.repeatMode = next
                            }
                        },
                        onToggleAutoplay = {
                            controller?.toggleAutoplay()
                        },
                        onJumpTo = { index ->
                            controller?.let { c ->
                                c.seekTo(index, 0L)
                                c.play()
                            }
                        },
                        onRemoveFromQueue = { controller?.removeMediaItem(it) },
                        onMoveInQueue = { from, to -> controller?.moveMediaItem(from, to) },
                        onQueueDragActiveChange = { active -> controller?.setQueueDragActive(active) },
                        onOpenMenu = {
                            actions.onOpenSongMenu(song)
                        },
                        onOpenAlbum = { id ->
                            actions.onDismissPlayer()
                            viewModel.openDetail(
                                id,
                                song.albumName ?: song.title,
                                song.artist,
                                song.thumbnailUrl,
                                BrowseType.ALBUM,
                            )
                        },
                        onOpenArtist = { id, name ->
                            actions.onDismissPlayer()
                            if (id != null) {
                                viewModel.openDetail(
                                    id,
                                    name,
                                    context.getString(R.string.artist),
                                    null,
                                    BrowseType.ARTIST,
                                )
                            } else {
                                scope.launch {
                                    val found = YtMusicRepository.findArtistPageId(name).getOrNull()
                                    if (found != null) {
                                        viewModel.openDetail(
                                            found,
                                            name,
                                            context.getString(R.string.artist),
                                            null,
                                            BrowseType.ARTIST,
                                        )
                                    } else {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.couldnt_find, name),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            }
                        },
                        onOpenPlaybackSource = {
                            actions.onOpenPlaybackSource(displayedSong)
                        },
                        lyrics = state.lyrics,
                        lyricsSource = state.lyricsSource,
                        lyricsProviderStates = state.lyricsProviderStates,
                        onSelectLyricsProvider = viewModel::selectLyricsProvider,
                        lyricsUnavailable = state.lyricsChecked && state.lyrics.isNullOrEmpty(),
                        lyricsOffsetOpen = state.showLyricsOffset,
                        onDismissLyricsOffset = actions.onDismissLyricsOffset,
                        onListenTogether = {
                            actions.onDismissPlayer()
                            navigator.pushAll(AppRoute.Settings, AppRoute.ListenTogether)
                        },
                        onClearQueue = {
                            controller?.let { c ->
                                QueueCoordinator.clearUserQueue(c)
                            }
                        },
                    )
                }
            }

            IosMenuPopover(
                item = state.songActions?.takeIf { state.songMenuOrigin == null && state.menuFromPlayer },
                anchor = state.songMenuAnchor,
                onDismiss = actions.onDismissSongActions,
                backdrop = if (state.playerLiveCanvas) livePlayerBackdrop else playerBackdrop,
                centerOnAnchor = true,
            ) { song ->
                songMenuContent(song)
            }

            AddSongsDialog(
                open = addSongsOpen,
                title = if (addSongsPlaylist != null || addSongsToLikedMusic) {
                    context.getString(R.string.add_songs_to_playlist)
                } else {
                    null
                },
                query = state.query,
                onQueryChange = actions.onSearchQueryChange,
                onSubmit = actions.onSubmitSearch,
                results = state.results,
                typeahead = state.addSongsTypeahead,
                suggestions = addSongsSuggestions,
                filter = state.filter,
                onFilterChange = actions.onFilterChange,
                onLiveSearch = actions.onLiveSearch,
                backdrop = if (state.playerLiveCanvas) livePlayerBackdrop else playerBackdrop,
                onAdd = { song ->
                    val playlist = addSongsPlaylist
                    when {
                        playlist != null ->
                            actions.onAddToPlaylists(listOf<UserPlaylist>(playlist), song) { added, _, failed ->
                                if (added == 0 && failed > 0) {
                                    actions.onShowQueueNotice(context.getString(R.string.failed))
                                }
                            }
                        addSongsToLikedMusic -> actions.onAddSongToLikedMusic(song) { added ->
                            if (added) actions.onShowQueueNotice(context.getString(R.string.song_added_to_liked_music))
                        }
                        else -> actions.onAddToQueue(song)
                    }
                },
                onDismiss = {
                    AddSongsRequest.hide()
                    actions.onSearchQueryChange("")
                    addSongsPreviousFilter?.let(actions.onFilterChange)
                    addSongsPreviousFilter = null
                },
            )
        }
    }
}

@Composable
internal fun BitChordPlayerUi(
    state: MainPlayerSheetState,
    actions: MainPlayerSheetActions,
    playerSheetMotion: PlayerSheetMotion,
    playerDock: PlayerDock,
    activeDock: PlayerDock?,
    playerBackdrop: SnapshotBackdrop,
    livePlayerBackdrop: LayerBackdrop,
    controller: MediaController?,
    viewModel: MainViewModel,
    navigator: AppNavigator,
    songMenuState: SongMenuState,
    songMenuActions: SongMenuActions,
) {
    MainPlayerSheet(
        state = state,
        actions = actions,
        playerSheetMotion = playerSheetMotion,
        playerDock = playerDock,
        activeDock = activeDock,
        playerBackdrop = playerBackdrop,
        livePlayerBackdrop = livePlayerBackdrop,
        controller = controller,
        viewModel = viewModel,
        navigator = navigator,
        songMenuContent = { song ->
            SongMenuContent(song, songMenuState, songMenuActions, controller, viewModel)
        },
    )
}

