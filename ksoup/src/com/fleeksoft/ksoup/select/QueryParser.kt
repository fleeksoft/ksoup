/*
 * Kotlin port of jsoup's QueryParser.java
 * Copyright © 2009–2025 Jonathan Hedley
 * Copyright © 2023–2025 FLEEK SOFT
 * Licensed under the MIT License
 * https://jsoup.org
 */

package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.helper.Validate.isTrue
import com.fleeksoft.ksoup.internal.Normalizer.normalize
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.parser.TokenQueue
import com.fleeksoft.ksoup.ported.jsSupportedRegex
import com.fleeksoft.ksoup.select.StructuralEvaluator.ImmediateParentRun


/**
 * Parses a CSS selector into an Evaluator tree.
 */
public class QueryParser private constructor(query: String) {
    private val tq: TokenQueue
    private val query: String

    /**
     * Parse the query. We use this simplified expression of the grammar:
     * <pre>
     * SelectorGroup   ::= Selector (',' Selector)*
     * Selector        ::= [ Combinator ] SimpleSequence ( Combinator SimpleSequence )*
     * SimpleSequence  ::= [ TypeSelector ] ( ID | Class | Attribute | Pseudo )*
     * Pseudo           ::= ':' Name [ '(' SelectorGroup ')' ]
     * Combinator      ::= S+         // descendant (whitespace)
     * | '>'       // child
     * | '+'       // adjacent sibling
     * | '~'       // general sibling
     * </pre>
     *
     * See <a href="https://www.w3.org/TR/selectors-4/#grammar">selectors-4</a> for the real thing
     */
    fun parse(): Evaluator {
        val eval = parseSelectorGroup()
        tq.consumeWhitespace()
        if (!tq.isEmpty()) throw Selector.SelectorParseException("Could not parse query '$query': unexpected token at '${tq.remainder()}'")
        return eval
    }

    fun parseSelectorGroup(): Evaluator {
        // SelectorGroup. Into an Or if > 1 Selector
        var left = parseSelector()
        while (tq.matchChomp(',')) {
            val right = parseSelector()
            left = or(left, right)
        }
        return left
    }

    fun parseSelector(): Evaluator {
        // Selector ::= [ Combinator ] SimpleSequence ( Combinator SimpleSequence )*
        tq.consumeWhitespace()

        var left: Evaluator
        left = if (tq.matchesAny(*Combinators)) {
            // e.g. query is "> div"; left side is root element
            StructuralEvaluator.Root()
        } else {
            parseSimpleSequence()
        }

        while (true) {
            var combinator = 0.toChar()
            if (tq.consumeWhitespace()) combinator = ' ' // maybe descendant?

            if (tq.matchesAny(*Combinators))  // no, explicit
                combinator = tq.consume()
            else if (tq.matchesAny(*SequenceEnders))  // , - space after simple like "foo , bar"; ) - close of :has()
                break

            if (combinator.code != 0) {
                val right = parseSimpleSequence()
                left = combinator(left, combinator, right)
            } else {
                break
            }
        }
        return left
    }

    fun parseSimpleSequence(): Evaluator {
        // SimpleSequence ::= TypeSelector? ( Hash | Class | Pseudo )*
        var left: Evaluator? = null
        tq.consumeWhitespace()

        // one optional type selector
        if (tq.matchesWord() || tq.matches("*|")) left = byTag()
        else if (tq.matchChomp('*')) left = Evaluator.AllElements()

        // zero or more subclasses (#, ., [)
        while (true) {
            val right: Evaluator? = parseSubclass()
            if (right != null) left = and(left, right)
            else break // no more simple tokens
        }

        if (left == null) throw Selector.SelectorParseException("Could not parse query '$query': unexpected token at '${tq.remainder()}'")
        return left
    }

    fun parseSubclass(): Evaluator? {
        //  Subclass: ID | Class | Attribute | Pseudo
        return if (tq.matchChomp('#')) byId()
        else if (tq.matchChomp('.')) byClass()
        else if (tq.matches('[')) byAttribute()
        else if (tq.matchChomp(':')) parsePseudoSelector()
        else null
    }

