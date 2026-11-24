package com.music.bitchord.ui.screens

import com.music.bitchord.ui.icons.SfIcons
import android.content.Context
import androidx.annotation.StringRes
import android.content.Intent
import android.media.AudioFormat
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOff
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Gradient
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.MotionPhotosOff
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.WebAssetOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.Wifi
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import com.music.bitchord.data.lyrics.translationLanguageName
import com.music.bitchord.ui.components.CleanConfirmationAlert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import com.music.bitchord.ui.components.IosSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import com.music.bitchord.AppIcon
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import com.music.bitchord.ui.components.PillSegmentedControl
import com.music.bitchord.ui.components.isGlassSupported
import com.music.bitchord.ui.components.languageDisplayNameRes
import com.music.bitchord.ui.components.MessageState
import com.music.bitchord.ui.components.SearchField
import com.music.bitchord.ui.components.thumbnailBorder
import com.music.bitchord.ui.performance.resolvePerformanceRefreshRate
import com.music.bitchord.ui.performance.supportedPerformanceRefreshRates
import com.music.bitchord.data.model.Account
import com.music.bitchord.data.LocalMediaRepository
import com.music.bitchord.data.NerdStats
import com.music.bitchord.data.scrobbling.LastFM
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.OutputPcmMode
import com.music.bitchord.data.settings.BitPerfectMode
import com.music.bitchord.playback.AudioOutputStatus
import com.music.bitchord.playback.audio.bitperfect.BitPerfectOutput
import com.music.bitchord.playback.smart.AutomixModels
import com.music.bitchord.data.settings.AutomixPerformanceMode
import com.music.bitchord.R
import com.music.bitchord.data.sources.DeviceCodecs
import com.music.bitchord.data.settings.AudioQuality
import com.music.bitchord.data.settings.DownloadQuality
import com.music.bitchord.data.settings.ThemeMode
import com.music.bitchord.data.settings.AccentColor
import com.music.bitchord.data.settings.AppFont
import com.music.bitchord.data.settings.PlayerTheme
import com.music.bitchord.data.stats.Backup
import com.music.bitchord.playback.AudioCache
import com.music.bitchord.ui.player.fullBleedArtworkAvailable
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.util.Locale

