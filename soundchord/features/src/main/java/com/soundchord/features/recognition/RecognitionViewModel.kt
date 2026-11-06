package com.soundchord.features.recognition

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soundchord.features.recognition.data.MusicRecognitionService
import com.soundchord.features.recognition.model.RecognitionStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecognitionViewModel(application: Application) : AndroidViewModel(application) {
    val status: StateFlow<RecognitionStatus> = MusicRecognitionService.status
    private var job: Job? = null
    fun start() {
        if (job?.isActive == true) return
        job = viewModelScope.launch { MusicRecognitionService.recognize(getApplication()) }
    }
    fun cancel() { job?.cancel(); job = null; MusicRecognitionService.reset() }
    override fun onCleared() { cancel(); super.onCleared() }
}
