package com.music.bitchord.ui.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Controller to show the Apple Music playlist import dialog from anywhere
 * in the UI (e.g. [LibraryScreen]) without touching MainActivity.
 */
object AppleMusicImportController {
    var showDialog by mutableStateOf(false)
}
