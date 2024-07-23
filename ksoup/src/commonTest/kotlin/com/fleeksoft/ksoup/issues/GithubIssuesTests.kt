package com.fleeksoft.ksoup.issues

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.Platform
import com.fleeksoft.ksoup.TestHelper
import com.fleeksoft.ksoup.isWindows
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class GithubIssuesTests {
    @Test
    fun testIssue20DuplicateElements() {
        if (Platform.isWindows()) {
//            gzip not supported yet
            return
        }
        //    https://github.com/fleeksoft/ksoup/issues/20
        runTest {
            val file: Path = Path(TestHelper.getResourceAbsolutePath("htmltests/issue20.html.gz"))
            Ksoup.parse(TestHelper.getFileAsString(file))
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
}
