package com.fleeksoft.ksoup.integration

import com.fleeksoft.io.byteInputStream
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.TestHelper
import com.fleeksoft.ksoup.integration.FuzzFixesIT.Companion.fuzzTestFiles
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.parameterizedTestSuspend
import com.fleeksoft.ksoup.parseInput
import com.fleeksoft.ksoup.parser.Parser
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class FuzzFixesTest {
    @Test
    fun blankAbsAttr() {
        // https://github.com/jhy/jsoup/issues/1541
        val html = "b<bodY abs: abs:abs: abs:abs:abs>"
        val doc = Ksoup.parse(html)
        assertNotNull(doc)
    }

    @Test
    fun bookmark() {
        // https://github.com/jhy/jsoup/issues/1576
        val html = "<?a<U<P<A "
        val doc: Document = Ksoup.parse(html)
        assertNotNull(doc)

        val xmlDoc: Document = Parser.xmlParser().parseInput(html, "")
        assertNotNull(xmlDoc)
    }

    @Test
    fun fragment() {
        Parser.htmlParser().parseFragmentInput("<frameset>>l\u0000<\u0000<ditl>\u0000< \\", Element("colgroup"), "")
    }

    @Test
    fun testHtmlParse() = runTest {
        parameterizedTestSuspend(fuzzTestFiles) { fuzzFile ->
            val input = TestHelper.readResourceAsString(fuzzFile).byteInputStream()
            var doc: Document = Ksoup.parseInput(input, charsetName = "UTF-8", baseUri = "https://example.com/")
            assertNotNull(doc)
            doc = Ksoup.parseInput(
                input,
                charsetName = "UTF-8",
                baseUri = ""
            ) // no base href attr; so same as a parse(string), which can have subtly different semantics
            assertNotNull(doc)
        }
    }

    @Test
    fun testHtmlFragmentParse() = runTest {
        parameterizedTestSuspend(fuzzTestFiles) { fuzzFile ->
            val html = TestHelper.readResourceAsString(fuzzFile)
            val doc: Document = Ksoup.parseBodyFragment(html)
            assertNotNull(doc)
        }
    }

    @Test
    fun testXmlParse() = runTest {
        parameterizedTestSuspend(fuzzTestFiles) { fuzzFile ->
            val input = TestHelper.readResourceAsString(fuzzFile).byteInputStream()
            var doc: Document = Ksoup.parseInput(input, charsetName = "UTF-8", baseUri = "https://example.com/", parser = Parser.xmlParser())
            assertNotNull(doc)
            doc = Ksoup.parseInput(input, charsetName = "UTF-8", baseUri = "", parser = Parser.xmlParser()) // no base href attr
        }
    }
}