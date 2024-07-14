package com.fleeksoft.ksoup.nodes

import kotlin.test.Test
import kotlin.test.assertEquals

class DataNodeTest {
    @Test
    fun xmlOutputScriptWithCData() {
        val node = DataNode("//<![CDATA[\nscript && <> data]]>")
        node._parentNode = Element("script")
        val accum = StringBuilder()
        node.outerHtmlHead(accum, 0, Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml))
        assertEquals("//<![CDATA[\nscript && <> data]]>", accum.toString())
    }

    @Test
    fun xmlOutputScriptWithoutCData() {
        val node = DataNode("script && <> data")
        node._parentNode = Element("script")
        val accum = StringBuilder()
        node.outerHtmlHead(accum, 0, Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml))
        assertEquals("//<![CDATA[\nscript && <> data\n//]]>", accum.toString())
    }

    @Test
    fun xmlOutputStyleWithCData() {
        val node = DataNode("/*<![CDATA[*/\nstyle && <> data]]>")
        node._parentNode = Element("style")
        val accum = StringBuilder()
        node.outerHtmlHead(accum, 0, Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml))
        assertEquals("/*<![CDATA[*/\nstyle && <> data]]>", accum.toString())
    }

    @Test
    fun xmlOutputStyleWithoutCData() {
        val node = DataNode("style && <> data")
        node._parentNode = Element("style")
        val accum = StringBuilder()
        node.outerHtmlHead(accum, 0, Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml))
        assertEquals("/*<![CDATA[*/\nstyle && <> data\n/*]]>*/", accum.toString())
    }

    @Test
    fun xmlOutputOtherWithCData() {
        val node = DataNode("<![CDATA[other && <> data]]>")
        node._parentNode = Element("other")
        val accum = StringBuilder()
        node.outerHtmlHead(accum, 0, Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml))
        assertEquals("<![CDATA[other && <> data]]>", accum.toString())
    }

    @Test
    fun xmlOutputOtherWithoutCData() {
        val node = DataNode("other && <> data")
        node._parentNode = Element("other")
        val accum = StringBuilder()
        node.outerHtmlHead(accum, 0, Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml))
        assertEquals("<![CDATA[other && <> data]]>", accum.toString())
    }

    @Test
    fun xmlOutputOrphanWithoutCData() {
        val node = DataNode("other && <> data")
        val accum = StringBuilder()
        node.outerHtmlHead(accum, 0, Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml))
        assertEquals("<![CDATA[other && <> data]]>", accum.toString())
    }
}
