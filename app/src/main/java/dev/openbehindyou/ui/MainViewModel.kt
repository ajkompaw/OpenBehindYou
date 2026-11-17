package dev.openbehindyou.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.openbehindyou.OpenBehindYouApplication
import dev.openbehindyou.detector.Sensitivity
import dev.openbehindyou.service.GuardController
import dev.openbehindyou.service.GuardRuntime
import dev.openbehindyou.service.GuardSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository =
        (application as OpenBehindYouApplication).settingsRepository

    val guard: StateFlow<GuardSnapshot> = GuardRuntime.snapshot

    val sensitivity: StateFlow<Sensitivity> = settingsRepository.sensitivity.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = Sensitivity.MEDIUM,
    )

    fun setSensitivity(sensitivity: Sensitivity) {
        viewModelScope.launch {
            settingsRepository.setSensitivity(sensitivity)
        }
    }

    fun startGuard() {
        GuardController.start(getApplication())
    }

    fun stopGuard() {
        GuardController.stop(getApplication())
    }
}
