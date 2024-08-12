package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.parameterizedTest
import com.fleeksoft.ksoup.ported.openBufferReader
import kotlin.test.Test
import kotlin.test.assertEquals

class StreamCharReaderTest {
    @Test
    fun testCharReaderMarkSkipReset() {
        /*val randomUnicodeStrings = listOf(
    "©頷ӨҤもタ編倏病Ҿ0沑âチ麕üӨҀ🙌とガӃ🙄Ҥzせø觧ҥ",
    "Ђヹ肯みцë匓ンê😺り磬バëӇØ゚琫ら儂脸😨D亢JZEÕキ燗😨🙉ュӼ`ぺ",
    "😳捇😗Ҧヿィ😲😵SӚåぐルҩS😯yѹ=ӪӠÀrキえÄ🙎へ¶Mたじ😞冃😃a\xa0ÙҒ樗ボ😨",
    "らーçウごネ粦😓õ姗ӏ(",
    "Sm糛҆Òう楢ょ😽ê."
)*/
        //        X5O!P%@AP[4\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*
// use random strings
//        totChars % size = totalSegments
//        totChars % size = lastSegment

        parameterizedTest((3..100).toMutableList().apply { add(SharedConstants.DefaultBufferSize) }) { chunkSize ->
            val charReader = "abcdefghijklm".openBufferReader().toStreamCharReader(chunkSize = chunkSize)

            charReader.mark(1111)
            val charArray = CharArray(3)
            assertEquals(3, charReader.readCharArray(charArray, 0, 3))
            assertEquals("abc", charArray.concatToString())
            charReader.reset()
//            charReader.skip(3)
            assertEquals(3, charReader.readCharArray(charArray, 0, 3))
            assertEquals("abc", charArray.concatToString())

            charReader.mark(1111)
            assertEquals(3, charReader.readCharArray(charArray, 0, 3))
            assertEquals("def", charArray.concatToString())
            charReader.reset()
//            charReader.skip(3)
            assertEquals(3, charReader.readCharArray(charArray, 0, 3))
            assertEquals("def", charArray.concatToString())

            charReader.mark(1111)
            assertEquals(3, charReader.readCharArray(charArray, 0, 3))
            assertEquals("ghi", charArray.concatToString())
            charReader.reset()
//            charReader.skip(3)
            assertEquals(3, charReader.readCharArray(charArray, 0, 3))
            assertEquals("ghi", charArray.concatToString())

            charReader.mark(1111)
            assertEquals(3, charReader.readCharArray(charArray, 0, 3))
            assertEquals("jkl", charArray.concatToString())
            charReader.reset()
//            charReader.skip(3)
            assertEquals(3, charReader.readCharArray(charArray, 0, 3))
            assertEquals("jkl", charArray.concatToString())

            charReader.mark(1111)
            assertEquals(1, charReader.readCharArray(charArray, 0, 3))
            assertEquals("mkl", charArray.concatToString())
            charReader.reset()
            assertEquals(1, charReader.readCharArray(charArray, 0, 3))
            assertEquals("mkl", charArray.concatToString())
        }
    }

    @Test
    fun testCharSequence() {
        val input = "abcdefghijklmnopqrstuvwxyz"
        val charReader = input.toStreamCharReader()
        input.forEach {
            assertEquals("$it", charReader.read(1))
        }
    }

    @Test
    fun testCharArrayRead() {
        val input = "abcdefghijklmnopqrstuvwxyz"
        val charReader = input.toStreamCharReader()
        run {
            val charArray = CharArray(7)
            assertEquals(7, charReader.readCharArray(charArray, 0, 7))
            assertEquals(input.substring(0..6), charArray.concatToString())
        }

        run {
            val charArray = CharArray(7)
            assertEquals(7, charReader.readCharArray(charArray, 0, 7))
            assertEquals(input.substring(7..13), charArray.concatToString())
        }

        run {
            val charArray = CharArray(12)
            assertEquals(12, charReader.readCharArray(charArray, 0, 12))
            assertEquals(input.substring(14..25), charArray.concatToString())
        }
    }

    @Test
    fun testMarkableCharReader() {
        val charReader = "abcdefghijklmnopqrstuvwxyz".toStreamCharReader()
        assertEquals("a", charReader.read(1))
        assertEquals("b", charReader.read(1))
        charReader.mark(100)
        assertEquals("c", charReader.read(1))
        assertEquals("d", charReader.read(1))
        charReader.reset()
        assertEquals("c", charReader.read(1))
        assertEquals("d", charReader.read(1))
        assertEquals("e", charReader.read(1))
    }

    @Test
    fun testSkipCharReader() {
        val charReader = "abcdefghijklmnopqrstuvwxyz".toStreamCharReader()
        assertEquals("a", charReader.read(1))
        assertEquals("b", charReader.read(1))
        charReader.skip(3)
        assertEquals("f", charReader.read(1))
        assertEquals("g", charReader.read(1))
        assertEquals("h", charReader.read(1))
        charReader.skip(2)
        assertEquals("k", charReader.read(1))
        assertEquals("l", charReader.read(1))
        assertEquals("m", charReader.read(1))
    }

    @Test
    fun testMarkableSkipReader() {
        val charReader = "abcdefghijklmnopqrstuvwxyz".toStreamCharReader()
        assertEquals("a", charReader.read(1))
        assertEquals("b", charReader.read(1))
        assertEquals("c", charReader.read(1))
        charReader.skip(2)
        charReader.mark(100)
        assertEquals("f", charReader.read(1))
        assertEquals("g", charReader.read(1))
        assertEquals("h", charReader.read(1))
        charReader.reset()
        assertEquals("f", charReader.read(1))
        assertEquals("g", charReader.read(1))
        assertEquals("h", charReader.read(1))
        assertEquals("i", charReader.read(1))
        assertEquals("j", charReader.read(1))
        assertEquals("k", charReader.read(1))
    }
}
