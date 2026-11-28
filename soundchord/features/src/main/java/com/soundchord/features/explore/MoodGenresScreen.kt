package com.soundchord.features.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.MoodGenre
import com.music.bitchord.data.model.MoodGenreSection
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.artworkAt
import com.soundchord.features.R

@Composable
fun MoodGenresScreen(
    moodState: UiState<List<MoodGenreSection>>,
    onBack: () -> Unit,
    onCategoryClick: (MoodGenre) -> Unit,
    onRetry: () -> Unit,
    onAccountClick: () -> Unit = {},
    accountPhotoUrl: String? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
) {
    val topInset = contentPadding.calculateTopPadding()
    val bottomInset = contentPadding.calculateBottomPadding()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = topInset),
    ) {
        // Large Title
        Text(
            text = stringResource(R.string.explore_moods_genres),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        when (moodState) {
            UiState.Loading -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = bottomInset + 24.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(8) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 10.5f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        )
                    }
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomInset),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = moodState.message.ifBlank { stringResource(R.string.explore_empty_moods) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = onRetry) {
                            Text(text = stringResource(R.string.explore_retry))
                        }
                    }
                }
            }
            is UiState.Success -> {
                val allMoods = remember(moodState.data) {
                    moodState.data
                        .flatMap { it.items }
                        .distinctBy { it.browseId to it.params }
                }

                if (allMoods.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottomInset),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.explore_empty_moods),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = bottomInset + 24.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = allMoods,
                            key = { "${it.browseId}_${it.params}" },
                            contentType = { "mood_genre_card" },
                        ) { mood ->
                            MoodGenreGridCard(
                                mood = mood,
                                onClick = { onCategoryClick(mood) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodGenreGridCard(
    mood: MoodGenre,
    onClick: () -> Unit,
) {
    val vividAccents = listOf(
        Color(0xFFFF3D81), Color(0xFF7C4DFF), Color(0xFF00B8D9),
        Color(0xFFFF8A00), Color(0xFF00A86B), Color(0xFF365DFF),
    )
    val accent = vividAccents[(mood.title.hashCode().and(Int.MAX_VALUE)) % vividAccents.size]

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10.5f)
            .clip(RoundedCornerShape(18.dp))
            .background(mood.stripeColor?.let { Color(it.toInt()) } ?: Color(0xFF384653))
            .clickable(onClick = onClick),
    ) {
        mood.thumbnailUrl?.let { artwork ->
            AsyncImage(
                model = artwork.artworkAt(CARD_ART_PX),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Vivid accent gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to accent.copy(alpha = 0.22f),
                        0.55f to accent.copy(alpha = 0.08f),
                        1f to Color.Transparent,
                    ),
                ),
        )

        // Contrast dark gradient overlay for bottom text
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.48f to Color.Black.copy(alpha = 0.15f),
                        1f to Color.Black.copy(alpha = 0.78f),
                    ),
                ),
        )

        // Bold white title
        Text(
            text = mood.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}
