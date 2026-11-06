package com.soundchord.features.listentogether

/** Small feature-facing adapter; the app supplies the party/player operation. */
class RemoteAudioModeController(private val applyMode: (Boolean) -> Unit) {
    fun setEnabled(enabled: Boolean) = applyMode(enabled)
}
