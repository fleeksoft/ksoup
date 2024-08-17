package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.Ksoup.parse
import com.fleeksoft.ksoup.TestHelper
import com.fleeksoft.ksoup.internal.StringUtil.inSorted
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HtmlTreeBuilderStateTest {
    @BeforeTest
    fun initKsoup() {
        TestHelper.initKsoup()
    }

    private val constantArrays = arrayListOf(
        HtmlTreeBuilderState.Constants.InHeadEmpty,
        HtmlTreeBuilderState.Constants.InHeadRaw,
        HtmlTreeBuilderState.Constants.InHeadEnd,
        HtmlTreeBuilderState.Constants.AfterHeadBody,
        HtmlTreeBuilderState.Constants.BeforeHtmlToHead,
        HtmlTreeBuilderState.Constants.InHeadNoScriptHead,
        HtmlTreeBuilderState.Constants.InBodyStartToHead,
        HtmlTreeBuilderState.Constants.InBodyStartPClosers,
        HtmlTreeBuilderState.Constants.Headings,
        HtmlTreeBuilderState.Constants.InBodyStartLiBreakers,
        HtmlTreeBuilderState.Constants.DdDt,
        HtmlTreeBuilderState.Constants.InBodyStartApplets,
        HtmlTreeBuilderState.Constants.InBodyStartMedia,
        HtmlTreeBuilderState.Constants.InBodyStartInputAttribs,
        HtmlTreeBuilderState.Constants.InBodyStartDrop,
        HtmlTreeBuilderState.Constants.InBodyEndClosers,
        HtmlTreeBuilderState.Constants.InBodyEndOtherErrors,
        HtmlTreeBuilderState.Constants.InBodyEndAdoptionFormatters,
        HtmlTreeBuilderState.Constants.InBodyEndTableFosters,
        HtmlTreeBuilderState.Constants.InTableToBody,
        HtmlTreeBuilderState.Constants.InTableAddBody,
        HtmlTreeBuilderState.Constants.InTableToHead,
        HtmlTreeBuilderState.Constants.InCellNames,
        HtmlTreeBuilderState.Constants.InCellBody,
        HtmlTreeBuilderState.Constants.InCellTable,
        HtmlTreeBuilderState.Constants.InCellCol,
        HtmlTreeBuilderState.Constants.InTableEndErr,
        HtmlTreeBuilderState.Constants.InTableFoster,
        HtmlTreeBuilderState.Constants.InTableBodyExit,
        HtmlTreeBuilderState.Constants.InTableBodyEndIgnore,
        HtmlTreeBuilderState.Constants.InRowMissing,
        HtmlTreeBuilderState.Constants.InRowIgnore,
        HtmlTreeBuilderState.Constants.InSelectEnd,
        HtmlTreeBuilderState.Constants.InSelectTableEnd,
        HtmlTreeBuilderState.Constants.InTableEndIgnore,
        HtmlTreeBuilderState.Constants.InHeadNoscriptIgnore,
        HtmlTreeBuilderState.Constants.InCaptionIgnore,
        HtmlTreeBuilderState.Constants.InTemplateToHead,
        HtmlTreeBuilderState.Constants.InTemplateToTable,
        HtmlTreeBuilderState.Constants.InForeignToHtml,
    )

    @Test
    fun ensureArraysAreSorted() {
        ensureSorted(
            constantArrays,
        )
        assertEquals(40, constantArrays.size)
    }

    @Test
    fun ensureTagSearchesAreKnownTags() {
        for (constant in constantArrays) {
            for (tagName in constant) {
                if (inSorted(
                        tagName,
                        HtmlTreeBuilderState.Constants.InBodyStartInputAttribs,
                    )
                ) {
                    continue // odd one out in the constant
                }
                assertTrue(Tag.isKnownTag(tagName), "Unknown tag name: $tagName")
            }
        }
    }

    @Test
    fun nestedAnchorElements01() {
        val html = """<html>
  <body>
    <a href='#1'>
        <div>
          <a href='#2'>child</a>
        </div>
    </a>
  </body>
</html>"""
        val s = parse(html).toString()
        assertEquals(
            """<html>
 <head></head>
 <body>
  <a href="#1"> </a>
  <div>
   <a href="#1"> </a><a href="#2">child</a>
  </div>
 </body>
</html>""",
            s,
        )
    }

    @Test
    fun nestedAnchorElements02() {
        val html = """<html>
  <body>
    <a href='#1'>
      <div>
        <div>
          <a href='#2'>child</a>
        </div>
      </div>
    </a>
  </body>
</html>"""
        val s = parse(html).toString()
        assertEquals(
            """<html>
 <head></head>
 <body>
  <a href="#1"> </a>
  <div>
   <a href="#1"> </a>
   <div>
    <a href="#1"> </a><a href="#2">child</a>
   </div>
  </div>
 </body>
</html>""",
            s,
        )
    }

    companion object {
        fun ensureSorted(constants: ArrayList<Array<String>>) {
            for (array in constants) {
                val copy = array.copyOf(array.size)
                array.sort()
                assertTrue(array.contentEquals(copy))
//                assertArrayEquals(array, copy)
            }
        }
    }
}
