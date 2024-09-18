package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.Ksoup.parse
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.ported.toCodePoint
import kotlin.test.Test
import kotlin.test.assertEquals

class EntitiesTest {

    @Test
    fun escape() {
        // escape is maximal (as in the escapes cover use in both text and attributes; vs Element.html() which checks if attribute or text and minimises escapes
        val text = "Hello &<> Å å π 新 there ¾ © » ' \""
        val escapedAscii = Entities.escape(text, Document.OutputSettings().charset("ISO-8859-1").escapeMode(Entities.EscapeMode.base))
        val escapedAsciiFull = Entities.escape(text, Document.OutputSettings().charset("ISO-8859-1").escapeMode(Entities.EscapeMode.extended))
        val escapedAsciiXhtml = Entities.escape(text, Document.OutputSettings().charset("ISO-8859-1").escapeMode(Entities.EscapeMode.xhtml))
        val escapedUtfFull = Entities.escape(text, Document.OutputSettings().charset("UTF-8").escapeMode(Entities.EscapeMode.extended))
        val escapedUtfMin = Entities.escape(text, Document.OutputSettings().charset("UTF-8").escapeMode(Entities.EscapeMode.xhtml))

        assertEquals("Hello &amp;&lt;&gt; Å å &#x3c0; &#x65b0; there ¾ © » &apos; &quot;", escapedAscii)
        assertEquals("Hello &amp;&lt;&gt; Å å &pi; &#x65b0; there ¾ © » &apos; &quot;", escapedAsciiFull)
        assertEquals("Hello &amp;&lt;&gt; Å å &#x3c0; &#x65b0; there ¾ © » &#x27; &quot;", escapedAsciiXhtml)
        assertEquals("Hello &amp;&lt;&gt; Å å π 新 there ¾ © » &apos; &quot;", escapedUtfFull)
        assertEquals("Hello &amp;&lt;&gt; Å å π 新 there ¾ © » &#x27; &quot;", escapedUtfMin)
        // odd that it's defined as aring in base but angst in full

        // round trip
        assertEquals(text, Entities.unescape(escapedAscii))
        assertEquals(text, Entities.unescape(escapedAsciiFull))
        assertEquals(text, Entities.unescape(escapedAsciiXhtml))
        assertEquals(text, Entities.unescape(escapedUtfFull))
        assertEquals(text, Entities.unescape(escapedUtfMin))
    }

    @Test
    fun escapeDefaults() {
        val text = "Hello &<> Å å π 新 there ¾ © » ' \""
        val escaped = Entities.escape(text)
        assertEquals("Hello &amp;&lt;&gt; Å å π 新 there ¾ © » &apos; &quot;", escaped)
    }

    @Test
    fun escapedSupplementary() {
        val text = "\uD835\uDD59"
        val escapedAscii = Entities.escape(text, Document.OutputSettings().charset("ISO-8859-1").escapeMode(Entities.EscapeMode.base))
        assertEquals("&#x1d559;", escapedAscii)
        val escapedAsciiFull = Entities.escape(text, Document.OutputSettings().charset("ISO-8859-1").escapeMode(Entities.EscapeMode.extended))
        assertEquals("&hopf;", escapedAsciiFull)
        val escapedUtf = Entities.escape(text, Document.OutputSettings().charset("UTF-8").escapeMode(Entities.EscapeMode.extended))
        assertEquals(text, escapedUtf)
    }

    @Test
    fun unescapeMultiChars() {
        val text =
            "&NestedGreaterGreater; &nGg; &nGt; &nGtv; &Gt; &gg;" // gg is not combo, but 8811 could conflict with NestedGreaterGreater or others
        val un = "≫ ⋙̸ ≫⃒ ≫̸ ≫ ≫"
        assertEquals(un, Entities.unescape(text))
        val escaped = Entities.escape(un, Document.OutputSettings().charset("ISO-8859-1").escapeMode(Entities.EscapeMode.extended))
        assertEquals("&Gt; &Gg;&#x338; &Gt;&#x20d2; &Gt;&#x338; &Gt; &Gt;", escaped)
        assertEquals(un, Entities.unescape(escaped))
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
        assertEquals("≫⃒", Entities.getByName("nGt"))
        assertEquals("fj", Entities.getByName("fjlig"))
        assertEquals("≫", Entities.getByName("gg"))
        assertEquals("©", Entities.getByName("copy"))
    }

