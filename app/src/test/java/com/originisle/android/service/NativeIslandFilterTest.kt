package com.originisle.android.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeIslandFilterTest {
    private val own = "com.autonavi.minimap"

    @Test
    fun `own posts are never treated as foreign native islands`() {
        assertFalse(NativeIslandFilter.isNativeIsland("VIVO_SUPERX_TAG", setOf("notification.superx.template"), own, own))
    }

    @Test
    fun `foreign post under the superx tag is native`() {
        assertTrue(NativeIslandFilter.isNativeIsland("VIVO_SUPERX_TAG", emptySet(), "com.sankuai.meituan", own))
    }

    @Test
    fun `foreign post with superx extras is native`() {
        assertTrue(NativeIslandFilter.isNativeIsland(null, setOf("android.title", "island.superx.leftInfo"), "com.sdu.didi.psnger", own))
        assertTrue(NativeIslandFilter.isNativeIsland(null, setOf("notification.superx.operation"), "com.eg.android.AlipayGphone", own))
    }

    @Test
    fun `ordinary foreign notification is not native`() {
        assertFalse(NativeIslandFilter.isNativeIsland(null, setOf("android.title", "android.text"), "com.tencent.mm", own))
        assertFalse(NativeIslandFilter.isNativeIsland("some_tag", emptySet(), "com.tencent.mm", own))
    }
}
