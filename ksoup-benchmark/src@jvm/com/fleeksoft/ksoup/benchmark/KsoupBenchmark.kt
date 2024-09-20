package com.fleeksoft.ksoup.benchmark

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.select.Elements
import com.fleeksoft.ksoup.select.Evaluator
import kotlinx.benchmark.*
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString


@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class KsoupBenchmark {
    private lateinit var fileData: String
    private lateinit var doc1: Document

    @Setup
    fun setUp() {
        fileData =
            SystemFileSystem.source(Path("/Users/sabeeh/IdeaProjects/ksoup-benchmark/ksoup-test/testResources/test.txt")).buffered().readString()
        doc1 = parseHtml()
    }

    @Benchmark
    fun parse() {
        val doc = parseHtml()
    }

    @Benchmark
    fun select() {
        val doc = parseHtml()
        doc.getElementsByClass("an-info").mapNotNull { anInfo ->
            anInfo.parent()?.let { a ->
                val attr = a.attr("href")
                if (attr.isEmpty()) return@let null

                attr.substringAfter("/Home/Bangumi/", "")
                    .takeIf { it.isNotBlank() }
            }
        }
    }

    private fun parseHtml() = Ksoup.parse(fileData)
}