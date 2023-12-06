package com.fleeksoft.ksoup

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformTest {
    @Test
    fun testJsSupportedRegex() {
        val regex2 = jsSupportedRegex("img[src~=(?i)\\.(png|jpe?g)]")
        val expected2 = """img[src~=\.(png|jpe?g)]"""
        assertEquals(expected2, regex2.pattern)
        assertEquals(RegexOption.IGNORE_CASE, regex2.options.first())
    }
}
