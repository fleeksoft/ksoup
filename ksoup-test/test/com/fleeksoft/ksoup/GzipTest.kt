package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.ported.io.Charsets
import com.fleeksoft.ksoup.ported.toString
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GzipTest {
    @Test
    fun testReadGzipFile() = runTest {
        val gzipFileStr = TestHelper.readGzipResource("htmltests/gzip.html.gz").readAllBytes()
            .toString(charset = Charsets.UTF8)
        val expected = """<title>Gzip test</title>

<p>This is a gzipped HTML file.</p>

"""
        assertEquals(expected, gzipFileStr)
    }
}
