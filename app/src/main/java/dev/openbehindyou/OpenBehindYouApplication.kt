package dev.openbehindyou

import android.app.Application
import dev.openbehindyou.data.SettingsRepository

class OpenBehindYouApplication : Application() {
    val settingsRepository: SettingsRepository by lazy(LazyThreadSafetyMode.NONE) {
        SettingsRepository(this)
    }
}
