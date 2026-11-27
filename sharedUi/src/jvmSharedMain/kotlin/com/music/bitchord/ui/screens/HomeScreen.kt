package com.music.bitchord.ui.screens

import com.music.bitchord.ui.components.coverSharedElement
import com.music.bitchord.ui.components.rememberCoverTap
import com.music.bitchord.ui.components.trackArtSize
import com.music.bitchord.ui.components.contextClick
import com.music.bitchord.ui.AppUi
import com.music.bitchord.sharedui.resources.*
import org.jetbrains.compose.resources.painterResource
import com.music.bitchord.ui.festive.GoldenLogo
import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.foundation.Image
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.music.bitchord.ui.components.longPressMenuClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import com.music.bitchord.ui.components.ShelfRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Storage
import com.music.bitchord.ui.icons.BitChordIcons
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.ShelfLayout
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.TopPick
import com.music.bitchord.data.model.TopPickMood
import com.music.bitchord.data.model.UiState
import java.util.Locale
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.ui.components.MessageState
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.RowMoreButton
import com.music.bitchord.ui.components.PullToRefresh
import com.music.bitchord.ui.components.SHELF_CARD_WIDTH
import com.music.bitchord.ui.components.VideoThumbnails
import com.music.bitchord.ui.components.SearchPlayingBars
import com.music.bitchord.ui.components.SignInBanner
import com.music.bitchord.ui.components.SongMixes4RowSkeleton
import com.music.bitchord.ui.components.feedMoreSkeleton
import com.music.bitchord.ui.components.feedSkeleton
import com.music.bitchord.ui.components.thumbnailBorder
import com.music.bitchord.ui.components.trackColumnWidth
import com.music.bitchord.ui.player.MeshGradientBackground
import com.music.bitchord.ui.player.MeshPalette

/** How far the Listen now heading and feed sit above the shared top inset. */
private val HOME_TITLE_LIFT = 28.dp

private const val RECENTS_TITLE = "Recents"
private const val RECENT_TRACKS_PER_COLUMN = 4

private fun List<ShelfItem>.playingItemMatcher(song: Song?): (ShelfItem) -> Boolean {
    song ?: return { false }
    // The card that was played, when the shelf holds it, and only that one —
    // see [com.music.bitchord.data.model.playingRowMatcher], which rows use.
    if (any { it.videoId == song.videoId }) return { it.videoId == song.videoId }
    val byName = firstOrNull { it.videoId != null && it.title == song.title && it.subtitle == song.artist }
        ?: return { false }
    return { it == byName }
}

/** In-memory singleton store for Home row scroll positions across compositions */
object HomeScrollStateHolder {
    val rowScrollStates = mutableMapOf<String, Pair<Int, Int>>()

    fun getRowScroll(key: String): Pair<Int, Int> = rowScrollStates[key] ?: (0 to 0)

    fun saveRowScroll(key: String, index: Int, offset: Int) {
        rowScrollStates[key] = index to offset
    }
}

/** In-memory singleton store for native Home shelves across reloads and tab navigations */
object NativeHomeShelvesHolder {
    var lastValidDiscoveriesFavorites: HomeShelf? = null
    var lastValidDailyDiscovery: HomeShelf? = null
    var lastValidSongMixes: HomeShelf? = null
}

private val KidsJunkTerms = listOf(
    "biper", "gallina pintadita", "patito juan", "luli pampin", "cocomelon", "baby shark",
    "plim plim", "vaca lola", "reino infantil", "zenón", "zenon", "cantajuego", "pinkfong",
    "canciones infantiles", "canción infantil", "cancion infantil", "infantil", "infantiles",
    "canciones de cuna", "cancion de cuna", "lullaby", "lullabies", "nursery rhyme", "nursery rhymes",
    "para dormir bebés", "para dormir bebes", "música infantil", "musica infantil", "para niños", "para ninos",
    "kids songs", "super simple songs", "chuchu tv", "toobys", "toddler", "canciones para niños",
)

private fun isKidsOrNurseryTrack(title: String, subtitle: String = ""): Boolean {
    val t = title.lowercase(Locale.ROOT)
    val s = subtitle.lowercase(Locale.ROOT)
    return KidsJunkTerms.any { t.contains(it) || s.contains(it) }
}

private fun isViewCountSubtitle(subtitle: String): Boolean {
    val s = subtitle.lowercase(Locale.ROOT)
    return s.contains("visualizaciones") || s.contains("views") || s.contains("vistas") ||
        s.contains("lượt xem") || s.contains("m de visualizaciones") || s.contains("k de visualizaciones")
}

private fun isVideoTitle(title: String): Boolean {
    val t = title.lowercase(Locale.ROOT)
    return t.contains("(official video)") || t.contains("(video oficial)") ||
        t.contains("(official music video)") || t.contains("(video musical oficial)") ||
        t.contains("happy birthday") || t.contains("cumpleaños feliz")
}

private fun isCleanMusicTrack(item: ShelfItem): Boolean {
    if (item.isVideo) return false
    if (isViewCountSubtitle(item.subtitle)) return false
    if (isVideoTitle(item.title)) return false
    if (isKidsOrNurseryTrack(item.title, item.subtitle)) return false
    val bId = item.browseId
    return item.videoId != null || (bId != null && !bId.startsWith("UC"))
}

private fun isCleanIndividualSong(item: ShelfItem): Boolean {
    if (item.videoId.isNullOrBlank()) return false
    if (item.isVideo) return false
    if (isViewCountSubtitle(item.subtitle)) return false
    if (isVideoTitle(item.title)) return false
    if (isKidsOrNurseryTrack(item.title, item.subtitle)) return false
    val bId = item.browseId
    if (bId != null) {
        val bLower = bId.lowercase(Locale.ROOT)
        if (bLower.startsWith("vl") || bLower.startsWith("rd") || bLower.startsWith("mp") || bLower.startsWith("uc")) {
            return false
        }
    }
    val sub = item.subtitle.lowercase(Locale.ROOT)
    if (sub.contains("playlist") || sub.contains("lista de reproducción") ||
        sub.contains("álbum") || sub.contains("album") || sub.contains("ep") ||
        sub.contains("mix") || sub.contains("radio")) {
        return false
    }
    return true
}

class HomeRowScrollController(
    val getScroll: (String) -> Pair<Int, Int> = { HomeScrollStateHolder.getRowScroll(it) },
    val saveScroll: (String, Int, Int) -> Unit = { key, index, offset ->
        HomeScrollStateHolder.saveRowScroll(key, index, offset)
    },
)

val LocalHomeRowScrollController = staticCompositionLocalOf { HomeRowScrollController() }

@Composable
fun rememberHomePersistentRowState(sectionKey: String): LazyListState {
    val controller = LocalHomeRowScrollController.current
    val (initialIndex, initialOffset) = remember(sectionKey) {
        controller.getScroll(sectionKey)
    }
    val rowState = rememberSaveable(sectionKey, saver = LazyListState.Saver) {
        LazyListState(initialIndex, initialOffset)
    }

    DisposableEffect(sectionKey, rowState) {
        onDispose {
            controller.saveScroll(sectionKey, rowState.firstVisibleItemIndex, rowState.firstVisibleItemScrollOffset)
        }
    }

    LaunchedEffect(sectionKey, rowState) {
        snapshotFlow { rowState.firstVisibleItemIndex to rowState.firstVisibleItemScrollOffset }
            .collect { (idx, off) ->
                controller.saveScroll(sectionKey, idx, off)
            }
    }

    return rowState
}

private fun HomeShelf.isHomeExcluded(): Boolean {
    val label = title.trim().lowercase()
    val isPrograms = listOf("programas para ti", "programas", "shows for you", "podcasts", "podcast", "episodios", "episodes").any(label::contains)
    val isRecents = title.equals(RECENTS_TITLE, ignoreCase = true) ||
        label == "recents" || label == "recently played" || label == "recientes"
    return isPrograms || isRecents
}

private val VideoJunkTerms = listOf(
    "karaoke", "sing king", "instrumental", "8d audio", "sped up", "slowed",
    "lyrics", "letra", "nightcore", "bass boosted", "tribute",
    "mix", "playlist", "recopilacion", "recopilación", "compilation",
    "serenata", "serenatas",
    "concierto", "conciertos", "concert", "en vivo", "live at", "live from", "live in",
    "festival", "festival.", "tour", "world tour", "en directo", "full show",
    "full set", "set live", "liveset", "live session", "live performance",
    "dj set", "boiler room", "tomorrowland", "coachella", "rolling loud",
    "lollapalooza", "astroworld", "primavera sound", "rock in rio",
)

private fun isCleanOfficialMusicVideo(item: ShelfItem): Boolean {
    // 1. Debe ser un video individual válido con videoId
    if (item.videoId.isNullOrBlank()) return false

    // 2. No debe ser una lista de reproducción ni colección
    val bId = item.browseId
    if (bId != null) {
        val bLower = bId.lowercase(Locale.ROOT)
        if (bLower.startsWith("vl") || bLower.startsWith("rd") || bLower.startsWith("mp") || bLower.startsWith("uc")) {
            return false
        }
    }

    // 3. Chequeo de términos no deseados (mixes, playlists, serenatas, conciertos, festivales, etc.)
    val t = item.title.lowercase(Locale.ROOT)
    val s = item.subtitle.lowercase(Locale.ROOT)

    if (VideoJunkTerms.any { t.contains(it) || s.contains(it) }) return false
    if (isKidsOrNurseryTrack(item.title, item.subtitle)) return false

    // 4. Excluir si el subtítulo indica playlist, álbum, mix o radio
    if (s.contains("playlist") || s.contains("lista de reproducción") ||
        s.contains("álbum") || s.contains("album") || s.contains("mix") ||
        s.contains("radio") || s.contains("recopilación") || s.contains("recopilacion")) {
        return false
    }

    return true
}

