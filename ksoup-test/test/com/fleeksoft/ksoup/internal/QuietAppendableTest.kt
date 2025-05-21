package com.fleeksoft.ksoup.internal

import com.fleeksoft.io.exception.IOException
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.exception.SerializationException
import com.fleeksoft.ksoup.internal.QuietAppendable.StringBuilderAppendable
import com.fleeksoft.ksoup.nodes.Document
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue


internal class QuietAppendableTest {
    @Test
    fun wrap() {
        assertIs<StringBuilderAppendable>(QuietAppendable.wrap(StringBuilder()))

        // TODO: wait for CharArrayWriter in fleeksoft-io
//        assertIs<BaseAppendable>(QuietAppendable.wrap(CharArrayWriter()))
    }

    @Test
    fun supplemental() {
        // hits append(char[] chars, int offset, int len) with len 2 for supplemental codepoint
        val expect = "😀"
        val chars = CharArray(2)
        chars[0] = expect[0]
        chars[1] = expect[1]
        assertEquals(2, expect.length)

        val sb = QuietAppendable.wrap(StringBuilder())
        sb.append(chars, 0, 2)
        val s: String? = sb.toString()
        assertEquals(expect, s)

        // TODO: wait for CharArrayWriter in fleeksoft-io
        /*val cw: CharArrayWriter = CharArrayWriter()
        val qa = QuietAppendable.wrap(cw)
        qa.append(chars, 0, 2)
        val out: String? = cw.toString()
        assertEquals(expect, out)*/
    }

    @Test
    fun appendThrowsSerializationException() {
        val doc: Document = Ksoup.parse("<div>")
        val brokenWriter: Appendable = brokenAppender()
        var threw = false
        try {
            doc.html(brokenWriter)
        } catch (e: SerializationException) {
            threw = true
            val cause = e.cause
            assertEquals("broken", cause?.message)
            assertIs<IOException>(cause)
        }
        assertTrue(threw)
    }

    companion object {
        private fun brokenAppender(): Appendable {
            // returns an Appendable that throws an IOException on any put
            return object : Appendable {
                override fun append(value: CharSequence?): Appendable {
                    throw IOException("broken")
                }

                override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): Appendable {
                    throw IOException("broken")
                }

                override fun append(value: Char): Appendable {
                    throw IOException("broken")
                }
            }
        }
    }
}
