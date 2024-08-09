package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.helper.computeIfAbsent
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.NodeIterator
import com.fleeksoft.ksoup.ported.IdentityHashMap
import com.fleeksoft.ksoup.ported.ThreadLocal

/**
 * Base structural evaluator.
 */
public abstract class StructuralEvaluator(public val evaluator: Evaluator) : Evaluator() {
    // Memoize inner matches, to save repeated re-evaluations of parent, sibling etc.
    // root + element: Boolean matches. ThreadLocal in case the Evaluator is compiled then reused across multi threads
    public val threadMemo: ThreadLocal<IdentityHashMap<Element, IdentityHashMap<Element, Boolean>>> =
        ThreadLocal { IdentityHashMap() }

    public fun memoMatches(
        root: Element,
        element: Element,
    ): Boolean {
        val rootMemo = threadMemo.get()
        val memo: MutableMap<Element, Boolean> = rootMemo.computeIfAbsent(root) { IdentityHashMap() }
        return memo.computeIfAbsent(element) { key -> evaluator.matches(root, key) }
    }

    override fun reset() {
        threadMemo.get()?.clear()
        super.reset()
    }

    internal class Root : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return root === element
        }

        override fun cost(): Int {
            return 1
        }

        override fun toString(): String {
            return ""
        }
    }

    internal class Has(evaluator: Evaluator) : StructuralEvaluator(evaluator) {
        companion object {
            private val nodeIterator: ThreadLocal<NodeIterator<Element>> =
                ThreadLocal { NodeIterator(Element("html"), Element::class) }
        }

        private val checkSiblings = evalWantsSiblings(evaluator) // evaluating against siblings (or children)

        override fun matches(root: Element, element: Element): Boolean {
            if (checkSiblings) { // evaluating against siblings
                var sib = element.firstElementSibling()
                while (sib != null) {
                    if (sib !== element && evaluator.matches(element, sib)) { // don't match against self
                        return true
                    }
                    sib = sib.nextElementSibling()
                }
            } else {
                // otherwise we only want to match children (or below), and not the input element. And we want to minimize GCs so reusing the Iterator obj
                val it = nodeIterator.get()
                it.restart(element)
                while (it.hasNext()) {
                    val el = it.next()
                    if (el === element) continue  // don't match self, only descendants

                    if (evaluator.matches(element, el)) return true
                }
            }
            return false
        }

        /* Test if the :has sub-clause wants sibling elements (vs nested elements) - will be a Combining eval */
        private fun evalWantsSiblings(eval: Evaluator): Boolean {
            if (eval is CombiningEvaluator) {
                for (innerEval in eval.evaluators) {
                    if (innerEval is PreviousSibling || innerEval is ImmediatePreviousSibling) return true
                }
            }
            return false
        }

        override fun cost(): Int {
            return 10 * evaluator.cost()
        }

        override fun toString(): String {
            return ":has($evaluator)"
        }
    }

    /** Implements the :is(sub-query) pseudo-selector  */
    internal class Is(evaluator: Evaluator) : StructuralEvaluator(evaluator) {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return evaluator.matches(root, element)
        }

        override fun cost(): Int {
            return 2 + evaluator.cost()
        }

        override fun toString(): String {
            return ":is($evaluator)"
        }
    }

    internal class Not(evaluator: Evaluator) : StructuralEvaluator(evaluator) {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return !memoMatches(root, element)
        }

        override fun cost(): Int {
            return 2 + evaluator.cost()
        }

        override fun toString(): String {
            return ":not($evaluator)"
        }
    }

    public class Parent(evaluator: Evaluator) : StructuralEvaluator(evaluator) {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            if (root === element) return false
            var parent: Element? = element.parent()
            while (parent != null) {
                if (memoMatches(root, parent)) return true
                if (parent === root) break
                parent = parent.parent()
            }
            return false
        }

        override fun cost(): Int {
            return 2 * evaluator.cost()
        }

        override fun toString(): String {
            return "$evaluator "
        }
    }

    /**
     * Holds a list of evaluators for one > two > three immediate parent matches, and the final direct evaluator under
     * test. To match, these are effectively ANDed together, starting from the last, matching up to the first.
     */
    public class ImmediateParentRun(evaluator: Evaluator) : Evaluator() {
        public val evaluators: ArrayList<Evaluator> = ArrayList<Evaluator>()
        private var _cost = 2

        init {
            evaluators.add(evaluator)
            _cost += evaluator.cost()
        }

        public fun add(evaluator: Evaluator) {
            evaluators.add(evaluator)
            _cost += evaluator.cost()
        }

        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            var el: Element? = element
            if (el === root) return false // cannot match as the second eval (first parent test) would be above the root
            for (i in evaluators.indices.reversed()) {
                if (el == null) return false
                val eval: Evaluator = evaluators.get(i)
                if (!eval.matches(root, el)) return false
                el = el.parent()
            }
            return true
        }

        override fun cost(): Int {
            return _cost
        }

        override fun toString(): String {
            return StringUtil.join(evaluators, " > ")
        }
    }

    public class PreviousSibling(evaluator: Evaluator) : StructuralEvaluator(evaluator) {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            if (root === element) return false
            var sibling: Element? = element.firstElementSibling()
            while (sibling != null) {
                if (sibling === element) break
                if (memoMatches(root, sibling)) return true
                sibling = sibling.nextElementSibling()
            }
            return false
        }

        override fun cost(): Int {
            return 3 * evaluator.cost()
        }

        override fun toString(): String {
            return "$evaluator ~ "
        }
    }

    internal class ImmediatePreviousSibling(evaluator: Evaluator) : StructuralEvaluator(evaluator) {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            if (root === element) return false
            val prev: Element? = element.previousElementSibling()
            return prev != null && memoMatches(root, prev)
        }

        override fun cost(): Int {
            return 2 + evaluator.cost()
        }

        override fun toString(): String {
            return "$evaluator + "
        }
    }
}
