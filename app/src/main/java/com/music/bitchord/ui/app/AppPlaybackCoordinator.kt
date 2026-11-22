package com.music.bitchord.ui.app

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.media3.session.MediaController
import com.music.bitchord.INITIAL_RADIO_TRACKS
import com.music.bitchord.R
import com.music.bitchord.data.TrackLog
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.listentogether.partyQueueIndexOf
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.PlaybackSourceType
import com.music.bitchord.data.model.QueueTier
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.TopPick
import com.music.bitchord.data.model.TopPickKind
import com.music.bitchord.data.model.TopPickMood
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.webdav.WebDavConfig
import com.music.bitchord.data.webdav.WebDavUploads
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.TopPicks
import com.music.bitchord.data.model.isUnresolvedSpotify
import com.music.bitchord.playback.QueueSource
import com.music.bitchord.playback.BoundedQueuePlanner
import com.music.bitchord.playback.QueueCoordinator
import com.music.bitchord.playback.QueueCoordinator.asQueueEntry
import com.music.bitchord.playback.beginRadioQueue
import com.music.bitchord.playback.cast.CastController
import com.music.bitchord.playback.commitRadioQueue
import com.music.bitchord.playback.fromAutoplay
import com.music.bitchord.playback.loadAutoplayTracks
import com.music.bitchord.playback.playBoundedContext
import com.music.bitchord.playback.playSongs
import com.music.bitchord.playback.smart.VersionAudioAligner
import com.music.bitchord.playback.swapToVersion
import com.music.bitchord.playback.toMediaItem
import com.music.bitchord.playback.toSong
import com.music.bitchord.ui.MainViewModel
import com.music.bitchord.data.innertube.InnertubeParser
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.ui.components.BrowseTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class VersionSwitchController(
    val switchingAudioVersionState: MutableState<Boolean>,
    val convertedFromVideoState: MutableState<Song?>,
    val convertedAudioIdState: MutableState<String?>,
    val convertedFromAudioState: MutableState<Song?>,
    val convertedVideoIdState: MutableState<String?>,
    val keepVideoIdState: MutableState<String?>,
    val optimisticVersionSongState: MutableState<Song?>,
    val missingVersions: MutableSet<String>,
) {
    var switchingAudioVersion: Boolean
        get() = switchingAudioVersionState.value
        set(value) { switchingAudioVersionState.value = value }

    var convertedFromVideo: Song?
        get() = convertedFromVideoState.value
        set(value) { convertedFromVideoState.value = value }

    var convertedAudioId: String?
        get() = convertedAudioIdState.value
        set(value) { convertedAudioIdState.value = value }

    var convertedFromAudio: Song?
        get() = convertedFromAudioState.value
        set(value) { convertedFromAudioState.value = value }

    var convertedVideoId: String?
        get() = convertedVideoIdState.value
        set(value) { convertedVideoIdState.value = value }

    var keepVideoId: String?
        get() = keepVideoIdState.value
        set(value) { keepVideoIdState.value = value }

    var optimisticVersionSong: Song?
        get() = optimisticVersionSongState.value
        set(value) { optimisticVersionSongState.value = value }

    fun onSongChanged(
        currentVideoId: String?,
        onClearActiveRadioSeed: () -> Unit,
    ) {
        if (optimisticVersionSong?.videoId == currentVideoId ||
            (optimisticVersionSong != null &&
                currentVideoId != convertedAudioId &&
                currentVideoId != convertedVideoId &&
                currentVideoId != keepVideoId)
        ) {
            optimisticVersionSong = null
        }
        onClearActiveRadioSeed()
        if (keepVideoId != currentVideoId) keepVideoId = null
        if (currentVideoId != convertedAudioId) {
            convertedFromVideo = null
            convertedAudioId = null
            switchingAudioVersion = false
        }
        if (currentVideoId != convertedVideoId) {
            convertedFromAudio = null
            convertedVideoId = null
            switchingAudioVersion = false
        }
    }

    suspend fun switchToMusicOnly(
        controller: MediaController?,
        song: Song,
        pauseWhileResolving: Boolean,
        versionContext: Context,
        showQueueNotice: (String) -> Unit,
    ) {
        val c = controller ?: return
        val index = c.currentMediaItemIndex
        if (index !in 0 until c.mediaItemCount ||
            c.currentMediaItem?.mediaId != song.videoId ||
            switchingAudioVersion
        ) return

        val resumeAfterResolution = pauseWhileResolving && c.playWhenReady
        switchingAudioVersion = true
        keepVideoId = null
        val holdUntilAligned = AppSettings.smartVersionAlignment.value
        if (holdUntilAligned) AppSettings.versionAlignmentInProgress.value = true
        if (pauseWhileResolving) c.pause()
        try {
            TrackLog.d("Player", "audio switch requested for '${song.title}'", song.videoId)
            val audio = runCatching { YtMusicRepository.resolveAudio(song) }.getOrNull()
            val stillCurrent = c.currentMediaItemIndex == index &&
                c.currentMediaItem?.mediaId == song.videoId

            if (audio == null || audio.videoId == song.videoId) {
                TrackLog.w("Player", "audio switch found no distinct official song", song.videoId)
                if (song.videoId !in missingVersions) missingVersions += song.videoId
                if (stillCurrent) showQueueNotice(versionContext.getString(R.string.no_audio_version))
                if (stillCurrent && resumeAfterResolution) c.play()
                return
            }
            if (!stillCurrent) {
                TrackLog.d("Player", "audio switch discarded; listener changed track", song.videoId)
                return
            }

            convertedFromVideo = song
            convertedAudioId = audio.videoId
            TrackLog.d("Player", "audio switch applying '${audio.title}' (${audio.videoId})", song.videoId)
            val target = audio.copy(
                isVideoOrigin = true,
                queueTier = song.queueTier,
                queueEntryId = song.queueEntryId,
                radioName = song.radioName,
                playbackSource = song.playbackSource,
                playbackSourceType = song.playbackSourceType,
                playbackSourceId = song.playbackSourceId,
            )
            if (!holdUntilAligned ||
                VersionAudioAligner.getCachedOffsetMs(song.videoId, target.videoId) != null ||
                YtMusicRepository.versionTimeMap(song.videoId, target.videoId) != null
            ) {
                optimisticVersionSong = target
            }
            if (holdUntilAligned) AppSettings.versionAlignmentInProgress.value = false
            c.swapToVersion(target)
        } finally {
            if (holdUntilAligned) AppSettings.versionAlignmentInProgress.value = false
            switchingAudioVersion = false
        }
    }

    suspend fun switchToVideo(
        controller: MediaController?,
        song: Song,
        pauseWhileResolving: Boolean,
        versionContext: Context,
        showQueueNotice: (String) -> Unit,
    ) {
        val c = controller ?: return
        val index = c.currentMediaItemIndex
        if (index !in 0 until c.mediaItemCount ||
            c.currentMediaItem?.mediaId != song.videoId ||
            switchingAudioVersion
        ) return

        val resumeAfterResolution = pauseWhileResolving && c.playWhenReady
        switchingAudioVersion = true
        val holdUntilAligned = AppSettings.smartVersionAlignment.value
        if (holdUntilAligned) AppSettings.versionAlignmentInProgress.value = true
        if (pauseWhileResolving) c.pause()
        try {
            TrackLog.d("Player", "video switch requested for '${song.title}'", song.videoId)
            val video = runCatching { YtMusicRepository.resolveVideo(song) }.getOrNull()
            val stillCurrent = c.currentMediaItemIndex == index &&
                c.currentMediaItem?.mediaId == song.videoId

            if (video == null || video.videoId == song.videoId) {
                TrackLog.w("Player", "video switch found no distinct video", song.videoId)
                if (song.videoId !in missingVersions) missingVersions += song.videoId
                if (stillCurrent) showQueueNotice(versionContext.getString(R.string.no_video_version))
                if (stillCurrent && resumeAfterResolution) c.play()
                return
            }
            if (!stillCurrent) {
                TrackLog.d("Player", "video switch discarded; listener changed track", song.videoId)
                return
            }

            convertedFromAudio = song
            convertedVideoId = video.videoId
            TrackLog.d("Player", "video switch applying '${video.title}' (${video.videoId})", song.videoId)
            val target = video.copy(
                queueTier = song.queueTier,
                queueEntryId = song.queueEntryId,
                radioName = song.radioName,
                playbackSource = song.playbackSource,
                playbackSourceType = song.playbackSourceType,
                playbackSourceId = song.playbackSourceId,
            )
            if (!holdUntilAligned ||
                VersionAudioAligner.getCachedOffsetMs(song.videoId, target.videoId) != null ||
                YtMusicRepository.versionTimeMap(song.videoId, target.videoId) != null
            ) {
                optimisticVersionSong = target
            }
            if (holdUntilAligned) AppSettings.versionAlignmentInProgress.value = false
            c.swapToVersion(target)
        } finally {
            if (holdUntilAligned) AppSettings.versionAlignmentInProgress.value = false
            switchingAudioVersion = false
        }
    }
}

