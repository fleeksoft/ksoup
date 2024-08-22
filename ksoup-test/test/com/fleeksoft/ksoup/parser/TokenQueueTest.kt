package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.Ksoup.parse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Token queue tests.
 */
class TokenQueueTest {

    @Test
    fun chompBalanced() {
        val tq = TokenQueue(":contains(one (two) three) four")
        val pre = tq.consumeTo("(")
        val guts = tq.chompBalanced('(', ')')
        val remainder = tq.remainder()
        assertEquals(":contains", pre)
        assertEquals("one (two) three", guts)
        assertEquals(" four", remainder)
    }

    @Test
    fun chompEscapedBalanced() {
        val tq = TokenQueue(":contains(one (two) \\( \\) \\) three) four")
        val pre = tq.consumeTo("(")
        val guts = tq.chompBalanced('(', ')')
        val remainder = tq.remainder()
        assertEquals(":contains", pre)
        assertEquals("one (two) \\( \\) \\) three", guts)
        assertEquals("one (two) ( ) ) three", TokenQueue.unescape(guts))
        assertEquals(" four", remainder)
    }

    @Test
    fun chompBalancedMatchesAsMuchAsPossible() {
        val tq = TokenQueue("unbalanced(something(or another)) else")
        tq.consumeTo("(")
        val match = tq.chompBalanced('(', ')')
        assertEquals("something(or another)", match)
    }

    @Test
    fun unescape() {
        assertEquals("one ( ) \\", TokenQueue.unescape("one \\( \\) \\\\"))
    }

    @Test
    fun unescape_2() {
        assertEquals("\\&", TokenQueue.unescape("\\\\\\&"))
    }

    @Test
    fun escapeCssIdentifier() {
        assertEquals("one\\#two\\.three\\/four\\\\five", TokenQueue.escapeCssIdentifier("one#two.three/four\\five"))
    }

    @Test
    fun chompToIgnoreCase() {
        val t = "<textarea>one < two </TEXTarea>"
        var tq = TokenQueue(t)
        var data = tq.chompToIgnoreCase("</textarea")
        assertEquals("<textarea>one < two ", data)
        tq = TokenQueue("<textarea> one two < three </oops>")
        data = tq.chompToIgnoreCase("</textarea")
        assertEquals("<textarea> one two < three </oops>", data)
    }

    @Test
    fun addFirst() {
        val tq = TokenQueue("One Two")
        tq.consumeWord()
        tq.addFirst("Three")
        assertEquals("Three Two", tq.remainder())
    }

    @Test
    fun consumeToIgnoreSecondCallTest() {
        val t = "<textarea>one < two </TEXTarea> third </TEXTarea>"
        val tq = TokenQueue(t)
        var data = tq.chompToIgnoreCase("</textarea>")
        assertEquals("<textarea>one < two ", data)
        data = tq.chompToIgnoreCase("</textarea>")
        assertEquals(" third ", data)
    }

    @Test
    fun testNestedQuotes() {
        validateNestedQuotes(
            "<html><body><a id=\"identifier\" onclick=\"func('arg')\" /></body></html>",
            "a[onclick*=\"('arg\"]",
        )
        validateNestedQuotes(
            "<html><body><a id=\"identifier\" onclick=func('arg') /></body></html>",
            "a[onclick*=\"('arg\"]",
        )
        validateNestedQuotes(
            "<html><body><a id=\"identifier\" onclick='func(\"arg\")' /></body></html>",
            "a[onclick*='(\"arg']",
        )
        validateNestedQuotes(
            "<html><body><a id=\"identifier\" onclick=func(\"arg\") /></body></html>",
            "a[onclick*='(\"arg']",
        )
    }

    @Test
    fun chompBalancedThrowIllegalArgumentException() {
        try {
            val tq = TokenQueue("unbalanced(something(or another)) else")
            tq.consumeTo("(")
            tq.chompBalanced('(', '+')
            fail("should have thrown IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertEquals("Did not find balanced marker at 'something(or another)) else'", expected.message)
        }
    }

    @Test
    fun testQuotedPattern() {
        val doc = parse("<div>\\) foo1</div><div>( foo2</div><div>1) foo3</div>")
        assertEquals(
            "\n\\) foo1",
            doc.select("div:matches(" + Regex.escape("\\)") + ")")[0].childNode(0).toString(),
        )
        assertEquals(
            "\n( foo2",
            doc.select("div:matches(" + Regex.escape("(") + ")")[0].childNode(0).toString(),
        )
        assertEquals(
            "\n1) foo3",
            doc.select("div:matches(" + Regex.escape("1)") + ")")[0].childNode(0).toString(),
        )
    }

    @Test
    fun consumeEscapedTag() {
        val q = TokenQueue("p\\\\p p\\.p p\\:p p\\!p")
        assertEquals("p\\p", q.consumeElementSelector())
        assertTrue(q.consumeWhitespace())
        assertEquals("p.p", q.consumeElementSelector())
        assertTrue(q.consumeWhitespace())
        assertEquals("p:p", q.consumeElementSelector())
        assertTrue(q.consumeWhitespace())
        assertEquals("p!p", q.consumeElementSelector())
        assertTrue(q.isEmpty())
    }

    @Test
    fun consumeEscapedId() {
        val q = TokenQueue("i\\.d i\\\\d")
        assertEquals("i.d", q.consumeCssIdentifier())
        assertTrue(q.consumeWhitespace())
        assertEquals("i\\d", q.consumeCssIdentifier())
        assertTrue(q.isEmpty())
    }

    companion object {
        private fun validateNestedQuotes(
            html: String,
            selector: String,
        ) {
            assertEquals(
                "#identifier",
                parse(html).select(selector).first()!!
                    .cssSelector(),
            )
        }
    }
}
