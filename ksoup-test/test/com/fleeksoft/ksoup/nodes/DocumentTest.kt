package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.Platform
import com.fleeksoft.ksoup.TestHelper
import com.fleeksoft.ksoup.isJS
import com.fleeksoft.ksoup.parser.ParseSettings
import com.fleeksoft.ksoup.parser.Parser
import korlibs.io.lang.Charset
import korlibs.io.lang.toByteArray
import korlibs.io.stream.openSync
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Tests for Document.
 *
 * @author Sabeeh, fleeksoft@gmail.com
 */
class DocumentTest {

    @Test
    fun setTextPreservesDocumentStructure() {
        val doc = Ksoup.parse("<p>Hello</p>")
        doc.text("Replaced")
        assertEquals("Replaced", doc.text())
        assertEquals("Replaced", doc.body().text())
        assertEquals(1, doc.select("head").size)
    }

    @Test
    fun testTitles() {
        val noTitle = Ksoup.parse("<p>Hello</p>")
        val withTitle = Ksoup.parse("<title>First</title><title>Ignore</title><p>Hello</p>")
        assertEquals("", noTitle.title())
        noTitle.title("Hello")
        assertEquals("Hello", noTitle.title())
        assertEquals("Hello", noTitle.select("title").first()!!.text())
        assertEquals("First", withTitle.title())
        withTitle.title("Hello")
        assertEquals("Hello", withTitle.title())
        assertEquals("Hello", withTitle.select("title").first()!!.text())
        val normaliseTitle = Ksoup.parse("<title>   Hello\nthere   \n   now   \n")
        assertEquals("Hello there now", normaliseTitle.title())
    }

    @Test
    fun testOutputEncoding() {
        val doc = Ksoup.parse("<p title=π>π & < > </p>")
        // default is utf-8
        assertEquals("<p title=\"π\">π &amp; &lt; &gt;</p>", doc.body().html())
        assertEquals("UTF-8", doc.outputSettings().charset().name.uppercase())

        doc.outputSettings().charset("ISO-8859-1")
        assertEquals(Entities.EscapeMode.base, doc.outputSettings().escapeMode())
        assertEquals("<p title=\"&#x3c0;\">&#x3c0; &amp; &lt; &gt;</p>", doc.body().html())

        doc.outputSettings().escapeMode(Entities.EscapeMode.extended)
        assertEquals("<p title=\"&pi;\">&pi; &amp; &lt; &gt;</p>", doc.body().html())
    }

