package com.fleeksoft.ksoup

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.readByteString
import kotlinx.io.readString
import kotlin.test.Test
import kotlin.test.assertEquals

class GzipTest {
    @Test
    fun testReadGzipFile() {
        if (Platform.isWindows()) {
//            gzip not supported yet
            return
        }
        val gzipFileStr =
            readGzipFile(Path(TestHelper.getResourceAbsolutePath("htmltests/gzip.html.gz"))).buffered().readString()
        val expected = """<title>Gzip test</title>

<p>This is a gzipped HTML file.</p>

"""
        assertEquals(expected, gzipFileStr)
    }
}
