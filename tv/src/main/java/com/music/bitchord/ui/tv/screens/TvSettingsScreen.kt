package com.music.bitchord.ui.tv.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.music.bitchord.R
import com.music.bitchord.BuildConfig
import com.music.bitchord.data.AppUpdateChecker
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.TvSettings
import com.music.bitchord.ui.MainViewModel
import com.music.bitchord.ui.tv.TvAppMark
import com.music.bitchord.ui.tv.audio.TvSpatialAudioEngine
import com.music.bitchord.ui.tv.components.TvChromePinned
import com.music.bitchord.ui.tv.components.TvListHeader
import com.music.bitchord.ui.tv.components.TvListRow
import com.music.bitchord.ui.tv.components.TvDialog
import com.music.bitchord.ui.tv.components.TvTextField
import com.music.bitchord.ui.tv.dialogs.TvAboutDialog
import com.music.bitchord.ui.tv.dialogs.TvEqualizerDialog
import com.music.bitchord.ui.tv.dialogs.TvLyricsSourcesDialog
import com.music.bitchord.ui.tv.dialogs.TvRefreshRateDialog
import com.music.bitchord.ui.tv.dialogs.TvScrobbleDialog
import com.music.bitchord.data.settings.TvPlayerStyle
import com.music.bitchord.ui.tv.dialogs.TvThemeDialog
import com.music.bitchord.ui.tv.dialogs.TvUpdateDialog
import com.music.bitchord.ui.tv.display.TvRefreshRateController
import com.music.bitchord.ui.tv.display.TvRefreshRatePreference
import com.music.bitchord.data.lyrics.TRANSLATION_LANGUAGES
import com.music.bitchord.data.lyrics.translationLanguageName
import java.util.Locale
import com.music.bitchord.ui.tv.personalization.AppThemeOption
import com.music.bitchord.ui.tv.theme.TvDimensions
import com.music.bitchord.ui.tv.theme.TvGlass
import com.music.bitchord.ui.tv.theme.TvType

// Discord Rich Presence is hidden on TV for now; TvDiscordDialog is kept for when it returns.
private enum class SettingsSheet { Theme, RefreshRate, Equalizer, Scrobbling, LyricsSources, TranslationLanguage, Language, Update, About }

/**
 * Settings, as tvOS lays it out: the app on the left with a plain description of
 * whatever row has focus, the grouped rows on the right. Values are written out
 * ("On", "6 s") and change in place on select; rows that need a choice open one.
 */
