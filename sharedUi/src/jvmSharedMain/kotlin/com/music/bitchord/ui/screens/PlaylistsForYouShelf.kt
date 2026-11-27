package com.music.bitchord.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.TopPick
import com.music.bitchord.data.model.TopPickKind
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.sharedui.resources.*
import com.music.bitchord.ui.components.AnimatedHeroGradientCard
import com.music.bitchord.ui.components.FavoritesMixFluidPalette
import com.music.bitchord.ui.components.NewMusicMixFluidPalette
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.ShelfRow
import com.music.bitchord.ui.components.contextClick
import com.music.bitchord.ui.components.longPressMenuClickable
import com.music.bitchord.ui.components.thumbnailBorder
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val MINI_HERO_WIDTH = 175.dp
private val MINI_HERO_HEIGHT = 250.dp
private val MiniHeroShape = RoundedCornerShape(22.dp)

/**
 * Reusable compact vertical Mini-Hero shelf: "Playlists para ti" (Ticket #62).
 * Displays exactly 12 compact vertical cards (width ~175.dp, height ~250.dp, corners 22.dp):
 * - Card #1 (Animated Anchor): "Tus favoritos" (Favorites Mix) with violet fluid gradient.
 * - Card #2 (Animated Anchor): "Nueva música" (New Music Mix) with coral fluid gradient.
 * - Cards #3 to #12: 10 dynamic editorial and personalized playlists with SoundChord logo,
 *   full-bleed artwork, bottom gradient, and descriptive subtitle.
 */
@Composable
fun PlaylistsForYouShelf(
    playlists: List<TopPick>,
    onClick: (TopPick, String) -> Unit,
    onLongPress: ((TopPick) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (playlists.isEmpty()) return

    val favoritesTitle = stringResource(Res.string.top_pick_favorites_mix)
    val favoritesEyebrow = stringResource(Res.string.top_pick_made_for_you)
    val favoritesDefaultSub = stringResource(Res.string.top_pick_favorites_subtitle)

    val newMusicTitle = stringResource(Res.string.top_pick_new_music_mix)
    val newMusicEyebrow = stringResource(Res.string.top_pick_made_for_you)
    val newMusicDefaultSub = stringResource(Res.string.top_pick_new_music_subtitle)

    val rowState = rememberHomePersistentRowState("home_shelf_playlists_for_you")

    Column(modifier.padding(bottom = 26.dp)) {
        SectionHeader(
            title = stringResource(Res.string.playlists_for_you),
            subtitle = "",
        )
        ShelfRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(
                items = playlists.take(12),
                key = { index, item -> "${index}_${item.kind}_${item.browseId ?: item.title}" },
            ) { index, pick ->
                val longClick = onLongPress?.let { { it(pick) } }

                when (pick.kind) {
                    TopPickKind.FAVORITES_MIX -> {
                        AnimatedHeroGradientCard(
                            title = favoritesTitle,
                            eyebrow = favoritesEyebrow,
                            subtitle = pick.subtitle.ifBlank { favoritesDefaultSub },
                            colors = FavoritesMixFluidPalette,
                            onClick = { onClick(pick, favoritesTitle) },
                            onLongClick = longClick,
                            eyebrowColor = Color(0xFFE2C9FF),
                            cardHeight = MINI_HERO_HEIGHT,
                            cardShape = MiniHeroShape,
                            compact = true,
                            modifier = Modifier.width(MINI_HERO_WIDTH),
                        )
                    }
                    TopPickKind.NEW_RELEASE_MIX -> {
                        AnimatedHeroGradientCard(
                            title = newMusicTitle,
                            eyebrow = newMusicEyebrow,
                            subtitle = pick.subtitle.ifBlank { newMusicDefaultSub },
                            colors = NewMusicMixFluidPalette,
                            onClick = { onClick(pick, newMusicTitle) },
                            onLongClick = longClick,
                            eyebrowColor = Color(0xFFFFD2DA),
                            cardHeight = MINI_HERO_HEIGHT,
                            cardShape = MiniHeroShape,
                            compact = true,
                            modifier = Modifier.width(MINI_HERO_WIDTH),
                        )
                    }
                    else -> {
                        PlaylistMiniHeroCard(
                            pick = pick,
                            onClick = { onClick(pick, pick.title) },
                            onLongClick = longClick,
                            modifier = Modifier.width(MINI_HERO_WIDTH),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact vertical Mini-Hero playlist card (Ticket #62):
 * - Fixed size 175.dp x 250.dp with continuous 22.dp rounded corners.
 * - High-res cover cropped to card bounds.
 * - SoundChord logo / isotype in top-right corner.
 * - Bottom gradient scrim for high contrast.
 * - Bold title and descriptive subtitle underneath.
 */
@Composable
fun PlaylistMiniHeroCard(
    pick: TopPick,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .height(MINI_HERO_HEIGHT)
            .clip(MiniHeroShape)
            .thumbnailBorder(MiniHeroShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongClick)
            .contextClick(onLongClick),
    ) {
        // High-resolution artwork cover filling card
        val artworkUrl = pick.images.firstOrNull()?.artworkAt(CARD_ART_PX)
        AsyncImage(
            model = artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Subtle gradient scrim at bottom to ensure caption legibility while keeping center vivid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.40f to Color.Transparent,
                        0.68f to Color.Black.copy(alpha = 0.48f),
                        0.88f to Color.Black.copy(alpha = 0.72f),
                        1.00f to Color.Black.copy(alpha = 0.88f),
                    ),
                ),
        )

        // SoundChord isotype in top-right corner
        Icon(
            painter = painterResource(Res.drawable.ic_logo),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 14.dp)
                .height(15.dp)
                .aspectRatio(730f / 484f),
        )

        // Descriptive caption at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Text(
                text = pick.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (pick.subtitle.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = pick.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = Color.White.copy(alpha = 0.84f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