@Composable
internal fun rememberVersionSwitchController(): VersionSwitchController {
    val switchingAudioVersion = remember { mutableStateOf(false) }
    val convertedFromVideo = remember { mutableStateOf<Song?>(null) }
    val convertedAudioId = remember { mutableStateOf<String?>(null) }
    val convertedFromAudio = remember { mutableStateOf<Song?>(null) }
    val convertedVideoId = remember { mutableStateOf<String?>(null) }
    val keepVideoId = remember { mutableStateOf<String?>(null) }
    val optimisticVersionSong = remember { mutableStateOf<Song?>(null) }
    val missingVersions = remember { mutableSetOf<String>() }
    return remember {
        VersionSwitchController(
            switchingAudioVersionState = switchingAudioVersion,
            convertedFromVideoState = convertedFromVideo,
            convertedAudioIdState = convertedAudioId,
            convertedFromAudioState = convertedFromAudio,
            convertedVideoIdState = convertedVideoId,
            keepVideoIdState = keepVideoId,
            optimisticVersionSongState = optimisticVersionSong,
            missingVersions = missingVersions,
        )
    }
}

internal fun uploadTracksToWebDav(
    scope: CoroutineScope,
    context: Context,
    songs: List<Song>,
    topDetailBrowseId: String?,
    viewModel: MainViewModel,
    showQueueNotice: (String) -> Unit,
) {
    scope.launch {
        val summary = WebDavUploads.upload(context, songs)
        if (summary.total > 0) {
            showQueueNotice(
                context.getString(
                    R.string.webdav_upload_summary,
                    summary.uploaded,
                    summary.skipped,
                    summary.failed,
                ),
            )
        }
        if (summary.uploaded > 0 &&
            topDetailBrowseId == WebDavConfig.BROWSE_ID
        ) {
            viewModel.reloadLocalDetail(WebDavConfig.BROWSE_ID)
        }
    }
}

