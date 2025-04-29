package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.select.Selector.SelectorParseException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame


/**
 * Tests for the Selector Query Parser.
 *
 */
class QueryParserTest {

    @Test
    fun testConsumeSubQuery() {
        val doc: Document = Ksoup.parse(
            "<html><head>h</head><body>" +
                    "<li><strong>l1</strong></li>" +
                    "<a><li><strong>l2</strong></li></a>" +
                    "<p><strong>yes</strong></p>" +
                    "</body></html>",
        )
        assertEquals("l1 yes", doc.body().select(">p>strong,>li>strong").text()) // selecting immediate from body
        assertEquals("l1 yes", doc.body().select(" > p > strong , > li > strong").text()); // space variants
        assertEquals("l2 yes", doc.select("body>p>strong,body>*>li>strong").text())
        assertEquals("l2 yes", doc.select("body>*>li>strong,body>p>strong").text())
        assertEquals("l2 yes", doc.select("body>p>strong,body>*>li>strong").text())
    }

    @Test
    fun testImmediateParentRun() {
        val query = "div > p > bold.brass"
        assertEquals(
            "(ImmediateParentRun (Tag 'div')(Tag 'p')(And (Tag 'bold')(Class '.brass')))",
            EvaluatorDebug.sexpr(query),
        )

        /*
        <ImmediateParentRun css="div > p > bold.brass" cost="11">
          <Tag css="div" cost="1"></Tag>
          <Tag css="p" cost="1"></Tag>
          <And css="bold.brass" cost="7">
            <Tag css="bold" cost="1"></Tag>
            <Class css=".brass" cost="6"></Class>
          </And>
        </ImmediateParentRun>
         */
    }

    @Test
    fun testOrGetsCorrectPrecedence() {
        // tests that a selector "a b, c d, e f" evals to (a AND b) OR (c AND d) OR (e AND f)"
        // top level or, three child ands
        val query = "a b, c d, e f"
        val parsed: String = EvaluatorDebug.sexpr(query)
        assertEquals("(Or (And (Tag 'b')(Ancestor (Tag 'a')))(And (Tag 'd')(Ancestor (Tag 'c')))(And (Tag 'f')(Ancestor (Tag 'e'))))", parsed)

        /*
        <Or css="a b, c d, e f" cost="9">
          <And css="a b" cost="3">
            <Tag css="b" cost="1"></Tag>
            <Parent css="a " cost="2">
              <Tag css="a" cost="1"></Tag>
            </Parent>
          </And>
          <And css="c d" cost="3">
            <Tag css="d" cost="1"></Tag>
            <Parent css="c " cost="2">
              <Tag css="c" cost="1"></Tag>
            </Parent>
          </And>
          <And css="e f" cost="3">
            <Tag css="f" cost="1"></Tag>
            <Parent css="e " cost="2">
              <Tag css="e" cost="1"></Tag>
            </Parent>
          </And>
        </Or>
         */
    }

    @Test
    fun testParsesMultiCorrectly() {
        val query = ".foo.qux[attr=bar] > ol.bar, ol > li + li"
        val parsed: String = EvaluatorDebug.sexpr(query)
        assertEquals(
            "(Or (And (Tag 'li')(ImmediatePreviousSibling (ImmediateParentRun (Tag 'ol')(Tag 'li'))))(ImmediateParentRun (And (AttributeWithValue '[attr=bar]')(Class '.foo')(Class '.qux'))(And (Tag 'ol')(Class '.bar'))))",
            parsed,
        )

        /*
        <Or css=".foo.qux[attr=bar] > ol.bar, ol > li + li" cost="31">
          <And css="ol > li + li" cost="7">
            <Tag css="li" cost="1"></Tag>
            <ImmediatePreviousSibling css="ol > li + " cost="6">
              <ImmediateParentRun css="ol > li" cost="4">
                <Tag css="ol" cost="1"></Tag>
                <Tag css="li" cost="1"></Tag>
              </ImmediateParentRun>
            </ImmediatePreviousSibling>
          </And>
          <ImmediateParentRun css=".foo.qux[attr=bar] > ol.bar" cost="24">
            <And css=".foo.qux[attr=bar]" cost="15">
              <AttributeWithValue css="[attr=bar]" cost="3"></AttributeWithValue>
              <Class css=".foo" cost="6"></Class>
              <Class css=".qux" cost="6"></Class>
            </And>
            <And css="ol.bar" cost="7">
              <Tag css="ol" cost="1"></Tag>
              <Class css=".bar" cost="6"></Class>
            </And>
          </ImmediateParentRun>
        </Or>
         */
    }

    @Test
    fun idDescenderClassOrder() {
        // https://github.com/jhy/Ksoup/issues/2254
        // '#id .class' cost
        val query = "#id .class"
        val parsed: String = EvaluatorDebug.sexpr(query)
        assertEquals("(And (Class '.class')(Ancestor (Id '#id')))", parsed)

        /*
        <And css="#id .class" cost="22">
         <Class css=".class" cost="6"></Class>
         <Ancestor css="#id " cost="16">
          <Id css="#id" cost="2"></Id>
         </Ancestor>
        </And>
         */
    }


