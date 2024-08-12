package com.fleeksoft.ksoup

import korlibs.io.file.std.uniVfs
import kotlinx.coroutines.test.runTest
import org.jsoup.Jsoup
import kotlin.system.measureTimeMillis
import kotlin.test.Test

class PerformanceComparisonTest {
    @Test
    fun compareWithJsoup() = runTest {
        if (BuildConfig.isGithubActions) {
            return@runTest
        }

        val testData = TestHelper.getResourceAbsolutePath("test.txt").uniVfs.readString()

        val ksoupParseTimes = mutableListOf<Long>()
        val ksoupSelectTimes = mutableListOf<Long>()
        val jsoupParseTimes = mutableListOf<Long>()
        val jsoupSelectTimes = mutableListOf<Long>()

        // Perform multiple tests
        repeat(10) {
            ksoupTest(testData, ksoupParseTimes, ksoupSelectTimes)
            jsoupTest(testData, jsoupParseTimes, jsoupSelectTimes)
        }

        // Print comparison table
        printComparisonTable(ksoupParseTimes, ksoupSelectTimes, jsoupParseTimes, jsoupSelectTimes)
    }

    private fun ksoupTest(data: String, parseTimes: MutableList<Long>, selectTimes: MutableList<Long>) {
        val parseTime = measureTimeMillis {
            Ksoup.parse(data)
        }
        val selectTime = measureTimeMillis {
            val doc = Ksoup.parse(data)
            doc.getElementsByClass("an-info").mapNotNull { anInfo ->
                anInfo.parent()?.let { a ->
                    val attr = a.attr("href")
                    if (attr.isEmpty()) return@let null

                    attr.substringAfter("/Home/Bangumi/", "")
                        .takeIf { it.isNotBlank() }
                }
            }
        }
        parseTimes.add(parseTime)
        selectTimes.add(selectTime)
    }

    private fun jsoupTest(data: String, parseTimes: MutableList<Long>, selectTimes: MutableList<Long>) {
        val parseTime = measureTimeMillis {
            Jsoup.parse(data)
        }
        val selectTime = measureTimeMillis {
            val doc = Jsoup.parse(data)
            doc.getElementsByClass("an-info").mapNotNull { anInfo ->
                anInfo.parent()?.let { a ->
                    val attr = a.attr("href")
                    if (attr.isEmpty()) return@let null

                    attr.substringAfter("/Home/Bangumi/", "")
                        .takeIf { it.isNotBlank() }
                }
            }
        }
        parseTimes.add(parseTime)
        selectTimes.add(selectTime)
    }

    private fun printComparisonTable(
        ksoupParseTimes: List<Long>,
        ksoupSelectTimes: List<Long>,
        jsoupParseTimes: List<Long>,
        jsoupSelectTimes: List<Long>
    ) {
        // Define column widths
        val col1Width = 5 // Test number
        val col2Width = 20 // Ksoup Parse
        val col3Width = 20 // Jsoup Parse
        val col4Width = 20 // Ksoup Select
        val col5Width = 20 // Jsoup Select

        // Print table header
        println("\nComparison Table")
        println(
            "Test${" ".repeat(col1Width - 4)}| Ksoup Parse (ms)${" ".repeat(col2Width - 15)}| Jsoup Parse (ms)${" ".repeat(col3Width - 15)}| Ksoup Select (ms)${
                " ".repeat(
                    col4Width - 17
                )
            }| Jsoup Select (ms)"
        )
        println("".padEnd(col1Width + col2Width + col3Width + col4Width + col5Width + 7, '-'))

        // Print test results
        for (i in ksoupParseTimes.indices) {
            println(
                "%-${col1Width}d| %-${col2Width}d| %-${col3Width}d| %-${col4Width}d| %-${col5Width}d".format(
                    i + 1, ksoupParseTimes[i], jsoupParseTimes[i], ksoupSelectTimes[i], jsoupSelectTimes[i]
                )
            )
        }

        // Calculate averages
        val avgKsoupParseTime = ksoupParseTimes.average()
        val avgJsoupParseTime = jsoupParseTimes.average()
        val avgKsoupSelectTime = ksoupSelectTimes.average()
        val avgJsoupSelectTime = jsoupSelectTimes.average()

        // Print average results
        println("".padEnd(col1Width + col2Width + col3Width + col4Width + col5Width + 7, '-'))
        println(
            "%-${col1Width}s| %-${col2Width}.2f| %-${col3Width}.2f| %-${col4Width}.2f| %-${col5Width}.2f".format(
                "Avg", avgKsoupParseTime, avgJsoupParseTime, avgKsoupSelectTime, avgJsoupSelectTime
            )
        )
    }
}