package com.fleeksoft.ksoup.parser

import com.fleeksoft.charset.Charsets
import com.fleeksoft.io.byteInputStream
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.TextUtil
import com.fleeksoft.ksoup.nodes.*
import com.fleeksoft.ksoup.nodes.Document.OutputSettings.Syntax
import com.fleeksoft.ksoup.parseInput
import kotlinx.coroutines.test.runTest
import kotlin.test.*


/**
 * Tests XmlTreeBuilder.
 *
 */
class XmlTreeBuilderTest {

    @Test
    fun testSimpleXmlParse() {
        val xml = "<doc id=2 href='/bar'>Foo <br /><link>One</link><link>Two</link></doc>"
        val tb = XmlTreeBuilder()
        val doc = tb.parse(input = xml, baseUri = "http://foo.com/")
        assertEquals(
            "<doc id=\"2\" href=\"/bar\">Foo <br /><link>One</link><link>Two</link></doc>",
            TextUtil.stripNewlines(doc.html()),
        )
        assertEquals(doc.getElementById("2")!!.absUrl("href"), "http://foo.com/bar")
    }

    @Test
    fun testPopToClose() {
        // test: </val> closes Two, </bar> ignored
        val xml = "<doc><val>One<val>Two</val></bar>Three</doc>"
        val tb = XmlTreeBuilder()
        val doc = tb.parse(xml, "http://foo.com/")
        assertEquals(
            "<doc><val>One<val>Two</val>Three</val></doc>",
            TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun testCommentAndDocType() {
        val xml = "<!DOCTYPE HTML><!-- a comment -->One <qux />Two"
        val tb = XmlTreeBuilder()
        val doc = tb.parse(xml, "http://foo.com/")
        assertEquals(
            "<!DOCTYPE HTML><!-- a comment -->One <qux />Two",
            TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun testSupplyParserToKsoupClass() {
        val xml = "<doc><val>One<val>Two</val></bar>Three</doc>"
        val doc = Ksoup.parse(html = xml, baseUri = "http://foo.com/", parser = Parser.xmlParser())
        assertEquals(
            "<doc><val>One<val>Two</val>Three</val></doc>",
            TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun testSupplyParserToDataStream() = runTest {
        val xmlTest = """<doc><val>One<val>Two</val>Three</val></doc>"""
        val doc = Ksoup.parseInput(
            input = xmlTest.byteInputStream(),
            baseUri = "http://foo.com",
            charsetName = null,
            parser = Parser.xmlParser(),
        )
        assertEquals(
            "<doc><val>One<val>Two</val>Three</val></doc>",
            TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun testDoesNotForceSelfClosingKnownTags() {
        // html will force "<br>one</br>" to logically "<br />One<br />". XML should be stay "<br>one</br> -- don't recognise tag.
        val htmlDoc = Ksoup.parse("<br>one</br>")
        assertEquals("<br>\none\n<br>", htmlDoc.body().html())
        val xmlDoc = Ksoup.parse(html = "<br>one</br>", baseUri = "", parser = Parser.xmlParser())
        assertEquals("<br>one</br>", xmlDoc.html())
    }

    @Test
    fun handlesXmlDeclarationAsDeclaration() {
        val html = "<?xml encoding='UTF-8' ?><body>One</body><!-- comment -->"
        val doc = Ksoup.parse(html = html, baseUri = "", parser = Parser.xmlParser())
        assertEquals("<?xml encoding=\"UTF-8\"?><body>One</body><!-- comment -->", doc.outerHtml())
        assertEquals("#declaration", doc.childNode(0).nodeName())
        assertEquals("#comment", doc.childNode(2).nodeName())
    }

    @Test
    fun xmlFragment() {
        val xml = "<one src='/foo/' />Two<three><four /></three>"
        val nodes = Parser.parseXmlFragment(fragmentXml = xml, baseUri = "http://example.com/")
        assertEquals(3, nodes.size)
        assertEquals(expected = "http://example.com/foo/", actual = nodes[0].absUrl("src"))
        assertEquals(expected = "one", actual = nodes[0].nodeName())
        assertEquals(expected = "Two", actual = (nodes[1] as TextNode).text())
    }

    @Test
    fun xmlParseDefaultsToHtmlOutputSyntax() {
        val doc = Ksoup.parse(html = "x", baseUri = "", parser = Parser.xmlParser())
        assertEquals(Syntax.xml, doc.outputSettings().syntax())
    }

    @Test
    fun testDoesHandleEOFInTag() {
        val html = "<img src=asdf onerror=\"alert(1)\" x="
        val xmlDoc = Ksoup.parse(html = html, baseUri = "", parser = Parser.xmlParser())
        assertEquals("<img src=\"asdf\" onerror=\"alert(1)\" x=\"\"></img>", xmlDoc.html())
    }

    @Test
    fun testDetectCharsetEncodingDeclaration() = runTest {
        val xml = """
            <?xml version="1.0" encoding="ISO-8859-1"?>
            <data>äöåéü</data>
        """.trimIndent()
        val doc = Ksoup.parseInput(
            input = xml.byteInputStream(Charsets.forName("ISO-8859-1")),
            baseUri = "http://example.com/",
            charsetName = null,
            parser = Parser.xmlParser()
        )
        assertEquals("ISO-8859-1", doc.charset().name().uppercase())
        assertEquals(
            "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><data>äöåéü</data>",
            TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun testParseDeclarationAttributes() {
        val xml = "<?xml version='1' encoding='UTF-8' something='else'?><val>One</val>"
        val doc = Ksoup.parse(html = xml, baseUri = "", parser = Parser.xmlParser())
        val decl = doc.childNode(0) as XmlDeclaration
        assertEquals("1", decl.attr("version"))
        assertEquals("UTF-8", decl.attr("encoding"))
        assertEquals("else", decl.attr("something"))
        assertEquals("version=\"1\" encoding=\"UTF-8\" something=\"else\"", decl.getWholeDeclaration())
        assertEquals(
            "<?xml version=\"1\" encoding=\"UTF-8\" something=\"else\"?>",
            decl.outerHtml(),
        )
    }

    @Test
    fun testParseDeclarationWithoutAttributes() {
        val xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<?myProcessingInstruction My Processing instruction.?>"
        val doc = Ksoup.parse(html = xml, baseUri = "", parser = Parser.xmlParser())
        val decl = doc.childNode(2) as XmlDeclaration
        assertEquals("myProcessingInstruction", decl.name())
        assertTrue(decl.hasAttr("My"))
        assertEquals("<?myProcessingInstruction My Processing instruction.?>", decl.outerHtml())
    }

    @Test
    fun caseSensitiveDeclaration() {
        val xml = "<?XML version='1' encoding='UTF-8' something='else'?>"
        val doc = Ksoup.parse(html = xml, baseUri = "", parser = Parser.xmlParser())
        assertEquals("<?XML version=\"1\" encoding=\"UTF-8\" something=\"else\"?>", doc.outerHtml())
    }

    @Test
    fun testCreatesValidProlog() {
        val document = Document.createShell("")
        document.outputSettings().syntax(Syntax.xml)
        document.charset(Charsets.UTF8)
        assertEquals(
            """<?xml version="1.0" encoding="UTF-8"?>
<html>
 <head></head>
 <body></body>
</html>""",
            document.outerHtml(),
        )
    }

    @Test
    fun preservesCaseByDefault() {
        val xml = "<CHECK>One</CHECK><TEST ID=1>Check</TEST>"
        val doc = Ksoup.parse(html = xml, baseUri = "", parser = Parser.xmlParser())
        assertEquals(
            "<CHECK>One</CHECK><TEST ID=\"1\">Check</TEST>",
            TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun appendPreservesCaseByDefault() {
        val xml = "<One>One</One>"
        val doc = Ksoup.parse(html = xml, baseUri = "", parser = Parser.xmlParser())
        val one = doc.select("One")
        one.append("<Two ID=2>Two</Two>")
        assertEquals("<One>One<Two ID=\"2\">Two</Two></One>", TextUtil.stripNewlines(doc.html()))
    }

    @Test
    fun disablesPrettyPrintingByDefault() {
        val xml = "\n\n<div><one>One</one><one>\n Two</one>\n</div>\n "
        val doc = Ksoup.parse(html = xml, baseUri = "", parser = Parser.xmlParser())
        assertEquals(xml, doc.html())
    }

    @Test
    fun canNormalizeCase() {
        val xml = "<TEST ID=1>Check</TEST>"
        val doc = Ksoup.parse(html = xml, baseUri = "", parser = Parser.xmlParser().settings(ParseSettings.htmlDefault))
        assertEquals("<test id=\"1\">Check</test>", TextUtil.stripNewlines(doc.html()))
    }

    @Test
    fun normalizesDiscordantTags() {
        val parser = Parser.xmlParser().settings(ParseSettings.htmlDefault)
        val document = Ksoup.parse(html = "<div>test</DIV><p></p>", baseUri = "", parser = parser)
        assertEquals("<div>test</div><p></p>", document.html())
        // was failing -> toString() = "<div>\n test\n <p></p>\n</div>"
    }

    @Test
    fun roundTripsCdata() {
        val xml = "<div id=1><![CDATA[\n<html>\n <foo><&amp;]]></div>"
        val doc = Ksoup.parse(html = xml, baseUri = "", parser = Parser.xmlParser())
        val div = doc.getElementById("1")
        assertEquals("<html>\n <foo><&amp;", div!!.text())
        assertEquals(0, div.children().size)
        assertEquals(1, div.childNodeSize()) // no elements, one text node
        assertEquals("<div id=\"1\"><![CDATA[\n<html>\n <foo><&amp;]]></div>", div.outerHtml())
        val cdata = div.textNodes()[0] as CDataNode
        assertEquals("\n<html>\n <foo><&amp;", cdata.text())
    }

    @Test
    fun cdataPreservesWhiteSpace() {
        val xml = "<script type=\"text/javascript\">//<![CDATA[\n\n  foo();\n//]]></script>"
        val doc = Ksoup.parse(html = xml, baseUri = "", parser = Parser.xmlParser())
        assertEquals(xml, doc.outerHtml())
        assertEquals("//\n\n  foo();\n//", doc.selectFirst("script")!!.text())
    }

    @Test
    fun handlesDodgyXmlDecl() {
        val xml = "<?xml version='1.0'><val>One</val>"
        val doc = Ksoup.parse(html = xml, baseUri = "", parser = Parser.xmlParser())
        assertEquals("One", doc.select("val").text())
    }

    @Test
    fun handlesLTinScript() {
        val html = "<script> var a=\"<?\"; var b=\"?>\"; </script>"
        val doc = Ksoup.parse(html = html, baseUri = "", parser = Parser.xmlParser())
        assertEquals(
            "<script> var a=\"<!--?\"; var b=\"?-->\"; </script>",
            doc.html()
        ) // converted from pseudo xmldecl to comment
    }

    @Test
    fun dropsDuplicateAttributes() {
        // case sensitive, so should drop Four and Five
        val html =
            "<p One=One ONE=Two one=Three One=Four ONE=Five two=Six two=Seven Two=Eight>Text</p>"
        val parser = Parser.xmlParser().setTrackErrors(10)
        val doc = parser.parseInput(html, "")
        assertEquals(
            "<p One=\"One\" ONE=\"Two\" one=\"Three\" two=\"Six\" Two=\"Eight\">Text</p>",
            doc.selectFirst("p")!!
                .outerHtml(),
        )
    }

    @Test
    fun readerClosedAfterParse() {
        val doc = Ksoup.parse(html = "Hello", baseUri = "", parser = Parser.xmlParser())
        val treeBuilder = doc.parser()!!.getTreeBuilder()
        assertTrue(treeBuilder.reader.isClosed())
        assertNull(treeBuilder.tokeniser)
    }

    @Test
    fun xmlParserEnablesXmlOutputAndEscapes() {
        // Test that when using the XML parser, the output mode and escape mode default to XHTML entities
        val doc = Ksoup.parse(html = "<p one='&lt;two&gt;&copy'>Three</p>", baseUri = "", parser = Parser.xmlParser())
        assertEquals(doc.outputSettings().syntax(), Syntax.xml)
        assertEquals(doc.outputSettings().escapeMode(), Entities.EscapeMode.xhtml)
        assertEquals("<p one=\"&lt;two>©\">Three</p>", doc.html()) // only the < should be escaped
    }

    @Test
    fun xmlSyntaxEscapesLtInAttributes() {
        // Regardless of the entity escape mode, make sure < is escaped in attributes when in XML
        val doc = Ksoup.parse(html = "<p one='&lt;two&gt;&copy'>Three</p>", baseUri = "", parser = Parser.xmlParser())
        doc.outputSettings().escapeMode(Entities.EscapeMode.extended)
        doc.outputSettings().charset("ISO-8859-1") // to make sure &copy; is output
        assertEquals(doc.outputSettings().syntax(), Syntax.xml)
        assertEquals("<p one=\"&lt;two>©\">Three</p>", doc.html())
    }

    @Test
    fun xmlOutputCorrectsInvalidAttributeNames() {
        val xml = "<body style=\"color: red\" \" name\"><div =\"\"></div></body>"
        val doc = Ksoup.parse(xml, Parser.xmlParser())
        assertEquals(Syntax.xml, doc.outputSettings().syntax())
        val out = doc.html()
        assertEquals("<body style=\"color: red\" _=\"\" name_=\"\"><div _=\"\"></div></body>", out)
    }

    @Test
    fun xmlValidAttributes() {
        val xml = "<a bB1-_:.=foo _9!=bar xmlns:p1=qux>One</a>"
        val doc = Ksoup.parse(xml, Parser.xmlParser())
        assertEquals(Syntax.xml, doc.outputSettings().syntax())

        val out = doc.html()
        assertEquals("<a bB1-_:.=\"foo\" _9_=\"bar\" xmlns:p1=\"qux\">One</a>", out) // first is same, second coerced
    }

    @Test
    fun customTagsAreFlyweights() {
        val xml = "<foo>Foo</foo><foo>Foo</foo><FOO>FOO</FOO><FOO>FOO</FOO>"
        val doc = Ksoup.parse(xml, Parser.xmlParser())
        val els = doc.children()
        val t1 = els[0].tag()
        val t2 = els[1].tag()
        val t3 = els[2].tag()
        val t4 = els[3].tag()
        assertEquals("foo", t1.tagName)
        assertEquals("FOO", t3.tagName)
        assertSame(t1, t2)
        assertSame(t3, t4)
    }

    @Test
    fun rootHasXmlSettings() {
        val doc = Ksoup.parse("<foo>", Parser.xmlParser())
        val settings = doc.parser()!!.settings()
        assertTrue(settings!!.preserveTagCase())
        assertTrue(settings.preserveAttributeCase())
        assertEquals(Parser.NamespaceXml, doc.parser()!!.defaultNamespace())
    }

    @Test
    fun xmlNamespace() {
        val xml = "<foo><bar><div><svg><math>Qux</bar></foo>"
        val doc = Ksoup.parse(xml, Parser.xmlParser())
        assertXmlNamespace(doc)
        val els = doc.select("*")
        for (el in els) {
            assertXmlNamespace(el)
        }
        val clone = doc.clone()
        assertXmlNamespace(clone)
        assertXmlNamespace(clone.expectFirst("bar"))
        val shallow = doc.shallowClone()
        assertXmlNamespace(shallow)
    }


    @Test
    fun declarations() {
        val xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><!DOCTYPE html\n" +
                "  PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
                "  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" +
                "<!ELEMENT footnote (#PCDATA|a)*>"
        val doc: Document = Ksoup.parse(xml, Parser.xmlParser())

        val proc = doc.childNode(0) as XmlDeclaration
        val doctype = doc.childNode(1) as DocumentType
        val decl = doc.childNode(2) as XmlDeclaration

        assertEquals("xml", proc.name())
        assertEquals("1.0", proc.attr("version"))
        assertEquals("utf-8", proc.attr("encoding"))
        assertEquals("version=\"1.0\" encoding=\"utf-8\"", proc.getWholeDeclaration())
        assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>", proc.outerHtml())

        assertEquals("html", doctype.name())
        assertEquals("-//W3C//DTD XHTML 1.0 Transitional//EN", doctype.attr("publicId"))
        assertEquals("http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd", doctype.attr("systemId"))
        assertEquals(
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">",
            doctype.outerHtml()
        )

        assertEquals("ELEMENT", decl.name())
        assertEquals("footnote (#PCDATA|a)*", decl.getWholeDeclaration())
        assertTrue(decl.hasAttr("footNote"))
        assertFalse(decl.hasAttr("ELEMENT"))
        assertEquals("<!ELEMENT footnote (#PCDATA|a)*>", decl.outerHtml())

        assertEquals(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" +
                    "<!ELEMENT footnote (#PCDATA|a)*>", doc.outerHtml()
        )
    }

    @Test
    fun declarationWithGt() {
        // https://github.com/jhy/Ksoup/issues/1947
        val xml = "<x><?xmlDeclaration att1=\"value1\" att2=\"&lt;val2>\"?></x>"
        val doc: Document = Ksoup.parse(xml, Parser.xmlParser())
        assertEquals(xml, doc.html())
        val decl = doc.expectFirst("x").childNode(0) as XmlDeclaration
        assertEquals("<val2>", decl.attr("att2"))
    }

    @Test
    fun xmlHeaderIsValid() {
        // https://github.com/jhy/Ksoup/issues/2298
        var xml = "<?xml version=\"1.0\"?>\n<root></root>"
        val expect = xml

        var doc: Document = Ksoup.parse(xml, Parser.xmlParser().setTrackErrors(10))
        assertEquals(0, doc.parser()!!.getErrors().size)
        assertEquals(expect, doc.html())

        xml = "<?xml version=\"1.0\" ?>\n<root></root>"
        doc = Ksoup.parse(xml, Parser.xmlParser().setTrackErrors(10))
        assertEquals(0, doc.parser()!!.getErrors().size)
        assertEquals(expect, doc.html())
    }

    @Test
    fun canSetCustomRcdataTag() {
        val inner = "Blah\nblah\n<foo></foo>&quot;"
        val innerText = "Blah\nblah\n<foo></foo>\""

        val xml = "<x><y><z>$inner</z></y></x><x><z id=2></z>"
        val custom = TagSet()
        val z = custom.valueOf("z", Parser.NamespaceXml, ParseSettings.preserveCase)
        z.set(Tag.RcData)

        val doc: Document = Ksoup.parse(xml, Parser.xmlParser().tagSet(custom))
        val zEl = doc.expectFirst("z")
        assertNotSame(z, zEl.tag()) // not same because we copy the tagset
        assertEquals(z, zEl.tag())

        assertEquals(1, zEl.childNodeSize())
        val child = zEl.childNode(0)
        assertTrue(child is TextNode)
        assertEquals(innerText, child.getWholeText())

        // test fragment context parse - should parse <foo> as text
        val z2 = doc.expectFirst("#2")
        z2.html(inner)
        assertEquals(innerText, z2.wholeText())
    }

    @Test
    fun canSetCustomDataTag() {
        val inner = "Blah\nblah\n<foo></foo>&quot;" // no character refs, will be as-is

        val xml = "<x><y><z>$inner</z></y></x><x><z id=2></z>"
        val custom = TagSet()
        val z = custom.valueOf("z", Parser.NamespaceXml, ParseSettings.preserveCase)
        z.set(Tag.Data)

        val doc: Document = Ksoup.parse(xml, Parser.xmlParser().tagSet(custom))
        val zEl = doc.expectFirst("z")
        assertNotSame(z, zEl.tag()) // not same because we copy the tagset
        assertEquals(z, zEl.tag())

        assertEquals(1, zEl.childNodeSize())
        val child = zEl.childNode(0)
        assertTrue(child is DataNode)
        assertEquals(inner, child.getWholeData())
        assertEquals(inner, zEl.data())

        // test fragment context parse - should parse <foo> as data
        val z2 = doc.expectFirst("#2")
        z2.html(inner)
        assertEquals(inner, child.getWholeData())
        assertEquals(inner, zEl.data())
    }

    @Test
    fun canSetCustomVoid() {
        val ns = "custom"
        val xml = "<x xmlns=custom><foo><link><meta>"
        val custom = TagSet()
        custom.valueOf("link", ns).set(Tag.Void)
        custom.valueOf("meta", ns).set(Tag.Void)
        custom.valueOf("foo", "other").set(Tag.Void) // ns doesn't match, won't impact

        val doc: Document = Ksoup.parse(xml, Parser.xmlParser().tagSet(custom))
        val expect = "<x xmlns=\"custom\"><foo><link /><meta /></foo></x>"
        assertEquals(expect, doc.html())
    }

    @Test
    fun canSupplyWithHtmlTagSet() {
        // use the properties of html tag set but without HtmlTreeBuilder rules
        val xml = "<html xmlns=${Parser.NamespaceHtml}><div><script>a<b</script><img><p>"
        val doc: Document = Ksoup.parse(xml, Parser.xmlParser().tagSet(TagSet.Html()))
        doc.outputSettings().prettyPrint(true)
        var expect = "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                " <div>\n" +
                "  <script>//<![CDATA[\n" +
                "a<b\n" +
                "//]]></script>\n" +
                "  <img />\n" +
                "  <p></p>\n" +
                " </div>\n" +
                "</html>"
        assertEquals(expect, doc.html())

        doc.outputSettings().syntax(Syntax.html)
        expect = "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                " <div>\n" +
                "  <script>a<b</script>\n" +
                "  <img>\n" +
                "  <p></p>\n" +
                " </div>\n" +
                "</html>"
        assertEquals(expect, doc.html())
    }

    @Test
    fun prettyFormatsTextInline() {
        // https://github.com/jhy/Ksoup/issues/2141
        val xml = "<package><metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" +
                "<dc:identifier id=\"pub-id\">id</dc:identifier>\n" +
                "<dc:title>title</dc:title>\n" +
                "<dc:language>ja</dc:language>\n" +
                "<dc:description>desc</dc:description>\n" +
                "</metadata></package>"
        val doc: Document = Ksoup.parse(xml, Parser.xmlParser())
        doc.outputSettings().prettyPrint(true)
        assertEquals(
            "<package>\n" +
                    " <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" +
                    "  <dc:identifier id=\"pub-id\">id</dc:identifier> <dc:title>title</dc:title> <dc:language>ja</dc:language> <dc:description>desc</dc:description>\n" +
                    " </metadata>\n" +
                    "</package>", doc.html()
        );

        // can customize
        val meta = doc.expectFirst("metadata")
        val metaTag = meta.tag()
        metaTag.set(Tag.Block)
        // set all the inner els of meta to be blocks
        for (inner in meta) inner.tag().set(Tag.Block)

        assertEquals(
            "<package>\n" +
                    " <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" +
                    "  <dc:identifier id=\"pub-id\">id</dc:identifier>\n" +
                    "  <dc:title>title</dc:title>\n" +
                    "  <dc:language>ja</dc:language>\n" +
                    "  <dc:description>desc</dc:description>\n" +
                    " </metadata>\n" +
                    "</package>", doc.html()
        )
    }

    // namespace tests
    @Test
    fun xmlns() {
        // example from the xml namespace spec https://www.w3.org/TR/xml-names/
        val xml = "<?xml version=\"1.0\"?>\n" +
                "<!-- both namespace prefixes are available throughout -->\n" +
                "<bk:book xmlns:bk=\"urn:loc.gov:books\" xmlns:isbn=\"urn:ISBN:0-395-36341-6\">\n" +
                "    <bk:title>Cheaper by the Dozen</bk:title>\n" +
                "    <isbn:number>1568491379</isbn:number>\n" +
                "</bk:book>"
        val doc: Document = Ksoup.parse(xml, Parser.xmlParser())

        val book = doc.expectFirst("bk|book")
        assertEquals("bk:book", book.tag().name())
        assertEquals("bk", book.tag().prefix())
        assertEquals("book", book.tag().localName())
        assertEquals("urn:loc.gov:books", book.tag().namespace())

        val title = doc.expectFirst("bk|title")
        assertEquals("bk:title", title.tag().name())
        assertEquals("urn:loc.gov:books", title.tag().namespace())

        val number = doc.expectFirst("isbn|number")
        assertEquals("isbn:number", number.tag().name())
        assertEquals("urn:ISBN:0-395-36341-6", number.tag().namespace())

        // and we didn't modify the dom
        assertEquals(xml, doc.html())
    }

    @Test
    fun unprefixedDefaults() {
        val xml = "<?xml version=\"1.0\"?>\n" +
                "<!-- elements are in the HTML namespace, in this case by default -->\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                "  <head><title>Frobnostication</title></head>\n" +
                "  <body><p>Moved to \n" +
                "    <a href=\"http://frob.example.com\">here</a>.</p></body>\n" +
                "</html>"

        val doc: Document = Ksoup.parse(xml, Parser.xmlParser())
        val html = doc.expectFirst("html")
        assertEquals(Parser.NamespaceHtml, html.tag().namespace())
        val a = doc.expectFirst("a")
        assertEquals(Parser.NamespaceHtml, a.tag().namespace())
    }

    @Test
    fun emptyDefault() {
        val xml = "<?xml version='1.0'?>\n" +
                "<Beers>\n" +
                "  <!-- the default namespace inside tables is that of HTML -->\n" +
                "  <table xmlns='http://www.w3.org/1999/xhtml'>\n" +
                "   <th><td>Name</td><td>Origin</td><td>Description</td></th>\n" +
                "   <tr> \n" +
                "     <!-- no default namespace inside table cells -->\n" +
                "     <td><brandName xmlns=\"\">Huntsman</brandName></td>\n" +
                "     <td><origin xmlns=\"\">Bath, UK</origin></td>\n" +
                "     <td>\n" +
                "       <details xmlns=\"\"><class>Bitter</class><hop>Fuggles</hop>\n" +
                "         <pro>Wonderful hop, light alcohol, good summer beer</pro>\n" +
                "         <con>Fragile; excessive variance pub to pub</con>\n" +
                "         </details>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "    </table>\n" +
                "  </Beers>"

        val doc: Document = Ksoup.parse(xml, Parser.xmlParser())
        val beers = doc.expectFirst("Beers")
        assertEquals(Parser.NamespaceXml, beers.tag().namespace())
        val td = doc.expectFirst("td")
        assertEquals(Parser.NamespaceHtml, td.tag().namespace())
        val origin = doc.expectFirst("origin")
        assertEquals("", origin.tag().namespace())
        val pro = doc.expectFirst("pro")
        assertEquals("", pro.tag().namespace())
    }

    @Test
    fun namespacedAttribute() {
        val xml = "<x xmlns:edi='http://ecommerce.example.org/schema'>\n" +
                "  <!-- the 'taxClass' attribute's namespace is http://ecommerce.example.org/schema -->\n" +
                "  <lineItem edi:taxClass=\"exempt\" other=foo>Baby food</lineItem>\n" +
                "</x>"

        val doc: Document = Ksoup.parse(xml, Parser.xmlParser())
        val lineItem = doc.expectFirst("lineItem")

        val taxClass = lineItem.attribute("edi:taxClass")
        assertNotNull(taxClass)
        assertEquals("edi", taxClass.prefix())
        assertEquals("taxClass", taxClass.localName())
        assertEquals("http://ecommerce.example.org/schema", taxClass.namespace())

        val other = lineItem.attribute("other")
        assertNotNull(other)
        assertEquals("foo", other.value)
        assertEquals("", other.prefix())
        assertEquals("other", other.localName())
        assertEquals("", other.namespace())
    }

    @Test
    fun elementsViaAppendHtmlAreNamespaced() {
        // tests that when elements / attributes are added via a fragment parse, they inherit the namespace stack, and can still override
        val xml = "<out xmlns='/out'><bk:book xmlns:bk='/books' xmlns:edi='/edi'><bk:title>Test</bk:title><li edi:foo='bar'></bk:book></out>"
        val doc: Document = Ksoup.parse(xml, Parser.xmlParser())

        // insert some parsed xml, inherit bk and edi, and with an inner node override bk
        val book = doc.expectFirst("bk|book")
        book.append("<bk:content edi:foo=qux>Content</bk:content>")

        val out = doc.expectFirst("out")
        assertEquals("/out", out.tag().namespace())

        val content = book.expectFirst("bk|content")
        assertEquals("bk:content", content.tag().name())
        assertEquals("/books", content.tag().namespace())
        assertEquals("/edi", content.attribute("edi:foo")!!.namespace())

        content.append("<data>Data</data><html xmlns='/html' xmlns:bk='/update'><p>Foo</p><bk:news>News</bk:news></html>")
        // p should be in /html, news in /update
        val p = content.expectFirst("p")
        assertEquals("/html", p.tag().namespace())
        val news = content.expectFirst("bk|news")
        assertEquals("/update", news.tag().namespace())
        val data = content.expectFirst("data")
        assertEquals("/out", data.tag().namespace())
    }

    @Test
    fun selfClosingOK() {
        // In XML, all tags can be self-closing regardless of tag type
        val parser = Parser.xmlParser().setTrackErrors(10)
        val xml = "<div id='1'/><p/><div>Foo</div><div></div><foo></foo>"
        val doc: Document = Ksoup.parse(html = xml, parser = parser, baseUri = "")
        val errors = parser.getErrors()
        assertEquals(0, errors.size)
        assertEquals("<div id=\"1\" /><p /><div>Foo</div><div /><foo></foo>", TextUtil.stripNewlines(doc.outerHtml()))
        // we infer that empty els can be represented with self-closing if seen in parse
    }

    companion object {
        private fun assertXmlNamespace(el: Element) {
            assertEquals(Parser.NamespaceXml, el.tag().namespace(), "Element ${el.tagName()} not in XML namespace")
        }
    }
}
