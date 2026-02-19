@file:OptIn(ExperimentalTime::class)

package com.fleeksoft.ksoup

import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.time.ExperimentalTime

class PerformanceTest {

    @Test
    @Ignore
    fun testFileParse() = runTest {
        val data = TestHelper.readResource("test.txt").readAllBytes().decodeToString()
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