    @Test
    fun escapeSupplementaryCharacter() {
        val text = 135361.toCodePoint().toChars().concatToString()
        val escapedAscii = Entities.escape(text, Document.OutputSettings().charset("ISO-8859-1").escapeMode(Entities.EscapeMode.base))
        assertEquals("&#x210c1;", escapedAscii)
        val escapedUtf = Entities.escape(text, Document.OutputSettings().charset("UTF-8").escapeMode(Entities.EscapeMode.base))
        assertEquals(text, escapedUtf)
    }

    @Test
    fun notMissingMultis() {
        val text = "&nparsl;"
        val un = "\u2AFD\u20E5"
        assertEquals(un, Entities.unescape(text))
    }

    @Test
    fun notMissingSupplementals() {
        val text = "&npolint; &qfr;"
        val un = "⨔ \uD835\uDD2E" // 𝔮
        assertEquals(un, Entities.unescape(text))
    }

    @Test
    fun unescape() {
        val text =
            "Hello &AElig; &amp;&LT&gt; &reg &angst; &angst &#960; &#960 &#x65B0; there &! &frac34; &copy; &COPY;"
        assertEquals("Hello Æ &<> ® Å &angst π π 新 there &! ¾ © ©", Entities.unescape(text))
        assertEquals("&0987654321; &unknown", Entities.unescape("&0987654321; &unknown"))
    }

    @Test
    fun strictUnescape() { // for attributes, enforce strict unescaping (must look like &#xxx; , not just &#xxx)
        val text = "Hello &amp= &amp;"
        assertEquals("Hello &amp= &", Entities.unescape(text, true))
        assertEquals("Hello &= &", Entities.unescape(text))
        assertEquals("Hello &= &", Entities.unescape(text, false))
    }

    @Test
    fun caseSensitive() {
        val unescaped = "Ü ü & &"
        assertEquals(
            "Ü ü &amp; &amp;",
            Entities.escape(unescaped, Document.OutputSettings().charset("ISO-8859-1").escapeMode(Entities.EscapeMode.extended)),
        )
        val escaped = "&Uuml; &uuml; &amp; &AMP"
        assertEquals("Ü ü & &", Entities.unescape(escaped))
    }

    @Test
    fun quoteReplacements() {
        val escaped = "&#92; &#36;"
        val unescaped = "\\ $"
        assertEquals(unescaped, Entities.unescape(escaped))
    }

    @Test
    fun letterDigitEntities() {
        val html = "<p>&sup1;&sup2;&sup3;&frac14;&frac12;&frac34;</p>"
        val doc = parse(html)
        doc.outputSettings().charset("ISO-8859-1")
        val p = doc.select("p").first()
        assertEquals("¹²³¼½¾", p!!.html())
        assertEquals("¹²³¼½¾", p.text())
        doc.outputSettings().charset("UTF-8")
        assertEquals("¹²³¼½¾", p.html())
    }

    @Test
    fun noSpuriousDecodes() {
        val string = "http://www.foo.com?a=1&num_rooms=1&children=0&int=VA&b=2"
        assertEquals(string, Entities.unescape(string))
    }

    @Test
    fun escapesGtInXmlAttributesButNotInHtml() {
        //< is OK in HTML attribute values, but not in XML
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
        // we escape ascii control characters in both HTML and XML for compatibility. Required in XML and probably
        // easier to read in HTML
        val input = "<a foo=\"&#x1b;esc&#x7;bell\">Text &#x1b; &#x7;</a>"
        val doc = parse(input)
        assertEquals(input, doc.body().html())
        val xml = parse(html = input, baseUri = "", parser = Parser.xmlParser())
        assertEquals(input, xml.html())
    }

    @Test
    fun escapeByClonedOutputSettings() {
        val outputSettings = Document.OutputSettings()
        val text = "Hello &<> Å å π 新 there ¾ © »"
        val clone1 = outputSettings.clone()
        val clone2 = outputSettings.clone()
        val escaped1 = Entities.escape(text, clone1)
        val escaped2 = Entities.escape(text, clone2)
        assertEquals(escaped1, escaped2)
    }

    @Test
    fun parseHtmlEncodedEmojiMultipoint() {
        val emoji = Parser.unescapeEntities("&#55357;&#56495;", false) // 💯
        assertEquals("\uD83D\uDCAF", emoji)
    }

    @Test
    fun parseHtmlEncodedEmoji() {
        val emoji = Parser.unescapeEntities("&#128175;", false) // 💯
        assertEquals("\uD83D\uDCAF", emoji)
    }
}
