package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import kotlin.test.Test
import kotlin.test.assertEquals


class EvaluatorTest {
    @Test
    fun testTagToString() {
        val evaluator = Evaluator.Tag("div")
        assertEquals("div", evaluator.toString())
    }

    @Test
    fun testTagStartsWithToString() {
        val evaluator = Evaluator.TagStartsWith("ns")
        assertEquals("ns|*", evaluator.toString())
    }

    @Test
    fun testTagEndsWithToString() {
        val evaluator = Evaluator.TagEndsWith("div")
        assertEquals("*|div", evaluator.toString())
    }

    @Test
    fun testAttributeToString() {
        val evaluator = Evaluator.Attribute("example")
        assertEquals("[example]", evaluator.toString())
    }

    @Test
    fun testAttributeStartingToString() {
        val evaluator = Evaluator.AttributeStarting("example")
        assertEquals("[^example]", evaluator.toString())
    }

    @Test
    fun testAttributeWithValueToString() {
        val evaluator = Evaluator.AttributeWithValue("example", "value")
        assertEquals("[example=value]", evaluator.toString())
    }

    @Test
    fun testAttributeWithValueNotToString() {
        val evaluator = Evaluator.AttributeWithValueNot("example", "value")
        assertEquals("[example!=value]", evaluator.toString())
    }

    @Test
    fun testAttributeWithValueStartingToString() {
        val evaluator = Evaluator.AttributeWithValueStarting("example", "value")
        assertEquals("[example^=value]", evaluator.toString())
    }

    @Test
    fun testAttributeWithValueEndingToString() {
        val evaluator = Evaluator.AttributeWithValueEnding("example", "value")
        assertEquals("[example$=value]", evaluator.toString())
    }

    @Test
    fun testAttributeWithValueContainingToString() {
        val evaluator =
            Evaluator.AttributeWithValueContaining("example", "value")
        assertEquals("[example*=value]", evaluator.toString())
    }

    @Test
    fun testAttributeWithValueMatchingToString() {
        val regex = Regex("value")
        val evaluator = Evaluator.AttributeWithValueMatching("example", regex)
        assertEquals("[example~=value]", evaluator.toString())
    }

    @Test
    fun testIdToString() {
        val evaluator = Evaluator.Id("exampleId")
        assertEquals("#exampleId", evaluator.toString())
    }

    @Test
    fun testClassToString() {
        val evaluator = Evaluator.Class("exampleClass")
        assertEquals(".exampleClass", evaluator.toString())
    }

    @Test
    fun testAllElementsToString() {
        val evaluator = Evaluator.AllElements()
        assertEquals("*", evaluator.toString())
    }

    @Test
    fun testIndexLessThanToString() {
        val evaluator = Evaluator.IndexLessThan(5)
        assertEquals(":lt(5)", evaluator.toString())
    }

    @Test
    fun testIndexGreaterThanToString() {
        val evaluator = Evaluator.IndexGreaterThan(5)
        assertEquals(":gt(5)", evaluator.toString())
    }

    @Test
    fun testIndexEqualsToString() {
        val evaluator = Evaluator.IndexEquals(5)
        assertEquals(":eq(5)", evaluator.toString())
    }

    @Test
    fun testIsLastChildToString() {
        val evaluator = Evaluator.IsLastChild()
        assertEquals(":last-child", evaluator.toString())
    }

    @Test
    fun testIsFirstOfTypeToString() {
        val evaluator = Evaluator.IsFirstOfType()
        assertEquals(":first-of-type", evaluator.toString())
    }

    @Test
    fun testIsLastOfTypeToString() {
        val evaluator = Evaluator.IsLastOfType()
        assertEquals(":last-of-type", evaluator.toString())
    }

    @Test
    fun testIsNthChildToStringVariants() {
        val evaluator1 = Evaluator.IsNthChild(0, 3)
        assertEquals(":nth-child(3)", evaluator1.toString())

        val evaluator2 = Evaluator.IsNthChild(2, 0)
        assertEquals(":nth-child(2n)", evaluator2.toString())

        val evaluator3 = Evaluator.IsNthChild(2, 3)
        assertEquals(":nth-child(2n+3)", evaluator3.toString())
    }

    @Test
    fun testIsNthChildToString() {
        val evaluator = Evaluator.IsNthChild(2, 3)
        assertEquals(":nth-child(2n+3)", evaluator.toString())
    }

    @Test
    fun testIsNthLastChildToString() {
        val evaluator = Evaluator.IsNthLastChild(2, 3)
        assertEquals(":nth-last-child(2n+3)", evaluator.toString())
    }

