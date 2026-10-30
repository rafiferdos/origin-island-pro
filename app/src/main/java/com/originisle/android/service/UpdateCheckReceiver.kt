package com.originisle.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.originisle.android.ui.UpdateChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by AlarmManager or system boot to perform daily automated update checks.
 */
class UpdateCheckReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DAILY_UPDATE_CHECK = "com.originisle.android.action.DAILY_UPDATE_CHECK"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                UpdateChecker.checkDailyIfNeeded(context, force = true)
            } finally {
                pending.finish()
            }
        }
    }
}
