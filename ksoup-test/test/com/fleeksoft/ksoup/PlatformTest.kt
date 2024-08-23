package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.ported.jsSupportedRegex
import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformTest {

    @Test
    fun testJsSupportedRegex() {
        val regex2 = jsSupportedRegex("img[src~=(?i)\\.(png|jpe?g)]")
        val expected2 = if (Platform.isJsOrWasm()) """img[src~=\.(png|jpe?g)]""" else """img[src~=(?i)\.(png|jpe?g)]"""
        assertEquals(expected2, regex2.pattern)
        if (Platform.isJsOrWasm()) {
            assertEquals(RegexOption.IGNORE_CASE, regex2.options.first())
        }
    }
}
