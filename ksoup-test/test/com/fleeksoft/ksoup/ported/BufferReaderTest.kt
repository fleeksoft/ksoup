package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.Platform
import com.fleeksoft.ksoup.isApple
import com.fleeksoft.ksoup.isJS
import com.fleeksoft.ksoup.isWindows
import korlibs.io.lang.Charset
import korlibs.io.lang.toByteArray
import korlibs.io.stream.openSync
import kotlin.test.Test
import kotlin.test.assertEquals

class BufferReaderTest {
    @Test
    fun testMixCharReader() {
        val inputData = "ä<a>ä</a>"
        val streamCharReader = inputData.openSync().toStreamCharReader()
        inputData.toCharArray().forEach { char ->
            val charArray = CharArray(1)
            assertEquals(1, streamCharReader.readCharArray(charArray, 0, 1))
            assertEquals(char, charArray[0])
        }
        val charArray = CharArray(1) { ' ' }
        assertEquals(-1, streamCharReader.readCharArray(charArray, 0, 1))
        assertEquals(' ', charArray[0])
    }

    @Test
    fun testMixCharArrayReader() {
        val inputData = "ä<a>ä</a>"
        val streamCharReader = inputData.openSync().toStreamCharReader()
        inputData.toCharArray().forEach {
            val charArray = CharArray(1)
            assertEquals(1, streamCharReader.readCharArray(charArray = charArray, offset = 0, count = 1))
            assertEquals(it, charArray[0])
        }
        val charArray =
            CharArray(1) {
                ' '
            }
        assertEquals(
            -1,
            streamCharReader.readCharArray(charArray, 0, 1),
        )
        assertEquals(' ', charArray[0])
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

        assertEquals(specialText1, specialText1.openSync().toStreamCharReader().read(specialText1.length))
        assertEquals(specialText2, specialText2.openSync().toStreamCharReader().read(specialText2.length))

        assertEquals(
            specialText3,
            specialText3.toByteArray(Charset.forName("euc-kr")).openSync()
                .toStreamCharReader(Charset.forName("euc-kr")).read(specialText3.length),
        )

        assertEquals(
            specialText2,
            specialText2.toByteArray(Charset.forName("iso-8859-1")).openSync()
                .toStreamCharReader(Charset.forName("iso-8859-1")).read(specialText2.length),
        )
    }

    @Test
    fun testRewind() {
        val inputData = "Hello, Reader!"
        val reader = inputData.openSync()
        val buffer = ByteArray(6)
        reader.read(buffer, 0, 6)
        assertEquals("Hello,", buffer.decodeToString())
    }
}
