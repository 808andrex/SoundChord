package com.soundchord.features.playlistimport.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.bitchord.data.model.PlaylistPrivacy
import com.music.bitchord.data.model.Song
import com.soundchord.features.playlistimport.data.AppleMusicPlaylist
import com.soundchord.features.playlistimport.data.AppleMusicPlaylistClient
import com.soundchord.features.playlistimport.data.AppleMusicPlaylistRepository
import com.soundchord.features.playlistimport.data.AppleMusicTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AppleMusicImportStep {
    data object Input : AppleMusicImportStep
    data object LoadingMetadata : AppleMusicImportStep
    data class Preview(val playlist: AppleMusicPlaylist) : AppleMusicImportStep
    data class Matching(
        val playlist: AppleMusicPlaylist,
        val completed: Int,
        val total: Int,
    ) : AppleMusicImportStep
    data class Completed(
        val playlist: AppleMusicPlaylist,
        val matchedSongs: List<Song>,
        val unmatchedTracks: List<AppleMusicTrack>,
    ) : AppleMusicImportStep
}

data class AppleMusicImportUiState(
    val link: String = "",
    val isValidUrl: Boolean = false,
    val step: AppleMusicImportStep = AppleMusicImportStep.Input,
    val privacy: PlaylistPrivacy = PlaylistPrivacy.PRIVATE,
    val selectedLanguage: String = "No Preference",
    val customLanguageInput: String = "",
    val errorMessage: String? = null,
)

class AppleMusicImportViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AppleMusicImportUiState())
    val uiState: StateFlow<AppleMusicImportUiState> = _uiState.asStateFlow()

    fun onLinkChanged(newLink: String) {
        val isValid = AppleMusicPlaylistClient.parsePlaylistUrl(newLink) != null
        _uiState.update {
            it.copy(
                link = newLink,
                isValidUrl = isValid,
                errorMessage = null,
            )
        }
    }

    fun onPrivacyChanged(privacy: PlaylistPrivacy) {
        _uiState.update { it.copy(privacy = privacy) }
    }

    fun onLanguageChanged(language: String) {
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun onCustomLanguageChanged(custom: String) {
        _uiState.update { it.copy(customLanguageInput = custom) }
    }

    fun loadPreview() {
        val link = _uiState.value.link.trim()
        val parsed = AppleMusicPlaylistClient.parsePlaylistUrl(link)
        if (parsed == null) {
            _uiState.update { it.copy(errorMessage = "invalid_url") }
            return
        }

        _uiState.update { it.copy(step = AppleMusicImportStep.LoadingMetadata, errorMessage = null) }
        viewModelScope.launch {
            try {
                val playlist = AppleMusicPlaylistClient.fetchPlaylist(parsed.first, parsed.second)
                _uiState.update { it.copy(step = AppleMusicImportStep.Preview(playlist)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        step = AppleMusicImportStep.Input,
                        errorMessage = e.message ?: "fetch_failed",
                    )
                }
            }
        }
    }

    fun startMatching(
        onSuccess: (playlist: AppleMusicPlaylist, privacy: PlaylistPrivacy, songs: List<Song>) -> Unit,
    ) {
        val currentStep = _uiState.value.step
        val playlist = when (currentStep) {
            is AppleMusicImportStep.Preview -> currentStep.playlist
            else -> return
        }

        val effectiveLanguage = if (_uiState.value.selectedLanguage == "Manual / Other") {
            _uiState.value.customLanguageInput.trim().ifBlank { null }
        } else {
            _uiState.value.selectedLanguage
        }

        _uiState.update {
            it.copy(
                step = AppleMusicImportStep.Matching(playlist, 0, playlist.tracks.size),
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            try {
                val (matchedSongs, unmatched) = AppleMusicPlaylistRepository.resolveAndMatchTracks(
                    tracks = playlist.tracks,
                    language = effectiveLanguage,
                    onProgress = { done, total ->
                        _uiState.update {
                            it.copy(step = AppleMusicImportStep.Matching(playlist, done, total))
                        }
                    },
                )

                if (matchedSongs.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            step = AppleMusicImportStep.Preview(playlist),
                            errorMessage = "none_found",
                        )
                    }
                    return@launch
                }

                if (unmatched.isEmpty()) {
                    onSuccess(playlist, _uiState.value.privacy, matchedSongs)
                } else {
                    _uiState.update {
                        it.copy(
                            step = AppleMusicImportStep.Completed(playlist, matchedSongs, unmatched),
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        step = AppleMusicImportStep.Preview(playlist),
                        errorMessage = e.message ?: "match_failed",
                    )
                }
            }
        }
    }

    fun confirmSave(
        onSuccess: (playlist: AppleMusicPlaylist, privacy: PlaylistPrivacy, songs: List<Song>) -> Unit,
    ) {
        val currentStep = _uiState.value.step
        if (currentStep is AppleMusicImportStep.Completed) {
            onSuccess(currentStep.playlist, _uiState.value.privacy, currentStep.matchedSongs)
        }
    }

    fun resetToInput() {
        _uiState.update {
            it.copy(
                step = AppleMusicImportStep.Input,
                errorMessage = null,
            )
        }
    }

    fun reset() {
        _uiState.value = AppleMusicImportUiState()
    }
}