    @Test
    fun testXhtmlReferences() {
        val doc = Ksoup.parse("&lt; &gt; &amp; &quot; &apos; &times;")
        doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml)
        assertEquals("&lt; &gt; &amp; \" ' ×", doc.body().html())
    }

    @Test
    fun testNormalisesStructure() {
        val doc =
            Ksoup.parse("<html><head><script>one</script><noscript><p>two</p></noscript></head><body><p>three</p></body><p>four</p></html>")
        assertEquals(
            "<html><head><script>one</script><noscript>&lt;p&gt;two</noscript></head><body><p>three</p><p>four</p></body></html>",
            com.fleeksoft.ksoup.TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun accessorsWillNormalizeStructure() {
        val doc = Document("")
        assertEquals("", doc.html())
        val body = doc.body()
        assertEquals("body", body.tagName())
        val head = doc.head()
        assertEquals("head", head.tagName())
        assertEquals(
            "<html><head></head><body></body></html>",
            com.fleeksoft.ksoup.TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun accessorsAreCaseInsensitive() {
        val parser = Parser.htmlParser().settings(ParseSettings.preserveCase)
        val doc =
            parser.parseInput(
                "<!DOCTYPE html><HTML><HEAD><TITLE>SHOUTY</TITLE></HEAD><BODY>HELLO</BODY></HTML>",
                "",
            )
        val body = doc.body()
        assertEquals("BODY", body.tagName())
        assertEquals("body", body.normalName())
        val head = doc.head()
        assertEquals("HEAD", head.tagName())
        assertEquals("body", body.normalName())
        val root = doc.selectFirst("html")
        assertEquals("HTML", root!!.tagName())
        assertEquals("html", root.normalName())
        assertEquals("SHOUTY", doc.title())
    }

    @Test
    fun testClone() {
        val doc = Ksoup.parse("<title>Hello</title> <p>One<p>Two")
        val clone = doc.clone()
        assertEquals(
            "<html><head><title>Hello</title></head><body><p>One</p><p>Two</p></body></html>",
            com.fleeksoft.ksoup.TextUtil.stripNewlines(clone.html()),
        )
        clone.title("Hello there")
        clone.expectFirst("p").text("One more").attr("id", "1")
        assertEquals(
            "<html><head><title>Hello there</title></head><body><p id=\"1\">One more</p><p>Two</p></body></html>",
            com.fleeksoft.ksoup.TextUtil.stripNewlines(clone.html()),
        )
        assertEquals(
            "<html><head><title>Hello</title></head><body><p>One</p><p>Two</p></body></html>",
            com.fleeksoft.ksoup.TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun testBasicIndent() {
        val doc = Ksoup.parse("<title>Hello</title> <p>One<p>Two")
        val expect =
            "<html>\n <head>\n  <title>Hello</title>\n </head>\n <body>\n  <p>One</p>\n  <p>Two</p>\n </body>\n</html>"
        assertEquals(expect, doc.html())
    }

    @Test
    fun testClonesDeclarations() {
        val doc = Ksoup.parse("<!DOCTYPE html><html><head><title>Doctype test")
        val clone = doc.clone()
        assertEquals(doc.html(), clone.html())
        assertEquals(
            "<!doctype html><html><head><title>Doctype test</title></head><body></body></html>",
            com.fleeksoft.ksoup.TextUtil.stripNewlines(clone.html()),
        )
    }

    @Test
    fun testLocation() = runTest {
        if (Platform.isJS()) {
//            js resource access issue
            return@runTest
        }
        // tests location vs base href
        val `in`: String = TestHelper.getResourceAbsolutePath("htmltests/basehref.html")
        val doc: Document = Ksoup.parseFile(
            filePath = `in`,
            baseUri = "http://example.com/",
            charsetName = "UTF-8",
        )
        val location = doc.location()
        val baseUri = doc.baseUri()
        assertEquals("http://example.com/", location)
        assertEquals("https://example.com/path/file.html?query", baseUri)
        assertEquals("./anotherfile.html", doc.expectFirst("a").attr("href"))
        assertEquals(
            "https://example.com/path/anotherfile.html",
            doc.expectFirst("a").attr("abs:href"),
        )
    }

    @Test
    fun testLocationFromString() {
        val doc = Ksoup.parse("<p>Hello")
        assertEquals("", doc.location())
    }

    @Test
    fun testHtmlAndXmlSyntax() {
        val h = "<!DOCTYPE html><body><img async checked='checked' src='&<>\"'>&lt;&gt;&amp;&quot;<foo />bar"
        val doc = Ksoup.parse(h)
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.html)
        assertEquals(
            """<!doctype html>
<html>
 <head></head>
 <body>
  <img async checked src="&amp;<>&quot;">&lt;&gt;&amp;"<foo />bar
 </body>
</html>""",
            doc.html(),
        )
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml)
        assertEquals(
            """<!DOCTYPE html>
<html>
 <head></head>
 <body>
  <img async="" checked="checked" src="&amp;&lt;>&quot;" />&lt;&gt;&amp;"<foo />bar
 </body>
</html>""",
            doc.html(),
        )
    }

    @Test
    fun htmlParseDefaultsToHtmlOutputSyntax() {
        val doc = Ksoup.parse("x")
        assertEquals(Document.OutputSettings.Syntax.html, doc.outputSettings().syntax())
    }

    @Test
    fun testHtmlAppendable() {
        val htmlContent =
            "<html><head><title>Hello</title></head><body><p>One</p><p>Two</p></body></html>"
        val document = Ksoup.parse(htmlContent)
        val outputSettings = Document.OutputSettings()
        outputSettings.prettyPrint(false)
        document.outputSettings(outputSettings)
        assertEquals(htmlContent, document.html(StringBuilder()).toString())
    }

    @Test
    fun testOverflowClone() {
        if (Platform.isJS()) {
            // FIXME: timeout error for js
            return
        }

        val sb = StringBuilder()
        sb.append("<head><base href='https://ksoup.org/'>")
        for (i in 0..99999) {
            sb.append("<div>")
        }
        sb.append("<p>Hello <a href='/example.html'>there</a>")
        val doc = Ksoup.parse(sb.toString())
        val expectedLink = "https://ksoup.org/example.html"
        assertEquals(expectedLink, doc.selectFirst("a")!!.attr("abs:href"))
        val clone = doc.clone()
        doc.hasSameValue(clone)
        assertEquals(expectedLink, clone.selectFirst("a")!!.attr("abs:href"))
    }

    @Test
    fun testDocumentsWithSameContentAreEqual() {
        val docA = Ksoup.parse("<div/>One")
        val docB = Ksoup.parse("<div/>One")
        val docC = Ksoup.parse("<div/>Two")
        assertNotEquals(docA, docB)
        assertEquals(docA, docA)
        assertEquals(docA.hashCode(), docA.hashCode())
        assertNotEquals(docA.hashCode(), docC.hashCode())
    }

    @Test
    fun testDocumentsWithSameContentAreVerifiable() {
        val docA = Ksoup.parse("<div/>One")
        val docB = Ksoup.parse("<div/>One")
        val docC = Ksoup.parse("<div/>Two")
        assertTrue(docA.hasSameValue(docB))
        assertFalse(docA.hasSameValue(docC))
    }

    @Test
    fun testMetaCharsetUpdateUtf8() {
        val doc = createHtmlDocument("changeThis")
        doc.updateMetaCharsetElement(true)
        doc.charset(Charset.forName(charsetUtf8))
        val htmlCharsetUTF8 = """<html>
 <head>
  <meta charset="$charsetUtf8">
 </head>
 <body></body>
</html>"""
        assertEquals(htmlCharsetUTF8, doc.toString())
        val selectedElement = doc.select("meta[charset]").first()
        assertEquals(charsetUtf8, doc.charset().name.uppercase())
        assertEquals(charsetUtf8, selectedElement!!.attr("charset"))
        assertEquals(doc.charset(), doc.outputSettings().charset())
    }

    @Test
    fun testMetaCharsetUpdateIso8859() {
        val doc = createHtmlDocument("changeThis")
        doc.updateMetaCharsetElement(true)
        doc.charset(Charset.forName(charsetIso8859))
        val htmlCharsetISO = """<html>
 <head>
  <meta charset="$charsetIso8859">
 </head>
 <body></body>
</html>"""
        assertEquals(htmlCharsetISO, doc.toString())
        val selectedElement = doc.select("meta[charset]").first()
        assertEquals(charsetIso8859, doc.charset().name.uppercase())
        assertEquals(charsetIso8859, selectedElement!!.attr("charset"))
        assertEquals(doc.charset(), doc.outputSettings().charset())
    }

    @Test
    fun testMetaCharsetUpdateNoCharset() {
        val docNoCharset = Document.createShell("")
        docNoCharset.updateMetaCharsetElement(true)
        docNoCharset.charset(Charset.forName(charsetUtf8))
        assertEquals(
            charsetUtf8,
            docNoCharset.select("meta[charset]").first()!!
                .attr("charset"),
        )
        val htmlCharsetUTF8 = """<html>
 <head>
  <meta charset="$charsetUtf8">
 </head>
 <body></body>
</html>"""
        assertEquals(htmlCharsetUTF8, docNoCharset.toString())
    }

    @Test
    fun testMetaCharsetUpdateDisabled() {
        val docDisabled = Document.createShell("")
        val htmlNoCharset = """<html>
 <head></head>
 <body></body>
</html>"""
        assertEquals(htmlNoCharset, docDisabled.toString())
        assertNull(docDisabled.select("meta[charset]").first())
    }

    @Test
    fun testMetaCharsetUpdateDisabledNoChanges() {
        val doc = createHtmlDocument("dontTouch")
        val htmlCharset = """<html>
 <head>
  <meta charset="dontTouch">
  <meta name="charset" content="dontTouch">
 </head>
 <body></body>
</html>"""
        assertEquals(htmlCharset, doc.toString())
        var selectedElement = doc.select("meta[charset]").first()
        assertNotNull(selectedElement)
        assertEquals("dontTouch", selectedElement.attr("charset"))
        selectedElement = doc.select("meta[name=charset]").first()
        assertNotNull(selectedElement)
        assertEquals("dontTouch", selectedElement.attr("content"))
    }

    @Test
    fun testMetaCharsetUpdateEnabledAfterCharsetChange() {
        val doc = createHtmlDocument("dontTouch")
        doc.charset(Charset.forName(charsetUtf8))
        val selectedElement = doc.select("meta[charset]").first()
        assertEquals(charsetUtf8, selectedElement!!.attr("charset"))
        assertTrue(doc.select("meta[name=charset]").isEmpty())
    }

    @Test
    fun testMetaCharsetUpdateCleanup() {
        val doc = createHtmlDocument("dontTouch")
        doc.updateMetaCharsetElement(true)
        doc.charset(Charset.forName(charsetUtf8))
        val htmlCharsetUTF8 = """<html>
 <head>
  <meta charset="$charsetUtf8">
 </head>
 <body></body>
</html>"""
        assertEquals(htmlCharsetUTF8, doc.toString())
    }

    @Test
    fun testMetaCharsetUpdateXmlUtf8() {
        val doc = createXmlDocument("1.0", "changeThis", true)
        doc.updateMetaCharsetElement(true)
        doc.charset(Charset.forName(charsetUtf8))
        val xmlCharsetUTF8 = """<?xml version="1.0" encoding="$charsetUtf8"?>
<root>
 node
</root>"""
        assertEquals(xmlCharsetUTF8, doc.toString())
        val selectedNode = doc.childNode(0) as XmlDeclaration
        assertEquals(charsetUtf8, doc.charset().name.uppercase())
        assertEquals(charsetUtf8, selectedNode.attr("encoding"))
        assertEquals(doc.charset(), doc.outputSettings().charset())
    }

    @Test
    fun testMetaCharsetUpdateXmlIso8859() {
        val doc = createXmlDocument("1.0", "changeThis", true)
        doc.updateMetaCharsetElement(true)
        doc.charset(Charset.forName(charsetIso8859))
        val xmlCharsetISO = """<?xml version="1.0" encoding="$charsetIso8859"?>
<root>
 node
</root>"""
        assertEquals(xmlCharsetISO, doc.toString())
        val selectedNode = doc.childNode(0) as XmlDeclaration
        assertEquals(charsetIso8859, doc.charset().name.uppercase())
        assertEquals(charsetIso8859, selectedNode.attr("encoding"))
        assertEquals(doc.charset(), doc.outputSettings().charset())
    }

    @Test
    fun testMetaCharsetUpdateXmlNoCharset() {
        val doc = createXmlDocument("1.0", "none", false)
        doc.updateMetaCharsetElement(true)
        doc.charset(Charset.forName(charsetUtf8))
        val xmlCharsetUTF8 = """<?xml version="1.0" encoding="$charsetUtf8"?>
<root>
 node
</root>"""
        assertEquals(xmlCharsetUTF8, doc.toString())
        val selectedNode = doc.childNode(0) as XmlDeclaration
        assertEquals(charsetUtf8, selectedNode.attr("encoding"))
    }

    @Test
    fun testMetaCharsetUpdateXmlDisabled() {
        val doc = createXmlDocument("none", "none", false)
        val xmlNoCharset = """<root>
 node
</root>"""
        assertEquals(xmlNoCharset, doc.toString())
    }

    @Test
    fun testMetaCharsetUpdateXmlDisabledNoChanges() {
        val doc = createXmlDocument("dontTouch", "dontTouch", true)
        val xmlCharset = """<?xml version="dontTouch" encoding="dontTouch"?>
<root>
 node
</root>"""
        assertEquals(xmlCharset, doc.toString())
        val selectedNode = doc.childNode(0) as XmlDeclaration
        assertEquals("dontTouch", selectedNode.attr("encoding"))
        assertEquals("dontTouch", selectedNode.attr("version"))
    }

    @Test
    fun testMetaCharsetUpdatedDisabledPerDefault() {
        val doc = createHtmlDocument("none")
        assertFalse(doc.updateMetaCharsetElement())
    }

    private fun createHtmlDocument(charset: String): Document {
        val doc = Document.createShell("")
        doc.head().appendElement("meta").attr("charset", charset)
        doc.head().appendElement("meta").attr("name", "charset").attr("content", charset)
        return doc
    }

    private fun createXmlDocument(
        version: String,
        charset: String,
        addDecl: Boolean,
    ): Document {
        val doc = Document("")
        doc.appendElement("root").text("node")
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml)
        if (addDecl) {
            val decl = XmlDeclaration("xml", false)
            decl.attr("version", version)
            decl.attr("encoding", charset)
            doc.prependChild(decl)
        }
        return doc
    }

    @Test
    fun testShiftJisRoundtrip() {
        if (Platform.isJS()) {
            // Shift_JIS not supported
            return
        }
        val input = (
                "<html>" +
                        "<head>" +
                        "<meta http-equiv=\"content-type\" content=\"text/html; charset=Shift_JIS\" />" +
                        "</head>" +
                        "<body>" +
                        "before&nbsp;after" +
                        "</body>" +
                        "</html>"
                )
        val inputStream = input.encodeToByteArray().openSync()
        val doc: Document = Ksoup.parse(syncStream = inputStream, baseUri = "http://example.com", charsetName = null)
        doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml)
        val output = doc.html().toByteArray(doc.outputSettings().charset()).decodeToString()
        assertFalse(output.contains("?"), "Should not have contained a '?'.")
        assertTrue(
            output.contains("&#xa0;") || output.contains("&nbsp;"),
            "Should have contained a '&#xa0;' or a '&nbsp;'.",
        )
    }

    @Test
    fun testDocumentTypeGet() {
        val html = "\n\n<!-- comment -->  <!doctype html><p>One</p>"
        val doc = Ksoup.parse(html)
        val documentType = doc.documentType()
        assertNotNull(documentType)
        assertEquals("html", documentType.name())
    }

    @Test
    fun framesetSupportsBodyMethod() {
        val html =
            "<html><head><title>Frame Test</title></head><frameset id=id><frame src=foo.html></frameset>"
        val doc = Ksoup.parse(html)
        val head = doc.head()
        assertNotNull(head)
        assertEquals("Frame Test", doc.title())

        // Frameset docs per html5 spec have no body element - but instead a frameset elelemt
        assertNull(doc.selectFirst("body"))
        val frameset = doc.selectFirst("frameset")
        assertNotNull(frameset)

        // the body() method returns body or frameset and does not otherwise modify the document
        // doing it in body() vs parse keeps the html close to original for round-trip option
        val body = doc.body()
        assertNotNull(body)
        assertSame(frameset, body)
        assertEquals("frame", body.child(0).tagName())
        assertNull(doc.selectFirst("body")) // did not vivify a body element
        val expected = """<html>
 <head>
  <title>Frame Test</title>
 </head>
 <frameset id="id">
  <frame src="foo.html">
 </frameset>
</html>"""
        assertEquals(expected, doc.html())
    }

    @Test
    fun forms() {
        val html = "<body><form id=1><input name=foo></form><form id=2><input name=bar>"
        val doc = Ksoup.parse(html)
        val forms = doc.forms()
        assertEquals(2, forms.size)
        val form = forms[1]
        assertEquals(1, form.elements().size)
        assertEquals("bar", form.elements().first()!!.attr("name"))
        val emptyHtml = "<body>"
        val emptyDoc = Ksoup.parse(emptyHtml)
        assertEquals(0, emptyDoc.forms().size)
    }

    @Test
    fun expectForm() {
        val html =
            "<body><div name=form></div><form id=1 name=form><input name=foo></form><form id=2><input name=bar>"
        val doc = Ksoup.parse(html)

        // test finds first <form>
        val formEl1 = doc.expectForm("[name=form]")
        assertEquals("1", formEl1!!.id()) // and not the div
        val formEl2 = doc.expectForm("form")
        assertEquals("1", formEl2!!.id())
        val formEl3 = doc.expectForm("form:has([name=bar])")
        assertEquals("2", formEl3!!.id())
        var threw = false
        try {
            val nix = doc.expectForm("div")
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    companion object {
        private const val charsetUtf8 = "UTF-8"
        private const val charsetIso8859 = "ISO-8859-1"
    }
}
