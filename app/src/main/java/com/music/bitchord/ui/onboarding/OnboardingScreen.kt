package com.music.bitchord.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.ThemeMode
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.ic_google_g
import com.music.bitchord.sharedui.resources.ic_logo
import org.jetbrains.compose.resources.painterResource
import com.music.bitchord.ui.icons.BitChordIcons
import com.soundchord.features.icons.SoundChordIcons
import com.music.bitchord.ui.player.MeshGradientBackground
import com.music.bitchord.ui.player.MESH_CYCLE_MILLIS
import com.music.bitchord.ui.player.MeshPalette
import com.music.bitchord.ui.player.WelcomeMeshPalettes
import com.music.bitchord.ui.player.cyclingMeshPalette
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

/**
 * The welcome pages: name, artist picks, appearance preview, then YouTube sign-in.
 *
 * Shown once — to new installs and, because the flag it sets is new, to anyone
 * updating from a build without it. Existing accounts can review their account
 * on the final page without being prompted to sign in again.
 *
 * Always drawn dark, whatever the theme, for the same reason Replay is: the
 * mesh is colour, and white type is what sits on colour.
 */
@Composable
fun OnboardingScreen(
    signedIn: Boolean,
    initialName: String,
    accountName: String? = null,
    accountAvatarUrl: String? = null,
    /** Saves the party name. Called on Next, before the second page. */
    onNameChosen: (String) -> Unit,
    /** Opens the Google sign-in. The caller finishes onboarding once it lands. */
    onSignIn: () -> Unit,
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel? = null,
    modifier: Modifier = Modifier,
) {
    var page by rememberSaveable { mutableIntStateOf(PAGE_NAME) }
    var name by rememberSaveable { mutableStateOf(initialName) }
    val focusManager = LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val artistsVm = viewModel ?: composeViewModel(
        factory = remember(context) { OnboardingViewModel.Factory(context.applicationContext as android.app.Application) },
    )

    BackHandler(enabled = page > PAGE_NAME) { page-- }
    // The mesh never settles on one palette: each crossfade is as long as the
    // hold, so one runs straight into the next.
    val palette = cyclingMeshPalette(if (page == PAGE_CONNECT) ConnectPalettes else WelcomeMeshPalettes)

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            // Swallows every touch the pages don't, so nothing reaches the app
            // laid out underneath — and a tap on the backdrop puts the keyboard
            // away, which is where a thumb goes to dismiss one.
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
    ) {
        MeshGradientBackground(
            palette = palette,
            trackKey = page,
            continuous = true,
            driftMillis = 6_000,
            colorFadeMillis = MESH_CYCLE_MILLIS,
        )
        // The mesh carries its own light scrim for a player's controls; two
        // full-width buttons and a paragraph want the foot of it a shade deeper.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.05f),
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.45f),
                    ),
                ),
        )

        AnimatedContent(
            targetState = page,
            transitionSpec = {
                val forward = targetState > initialState
                val slide = tween<IntOffset>(420)
                (slideInHorizontally(slide) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(320, 100)))
                    .togetherWith(
                        slideOutHorizontally(slide) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(200)),
                    )
            },
            label = "onboardingPage",
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
        ) { shown ->
            OnboardingPage {
                when (shown) {
                    PAGE_NAME -> NamePage(
                        name = name,
                        onNameChange = { name = it.take(NAME_LIMIT) },
                        onNext = next@{
                            val chosen = name.trim()
                            if (chosen.isEmpty()) return@next
                            focusManager.clearFocus()
                            onNameChosen(chosen)
                            page = PAGE_ARTISTS
                        },
                    )
                    PAGE_ARTISTS -> ArtistsPage(
                        viewModel = artistsVm,
                        onBack = { page = PAGE_NAME },
                        onSkip = { page = PAGE_LOOK },
                        onContinue = { page = PAGE_LOOK },
                    )
                    PAGE_LOOK -> LookPage(
                        viewModel = artistsVm,
                        onBack = { page = PAGE_ARTISTS },
                        onContinue = { page = PAGE_CONNECT },
                    )
                    else -> ConnectPage(
                        signedIn = signedIn,
                        accountName = accountName,
                        accountAvatarUrl = accountAvatarUrl,
                        onBack = { page = PAGE_LOOK },
                        onSignIn = onSignIn,
                        onFinish = onFinish,
                    )
                }
            }
        }
    }
}

