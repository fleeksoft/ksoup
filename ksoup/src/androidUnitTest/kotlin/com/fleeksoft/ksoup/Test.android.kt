package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.integration.ParseTest
import com.fleeksoft.ksoup.nodes.Document
import io.ktor.utils.io.charsets.*
import org.junit.Assert.assertTrue
import java.io.FileInputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidGreetingTest {

    @Test
    fun testLowercaseUtf8CharsetWithInputStream() {
        val inputStream = FileInputStream(ParseTest.getResourceAbsolutePath("htmltests/lowercase-charset-test.html"))
        val doc: Document = Ksoup.parse(inputStream = inputStream, baseUri = "", charsetName = null)
        val form = doc.select("#form").first()
        assertEquals(2, form!!.children().size)
        assertEquals("UTF-8", doc.outputSettings().charset().name.uppercase())
    }
}