package com.music.bitchord.playback

import androidx.media3.common.Player

/** Mutes this device's player without changing playback state or position. */
internal class PartyLocalAudioRole {
    private var mutedByParty = false
    private var savedVolume: Float? = null

    fun apply(player: Player?, muteLocal: Boolean?) {
        if (player == null || muteLocal == null) return
        if (muteLocal && mutedByParty) {
            player.volume = 0f
            return
        }
        if (!muteLocal && !mutedByParty) return
        if (muteLocal) {
            savedVolume = player.volume
            mutedByParty = true
            player.volume = 0f
        } else {
            mutedByParty = false
            player.volume = savedVolume ?: player.volume
            savedVolume = null
        }
    }

    fun setUserMute(player: Player?, muted: Boolean) {
        if (player == null) return
        if (muted) {
            if (!mutedByParty) savedVolume = player.volume
            mutedByParty = true
            player.volume = 0f
        } else {
            mutedByParty = false
            player.volume = savedVolume ?: player.volume
            savedVolume = null
        }
    }

    fun restore(player: Player?) {
        if (!mutedByParty || player == null) return
        mutedByParty = false
        player.volume = savedVolume ?: player.volume
        savedVolume = null
    }
}