/** The column every page is laid out in, capped so a tablet doesn't stretch it. */
@Composable
private fun OnboardingPage(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .padding(horizontal = 28.dp),
            content = content,
        )
    }
}

@Composable
private fun ArtistsPage(
    viewModel: OnboardingViewModel,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
) {
    val query by viewModel.searchQuery.collectAsState()
    val popular by viewModel.artists.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val loading by viewModel.searchLoading.collectAsState()
    val artistsLoading by viewModel.artistsLoading.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val countryName = viewModel.countryName
    val shown = if (query.isBlank()) popular else results
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.align(Alignment.CenterStart)) { BackButton(onBack) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(4) { index ->
                    Box(
                        Modifier.width(if (index == PAGE_ARTISTS) 22.dp else 7.dp).height(7.dp)
                            .clip(CircleShape)
                            .background(if (index <= PAGE_ARTISTS) Color.White else Color.White.copy(alpha = .35f)),
                    )
                }
            }
            Text(
                stringResource(R.string.onboarding_skip_artists),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.CenterEnd).clip(CircleShape)
                    .background(Color.White.copy(alpha = .16f)).clickable(onClick = onSkip)
                    .padding(horizontal = 18.dp, vertical = 11.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.onboarding_artists_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(7.dp))
        Text(
            if (countryName.isBlank()) stringResource(R.string.onboarding_artists_description)
            else stringResource(R.string.onboarding_artists_description_country, countryName),
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 20.sp,
            color = Color.White.copy(alpha = .72f),
        )
        Spacer(Modifier.height(16.dp))
        ArtistSearchField(query, viewModel::onSearchQueryChanged)
        Spacer(Modifier.height(10.dp))
        if (loading || (query.isBlank() && artistsLoading)) {
            androidx.compose.material3.LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = .16f),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
        ) {
            items(shown, key = { it.artist.browseId }) { suggestion ->
                ArtistChoiceTile(
                    suggestion = suggestion,
                    selected = selected.any { it.browseId == suggestion.artist.browseId },
                    onClick = { viewModel.toggleArtist(suggestion) },
                )
            }
            if (shown.isEmpty() && !loading && !artistsLoading) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(R.string.onboarding_artists_empty),
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        color = Color.White.copy(alpha = .7f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        val continueLabel = when {
            selected.isEmpty() -> stringResource(R.string.onboarding_choose_artist)
            selected.size == 1 -> stringResource(R.string.onboarding_continue_one)
            else -> stringResource(R.string.onboarding_continue_count, selected.size)
        }
        val selectedAvatars: (@Composable () -> Unit)? = if (selected.isEmpty()) {
            null
        } else {
            { SelectedArtistAvatars(selected.take(3)) }
        }
        PillButton(
            label = continueLabel,
            onClick = onContinue,
            enabled = selected.isNotEmpty(),
            primary = true,
            leading = selectedAvatars,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LookPage(
    viewModel: OnboardingViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val selected by viewModel.selected.collectAsState()
    val theme by AppSettings.themeMode.collectAsState()
    val liquidGlass by AppSettings.liquidGlass.collectAsState()
    Column(Modifier.fillMaxSize()) {
        OnboardingStepHeader(step = PAGE_LOOK, onBack = onBack)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.onboarding_look_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.onboarding_look_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = .72f),
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(14.dp))
            OnboardingLookPreview(selected = selected, themeMode = theme, liquidGlass = liquidGlass)
            OnboardingSectionLabel(stringResource(R.string.onboarding_theme_label))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    SettingChoicePill(
                        label = when (mode) {
                            ThemeMode.SYSTEM -> stringResource(R.string.onboarding_theme_system)
                            ThemeMode.LIGHT -> stringResource(R.string.onboarding_theme_light)
                            ThemeMode.DARK -> stringResource(R.string.onboarding_theme_dark)
                        },
                        selected = theme == mode,
                        modifier = Modifier.weight(1f),
                        onClick = { AppSettings.setThemeMode(mode) },
                    )
                }
            }
            OnboardingSectionLabel(stringResource(R.string.onboarding_glass_label))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingChoicePill(
                    stringResource(R.string.onboarding_glass_standard),
                    selected = !liquidGlass,
                    modifier = Modifier.weight(1f),
                    onClick = { AppSettings.setLiquidGlass(false) },
                )
                SettingChoicePill(
                    stringResource(R.string.onboarding_glass_liquid),
                    selected = liquidGlass,
                    modifier = Modifier.weight(1f),
                    onClick = { AppSettings.setLiquidGlass(true) },
                )
            }
            Text(
                stringResource(if (liquidGlass) R.string.onboarding_glass_liquid_description else R.string.onboarding_glass_standard_description),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = .66f),
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, end = 4.dp, bottom = 16.dp),
            )
        }
        PillButton(
            label = stringResource(R.string.onboarding_continue),
            onClick = onContinue,
            primary = true,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun OnboardingStepHeader(step: Int, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.align(Alignment.CenterStart)) { BackButton(onBack) }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(4) { index ->
                Box(
                    Modifier.width(if (index == step) 22.dp else 7.dp).height(7.dp)
                        .clip(CircleShape)
                        .background(if (index <= step) Color.White else Color.White.copy(alpha = .35f)),
                )
            }
        }
    }
}

