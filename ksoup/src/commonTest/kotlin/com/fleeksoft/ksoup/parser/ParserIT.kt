package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.*
import com.fleeksoft.ksoup.ported.System
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Longer running Parser tests.
 */
class ParserIT {

    @Test
    fun handlesDeepStack() {
        if ((Platform.isJS() || Platform.isWindows()) && BuildConfig.isGithubActions) {
//            The GitHub action is taking too much time.
            return
        }
        // inspired by http://sv.stargate.wikia.com/wiki/M2J and https://github.com/jhy/jsoup/issues/955
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
        assertTrue(System.currentTimeMillis() - start < 40000) // I get ~ 1.5 seconds, but others have reported slower
        // was originally much longer, or stack overflow.
    }
}
