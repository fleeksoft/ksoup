package com.fleeksoft.ksoup.integration

import com.fleeksoft.ksoup.Ksoup.parse
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parameterizedTest
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.ported.BufferReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests fixes for issues raised by the OSS Fuzz project @ https://oss-fuzz.com/testcases?project=jsoup. Contains inline
 * string cases causing exceptions. Timeout tests are in FuzzFixesIT.
 */
class FuzzFixesTest {
    @Test
    fun blankAbsAttr() {
        // https://github.com/jhy/jsoup/issues/1541
        val html = "b<bodY abs: abs:abs: abs:abs:abs>"
        val doc = parse(html)
        assertNotNull(doc)
    }

    @Test
    fun bookmark() {
        // https://github.com/jhy/jsoup/issues/1576
        val html = "<?a<U<P<A "
        val doc = parse(html)
        assertNotNull(doc)
        val xmlDoc = Parser.xmlParser().parseInput(html, "")
        assertNotNull(xmlDoc)
    }

    @Test
    fun testHtmlParse() {
        parameterizedTest(testFiles()) { file ->
            val doc: Document = parse(BufferReader(file.readBytes()), "UTF-8", "https://example.com/")
            assertNotNull(doc)
        }
    }

    @Test
    fun testXmlParse() {
        parameterizedTest(testFiles()) { file ->
            val doc = parse(BufferReader(file.readBytes()), "UTF-8", "https://example.com/", Parser.xmlParser())
            assertNotNull(doc)
        }
    }

    companion object {
        private fun testFiles(): List<File> {
            val files = FuzzFixesIT.testDir.listFiles()!!.toList()
            assertNotNull(files)
            assertTrue(files.size > 10)
            return files
        }
    }
}
