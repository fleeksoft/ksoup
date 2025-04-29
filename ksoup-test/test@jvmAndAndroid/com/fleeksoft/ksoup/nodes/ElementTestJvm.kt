package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.Ksoup
import org.junit.Test
import kotlin.test.assertEquals


class ElementTestJvm {

    //    StringBuffer adding \n in start but not when using StringBuilder
    @Test
    fun outerHtmlAppendable() {
        val doc: Document = Ksoup.parse("<div>One</div>")
        val buffer = StringBuffer()
        doc.body().outerHtml(buffer)
        assertEquals("<body>\n <div>One</div>\n</body>", buffer.toString())
        val builder = StringBuilder()
        doc.body().outerHtml(builder)
        assertEquals("<body>\n <div>One</div>\n</body>", builder.toString())
    }
}
