package com.originisle.android

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.originisle.android.island.OriginIslandBuilder
import com.originisle.android.service.NotificationCastListener
import com.originisle.android.sports.SportsFeed
import com.originisle.android.ui.UpdateChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OriginIsleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        SportsFeed.init(this)
        OriginIslandBuilder.grantScenes(this)
        // Every process start, not just the UI's: a service restart is the likeliest way back in
        // after the kill that could have left the component half-toggled.
        NotificationCastListener.ensureEnabled(this)

        // Schedule and trigger daily automated background update checks
        UpdateChecker.scheduleDailyCheck(this)
        CoroutineScope(Dispatchers.IO).launch {
            UpdateChecker.checkDailyIfNeeded(this@OriginIsleApp)
        }
    }
}
