package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.internal.StringUtil.inSorted
import com.fleeksoft.ksoup.nodes.Attributes
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.DocumentType
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.parser.HtmlTreeBuilderState.Constants.AfterHeadBody
import com.fleeksoft.ksoup.parser.HtmlTreeBuilderState.Constants.BeforeHtmlToHead
import com.fleeksoft.ksoup.parser.HtmlTreeBuilderState.Constants.InBodyEndOtherErrors
import com.fleeksoft.ksoup.parser.HtmlTreeBuilderState.Constants.InBodyStartToHead
import com.fleeksoft.ksoup.parser.HtmlTreeBuilderState.Constants.InHeadEmpty
import com.fleeksoft.ksoup.parser.HtmlTreeBuilderState.Constants.InHeadNoScriptHead
import com.fleeksoft.ksoup.parser.HtmlTreeBuilderState.Constants.InHeadNoscriptIgnore
import com.fleeksoft.ksoup.parser.HtmlTreeBuilderState.Constants.InHeadRaw
import com.fleeksoft.ksoup.parser.HtmlTreeBuilderState.Constants.InTableFoster
import com.fleeksoft.ksoup.parser.HtmlTreeBuilderState.Constants.InTableToBody

/**
 * The Tree Builder's current state. Each state embodies the processing for the state, and transitions to other states.
 */