@Composable
private fun OnboardingSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = Color.White.copy(alpha = .8f),
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 9.dp),
    )
}

@Composable
private fun SettingChoicePill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        if (selected) Color.White else Color.White.copy(alpha = .14f),
        spring(stiffness = 520f),
        label = "onboardingChoiceBackground",
    )
    val foreground by animateColorAsState(
        if (selected) Color(0xFF111113) else Color.White,
        spring(stiffness = 520f),
        label = "onboardingChoiceForeground",
    )
    val scale by animateFloatAsState(if (selected) 1f else .98f, spring(stiffness = 520f), label = "onboardingChoiceScale")
    Box(
        modifier.height(42.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape).background(background).clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = foreground, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun OnboardingLookPreview(
    selected: List<com.music.bitchord.data.settings.AppSettings.OnboardingArtist>,
    themeMode: ThemeMode,
    liquidGlass: Boolean,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val containerTarget = if (dark) Color(0xFF111114) else Color(0xFFF6F5F8)
    val cardTarget = if (dark) Color(0xFF242429) else Color(0xFFF1F0F4)
    val foregroundTarget = if (dark) Color.White else Color(0xFF17171A)
    val mutedTarget = if (dark) Color.White.copy(alpha = .68f) else Color(0xFF626268)
    val container by animateColorAsState(containerTarget, spring(stiffness = 360f), label = "previewContainer")
    val card by animateColorAsState(
        if (liquidGlass) cardTarget.copy(alpha = .54f) else cardTarget,
        spring(stiffness = 360f),
        label = "previewCard",
    )
    val foreground by animateColorAsState(foregroundTarget, spring(stiffness = 360f), label = "previewForeground")
    val muted by animateColorAsState(mutedTarget, spring(stiffness = 360f), label = "previewMuted")
    val glassAlpha by animateFloatAsState(if (liquidGlass) .76f else 1f, spring(stiffness = 360f), label = "previewGlassAlpha")
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val lead = selected.firstOrNull()
    val selectedArtistNames = selected.take(2).joinToString { it.name }
    val previewArtistLine = if (selectedArtistNames.isBlank()) stringResource(R.string.app_name) else selectedArtistNames
    val shape = RoundedCornerShape(26.dp)

    Column(
        Modifier.fillMaxWidth().height(242.dp).clip(shape)
            .background(container)
            .border(1.dp, Color.White.copy(alpha = if (liquidGlass) .28f else .12f), shape)
            .padding(12.dp),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(18.dp))) {
            Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(Color(0xFFCE6F44), Color(0xFF5B3554), Color(0xFF202A40)))))
            if (!lead?.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = lead?.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize().blur(if (liquidGlass) 20.dp else 0.dp).graphicsLayer { alpha = if (liquidGlass) .68f else .4f },
                    contentScale = ContentScale.Crop,
                )
            }
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = if (dark) .30f else .12f)))
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Text(stringResource(R.string.onboarding_preview_title), color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy((-7).dp)) {
                    if (selected.isEmpty()) {
                        repeat(4) { index ->
                            Box(Modifier.size(38.dp).clip(CircleShape).background(listOf(Color(0xFFDA7E54), Color(0xFF765472), Color(0xFF548596), Color(0xFFB98A48))[index]))
                        }
                    } else {
                        selected.take(4).forEach { artist ->
                            Box(Modifier.size(38.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = .8f), CircleShape)) {
                                if (!artist.thumbnailUrl.isNullOrBlank()) AsyncImage(artist.thumbnailUrl, null, Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                else Box(Modifier.fillMaxSize().background(Color(0xFF28282D)), contentAlignment = Alignment.Center) {
                                    Text(artist.name.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.fillMaxWidth().clip(CircleShape).background(card.copy(alpha = glassAlpha)).border(1.dp, Color.White.copy(alpha = if (liquidGlass) .34f else .12f), CircleShape).padding(horizontal = 9.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                        if (!lead?.thumbnailUrl.isNullOrBlank()) AsyncImage(lead?.thumbnailUrl, null, Modifier.fillMaxSize().clip(RoundedCornerShape(9.dp)), contentScale = ContentScale.Crop)
                        else Icon(Icons.Rounded.Radio, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(lead?.name ?: stringResource(R.string.onboarding_preview_title), color = foreground, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(previewArtistLine, color = muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Rounded.PlayArrow, null, tint = foreground, modifier = Modifier.size(23.dp))
                    Icon(Icons.Rounded.SkipNext, null, tint = foreground, modifier = Modifier.padding(start = 6.dp, end = 2.dp).size(23.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val tabs = listOf(
                Triple(stringResource(R.string.home), BitChordIcons.Home, 0),
                Triple(stringResource(R.string.explore), BitChordIcons.TabExplore, 1),
                Triple(stringResource(R.string.onboarding_tab_radio), SoundChordIcons.TabRadio, 2),
                Triple(stringResource(R.string.library), BitChordIcons.TabLibrary, 3),
            )
            Row(
                Modifier.weight(1f).height(48.dp).clip(CircleShape).background(card.copy(alpha = glassAlpha)).border(1.dp, Color.White.copy(alpha = if (liquidGlass) .34f else .12f), CircleShape).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { (label, icon, index) ->
                    val active by animateColorAsState(if (selectedTab == index) Color(0xFFE62E55) else foreground.copy(alpha = .75f), spring(stiffness = 360f), label = "previewTab")
                    Column(
                        Modifier.weight(1f).fillMaxSize().clip(CircleShape).clickable { selectedTab = index },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = active, modifier = Modifier.size(18.dp))
                        Text(label, color = if (selectedTab == index) active else muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(card.copy(alpha = glassAlpha)).border(1.dp, Color.White.copy(alpha = if (liquidGlass) .34f else .12f), CircleShape).clickable { selectedTab = 4 },
                contentAlignment = Alignment.Center,
            ) {
                val searchTint by animateColorAsState(if (selectedTab == 4) Color(0xFFE62E55) else foreground, spring(stiffness = 360f), label = "previewSearch")
                Icon(BitChordIcons.TabSearch, contentDescription = stringResource(R.string.search), tint = searchTint, modifier = Modifier.size(21.dp))
            }
        }
    }
}

@Composable
private fun ArtistSearchField(value: String, onValueChange: (String) -> Unit) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = .14f)).padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = .7f), modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(11.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(stringResource(R.string.onboarding_search_artists), style = textStyle, color = Color.White.copy(alpha = .5f))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.onboarding_clear_search), tint = Color.White.copy(alpha = .7f), modifier = Modifier.size(22.dp).clickable { onValueChange("") })
        }
    }
}

@Composable
private fun ArtistChoiceTile(
    suggestion: OnboardingViewModel.ArtistSuggestion,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val artist = suggestion.artist
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (!artist.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artist.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.size(82.dp).clip(CircleShape)
                        .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color.White.copy(alpha = .3f), CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(Modifier.size(82.dp).clip(CircleShape).background(Color.White.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                    Text(artist.title.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.headlineSmall)
                }
            }
            if (selected) {
                Box(Modifier.align(Alignment.BottomEnd).padding(end = 8.dp).size(25.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(17.dp))
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            artist.title,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        suggestion.relatedToArtistName?.let { sourceName ->
            Text(
                stringResource(R.string.onboarding_similar_to_artist, sourceName),
                color = Color.White.copy(alpha = .58f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SelectedArtistAvatars(artists: List<com.music.bitchord.data.settings.AppSettings.OnboardingArtist>) {
    Box(Modifier.width(66.dp).height(34.dp)) {
        artists.take(3).forEachIndexed { index, artist ->
            Box(
                Modifier.offset(x = (index * 16).dp).size(32.dp).clip(CircleShape)
                    .border(1.5.dp, Color.Black.copy(alpha = .25f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (!artist.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = artist.thumbnailUrl,
                        contentDescription = artist.name,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Color(0xFF28282D)), contentAlignment = Alignment.Center) {
                        Text(artist.name.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.NamePage(
    name: String,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    // With the keyboard up there is room for the title or the logo, not both;
    // the title is the one that says where you are.
    val keyboardUp = WindowInsets.isImeVisible
    AnimatedVisibility(
        visible = !keyboardUp,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Column {
            Spacer(Modifier.height(56.dp))
            Image(
                painter = painterResource(Res.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier.size(width = 46.dp, height = 30.dp),
            )
        }
    }
    Spacer(Modifier.height(24.dp))
    Title(stringResource(R.string.onboarding_title))

    Spacer(Modifier.weight(1f))

    Text(
        text = stringResource(R.string.onboarding_name_prompt),
        style = MaterialTheme.typography.titleMedium,
        fontSize = 17.sp,
        color = Color.White.copy(alpha = 0.78f),
        modifier = Modifier.padding(start = 4.dp),
    )
    Spacer(Modifier.height(12.dp))
    NameField(value = name, onValueChange = onNameChange, onDone = onNext)
    Spacer(Modifier.height(10.dp))
    Text(
        text = stringResource(R.string.onboarding_name_footer),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.55f),
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    Spacer(Modifier.height(28.dp))
    PillButton(
        label = stringResource(R.string.onboarding_next),
        onClick = onNext,
        enabled = name.isNotBlank(),
        primary = true,
    )
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun ColumnScope.ConnectPage(
    signedIn: Boolean,
    accountName: String?,
    accountAvatarUrl: String?,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onFinish: () -> Unit,
) {
    OnboardingStepHeader(step = PAGE_CONNECT, onBack = onBack)
    Spacer(Modifier.height(12.dp))
    Title(stringResource(if (signedIn) R.string.onboarding_connect_title_signed_in else R.string.onboarding_connect_title))
    Spacer(Modifier.height(14.dp))
    Text(
        text = if (signedIn) {
            stringResource(R.string.onboarding_account_connected, accountName?.takeIf(String::isNotBlank) ?: stringResource(R.string.onboarding_account_fallback))
        } else {
            stringResource(R.string.onboarding_connect_body)
        },
        style = MaterialTheme.typography.bodyLarge,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        color = Color.White.copy(alpha = 0.72f),
    )

    if (signedIn) {
        Spacer(Modifier.height(28.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .14f)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                if (!accountAvatarUrl.isNullOrBlank()) {
                    AsyncImage(accountAvatarUrl, accountName, Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Rounded.AccountCircle, null, Modifier.size(48.dp), tint = Color.White)
                }
            }
            Text(
                accountName?.takeIf(String::isNotBlank) ?: stringResource(R.string.onboarding_account_fallback),
                Modifier.padding(start = 14.dp),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    Spacer(Modifier.weight(1f))

    if (signedIn) {
        PillButton(label = stringResource(R.string.onboarding_start_listening), onClick = onFinish, primary = true)
    } else {
        PillButton(
            label = stringResource(R.string.onboarding_sign_in_google),
            onClick = onSignIn,
            primary = true,
            leading = {
                Image(
                    painter = painterResource(Res.drawable.ic_google_g),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
        )
        Spacer(Modifier.height(12.dp))
        PillButton(
            label = stringResource(R.string.onboarding_skip_sign_in),
            onClick = onFinish,
            primary = false,
        )
    }
    Spacer(Modifier.height(16.dp))
}

/** Replay's heading, scaled up: the one line each page exists to say. */
@Composable
private fun Title(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = 40.sp,
            lineHeight = 44.sp,
            letterSpacing = (-1.2).sp,
        ),
        color = Color.White,
    )
}

/**
 * The name, set in the heading's own face and weight so what you type reads
 * as part of the page rather than a form on top of it.
 */
@Composable
private fun NameField(value: String, onValueChange: (String) -> Unit, onDone: () -> Unit) {
    val style = MaterialTheme.typography.displayLarge.copy(
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.6).sp,
        color = Color.White,
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                text = stringResource(R.string.onboarding_name_hint),
                style = style,
                color = Color.White.copy(alpha = 0.38f),
                maxLines = 1,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { onDone() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * A full-width capsule. Primary is white on the mesh, the way Apple Music's
 * own welcome sheet does it; secondary is the same shape in frosted white, so
 * the pair reads as a choice rather than a button and an afterthought.
 */
@Composable
private fun PillButton(
    label: String,
    onClick: () -> Unit,
    primary: Boolean,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(stiffness = 900f), label = "pillScale")
    val alpha by animateFloatAsState(if (enabled) 1f else 0.4f, tween(180), label = "pillAlpha")
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(CircleShape)
            .background(if (primary) Color.White else Color.White.copy(alpha = 0.16f))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 17.sp,
            fontWeight = FontWeight.W700,
            color = if (primary) Color.Black else Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = BitChordIcons.ChevronRight,
            contentDescription = stringResource(R.string.onboarding_back),
            tint = Color.White,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { scaleX = -1f },
        )
    }
}

private const val PAGE_NAME = 0
private const val PAGE_ARTISTS = 1
private const val PAGE_LOOK = 2
private const val PAGE_CONNECT = 3

/** Long enough for a full name; the party server keeps the first 80. */
private const val NAME_LIMIT = 40

/** The account page keeps leaning into YouTube's red while it moves. */
private val ConnectPalettes = listOf(
    MeshPalette(listOf(Color(0xFFFF1E3C), Color(0xFFC2185B), Color(0xFFFF8F3D), Color(0xFF4A1C8C))),
    MeshPalette(listOf(Color(0xFFE53935), Color(0xFFFF6E40), Color(0xFFAD1457), Color(0xFF311B92))),
    MeshPalette(listOf(Color(0xFFFF3D00), Color(0xFFD81B60), Color(0xFF8E24AA), Color(0xFFB71C1C))),
    MeshPalette(listOf(Color(0xFFF50057), Color(0xFFFF9100), Color(0xFFC51162), Color(0xFF4527A0))),
)
