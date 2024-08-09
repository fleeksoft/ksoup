package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.Ksoup.parse
import com.fleeksoft.ksoup.parser.ParseSettings
import com.fleeksoft.ksoup.parser.Parser
import de.cketti.codepoints.deluxe.toCodePoint
import kotlin.test.*

class AttributeTest {
    @Test
    fun html() {
        val attr = Attribute("key", "value &")
        assertEquals("key=\"value &amp;\"", attr.html())
        assertEquals(attr.html(), attr.toString())
    }

    @Test
    fun testWithSupplementaryCharacterInAttributeKeyAndValue() {
        val s = 135361.toCodePoint().toChars().concatToString()
        val attr = Attribute(s, "A" + s + "B")
        assertEquals(s + "=\"A" + s + "B\"", attr.html())
        assertEquals(attr.html(), attr.toString())
    }

    @Test
    fun validatesKeysNotEmpty() {
        assertFailsWith<IllegalArgumentException> { Attribute(" ", "Check") }
    }

    @Test
    fun validatesKeysNotEmptyViaSet() {
        assertFailsWith<IllegalArgumentException> {
            val attr = Attribute("One", "Check")
            attr.setKey(" ")
        }
    }

    @Test
    fun booleanAttributesAreEmptyStringValues() {
        val doc = parse("<div hidden>")
        val attributes = doc.body().child(0).attributes()
        assertEquals("", attributes["hidden"])
        val first = attributes.iterator().next()
        assertEquals("hidden", first.key)
        assertEquals("", first.value)
        assertFalse(first.hasDeclaredValue())
        assertTrue(Attribute.isBooleanAttribute(first.key))
    }

    @Test
    fun settersOnOrphanAttribute() {
        val attr = Attribute("one", "two")
        attr.setKey("three")
        val oldVal = attr.setValue("four")
        assertEquals("two", oldVal)
        assertEquals("three", attr.key)
        assertEquals("four", attr.value)
        assertNull(attr.parent)
    }

    @Test
    fun hasValue() {
        val a1 = Attribute("one", "")
        val a2 = Attribute("two", null)
        val a3 = Attribute("thr", "thr")
        assertTrue(a1.hasDeclaredValue())
        assertFalse(a2.hasDeclaredValue())
        assertTrue(a3.hasDeclaredValue())
    }

    @Test
    fun canSetValueToNull() {
        val attr = Attribute("one", "val")
        var oldVal = attr.setValue(null)
        assertEquals("one", attr.html())
        assertEquals("val", oldVal)
        oldVal = attr.setValue("foo")
        assertEquals("", oldVal) // string, not null
    }

    @Test
    fun booleanAttributesAreNotCaseSensitive() {
        assertTrue(Attribute.isBooleanAttribute("required"))
        assertTrue(Attribute.isBooleanAttribute("REQUIRED"))
        assertTrue(Attribute.isBooleanAttribute("rEQUIREd"))
        assertFalse(Attribute.isBooleanAttribute("random string"))
        val html = "<a href=autofocus REQUIRED>One</a>"
        val doc = parse(html)
        assertEquals("<a href=\"autofocus\" required>One</a>", doc.selectFirst("a")!!.outerHtml())
        val doc2 = parse(html, Parser.htmlParser().settings(ParseSettings.preserveCase))
        assertEquals("<a href=\"autofocus\" REQUIRED>One</a>", doc2.selectFirst("a")!!.outerHtml())
    }
}