@Composable
fun TvSettingsScreen(
    viewModel: MainViewModel,
    onOpenAccount: () -> Unit,
    onRunSetupAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvChromePinned()
    val signedIn by viewModel.signedIn.collectAsState()
    val account by viewModel.account.collectAsState()

    val automix by AppSettings.smartFadeEnabled.collectAsState()
    val crossfade by AppSettings.crossfadeSeconds.collectAsState()
    val autoplay by AppSettings.autoplay.collectAsState()
    val skipSilence by AppSettings.skipSilence.collectAsState()
    val eqEnabled by AppSettings.equalizerEnabled.collectAsState()
    val spatial by TvSettings.spatialAudioEnabled.collectAsState()
    val loudness by AppSettings.loudnessNormalization.collectAsState()
    val usbDac by AppSettings.preferUsbDac.collectAsState()
    val syncedLyrics by AppSettings.syncedLyrics.collectAsState()
    val lyricsSources by AppSettings.lyricsSources.collectAsState()
    val canvas by AppSettings.animatedCanvas.collectAsState()
    val lyricsCanvas by TvSettings.tvLyricsCanvasEnabled.collectAsState()
    val nerdStats by AppSettings.showNerdStats.collectAsState()
    val themeId by TvSettings.tvTheme.collectAsState()
    val playerStyle by TvSettings.tvPlayerStyle.collectAsState()
    val uiScale by TvSettings.tvUiScale.collectAsState()
    val refreshRate by TvRefreshRateController.preference.collectAsState()
    val reduceMotion by AppSettings.reduceAnimation.collectAsState()
    val reduceTransparency by AppSettings.reduceDynamicBlur.collectAsState()
    val translationLanguage by AppSettings.translationLanguage.collectAsState()
    val appLanguage = AppCompatDelegate.getApplicationLocales().get(0)?.language.orEmpty()
    val context = LocalContext.current
    fun contextString(id: Int): String = context.getString(id)
    val appLanguageLabel = when (appLanguage) {
        "en" -> stringResource(R.string.tv_language_english_name)
        "es" -> stringResource(R.string.tv_language_spanish_name)
        else -> stringResource(R.string.tv_language_system_default)
    }

    var blurb by remember(context) { mutableStateOf(context.getString(R.string.tv_settings_blurb)) }
    var sheet by remember { mutableStateOf<SettingsSheet?>(null) }
    val update by AppUpdateChecker.available.collectAsState()

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(top = TvDimensions.ContentTop),
    ) {
        Column(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TvAppMark(size = 120.dp)
            Spacer(modifier = Modifier.height(22.dp))
            AnimatedContent(
                targetState = blurb,
                transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                label = "settingsBlurb",
            ) { text ->
                Text(
                    text = text,
                    style = TvType.Callout,
                    color = TvGlass.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 272.dp),
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(end = TvDimensions.SafeMarginHorizontal, bottom = 38.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            item { TvListHeader(stringResource(R.string.tv_settings_account)) }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_youtube_music),
                    value = if (signedIn) account?.name ?: stringResource(R.string.tv_settings_signed_in) else stringResource(R.string.tv_settings_sign_in),
                    onFocused = { blurb = contextString(R.string.tv_settings_account_description) },
                    onClick = onOpenAccount,
                )
            }

            item { TvListHeader(stringResource(R.string.tv_settings_playback)) }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_automix),
                    value = stringResource(if (automix) R.string.tv_on else R.string.tv_off),
                    onFocused = { blurb = contextString(R.string.tv_settings_automix_description) },
                    onClick = { AppSettings.setSmartFadeEnabled(!automix) },
                )
            }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_crossfade),
                    value = if (crossfade > 0) stringResource(R.string.tv_crossfade_seconds, crossfade) else stringResource(R.string.tv_off),
                    onFocused = { blurb = contextString(R.string.tv_settings_crossfade_description) },
                    onClick = {
                        AppSettings.setCrossfadeSeconds(
                            when (crossfade) {
                                0 -> 3
                                3 -> 6
                                6 -> 10
                                else -> 0
                            },
                        )
                    },
                )
            }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_autoplay),
                    value = stringResource(if (autoplay) R.string.tv_on else R.string.tv_off),
                    onFocused = { blurb = contextString(R.string.tv_settings_autoplay_description) },
                    onClick = { AppSettings.setAutoplay(!autoplay) },
                )
            }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_skip_silence),
                    value = stringResource(if (skipSilence) R.string.tv_on else R.string.tv_off),
                    onFocused = { blurb = contextString(R.string.tv_settings_skip_silence_description) },
                    onClick = { AppSettings.setSkipSilence(!skipSilence) },
                )
            }

            item { TvListHeader(stringResource(R.string.tv_settings_sound)) }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_equalizer),
                    value = stringResource(if (eqEnabled) R.string.tv_on else R.string.tv_off),
                    onFocused = { blurb = contextString(R.string.tv_settings_equalizer_description) },
                    onClick = { sheet = SettingsSheet.Equalizer },
                )
            }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_spatial_audio),
                    value = stringResource(if (spatial) R.string.tv_on else R.string.tv_off),
                    onFocused = { blurb = contextString(R.string.tv_settings_spatial_audio_description) },
                    onClick = {
                        TvSettings.setSpatialAudioEnabled(!spatial)
                        TvSpatialAudioEngine.setEnabled(!spatial)
                    },
                )
            }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_sound_check),
                    value = stringResource(if (loudness) R.string.tv_on else R.string.tv_off),
                    onFocused = { blurb = contextString(R.string.tv_settings_sound_check_description) },
                    onClick = { AppSettings.setLoudnessNormalization(!loudness) },
                )
            }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_usb_dac),
                    value = stringResource(if (usbDac) R.string.tv_on else R.string.tv_off),
                    onFocused = { blurb = contextString(R.string.tv_settings_usb_dac_description) },
                    onClick = { AppSettings.setPreferUsbDac(!usbDac) },
                )
            }

            item { TvListHeader(stringResource(R.string.tv_settings_now_playing)) }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_synced_lyrics),
                    value = stringResource(if (syncedLyrics) R.string.tv_on else R.string.tv_off),
                    onFocused = { blurb = contextString(R.string.tv_settings_synced_lyrics_description) },
                    onClick = { AppSettings.setSyncedLyrics(!syncedLyrics) },
                )
            }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_lyrics_sources),
                    value = stringResource(R.string.tv_settings_lyrics_sources_value, lyricsSources.size),
                    onFocused = { blurb = contextString(R.string.tv_settings_lyrics_sources_description) },
                    onClick = { sheet = SettingsSheet.LyricsSources },
                )
            }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_translation_language),
                    value = if (translationLanguage.isBlank()) stringResource(R.string.tv_translation_use_app_language)
                    else translationLanguageName(translationLanguage, Locale.getDefault()),
                    onClick = { sheet = SettingsSheet.TranslationLanguage },
                )
            }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_motion_artwork),
                    value = stringResource(if (canvas) R.string.tv_on else R.string.tv_off),
                    onFocused = { blurb = contextString(R.string.tv_settings_motion_artwork_description) },
                    onClick = { AppSettings.setAnimatedCanvas(!canvas) },
                )
            }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_motion_lyrics),
                    value = stringResource(if (lyricsCanvas) R.string.tv_on else R.string.tv_off),
                    onFocused = { blurb = contextString(R.string.tv_settings_motion_lyrics_description) },
                    onClick = { TvSettings.setTvLyricsCanvasEnabled(!lyricsCanvas) },
                )
            }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_nerd_stats),
                    value = stringResource(if (nerdStats) R.string.tv_on else R.string.tv_off),
                    onFocused = { blurb = contextString(R.string.tv_settings_nerd_stats_description) },
                    onClick = { AppSettings.setShowNerdStats(!nerdStats) },
                )
            }

            item { TvListHeader(stringResource(R.string.tv_settings_display)) }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_appearance),
                    value = stringResource(AppThemeOption.fromId(themeId).titleRes()),
                    onFocused = { blurb = contextString(R.string.tv_settings_appearance_description) },
                    onClick = { sheet = SettingsSheet.Theme },
                )
            }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_player_background),
                    value = if (playerStyle == TvPlayerStyle.DEFAULT) stringResource(R.string.tv_default) else "AMOLED",
                    onFocused = { blurb = contextString(R.string.tv_settings_player_background_description) },
                    onClick = {
                        val styles = TvPlayerStyle.entries
                        TvSettings.setTvPlayerStyle(styles[(playerStyle.ordinal + 1) % styles.size])
                    },
                )
            }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_interface_size),
                    value = "${(uiScale * 100).toInt()}%",
                    onFocused = { blurb = contextString(R.string.tv_settings_interface_size_description) },
                    onClick = {
                        val steps = listOf(0.8f, 0.9f, 1f, 1.1f, 1.2f, 1.3f)
                        val next = steps.firstOrNull { it > uiScale + 0.01f } ?: steps.first()
                        TvSettings.setTvUiScale(next)
                    },
                )
            }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_refresh_rate),
                    value = stringResource(refreshRate.titleRes()),
                    onFocused = { blurb = contextString(R.string.tv_settings_refresh_rate_description) },
                    onClick = { sheet = SettingsSheet.RefreshRate },
                )
            }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_reduce_motion),
                    value = stringResource(if (reduceMotion) R.string.tv_on else R.string.tv_off),
                    onFocused = { blurb = contextString(R.string.tv_settings_reduce_motion_description) },
                    onClick = { AppSettings.setReduceAnimation(!reduceMotion) },
                )
            }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_reduce_transparency),
                    value = stringResource(if (reduceTransparency) R.string.tv_on else R.string.tv_off),
                    onFocused = { blurb = contextString(R.string.tv_settings_reduce_transparency_description) },
                    onClick = { AppSettings.setReduceDynamicBlur(!reduceTransparency) },
                )
            }

            item { TvListHeader(stringResource(R.string.tv_settings_connections)) }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_scrobbling),
                    onFocused = { blurb = contextString(R.string.tv_settings_scrobbling_description) },
                    onClick = { sheet = SettingsSheet.Scrobbling },
                )
            }

            item { TvListHeader(stringResource(R.string.tv_settings_general)) }
            if (update != null) {
                item {
                    TvListRow(
                        title = stringResource(R.string.tv_settings_software_update),
                        value = stringResource(R.string.tv_settings_update_available, update?.version.orEmpty()),
                        onFocused = { blurb = contextString(R.string.tv_settings_update_description) },
                        onClick = { sheet = SettingsSheet.Update },
                    )
                }
            }
            item {
                TvListRow(
                    title = stringResource(R.string.app_language),
                    value = appLanguageLabel,
                    onFocused = { blurb = contextString(R.string.app_language_description) },
                    onClick = { sheet = SettingsSheet.Language },
                )
            }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_setup_again),
                    onFocused = { blurb = contextString(R.string.tv_settings_setup_again_description) },
                    onClick = onRunSetupAgain,
                )
            }
            item {
                TvListRow(
                    title = stringResource(R.string.tv_settings_about),
                    value = BuildConfig.VERSION_NAME,
                    onFocused = { blurb = contextString(R.string.tv_settings_blurb) },
                    onClick = { sheet = SettingsSheet.About },
                )
            }
        }
    }

    when (sheet) {
        SettingsSheet.Theme -> TvThemeDialog(onDismiss = { sheet = null })
        SettingsSheet.RefreshRate -> TvRefreshRateDialog(onDismiss = { sheet = null })
        SettingsSheet.Equalizer -> TvEqualizerDialog(onDismiss = { sheet = null })
        SettingsSheet.Scrobbling -> TvScrobbleDialog(onDismiss = { sheet = null })
        SettingsSheet.LyricsSources -> TvLyricsSourcesDialog(onDismiss = { sheet = null })
        SettingsSheet.TranslationLanguage -> TvTranslationLanguageDialog(
            selected = translationLanguage,
            onDismiss = { sheet = null },
            onSelect = { language -> AppSettings.setTranslationLanguage(language); sheet = null },
        )
        SettingsSheet.Language -> TvLanguageDialog(
            selectedLanguage = appLanguage,
            onDismiss = { sheet = null },
            onSelect = { tag ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                sheet = null
            },
        )
        SettingsSheet.Update -> update?.let { TvUpdateDialog(update = it, onDismiss = { sheet = null }) }
        SettingsSheet.About -> TvAboutDialog(onDismiss = { sheet = null })
        null -> Unit
    }
}