internal fun openByName(
    context: Context,
    scope: CoroutineScope,
    viewModel: MainViewModel,
    browseId: String?,
    name: String,
    subtitle: String?,
    type: BrowseType,
    artwork: String? = null,
) {
    if (browseId != null) {
        val credit = subtitle ?: context.getString(
            if (type == BrowseType.ARTIST) R.string.artist else R.string.album,
        )
        viewModel.openDetail(browseId, name, credit, artwork, type)
        return
    }
    scope.launch {
        val filter = if (type == BrowseType.ARTIST) {
            SearchFilter.ARTISTS
        } else {
            SearchFilter.ALBUMS
        }
        val query = listOfNotNull(name, subtitle).joinToString(" ")
        val hit = YtMusicRepository.search(query, filter).getOrNull()
            ?.filterIsInstance<SearchResult.Browse>()
            ?.firstOrNull()
            ?.item
        if (hit == null) {
            Toast.makeText(
                context,
                context.getString(R.string.couldnt_find, name),
                Toast.LENGTH_SHORT,
            ).show()
        } else {
            viewModel.openDetail(
                hit.browseId,
                hit.title,
                hit.subtitle,
                hit.thumbnailUrl ?: artwork,
                hit.type,
            )
        }
    }
}

internal fun playFrom(
    scope: CoroutineScope,
    controller: MediaController?,
    playerQueue: List<Song>,
    allSongs: List<Song>,
    allIndex: Int,
    source: QueueSource,
    boundedContext: Boolean = false,
    continuationToken: String? = null,
    refusedByHost: () -> Boolean,
    onPlayRequested: () -> Unit = {},
) {
    if (allSongs.getOrNull(allIndex)?.isUnresolvedSpotify == true) return
    val songs = allSongs.filterNot { it.isUnresolvedSpotify }
    val index = songs.indexOf(allSongs.getOrNull(allIndex)).coerceAtLeast(0)
    onPlayRequested()
    scope.launch {
        if (refusedByHost()) return@launch
        val c = controller ?: return@launch
        val currentTimeline = playerQueue.takeIf { it.size == c.mediaItemCount }
            ?: (0 until c.mediaItemCount).map { c.getMediaItemAt(it).toSong() }
        val currentIndex = c.currentMediaItemIndex

        if (ListenTogether.state.value.inParty) {
            val selectedSong = songs.getOrNull(index) ?: return@launch
            val party = ListenTogether.state.value
            val partyQueue = party.queue.items
            val partyIndex = partyQueueIndexOf(party.queue, party.playback, party.playback.track?.videoId)
            val upcomingPartyTracks = if (partyIndex >= 0) {
                partyQueue.drop(partyIndex + 1)
            } else {
                emptyList()
            }
            val timeline = QueueCoordinator.buildPartyPlaybackQueue(
                tappedSong = selectedSong,
                source = source,
                upcomingPartyTracks = upcomingPartyTracks,
            )
            c.playSongs(timeline, 0)
        } else {
            val bounded = boundedContext || songs.size > BoundedQueuePlanner.MAX_PHYSICAL_CONTEXT
            if (bounded && !CastController.state.value.casting) {
                c.playBoundedContext(songs, index, source, continuationToken)
            } else {
                val result = QueueCoordinator.buildContextQueue(
                    currentTimeline = currentTimeline,
                    currentIndex = currentIndex,
                    newContextSongs = songs,
                    selectedIndex = index,
                    contextSource = source,
                )
                c.playSongs(result.timeline, result.startIndex)
            }
        }
    }
}

