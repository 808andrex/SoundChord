package com.music.bitchord.ui.app

import com.music.bitchord.SNAPSHOT_RELEASE_DELAY_MS
import com.music.bitchord.TAB_LIBRARY

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import com.music.bitchord.R
import com.music.bitchord.auth.DiscordLoginScreen
import com.music.bitchord.auth.GoogleAccountSession
import com.music.bitchord.auth.SpotifyLoginScreen
import com.music.bitchord.auth.WebSessionMode
import com.music.bitchord.auth.YtMusicLoginScreen
import com.music.bitchord.data.AppUpdateChecker
import com.music.bitchord.data.LocalGroups
import com.music.bitchord.data.OfflinePlaylist
import com.music.bitchord.data.OfflinePlaylists
import com.music.bitchord.data.TrackLog
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.model.Account
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.DetailPage
import com.music.bitchord.data.model.LibraryPage
import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.model.PlaybackSourceType
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.UserPlaylist
import com.music.bitchord.data.scrobbling.LastFM
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.AudioQuality
import com.music.bitchord.data.settings.BitPerfectMode
import com.music.bitchord.data.settings.LibraryViewType
import com.music.bitchord.data.settings.LocalMusicSort
import com.music.bitchord.data.settings.SongSort
import com.music.bitchord.data.sources.SourceConfig
import com.music.bitchord.data.sources.SourceKind
import com.music.bitchord.data.sources.SourceRegistry
import com.music.bitchord.download.DownloadTarget
import com.music.bitchord.download.Downloads
import com.music.bitchord.playback.AudioCache
import com.music.bitchord.playback.QualityUpgrade
import com.music.bitchord.playback.QueueShuffle
import com.music.bitchord.playback.QueueSource
import com.music.bitchord.playback.smart.VersionAudioAligner
import com.music.bitchord.playback.swapToVersion
import com.music.bitchord.ui.AddonBlockedAlert
import com.music.bitchord.ui.MainViewModel
import com.music.bitchord.ui.components.AccountProfileSelector
import com.music.bitchord.ui.components.AppLanguageDialog
import com.music.bitchord.ui.components.BrowseActionsMenu
import com.music.bitchord.ui.components.BrowseTarget
import com.music.bitchord.ui.components.ChoiceAlert
import com.music.bitchord.ui.components.ConfirmationAlert
import com.music.bitchord.ui.components.DacVolumeHud
import com.music.bitchord.ui.components.FieldConfig
import com.music.bitchord.ui.components.HeldContextMenu
import com.music.bitchord.ui.components.HeldItem
import com.music.bitchord.ui.components.InfoAlert
import com.music.bitchord.ui.components.IosMenuPopover
import com.music.bitchord.ui.components.LastfmLoginAlert
import com.music.bitchord.ui.components.ListenBrainzTokenAlert
import com.music.bitchord.ui.components.LyricsSourcesDialog
import com.music.bitchord.ui.components.PlaylistAddSongsHost
import com.music.bitchord.ui.components.ServerEditorHost
import com.music.bitchord.ui.components.SpotifyImportAlert
import com.music.bitchord.ui.components.StationActionsMenu
import com.music.bitchord.ui.components.StationMenuTarget
import com.music.bitchord.ui.components.TranslationLanguageDialog
import com.music.bitchord.ui.components.UpdateAvailableDialog
import com.music.bitchord.ui.components.WebDavConflictAlert
import com.music.bitchord.ui.components.backdrop.backdrops.SnapshotBackdrop
import com.music.bitchord.ui.components.isGlassSupported
import com.music.bitchord.ui.festive.DiwaliGreetingScreen
import com.music.bitchord.ui.navigation.AppNavigator
import com.music.bitchord.ui.navigation.AppRoute
import com.music.bitchord.ui.onboarding.OnboardingScreen
import com.music.bitchord.ui.player.AddSongsRequest
import com.music.bitchord.ui.replay.ReplayShareSheet
import com.music.bitchord.ui.replay.ReplayState
import com.music.bitchord.ui.replay.ReplayStories
import com.music.bitchord.ui.replay.ReplayStoryPage
import com.music.bitchord.ui.components.EditPlaylistSheet
import com.music.bitchord.ui.components.PlaylistPickerSheet
import com.music.bitchord.ui.components.SongActionsMenu
import com.music.bitchord.ui.player.MixViewRequest
import com.music.bitchord.playback.OriginalVersion
import com.music.bitchord.data.NerdStats
import com.music.bitchord.playback.hasYouTubeOriginal
import com.music.bitchord.playback.revertToOriginal
import com.music.bitchord.playback.upgradeQuality
import com.music.bitchord.shouldSkipAfterDislike
import com.music.bitchord.ui.screens.AboutWhatsNewOverlay
import com.music.bitchord.ui.screens.DiscordDialog
import com.music.bitchord.ui.screens.DiscordDialogHost
import com.music.bitchord.ui.screens.PartyServerEditor
import com.music.bitchord.ui.screens.SignatureApiEditor
import com.music.bitchord.ui.screens.SourceEditorAlert
import com.music.bitchord.ui.screens.SpotifyCanvasAuthScreen
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal val OnboardingDismissEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

internal fun playingYouTubesOwn(videoId: String, controller: MediaController?): Boolean {
    val item = controller?.currentMediaItem?.takeIf { it.mediaId == videoId } ?: return false
    val uri = item.localConfiguration?.uri
    return QualityUpgrade.isKnownToBePlayingYouTubesOwn(
        videoId,
        uri?.let(QualityUpgrade::cacheTag),
        uri?.let(AudioCache::originOf),
    )
}

private fun String?.isDeviceFolder(): Boolean =
    this != null && startsWith("local:") &&
        !startsWith(Downloads.PLAYLIST_PREFIX) && !startsWith(LocalGroups.PREFIX) &&
        !startsWith(OfflinePlaylists.PREFIX)

@Immutable
data class SongMenuState(
    val menuFromPlayer: Boolean,
    val partyState: ListenTogether.State,
    val signedIn: Boolean,
    val account: Account?,
    val switchingAudioVersion: Boolean,
    val convertedFromVideo: Song?,
    val convertedFromAudio: Song?,
    val convertedAudioId: String?,
    val convertedVideoId: String?,
    val keepVideoId: String?,
    val optimisticVersionSong: Song?,
    val playlists: List<UserPlaylist>,
    val playlistsLoading: Boolean,
    val likeStatuses: Map<String, LikeStatus>,
    val offlinePlaylists: List<OfflinePlaylist>,
    val addonCeiling: AudioQuality,
    val detail: DetailPage?,
    val versionContext: Context,
)

@Composable
internal fun rememberSongMenuState(
    menuFromPlayer: Boolean,
    partyState: ListenTogether.State,
    signedIn: Boolean,
    account: Account?,
    switchingAudioVersion: Boolean,
    convertedFromVideo: Song?,
    convertedFromAudio: Song?,
    convertedAudioId: String?,
    convertedVideoId: String?,
    keepVideoId: String?,
    optimisticVersionSong: Song?,
    playlists: List<UserPlaylist>,
    playlistsLoading: Boolean,
    likeStatuses: Map<String, LikeStatus>,
    offlinePlaylists: List<OfflinePlaylist>,
    addonCeiling: AudioQuality,
    detail: DetailPage?,
    versionContext: Context,
): SongMenuState = SongMenuState(
    menuFromPlayer = menuFromPlayer,
    partyState = partyState,
    signedIn = signedIn,
    account = account,
    switchingAudioVersion = switchingAudioVersion,
    convertedFromVideo = convertedFromVideo,
    convertedFromAudio = convertedFromAudio,
    convertedAudioId = convertedAudioId,
    convertedVideoId = convertedVideoId,
    keepVideoId = keepVideoId,
    optimisticVersionSong = optimisticVersionSong,
    playlists = playlists,
    playlistsLoading = playlistsLoading,
    likeStatuses = likeStatuses,
    offlinePlaylists = offlinePlaylists,
    addonCeiling = addonCeiling,
    detail = detail,
    versionContext = versionContext,
)

@Immutable
data class SongMenuActions(
    val onDismissPlayer: () -> Unit,
    val onDismissActions: () -> Unit,
    val onPlaySongsNext: (List<Song>) -> Unit,
    val onAddSongsToQueue: (List<Song>) -> Unit,
    val onStartDownload: (List<Song>, DownloadTarget?) -> Unit,
    val onStartPlaylistPicker: (Song) -> Unit,
    val onStartOfflinePlaylistPicker: (Song) -> Unit,
    val onShowQueueNotice: (String) -> Unit,
    val onPlayRadio: (Song, QueueSource) -> Unit,
    val onSwitchToMusicOnly: suspend (Song, Boolean) -> Unit,
    val onSwitchToVideo: suspend (Song, Boolean) -> Unit,
    val onUpdateOptimisticVersion: (Song?) -> Unit,
    val onUpdateConvertedVideo: (Song?, String?) -> Unit,
    val onUpdateConvertedAudio: (Song?, String?) -> Unit,
    val onUpdateKeepVideoId: (String?) -> Unit,
    val onOpenLyricsOffset: () -> Unit = {},
)

