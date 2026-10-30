package com.originisle.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.originisle.android.cards.RaidenCard

/**
 * BroadcastReceiver for Route B: Direct Raiden live state stream.
 * Enables zero-notification direct updates from Muse, CLI tools, ADB, Termux, or Hermes Agent.
 *
 * Example broadcast:
 * adb shell am broadcast -a com.originisle.android.action.RAIDEN_STATE --es state "coding" --es task "Coding in Origin Island Pro"
 */
class RaidenStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RAIDEN_STATE) return

        val state = intent.getStringExtra("state")
            ?: intent.getStringExtra("raiden_state")
            ?: "working"
        val task = intent.getStringExtra("task")
            ?: intent.getStringExtra("text")
            ?: intent.getStringExtra("details")
        val file = intent.getStringExtra("file")
        val sourcePkg = intent.getStringExtra("source_pkg")
            ?: intent.getStringExtra("muse_pkg")

        Log.d(TAG, "RaidenStateReceiver broadcast received: state=$state task=$task file=$file")
        RaidenCard.postDirectState(context, state, task, file, sourcePkg)
    }

    companion object {
        const val TAG = "RaidenStateReceiver"
        const val ACTION_RAIDEN_STATE = "com.originisle.android.action.RAIDEN_STATE"
    }
}