    private fun parsePseudoSelector(): Evaluator {
        val pseudo: String = tq.consumeCssIdentifier()
        return when (pseudo) {
            "lt" -> Evaluator.IndexLessThan(consumeIndex())
            "gt" -> Evaluator.IndexGreaterThan(consumeIndex())
            "eq" -> Evaluator.IndexEquals(consumeIndex())
            "has" -> has()
            "is" -> `is`()
            "contains" -> contains(false)
            "containsOwn" -> contains(true)
            "containsWholeText" -> containsWholeText(false)
            "containsWholeOwnText" -> containsWholeText(true)
            "containsData" -> containsData()
            "matches" -> matches(false)
            "matchesOwn" -> matches(true)
            "matchesWholeText" -> matchesWholeText(false)
            "matchesWholeOwnText" -> matchesWholeText(true)
            "not" -> not()
            "nth-child" -> cssNthChild(last = false, ofType = false)
            "nth-last-child" -> cssNthChild(last = true, ofType = false)
            "nth-of-type" -> cssNthChild(last = false, ofType = true)
            "nth-last-of-type" -> cssNthChild(last = true, ofType = true)
            "first-child" -> Evaluator.IsFirstChild()
            "last-child" -> Evaluator.IsLastChild()
            "first-of-type" -> Evaluator.IsFirstOfType()
            "last-of-type" -> Evaluator.IsLastOfType()
            "only-child" -> Evaluator.IsOnlyChild()
            "only-of-type" -> Evaluator.IsOnlyOfType()
            "empty" -> Evaluator.IsEmpty()
            "root" -> Evaluator.IsRoot()
            "matchText" -> Evaluator.MatchText()
            else -> throw Selector.SelectorParseException(
                "Could not parse query '$query': unexpected token at '${tq.remainder()}'",
            )
        }
    }

    private fun byId(): Evaluator {
        val id: String = tq.consumeCssIdentifier()
        Validate.notEmpty(id)
        return Evaluator.Id(id)
    }

    private fun byClass(): Evaluator {
        val className: String = tq.consumeCssIdentifier()
        Validate.notEmpty(className)
        return Evaluator.Class(className.trim { it <= ' ' })
    }

    private fun byTag(): Evaluator {
        // todo - these aren't dealing perfectly with case sensitivity. For case sensitive parsers, we should also make
        // the tag in the selector case-sensitive (and also attribute names). But for now, normalize (lower-case) for
        // consistency - both the selector and the element tag
        var tagName: String = normalize(tq.consumeElementSelector())
        Validate.notEmpty(tagName)

        // namespaces:
        if (tagName.startsWith("*|")) { // namespaces: wildcard match equals(tagName) or ending in ":"+tagName
            val plainTag = tagName.substring(2); // strip *|
            return CombiningEvaluator.Or(Evaluator.Tag(plainTag), Evaluator.TagEndsWith(":$plainTag"))
        } else if (tagName.endsWith("|*")) { // ns|*
            val ns = "${tagName.substring(0, tagName.length - 2)}:"; // strip |*, to ns:
            return Evaluator.TagStartsWith(ns);
        } else if (tagName.contains("|")) { // flip "abc|def" to "abc:def"
            tagName = tagName.replace("|", ":");
        }

        return Evaluator.Tag(tagName);
    }

    private fun byAttribute(): Evaluator {
        val cq = TokenQueue(tq.chompBalanced('[', ']')) // content queue
        val key: String =
            cq.consumeToAny(*AttributeEvals) // eq, not, start, end, contain, match, (no val)
        Validate.notEmpty(key)
        cq.consumeWhitespace()
        val eval: Evaluator
        if (cq.isEmpty()) {
            eval =
                if (key.startsWith("^")) {
                    Evaluator.AttributeStarting(key.substring(1))
                } else if (key == "*") {
                    // any attribute
                    Evaluator.AttributeStarting("")
                } else {
                    Evaluator.Attribute(key)
                }
        } else {
            if (cq.matchChomp('=')) {
                eval = Evaluator.AttributeWithValue(key, cq.remainder())
            } else if (cq.matchChomp("!=")) {
                eval = Evaluator.AttributeWithValueNot(key, cq.remainder())
            } else if (cq.matchChomp("^=")) {
                eval = Evaluator.AttributeWithValueStarting(key, cq.remainder())
            } else if (cq.matchChomp("$=")) {
                eval = Evaluator.AttributeWithValueEnding(key, cq.remainder())
            } else if (cq.matchChomp("*=")) {
                eval = Evaluator.AttributeWithValueContaining(key, cq.remainder())
            } else if (cq.matchChomp("~=")) {
                eval = Evaluator.AttributeWithValueMatching(key, jsSupportedRegex(cq.remainder()))
            } else {
                throw Selector.SelectorParseException(
                    "Could not parse attribute query '$query': unexpected token at '${cq.remainder()}'",
                )
            }
        }
        return eval
    }

