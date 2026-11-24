package com.music.bitchord.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.AppUpdateChecker
import com.music.bitchord.data.settings.AppSettings
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay

/** Highlight note with an icon, bold title and optional description. */
data class HighlightNote(
    val icon: ImageVector,
    val title: String,
    val description: String = "",
)

/** Structured release notes for the What's New modal. */
data class WhatsNewData(
    val version: String,
    val highlights: List<HighlightNote>,
    val visualImprovements: List<String>,
    val fixes: List<String>,
)

/**
 * Manages detection of seen app versions in SharedPreferences.
 * Determines whether the What's New dialog should be shown automatically on launch.
 */
object WhatsNewManager {
    private const val PREFS = "whats_new"
    private const val KEY_SEEN = "seen"
    private const val KEY_PENDING_AFTER_UPDATE = "pending_after_update"

    /** Marks that an update APK was handed to the system installer. */
    fun markPendingUpdate(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PENDING_AFTER_UPDATE, true)
            .apply()
    }

    /**
     * Checks if What's New is due to be presented automatically.
     * If due, records the version in SharedPreferences so it only fires once.
     * Fresh installations skip this dialog.
     */
    fun checkAndMarkDue(context: Context, currentVersion: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pending = prefs.getBoolean(KEY_PENDING_AFTER_UPDATE, false)
        val seen = prefs.getString(KEY_SEEN, null)

        if (!pending && seen == currentVersion) return false

        prefs.edit()
            .remove(KEY_PENDING_AFTER_UPDATE)
            .putString(KEY_SEEN, currentVersion)
            .apply()

        val isFreshInstall = seen == null && !pending && runCatching {
            val info = if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            info.firstInstallTime == info.lastUpdateTime
        }.getOrDefault(true)

        return !isFreshInstall
    }
}

/**
 * Provides structured release note catalog data, parsing GitHub release markdown
 * with seamless fallback to localized built-in highlights for v1.3.0.
 */
object WhatsNewCatalog {

    /** Returns structured local fallback notes for the current release. */
    fun getFallbackData(context: Context, version: String): WhatsNewData {
        val highlights = listOf(
            HighlightNote(
                icon = Icons.Rounded.Radio,
                title = context.getString(R.string.whats_new_v13_highlight_radio_title),
                description = context.getString(R.string.whats_new_v13_highlight_radio_desc),
            ),
            HighlightNote(
                icon = Icons.Rounded.GraphicEq,
                title = context.getString(R.string.whats_new_v13_highlight_automix_title),
                description = context.getString(R.string.whats_new_v13_highlight_automix_desc),
            ),
            HighlightNote(
                icon = Icons.Rounded.Widgets,
                title = context.getString(R.string.whats_new_v13_highlight_widgets_title),
                description = context.getString(R.string.whats_new_v13_highlight_widgets_desc),
            ),
            HighlightNote(
                icon = Icons.Rounded.BlurOn,
                title = context.getString(R.string.whats_new_v13_highlight_glass_title),
                description = context.getString(R.string.whats_new_v13_highlight_glass_desc),
            ),
            HighlightNote(
                icon = Icons.Rounded.Lyrics,
                title = context.getString(R.string.whats_new_v13_highlight_lyrics_title),
                description = context.getString(R.string.whats_new_v13_highlight_lyrics_desc),
            ),
        )

        val visualImprovements = listOf(
            context.getString(R.string.whats_new_v13_visual_1),
            context.getString(R.string.whats_new_v13_visual_2),
            context.getString(R.string.whats_new_v13_visual_3),
            context.getString(R.string.whats_new_v13_visual_4),
            context.getString(R.string.whats_new_v13_visual_5),
        )

        val fixes = listOf(
            context.getString(R.string.whats_new_v13_fix_1),
            context.getString(R.string.whats_new_v13_fix_2),
            context.getString(R.string.whats_new_v13_fix_3),
            context.getString(R.string.whats_new_v13_fix_4),
            context.getString(R.string.whats_new_v13_fix_5),
        )

        return WhatsNewData(
            version = version,
            highlights = highlights,
            visualImprovements = visualImprovements,
            fixes = fixes,
        )
    }

