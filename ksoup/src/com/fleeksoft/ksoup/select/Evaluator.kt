/*
 * Kotlin port of jsoup's Evaluator.java
 * Copyright © 2009–2025 Jonathan Hedley
 * Copyright © 2023–2025 FLEEK SOFT
 * Licensed under the MIT License
 * https://jsoup.org
 */

package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.Normalizer.lowerCase
import com.fleeksoft.ksoup.internal.Normalizer.normalize
import com.fleeksoft.ksoup.internal.StringUtil.normaliseWhitespace
import com.fleeksoft.ksoup.nodes.*
import com.fleeksoft.ksoup.parser.ParseSettings

/**
 * An Evaluator tests if an element (or a node) meets the selector's requirements. Obtain an evaluator for a given CSS selector
 * with {@link Selector#evaluatorOf(String css)}. If you are executing the same selector on many elements (or documents), it
 * can be more efficient to compile and reuse an Evaluator than to reparse the selector on each invocation of select().
 * <p>Evaluators are thread-safe and may be used concurrently across multiple documents.</p>
 */
public abstract class Evaluator protected constructor() {
    /**
     * Provides a Predicate for this Evaluator, matching the test Element.
     * @param root the root Element, for match evaluation
     * @return a predicate that accepts an Element to test for matches with this Evaluator
     */
    public fun asPredicate(root: Element): (Element) -> Boolean = { element -> matches(root, element) }

    fun asNodePredicate(root: Element): (Node) -> Boolean {
        return { node: Node -> matches(root, node) }
    }

    /**
     * Test if the element meets the evaluator's requirements.
     *
     * @param root    Root of the matching subtree
     * @param element tested element
     * @return Returns <tt>true</tt> if the requirements are met or
     * <tt>false</tt> otherwise
     */
    public abstract fun matches(root: Element, element: Element): Boolean

    open fun matches(root: Element, node: Node): Boolean {
        if (node is Element) {
            return matches(root, node)
        } else if (node is LeafNode && wantsNodes()) {
            return matches(root, node)
        }
        return false
    }

    open fun matches(root: Element, leafNode: LeafNode): Boolean {
        return false
    }

    open fun wantsNodes(): Boolean {
        return false
    }

    /**
     * Reset any internal state in this Evaluator before executing a new Collector evaluation.
     */
    public open fun reset() {}

    /**
     * A relative evaluator cost function. During evaluation, Evaluators are sorted by ascending cost as an optimization.
     * @return the relative cost of this Evaluator
     */
    public open fun cost(): Int {
        return 5 // a nominal default cost
    }