internal fun HomeShelf.isListenAgainShelf(): Boolean {
    val label = title.trim().lowercase()
    return label == "listen again" || label == "vuelve a escuchar" ||
        label == "escuchar de nuevo" || label == "nghe lại" ||
        label == "もう一度聴く" || label == "다시 듣기" ||
        label.contains("listen again") || label.contains("vuelve a escuchar")
}

private val DaypartKnownTitles = setOf(
    "para empezar el día", "start your day",
    "energía matutina", "morning energy",
    "novedades para ti", "new for you",
    "música para concentrarte", "music to focus",
    "dale al gym", "work out",
    "en modo enfoque", "focus mode",
    "tendencias de hoy", "today's trends",
    "recarga de tarde", "afternoon recharge",
    "para el camino", "on the road",
    "vibra de previa", "party starters",
    "tardeo y chill", "sunset & chill",
    "solo imprescindibles", "just essentials",
    "tus favoritos de siempre", "all-time favorites",
    "modo nocturno", "night mode",
    "para bajar el ritmo", "slow down",
    "sesión de madrugada", "late night session",
    "antes de dormir", "before sleep",
    "workout", "chill", "essentials",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState<List<HomeShelf>>,
    listState: LazyListState,
    /** The tapped item and the recommendation shelf it came from. */
    onItemClick: (ShelfItem, String) -> Unit,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    /** The page's heading, or null for none — the desktop's pages carry no heading. */
    title: String?,
    /** Casts the wordmark above the heading in glowing gold, for a festival. */
    festiveLogo: Boolean = false,
    /** Tapping the festive wordmark, e.g. to bring its greeting back. */
    onFestiveLogoClick: (() -> Unit)? = null,
    signedIn: Boolean = true,
    onSignIn: (() -> Unit)? = null,
    /**
     * Holding a card rather than tapping it. Every card on the feed answers,
     * whichever kind it is: the caller reads the item the same way it does for
     * a tap, so a track card opens the track menu and a card that points at a
     * collection opens the album / playlist one.
     */
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    // Explore doesn't page — only Home has a continuation worth following.
    onLoadMore: (() -> Unit)? = null,
    loadingMore: Boolean = false,
    recentlyPlayedLoading: Boolean = false,
    currentSong: Song? = null,
    isPlaying: Boolean = false,
    /**
     * The "Top Picks for You" row above Recents. Null while it has not been
     * built yet, empty when there is nothing to pick from. The row is only
     * drawn by a caller that can act on it, i.e. passes [onTopPickClick].
     */
    topPicks: List<TopPick>? = null,
    topPicksLoading: Boolean = false,
    /** The signed-in account's name, for the personal station's "Name's Station". */
    listenerName: String? = null,
    onTopPickClick: ((TopPick, String) -> Unit)? = null,
    onTopPickLongPress: ((TopPick) -> Unit)? = null,
    /** A Find Your Mood tile, and its name. The shelf is only drawn when this is set. */
    onMoodClick: ((TopPickMood, String) -> Unit)? = null,
    /** Find Your Mood's Show all: every mood and activity. */
    onMoodsShowAll: () -> Unit = {},
    /** Whether the Top Picks and mood artwork may animate — see [LocalArtworkMotion]. */
    artworkMotion: Boolean = true,
    editorialShelves: List<HomeShelf> = emptyList(),
    daypartShelves: List<HomeShelf> = emptyList(),
    personalizedEssentialsShelf: HomeShelf? = null,
    stationsForYou: List<TopPick>? = null,
    onStationClick: ((TopPick, String) -> Unit)? = onTopPickClick,
) {
    // Top Picks' pictures and motion wait for the feed — see LocalArtworkImages.
    val feedReady = state !is UiState.Loading
    val visibleShelves = remember(state) {
        (state as? UiState.Success)?.data?.filter { !it.isHomeExcluded() }.orEmpty()
    }
    val dynamicShelf1 = daypartShelves.getOrNull(0)
    val dynamicShelf2 = daypartShelves.getOrNull(1)
    val dynamicShelf3 = daypartShelves.getOrNull(2)
    val dynamicShelf4 = daypartShelves.getOrNull(3)
    val listenAgainShelf = remember(visibleShelves) {
        visibleShelves.firstOrNull { it.isListenAgainShelf() }
    }
    val albumsAndSinglesShelf = remember(visibleShelves) {
        visibleShelves.firstOrNull { s ->
            val t = s.title.trim().lowercase(Locale.ROOT)
            t.contains("new release") || t.contains("novedades") || t.contains("lanzamientos") ||
                t.contains("albums & singles") || t.contains("álbumes y singles") || t.contains("bản phát hành mới") ||
                t.contains("mới phát hành") || t.contains("latest")
        }
    }
    val musicVideosShelf = remember(visibleShelves) {
        val found = visibleShelves.firstOrNull { s ->
            val t = s.title.trim().lowercase(Locale.ROOT)
            s.layout == ShelfLayout.VIDEO ||
                t.contains("music video") || t.contains("vídeo") || t.contains("video") ||
                s.items.all { it.isVideo }
        }
        val cleanItems = found?.items?.filter { isCleanOfficialMusicVideo(it) }.orEmpty()
        if (cleanItems.size >= 10) {
            found
        } else {
            // Pool official music videos from all visible shelves (history, recommendations, artist sections)
            val pooledVideos = mutableListOf<ShelfItem>()
            pooledVideos.addAll(cleanItems)

            visibleShelves.forEach { shelf ->
                shelf.items.forEach { item ->
                    if (isCleanOfficialMusicVideo(item) && pooledVideos.none { it.videoId == item.videoId }) {
                        pooledVideos.add(item)
                    }
                }
            }

            if (pooledVideos.isNotEmpty()) {
                val base = found ?: HomeShelf(
                    title = "Videos musicales",
                    items = emptyList(),
                    subtitle = "",
                    layout = ShelfLayout.VIDEO,
                )
                base.copy(items = pooledVideos.take(12), layout = ShelfLayout.VIDEO)
            } else {
                found
            }
        }
    }
    val essentialsShelf = personalizedEssentialsShelf ?: remember(visibleShelves, editorialShelves) {
        editorialShelves.firstOrNull { s ->
            val t = s.title.trim().lowercase(Locale.ROOT)
            t == "imprescindibles" || t == "essentials" || t.contains("imprescindibles") || t.contains("essentials")
        } ?: visibleShelves.firstOrNull { s ->
            val t = s.title.trim().lowercase(Locale.ROOT)
            t == "imprescindibles" || t == "essentials" || t.contains("imprescindibles") || t.contains("essentials")
        }
    }
    val songMixesShelf = remember(visibleShelves, albumsAndSinglesShelf, musicVideosShelf) {
        val found = visibleShelves.firstOrNull { s ->
            if (s.layout == ShelfLayout.VIDEO || s == albumsAndSinglesShelf || s == musicVideosShelf || s.isListenAgainShelf()) return@firstOrNull false
            val t = s.title.trim().lowercase(Locale.ROOT)
            if (t.contains("video") || t.contains("vídeo") || t.contains("podcast") || t.contains("programa")) return@firstOrNull false
            (s.layout == ShelfLayout.LIST ||
                t.contains("mixed") || t.contains("quick") || t.contains("picks") ||
                t.contains("mixes para ti") || t.contains("dành riêng") || t.contains("selección") ||
                t.contains("song mix")) && s.items.any { isCleanMusicTrack(it) }
        }?.let { raw ->
            val cleanItems = raw.items.filter { isCleanMusicTrack(it) }
            if (cleanItems.isNotEmpty()) raw.copy(items = cleanItems) else null
        }

        if (found != null && found.items.size >= 4) {
            NativeHomeShelvesHolder.lastValidSongMixes = found
            found
        } else {
            NativeHomeShelvesHolder.lastValidSongMixes?.takeIf { it.items.size >= 4 } ?: found
        }
    }
    val libraryShelf = remember(visibleShelves) {
        visibleShelves.firstOrNull { s ->
            if (s.layout == ShelfLayout.VIDEO) return@firstOrNull false
            val t = s.title.trim().lowercase(Locale.ROOT)
            if (t.contains("video") || t.contains("vídeo")) return@firstOrNull false
            t.contains("from your library") || t.contains("từ thư viện") || t.contains("de tu biblioteca") ||
                t.contains("library") || t.contains("biblioteca")
        }
    }
    val discoveriesAndFavoritesShelf = remember(visibleShelves) {
        val found = visibleShelves.firstOrNull { s ->
            if (s.layout == ShelfLayout.VIDEO) return@firstOrNull false
            val t = s.title.trim().lowercase(Locale.ROOT)
            if (t.contains("video") || t.contains("vídeo") || t.contains("podcast") || t.contains("programa")) return@firstOrNull false
            val isMixShelf = t.contains("forgotten") || t.contains("olvidados") || t.contains("quen thuộc") ||
                t.contains("archive") || t.contains("archivo") || t.contains("favoritos de siempre") ||
                t.contains("temas escuchados") || t.contains("replay mix") || t.contains("deep cuts") ||
                t.contains("música que me gusta")
            isMixShelf && s.items.any { !it.isVideo && !isViewCountSubtitle(it.subtitle) }
        }?.let { raw ->
            val cleanItems = raw.items.filter { !it.isVideo && !isViewCountSubtitle(it.subtitle) && !isVideoTitle(it.title) }
            if (cleanItems.isNotEmpty()) raw.copy(items = cleanItems) else null
        }
        found?.also { NativeHomeShelvesHolder.lastValidDiscoveriesFavorites = it }
            ?: NativeHomeShelvesHolder.lastValidDiscoveriesFavorites
    }
    val dailyDiscoveryShelf = remember(visibleShelves, songMixesShelf, albumsAndSinglesShelf, musicVideosShelf, listenAgainShelf, discoveriesAndFavoritesShelf) {
        val found = visibleShelves.firstOrNull { s ->
            if (s.layout == ShelfLayout.VIDEO || s == albumsAndSinglesShelf || s == musicVideosShelf || s == songMixesShelf || s == discoveriesAndFavoritesShelf || s.isListenAgainShelf()) return@firstOrNull false
            val t = s.title.trim().lowercase(Locale.ROOT)
            if (t.contains("video") || t.contains("vídeo") || t.contains("podcast") || t.contains("programa") || t.contains("shows")) return@firstOrNull false
            val isDiscoveryTitle = t.contains("daily discovery") || t.contains("descubrimiento diario") ||
                t.contains("discovery mix") || t.contains("mix de descubrimientos") ||
                t.contains("descubrimiento") || t.contains("discovery") || t.contains("descubre") ||
                t.contains("mixed for you") || (t.contains("para ti") && !t.contains("mixes")) ||
                (t.contains("for you") && !t.contains("mixes"))
            isDiscoveryTitle && s.items.any { isCleanMusicTrack(it) }
        }?.let { raw ->
            // Si la shelf contiene canciones individuales (con videoId), filtramos estrictamente solo canciones
            val trackItems = raw.items.filter { isCleanMusicTrack(it) && it.videoId != null }
            if (trackItems.isNotEmpty()) {
                raw.copy(items = trackItems)
            } else {
                val cleanItems = raw.items.filter { isCleanMusicTrack(it) }
                if (cleanItems.isNotEmpty()) raw.copy(items = cleanItems) else null
            }
        }
        found?.also { NativeHomeShelvesHolder.lastValidDailyDiscovery = it }
            ?: NativeHomeShelvesHolder.lastValidDailyDiscovery
    }
    val customMixesShelf = remember(visibleShelves, songMixesShelf, dailyDiscoveryShelf, discoveriesAndFavoritesShelf, musicVideosShelf) {
        visibleShelves.firstOrNull { s ->
            if (s.layout == ShelfLayout.VIDEO || s == musicVideosShelf) return@firstOrNull false
            val t = s.title.trim().lowercase(Locale.ROOT)
            if (t.contains("video") || t.contains("vídeo") || t.contains("podcast") || t.contains("programa")) return@firstOrNull false
            s != songMixesShelf && s != dailyDiscoveryShelf && s != discoveriesAndFavoritesShelf && !s.isListenAgainShelf() &&
                (t.contains("your mixes") || t.contains("tus mixes") || t.contains("artist mix") ||
                    t.contains("custom mixes") || t.contains("mixes personalizados") ||
                    t.contains("my mix") || t.contains("mi mix") || t.contains("mix")) &&
                s.items.any { !it.isVideo && !isViewCountSubtitle(it.subtitle) }
        }?.let { raw ->
            val cleanItems = raw.items.filter { !it.isVideo && !isViewCountSubtitle(it.subtitle) && !isVideoTitle(it.title) }
            if (cleanItems.isNotEmpty()) raw.copy(items = cleanItems) else null
        }
    }
    val albumsAndSinglesTitle = stringResource(Res.string.shelf_new_releases)
    val musicVideosTitle = stringResource(Res.string.shelf_music_videos)
    val essentialsTitle = stringResource(Res.string.shelf_essentials)
    val songMixesTitle = stringResource(Res.string.shelf_song_mixes)
    val libraryTitle = stringResource(Res.string.shelf_from_your_library)
    val discoveriesAndFavoritesTitle = stringResource(Res.string.shelf_discoveries_and_favorites)
    val dailyDiscoveryTitle = stringResource(Res.string.shelf_daily_discovery)
    val customMixesTitle = stringResource(Res.string.shelf_custom_mixes)
    CompositionLocalProvider(
        LocalArtworkMotion provides (artworkMotion && feedReady),
        LocalArtworkImages provides feedReady,
    ) {
    // The loops also hold still while the feed is being scrolled — see
    // LocalArtworkScrolling for why that is asked rather than provided.
    PausedWhileScrolling(listState) {

    PullToRefresh(
        refreshing = refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            // The heading sits higher than the other tabs' first row: pull the
            // whole feed up by [HOME_TITLE_LIFT] with it.
            contentPadding = if (title != null) {
                PaddingValues(
                    top = (contentPadding.calculateTopPadding() - HOME_TITLE_LIFT).coerceAtLeast(0.dp),
                    bottom = contentPadding.calculateBottomPadding(),
                )
            } else {
                contentPadding
            },
        ) {
            if (title != null) {
                item(key = "home-title") { HomeTitle(title, festiveLogo = festiveLogo, onFestiveLogoClick = onFestiveLogoClick) }
            }
            if (!signedIn && onSignIn != null) {
                item(key = "home-signin") {
                    // Read in this item, so a scroll starting or ending
                    // recomposes the banner alone.
                    SignInBanner(
                        onSignIn = onSignIn,
                        modifier = Modifier.padding(bottom = 8.dp),
                        motion = LocalArtworkMotion.current && !listState.isScrollInProgress,
                    )
                }
            }
            // 1. Zona Superior Fija: Carrusel Hero superior ("Top selecciones para ti")
            if (onTopPickClick != null) {
                when {
                    !topPicks.isNullOrEmpty() -> item(key = "top-picks") {
                        TopPicksShelf(
                            picks = topPicks,
                            listenerName = listenerName,
                            onClick = onTopPickClick,
                            onLongPress = onTopPickLongPress,
                        )
                    }
                    topPicks == null && topPicksLoading -> topPicksSkeleton()
                }
            }
            when (state) {
                is UiState.Loading -> {
                    feedSkeleton()
                }
                is UiState.Error -> item(key = "home-error") {
                    MessageState(state.message, actionLabel = stringResource(Res.string.retry), onAction = onRetry)
                }
                is UiState.Success -> {
                    // 2. Zona Superior Fija: Sección fija "Vuelve a escuchar" (con subtítulo de usuario)
                    if (listenAgainShelf != null && listenAgainShelf.items.isNotEmpty()) {
                        item(key = "fixed-listen-again", contentType = "listen_again_shelf") {
                            ListenAgainShelf(
                                shelf = listenAgainShelf,
                                listenerName = listenerName,
                                onItemClick = { item -> onItemClick(item, listenAgainShelf.title) },
                                onItemLongPress = onItemLongPress,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                            )
                        }
                    }

                    // 3. Zona Superior Fija: Sección fija "Estado de ánimo" ("Find Your Mood")
                    if (onMoodClick != null) {
                        item(key = "find-your-mood", contentType = "find_your_mood") {
                            FindYourMoodShelf(onMoodClick, onMoodsShowAll)
                        }
                    }

                    // Dinámico #1 (Género + Hora del día): Justo debajo de Estado de ánimo
                    if (dynamicShelf1 != null && dynamicShelf1.items.isNotEmpty()) {
                        item(
                            key = "daypart-shelf-1-${dynamicShelf1.title}",
                            contentType = "shelf_cards",
                        ) {
                            Shelf(
                                shelf = dynamicShelf1,
                                onItemClick = { item -> onItemClick(item, dynamicShelf1.title) },
                                onItemLongPress = onItemLongPress,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                            )
                        }
                    }

                    // 5. Álbumes y singles (tarjetas cuadradas de nuevos álbumes y sencillos)
                    if (albumsAndSinglesShelf != null && albumsAndSinglesShelf.items.isNotEmpty()) {
                        item(key = "fixed-albums-singles", contentType = "shelf_cards") {
                            val shelf = albumsAndSinglesShelf.copy(
                                title = albumsAndSinglesTitle,
                                layout = ShelfLayout.CARDS,
                            )
                            Shelf(
                                shelf = shelf,
                                onItemClick = { item -> onItemClick(item, shelf.title) },
                                onItemLongPress = onItemLongPress,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                            )
                        }
                    }

                    // 5.1 Emisoras para ti (Ticket #60: carrusel fijo de 10 a 12 tarjetas cuadradas personalizadas desde YouTube Music)
                    if (!stationsForYou.isNullOrEmpty() && onStationClick != null) {
                        item(key = "fixed-stations-for-you", contentType = "shelf_stations") {
                            StationsForYouShelf(
                                stations = stationsForYou,
                                listenerName = listenerName,
                                onClick = onStationClick,
                            )
                        }
                    }

                    // 6. Videos musicales (tarjetas panorámicas 16:9 con filtro estricto: solo videos oficiales de artistas, no mixes/conciertos/serenatas)
                    if (musicVideosShelf != null && musicVideosShelf.items.isNotEmpty()) {
                        val cleanItems = musicVideosShelf.items.filter { isCleanOfficialMusicVideo(it) }
                        if (cleanItems.isNotEmpty()) {
                            item(key = "fixed-music-videos", contentType = "shelf_videos") {
                                val shelf = musicVideosShelf.copy(
                                    title = musicVideosTitle,
                                    items = cleanItems,
                                    layout = ShelfLayout.VIDEO,
                                )
                                Shelf(
                                    shelf = shelf,
                                    onItemClick = { item -> onItemClick(item, shelf.title) },
                                    onItemLongPress = onItemLongPress,
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                )
                            }
                        }
                    }

                    // 7. Imprescindibles (sección fija dedicada con playlists Essentials / Imprescindibles)
                    if (essentialsShelf != null && essentialsShelf.items.isNotEmpty()) {
                        item(key = "fixed-essentials", contentType = "shelf_cards") {
                            val shelf = essentialsShelf.copy(
                                title = essentialsTitle,
                                layout = ShelfLayout.CARDS,
                            )
                            Shelf(
                                shelf = shelf,
                                onItemClick = { item -> onItemClick(item, shelf.title) },
                                onItemLongPress = onItemLongPress,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                            )
                        }
                    }

                    // 8. Mixes de canciones (cuadrícula/carrusel compacto de 4 filas con mixes de canciones)
                    val effectiveSongMixes = songMixesShelf?.takeIf { it.items.size >= 4 }
                        ?: NativeHomeShelvesHolder.lastValidSongMixes?.takeIf { it.items.size >= 4 }

                    if (effectiveSongMixes != null && effectiveSongMixes.items.size >= 4) {
                        item(key = "fixed-song-mixes", contentType = "shelf_list") {
                            val shelf = effectiveSongMixes.copy(
                                title = songMixesTitle,
                                layout = ShelfLayout.LIST,
                            )
                            RecentShelf(
                                shelf = shelf,
                                onItemClick = { item -> onItemClick(item, shelf.title) },
                                onItemLongPress = onItemLongPress,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                            )
                        }
                    } else if (state is UiState.Loading || (songMixesShelf != null && songMixesShelf.items.size < 4)) {
                        item(key = "fixed-song-mixes-skeleton", contentType = "shelf_list_skeleton") {
                            SongMixes4RowSkeleton(title = songMixesTitle)
                        }
                    }

                    // Dinámico #2 ("Porque escuchas a [Artista]"): Entre Mixes de canciones y De tu biblioteca
                    if (dynamicShelf2 != null && dynamicShelf2.items.isNotEmpty()) {
                        item(
                            key = "daypart-shelf-2-${dynamicShelf2.title}",
                            contentType = "shelf_cards",
                        ) {
                            Shelf(
                                shelf = dynamicShelf2,
                                onItemClick = { item -> onItemClick(item, dynamicShelf2.title) },
                                onItemLongPress = onItemLongPress,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                            )
                        }
                    }

                    // 10. De tu biblioteca (carrusel horizontal con álbumes y playlists guardadas/creadas)
                    if (libraryShelf != null && libraryShelf.items.isNotEmpty()) {
                        item(key = "fixed-library", contentType = "shelf_cards") {
                            val shelf = libraryShelf.copy(
                                title = libraryTitle,
                                layout = ShelfLayout.CARDS,
                            )
                            Shelf(
                                shelf = shelf,
                                onItemClick = { item -> onItemClick(item, shelf.title) },
                                onItemLongPress = onItemLongPress,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                            )
                        }
                    }

                    // 11. Nuevos descubrimientos y los favoritos de siempre (carrusel horizontal)
                    if (discoveriesAndFavoritesShelf != null && discoveriesAndFavoritesShelf.items.isNotEmpty()) {
                        item(key = "fixed-discoveries-favorites", contentType = "shelf_cards") {
                            val shelf = discoveriesAndFavoritesShelf.copy(
                                title = discoveriesAndFavoritesTitle,
                                layout = ShelfLayout.CARDS,
                            )
                            Shelf(
                                shelf = shelf,
                                onItemClick = { item -> onItemClick(item, shelf.title) },
                                onItemLongPress = onItemLongPress,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                            )
                        }
                    }

                    // Dinámico #3 ("Para concentrarte" / "Tardeo y chill" / "Para relajarte" / "Sesión de madrugada"): Entre Nuevos descubrimientos y Tu descubrimiento diario
                    if (dynamicShelf3 != null && dynamicShelf3.items.isNotEmpty()) {
                        item(
                            key = "daypart-shelf-3-${dynamicShelf3.title}",
                            contentType = "shelf_cards",
                        ) {
                            Shelf(
                                shelf = dynamicShelf3,
                                onItemClick = { item -> onItemClick(item, dynamicShelf3.title) },
                                onItemLongPress = onItemLongPress,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                            )
                        }
                    }

                    // Dinámico #4 ("Para empezar con ritmo" / "Tendencias en tu estilo" / "Para bajar el ritmo"): Antes de Tu descubrimiento diario
                    if (dynamicShelf4 != null && dynamicShelf4.items.isNotEmpty()) {
                        item(
                            key = "daypart-shelf-4-${dynamicShelf4.title}",
                            contentType = "shelf_cards",
                        ) {
                            Shelf(
                                shelf = dynamicShelf4,
                                onItemClick = { item -> onItemClick(item, dynamicShelf4.title) },
                                onItemLongPress = onItemLongPress,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                            )
                        }
                    }

                    // 12. Tu descubrimiento diario (carrusel horizontal entre Para bajar el ritmo y Mixes personalizados)
                    if (dailyDiscoveryShelf != null && dailyDiscoveryShelf.items.isNotEmpty()) {
                        item(key = "fixed-daily-discovery", contentType = "shelf_cards") {
                            val shelf = dailyDiscoveryShelf.copy(
                                title = dailyDiscoveryTitle,
                                layout = ShelfLayout.CARDS,
                            )
                            Shelf(
                                shelf = shelf,
                                onItemClick = { item -> onItemClick(item, shelf.title) },
                                onItemLongPress = onItemLongPress,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                            )
                        }
                    }

                    // 13. Mixes personalizados (carrusel horizontal con mixes por artista: Mi Mix 1, Mi Mix 2...)
                    if (customMixesShelf != null && customMixesShelf.items.isNotEmpty()) {
                        item(key = "fixed-custom-mixes", contentType = "shelf_cards") {
                            val shelf = customMixesShelf.copy(
                                title = customMixesTitle,
                                layout = ShelfLayout.CARDS,
                            )
                            Shelf(
                                shelf = shelf,
                                onItemClick = { item -> onItemClick(item, shelf.title) },
                                onItemLongPress = onItemLongPress,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                            )
                        }
                    }

                    if (loadingMore) feedMoreSkeleton()
                }
            }
        }
    }

    }
    }

    if (onLoadMore != null && state is UiState.Success) {
        val loadMore by rememberUpdatedState(onLoadMore)
        // Re-checked on every layout change, rather than on the rising edge of
        // "the tail is in view". A page that appends only a shelf or two leaves
        // the list still near its end, so an edge-triggered effect would never
        // fire a second time: the feed dead-ended at the bottom with no
        // skeleton and no request in flight to explain it. Restarting on
        // [loadingMore] re-checks the moment a page settles, so the next one is
        // asked for while the tail is still on screen to show it loading.
        LaunchedEffect(listState, loadingMore) {
            snapshotFlow {
                val layout = listState.layoutInfo
                (layout.visibleItemsInfo.lastOrNull()?.index ?: -1) to layout.totalItemsCount
            }.collect { (lastVisible, total) ->
                if (!loadingMore && total > 0 && lastVisible >= total - 3) loadMore()
            }
        }
    }
}

