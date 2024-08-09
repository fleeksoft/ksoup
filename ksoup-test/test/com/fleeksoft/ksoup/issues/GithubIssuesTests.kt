package com.fleeksoft.ksoup.issues

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.TestHelper
import korlibs.io.file.std.uniVfs
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GithubIssuesTests {
    @Test
    fun testIssue20DuplicateElements() = runTest {
        //    https://github.com/fleeksoft/ksoup/issues/20
        Ksoup.parse(TestHelper.getFileAsString(TestHelper.getResourceAbsolutePath("htmltests/issue20.html.gz").uniVfs))
//            Ksoup.parseGetRequest("https://www.dm530w.org/")
            .apply {
                body().select("div[class=firs l]")
                    .firstOrNull()?.let { element ->
                        val titles = element.select("div[class=dtit]")
                        val contents = element.select("div[class=img]")
                        println("titles: ${titles.size}, contents: ${contents.size}")
                        assertEquals(6, titles.size)
                        assertEquals(6, contents.size)
                    }
            }
    }
}
