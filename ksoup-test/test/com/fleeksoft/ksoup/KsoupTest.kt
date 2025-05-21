package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals


class KsoupTest {


    // Tests for the Ksoup class. Mostly for code coverage for methods that haven't been covered elsewhere already.
    @Test
    fun parseWithPath() = runTest {
        val path = "htmltests/medium.html"
        var doc: Document = TestHelper.parseResource(resourceName = path, charsetName = "UTF-8", baseUri = "https://example.com/")
        val title = "Medium HTML"
        assertEquals(title, doc.title())

        // parse(Path path)
        doc = TestHelper.parseResource(path)
        assertEquals(title, doc.title())

        // (Path path, @Nullable String charsetName, String baseUri, Parser parser)
        doc = TestHelper.parseResource(resourceName = path, charsetName = "UTF-8", baseUri = "https://example.com/", parser = Parser.htmlParser())
        assertEquals(title, doc.title())
    }

}