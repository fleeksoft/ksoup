package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.Ksoup.parse
import com.fleeksoft.ksoup.Platform
import com.fleeksoft.ksoup.PlatformType
import com.fleeksoft.ksoup.nodes.Entities.escape
import com.fleeksoft.ksoup.nodes.Entities.getByName
import com.fleeksoft.ksoup.nodes.Entities.unescape
import com.fleeksoft.ksoup.parser.Parser
import de.cketti.codepoints.deluxe.toCodePoint
import kotlin.test.*
import kotlin.test.Test

class EntitiesTest {
    @Test
    fun escape() {
        if (Platform.current == PlatformType.JS) {
            // FIXME: ascii charset not supported for js
            return
        }

        val text = "Hello &<> Å å π 新 there ¾ © »"
        val escapedAscii = escape(text, Document.OutputSettings().charset("ascii").escapeMode(Entities.EscapeMode.base))
        val escapedAsciiFull =
            escape(text, Document.OutputSettings().charset("ascii").escapeMode(Entities.EscapeMode.extended))
        val escapedAsciiXhtml =
            escape(text, Document.OutputSettings().charset("ascii").escapeMode(Entities.EscapeMode.xhtml))
        val escapedUtfFull =
            escape(text, Document.OutputSettings().charset("UTF-8").escapeMode(Entities.EscapeMode.extended))
        val escapedUtfMin =
            escape(text, Document.OutputSettings().charset("UTF-8").escapeMode(Entities.EscapeMode.xhtml))
        assertEquals(
            "Hello &amp;&lt;&gt; &Aring; &aring; &#x3c0; &#x65b0; there &frac34; &copy; &raquo;",
            escapedAscii,
        )
        assertEquals(
            "Hello &amp;&lt;&gt; &angst; &aring; &pi; &#x65b0; there &frac34; &copy; &raquo;",
            escapedAsciiFull,
        )
        assertEquals(
            "Hello &amp;&lt;&gt; &#xc5; &#xe5; &#x3c0; &#x65b0; there &#xbe; &#xa9; &#xbb;",
            escapedAsciiXhtml,
        )
        assertEquals("Hello &amp;&lt;&gt; Å å π 新 there ¾ © »", escapedUtfFull)
        assertEquals("Hello &amp;&lt;&gt; Å å π 新 there ¾ © »", escapedUtfMin)
        // odd that it's defined as aring in base but angst in full

        // round trip
        assertEquals(text, unescape(escapedAscii))
        assertEquals(text, unescape(escapedAsciiFull))
        assertEquals(text, unescape(escapedAsciiXhtml))
        assertEquals(text, unescape(escapedUtfFull))
        assertEquals(text, unescape(escapedUtfMin))
    }

    @Test
    fun escapedSupplementary() {
        if (Platform.current == PlatformType.JS) {
            // FIXME: ascii charset not supported for js
            return
        }

        val text = "\uD835\uDD59"
        val escapedAscii = escape(text, Document.OutputSettings().charset("ascii").escapeMode(Entities.EscapeMode.base))
        assertEquals("&#x1d559;", escapedAscii)
        val escapedAsciiFull =
            escape(text, Document.OutputSettings().charset("ascii").escapeMode(Entities.EscapeMode.extended))
        assertEquals("&hopf;", escapedAsciiFull)
        val escapedUtf =
            escape(text, Document.OutputSettings().charset("UTF-8").escapeMode(Entities.EscapeMode.extended))
        assertEquals(text, escapedUtf)
    }

    @Test
    fun unescapeMultiChars() {
        if (Platform.current == PlatformType.JS) {
            // FIXME: ascii charset not supported for js
            return
        }

        val text =
            "&NestedGreaterGreater; &nGg; &nGt; &nGtv; &Gt; &gg;" // gg is not combo, but 8811 could conflict with NestedGreaterGreater or others
        val un = "≫ ⋙̸ ≫⃒ ≫̸ ≫ ≫"
        assertEquals(un, unescape(text))
        val escaped = escape(un, Document.OutputSettings().charset("ascii").escapeMode(Entities.EscapeMode.extended))
        assertEquals("&Gt; &Gg;&#x338; &Gt;&#x20d2; &Gt;&#x338; &Gt; &Gt;", escaped)
        assertEquals(un, unescape(escaped))
    }

    @Test
    fun xhtml() {
        assertEquals(38, Entities.EscapeMode.xhtml.codepointForName("amp"))
        assertEquals(62, Entities.EscapeMode.xhtml.codepointForName("gt"))
        assertEquals(60, Entities.EscapeMode.xhtml.codepointForName("lt"))
        assertEquals(34, Entities.EscapeMode.xhtml.codepointForName("quot"))
        assertEquals("amp", Entities.EscapeMode.xhtml.nameForCodepoint(38))
        assertEquals("gt", Entities.EscapeMode.xhtml.nameForCodepoint(62))
        assertEquals("lt", Entities.EscapeMode.xhtml.nameForCodepoint(60))
        assertEquals("quot", Entities.EscapeMode.xhtml.nameForCodepoint(34))
    }