/**
 * Grouped settings, in the shape phones have taught people to expect: inset
 * cards of rows, a leading glyph per row, the current value on the right, and a
 * plain-language footer under any group whose effect isn't obvious from its
 * title. Anything with more than two choices opens a sheet rather than pushing
 * a row of chips into the layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    /** The window's width, for the gates that depend on it. */
    windowWidth: Dp,
    signedIn: Boolean,
    account: Account?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onAccountScrobbling: () -> Unit,
    onEqualizer: () -> Unit,
    onOpenReplay: () -> Unit,
    /** Opens [DriveBackupScreen], under Backup and Restore. */
    onDriveBackup: () -> Unit = {},
    onLyricsSources: () -> Unit,
    onTranslationLanguage: () -> Unit,
    onSources: () -> Unit,
    onListenTogether: () -> Unit,
    onAppLanguage: () -> Unit,
    onEditSignatureApi: () -> Unit,
    /**
     * Asks before [mode] is turned on: it hands volume to the DAC at full
     * scale. Shown in the app's frosted alert, which needs the app's blur.
     */
    onConfirmBitPerfect: (BitPerfectMode) -> Unit,
    /** Asks before a DAC with no settable volume may play at full scale. */
    onConfirmFixedVolume: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    /** Optional focused category page; null shows the category index and global search. */
    section: SettingsCategory? = null,
    /** Retained for existing settings navigation routes. */
    onOpenSection: (SettingsCategory) -> Unit = {},
    /** Opens a choice with more options than its row can show, as a page. */
    onOpenChoice: (SettingsChoiceKind) -> Unit = {},
    /** Opens the full About page. */
    onOpenAbout: () -> Unit = {},
) {
    val context = LocalContext.current

    val wifiQuality by AppSettings.audioQualityWifi.collectAsStateWithLifecycle()
    val cellularQuality by AppSettings.audioQualityCellular.collectAsStateWithLifecycle()
    val metered by AppSettings.meteredConnection.collectAsStateWithLifecycle()
    val crossfade by AppSettings.crossfadeSeconds.collectAsStateWithLifecycle()
    val crossfadeOnSkip by AppSettings.crossfadeOnSkip.collectAsStateWithLifecycle()
    val smartFade by AppSettings.smartFadeEnabled.collectAsStateWithLifecycle()
    val automixModels by AutomixModels.state.collectAsStateWithLifecycle()
    val automixPerformance by AppSettings.automixPerformanceMode.collectAsStateWithLifecycle()
    val skipSilence by AppSettings.skipSilence.collectAsStateWithLifecycle()
    val dolbyAtmos by AppSettings.dolbyAtmos.collectAsStateWithLifecycle()
    val directSourceStream by AppSettings.directSourceStream.collectAsStateWithLifecycle()
    // A property of the hardware, so it is read once rather than remembered
    // against a key that can never change — see [DeviceCodecs.playsDolbyAtmos],
    // which caches the codec-list walk for the life of the process.
    val dolbyAtmosSupported = DeviceCodecs.playsDolbyAtmos
    val spatialAudio by AppSettings.spatialAudio.collectAsStateWithLifecycle()
    val nerdStats by AppSettings.showNerdStats.collectAsStateWithLifecycle()
    val listenTogetherButton by AppSettings.showListenTogetherButton.collectAsStateWithLifecycle()
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val disableBlendEffects by AppSettings.disableBlendEffects.collectAsStateWithLifecycle()
    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val glassPerformance by AppSettings.glassPerformance.collectAsStateWithLifecycle()
    val progressiveBlur by AppSettings.progressiveBlur.collectAsStateWithLifecycle()
    val liquidGlassSupported = isGlassSupported()
    val lyricsBlur by AppSettings.lyricsBlur.collectAsStateWithLifecycle()
    val coverSlide by AppSettings.coverSlideAnimation.collectAsStateWithLifecycle()
    val coverFluid by AppSettings.coverFluidAnimation.collectAsStateWithLifecycle()
    val signatureApiEnabled by AppSettings.signatureApiEnabled.collectAsStateWithLifecycle()
    val signatureApiUrl by AppSettings.signatureApiUrl.collectAsStateWithLifecycle()
    val fullBleedArtwork by AppSettings.fullBleedArtwork.collectAsStateWithLifecycle()
    val fullBleedDetailArtwork by AppSettings.fullBleedDetailArtwork.collectAsStateWithLifecycle()
    val playerTheme by AppSettings.playerTheme.collectAsStateWithLifecycle()
    val syncedLyrics by AppSettings.syncedLyrics.collectAsStateWithLifecycle()
    val lyricsSources by AppSettings.lyricsSources.collectAsStateWithLifecycle()
    val translationLanguage by AppSettings.translationLanguage.collectAsStateWithLifecycle()
    val theme by AppSettings.themeMode.collectAsStateWithLifecycle()
    val appFont by AppSettings.appFont.collectAsStateWithLifecycle()
    val accentColor by AppSettings.accentColor.collectAsStateWithLifecycle()
    val seenNewFeatures by AppSettings.seenNewFeatures.collectAsStateWithLifecycle()
    val coverAnimationMenuIsNew = AppSettings.FEATURE_COVER_ANIMATION_MENU !in seenNewFeatures
    val coverAnimSlideIsNew = AppSettings.FEATURE_COVER_ANIM_SLIDE !in seenNewFeatures
    val coverAnimFluidIsNew = AppSettings.FEATURE_COVER_ANIM_FLUID !in seenNewFeatures
    val coverAnimAutomixIsNew = AppSettings.FEATURE_COVER_ANIM_AUTOMIX !in seenNewFeatures
    val crossfadeOnNextIsNew = AppSettings.FEATURE_CROSSFADE_ON_NEXT !in seenNewFeatures
    val youtubePremiumIsNew = AppSettings.FEATURE_YOUTUBE_PREMIUM_AUDIO !in seenNewFeatures
    val downloadLikedSongs by AppSettings.downloadLikedSongs.collectAsStateWithLifecycle()
    val downloadLikedSongsIsNew = AppSettings.FEATURE_DOWNLOAD_LIKED_SONGS !in seenNewFeatures
    val bestPart by AppSettings.showBestPart.collectAsStateWithLifecycle()
    val bestPartIsNew = AppSettings.FEATURE_BEST_PART !in seenNewFeatures
    val sleepTimerFadeOut by AppSettings.sleepTimerFadeOut.collectAsStateWithLifecycle()
    val sleepTimerFadeOutIsNew = AppSettings.FEATURE_SLEEP_TIMER_FADE_OUT !in seenNewFeatures
    val albumGaplessPlayback by AppSettings.albumGaplessPlayback.collectAsStateWithLifecycle()
    val albumGaplessPlaybackIsNew = AppSettings.FEATURE_ALBUM_GAPLESS_PLAYBACK !in seenNewFeatures
    val automixModelIsNew = AppSettings.FEATURE_AUTOMIX_MODEL !in seenNewFeatures
    val driveBackupIsNew = AppSettings.FEATURE_DRIVE_BACKUP !in seenNewFeatures
    val showTotalDuration by AppSettings.showTotalDuration.collectAsStateWithLifecycle()
    val showTotalDurationIsNew = AppSettings.FEATURE_SHOW_TOTAL_DURATION !in seenNewFeatures
    val hideLyricsPreview by AppSettings.hideLyricsPreview.collectAsStateWithLifecycle()
    val hideLyricsPreviewIsNew = AppSettings.FEATURE_HIDE_LYRICS_PREVIEW !in seenNewFeatures
    val disableSpotifyCanvas by AppSettings.disableSpotifyCanvas.collectAsStateWithLifecycle()
    val disableSpotifyCanvasIsNew = AppSettings.FEATURE_DISABLE_SPOTIFY_CANVAS !in seenNewFeatures
    val showPlayerShuffleRepeatButtons by AppSettings.showPlayerShuffleRepeatButtons.collectAsStateWithLifecycle()
    val playerShuffleRepeatIsNew = AppSettings.FEATURE_PLAYER_SHUFFLE_REPEAT !in seenNewFeatures
    val playerLyricsIsNew = bestPartIsNew || coverAnimationMenuIsNew || coverAnimSlideIsNew || coverAnimFluidIsNew || coverAnimAutomixIsNew || showTotalDurationIsNew || hideLyricsPreviewIsNew || disableSpotifyCanvasIsNew || playerShuffleRepeatIsNew
    val audioPlaybackIsNew = sleepTimerFadeOutIsNew || crossfadeOnNextIsNew || automixModelIsNew || albumGaplessPlaybackIsNew
    val yourDataIsNew = driveBackupIsNew
    val accentIsDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val sessionId by AppSettings.audioSessionId.collectAsStateWithLifecycle()
    val outputPcmMode by AppSettings.outputPcmMode.collectAsStateWithLifecycle()
    val preferUsbDac by AppSettings.preferUsbDac.collectAsStateWithLifecycle()
    val bitPerfectMode by AppSettings.bitPerfectMode.collectAsStateWithLifecycle()
    val bitPerfectStatus by BitPerfectOutput.status.collectAsStateWithLifecycle()
    val bitPerfectReady by BitPerfectOutput.ready.collectAsStateWithLifecycle()
    val dacVolume by BitPerfectOutput.hardwareVolume.collectAsStateWithLifecycle()
    val usbAllowFixedVolume by AppSettings.usbAllowFixedVolume.collectAsStateWithLifecycle()
    val convertForLimitedDac by AppSettings.convertForLimitedDac.collectAsStateWithLifecycle()
    val loudnessNormalization by AppSettings.loudnessNormalization.collectAsStateWithLifecycle()
    val loudnessOffOnSpeaker by AppSettings.loudnessOffOnSpeaker.collectAsStateWithLifecycle()
    val outputStatus by AudioOutputStatus.current.collectAsStateWithLifecycle()
    val playingFormat by NerdStats.current.collectAsStateWithLifecycle()
    val playingDolbyAtmos = playingFormat?.isDolbyAtmos == true
    val cacheLimitBytes by AppSettings.audioCacheLimitBytes.collectAsStateWithLifecycle()
    val showCacheFolder by AppSettings.showCacheFolder.collectAsStateWithLifecycle()
    val downloadQuality by AppSettings.downloadQuality.collectAsStateWithLifecycle()
    val wifiOnlyDownloads by AppSettings.wifiOnlyDownloads.collectAsStateWithLifecycle()
    val exportDownloads by AppSettings.exportDownloads.collectAsStateWithLifecycle()
    val syncDownloadedPlaylists by AppSettings.syncDownloadedPlaylists.collectAsStateWithLifecycle()
    val stopOnTaskRemoved by AppSettings.stopOnTaskRemoved.collectAsStateWithLifecycle()
    val hideVolumeBar by AppSettings.hideVolumeBar.collectAsStateWithLifecycle()
    val hideStatusBarInPlayer by AppSettings.hideStatusBarInPlayer.collectAsStateWithLifecycle()
    val hideSongStatus by AppSettings.hideSongStatus.collectAsStateWithLifecycle()
    val showQueueHistory by AppSettings.showQueueHistory.collectAsStateWithLifecycle()
    val swipeToPlayNext by AppSettings.swipeToPlayNext.collectAsStateWithLifecycle()
    val dontRepeatSuggestions by AppSettings.dontRepeatSuggestions.collectAsStateWithLifecycle()
    val preferMusicOnly by AppSettings.preferMusicOnly.collectAsStateWithLifecycle()
    val skipUnplayableTracks by AppSettings.skipUnplayableTracks.collectAsStateWithLifecycle()
    val squareVideoThumbnails by AppSettings.squareVideoThumbnails.collectAsStateWithLifecycle()
    val hideRecents by AppSettings.hideRecents.collectAsStateWithLifecycle()
    val hideHomeVideos by AppSettings.hideHomeVideos.collectAsStateWithLifecycle()
    val hideTopPicks by AppSettings.hideTopPicks.collectAsStateWithLifecycle()
    val hideHomeMoods by AppSettings.hideHomeMoods.collectAsStateWithLifecycle()
    val smartVersionAlignment by AppSettings.smartVersionAlignment.collectAsStateWithLifecycle()
    val filterNonMusicAudio by AppSettings.filterNonMusicAudio.collectAsStateWithLifecycle()
    val localMusicFolderUri by AppSettings.localMusicFolderUri.collectAsStateWithLifecycle()
    val highPerformanceMode by AppSettings.highPerformanceMode.collectAsStateWithLifecycle()
    val performanceRefreshRate by AppSettings.performanceRefreshRate.collectAsStateWithLifecycle()
    val currentDisplay = LocalView.current.display
    val supportedRefreshRates = remember(currentDisplay) {
        currentDisplay.supportedPerformanceRefreshRates()
    }
    val selectedPerformanceRefreshRate = remember(currentDisplay, performanceRefreshRate) {
        currentDisplay.resolvePerformanceRefreshRate(performanceRefreshRate)
    }

    LaunchedEffect(selectedPerformanceRefreshRate, performanceRefreshRate) {
        if (selectedPerformanceRefreshRate != performanceRefreshRate) {
            AppSettings.setPerformanceRefreshRate(selectedPerformanceRefreshRate)
        }
    }

    // Scrobbling states
    val lastfmEnabled by AppSettings.lastfmEnabled.collectAsStateWithLifecycle()
    val lastfmUsername by AppSettings.lastfmUsername.collectAsStateWithLifecycle()
    val lastfmSessionKey by AppSettings.lastfmSessionKey.collectAsStateWithLifecycle()
    val lastfmScrobbleEnabled by AppSettings.lastfmScrobbleEnabled.collectAsStateWithLifecycle()
    val lastfmNowPlayingEnabled by AppSettings.lastfmNowPlaying.collectAsStateWithLifecycle()
    val scrobbleMinDuration by AppSettings.scrobbleMinDuration.collectAsStateWithLifecycle()
    val scrobbleDelayPercent by AppSettings.scrobbleDelayPercent.collectAsStateWithLifecycle()
    val scrobbleDelaySeconds by AppSettings.scrobbleDelaySeconds.collectAsStateWithLifecycle()
    val listenBrainzEnabled by AppSettings.listenBrainzEnabled.collectAsStateWithLifecycle()
    val listenBrainzToken by AppSettings.listenBrainzToken.collectAsStateWithLifecycle()

    val replayGenres by AppSettings.replayGenres.collectAsStateWithLifecycle()

    // Read here so the row can say "In a party · ABC123" rather than making
    // somebody open the screen to find out whether they are still in one.
    val party by ListenTogether.state.collectAsStateWithLifecycle()

    // Filters the rows below — see [SettingsSearch]. Blank shows everything,
    // exactly as if the field weren't there.
    // Saveable so a search survives a trip into one of the pages it found.
    var searchQuery by rememberSaveable { mutableStateOf("") }
    // What the last export or import did, shown on the row that did it rather
    // than as a toast: a backup is the one action here whose outcome nobody can
    // check by looking at the app afterwards. Held per direction, or an import's
    // result reports itself under the word "Export".
    var exportStatus by remember { mutableStateOf<String?>(null) }
    var importStatus by remember { mutableStateOf<String?>(null) }
    var confirmImport by remember { mutableStateOf(false) }
    var confirmClearSongCache by remember { mutableStateOf(false) }
    var confirmDeleteAutomixModel by remember { mutableStateOf(false) }
    var showPerformanceWarning by remember { mutableStateOf(false) }
    var showPerformanceConfirmation by remember { mutableStateOf(false) }
    val backupScope = rememberCoroutineScope()

    val batterySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        showPerformanceConfirmation = true
    }
    val localMusicFolderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { folder ->
        if (folder == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                folder,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        AppSettings.setLocalMusicFolderUri(folder.toString())
    }

    /**
     * Both halves go through the system document picker rather than a path of
     * this app's own choosing. That is what puts the file somewhere the user can
     * actually find it — Drive, Files, a folder they already back up — and it
     * means neither direction needs a storage permission, since the grant
     * arrives with the document they picked.
     */
    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { target ->
        if (target == null) return@rememberLauncherForActivityResult
        backupScope.launch {
            exportStatus = Backup.exportTo(context, target).fold(
                onSuccess = { months ->
                    context.getString(R.string.export_succeeded, context.countOfMonths(months))
                },
                onFailure = {
                    context.getString(R.string.export_failed, it.message ?: context.getString(R.string.unknown_error))
                },
            )
        }
    }
    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { source ->
        if (source == null) return@rememberLauncherForActivityResult
        backupScope.launch {
            importStatus = Backup.importFrom(context, source).fold(
                onSuccess = {
                    context.getString(R.string.import_succeeded, context.countOfMonths(it.months), it.from)
                },
                onFailure = {
                    context.getString(R.string.import_failed, it.message ?: context.getString(R.string.unknown_error))
                },
            )
        }
    }
    var showListenBrainzTokenDialog by remember { mutableStateOf(false) }
    var showLastfmLoginDialog by remember { mutableStateOf(false) }
    val scrobbleScope = rememberCoroutineScope()

    val version = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()?.substringBefore('-')?.removeSuffix(".0") ?: "1.3"
    }

    // Made fresh on every pass rather than remembered: it tallies the rows
    // that answered to the query, and that tally has to start from zero each
    // time the query changes.
    //
    // A category page shows all of its rows, so it searches for nothing. The
    // main page searches every category at once: typed into, it lays out the
    // rows that answer from all of them, as the single page Settings once was.
    val search = SettingsSearch(if (section == null) searchQuery else "", inPage = section != null)
    val searching = section == null && searchQuery.isNotBlank()
    val showPlayerLyricsInLegacyAppearance = searching || section == SettingsCategory.APPEARANCE
    fun shows(category: SettingsCategory) = when (section) {
        null -> searching
        SettingsCategory.DOWNLOADS -> category == SettingsCategory.DOWNLOADS ||
            category == SettingsCategory.LOCAL_MUSIC || category == SettingsCategory.CONTENT_PREFERENCES
        SettingsCategory.DOWNLOADS_LOCAL -> category == SettingsCategory.DOWNLOADS || category == SettingsCategory.LOCAL_MUSIC
        SettingsCategory.PERFORMANCE, SettingsCategory.LANGUAGE_PERFORMANCE -> category == SettingsCategory.PERFORMANCE || category == SettingsCategory.LANGUAGE
        SettingsCategory.PLAYBACK, SettingsCategory.AUDIO_REPRODUCTION -> category == SettingsCategory.PLAYBACK || category == SettingsCategory.MISCELLANEOUS
        SettingsCategory.PLAYER_LYRICS -> false
        SettingsCategory.APP_APPEARANCE -> category == SettingsCategory.APPEARANCE
        SettingsCategory.LIBRARY_CONTENT -> category == SettingsCategory.CONTENT_PREFERENCES
        SettingsCategory.STORAGE_CACHE -> category == SettingsCategory.STORAGE
        SettingsCategory.YOUR_DATA_REPLAY -> category == SettingsCategory.YOUR_DATA
        else -> section == category
    }

    // What is left after a keystroke is a different list, and the offset the
    // last one was scrolled to means nothing in it.
    val scrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    var isFirstSearchComposition by remember { mutableStateOf(true) }
    LaunchedEffect(searchQuery) {
        if (isFirstSearchComposition) {
            // Skip the initial composition — on Activity recreation, and on
            // coming back from a page opened here: rememberSaveable already
            // restored the scroll position and the query it belongs to.
            isFirstSearchComposition = false
        } else {
            isFirstSearchComposition = false
            scrollState.scrollTo(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        if (section == null) {
            SettingsPageTitle(stringResource(R.string.settings))
            SearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSubmit = {},
                placeholder = stringResource(R.string.settings_search_hint),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
                .verticalScroll(scrollState)
                .padding(bottom = contentPadding.calculateBottomPadding()),
        ) {
        val rootHome = section == null && !searching
        if (rootHome) {
            val accountTitle = stringResource(R.string.account_integrations)
            SettingsGroup(topSpacing = 12.dp) {
                val avatarUrl = account?.thumbnailUrl?.takeIf { signedIn }
                SettingsRow(
                    icon = SfIcons.Person,
                    iconContent = avatarUrl?.let { url ->
                        {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(ICON_SIZE).clip(CircleShape),
                            )
                        }
                    },
                    title = accountTitle,
                    subtitle = account?.email?.takeIf { it.isNotBlank() }
                        ?: stringResource(if (signedIn) R.string.signed_in else R.string.not_signed_in),
                    badge = stringResource(R.string.settings_badge_new).takeIf { youtubePremiumIsNew },
                    onClick = {
                        AppSettings.markFeatureSeen(AppSettings.FEATURE_YOUTUBE_PREMIUM_AUDIO)
                        onAccountScrobbling()
                    },
                )
            }

            SettingsGroup(topSpacing = 12.dp) {
                val listenTogetherTitle = stringResource(R.string.listen_together)
                SettingsRow(
                    icon = SfIcons.Groups,
                    title = listenTogetherTitle,
                    subtitle = party.code?.let {
                        stringResource(R.string.listen_together_in_party, it)
                    } ?: stringResource(R.string.listen_together_subtitle),
                    badge = party.members.size.takeIf { party.inParty && it > 1 }?.toString(),
                    onClick = onListenTogether,
                )
            }

            SettingsGroup(header = stringResource(R.string.settings_group_experience)) {
                SettingsRow(
                    iconPainter = painterResource(R.drawable.ic_settings_player_lyrics),
                    title = stringResource(R.string.settings_category_player_lyrics),
                    badge = stringResource(R.string.settings_badge_new).takeIf { playerLyricsIsNew },
                    onClick = { onOpenSection(SettingsCategory.PLAYER_LYRICS) },
                )
                RowDivider()
                SettingsRow(
                    icon = SfIcons.PlayCircle,
                    title = stringResource(R.string.settings_category_audio_playback),
                    badge = stringResource(R.string.settings_badge_new).takeIf { audioPlaybackIsNew },
                    onClick = { onOpenSection(SettingsCategory.AUDIO_REPRODUCTION) },
                )
                RowDivider()
                SettingsRow(
                    icon = SfIcons.Appearance,
                    title = stringResource(R.string.settings_category_appearance),
                    onClick = { onOpenSection(SettingsCategory.APP_APPEARANCE) },
                )
            }

            SettingsGroup(header = stringResource(R.string.settings_group_your_music)) {
                SettingsRow(
                    iconPainter = painterResource(R.drawable.ic_settings_library_content),
                    title = stringResource(R.string.settings_category_library_content),
                    onClick = { onOpenSection(SettingsCategory.LIBRARY_CONTENT) },
                )
                RowDivider()
                SettingsRow(
                    icon = SfIcons.Download,
                    title = stringResource(R.string.settings_category_downloads_local),
                    badge = stringResource(R.string.settings_badge_new).takeIf { downloadLikedSongsIsNew },
                    onClick = { onOpenSection(SettingsCategory.DOWNLOADS_LOCAL) },
                )
            }

            SettingsGroup(header = stringResource(R.string.settings_group_data_system)) {
                SettingsRow(
                    icon = SfIcons.Storage,
                    title = stringResource(R.string.settings_category_storage_cache),
                    onClick = { onOpenSection(SettingsCategory.STORAGE_CACHE) },
                )
                RowDivider()
                SettingsRow(
                    icon = SfIcons.YourData,
                    title = stringResource(R.string.settings_category_your_data_replay),
                    badge = stringResource(R.string.settings_badge_new).takeIf { yourDataIsNew },
                    onClick = { onOpenSection(SettingsCategory.YOUR_DATA_REPLAY) },
                )
                RowDivider()
                SettingsRow(
                    iconPainter = painterResource(R.drawable.ic_settings_language_performance),
                    title = stringResource(R.string.settings_category_language_performance),
                    onClick = { onOpenSection(SettingsCategory.LANGUAGE_PERFORMANCE) },
                )
            }

            SettingsGroup(header = stringResource(R.string.settings_group_about)) {
                SettingsRow(
                    iconPainter = painterResource(R.drawable.ic_settings_about),
                    title = stringResource(R.string.settings_category_about),
                    onClick = { onOpenAbout() },
                )
            }
        }
        if (section != null || searching) {
        if (section != null) SettingsPageTitle(stringResource(section.title))
        val audioSearchTerms = remember(context) {
            listOf(
                R.string.settings_category_audio_playback,
                R.string.audio_quality,
                R.string.source,
                R.string.direct_source_stream,
                R.string.on_wifi,
                R.string.on_mobile_data,
                R.string.dolby_atmos,
                R.string.output_precision,
                R.string.prefer_usb_dac,
                R.string.bit_perfect_output,
                R.string.dac_volume,
                R.string.usb_allow_fixed_volume,
                R.string.convert_for_limited_dac,
                R.string.equalizer,
                R.string.loudness_normalization,
                R.string.loudness_off_on_speaker,
                R.string.crossfade,
                R.string.crossfade_on_skip,
                R.string.automix,
                R.string.automix_performance,
                R.string.skip_silence,
                R.string.sleep_timer_fade_out,
                R.string.spatial_audio,
                R.string.smart_version_alignment,
                R.string.play_next_on_swipe,
                R.string.dont_repeat_songs,
                R.string.stop_music_on_close,
            ).map { context.getString(it) }
        }
        if (section == null && searching && audioSearchTerms.any {
                it.contains(searchQuery.trim(), ignoreCase = true)
            }
        ) {
            SettingsGroup(topSpacing = 12.dp) {
                SettingsRow(
                    icon = SfIcons.PlayCircle,
                    title = stringResource(R.string.settings_category_audio_playback),
                    badge = stringResource(R.string.settings_badge_new).takeIf { audioPlaybackIsNew },
                    onClick = { onOpenSection(SettingsCategory.AUDIO_REPRODUCTION) },
                )
            }
        }
        val appearanceSearchTerms = remember(context) {
            listOf(
                R.string.settings_category_appearance,
                R.string.theme,
                R.string.font,
                R.string.accent_color,
                R.string.app_icon,
                R.string.liquid_glass,
                R.string.glass_performance,
                R.string.progressive_blur,
                R.string.reduce_dynamic_blur,
                R.string.disable_blend_effects,
                R.string.reduce_animation,
                R.string.full_screen_detail_cover,
            ).map { context.getString(it) }
        }
        if (section == null && searching && appearanceSearchTerms.any {
                it.contains(searchQuery.trim(), ignoreCase = true)
            }
        ) {
            SettingsGroup(topSpacing = 12.dp) {
                SettingsRow(
                    icon = SfIcons.Appearance,
                    title = stringResource(R.string.settings_category_appearance),
                    onClick = { onOpenSection(SettingsCategory.APP_APPEARANCE) },
                )
            }
        }
        val libraryContentSearchTerms = remember(context) {
            listOf(
                R.string.settings_category_library_content,
                R.string.prefer_music_only,
                R.string.hide_home_videos,
                R.string.square_video_thumbnails,
                R.string.skip_unplayable_tracks,
                R.string.canvas_cover_settings,
                R.string.cover_animation,
                R.string.hide_recents,
                R.string.hide_top_picks,
                R.string.hide_home_moods,
            ).map { context.getString(it) }
        }
        if (section == null && searching && libraryContentSearchTerms.any {
                it.contains(searchQuery.trim(), ignoreCase = true)
            }
        ) {
            SettingsGroup(topSpacing = 12.dp) {
                SettingsRow(
                    icon = SfIcons.LibraryMusic,
                    title = stringResource(R.string.settings_category_library_content),
                    onClick = { onOpenSection(SettingsCategory.LIBRARY_CONTENT) },
                )
            }
        }
        val downloadsLocalSearchTerms = remember(context) {
            listOf(
                R.string.settings_category_downloads_local,
                R.string.download_quality,
                R.string.download_wifi_only,
                R.string.export_compatible_downloads,
                R.string.sync_downloaded_playlists,
                R.string.download_liked_songs,
                R.string.local_music_folder,
                R.string.use_all_audio_folders,
                R.string.filter_non_music_audio,
            ).map { context.getString(it) }
        }
        if (section == null && searching && downloadsLocalSearchTerms.any {
                it.contains(searchQuery.trim(), ignoreCase = true)
            }
        ) {
            SettingsGroup(topSpacing = 12.dp) {
                SettingsRow(
                    icon = SfIcons.Download,
                    title = stringResource(R.string.settings_category_downloads_local),
                    badge = stringResource(R.string.settings_badge_new).takeIf { downloadLikedSongsIsNew },
                    onClick = { onOpenSection(SettingsCategory.DOWNLOADS_LOCAL) },
                )
            }
        }
        val aboutSearchTerms = remember(context) {
            listOf(
                R.string.settings_category_about,
                R.string.about_updates,
                R.string.about_community,
                R.string.about_developers,
                R.string.about_help,
                R.string.about_whats_new,
                R.string.about_check_updates,
                R.string.about_auto_updates,
                R.string.about_source_code,
                R.string.about_community,
                R.string.about_support,
                R.string.about_star_github,
                R.string.about_source_code_subtitle,
                R.string.about_developer_subtitle,
                R.string.about_report_issue,
            ).map { context.getString(it) }
        }
        if (section == null && searching && aboutSearchTerms.any {
                it.contains(searchQuery.trim(), ignoreCase = true)
            }
        ) {
            SettingsGroup(topSpacing = 12.dp) {
                SettingsRow(
                    iconPainter = painterResource(R.drawable.ic_settings_about),
                    title = stringResource(R.string.settings_category_about),
                    onClick = { onOpenAbout() },
                )
            }
        }
        if (section == SettingsCategory.PLAYER_LYRICS) {
            SettingsGroup(header = stringResource(R.string.settings_group_player_controls)) {
                val playerThemeTitle = stringResource(R.string.player_theme)
                SettingsRow(
                    icon = SfIcons.Gradient,
                    title = playerThemeTitle,
                    subtitle = playerTheme.description(),
                )
                SegmentedControl(
                    options = PlayerTheme.entries.map { it.localizedLabel() },
                    selectedIndex = PlayerTheme.entries.indexOf(playerTheme),
                    onSelect = { AppSettings.setPlayerTheme(PlayerTheme.entries[it]) },
                    modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp),
                )
                if (fullBleedArtworkAvailable(windowWidth)) {
                    RowDivider()
                    SettingsRow(
                        icon = SfIcons.Fullscreen,
                        title = stringResource(R.string.full_screen_cover_art),
                        subtitle = stringResource(R.string.full_screen_cover_art_subtitle),
                        trailing = {
                            IosSwitch(checked = fullBleedArtwork, onCheckedChange = AppSettings::setFullBleedArtwork)
                        },
                        onClick = { AppSettings.setFullBleedArtwork(!fullBleedArtwork) },
                    )
                }
                RowDivider()
                SettingsRow(
                    icon = SfIcons.VolumeOff,
                    title = stringResource(R.string.hide_volume_bar),
                    subtitle = stringResource(R.string.hide_volume_bar_subtitle),
                    trailing = { IosSwitch(checked = hideVolumeBar, onCheckedChange = AppSettings::setHideVolumeBar) },
                    onClick = { AppSettings.setHideVolumeBar(!hideVolumeBar) },
                )
                RowDivider()
                SettingsRow(
                    icon = SfIcons.Groups,
                    title = stringResource(R.string.show_listen_together_button),
                    subtitle = stringResource(R.string.show_listen_together_button_subtitle),
                    trailing = { IosSwitch(checked = listenTogetherButton, onCheckedChange = AppSettings::setShowListenTogetherButton) },
                    onClick = { AppSettings.setShowListenTogetherButton(!listenTogetherButton) },
                )
                RowDivider()
                SettingsRow(
                    icon = SfIcons.WebAssetOff,
                    title = stringResource(R.string.hide_status_bar_in_player),
                    subtitle = stringResource(R.string.hide_status_bar_in_player_subtitle),
                    trailing = { IosSwitch(checked = hideStatusBarInPlayer, onCheckedChange = AppSettings::setHideStatusBarInPlayer) },
                    onClick = { AppSettings.setHideStatusBarInPlayer(!hideStatusBarInPlayer) },
                )
                RowDivider()
                SettingsRow(
                    icon = SfIcons.History,
                    title = stringResource(R.string.show_queue_history),
                    subtitle = stringResource(R.string.show_queue_history_subtitle),
                    trailing = { IosSwitch(checked = showQueueHistory, onCheckedChange = AppSettings::setShowQueueHistory) },
                    onClick = { AppSettings.setShowQueueHistory(!showQueueHistory) },
                )
                RowDivider()
                SettingsRow(
                    icon = SfIcons.VisibilityOff,
                    title = stringResource(R.string.hide_song_status),
                    subtitle = stringResource(R.string.hide_song_status_subtitle),
                    trailing = { IosSwitch(checked = hideSongStatus, onCheckedChange = AppSettings::setHideSongStatus) },
                    onClick = { AppSettings.setHideSongStatus(!hideSongStatus) },
                )
                RowDivider()
                val showTotalDurationTitle = stringResource(R.string.show_total_duration)
                SettingsRow(
                    icon = SfIcons.Clock,
                    title = showTotalDurationTitle,
                    subtitle = stringResource(R.string.show_total_duration_subtitle),
                    badge = stringResource(R.string.settings_badge_new).takeIf { showTotalDurationIsNew },
                    trailing = {
                        IosSwitch(
                            checked = showTotalDuration,
                            onCheckedChange = { checked ->
                                AppSettings.markFeatureSeen(AppSettings.FEATURE_SHOW_TOTAL_DURATION)
                                AppSettings.setShowTotalDuration(checked)
                            },
                        )
                    },
                    onClick = {
                        AppSettings.markFeatureSeen(AppSettings.FEATURE_SHOW_TOTAL_DURATION)
                        AppSettings.setShowTotalDuration(!showTotalDuration)
                    },
                )
                RowDivider()
                val showPlayerShuffleRepeatButtonsTitle = stringResource(R.string.show_player_shuffle_repeat_buttons)
                SettingsRow(
                    icon = SfIcons.Shuffle,
                    title = showPlayerShuffleRepeatButtonsTitle,
                    subtitle = stringResource(R.string.show_player_shuffle_repeat_buttons_subtitle),
                    badge = stringResource(R.string.settings_badge_new).takeIf { playerShuffleRepeatIsNew },
                    trailing = {
                        IosSwitch(
                            checked = showPlayerShuffleRepeatButtons,
                            onCheckedChange = { checked ->
                                AppSettings.markFeatureSeen(AppSettings.FEATURE_PLAYER_SHUFFLE_REPEAT)
                                AppSettings.setShowPlayerShuffleRepeatButtons(checked)
                            },
                        )
                    },
                    onClick = {
                        AppSettings.markFeatureSeen(AppSettings.FEATURE_PLAYER_SHUFFLE_REPEAT)
                        AppSettings.setShowPlayerShuffleRepeatButtons(!showPlayerShuffleRepeatButtons)
                    },
                )
                RowDivider()
                SettingsRow(
                    icon = SfIcons.BarChart,
                    title = stringResource(R.string.show_nerd_stats),
                    subtitle = stringResource(R.string.show_nerd_stats_subtitle),
                    trailing = { IosSwitch(checked = nerdStats, onCheckedChange = AppSettings::setShowNerdStats) },
                    onClick = { AppSettings.setShowNerdStats(!nerdStats) },
                )
                RowDivider()
                SettingsRow(
                    icon = SfIcons.MusicNote,
                    title = stringResource(R.string.show_best_part),
                    subtitle = stringResource(R.string.show_best_part_subtitle),
                    badge = stringResource(R.string.settings_badge_new).takeIf { bestPartIsNew },
                    trailing = {
                        IosSwitch(
                            checked = bestPart,
                            onCheckedChange = {
                                AppSettings.markFeatureSeen(AppSettings.FEATURE_BEST_PART)
                                AppSettings.setShowBestPart(it)
                            },
                        )
                    },
                    onClick = {
                        AppSettings.markFeatureSeen(AppSettings.FEATURE_BEST_PART)
                        AppSettings.setShowBestPart(!bestPart)
                    },
                )
            }

            SettingsGroup(header = stringResource(R.string.settings_group_cover_canvas)) {
                val coverAnimationValue = stringResource(
                    when {
                        !coverSlide -> R.string.cover_animation_disabled
                        coverFluid && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> R.string.cover_animation_fluid
                        else -> R.string.cover_animation_slide
                    },
                )
                SettingsRow(
                    icon = SfIcons.Animation,
                    title = stringResource(R.string.cover_animation),
                    value = coverAnimationValue,
                    badge = stringResource(R.string.settings_badge_new).takeIf { coverAnimationMenuIsNew },
                    onClick = {
                        AppSettings.markFeatureSeen(AppSettings.FEATURE_COVER_ANIMATION_MENU)
                        onOpenChoice(SettingsChoiceKind.COVER_ANIMATION)
                    },
                )
                RowDivider()
                SettingsRow(
                    icon = SfIcons.Draw,
                    title = stringResource(R.string.artist_signatures),
                    subtitle = stringResource(R.string.artist_signatures_subtitle),
                    trailing = { IosSwitch(checked = signatureApiEnabled, onCheckedChange = AppSettings::setSignatureApiEnabled) },
                    onClick = { AppSettings.setSignatureApiEnabled(!signatureApiEnabled) },
                )
                if (signatureApiEnabled) {
                    RowDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onEditSignatureApi)
                            .padding(start = ROW_INSET, end = ROW_INSET, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = signatureApiUrl.ifBlank { stringResource(R.string.artist_signatures_unconfigured) },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Chevron()
                    }
                }
                RowDivider()
                val disableSpotifyCanvasTitle = stringResource(R.string.disable_spotify_canvas)
                SettingsRow(
                    icon = SfIcons.MotionPhotosOff,
                    title = disableSpotifyCanvasTitle,
                    subtitle = stringResource(R.string.disable_spotify_canvas_subtitle),
                    badge = stringResource(R.string.settings_badge_new).takeIf { disableSpotifyCanvasIsNew },
                    trailing = {
                        IosSwitch(
                            checked = disableSpotifyCanvas,
                            onCheckedChange = { checked ->
                                AppSettings.markFeatureSeen(AppSettings.FEATURE_DISABLE_SPOTIFY_CANVAS)
                                AppSettings.setDisableSpotifyCanvas(checked)
                            },
                        )
                    },
                    onClick = {
                        AppSettings.markFeatureSeen(AppSettings.FEATURE_DISABLE_SPOTIFY_CANVAS)
                        AppSettings.setDisableSpotifyCanvas(!disableSpotifyCanvas)
                    },
                )
            }

            SettingsGroup(header = stringResource(R.string.settings_group_lyrics_translation)) {
                val lyricsHidden = hideLyricsPreview || !syncedLyrics
                val setLyricsHidden: (Boolean) -> Unit = { hide ->
                    AppSettings.markFeatureSeen(AppSettings.FEATURE_HIDE_LYRICS_PREVIEW)
                    AppSettings.setHideLyricsPreview(hide)
                    if (!hide && !syncedLyrics) AppSettings.setSyncedLyrics(true)
                }
                val hideLyricsTitle = stringResource(R.string.hide_lyrics_preview)
                SettingsRow(
                    icon = SfIcons.VisibilityOff,
                    title = hideLyricsTitle,
                    subtitle = stringResource(R.string.hide_lyrics_preview_subtitle),
                    badge = stringResource(R.string.settings_badge_new).takeIf { hideLyricsPreviewIsNew },
                    trailing = {
                        IosSwitch(
                            checked = lyricsHidden,
                            onCheckedChange = setLyricsHidden,
                        )
                    },
                    onClick = { setLyricsHidden(!lyricsHidden) },
                )
                RowDivider()
                SettingsRow(
                    icon = SfIcons.Notes,
                    title = stringResource(R.string.synced_lyrics),
                    subtitle = stringResource(R.string.synced_lyrics_subtitle),
                    trailing = { IosSwitch(checked = syncedLyrics, onCheckedChange = AppSettings::setSyncedLyrics) },
                    onClick = { AppSettings.setSyncedLyrics(!syncedLyrics) },
                )
                if (syncedLyrics) {
                    RowDivider()
                    SettingsRow(
                        icon = SfIcons.BlurOn,
                        title = stringResource(R.string.blur_unfocused_lyrics),
                        subtitle = stringResource(R.string.blur_unfocused_lyrics_subtitle),
                        trailing = { IosSwitch(checked = lyricsBlur, onCheckedChange = AppSettings::setLyricsBlur) },
                        onClick = { AppSettings.setLyricsBlur(!lyricsBlur) },
                    )
                    RowDivider()
                    SettingsRow(
                        icon = SfIcons.Language,
                        title = stringResource(R.string.lyrics_sources),
                        subtitle = lyricsSources.sortedBy { it.ordinal }.joinToString(", ") { it.label }
                            .ifEmpty { stringResource(R.string.no_lyrics_sources_enabled) },
                        trailing = { Chevron() },
                        onClick = onLyricsSources,
                    )
                    RowDivider()
                    val translationTitle = stringResource(R.string.translation_language)
                    SettingsRow(
                        icon = SfIcons.Translate,
                        title = translationTitle,
                        subtitle = if (translationLanguage.isBlank()) {
                            stringResource(R.string.translation_language_subtitle)
                        } else {
                            translationLanguageName(
                                translationLanguage,
                                AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault(),
                            )
                        },
                        trailing = { Chevron() },
                        onClick = onTranslationLanguage,
                    )
                }
            }
        }
        if (section == null && searching) {
            SettingsBlockHeader(stringResource(R.string.settings_group_account_connection), searchQuery.isBlank())
            SearchableSettingsGroup(search) {
                val accountTitle = stringResource(R.string.account_integrations)
                // What the row opens is the scrobbling screen, so the services it
                // signs into are worth typing at this field even though none of
                // them is named on the row itself.
                row(accountTitle, "scrobbling", "last.fm", "listenbrainz") {
                    val avatarUrl = account?.thumbnailUrl?.takeIf { signedIn }
                    SettingsRow(
                        icon = SfIcons.Person,
                        iconContent = avatarUrl?.let { url ->
                            {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(ICON_SIZE).clip(CircleShape),
                                )
                            }
                        },
                        title = accountTitle,
                        subtitle = account?.email?.takeIf { it.isNotBlank() }
                            ?: stringResource(if (signedIn) R.string.signed_in else R.string.not_signed_in),
                        onClick = onAccountScrobbling,
                    )
                }
                // Sits with the account rather than with Playback: a party is up to
                // five signed-in people, and being signed in is the whole of what
                // the row needs before it will do anything.
                val listenTogetherTitle = stringResource(R.string.listen_together)
                row(listenTogetherTitle, "jam", "party", "sync", "friends") {
                    SettingsRow(
                        icon = SfIcons.Groups,
                        title = listenTogetherTitle,
                        subtitle = party.code?.let {
                            stringResource(R.string.listen_together_in_party, it)
                        } ?: stringResource(R.string.listen_together_subtitle),
                        badge = party.members.size.takeIf { party.inParty && it > 1 }?.toString(),
                        onClick = onListenTogether,
                    )
                }
            }
        }

        if ((section == null && searching) || section == SettingsCategory.PLAYBACK || section == SettingsCategory.AUDIO_REPRODUCTION) {
            // The row that used to sit at the top of this group was called
            // "Lossless / HQ Audio" and toggled `SourceRegistry.setModuleEnabled` —
            // it switched the *module source* on and off, not lossless. Sources
            // above lists that as the module's own row now. Lossless itself is no
            // longer a setting at all — see
            // [SourceResolver.requestForNow][com.music.bitchord.data.sources.SourceResolver.requestForNow].
            if (section == null) SettingsBlockHeader(stringResource(R.string.settings_group_audio_playback), searchQuery.isBlank())
            SearchableSettingsGroup(
                search = search,
                header = stringResource(R.string.settings_group_audio_sources),
                searchAlias = stringResource(R.string.settings_category_audio_playback),
            ) {
                val sourceTitle = stringResource(R.string.source)
                val sourceSubtitle = stringResource(R.string.sources_subtitle)
                row(sourceTitle, sourceSubtitle, "addon", "lossless") {
                    SettingsRow(
                        icon = SfIcons.Extension,
                        title = sourceTitle,
                        subtitle = sourceSubtitle,
                        onClick = onSources,
                    )
                }
                // One line on purpose: it qualifies the row above rather than
                // being a setting of its own, so it reads as part of Sources.
                val directSourceTitle = stringResource(R.string.direct_source_stream)
                // Drawn like the animated-cover cellular line: no icon, no divider.
                row(directSourceTitle, "addon", "direct", "prefetch", "read-ahead", divided = false) {
                    // Off for the length of a party: holding every fetch until a
                    // track is about to play would leave this phone behind the
                    // others at each sync — see [DirectSourceGate.isActive].
                    SettingsSubRow(
                        title = directSourceTitle,
                        checked = directSourceStream && !party.inParty,
                        onCheckedChange = AppSettings::setDirectSourceStream,
                        subtitle = stringResource(R.string.direct_source_stream_in_party).takeIf { party.inParty },
                        enabled = !party.inParty,
                    )
                }
                val onWifiTitle = stringResource(R.string.on_wifi)
                row(onWifiTitle, "wi-fi", "streaming quality") {
                    SettingsRow(
                        icon = SfIcons.Wifi,
                        title = onWifiTitle,
                        badge = stringResource(R.string.in_use).takeIf { metered == false },
                        value = wifiQuality.localizedLabel(),
                        onClick = { onOpenChoice(SettingsChoiceKind.AUDIO_QUALITY_WIFI) },
                    )
                }
                val onMobileDataTitle = stringResource(R.string.on_mobile_data)
                row(onMobileDataTitle, "cellular", "streaming quality") {
                    SettingsRow(
                        icon = SfIcons.SignalCellularAlt,
                        title = onMobileDataTitle,
                        badge = stringResource(R.string.in_use).takeIf { metered == true },
                        value = cellularQuality.localizedLabel(),
                        onClick = { onOpenChoice(SettingsChoiceKind.AUDIO_QUALITY_CELLULAR) },
                    )
                }
                // Sits with the quality ceilings rather than with Playback: it
                // decides which version of a track gets fetched, the same question
                // the two rows above answer, and not how one is played back.
                //
                // Greyed rather than hidden where the device can't decode E-AC-3.
                // A missing row reads as a feature the app doesn't have; a disabled
                // one with a reason under it is the difference between "BitChord
                // has no Atmos" and "this phone has no Dolby decoder", and only the
                // second is true. The stored preference is left untouched either
                // way — see [AppSettings.dolbyAtmos].
                val dolbyAtmosTitle = stringResource(R.string.dolby_atmos)
                row(dolbyAtmosTitle, "surround", "e-ac-3") {
                    BypassedByBitPerfect(bitPerfectReady) {
                        SettingsRow(
                            iconPainter = painterResource(R.drawable.ic_dolby_atmos),
                            title = dolbyAtmosTitle,
                            subtitle = stringResource(
                                if (dolbyAtmosSupported) {
                                    R.string.dolby_atmos_subtitle
                                } else {
                                    R.string.dolby_atmos_unavailable
                                },
                            ),
                            enabled = dolbyAtmosSupported,
                            trailing = {
                                IosSwitch(
                                    checked = dolbyAtmos && dolbyAtmosSupported,
                                    onCheckedChange = AppSettings::setDolbyAtmos,
                                    enabled = dolbyAtmosSupported,
                                )
                            },
                            onClick = { AppSettings.setDolbyAtmos(!dolbyAtmos) },
                        )
                    }
                }
            }

        }

        // Its own group rather than rows bolted onto the two above, because a
        // download is not a third kind of connection. The ceilings answer "what
        // does this minute cost"; these answer "what am I keeping, and when may
        // it be fetched" — and those two questions only make sense read
        // together, which is what puts them side by side here.
        if (shows(SettingsCategory.DOWNLOADS)) SearchableSettingsGroup(search, header = stringResource(R.string.settings_group_downloads)) {
            val downloadQualityTitle = stringResource(R.string.download_quality)
            row(downloadQualityTitle, "offline", "lossless") {
                SettingsRow(
                    icon = SfIcons.Download,
                    title = downloadQualityTitle,
                    subtitle = stringResource(R.string.download_quality_subtitle, downloadQuality.perTrack),
                    value = downloadQuality.localizedLabel(),
                    onClick = { onOpenChoice(SettingsChoiceKind.DOWNLOAD_QUALITY) },
                )
            }
            // Reads as part of Download quality above it, not as a setting
            // of its own — same treatment as Play animated cover over
            // cellular gets under Animated cover art.
            val downloadWifiOnlyTitle = stringResource(R.string.download_wifi_only)
            row(downloadWifiOnlyTitle, "wi-fi", "cellular", divided = false) {
                SettingsSubRow(
                    title = downloadWifiOnlyTitle,
                    checked = wifiOnlyDownloads,
                    onCheckedChange = AppSettings::setWifiOnlyDownloads,
                    badge = stringResource(R.string.blocking).takeIf { wifiOnlyDownloads && metered == true },
                )
            }
            val exportDownloadsTitle = stringResource(R.string.export_compatible_downloads)
            row(exportDownloadsTitle, "music folder") {
                SettingsSubRow(
                    title = exportDownloadsTitle,
                    checked = exportDownloads,
                    onCheckedChange = AppSettings::setExportDownloads,
                    subtitle = "Music/SoundChord".takeIf { exportDownloads },
                )
            }
            val syncPlaylistsTitle = stringResource(R.string.sync_downloaded_playlists)
            row(syncPlaylistsTitle, "playlist", "new songs", "background", "automatic") {
                SettingsSubRow(
                    title = syncPlaylistsTitle,
                    checked = syncDownloadedPlaylists,
                    onCheckedChange = AppSettings::setSyncDownloadedPlaylists,
                    subtitle = stringResource(R.string.sync_downloaded_playlists_subtitle),
                )
            }
            val downloadLikedTitle = stringResource(R.string.download_liked_songs)
            row(downloadLikedTitle, "liked", "favorites", "offline", "automatic") {
                SettingsSubRow(
                    title = downloadLikedTitle,
                    checked = downloadLikedSongs,
                    badge = stringResource(R.string.settings_badge_new).takeIf { downloadLikedSongsIsNew },
                    onCheckedChange = {
                        AppSettings.markFeatureSeen(AppSettings.FEATURE_DOWNLOAD_LIKED_SONGS)
                        AppSettings.setDownloadLikedSongs(it)
                    },
                    subtitle = stringResource(R.string.download_liked_songs_subtitle),
                )
            }
        }

        if (shows(SettingsCategory.PLAYBACK)) SearchableSettingsGroup(
            search = search,
            header = stringResource(R.string.settings_group_audio_dac),
            searchAlias = stringResource(R.string.settings_category_audio_playback),
            footer = stringResource(R.string.bit_perfect_bypassed_footer).takeIf { bitPerfectReady },
        ) {
            val outputPrecisionTitle = stringResource(R.string.output_precision)
            row(outputPrecisionTitle, "pcm", "bit depth", "sample rate", "dac") {
                SettingsRow(
                    icon = SfIcons.GraphicEq,
                    title = outputPrecisionTitle,
                    subtitle = buildString {
                        append(outputStatus.sink)
                        append(" · ")
                        append(outputStatus.deviceName)
                        (outputStatus.actualSampleRateHz ?: outputStatus.sampleRatesHz.firstOrNull())
                            ?.let { append(" · ${it / 1000.0} kHz") }
                        append(" · ")
                        append(AudioOutputStatus.encodingLabel(outputStatus))
                    },
                )
                SegmentedControl(
                    options = OutputPcmMode.entries.map(OutputPcmMode::label),
                    selectedIndex = OutputPcmMode.entries.indexOf(outputPcmMode),
                    onSelect = { AppSettings.setOutputPcmMode(OutputPcmMode.entries[it]) },
                    modifier = Modifier.padding(start = TEXT_INSET, end = ROW_INSET, bottom = 14.dp),
                )
            }
            val preferUsbDacTitle = stringResource(R.string.prefer_usb_dac)
            row(preferUsbDacTitle, "headphone", "output") {
                SettingsSubRow(
                    title = preferUsbDacTitle,
                    checked = preferUsbDac,
                    onCheckedChange = AppSettings::setPreferUsbDac,
                    badge = stringResource(R.string.connected).takeIf { outputStatus.isUsb },
                )
            }
            val bitPerfectTitle = stringResource(R.string.bit_perfect_output)
            row(bitPerfectTitle, "usb", "dac", "exclusive", "hi-res", "bitperfect") {
                // The mode and the device it names can both be long, so they
                // go under the title rather than beside it, where they left
                // the title and status no room.
                SettingsRow(
                    icon = SfIcons.Usb,
                    title = bitPerfectTitle,
                    subtitleContent = {
                        Column {
                            Text(
                                text = bitPerfectSubtitle(bitPerfectMode, bitPerfectStatus),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = bitPerfectMode.localizedLabel(),
                                modifier = Modifier.padding(top = 2.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    onClick = { onOpenChoice(SettingsChoiceKind.BIT_PERFECT) },
                )
            }
            // The DAC's own attenuator: the only volume direct USB has, since
            // nothing upstream may scale a sample. It and the row after it
            // belong to Bit-perfect output, so neither is divided from it.
            dacVolume?.let { volume ->
                val dacVolumeTitle = stringResource(R.string.dac_volume)
                row(dacVolumeTitle, "volume", "usb", "dac", divided = false) {
                    SliderRow(
                        icon = SfIcons.VolumeUp,
                        title = dacVolumeTitle,
                        subtitle = stringResource(R.string.dac_volume_subtitle),
                        value = "%.1f dB".format(volume.currentDb),
                        sliderValue = volume.currentDb.toFloat(),
                        onSliderValue = { BitPerfectOutput.setHardwareVolume(it.toDouble()) },
                        valueRange = volume.minDb.toFloat()..volume.maxDb.toFloat(),
                        steps = 0,
                    )
                }
            }
            if (bitPerfectMode == BitPerfectMode.USB_DIRECT) {
                val fixedVolumeTitle = stringResource(R.string.usb_allow_fixed_volume)
                row(fixedVolumeTitle, "volume", "usb", "dac", divided = false) {
                    SettingsSubRow(
                        title = fixedVolumeTitle,
                        subtitle = stringResource(R.string.usb_allow_fixed_volume_subtitle),
                        checked = usbAllowFixedVolume,
                        onCheckedChange = { on ->
                            if (on) onConfirmFixedVolume() else AppSettings.setUsbAllowFixedVolume(false)
                        },
                    )
                }
            }
            if (bitPerfectMode != BitPerfectMode.OFF && bitPerfectMode.isSupported) {
                val convertTitle = stringResource(R.string.convert_for_limited_dac)
                row(convertTitle, "convert", "resample", "usb", "dac", "bitperfect", divided = false) {
                    SettingsSubRow(
                        title = convertTitle,
                        subtitle = stringResource(R.string.convert_for_limited_dac_subtitle),
                        checked = convertForLimitedDac,
                        onCheckedChange = AppSettings::setConvertForLimitedDac,
                    )
                }
            }
        }

        if (shows(SettingsCategory.PLAYBACK)) SearchableSettingsGroup(
            search = search,
            header = stringResource(R.string.settings_group_audio_effects),
            searchAlias = stringResource(R.string.settings_category_audio_playback),
        ) {
            val loudnessTitle = stringResource(R.string.loudness_normalization)
            row(loudnessTitle, "loudness", "volume", "normalize", "replaygain", "lufs") {
                BypassedByBitPerfect(bitPerfectReady) {
                    SettingsRow(
                        icon = SfIcons.VolumeUp,
                        title = loudnessTitle,
                        subtitle = stringResource(R.string.loudness_normalization_subtitle),
                        trailing = {
                            IosSwitch(
                                checked = loudnessNormalization,
                                onCheckedChange = AppSettings::setLoudnessNormalization,
                            )
                        },
                        onClick = { AppSettings.setLoudnessNormalization(!loudnessNormalization) },
                    )
                }
            }
            val loudnessSpeakerTitle = stringResource(R.string.loudness_off_on_speaker)
            row(loudnessSpeakerTitle, "loudness", "speaker", "normalize") {
                BypassedByBitPerfect(bitPerfectReady) {
                    SettingsSubRow(
                        title = loudnessSpeakerTitle,
                        checked = loudnessOffOnSpeaker,
                        onCheckedChange = AppSettings::setLoudnessOffOnSpeaker,
                    )
                }
            }
            // Automix decides its own length from each pair of tracks —
            // tempo, key, structure — so it replaces the manual slider rather
            // than needing it set to anything first.
            if (!smartFade) {
                val crossfadeTitle = stringResource(R.string.crossfade)
                row(crossfadeTitle, "fade", "gapless") {
                    BypassedByBitPerfect(bitPerfectReady) {
                        SliderRow(
                            icon = SfIcons.Waves,
                            title = crossfadeTitle,
                            subtitle = stringResource(R.string.crossfade_subtitle),
                            value = if (crossfade == 0) stringResource(R.string.off) else "${crossfade}s",
                            sliderValue = crossfade.toFloat(),
                            onSliderValue = { AppSettings.setCrossfadeSeconds(it.roundToInt()) },
                            valueRange = 0f..12f,
                            steps = 11,
                        )
                    }
                }
            }
            val crossfadeOnSkipTitle = stringResource(R.string.crossfade_on_skip)
            row(crossfadeOnSkipTitle, "crossfade", "skip", "next", "previous") {
                BypassedByBitPerfect(bitPerfectReady) {
                    SettingsRow(
                        icon = SfIcons.Waves,
                        title = crossfadeOnSkipTitle,
                        subtitle = stringResource(R.string.crossfade_on_skip_subtitle),
                        trailing = {
                            IosSwitch(
                                checked = crossfadeOnSkip,
                                onCheckedChange = {
                                    AppSettings.markFeatureSeen(AppSettings.FEATURE_CROSSFADE_ON_NEXT)
                                    AppSettings.setCrossfadeOnSkip(it)
                                },
                            )
                        },
                        badge = stringResource(R.string.settings_badge_new).takeIf { crossfadeOnNextIsNew },
                        onClick = {
                            AppSettings.markFeatureSeen(AppSettings.FEATURE_CROSSFADE_ON_NEXT)
                            AppSettings.setCrossfadeOnSkip(!crossfadeOnSkip)
                        },
                    )
                }
            }
            val automixTitle = stringResource(R.string.automix)
            row(automixTitle, "crossfade", "smart fade", "mix") {
                BypassedByBitPerfect(bitPerfectReady) {
                    SettingsRow(
                        iconPainter = painterResource(R.drawable.automix_icon),
                        title = automixTitle,
                        subtitle = stringResource(
                            if (smartFade) R.string.automix_enabled_subtitle else R.string.automix_disabled_subtitle,
                        ),
                        trailing = {
                            IosSwitch(
                                checked = smartFade,
                                onCheckedChange = { on ->
                                    if (on) AppSettings.markFeatureSeen(AppSettings.FEATURE_AUTOMIX_MODEL)
                                    AppSettings.setSmartFadeEnabled(on)
                                },
                            )
                        },
                        onClick = {
                            val next = !smartFade
                            if (next) AppSettings.markFeatureSeen(AppSettings.FEATURE_AUTOMIX_MODEL)
                            AppSettings.setSmartFadeEnabled(next)
                        },
                    )
                }
            }
            val albumGaplessTitle = stringResource(R.string.album_gapless_playback)
            row(albumGaplessTitle, "album", "gapless", "automix", "crossfade") {
                BypassedByBitPerfect(bitPerfectReady) {
                    SettingsRow(
                        icon = SfIcons.Album,
                        title = albumGaplessTitle,
                        subtitle = stringResource(R.string.album_gapless_playback_subtitle),
                        badge = stringResource(R.string.settings_badge_new).takeIf { albumGaplessPlaybackIsNew },
                        trailing = {
                            IosSwitch(
                                checked = albumGaplessPlayback,
                                onCheckedChange = { on ->
                                    AppSettings.markFeatureSeen(AppSettings.FEATURE_ALBUM_GAPLESS_PLAYBACK)
                                    AppSettings.setAlbumGaplessPlayback(on)
                                },
                            )
                        },
                        onClick = {
                            AppSettings.markFeatureSeen(AppSettings.FEATURE_ALBUM_GAPLESS_PLAYBACK)
                            AppSettings.setAlbumGaplessPlayback(!albumGaplessPlayback)
                        },
                    )
                }
            }
            // The weights Automix analyses with live outside the APK (see
            // AutomixModels): switching Automix on fetches them by itself,
            // and this row lets them be fetched ahead of time, retried after
            // a failure, or removed to free the space.
            val automixModelTitle = stringResource(R.string.automix_model)
            row(automixModelTitle, "automix", "model", "download", "storage", "free space") {
                val modelMb = (AutomixModels.totalBytes / 1_000_000).toInt()
                val badgeNew = stringResource(R.string.settings_badge_new).takeIf { automixModelIsNew }
                when (val model = automixModels) {
                    is AutomixModels.State.Downloading -> SettingsRow(
                        icon = SfIcons.Download,
                        title = automixModelTitle,
                        badge = badgeNew,
                        subtitle = stringResource(
                            R.string.automix_models_downloading,
                            modelMb,
                            (model.fraction * 100).toInt(),
                        ),
                    )
                    AutomixModels.State.Failed -> SettingsRow(
                        icon = SfIcons.Download,
                        title = automixModelTitle,
                        badge = badgeNew,
                        subtitleContent = {
                            SubtitleWithAction(
                                stringResource(R.string.automix_models_failed),
                                stringResource(R.string.retry),
                            )
                        },
                        onClick = {
                            AppSettings.markFeatureSeen(AppSettings.FEATURE_AUTOMIX_MODEL)
                            AutomixModels.download()
                        },
                        trailing = {},
                    )
                    AutomixModels.State.Ready -> SettingsRow(
                        icon = SfIcons.Downloaded,
                        title = automixModelTitle,
                        badge = badgeNew,
                        subtitleContent = {
                            SubtitleWithAction(
                                stringResource(R.string.automix_model_ready, modelMb),
                                stringResource(R.string.delete),
                                actionColor = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            AppSettings.markFeatureSeen(AppSettings.FEATURE_AUTOMIX_MODEL)
                            confirmDeleteAutomixModel = true
                        },
                        trailing = {},
                    )
                    AutomixModels.State.Missing -> SettingsRow(
                        icon = SfIcons.Download,
                        title = automixModelTitle,
                        badge = badgeNew,
                        subtitleContent = {
                            SubtitleWithAction(
                                stringResource(R.string.automix_model_missing),
                                stringResource(R.string.automix_model_download, modelMb),
                            )
                        },
                        onClick = {
                            AppSettings.markFeatureSeen(AppSettings.FEATURE_AUTOMIX_MODEL)
                            AutomixModels.download()
                        },
                        trailing = {},
                    )
                }
            }
            val automixPerformanceTitle = stringResource(R.string.automix_performance)
            row(automixPerformanceTitle, "cpu", "battery") {
                BypassedByBitPerfect(bitPerfectReady) {
                    SettingsRow(
                        icon = SfIcons.Tune,
                        title = automixPerformanceTitle,
                        subtitle = stringResource(R.string.automix_performance_subtitle),
                        value = automixPerformance.localizedLabel(),
                        onClick = { onOpenChoice(SettingsChoiceKind.AUTOMIX_PERFORMANCE) },
                    )
                }
            }
            val skipSilenceTitle = stringResource(R.string.skip_silence)
            row(skipSilenceTitle, "silence") {
                BypassedByBitPerfect(bitPerfectReady) {
                    SettingsRow(
                        icon = SfIcons.VolumeOff,
                        title = skipSilenceTitle,
                        // Silence skipping is Media3's own processor, and
                        // `DefaultAudioSink.configure` appends that processor list
                        // only on its 16-bit branch — the float branch gets the
                        // format converter and nothing else. So on a float track
                        // this switch really does nothing, and the row says so
                        // rather than letting someone toggle it and wonder. The
                        // equaliser is unaffected: it runs upstream of that sink.
                        subtitle = if (outputStatus.actualEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                            stringResource(R.string.skip_silence_float_subtitle)
                        } else {
                            stringResource(R.string.skip_silence_subtitle)
                        },
                        trailing = {
                            IosSwitch(
                                checked = skipSilence,
                                onCheckedChange = AppSettings::setSkipSilence,
                            )
                        },
                        onClick = { AppSettings.setSkipSilence(!skipSilence) },
                    )
                }
            }
            val sleepTimerFadeOutTitle = stringResource(R.string.sleep_timer_fade_out)
            row(sleepTimerFadeOutTitle, "sleep", "timer", "fade") {
                SettingsRow(
                    icon = SfIcons.SleepTimer,
                    title = sleepTimerFadeOutTitle,
                    subtitle = stringResource(R.string.sleep_timer_fade_out_subtitle),
                    badge = stringResource(R.string.settings_badge_new).takeIf { sleepTimerFadeOutIsNew },
                    trailing = {
                        IosSwitch(
                            checked = sleepTimerFadeOut,
                            onCheckedChange = {
                                AppSettings.markFeatureSeen(AppSettings.FEATURE_SLEEP_TIMER_FADE_OUT)
                                AppSettings.setSleepTimerFadeOut(it)
                            },
                        )
                    },
                    onClick = {
                        AppSettings.markFeatureSeen(AppSettings.FEATURE_SLEEP_TIMER_FADE_OUT)
                        AppSettings.setSleepTimerFadeOut(!sleepTimerFadeOut)
                    },
                )
            }
            val spatialAudioTitle = stringResource(R.string.spatial_audio)
            row(spatialAudioTitle, "surround", "3d") {
                BypassedByBitPerfect(bitPerfectReady) {
                    SettingsRow(
                        icon = SfIcons.SurroundSound,
                        title = spatialAudioTitle,
                        // Widening a JOC stream would fight the object-based mix
                        // Dolby has already spatialized, so the service holds this
                        // off for the duration of an Atmos track — see
                        // `PlaybackService.applySpatialAudioEnabled`. Said on the
                        // row because the switch stays where the listener left it,
                        // and a switch reading "on" over an effect that is not
                        // running is the same silent lie the equaliser screen used
                        // to tell.
                        subtitle = if (playingDolbyAtmos) {
                            stringResource(R.string.spatial_audio_atmos_subtitle)
                        } else {
                            stringResource(R.string.spatial_audio_subtitle)
                        },
                        trailing = {
                            IosSwitch(
                                checked = spatialAudio,
                                onCheckedChange = AppSettings::setSpatialAudio,
                            )
                        },
                        onClick = { AppSettings.setSpatialAudio(!spatialAudio) },
                    )
                }
            }
            // The system panel is not listed here as well. A device with a
            // Dolby or Dirac panel has something BitChord cannot reproduce and
            // keeps its row — but one level in, at the foot of the equaliser
            // screen, rather than as a second equaliser entry alongside ours.
            val equalizerTitle = stringResource(R.string.equalizer)
            row(equalizerTitle, "eq", "bass", "treble") {
                BypassedByBitPerfect(bitPerfectReady) {
                    SettingsRow(
                        icon = SfIcons.Tune,
                        title = equalizerTitle,
                        subtitle = stringResource(R.string.equalizer_subtitle),
                        onClick = onEqualizer,
                    )
                }
            }
        }

        if (shows(SettingsCategory.LOCAL_MUSIC)) SearchableSettingsGroup(search, header = stringResource(R.string.settings_group_local_music)) {
            val localMusicFolderTitle = stringResource(R.string.local_music_folder)
            row(localMusicFolderTitle, "folder", "offline", "files") {
                SettingsRow(
                    icon = SfIcons.Folder,
                    title = localMusicFolderTitle,
                    subtitle = LocalMediaRepository.selectedFolderLabel(localMusicFolderUri)
                        ?: stringResource(R.string.all_audio_folders),
                    onClick = { localMusicFolderPicker.launch(null) },
                )
            }
            if (localMusicFolderUri.isNotBlank()) {
                val useAllAudioFoldersTitle = stringResource(R.string.use_all_audio_folders)
                row(useAllAudioFoldersTitle, "folder") {
                    SettingsRow(
                        icon = SfIcons.LibraryMusic,
                        title = useAllAudioFoldersTitle,
                        subtitle = stringResource(R.string.use_all_audio_folders_subtitle),
                        onClick = { AppSettings.setLocalMusicFolderUri("") },
                    )
                }
            }
            val filterNonMusicAudioTitle = stringResource(R.string.filter_non_music_audio)
            row(filterNonMusicAudioTitle, "podcast", "recording") {
                SettingsRow(
                    icon = SfIcons.FilterAlt,
                    title = filterNonMusicAudioTitle,
                    subtitle = stringResource(R.string.filter_non_music_audio_subtitle),
                    trailing = {
                        IosSwitch(
                            checked = filterNonMusicAudio,
                            onCheckedChange = AppSettings::setFilterNonMusicAudio,
                        )
                    },
                    onClick = { AppSettings.setFilterNonMusicAudio(!filterNonMusicAudio) },
                )
            }
        }

        if (section == null) SettingsBlockHeader(stringResource(R.string.settings_group_visual_multimedia), searchQuery.isBlank())
        if (shows(SettingsCategory.CONTENT_PREFERENCES)) SearchableSettingsGroup(search, header = stringResource(R.string.settings_group_multimedia_display)) {
            val preferMusicOnlyTitle = stringResource(R.string.prefer_music_only)
            row(preferMusicOnlyTitle, "video", "audio", "music video", "convert", "song") {
                SettingsRow(
                    icon = SfIcons.Video,
                    title = preferMusicOnlyTitle,
                    subtitle = stringResource(R.string.prefer_music_only_subtitle),
                    trailing = {
                        IosSwitch(
                            checked = preferMusicOnly,
                            onCheckedChange = AppSettings::setPreferMusicOnly,
                        )
                    },
                    onClick = { AppSettings.setPreferMusicOnly(!preferMusicOnly) },
                )
            }
            val hideHomeVideosTitle = stringResource(R.string.hide_home_videos)
            row(hideHomeVideosTitle, "video", "music video", "feed", "home") {
                SettingsRow(
                    icon = SfIcons.VisibilityOff,
                    title = hideHomeVideosTitle,
                    subtitle = stringResource(R.string.hide_home_videos_subtitle),
                    trailing = {
                        IosSwitch(
                            checked = hideHomeVideos,
                            onCheckedChange = AppSettings::setHideHomeVideos,
                        )
                    },
                    onClick = { AppSettings.setHideHomeVideos(!hideHomeVideos) },
                )
            }
            val squareVideoThumbnailsTitle = stringResource(R.string.square_video_thumbnails)
            row(squareVideoThumbnailsTitle, "video", "thumbnail", "cover", "artwork", "player") {
                SettingsRow(
                    icon = SfIcons.Apps,
                    title = squareVideoThumbnailsTitle,
                    subtitle = stringResource(R.string.square_video_thumbnails_subtitle),
                    trailing = {
                        IosSwitch(
                            checked = squareVideoThumbnails,
                            onCheckedChange = AppSettings::setSquareVideoThumbnails,
                        )
                    },
                    onClick = { AppSettings.setSquareVideoThumbnails(!squareVideoThumbnails) },
                )
            }
            val skipUnplayableTitle = stringResource(R.string.skip_unplayable_tracks)
            row(skipUnplayableTitle, "unplayable", "unavailable", "source", "skip", "queue") {
                SettingsRow(
                    icon = SfIcons.PlayNext,
                    title = skipUnplayableTitle,
                    subtitle = stringResource(R.string.skip_unplayable_tracks_subtitle),
                    trailing = {
                        IosSwitch(
                            checked = skipUnplayableTracks,
                            onCheckedChange = {
                                AppSettings.setSkipUnplayableTracks(it)
                            },
                        )
                    },
                    onClick = {
                        AppSettings.setSkipUnplayableTracks(!skipUnplayableTracks)
                    },
                )
            }
            val canvasSetupTitle = stringResource(R.string.canvas_cover_settings)
            row(canvasSetupTitle, "canvas", "animated cover", "spotify", "cover", "video") {
                SettingsRow(
                    icon = SfIcons.Animation,
                    title = canvasSetupTitle,
                    subtitle = stringResource(R.string.spotify_canvas_settings_subtitle),
                    onClick = { onOpenChoice(SettingsChoiceKind.COVER_ANIMATION) },
                )
            }
        }

        if (shows(SettingsCategory.CONTENT_PREFERENCES)) SearchableSettingsGroup(search, header = stringResource(R.string.settings_group_home_screen)) {
            val hideRecentsTitle = stringResource(R.string.hide_recents)
            row(hideRecentsTitle, "recents", "recent", "history", "feed", "home") {
                SettingsRow(
                    icon = SfIcons.VisibilityOff,
                    title = hideRecentsTitle,
                    trailing = {
                        IosSwitch(
                            checked = hideRecents,
                            onCheckedChange = AppSettings::setHideRecents,
                        )
                    },
                    onClick = { AppSettings.setHideRecents(!hideRecents) },
                )
            }
            val hideTopPicksTitle = stringResource(R.string.hide_top_picks)
            row(hideTopPicksTitle, "top picks", "recommendations", "feed", "home") {
                SettingsRow(
                    icon = SfIcons.AutoAwesome,
                    title = hideTopPicksTitle,
                    trailing = {
                        IosSwitch(
                            checked = hideTopPicks,
                            onCheckedChange = AppSettings::setHideTopPicks,
                        )
                    },
                    onClick = { AppSettings.setHideTopPicks(!hideTopPicks) },
                )
            }
            val hideHomeMoodsTitle = stringResource(R.string.hide_home_moods)
            row(hideHomeMoodsTitle, "mood", "moods", "find your mood", "feed", "home") {
                SettingsRow(
                    icon = SfIcons.Star,
                    title = hideHomeMoodsTitle,
                    trailing = {
                        IosSwitch(
                            checked = hideHomeMoods,
                            onCheckedChange = AppSettings::setHideHomeMoods,
                        )
                    },
                    onClick = { AppSettings.setHideHomeMoods(!hideHomeMoods) },
                )
            }
        }

        if (section == null) SettingsBlockHeader(stringResource(R.string.settings_group_appearance_performance), searchQuery.isBlank())
        if (shows(SettingsCategory.APPEARANCE)) SearchableSettingsGroup(search, header = stringResource(R.string.settings_group_theme_icon)) {
            val themeTitle = stringResource(R.string.theme)
            row(themeTitle, "dark mode", "light mode") {
                SettingsRow(icon = Icons.Rounded.Brightness4, title = themeTitle)
                SegmentedControl(
                    options = ThemeMode.entries.map { it.localizedLabel() },
                    selectedIndex = ThemeMode.entries.indexOf(theme),
                    onSelect = { AppSettings.setThemeMode(ThemeMode.entries[it]) },
                    modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp),
                )
            }
            val fontTitle = stringResource(R.string.font)
            row(fontTitle, "typeface", "sf pro", "legacy") {
                SettingsRow(
                    icon = SfIcons.Font,
                    title = fontTitle,
                    subtitle = appFont.description(),
                    value = appFont.localizedLabel(),
                    onClick = { onOpenChoice(SettingsChoiceKind.FONT) },
                )
            }
            val accentTitle = stringResource(R.string.accent_color)
            row(accentTitle, "accent", "colour", "color", "theme") {
                SettingsRow(
                    icon = SfIcons.BlurOn,
                    title = accentTitle,
                    onClick = {
                        onOpenChoice(SettingsChoiceKind.ACCENT)
                    },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = accentColor.localizedLabel(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                            )
                            Spacer(Modifier.width(5.dp))
                            Box(
                                Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(accentColor.colorFor(accentIsDark)))
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f), CircleShape),
                            )
                            Spacer(Modifier.width(2.dp))
                            Chevron()
                        }
                    },
                )
            }
            val appIconTitle = stringResource(R.string.app_icon)
            row(appIconTitle, "icon", "launcher", "logo") {
                var selectedIcon by remember { mutableStateOf(AppIcon.current(context)) }
                SettingsRow(icon = SfIcons.Apps, title = appIconTitle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AppIcon.entries.forEach { option ->
                        val selected = option == selectedIcon
                        Column(
                            modifier = Modifier
                                .width(74.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedIcon = option
                                    when (option) {
                                        AppIcon.Monochrome -> AppSettings.markFeatureSeen(AppSettings.FEATURE_ICON_MONOCHROME)
                                        AppIcon.ClassicRed -> AppSettings.markFeatureSeen(AppSettings.FEATURE_ICON_RED)
                                        AppIcon.SpotifyGreen -> AppSettings.markFeatureSeen(AppSettings.FEATURE_ICON_GREEN)
                                        AppIcon.Default -> Unit
                                    }
                                    AppIcon.apply(context, option)
                                }
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // The preview matches the launcher's framed icon scale.
                            val preview = option.previewRes
                            Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                                if (preview == null) {
                                    AndroidView(
                                        factory = { ctx ->
                                            android.widget.ImageView(ctx).apply { setImageResource(option.iconRes) }
                                        },
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(20.dp)),
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(preview),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .scale(1.5f),
                                    )
                                }
                                val featureKey = when (option) {
                                    AppIcon.Monochrome -> AppSettings.FEATURE_ICON_MONOCHROME
                                    AppIcon.ClassicRed -> AppSettings.FEATURE_ICON_RED
                                    AppIcon.SpotifyGreen -> AppSettings.FEATURE_ICON_GREEN
                                    AppIcon.Default -> null
                                }
                                if (featureKey != null && featureKey !in seenNewFeatures) {
                                    Box(Modifier.align(Alignment.TopEnd).padding(2.dp)) {
                                        AppIconNewBadge(stringResource(R.string.settings_badge_new))
                                    }
                                }
                            }
                            Text(
                                text = stringResource(option.labelRes),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }
        }

        if (shows(SettingsCategory.APPEARANCE)) SearchableSettingsGroup(
            search = search,
            header = stringResource(R.string.settings_group_glass_effects),
        ) {
            val reduceAnimationTitle = stringResource(R.string.reduce_animation)
            row(reduceAnimationTitle, "motion") {
                SettingsRow(
                    icon = SfIcons.MotionPhotosOff,
                    title = reduceAnimationTitle,
                    subtitle = stringResource(R.string.reduce_animation_subtitle),
                    trailing = {
                        IosSwitch(
                            checked = reduceAnimation,
                            onCheckedChange = AppSettings::setReduceAnimation,
                        )
                    },
                    onClick = { AppSettings.setReduceAnimation(!reduceAnimation) },
                )
            }
            if (showPlayerLyricsInLegacyAppearance) {
                val coverAnimationTitle = stringResource(R.string.cover_animation)
                val coverAnimationValue = stringResource(
                    when {
                        !coverSlide -> R.string.cover_animation_disabled
                        coverFluid && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> R.string.cover_animation_fluid
                        else -> R.string.cover_animation_slide
                    },
                )
                row(coverAnimationTitle, stringResource(R.string.settings_category_player_lyrics), "cover", "transition", "fluid", "slide", "motion", "canvas", "video", "spotify", "cellular", "sampul", "cair", "animada", "seluler") {
                    SettingsRow(
                        icon = SfIcons.Animation,
                        title = coverAnimationTitle,
                        value = coverAnimationValue,
                        badge = stringResource(R.string.settings_badge_new).takeIf { coverAnimationMenuIsNew },
                        onClick = {
                            AppSettings.markFeatureSeen(AppSettings.FEATURE_COVER_ANIMATION_MENU)
                            onOpenChoice(SettingsChoiceKind.COVER_ANIMATION)
                        },
                    )
                }
            }
            val reduceDynamicBlurTitle = stringResource(R.string.reduce_dynamic_blur)
            row(reduceDynamicBlurTitle, "blur", "performance") {
                SettingsRow(
                    icon = SfIcons.BlurOff,
                    title = reduceDynamicBlurTitle,
                    subtitle = stringResource(R.string.reduce_dynamic_blur_subtitle),
                    trailing = {
                        IosSwitch(
                            checked = reduceDynamicBlur,
                            onCheckedChange = AppSettings::setReduceDynamicBlur,
                        )
                    },
                    onClick = { AppSettings.setReduceDynamicBlur(!reduceDynamicBlur) },
                )
            }
            val disableBlendEffectsTitle = stringResource(R.string.disable_blend_effects)
            row(disableBlendEffectsTitle, "blend", "blending", "colour", "color", "tint", "player", "menu") {
                SettingsRow(
                    icon = SfIcons.Gradient,
                    title = disableBlendEffectsTitle,
                    subtitle = stringResource(R.string.disable_blend_effects_subtitle),
                    trailing = {
                        IosSwitch(
                            checked = disableBlendEffects,
                            onCheckedChange = AppSettings::setDisableBlendEffects,
                        )
                    },
                    onClick = { AppSettings.setDisableBlendEffects(!disableBlendEffects) },
                )
            }
            val liquidGlassTitle = stringResource(R.string.liquid_glass)
            row(liquidGlassTitle, "glass", "blur") {
                SettingsRow(
                    icon = SfIcons.AutoAwesome,
                    title = liquidGlassTitle,
                    subtitle = stringResource(
                        if (liquidGlassSupported) {
                            R.string.liquid_glass_subtitle
                        } else {
                            R.string.liquid_glass_unavailable
                        },
                    ),
                    enabled = liquidGlassSupported,
                    trailing = {
                        IosSwitch(
                            checked = liquidGlass,
                            onCheckedChange = AppSettings::setLiquidGlass,
                            enabled = liquidGlassSupported,
                        )
                    },
                    onClick = { AppSettings.setLiquidGlass(!liquidGlass) },
                )
            }
            // One line each, like Direct Source Stream under Sources, and only
            // while Liquid Glass is on: they qualify the glass rather than standing
            // on their own. Progressive Blur has its own switch because it costs
            // frames of its own. Reduce dynamic blur turns it off.
            if (liquidGlass && liquidGlassSupported) {
                val glassPerformanceTitle = stringResource(R.string.glass_performance)
                row(glassPerformanceTitle, "glass", "performance", "refraction", "frosted", "lag", divided = false) {
                    SettingsSubRow(
                        title = glassPerformanceTitle,
                        checked = glassPerformance,
                        onCheckedChange = AppSettings::setGlassPerformance,
                    )
                }
                val progressiveBlurTitle = stringResource(R.string.progressive_blur)
                row(progressiveBlurTitle, "blur", "status bar", "top", divided = false) {
                    SettingsSubRow(
                        title = progressiveBlurTitle,
                        checked = progressiveBlur,
                        onCheckedChange = AppSettings::setProgressiveBlur,
                    )
                }
            }
            // Left out where the player won't honour it: a window too wide for
            // the player to fill and too narrow to stand a page beside it keeps
            // the sleeve either way. A docked pane is a phone's width, so it does
            // honour it — see [fullBleedArtworkAvailable].
            if (showPlayerLyricsInLegacyAppearance && fullBleedArtworkAvailable(windowWidth)) {
                val fullScreenCoverArtTitle = stringResource(R.string.full_screen_cover_art)
                row(fullScreenCoverArtTitle, stringResource(R.string.settings_category_player_lyrics), "artwork", "player") {
                    SettingsRow(
                        icon = SfIcons.Fullscreen,
                        title = fullScreenCoverArtTitle,
                        subtitle = stringResource(R.string.full_screen_cover_art_subtitle),
                        trailing = {
                            IosSwitch(
                                checked = fullBleedArtwork,
                                onCheckedChange = AppSettings::setFullBleedArtwork,
                            )
                        },
                        onClick = { AppSettings.setFullBleedArtwork(!fullBleedArtwork) },
                    )
                }
            }
            // Listed at every width: the page itself keeps the framed sleeve
            // wherever a square the window's width wouldn't fit — see
            // [ReleaseHeader].
            if (showPlayerLyricsInLegacyAppearance) {
                val playerThemeTitle = stringResource(R.string.player_theme)
                row(playerThemeTitle, stringResource(R.string.settings_category_player_lyrics), "background", "player", "mesh", "gradient", "legacy", "simple", "blur") {
                    SettingsRow(
                        icon = SfIcons.Gradient,
                        title = playerThemeTitle,
                        subtitle = playerTheme.description(),
                    )
                    SegmentedControl(
                        options = PlayerTheme.entries.map { it.localizedLabel() },
                        selectedIndex = PlayerTheme.entries.indexOf(playerTheme),
                        onSelect = { AppSettings.setPlayerTheme(PlayerTheme.entries[it]) },
                        modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp),
                    )
                }
            }
            if (showPlayerLyricsInLegacyAppearance) {
                val artistSignaturesTitle = stringResource(R.string.artist_signatures)
                row(artistSignaturesTitle, stringResource(R.string.settings_category_player_lyrics), "signature", "artist", "logo") {
                    SettingsRow(
                        icon = SfIcons.Draw,
                        title = artistSignaturesTitle,
                        subtitle = stringResource(R.string.artist_signatures_subtitle),
                        trailing = {
                            IosSwitch(
                                checked = signatureApiEnabled,
                                onCheckedChange = AppSettings::setSignatureApiEnabled,
                            )
                        },
                        onClick = { AppSettings.setSignatureApiEnabled(!signatureApiEnabled) },
                    )
                }
            }
            if (showPlayerLyricsInLegacyAppearance && signatureApiEnabled) {
                val signatureUrlTitle = stringResource(R.string.artist_signatures_url)
                row(signatureUrlTitle, stringResource(R.string.settings_category_player_lyrics), "signature", "api", "url", divided = false) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onEditSignatureApi)
                            .padding(start = ROW_INSET, end = ROW_INSET, top = 4.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = signatureApiUrl.ifBlank {
                                stringResource(R.string.artist_signatures_unconfigured)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Chevron()
                    }
                }
            }
            if (showPlayerLyricsInLegacyAppearance) {
                val lyricsHidden = hideLyricsPreview || !syncedLyrics
                val setLyricsHidden: (Boolean) -> Unit = { hide ->
                    AppSettings.markFeatureSeen(AppSettings.FEATURE_HIDE_LYRICS_PREVIEW)
                    AppSettings.setHideLyricsPreview(hide)
                    if (!hide && !syncedLyrics) AppSettings.setSyncedLyrics(true)
                }
                val hideLyricsTitle = stringResource(R.string.hide_lyrics_preview)
                row(hideLyricsTitle, stringResource(R.string.settings_category_player_lyrics), "lyrics", "hide", "synced", "translation", "lrclib", "musixmatch") {
                    SettingsRow(
                        icon = SfIcons.VisibilityOff,
                        title = hideLyricsTitle,
                        subtitle = stringResource(R.string.hide_lyrics_preview_subtitle),
                        badge = stringResource(R.string.settings_badge_new).takeIf { hideLyricsPreviewIsNew },
                        trailing = {
                            IosSwitch(
                                checked = lyricsHidden,
                                onCheckedChange = setLyricsHidden,
                            )
                        },
                        onClick = { setLyricsHidden(!lyricsHidden) },
                    )
                }
                val syncedLyricsTitle = stringResource(R.string.synced_lyrics)
                row(syncedLyricsTitle, stringResource(R.string.settings_category_player_lyrics), "lyrics", "translation", "lrclib", "musixmatch") {
                    SettingsRow(
                        icon = SfIcons.Notes,
                        title = syncedLyricsTitle,
                        subtitle = stringResource(R.string.synced_lyrics_subtitle),
                        trailing = {
                            IosSwitch(
                                checked = syncedLyrics,
                                onCheckedChange = AppSettings::setSyncedLyrics,
                            )
                        },
                        onClick = { AppSettings.setSyncedLyrics(!syncedLyrics) },
                    )
                }
            }
            if (showPlayerLyricsInLegacyAppearance && syncedLyrics) {
                val lyricsBlurTitle = stringResource(R.string.blur_unfocused_lyrics)
                row(lyricsBlurTitle, stringResource(R.string.settings_category_player_lyrics), "lyrics", "blur") {
                    SettingsRow(
                        icon = SfIcons.BlurOn,
                        title = lyricsBlurTitle,
                        subtitle = stringResource(R.string.blur_unfocused_lyrics_subtitle),
                        trailing = {
                            IosSwitch(
                                checked = lyricsBlur,
                                onCheckedChange = AppSettings::setLyricsBlur,
                            )
                        },
                        onClick = { AppSettings.setLyricsBlur(!lyricsBlur) },
                    )
                }
                val lyricsSourcesTitle = stringResource(R.string.lyrics_sources)
                row(lyricsSourcesTitle, stringResource(R.string.settings_category_player_lyrics), "lyrics", "lrclib", "musixmatch") {
                    SettingsRow(
                        icon = SfIcons.Language,
                        title = lyricsSourcesTitle,
                        subtitle = lyricsSources
                            .sortedBy { it.ordinal }
                            .joinToString(", ") { it.label }
                            .ifEmpty { stringResource(R.string.no_lyrics_sources_enabled) },
                        trailing = { Chevron() },
                        onClick = onLyricsSources,
                    )
                }
                val translationLanguageTitle = stringResource(R.string.translation_language)
                row(translationLanguageTitle, stringResource(R.string.settings_category_player_lyrics), "lyrics", "translate") {
                    SettingsRow(
                        icon = SfIcons.Translate,
                        title = translationLanguageTitle,
                        subtitle = if (translationLanguage.isBlank()) {
                            stringResource(R.string.translation_language_subtitle)
                        } else {
                            translationLanguageName(
                                translationLanguage,
                                AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault(),
                            )
                        },
                        trailing = { Chevron() },
                        onClick = onTranslationLanguage,
                    )
                }
            }
        }

        if (shows(SettingsCategory.APPEARANCE)) SearchableSettingsGroup(
            search = search,
            header = stringResource(R.string.settings_group_covers_screens),
        ) {
            val fullScreenDetailCoverTitle = stringResource(R.string.full_screen_detail_cover)
            row(fullScreenDetailCoverTitle, "artwork", "album", "playlist", "cover") {
                SettingsRow(
                    icon = SfIcons.Fullscreen,
                    title = fullScreenDetailCoverTitle,
                    subtitle = stringResource(R.string.full_screen_detail_cover_subtitle),
                    trailing = {
                        IosSwitch(
                            checked = fullBleedDetailArtwork,
                            onCheckedChange = AppSettings::setFullBleedDetailArtwork,
                        )
                    },
                    onClick = { AppSettings.setFullBleedDetailArtwork(!fullBleedDetailArtwork) },
                )
            }
        }

        if (shows(SettingsCategory.PERFORMANCE)) SearchableSettingsGroup(search, header = stringResource(R.string.settings_subcategory_performance)) {
            val highPerformanceModeTitle = stringResource(R.string.high_performance_mode)
            row(highPerformanceModeTitle, "frame rate", "refresh rate", "hz", "battery", "smooth") {
                SettingsRow(
                    icon = SfIcons.Performance,
                    title = highPerformanceModeTitle,
                    subtitle = if (highPerformanceMode) {
                        stringResource(R.string.high_performance_active, selectedPerformanceRefreshRate)
                    } else {
                        stringResource(R.string.high_performance_subtitle)
                    },
                    trailing = {
                        IosSwitch(
                            checked = highPerformanceMode,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    showPerformanceWarning = true
                                } else {
                                    AppSettings.setHighPerformanceMode(false)
                                }
                            },
                        )
                    },
                    onClick = {
                        if (highPerformanceMode) {
                            AppSettings.setHighPerformanceMode(false)
                        } else {
                            showPerformanceWarning = true
                        }
                    },
                )
            }
            if (highPerformanceMode) {
                val refreshRateTitle = stringResource(R.string.refresh_rate)
                row(refreshRateTitle, "hz", "frame rate") {
                    SettingsRow(
                        icon = SfIcons.FrameRate,
                        title = refreshRateTitle,
                    )
                    SegmentedControl(
                        options = supportedRefreshRates.map { "$it Hz" },
                        selectedIndex = supportedRefreshRates.indexOf(selectedPerformanceRefreshRate),
                        onSelect = { index ->
                            AppSettings.setPerformanceRefreshRate(supportedRefreshRates[index])
                        },
                        modifier = Modifier.padding(
                            start = TEXT_INSET,
                            end = ROW_INSET,
                            bottom = 14.dp,
                        ),
                    )
                }
            }
        }

        val cacheUnlimited = cacheLimitBytes == AppSettings.UNLIMITED_CACHE_LIMIT_BYTES
        // The slider's last stop, one step past the largest fixed size, is
        // "Unlimited" — see [AppSettings.setAudioCacheLimitBytes].
        val cacheLimitMb = if (cacheUnlimited) CACHE_UNLIMITED_STOP_MB else (cacheLimitBytes / (1024 * 1024)).toInt()
        val unlimitedLabel = stringResource(R.string.song_cache_unlimited)
        val cacheLimitLabel = if (cacheUnlimited) unlimitedLabel else formatCacheSize(cacheLimitMb)
        if (section == null) SettingsBlockHeader(stringResource(R.string.settings_group_data_storage_system), searchQuery.isBlank())
        if (shows(SettingsCategory.STORAGE)) SearchableSettingsGroup(search, header = stringResource(R.string.settings_subcategory_storage_cache)) {
            val showCacheFolderTitle = stringResource(R.string.show_cache_folder)
            row(showCacheFolderTitle, "cache", "cached songs", "library", "folder") {
                SettingsRow(
                    icon = SfIcons.Cached,
                    title = showCacheFolderTitle,
                    subtitle = stringResource(R.string.show_cache_folder_subtitle),
                    trailing = {
                        IosSwitch(
                            checked = showCacheFolder,
                            onCheckedChange = AppSettings::setShowCacheFolder,
                        )
                    },
                    onClick = { AppSettings.setShowCacheFolder(!showCacheFolder) },
                )
            }
            val songCacheLimitTitle = stringResource(R.string.song_cache_limit)
            row(songCacheLimitTitle, "cache", "space", "unlimited") {
                SliderRow(
                    icon = SfIcons.Storage,
                    title = songCacheLimitTitle,
                    subtitle = when {
                        cacheUnlimited -> stringResource(R.string.song_cache_unlimited_subtitle)
                        cacheLimitMb > CACHE_WARNING_MB ->
                            stringResource(R.string.song_cache_large_subtitle, cacheLimitLabel)
                        else -> stringResource(R.string.song_cache_limit_subtitle)
                    },
                    value = cacheLimitLabel,
                    sliderValue = cacheLimitMb.toFloat(),
                    onSliderValue = {
                        AppSettings.setAudioCacheLimitBytes(it.roundToInt().toLong() * 1024 * 1024)
                    },
                    valueRange = (AppSettings.DEFAULT_CACHE_LIMIT_BYTES / (1024 * 1024)).toFloat()..
                        CACHE_UNLIMITED_STOP_MB.toFloat(),
                    steps = 19,
                )
            }
            val clearSongCacheTitle = stringResource(R.string.clear_song_cache)
            row(clearSongCacheTitle, "cache", "free space") {
                SettingsRow(
                    icon = SfIcons.DeleteSweep,
                    title = clearSongCacheTitle,
                    subtitle = stringResource(R.string.clear_song_cache_subtitle),
                    onClick = { confirmClearSongCache = true },
                )
            }
            val clearImageCacheTitle = stringResource(R.string.clear_image_cache)
            row(clearImageCacheTitle, "cache", "artwork", "free space") {
                SettingsRow(
                    icon = SfIcons.DeleteSweep,
                    title = clearImageCacheTitle,
                    subtitle = stringResource(R.string.clear_image_cache_subtitle),
                    onClick = {
                        val loader = SingletonImageLoader.get(context)
                        loader.memoryCache?.clear()
                        loader.diskCache?.clear()
                        Toast.makeText(context, context.getString(R.string.image_cache_cleared), Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }

        // Only in a build that carries the watch app — see app/build.gradle.kts.
        // On the main page it is listed with the categories; here it answers
        // a search.
        if (shows(SettingsCategory.YOUR_DATA)) SearchableSettingsGroup(search, header = stringResource(R.string.settings_group_data_summary)) {
            val replayTitle = stringResource(R.string.replay)
            row(replayTitle, "stats", "history", "wrapped") {
                SettingsRow(
                    icon = SfIcons.BarChart,
                    title = replayTitle,
                    subtitle = stringResource(R.string.replay_subtitle),
                    onClick = onOpenReplay,
                )
            }
            val workOutGenresTitle = stringResource(R.string.work_out_genres)
            row(workOutGenresTitle, "genre", "replay") {
                SettingsRow(
                    icon = SfIcons.LocalOffer,
                    title = workOutGenresTitle,
                    subtitle = if (replayGenres) {
                        stringResource(R.string.replay_genres_enabled_subtitle)
                    } else {
                        stringResource(R.string.replay_genres_disabled_subtitle)
                    },
                    trailing = {
                        IosSwitch(
                            checked = replayGenres,
                            onCheckedChange = AppSettings::setReplayGenres,
                        )
                    },
                    onClick = { AppSettings.setReplayGenres(!replayGenres) },
                )
            }
        }

        if (shows(SettingsCategory.YOUR_DATA)) SearchableSettingsGroup(search, header = stringResource(R.string.settings_group_data_backup)) {
            val exportDataTitle = stringResource(R.string.export_data)
            row(exportDataTitle, "backup") {
                SettingsRow(
                    icon = SfIcons.FileUpload,
                    title = exportDataTitle,
                    subtitle = exportStatus ?: stringResource(R.string.export_data_subtitle),
                    onClick = { exportPicker.launch(Backup.suggestedName()) },
                )
            }
            val importDataTitle = stringResource(R.string.import_data)
            row(importDataTitle, "backup", "restore") {
                SettingsRow(
                    icon = SfIcons.FileDownload,
                    title = importDataTitle,
                    subtitle = importStatus ?: stringResource(R.string.import_data_subtitle),
                    onClick = { confirmImport = true },
                )
            }
            val driveBackupTitle = stringResource(R.string.drive_backup_title)
            row(driveBackupTitle, "backup", "restore", "google", "drive", "cloud") {
                DriveBackupSettingsRow(
                    title = driveBackupTitle,
                    badge = stringResource(R.string.settings_badge_new).takeIf { driveBackupIsNew },
                    onClick = {
                        AppSettings.markFeatureSeen(AppSettings.FEATURE_DRIVE_BACKUP)
                        onDriveBackup()
                    },
                )
            }
        }

        // No footer: it only restated the stop-on-close row's own subtitle,
        // which sits four rows above it and says the same thing in fewer words.
        if (shows(SettingsCategory.MISCELLANEOUS)) SearchableSettingsGroup(
            search = search,
            header = stringResource(R.string.settings_group_queue_behavior),
            searchAlias = stringResource(R.string.settings_category_audio_playback),
        ) {
            val playNextOnSwipeTitle = stringResource(R.string.play_next_on_swipe)
            row(playNextOnSwipeTitle, "swipe", "queue") {
                SettingsRow(
                    icon = SfIcons.PlaylistPlay,
                    title = playNextOnSwipeTitle,
                    subtitle = if (swipeToPlayNext) {
                        stringResource(R.string.swipe_plays_next)
                    } else {
                        stringResource(R.string.swipe_adds_to_queue)
                    },
                    trailing = {
                        IosSwitch(
                            checked = swipeToPlayNext,
                            onCheckedChange = AppSettings::setSwipeToPlayNext,
                        )
                    },
                    onClick = { AppSettings.setSwipeToPlayNext(!swipeToPlayNext) },
                )
            }
            val dontRepeatSongsTitle = stringResource(R.string.dont_repeat_songs)
            row(dontRepeatSongsTitle, "radio", "suggestions") {
                SettingsRow(
                    icon = SfIcons.History,
                    title = dontRepeatSongsTitle,
                    subtitle = stringResource(R.string.dont_repeat_songs_subtitle),
                    trailing = {
                        IosSwitch(
                            checked = dontRepeatSuggestions,
                            onCheckedChange = AppSettings::setDontRepeatSuggestions,
                        )
                    },
                    onClick = { AppSettings.setDontRepeatSuggestions(!dontRepeatSuggestions) },
                )
            }
            val stopMusicOnCloseTitle = stringResource(R.string.stop_music_on_close)
            row(stopMusicOnCloseTitle, "notification", "background") {
                SettingsRow(
                    icon = SfIcons.MusicOff,
                    title = stopMusicOnCloseTitle,
                    subtitle = stringResource(R.string.stop_music_on_close_subtitle),
                    trailing = {
                        IosSwitch(
                            checked = stopOnTaskRemoved,
                            onCheckedChange = AppSettings::setStopOnTaskRemoved,
                        )
                    },
                    onClick = { AppSettings.setStopOnTaskRemoved(!stopOnTaskRemoved) },
                )
            }
            if ((section == null && searching) || section == SettingsCategory.MISCELLANEOUS) {
            val hideVolumeBarTitle = stringResource(R.string.hide_volume_bar)
            row(hideVolumeBarTitle, "volume", "player") {
                SettingsRow(
                    icon = SfIcons.VolumeOff,
                    title = hideVolumeBarTitle,
                    subtitle = stringResource(R.string.hide_volume_bar_subtitle),
                    trailing = {
                        IosSwitch(
                            checked = hideVolumeBar,
                            onCheckedChange = AppSettings::setHideVolumeBar,
                        )
                    },
                    onClick = { AppSettings.setHideVolumeBar(!hideVolumeBar) },
                )
            }
            val listenTogetherButtonTitle = stringResource(R.string.show_listen_together_button)
            row(listenTogetherButtonTitle, "party", "jam", "output", "headphones", "player") {
                SettingsRow(
                    icon = SfIcons.Groups,
                    title = listenTogetherButtonTitle,
                    subtitle = stringResource(R.string.show_listen_together_button_subtitle),
                    trailing = {
                        IosSwitch(
                            checked = listenTogetherButton,
                            onCheckedChange = AppSettings::setShowListenTogetherButton,
                        )
                    },
                    onClick = { AppSettings.setShowListenTogetherButton(!listenTogetherButton) },
                )
            }
            val hideStatusBarTitle = stringResource(R.string.hide_status_bar_in_player)
            row(hideStatusBarTitle, "status bar", "immersive", "fullscreen", "player") {
                SettingsRow(
                    icon = SfIcons.WebAssetOff,
                    title = hideStatusBarTitle,
                    subtitle = stringResource(R.string.hide_status_bar_in_player_subtitle),
                    trailing = {
                        IosSwitch(
                            checked = hideStatusBarInPlayer,
                            onCheckedChange = AppSettings::setHideStatusBarInPlayer,
                        )
                    },
                    onClick = { AppSettings.setHideStatusBarInPlayer(!hideStatusBarInPlayer) },
                )
            }
            val showQueueHistoryTitle = stringResource(R.string.show_queue_history)
            row(showQueueHistoryTitle, "player", "queue", "history", "previously played") {
                SettingsRow(
                    icon = SfIcons.History,
                    title = showQueueHistoryTitle,
                    subtitle = stringResource(R.string.show_queue_history_subtitle),
                    trailing = {
                        IosSwitch(
                            checked = showQueueHistory,
                            onCheckedChange = AppSettings::setShowQueueHistory,
                        )
                    },
                    onClick = { AppSettings.setShowQueueHistory(!showQueueHistory) },
                )
            }
            val hideSongStatusTitle = stringResource(R.string.hide_song_status)
            row(hideSongStatusTitle, "player", "playing from", "played by") {
                SettingsRow(
                    icon = SfIcons.VisibilityOff,
                    title = hideSongStatusTitle,
                    subtitle = stringResource(R.string.hide_song_status_subtitle),
                    trailing = {
                        IosSwitch(
                            checked = hideSongStatus,
                            onCheckedChange = AppSettings::setHideSongStatus,
                        )
                    },
                    onClick = { AppSettings.setHideSongStatus(!hideSongStatus) },
                )
            }
            val showTotalDurationTitle = stringResource(R.string.show_total_duration)
            row(showTotalDurationTitle, "seek bar", "remaining time", "duration", "song length") {
                SettingsRow(
                    icon = SfIcons.Clock,
                    title = showTotalDurationTitle,
                    subtitle = stringResource(R.string.show_total_duration_subtitle),
                    badge = stringResource(R.string.settings_badge_new).takeIf { showTotalDurationIsNew },
                    trailing = {
                        IosSwitch(
                            checked = showTotalDuration,
                            onCheckedChange = { checked ->
                                AppSettings.markFeatureSeen(AppSettings.FEATURE_SHOW_TOTAL_DURATION)
                                AppSettings.setShowTotalDuration(checked)
                            },
                        )
                    },
                    onClick = {
                        AppSettings.markFeatureSeen(AppSettings.FEATURE_SHOW_TOTAL_DURATION)
                        AppSettings.setShowTotalDuration(!showTotalDuration)
                    },
                )
            }
            val showPlayerShuffleRepeatButtonsTitle = stringResource(R.string.show_player_shuffle_repeat_buttons)
            row(showPlayerShuffleRepeatButtonsTitle, "shuffle", "repeat", "random", "aleatorio", "repetir", "loop", "transport") {
                SettingsRow(
                    icon = SfIcons.Shuffle,
                    title = showPlayerShuffleRepeatButtonsTitle,
                    subtitle = stringResource(R.string.show_player_shuffle_repeat_buttons_subtitle),
                    badge = stringResource(R.string.settings_badge_new).takeIf { playerShuffleRepeatIsNew },
                    trailing = {
                        IosSwitch(
                            checked = showPlayerShuffleRepeatButtons,
                            onCheckedChange = { checked ->
                                AppSettings.markFeatureSeen(AppSettings.FEATURE_PLAYER_SHUFFLE_REPEAT)
                                AppSettings.setShowPlayerShuffleRepeatButtons(checked)
                            },
                        )
                    },
                    onClick = {
                        AppSettings.markFeatureSeen(AppSettings.FEATURE_PLAYER_SHUFFLE_REPEAT)
                        AppSettings.setShowPlayerShuffleRepeatButtons(!showPlayerShuffleRepeatButtons)
                    },
                )
            }
            }
            val smartAlignmentTitle = stringResource(R.string.smart_version_alignment)
            row(smartAlignmentTitle, "alignment", "sync", "waveform", "video", "audio", "skit", "intro") {
                SettingsRow(
                    icon = SfIcons.GraphicEq,
                    title = smartAlignmentTitle,
                    subtitle = stringResource(R.string.smart_version_alignment_subtitle),
                    trailing = {
                        IosSwitch(
                            checked = smartVersionAlignment,
                            onCheckedChange = AppSettings::setSmartVersionAlignment,
                        )
                    },
                    onClick = { AppSettings.setSmartVersionAlignment(!smartVersionAlignment) },
                )
            }
            if ((section == null && searching) || section == SettingsCategory.MISCELLANEOUS) {
                val bestPartTitle = stringResource(R.string.show_best_part)
                row(bestPartTitle, "best part", "chorus", "skip", "replayed", "highlight") {
                    SettingsRow(
                        icon = SfIcons.MusicNote,
                        title = bestPartTitle,
                        subtitle = stringResource(R.string.show_best_part_subtitle),
                        badge = stringResource(R.string.settings_badge_new).takeIf { bestPartIsNew },
                        trailing = {
                            IosSwitch(
                                checked = bestPart,
                                onCheckedChange = {
                                    AppSettings.markFeatureSeen(AppSettings.FEATURE_BEST_PART)
                                    AppSettings.setShowBestPart(it)
                                },
                            )
                        },
                        onClick = {
                            AppSettings.markFeatureSeen(AppSettings.FEATURE_BEST_PART)
                            AppSettings.setShowBestPart(!bestPart)
                        },
                    )
                }
                val showNerdStatsTitle = stringResource(R.string.show_nerd_stats)
                row(showNerdStatsTitle, "debug", "bitrate", "codec") {
                    SettingsRow(
                        icon = SfIcons.BarChart,
                        title = showNerdStatsTitle,
                        subtitle = stringResource(R.string.show_nerd_stats_subtitle),
                        trailing = {
                            IosSwitch(
                                checked = nerdStats,
                                onCheckedChange = AppSettings::setShowNerdStats,
                            )
                        },
                        onClick = { AppSettings.setShowNerdStats(!nerdStats) },
                    )
                }
            }
        }

        if (shows(SettingsCategory.LANGUAGE)) SearchableSettingsGroup(search, header = stringResource(R.string.settings_subcategory_app_language)) {
            val appLanguageTitle = stringResource(R.string.app_language)
            row(appLanguageTitle, "locale", "translate") {
                val selectedLanguage = AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag()
                    ?: Locale.getDefault().toLanguageTag()
                SettingsRow(
                    icon = SfIcons.Language,
                    title = appLanguageTitle,
                    subtitle = stringResource(languageDisplayNameRes(selectedLanguage)),
                    onClick = onAppLanguage,
                )
            }
        }

        // Read after every group above has had its turn at the query, which is
        // what makes this an accurate "nothing here" rather than a guess.
        if (searching && !search.anyMatch) {
            MessageState(stringResource(R.string.settings_search_empty, searchQuery))
        }

        // The version line is the page's colophon, not a setting: it belongs to
        // the whole list, so it goes when the list is narrowed to a few rows.
        }

        if (rootHome) {
        Text(
            text = stringResource(R.string.settings_footer_brand, version),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
        )
        }
        }
    }

    // Everything in the Cached songs folder goes with it, so say so first.
    if (confirmClearSongCache) {
        AlertDialog(
            onDismissRequest = { confirmClearSongCache = false },
            title = { Text(stringResource(R.string.clear_song_cache_title)) },
            text = { Text(stringResource(R.string.clear_song_cache_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearSongCache = false
                    AudioCache.clear {
                        Toast.makeText(context, context.getString(R.string.song_cache_cleared), Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearSongCache = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Deleting also turns Automix off: left on, its next analysis pass would
    // simply download the model again.
    if (confirmDeleteAutomixModel) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAutomixModel = false },
            title = { Text(stringResource(R.string.automix_model_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.automix_model_delete_warning,
                        (AutomixModels.totalBytes / 1_000_000).toInt(),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteAutomixModel = false
                    AppSettings.setSmartFadeEnabled(false)
                    AutomixModels.delete()
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAutomixModel = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Asked before the picker opens rather than after a file is chosen: the
    // thing being confirmed is that this device's own history is about to be
    // thrown away, and that is true whichever file gets picked.
    if (confirmImport) {
        CleanConfirmationAlert(
            title = stringResource(R.string.import_backup_title),
            description = stringResource(R.string.import_backup_warning),
            confirmLabel = stringResource(R.string.choose_file),
            onConfirm = {
                confirmImport = false
                importPicker.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onDismiss = { confirmImport = false },
            destructive = true,
        )
    }

    if (showPerformanceWarning) {
        AlertDialog(
            onDismissRequest = { showPerformanceWarning = false },
            title = { Text(stringResource(R.string.high_performance_before_enabling)) },
            text = { Text(stringResource(R.string.high_performance_battery_warning)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPerformanceWarning = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        val settingsIntent = intent.takeIf {
                            it.resolveActivity(context.packageManager) != null
                        } ?: Intent(Settings.ACTION_SETTINGS)
                        batterySettingsLauncher.launch(settingsIntent)
                    },
                ) {
                    Text(stringResource(R.string.open_battery_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPerformanceWarning = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showPerformanceConfirmation) {
        AlertDialog(
            onDismissRequest = { showPerformanceConfirmation = false },
            title = { Text(stringResource(R.string.enable_high_performance_title)) },
            text = { Text(stringResource(R.string.enable_high_performance_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPerformanceConfirmation = false
                        AppSettings.setHighPerformanceMode(true)
                    },
                ) {
                    Text(stringResource(R.string.enable))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPerformanceConfirmation = false }) {
                    Text(stringResource(R.string.not_yet))
                }
            },
        )
    }

    if (showListenBrainzTokenDialog) {
        var tokenInput by remember { mutableStateOf(listenBrainzToken) }
        AlertDialog(
            onDismissRequest = { showListenBrainzTokenDialog = false },
            title = { Text(stringResource(R.string.listenbrainz_token)) },
            text = {
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text(stringResource(R.string.api_token)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    AppSettings.setListenBrainzToken(tokenInput.trim())
                    showListenBrainzTokenDialog = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showListenBrainzTokenDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showLastfmLoginDialog) {
        var usernameInput by remember { mutableStateOf("") }
        var passwordInput by remember { mutableStateOf("") }
        var lastfmError by remember { mutableStateOf<String?>(null) }
        var lastfmLoading by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!lastfmLoading) showLastfmLoginDialog = false },
            title = { Text(stringResource(R.string.lastfm_login)) },
            text = {
                Column {
                    if (lastfmError != null) {
                        Text(
                            text = lastfmError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text(stringResource(R.string.username)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text(stringResource(R.string.password)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        lastfmLoading = true
                        lastfmError = null
                        scrobbleScope.launch {
                            try {
                                // Use the credentials supplied for this build.
                                LastFM.initialize(
                                    apiKey = AppSettings.lastfmApiKey.value,
                                    secret = AppSettings.lastfmSecret.value,
                                )
                                LastFM.getMobileSession(usernameInput.trim(), passwordInput)
                                    .onSuccess { auth ->
                                        AppSettings.setLastfmSessionKey(auth.session.key)
                                        AppSettings.setLastfmUsername(auth.session.name)
                                        AppSettings.setLastfmEnabled(true)
                                        showLastfmLoginDialog = false
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
                    enabled = !lastfmLoading && usernameInput.isNotBlank() && passwordInput.isNotBlank(),
                ) {
                    Text(stringResource(if (lastfmLoading) R.string.signing_in else R.string.sign_in))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLastfmLoginDialog = false }, enabled = !lastfmLoading) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

}

/** "3 months of listening" — the unit a backup is actually measured in. */
internal fun Context.countOfMonths(months: Int): String = if (months == 0) {
    getString(R.string.no_listening_history)
} else {
    resources.getQuantityString(R.plurals.listening_month_count, months, months)
}

@Composable
private fun AudioQuality.localizedLabel(): String = stringResource(
    when (this) {
        AudioQuality.LOW -> R.string.low
        AudioQuality.MEDIUM -> R.string.medium
        AudioQuality.HIGH -> R.string.high
        AudioQuality.LOSSLESS -> R.string.lossless
    },
)

@Composable
private fun DownloadQuality.localizedLabel(): String = stringResource(
    when (this) {
        DownloadQuality.STANDARD -> R.string.standard
        DownloadQuality.HIGH -> R.string.high
        DownloadQuality.LOSSLESS -> R.string.lossless
    },
)

@Composable
private fun ThemeMode.localizedLabel(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.system
        ThemeMode.LIGHT -> R.string.light
        ThemeMode.DARK -> R.string.dark
    },
)

@Composable
private fun PlayerTheme.localizedLabel(): String = stringResource(
    when (this) {
        PlayerTheme.DEFAULT -> R.string.player_theme_default
        PlayerTheme.LEGACY -> R.string.player_theme_legacy
        PlayerTheme.SIMPLE -> R.string.player_theme_simple
    },
)

@Composable
private fun PlayerTheme.description(): String = stringResource(
    when (this) {
        PlayerTheme.DEFAULT -> R.string.player_theme_default_subtitle
        PlayerTheme.LEGACY -> R.string.player_theme_legacy_subtitle
        PlayerTheme.SIMPLE -> R.string.player_theme_simple_subtitle
    },
)

@Composable
private fun AppFont.localizedLabel(): String = stringResource(
    when (this) {
        AppFont.DEFAULT -> R.string.font_default
        AppFont.LEGACY -> R.string.font_legacy
    },
)

/** The typeface each [AppFont] sets, named under its label. */
@Composable
private fun AppFont.description(): String = stringResource(
    when (this) {
        AppFont.DEFAULT -> R.string.font_default_subtitle
        AppFont.LEGACY -> R.string.font_legacy_subtitle
    },
)

@Composable
private fun AccentColor.localizedLabel(): String = stringResource(
    when (this) {
        AccentColor.DEFAULT -> R.string.accent_default
        AccentColor.RED -> R.string.accent_red
        AccentColor.ORANGE -> R.string.accent_orange
        AccentColor.YELLOW -> R.string.accent_yellow
        AccentColor.GREEN -> R.string.accent_green
        AccentColor.TEAL -> R.string.accent_teal
        AccentColor.BLUE -> R.string.accent_blue
        AccentColor.INDIGO -> R.string.accent_indigo
        AccentColor.PURPLE -> R.string.accent_purple
        AccentColor.PINK -> R.string.accent_pink
        AccentColor.GRAY -> R.string.accent_gray
    },
)

private fun AccentColor.colorFor(dark: Boolean): Long = if (dark) darkColor else lightColor

@Composable
private fun AutomixPerformanceMode.localizedLabel(): String = stringResource(
    when (this) {
        AutomixPerformanceMode.EFFICIENT -> R.string.automix_mode_efficient
        AutomixPerformanceMode.BALANCED -> R.string.automix_mode_balanced
        AutomixPerformanceMode.PERFORMANCE -> R.string.automix_mode_performance
    },
)

/**
 * Hands the device's own effect panel this app's audio session.
 *
 * Shared with [EqualizerScreen], which offers the same escape hatch a second
 * time next to the equaliser it might be an alternative to.
 */
internal fun openEqualizer(context: Context, sessionId: Int) {
    val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
    }
    runCatching { context.startActivity(intent) }.onFailure {
        Toast.makeText(context, context.getString(R.string.no_equalizer), Toast.LENGTH_SHORT).show()
    }
}

/** Above this, the cache limit slider's subtitle warns rather than reassures. */
private const val CACHE_WARNING_MB = 2048

/**
 * Where the cache limit slider's "Unlimited" stop sits: one 512MB step past
 * [AppSettings.MAX_CACHE_LIMIT_BYTES], so the fixed sizes keep their positions.
 */
private const val CACHE_UNLIMITED_STOP_MB = 10 * 1024 + 512

/** "512 MB", "2 GB", "2.5 GB" — whichever reads more naturally at that size. */
private fun formatCacheSize(mb: Int): String {
    if (mb < 1024) return "$mb MB"
    val gb = mb / 1024f
    return if (gb == gb.toInt().toFloat()) "${gb.toInt()} GB" else "%.1f GB".format(Locale.ROOT, gb)
}

/** Who you're signed in as, straight from YouTube Music's account menu. */
@Composable
internal fun AccountCard(
    signedIn: Boolean,
    account: Account?,
    onSignIn: () -> Unit,
    onClick: (() -> Unit)? = null,
    /** Sits inside a [SettingsGroup] that already supplies the card, so draws none of its own. */
    grouped: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (grouped) {
                    Modifier
                } else {
                    Modifier
                        .padding(horizontal = GROUP_INSET)
                        .clip(GroupShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                },
            )
            .then(
                when {
                    signedIn && onClick != null -> Modifier.clickable(onClick = onClick)
                    !signedIn -> Modifier.clickable(onClick = onSignIn)
                    else -> Modifier
                },
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (account?.thumbnailUrl != null) {
            AsyncImage(
                model = account.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(52.dp).clip(CircleShape).thumbnailBorder(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    SfIcons.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = account?.name
                    ?: stringResource(if (signedIn) R.string.signed_in else R.string.not_signed_in),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = account?.email?.takeIf { it.isNotBlank() }
                    ?: stringResource(
                        if (signedIn) R.string.youtube_music_account else R.string.tap_to_sign_in_google,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!signedIn) {
            Spacer(Modifier.width(8.dp))
            Chevron()
        }
    }
}

/**
 * A setting the bit-perfect output bypasses, faded and closed to touch while
 * that output is ready. With the mode on but its DAC unplugged the setting
 * works, and looks it.
 *
 * Only the touches that would change something are taken, so the page still
 * scrolls when a swipe starts on one of these rows: the press and the
 * release (no ripple, no click, no switch flip) and a sideways drag (no
 * slider). A vertical drag is left alone, and the list's scrolling still
 * starts from a press that was taken, so it scrolls as from any other row.
 */
@Composable
private fun BypassedByBitPerfect(bypassed: Boolean, content: @Composable () -> Unit) {
    val alpha by animateFloatAsState(if (bypassed) 0.38f else 1f, label = "bypassed")
    // A Column, not a Box: a row can be several stacked pieces (a title row
    // and its control), and a Box would draw them over one another.
    Column(
        Modifier
            .alpha(alpha)
            .then(
                if (bypassed) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial).consume()
                            var sideways: Boolean? = null
                            var travel = Offset.Zero
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { change ->
                                    when {
                                        change.changedToUpIgnoreConsumed() -> change.consume()
                                        change.positionChanged() -> {
                                            travel += change.positionChange()
                                            if (sideways == null && travel.getDistance() > viewConfiguration.touchSlop / 2) {
                                                sideways = abs(travel.x) > abs(travel.y)
                                            }
                                            if (sideways == true) change.consume()
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        content()
    }
}

@Composable
private fun BitPerfectMode.localizedLabel(): String = stringResource(
    when (this) {
        BitPerfectMode.OFF -> R.string.off
        BitPerfectMode.USB_ANDROID -> R.string.bit_perfect_mode_usb_android
        BitPerfectMode.DAP_EXCLUSIVE -> R.string.bit_perfect_mode_dap_exclusive
        BitPerfectMode.USB_DIRECT -> R.string.bit_perfect_mode_usb_direct
    },
)

/** Whether this phone can use [this] at all, independent of what is plugged in. */
private val BitPerfectMode.isSupported: Boolean
    get() = when (this) {
        BitPerfectMode.OFF -> true
        BitPerfectMode.USB_ANDROID -> Build.VERSION.SDK_INT >= 34
        BitPerfectMode.DAP_EXCLUSIVE -> Build.VERSION.SDK_INT >= 27
        BitPerfectMode.USB_DIRECT -> true
    }

@Composable
private fun BitPerfectMode.description(): String = stringResource(
    when {
        this == BitPerfectMode.USB_ANDROID && !isSupported -> R.string.bit_perfect_requires_android_14
        this == BitPerfectMode.DAP_EXCLUSIVE && !isSupported -> R.string.bit_perfect_requires_android_8_1
        this == BitPerfectMode.OFF -> R.string.bit_perfect_mode_off_subtitle
        this == BitPerfectMode.USB_ANDROID -> R.string.bit_perfect_mode_usb_android_subtitle
        this == BitPerfectMode.USB_DIRECT -> R.string.bit_perfect_mode_usb_direct_subtitle
        else -> R.string.bit_perfect_mode_dap_exclusive_subtitle
    },
)

/**
 * What the exclusive output is doing now, rather than what was asked of it:
 * a mode can be on while the playing track still goes through the mixer.
 */
@Composable
private fun bitPerfectSubtitle(mode: BitPerfectMode, status: BitPerfectOutput.Status): String = when {
    mode == BitPerfectMode.OFF || !mode.isSupported -> mode.description()
    status.active && status.dacLimited -> stringResource(
        R.string.bit_perfect_status_dac_limited,
        status.deviceName.orEmpty(),
        "%.1f".format(status.sampleRateHz / 1000.0),
        status.outputBits,
    )
    status.active && !status.exact -> stringResource(
        R.string.bit_perfect_status_inexact,
        status.deviceName.orEmpty(),
    )
    status.active -> stringResource(
        R.string.bit_perfect_status_active,
        status.deviceName.orEmpty(),
        "%.1f".format(status.sampleRateHz / 1000.0),
        status.outputBits,
    )
    status.fallbackReason != null -> stringResource(R.string.bit_perfect_status_unavailable, status.fallbackReason)
    else -> stringResource(R.string.bit_perfect_status_waiting)
}

// ---- Building blocks --------------------------------------------------------

internal val GroupShape = RoundedCornerShape(14.dp)
internal val GROUP_INSET = 16.dp
internal val ROW_INSET = 16.dp
internal val ICON_SIZE = 22.dp
internal val ICON_GAP = 14.dp

/** Where a row's text starts — dividers are inset to match, as on iOS. */
internal val TEXT_INSET = ROW_INSET + ICON_SIZE + ICON_GAP

/**
 * What is typed into the field under the Settings title, and whether anything
 * on the screen has answered to it.
 *
 * A blank query matches everything, so the screen with the field untouched is
 * the screen as it was before there was a field. Rows ask [matches] as they
 * are composed; the empty state at the foot of the list reads [anyMatch] after
 * every group has had its turn, which is why this is made fresh on each pass
 * rather than remembered.
 */
private class SettingsSearch(
    query: String,
    /** Drawing one category's page rather than the main one: its top bar names it. */
    val inPage: Boolean = false,
) {
    // Trimmed, because a trailing space is a typo rather than a search for
    // something ending in one — and on a phone keyboard it is one keystroke
    // away from every word typed.
    private val needle = query.trim()

    var anyMatch = false
        private set

    fun matches(vararg keywords: String?): Boolean {
        val hit = needle.isEmpty() ||
            keywords.any { it?.contains(needle, ignoreCase = true) == true }
        if (hit) anyMatch = true
        return hit
    }
}

/** Collects the rows of one group that survived the search. */
private class SettingsGroupScope(
    private val search: SettingsSearch,
    private val header: String?,
    private val searchAlias: String? = null,
) {
    class Entry(val divided: Boolean, val content: @Composable () -> Unit)

    val entries = mutableListOf<Entry>()

    /**
     * One searchable setting. [keywords] is everything somebody might type
     * looking for it: its title at least, plus whatever its subtitle or the
     * controls under it say that the title doesn't — "Spotify", say, for the
     * canvas link that lives under Animated cover art.
     *
     * [divided] is false for the rows drawn as part of the row above them
     * rather than as settings in their own right, so they keep hugging their
     * parent when both survive the filter.
     */
    fun row(
        vararg keywords: String?,
        divided: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        // The header counts for every row beneath it: searching "playback"
        // should turn up the playback group entire, not nothing at all.
        if (search.matches(header, searchAlias, *keywords)) entries += Entry(divided, content)
    }
}

/**
 * A [SettingsGroup] whose rows are filtered by the search field, and which
 * takes itself off the screen when none of them are left. Dividers fall
 * between the rows that survived rather than around the gaps left by the ones
 * that didn't, so a filtered card is indistinguishable from a hand-written one.
 *
 * Inline so [content] is not a restartable lambda of its own. It only collects
 * rows into [SettingsGroupScope.entries], and drawing them happens here: as its
 * own recompose scope, a setting it reads — Automix hiding the crossfade
 * slider, say — re-ran it alone, filling a list nothing redrew, and the row
 * only came or went whenever something else recomposed the screen.
 */
@Composable
private inline fun SearchableSettingsGroup(
    search: SettingsSearch,
    header: String? = null,
    searchAlias: String? = null,
    footer: String? = null,
    topSpacing: Dp = 26.dp,
    content: @Composable SettingsGroupScope.() -> Unit,
) {
    val scope = SettingsGroupScope(search, header, searchAlias)
    scope.content()
    if (scope.entries.isEmpty()) return
    SettingsGroup(
        header = header,
        footer = footer,
        topSpacing = topSpacing,
    ) {
        scope.entries.forEachIndexed { index, entry ->
            when {
                index > 0 && entry.divided -> RowDivider()
                // An undivided row is drawn tight to the top because something
                // it belongs to is usually above it. Filtered down to itself it
                // has nothing to sit under, so it is given that room back.
                index == 0 && !entry.divided -> Spacer(Modifier.height(10.dp))
            }
            CompositionLocalProvider(LocalSubRowStandalone provides entry.divided) {
                entry.content()
            }
        }
    }
}

/**
 * A top-level category heading with a quiet divider. Hidden during search so
 * matching settings remain compact while the search spans every category.
 */
@Composable
private fun SettingsBlockHeader(
    text: String,
    visible: Boolean,
) {
    if (!visible) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 4.dp),
    ) {
        Text(
            text = text.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 10.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        )
    }
}

/**
 * The large heading a Settings page leads with, under the bar's own small one —
 * the main page's "Settings", and every page opened from it.
 */
@Composable
internal fun SettingsPageTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp),
    )
}

/**
 * One inset card of rows, with an uppercase header above and an optional
 * plain-language [footer] below. Rows are separated by [RowDivider].
 */
@Composable
internal fun SettingsGroup(
    header: String? = null,
    footer: String? = null,
    /** Room above the card, where there is no [header] to provide it. */
    topSpacing: Dp = 26.dp,
    content: @Composable () -> Unit,
) {
    if (header != null) {
        Text(
            text = header.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = GROUP_INSET + 4.dp,
                end = GROUP_INSET,
                top = 26.dp,
                bottom = 8.dp,
            ),
        )
    } else {
        Spacer(Modifier.height(topSpacing))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GROUP_INSET)
            .clip(GroupShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        content()
    }
    if (footer != null) {
        Text(
            text = footer,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = GROUP_INSET + 4.dp,
                end = GROUP_INSET + 4.dp,
                top = 8.dp,
            ),
        )
    }
}

@Composable
internal fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = TEXT_INSET),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline,
    )
}

/**
 * A row's description with what tapping the row does set under it in
 * [actionColor] — for rows whose action is too long to sit on the right.
 */
@Composable
private fun SubtitleWithAction(
    subtitle: String,
    action: String,
    actionColor: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 5,
    )
    Text(
        text = action,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = actionColor,
        modifier = Modifier.padding(top = 6.dp),
    )
}

/**
 * The standard row: glyph, title, optional subtitle, and on the right either
 * [trailing] (a switch, say) or the current [value] followed by a chevron.
 *
 * [iconPainter] is for the handful of rows whose glyph is a drawable rather
 * than a Material icon — the Dolby double-D, which is a mark and not something
 * to approximate with the nearest speaker outline. Exactly one of it and [icon]
 * is expected; the painter wins where both are given.
 *
 * [iconContent] takes the glyph's place outright — the account row's avatar —
 * and wins over both.
 */
@Composable
internal fun SettingsRow(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    subtitleContent: (@Composable () -> Unit)? = null,
    value: String? = null,
    badge: String? = null,
    enabled: Boolean = true,
    iconPainter: Painter? = null,
    iconContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .alpha(if (enabled) 1f else 0.45f)
            .heightIn(min = 52.dp)
            .padding(horizontal = ROW_INSET, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconContent != null) {
            iconContent()
        } else if (iconPainter != null) {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(ICON_SIZE),
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(ICON_SIZE),
            )
        }
        Spacer(Modifier.width(ICON_GAP))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Badge(badge)
                }
            }
            if (subtitleContent != null) {
                subtitleContent()
            } else if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 5,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        if (trailing != null) {
            trailing()
        } else if (value != null || onClick != null) {
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.width(4.dp))
            }
            Chevron()
        }
    }
}

@Composable
private fun CoverAnimationToggle(
    icon: ImageVector,
    title: Int,
    subtitle: Int,
    checked: Boolean,
    badge: String? = null,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    SettingsRow(
        icon = icon,
        title = stringResource(title),
        subtitle = stringResource(subtitle),
        badge = badge,
        enabled = enabled,
        trailing = { IosSwitch(checked = checked, onCheckedChange = onChange, enabled = enabled) },
        onClick = { onChange(!checked) },
    )
}

/**
 * Whether the [SettingsSubRow] being drawn is a row of its own, under a
 * divider or at the top of its card, rather than hanging off the row above.
 * Set per row by [SearchableSettingsGroup], which knows which rows are divided.
 */
private val LocalSubRowStandalone = staticCompositionLocalOf { false }

/**
 * A toggle that reads as part of the option above it rather than a setting
 * of its own: no icon, no divider, and pulled up close against its parent
 * instead of getting the same breathing room a full [SettingsRow] gets.
 *
 * Also drawn as a plain toggle row of its own, under a divider. There it gets
 * the same gap above its title as below it, or the title sits on the line.
 */
@Composable
internal fun SettingsSubRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    badge: String? = null,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .alpha(if (enabled) 1f else 0.45f)
            .padding(
                start = ROW_INSET,
                end = ROW_INSET,
                top = if (LocalSubRowStandalone.current) 10.dp else 0.dp,
                bottom = 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Badge(badge)
                }
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 5,
                )
            }
        }
        IosSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

/** Marks the connection whose ceiling is actually in force right now. */
@Composable
internal fun Badge(text: String) {
    Text(
        text = text.uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun AppIconNewBadge(text: String) {
    Text(
        text = text.uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF111113),
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color.White.copy(alpha = 0.94f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
internal fun Chevron() {
    Icon(
        SfIcons.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.size(20.dp),
    )
}

/** A continuous setting: label and current value on one line, track beneath. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SliderRow(
    icon: ImageVector,
    title: String,
    value: String,
    sliderValue: Float,
    onSliderValue: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    subtitle: String? = null,
) {
    val colors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.outline,
    )
    Column(Modifier.padding(start = ROW_INSET, end = ROW_INSET, top = 12.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(ICON_SIZE),
            )
            Spacer(Modifier.width(ICON_GAP))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = onSliderValue,
            valueRange = valueRange,
            steps = steps,
            colors = colors,
            // Bare track: the step ticks and the end-stop dot are noise when the
            // value is already spelled out on the line above.
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    colors = colors,
                    drawStopIndicator = null,
                    drawTick = { _, _ -> },
                )
            },
            modifier = Modifier.padding(start = ICON_SIZE + ICON_GAP),
        )
    }
}

/** Sign out: centered, accent-coloured, no glyph — the shape of a real one. */
@Composable
internal fun DestructiveRow(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Sliding pill selector, for the handful of settings with two or three states —
 * and the Search tab's source switcher. See [PillSegmentedControl].
 */
@Composable
internal fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    disabled: Set<Int> = emptySet(),
) {
    PillSegmentedControl(options, selectedIndex, onSelect, modifier, enabled, disabled)
}

// ---- Pages ------------------------------------------------------------------

/** Root destinations plus the legacy detail groups they currently compose. */
enum class SettingsCategory(@param:StringRes val title: Int) {
    PLAYER_LYRICS(R.string.settings_category_player_lyrics),
    AUDIO_REPRODUCTION(R.string.settings_category_audio_playback),
    APP_APPEARANCE(R.string.settings_category_appearance),
    LIBRARY_CONTENT(R.string.settings_category_library_content),
    DOWNLOADS_LOCAL(R.string.settings_category_downloads_local),
    STORAGE_CACHE(R.string.settings_category_storage_cache),
    YOUR_DATA_REPLAY(R.string.settings_category_your_data_replay),
    LANGUAGE_PERFORMANCE(R.string.settings_category_language_performance),
    DOWNLOADS(R.string.downloads),
    PLAYBACK(R.string.playback),
    CONTENT_PREFERENCES(R.string.content_preferences),
    APPEARANCE(R.string.appearance),
    PERFORMANCE(R.string.performance),
    LOCAL_MUSIC(R.string.local_music),
    STORAGE(R.string.storage),
    YOUR_DATA(R.string.your_data),
    MISCELLANEOUS(R.string.miscellaneous),
    LANGUAGE(R.string.language),
    ;

    val icon: ImageVector
        get() = when (this) {
            PLAYER_LYRICS -> SfIcons.Notes
            AUDIO_REPRODUCTION -> SfIcons.PlayCircle
            APP_APPEARANCE -> SfIcons.Appearance
            LIBRARY_CONTENT -> SfIcons.LibraryMusic
            DOWNLOADS_LOCAL -> SfIcons.Download
            STORAGE_CACHE -> SfIcons.Storage
            YOUR_DATA_REPLAY -> SfIcons.YourData
            LANGUAGE_PERFORMANCE -> SfIcons.Language
            DOWNLOADS -> SfIcons.Download
            PLAYBACK -> SfIcons.PlayCircle
            CONTENT_PREFERENCES -> SfIcons.Video
            APPEARANCE -> SfIcons.Appearance
            PERFORMANCE -> SfIcons.Performance
            LOCAL_MUSIC -> SfIcons.LibraryMusic
            STORAGE -> SfIcons.Storage
            YOUR_DATA -> SfIcons.YourData
            MISCELLANEOUS -> SfIcons.Miscellaneous
            LANGUAGE -> SfIcons.Language
        }
}

/** The settings whose options are a page of their own — see [SettingsChoicePage]. */
enum class SettingsChoiceKind(@param:StringRes val title: Int) {
    AUDIO_QUALITY_WIFI(R.string.on_wifi),
    AUDIO_QUALITY_CELLULAR(R.string.on_mobile_data),
    DOWNLOAD_QUALITY(R.string.download_quality),
    AUTOMIX_PERFORMANCE(R.string.automix_performance),
    BIT_PERFECT(R.string.bit_perfect_output),
    FONT(R.string.font),
    ACCENT(R.string.accent_color),
    COVER_ANIMATION(R.string.cover_animation),
}

/**
 * One of [SettingsChoiceKind]'s settings as a page: its options in a card,
 * the one in force ticked, and what the choice is about in the footer. A pick
 * is saved at once and [onChosen] then leaves the page, as the drawer this
 * replaces closed on one.
 */
@Composable
fun SettingsChoicePage(
    choice: SettingsChoiceKind,
    onChosen: () -> Unit,
    /** Asks before a bit-perfect mode is turned on — see [SettingsScreen]. */
    onConfirmBitPerfect: (BitPerfectMode) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onSpotifyCanvasAuth: () -> Unit = {},
) {
    val seenNewFeatures by AppSettings.seenNewFeatures.collectAsStateWithLifecycle()
    val coverAnimSlideIsNew = AppSettings.FEATURE_COVER_ANIM_SLIDE !in seenNewFeatures
    val coverAnimFluidIsNew = AppSettings.FEATURE_COVER_ANIM_FLUID !in seenNewFeatures
    val coverAnimAutomixIsNew = AppSettings.FEATURE_COVER_ANIM_AUTOMIX !in seenNewFeatures
    val disableSpotifyCanvasIsNew = AppSettings.FEATURE_DISABLE_SPOTIFY_CANVAS !in seenNewFeatures
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsPageTitle(stringResource(choice.title))
        when (choice) {
            SettingsChoiceKind.AUDIO_QUALITY_WIFI, SettingsChoiceKind.AUDIO_QUALITY_CELLULAR -> {
                val wifi = choice == SettingsChoiceKind.AUDIO_QUALITY_WIFI
                val wifiQuality by AppSettings.audioQualityWifi.collectAsStateWithLifecycle()
                val cellularQuality by AppSettings.audioQualityCellular.collectAsStateWithLifecycle()
                ChoiceGroup(
                    // Best first — the option most people want shouldn't be last.
                    options = AudioQuality.entries.reversed(),
                    selected = if (wifi) wifiQuality else cellularQuality,
                    label = { it.localizedLabel() },
                    detail = { stringResource(R.string.quality_hourly, it.detail, it.hourly) },
                    footer = stringResource(
                        R.string.audio_quality_connection,
                        stringResource(if (wifi) R.string.wifi else R.string.mobile_data).lowercase(Locale.getDefault()),
                    ),
                ) { quality ->
                    // Writes the one ceiling being edited and nothing else —
                    // which sources a rung consults is read per stream off the
                    // connection in force; see [AudioQuality.permits].
                    if (wifi) AppSettings.setAudioQualityWifi(quality) else AppSettings.setAudioQualityCellular(quality)
                    onChosen()
                }
            }

            // Priced per track rather than per hour, unlike the streaming
            // ceilings: a stream is paid for again on every replay, a download
            // once. Same list, different question.
            SettingsChoiceKind.DOWNLOAD_QUALITY -> {
                val selected by AppSettings.downloadQuality.collectAsStateWithLifecycle()
                ChoiceGroup(
                    options = DownloadQuality.entries.reversed(),
                    selected = selected,
                    label = { it.localizedLabel() },
                    detail = { stringResource(R.string.quality_per_track, it.detail, it.perTrack) },
                    footer = stringResource(R.string.download_quality_dialog_subtitle),
                ) { quality ->
                    AppSettings.setDownloadQuality(quality)
                    onChosen()
                }
            }

            SettingsChoiceKind.AUTOMIX_PERFORMANCE -> {
                val selected by AppSettings.automixPerformanceMode.collectAsStateWithLifecycle()
                ChoiceGroup(
                    options = AutomixPerformanceMode.entries,
                    selected = selected,
                    label = { it.localizedLabel() },
                    detail = {
                        stringResource(
                            when (it) {
                                AutomixPerformanceMode.EFFICIENT -> R.string.automix_mode_efficient_subtitle
                                AutomixPerformanceMode.BALANCED -> R.string.automix_mode_balanced_subtitle
                                AutomixPerformanceMode.PERFORMANCE -> R.string.automix_mode_performance_subtitle
                            },
                        )
                    },
                    footer = stringResource(R.string.automix_performance_warning),
                ) { mode ->
                    AppSettings.setAutomixPerformanceMode(mode)
                    onChosen()
                }
            }

            SettingsChoiceKind.BIT_PERFECT -> {
                val selected by AppSettings.bitPerfectMode.collectAsStateWithLifecycle()
                ChoiceGroup(
                    options = BitPerfectMode.entries,
                    selected = selected,
                    label = { it.localizedLabel() },
                    detail = { it.description() },
                    footer = stringResource(R.string.bit_perfect_sheet_warning),
                    enabled = { it.isSupported },
                ) { mode ->
                    onChosen()
                    // Leaving the mode needs no warning; entering it hands
                    // volume to the DAC, at full scale.
                    if (mode == BitPerfectMode.OFF) AppSettings.setBitPerfectMode(mode) else onConfirmBitPerfect(mode)
                }
            }

            SettingsChoiceKind.FONT -> {
                val selected by AppSettings.appFont.collectAsStateWithLifecycle()
                ChoiceGroup(
                    options = AppFont.entries,
                    selected = selected,
                    label = { it.localizedLabel() },
                    detail = { it.description() },
                    footer = stringResource(R.string.font_footer),
                ) { font ->
                    AppSettings.setAppFont(font)
                    onChosen()
                }
            }

            SettingsChoiceKind.ACCENT -> {
                val selected by AppSettings.accentColor.collectAsStateWithLifecycle()
                AccentColorPicker(selected = selected, onSelect = AppSettings::setAccentColor)
            }

            SettingsChoiceKind.COVER_ANIMATION -> {
                val slide by AppSettings.coverSlideAnimation.collectAsStateWithLifecycle()
                val fluid by AppSettings.coverFluidAnimation.collectAsStateWithLifecycle()
                val mixOnly by AppSettings.coverMixOnlyEffects.collectAsStateWithLifecycle()
                val animated by AppSettings.animatedCanvas.collectAsStateWithLifecycle()
                val cellular by AppSettings.canvasOverCellular.collectAsStateWithLifecycle()
                val disableSpotifyCanvas by AppSettings.disableSpotifyCanvas.collectAsStateWithLifecycle()
                SettingsGroup(header = stringResource(R.string.cover_transition_section), topSpacing = 0.dp) {
                    CoverAnimationToggle(
                        icon = SfIcons.Animation,
                        title = R.string.cover_animation_enabled,
                        subtitle = R.string.cover_animation_enabled_subtitle,
                        checked = slide,
                        badge = stringResource(R.string.settings_badge_new).takeIf { coverAnimSlideIsNew },
                        onChange = {
                            AppSettings.markFeatureSeen(AppSettings.FEATURE_COVER_ANIM_SLIDE)
                            AppSettings.setCoverSlideAnimation(it)
                        },
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        RowDivider()
                        CoverAnimationToggle(
                            icon = SfIcons.Waves,
                            title = R.string.fluid_cover_effect,
                            subtitle = if (slide) R.string.fluid_cover_effect_subtitle else R.string.cover_fluid_locked,
                            checked = fluid,
                            enabled = slide,
                            badge = stringResource(R.string.settings_badge_new).takeIf { coverAnimFluidIsNew },
                            onChange = {
                                AppSettings.markFeatureSeen(AppSettings.FEATURE_COVER_ANIM_FLUID)
                                AppSettings.setCoverFluidAnimation(it)
                            },
                        )
                    }
                    RowDivider()
                    CoverAnimationToggle(
                        icon = SfIcons.AutoAwesome,
                        title = R.string.animate_only_during_mix,
                        subtitle = if (slide) R.string.animate_only_during_mix_subtitle else R.string.cover_fluid_locked,
                        checked = mixOnly,
                        enabled = slide,
                        badge = stringResource(R.string.settings_badge_new).takeIf { coverAnimAutomixIsNew },
                        onChange = {
                            AppSettings.markFeatureSeen(AppSettings.FEATURE_COVER_ANIM_AUTOMIX)
                            AppSettings.setCoverMixOnlyEffects(it)
                        },
                    )
                }
                SettingsGroup(header = stringResource(R.string.animated_cover_section)) {
                    CoverAnimationToggle(
                        icon = SfIcons.Video,
                        title = R.string.animated_cover_art,
                        subtitle = R.string.animated_cover_art_subtitle,
                        checked = animated,
                        onChange = AppSettings::setAnimatedCanvas,
                    )
                    RowDivider()
                    CoverAnimationToggle(
                        icon = SfIcons.SignalCellularAlt,
                        title = R.string.play_animated_cover_over_cellular,
                        subtitle = if (animated) R.string.play_animated_cover_over_cellular_subtitle else R.string.animated_cover_locked,
                        checked = cellular,
                        enabled = animated,
                        onChange = AppSettings::setCanvasOverCellular,
                    )
                }
                SettingsGroup(header = stringResource(R.string.spotify_canvas_section)) {
                    val disableSpotifyCanvasTitle = stringResource(R.string.disable_spotify_canvas)
                    SettingsRow(
                        icon = SfIcons.MotionPhotosOff,
                        title = disableSpotifyCanvasTitle,
                        subtitle = stringResource(R.string.disable_spotify_canvas_subtitle),
                        badge = stringResource(R.string.settings_badge_new).takeIf { disableSpotifyCanvasIsNew },
                        trailing = {
                            IosSwitch(
                                checked = disableSpotifyCanvas,
                                onCheckedChange = { checked ->
                                    AppSettings.markFeatureSeen(AppSettings.FEATURE_DISABLE_SPOTIFY_CANVAS)
                                    AppSettings.setDisableSpotifyCanvas(checked)
                                },
                            )
                        },
                        onClick = {
                            AppSettings.markFeatureSeen(AppSettings.FEATURE_DISABLE_SPOTIFY_CANVAS)
                            AppSettings.setDisableSpotifyCanvas(!disableSpotifyCanvas)
                        },
                    )
                    RowDivider()
                    SettingsRow(
                        icon = SfIcons.Key,
                        title = stringResource(R.string.integrate_spotify_canvas),
                        subtitle = stringResource(if (animated) R.string.spotify_canvas_settings_subtitle else R.string.animated_cover_locked),
                        enabled = animated,
                        trailing = { Chevron() },
                        onClick = onSpotifyCanvasAuth,
                    )
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

/** The fixed global accents, with the selected swatch haloed and checked. */
@Composable
private fun AccentColorPicker(
    selected: AccentColor,
    onSelect: (AccentColor) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    SettingsGroup(footer = stringResource(R.string.accent_color_footer), topSpacing = 0.dp) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            AccentColor.entries.chunked(5).forEach { line ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ROW_INSET, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    line.forEach { accent ->
                        val tint = Color(accent.colorFor(dark))
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (accent == selected) tint.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelect(accent)
                                }
                                .padding(6.dp)
                                .background(tint, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (accent == selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = accent.localizedLabel(),
                                    tint = Color(if (dark) accent.onDarkColor else accent.onLightColor),
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                    repeat(5 - line.size) { Spacer(Modifier.size(52.dp)) }
                }
            }
        }
    }
}

/**
 * A card of options, one of them ticked: the body of every choice page. A
 * label and a dimmer line under it per option; [enabled] greys out the ones
 * this device can't take.
 */
@Composable
internal fun <T> ChoiceGroup(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    footer: String?,
    detail: (@Composable (T) -> String)? = null,
    enabled: (T) -> Boolean = { true },
    /** Set where the card is one of several on its page rather than the whole of it. */
    header: String? = null,
    onSelect: (T) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    // Every page it is the body of leads with its large heading — see
    // [SettingsPageTitle], whose own bottom padding is all the room it needs.
    SettingsGroup(header = header, footer = footer, topSpacing = 0.dp) {
        options.forEachIndexed { index, option ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = ROW_INSET),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            val available = enabled(option)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = available) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(option)
                    }
                    .alpha(if (available) 1f else 0.45f)
                    .heightIn(min = 52.dp)
                    .padding(horizontal = ROW_INSET, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = label(option),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    detail?.let {
                        Text(
                            text = it(option),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (option == selected) {
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        SfIcons.Check,
                        contentDescription = stringResource(R.string.selected),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