internal fun startRadioStation(
    scope: CoroutineScope,
    context: Context,
    controller: MediaController?,
    song: Song,
    refusedByHost: () -> Boolean,
    onPlayRequested: () -> Int,
    isCurrentRequest: (Int) -> Boolean,
    onActiveRadioSeedChanged: (Pair<String, String>?) -> Unit,
) {
    val originalController = controller
    if (originalController != null && !refusedByHost()) {
        val request = onPlayRequested()
        val originalManualQueue = (0 until originalController.mediaItemCount)
            .map { originalController.getMediaItemAt(it) }
            .filterNot { it.fromAutoplay }
            .map { it.mediaId }
        scope.launch {
            val seed = song.copy(
                radioName = song.title,
                playbackSource = song.title,
                playbackSourceType = PlaybackSourceType.SHARED_LINK,
                playbackSourceId = song.videoId,
                isLiveRadioStream = false,
            )
            val related = loadAutoplayTracks(
                existing = listOf(seed),
                seedSong = seed,
                limit = INITIAL_RADIO_TRACKS,
            ).getOrElse {
                if (isCurrentRequest(request)) {
                    Toast.makeText(context, R.string.couldnt_load_tracks, Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            if (related.isEmpty()) {
                if (isCurrentRequest(request)) {
                    Toast.makeText(context, R.string.couldnt_load_tracks, Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            val activeController = controller ?: return@launch
            val activeManualQueue = (0 until activeController.mediaItemCount)
                .map { activeController.getMediaItemAt(it) }
                .filterNot { it.fromAutoplay }
                .map { it.mediaId }
            if (!isCurrentRequest(request) || activeManualQueue != originalManualQueue) {
                return@launch
            }
            activeController.beginRadioQueue()
            val currentIndex = activeController.currentMediaItemIndex
            val currentItem = activeController.currentMediaItem
            if (currentIndex >= 0 && currentItem?.mediaId == song.videoId) {
                activeController.addMediaItems(currentIndex + 1, related.map { it.toMediaItem() })
                val oldTail = currentIndex + 1 + related.size
                if (oldTail < activeController.mediaItemCount) {
                    activeController.removeMediaItems(oldTail, activeController.mediaItemCount)
                }
                if (currentIndex > 0) activeController.removeMediaItems(0, currentIndex)
                onActiveRadioSeedChanged(song.videoId to song.title)
            } else {
                onActiveRadioSeedChanged(null)
                activeController.playSongs(listOf(seed) + related, 0)
            }
            activeController.commitRadioQueue()
            Toast.makeText(
                context,
                context.getString(R.string.radio_started, song.title),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
}

internal fun openTopPickCard(
    scope: CoroutineScope,
    context: Context,
    controller: MediaController?,
    viewModel: MainViewModel,
    pick: TopPick,
    label: String,
    refusedByHost: () -> Boolean,
    onPlayRequested: () -> Int,
    isCurrentRequest: (Int) -> Boolean,
    onActiveRadioSeedChanged: (Pair<String, String>?) -> Unit,
) {
    val browseId = pick.browseId
    if (pick.kind == TopPickKind.LATEST_RELEASE && browseId != null) {
        viewModel.openDetail(
            browseId = browseId,
            title = pick.title,
            subtitle = pick.artistName.orEmpty(),
            thumbnailUrl = pick.images.firstOrNull(),
        )
    } else if (controller != null && !refusedByHost()) {
        val request = onPlayRequested()
        scope.launch {
            val songs = TopPicks.queue(pick).getOrNull().orEmpty()
            if (!isCurrentRequest(request)) return@launch
            val activeController = controller ?: return@launch
            if (songs.isEmpty()) {
                Toast.makeText(context, R.string.couldnt_load_tracks, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val sourceId = pick.watch?.playlistId ?: pick.mood?.name
            val station = songs.map {
                it.copy(
                    radioName = label,
                    playbackSource = label,
                    playbackSourceType = PlaybackSourceType.HOME,
                    playbackSourceId = sourceId,
                )
            }
            onActiveRadioSeedChanged(null)
            activeController.beginRadioQueue()
            activeController.playSongs(station, 0)
            activeController.commitRadioQueue()
        }
    }
}

internal fun enqueueSingleTrack(
    scope: CoroutineScope,
    context: Context,
    controller: MediaController?,
    playerQueue: List<Song>,
    song: Song,
    next: Boolean,
    queueLabel: String,
    refusedByHost: () -> Boolean,
    showQueueNotice: (String) -> Unit,
) {
    scope.launch {
        if (refusedByHost()) return@launch
        controller?.let {
            if (ListenTogether.state.value.inParty) {
                val upcoming = (it.mediaItemCount - (it.currentMediaItemIndex + 1)).coerceAtLeast(0)
                if (upcoming >= 25) {
                    showQueueNotice(context.getString(R.string.party_queue_full, 25))
                    return@launch
                }
            }
            val current = it.currentMediaItem?.toSong()
            val timeline = playerQueue.takeIf { q -> q.size == it.mediaItemCount }
                ?: (0 until it.mediaItemCount).map { idx -> it.getMediaItemAt(idx).toSong() }
            val at = QueueCoordinator.findUserQueueInsertionIndex(
                timeline = timeline,
                currentIndex = it.currentMediaItemIndex,
                isNext = next,
            )
            val queued = song.copy(
                radioName = current?.radioName,
                playbackSource = current?.playbackSource ?: queueLabel,
                playbackSourceType = current?.playbackSourceType ?: PlaybackSourceType.QUEUE,
                playbackSourceId = current?.playbackSourceId,
            ).asQueueEntry(QueueTier.USER_QUEUE)
            it.addMediaItem(at, queued.toMediaItem())
            showQueueNotice(
                context.getString(
                    if (next) R.string.song_will_play_next else R.string.song_added_to_queue,
                ),
            )
        }
    }
}

internal fun enqueueBatchSongs(
    scope: CoroutineScope,
    context: Context,
    controller: MediaController?,
    playerQueue: List<Song>,
    songs: List<Song>,
    next: Boolean,
    queueLabel: String,
    refusedByHost: () -> Boolean,
    showQueueNotice: (String) -> Unit,
    playFallback: (List<Song>, Int) -> Unit,
) {
    if (songs.isNotEmpty()) {
        scope.launch {
            if (refusedByHost()) return@launch
            val c = controller
            if (c == null || c.mediaItemCount == 0) {
                playFallback(songs, 0)
            } else {
                val toAdd = if (ListenTogether.state.value.inParty) {
                    val upcoming = (c.mediaItemCount - (c.currentMediaItemIndex + 1)).coerceAtLeast(0)
                    val slotsLeft = (25 - upcoming).coerceAtLeast(0)
                    if (slotsLeft <= 0) {
                        showQueueNotice(context.getString(R.string.party_queue_full, 25))
                        return@launch
                    }
                    songs.take(slotsLeft)
                } else {
                    songs
                }
                val timeline = playerQueue.takeIf { q -> q.size == c.mediaItemCount }
                    ?: (0 until c.mediaItemCount).map { idx -> c.getMediaItemAt(idx).toSong() }
                val at = QueueCoordinator.findUserQueueInsertionIndex(
                    timeline = timeline,
                    currentIndex = c.currentMediaItemIndex,
                    isNext = next,
                )
                val current = c.currentMediaItem?.toSong()
                c.addMediaItems(
                    at,
                    toAdd.map {
                        it.copy(
                            radioName = current?.radioName,
                            playbackSource = current?.playbackSource ?: queueLabel,
                            playbackSourceType = current?.playbackSourceType ?: PlaybackSourceType.QUEUE,
                            playbackSourceId = current?.playbackSourceId,
                        ).asQueueEntry(QueueTier.USER_QUEUE).toMediaItem()
                    },
                )
                val message = context.resources.getQuantityString(
                    if (next) R.plurals.songs_will_play_next else R.plurals.songs_added_to_queue,
                    toAdd.size,
                    toAdd.size,
                )
                showQueueNotice(message)
            }
        }
    }
}

internal fun shelfSong(item: ShelfItem): Song? {
    return item.videoId?.let { videoId ->
        Song(
            videoId = videoId,
            title = item.title,
            artist = InnertubeParser.artistFromSubtitle(item.subtitle),
            thumbnailUrl = item.thumbnailUrl,
            isVideo = item.isVideo,
        )
    }
}

internal fun withBrowseSongs(
    context: Context,
    viewModel: MainViewModel,
    target: BrowseTarget,
    action: (List<Song>) -> Unit,
) {
    val stamp: (List<Song>) -> Unit = { songs ->
        action(
            songs.map { song ->
                song.copy(
                    albumName = if (target.type == BrowseType.ALBUM) {
                        song.albumName ?: target.title
                    } else {
                        song.albumName
                    },
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
                onFailure = {
                    val message = it.message ?: context.getString(R.string.couldnt_load_tracks)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                },
            )
        }
    }
}

