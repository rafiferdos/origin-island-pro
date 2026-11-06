package com.originisle.android.island

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Pins the reverse-engineered SuperX wire format.
 *
 * The protocol in [OriginIslandConstants] was recovered from a live device, not from
 * documentation. Nothing else in the test suite would notice if a refactor dropped a key, wrote a
 * value under the wrong name, or changed a fallback — the app would simply render a blank or
 * malformed card on the island. These tests assert the exact key set and the exact value written
 * for every template and every right-island variant, so the protocol stops being folklore.
 *
 * Runs on Robolectric because the payload is an [android.os.Bundle] of [Icon]s, which the plain
 * JVM android.jar stubs out.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BundleCodecWireFormatTest {
    private val progressBarColor = 0xFFAAAAAA.toInt()
    private val progressBarBgColor = 0xFFBBBBBB.toInt()

    private val defaults = ColorDefaults(progressBarColor, progressBarBgColor)

    private fun icon(): Icon =
        Icon.createWithBitmap(Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888))

    private fun build(
        template: Int,
        rightTemplate: Int = 0,
        title: String = "Title",
        content: String = "Content",
        configure: SuperXRequest.() -> SuperXRequest = { this },
    ): Bundle {
        val base = SuperXRequest(
            template = template,
            rightTemplate = rightTemplate,
            title = title,
            content = content,
            scene = "NAVIGATION",
            defaultIcon = icon(),
            leftIcon = icon(),
            rightIcon = icon(),
        )
        return BundleCodec.buildBundlePure(base.configure(), defaults)
    }

    private fun infosOf(bundle: Bundle): Bundle = bundle.getBundle(OriginIslandConstants.BUNDLE_KEY_INFOS)!!

    private fun islandOf(bundle: Bundle): Bundle =
        bundle.getBundle(OriginIslandConstants.BUNDLE_KEY_ISLAND_INFOS)!!

    private fun baseOf(bundle: Bundle): Bundle =
        bundle.getBundle(OriginIslandConstants.BUNDLE_KEY_BASE_INFOS)!!

    private fun shortOf(bundle: Bundle): Bundle =
        bundle.getBundle(OriginIslandConstants.BUNDLE_KEY_SHORT_INFOS)!!

    private fun capsuleOf(bundle: Bundle): Bundle =
        bundle.getBundle(OriginIslandConstants.BUNDLE_KEY_CAPSULE)!!

    private fun leftOf(bundle: Bundle): Bundle =
        islandOf(bundle).getBundle(OriginIslandConstants.BUNDLE_KEY_ISLAND_LEFT_INFO)!!

    private fun rightOf(bundle: Bundle): Bundle =
        islandOf(bundle).getBundle(OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_INFO)!!

    // --- envelope --------------------------------------------------------------------------

    @Test
    fun `envelope carries operation, template, scene and change record`() {
        val bundle = build(1)
        assertEquals(0, bundle.getInt(OriginIslandConstants.BUNDLE_KEY_OPERATION))
        assertEquals(1, bundle.getInt(OriginIslandConstants.BUNDLE_KEY_TEMPLATE))
        assertEquals("NAVIGATION", bundle.getString(OriginIslandConstants.BUNDLE_KEY_SCENE))
        assertEquals(0, bundle.getInt(OriginIslandConstants.BUNDLE_KEY_CHANGE_RECORD))
        assertTrue(bundle.getBoolean(OriginIslandConstants.BUNDLE_KEY_SHOW_NOTIFY))
        assertTrue(bundle.getBoolean(OriginIslandConstants.BUNDLE_KEY_DISMISS_WHEN_KILL))
        assertFalse(bundle.getBoolean(OriginIslandConstants.BUNDLE_KEY_SOUND))
    }

    @Test
    fun `all four payload sections are always present`() {
        val bundle = build(1)
        assertNotNull(bundle.getBundle(OriginIslandConstants.BUNDLE_KEY_BASE_INFOS))
        assertNotNull(bundle.getBundle(OriginIslandConstants.BUNDLE_KEY_INFOS))
        assertNotNull(bundle.getBundle(OriginIslandConstants.BUNDLE_KEY_SHORT_INFOS))
        assertNotNull(bundle.getBundle(OriginIslandConstants.BUNDLE_KEY_ISLAND_INFOS))
        assertNotNull(bundle.getBundle(OriginIslandConstants.BUNDLE_KEY_CAPSULE))
    }

    @Test
    fun `shortInfos mirrors title and content`() {
        val bundle = build(1)
        assertEquals("Content", shortOf(bundle).getString(OriginIslandConstants.BUNDLE_KEY_SHORT_INFO_CORE_INFO_SHORT))
        assertEquals("Title", shortOf(bundle).getString(OriginIslandConstants.BUNDLE_KEY_SHORT_INFO_DESCRIBE_SHORT))
    }

    @Test
    fun `end bundle ends the card`() {
        val bundle = BundleCodec.buildEndBundle("TAXI")
        assertEquals(2, bundle.getInt(OriginIslandConstants.BUNDLE_KEY_OPERATION))
        assertFalse(bundle.getBoolean(OriginIslandConstants.BUNDLE_KEY_SHOW_NOTIFY))
        assertEquals("TAXI", bundle.getString(OriginIslandConstants.BUNDLE_KEY_SCENE))
        assertEquals(Int.MAX_VALUE, bundle.getInt(OriginIslandConstants.BUNDLE_KEY_CHANGE_RECORD))
    }

    @Test
    fun `island left template is two-line only for exactly two lines`() {
        assertEquals(
            3,
            islandOf(build(1) { copy(leftDoubleLine = listOf("a", "b")) })
                .getInt(OriginIslandConstants.BUNDLE_KEY_ISLAND_LEFT_TEMPLATE),
        )
        assertEquals(
            1,
            islandOf(build(1) { copy(leftDoubleLine = listOf("a")) })
                .getInt(OriginIslandConstants.BUNDLE_KEY_ISLAND_LEFT_TEMPLATE),
        )
        assertEquals(
            1,
            islandOf(build(1)).getInt(OriginIslandConstants.BUNDLE_KEY_ISLAND_LEFT_TEMPLATE),
        )
    }

    @Test
    fun `island always opts out of the companion bar and is in state 1`() {
        val island = islandOf(build(1))
        assertFalse(island.getBoolean(OriginIslandConstants.BUNDLE_KEY_ISLAND_SHOW_BAR_WHEN_CARD))
        assertEquals(1, island.getInt(OriginIslandConstants.BUNDLE_KEY_ISLAND_STATE))
    }

    @Test
    fun `capsule carries the right content and icon`() {
        val capsule = capsuleOf(
            build(1, rightTemplate = 4, configure = { copy(rightContent = "C") }),
        )
        assertEquals(1, capsule.getInt(OriginIslandConstants.BUNDLE_KEY_CAPSULE_STATE))
        assertEquals("C", capsule.getString(OriginIslandConstants.BUNDLE_KEY_CAPSULE_CONTENT))
        assertNotNull(capsule.getParcelable(OriginIslandConstants.BUNDLE_KEY_CAPSULE_ICON))
    }

    // --- template 1 -----------------------------------------------------------------------

    @Test
    fun `template 1 writes describe, coreInfo and image`() {
        val bundle = build(1, configure = {
            copy(extra1 = "Describe", extra2 = "Core", largeIcon = icon())
        })
        val infos = infosOf(bundle)
        assertEquals("Describe", infos.getString(OriginIslandConstants.BUNDLE_KEY_INFO_DESCRIBE))
        assertEquals("Core", infos.getString(OriginIslandConstants.BUNDLE_KEY_INFO_CORE_INFO))
        assertNotNull(infos.getParcelable<Icon>(OriginIslandConstants.BUNDLE_KEY_INFO_IMAGE))
    }

    // --- template 2 -----------------------------------------------------------------------

    @Test
    fun `template 2 writes a clean two-node progress bar`() {
        val bundle = build(2, configure = { copy(progress = 42) })
        val infos = infosOf(bundle)
        val nodes = infos.getParcelableArrayList<Icon>(OriginIslandConstants.BUNDLE_KEY_INFO_NODE_ICON)
        assertNotNull(nodes)
        assertEquals(2, nodes!!.size)
        assertEquals(42, infos.getInt(OriginIslandConstants.BUNDLE_KEY_INFO_PROGRESS))
        assertEquals(progressBarColor, infos.getInt(OriginIslandConstants.BUNDLE_KEY_INFO_PROGRESS_COLOR))
        assertEquals(progressBarBgColor, infos.getInt(OriginIslandConstants.BUNDLE_KEY_INFO_BG_COLOR))
    }

    @Test
    fun `template 2 progress is clamped to 0-100`() {
        val over = infosOf(build(2, configure = { copy(progress = 900) }))
        val under = infosOf(build(2, configure = { copy(progress = -5) }))
        assertEquals(100, over.getInt(OriginIslandConstants.BUNDLE_KEY_INFO_PROGRESS))
        assertEquals(0, under.getInt(OriginIslandConstants.BUNDLE_KEY_INFO_PROGRESS))
    }

    @Test
    fun `template 2 uses the supplied milestone icons and indicator`() {
        val start = icon()
        val tracker = icon()
        val bundle = build(2, configure = {
            copy(progressNodeIcons = listOf(start), progressIndicatorIcon = tracker)
        })
        val infos = infosOf(bundle)
        val nodes = infos.getParcelableArrayList<Icon>(OriginIslandConstants.BUNDLE_KEY_INFO_NODE_ICON)!!
        assertEquals(1, nodes.size)
        assertSame(start, nodes[0])
        assertSame(tracker, infos.getParcelable(OriginIslandConstants.BUNDLE_KEY_INFO_INDICATOR_ICON))
        assertEquals(2, infos.getInt(OriginIslandConstants.BUNDLE_KEY_INFO_INDICATOR_LOC))
    }

    @Test
    fun `template 2 injects up to three action buttons`() {
        val pi = pendingIntent()
        val bundle = build(2, configure = {
            copy(actions = listOf(CardAction("Pause", null, pi), CardAction("Cancel", null, pi)))
        })
        val infos = infosOf(bundle)
        assertEquals(1, infos.getInt(OriginIslandConstants.BUNDLE_KEY_INFO_BTN_TYPE))
        assertEquals(
            listOf("Pause", "Cancel"),
            infos.getStringArrayList(OriginIslandConstants.BUNDLE_KEY_INFO_BTN_TEXT_LIST),
        )
        assertEquals(
            2,
            infos.getParcelableArrayList<PendingIntent>(OriginIslandConstants.BUNDLE_KEY_INFO_BTN_CLICK_RESP_LIST)!!.size,
        )
    }

    // --- template 3 -----------------------------------------------------------------------

    @Test
    fun `template 3 writes the three-column scoreboard`() {
        val bundle = build(3, configure = {
            copy(extra1 = "Home", extra2 = "1", extra3 = "Away", extra4 = "2", content = "Live")
        })
        val infos = infosOf(bundle)
        assertEquals("Home", infos.getString(OriginIslandConstants.BUNDLE_KEY_INFO_LEFT_MAIN))
        assertEquals("1", infos.getString(OriginIslandConstants.BUNDLE_KEY_INFO_LEFT_SUB))
        assertEquals(1, infos.getInt(OriginIslandConstants.BUNDLE_KEY_INFO_MID_MAIN_TYPE))
        assertEquals(2, infos.getInt(OriginIslandConstants.BUNDLE_KEY_INFO_MID_SUB_TYPE))
        assertEquals("Live", infos.getString(OriginIslandConstants.BUNDLE_KEY_INFO_MID_SUB_MSG))
        assertEquals("Away", infos.getString(OriginIslandConstants.BUNDLE_KEY_INFO_RIGHT_MAIN))
        assertEquals("2", infos.getString(OriginIslandConstants.BUNDLE_KEY_INFO_RIGHT_SUB))
    }

    // --- template 4 -----------------------------------------------------------------------

    @Test
    fun `template 4 mirrors title and content into infos`() {
        // Template 4 previously wrote nothing into `infos` at all, which is why OriginOS generated
        // a duplicate "compat" notification for these cards.
        val bundle = build(4, title = "T4", content = "C4")
        val infos = infosOf(bundle)
        assertEquals("T4", infos.getString(OriginIslandConstants.BUNDLE_KEY_INFO_DESCRIBE))
        assertEquals("C4", infos.getString(OriginIslandConstants.BUNDLE_KEY_INFO_CORE_INFO))
        assertNotNull(infos.getParcelable<Icon>(OriginIslandConstants.BUNDLE_KEY_INFO_IMAGE))
    }

    @Test
    fun `template 4 base falls back to extra1 when subtext is blank`() {
        val bundle = build(4, configure = { copy(extra1 = "Fallback") })
        assertEquals(
            "Fallback",
            baseOf(bundle).getString(OriginIslandConstants.BUNDLE_KEY_BASE_SUB_TEXT),
        )
    }

    // --- template 5 -----------------------------------------------------------------------

    @Test
    fun `template 5 writes the navigation message`() {
        val bundle = build(5, configure = { copy(navMsg = "Turn left") })
        val infos = infosOf(bundle)
        assertNotNull(infos.getParcelable<Icon>(OriginIslandConstants.BUNDLE_KEY_INFO_NAV_ICON))
        assertEquals("Turn left", infos.getString(OriginIslandConstants.BUNDLE_KEY_INFO_NAV_MSG))
    }

    @Test
    fun `template 5 joins title and content when no nav message is given`() {
        val bundle = build(5, title = "Road", content = "Fast")
        assertEquals(
            "Road • Fast",
            infosOf(bundle).getString(OriginIslandConstants.BUNDLE_KEY_INFO_NAV_MSG),
        )
    }

    // --- template 8 -----------------------------------------------------------------------

    @Test
    fun `template 8 writes describe, coreInfo, image and buttons`() {
        val pi = pendingIntent()
        val bundle = build(8, configure = {
            copy(actions = listOf(CardAction("Open", null, pi)))
        })
        val infos = infosOf(bundle)
        assertEquals("Title", infos.getString(OriginIslandConstants.BUNDLE_KEY_INFO_DESCRIBE))
        assertEquals("Content", infos.getString(OriginIslandConstants.BUNDLE_KEY_INFO_CORE_INFO))
        assertNotNull(infos.getParcelable<Icon>(OriginIslandConstants.BUNDLE_KEY_INFO_IMAGE))
        assertEquals(
            listOf("Open"),
            infos.getStringArrayList(OriginIslandConstants.BUNDLE_KEY_INFO_BTN_TEXT_LIST),
        )
    }

    // --- template 9 -----------------------------------------------------------------------

    @Test
    fun `template 9 writes the direction card`() {
        val bundle = build(9, configure = { copy(navAssistText = "Keep left") })
        val infos = infosOf(bundle)
        assertNotNull(infos.getParcelable<Icon>(OriginIslandConstants.BUNDLE_KEY_INFO_DIR_ICON))
        assertEquals("Title", infos.getString(OriginIslandConstants.BUNDLE_KEY_INFO_MAIN_HIGHLIGHT_TEXT))
        assertEquals("Content", infos.getString(OriginIslandConstants.BUNDLE_KEY_INFO_MAIN_NORMAL_TEXT))
        assertEquals("Content", infos.getCharSequence(OriginIslandConstants.BUNDLE_KEY_INFO_DIR_SUB_TEXT)?.toString())
        assertEquals(1, infos.getInt(OriginIslandConstants.BUNDLE_KEY_INFO_DIR_ASSIST_TYPE))
        assertEquals("Keep left", infos.getString(OriginIslandConstants.BUNDLE_KEY_INFO_DIR_ASSIST_TEXT))
    }

    @Test
    fun `template 9 omits the assist block when there is no assist text`() {
        val infos = infosOf(build(9))
        assertFalse(infos.containsKey(OriginIslandConstants.BUNDLE_KEY_INFO_DIR_ASSIST_TYPE))
    }

    // --- right island templates ------------------------------------------------------------

    @Test
    fun `right template 1 writes the wave colours and state`() {
        val bundle = build(1, rightTemplate = 1, configure = {
            copy(fgColor = 0xFF123456.toInt(), waveState = 3)
        })
        val right = rightOf(bundle)
        assertEquals(
            listOf("#123456"),
            right.getStringArrayList(OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_WAVE_COLOR),
        )
        assertEquals(3, right.getInt(OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_WAVE_STATE))
    }

    @Test
    fun `right template 2 writes the progress ring`() {
        val right = rightOf(build(1, rightTemplate = 2, configure = { copy(progress = 77, progressState = 2) }))
        assertEquals(77, right.getInt(OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_PROGRESS))
        assertEquals(progressBarColor, right.getInt(OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_PROGRESS_COLOR))
        assertEquals(2, right.getInt(OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_PROGRESS_STATE))
    }

    @Test
    fun `right template 3 writes the loading colour`() {
        val right = rightOf(build(1, rightTemplate = 3, configure = { copy(fgColor = 0xFF00FF00.toInt()) }))
        assertEquals(0xFF00FF00.toInt(), right.getInt(OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_LOADING_COLOR))
    }

    @Test
    fun `right template 4 writes icon and content`() {
        val pi = pendingIntent()
        val bundle = build(1, rightTemplate = 4, configure = {
            copy(rightContent = "42", clickResp = pi)
        })
        val right = rightOf(bundle)
        assertNotNull(right.getParcelable<Icon>(OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_ICON))
        assertEquals("42", right.getCharSequence(OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_CONTENT)?.toString())
        assertNotNull(right.getParcelable(OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_CLICK_RESP))
    }

    @Test
    fun `right template 6 writes the capsule`() {
        val pi = pendingIntent()
        val bundle = build(1, rightTemplate = 6, configure = {
            copy(rightContent = "Capsule", bgColor = 0xFF112233.toInt(), clickResp = pi)
        })
        val right = rightOf(bundle)
        assertEquals(
            "Capsule",
            right.getCharSequence(OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_CAPSULE_CONTENT)?.toString(),
        )
        assertEquals(0xFF112233.toInt(), right.getInt(OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_BG_COLOR))
        assertNotNull(right.getParcelable(OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_CLICK_RESP))
    }

    @Test
    fun `right icon is omitted when showRightIcon is false`() {
        val bundle = build(1, rightTemplate = 4, configure = { copy(showRightIcon = false) })
        assertFalse(rightOf(bundle).containsKey(OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_ICON))
    }

    @Test
    fun `left icon is omitted when showLeftIcon is false`() {
        val bundle = build(1, configure = { copy(showLeftIcon = false) })
        assertFalse(leftOf(bundle).containsKey(OriginIslandConstants.BUNDLE_KEY_ISLAND_LEFT_ICON))
    }

    // --- flags -----------------------------------------------------------------------------

    @Test
    fun `optional flags are only written when set`() {
        val bundle = build(1)
        assertFalse(bundle.containsKey(OriginIslandConstants.BUNDLE_KEY_KEEP_DURATION))
        assertFalse(bundle.containsKey(OriginIslandConstants.BUNDLE_KEY_DISPLAYS))
        assertFalse(bundle.containsKey(OriginIslandConstants.BUNDLE_KEY_NEW_NODE))
        assertFalse(bundle.containsKey(OriginIslandConstants.BUNDLE_KEY_CARD_BG_COLOR))
        assertFalse(bundle.containsKey(OriginIslandConstants.BUNDLE_KEY_ISLAND_SHOW_TIME))
        assertFalse(bundle.containsKey(OriginIslandConstants.BUNDLE_KEY_ISLAND_FORCE_SHOW))
        assertFalse(bundle.containsKey(OriginIslandConstants.BUNDLE_KEY_ISLAND_PRIORITY))
        assertTrue(bundle.containsKey(OriginIslandConstants.BUNDLE_KEY_DISABLE_INVERT_COLOR))
        assertTrue(bundle.containsKey(OriginIslandConstants.BUNDLE_KEY_KEEP_SCREEN_ON).not())
    }

    @Test
    fun `keepScreenOn is written when requested`() {
        val bundle = build(1, configure = { copy(keepScreenOn = true) })
        assertTrue(bundle.getBoolean(OriginIslandConstants.BUNDLE_KEY_KEEP_SCREEN_ON))
    }

    @Test
    fun `light effect is written when a light colour is supplied`() {
        val bundle = build(1, configure = { copy(lightColor = 0xFF00AAFF.toInt(), lightMode = 2) })
        assertTrue(bundle.getBoolean(OriginIslandConstants.BUNDLE_KEY_EFFECT_IS_LIGHT))
        val info = bundle.getBundle(OriginIslandConstants.BUNDLE_KEY_EFFECT_LIGHT_INFO)!!
        assertEquals(2, info.getInt(OriginIslandConstants.BUNDLE_KEY_EFFECT_LIGHT_MODE))
        assertEquals(0xFF00AAFF.toInt(), info.getInt(OriginIslandConstants.BUNDLE_KEY_EFFECT_LIGHT_MAIN_COLOR))
    }

    @Test
    fun `base sub-info is an action capsule when an action has a pending intent`() {
        val pi = pendingIntent()
        val bundle = build(1, configure = { copy(actions = listOf(CardAction("Reply", null, pi))) })
        val base = baseOf(bundle)
        assertEquals(2, base.getInt(OriginIslandConstants.BUNDLE_KEY_BASE_SUB_INFO))
        assertEquals("Reply", base.getString(OriginIslandConstants.BUNDLE_KEY_BASE_SUB_TEXT))
        assertNotNull(base.getParcelable(OriginIslandConstants.BUNDLE_KEY_BASE_SUB_INFO_CLICK_RESP))
    }

    @Test
    fun `base sub-info is the subtext when no action qualifies`() {
        val bundle = build(1, configure = { copy(subText = "Sub") })
        val base = baseOf(bundle)
        assertEquals(1, base.getInt(OriginIslandConstants.BUNDLE_KEY_BASE_SUB_INFO))
        assertEquals("Sub", base.getString(OriginIslandConstants.BUNDLE_KEY_BASE_SUB_TEXT))
    }

    @Test
    fun `landing target is written to every section when supplied`() {
        val bundle = build(1, configure = { copy(landingPkg = "com.example.app", landingInfo = "info") })
        assertTrue(bundle.getString(OriginIslandConstants.BUNDLE_KEY_ISLAND_LANDING_PKG) == "com.example.app")
        assertEquals(
            "com.example.app",
            islandOf(bundle).getString(OriginIslandConstants.BUNDLE_KEY_ISLAND_LANDING_PKG),
        )
        assertEquals(
            "com.example.app",
            capsuleOf(bundle).getString(OriginIslandConstants.BUNDLE_KEY_ISLAND_LANDING_PKG),
        )
        assertTrue(islandOf(bundle).getBoolean(OriginIslandConstants.BUNDLE_KEY_ISLAND_PERMANENT))
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getActivity(
        RuntimeEnvironment.getApplication(),
        0,
        Intent(),
        PendingIntent.FLAG_IMMUTABLE,
    )
}
