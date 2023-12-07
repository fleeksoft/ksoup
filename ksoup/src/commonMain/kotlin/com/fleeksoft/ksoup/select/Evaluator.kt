package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.Normalizer.lowerCase
import com.fleeksoft.ksoup.internal.Normalizer.normalize
import com.fleeksoft.ksoup.internal.StringUtil.normaliseWhitespace
import com.fleeksoft.ksoup.nodes.Comment
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.DocumentType
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.PseudoTextElement
import com.fleeksoft.ksoup.nodes.TextNode
import com.fleeksoft.ksoup.nodes.XmlDeclaration
import com.fleeksoft.ksoup.parser.ParseSettings
import kotlin.jvm.JvmOverloads

/**
 * Evaluates that an element matches the selector.
 */
internal abstract class Evaluator protected constructor() {
    /**
     * Test if the element meets the evaluator's requirements.
     *
     * @param root    Root of the matching subtree
     * @param element tested element
     * @return Returns <tt>true</tt> if the requirements are met or
     * <tt>false</tt> otherwise
     */
    abstract fun matches(
        root: Element,
        element: Element,
    ): Boolean

    /**
     * Reset any internal state in this Evaluator before executing a new Collector evaluation.
     */
    open fun reset() {}

    /**
     * A relative evaluator cost function. During evaluation, Evaluators are sorted by ascending cost as an optimization.
     * @return the relative cost of this Evaluator
     */
    open fun cost(): Int {
        return 5 // a nominal default cost
    }

