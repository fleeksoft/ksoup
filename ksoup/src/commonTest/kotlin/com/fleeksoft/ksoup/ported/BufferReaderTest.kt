package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.Platform
import com.fleeksoft.ksoup.PlatformType
import com.fleeksoft.ksoup.TestHelper
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import okio.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BufferReaderTest {
    @Test
    fun testMixCharReader() {
        if (Platform.current != PlatformType.JVM && !TestHelper.forceAllTestsRun) {
            return
        }

        val inputData = "ä<a>ä</a>"
        val bufferReader = BufferReader(inputData)
        inputData.toCharArray().forEachIndexed { index, char ->
            val charArray = CharArray(1)
            assertEquals(
                1,
                bufferReader.readCharArray(charArray, 0, 1) {
                    val byteSize = if (index == 0 || index == 4) 2 else 1
                    assertEquals(byteSize, it, "read bytes not matched")
                },
            )
            assertEquals(char, charArray[0])
        }
        val charArray = CharArray(1) { ' ' }
        assertEquals(
            -1,
            bufferReader.readCharArray(charArray, len = 1) {
                assertEquals(0, it)
            },
        )
        assertEquals(' ', charArray[0])
    }

    @Test
    fun testMixCharArrayReader() {
        if (Platform.current != PlatformType.JVM && !TestHelper.forceAllTestsRun) {
            return
        }
        val inputData = "ä<a>ä</a>"
        val bufferReader = BufferReader(inputData)
        inputData.toCharArray().forEach {
            val charArray = CharArray(1)
            assertEquals(1, bufferReader.readCharArray(charArray = charArray, off = 0, len = 1))
            assertEquals(it, charArray[0])
        }
        val charArray =
            CharArray(1) {
                ' '
            }
        assertEquals(
            -1,
            bufferReader.readCharArray(charArray, len = 1) {
                assertEquals(0, it)
            },
        )
        assertEquals(' ', charArray[0])
    }

    @Test
    fun testSpecialCharsBufferReader() {
        if (Platform.current == PlatformType.JS || Platform.current == PlatformType.IOS) {
            // FIXME: euc-kr charset not supported
            return
        }

        val specialText1 = "Hello &amp;&lt;&gt; Å å π 新 there ¾ © »"
        val specialText2 = "Übergrößenträger"
        val specialText3 = "한국어"

        assertEquals(specialText1, BufferReader(specialText1).readString())

        assertEquals(
            specialText3,
            BufferReader(
                byteArray = specialText3.toByteArray(Charset.forName("euc-kr")),
                charset = "euc-kr",
            ).readString(specialText3.length.toLong()),
        )

        assertEquals(specialText2, BufferReader(specialText2).readString())
        assertEquals(
            specialText2,
            BufferReader(
                byteArray = specialText2.toByteArray(Charset.forName("iso-8859-1")),
                charset = "iso-8859-1",
            ).readString(specialText2.length.toLong()),
        )

        val charArray = CharArray(specialText2.length)
        assertEquals(
            specialText2.length,
            BufferReader(
                byteArray = specialText2.toByteArray(Charset.forName("iso-8859-1")),
                charset = "iso-8859-1",
            ).readCharArray(charArray, 0, specialText2.length),
        )
        assertEquals(specialText2, charArray.concatToString())

        val charArray2 = CharArray(specialText3.length)
        assertEquals(
            specialText3.length,
            BufferReader(
                byteArray = specialText3.toByteArray(Charset.forName("euc-kr")),
                charset = "euc-kr",
            ).readCharArray(charArray2, 0, specialText3.length),
        )
        assertEquals(specialText3, charArray2.concatToString())
    }

    @Test
    fun testDetermineCharSize() {
        val char1 = 'H'
        val char2 = 'Ü'
        val char3 = '한'
        val char4 = '국'
        val char5 = '어'

        assertEquals(1, BufferReader.determineCharSize(char1.toString().toByteArray()[0].toInt()))
        assertEquals(2, BufferReader.determineCharSize(char2.toString().toByteArray()[0].toInt()))
        assertEquals(3, BufferReader.determineCharSize(char3.toString().toByteArray()[0].toInt()))
        assertEquals(3, BufferReader.determineCharSize(char4.toString().toByteArray()[0].toInt()))
        assertEquals(3, BufferReader.determineCharSize(char5.toString().toByteArray()[0].toInt()))
    }

    @Test
    fun testReadSingleCharacter() {
        val reader = BufferReader("Hello, Reader!")
        val character = reader.read()
        assertEquals('H'.code, character)
    }

    @Test
    fun testReadIntoArray() {
        val reader = BufferReader("Hello, Reader!")
        val buffer = ByteArray(5)
        val numCharsRead = reader[buffer]
        assertEquals(5, numCharsRead)
        assertEquals("Hello", buffer.decodeToString())
    }

    @Test
    fun testSkipCharacters() {
        val reader = BufferReader("Hello, Reader!")
        reader.skip(7)
        val buffer = ByteArray(6)
        reader[buffer]
        assertEquals("Reader", buffer.decodeToString())
    }

    @Test
    fun testRewind() {
        val inputData = "Hello, Reader!"
        val reader = BufferReader(inputData)
        val buffer = ByteArray(6)
        reader[buffer]
        assertEquals("Hello,", buffer.decodeToString())
    }

    @Test
    fun testClose() {
        val reader = BufferReader("Hello, Reader!")
        reader.close()
        assertFailsWith<IOException> {
            reader.read()
        }
    }
}
