package com.originisle.android.cards

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.util.PriorityArbitrator

/**
 * 2FA & Device Sign-in Approval Prompt Card for Origin Island Pro.
 * Intercepts verification pushes from Google, Microsoft Authenticator, Steam, Okta, Duo, etc.
 * Surfaces interactive "Approve" and "Deny" buttons directly on the island pill so the user
 * can authorize logins with one tap without interrupting their flow.
 */
object AuthPromptCard {

    val AUTH_APPS = setOf(
        "com.google.android.gms",
        "com.azure.authenticator",
        "com.valvesoftware.android.steam.community",
        "com.okta.android.auth",
        "com.duosecurity.duomobile",
        "com.authy.authy",
    )

    val AUTH_KEYWORDS = listOf(
        "approve sign-in", "approve login", "signing in?", "is it you?",
        "login request", "verify your identity", "approve request",
        "trying to sign in", "sign-in attempt", "tap yes", "deny",
    )

    fun isAuthPrompt(packageName: String, haystack: String): Boolean {
        val hasKeyword = AUTH_KEYWORDS.any { haystack.contains(it, ignoreCase = true) }
        val isAuthApp = packageName in AUTH_APPS
        return (isAuthApp && hasKeyword) || (hasKeyword && haystack.contains("sign", ignoreCase = true))
    }

    fun post(
        context: Context,
        id: Int,
        title: String,
        details: String,
        appLabel: String,
        appIcon: Icon?,
        clickResp: PendingIntent?,
        actions: List<Notification.Action> = emptyList(),
        sourcePkg: String? = null,
    ) {
        val usable = actions.filter { a ->
            a.actionIntent != null && !a.title.isNullOrBlank()
        }.take(2)

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            sourcePkg?.let { putExtra("source_pkg", it) }
            putExtra("is_ongoing", false)
            putExtra("oi_priority", PriorityArbitrator.PRIORITY_AUTH_PROMPT)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template",
                if (usable.isNotEmpty()) OriginIslandConstants.TEMPLATE_BUTTONS
                else OriginIslandConstants.TEMPLATE_BASE,
            )
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
            putExtra("title", title.ifBlank { "Sign-in request" })
            putExtra("text", details.ifBlank { "Approve or deny login" })
            putExtra("subtext", appLabel)
            putExtra("source_app", appLabel)

            // Short compact pill
            putExtra("oi_left_content", "Sign-in")
            appIcon?.let {
                putExtra("oi_left_icon", it)
                putExtra("oi_large_icon", it)
            }
            putExtra("oi_right_content", "Approve?")
            putExtra("status_chip_text", "Approve?")
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("source_content_intent", clickResp)
            if (usable.isNotEmpty()) putParcelableArrayListExtra("actions", ArrayList(usable))
        }
        context.startService(intent)
    }
}
