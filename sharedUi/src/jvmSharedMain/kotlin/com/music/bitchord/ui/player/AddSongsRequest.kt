package com.music.bitchord.ui.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shared signal between the queue row and the Android player overlay host. */
object AddSongsRequest {
    private val mutableOpen = MutableStateFlow(false)
    val open = mutableOpen.asStateFlow()
    fun show() { mutableOpen.value = true }
    fun hide() { mutableOpen.value = false }
}
