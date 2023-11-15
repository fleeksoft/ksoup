package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.Ksoup.parse
import com.fleeksoft.ksoup.select.StructuralEvaluator.ImmediateParentRun
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests for the Selector Query Parser.
 *
 * @author Sabeeh
 */
class QueryParserTest {
    @Test
    fun testConsumeSubQuery() {
        val doc = parse(
            "<html><head>h</head><body>" +
                    "<li><strong>l1</strong></li>" +
                    "<a><li><strong>l2</strong></li></a>" +
                    "<p><strong>yes</strong></p>" +
                    "</body></html>"
        )
        assertEquals(
            "l1 yes",
            doc.body().select(">p>strong,>li>strong").text()
        ) // selecting immediate from body
        assertEquals("l2 yes", doc.select("body>p>strong,body>*>li>strong").text())
        assertEquals("l2 yes", doc.select("body>*>li>strong,body>p>strong").text())
        assertEquals("l2 yes", doc.select("body>p>strong,body>*>li>strong").text())
    }

    @Test
    fun testImmediateParentRun() {
        val query = "div > p > bold.brass"
        val eval1 = QueryParser.parse(query)
        assertEquals(query, eval1.toString())
        val run = eval1 as ImmediateParentRun
        assertTrue(run.evaluators[0] is Evaluator.Tag)
        assertTrue(run.evaluators[1] is Evaluator.Tag)
        assertTrue(run.evaluators[2] is CombiningEvaluator.And)
    }

    @Test
    fun testOrGetsCorrectPrecedence() {
        // tests that a selector "a b, c d, e f" evals to (a AND b) OR (c AND d) OR (e AND f)"
        // top level or, three child ands
        val eval = QueryParser.parse("a b, c d, e f")
        assertTrue(eval is CombiningEvaluator.Or)
        val or = eval as CombiningEvaluator.Or
        assertEquals(3, or.evaluators.size)
        for (innerEval in or.evaluators) {
            assertTrue(innerEval is CombiningEvaluator.And)
            val and = innerEval as CombiningEvaluator.And
            assertEquals(2, and.evaluators.size)
            assertTrue(and.evaluators[0] is StructuralEvaluator.Parent)
            assertTrue(and.evaluators[1] is Evaluator.Tag)
        }
    }

    @Test
    fun testParsesMultiCorrectly() {
        val query = ".foo.qux > ol.bar, ol > li + li"
        val eval = QueryParser.parse(query)
        assertTrue(eval is CombiningEvaluator.Or)
        val or = eval as CombiningEvaluator.Or
        assertEquals(2, or.evaluators.size)
        val run = or.evaluators[0] as ImmediateParentRun
        val andRight = or.evaluators[1] as CombiningEvaluator.And
        assertEquals(".foo.qux > ol.bar", run.toString())
        assertEquals(2, run.evaluators.size)
        val runAnd = run.evaluators[0]
        assertTrue(runAnd is CombiningEvaluator.And)
        assertEquals(".foo.qux", runAnd.toString())
        assertEquals("ol > li + li", andRight.toString())
        assertEquals(2, andRight.evaluators.size)
        assertEquals(query, eval.toString())
    }

    @Test
    fun exceptionOnUncloseAttribute() {
        assertFailsWith<Selector.SelectorParseException> { QueryParser.parse("section > a[href=\"]") }
    }

    @Test
    fun testParsesSingleQuoteInContains() {
        assertFailsWith<Selector.SelectorParseException> { QueryParser.parse("p:contains(One \" One)") }
    }

    @Test
    fun exceptOnEmptySelector() {
        assertFailsWith<Selector.SelectorParseException> { QueryParser.parse("") }
    }

    @Test
    fun okOnSpacesForeAndAft() {
        val parse = QueryParser.parse(" span div  ")
        assertEquals("span div", parse.toString())
    }

    @Test
    fun structuralEvaluatorsToString() {
        val q = "a:not(:has(span.foo)) b d > e + f ~ g"
        val parse = QueryParser.parse(q)
        assertEquals(q, parse.toString())
    }
}