@Composable
internal fun SongMenuContent(
    heldSong: Song,
    state: SongMenuState,
    actions: SongMenuActions,
    controller: MediaController?,
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val fromPlayer = state.menuFromPlayer

    val needsLinks = !fromPlayer && !heldSong.isLiveRadioStream &&
        heldSong.playbackSourceId?.startsWith("radio:") != true && heldSong.localUri == null &&
        heldSong.videoId.isNotBlank() &&
        (heldSong.albumId == null || heldSong.artistId == null)
    var menuLinks by remember(heldSong.videoId) { mutableStateOf<Song?>(null) }
    var menuLinksLoading by remember(heldSong.videoId) { mutableStateOf(needsLinks) }
    LaunchedEffect(heldSong.videoId, needsLinks) {
        if (!needsLinks) return@LaunchedEffect
        menuLinksLoading = true
        menuLinks = YtMusicRepository.trackLinks(heldSong.videoId).getOrNull()
        menuLinksLoading = false
    }
    val song = menuLinks?.takeIf { needsLinks && it.videoId == heldSong.videoId }?.let { extra ->
        heldSong.copy(
            artistId = heldSong.artistId ?: extra.artistId,
            albumId = heldSong.albumId ?: extra.albumId,
            albumName = heldSong.albumName ?: extra.albumName,
        )
    } ?: heldSong

    val share: () -> Unit = {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                if (!song.radioName.isNullOrBlank()) "${song.radioName}\n${song.videoId}"
                else "https://music.youtube.com/watch?v=${song.videoId}",
            )
        }
        context.startActivity(Intent.createChooser(sendIntent, song.title))
        actions.onDismissActions()
    }

    val openPage: (String, String, String, BrowseType) -> Unit = { id, title, sub, type ->
        actions.onDismissActions()
        actions.onDismissPlayer()
        val art = song.thumbnailUrl.takeUnless { type == BrowseType.ARTIST }
        viewModel.openDetail(id, title, sub, art, type)
    }

    val versionEffectiveSong = state.optimisticVersionSong?.takeIf {
        it.videoId == state.convertedAudioId || it.videoId == state.convertedVideoId || it.videoId == state.keepVideoId ||
            it.videoId == YtMusicRepository.cachedAudioVersion(song.videoId)?.videoId ||
            it.videoId == YtMusicRepository.cachedVideoVersion(song.videoId)?.videoId
    } ?: song

    fun versionAlignmentPending(targetId: String): Boolean =
        AppSettings.smartVersionAlignment.value &&
            VersionAudioAligner.getCachedOffsetMs(song.videoId, targetId) == null &&
            YtMusicRepository.versionTimeMap(song.videoId, targetId) == null

    val menuIsAudioVersion = if (state.optimisticVersionSong != null) {
        !state.optimisticVersionSong.isVideo
    } else {
        !song.isVideo && state.convertedVideoId != song.videoId
    }

    val onToggleVersion: (() -> Unit)? = if (!song.isLiveRadioStream && song.playbackSourceId?.startsWith("radio:") != true &&
        controller?.currentMediaItem?.mediaId == song.videoId
    ) {
        toggle@{
            actions.onDismissActions()
            if (state.partyState.inParty) {
                actions.onShowQueueNotice(state.versionContext.getString(R.string.version_switch_in_party))
                return@toggle
            }
            if (state.switchingAudioVersion) return@toggle
            val c = controller
            val original = state.convertedFromVideo
            val originalAudio = state.convertedFromAudio
            when {
                c == null -> Unit
                original != null && (
                    state.convertedAudioId == song.videoId ||
                        state.convertedAudioId == versionEffectiveSong.videoId ||
                        state.convertedAudioId == state.optimisticVersionSong?.videoId
                    ) -> {
                    actions.onUpdateKeepVideoId(original.videoId)
                    actions.onUpdateConvertedVideo(null, null)
                    if (!versionAlignmentPending(original.videoId)) actions.onUpdateOptimisticVersion(original)
                    c.swapToVersion(original)
                }
                originalAudio != null && (
                    state.convertedVideoId == song.videoId ||
                        state.convertedVideoId == versionEffectiveSong.videoId ||
                        state.convertedVideoId == state.optimisticVersionSong?.videoId
                    ) -> {
                    actions.onUpdateConvertedAudio(null, null)
                    if (!versionAlignmentPending(originalAudio.videoId)) actions.onUpdateOptimisticVersion(originalAudio)
                    c.swapToVersion(originalAudio)
                }
                versionEffectiveSong.isVideo || song.isVideo -> {
                    val cached = YtMusicRepository.cachedAudioVersion(song.videoId)
                        ?: YtMusicRepository.cachedAudioVersion(versionEffectiveSong.videoId)
                    if (cached != null && cached.videoId != song.videoId &&
                        !versionAlignmentPending(cached.videoId)
                    ) {
                        actions.onUpdateOptimisticVersion(
                            cached.copy(
                                isVideoOrigin = true,
                                queueTier = song.queueTier,
                                queueEntryId = song.queueEntryId,
                                radioName = song.radioName,
                                playbackSource = song.playbackSource,
                                playbackSourceType = song.playbackSourceType,
                                playbackSourceId = song.playbackSourceId,
                            ),
                        )
                    }
                    scope.launch { actions.onSwitchToMusicOnly(song, false) }
                }
                else -> {
                    val cached = YtMusicRepository.cachedVideoVersion(song.videoId)
                        ?: YtMusicRepository.cachedVideoVersion(versionEffectiveSong.videoId)
                    if (cached != null && cached.videoId != song.videoId &&
                        !versionAlignmentPending(cached.videoId)
                    ) {
                        actions.onUpdateOptimisticVersion(
                            cached.copy(
                                queueTier = song.queueTier,
                                queueEntryId = song.queueEntryId,
                                radioName = song.radioName,
                                playbackSource = song.playbackSource,
                                playbackSourceType = song.playbackSourceType,
                                playbackSourceId = song.playbackSourceId,
                            ),
                        )
                    }
                    scope.launch { actions.onSwitchToVideo(song, false) }
                }
            }
        }
    } else {
        null
    }

    LaunchedEffect(song.videoId) { viewModel.loadSongMenu(song.videoId) }

    val editable = viewModel.editablePlaylist(state.detail?.browseId)
        ?.takeIf { !fromPlayer && song.setVideoId != null }

    val automixOn by AppSettings.smartFadeEnabled.collectAsStateWithLifecycle()
    val cacheVersion by AudioCache.contentsChanged.collectAsStateWithLifecycle()
    val inCache by produceState(false, song.videoId, cacheVersion) {
        value = song.localUri == null &&
            withContext(Dispatchers.IO) { AudioCache.holds(song.videoId) }
    }

    SongActionsMenu(
        song = song,
        signedIn = state.signedIn,
        likeStatus = state.likeStatuses[song.videoId] ?: LikeStatus.INDIFFERENT,
        onPlayNext = {
            actions.onDismissActions()
            actions.onPlaySongsNext(listOf(song))
        },
        onAddToQueue = {
            actions.onDismissActions()
            actions.onAddSongsToQueue(listOf(song))
        },
        onStartRadio = {
            actions.onDismissActions()
            actions.onDismissPlayer()
            actions.onPlayRadio(song, QueueSource(song.title, PlaybackSourceType.QUEUE, id = song.playbackSourceId))
        },
        onDownload = {
            actions.onDismissActions()
            actions.onStartDownload(listOf(song), null)
        },
        onUploadToWebDav = if (com.music.bitchord.data.webdav.WebDavConfig.isConfigured(AppSettings.webdavUrl.value) &&
            com.music.bitchord.data.webdav.WebDavUploads.isUploadable(song)
        ) {
            {
                actions.onDismissActions()
                scope.launch {
                    val summary = com.music.bitchord.data.webdav.WebDavUploads.upload(context, listOf(song))
                    if (summary.total > 0) {
                        actions.onShowQueueNotice(
                            context.getString(
                                R.string.webdav_upload_summary,
                                summary.uploaded,
                                summary.skipped,
                                summary.failed,
                            ),
                        )
                    }
                }
            }
        } else {
            null
        },
        onToggleLike = {
            actions.onDismissActions()
        },
        onToggleDislike = {
            val previousStatus = viewModel.toggleDislike(song.videoId)
            if (previousStatus != null && shouldSkipAfterDislike(previousStatus, song.videoId, controller?.currentMediaItem?.mediaId)) {
                controller?.seekToNextMediaItem()
            }
        },
        onAddToPlaylist = {
            actions.onDismissActions()
            if (song.localUri != null && (song.videoId.startsWith("content://") || song.videoId.startsWith("file://"))) {
                actions.onStartOfflinePlaylistPicker(song)
            } else {
                viewModel.loadPlaylists()
                actions.onStartPlaylistPicker(song)
            }
        },
        onOpenAlbum = { id ->
            openPage(id, song.albumName ?: song.title, song.artist, BrowseType.ALBUM)
        },
        onOpenArtist = { id ->
            openPage(id, song.artist, context.getString(R.string.artist), BrowseType.ARTIST)
        },
        onRemoveFromPlaylist = editable?.let { playlist ->
            {
                actions.onDismissActions()
                viewModel.removeFromPlaylist(playlist.browseId, song)
            }
        },
        onRemoveFromCache = if (inCache) {
            {
                actions.onDismissActions()
                scope.launch {
                    val removed = AudioCache.remove(song.videoId)
                    if (removed > 0) actions.onShowQueueNotice(context.getString(R.string.removed_from_cache))
                }
            }
        } else {
            null
        },
        onRemoveFromHistory = song.feedbackToken?.let { token ->
            {
                actions.onDismissActions()
                viewModel.removeFromHistory(token, onFailure = {
                    Toast.makeText(context, R.string.remove_from_history_failed, Toast.LENGTH_SHORT).show()
                })
            }
        },
        resolvingLinks = menuLinksLoading,
        compactPlayerMenu = fromPlayer && song.playbackSourceId?.startsWith("radio:") == true,
        showSleepTimer = fromPlayer,
        onRollbackToOriginal = if (fromPlayer && !song.isLiveRadioStream && song.playbackSourceId?.startsWith("radio:") != true &&
            song.hasYouTubeOriginal() &&
            song.localUri == null &&
            song.videoId !in OriginalVersion.pinned.value &&
            !playingYouTubesOwn(song.videoId, controller) &&
            controller?.currentMediaItem?.mediaId == song.videoId
        ) {
            {
                controller?.revertToOriginal()
                actions.onDismissActions()
            }
        } else {
            null
        },
        onUpgradeQuality = if (fromPlayer && !song.isLiveRadioStream && song.playbackSourceId?.startsWith("radio:") != true &&
            (song.videoId in OriginalVersion.pinned.value || playingYouTubesOwn(song.videoId, controller)) &&
            song.localUri == null &&
            controller?.currentMediaItem?.mediaId == song.videoId
        ) {
            {
                controller.upgradeQuality()
                actions.onDismissActions()
            }
        } else {
            null
        },
        upgradeQualityInProgress = fromPlayer && !song.isLiveRadioStream && song.playbackSourceId?.startsWith("radio:") != true &&
            song.videoId in NerdStats.racingLossless.value,
        onToggleAudioVersion = onToggleVersion,
        isAudioVersion = menuIsAudioVersion,
        audioVersionMissing = false,
        onShare = share.takeIf { song.videoId.isNotBlank() },
        onLyricsOffset = if (fromPlayer) {
            {
                actions.onDismissActions()
                actions.onOpenLyricsOffset()
            }
        } else {
            null
        },
        onCopyLog = if (fromPlayer) {
            {
                actions.onDismissActions()
                scope.launch {
                    val text = TrackLog.forTrack(song, NerdStats.current.value)
                    clipboard.setText(AnnotatedString(text))
                    Toast.makeText(
                        context,
                        context.resources.getQuantityString(
                            R.plurals.log_copied_line_count,
                            text.lineSequence().count(),
                            text.lineSequence().count(),
                        ),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        } else {
            null
        },
        onAutomixOverview = if (fromPlayer && automixOn) {
            {
                actions.onDismissActions()
                MixViewRequest.open = true
            }
        } else {
            null
        },
    )
}

@Immutable
data class GlobalSheetsHostState(
    val route: AppRoute?,
    val hazeState: HazeState,
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
    val blockedAddonSong: Song?,
    val addonLeftOutOfDownload: Pair<String, SourceRegistry.DownloadExclusion>?,
    val showAboutWhatsNew: Boolean,
    val songMenuState: SongMenuState,
)

@Composable
internal fun rememberGlobalSheetsHostState(
    route: AppRoute?,
    hazeState: HazeState,
    pageBackdrop: SnapshotBackdrop,
    reduceDynamicBlur: Boolean,
    playerRaised: Boolean,
    songActions: Song?,
    songMenuOrigin: HeldItem?,
    songMenuAnchor: Rect?,
    menuFromPlayer: Boolean,
    browseActions: BrowseTarget?,
    browseMenuOrigin: HeldItem?,
    browseMenuAnchor: Rect?,
    browseRenameInSheet: Boolean,
    stationActions: StationMenuTarget?,
    replay: ReplayState,
    replayStory: ReplayStoryPage?,
    showReplayShare: Boolean,
    replaySharePage: ReplayStoryPage?,
    account: Account?,
    playlistTarget: Song?,
    creatingPlaylist: Boolean,
    playlists: List<UserPlaylist>,
    playlistsLoading: Boolean,
    offlinePlaylistTarget: Song?,
    creatingOfflinePlaylist: Boolean,
    offlinePlaylists: List<OfflinePlaylist>,
    showSpotifyImportDialog: Boolean,
    signedIn: Boolean,
    editPlaylistTarget: UserPlaylist?,
    detailStack: List<DetailPage>,
    libraryState: UiState<LibraryPage>,
    likeStatuses: Map<String, LikeStatus>,
    query: String,
    results: UiState<List<SearchResult>>?,
    filter: SearchFilter,
    onboardingShown: MutableTransitionState<Boolean>,
    webSession: WebSessionMode?,
    googleAccounts: List<GoogleAccountSession>,
    activeAccountId: String?,
    activeProfileId: String?,
    showDiwali: Boolean,
    showUpdateDialog: Boolean,
    onboardingComplete: Boolean,
    updateAvailable: AppUpdateChecker.UpdateInfo?,
    showLyricsSources: Boolean,
    songSortMenuOpen: Boolean,
    songSort: SongSort,
    detail: DetailPage?,
    folderMenuOpen: Boolean,
    showAccountSelector: Boolean,
    showAppLanguage: Boolean,
    showTranslationLanguage: Boolean,
    showListenBrainzLogin: Boolean,
    listenBrainzToken: String,
    showLastfmLogin: Boolean,
    showWebDavEditor: Boolean,
    showSmbEditor: Boolean,
    showDiscordLogin: Boolean,
    showSpotifyLogin: Boolean,
    showSpotifyCanvasAuth: Boolean,
    discordDialog: DiscordDialog?,
    editingPartyServer: Boolean,
    editingSignatureApi: Boolean,
    editingSource: SourceConfig?,
    choosingSourceQuality: SourceConfig?,
    explainingOutdatedAddon: SourceConfig?,
    confirmJioSaavn: Boolean,
    confirmBitPerfect: BitPerfectMode?,
    confirmFixedVolume: Boolean,
    blockedAddonSong: Song?,
    addonLeftOutOfDownload: Pair<String, SourceRegistry.DownloadExclusion>?,
    showAboutWhatsNew: Boolean,
    songMenuState: SongMenuState,
): GlobalSheetsHostState = GlobalSheetsHostState(
    route = route,
    hazeState = hazeState,
    pageBackdrop = pageBackdrop,
    reduceDynamicBlur = reduceDynamicBlur,
    playerRaised = playerRaised,
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
    songMenuState = songMenuState,
)

@Immutable
data class GlobalSheetsHostActions(
    val onDismissSongActions: () -> Unit,
    val onDismissBrowseActions: () -> Unit,
    val onDismissStationActions: () -> Unit,
    val onDismissReplayStory: () -> Unit,
    val onShareReplayCard: (ReplayStoryPage) -> Unit,
    val onDismissReplayShare: () -> Unit,
    val onDismissPlaylistPicker: () -> Unit,
    val onDismissOfflinePlaylistPicker: () -> Unit,
    val onDismissSpotifyImport: () -> Unit,
    val onDismissEditPlaylist: () -> Unit,
    val onDismissWebSession: () -> Unit,
    val onDismissDiwali: () -> Unit,
    val onDismissUpdateDialog: () -> Unit,
    val onDismissLyricsSources: () -> Unit,
    val onDismissSongSortMenu: () -> Unit,
    val onDismissFolderMenu: () -> Unit,
    val onDismissAccountSelector: () -> Unit,
    val onDismissAppLanguage: () -> Unit,
    val onDismissTranslationLanguage: () -> Unit,
    val onDismissListenBrainzLogin: () -> Unit,
    val onDismissLastfmLogin: () -> Unit,
    val onDismissWebDavEditor: () -> Unit,
    val onDismissSmbEditor: () -> Unit,
    val onDismissDiscordLogin: () -> Unit,
    val onDismissSpotifyLogin: () -> Unit,
    val onDismissSpotifyCanvasAuth: () -> Unit,
    val onDismissDiscordDialog: () -> Unit,
    val onDismissPartyServer: () -> Unit,
    val onDismissSignatureApi: () -> Unit,
    val onDismissEditingSource: () -> Unit,
    val onDismissChoosingSourceQuality: () -> Unit,
    val onDismissExplainingOutdatedAddon: () -> Unit,
    val onDismissConfirmJioSaavn: () -> Unit,
    val onDismissConfirmBitPerfect: () -> Unit,
    val onDismissConfirmFixedVolume: () -> Unit,
    val onDismissBlockedAddonSong: () -> Unit,
    val onDismissAddonLeftOutOfDownload: () -> Unit,
    val onDismissAboutWhatsNew: () -> Unit,
    val onShowQueueNotice: (String) -> Unit,
    val onSelectTab: (Int) -> Unit,
    val onDismissPlayer: () -> Unit,
    val onPlaySongsNext: (List<Song>) -> Unit,
    val onAddSongsToQueue: (List<Song>) -> Unit,
    val onPlayFrom: (List<Song>, Int) -> Unit,
    val onStartDownload: (List<Song>, DownloadTarget?) -> Unit,
    val onStartPlaylistPicker: (Song) -> Unit,
    val onStartOfflinePlaylistPicker: (Song) -> Unit,
    val onFolderSortOf: (String) -> LocalMusicSort,
    val onFolderViewTypeOf: (String) -> LibraryViewType,
    val onBrowseRenameInSheetChange: (Boolean) -> Unit,
    val onEditPlaylistTargetChange: (UserPlaylist?) -> Unit,
    val onWebSessionModeChange: (WebSessionMode?) -> Unit,
    val onCreatingPlaylistChange: (Boolean) -> Unit,
    val onCreatingOfflinePlaylistChange: (Boolean) -> Unit,
    val onSongMenuActions: SongMenuActions,
)

@Composable
internal fun rememberGlobalSheetsHostActions(
    onDismissSongActions: () -> Unit,
    onDismissBrowseActions: () -> Unit,
    onDismissStationActions: () -> Unit,
    onDismissReplayStory: () -> Unit,
    onShareReplayCard: (ReplayStoryPage) -> Unit,
    onDismissReplayShare: () -> Unit,
    onDismissPlaylistPicker: () -> Unit,
    onDismissOfflinePlaylistPicker: () -> Unit,
    onDismissSpotifyImport: () -> Unit,
    onDismissEditPlaylist: () -> Unit,
    onDismissWebSession: () -> Unit,
    onDismissDiwali: () -> Unit,
    onDismissUpdateDialog: () -> Unit,
    onDismissLyricsSources: () -> Unit,
    onDismissSongSortMenu: () -> Unit,
    onDismissFolderMenu: () -> Unit,
    onDismissAccountSelector: () -> Unit,
    onDismissAppLanguage: () -> Unit,
    onDismissTranslationLanguage: () -> Unit,
    onDismissListenBrainzLogin: () -> Unit,
    onDismissLastfmLogin: () -> Unit,
    onDismissWebDavEditor: () -> Unit,
    onDismissSmbEditor: () -> Unit,
    onDismissDiscordLogin: () -> Unit,
    onDismissSpotifyLogin: () -> Unit,
    onDismissSpotifyCanvasAuth: () -> Unit,
    onDismissDiscordDialog: () -> Unit,
    onDismissPartyServer: () -> Unit,
    onDismissSignatureApi: () -> Unit,
    onDismissEditingSource: () -> Unit,
    onDismissChoosingSourceQuality: () -> Unit,
    onDismissExplainingOutdatedAddon: () -> Unit,
    onDismissConfirmJioSaavn: () -> Unit,
    onDismissConfirmBitPerfect: () -> Unit,
    onDismissConfirmFixedVolume: () -> Unit,
    onDismissBlockedAddonSong: () -> Unit,
    onDismissAddonLeftOutOfDownload: () -> Unit,
    onDismissAboutWhatsNew: () -> Unit,
    onShowQueueNotice: (String) -> Unit,
    onSelectTab: (Int) -> Unit,
    onDismissPlayer: () -> Unit,
    onPlaySongsNext: (List<Song>) -> Unit,
    onAddSongsToQueue: (List<Song>) -> Unit,
    onPlayFrom: (List<Song>, Int) -> Unit,
    onStartDownload: (List<Song>, DownloadTarget?) -> Unit,
    onStartPlaylistPicker: (Song) -> Unit,
    onStartOfflinePlaylistPicker: (Song) -> Unit,
    onFolderSortOf: (String) -> LocalMusicSort,
    onFolderViewTypeOf: (String) -> LibraryViewType,
    onBrowseRenameInSheetChange: (Boolean) -> Unit,
    onEditPlaylistTargetChange: (UserPlaylist?) -> Unit,
    onWebSessionModeChange: (WebSessionMode?) -> Unit,
    onCreatingPlaylistChange: (Boolean) -> Unit,
    onCreatingOfflinePlaylistChange: (Boolean) -> Unit,
    onSongMenuActions: SongMenuActions,
): GlobalSheetsHostActions = GlobalSheetsHostActions(
    onDismissSongActions = onDismissSongActions,
    onDismissBrowseActions = onDismissBrowseActions,
    onDismissStationActions = onDismissStationActions,
    onDismissReplayStory = onDismissReplayStory,
    onShareReplayCard = onShareReplayCard,
    onDismissReplayShare = onDismissReplayShare,
    onDismissPlaylistPicker = onDismissPlaylistPicker,
    onDismissOfflinePlaylistPicker = onDismissOfflinePlaylistPicker,
    onDismissSpotifyImport = onDismissSpotifyImport,
    onDismissEditPlaylist = onDismissEditPlaylist,
    onDismissWebSession = onDismissWebSession,
    onDismissDiwali = onDismissDiwali,
    onDismissUpdateDialog = onDismissUpdateDialog,
    onDismissLyricsSources = onDismissLyricsSources,
    onDismissSongSortMenu = onDismissSongSortMenu,
    onDismissFolderMenu = onDismissFolderMenu,
    onDismissAccountSelector = onDismissAccountSelector,
    onDismissAppLanguage = onDismissAppLanguage,
    onDismissTranslationLanguage = onDismissTranslationLanguage,
    onDismissListenBrainzLogin = onDismissListenBrainzLogin,
    onDismissLastfmLogin = onDismissLastfmLogin,
    onDismissWebDavEditor = onDismissWebDavEditor,
    onDismissSmbEditor = onDismissSmbEditor,
    onDismissDiscordLogin = onDismissDiscordLogin,
    onDismissSpotifyLogin = onDismissSpotifyLogin,
    onDismissSpotifyCanvasAuth = onDismissSpotifyCanvasAuth,
    onDismissDiscordDialog = onDismissDiscordDialog,
    onDismissPartyServer = onDismissPartyServer,
    onDismissSignatureApi = onDismissSignatureApi,
    onDismissEditingSource = onDismissEditingSource,
    onDismissChoosingSourceQuality = onDismissChoosingSourceQuality,
    onDismissExplainingOutdatedAddon = onDismissExplainingOutdatedAddon,
    onDismissConfirmJioSaavn = onDismissConfirmJioSaavn,
    onDismissConfirmBitPerfect = onDismissConfirmBitPerfect,
    onDismissConfirmFixedVolume = onDismissConfirmFixedVolume,
    onDismissBlockedAddonSong = onDismissBlockedAddonSong,
    onDismissAddonLeftOutOfDownload = onDismissAddonLeftOutOfDownload,
    onDismissAboutWhatsNew = onDismissAboutWhatsNew,
    onShowQueueNotice = onShowQueueNotice,
    onSelectTab = onSelectTab,
    onDismissPlayer = onDismissPlayer,
    onPlaySongsNext = onPlaySongsNext,
    onAddSongsToQueue = onAddSongsToQueue,
    onPlayFrom = onPlayFrom,
    onStartDownload = onStartDownload,
    onStartPlaylistPicker = onStartPlaylistPicker,
    onStartOfflinePlaylistPicker = onStartOfflinePlaylistPicker,
    onFolderSortOf = onFolderSortOf,
    onFolderViewTypeOf = onFolderViewTypeOf,
    onBrowseRenameInSheetChange = onBrowseRenameInSheetChange,
    onEditPlaylistTargetChange = onEditPlaylistTargetChange,
    onWebSessionModeChange = onWebSessionModeChange,
    onCreatingPlaylistChange = onCreatingPlaylistChange,
    onCreatingOfflinePlaylistChange = onCreatingOfflinePlaylistChange,
    onSongMenuActions = onSongMenuActions,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GlobalSheetsHost(
    state: GlobalSheetsHostState,
    actions: GlobalSheetsHostActions,
    controller: MediaController?,
    viewModel: MainViewModel,
    navigator: AppNavigator,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Isolated {
        val pageMenuUp =
            (state.songActions != null && state.songMenuOrigin == null && !(state.menuFromPlayer && state.playerRaised)) ||
                (state.browseActions != null && state.browseMenuOrigin == null && !state.browseRenameInSheet)
        LaunchedEffect(pageMenuUp, state.reduceDynamicBlur) {
            if (pageMenuUp && !state.reduceDynamicBlur && isGlassSupported()) {
                state.pageBackdrop.capture()
            } else {
                delay(SNAPSHOT_RELEASE_DELAY_MS)
                state.pageBackdrop.release()
            }
        }
    }

    state.replayStory?.let { start ->
        state.replay.summary?.takeUnless { it.isEmpty }?.let { summary ->
            ReplayStories(
                summary = summary,
                start = start,
                onClose = actions.onDismissReplayStory,
                onShare = { card ->
                    actions.onShareReplayCard(card)
                },
                paused = state.showReplayShare,
            )
        }
    }

    if (state.showReplayShare) {
        state.replay.summary?.takeUnless { it.isEmpty }?.let { summary ->
            ModalBottomSheet(
                onDismissRequest = actions.onDismissReplayShare,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                ReplayShareSheet(
                    summary = summary,
                    holder = state.account?.name.orEmpty(),
                    memberSince = state.replay.memberSince,
                    page = state.replaySharePage,
                    onDismiss = actions.onDismissReplayShare,
                )
            }
        }
    }

    Isolated {
        StationActionsMenu(
            target = state.stationActions,
            hazeState = state.hazeState,
            onDismiss = actions.onDismissStationActions,
        )
        HeldContextMenu(
            item = state.songActions,
            held = state.songMenuOrigin,
            hazeState = state.hazeState,
            onDismiss = actions.onDismissSongActions,
            onPreviewClick = { song ->
                song.albumId?.let { id ->
                    actions.onDismissSongActions()
                    viewModel.openDetail(id, song.albumName ?: song.title, song.artist, song.thumbnailUrl, BrowseType.ALBUM)
                }
            },
        ) { song ->
            SongMenuContent(song, state.songMenuState, actions.onSongMenuActions, controller, viewModel)
        }
        LaunchedEffect(state.playerRaised) {
            if (!state.playerRaised && state.menuFromPlayer) actions.onDismissSongActions()
        }
        IosMenuPopover(
            item = state.songActions?.takeIf { state.songMenuOrigin == null && !(state.menuFromPlayer && state.playerRaised) },
            anchor = state.songMenuAnchor,
            onDismiss = actions.onDismissSongActions,
            backdrop = state.pageBackdrop,
            hazeState = state.hazeState,
        ) { song ->
            SongMenuContent(song, state.songMenuState, actions.onSongMenuActions, controller, viewModel)
        }
    }

    if (state.playlistTarget != null || state.creatingPlaylist) {
        val target = state.playlistTarget
        val dismiss = {
            actions.onDismissPlaylistPicker()
            actions.onCreatingPlaylistChange(false)
        }
        PlaylistPickerSheet(
            playlists = state.playlists,
            loading = state.playlistsLoading,
            song = target,
            startCreating = target == null,
            onDismiss = dismiss,
            onAdd = { picked ->
                target?.let { song ->
                    viewModel.addToPlaylists(picked, song) { added, alreadyThere, failed ->
                        actions.onShowQueueNotice(
                            when {
                                added > 1 -> context.resources.getQuantityString(
                                    R.plurals.added_to_playlists_notice, added, added,
                                )
                                added == 1 -> context.getString(R.string.song_added_to_playlist)
                                alreadyThere > 0 && failed == 0 ->
                                    context.getString(R.string.song_already_in_playlist)
                                else -> context.getString(R.string.failed)
                            },
                        )
                    }
                }
            },
            onCreate = { title, privacy, coverUri ->
                viewModel.createPlaylist(title, privacy, target, coverUri) { created ->
                    actions.onCreatingPlaylistChange(false)
                    if (target == null) {
                        dismiss()
                        viewModel.openDetail(
                            browseId = created.browseId,
                            title = created.title,
                            subtitle = created.subtitle,
                            thumbnailUrl = created.thumbnailUrl,
                            type = BrowseType.PLAYLIST,
                        )
                        AddSongsRequest.showForPlaylist(created)
                    }
                }
            },
        )
    }

    if (state.offlinePlaylistTarget != null || state.creatingOfflinePlaylist) {
        val target = state.offlinePlaylistTarget
        val dismiss = {
            actions.onDismissOfflinePlaylistPicker()
            actions.onCreatingOfflinePlaylistChange(false)
        }
        val choices = remember(state.offlinePlaylists) {
            state.offlinePlaylists.map { playlist ->
                UserPlaylist(
                    playlistId = playlist.id,
                    title = playlist.title,
                    subtitle = context.resources.getQuantityString(
                        R.plurals.song_count_plural,
                        playlist.songs.size,
                        playlist.songs.size,
                    ),
                    thumbnailUrl = if (playlist.isLiked) null else
                        com.music.bitchord.data.PlaylistCovers.getCoverUri(context, playlist.id)?.toString()
                            ?: playlist.songs.firstNotNullOfOrNull { it.thumbnailUrl },
                )
            }
        }
        PlaylistPickerSheet(
            playlists = choices,
            loading = false,
            song = target,
            startCreating = target == null,
            offline = true,
            onDismiss = dismiss,
            onAdd = { picked ->
                target?.let { song ->
                    val result = OfflinePlaylists.add(picked.mapTo(HashSet()) { it.playlistId }, listOf(song))
                    actions.onShowQueueNotice(
                        when {
                            result.added > 1 -> context.resources.getQuantityString(
                                R.plurals.added_to_playlists_notice, result.added, result.added,
                            )
                            result.added == 1 -> context.getString(R.string.song_added_to_playlist)
                            result.alreadyThere > 0 -> context.getString(R.string.song_already_in_playlist)
                            else -> context.getString(R.string.failed)
                        },
                    )
                }
            },
            onCreate = { title, _, _ ->
                OfflinePlaylists.create(title, listOfNotNull(target))
                actions.onCreatingOfflinePlaylistChange(false)
                if (target != null) actions.onShowQueueNotice(context.getString(R.string.song_added_to_playlist))
            },
        )
    }

    if (state.showSpotifyImportDialog) {
        BackHandler { actions.onDismissSpotifyImport() }
        SpotifyImportAlert(
            hazeState = state.hazeState,
            signedIn = state.signedIn,
            onImported = { title, privacy, songs ->
                viewModel.createPlaylistWithVideoIds(
                    title,
                    privacy,
                    songs.map { it.videoId },
                    songs,
                ) { browseId, pTitle, savedLocally ->
                    actions.onShowQueueNotice(
                        context.getString(
                            if (savedLocally && state.signedIn) R.string.spotify_import_local_fallback
                            else R.string.spotify_import_done,
                            pTitle,
                        ),
                    )
                    browseId?.let { id ->
                        viewModel.openDetail(id, pTitle, "${songs.size} songs", songs.firstOrNull()?.thumbnailUrl)
                    }
                }
            },
            onDismiss = actions.onDismissSpotifyImport,
        )
    }

    val withBrowseSongs: (BrowseTarget, (List<Song>) -> Unit) -> Unit = { target, action ->
        val stamp: (List<Song>) -> Unit = { songs ->
            action(
                songs.map { s ->
                    s.copy(
                        albumName = if (target.type == BrowseType.ALBUM) s.albumName ?: target.title else s.albumName,
                        playbackSource = target.title,
                        playbackSourceType = PlaybackSourceType.BROWSE,
                        playbackSourceId = target.browseId,
                    )
                },
            )
        }
        when {
            target.songs.isNotEmpty() -> stamp(target.songs)
            target.browseId == null ->
                Toast.makeText(context, context.getString(R.string.no_tracks_here), Toast.LENGTH_SHORT).show()
            else -> viewModel.collectSongs(target.browseId, target.thumbnailUrl) { result ->
                result.fold(
                    onSuccess = stamp,
                    onFailure = { Toast.makeText(context, context.getString(R.string.failed), Toast.LENGTH_SHORT).show() },
                )
            }
        }
    }

    val browseMenuBody: @Composable (BrowseTarget) -> Unit = { target ->
        val act: ((List<Song>) -> Unit) -> () -> Unit = { action ->
            {
                actions.onDismissBrowseActions()
                withBrowseSongs(target, action)
            }
        }
        LaunchedEffect(target.browseId) {
            viewModel.resolvePlaylistOwnership(target.browseId)
        }
        val ownedPlaylists by viewModel.playlistOwned.collectAsStateWithLifecycle()
        val playlist = target.browseId
            ?.takeIf { state.signedIn && ownedPlaylists[it] == true }
            ?.let { id -> state.playlists.firstOrNull { it.browseId == id } }
        val remote = target.browseId?.startsWith("local:") == false
        val downloadable = remote || target.browseId?.startsWith("local:playlist:sp_local_") == true
        val pinnedPlaylists by AppSettings.pinnedPlaylists.collectAsStateWithLifecycle()
        val pinnableId = target.browseId?.takeIf {
            target.type == BrowseType.PLAYLIST &&
                !it.startsWith(OfflinePlaylists.PREFIX) && !it.startsWith(LocalGroups.PREFIX)
        }
        val offlineEditable = OfflinePlaylists.get(target.browseId)?.takeUnless { it.isLiked }
        BrowseActionsMenu(
            target = target.copy(
                playlist = playlist ?: offlineEditable?.let {
                    UserPlaylist(playlistId = it.id, title = it.title, subtitle = "", thumbnailUrl = null)
                },
            ),
            onRenameInSheet = {
                if (playlist != null) {
                    actions.onDismissBrowseActions()
                    actions.onEditPlaylistTargetChange(playlist)
                } else {
                    actions.onBrowseRenameInSheetChange(true)
                }
            },
            startRenaming = state.browseRenameInSheet,
            onPlayNext = act(actions.onPlaySongsNext),
            onAddToQueue = act(actions.onAddSongsToQueue),
            onPlay = act { songs -> actions.onPlayFrom(songs, 0) }.takeIf { target.fromCard },
            onShuffle = act { songs ->
                QueueShuffle.enableForNextQueue()
                actions.onPlayFrom(songs, songs.indices.random())
            }.takeIf { target.fromCard },
            onOpen = target.browseId
                ?.takeIf { target.fromCard }
                ?.let { id ->
                    {
                        actions.onDismissBrowseActions()
                        viewModel.openDetail(
                            browseId = id,
                            title = target.title,
                            subtitle = target.subtitle,
                            thumbnailUrl = target.thumbnailUrl,
                            type = target.type,
                        )
                    }
                },
            onDownloadAll = act { songs ->
                actions.onStartDownload(
                    songs,
                    target.browseId
                        ?.takeIf { target.type != BrowseType.ARTIST }
                        ?.let { id ->
                            DownloadTarget(
                                id = id,
                                title = target.title,
                                subtitle = target.subtitle,
                                thumbnailUrl = target.thumbnailUrl,
                                playlist = target.type == BrowseType.PLAYLIST,
                            )
                        },
                )
            }.takeIf { downloadable },
            onShare = target.browseId
                ?.takeIf { remote && (target.type == BrowseType.ALBUM || target.type == BrowseType.PLAYLIST) }
                ?.let { id ->
                    {
                        val url = if (target.type == BrowseType.PLAYLIST) {
                            "https://music.youtube.com/playlist?list=${id.removePrefix("VL")}"
                        } else {
                            "https://music.youtube.com/browse/$id"
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, url)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, target.title))
                        actions.onDismissBrowseActions()
                    }
                },
            isPinned = pinnableId != null && pinnableId in pinnedPlaylists,
            onTogglePin = pinnableId?.let { id ->
                {
                    val nowPinned = AppSettings.togglePinnedPlaylist(id)
                    if (!nowPinned && id !in pinnedPlaylists) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.pinned_playlist_limit, AppSettings.MAX_PINNED_PLAYLISTS),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    actions.onDismissBrowseActions()
                }
            },
            onRename = playlist?.let { p ->
                { name: String ->
                    actions.onDismissBrowseActions()
                    viewModel.renamePlaylist(p, name)
                }
            } ?: offlineEditable?.let { p ->
                { name: String ->
                    actions.onDismissBrowseActions()
                    OfflinePlaylists.rename(p.browseId, name)
                    viewModel.retitleDetail(p.browseId, name.trim())
                }
            },
            isOwnedPlaylist = playlist != null,
            onReorder = playlist?.let { p ->
                {
                    actions.onDismissBrowseActions()
                    if (state.detail?.browseId != p.browseId) {
                        viewModel.openDetail(
                            browseId = p.browseId,
                            title = p.title,
                            subtitle = p.subtitle,
                            thumbnailUrl = p.thumbnailUrl,
                            type = BrowseType.PLAYLIST,
                        )
                    }
                }
            },
            onDelete = playlist?.let { p ->
                {
                    actions.onDismissBrowseActions()
                    if (state.detail?.browseId == p.browseId) {
                        viewModel.closeDetail()
                        actions.onSelectTab(TAB_LIBRARY)
                    }
                    viewModel.deletePlaylist(p)
                }
            } ?: offlineEditable?.let { p ->
                {
                    actions.onDismissBrowseActions()
                    if (state.detail?.browseId == p.browseId) viewModel.closeDetail()
                    OfflinePlaylists.delete(p.browseId)
                }
            },
            onDeleteDownload = target.downloadId?.let { id ->
                {
                    actions.onDismissBrowseActions()
                    scope.launch { Downloads.deleteCollection(context, id) }
                }
            },
        )
    }

    Isolated {
        state.editPlaylistTarget?.let { playlist ->
            val customCover = com.music.bitchord.data.PlaylistCovers
                .getCoverUri(context, playlist.playlistId)
            EditPlaylistSheet(
                playlist = playlist,
                customCoverUri = customCover,
                onDismiss = actions.onDismissEditPlaylist,
                onSave = { title, newCover, removeCover ->
                    if (title != playlist.title) viewModel.renamePlaylist(playlist, title)
                    if (newCover != null || removeCover) {
                        viewModel.updatePlaylistCover(playlist, newCover, removeCover)
                    }
                    actions.onDismissEditPlaylist()
                },
            )
        }
        state.browseActions?.takeIf { state.browseRenameInSheet }?.let { target ->
            ModalBottomSheet(
                onDismissRequest = actions.onDismissBrowseActions,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                browseMenuBody(target)
            }
        }
        HeldContextMenu(
            item = state.browseActions,
            held = state.browseMenuOrigin,
            hazeState = state.hazeState,
            onDismiss = actions.onDismissBrowseActions,
            onPreviewClick = { target ->
                target.browseId?.let { id ->
                    actions.onDismissBrowseActions()
                    viewModel.openDetail(
                        browseId = id,
                        title = target.title,
                        subtitle = target.subtitle,
                        thumbnailUrl = target.thumbnailUrl,
                        type = target.type,
                    )
                }
            },
        ) { target ->
            browseMenuBody(target)
        }
        IosMenuPopover(
            item = state.browseActions?.takeIf { state.browseMenuOrigin == null && !state.browseRenameInSheet },
            anchor = state.browseMenuAnchor,
            onDismiss = actions.onDismissBrowseActions,
            backdrop = state.pageBackdrop,
            hazeState = state.hazeState,
        ) { target ->
            browseMenuBody(target)
        }
    }

    PlaylistAddSongsHost(
        viewModel = viewModel,
        detailStack = state.detailStack,
        library = state.libraryState,
        likeStatuses = state.likeStatuses,
        query = state.query,
        results = state.results,
        filter = state.filter,
        hazeState = state.hazeState,
        onNotice = actions.onShowQueueNotice,
    )

    AnimatedVisibility(
        visibleState = state.onboardingShown,
        enter = EnterTransition.None,
        exit = slideOutVertically(
            tween<IntOffset>(durationMillis = 560, easing = OnboardingDismissEasing),
        ) { it },
    ) {
        OnboardingScreen(
            signedIn = state.signedIn,
            initialName = remember {
                ListenTogether.nickname().ifBlank { state.account?.name.orEmpty() }
            },
            accountName = state.account?.name,
            accountAvatarUrl = state.account?.thumbnailUrl,
            onNameChosen = ListenTogether::setNickname,
            onSignIn = { actions.onWebSessionModeChange(WebSessionMode.SIGN_IN) },
            onFinish = {
                AppSettings.setOnboardingComplete()
                viewModel.refreshTopPicksWithOnboardingSeeds()
            },
        )
    }

    state.webSession?.let { mode ->
        BackHandler { actions.onDismissWebSession() }
        var captureRequest by remember(mode) { mutableIntStateOf(0) }
        var captureFailed by remember(mode) { mutableStateOf(false) }
        var pageReady by remember(mode) { mutableStateOf(false) }
        var confirmingProfile by remember(mode) { mutableStateOf(false) }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = actions.onDismissWebSession) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = when (mode) {
                                WebSessionMode.SIGN_IN -> stringResource(R.string.sign_in_youtube_music)
                                WebSessionMode.SWITCH_CHANNEL -> stringResource(R.string.choose_profile)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        if (pageReady) {
                            Text(
                                text = if (captureFailed) {
                                    stringResource(R.string.profile_unavailable)
                                } else {
                                    stringResource(R.string.switch_profile_hint)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                            )
                        }
                    }
                    if (pageReady) {
                        TextButton(onClick = {
                            captureFailed = false
                            confirmingProfile = true
                            captureRequest++
                        }, enabled = !confirmingProfile) {
                            Text(
                                if (confirmingProfile) stringResource(R.string.checking)
                                else stringResource(R.string.use_this_profile),
                            )
                        }
                    }
                }
                YtMusicLoginScreen(
                    mode = mode,
                    initialCookie = if (mode == WebSessionMode.SWITCH_CHANNEL) {
                        state.googleAccounts.firstOrNull { it.accountId == state.activeAccountId }?.cookie
                    } else null,
                    captureRequest = captureRequest,
                    onPageReady = { pageReady = it },
                    onCaptureUnavailable = {
                        confirmingProfile = false
                        captureFailed = true
                    },
                    onCaptured = { session ->
                        viewModel.onWebSession(session, mode) { accepted ->
                            confirmingProfile = false
                            if (accepted) {
                                actions.onDismissWebSession()
                                if (mode == WebSessionMode.SIGN_IN) {
                                    navigator.clear()
                                    actions.onSelectTab(TAB_LIBRARY)
                                }
                            } else {
                                captureFailed = true
                            }
                        }
                    },
                )
            }
        }
    }

    AnimatedVisibility(
        visible = state.showDiwali,
        enter = fadeIn(tween(400)),
        exit = fadeOut(tween(300)),
    ) {
        val dismissDiwali = {
            actions.onDismissDiwali()
            AppSettings.setDiwaliGreetingShown()
        }
        BackHandler(onBack = dismissDiwali)
        DiwaliGreetingScreen(onDismiss = dismissDiwali)
    }

    if (state.showUpdateDialog && state.onboardingComplete && !state.showDiwali) {
        state.updateAvailable?.let { update ->
            UpdateAvailableDialog(
                version = update.version,
                notes = update.notes,
                hazeState = state.hazeState,
                onDismiss = actions.onDismissUpdateDialog,
                onDownload = {
                    if (update.apkUrl != null) {
                        scope.launch { AppUpdateChecker.downloadApk(context) }
                    } else {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.releaseUrl)))
                        actions.onDismissUpdateDialog()
                    }
                },
                onCancelDownload = {
                    AppUpdateChecker.cancelDownload()
                },
                onInstall = {
                    val ready = AppUpdateChecker.download.value as? AppUpdateChecker.DownloadState.Ready
                    ready?.let { AppUpdateChecker.installApk(context, it.file) }
                },
                onOpenReleasePage = {
                    AppUpdateChecker.resetDownload()
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.releaseUrl)))
                    actions.onDismissUpdateDialog()
                },
            )
        }
    }

    if (state.showLyricsSources) {
        BackHandler { actions.onDismissLyricsSources() }
        LyricsSourcesDialog(
            hazeState = state.hazeState,
            onDismiss = actions.onDismissLyricsSources,
        )
    }

    if (state.songSortMenuOpen) {
        BackHandler { actions.onDismissSongSortMenu() }
        FrostedSortMenu(
            hazeState = state.hazeState,
            selected = state.songSort,
            onSelect = { option ->
                state.detail?.browseId?.let { AppSettings.setDetailSongSort(it, option) }
                actions.onDismissSongSortMenu()
            },
            onFlipDateDirection = {
                state.detail?.browseId?.let { browseId ->
                    val next = when (state.songSort) {
                        SongSort.DATE_ADDED_DESC -> SongSort.DATE_ADDED_ASC
                        SongSort.DATE_ADDED_ASC -> SongSort.DATE_ADDED_DESC
                        else -> SongSort.DATE_ADDED_DESC
                    }
                    AppSettings.setDetailSongSort(browseId, next)
                }
            },
            onDismiss = actions.onDismissSongSortMenu,
        )
    }

    val menuFolder = state.detail?.browseId?.takeIf { it.isDeviceFolder() }
    if (state.folderMenuOpen && menuFolder != null) {
        BackHandler { actions.onDismissFolderMenu() }
        val downloadsFolder = menuFolder == "local:downloads"
        val viewType = actions.onFolderViewTypeOf(menuFolder)
        FrostedFolderMenu(
            hazeState = state.hazeState,
            sort = actions.onFolderSortOf(menuFolder),
            viewType = viewType,
            onSort = { option ->
                if (downloadsFolder) AppSettings.setDownloadedMusicSort(option)
                else AppSettings.setLocalMusicSort(option)
                actions.onDismissFolderMenu()
            },
            onToggleViewType = {
                val next = if (viewType == LibraryViewType.GRID) LibraryViewType.LIST else LibraryViewType.GRID
                if (downloadsFolder) AppSettings.setDownloadedMusicViewType(next)
                else AppSettings.setLocalMusicViewType(next)
                actions.onDismissFolderMenu()
            },
            onDismiss = actions.onDismissFolderMenu,
        )
    }

    if (state.folderMenuOpen && state.route == AppRoute.RadioFavorites) {
        BackHandler { actions.onDismissFolderMenu() }
        FrostedRadioFavoritesSortMenu(
            hazeState = state.hazeState,
            selected = AppSettings.librarySort.collectAsStateWithLifecycle().value,
            onSelect = { option ->
                AppSettings.librarySort.value = option
                actions.onDismissFolderMenu()
            },
            onDismiss = actions.onDismissFolderMenu,
        )
    }

    if (state.showAccountSelector) {
        BackHandler { actions.onDismissAccountSelector() }
        AccountProfileSelector(
            accounts = state.googleAccounts,
            activeAccountId = state.activeAccountId,
            activeProfileId = state.activeProfileId,
            hazeState = state.hazeState,
            onSelect = { selected, profile -> viewModel.selectProfile(selected.accountId, profile.profileId) },
            onAddAccount = {
                actions.onDismissAccountSelector()
                actions.onWebSessionModeChange(WebSessionMode.SIGN_IN)
            },
            onRemoveAccount = { selected ->
                viewModel.removeAccount(selected.accountId)
                actions.onDismissAccountSelector()
            },
            onOpenSettings = {
                actions.onDismissAccountSelector()
                navigator.push(AppRoute.Settings)
            },
            onDismiss = actions.onDismissAccountSelector,
        )
    }

    if (state.showAppLanguage) {
        BackHandler { actions.onDismissAppLanguage() }
        AppLanguageDialog(
            hazeState = state.hazeState,
            onDismiss = actions.onDismissAppLanguage,
        )
    }

    if (state.showTranslationLanguage) {
        BackHandler { actions.onDismissTranslationLanguage() }
        TranslationLanguageDialog(
            hazeState = state.hazeState,
            onDismiss = actions.onDismissTranslationLanguage,
        )
    }

    if (state.showListenBrainzLogin) {
        var tokenInput by remember { mutableStateOf(state.listenBrainzToken) }
        ListenBrainzTokenAlert(
            hazeState = state.hazeState,
            tokenInput = tokenInput,
            onTokenInputChange = { tokenInput = it },
            onSave = {
                AppSettings.setListenBrainzToken(tokenInput.trim())
                actions.onDismissListenBrainzLogin()
            },
            onDismiss = actions.onDismissListenBrainzLogin,
        )
    }

    if (state.showLastfmLogin) {
        var usernameInput by remember { mutableStateOf("") }
        var passwordInput by remember { mutableStateOf("") }
        var lastfmError by remember { mutableStateOf<String?>(null) }
        var lastfmLoading by remember { mutableStateOf(false) }
        LastfmLoginAlert(
            hazeState = state.hazeState,
            usernameInput = usernameInput,
            onUsernameInputChange = { usernameInput = it },
            passwordInput = passwordInput,
            onPasswordInputChange = { passwordInput = it },
            error = lastfmError,
            loading = lastfmLoading,
            onSignIn = {
                lastfmLoading = true
                lastfmError = null
                scope.launch {
                    try {
                        LastFM.initialize(
                            apiKey = AppSettings.lastfmApiKey.value,
                            secret = AppSettings.lastfmSecret.value,
                        )
                        LastFM.getMobileSession(usernameInput.trim(), passwordInput)
                            .onSuccess { auth ->
                                AppSettings.setLastfmSessionKey(auth.session.key)
                                AppSettings.setLastfmUsername(auth.session.name)
                                AppSettings.setLastfmEnabled(true)
                                actions.onDismissLastfmLogin()
                            }
                            .onFailure { e ->
                                lastfmError = e.message ?: context.getString(R.string.login_failed)
                            }
                    } catch (e: Exception) {
                        lastfmError = e.message ?: context.getString(R.string.login_failed)
                    } finally {
                        lastfmLoading = false
                    }
                }
            },
            onDismiss = { if (!lastfmLoading) actions.onDismissLastfmLogin() },
        )
    }

    if (state.showWebDavEditor) {
        BackHandler { actions.onDismissWebDavEditor() }
        ServerEditorHost(
            hazeState = state.hazeState,
            title = stringResource(R.string.webdav),
            description = stringResource(R.string.webdav_description),
            fields = listOf(
                FieldConfig(
                    initial = AppSettings.webdavUrl.value,
                    placeholder = stringResource(R.string.webdav_server_url_hint),
                    keyboardType = KeyboardType.Uri,
                ),
                FieldConfig(
                    initial = AppSettings.webdavUsername.value,
                    placeholder = stringResource(R.string.username),
                ),
                FieldConfig(
                    initial = AppSettings.webdavPassword.value,
                    placeholder = stringResource(R.string.password),
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                ),
            ),
            canSubmit = { it[0].isNotBlank() },
            testFailedRes = R.string.webdav_test_failed,
            onTest = { (url, username, password) ->
                com.music.bitchord.data.webdav.WebDavRepository.testConnection(
                    url.trim(),
                    username.trim(),
                    password,
                )
            },
            onSave = { (url, username, password) ->
                AppSettings.setWebDavUrl(url.trim())
                AppSettings.setWebDavUsername(username.trim())
                AppSettings.setWebDavPassword(password)
                actions.onDismissWebDavEditor()
            },
            onDismiss = actions.onDismissWebDavEditor,
        )
    }

    if (state.showSmbEditor) {
        BackHandler { actions.onDismissSmbEditor() }
        ServerEditorHost(
            hazeState = state.hazeState,
            title = stringResource(R.string.smb),
            description = stringResource(R.string.smb_description),
            fields = listOf(
                FieldConfig(
                    initial = AppSettings.smbHost.value,
                    placeholder = stringResource(R.string.smb_server_hint),
                    keyboardType = KeyboardType.Uri,
                ),
                FieldConfig(
                    initial = AppSettings.smbShare.value,
                    placeholder = stringResource(R.string.smb_share_hint),
                ),
                FieldConfig(
                    initial = AppSettings.smbBasePath.value,
                    placeholder = stringResource(R.string.smb_folder_hint),
                ),
                FieldConfig(
                    initial = AppSettings.smbUsername.value,
                    placeholder = stringResource(R.string.username),
                ),
                FieldConfig(
                    initial = AppSettings.smbPassword.value,
                    placeholder = stringResource(R.string.password),
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                ),
            ),
            canSubmit = { it[0].isNotBlank() && it[1].isNotBlank() },
            testFailedRes = R.string.smb_test_failed,
            onTest = { (host, share, folder, username, password) ->
                com.music.bitchord.data.smb.SmbRepository.testConnection(
                    host.trim(),
                    share.trim(),
                    folder.trim(),
                    username.trim(),
                    password,
                )
            },
            onSave = { (host, share, folder, username, password) ->
                AppSettings.setSmbHost(host.trim())
                AppSettings.setSmbShare(share.trim())
                AppSettings.setSmbBasePath(folder.trim())
                AppSettings.setSmbUsername(username.trim())
                AppSettings.setSmbPassword(password)
                actions.onDismissSmbEditor()
            },
            onDismiss = actions.onDismissSmbEditor,
        )
    }

    val uploadConflict: com.music.bitchord.data.webdav.WebDavUploads.ConflictRequest? by com.music.bitchord.data.webdav.WebDavUploads.conflict.collectAsStateWithLifecycle()
    uploadConflict?.let { req: com.music.bitchord.data.webdav.WebDavUploads.ConflictRequest ->
        var applyToAll by remember(req) { mutableStateOf(false) }
        val answer: (com.music.bitchord.data.webdav.WebDavUploads.Choice) -> Unit = { choice ->
            req.answer.complete(
                com.music.bitchord.data.webdav.WebDavUploads.Resolution(choice, applyToAll),
            )
        }
        BackHandler { answer(com.music.bitchord.data.webdav.WebDavUploads.Choice.SKIP) }
        WebDavConflictAlert(
            hazeState = state.hazeState,
            fileName = req.fileName,
            showApplyToAll = req.remaining > 0,
            applyToAll = applyToAll,
            onApplyToAllChange = { applyToAll = it },
            onOverwrite = { answer(com.music.bitchord.data.webdav.WebDavUploads.Choice.OVERWRITE) },
            onKeepBoth = { answer(com.music.bitchord.data.webdav.WebDavUploads.Choice.KEEP_BOTH) },
            onSkip = { answer(com.music.bitchord.data.webdav.WebDavUploads.Choice.SKIP) },
            onDismiss = { answer(com.music.bitchord.data.webdav.WebDavUploads.Choice.SKIP) },
        )
    }

    if (state.showDiscordLogin) {
        BackHandler { actions.onDismissDiscordLogin() }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = actions.onDismissDiscordLogin) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Text(
                        "Sign in to Discord",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                DiscordLoginScreen(
                    onTokenCaptured = { token ->
                        AppSettings.setDiscordToken(token)
                        actions.onDismissDiscordLogin()
                    },
                )
            }
        }
    }

    if (state.showSpotifyLogin) {
        BackHandler { actions.onDismissSpotifyLogin() }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = actions.onDismissSpotifyLogin) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Text(
                        stringResource(R.string.spotify_sign_in),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                SpotifyLoginScreen(
                    modifier = Modifier.navigationBarsPadding(),
                    onConnected = { token ->
                        AppSettings.setSpotifySpdcToken(token)
                        actions.onDismissSpotifyLogin()
                    },
                )
            }
        }
    }

    if (state.showSpotifyCanvasAuth) {
        BackHandler { actions.onDismissSpotifyCanvasAuth() }
        SpotifyCanvasAuthScreen(
            onNavigateUp = actions.onDismissSpotifyCanvasAuth,
        )
    }

    state.discordDialog?.let { which ->
        DiscordDialogHost(
            which = which,
            hazeState = state.hazeState,
            onDismiss = actions.onDismissDiscordDialog,
        )
    }

    if (state.editingPartyServer) {
        PartyServerEditor(
            hazeState = state.hazeState,
            onDismiss = actions.onDismissPartyServer,
        )
    }

    if (state.editingSignatureApi) {
        SignatureApiEditor(
            hazeState = state.hazeState,
            onDismiss = actions.onDismissSignatureApi,
        )
    }

    state.editingSource?.let { config ->
        SourceEditorAlert(
            hazeState = state.hazeState,
            config = config,
            onDismiss = actions.onDismissEditingSource,
            onSaved = actions.onDismissEditingSource,
            onDelete = {
                SourceRegistry.remove(config.id)
                actions.onDismissEditingSource()
            },
            scope = scope,
        )
    }

    state.choosingSourceQuality?.let { config ->
        ChoiceAlert(
            hazeState = state.hazeState,
            title = stringResource(R.string.audio_quality),
            message = null,
            options = config.qualityOptions,
            selected = config.activeQuality,
            label = { it?.label.orEmpty() },
            onSelect = { option ->
                SourceRegistry.config(config.id)?.let {
                    SourceRegistry.update(it.copy(pickedQuality = option?.value))
                }
                actions.onDismissChoosingSourceQuality()
            },
            onDismiss = actions.onDismissChoosingSourceQuality,
        )
    }

    state.explainingOutdatedAddon?.let { config ->
        InfoAlert(
            hazeState = state.hazeState,
            title = config.displayName,
            description = stringResource(R.string.source_needs_update),
            onDismiss = actions.onDismissExplainingOutdatedAddon,
        )
    }

    if (state.confirmJioSaavn) {
        ConfirmationAlert(
            hazeState = state.hazeState,
            title = stringResource(R.string.enable_jiosaavn),
            description = stringResource(R.string.jiosaavn_mismatch_warning),
            confirmLabel = stringResource(R.string.enable_anyway),
            onConfirm = {
                SourceRegistry.configs.value
                    .firstOrNull { it.kind == SourceKind.JIOSAAVN }
                    ?.let { SourceRegistry.setEnabled(it.id, true) }
                actions.onDismissConfirmJioSaavn()
            },
            onDismiss = actions.onDismissConfirmJioSaavn,
        )
    }

    state.confirmBitPerfect?.let { mode ->
        ConfirmationAlert(
            hazeState = state.hazeState,
            title = stringResource(R.string.bit_perfect_confirm_title),
            description = stringResource(R.string.bit_perfect_confirm_message),
            confirmLabel = stringResource(R.string.enable),
            onConfirm = {
                AppSettings.setBitPerfectMode(mode)
                actions.onDismissConfirmBitPerfect()
            },
            onDismiss = actions.onDismissConfirmBitPerfect,
        )
    }

    if (state.confirmFixedVolume) {
        ConfirmationAlert(
            hazeState = state.hazeState,
            title = stringResource(R.string.usb_allow_fixed_volume_confirm_title),
            description = stringResource(R.string.usb_allow_fixed_volume_confirm_message),
            confirmLabel = stringResource(R.string.enable),
            onConfirm = {
                AppSettings.setUsbAllowFixedVolume(true)
                actions.onDismissConfirmFixedVolume()
            },
            onDismiss = actions.onDismissConfirmFixedVolume,
        )
    }

    DacVolumeHud(state.hazeState)

    state.blockedAddonSong?.let { song ->
        AddonBlockedAlert(
            hazeState = state.hazeState,
            song = song,
            onDismiss = actions.onDismissBlockedAddonSong,
        )
    }

    state.addonLeftOutOfDownload?.let { (addon, why) ->
        InfoAlert(
            hazeState = state.hazeState,
            title = stringResource(R.string.download_addon_left_out_title, addon),
            description = when (why) {
                SourceRegistry.DownloadExclusion.DOWNLOADS_NOT_ALLOWED ->
                    stringResource(R.string.download_addon_not_allowed, addon)
                SourceRegistry.DownloadExclusion.NEEDS_LOSSLESS_OUTPUT ->
                    stringResource(R.string.download_addon_needs_lossless_output, addon)
            },
            onDismiss = actions.onDismissAddonLeftOutOfDownload,
            inOwnWindow = state.playerRaised || state.songActions != null || state.browseActions != null,
        )
    }

    AboutWhatsNewOverlay(
        visible = state.showAboutWhatsNew,
        hazeState = state.hazeState,
        onDismiss = actions.onDismissAboutWhatsNew,
    )
}

@Composable
internal fun BitChordGlobalSheetsUi(
    state: GlobalSheetsHostState,
    actions: GlobalSheetsHostActions,
    controller: MediaController?,
    viewModel: MainViewModel,
    navigator: AppNavigator,
) {
    GlobalSheetsHost(
        state = state,
        actions = actions,
        controller = controller,
        viewModel = viewModel,
        navigator = navigator,
    )
}

