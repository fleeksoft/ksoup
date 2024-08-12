package com.fleeksoft.ksoup

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GzipTest {
    @Test
    fun testReadGzipFile() = runTest {
        val gzipFileStr = TestHelper.readGzipResource("htmltests/gzip.html.gz").readAllBytes().decodeToString()
        val expected = """<title>Gzip test</title>

<p>This is a gzipped HTML file.</p>

"""
        assertEquals(expected, gzipFileStr)
    }
}
