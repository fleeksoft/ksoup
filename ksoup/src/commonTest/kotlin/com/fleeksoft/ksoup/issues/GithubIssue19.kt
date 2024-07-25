import com.fleeksoft.ksoup.*
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.select.Elements
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertTrue

class GithubIssue19 {
    val PNG_BASE64_HEADER = "data:image/png;base64,"

    //    https://github.com/fleeksoft/ksoup/issues/19

    @Test
    fun testAttributeIncorrectMixCharsetIssue() {
        if (Platform.isWindows() || Platform.isWasmJs()) {
//            gzip not supported yet
            return
        }
        val document: Document = Ksoup.parseFile(TestHelper.getResourceAbsolutePath("htmltests/issue19.html.gz"))
        val imagesEls: Elements = document.select("img")
        for (imagesEl in imagesEls) {
            val attr: String = imagesEl.attr("src")
            if (!attr.startsWith(PNG_BASE64_HEADER)) {
                continue
            }
            val src = attr.replaceFirst(PNG_BASE64_HEADER.toRegex(), "")
            if (src.length % 4 != 0) {
                throw Exception("Base64 string length is not a multiple of 4.")
            }
        }
        /*
        val doc = Ksoup.parseFile(TestHelper.getResourceAbsolutePath("htmltests/issue19.html"))
        resolveFolderChildInfos(doc)*/
    }

    fun resolveFolderChildInfos(doc: Document) {
        val body = doc.select("body")
        val main = body.select("main").first()!!
        val ul = main.children()[0]
        val li = ul.getElementsByTag("li")
        li.forEach { fi ->
            val childElement = fi.getElementsByTag("a")[0]
            val childClass = childElement.attr("class")
            val isFolder = childClass == "folder-link"
            val spans = fi.getElementsByTag("span")
            if (isFolder) {
                // Skip
            } else {
                val thumbnailBase64 = spans.select(".document-icon").first()?.select("img")?.first()?.attr("src")
                val thumbnailBytes = resolveImage(thumbnailBase64)
                assertTrue { thumbnailBytes != null && thumbnailBytes.isNotEmpty() }
            }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun resolveImage(imgBase64: String?): ByteArray? {
        return imgBase64?.let {
            lateinit var strippedImgBase64: String
            try {
                if (it.startsWith(PNG_BASE64_HEADER)) {
                    strippedImgBase64 = it.replaceFirst(PNG_BASE64_HEADER, "")
                    if (strippedImgBase64.length % 4 != 0) {
                        println("strippedImgBase64: $strippedImgBase64")
                        throw Exception("Base64 string length is not a multiple of 4.")
                    }
                    Base64.Default.decode(strippedImgBase64)
                } else {
                    println("Unknown image format: '${imgBase64.take(20)}'")
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw Exception(
                    "Could not resolve image from base 64 string. (start='${
                        imgBase64.replaceFirst(PNG_BASE64_HEADER, "").take(
                            100,
                        )
                    }', end='${imgBase64.takeLast(20)}', base64Length=${strippedImgBase64.length})",
                    e,
                )
            }
        }
    }
}
