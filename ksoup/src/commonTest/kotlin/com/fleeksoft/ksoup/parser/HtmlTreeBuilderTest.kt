package com.fleeksoft.ksoup.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlTreeBuilderTest {
    private val htmlTreeBuilderConstants = arrayListOf(
        HtmlTreeBuilder.TagsSearchInScope,
        HtmlTreeBuilder.TagSearchList,
        HtmlTreeBuilder.TagSearchButton,
        HtmlTreeBuilder.TagSearchTableScope,
        HtmlTreeBuilder.TagSearchSelectScope,
        HtmlTreeBuilder.TagSearchEndTags,
        HtmlTreeBuilder.TagThoroughSearchEndTags,
        HtmlTreeBuilder.TagSearchSpecial,
        HtmlTreeBuilder.TagMathMlTextIntegration,
        HtmlTreeBuilder.TagSvgHtmlIntegration,
    )

    @Test
    fun ensureSearchArraysAreSorted() {
        val constants: ArrayList<Array<String>> = htmlTreeBuilderConstants
        HtmlTreeBuilderStateTest.ensureSorted(constants)
        assertEquals(10, constants.size)
    }
}