    /**
     * Evaluator for tag name
     */
    public class Tag(private val tagName: String) : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return element.nameIs(tagName)
        }

        override fun cost(): Int {
            return 1
        }

        override fun toString(): String {
            return tagName
        }
    }

    /**
     * Evaluator for tag name that starts with prefix; used for ns|*
     */
    public class TagStartsWith(private val tagName: String) : Evaluator() {
        override fun matches(root: Element, element: Element): Boolean {
            return element.normalName().startsWith(tagName)
        }

        override fun toString(): String {
            return "${tagName}|*"
        }
    }

    /**
     * Evaluator for tag name that ends with suffix; used for *|el
     */
    public class TagEndsWith(private val tagName: String) : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return element.normalName().endsWith(tagName)
        }

        override fun toString(): String {
            return "*|${tagName}"
        }
    }

    /**
     * Evaluator for element id
     */
    public class Id(private val id: String) : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return id == element.id()
        }

        override fun cost(): Int {
            return 2
        }

        override fun toString(): String {
            return "#$id"
        }
    }

    /**
     * Evaluator for element class
     */
    public class Class(private val className: String) : Evaluator() {
        override fun matches(root: Element, element: Element): Boolean {
            return element.hasClass(className)
        }

        override fun cost(): Int {
            return 8 // does whitespace scanning; more than .contains()
        }

        override fun toString(): String {
            return ".$className"
        }
    }

    /**
     * Evaluator for attribute name matching
     */
    public class Attribute(private val key: String) : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return element.hasAttr(key)
        }

        override fun cost(): Int {
            return 2
        }

        override fun toString(): String {
            return "[$key]"
        }
    }

    /**
     * Evaluator for attribute name prefix matching
     */
    public class AttributeStarting(keyPrefix: String) : Evaluator() {
        private val keyPrefix: String = lowerCase(keyPrefix)

        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            val values: List<com.fleeksoft.ksoup.nodes.Attribute> = element.attributes().asList()
            for (attribute in values) {
                if (lowerCase(attribute.key).startsWith(keyPrefix)) return true
            }
            return false
        }

        override fun cost(): Int {
            return 6
        }

        override fun toString(): String {
            return "[^$keyPrefix]"
        }
    }

    /**
     * Evaluator for attribute name/value matching
     */
    public class AttributeWithValue(key: String, value: String) : AttributeKeyPair(key, value) {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return element.hasAttr(key) && value.equals(element.attr(key).trim(), ignoreCase = true)
        }

        override fun cost(): Int {
            return 3
        }

        override fun toString(): String {
            return "[$key=$value]"
        }
    }

    /**
     * Evaluator for attribute name != value matching
     */
    public class AttributeWithValueNot(key: String, value: String) : AttributeKeyPair(key, value) {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return !value.equals(element.attr(key), ignoreCase = true)
        }

        override fun cost(): Int {
            return 3
        }

        override fun toString(): String {
            return "[$key!=$value]"
        }
    }

    /**
     * Evaluator for attribute name/value matching (value prefix)
     */
    public class AttributeWithValueStarting(key: String, value: String) :
        AttributeKeyPair(key, value, false) {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return element.hasAttr(key) && lowerCase(element.attr(key)).startsWith(value) // value is lower case already
        }

        override fun cost(): Int {
            return 4
        }

        override fun toString(): String {
            return "[$key^=$value]"
        }
    }

    /**
     * Evaluator for attribute name/value matching (value ending)
     */
    public class AttributeWithValueEnding(key: String, value: String) :
        AttributeKeyPair(key, value, false) {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return element.hasAttr(key) && lowerCase(element.attr(key)).endsWith(value) // value is lower case
        }

        override fun cost(): Int {
            return 4
        }

        override fun toString(): String {
            return "[$key$=$value]"
        }
    }

    /**
     * Evaluator for attribute name/value matching (value containing)
     */
    public class AttributeWithValueContaining(key: String, value: String) : AttributeKeyPair(key, value) {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return element.hasAttr(key) && lowerCase(element.attr(key)).contains(value) // value is lower case
        }

        override fun cost(): Int {
            return 6
        }

        override fun toString(): String {
            return "[$key*=$value]"
        }
    }

    /**
     * Evaluator for attribute name/value matching (value regex matching)
     */
    class AttributeWithValueMatching(key: String?, var regex: Regex) : Evaluator() {
        var key: String = normalize(key)

        override fun matches(root: Element, element: Element): Boolean {
            // TODO: test regex.find vs pattern.matcher
            return element.hasAttr(key) && regex.find(element.attr(key)) != null
        }

        override fun cost(): Int {
            return 8
        }

        override fun toString(): String {
            return "[$key~=${regex.pattern}]"
        }
    }

    /**
     * Abstract evaluator for attribute name/value matching
     */
    public abstract class AttributeKeyPair(key: String, value: String, trimQuoted: Boolean = true) : Evaluator() {
        public var key: String
        public var value: String

        init {
            var resultValue = value
            Validate.notEmpty(key)
            Validate.notEmpty(resultValue)
            this.key = normalize(key)
            val quoted = resultValue.startsWith("'") && resultValue.endsWith("'")
                    || resultValue.startsWith("\"") && resultValue.endsWith("\"")
            if (quoted) resultValue = value.substring(1, resultValue.length - 1)


            // normalize value based on whether it was quoted and trimQuoted flag
            // keeps whitespace for attribute val starting or ending, when quoted
            if (trimQuoted || !quoted) this.value = normalize(resultValue) // lowercase and trims
            else this.value = lowerCase(resultValue) // only lowercase
        }
    }

    /**
     * Evaluator for any / all element matching
     */
    class AllElements : Evaluator() {
        override fun matches(root: Element, element: Element): Boolean {
            return true
        }

        override fun cost(): Int {
            return 10
        }

        override fun toString(): String {
            return "*"
        }
    }

    /**
     * Evaluator for matching by sibling index number (e &lt; idx)
     */
    class IndexLessThan(index: Int) : IndexEvaluator(index) {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return root != element && element.elementSiblingIndex() < index
        }

        override fun toString(): String {
            return ":lt($index)"
        }
    }

    /**
     * Evaluator for matching by sibling index number (e &gt; idx)
     */
    class IndexGreaterThan(index: Int) : IndexEvaluator(index) {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return element.elementSiblingIndex() > index
        }

        override fun toString(): String {
            return ":gt($index)"
        }
    }

    /**
     * Evaluator for matching by sibling index number (e = idx)
     */
    class IndexEquals(index: Int) : IndexEvaluator(index) {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return element.elementSiblingIndex() == index
        }

        override fun toString(): String {
            return ":eq($index)"
        }
    }

    /**
     * Evaluator for matching the last sibling (css :last-child)
     */
    class IsLastChild : Evaluator() {
        override fun matches(root: Element, element: Element): Boolean {
            val p: Element? = element.parent()
            return p != null && p !is Document && element === p.lastElementChild()
        }

        override fun toString(): String {
            return ":last-child"
        }
    }

    public class IsFirstOfType : IsNthOfType(0, 1) {
        override fun toString(): String {
            return ":first-of-type"
        }
    }

    public class IsLastOfType : IsNthLastOfType(0, 1) {
        override fun toString(): String {
            return ":last-of-type"
        }
    }

    /**
     * Base class for CSS :nth-* evaluators (e.g. :nth-child, :nth-of-type).
     */
    abstract class CssNthEvaluator(val a: Int, val b: Int) : Evaluator() {

        /** Convenience constructor for just an offset (a = 0). */
        constructor(offset: Int) : this(0, offset)

        override fun matches(root: Element, element: Element): Boolean {
            val parent = element.parent() ?: return false
            if (parent is Document) return false

            val pos = calculatePosition(root, element)
            return if (a == 0) {
                pos == b
            } else {
                (pos - b) * a >= 0 && (pos - b) % a == 0
            }
        }

        override fun toString(): String {
            val pseudo = getPseudoClass()
            return when {
                a == 0 -> ":$pseudo($b)"
                b == 0 -> ":$pseudo(${a}n)"
                else -> {
                    val sign = if (b >= 0) "+$b" else "$b"
                    ":$pseudo(${a}n$sign)"
                }
            }
        }

        /** Returns the CSS pseudo-class (e.g. "nth-child"). */
        protected abstract fun getPseudoClass(): String

        /** Computes the position of [element] under [root]. */
        abstract fun calculatePosition(root: Element, element: Element): Int
    }

    /**
     * css-compatible Evaluator for :eq (css :nth-child)
     *
     * @see IndexEquals
     */
    class IsNthChild(a: Int, b: Int) : CssNthEvaluator(a, b) {
        override fun calculatePosition(root: Element, element: Element): Int =
            element.elementSiblingIndex() + 1

        override fun getPseudoClass(): String = "nth-child"
    }

    /**
     * css pseudo‑class :nth‑last‑child
     *
     * @see IndexEquals
     */
    class IsNthLastChild(a: Int, b: Int) : CssNthEvaluator(a, b) {
        override fun calculatePosition(root: Element, element: Element): Int {
            if (element.parent() == null) return 0
            return element.parent()!!.childrenSize() - element.elementSiblingIndex()
        }

        override fun getPseudoClass(): String = "nth-last-child"
    }

    /**
     * css pseudo‑class nth‑of‑type
     */
    open class IsNthOfType(a: Int, b: Int) : CssNthEvaluator(a, b) {
        override fun calculatePosition(root: Element, element: Element): Int {
            val parent = element.parent() ?: return 0

            var pos = 0
            val size = parent.childNodeSize()
            for (i in 0 until size) {
                val node = parent.childNode(i)
                if (node.normalName() == element.normalName()) pos++
                if (node == element) break
            }
            return pos
        }

        override fun getPseudoClass(): String = "nth-of-type"
    }


    /**
     * css pseudo‑class nth‑last‑of‑type
     */
    open class IsNthLastOfType(a: Int, b: Int) : CssNthEvaluator(a, b) {
        override fun calculatePosition(root: Element, element: Element): Int {
            val parent = element.parent() ?: return 0

            var pos = 0
            var next: Element? = element
            while (next != null) {
                if (next.normalName() == element.normalName()) pos++
                next = next.nextElementSibling()
            }
            return pos
        }

        override fun getPseudoClass(): String = "nth-last-of-type"
    }

    /**
     * Evaluator for matching the first sibling (css :first-child)
     */
    public class IsFirstChild : Evaluator() {
        override fun matches(root: Element, element: Element): Boolean {
            val p: Element? = element.parent()
            return p != null && p !is Document && element === p.firstElementChild()
        }

        override fun toString(): String {
            return ":first-child"
        }
    }

    /**
     * css3 pseudo-class :root
     * @see [:root selector](http://www.w3.org/TR/selectors/.root-pseudo)
     */
    public class IsRoot : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            val r: Element? = if (root is Document) root.firstElementChild() else root
            return element === r
        }

        override fun cost(): Int {
            return 1
        }

        override fun toString(): String {
            return ":root"
        }
    }

    public class IsOnlyChild : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            val p: Element? = element.parent()
            return p != null && p !is Document && element.siblingElements().isEmpty()
        }

        override fun toString(): String {
            return ":only-child"
        }
    }

    public class IsOnlyOfType : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            val p: Element? = element.parent()
            if (p == null || p is Document) return false
            var pos = 0
            var next: Element? = p.firstElementChild()
            while (next != null) {
                if (next.normalName() == element.normalName()) pos++
                if (pos > 1) break
                next = next.nextElementSibling()
            }
            return pos == 1
        }

        override fun toString(): String {
            return ":only-of-type"
        }
    }

    public class IsEmpty : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            var n = element.firstChild()
            while (n != null) {
                if (n is TextNode) {
                    if (!n.isBlank()) return false // non-blank text: not empty
                } else if (!(n is Comment || n is XmlDeclaration || n is DocumentType)) {
                    return false; // non "blank" element: not empty
                }

                n = n.nextSibling()
            }

            return true
        }

        override fun toString(): String {
            return ":empty"
        }
    }

    /**
     * Abstract evaluator for sibling index matching
     *
     * @author ant
     */
    public abstract class IndexEvaluator(public var index: Int) : Evaluator()

    /**
     * Evaluator for matching Element (and its descendants) text
     */
    public class ContainsText(searchText: String) : Evaluator() {
        private val searchText: String = lowerCase(normaliseWhitespace(searchText))

        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return lowerCase(element.text()).contains(searchText)
        }

        override fun cost(): Int {
            return 10
        }

        override fun toString(): String {
            return ":contains($searchText)"
        }
    }

    /**
     * Evaluator for matching Element (and its descendants) wholeText. Neither the input nor the element text is
     * normalized. `:containsWholeText()`
     */
    public class ContainsWholeText(private val searchText: String) : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return element.wholeText().contains(searchText)
        }

        override fun cost(): Int {
            return 10
        }

        override fun toString(): String {
            return ":containsWholeText($searchText)"
        }
    }

    /**
     * Evaluator for matching Element (but **not** its descendants) wholeText. Neither the input nor the element text is
     * normalized. `:containsWholeOwnText()`
     */
    public class ContainsWholeOwnText(private val searchText: String) : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return element.wholeOwnText().contains(searchText)
        }

        override fun toString(): String {
            return ":containsWholeOwnText($searchText)"
        }
    }

    /**
     * Evaluator for matching Element (and its descendants) data
     */
    public class ContainsData(searchText: String?) : Evaluator() {
        private val searchText: String = lowerCase(searchText)

        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return lowerCase(element.data()).contains(searchText) // not whitespace normalized
        }

        override fun toString(): String {
            return ":containsData($searchText)"
        }
    }

    /**
     * Evaluator for matching Element's own text
     */
    public class ContainsOwnText(searchText: String) : Evaluator() {
        private val searchText: String = lowerCase(normaliseWhitespace(searchText))

        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return lowerCase(element.ownText()).contains(searchText)
        }

        override fun toString(): String {
            return ":containsOwn($searchText)"
        }
    }

    /**
     * Evaluator for matching Element (and its descendants) text with regex
     */
    public class Matches(private val pattern: Regex) : Evaluator() {
        override fun matches(root: Element, element: Element): Boolean {
            return pattern.containsMatchIn(element.text())
        }

        override fun cost(): Int = 8

        override fun toString(): String = ":matches(${pattern.pattern})"
    }

    /**
     * Evaluator for matching Element's own text with regex
     */
    public class MatchesOwn(private val pattern: Regex) : Evaluator() {
        override fun matches(root: Element, element: Element): Boolean {
            return pattern.containsMatchIn(element.ownText())
        }

        override fun cost(): Int = 7

        override fun toString(): String = ":matchesOwn(${pattern.pattern})"
    }

    /**
     * Evaluator for matching Element (and its descendants) whole text with regex.
     */
    public class MatchesWholeText(private val pattern: Regex) : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return pattern.containsMatchIn(element.wholeText())
        }

        override fun cost(): Int = 8

        override fun toString(): String = ":matchesWholeText(${pattern.pattern})"
    }

    /**
     * Evaluator for matching Element's own whole text with regex.
     */
    public class MatchesWholeOwnText(private val pattern: Regex) : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return pattern.containsMatchIn(element.wholeOwnText())
        }

        override fun cost(): Int = 7

        override fun toString(): String = ":matchesWholeOwnText(${pattern.pattern})"
    }

    @Deprecated("This selector is deprecated and will be removed in a future version. Migrate to <code>::textnode</code> using the <code>Element#selectNodes()</code> method instead.")
    public class MatchText : Evaluator() {
        companion object {
            private var loggedError: Boolean = false
        }

        init {


            // log a deprecated error on first use; users typically won't directly construct this Evaluator and so won't otherwise get deprecation warnings
            if (!loggedError) {
                loggedError = true
                println("WARNING: :matchText selector is deprecated and will be removed in a future version. Use Element#selectNodes(String, Class) with selector ::textnode and class TextNode instead.")
            }
        }

        override fun matches(root: Element, element: Element): Boolean {
            if (element is PseudoTextElement) return true
            val textNodes: List<TextNode> = element.textNodes()
            for (textNode in textNodes) {
                val pel =
                    PseudoTextElement(
                        com.fleeksoft.ksoup.parser.Tag.valueOf(
                            element.tagName(),
                            element.tag().namespace(),
                            ParseSettings.preserveCase,
                        ),
                        element.baseUri(),
                        element.attributes(),
                    )
                textNode.replaceWith(pel)
                pel.appendChild(textNode)
            }
            return false
        }

        override fun cost(): Int {
            return -1 // forces first evaluation, which prepares the DOM for later evaluator matches
        }

        override fun toString(): String {
            return ":matchText"
        }
    }
}