    /**
     * Create a new QueryParser.
     * @param query CSS query
     */
    init {
        Validate.notEmpty(query)
        val trimmedQuery = query.trim { it <= ' ' }
        this.query = trimmedQuery
        tq = TokenQueue(trimmedQuery)
    }

    private fun cssNthChild(last: Boolean, ofType: Boolean): Evaluator {
        // normalize & consumeParens() are assumed to be available in this context
        val arg = normalize(consumeParens())
        val (step, offset) = when {
            arg.equals("odd", ignoreCase = true) -> 2 to 1
            arg.equals("even", ignoreCase = true) -> 2 to 0
            else -> {
                // try the “an+b” syntax
                NthStepOffset.matchEntire(arg)?.let { m ->
                    val signGroup = m.groupValues[2]    // “+” or “-” or ""
                    val digitsGroup = m.groupValues[3]    // e.g. “3” or ""
                    val offsetGroup = m.groupValues[4]    // e.g. “+2” or “-1” or ""
                    val step = if (digitsGroup.isNotEmpty()) {
                        // has an explicit coefficient
                        m.groupValues[1].replaceFirst("^\\+".toRegex(), "").toInt()
                    } else {
                        // just “n” or “-n”
                        if (signGroup == "-") -1 else 1
                    }
                    val offset = if (offsetGroup.isNotEmpty()) {
                        offsetGroup.replaceFirst("^\\+".toRegex(), "").toInt()
                    } else 0
                    step to offset
                }
                // or a simple integer
                    ?: NthOffset.matchEntire(arg)?.let { m ->
                        val off = m.value.replaceFirst("^\\+".toRegex(), "").toInt()
                        0 to off
                    }
                    // or fail
                    ?: throw Selector.SelectorParseException(
                        "Could not parse nth-index '$arg': unexpected format"
                    )
            }
        }

        return when {
            ofType && last -> Evaluator.IsNthLastOfType(step, offset)
            ofType -> Evaluator.IsNthOfType(step, offset)
            last -> Evaluator.IsNthLastChild(step, offset)
            else -> Evaluator.IsNthChild(step, offset)
        }
    }

    private fun consumeParens(): String {
        return tq.chompBalanced('(', ')')
    }

    private fun consumeIndex(): Int {
        val index = consumeParens().trim { it <= ' ' }
        isTrue(StringUtil.isNumeric(index), "Index must be numeric")
        return index.toInt()
    }

    // pseudo selector :has(el)
    private fun has(): Evaluator {
        return parseNested({ StructuralEvaluator.Has(it) }, ":has() must have a selector")
    }

    // pseudo selector :is()
    private fun `is`(): Evaluator {
        return parseNested({ StructuralEvaluator.Is(it) }, ":is() must have a selector")
    }

    private fun parseNested(func: (Evaluator) -> StructuralEvaluator, err: String?): Evaluator {
        isTrue(tq.matchChomp('('), err)
        val eval: Evaluator = parseSelectorGroup()
        isTrue(tq.matchChomp(')'), err)
        return func(eval)
    }

    // pseudo selector :contains(text), containsOwn(text)
    private fun contains(own: Boolean): Evaluator {
        val query = if (own) ":containsOwn" else ":contains"
        val searchText: String = TokenQueue.unescape(consumeParens())
        Validate.notEmpty(searchText, "$query(text) query must not be empty")
        return if (own) Evaluator.ContainsOwnText(searchText) else Evaluator.ContainsText(searchText)
    }