enum class HtmlTreeBuilderState {
    Initial {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            if (isWhitespace(t)) {
                return true // ignore whitespace until we get the first content
            } else if (t.isComment()) {
                tb.insert(t.asComment())
            } else if (t.isDoctype()) {
                // todo: parse error check on expected doctypes
                // todo: quirk state check on doctype ids
                val d: Token.Doctype = t.asDoctype()
                val doctype = DocumentType(
                    tb.settings!!.normalizeTag(d.getName()),
                    d.getPublicIdentifier(),
                    d.getSystemIdentifier(),
                )
                doctype.setPubSysKey(d.pubSysKey)
                tb.document.appendChild(doctype)
                tb.onNodeInserted(doctype, t)
                if (d.isForceQuirks) tb.document.quirksMode(Document.QuirksMode.quirks)
                tb.transition(BeforeHtml)
            } else {
                // todo: check not iframe srcdoc
                tb.transition(BeforeHtml)
                return tb.process(t) // re-process token
            }
            return true
        }
    },
    BeforeHtml {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            if (t.isDoctype()) {
                tb.error(this)
                return false
            } else if (t.isComment()) {
                tb.insert(t.asComment())
            } else if (isWhitespace(t)) {
                tb.insert(t.asCharacter()) // out of spec - include whitespace
            } else if (t.isStartTag() && t.asStartTag().normalName().equals("html")) {
                tb.insert(t.asStartTag())
                tb.transition(BeforeHead)
            } else if (t.isEndTag() && inSorted(t.asEndTag().normalName(), BeforeHtmlToHead)) {
                return anythingElse(t, tb)
            } else if (t.isEndTag()) {
                tb.error(this)
                return false
            } else {
                return anythingElse(t, tb)
            }
            return true
        }

        private fun anythingElse(t: Token, tb: HtmlTreeBuilder): Boolean {
            tb.insertStartTag("html")
            tb.transition(BeforeHead)
            return tb.process(t)
        }
    },
    BeforeHead {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            if (isWhitespace(t)) {
                tb.insert(t.asCharacter()) // out of spec - include whitespace
            } else if (t.isComment()) {
                tb.insert(t.asComment())
            } else if (t.isDoctype()) {
                tb.error(this)
                return false
            } else if (t.isStartTag() && t.asStartTag().normalName().equals("html")) {
                return InBody.process(t, tb) // does not transition
            } else if (t.isStartTag() && t.asStartTag().normalName().equals("head")) {
                val head: Element = tb.insert(t.asStartTag())
                tb.setHeadElement(head)
                tb.transition(InHead)
            } else if (t.isEndTag() && inSorted(t.asEndTag().normalName(), BeforeHtmlToHead)) {
                tb.processStartTag("head")
                return tb.process(t)
            } else if (t.isEndTag()) {
                tb.error(this)
                return false
            } else {
                tb.processStartTag("head")
                return tb.process(t)
            }
            return true
        }
    },
    InHead {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            if (isWhitespace(t)) {
                tb.insert(t.asCharacter()) // out of spec - include whitespace
                return true
            }
            when (t.type) {
                Token.TokenType.Comment -> tb.insert(t.asComment())
                Token.TokenType.Doctype -> {
                    tb.error(this)
                    return false
                }

                Token.TokenType.StartTag -> {
                    val start: Token.StartTag = t.asStartTag()
                    var name: String = start.normalName()
                    if (name == "html") {
                        return InBody.process(t, tb)
                    } else if (inSorted(name, InHeadEmpty)) {
                        val el: Element = tb.insertEmpty(start)
                        // com.fleeksoft.ksoup special: update base the first time it is seen
                        if (name == "base" && el.hasAttr("href")) tb.maybeSetBaseUri(el)
                    } else if (name == "meta") {
                        tb.insertEmpty(start)
                        // todo: charset switches
                    } else if (name == "title") {
                        handleRcData(start, tb)
                    } else if (inSorted(name, InHeadRaw)) {
                        handleRawtext(start, tb)
                    } else if (name == "noscript") {
                        // else if noscript && scripting flag = true: rawtext (com.fleeksoft.ksoup doesn't run script, to handle as noscript)
                        tb.insert(start)
                        tb.transition(InHeadNoscript)
                    } else if (name == "script") {
                        // skips some script rules as won't execute them
                        tb.tokeniser!!.transition(TokeniserState.ScriptData)
                        tb.markInsertionMode()
                        tb.transition(Text)
                        tb.insert(start)
                    } else if (name == "head") {
                        tb.error(this)
                        return false
                    } else if (name == "template") {
                        tb.insert(start)
                        tb.insertMarkerToFormattingElements()
                        tb.framesetOk(false)
                        tb.transition(InTemplate)
                        tb.pushTemplateMode(InTemplate)
                    } else {
                        return anythingElse(t, tb)
                    }
                }

                Token.TokenType.EndTag -> {
                    val end: Token.EndTag = t.asEndTag()
                    val name = end.normalName()
                    if (name == "head") {
                        tb.pop()
                        tb.transition(AfterHead)
                    } else if (inSorted(name, Constants.InHeadEnd)) {
                        return anythingElse(t, tb)
                    } else if (name == "template") {
                        if (!tb.onStack(name)) {
                            tb.error(this)
                        } else {
                            tb.generateImpliedEndTags(true)
                            if (!tb.currentElementIs(name)) tb.error(this)
                            tb.popStackToClose(name)
                            tb.clearFormattingElementsToLastMarker()
                            tb.popTemplateMode()
                            tb.resetInsertionMode()
                        }
                    } else {
                        tb.error(this)
                        return false
                    }
                }

                else -> return anythingElse(t, tb)
            }
            return true
        }

        private fun anythingElse(t: Token, tb: TreeBuilder): Boolean {
            tb.processEndTag("head")
            return tb.process(t)
        }
    },
    InHeadNoscript {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            if (t.isDoctype()) {
                tb.error(this)
            } else if (t.isStartTag() && t.asStartTag().normalName().equals("html")) {
                return tb.process(t, InBody)
            } else if (t.isEndTag() && t.asEndTag().normalName().equals("noscript")) {
                tb.pop()
                tb.transition(InHead)
            } else if (isWhitespace(t) || t.isComment() || t.isStartTag() && inSorted(
                    t.asStartTag().normalName(),
                    InHeadNoScriptHead,
                )
            ) {
                return tb.process(t, InHead)
            } else if (t.isEndTag() && t.asEndTag().normalName().equals("br")) {
                return anythingElse(t, tb)
            } else if (t.isStartTag() && inSorted(
                    t.asStartTag().normalName(),
                    InHeadNoscriptIgnore,
                ) || t.isEndTag()
            ) {
                tb.error(this)
                return false
            } else {
                return anythingElse(t, tb)
            }
            return true
        }

        private fun anythingElse(t: Token, tb: HtmlTreeBuilder): Boolean {
            // note that this deviates from spec, which is to pop out of noscript and reprocess in head:
            // https://html.spec.whatwg.org/multipage/parsing.html#parsing-main-inheadnoscript
            // allows content to be inserted as data
            tb.error(this)
            tb.insert(Token.Character().data(t.toString()))
            return true
        }
    },
    AfterHead {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            if (isWhitespace(t)) {
                tb.insert(t.asCharacter())
            } else if (t.isComment()) {
                tb.insert(t.asComment())
            } else if (t.isDoctype()) {
                tb.error(this)
            } else if (t.isStartTag()) {
                val startTag: Token.StartTag = t.asStartTag()
                val name: String = startTag.normalName()
                if (name == "html") {
                    return tb.process(t, InBody)
                } else if (name == "body") {
                    tb.insert(startTag)
                    tb.framesetOk(false)
                    tb.transition(InBody)
                } else if (name == "frameset") {
                    tb.insert(startTag)
                    tb.transition(InFrameset)
                } else if (inSorted(name, InBodyStartToHead)) {
                    tb.error(this)
                    val head: Element = tb.getHeadElement()!!
                    tb.push(head)
                    tb.process(t, InHead)
                    tb.removeFromStack(head)
                } else if (name == "head") {
                    tb.error(this)
                    return false
                } else {
                    anythingElse(t, tb)
                }
            } else if (t.isEndTag()) {
                val name: String = t.asEndTag().normalName()
                if (inSorted(name, AfterHeadBody)) {
                    anythingElse(t, tb)
                } else if (name == "template") {
                    tb.process(t, InHead)
                } else {
                    tb.error(this)
                    return false
                }
            } else {
                anythingElse(t, tb)
            }
            return true
        }

        private fun anythingElse(t: Token, tb: HtmlTreeBuilder): Boolean {
            tb.processStartTag("body")
            tb.framesetOk(true)
            return tb.process(t)
        }
    },
    InBody {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            when (t.type!!) {
                Token.TokenType.Character -> {
                    val c: Token.Character = t.asCharacter()
                    if (c.data.equals(nullString)) {
                        // todo confirm that check
                        tb.error(this)
                        return false
                    } else if (tb.framesetOk() && isWhitespace(c)) { // don't check if whitespace if frames already closed
                        tb.reconstructFormattingElements()
                        tb.insert(c)
                    } else {
                        tb.reconstructFormattingElements()
                        tb.insert(c)
                        tb.framesetOk(false)
                    }
                }

                Token.TokenType.Comment -> {
                    tb.insert(t.asComment())
                }

                Token.TokenType.Doctype -> {
                    tb.error(this)
                    return false
                }

                Token.TokenType.StartTag -> return inBodyStartTag(t, tb)
                Token.TokenType.EndTag -> return inBodyEndTag(t, tb)
                Token.TokenType.EOF -> {
                    if (tb.templateModeSize() > 0) return tb.process(t, InTemplate)
                    if (tb.onStackNot(InBodyEndOtherErrors)) tb.error(this)
                }
            }
            return true
        }

        private fun inBodyStartTag(t: Token, tb: HtmlTreeBuilder): Boolean {
            val startTag: Token.StartTag = t.asStartTag()
            val name: String = startTag.normalName()
            val stack: ArrayList<Element?>
            var el: Element?
            when (name) {
                "a" -> {
                    if (tb.getActiveFormattingElement("a") != null) {
                        tb.error(this)
                        tb.processEndTag("a")

                        // still on stack?
                        val remainingA: Element? = tb.getFromStack("a")
                        if (remainingA != null) {
                            tb.removeFromActiveFormattingElements(remainingA)
                            tb.removeFromStack(remainingA)
                        }
                    }
                    tb.reconstructFormattingElements()
                    el = tb.insert(startTag)
                    tb.pushActiveFormattingElements(el)
                }

                "span" -> {
                    // same as final else, but short circuits lots of checks
                    tb.reconstructFormattingElements()
                    tb.insert(startTag)
                }

                "li" -> {
                    tb.framesetOk(false)
                    stack = tb.stack
                    var i: Int = stack.size - 1
                    while (i > 0) {
                        el = stack[i]
                        if (el?.normalName() == "li") {
                            tb.processEndTag("li")
                            break
                        }
                        if (tb.isSpecial(el!!) && !inSorted(
                                el.normalName(),
                                Constants.InBodyStartLiBreakers,
                            )
                        ) {
                            break
                        }
                        i--
                    }
                    if (tb.inButtonScope("p")) {
                        tb.processEndTag("p")
                    }
                    tb.insert(startTag)
                }

                "html" -> {
                    tb.error(this)
                    if (tb.onStack("template")) return false // ignore
                    // otherwise, merge attributes onto real html (if present)
                    stack = tb.stack
                    if (stack.size > 0) {
                        val html: Element = tb.stack[0]!!
                        if (startTag.hasAttributes()) {
                            for (attribute in (startTag.attributes ?: emptyList())) {
                                if (!html.hasAttr(attribute!!.key)) {
                                    html.attributes()
                                        .put(attribute)
                                }
                            }
                        }
                    }
                }

                "body" -> {
                    tb.error(this)
                    stack = tb.stack
                    if (stack.size == 1 || stack.size > 2 && stack[1]?.normalName() != "body" ||
                        tb.onStack("template")
                    ) {
                        // only in fragment case
                        return false // ignore
                    } else {
                        tb.framesetOk(false)
                        // will be on stack if this is a nested body. won't be if closed (which is a variance from spec, which leaves it on)
                        var body: Element? = null
                        if (startTag.hasAttributes() && tb.getFromStack("body")
                                .also { body = it!! } != null
                        ) { // we only ever put one body on stack
                            for (attribute in startTag.attributes!!) {
                                if (body?.hasAttr(attribute.key) != true) {
                                    body?.attributes()
                                        ?.put(attribute)
                                }
                            }
                        }
                    }
                }

                "frameset" -> {
                    tb.error(this)
                    stack = tb.stack
                    if (stack.size == 1 || stack.size > 2 && stack[1]?.normalName() != "body") {
                        // only in fragment case
                        return false // ignore
                    } else if (!tb.framesetOk()) {
                        return false // ignore frameset
                    } else {
                        val second: Element? = stack[1]
                        if (second?.parent() != null) second?.remove()
                        // pop up to html element
                        while (stack.size > 1) stack.removeAt(stack.size - 1)
                        tb.insert(startTag)
                        tb.transition(InFrameset)
                    }
                }

                "form" -> {
                    if (tb.getFormElement() != null && !tb.onStack("template")) {
                        tb.error(this)
                        return false
                    }
                    if (tb.inButtonScope("p")) {
                        tb.closeElement("p")
                    }
                    tb.insertForm(startTag, true, true) // won't associate to any template
                }

                "plaintext" -> {
                    if (tb.inButtonScope("p")) {
                        tb.processEndTag("p")
                    }
                    tb.insert(startTag)
                    tb.tokeniser!!.transition(TokeniserState.PLAINTEXT) // once in, never gets out
                }

                "button" -> if (tb.inButtonScope("button")) {
                    // close and reprocess
                    tb.error(this)
                    tb.processEndTag("button")
                    tb.process(startTag)
                } else {
                    tb.reconstructFormattingElements()
                    tb.insert(startTag)
                    tb.framesetOk(false)
                }

                "nobr" -> {
                    tb.reconstructFormattingElements()
                    if (tb.inScope("nobr")) {
                        tb.error(this)
                        tb.processEndTag("nobr")
                        tb.reconstructFormattingElements()
                    }
                    el = tb.insert(startTag)
                    tb.pushActiveFormattingElements(el)
                }

                "table" -> {
                    if (
                        tb.document.quirksMode() !== Document.QuirksMode.quirks &&
                        tb.inButtonScope("p")
                    ) {
                        tb.processEndTag("p")
                    }
                    tb.insert(startTag)
                    tb.framesetOk(false)
                    tb.transition(InTable)
                }

                "input" -> {
                    tb.reconstructFormattingElements()
                    el = tb.insertEmpty(startTag)
                    if (!el.attr("type").equals("hidden", ignoreCase = true)) tb.framesetOk(false)
                }

                "hr" -> {
                    if (tb.inButtonScope("p")) {
                        tb.processEndTag("p")
                    }
                    tb.insertEmpty(startTag)
                    tb.framesetOk(false)
                }

                "image" -> if (tb.getFromStack("svg") == null) {
                    return tb.process(startTag.name("img")) // change <image> to <img>, unless in svg
                } else {
                    tb.insert(startTag)
                }

                "isindex" -> {
                    // how much do we care about the early 90s?
                    tb.error(this)
                    if (tb.getFormElement() != null) return false
                    tb.processStartTag("form")
                    if (startTag.hasAttribute("action")) {
                        val form: Element? = tb.getFormElement()
                        if (form != null && startTag.hasAttribute("action")) {
                            val action: String = startTag.attributes!!["action"]
                            form.attributes().put(
                                "action",
                                action,
                            ) // always LC, so don't need to scan up for ownerdoc
                        }
                    }
                    tb.processStartTag("hr")
                    tb.processStartTag("label")
                    // hope you like english.
                    val prompt =
                        if (startTag.hasAttribute("prompt")) startTag.attributes!!["prompt"] else "This is a searchable index. Enter search keywords: "
                    tb.process(Token.Character().data(prompt))

                    // input
                    val inputAttribs = Attributes()
                    if (startTag.hasAttributes()) {
                        for (attr in startTag.attributes!!) {
                            if (!inSorted(
                                    attr.key,
                                    Constants.InBodyStartInputAttribs,
                                )
                            ) {
                                inputAttribs.put(attr)
                            }
                        }
                    }
                    inputAttribs.put("name", "isindex")
                    tb.processStartTag("input", inputAttribs)
                    tb.processEndTag("label")
                    tb.processStartTag("hr")
                    tb.processEndTag("form")
                }

                "textarea" -> {
                    tb.insert(startTag)
                    if (!startTag.isSelfClosing) {
                        tb.tokeniser!!.transition(TokeniserState.Rcdata)
                        tb.markInsertionMode()
                        tb.framesetOk(false)
                        tb.transition(Text)
                    }
                }

                "xmp" -> {
                    if (tb.inButtonScope("p")) {
                        tb.processEndTag("p")
                    }
                    tb.reconstructFormattingElements()
                    tb.framesetOk(false)
                    handleRawtext(startTag, tb)
                }

                "iframe" -> {
                    tb.framesetOk(false)
                    handleRawtext(startTag, tb)
                }

                "noembed" -> // also handle noscript if script enabled
                    handleRawtext(startTag, tb)

                "select" -> {
                    tb.reconstructFormattingElements()
                    tb.insert(startTag)
                    tb.framesetOk(false)
                    if (!startTag.isSelfClosing) {
                        // don't change states if not added to the stack
                        val state: HtmlTreeBuilderState? = tb.state()
                        if (state == InTable || state == InCaption || state == InTableBody || state == InRow || state == InCell) {
                            tb.transition(
                                InSelectInTable,
                            )
                        } else {
                            tb.transition(InSelect)
                        }
                    }
                }

                "math" -> {
                    tb.reconstructFormattingElements()
                    tb.insertForeign(startTag, Parser.NamespaceMathml)
                }

                "svg" -> {
                    tb.reconstructFormattingElements()
                    tb.insertForeign(startTag, Parser.NamespaceSvg)
                }

                "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    if (tb.inButtonScope("p")) {
                        tb.processEndTag("p")
                    }
                    if (inSorted(tb.currentElement().normalName(), Constants.Headings)) {
                        tb.error(this)
                        tb.pop()
                    }
                    tb.insert(startTag)
                }

                "pre", "listing" -> {
                    if (tb.inButtonScope("p")) {
                        tb.processEndTag("p")
                    }
                    tb.insert(startTag)
                    tb.reader.matchConsume("\n") // ignore LF if next token
                    tb.framesetOk(false)
                }

                "dd", "dt" -> {
                    tb.framesetOk(false)
                    stack = tb.stack
                    val bottom: Int = stack.size - 1
                    val upper = if (bottom >= MaxStackScan) bottom - MaxStackScan else 0
                    var i = bottom
                    while (i >= upper) {
                        el = stack[i] ?: continue
                        if (inSorted(el.normalName(), Constants.DdDt)) {
                            tb.processEndTag(el.normalName())
                            break
                        }
                        if (tb.isSpecial(el) && !inSorted(
                                el.normalName(),
                                Constants.InBodyStartLiBreakers,
                            )
                        ) {
                            break
                        }
                        i--
                    }
                    if (tb.inButtonScope("p")) {
                        tb.processEndTag("p")
                    }
                    tb.insert(startTag)
                }

                "optgroup", "option" -> {
                    if (tb.currentElementIs("option")) tb.processEndTag("option")
                    tb.reconstructFormattingElements()
                    tb.insert(startTag)
                }

                "rb", "rtc" -> {
                    if (tb.inScope("ruby")) {
                        tb.generateImpliedEndTags()
                        if (!tb.currentElementIs("ruby")) tb.error(this)
                    }
                    tb.insert(startTag)
                }

                "rp", "rt" -> {
                    if (tb.inScope("ruby")) {
                        tb.generateImpliedEndTags("rtc")
                        if (!tb.currentElementIs("rtc") && !tb.currentElementIs("ruby")) {
                            tb.error(
                                this,
                            )
                        }
                    }
                    tb.insert(startTag)
                }

                "area", "br", "embed", "img", "keygen", "wbr" -> {
                    tb.reconstructFormattingElements()
                    tb.insertEmpty(startTag)
                    tb.framesetOk(false)
                }

                "b", "big", "code", "em", "font", "i", "s", "small", "strike", "strong", "tt", "u" -> {
                    tb.reconstructFormattingElements()
                    el = tb.insert(startTag)
                    tb.pushActiveFormattingElements(el)
                }

                else -> // todo - bring scan groups in if desired
                    if (!Tag.isKnownTag(name)) { // no special rules for custom tags
                        tb.insert(startTag)
                    } else if (inSorted(name, Constants.InBodyStartPClosers)) {
                        if (tb.inButtonScope("p")) {
                            tb.processEndTag("p")
                        }
                        tb.insert(startTag)
                    } else if (inSorted(name, Constants.InBodyStartToHead)) {
                        return tb.process(t, InHead)
                    } else if (inSorted(name, Constants.InBodyStartApplets)) {
                        tb.reconstructFormattingElements()
                        tb.insert(startTag)
                        tb.insertMarkerToFormattingElements()
                        tb.framesetOk(false)
                    } else if (inSorted(name, Constants.InBodyStartMedia)) {
                        tb.insertEmpty(startTag)
                    } else if (inSorted(name, Constants.InBodyStartDrop)) {
                        tb.error(this)
                        return false
                    } else {
                        tb.reconstructFormattingElements()
                        tb.insert(startTag)
                    }
            }
            return true
        }

        private val MaxStackScan = 24 // used for DD / DT scan, prevents runaway
        private fun inBodyEndTag(t: Token, tb: HtmlTreeBuilder): Boolean {
            val endTag: Token.EndTag = t.asEndTag()
            val name: String = endTag.normalName()
            when (name) {
                "template" -> tb.process(t, InHead)
                "sarcasm", "span" -> // same as final fall through, but saves short circuit
                    return anyOtherEndTag(t, tb)

                "li" -> if (!tb.inListItemScope(name)) {
                    tb.error(this)
                    return false
                } else {
                    tb.generateImpliedEndTags(name)
                    if (!tb.currentElementIs(name)) tb.error(this)
                    tb.popStackToClose(name)
                }

                "body" -> if (!tb.inScope("body")) {
                    tb.error(this)
                    return false
                } else {
                    if (tb.onStackNot(InBodyEndOtherErrors)) tb.error(this)
                    tb.transition(AfterBody)
                }

                "html" -> return if (!tb.onStack("body")) {
                    tb.error(this)
                    false // ignore
                } else {
                    if (tb.onStackNot(InBodyEndOtherErrors)) tb.error(this)
                    tb.transition(AfterBody)
                    tb.process(t) // re-process
                }

                "form" -> if (!tb.onStack("template")) {
                    val currentForm: Element? = tb.getFormElement()
                    tb.setFormElement(null)
                    if (currentForm == null || !tb.inScope(name)) {
                        tb.error(this)
                        return false
                    }
                    tb.generateImpliedEndTags()
                    if (!tb.currentElementIs(name)) tb.error(this)
                    // remove currentForm from stack. will shift anything under up.
                    tb.removeFromStack(currentForm)
                } else { // template on stack
                    if (!tb.inScope(name)) {
                        tb.error(this)
                        return false
                    }
                    tb.generateImpliedEndTags()
                    if (!tb.currentElementIs(name)) tb.error(this)
                    tb.popStackToClose(name)
                }

                "p" -> if (!tb.inButtonScope(name)) {
                    tb.error(this)
                    tb.processStartTag(name) // if no p to close, creates an empty <p></p>
                    return tb.process(endTag)
                } else {
                    tb.generateImpliedEndTags(name)
                    if (!tb.currentElementIs(name)) tb.error(this)
                    tb.popStackToClose(name)
                }

                "dd", "dt" -> if (!tb.inScope(name)) {
                    tb.error(this)
                    return false
                } else {
                    tb.generateImpliedEndTags(name)
                    if (!tb.currentElementIs(name)) tb.error(this)
                    tb.popStackToClose(name)
                }

                "h1", "h2", "h3", "h4", "h5", "h6" -> if (!tb.inScope(Constants.Headings)) {
                    tb.error(this)
                    return false
                } else {
                    tb.generateImpliedEndTags(name)
                    if (!tb.currentElementIs(name)) tb.error(this)
                    tb.popStackToClose(*Constants.Headings)
                }

                "br" -> {
                    tb.error(this)
                    tb.processStartTag("br")
                    return false
                }

                else -> // todo - move rest to switch if desired
                    if (inSorted(name, Constants.InBodyEndAdoptionFormatters)) {
                        return inBodyEndTagAdoption(t, tb)
                    } else if (inSorted(name, Constants.InBodyEndClosers)) {
                        if (!tb.inScope(name)) {
                            // nothing to close
                            tb.error(this)
                            return false
                        } else {
                            tb.generateImpliedEndTags()
                            if (!tb.currentElementIs(name)) tb.error(this)
                            tb.popStackToClose(name)
                        }
                    } else if (inSorted(name, Constants.InBodyStartApplets)) {
                        if (!tb.inScope("name")) {
                            if (!tb.inScope(name)) {
                                tb.error(this)
                                return false
                            }
                            tb.generateImpliedEndTags()
                            if (!tb.currentElementIs(name)) tb.error(this)
                            tb.popStackToClose(name)
                            tb.clearFormattingElementsToLastMarker()
                        }
                    } else {
                        return anyOtherEndTag(t, tb)
                    }
            }
            return true
        }

        fun anyOtherEndTag(t: Token, tb: HtmlTreeBuilder): Boolean {
            val name: String =
                t.asEndTag().normalName!! // case insensitive search - goal is to preserve output case, not for the parse to be case sensitive
            val stack: ArrayList<Element> = arrayListOf(
                *tb.stack.mapNotNull { it }.toList()
                    .toTypedArray(),
            )

            // deviate from spec slightly to speed when super deeply nested
            val elFromStack: Element? = tb.getFromStack(name)
            if (elFromStack == null) {
                tb.error(this)
                return false
            }
            for (pos in stack.indices.reversed()) {
                val node: Element = stack[pos]
                if (node.normalName() == name) {
                    tb.generateImpliedEndTags(name)
                    if (!tb.currentElementIs(name)) tb.error(this)
                    tb.popStackToClose(name)
                    break
                } else {
                    if (tb.isSpecial(node)) {
                        tb.error(this)
                        return false
                    }
                }
            }
            return true
        }

        // Adoption Agency Algorithm.
        private fun inBodyEndTagAdoption(t: Token, tb: HtmlTreeBuilder): Boolean {
            val endTag: Token.EndTag = t.asEndTag()
            val name: String = endTag.normalName()
            val stack: ArrayList<Element> = tb.stack as ArrayList<Element>
            var el: Element
            for (i in 0..7) {
                val formatEl: Element? = tb.getActiveFormattingElement(name)
                if (formatEl == null) {
                    return anyOtherEndTag(t, tb)
                } else if (!tb.onStack(formatEl)) {
                    tb.error(this)
                    tb.removeFromActiveFormattingElements(formatEl)
                    return true
                } else if (!tb.inScope(formatEl.normalName())) {
                    tb.error(this)
                    return false
                } else if (tb.currentElement() !== formatEl) {
                    tb.error(this)
                }
                var furthestBlock: Element? = null
                var commonAncestor: Element? = null
                var seenFormattingElement = false
                // the spec doesn't limit to < 64, but in degenerate cases (9000+ stack depth) this prevents run-aways
                val stackSize: Int = stack.size
                var bookmark = -1
                var si = 1
                while (si < stackSize && si < 64) {
                    // TODO: this no longer matches the current spec at https://html.spec.whatwg.org/#adoption-agency-algorithm and should be updated
                    el = stack[si]
                    if (el == formatEl) {
                        commonAncestor = stack[si - 1]
                        seenFormattingElement = true
                        // Let a bookmark note the position of the formatting element in the list of active formatting elements relative to the elements on either side of it in the list.
                        bookmark = tb.positionOfElement(el)
                    } else if (seenFormattingElement && tb.isSpecial(el)) {
                        furthestBlock = el
                        break
                    }
                    si++
                }
                if (furthestBlock == null) {
                    tb.popStackToClose(formatEl.normalName())
                    tb.removeFromActiveFormattingElements(formatEl)
                    return true
                }
                var node: Element = furthestBlock
                var lastNode: Element? = furthestBlock
                for (j in 0..2) {
                    if (tb.onStack(node)) node = tb.aboveOnStack(node)!!
                    if (!tb.isInActiveFormattingElements(node)) { // note no bookmark check
                        tb.removeFromStack(node)
                        continue
                    } else if (node === formatEl) break
                    val replacement = Element(
                        tb.tagFor(node.nodeName(), ParseSettings.preserveCase),
                        tb.baseUri,
                    )
                    // case will follow the original node (so honours ParseSettings)
                    tb.replaceActiveFormattingElement(node, replacement)
                    tb.replaceOnStack(node, replacement)
                    node = replacement
                    if (lastNode === furthestBlock) {
                        // move the aforementioned bookmark to be immediately after the new node in the list of active formatting elements.
                        // not getting how this bookmark both straddles the element above, but is inbetween here...
                        bookmark = tb.positionOfElement(node) + 1
                    }
                    if (lastNode!!.parent() != null) lastNode.remove()
                    node.appendChild(lastNode)
                    lastNode = node
                }
                if (commonAncestor != null) { // safety check, but would be an error if null
                    if (inSorted(commonAncestor.normalName(), Constants.InBodyEndTableFosters)) {
                        if (lastNode!!.parent() != null) lastNode.remove()
                        tb.insertInFosterParent(lastNode)
                    } else {
                        if (lastNode!!.parent() != null) lastNode.remove()
                        commonAncestor.appendChild(lastNode)
                    }
                }
                val adopter = Element(formatEl.tag(), tb.baseUri)
                adopter.attributes().addAll(formatEl.attributes())
                adopter.appendChildren(furthestBlock.childNodes())
                furthestBlock.appendChild(adopter)
                tb.removeFromActiveFormattingElements(formatEl)
                // insert the new element into the list of active formatting elements at the position of the aforementioned bookmark.
                tb.pushWithBookmark(adopter, bookmark)
                tb.removeFromStack(formatEl)
                tb.insertOnStackAfter(furthestBlock, adopter)
            }
            return true
        }
    },
    Text {
        // in script, style etc. normally treated as data tags
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            if (t.isCharacter()) {
                tb.insert(t.asCharacter())
            } else if (t.isEOF()) {
                tb.error(this)
                // if current node is script: already started
                tb.pop()
                tb.transition(tb.originalState())
                return tb.process(t)
            } else if (t.isEndTag()) {
                // if: An end tag whose tag name is "script" -- scripting nesting level, if evaluating scripts
                tb.pop()
                tb.transition(tb.originalState())
            }
            return true
        }
    },
    InTable {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            if (t.isCharacter() && inSorted(tb.currentElement().normalName(), InTableFoster)) {
                tb.resetPendingTableCharacters()
                tb.markInsertionMode()
                tb.transition(InTableText)
                return tb.process(t)
            } else if (t.isComment()) {
                tb.insert(t.asComment())
                return true
            } else if (t.isDoctype()) {
                tb.error(this)
                return false
            } else if (t.isStartTag()) {
                val startTag: Token.StartTag = t.asStartTag()
                val name: String = startTag.normalName()
                if (name == "caption") {
                    tb.clearStackToTableContext()
                    tb.insertMarkerToFormattingElements()
                    tb.insert(startTag)
                    tb.transition(InCaption)
                } else if (name == "colgroup") {
                    tb.clearStackToTableContext()
                    tb.insert(startTag)
                    tb.transition(InColumnGroup)
                } else if (name == "col") {
                    tb.clearStackToTableContext()
                    tb.processStartTag("colgroup")
                    return tb.process(t)
                } else if (inSorted(name, Constants.InTableToBody)) {
                    tb.clearStackToTableContext()
                    tb.insert(startTag)
                    tb.transition(InTableBody)
                } else if (inSorted(name, Constants.InTableAddBody)) {
                    tb.clearStackToTableContext()
                    tb.processStartTag("tbody")
                    return tb.process(t)
                } else if (name == "table") {
                    tb.error(this)
                    return if (!tb.inTableScope(name)) { // ignore it
                        false
                    } else {
                        tb.popStackToClose(name)
                        if (!tb.resetInsertionMode()) {
                            // not per spec - but haven't transitioned out of table. so try something else
                            tb.insert(startTag)
                            return true
                        }
                        tb.process(t)
                    }
                } else if (inSorted(name, Constants.InTableToHead)) {
                    return tb.process(t, InHead)
                } else if (name == "input") {
                    if (!(
                                startTag.hasAttributes() &&
                                        startTag.attributes!!["type"].equals(
                                            "hidden",
                                            ignoreCase = true,
                                        )
                                )
                    ) {
                        return anythingElse(t, tb)
                    } else {
                        tb.insertEmpty(startTag)
                    }
                } else if (name == "form") {
                    tb.error(this)
                    if (tb.getFormElement() != null || tb.onStack("template")) {
                        return false
                    } else {
                        tb.insertForm(
                            startTag,
                            false,
                            false,
                        ) // not added to stack. can associate to template
                    }
                } else {
                    return anythingElse(t, tb)
                }
                return true // todo: check if should return processed http://www.whatwg.org/specs/web-apps/current-work/multipage/tree-construction.html#parsing-main-intable
            } else if (t.isEndTag()) {
                val endTag: Token.EndTag = t.asEndTag()
                val name: String = endTag.normalName()
                if (name == "table") {
                    if (!tb.inTableScope(name)) {
                        tb.error(this)
                        return false
                    } else {
                        tb.popStackToClose("table")
                        tb.resetInsertionMode()
                    }
                } else if (inSorted(name, Constants.InTableEndErr)) {
                    tb.error(this)
                    return false
                } else if (name == "template") {
                    tb.process(t, InHead)
                } else {
                    return anythingElse(t, tb)
                }
                return true // todo: as above todo
            } else if (t.isEOF()) {
                if (tb.currentElementIs("html")) tb.error(this)
                return true // stops parsing
            }
            return anythingElse(t, tb)
        }

        fun anythingElse(t: Token, tb: HtmlTreeBuilder): Boolean {
            tb.error(this)
            tb.isFosterInserts = true
            tb.process(t, InBody)
            tb.isFosterInserts = false
            return true
        }
    },
    InTableText {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            if (t.type === Token.TokenType.Character) {
                val c: Token.Character = t.asCharacter()
                if (c.data.equals(nullString)) {
                    tb.error(this)
                    return false
                } else {
                    tb.addPendingTableCharacters(c)
                }
            } else {
                if (tb.getPendingTableCharacters()!!.isNotEmpty()) {
                    for (c in tb.getPendingTableCharacters()!!) {
                        if (!isWhitespace(c)) {
                            // InTable anything else section:
                            tb.error(this)
                            if (inSorted(tb.currentElement().normalName(), InTableFoster)) {
                                tb.isFosterInserts = true
                                tb.process(c, InBody)
                                tb.isFosterInserts = false
                            } else {
                                tb.process(c, InBody)
                            }
                        } else {
                            tb.insert(c)
                        }
                    }
                    tb.resetPendingTableCharacters()
                }
                tb.transition(tb.originalState())
                return tb.process(t)
            }
            return true
        }
    },
    InCaption {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            if (t.isEndTag() && t.asEndTag().normalName().equals("caption")) {
                val endTag: Token.EndTag = t.asEndTag()
                val name: String = endTag.normalName()
                if (!tb.inTableScope(name)) {
                    tb.error(this)
                    return false
                } else {
                    tb.generateImpliedEndTags()
                    if (!tb.currentElementIs("caption")) tb.error(this)
                    tb.popStackToClose("caption")
                    tb.clearFormattingElementsToLastMarker()
                    tb.transition(InTable)
                }
            } else if (t.isStartTag() && inSorted(
                    t.asStartTag().normalName(),
                    Constants.InCellCol,
                ) ||
                t.isEndTag() && t.asEndTag().normalName().equals("table")
            ) {
                tb.error(this)
                val processed: Boolean = tb.processEndTag("caption")
                if (processed) return tb.process(t)
            } else if (t.isEndTag() && inSorted(
                    t.asEndTag().normalName(),
                    Constants.InCaptionIgnore,
                )
            ) {
                tb.error(this)
                return false
            } else {
                return tb.process(t, InBody)
            }
            return true
        }
    },
    InColumnGroup {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            if (isWhitespace(t)) {
                tb.insert(t.asCharacter())
                return true
            }
            when (t.type) {
                Token.TokenType.Comment -> tb.insert(t.asComment())
                Token.TokenType.Doctype -> tb.error(this)
                Token.TokenType.StartTag -> {
                    val startTag: Token.StartTag = t.asStartTag()
                    when (startTag.normalName()) {
                        "html" -> return tb.process(t, InBody)
                        "col" -> tb.insertEmpty(startTag)
                        "template" -> tb.process(t, InHead)
                        else -> return anythingElse(t, tb)
                    }
                }

                Token.TokenType.EndTag -> {
                    val endTag: Token.EndTag = t.asEndTag()
                    val name: String = endTag.normalName()
                    when (name) {
                        "colgroup" -> if (!tb.currentElementIs(name)) {
                            tb.error(this)
                            return false
                        } else {
                            tb.pop()
                            tb.transition(InTable)
                        }

                        "template" -> tb.process(t, InHead)
                        else -> return anythingElse(t, tb)
                    }
                }

                Token.TokenType.EOF -> return if (tb.currentElementIs("html")) {
                    true // stop parsing; frag case
                } else {
                    anythingElse(t, tb)
                }

                else -> return anythingElse(t, tb)
            }
            return true
        }

        private fun anythingElse(t: Token, tb: HtmlTreeBuilder): Boolean {
            if (!tb.currentElementIs("colgroup")) {
                tb.error(this)
                return false
            }
            tb.pop()
            tb.transition(InTable)
            tb.process(t)
            return true
        }
    },
    InTableBody {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            when (t.type) {
                Token.TokenType.StartTag -> {
                    val startTag: Token.StartTag = t.asStartTag()
                    val name: String = startTag.normalName()
                    if (name == "tr") {
                        tb.clearStackToTableBodyContext()
                        tb.insert(startTag)
                        tb.transition(InRow)
                    } else if (inSorted(name, Constants.InCellNames)) {
                        tb.error(this)
                        tb.processStartTag("tr")
                        return tb.process(startTag)
                    } else {
                        return if (inSorted(name, Constants.InTableBodyExit)) {
                            exitTableBody(t, tb)
                        } else {
                            anythingElse(t, tb)
                        }
                    }
                }

                Token.TokenType.EndTag -> {
                    val endTag: Token.EndTag = t.asEndTag()
                    val name = endTag.normalName()
                    if (inSorted(name, Constants.InTableEndIgnore)) {
                        if (!tb.inTableScope(name)) {
                            tb.error(this)
                            return false
                        } else {
                            tb.clearStackToTableBodyContext()
                            tb.pop()
                            tb.transition(InTable)
                        }
                    } else if (name == "table") {
                        return exitTableBody(t, tb)
                    } else if (inSorted(name, Constants.InTableBodyEndIgnore)) {
                        tb.error(this)
                        return false
                    } else {
                        return anythingElse(t, tb)
                    }
                }

                else -> return anythingElse(t, tb)
            }
            return true
        }

        private fun exitTableBody(t: Token, tb: HtmlTreeBuilder): Boolean {
            if (!(tb.inTableScope("tbody") || tb.inTableScope("thead") || tb.inScope("tfoot"))) {
                // frag case
                tb.error(this)
                return false
            }
            tb.clearStackToTableBodyContext()
            tb.processEndTag(tb.currentElement().normalName()) // tbody, tfoot, thead
            return tb.process(t)
        }

        private fun anythingElse(t: Token, tb: HtmlTreeBuilder): Boolean {
            return tb.process(t, InTable)
        }
    },
    InRow {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            if (t.isStartTag()) {
                val startTag: Token.StartTag = t.asStartTag()
                val name: String = startTag.normalName()
                if (inSorted(name, Constants.InCellNames)) { // th, th
                    tb.clearStackToTableRowContext()
                    tb.insert(startTag)
                    tb.transition(InCell)
                    tb.insertMarkerToFormattingElements()
                } else if (inSorted(
                        name,
                        Constants.InRowMissing,
                    )
                ) { // "caption", "col", "colgroup", "tbody", "tfoot", "thead", "tr"
                    if (!tb.inTableScope("tr")) {
                        tb.error(this)
                        return false
                    }
                    tb.clearStackToTableRowContext()
                    tb.pop() // tr
                    tb.transition(InTableBody)
                    return tb.process(t)
                } else {
                    return anythingElse(t, tb)
                }
            } else if (t.isEndTag()) {
                val endTag: Token.EndTag = t.asEndTag()
                val name: String = endTag.normalName()
                if (name == "tr") {
                    if (!tb.inTableScope(name)) {
                        tb.error(this) // frag
                        return false
                    }
                    tb.clearStackToTableRowContext()
                    tb.pop() // tr
                    tb.transition(InTableBody)
                } else if (name == "table") {
                    if (!tb.inTableScope("tr")) {
                        tb.error(this)
                        return false
                    }
                    tb.clearStackToTableRowContext()
                    tb.pop() // tr
                    tb.transition(InTableBody)
                    return tb.process(t)
                } else if (inSorted(name, InTableToBody)) { // "tbody", "tfoot", "thead"
                    if (!tb.inTableScope(name)) {
                        tb.error(this)
                        return false
                    }
                    if (!tb.inTableScope("tr")) {
                        // not an error per spec?
                        return false
                    }
                    tb.clearStackToTableRowContext()
                    tb.pop() // tr
                    tb.transition(InTableBody)
                    return tb.process(t)
                } else if (inSorted(name, Constants.InRowIgnore)) {
                    tb.error(this)
                    return false
                } else {
                    return anythingElse(t, tb)
                }
            } else {
                return anythingElse(t, tb)
            }
            return true
        }

        private fun anythingElse(t: Token, tb: HtmlTreeBuilder): Boolean {
            return tb.process(t, InTable)
        }
    },
    InCell {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            if (t.isEndTag()) {
                val endTag: Token.EndTag = t.asEndTag()
                val name: String = endTag.normalName()
                if (inSorted(name, Constants.InCellNames)) {
                    if (!tb.inTableScope(name)) {
                        tb.error(this)
                        tb.transition(InRow) // might not be in scope if empty: <td /> and processing fake end tag
                        return false
                    }
                    tb.generateImpliedEndTags()
                    if (!tb.currentElementIs(name)) tb.error(this)
                    tb.popStackToClose(name)
                    tb.clearFormattingElementsToLastMarker()
                    tb.transition(InRow)
                } else if (inSorted(name, Constants.InCellBody)) {
                    tb.error(this)
                    return false
                } else if (inSorted(name, Constants.InCellTable)) {
                    if (!tb.inTableScope(name)) {
                        tb.error(this)
                        return false
                    }
                    closeCell(tb)
                    return tb.process(t)
                } else {
                    return anythingElse(t, tb)
                }
            } else if (t.isStartTag() &&
                inSorted(t.asStartTag().normalName(), Constants.InCellCol)
            ) {
                if (!(tb.inTableScope("td") || tb.inTableScope("th"))) {
                    tb.error(this)
                    return false
                }
                closeCell(tb)
                return tb.process(t)
            } else {
                return anythingElse(t, tb)
            }
            return true
        }

        private fun anythingElse(t: Token, tb: HtmlTreeBuilder): Boolean {
            return tb.process(t, InBody)
        }

        private fun closeCell(tb: HtmlTreeBuilder) {
            if (tb.inTableScope("td")) tb.processEndTag("td") else tb.processEndTag("th") // only here if th or td in scope
        }
    },
    InSelect {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            when (t.type) {
                Token.TokenType.Character -> {
                    val c: Token.Character = t.asCharacter()
                    if (c.data == nullString) {
                        tb.error(this)
                        return false
                    } else {
                        tb.insert(c)
                    }
                }

                Token.TokenType.Comment -> tb.insert(t.asComment())
                Token.TokenType.Doctype -> {
                    tb.error(this)
                    return false
                }

                Token.TokenType.StartTag -> {
                    val start: Token.StartTag = t.asStartTag()
                    val name: String = start.normalName()
                    if (name == "html") {
                        return tb.process(
                            start,
                            InBody,
                        )
                    } else if (name == "option") {
                        if (tb.currentElementIs("option")) tb.processEndTag("option")
                        tb.insert(start)
                    } else if (name == "optgroup") {
                        if (tb.currentElementIs("option")) tb.processEndTag("option") // pop option and flow to pop optgroup
                        if (tb.currentElementIs("optgroup")) tb.processEndTag("optgroup")
                        tb.insert(start)
                    } else if (name == "select") {
                        tb.error(this)
                        return tb.processEndTag("select")
                    } else if (inSorted(name, Constants.InSelectEnd)) {
                        tb.error(this)
                        if (!tb.inSelectScope("select")) return false // frag
                        tb.processEndTag("select")
                        return tb.process(start)
                    } else {
                        return if (name == "script" || name == "template") {
                            tb.process(t, InHead)
                        } else {
                            anythingElse(t, tb)
                        }
                    }
                }

                Token.TokenType.EndTag -> {
                    val end: Token.EndTag = t.asEndTag()
                    val name = end.normalName()
                    when (name) {
                        "optgroup" -> {
                            if (tb.currentElementIs("option") &&
                                tb.aboveOnStack(tb.currentElement()) != null &&
                                tb.aboveOnStack(tb.currentElement())?.normalName() == "optgroup"
                            ) {
                                tb.processEndTag("option")
                            }
                            if (tb.currentElementIs("optgroup")) tb.pop() else tb.error(this)
                        }

                        "option" -> if (tb.currentElementIs("option")) tb.pop() else tb.error(this)
                        "select" -> if (!tb.inSelectScope(name)) {
                            tb.error(this)
                            return false
                        } else {
                            tb.popStackToClose(name)
                            tb.resetInsertionMode()
                        }

                        "template" -> return tb.process(t, InHead)
                        else -> return anythingElse(t, tb)
                    }
                }

                Token.TokenType.EOF -> if (!tb.currentElementIs("html")) tb.error(this)
                else -> return anythingElse(t, tb)
            }
            return true
        }

        private fun anythingElse(t: Token, tb: HtmlTreeBuilder): Boolean {
            tb.error(this)
            return false
        }
    },
    InSelectInTable {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            return if (t.isStartTag() && inSorted(
                    t.asStartTag().normalName(),
                    Constants.InSelectTableEnd,
                )
            ) {
                tb.error(this)
                tb.popStackToClose("select")
                tb.resetInsertionMode()
                tb.process(t)
            } else if (t.isEndTag() && inSorted(
                    t.asEndTag().normalName(),
                    Constants.InSelectTableEnd,
                )
            ) {
                tb.error(this)
                if (tb.inTableScope(t.asEndTag().normalName())) {
                    tb.popStackToClose("select")
                    tb.resetInsertionMode()
                    tb.process(t)
                } else {
                    false
                }
            } else {
                tb.process(t, InSelect)
            }
        }
    },
    InTemplate {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            val name: String
            when (t.type) {
                Token.TokenType.Character, Token.TokenType.Comment, Token.TokenType.Doctype -> tb.process(
                    t,
                    InBody,
                )

                Token.TokenType.StartTag -> {
                    name = t.asStartTag().normalName()
                    if (inSorted(name, Constants.InTemplateToHead)) {
                        tb.process(t, InHead)
                    } else if (inSorted(
                            name,
                            Constants.InTemplateToTable,
                        )
                    ) {
                        tb.popTemplateMode()
                        tb.pushTemplateMode(InTable)
                        tb.transition(InTable)
                        return tb.process(t)
                    } else if (name == "col") {
                        tb.popTemplateMode()
                        tb.pushTemplateMode(InColumnGroup)
                        tb.transition(InColumnGroup)
                        return tb.process(t)
                    } else if (name == "tr") {
                        tb.popTemplateMode()
                        tb.pushTemplateMode(InTableBody)
                        tb.transition(InTableBody)
                        return tb.process(t)
                    } else if (name == "td" || name == "th") {
                        tb.popTemplateMode()
                        tb.pushTemplateMode(InRow)
                        tb.transition(InRow)
                        return tb.process(t)
                    } else {
                        tb.popTemplateMode()
                        tb.pushTemplateMode(InBody)
                        tb.transition(InBody)
                        return tb.process(t)
                    }
                }

                Token.TokenType.EndTag -> {
                    name = t.asEndTag().normalName()
                    if (name == "template") {
                        tb.process(t, InHead)
                    } else {
                        tb.error(this)
                        return false
                    }
                }

                Token.TokenType.EOF -> {
                    if (!tb.onStack("template")) { // stop parsing
                        return true
                    }
                    tb.error(this)
                    tb.popStackToClose("template")
                    tb.clearFormattingElementsToLastMarker()
                    tb.popTemplateMode()
                    tb.resetInsertionMode()
                    // spec deviation - if we did not break out of Template, stop processing, and don't worry about cleaning up ultra-deep template stacks
                    // limited depth because this can recurse and will blow stack if too deep
                    return if (tb.state() !== InTemplate && tb.templateModeSize() < 12) {
                        tb.process(
                            t,
                        )
                    } else {
                        true
                    }
                }

                null -> {}
            }
            return true
        }
    },
    AfterBody {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            if (isWhitespace(t)) {
                // spec deviation - currently body is still on stack, but we want this to go to the html node
                val html: Element? = tb.getFromStack("html")
                if (html != null) {
                    tb.insert(t.asCharacter(), html)
                } else {
                    tb.process(
                        t,
                        InBody,
                    ) // will get into body
                }
            } else if (t.isComment()) {
                tb.insert(t.asComment()) // into html node
            } else if (t.isDoctype()) {
                tb.error(this)
                return false
            } else if (t.isStartTag() && t.asStartTag().normalName().equals("html")) {
                return tb.process(t, InBody)
            } else if (t.isEndTag() && t.asEndTag().normalName().equals("html")) {
                if (tb.isFragmentParsing) {
                    tb.error(this)
                    return false
                } else {
                    tb.transition(AfterAfterBody)
                }
            } else if (t.isEOF()) {
                // chillax! we're done
            } else {
                tb.error(this)
                tb.resetBody()
                return tb.process(t)
            }
            return true
        }
    },
    InFrameset {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            if (isWhitespace(t)) {
                tb.insert(t.asCharacter())
            } else if (t.isComment()) {
                tb.insert(t.asComment())
            } else if (t.isDoctype()) {
                tb.error(this)
                return false
            } else if (t.isStartTag()) {
                val start: Token.StartTag = t.asStartTag()
                when (start.normalName()) {
                    "html" -> return tb.process(start, InBody)
                    "frameset" -> tb.insert(start)
                    "frame" -> tb.insertEmpty(start)
                    "noframes" -> return tb.process(start, InHead)
                    else -> {
                        tb.error(this)
                        return false
                    }
                }
            } else if (t.isEndTag() && t.asEndTag().normalName().equals("frameset")) {
                if (tb.currentElementIs("html")) { // frag
                    tb.error(this)
                    return false
                } else {
                    tb.pop()
                    if (!tb.isFragmentParsing && !tb.currentElementIs("frameset")) {
                        tb.transition(AfterFrameset)
                    }
                }
            } else if (t.isEOF()) {
                if (!tb.currentElementIs("html")) {
                    tb.error(this)
                    return true
                }
            } else {
                tb.error(this)
                return false
            }
            return true
        }
    },
    AfterFrameset {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            if (isWhitespace(t)) {
                tb.insert(t.asCharacter())
            } else if (t.isComment()) {
                tb.insert(t.asComment())
            } else if (t.isDoctype()) {
                tb.error(this)
                return false
            } else if (t.isStartTag() && t.asStartTag().normalName().equals("html")) {
                return tb.process(t, InBody)
            } else if (t.isEndTag() && t.asEndTag().normalName().equals("html")) {
                tb.transition(AfterAfterFrameset)
            } else if (t.isStartTag() && t.asStartTag().normalName().equals("noframes")) {
                return tb.process(t, InHead)
            } else if (t.isEOF()) {
                // cool your heels, we're complete
            } else {
                tb.error(this)
                return false
            }
            return true
        }
    },
    AfterAfterBody {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            if (t.isComment()) {
                tb.insert(t.asComment())
            } else if (t.isDoctype() || t.isStartTag() && t.asStartTag().normalName()
                    .equals("html")
            ) {
                return tb.process(t, InBody)
            } else if (isWhitespace(t)) {
                // spec deviation - body and html still on stack, but want this space to go after </html>
                val doc: Element = tb.document
                tb.insert(t.asCharacter(), doc)
            } else if (t.isEOF()) {
                // nice work chuck
            } else {
                tb.error(this)
                tb.resetBody()
                return tb.process(t)
            }
            return true
        }
    },
    AfterAfterFrameset {
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            if (t.isComment()) {
                tb.insert(t.asComment())
            } else if (t.isDoctype() || isWhitespace(t) || t.isStartTag() && t.asStartTag()
                    .normalName().equals("html")
            ) {
                return tb.process(t, InBody)
            } else if (t.isEOF()) {
                // nice work chuck
            } else if (t.isStartTag() && t.asStartTag().normalName().equals("noframes")) {
                return tb.process(t, InHead)
            } else {
                tb.error(this)
                return false
            }
            return true
        }
    },
    ForeignContent {
        // https://html.spec.whatwg.org/multipage/parsing.html#parsing-main-inforeign
        override fun process(t: Token, tb: HtmlTreeBuilder): Boolean {
            when (t.type) {
                Token.TokenType.Character -> {
                    val c: Token.Character = t.asCharacter()
                    if (c.data.equals(nullString)) {
                        tb.error(this)
                    } else if (isWhitespace(c)) {
                        tb.insert(c)
                    } else {
                        tb.insert(c)
                        tb.framesetOk(false)
                    }
                }

                Token.TokenType.Comment -> tb.insert(t.asComment())
                Token.TokenType.Doctype -> tb.error(this)
                Token.TokenType.StartTag -> {
                    val start: Token.StartTag = t.asStartTag()
                    if (StringUtil.isIn(start.normalName!!, *Constants.InForeignToHtml)) {
                        return processAsHtml(t, tb)
                    }
                    if (start.normalName.equals("font") && (
                                start.hasAttributeIgnoreCase("color") ||
                                        start.hasAttributeIgnoreCase("face") ||
                                        start.hasAttributeIgnoreCase("size")
                                )
                    ) {
                        return processAsHtml(t, tb)
                    }

                    // Any other start:
                    // (whatwg says to fix up tag name and attribute case per a table - we will preserve original case instead)
                    tb.insertForeign(start, tb.currentElement().tag().namespace())
                }

                Token.TokenType.EndTag -> {
                    val end: Token.EndTag = t.asEndTag()
                    if (end.normalName.equals("br") || end.normalName.equals("p")) {
                        return processAsHtml(
                            t,
                            tb,
                        )
                    }
                    if (end.normalName.equals("script") && tb.currentElementIs(
                            "script",
                            Parser.NamespaceSvg,
                        )
                    ) {
                        // script level and execution elided.
                        tb.pop()
                        return true
                    }

                    // Any other end tag
                    val stack: ArrayList<Element> = arrayListOf(
                        *tb.stack.mapNotNull { it }.toList()
                            .toTypedArray(),
                    )
                    if (stack.isEmpty()) Validate.wtf("Stack unexpectedly empty")
                    var i: Int = stack.size - 1
                    var el: Element = stack[i]
                    if (el.normalName() != end.normalName) tb.error(this)
                    while (i != 0) {
                        if (el.normalName() == end.normalName) {
                            tb.popStackToCloseAnyNamespace(el.normalName())
                            return true
                        }
                        i--
                        el = stack[i]
                        if (el.tag().namespace() == Parser.NamespaceHtml) {
                            return processAsHtml(t, tb)
                        }
                    }
                }

                Token.TokenType.EOF -> {}
                null -> {}
            }
            return true
        }

        fun processAsHtml(t: Token, tb: HtmlTreeBuilder): Boolean {
            return tb.state()!!.process(t, tb)
        }
    },
    ;

    abstract fun process(t: Token, tb: HtmlTreeBuilder): Boolean

    // lists of tags to search through
    object Constants {
        val InHeadEmpty = arrayOf("base", "basefont", "bgsound", "command", "link")
        val InHeadRaw = arrayOf("noframes", "style")
        val InHeadEnd = arrayOf<String>("body", "br", "html")
        val AfterHeadBody = arrayOf("body", "br", "html")
        val BeforeHtmlToHead = arrayOf("body", "br", "head", "html")
        val InHeadNoScriptHead = arrayOf("basefont", "bgsound", "link", "meta", "noframes", "style")
        val InBodyStartToHead = arrayOf<String>(
            "base",
            "basefont",
            "bgsound",
            "command",
            "link",
            "meta",
            "noframes",
            "script",
            "style",
            "template",
            "title",
        )
        val InBodyStartPClosers = arrayOf<String>(
            "address", "article", "aside", "blockquote", "center", "details", "dir", "div", "dl",
            "fieldset", "figcaption", "figure", "footer", "header", "hgroup", "menu", "nav", "ol",
            "p", "section", "summary", "ul",
        )
        val Headings = arrayOf<String>("h1", "h2", "h3", "h4", "h5", "h6")
        val InBodyStartLiBreakers = arrayOf<String>("address", "div", "p")
        val DdDt = arrayOf<String>("dd", "dt")
        val InBodyStartApplets = arrayOf<String>("applet", "marquee", "object")
        val InBodyStartMedia = arrayOf<String>("param", "source", "track")
        val InBodyStartInputAttribs = arrayOf<String>("action", "name", "prompt")
        val InBodyStartDrop = arrayOf<String>(
            "caption",
            "col",
            "colgroup",
            "frame",
            "head",
            "tbody",
            "td",
            "tfoot",
            "th",
            "thead",
            "tr",
        )
        val InBodyEndClosers = arrayOf<String>(
            "address",
            "article",
            "aside",
            "blockquote",
            "button",
            "center",
            "details",
            "dir",
            "div",
            "dl",
            "fieldset",
            "figcaption",
            "figure",
            "footer",
            "header",
            "hgroup",
            "listing",
            "menu",
            "nav",
            "ol",
            "pre",
            "section",
            "summary",
            "ul",
        )
        val InBodyEndOtherErrors = arrayOf(
            "body",
            "dd",
            "dt",
            "html",
            "li",
            "optgroup",
            "option",
            "p",
            "rb",
            "rp",
            "rt",
            "rtc",
            "tbody",
            "td",
            "tfoot",
            "th",
            "thead",
            "tr",
        )
        val InBodyEndAdoptionFormatters = arrayOf<String>(
            "a",
            "b",
            "big",
            "code",
            "em",
            "font",
            "i",
            "nobr",
            "s",
            "small",
            "strike",
            "strong",
            "tt",
            "u",
        )
        val InBodyEndTableFosters = arrayOf<String>("table", "tbody", "tfoot", "thead", "tr")
        val InTableToBody = arrayOf("tbody", "tfoot", "thead")
        val InTableAddBody = arrayOf("td", "th", "tr")
        val InTableToHead = arrayOf("script", "style", "template")
        val InCellNames = arrayOf<String>("td", "th")
        val InCellBody = arrayOf<String>("body", "caption", "col", "colgroup", "html")
        val InCellTable = arrayOf<String>("table", "tbody", "tfoot", "thead", "tr")
        val InCellCol = arrayOf<String>(
            "caption",
            "col",
            "colgroup",
            "tbody",
            "td",
            "tfoot",
            "th",
            "thead",
            "tr",
        )
        val InTableEndErr = arrayOf(
            "body",
            "caption",
            "col",
            "colgroup",
            "html",
            "tbody",
            "td",
            "tfoot",
            "th",
            "thead",
            "tr",
        )
        val InTableFoster = arrayOf("table", "tbody", "tfoot", "thead", "tr")
        val InTableBodyExit = arrayOf("caption", "col", "colgroup", "tbody", "tfoot", "thead")
        val InTableBodyEndIgnore = arrayOf("body", "caption", "col", "colgroup", "html", "td", "th", "tr")
        val InRowMissing = arrayOf("caption", "col", "colgroup", "tbody", "tfoot", "thead", "tr")
        val InRowIgnore = arrayOf("body", "caption", "col", "colgroup", "html", "td", "th")
        val InSelectEnd = arrayOf("input", "keygen", "textarea")
        val InSelectTableEnd = arrayOf("caption", "table", "tbody", "td", "tfoot", "th", "thead", "tr")
        val InTableEndIgnore = arrayOf("tbody", "tfoot", "thead")
        val InHeadNoscriptIgnore = arrayOf("head", "noscript")
        val InCaptionIgnore = arrayOf("body", "col", "colgroup", "html", "tbody", "td", "tfoot", "th", "thead", "tr")
        val InTemplateToHead = arrayOf(
            "base",
            "basefont",
            "bgsound",
            "link",
            "meta",
            "noframes",
            "script",
            "style",
            "template",
            "title",
        )
        val InTemplateToTable = arrayOf("caption", "colgroup", "tbody", "tfoot", "thead")
        val InForeignToHtml = arrayOf(
            "b",
            "big",
            "blockquote",
            "body",
            "br",
            "center",
            "code",
            "dd",
            "div",
            "dl",
            "dt",
            "em",
            "embed",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "head",
            "hr",
            "i",
            "img",
            "li",
            "listing",
            "menu",
            "meta",
            "nobr",
            "ol",
            "p",
            "pre",
            "ruby",
            "s",
            "small",
            "span",
            "strike",
            "strong",
            "sub",
            "sup",
            "table",
            "tt",
            "u",
            "ul",
            "var",
        )
    }

    companion object {
        private const val nullString = '\u0000'.toString()
        private fun isWhitespace(t: Token): Boolean {
            if (t.isCharacter()) {
                val data: String = t.asCharacter().data!!
                return StringUtil.isBlank(data)
            }
            return false
        }

        private fun handleRcData(startTag: Token.StartTag, tb: HtmlTreeBuilder) {
            tb.tokeniser!!.transition(TokeniserState.Rcdata)
            tb.markInsertionMode()
            tb.transition(Text)
            tb.insert(startTag)
        }

        private fun handleRawtext(startTag: Token.StartTag, tb: HtmlTreeBuilder) {
            tb.tokeniser!!.transition(TokeniserState.Rawtext)
            tb.markInsertionMode()
            tb.transition(Text)
            tb.insert(startTag)
        }
    }
}
