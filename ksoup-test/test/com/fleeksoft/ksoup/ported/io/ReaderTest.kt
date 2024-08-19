package com.fleeksoft.ksoup.ported.io

import com.fleeksoft.ksoup.TestHelper
import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.ported.openSourceReader
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderTest {
    @BeforeTest
    fun initKsoup() {
        TestHelper.initKsoup()
    }

    private fun readerStringTestStarter(input: String, testBody: (input: String, reader: Reader) -> Unit) {
        testBody(input, StringReader(input))
        testBody(input, BufferedReader(StringReader(input)))
        testBody(input, BufferedReader(InputSourceReader(input.openSourceReader())))
        testBody(input, BufferedReader(InputSourceReader(input.openSourceReader()), SharedConstants.DefaultBufferSize))
    }

    @Test
    fun testCharReaderMarkSkipReset() = readerStringTestStarter("abcdefghijklm") { input, reader ->

        reader.mark(1111)
        val charArray = CharArray(3)
        assertEquals(3, reader.read(charArray, 0, 3))
        assertEquals("abc", charArray.concatToString())
        reader.reset()
//            charReader.skip(3)
        assertEquals(3, reader.read(charArray, 0, 3))
        assertEquals("abc", charArray.concatToString())

        reader.mark(1111)
        assertEquals(3, reader.read(charArray, 0, 3))
        assertEquals("def", charArray.concatToString())
        reader.reset()
//            charReader.skip(3)
        assertEquals(3, reader.read(charArray, 0, 3))
        assertEquals("def", charArray.concatToString())

        reader.mark(1111)
        assertEquals(3, reader.read(charArray, 0, 3))
        assertEquals("ghi", charArray.concatToString())
        reader.reset()
//            charReader.skip(3)
        assertEquals(3, reader.read(charArray, 0, 3))
        assertEquals("ghi", charArray.concatToString())

        reader.mark(1111)
        assertEquals(3, reader.read(charArray, 0, 3))
        assertEquals("jkl", charArray.concatToString())
        reader.reset()
//            charReader.skip(3)
        assertEquals(3, reader.read(charArray, 0, 3))
        assertEquals("jkl", charArray.concatToString())

        reader.mark(1111)
        assertEquals(1, reader.read(charArray, 0, 3))
        assertEquals("mkl", charArray.concatToString())
        reader.reset()
        assertEquals(1, reader.read(charArray, 0, 3))
        assertEquals("mkl", charArray.concatToString())
    }


    @Test
    fun testCharSequence() = readerStringTestStarter("abcdefghijklmnopqrstuvwxyz") { input, reader ->
        input.forEach {
            assertEquals(it, reader.read().toChar())
        }
    }

    /*@Test
    fun testRandomLargeCharSequence() {
        (1..100000).forEach {
            println("testRandomLargeCharSequence: $it")
            readerStringTestStarter("abcdefghijklmnopqrstuvwxyz".repeat(it)) { input, reader ->
                input.forEach {
                    assertEquals(it, reader.read().toChar())
                }
            }
        }
    }*/

    @Test
    fun testLargeCharSequence() = readerStringTestStarter("abcdefghijklmnopqrstuvwxyz".repeat((10..500).random())) { input, reader ->
        input.forEach {
            assertEquals(it, reader.read().toChar())
        }
    }

    @Test
    fun testCharArrayRead() = readerStringTestStarter("abcdefghijklmnopqrstuvwxyz") { input, reader ->
        run {
            val charArray = CharArray(7)
            assertEquals(7, reader.read(charArray, 0, 7))
            assertEquals(input.substring(0..6), charArray.concatToString())
        }

        run {
            val charArray = CharArray(7)
            assertEquals(7, reader.read(charArray, 0, 7))
            assertEquals(input.substring(7..13), charArray.concatToString())
        }

        run {
            val charArray = CharArray(12)
            assertEquals(12, reader.read(charArray, 0, 12))
            assertEquals(input.substring(14..25), charArray.concatToString())
        }
    }

    @Test
    fun testMarkableCharReader() = readerStringTestStarter("abcdefghijklmnopqrstuvwxyz") { input, reader ->
        assertEquals('a', reader.read().toChar())
        assertEquals('b', reader.read().toChar())
        reader.mark(100)
        assertEquals('c', reader.read().toChar())
        assertEquals('d', reader.read().toChar())
        reader.reset()
        assertEquals('c', reader.read().toChar())
        assertEquals('d', reader.read().toChar())
        assertEquals('e', reader.read().toChar())
    }

    @Test
    fun testSkipCharReader() = readerStringTestStarter("abcdefghijklmnopqrstuvwxyz") { input, reader ->
        assertEquals('a', reader.read().toChar())
        assertEquals('b', reader.read().toChar())
        reader.skip(3)
        assertEquals('f', reader.read().toChar())
        assertEquals('g', reader.read().toChar())
        assertEquals('h', reader.read().toChar())
        reader.skip(2)
        assertEquals('k', reader.read().toChar())
        assertEquals('l', reader.read().toChar())
        assertEquals('m', reader.read().toChar())
    }

    @Test
    fun testMarkableSkipReader() = readerStringTestStarter("abcdefghijklmnopqrstuvwxyz") { input, reader ->
        assertEquals('a', reader.read().toChar())
        assertEquals('b', reader.read().toChar())
        assertEquals('c', reader.read().toChar())
        reader.skip(2)
        reader.mark(100)
        assertEquals('f', reader.read().toChar())
        assertEquals('g', reader.read().toChar())
        assertEquals('h', reader.read().toChar())
        reader.reset()
        assertEquals('f', reader.read().toChar())
        assertEquals('g', reader.read().toChar())
        assertEquals('h', reader.read().toChar())
        assertEquals('i', reader.read().toChar())
        assertEquals('j', reader.read().toChar())
        assertEquals('k', reader.read().toChar())
    }

    private fun testMixCharReader(inputData: String) = readerStringTestStarter(inputData) { inputData, reader ->
        inputData.toCharArray().forEach { char ->
            val charArray = CharArray(1)
            assertEquals(1, reader.read(charArray, 0, 1))
            assertEquals(char, charArray[0])
        }
        val charArray = CharArray(1) { ' ' }
        assertEquals(-1, reader.read(charArray, 0, 1))
        assertEquals(' ', charArray[0])
    }

    @Test
    fun testMixCharReader() {
        val inputData = "ä<a>ä</a>"
        testMixCharReader(inputData)
    }

    @Test
    fun testMixCharReader2() {
        val inputData = "한국어"
        testMixCharReader(inputData)
    }

    @Test
    fun testMixCharReader3() {
        val inputData = "Übergrößenträger"
        testMixCharReader(inputData)
    }
}