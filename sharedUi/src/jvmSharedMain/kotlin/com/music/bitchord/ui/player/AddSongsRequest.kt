package com.music.bitchord.ui.player

import com.music.bitchord.data.model.UserPlaylist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shared signal between the queue row and the Android player overlay host. */
object AddSongsRequest {
    private val mutableOpen = MutableStateFlow(false)
    val open = mutableOpen.asStateFlow()
    private val mutablePlaylist = MutableStateFlow<UserPlaylist?>(null)
    val playlist = mutablePlaylist.asStateFlow()
    private val mutableLikedMusic = MutableStateFlow(false)
    val likedMusic = mutableLikedMusic.asStateFlow()

    fun show() {
        mutablePlaylist.value = null
        mutableLikedMusic.value = false
        mutableOpen.value = true
    }

    fun showForPlaylist(playlist: UserPlaylist) {
        mutablePlaylist.value = playlist
        mutableLikedMusic.value = false
        mutableOpen.value = true
    }

    fun showForLikedMusic() {
        mutablePlaylist.value = null
        mutableLikedMusic.value = true
        mutableOpen.value = true
    }

    fun hide() {
        mutableOpen.value = false
        mutablePlaylist.value = null
        mutableLikedMusic.value = false
    }
}