    @Test
    fun getByName() {
        assertEquals("≫⃒", getByName("nGt"))
        assertEquals("fj", getByName("fjlig"))
        assertEquals("≫", getByName("gg"))
        assertEquals("©", getByName("copy"))
    }

    @Test
    fun escapeSupplementaryCharacter() {
        if (Platform.current == PlatformType.JS) {
            // FIXME: ascii charset not supported for js
            return
        }

        val text = 135361.toCodePoint().toChars().concatToString()
        val escapedAscii = escape(text, Document.OutputSettings().charset("ascii").escapeMode(Entities.EscapeMode.base))
        assertEquals("&#x210c1;", escapedAscii)
        val escapedUtf = escape(text, Document.OutputSettings().charset("UTF-8").escapeMode(Entities.EscapeMode.base))
        assertEquals(text, escapedUtf)
    }

    @Test
    fun notMissingMultis() {
        val text = "&nparsl;"
        val un = "\u2AFD\u20E5"
        assertEquals(un, unescape(text))
    }

    @Test
    fun notMissingSupplementals() {
        val text = "&npolint; &qfr;"
        val un = "⨔ \uD835\uDD2E" // 𝔮
        assertEquals(un, unescape(text))
    }

    @Test
    fun unescape() {
        val text =
            "Hello &AElig; &amp;&LT&gt; &reg &angst; &angst &#960; &#960 &#x65B0; there &! &frac34; &copy; &COPY;"
        assertEquals("Hello Æ &<> ® Å &angst π π 新 there &! ¾ © ©", unescape(text))
        assertEquals("&0987654321; &unknown", unescape("&0987654321; &unknown"))
    }

    @Test
    fun strictUnescape() { // for attributes, enforce strict unescaping (must look like &#xxx; , not just &#xxx)
        val text = "Hello &amp= &amp;"
        assertEquals("Hello &amp= &", unescape(text, true))
        assertEquals("Hello &= &", unescape(text))
        assertEquals("Hello &= &", unescape(text, false))
    }

    @Test
    fun caseSensitive() {
        if (Platform.current == PlatformType.JS) {
            // FIXME: ascii charset not supported for js
            return
        }

        val unescaped = "Ü ü & &"
        assertEquals(
            "&Uuml; &uuml; &amp; &amp;",
            escape(unescaped, Document.OutputSettings().charset("ascii").escapeMode(Entities.EscapeMode.extended)),
        )
        val escaped = "&Uuml; &uuml; &amp; &AMP"
        assertEquals("Ü ü & &", unescape(escaped))
    }

    @Test
    fun quoteReplacements() {
        val escaped = "&#92; &#36;"
        val unescaped = "\\ $"
        assertEquals(unescaped, unescape(escaped))
    }

    @Test
    fun letterDigitEntities() {
        if (Platform.current == PlatformType.JS) {
            // FIXME: ascii charset not supported for js
            return
        }

        val html = "<p>&sup1;&sup2;&sup3;&frac14;&frac12;&frac34;</p>"
        val doc = parse(html)
        doc.outputSettings().charset("ascii")
        val p = doc.select("p").first()
        assertEquals("&sup1;&sup2;&sup3;&frac14;&frac12;&frac34;", p!!.html())
        assertEquals("¹²³¼½¾", p.text())
        doc.outputSettings().charset("UTF-8")
        assertEquals("¹²³¼½¾", p.html())
    }

    @Test
    fun noSpuriousDecodes() {
        val string = "http://www.foo.com?a=1&num_rooms=1&children=0&int=VA&b=2"
        assertEquals(string, unescape(string))
    }

    @Test
    fun escapesGtInXmlAttributesButNotInHtml() {
        // https://github.com/jhy/jsoup/issues/528 - < is OK in HTML attribute values, but not in XML
        val docHtml = "<a title='<p>One</p>'>One</a>"
        val doc = parse(docHtml)
        val element = doc.select("a").first()
        doc.outputSettings().escapeMode(Entities.EscapeMode.base)
        assertEquals("<a title=\"<p>One</p>\">One</a>", element!!.outerHtml())
        doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml)
        assertEquals("<a title=\"&lt;p>One&lt;/p>\">One</a>", element.outerHtml())
    }

    @Test
    fun controlCharactersAreEscaped() {
        // https://github.com/jhy/jsoup/issues/1556
        // we escape ascii control characters in both HTML and XML for compatibility. Required in XML and probably
        // easier to read in HTML
        val input = "<a foo=\"&#x1b;esc&#x7;bell\">Text &#x1b; &#x7;</a>"
        val doc = parse(input)
        assertEquals(input, doc.body().html())
        val xml = parse(input, "", Parser.xmlParser())
        assertEquals(input, xml.html())
    }

    @Test
    fun escapeByClonedOutputSettings() {
        val outputSettings = Document.OutputSettings()
        val text = "Hello &<> Å å π 新 there ¾ © »"
        val clone1 = outputSettings.clone()
        val clone2 = outputSettings.clone()
        val escaped1 = escape(text, clone1)
        val escaped2 = escape(text, clone2)
        assertEquals(escaped1, escaped2)
    }
}
