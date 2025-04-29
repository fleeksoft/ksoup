package com.fleeksoft.ksoup.parser

import co.touchlab.stately.collections.ConcurrentMutableList
import com.fleeksoft.ksoup.BuildConfig
import com.fleeksoft.ksoup.Platform
import com.fleeksoft.ksoup.System
import com.fleeksoft.ksoup.isJsOrWasm
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
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

    @Test
    fun parserIsThreadSafe() = runTest {
        val html = "<div id=1><div id=2><div id=3>Text.</div></div></div>"
        val parser = Parser.htmlParser()
        val expectDoc: Document = parser.parseInput(html, "")

        val numCoroutines = 10
        val numLoops = 20
        val toCheck = ConcurrentMutableList<Document>()

        val jobs = List(numCoroutines) {
            launch(Dispatchers.Default) {
                repeat(numLoops) {
                    val doc = parser.parseInput(html, "")
                    toCheck.add(doc)
                }
            }
        }
        jobs.joinAll()

        toCheck.forEach { doc ->
            assertTrue(doc.hasSameValue(expectDoc))
        }
    }

    @Test
    fun parserIsThreadSafeWithCloneAndAppend() = runTest {
        val html = "<div id=1><div id=2><div id=3></div></div></div>"
        val append = "<div id=4>Text.</div>"
        val parser = Parser.htmlParser()
        val baseDoc: Document = parser.parseInput(html, "")
        val baseElement: Element = baseDoc.expectFirst("#3")

        val numCoroutines = 10
        val numLoops = 20
        val toCheck = ConcurrentMutableList<Element>()

        val jobs = List(numCoroutines) {
            launch(Dispatchers.Default) {
                repeat(numLoops) {
                    val cloned: Element = baseElement.clone()
                    cloned.append(append)
                    toCheck.add(cloned)
                }
            }
        }
        jobs.joinAll()

        baseElement.append(append)
        toCheck.forEach { element ->
            assertTrue(element.hasSameValue(baseElement))
        }
    }
}