    /**
     * Evaluator for tag name
     */
    class Tag(private val tagName: String) : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return element.normalName() == tagName
        }

        override fun cost(): Int {
            return 1
        }

        override fun toString(): String {
            return tagName
        }
    }

    /**
     * Evaluator for tag name that ends with
     */
    class TagEndsWith(private val tagName: String) : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return element.normalName().endsWith(tagName)
        }

        override fun toString(): String {
            return tagName
        }
    }

    /**
     * Evaluator for element id
     */
    class Id(private val id: String) : Evaluator() {
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
    class Class(private val className: String) : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return element.hasClass(className)
        }

        override fun cost(): Int {
            return 6 // does whitespace scanning
        }

        override fun toString(): String {
            return ".$className"
        }
    }

    /**
     * Evaluator for attribute name matching
     */
    class Attribute(private val key: String) : Evaluator() {
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
    class AttributeStarting(keyPrefix: String?) : Evaluator() {
        private val keyPrefix: String

        init {
            Validate.notEmpty(keyPrefix)
            this.keyPrefix = lowerCase(keyPrefix)
        }

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
    class AttributeWithValue(key: String?, value: String) : AttributeKeyPair(key, value) {
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
    class AttributeWithValueNot(key: String, value: String) : AttributeKeyPair(key, value) {
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
    class AttributeWithValueStarting(key: String, value: String) :
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
    class AttributeWithValueEnding(key: String?, value: String) :
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
    class AttributeWithValueContaining(key: String, value: String) : AttributeKeyPair(key, value) {
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
    class AttributeWithValueMatching(key: String?, regex: Regex) : Evaluator() {
        var key: String
        var regex: Regex

        init {
            this.key = normalize(key)
            this.regex = regex
        }

        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            // TODO: test regex.find vs pattern.matcher
            return element.hasAttr(key) && regex.find(element.attr(key)) != null
        }

        override fun cost(): Int {
            return 8
        }

        override fun toString(): String {
            return "[$key~=$regex]"
        }
    }

    /**
     * Abstract evaluator for attribute name/value matching
     */
    abstract class AttributeKeyPair
        @JvmOverloads
        constructor(
            key: String?,
            value: String,
            trimValue: Boolean = true,
        ) : Evaluator() {
            var key: String
            var value: String

            init {
                var resultValue = value
                Validate.notEmpty(key)
                Validate.notEmpty(resultValue)
                this.key = normalize(key)
                val isStringLiteral = (
                    resultValue.startsWith("'") && resultValue.endsWith("'") ||
                        resultValue.startsWith("\"") && resultValue.endsWith("\"")
                )
                if (isStringLiteral) {
                    resultValue = resultValue.substring(1, resultValue.length - 1)
                }
                this.value = if (trimValue) normalize(resultValue) else normalize(resultValue, isStringLiteral)
            }
        }

    /**
     * Evaluator for any / all element matching
     */
    class AllElements : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
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
            return root !== element && element.elementSiblingIndex() < index
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
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            val p: Element? = element.parent()
            return p != null && p !is Document && element === p.lastElementChild()
        }

        override fun toString(): String {
            return ":last-child"
        }
    }

    class IsFirstOfType : IsNthOfType(0, 1) {
        override fun toString(): String {
            return ":first-of-type"
        }
    }

    class IsLastOfType : IsNthLastOfType(0, 1) {
        override fun toString(): String {
            return ":last-of-type"
        }
    }

    abstract class CssNthEvaluator(protected val a: Int, protected val b: Int) : Evaluator() {
        constructor(b: Int) : this(0, b)

        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            val p: Element? = element.parent()
            if (p == null || p is Document) return false
            val pos = calculatePosition(root, element)
            return if (a == 0) pos == b else (pos - b) * a >= 0 && (pos - b) % a == 0
        }

        override fun toString(): String {
            if (a == 0) return ":$pseudoClass($b)"
            return if (b == 0) {
                ":$pseudoClass(${a}n)"
            } else {
                val sign = if (b >= 0) "+" else ""
                ":$pseudoClass(${a}n${sign}$b)"
            }
        }

        protected abstract val pseudoClass: String?

        protected abstract fun calculatePosition(
            root: Element,
            element: Element,
        ): Int
    }

    /**
     * css-compatible Evaluator for :eq (css :nth-child)
     *
     * @see IndexEquals
     */
    open class IsNthChild(a: Int, b: Int) : CssNthEvaluator(a, b) {
        override fun calculatePosition(
            root: Element,
            element: Element,
        ): Int {
            return element.elementSiblingIndex() + 1
        }

        protected override val pseudoClass: String = "nth-child"
    }

    /**
     * css pseudo class :nth-last-child)
     *
     * @see IndexEquals
     */
    class IsNthLastChild(a: Int, b: Int) : CssNthEvaluator(a, b) {
        override fun calculatePosition(
            root: Element,
            element: Element,
        ): Int {
            val parent: Element? = element.parent()
            return if (parent == null) {
                0
            } else {
                parent.childrenSize() - element.elementSiblingIndex()
            }
        }

        override val pseudoClass: String = "nth-last-child"
    }

    /**
     * css pseudo class nth-of-type
     *
     */
    open class IsNthOfType(a: Int, b: Int) : CssNthEvaluator(a, b) {
        override fun calculatePosition(
            root: Element,
            element: Element,
        ): Int {
            val parent: Element = element.parent() ?: return 0
            var pos = 0
            val size: Int = parent.childNodeSize()
            for (i in 0 until size) {
                val node: Node = parent.childNode(i)
                if (node.normalName() == element.normalName()) pos++
                if (node === element) break
            }
            return pos
        }

        protected override val pseudoClass: String = "nth-of-type"
    }

    open class IsNthLastOfType(a: Int, b: Int) : CssNthEvaluator(a, b) {
        override fun calculatePosition(
            root: Element,
            element: Element,
        ): Int {
            element.parent() ?: return 0
            var pos = 0
            var next: Element? = element
            while (next != null) {
                if (next.normalName() == element.normalName()) pos++
                next = next.nextElementSibling()
            }
            return pos
        }

        protected override val pseudoClass: String = "nth-last-of-type"
    }

    /**
     * Evaluator for matching the first sibling (css :first-child)
     */
    class IsFirstChild : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
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
    class IsRoot : Evaluator() {
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

    class IsOnlyChild : Evaluator() {
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

    class IsOnlyOfType : Evaluator() {
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

    class IsEmpty : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            val family: List<Node> = element.childNodes()
            for (n in family) {
                if (n is TextNode) return n.isBlank()
                if (!(n is Comment || n is XmlDeclaration || n is DocumentType)) return false
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
    abstract class IndexEvaluator(var index: Int) : Evaluator()

    /**
     * Evaluator for matching Element (and its descendants) text
     */
    class ContainsText(searchText: String) : Evaluator() {
        private val searchText: String

        init {
            this.searchText = lowerCase(normaliseWhitespace(searchText))
        }

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
     * @since 1.15.1.
     */
    class ContainsWholeText(private val searchText: String) : Evaluator() {
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
     * @since 1.15.1.
     */
    class ContainsWholeOwnText(private val searchText: String) : Evaluator() {
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
    class ContainsData(searchText: String?) : Evaluator() {
        private val searchText: String

        init {
            this.searchText = lowerCase(searchText)
        }

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
    class ContainsOwnText(searchText: String) : Evaluator() {
        private val searchText: String

        init {
            this.searchText = lowerCase(normaliseWhitespace(searchText))
        }

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
    class Matches(private val pattern: Regex) : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return pattern.containsMatchIn(element.text())
        }

        override fun cost(): Int = 8

        override fun toString(): String = ":matches($pattern)"
    }

    /**
     * Evaluator for matching Element's own text with regex
     */
    class MatchesOwn(private val pattern: Regex) : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return pattern.containsMatchIn(element.ownText())
        }

        override fun cost(): Int = 7

        override fun toString(): String = ":matchesOwn($pattern)"
    }

    /**
     * Evaluator for matching Element (and its descendants) whole text with regex.
     * @since 1.15.1.
     */
    class MatchesWholeText(private val pattern: Regex) : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return pattern.containsMatchIn(element.wholeText())
        }

        override fun cost(): Int = 8

        override fun toString(): String = ":matchesWholeText($pattern)"
    }

    /**
     * Evaluator for matching Element's own whole text with regex.
     * @since 1.15.1.
     */
    class MatchesWholeOwnText(private val pattern: Regex) : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            return pattern.containsMatchIn(element.wholeOwnText())
        }

        override fun cost(): Int = 7

        override fun toString(): String = ":matchesWholeOwnText($pattern)"
    }

    class MatchText : Evaluator() {
        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
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
