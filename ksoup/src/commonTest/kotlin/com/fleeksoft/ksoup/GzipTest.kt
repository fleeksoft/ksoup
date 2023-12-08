package com.fleeksoft.ksoup

import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals

class GzipTest {
    @Test
    fun testReadGzipFile() {
        val gzipFileStr =
            readGzipFile(TestHelper.getResourceAbsolutePath("htmltests/gzip.html.gz").toPath())
                .readByteString().utf8()
        val expected = """<title>Gzip test</title>

<p>This is a gzipped HTML file.</p>

"""
        assertEquals(expected, gzipFileStr)
    }
}
