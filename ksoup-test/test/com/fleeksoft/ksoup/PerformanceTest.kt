package com.fleeksoft.ksoup

import korlibs.io.file.std.uniVfs
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Ignore
import kotlin.test.Test

class PerformanceTest {

    @Test
    @Ignore
    fun testFileParse() = runTest {
        val data = TestHelper.getResourceAbsolutePath("test.txt").uniVfs.readString()
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val doc = Ksoup.parse(data)
        val selectTimeStamp = Clock.System.now().toEpochMilliseconds()
        println("ksoup: parseTime: ${selectTimeStamp - timestamp}")
        doc.getElementsByClass("an-info").mapNotNull { anInfo ->
            anInfo.parent()?.let { a ->
                val attr = a.attr("href")
                if (attr.isEmpty()) return@let null

                attr.substringAfter("/Home/Bangumi/", "")
                    .takeIf { it.isNotBlank() }
            }
        }

        println("ksoup: selectTime: ${Clock.System.now().toEpochMilliseconds() - timestamp}")
    }
}