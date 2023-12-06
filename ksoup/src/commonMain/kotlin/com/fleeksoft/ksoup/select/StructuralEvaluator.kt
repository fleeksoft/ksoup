package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.ported.IdentityHashMap

/**
 * Base structural evaluator.
 */
internal abstract class StructuralEvaluator(evaluator: Evaluator) : Evaluator() {
    val evaluator: Evaluator

    // Memoize inner matches, to save repeated re-evaluations of parent, sibling etc.
    // root + element: Boolean matches. ThreadLocal in case the Evaluator is compiled then reused across multi threads
    val threadMemo: IdentityHashMap<Element, IdentityHashMap<Element, Boolean>> =
        IdentityHashMap()

    init {
        this.evaluator = evaluator
    }

    fun memoMatches(root: Element, element: Element): Boolean {
        // not using computeIfAbsent, as the lambda impl requires a new Supplier closure object on every hit: tons of GC
        val rootMemo: IdentityHashMap<Element, IdentityHashMap<Element, Boolean>> = threadMemo
        var memo: IdentityHashMap<Element, Boolean>? = rootMemo[root]
        if (memo == null) {
            memo = IdentityHashMap<Element, Boolean>()
            rootMemo[root] = memo
        }
        var matches: Boolean? = memo.get(element)
        if (matches == null) {
            matches = evaluator.matches(root, element)
            memo[element] = matches
        }
        return matches
    }

    override fun reset() {
        threadMemo.clear()
        super.reset()
    }

    internal class Root : Evaluator() {
        override fun matches(root: Element, element: Element): Boolean {
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
        val finder: Collector.FirstFinder

        init {
            finder = Collector.FirstFinder(evaluator)
        }

        override fun matches(root: Element, element: Element): Boolean {
            // for :has, we only want to match children (or below), not the input element. And we want to minimize GCs
            for (i in 0 until element.childNodeSize()) {
                val node: Node = element.childNode(i)
                if (node is Element) {
                    val match: Element? = finder.find(element, node)
                    if (match != null) return true
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
        override fun matches(root: Element, element: Element): Boolean {
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
        override fun matches(root: Element, element: Element): Boolean {
            return !memoMatches(root, element)
        }

        override fun cost(): Int {
            return 2 + evaluator.cost()
        }

        override fun toString(): String {
            return ":not($evaluator)"
        }
    }

    class Parent(evaluator: Evaluator) : StructuralEvaluator(evaluator) {
        override fun matches(root: Element, element: Element): Boolean {
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

    @Deprecated("replaced by {@link  ImmediateParentRun}")
    internal class ImmediateParent(evaluator: Evaluator) : StructuralEvaluator(evaluator) {
        override fun matches(root: Element, element: Element): Boolean {
            if (root === element) return false
            val parent: Element? = element.parent()
            return parent != null && memoMatches(root, parent)
        }

        override fun cost(): Int {
            return 1 + evaluator.cost()
        }

        override fun toString(): String {
            return "$evaluator > "
        }
    }

    /**
     * Holds a list of evaluators for one > two > three immediate parent matches, and the final direct evaluator under
     * test. To match, these are effectively ANDed together, starting from the last, matching up to the first.
     */
    class ImmediateParentRun(evaluator: Evaluator) : Evaluator() {
        val evaluators: ArrayList<Evaluator> = ArrayList<Evaluator>()
        private var _cost = 2

        init {
            evaluators.add(evaluator)
            _cost += evaluator.cost()
        }

        fun add(evaluator: Evaluator) {
            evaluators.add(evaluator)
            _cost += evaluator.cost()
        }

        override fun matches(root: Element, element: Element): Boolean {
            var element: Element? = element
            if (element === root) return false // cannot match as the second eval (first parent test) would be above the root
            for (i in evaluators.indices.reversed()) {
                if (element == null) return false
                val eval: Evaluator = evaluators.get(i)
                if (!eval.matches(root, element)) return false
                element = element.parent()
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

    class PreviousSibling(evaluator: Evaluator) : StructuralEvaluator(evaluator) {
        override fun matches(root: Element, element: Element): Boolean {
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
        override fun matches(root: Element, element: Element): Boolean {
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