    @Test
    fun testIsNthOfTypeToString() {
        val evaluator = Evaluator.IsNthOfType(2, 3)
        assertEquals(":nth-of-type(2n+3)", evaluator.toString())
    }

    @Test
    fun testIsNthLastOfTypeToString() {
        val evaluator = Evaluator.IsNthLastOfType(2, 3)
        assertEquals(":nth-last-of-type(2n+3)", evaluator.toString())
    }

    @Test
    fun testIsFirstChildToString() {
        val evaluator = Evaluator.IsFirstChild()
        assertEquals(":first-child", evaluator.toString())
    }

    @Test
    fun testIsRootToString() {
        val evaluator = Evaluator.IsRoot()
        assertEquals(":root", evaluator.toString())
    }

    @Test
    fun testIsOnlyChildToString() {
        val evaluator = Evaluator.IsOnlyChild()
        assertEquals(":only-child", evaluator.toString())
    }

    @Test
    fun testIsOnlyOfTypeToString() {
        val evaluator = Evaluator.IsOnlyOfType()
        assertEquals(":only-of-type", evaluator.toString())
    }

    @Test
    fun testIsEmptyToString() {
        val evaluator = Evaluator.IsEmpty()
        assertEquals(":empty", evaluator.toString())
    }

    @Test
    fun testContainsTextToString() {
        val evaluator = Evaluator.ContainsText("example")
        assertEquals(":contains(example)", evaluator.toString())
    }

    @Test
    fun testContainsWholeTextToString() {
        val evaluator = Evaluator.ContainsWholeText("example")
        assertEquals(":containsWholeText(example)", evaluator.toString())
    }

    @Test
    fun testContainsWholeOwnTextToString() {
        val evaluator = Evaluator.ContainsWholeOwnText("example")
        assertEquals(":containsWholeOwnText(example)", evaluator.toString())
    }

    @Test
    fun testContainsDataToString() {
        val evaluator = Evaluator.ContainsData("example")
        assertEquals(":containsData(example)", evaluator.toString())
    }

    @Test
    fun testContainsOwnTextToString() {
        val evaluator = Evaluator.ContainsOwnText("example")
        assertEquals(":containsOwn(example)", evaluator.toString())
    }

    @Test
    fun testMatchesToString() {
        val regex = Regex("example")
        val evaluator = Evaluator.Matches(regex)
        assertEquals(":matches(example)", evaluator.toString())
    }

    @Test
    fun testMatchesOwnToString() {
        val regex = Regex("example")
        val evaluator = Evaluator.MatchesOwn(regex)
        assertEquals(":matchesOwn(example)", evaluator.toString())
    }

    @Test
    fun testMatchesWholeTextToString() {
        val regex = Regex("example")
        val evaluator = Evaluator.MatchesWholeText(regex)
        assertEquals(":matchesWholeText(example)", evaluator.toString())
    }

    @Test
    fun testMatchesWholeOwnTextToString() {
        val regex = Regex("example")
        val evaluator = Evaluator.MatchesWholeOwnText(regex)
        assertEquals(":matchesWholeOwnText(example)", evaluator.toString())
    }

    @Test
    fun testMatchTextToString() {
        val evaluator = Evaluator.MatchText()
        assertEquals(":matchText", evaluator.toString())
    }

    @Test
    fun nthPosition() {
        val orphan = Element("div")
        val doc: Document = Ksoup.parse("<div><p>One<p>Two<p>Three<p>Four</p><h1>Five</h1></div>")
        val div = doc.expectFirst("div")
        val ps = doc.select("p")
        val h1 = doc.expectFirst("h1")

        val lastchild: Evaluator.CssNthEvaluator = Evaluator.IsNthLastChild(1, 0)
        assertEquals(0, lastchild.calculatePosition(orphan, orphan))
        assertEquals(2, lastchild.calculatePosition(div, ps[3]))

        val nthType = Evaluator.IsNthOfType(1, 0)
        assertEquals(0, nthType.calculatePosition(orphan, orphan))
        assertEquals(1, nthType.calculatePosition(div, ps[0]))
        assertEquals(2, nthType.calculatePosition(div, ps[1]))
        assertEquals(1, nthType.calculatePosition(div, h1))

        val nthLastType = Evaluator.IsNthLastOfType(1, 0)
        assertEquals(0, nthLastType.calculatePosition(orphan, orphan))
        assertEquals(4, nthLastType.calculatePosition(div, ps[0]))
        assertEquals(3, nthLastType.calculatePosition(div, ps[1]))
        assertEquals(1, nthLastType.calculatePosition(div, h1))
    }
}