/** Complementary discovery shelves: recommended releases, suggested artists/albums, community mixes/playlists. */
private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedShelves(
    shelves: List<HomeShelf>,
    onItemClick: (ShelfItem, String) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    currentSong: Song?,
    isPlaying: Boolean,
    editorialShelves: List<HomeShelf> = emptyList(),
) {
    val placedEditorial = mutableSetOf<Int>()

    shelves.forEachIndexed { index, shelf ->
        if (index in 0 until editorialShelves.size) {
            val edShelf = editorialShelves[index]
            placedEditorial.add(index)
            item(key = "editorial-shelf-${edShelf.title}-$index", contentType = "shelf_cards") {
                Shelf(
                    shelf = edShelf.copy(subtitle = ""),
                    onItemClick = { item -> onItemClick(item, edShelf.title) },
                    onItemLongPress = onItemLongPress,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                )
            }
        }
        item(key = shelf.title + index, contentType = "shelf_${shelf.layout.name}") {
            val openItem: (ShelfItem) -> Unit = { item -> onItemClick(item, shelf.title) }
            // Listen again and Quick picks are lists on the web player: one pager for both.
            if (shelf.layout == ShelfLayout.LIST) {
                RecentShelf(
                    shelf = shelf,
                    onItemClick = openItem,
                    onItemLongPress = onItemLongPress,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                )
            } else {
                Shelf(
                    shelf = shelf,
                    onItemClick = openItem,
                    onItemLongPress = onItemLongPress,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                )
            }
        }
    }

    editorialShelves.forEachIndexed { idx, edShelf ->
        if (idx !in placedEditorial) {
            item(key = "editorial-shelf-${edShelf.title}-$idx", contentType = "shelf_cards") {
                Shelf(
                    shelf = edShelf.copy(subtitle = ""),
                    onItemClick = { item -> onItemClick(item, edShelf.title) },
                    onItemLongPress = onItemLongPress,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                )
            }
        }
    }
}