@Composable
private fun TvLanguageDialog(selectedLanguage: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    TvDialog(onDismissRequest = onDismiss, title = stringResource(R.string.tv_language_dialog_title)) {
        listOf(
            "es" to R.string.tv_language_spanish_name,
            "en" to R.string.tv_language_english_name,
            "" to R.string.tv_language_system_default,
        ).forEach { (tag, label) ->
            TvListRow(
                title = stringResource(label),
                trailingIcon = if (selectedLanguage == tag) Icons.Rounded.Check else null,
                onClick = { onSelect(tag) },
            )
        }
    }
}

@Composable
private fun TvTranslationLanguageDialog(selected: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val locale = Locale.getDefault()
    val languages = remember(locale) {
        TRANSLATION_LANGUAGES.map { language ->
            language.code to translationLanguageName(language.code, locale)
        }
    }
    val shown = remember(languages, query) {
        val needle = query.trim()
        if (needle.isEmpty()) languages else languages.filter { (code, name) ->
            name.contains(needle, ignoreCase = true) ||
                TRANSLATION_LANGUAGES.firstOrNull { it.code == code }?.fallbackName?.contains(needle, ignoreCase = true) == true
        }
    }
    TvDialog(onDismissRequest = onDismiss, title = stringResource(R.string.tv_translation_language), width = 576.dp) {
        TvTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.tv_translation_search),
            capitalization = KeyboardCapitalization.None,
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        )
        LazyColumn(
            modifier = Modifier.heightIn(max = 340.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                TvListRow(
                    title = stringResource(R.string.tv_translation_use_app_language),
                    subtitle = stringResource(R.string.tv_translation_follows_app),
                    trailingIcon = if (selected.isBlank()) Icons.Rounded.Check else null,
                    onClick = { onSelect("") },
                )
            }
            if (shown.isEmpty()) {
                item { Text(stringResource(R.string.tv_translation_no_matches), color = TvGlass.TextSecondary) }
            } else {
                items(shown, key = { it.first }) { (code, name) ->
                    TvListRow(
                        title = name,
                        trailingIcon = if (selected.equals(code, ignoreCase = true)) Icons.Rounded.Check else null,
                        onClick = { onSelect(code) },
                    )
                }
            }
        }
    }
}

private fun AppThemeOption.titleRes(): Int = when (this) {
    AppThemeOption.DYNAMIC_ARTWORK -> R.string.tv_theme_artwork
    AppThemeOption.MIDNIGHT -> R.string.tv_theme_midnight
    AppThemeOption.PURE_BLACK -> R.string.tv_theme_pure_black
}

private fun TvRefreshRatePreference.titleRes(): Int = when (this) {
    TvRefreshRatePreference.SYSTEM_AUTO -> R.string.tv_refresh_auto
    TvRefreshRatePreference.SMOOTH_60 -> R.string.tv_refresh_60
    TvRefreshRatePreference.ULTRA_120 -> R.string.tv_refresh_120
}