    private fun containsWholeText(own: Boolean): Evaluator {
        val query = if (own) ":containsWholeOwnText" else ":containsWholeText"
        val searchText: String = TokenQueue.unescape(consumeParens())
        Validate.notEmpty(searchText, "$query(text) query must not be empty")
        return if (own) Evaluator.ContainsWholeOwnText(searchText) else Evaluator.ContainsWholeText(searchText)
    }

    // pseudo selector :containsData(data)
    private fun containsData(): Evaluator {
        val searchText: String = TokenQueue.unescape(consumeParens())
        Validate.notEmpty(searchText, ":containsData(text) query must not be empty")
        return Evaluator.ContainsData(searchText)
    }

    // :matches(regex), matchesOwn(regex)
    private fun matches(own: Boolean): Evaluator {
        val query = if (own) ":matchesOwn" else ":matches"
        val regex = consumeParens() // don't unescape, as regex bits will be escaped
        Validate.notEmpty(regex, "$query(regex) query must not be empty")
        return if (own) {
            Evaluator.MatchesOwn(jsSupportedRegex(regex))
        } else {
            Evaluator.Matches(jsSupportedRegex(regex))
        }
    }

    // :matches(regex), matchesOwn(regex)
    private fun matchesWholeText(own: Boolean): Evaluator {
        val query = if (own) ":matchesWholeOwnText" else ":matchesWholeText"
        val regex = consumeParens() // don't unescape, as regex bits will be escaped
        Validate.notEmpty(regex, "$query(regex) query must not be empty")
        return if (own) {
            Evaluator.MatchesWholeOwnText(jsSupportedRegex(regex))
        } else {
            Evaluator.MatchesWholeText(jsSupportedRegex(regex))
        }
    }

    // :not(selector)
    private operator fun not(): Evaluator {
        val subQuery = consumeParens()
        Validate.notEmpty(subQuery, ":not(selector) subselect must not be empty")
        return StructuralEvaluator.Not(parse(subQuery))
    }

    override fun toString(): String {
        return query
    }

    public companion object {
        private val Combinators: CharArray = charArrayOf('>', '+', '~') // ' ' is also a combinator, but found implicitly
        private val SequenceEnders: CharArray = charArrayOf(',', ')')
        private val AttributeEvals = arrayOf("=", "!=", "^=", "$=", "*=", "~=")

        /**
         * Parse a CSS query into an Evaluator. If you are evaluating the same query repeatedly, it may be more efficient to
         * parse it once and reuse the Evaluator.
         *
         * @param query CSS query
         * @return Evaluator
         * @see Selector selector query syntax
         */
        public fun parse(query: String): Evaluator {
            return try {
                val p = QueryParser(query)
                p.parse()
            } catch (e: IllegalArgumentException) {
                throw Selector.SelectorParseException(e.message)
            }
        }

        fun combinator(left: Evaluator, combinator: Char, right: Evaluator): Evaluator {
            when (combinator) {
                '>' -> {
                    val run = left as? ImmediateParentRun ?: ImmediateParentRun(left)
                    run.add(right)
                    return run
                }

                ' ' -> return and(StructuralEvaluator.Ancestor(left), right)
                '+' -> return and(StructuralEvaluator.ImmediatePreviousSibling(left), right)
                '~' -> return and(StructuralEvaluator.PreviousSibling(left), right)
                else -> throw Selector.SelectorParseException("Unknown combinator '$combinator'")
            }
        }

        /** Merge two evals into an Or.  */
        fun or(left: Evaluator, right: Evaluator): Evaluator {
            if (left is CombiningEvaluator.Or) {
                left.add(right)
                return left
            }
            return CombiningEvaluator.Or(left, right)
        }

        /** Merge two evals into an And.  */
        fun and(left: Evaluator?, right: Evaluator): Evaluator {
            if (left == null) return right
            if (left is CombiningEvaluator.And) {
                left.add(right)
                return left
            }
            return CombiningEvaluator.And(left, right)
        }

        // pseudo selectors :first-child, :last-child, :nth-child, ...
        private val NthStepOffset: Regex = Regex("(([+-])?(\\d+)?)n(\\s*([+-])?\\s*\\d+)?", RegexOption.IGNORE_CASE)
        private val NthOffset: Regex = Regex("([+-])?(\\d+)")
    }
}