private val ListenAgainShape = RoundedCornerShape(18.dp)

/**
 * Fixed upper zone section: "Vuelve a escuchar" / "Listen Again"
 * Horizontal carousel of square cards (RoundedCornerShape(18.dp))
 * mixing albums, playlists, and tracks with 2 clean lines under each cover.
 */
@Composable
private fun ListenAgainShelf(
    shelf: HomeShelf,
    listenerName: String?,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    currentSong: Song?,
    isPlaying: Boolean,
) {
    Column(Modifier.padding(bottom = 26.dp)) {
        Column(
            modifier = Modifier
                .padding(horizontal = PAGE_GUTTER, vertical = 10.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = stringResource(Res.string.shelf_listen_again),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val userSubtitle = listenerName?.trim()?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.listen_again_default_sub)
            Spacer(Modifier.height(2.dp))
            Text(
                text = userSubtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val isCurrentItem = remember(shelf.items, currentSong) { shelf.items.playingItemMatcher(currentSong) }
        val rowState = rememberHomePersistentRowState("home_listen_again_${shelf.title}")
        ShelfRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(
                items = shelf.items,
                key = { item -> item.videoId ?: item.browseId ?: "${shelf.title}_${item.title}" },
                contentType = { "listen_again_card" },
            ) { item ->
                ListenAgainCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onLongPress = onItemLongPress?.let { { it(item) } },
                    isCurrent = isCurrentItem(item),
                    isPlaying = isCurrentItem(item) && isPlaying,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListenAgainCard(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier.width(SHELF_CARD_WIDTH),
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
) {
    val cardToken = remember { Any() }
    val openCard = rememberCoverTap(item.browseId ?: item.videoId, cardToken, onClick)
    Column(
        modifier = modifier
            .longPressMenuClickable(onClick = openCard, onLongClick = onLongPress)
            .contextClick(onLongPress),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(ListenAgainShape)
                .coverSharedElement(item.browseId ?: item.videoId, ListenAgainShape, cardToken)
                .thumbnailBorder(ListenAgainShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (isCurrent && isPlaying) SearchPlayingBars(Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.height(10.dp))
        // Línea 1: Título en negrita
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 18.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        Spacer(Modifier.height(2.dp))
        // Línea 2: Badge/separador en una sola línea (Álbum · Artista, Playlist · Descripción o Canción · Artista)
        val badgeText = formatListenAgainBadge(item)
        Text(
            text = badgeText,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                lineHeight = 16.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun formatListenAgainBadge(item: ShelfItem): String {
    val isTrack = item.videoId != null
    val isAlbum = !isTrack && (item.browseId?.startsWith("MPRE") == true || item.subtitle.contains("album", ignoreCase = true))
    val isPlaylist = !isTrack && (item.browseId?.startsWith("VL") == true || item.browseId?.startsWith("PL") == true || item.subtitle.contains("playlist", ignoreCase = true))

    val typeLabel = when {
        isAlbum -> stringResource(Res.string.shelf_album_item)
        isPlaylist -> stringResource(Res.string.playlist_label)
        isTrack -> stringResource(Res.string.shelf_song_item)
        else -> if (item.browseId != null) stringResource(Res.string.shelf_album_item) else stringResource(Res.string.shelf_song_item)
    }

    val cleanedSubtitle = item.subtitle
        .replace(Regex("""^(Song|Single|Album|Playlist|Canción|Sencillo|Álbum|Danh sách phát|Bài hát|Đĩa đơn)\s*[•·]\s*""", RegexOption.IGNORE_CASE), "")
        .trim()
        .ifBlank { item.title }

    val localizedSub = localizeCardSubtitle(cleanedSubtitle)
        .replace(" • ", " · ")
        .replace(" •", " ·")
        .replace("• ", "· ")

    return "$typeLabel · $localizedSub"
}

/** The same four-rows-per-page treatment used by an artist's Top songs. */
@Composable
private fun RecentShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    currentSong: Song?,
    isPlaying: Boolean,
) {
    Column(Modifier.padding(bottom = 26.dp)) {
        RecentSectionHeader(title = localizeShelfTitle(shelf.title), subtitle = localizeShelfSubtitle(shelf.subtitle))
        val isCurrentItem = remember(shelf.items, currentSong) { shelf.items.playingItemMatcher(currentSong) }
        val chunkedColumns = remember(shelf.items) { shelf.items.chunked(RECENT_TRACKS_PER_COLUMN) }
        BoxWithConstraints {
            val columnWidth = trackColumnWidth(maxWidth)
            val rowState = rememberHomePersistentRowState("home_recent_${shelf.title}")
            ShelfRow(
                state = rowState,
                contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = chunkedColumns,
                    key = { column -> column.firstOrNull()?.videoId ?: column.firstOrNull()?.browseId ?: column.hashCode() },
                    contentType = { "recent_column" },
                ) { column ->
                    Column(Modifier.width(columnWidth)) {
                        column.forEachIndexed { index, item ->
                            RecentTrackRow(
                                item = item,
                                onClick = { onItemClick(item) },
                                onLongPress = onItemLongPress?.let { { it(item) } },
                                isCurrent = isCurrentItem(item),
                                isPlaying = isCurrentItem(item) && isPlaying,
                            )
                            if (index < column.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(
                                        start = if (item.isVideo) 97.dp else 60.dp,
                                    ),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSectionHeader(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .padding(horizontal = PAGE_GUTTER, vertical = 10.dp)
            .fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RecentTrackRow(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    isCurrent: Boolean,
    isPlaying: Boolean,
) {
    CompactTrackRow(
        title = item.title,
        subtitle = item.subtitle,
        thumbnailUrl = item.thumbnailUrl,
        onClick = onClick,
        onLongPress = onLongPress,
        isCurrent = isCurrent,
        isPlaying = isPlaying,
        isVideo = item.isVideo,
    )
}

/**
 * One row of Recents' four-to-a-column pager. Public so the desktop artist page
 * can list its top songs the same way.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactTrackRow(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    /** A music video: its thumbnail at 16:9 rather than squared off. */
    isVideo: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress)
            .contextClick(onLongPress)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.trackArtSize(48.dp, isVideo)) {
            AsyncImage(
                model = thumbnailUrl.artworkAt(ROW_ART_PX),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(7.dp))
                    .thumbnailBorder(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            if (isCurrent && isPlaying) SearchPlayingBars(Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onLongPress != null) {
            RowMoreButton(onClick = onLongPress)
        }
    }
}

/**
 * Listen now's large heading, with the wordmark stacked above it.
 *
 * Only the home tab carries it — the top bar no longer shows the logo, so this
 * is where the app's mark lives. Its height is taken from the heading's own
 * font size rather than a fixed dp, so the two keep their proportion under the
 * system font scale.
 */
@Composable
private fun HomeTitle(
    title: String,
    modifier: Modifier = Modifier,
    festiveLogo: Boolean = false,
    onFestiveLogoClick: (() -> Unit)? = null,
) {
    val style = MaterialTheme.typography.displayLarge
    val logoHeight = with(LocalDensity.current) { (style.fontSize * LOGO_TO_FONT).toDp() }
    Column(modifier = modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp)) {
        val logoModifier = Modifier
            .padding(bottom = 14.dp)
            .height(logoHeight)
            .aspectRatio(LOGO_ASPECT)
        if (festiveLogo) {
            GoldenLogo(
                painter = painterResource(Res.drawable.ic_logo),
                modifier = logoModifier,
                onClick = onFestiveLogoClick,
            )
        } else {
            Icon(
                painter = painterResource(Res.drawable.ic_logo),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = logoModifier,
            )
        }
        // "Listen now" is hidden for now; the wordmark above stays.
        // Text(
        //     text = title,
        //     style = style,
        //     color = MaterialTheme.colorScheme.onBackground,
        // )
    }
}

/** The wordmark's height against the heading's font size. */
private const val LOGO_TO_FONT = 0.9f

/** ic_logo's viewport, 730 x 484. */
private const val LOGO_ASPECT = 730f / 484f

/**
 * Shared by the home feed, Explore and Library so headings line up across tabs.
 *
 * [onShowAll] is only ever set on Library, whose rows stop at five cards
 * rather than running the shelf's whole length — see [LibraryGridShelf].
 * Home and Explore never pass it, so their heading is unchanged.
 */
@Composable
internal fun SectionHeader(title: String, subtitle: String = "", onShowAll: (() -> Unit)? = null) {
    val displayTitle = localizeShelfTitle(title)
    val displaySubtitle = localizeShelfSubtitle(subtitle)
    Row(
        modifier = Modifier
            .padding(horizontal = PAGE_GUTTER, vertical = 10.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (displaySubtitle.isNotBlank()) {
                Text(
                    text = displaySubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onShowAll != null) {
            Text(
                text = stringResource(Res.string.show_all),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onShowAll)
                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
            )
        }
    }
}
@Composable
internal fun localizeShelfTitle(title: String): String {
    val trimmed = title.trim()
    return when {
        trimmed.equals("Para empezar el día", ignoreCase = true) ||
            trimmed.equals("Start Your Day", ignoreCase = true) ->
            stringResource(Res.string.daypart_morning_shelf_1)
        trimmed.equals("Energía matutina", ignoreCase = true) ||
            trimmed.equals("Morning Energy", ignoreCase = true) ->
            stringResource(Res.string.daypart_morning_shelf_2)
        trimmed.equals("Novedades para ti", ignoreCase = true) ||
            trimmed.equals("New for You", ignoreCase = true) ->
            stringResource(Res.string.daypart_morning_shelf_3)
        trimmed.equals("Música para concentrarte", ignoreCase = true) ||
            trimmed.equals("Music to Focus", ignoreCase = true) ->
            stringResource(Res.string.daypart_morning_shelf_4)
        trimmed.equals("En modo enfoque", ignoreCase = true) ||
            trimmed.equals("Focus Mode", ignoreCase = true) ->
            stringResource(Res.string.daypart_afternoon_shelf_2)
        trimmed.equals("Tendencias de hoy", ignoreCase = true) ||
            trimmed.equals("Today's Trends", ignoreCase = true) ->
            stringResource(Res.string.daypart_afternoon_shelf_3)
        trimmed.equals("Recarga de tarde", ignoreCase = true) ||
            trimmed.equals("Afternoon Recharge", ignoreCase = true) ->
            stringResource(Res.string.daypart_afternoon_shelf_4)
        trimmed.equals("Para el camino", ignoreCase = true) ||
            trimmed.equals("On the Road", ignoreCase = true) ->
            stringResource(Res.string.daypart_evening_shelf_1)
        trimmed.equals("Vibra de previa", ignoreCase = true) ||
            trimmed.equals("Party Starters", ignoreCase = true) ->
            stringResource(Res.string.daypart_evening_shelf_1_party)
        trimmed.equals("Tardeo y chill", ignoreCase = true) ||
            trimmed.equals("Sunset & Chill", ignoreCase = true) ->
            stringResource(Res.string.daypart_evening_shelf_2)
        trimmed.equals("Solo imprescindibles", ignoreCase = true) ||
            trimmed.equals("Just Essentials", ignoreCase = true) ->
            stringResource(Res.string.daypart_evening_shelf_3)
        trimmed.equals("Tus favoritos de siempre", ignoreCase = true) ||
            trimmed.equals("All-Time Favorites", ignoreCase = true) ->
            stringResource(Res.string.daypart_evening_shelf_4)
        trimmed.equals("Modo nocturno", ignoreCase = true) ||
            trimmed.equals("Night Mode", ignoreCase = true) ->
            stringResource(Res.string.daypart_night_shelf_1)
        trimmed.equals("Para bajar el ritmo", ignoreCase = true) ||
            trimmed.equals("Slow Down", ignoreCase = true) ->
            stringResource(Res.string.daypart_night_shelf_2)
        trimmed.equals("Sesión de madrugada", ignoreCase = true) ||
            trimmed.equals("Late Night Session", ignoreCase = true) ->
            stringResource(Res.string.daypart_night_shelf_3)
        trimmed.equals("Antes de dormir", ignoreCase = true) ||
            trimmed.equals("Before Sleep", ignoreCase = true) ->
            stringResource(Res.string.daypart_night_shelf_4)
        trimmed.equals("Dale al gym", ignoreCase = true) ||
            trimmed.equals("Work Out", ignoreCase = true) ->
            stringResource(Res.string.shelf_workout)
        trimmed.equals("De fiesta", ignoreCase = true) ||
            trimmed.equals("Party", ignoreCase = true) ->
            stringResource(Res.string.shelf_party)
        trimmed.equals("Para relajarte", ignoreCase = true) ||
            trimmed.equals("Chill", ignoreCase = true) ->
            stringResource(Res.string.shelf_chill)
        trimmed.equals("Para concentrarte", ignoreCase = true) ||
            trimmed.equals("Focus", ignoreCase = true) ->
            stringResource(Res.string.shelf_focus)
        trimmed.equals("Para empezar con ritmo", ignoreCase = true) ||
            trimmed.equals("Start with rhythm", ignoreCase = true) ->
            stringResource(Res.string.shelf_start_with_rhythm)
        trimmed.equals("Tendencias en tu estilo", ignoreCase = true) ||
            trimmed.equals("Trending in your style", ignoreCase = true) ->
            stringResource(Res.string.shelf_trends_in_your_style)
        trimmed.equals("Vibra de fin de semana", ignoreCase = true) ||
            trimmed.equals("Weekend vibe", ignoreCase = true) ->
            stringResource(Res.string.shelf_weekend_vibe)
        trimmed.equals("Imprescindibles", ignoreCase = true) ||
            trimmed.equals("Essentials", ignoreCase = true) ->
            stringResource(Res.string.shelf_essentials)
        trimmed.equals("Recents", ignoreCase = true) ||
            trimmed.equals("Recently played", ignoreCase = true) ||
            trimmed.equals("Gần đây", ignoreCase = true) ||
            trimmed.equals("最近", ignoreCase = true) ->
            stringResource(Res.string.shelf_recents)
        trimmed.equals("Playlists", ignoreCase = true) ||
            trimmed.equals("Danh sách phát", ignoreCase = true) ||
            trimmed.equals("再生リスト", ignoreCase = true) ->
            stringResource(Res.string.playlists)
        trimmed.equals("Albums", ignoreCase = true) ||
            trimmed.equals("Album", ignoreCase = true) ||
            trimmed.equals("アルバム", ignoreCase = true) ->
            stringResource(Res.string.albums)
        trimmed.equals("Artists", ignoreCase = true) ||
            trimmed.equals("Nghệ sĩ", ignoreCase = true) ||
            trimmed.equals("アーティスト", ignoreCase = true) ->
            stringResource(Res.string.artists)
        trimmed.equals("Subscriptions", ignoreCase = true) ||
            trimmed.equals("Đã đăng ký", ignoreCase = true) ||
            trimmed.equals("登録チャンネル", ignoreCase = true) ->
            stringResource(Res.string.subscriptions)
        trimmed.equals("Trending community playlists", ignoreCase = true) ||
            trimmed.equals("Danh sách phát cộng đồng thịnh hành", ignoreCase = true) ||
            trimmed.equals("Danh sách phát thịnh hành trong cộng đồng người dùng", ignoreCase = true) ||
            trimmed.equals("急上昇のコミュニティ再生リスト", ignoreCase = true) ->
            stringResource(Res.string.shelf_trending_community_playlists)
        trimmed.equals("Featured playlists for you", ignoreCase = true) ||
            trimmed.equals("Danh sách phát đề xuất cho bạn", ignoreCase = true) ||
            trimmed.equals("Danh sách phát nổi bật dành cho bạn", ignoreCase = true) ||
            trimmed.equals("おすすめの再生リスト", ignoreCase = true) ->
            stringResource(Res.string.shelf_featured_playlists_for_you)
        trimmed.equals("Quick picks", ignoreCase = true) ||
            trimmed.equals("Lựa chọn nhanh", ignoreCase = true) ||
            trimmed.equals("Chọn nhanh đài phát", ignoreCase = true) ||
            trimmed.equals("クイック ミックス", ignoreCase = true) ->
            stringResource(Res.string.shelf_quick_picks)
        trimmed.equals("Listen again", ignoreCase = true) ||
            trimmed.equals("Nghe lại", ignoreCase = true) ||
            trimmed.equals("もう一度聴く", ignoreCase = true) ->
            stringResource(Res.string.shelf_listen_again)
        trimmed.equals("Mixed for you", ignoreCase = true) ||
            trimmed.equals("Dành riêng cho bạn", ignoreCase = true) ||
            trimmed.equals("ミックス", ignoreCase = true) ->
            stringResource(Res.string.shelf_mixed_for_you)
        trimmed.startsWith("Similar to", ignoreCase = true) -> {
            val rest = trimmed.substring(10).trim()
            stringResource(Res.string.shelf_similar_to, rest)
        }
        trimmed.startsWith("Tương tự như", ignoreCase = true) -> {
            val rest = trimmed.substring(12).trim()
            stringResource(Res.string.shelf_similar_to, rest)
        }
        trimmed.equals("Forgotten favorites", ignoreCase = true) ||
            trimmed.equals("Giai điệu quen thuộc", ignoreCase = true) ||
            trimmed.equals("よく聴いたお気に入りの曲", ignoreCase = true) ->
            stringResource(Res.string.shelf_forgotten_favorites)
        trimmed.equals("Recommended music videos", ignoreCase = true) ||
            trimmed.equals("Video âm nhạc đề xuất", ignoreCase = true) ||
            trimmed.equals("おすすめのミュージック ビデオ", ignoreCase = true) ->
            stringResource(Res.string.shelf_recommended_music_videos)
        trimmed.equals("From your library", ignoreCase = true) ||
            trimmed.equals("Từ thư viện của bạn", ignoreCase = true) ||
            trimmed.equals("ライブラリから", ignoreCase = true) ->
            stringResource(Res.string.shelf_from_your_library)
        trimmed.equals("Charts", ignoreCase = true) ||
            trimmed.equals("Bảng xếp hạng", ignoreCase = true) ||
            trimmed.equals("チャート", ignoreCase = true) ->
            stringResource(Res.string.shelf_charts)
        trimmed.equals("New releases", ignoreCase = true) ||
            trimmed.equals("Bản phát hành mới", ignoreCase = true) ||
            trimmed.equals("最新リリース", ignoreCase = true) ->
            stringResource(Res.string.shelf_new_releases)
        trimmed.equals("Top music videos", ignoreCase = true) ||
            trimmed.equals("Video âm nhạc hàng đầu", ignoreCase = true) ||
            trimmed.equals("人気のミュージック ビデオ", ignoreCase = true) ->
            stringResource(Res.string.shelf_top_music_videos)
        trimmed.equals("For you", ignoreCase = true) ||
            trimmed.equals("Dành cho bạn", ignoreCase = true) ||
            trimmed.equals("あなたへのおすすめ", ignoreCase = true) ->
            stringResource(Res.string.shelf_for_you)
        trimmed.equals("Hits today", ignoreCase = true) ||
            trimmed.equals("Today's Hits", ignoreCase = true) ||
            trimmed.equals("Bản hit hôm nay", ignoreCase = true) ||
            trimmed.equals("今日のヒット曲", ignoreCase = true) ->
            stringResource(Res.string.shelf_hits_today)
        trimmed.equals("Artists on the rise", ignoreCase = true) ||
            trimmed.equals("Nghệ sĩ đang lên", ignoreCase = true) ->
            stringResource(Res.string.shelf_artists_on_the_rise)
        trimmed.equals("Concerts", ignoreCase = true) ||
            trimmed.equals("Buổi hòa nhạc", ignoreCase = true) ||
            trimmed.equals("コンサート", ignoreCase = true) ->
            stringResource(Res.string.shelf_concerts)
        trimmed.startsWith("Shorts", ignoreCase = true) ||
            trimmed.equals("Shorts nổi bật", ignoreCase = true) ||
            trimmed.equals("ショート", ignoreCase = true) ->
            stringResource(Res.string.shelf_shorts)
        trimmed.equals("Trending", ignoreCase = true) ||
            trimmed.equals("Thịnh hành", ignoreCase = true) ||
            trimmed.equals("急上昇", ignoreCase = true) ->
            stringResource(Res.string.shelf_trending)
        else -> title
    }
}
@Composable
internal fun localizeShelfSubtitle(subtitle: String): String {
    val trimmed = subtitle.trim()
    return when {
        trimmed.equals("Stations for every feeling and activity", ignoreCase = true) ||
            trimmed.equals("Estaciones para cada momento y actividad", ignoreCase = true) ->
            stringResource(Res.string.find_your_mood_sub)
        trimmed.equals("Your recent favorites and playlists", ignoreCase = true) ||
            trimmed.equals("Tus canciones y listas recientes", ignoreCase = true) ->
            stringResource(Res.string.listen_again_default_sub)
        trimmed.equals("Mixes alegres y playlists para arrancar con energía", ignoreCase = true) ||
            trimmed.equals("Upbeat mixes and playlists to start with energy", ignoreCase = true) ->
            stringResource(Res.string.daypart_morning_shelf_1_sub)
        trimmed.equals("Ritmos activos y pop/urbano para activarte", ignoreCase = true) ||
            trimmed.equals("Active rhythms and energetic pop/urban", ignoreCase = true) ->
            stringResource(Res.string.daypart_morning_shelf_2_sub)
        trimmed.equals("Los últimos lanzamientos de artistas que sigues", ignoreCase = true) ||
            trimmed.equals("Latest releases from artists you follow", ignoreCase = true) ->
            stringResource(Res.string.daypart_morning_shelf_3_sub)
        trimmed.equals("Lo-fi, instrumental y música ambiental para el enfoque", ignoreCase = true) ||
            trimmed.equals("Lo-fi, instrumental, and ambient focus sounds", ignoreCase = true) ->
            stringResource(Res.string.daypart_morning_shelf_4_sub)
        trimmed.equals("Entrenamiento, urbano y electrónica para motivarte", ignoreCase = true) ||
            trimmed.equals("Workout, urban, and electronic music to keep you motivated", ignoreCase = true) ->
            stringResource(Res.string.daypart_afternoon_shelf_1_sub)
        trimmed.equals("Beats para estudiar, programar o trabajar", ignoreCase = true) ||
            trimmed.equals("Beats for studying, coding, or working", ignoreCase = true) ->
            stringResource(Res.string.daypart_afternoon_shelf_2_sub)
        trimmed.equals("Los temas más escuchados y virales del momento", ignoreCase = true) ||
            trimmed.equals("Today's most played and viral tracks", ignoreCase = true) ||
            trimmed.equals("Today\'s most played and viral tracks", ignoreCase = true) ->
            stringResource(Res.string.daypart_afternoon_shelf_3_sub)
        trimmed.equals("Tus canciones más repetidas para la segunda mitad del día", ignoreCase = true) ||
            trimmed.equals("Your most replayed songs for the second half of the day", ignoreCase = true) ->
            stringResource(Res.string.daypart_afternoon_shelf_4_sub)
        trimmed.equals("Playlists para el trayecto a casa", ignoreCase = true) ||
            trimmed.equals("Soundtracks for the ride home", ignoreCase = true) ->
            stringResource(Res.string.daypart_evening_shelf_1_sub)
        trimmed.equals("Playlists de previa y vibra de fiesta", ignoreCase = true) ||
            trimmed.equals("Playlists editoriales de fiesta, previa, reggaetón y electrónica", ignoreCase = true) ||
            trimmed.equals("Weekend party vibe and pre-game playlists", ignoreCase = true) ->
            stringResource(Res.string.daypart_evening_shelf_1_party_sub)
        trimmed.equals("Música relajada para desconectar y descansar", ignoreCase = true) ||
            trimmed.equals("Música relajada, acústicos y chill para descansar", ignoreCase = true) ||
            trimmed.equals("Playlists suaves y acústicos para descansar", ignoreCase = true) ||
            trimmed.equals("Laid-back music to unwind and disconnect", ignoreCase = true) ->
            stringResource(Res.string.daypart_evening_shelf_2_sub)
        trimmed.equals("Pura concentración, instrumental y enfoque sin distracciones", ignoreCase = true) ||
            trimmed.equals("Pure focus, instrumental, and distraction-free sounds", ignoreCase = true) ->
            stringResource(Res.string.daypart_morning_shelf_4_sub)
        trimmed.equals("Las canciones esenciales que no pueden faltar", ignoreCase = true) ||
            trimmed.equals("Essential tracks that never miss", ignoreCase = true) ->
            stringResource(Res.string.daypart_evening_shelf_3_sub)
        trimmed.equals("Álbumes y temas guardados que nunca fallan", ignoreCase = true) ||
            trimmed.equals("Saved albums and songs that always deliver", ignoreCase = true) ->
            stringResource(Res.string.daypart_evening_shelf_4_sub)
        trimmed.equals("R&B suave, soul y atmósferas envolventes", ignoreCase = true) ||
            trimmed.equals("Smooth R&B, soul, and atmospheric grooves", ignoreCase = true) ->
            stringResource(Res.string.daypart_night_shelf_1_sub)
        trimmed.equals("Baladas, acústicos y sonidos serenos", ignoreCase = true) ||
            trimmed.equals("Ballads, acoustic tunes, and serene sounds", ignoreCase = true) ->
            stringResource(Res.string.daypart_night_shelf_2_sub)
        trimmed.equals("Electrónica profunda y mezclas para trasnochar", ignoreCase = true) ||
            trimmed.equals("Deep electronic and mixes for the late hours", ignoreCase = true) ->
            stringResource(Res.string.daypart_night_shelf_3_sub)
        trimmed.equals("Sonidos ambientales y melodías de descanso", ignoreCase = true) ||
            trimmed.equals("Ambient sounds and tranquil melodies for rest", ignoreCase = true) ->
            stringResource(Res.string.daypart_night_shelf_4_sub)
        trimmed.equals("TOP TUNES RIGHT NOW", ignoreCase = true) ||
            trimmed.equals("Top tunes right now", ignoreCase = true) ||
            trimmed.equals("Giai điệu hàng đầu hiện nay", ignoreCase = true) ||
            trimmed.equals("注目の曲", ignoreCase = true) ->
            stringResource(Res.string.shelf_top_tunes_right_now)
        trimmed.equals("From the community", ignoreCase = true) ||
            trimmed.equals("Từ cộng đồng", ignoreCase = true) ||
            trimmed.equals("コミュニティより", ignoreCase = true) ->
            stringResource(Res.string.shelf_from_the_community)
        trimmed.equals("YouTube Charts", ignoreCase = true) ||
            trimmed.equals("Bảng xếp hạng YouTube", ignoreCase = true) ||
            trimmed.equals("YouTube チャート", ignoreCase = true) ->
            stringResource(Res.string.shelf_youtube_charts)
        else -> subtitle
    }
}

@Composable
internal fun localizeCardSubtitle(subtitle: String): String {
    if (subtitle.isBlank()) return subtitle
    val delimiter = " • "
    val parts = subtitle.split(delimiter)
    val songLabel = stringResource(Res.string.shelf_song_item)
    val singleLabel = stringResource(Res.string.shelf_single)
    val chartLabel = stringResource(Res.string.shelf_chart)
    val playlistLabel = stringResource(Res.string.playlist)
    val localizedParts = parts.map { part ->
        val trimmed = part.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        when {
            trimmed.equals("Song", ignoreCase = true) || trimmed.equals("Titre", ignoreCase = true) ||
                trimmed.equals("Bài hát", ignoreCase = true) || trimmed.equals("曲", ignoreCase = true) -> songLabel
            trimmed.equals("Single", ignoreCase = true) || trimmed.equals("Đĩa đơn", ignoreCase = true) ||
                trimmed.equals("シングル", ignoreCase = true) -> singleLabel
            trimmed.equals("Chart", ignoreCase = true) || trimmed.equals("Bảng xếp hạng", ignoreCase = true) ||
                trimmed.equals("チャート", ignoreCase = true) -> chartLabel
            trimmed.equals("Playlist", ignoreCase = true) || trimmed.equals("Danh sách phát", ignoreCase = true) ||
                trimmed.equals("再生リスト", ignoreCase = true) -> playlistLabel
            lower.endsWith(" views") || lower.endsWith(" view") || lower.endsWith(" lượt xem") ||
                lower.endsWith(" 回視聴") || lower.endsWith("回視聴") -> {
                val count = trimmed.substringBeforeLast(' ', "").trim()
                if (count.any { it.isDigit() }) stringResource(Res.string.card_views_format, count) else part
            }
            lower.endsWith(" plays") || lower.endsWith(" play") || lower.endsWith(" lượt phát") ||
                lower.endsWith(" 回再生") || lower.endsWith("回再生") -> {
                val count = trimmed.substringBeforeLast(' ', "").trim()
                if (count.any { it.isDigit() }) stringResource(Res.string.card_plays_format, count) else part
            }
            lower.endsWith(" songs") || lower.endsWith(" song") || lower.endsWith(" bài hát") ||
                lower.endsWith(" 曲") || lower.endsWith("曲") -> {
                val count = trimmed.substringBeforeLast(' ', "").trim()
                if (count.any { it.isDigit() }) stringResource(Res.string.card_songs_format, count) else part
            }
            else -> part
        }
    }
    return localizedParts.joinToString(delimiter)
}

/**
 * [leadingCard] rides at the head of the row, ahead of the content — the
 * Library tab's "New playlist" tile, which belongs among the playlists rather
 * than in a bar somewhere above them. [onItemLongPress] opens the album /
 * playlist menu, and is null only where a card points at something with no
 * track list behind it to act on.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Shelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    leadingCard: (@Composable () -> Unit)? = null,
    currentSong: Song? = null,
    isPlaying: Boolean = false,
) {
    Column(Modifier.padding(bottom = 26.dp)) {
        SectionHeader(shelf.title, shelf.subtitle)
        val isCurrentItem = remember(shelf.items, currentSong) { shelf.items.playingItemMatcher(currentSong) }
        val rowState = rememberHomePersistentRowState("home_shelf_${shelf.title}")
        ShelfRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            leadingCard?.let { card -> item(key = "leading", contentType = "leading_card") { card() } }
            items(
                items = shelf.items,
                key = { item -> item.videoId ?: item.browseId ?: "${shelf.title}_${item.title}" },
                contentType = { if (shelf.layout == ShelfLayout.VIDEO) "video_card" else "shelf_card" },
            ) { item ->
                if (shelf.layout == ShelfLayout.VIDEO) {
                    VideoShelfCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        onLongPress = onItemLongPress?.let { { it(item) } },
                        isCurrent = isCurrentItem(item),
                        isPlaying = isCurrentItem(item) && isPlaying,
                    )
                } else {
                    ShelfCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        onLongPress = onItemLongPress?.let { { it(item) } },
                        isCurrent = isCurrentItem(item),
                        isPlaying = isCurrentItem(item) && isPlaying,
                    )
                }
            }
        }
    }
}

/**
 * A music video, as the web player lays one out: the thumbnail at its own
 * 16:9 rather than cropped into a square, the title and credit under it. About
 * as tall as the square cards, so a video shelf sits in the feed's rhythm.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoShelfCard(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier.width(VIDEO_CARD_WIDTH),
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    titleColor: Color = MaterialTheme.colorScheme.onBackground,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    placeholderColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val cardToken = remember { Any() }
    val openCard = rememberCoverTap(item.browseId ?: item.videoId, cardToken, onClick)
    Column(
        modifier = modifier
            .longPressMenuClickable(onClick = openCard, onLongClick = onLongPress)
            .contextClick(onLongPress),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(VIDEO_ASPECT)
                .clip(RoundedCornerShape(18.dp))
                .coverSharedElement(item.browseId ?: item.videoId, RoundedCornerShape(18.dp), cardToken)
                .thumbnailBorder(RoundedCornerShape(18.dp))
                .background(placeholderColor),
        ) {
            AsyncImage(
                // Video stills are not resizable googleusercontent art; the
                // largest the row listed is already about card size.
                model = item.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (isCurrent && isPlaying) SearchPlayingBars(Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = localizeCardSubtitle(item.subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = subtitleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A music video's frame, or a square one when the listener asked for that — see [VideoThumbnails]. */
val VIDEO_ASPECT: Float get() = if (VideoThumbnails.square) 1f else 16f / 9f

/** Wide enough that a 16:9 card stands about as tall as a square [SHELF_CARD_WIDTH] one. */
val VIDEO_CARD_WIDTH: Dp get() = if (VideoThumbnails.square) SHELF_CARD_WIDTH else 248.dp

/**
 * A card that isn't a thing yet — the dashed "New playlist" tile at the head
 * of the Library's playlist row, sized to sit in line with the covers beside
 * it rather than as a button bolted above them.
 */
@Composable
internal fun NewShelfCard(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(SHELF_CARD_WIDTH),
    /** A full-colour mark drawn in place of [icon], for a card that stands for a brand. */
    logo: DrawableResource? = null,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (logo != null) {
                // The mark is a disc with the bars cut out, so tinting it
                // leaves the bars clear: white on dark, black on light.
                Icon(
                    painter = painterResource(logo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(44.dp),
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * A device-folder card: mesh gradient in the folder's colours with its glyph
 * on top. One composable for local music, WebDAV and SMB rather than three
 * copies of the same box.
 */
@Composable
private fun ServiceCard(colors: List<Color>, trackKey: String, icon: ImageVector) {
    val palette = remember { MeshPalette(colors) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) {
        MeshGradientBackground(
            palette = palette,
            trackKey = trackKey,
            continuous = true,
            blurRadius = 24.dp,
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(40.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ShelfCard(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier.width(SHELF_CARD_WIDTH),
    /** Set on a Library playlist card that's in [AppSettings.pinnedPlaylists][com.music.bitchord.data.settings.AppSettings.pinnedPlaylists]. */
    isPinned: Boolean = false,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
) {
    val cardToken = remember { Any() }
    val openCard = rememberCoverTap(item.browseId, cardToken, onClick)
    Column(
        modifier = modifier
            .longPressMenuClickable(onClick = openCard, onLongClick = onLongPress)
            .contextClick(onLongPress),
    ) {
        when (item.browseId) {
            "local:downloads" -> {
                val palette = remember { MeshPalette(listOf(Color(0xFF1E3C72), Color(0xFF2A5298))) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    MeshGradientBackground(
                        palette = palette,
                        trackKey = "local:downloads",
                        continuous = true,
                        blurRadius = 24.dp,
                    )
                    Icon(
                        imageVector = BitChordIcons.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            "local:all" -> ServiceCard(
                colors = listOf(Color(0xFF134E5E), Color(0xFF71B280)),
                trackKey = "local:all",
                icon = Icons.Rounded.LibraryMusic,
            )
            "local:cache" -> ServiceCard(
                colors = listOf(Color(0xFF42275A), Color(0xFF734B6D)),
                trackKey = "local:cache",
                icon = Icons.Rounded.Cached,
            )
            "local:webdav" -> ServiceCard(
                colors = listOf(Color(0xFF3A1C71), Color(0xFFD76D77)),
                trackKey = "local:webdav",
                icon = Icons.Rounded.Folder,
            )
            "local:smb" -> ServiceCard(
                colors = listOf(Color(0xFF0F2027), Color(0xFF2C5364)),
                trackKey = "local:smb",
                icon = Icons.Rounded.Storage,
            )
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .coverSharedElement(item.browseId, RoundedCornerShape(18.dp), cardToken)
                        .thumbnailBorder(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    AsyncImage(
                        model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (isCurrent && isPlaying) SearchPlayingBars(Modifier.align(Alignment.Center))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isPinned) {
                Icon(
                    imageVector = BitChordIcons.Pin,
                    contentDescription = stringResource(Res.string.pinned),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        val displaySubtitle = localizeCardSubtitle(item.subtitle)
        Text(
            text = displaySubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
