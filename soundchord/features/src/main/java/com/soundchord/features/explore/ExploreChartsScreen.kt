package com.soundchord.features.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.components.trackColumnWidth
import com.soundchord.features.R
import com.soundchord.features.editorial.data.AppleMusicEditorialFeed
import com.soundchord.features.editorial.data.AppleMusicEditorialItem
import com.soundchord.features.editorial.data.AppleMusicEditorialRepository
import com.soundchord.features.editorial.data.AppleMusicEditorialSong
import com.soundchord.features.editorial.data.EditorialItemKind
import com.soundchord.features.playlistimport.data.AppleMusicPlaylistRepository
import com.soundchord.features.playlistimport.data.AppleMusicTrack
import kotlinx.coroutines.launch

@Composable
fun ExploreChartsScreen(
    feed: AppleMusicEditorialFeed,
    onBack: () -> Unit = {},
    onEditorialItemClick: (AppleMusicEditorialItem) -> Unit,
    onAccountClick: () -> Unit = {},
    accountPhotoUrl: String? = null,
    onPlaySong: ((Song) -> Unit)? = null,
    onSongMenu: ((Song) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
) {
    val topInset = contentPadding.calculateTopPadding()
    val bottomInset = contentPadding.calculateBottomPadding()

    var currentFeed by remember(feed) { mutableStateOf(feed) }
    var resolvingId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (currentFeed.topSongs.isEmpty() || currentFeed.topAlbums.isEmpty() || currentFeed.dailyTop100.isEmpty()) {
            val fresh = AppleMusicEditorialRepository.getEditorialFeed(forceRefresh = currentFeed.topAlbums.isEmpty() || currentFeed.topSongs.isEmpty())
            if (fresh.heroItems.isNotEmpty() || fresh.topSongs.isNotEmpty() || fresh.topAlbums.isNotEmpty() || fresh.dailyTop100.isNotEmpty()) {
                currentFeed = fresh
            }
        }
    }

    val handleSongClick: (AppleMusicEditorialSong) -> Unit = { song ->
        scope.launch {
            resolvingId = song.id
            val track = AppleMusicTrack(
                id = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                durationMs = song.durationMs,
                isrc = song.isrc,
                coverUrl = song.thumbnailUrl,
            )
            val resolved = AppleMusicPlaylistRepository.resolveTrack(track)
            resolvingId = null
            if (resolved != null) {
                onPlaySong?.invoke(resolved)
            }
        }
    }

    val handleSongMenuClick: (AppleMusicEditorialSong) -> Unit = { song ->
        scope.launch {
            val track = AppleMusicTrack(
                id = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                durationMs = song.durationMs,
                isrc = song.isrc,
                coverUrl = song.thumbnailUrl,
            )
            val resolved = AppleMusicPlaylistRepository.resolveTrack(track)
            if (resolved != null) {
                onSongMenu?.invoke(resolved)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = topInset),
    ) {
        // Título grande Charts en negrita (con espaciado limpio respetando la barra superior existente)
        Text(
            text = stringResource(R.string.explore_charts_rankings),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        )

        LazyColumn(
            contentPadding = PaddingValues(top = 4.dp, bottom = bottomInset + 96.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // 1. Top canciones (Cuadrícula horizontal de 3 filas por columna hasta Top 50)
            val rawTopSongs = currentFeed.topSongs.take(50).ifEmpty {
                (currentFeed.momentSongs + currentFeed.trendingSongs + currentFeed.newSongs)
                    .distinctBy { "${it.title.lowercase()}:${it.artist.lowercase()}" }
                    .take(50)
            }
            if (rawTopSongs.isNotEmpty()) {
                item(key = "charts_top_songs_section") {
                    ChartsTopSongsSection(
                        songs = rawTopSongs,
                        resolvingId = resolvingId,
                        onSongClick = handleSongClick,
                        onSongMenuClick = handleSongMenuClick,
                    )
                }
            }

            // 2. Lista por ciudad (Carrusel horizontal de hasta 15 playlists editoriales)
            val cityPlaylists = currentFeed.cityCharts
                .filter { it.id.isNotBlank() && !it.thumbnailUrl.isNullOrBlank() && it.thumbnailUrl.startsWith("https://") }
                .distinctBy { it.id }
                .take(15)
            if (cityPlaylists.isNotEmpty()) {
                item(key = "charts_city_charts_section") {
                    ChartsPlaylistsCarousel(
                        title = stringResource(R.string.editorial_section_city_charts_title),
                        items = cityPlaylists,
                        badgeLabel = stringResource(R.string.explore_charts_type_playlist),
                        onItemClick = onEditorialItemClick,
                    )
                }
            }

            // 3. Top 100 de hoy (Carrusel horizontal de hasta 15 playlists Top 100, deduplicado estrictamente)
            val top100Playlists = currentFeed.dailyTop100
                .filter { it.id.isNotBlank() && !it.thumbnailUrl.isNullOrBlank() && it.thumbnailUrl.startsWith("https://") }
                .distinctBy { it.id }
                .take(15)
            if (top100Playlists.isNotEmpty()) {
                item(key = "charts_top_100_section") {
                    ChartsPlaylistsCarousel(
                        title = stringResource(R.string.editorial_section_daily_top_100_title),
                        items = top100Playlists,
                        badgeLabel = stringResource(R.string.explore_charts_type_playlist),
                        onItemClick = onEditorialItemClick,
                    )
                }
            }

            // 4. Top álbumes (Carrusel horizontal hasta el Top 30)
            val topAlbums = currentFeed.topAlbums.take(30).ifEmpty {
                (currentFeed.heroItems.filter { it.kind == EditorialItemKind.ALBUM } +
                 currentFeed.comingSoon.filter { it.kind == EditorialItemKind.ALBUM } +
                 currentFeed.everyoneIsListening.filter { it.kind == EditorialItemKind.ALBUM })
            }.ifEmpty {
                AppleMusicEditorialRepository.getCuratedTopAlbumsSeed()
            }.filter { it.id.isNotBlank() && !it.thumbnailUrl.isNullOrBlank() && it.thumbnailUrl.startsWith("https://") }
                .distinctBy { it.id }
                .take(30)
            if (topAlbums.isNotEmpty()) {
                item(key = "charts_top_albums_section") {
                    ChartsRankedAlbumsCarousel(
                        title = stringResource(R.string.editorial_section_top_albums),
                        albums = topAlbums,
                        badgeLabel = stringResource(R.string.explore_charts_type_album),
                        onItemClick = onEditorialItemClick,
                    )
                }
            }
        }
    }
}

/**
 * Sección 1: Top Canciones con cuadrícula horizontal paginada de 3 filas por columna.
 * Desplaza horizontalmente hasta el Top 50 (#1 a #50).
 */
@Composable
private fun ChartsTopSongsSection(
    songs: List<AppleMusicEditorialSong>,
    resolvingId: String?,
    onSongClick: (AppleMusicEditorialSong) -> Unit,
    onSongMenuClick: (AppleMusicEditorialSong) -> Unit,
    modifier: Modifier = Modifier,
) {
    val validSongs = remember(songs) {
        songs
            .filter { it.id.isNotBlank() && !it.thumbnailUrl.isNullOrBlank() && it.thumbnailUrl.startsWith("https://") }
            .distinctBy { "${it.title.lowercase()}:${it.artist.lowercase()}" }
            .take(50)
    }
    val chunkedSongs = remember(validSongs) { validSongs.chunked(3) }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.editorial_section_top_songs),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        BoxWithConstraints {
            val colWidth = trackColumnWidth(maxWidth)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                itemsIndexed(
                    items = chunkedSongs,
                    key = { colIdx, chunk -> "chunk_${colIdx}_${chunk.firstOrNull()?.id ?: 0}" },
                ) { colIndex, columnTracks ->
                    Column(
                        modifier = Modifier.width(colWidth),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        columnTracks.forEachIndexed { rowIndex, song ->
                            val ranking = (colIndex * 3) + rowIndex + 1
                            val isResolving = resolvingId == song.id

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = !isResolving) { onSongClick(song) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Portada cuadrada redondeada
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (!song.thumbnailUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = song.thumbnailUrl,
                                            contentDescription = song.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                    if (isResolving) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Número dinámico de posición (#1 al #50)
                                Text(
                                    text = ranking.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.widthIn(min = 24.dp),
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                // Metadatos: Título en negrita arriba, Artista y badge debajo en la misma línea
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp),
                                    ) {
                                        Text(
                                            text = song.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false),
                                        )

                                        if (song.isExplicit) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            ChartExplicitBadge()
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))
                                        ChartSubtleTypeBadge(text = stringResource(R.string.explore_charts_type_song))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Secciones 2 y 3: Carrusel horizontal de playlists editoriales (Lista por ciudad / Top 100 de hoy).
 * Implementa claves únicas garantizadas y deduplicación para prevenir cierres inesperados en scroll.
 */
@Composable
private fun ChartsPlaylistsCarousel(
    title: String,
    items: List<AppleMusicEditorialItem>,
    badgeLabel: String,
    onItemClick: (AppleMusicEditorialItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val validItems = remember(items) {
        items
            .filter { it.id.isNotBlank() && !it.thumbnailUrl.isNullOrBlank() && it.thumbnailUrl.startsWith("https://") }
            .distinctBy { it.id }
    }
    if (validItems.isEmpty()) return

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(
                items = validItems,
                key = { index, item -> "${item.id}_$index" },
            ) { _, item ->
                Column(
                    modifier = Modifier
                        .width(152.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onItemClick(item) },
                ) {
                    Box(
                        modifier = Modifier
                            .size(152.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    ) {
                        item.thumbnailUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp, start = 2.dp, end = 2.dp),
                    ) {
                        Text(
                            text = item.subtitle.ifBlank { "Apple Music" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )

                        Spacer(modifier = Modifier.width(6.dp))
                        ChartSubtleTypeBadge(text = badgeLabel)
                    }
                }
            }
        }
    }
}

/**
 * Sección 4: Carrusel horizontal de Top Álbumes con número dinámico según ranking (#1 al #30).
 */
@Composable
private fun ChartsRankedAlbumsCarousel(
    title: String,
    albums: List<AppleMusicEditorialItem>,
    badgeLabel: String,
    onItemClick: (AppleMusicEditorialItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val validAlbums = remember(albums) {
        val filtered = albums
            .filter { it.id.isNotBlank() && !it.thumbnailUrl.isNullOrBlank() && it.thumbnailUrl.startsWith("https://") }
        AppleMusicEditorialRepository.deduplicateAlbumsKeepExplicit(filtered)
    }
    if (validAlbums.isEmpty()) return

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(
                items = validAlbums,
                key = { index, item -> "${item.id}_$index" },
            ) { index, item ->
                val ranking = index + 1
                Column(
                    modifier = Modifier
                        .width(152.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onItemClick(item) },
                ) {
                    Box(
                        modifier = Modifier
                            .size(152.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    ) {
                        item.thumbnailUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
                    ) {
                        Text(
                            text = ranking.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp, start = 2.dp, end = 2.dp),
                    ) {
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )

                        if (item.isExplicit) {
                            Spacer(modifier = Modifier.width(6.dp))
                            ChartExplicitBadge()
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                        ChartSubtleTypeBadge(text = badgeLabel)
                    }
                }
            }
        }
    }
}

/**
 * Badge sutil de tipo en la misma línea (PLAYLIST, ÁLBUM, CANCIÓN).
 */
@Composable
private fun ChartSubtleTypeBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            .padding(horizontal = 5.dp, vertical = 1.5.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Badge sutil de explicitud ("E").
 */
@Composable
private fun ChartExplicitBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF3A3A3C))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(
            text = "E",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = Color(0xFFAEAEB2),
        )
    }
}
