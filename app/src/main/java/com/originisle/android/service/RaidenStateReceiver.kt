package com.originisle.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import com.originisle.android.cards.RaidenCard
import com.originisle.android.log.CastLog

/**
 * BroadcastReceiver for Route B: Direct Raiden live state stream.
 *
 * NOTE: Muse app itself posts standard Android notifications (Route A).
 * Route B is a secured companion bridge designed for local Termux background daemons,
 * ADB automation scripts (e.g. scripts/raiden-bridge.sh), Hermes Agent, or local tooling.
 *
 * Security: Protected by permission "com.originisle.android.permission.RAIDEN_STATE" and sender UID check
 * allowing only self, shell (UID 2000), root (UID 0), or the confirmed Muse package UID.
 */
class RaidenStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RAIDEN_STATE) return

        // Sender verification check
        val senderUid = sentFromUid
        val myUid = Process.myUid()
        val isShell = senderUid == 2000 || senderUid == 0 // ADB shell or root
        val isSelf = senderUid == myUid
        val isConfirmedMuse = runCatching {
            val confirmed = RaidenCard.confirmMusePackage(context)
            if (confirmed != null) {
                val pkgUid = context.packageManager.getPackageUid(confirmed, 0)
                senderUid == pkgUid
            } else false
        }.getOrDefault(false)

        if (!isSelf && !isShell && !isConfirmedMuse) {
            Log.w(TAG, "Rejected unauthorized Raiden broadcast from UID $senderUid")
            CastLog.add("Raiden", "Rejected", "unauthorized broadcast sender (UID $senderUid)", false)
            return
        }

        val state = intent.getStringExtra("state")
            ?: intent.getStringExtra("raiden_state")
            ?: "working"
        val task = intent.getStringExtra("task")
            ?: intent.getStringExtra("text")
            ?: intent.getStringExtra("details")
        val file = intent.getStringExtra("file")
        val sourcePkg = intent.getStringExtra("source_pkg")
            ?: intent.getStringExtra("muse_pkg")

        Log.d(TAG, "RaidenStateReceiver verified broadcast from UID $senderUid: state=$state task=$task file=$file")
        RaidenCard.postDirectState(context, state, task, file, sourcePkg)
    }

    companion object {
        const val TAG = "RaidenStateReceiver"
        const val ACTION_RAIDEN_STATE = "com.originisle.android.action.RAIDEN_STATE"
    }
}