    /**
     * Parses a GitHub release markdown body, splitting sections into:
     * - Highlights / Features ("### Nuevas funciones" / "### Highlights")
     * - Visual improvements ("### Mejoras visuales" / "### Visual improvements")
     * - Fixes ("### Correcciones" / "### Fixes" / "### Bug fixes")
     *
     * Falls back to [getFallbackData] if parsing produces no meaningful entries.
     */
    fun parseReleaseNotes(markdown: String?, version: String, context: Context): WhatsNewData {
        if (markdown.isNullOrBlank()) {
            return getFallbackData(context, version)
        }

        val highlights = mutableListOf<HighlightNote>()
        val visualImprovements = mutableListOf<String>()
        val fixes = mutableListOf<String>()

        var currentSection = 0 // 1: highlights, 2: visual, 3: fixes

        markdown.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            if (line.startsWith('#')) {
                val header = line.trimStart('#').trim().lowercase()
                currentSection = when {
                    header.contains("nueva") || header.contains("destacad") ||
                        header.contains("highlight") || header.contains("feature") -> 1
                    header.contains("visual") || header.contains("diseño") ||
                        header.contains("design") || header.contains("interfaz") ||
                        header.contains("ui") || header.contains("estilo") -> 2
                    header.contains("correcci") || header.contains("fix") ||
                        header.contains("bug") || header.contains("error") -> 3
                    else -> currentSection
                }
                return@forEach
            }

            val isBullet = line.startsWith("- ") || line.startsWith("* ") ||
                line.startsWith("• ") || line.startsWith("+ ")
            if (isBullet) {
                val bulletContent = line.drop(2).trim()
                when (currentSection) {
                    1 -> {
                        val parsedNote = parseHighlightItem(bulletContent)
                        highlights.add(parsedNote)
                    }
                    2 -> {
                        visualImprovements.add(cleanMarkdown(bulletContent))
                    }
                    3 -> {
                        fixes.add(cleanMarkdown(bulletContent))
                    }
                    else -> {
                        // If no explicit section header was reached yet, treat as highlight
                        highlights.add(parseHighlightItem(bulletContent))
                    }
                }
            }
        }

        if (highlights.isEmpty() && visualImprovements.isEmpty() && fixes.isEmpty()) {
            return getFallbackData(context, version)
        }

        return WhatsNewData(
            version = version,
            highlights = highlights.ifEmpty { getFallbackData(context, version).highlights },
            visualImprovements = visualImprovements.ifEmpty { getFallbackData(context, version).visualImprovements },
            fixes = fixes.ifEmpty { getFallbackData(context, version).fixes },
        )
    }

    private fun parseHighlightItem(text: String): HighlightNote {
        val clean = cleanMarkdown(text)
        val colonIndex = clean.indexOf(':')
        val dashIndex = clean.indexOf(" - ")

        val (title, desc) = when {
            colonIndex > 0 && (dashIndex < 0 || colonIndex < dashIndex) -> {
                clean.substring(0, colonIndex).trim() to clean.substring(colonIndex + 1).trim()
            }
            dashIndex > 0 -> {
                clean.substring(0, dashIndex).trim() to clean.substring(dashIndex + 3).trim()
            }
            else -> clean to ""
        }

        val icon = pickIcon(title + " " + desc)
        return HighlightNote(icon = icon, title = title, description = desc)
    }

    private fun cleanMarkdown(text: String): String =
        text.replace("**", "").replace("__", "").trim()

    private fun pickIcon(text: String): ImageVector {
        val lower = text.lowercase()
        return when {
            lower.contains("radio") || lower.contains("emisora") || lower.contains("station") -> Icons.Rounded.Radio
            lower.contains("audio") || lower.contains("automix") || lower.contains("crossfade") ||
                lower.contains("ecualizador") || lower.contains("equalizer") || lower.contains("sound") -> Icons.Rounded.GraphicEq
            lower.contains("widget") -> Icons.Rounded.Widgets
            lower.contains("glass") || lower.contains("cristal") || lower.contains("diseño") ||
                lower.contains("animaci") || lower.contains("tema") -> Icons.Rounded.BlurOn
            lower.contains("letra") || lower.contains("lyric") -> Icons.Rounded.Lyrics
            lower.contains("notificaci") || lower.contains("status") -> Icons.Rounded.Notifications
            lower.contains("descarga") || lower.contains("download") || lower.contains("offline") -> Icons.Rounded.Download
            lower.contains("sync") || lower.contains("sincroniz") || lower.contains("drive") || lower.contains("backup") -> Icons.Rounded.Sync
            else -> Icons.Rounded.AutoAwesome
        }
    }
}

/**
 * Top hero card featuring SoundChord branding, bold version display,
 * and decorative vinyl/waveform audio canvas.
 */
