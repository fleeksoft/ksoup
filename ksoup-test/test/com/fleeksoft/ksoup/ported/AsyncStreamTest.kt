package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.Platform
import com.fleeksoft.ksoup.isApple
import com.fleeksoft.ksoup.isJS
import com.fleeksoft.ksoup.isWindows
import com.fleeksoft.ksoup.ported.io.Charset
import com.fleeksoft.ksoup.ported.io.openBufferReader
import kotlin.test.Test
import kotlin.test.assertEquals

class AsyncStreamTest {
    @Test
    fun testMixCharReader() {
        val inputData = "ä<a>ä</a>"
        val bufferReader = inputData.toStreamCharReader()
        inputData.toCharArray().forEachIndexed { index, char ->
            assertEquals("$char", bufferReader.read(1))
        }
        assertEquals("", bufferReader.read(1))
    }

    @Test
    fun testMixCharReader2() {
        val inputData = "한국어"
        val bufferReader = inputData.toStreamCharReader()
        inputData.toCharArray().forEachIndexed { index, char ->
            assertEquals("$char", bufferReader.read(1))
        }
        assertEquals("", bufferReader.read(1))
    }

    @Test
    fun testMixCharReader3() {
        val inputData = "Übergrößenträger"
        val bufferReader = inputData.toStreamCharReader()
        inputData.toCharArray().forEachIndexed { index, char ->
            assertEquals("$char", bufferReader.read(1))
        }
        assertEquals("", bufferReader.read(1))
    }

    @Test
    fun testSpecialCharsBufferReader() {
        if (Platform.isJS() || Platform.isApple() || Platform.isWindows()) {
            // FIXME: euc-kr charset not supported
            return
        }

        val specialText1 = "Hello &amp;&lt;&gt; Å å π 新 there ¾ © »"
        val specialText2 = "Übergrößenträger"
        val specialText3 = "한국어"

        assertEquals(specialText1, specialText1.toStreamCharReader().read(specialText1.length))
        assertEquals(specialText2, specialText2.toStreamCharReader().read(specialText2.length))
        assertEquals(specialText3, specialText3.toStreamCharReader().read(specialText3.length))

        assertEquals(
            specialText2,
            specialText2.toByteArray(Charset.forName("iso-8859-1")).openBufferReader()
                .toStreamCharReader(Charset.forName("iso-8859-1"))
                .read(specialText2.length),
        )

        assertEquals(
            specialText3,
            specialText3.toByteArray(Charset.forName("euc-kr")).openBufferReader()
                .toStreamCharReader(Charset.forName("euc-kr"))
                .read(specialText3.length),
        )
    }

    @Test
    fun testReadSingleCharacter() {
        val character = "Hello, Reader!".openBufferReader().read()
        assertEquals('H'.code.toByte(), character)
    }

    @Test
    fun testReadIntoArray() {
        val reader = "Hello, Reader!".openBufferReader()
        val buffer = ByteArray(5)
        val numCharsRead = reader.read(buffer)
        assertEquals(5, numCharsRead)
        assertEquals("Hello", buffer.decodeToString())
    }

    @Test
    fun testSkipCharacters() {
        val reader = "Hello, Reader!".openBufferReader()
        reader.skip(7)
        val buffer = ByteArray(6)
        reader.read(buffer)
        assertEquals("Reader", buffer.decodeToString())
    }

    @Test
    fun testRewind() {
        val inputData = "Hello, Reader!"
        val reader = inputData.openBufferReader()
        val buffer = ByteArray(6)
        reader.read(buffer)
        assertEquals("Hello,", buffer.decodeToString())
    }

    /*@Test
    fun testClose() {
        val reader = "Hello, Reader!".openBufferReader()
        reader.close()
        assertFailsWith<IOException> {
            reader.read()
        }
    }*/
}
