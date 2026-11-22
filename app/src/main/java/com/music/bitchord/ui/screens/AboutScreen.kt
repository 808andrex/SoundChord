package com.music.bitchord.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.music.bitchord.BuildConfig
import com.music.bitchord.R
import com.music.bitchord.data.AppUpdateChecker
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.components.IosSwitch
import com.music.bitchord.ui.components.AlertAction
import com.music.bitchord.ui.components.AlertRule
import com.music.bitchord.ui.components.optimizedHazeEffect
import com.music.bitchord.ui.icons.SfIcons
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch

private const val REPOSITORY_URL = "https://github.com/808andrex/SoundChord"
private const val DISCUSSIONS_URL = "$REPOSITORY_URL/discussions"
private const val ISSUES_URL = "$REPOSITORY_URL/issues"
private const val DEVELOPER_URL = "https://github.com/808andrex"
private const val BASE_PROJECT_URL = "https://github.com/kushagrasinghx/BitChord"

@Composable
fun AboutScreen(
    contentPadding: PaddingValues,
    onShowWhatsNew: () -> Unit,
    onShowUpdateDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val autoUpdates by AppSettings.autoUpdateCheck.collectAsState()
    var checking by remember { mutableStateOf(false) }
    var checkResult by remember { mutableStateOf<AppUpdateChecker.CheckResult?>(null) }
    val currentVersion = BuildConfig.VERSION_NAME

    Column(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsPageTitle(stringResource(R.string.settings_category_about))

        SettingsGroup(header = stringResource(R.string.about_updates)) {
            SettingsRow(
                icon = SfIcons.Notes,
                title = stringResource(R.string.about_whats_new),
                subtitle = stringResource(R.string.about_whats_new_subtitle, currentVersion),
                onClick = onShowWhatsNew,
            )
            RowDivider()
            SettingsRow(
                icon = SfIcons.Cached,
                title = stringResource(R.string.about_check_updates),
                subtitle = stringResource(R.string.about_current_version, currentVersion),
                trailing = {
                    if (checking) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(SfIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                onClick = {
                    if (!checking) {
                        checking = true
                        checkResult = null
                        scope.launch {
                            val result = AppUpdateChecker.check()
                            checking = false
                            checkResult = result
                            if (result == AppUpdateChecker.CheckResult.UPDATE_AVAILABLE) onShowUpdateDialog()
                        }
                    }
                },
            )
            checkResult?.let { result ->
                Text(
                    text = stringResource(
                        when (result) {
                            AppUpdateChecker.CheckResult.UPDATE_AVAILABLE -> R.string.about_update_found
                            AppUpdateChecker.CheckResult.UP_TO_DATE -> R.string.about_up_to_date
                            AppUpdateChecker.CheckResult.FAILED -> R.string.about_update_check_failed
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (result == AppUpdateChecker.CheckResult.FAILED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.padding(start = 72.dp, end = 16.dp, bottom = 12.dp),
                )
            }
            RowDivider()
            SettingsRow(
                icon = SfIcons.Clock,
                title = stringResource(R.string.about_auto_updates),
                subtitle = stringResource(R.string.about_auto_updates_subtitle),
                trailing = {
                    IosSwitch(
                        checked = autoUpdates,
                        onCheckedChange = AppSettings::setAutoUpdateCheck,
                    )
                },
                onClick = { AppSettings.setAutoUpdateCheck(!autoUpdates) },
            )
        }

        SettingsGroup(header = stringResource(R.string.about_community)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable { openUrl(context, DISCUSSIONS_URL) }
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(SfIcons.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.about_join_community),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.about_join_community_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(SfIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            RowDivider()
            SettingsRow(
                icon = SfIcons.Heart,
                title = stringResource(R.string.about_support),
                subtitle = stringResource(R.string.about_support_subtitle),
                onClick = { openUrl(context, REPOSITORY_URL) },
            )
            RowDivider()
            SettingsRow(
                icon = SfIcons.Star,
                title = stringResource(R.string.about_star_github),
                subtitle = stringResource(R.string.about_star_github_subtitle),
                onClick = { openUrl(context, REPOSITORY_URL) },
            )
            RowDivider()
            SettingsRow(
                icon = SfIcons.Info,
                title = stringResource(R.string.about_source_code),
                subtitle = stringResource(R.string.about_source_code_subtitle),
                onClick = { openUrl(context, REPOSITORY_URL) },
            )
        }

        SettingsGroup(header = stringResource(R.string.about_developers)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openUrl(context, DEVELOPER_URL) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = "https://github.com/808andrex.png",
                    contentDescription = stringResource(R.string.about_developer_avatar),
                    placeholder = painterResource(R.drawable.ic_settings_about),
                    error = painterResource(R.drawable.ic_settings_about),
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "808andrex",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stringResource(R.string.about_developer_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_github),
                        contentDescription = stringResource(R.string.about_github_profile),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            RowDivider()
            SettingsRow(
                icon = SfIcons.Info,
                title = stringResource(R.string.about_base_project),
                subtitle = stringResource(R.string.about_base_project_subtitle),
                onClick = { openUrl(context, BASE_PROJECT_URL) },
            )
        }

        SettingsGroup(header = stringResource(R.string.about_help)) {
            SettingsRow(
                icon = SfIcons.Bug,
                title = stringResource(R.string.about_report_issue),
                subtitle = stringResource(R.string.about_report_issue_subtitle),
                onClick = { openUrl(context, ISSUES_URL) },
            )
        }
        Spacer(Modifier.height(20.dp))
    }

}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun AboutWhatsNewOverlay(
    visible: Boolean,
    hazeState: HazeState,
    onDismiss: () -> Unit,
) {
    val version = BuildConfig.VERSION_NAME
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsState()
    val lightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val glassSurface = if (lightTheme) {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f)
    } else {
        Color(0xFF141418).copy(alpha = 0.88f)
    }
    val glassBorder = if (lightTheme) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.14f)
    var changelog by remember { mutableStateOf<String?>(null) }
    var loadingChangelog by remember { mutableStateOf(false) }
    BackHandler(enabled = visible, onBack = onDismiss)
    LaunchedEffect(visible) {
        if (visible && changelog == null && !loadingChangelog) {
            loadingChangelog = true
            try {
                changelog = AppUpdateChecker.currentReleaseNotes()
            } finally {
                loadingChangelog = false
            }
        }
    }
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize().zIndex(100f),
        enter = fadeIn(tween(160)) + scaleIn(
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 600f),
            initialScale = 0.9f,
        ),
        exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.94f),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val notesMaxHeight = minOf(560.dp, maxHeight * 0.72f)
            val notesMinHeight = minOf(340.dp, notesMaxHeight)
            Box(
                Modifier.fillMaxSize().then(
                    if (reduceDynamicBlur) {
                        Modifier.background(if (lightTheme) MaterialTheme.colorScheme.surfaceContainer else Color(0xFF141418))
                    } else {
                        Modifier.optimizedHazeEffect(
                            state = hazeState,
                            style = HazeMaterials.regular(glassSurface),
                        )
                    },
                ),
            )
            Box(
                Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = if (lightTheme) 0.25f else 0.65f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDismiss,
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .then(
                        if (reduceDynamicBlur) {
                            Modifier.background(if (lightTheme) MaterialTheme.colorScheme.surfaceContainer else Color(0xFF141418))
                        } else {
                            Modifier.optimizedHazeEffect(
                                state = hazeState,
                                style = HazeMaterials.regular(glassSurface),
                            )
                        },
                    )
                    .border(BorderStroke(1.dp, glassBorder), RoundedCornerShape(24.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {},
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.about_whats_new_title, version),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 4.dp)
                            .heightIn(min = notesMinHeight, max = notesMaxHeight)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        when {
                            loadingChangelog -> CircularProgressIndicator()
                            changelog.isNullOrBlank() -> Text(
                                stringResource(R.string.about_changelog_unavailable),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            else -> Text(
                                text = remember(changelog) { releaseNotesAnnotatedString(changelog.orEmpty()) },
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                AlertRule()
                AlertAction(label = stringResource(R.string.close), emphasised = true, onClick = onDismiss)
            }
        }
    }
}

private fun releaseNotesAnnotatedString(markdown: String) = buildAnnotatedString {
    markdown.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty()) {
            append("\n")
            return@forEach
        }

        val heading = line.startsWith("##")
        val bullet = line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ")
        val content = when {
            heading -> line.dropWhile { it == '#' }.trim()
            bullet -> line.drop(2).trim()
            else -> line
        }

        if (heading) withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)) {
            appendInlineMarkdown(content)
        } else {
            if (bullet) append("•  ")
            appendInlineMarkdown(content)
        }
        append("\n")
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendInlineMarkdown(text: String) {
    var index = 0
    while (index < text.length) {
        val boldStart = text.indexOf("**", index)
        if (boldStart < 0) {
            append(text.substring(index))
            break
        }
        append(text.substring(index, boldStart))
        val boldEnd = text.indexOf("**", boldStart + 2)
        if (boldEnd < 0) {
            append(text.substring(boldStart))
            break
        }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(text.substring(boldStart + 2, boldEnd))
        }
        index = boldEnd + 2
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
