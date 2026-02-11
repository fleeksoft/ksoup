package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parameterizedTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class StructuralEvaluatorTest {
    @Test
    fun selectorMemoIsClearedOnReset() = parameterizedTest(selectorMemoData()) { pair: Pair<String, Boolean> ->
        val selector = pair.first
        val expectMemos = pair.second
        // test that the structural evaluator memos are used, and are reset

        val doc: Document = Ksoup.parse(Html)
        val evaluator: Evaluator = Selector.evaluatorOf(selector)

        // collect all StructuralEvaluator instances from the parsed evaluator tree
        val structuralEvals: ArrayList<StructuralEvaluator> = ArrayList()
        collectEvals(evaluator, structuralEvals)

        // use Collector.stream vs Selector.select(), as the later is able to reset after executing
        Collector.stream(evaluator, doc).count() // consume stream to populate memos
        assertFalse(structuralEvals.isEmpty())

        var hadMemos = false
        for (se in structuralEvals) {
            if (!se.threadMemo.get().isEmpty()) {
                hadMemos = true
                break
            }
        }

        evaluator.reset()

        // verify all structural evaluator thread-local maps are cleared
        for (se in structuralEvals) {
            assertTrue(se.threadMemo.get().isEmpty())
        }

        assertEquals(expectMemos, hadMemos)
    }

    companion object {
        private val Html = "<div id=outer>" +
                "  <div class=a>" +
                "    <p class=p1>One</p>" +
                "    <p class=p2>Two</p>" +
                "  </div>" +
                "  <div class=b>" +
                "    <span>Span1</span>" +
                "    <a href=# class=link>Link</a>" +
                "  </div>" +
                "  <div class=c>" +
                "    <div class=inner>" +
                "      <p class=target>Target</p>" +
                "    </div>" +
                "  </div>" +
                "</div>"

        private fun selectorMemoData() = listOf(
            "div:not(.b)" to true,  // Not (uses memoMatches)
            "div p" to true,  // Ancestor (ancestor chain checks)
            "span ~ a" to true,  // PreviousSibling
            "span + a" to true,  // ImmediatePreviousSibling
            "div > span > a" to false,  // ImmediateParentRun does not use memoMatches
            "div:has(p)" to false // Has (coverage; does not use memo for these inputs)
        )

        private fun collectEvals(evaluator: Evaluator, out: ArrayList<StructuralEvaluator>) {
            // recursive traversal of evaluator trees to find StructuralEvaluator instances
            if (evaluator is CombiningEvaluator) {
                val ce: CombiningEvaluator = evaluator
                for (inner in ce.evaluators) {
                    collectEvals(inner, out)
                }
                return
            }

            if (evaluator is StructuralEvaluator.ImmediateParentRun) {
                val run: StructuralEvaluator.ImmediateParentRun = evaluator
                out.add(run)
                for (inner in run.evaluators) {
                    collectEvals(inner, out)
                }
                return
            }

            if (evaluator is StructuralEvaluator) {
                val se: StructuralEvaluator = evaluator as StructuralEvaluator
                out.add(se)
                collectEvals(se.evaluator, out)
            }
        }
    }
}
