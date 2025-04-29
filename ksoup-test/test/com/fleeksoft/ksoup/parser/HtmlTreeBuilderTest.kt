package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.Ksoup.parse
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.TagSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class HtmlTreeBuilderTest {

    @Test
    fun isSpecial() {
        val settings = ParseSettings.htmlDefault
        val htmlEl = Element(Tag.valueOf("div", Parser.NamespaceHtml, settings), "")
        assertTrue(HtmlTreeBuilder.isSpecial(htmlEl))

        val notHtml = Element(Tag.valueOf("not-html", Parser.NamespaceHtml, settings), "")
        assertFalse(HtmlTreeBuilder.isSpecial(notHtml))

        val mathEl = Element(Tag.valueOf("mi", Parser.NamespaceMathml, settings), "")
        assertTrue(HtmlTreeBuilder.isSpecial(mathEl))

        val notMathEl = Element(Tag.valueOf("not-math", Parser.NamespaceMathml, settings), "")
        assertFalse(HtmlTreeBuilder.isSpecial(notMathEl))

        val svgEl = Element(Tag.valueOf("title", Parser.NamespaceSvg, settings), "")
        assertTrue(HtmlTreeBuilder.isSpecial(svgEl))

        val notSvgEl = Element(Tag.valueOf("not-svg", Parser.NamespaceSvg, settings), "")
        assertFalse(HtmlTreeBuilder.isSpecial(notSvgEl))
    }

    @Test
    fun customRcdataTag() {
        val inner = "Blah\nblah\n<foo>Foo</foo>\n&quot;"
        val innerText = "Blah\nblah\n<foo>Foo</foo>\n\""
        val html = "<div><x>$inner</x></div><div><x id=2></x></div>"
        val custom = TagSet.Html()
        val x = custom.valueOf("x", Parser.NamespaceHtml)
        x.set(Tag.RcData)

        val doc = parse(html, Parser.htmlParser().tagSet(custom))
        val xEl = doc.expectFirst("x")
        assertEquals(x, xEl.tag())
        assertEquals(innerText, xEl.wholeText()) // <foo> is text no el

        // fragment parse context
        val x2 = doc.expectFirst("#2")
        x2.html(inner) // <foo> will be text not el, via custom fragment context element
        assertEquals(innerText, x2.wholeText())
    }

    @Test
    fun customDataTag() {
        val inner = "Blah\nblah\n<foo>Foo</foo>\n&quot;" // no character refs will be as-is
        val html = "<div><x>$inner</x></div><div><x id=2></x></div>"
        val custom = TagSet.Html()
        val x = custom.valueOf("x", Parser.NamespaceHtml)
        x.set(Tag.Data)

        val doc = parse(html, Parser.htmlParser().tagSet(custom))
        val xEl = doc.expectFirst("x")
        assertEquals(x, xEl.tag())
        assertEquals(inner, xEl.data())

        // fragment parse context
        val x2 = doc.expectFirst("#2")
        x2.html(inner) // <foo> will be text not el, via custom fragment context element
        assertEquals(inner, xEl.data())
    }
}