@Composable
fun ReleaseHero(
    version: String,
    status: String,
    modifier: Modifier = Modifier,
    versionLabel: String = stringResource(R.string.whats_new_version_label),
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(colors.primaryContainer, colors.secondaryContainer),
                ),
            )
            .border(1.dp, colors.onPrimaryContainer.copy(alpha = 0.12f), shape)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // App branding row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = colors.onPrimaryContainer,
            )
            Text(
                text = "SoundChord",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onPrimaryContainer,
            )
        }

        // Version info column
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = versionLabel,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onPrimaryContainer.copy(alpha = 0.70f),
            )
            Text(
                text = version,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimaryContainer,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onPrimaryContainer,
            )
        }

        // Decorative audio waveform & vinyl concentric circles
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            val ink = colors.onPrimaryContainer
            val center = Offset(size.width - 28.dp.toPx(), size.height / 2)

            // Concentric vinyl rings
            listOf(25f, 19f, 13f).forEach { radius ->
                drawCircle(
                    color = ink.copy(alpha = 0.28f),
                    radius = radius.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            drawCircle(
                color = ink,
                radius = 4.dp.toPx(),
                center = center,
            )

            // Audio waveform bars
            val available = (size.width - 72.dp.toPx()).coerceAtLeast(0f)
            val heights = listOf(
                0.16f, 0.26f, 0.42f, 0.30f, 0.56f, 0.78f, 0.48f, 0.90f,
                0.64f, 0.40f, 0.70f, 0.50f, 0.32f, 0.56f, 0.22f, 0.38f,
                0.18f, 0.28f,
            )
            heights.forEachIndexed { i, h ->
                val x = 3.dp.toPx() + available * i / heights.size
                drawLine(
                    color = ink.copy(alpha = 0.30f + h * 0.50f),
                    start = Offset(x, size.height * (1 - h) / 2),
                    end = Offset(x, size.height * (1 + h) / 2),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

/** Individual feature highlight card with cascading spring entrance. */
@Composable
internal fun HighlightCard(
    note: HighlightNote,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val reduceMotion by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val shown = remember { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            shown.snapTo(1f)
        } else {
            delay(60L + index.coerceAtMost(5) * 45L)
            shown.animateTo(1f, spring(dampingRatio = 0.90f, stiffness = 300f))
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = shown.value.coerceIn(0f, 1f)
                translationY = (1f - shown.value) * 24.dp.toPx()
            }
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(18.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = note.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (note.description.isNotEmpty()) {
                Text(
                    text = note.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/**
 * Expandable category accordion card with interactive header, item count label
 * and expand/collapse arrow icon.
 */
@Composable
internal fun CategoryAccordion(
    title: String,
    countLabel: String,
    items: List<String>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    var expanded by rememberSaveable(title) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .animateContentSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) {
                    stringResource(R.string.collapse)
                } else {
                    stringResource(R.string.expand)
                },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Complete redesigned "What's New" modal dialog for SoundChord.
 *
 * @param version The app version string (e.g. "1.3.0").
 * @param fromSettings True when opened manually from Settings › About, displaying "Close".
 *                     False when presented automatically after an update, displaying "Start listening".
 * @param hazeState Optional HazeState for background glass blur.
 * @param onDismiss Invoked when the card is dismissed.
 */
@Composable
fun WhatsNewSheet(
    version: String,
    fromSettings: Boolean,
    hazeState: HazeState?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var changelog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        changelog = AppUpdateChecker.currentReleaseNotes()
    }

    val data = remember(changelog, version) {
        WhatsNewCatalog.parseReleaseNotes(changelog, version, context)
    }

    val titleText = stringResource(R.string.whats_new_title_version, version)
    val buttonText = stringResource(
        if (fromSettings) R.string.close else R.string.whats_new_start_listening,
    )

    FloatingCard(
        onDismiss = onDismiss,
        title = titleText,
        hazeState = hazeState,
        modifier = modifier,
        actions = {
            val card = LocalFloatingCard.current
            Button(
                onClick = { card?.close() ?: onDismiss() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = CircleShape,
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Hero top banner
            ReleaseHero(
                version = version,
                status = stringResource(R.string.whats_new_hero_status),
            )

            // Section 1: Highlights header
            Text(
                text = stringResource(R.string.whats_new_highlights_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = 4.dp, start = 4.dp)
                    .semantics { heading() },
            )

            // Highlights list
            data.highlights.forEachIndexed { index, note ->
                HighlightCard(note = note, index = index)
            }

            // Section 2: Visual improvements accordion
            if (data.visualImprovements.isNotEmpty()) {
                CategoryAccordion(
                    title = stringResource(R.string.whats_new_visual_improvements_title),
                    countLabel = stringResource(
                        R.string.whats_new_visual_improvements_count,
                        data.visualImprovements.size,
                    ),
                    items = data.visualImprovements,
                )
            }

            // Section 3: Fixes accordion
            if (data.fixes.isNotEmpty()) {
                CategoryAccordion(
                    title = stringResource(R.string.whats_new_fixes_title),
                    countLabel = stringResource(
                        R.string.whats_new_fixes_count,
                        data.fixes.size,
                    ),
                    items = data.fixes,
                )
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}
