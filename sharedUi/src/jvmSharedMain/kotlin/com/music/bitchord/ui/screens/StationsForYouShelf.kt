package com.music.bitchord.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.HEADER_ART_PX
import com.music.bitchord.data.model.TopPick
import com.music.bitchord.data.model.TopPickKind
import com.music.bitchord.sharedui.resources.*
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.SHELF_CARD_WIDTH
import com.music.bitchord.ui.components.ShelfRow
import com.music.bitchord.ui.components.thumbnailBorder
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val StationCardShape = RoundedCornerShape(20.dp)

/**
 * Ensures Google/YouTube channel avatar URLs don't carry white circular letterbox rings
 * like `-k-c0x00ffffff-no-rj` and instead use clean borderless square cuts.
 */
private fun cleanAvatarUrl(url: String): String {
    if ("ggpht.com" !in url && "googleusercontent.com" !in url) return url
    return url.replace("-c0x00ffffff", "")
        .replace("-c0xffffffff", "")
        .replace("-k-no-rj", "-no-rj")
        .replace("-k-no", "-no")
        .replace("-k-c0x00ffffff", "")
}

/**
 * Custom 3-circle collage tailored for 1:1 square cards (Apple Music style).
 * Keeps clear background air and proportional spacing between the 3 artist avatars:
 * - Primary circle: ~52% width, top-left (padding start 12.dp, top 12.dp)
 * - Secondary circle right: ~32% width, mid-bottom right (padding end 12.dp, bottom 26.dp)
 * - Secondary circle bottom: ~25% width, bottom center-left (padding start 42.dp, bottom 10.dp)
 */
@Composable
private fun SquareArtistCollageArt(
    images: List<String>,
    palette: CollagePalette,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.background(Brush.verticalGradient(listOf(palette.top, palette.bottom))),
    ) {
        val w = maxWidth
        val d1 = w * 0.52f
        val d2 = w * 0.32f
        val d3 = w * 0.25f

        // Círculo principal (Artista base): ~52% ancho, arriba a la izquierda
        images.getOrNull(0)?.let { rawUrl ->
            val url = cleanAvatarUrl(rawUrl).croppedAt(HEADER_ART_PX)
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 12.dp)
                    .size(d1)
                    .clip(CircleShape),
            )
        }

        // Círculo secundario derecho: ~32% ancho, zona media-baja derecha
        images.getOrNull(1)?.let { rawUrl ->
            val url = cleanAvatarUrl(rawUrl).croppedAt(CARD_ART_PX)
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 26.dp)
                    .size(d2)
                    .clip(CircleShape),
            )
        }

        // Círculo secundario inferior: ~25% ancho, abajo centro-izquierda
        images.getOrNull(2)?.let { rawUrl ->
            val url = cleanAvatarUrl(rawUrl).croppedAt(CARD_ART_PX)
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 42.dp, bottom = 10.dp)
                    .size(d3)
                    .clip(CircleShape),
            )
        }
    }
}

/**
 * Compact and reusable 1:1 square station card (Ticket #60).
 * Displays:
 * - Square 1:1 cover with 20.dp rounded corners and subtle border.
 * - SoundChord logo / isotype in the top-right corner.
 * - Bold title underneath (user named station, discovery, or "[Artist] and similar artists").
 * - Single-line subtitle "Radio Station" ("Estación de radio").
 */
@Composable
fun StationSquareCard(
    pick: TopPick,
    listenerName: String?,
    onClick: (TopPick, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (pick.kind) {
        TopPickKind.PERSONAL_STATION -> {
            listenerName?.substringBefore(' ')?.takeIf { it.isNotBlank() }
                ?.let { stringResource(Res.string.top_pick_named_station, it) }
                ?: stringResource(Res.string.top_pick_your_station)
        }
        TopPickKind.DISCOVERY_STATION -> {
            stringResource(Res.string.top_pick_discovery_station)
        }
        else -> {
            val artist = pick.artistName?.takeIf { it.isNotBlank() } ?: pick.title
            stringResource(Res.string.top_pick_and_similar, artist)
        }
    }
    val subtitle = stringResource(Res.string.radio_station_subtitle)

    Column(
        modifier = modifier
            .width(SHELF_CARD_WIDTH)
            .clickable { onClick(pick, title) },
    ) {
        Box(
            modifier = Modifier
                .width(SHELF_CARD_WIDTH)
                .aspectRatio(1f)
                .clip(StationCardShape)
                .thumbnailBorder(StationCardShape),
        ) {
            when (pick.kind) {
                TopPickKind.PERSONAL_STATION -> {
                    PersonalStationArt(Modifier.fillMaxSize())
                }
                TopPickKind.DISCOVERY_STATION -> {
                    DiscoveryStationArt(Modifier.fillMaxSize())
                }
                else -> {
                    val palette = collagePalette(pick.artistName ?: pick.title)
                    SquareArtistCollageArt(
                        images = pick.images,
                        palette = palette,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // SoundChord isotype / logo on upper right
            Icon(
                painter = painterResource(Res.drawable.ic_logo),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
                    .height(13.dp)
                    .aspectRatio(730f / 484f),
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Dedicated "Stations for You" (Emisoras para ti) shelf row.
 * Houses 10 to 12 square 1:1 stations personalized from YouTube Music.
 */
@Composable
fun StationsForYouShelf(
    stations: List<TopPick>,
    listenerName: String?,
    onClick: (TopPick, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (stations.isEmpty()) return

    Column(modifier = modifier.padding(bottom = 26.dp)) {
        SectionHeader(
            title = stringResource(Res.string.stations_for_you),
            subtitle = "",
        )
        val rowState = rememberHomePersistentRowState("home_shelf_stations_for_you")
        ShelfRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(
                items = stations,
                key = { it.artistBrowseId ?: it.watch?.playlistId ?: "${it.kind}_${it.title}" },
            ) { station ->
                StationSquareCard(
                    pick = station,
                    listenerName = listenerName,
                    onClick = onClick,
                )
            }
        }
    }
}
