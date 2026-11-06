package com.originisle.android.cards

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Pins the canonical action filter.
 *
 * `GenericCard.usableActions` is now the single definition of "which notification actions may be
 * rendered as island buttons", and `DownloadCard` was switched onto it to fix a real bug: its
 * local filter kept `RemoteInput` ("Reply") actions, which the island renders as a button that
 * fires with an empty input — a button that does nothing when tapped.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GenericCardUsableActionsTest {

    private val pi: PendingIntent = PendingIntent.getActivity(
        RuntimeEnvironment.getApplication(),
        0,
        Intent(),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun action(
        title: String,
        intent: PendingIntent? = pi,
        remoteInputs: Array<RemoteInput>? = null,
        semanticAction: Int = 0,
        contextual: Boolean = false,
    ): Notification.Action {
        val icon = Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
        val builder = Notification.Action.Builder(icon, title, intent)
        remoteInputs?.forEach { builder.addRemoteInput(it) }
        if (semanticAction != 0) builder.setSemanticAction(semanticAction)
        if (contextual) builder.setContextual(true)
        return builder.build()
    }

    private fun remoteInput(): RemoteInput =
        RemoteInput.Builder("reply_key").setLabel("Reply").build()

    private fun notification(vararg actions: Notification.Action): Notification {
        val n = Notification()
        n.actions = actions.toList().toTypedArray()
        return n
    }

    @Test
    fun `an action with an intent and a title is usable`() {
        val n = notification(action("Open"))
        val usable = GenericCard.usableActions(n)
        assertEquals(listOf("Open"), usable.map { it.title.toString() })
    }

    @Test
    fun `an action that needs typed input is dropped`() {
        // This is the DownloadCard bug: such a button renders but fires an empty RemoteInput.
        val n = notification(action("Reply", remoteInputs = arrayOf(remoteInput())))
        assertTrue(GenericCard.usableActions(n).isEmpty())
    }

    @Test
    fun `an action with no pending intent is dropped`() {
        val n = notification(action("Nothing", intent = null))
        assertTrue(GenericCard.usableActions(n).isEmpty())
    }

    @Test
    fun `an action with a blank title is dropped`() {
        val n = notification(action("   "))
        assertTrue(GenericCard.usableActions(n).isEmpty())
    }

    @Test
    fun `a reply semantic action is dropped`() {
        val n = notification(
            action("Quick reply", semanticAction = Notification.Action.SEMANTIC_ACTION_REPLY),
        )
        assertTrue(GenericCard.usableActions(n).isEmpty())
    }

    @Test
    fun `a contextual smart-reply action is dropped`() {
        val n = notification(action("Smart reply", contextual = true))
        assertTrue(GenericCard.usableActions(n).isEmpty())
    }

    @Test
    fun `at most three actions are kept`() {
        val n = notification(action("One"), action("Two"), action("Three"), action("Four"))
        assertEquals(3, GenericCard.usableActions(n).size)
    }

    @Test
    fun `a notification with no actions yields nothing`() {
        assertTrue(GenericCard.usableActions(Notification()).isEmpty())
    }

    @Test
    fun `the returned list is a copy, not the notification's own`() {
        val n = notification(action("Open"), action("Reply", remoteInputs = arrayOf(remoteInput())))
        val usable = GenericCard.usableActions(n)
        assertEquals(2, n.actions!!.size)
        assertNotSame(n.actions?.toList(), usable)
    }
}