    @Test
    fun exceptionOnUncloseAttribute() {
        val exception: SelectorParseException =
            assertFailsWith<SelectorParseException> { QueryParser.parse("section > a[href=\"]") }
        assertEquals(
            "Did not find balanced marker at 'href=\"]'",
            exception.message
        )
    }

    @Test
    fun testParsesSingleQuoteInContains() {
        val exception: SelectorParseException =
            assertFailsWith<SelectorParseException> { QueryParser.parse("p:contains(One \" One)") }
        assertEquals(
            "Did not find balanced marker at 'One \" One)'",
            exception.message
        )
    }

    @Test
    fun exceptOnEmptySelector() {
        val exception: SelectorParseException = assertFailsWith<SelectorParseException> { QueryParser.parse("") }
        assertEquals("String must not be empty", exception.message)
    }

    @Test
    fun exceptOnUnhandledEvaluator() {
        val exception: SelectorParseException =
            assertFailsWith<SelectorParseException> { QueryParser.parse("div / foo") }
        assertEquals("Could not parse query 'div / foo': unexpected token at '/ foo'", exception.message)
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
        val parsed: String = EvaluatorDebug.sexpr(q)
        assertEquals(
            "(And (Tag 'g')(PreviousSibling (And (Tag 'f')(ImmediatePreviousSibling (ImmediateParentRun (And (Tag 'd')(Ancestor (And (Tag 'b')(Ancestor (And (Tag 'a')(Not (Has (And (Tag 'span')(Class '.foo')))))))))(Tag 'e'))))))",
            parsed
        )
    }

    @Test
    fun parsesOrAfterAttribute() {
        // https://github.com/jhy/Ksoup/issues/2073
        val q = "#parent [class*=child], .some-other-selector .nested"
        val parsed: String? = EvaluatorDebug.sexpr(q)
        assertEquals(
            "(Or (And (AttributeWithValueContaining '[class*=child]')(Ancestor (Id '#parent')))(And (Class '.nested')(Ancestor (Class '.some-other-selector'))))",
            parsed
        )

        assertEquals(
            "(Or (Class '.some-other-selector')(And (AttributeWithValueContaining '[class*=child]')(Ancestor (Id '#parent'))))",
            EvaluatorDebug.sexpr("#parent [class*=child], .some-other-selector")
        )
        assertEquals(
            "(Or (And (Id '#el')(AttributeWithValueContaining '[class*=child]'))(Class '.some-other-selector'))",
            EvaluatorDebug.sexpr("#el[class*=child], .some-other-selector")
        )
        assertEquals(
            "(Or (And (AttributeWithValueContaining '[class*=child]')(Ancestor (Id '#parent')))(And (Class '.nested')(Ancestor (Class '.some-other-selector'))))",
            EvaluatorDebug.sexpr("#parent [class*=child], .some-other-selector .nested")
        )
    }

    @Test
    fun parsesEscapedSubqueries() {
        val html = "<div class='-4a'>One</div> <div id='-4a'>Two</div>"
        val doc = Ksoup.parse(html)

        val classQ = "div.-\\34 a"
        val div1 = doc.expectFirst(classQ)
        assertEquals("One", div1.wholeText())

        val idQ = "#-\\34 a"
        val div2 = doc.expectFirst(idQ)
        assertEquals("Two", div2.wholeText())

        val genClassQ = "html > body > div.-\\34 a"
        assertEquals(genClassQ, div1.cssSelector())
        assertSame(div1, doc.expectFirst(genClassQ))

        val deepIdQ = "html > body > #-\\34 a"
        assertEquals(idQ, div2.cssSelector())
        assertSame(div2, doc.expectFirst(deepIdQ))

        assertEquals("(ImmediateParentRun (Tag 'html')(Tag 'body')(And (Tag 'div')(Class '.-4a')))", EvaluatorDebug.sexpr(genClassQ))
        assertEquals("(ImmediateParentRun (Tag 'html')(Tag 'body')(Id '#-4a'))", EvaluatorDebug.sexpr(deepIdQ))
    }

    @Test
    fun trailingParens() {
        val exception: SelectorParseException =
            assertFailsWith<SelectorParseException> { QueryParser.parse("div:has(p))") }
        assertEquals("Could not parse query 'div:has(p))': unexpected token at ')'", exception.message)
    }

    @Test
    fun consecutiveCombinators() {
        val exception1: SelectorParseException = assertFailsWith<SelectorParseException> { QueryParser.parse("div>>p") }
        assertEquals(
            "Could not parse query 'div>>p': unexpected token at '>p'",
            exception1.message
        )

        val exception2: SelectorParseException = assertFailsWith<SelectorParseException> { QueryParser.parse("+ + div") }
        assertEquals(
            "Could not parse query '+ + div': unexpected token at '+ div'",
            exception2.message
        )
    }
}
