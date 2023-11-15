package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.integration.ParseTest
import com.fleeksoft.ksoup.nodes.Attributes
import com.fleeksoft.ksoup.nodes.Document
import okio.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosGreetingTest {

    @Test
    fun loadsZGzipFile() {
        // compressed on win, with z suffix
        val `in`: String = ParseTest.getResourceAbsolutePath("htmltests/gzip.html.z")
        val doc: Document = Ksoup.parseFile(`in`, null)
        val title = doc.title()
        assertEquals("Gzip test", title)
        assertEquals("This is a gzipped HTML file.", doc.selectFirst("p")!!.text())
    }

    @Test
    fun html() {
        val a = Attributes()
        a.put("Tot", "a&p")
        a.put("Hello", "There")
        a.put("data-name", "Jsoup")
        assertEquals(3, a.size())
        assertTrue(a.hasKey("Tot"))
        assertTrue(a.hasKey("Hello"))
        assertTrue(a.hasKey("data-name"))
        assertFalse(a.hasKey("tot"))
        assertTrue(a.hasKeyIgnoreCase("tot"))
        assertEquals("There", a.getIgnoreCase("hEllo"))
        val dataset: Attributes.Dataset = a.dataset()
        assertEquals(1, dataset.size)
        assertEquals("Jsoup", dataset["name"])
        assertEquals("", a["tot"])
        assertEquals("a&p", a["Tot"])
        assertEquals("a&p", a.getIgnoreCase("tot"))
        assertEquals(" Tot=\"a&amp;p\" Hello=\"There\" data-name=\"Jsoup\"", a.html())
        assertEquals(a.html(), a.toString())
    }

    @Test
    fun testExample() {
        println("testExample running for testinng....")
//        assertTrue(Greeting().greet().contains("iOS"), "Check iOS is mentioned")
    }
}