package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.Ksoup.parse
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Tag
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CssTest {
    private lateinit var html: Document

    @BeforeTest
    fun init() {
        html = parse(htmlString)
    }

    @Test
    fun firstChild() {
        check(html.select("#pseudo :first-child"), "1")
        check(html.select("html:first-child"))
    }

    @Test
    fun lastChild() {
        check(html.select("#pseudo :last-child"), "10")
        check(html.select("html:last-child"))
    }

    @Test
    fun nthChild_simple() {
        for (i in 1..10) {
            check(html.select("#pseudo :nth-child($i)"), i.toString())
        }
    }

    @Test
    fun nthOfType_unknownTag() {
        for (i in 1..10) {
            check(html.select("#type svg:nth-of-type($i)"), i.toString())
        }
    }

    @Test
    fun nthLastChild_simple() {
        for (i in 1..10) {
            check(html.select("#pseudo :nth-last-child($i)"), (11 - i).toString())
        }
    }

    @Test
    fun nthOfType_simple() {
        for (i in 1..10) {
            check(html.select("#type p:nth-of-type($i)"), i.toString())
        }
    }

    @Test
    fun nthLastOfType_simple() {
        for (i in 1..10) {
            check(
                html.select("#type :nth-last-of-type($i)"),
                (11 - i).toString(),
                (11 - i).toString(),
                (11 - i).toString(),
                (11 - i).toString(),
            )
        }
    }

    @Test
    fun nthChild_advanced() {
        check(html.select("#pseudo :nth-child(-5)"))
        check(html.select("#pseudo :nth-child(odd)"), "1", "3", "5", "7", "9")
        check(html.select("#pseudo :nth-child(2n-1)"), "1", "3", "5", "7", "9")
        check(html.select("#pseudo :nth-child(2n+1)"), "1", "3", "5", "7", "9")
        check(html.select("#pseudo :nth-child(2n+3)"), "3", "5", "7", "9")
        check(html.select("#pseudo :nth-child(even)"), "2", "4", "6", "8", "10")
        check(html.select("#pseudo :nth-child(2n)"), "2", "4", "6", "8", "10")
        check(html.select("#pseudo :nth-child(3n-1)"), "2", "5", "8")
        check(html.select("#pseudo :nth-child(-2n+5)"), "1", "3", "5")
        check(html.select("#pseudo :nth-child(+5)"), "5")
    }

    @Test
    fun nthOfType_advanced() {
        check(html.select("#type :nth-of-type(-5)"))
        check(html.select("#type p:nth-of-type(odd)"), "1", "3", "5", "7", "9")
        check(html.select("#type em:nth-of-type(2n-1)"), "1", "3", "5", "7", "9")
        check(html.select("#type p:nth-of-type(2n+1)"), "1", "3", "5", "7", "9")
        check(html.select("#type span:nth-of-type(2n+3)"), "3", "5", "7", "9")
        check(html.select("#type p:nth-of-type(even)"), "2", "4", "6", "8", "10")
        check(html.select("#type p:nth-of-type(2n)"), "2", "4", "6", "8", "10")
        check(html.select("#type p:nth-of-type(3n-1)"), "2", "5", "8")
        check(html.select("#type p:nth-of-type(-2n+5)"), "1", "3", "5")
        check(html.select("#type :nth-of-type(+5)"), "5", "5", "5", "5")
    }

    @Test
    fun nthLastChild_advanced() {
        check(html.select("#pseudo :nth-last-child(-5)"))
        check(html.select("#pseudo :nth-last-child(odd)"), "2", "4", "6", "8", "10")
        check(html.select("#pseudo :nth-last-child(2n-1)"), "2", "4", "6", "8", "10")
        check(html.select("#pseudo :nth-last-child(2n+1)"), "2", "4", "6", "8", "10")
        check(html.select("#pseudo :nth-last-child(2n+3)"), "2", "4", "6", "8")
        check(html.select("#pseudo :nth-last-child(even)"), "1", "3", "5", "7", "9")
        check(html.select("#pseudo :nth-last-child(2n)"), "1", "3", "5", "7", "9")
        check(html.select("#pseudo :nth-last-child(3n-1)"), "3", "6", "9")
        check(html.select("#pseudo :nth-last-child(-2n+5)"), "6", "8", "10")
        check(html.select("#pseudo :nth-last-child(+5)"), "6")
    }

    @Test
    fun nthLastOfType_advanced() {
        check(html.select("#type :nth-last-of-type(-5)"))
        check(html.select("#type p:nth-last-of-type(odd)"), "2", "4", "6", "8", "10")
        check(html.select("#type em:nth-last-of-type(2n-1)"), "2", "4", "6", "8", "10")
        check(html.select("#type p:nth-last-of-type(2n+1)"), "2", "4", "6", "8", "10")
        check(html.select("#type span:nth-last-of-type(2n+3)"), "2", "4", "6", "8")
        check(html.select("#type p:nth-last-of-type(even)"), "1", "3", "5", "7", "9")
        check(html.select("#type p:nth-last-of-type(2n)"), "1", "3", "5", "7", "9")
        check(html.select("#type p:nth-last-of-type(3n-1)"), "3", "6", "9")
        check(html.select("#type span:nth-last-of-type(-2n+5)"), "6", "8", "10")
        check(html.select("#type :nth-last-of-type(+5)"), "6", "6", "6", "6")
    }

    @Test
    fun firstOfType() {
        check(html.select("div:not(#only) :first-of-type"), "1", "1", "1", "1", "1")
    }

    @Test
    fun lastOfType() {
        check(html.select("div:not(#only) :last-of-type"), "10", "10", "10", "10", "10")
    }

    @Test
    fun empty() {
        val sel = html.select(":empty")
        assertEquals(3, sel.size)
        assertEquals("head", sel[0].tagName())
        assertEquals("br", sel[1].tagName())
        assertEquals("p", sel[2].tagName())
    }

    @Test
    fun onlyChild() {
        val sel = html.select("span :only-child")
        assertEquals(1, sel.size)
        assertEquals("br", sel[0].tagName())
        check(html.select("#only :only-child"), "only")
    }

    @Test
    fun onlyOfType() {
        val sel = html.select(":only-of-type")
        assertEquals(6, sel.size)
        assertEquals("head", sel[0].tagName())
        assertEquals("body", sel[1].tagName())
        assertEquals("span", sel[2].tagName())
        assertEquals("br", sel[3].tagName())
        assertEquals("p", sel[4].tagName())
        assertTrue(sel[4].hasClass("empty"))
        assertEquals("em", sel[5].tagName())
    }

    private fun check(
        result: Elements,
        vararg expectedContent: String?,
    ) {
        assertEquals(expectedContent.size, result.size, "Number of elements")
        for (i in expectedContent.indices) {
            assertNotNull(result[i])
            assertEquals(expectedContent[i], result[i].ownText(), "Expected element")
        }
    }

    @Test
    fun root() {
        val sel = html.select(":root")
        assertEquals(1, sel.size)
        assertNotNull(sel[0])
        assertEquals(Tag.valueOf("html"), sel[0].tag())
        val sel2 = html.select("body").select(":root")
        assertEquals(1, sel2.size)
        assertNotNull(sel2[0])
        assertEquals(Tag.valueOf("body"), sel2[0].tag())
    }

    companion object {
        private lateinit var htmlString: String

        init {
            initClass()
        }

        private fun initClass() {
            val sb = StringBuilder("<html><head></head><body>")
            sb.append("<div id='pseudo'>")
            for (i in 1..10) {
                sb.append("<p>$i</p>")
            }
            sb.append("</div>")
            sb.append("<div id='type'>")
            for (i in 1..10) {
                sb.append("<p>$i</p>")
                sb.append("<span>$i</span>")
                sb.append("<em>$i</em>")
                sb.append("<svg>$i</svg>")
            }
            sb.append("</div>")
            sb.append("<span id='onlySpan'><br /></span>")
            sb.append("<p class='empty'><!-- Comment only is still empty! --></p>")
            sb.append("<div id='only'>")
            sb.append("Some text before the <em>only</em> child in this div")
            sb.append("</div>")
            sb.append("</body></html>")
            htmlString = sb.toString()
        }
    }
}
