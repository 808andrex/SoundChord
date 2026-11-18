package com.music.bitchord

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * The home-screen icon choices. Each is an `<activity-alias>` in the manifest
 * carrying the launcher intent filter; exactly one is enabled at a time.
 *
 * The aliases keep stable names for the choices that remain available. Unknown
 * or removed legacy selections resolve to [Default] in [current].
 */
enum class AppIcon(val alias: String, val iconRes: Int, val labelRes: Int, val previewRes: Int? = null) {
    Default("LauncherDefault", R.mipmap.ic_launcher, R.string.app_icon_default),
    Monochrome("LauncherMonochrome", R.mipmap.ic_monochrome, R.string.app_icon_monochrome),
    ClassicRed("LauncherClassicRed", R.mipmap.ic_classic_red, R.string.app_icon_classic_red),
    SpotifyGreen("LauncherSpotifyGreen", R.mipmap.ic_spotify_green, R.string.app_icon_spotify_green);

    private fun component(context: Context) =
        ComponentName(context.packageName, "com.music.bitchord.$alias")

    companion object {
        fun current(context: Context): AppIcon {
            val pm = context.packageManager
            val selected = entries.firstOrNull { icon ->
                val enabledState = runCatching {
                    pm.getComponentEnabledSetting(icon.component(context))
                }.getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)

                enabledState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                    (icon == Default && enabledState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
            }
            if (selected != null) return selected

            // A removed legacy alias may have been the only enabled one and may
            // have left Default explicitly disabled in PackageManager state.
            apply(context, Default)
            return Default
        }

        /** Enables [icon] before disabling the rest so there is never a moment with no launcher entry. */
        fun apply(context: Context, icon: AppIcon) {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                icon.component(context),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
            entries.filter { it != icon }.forEach {
                pm.setComponentEnabledSetting(
                    it.component(context),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }
}
