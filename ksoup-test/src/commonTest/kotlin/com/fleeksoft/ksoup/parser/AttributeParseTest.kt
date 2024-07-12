package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.Ksoup.parse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test suite for attribute parser.
 *
 * @author Sabeeh, fleeksoft@gmail.com
 */
class AttributeParseTest {
    @Test
    fun parsesRoughAttributeString() {
        val html = "<a id=\"123\" class=\"baz = 'bar'\" style = 'border: 2px'qux zim foo = 12 mux=18 />"
        // should be: <id=123>, <class=baz = 'bar'>, <qux=>, <zim=>, <foo=12>, <mux.=18>
        val el = parse(html).getElementsByTag("a")[0]
        val attr = el.attributes()
        assertEquals(7, attr.size())
        assertEquals("123", attr["id"])
        assertEquals("baz = 'bar'", attr["class"])
        assertEquals("border: 2px", attr["style"])
        assertEquals("", attr["qux"])
        assertEquals("", attr["zim"])
        assertEquals("12", attr["foo"])
        assertEquals("18", attr["mux"])
    }

    @Test
    fun handlesNewLinesAndReturns() {
        val html = "<a\r\nfoo='bar\r\nqux'\r\nbar\r\n=\r\ntwo>One</a>"
        val el = parse(html).select("a").first()
        assertEquals(2, el!!.attributes().size())
        assertEquals(
            "bar\r\nqux",
            el.attr("foo"),
        ) // currently preserves newlines in quoted attributes. todo confirm if should.
        assertEquals("two", el.attr("bar"))
    }

    @Test
    fun parsesEmptyString() {
        val html = "<a />"
        val el = parse(html).getElementsByTag("a")[0]
        val attr = el.attributes()
        assertEquals(0, attr.size())
    }

    @Test
    fun canStartWithEq() {
        val html = "<a =empty />"
        // TODO this is the weirdest thing in the spec - why not consider this an attribute with an empty name, not where name is '='?
        // am I reading it wrong? https://html.spec.whatwg.org/multipage/parsing.html#before-attribute-name-state
        val el = parse(html).getElementsByTag("a")[0]
        val attr = el.attributes()
        assertEquals(1, attr.size())
        assertTrue(attr.hasKey("=empty"))
        assertEquals("", attr["=empty"])
    }

    @Test
    fun strictAttributeUnescapes() {
        val html = "<a id=1 href='?foo=bar&mid&lt=true'>One</a> <a id=2 href='?foo=bar&lt;qux&lg=1'>Two</a>"
        val els = parse(html).select("a")
        assertEquals("?foo=bar&mid&lt=true", els.first()!!.attr("href"))
        assertEquals("?foo=bar<qux&lg=1", els.last()!!.attr("href"))
    }

    @Test
    fun moreAttributeUnescapes() {
        val html = "<a href='&wr_id=123&mid-size=true&ok=&wr'>Check</a>"
        val els = parse(html).select("a")
        assertEquals("&wr_id=123&mid-size=true&ok=&wr", els.first()!!.attr("href"))
    }

    @Test
    fun parsesBooleanAttributes() {
        val html = "<a normal=\"123\" boolean empty=\"\"></a>"
        val el = parse(html).select("a").first()
        assertEquals("123", el!!.attr("normal"))
        assertEquals("", el.attr("boolean"))
        assertEquals("", el.attr("empty"))
        val attributes = el.attributes().asList()
        assertEquals(3, attributes.size, "There should be 3 attribute present")
        assertEquals(html, el.outerHtml()) // vets boolean syntax
    }

    @Test
    fun dropsSlashFromAttributeName() {
        val html = "<img /onerror='doMyJob'/>"
        var doc = parse(html)
        assertFalse(doc.select("img[onerror]").isEmpty(), "SelfClosingStartTag ignores last character")
        assertEquals("<img onerror=\"doMyJob\">", doc.body().html())
        doc = parse(html, "", Parser.xmlParser())
        assertEquals("<img onerror=\"doMyJob\" />", doc.html())
    }
}
