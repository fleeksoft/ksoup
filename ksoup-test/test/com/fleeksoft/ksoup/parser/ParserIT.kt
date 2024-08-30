package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.BuildConfig
import com.fleeksoft.ksoup.Platform
import com.fleeksoft.ksoup.System
import com.fleeksoft.ksoup.isJsOrWasm
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Longer running Parser tests.
 */
class ParserIT {

    @Test
    @Ignore // disabled by default now, as there more specific unconsume tests
    fun testIssue1251() {
        // https://github.com/jhy/Ksoup/issues/1251
        val str = StringBuilder("<a href=\"\"ca")
        for (countSpaces in 0..99999) {
            try {
                Parser.htmlParser().setTrackErrors(1).parseInput(str.toString(), "")
            } catch (e: Exception) {
                throw AssertionError("failed at length " + str.length, e)
            }
            str.insert(countSpaces, ' ')
        }
    }

    @Test
    fun handlesDeepStack() {
        if (Platform.isJsOrWasm() && BuildConfig.isGithubActions) {
//            The GitHub action is taking too much time.
            return
        }
        // inspired by http://sv.stargate.wikia.com/wiki/M2J
        // I didn't put it in the integration tests, because explorer and intellij kept dieing trying to preview/index it

        // Arrange
        val longBody = StringBuilder(500000)
        for (i in 0..24999) {
            longBody.append(i).append("<dl><dd>")
        }
        for (i in 0..24999) {
            longBody.append(i).append("</dd></dl>")
        }

        // Act
        val start = System.currentTimeMillis()
        val doc = Parser.parseBodyFragment(longBody.toString(), "")

        // Assert
        assertEquals(2, doc.body().childNodeSize())
        assertEquals(25000, doc.select("dd").size)
        val end = System.currentTimeMillis() - start
        assertTrue(
            end < 40000,
            "Expected max time for this test was: 40 secs but it took ${end.div(1000)}"
        )// I get ~ 1.5 seconds, but others have reported slower
        // was originally much longer, or stack overflow.
    }
}
