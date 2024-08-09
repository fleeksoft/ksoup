package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.UncheckedIOException
import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.ported.StreamCharReader
import com.fleeksoft.ksoup.ported.toStreamCharReader
import com.fleeksoft.ksoup.select.Evaluator
import com.fleeksoft.ksoup.select.NodeVisitor
import com.fleeksoft.ksoup.select.QueryParser
import korlibs.datastructure.Queue
import korlibs.datastructure.toList
import korlibs.io.stream.openSync

/**
 * A StreamParser provides a progressive parse of its input. As each Element is completed, it is emitted via a Stream or
 * Iterator interface. Elements returned will be complete with all their children, and an (empty) next sibling, if
 * applicable.
 *
 * Elements (or their children) may be removed from the DOM during the parse, for e.g. to conserve memory, providing a
 * mechanism to parse an input document that would otherwise be too large to fit into memory, yet still providing a DOM
 * interface to the document and its elements.
 *
 *
 * Additionally, the parser provides a [.selectFirst] / [.selectNext], which will
 * run the parser until a hit is found, at which point the parse is suspended. It can be resumed via another
 * `select()` call, or via the [.stream] or [.iterator] methods.
 *
 *
 * Once the input has been fully read, the input Reader will be closed. Or, if the whole document does not need to be
 * read, call [.stop] and [.close].
 *
 * The [.document] method will return the Document being parsed into, which will be only partially complete
 * until the input is fully consumed.
 *
 * A StreamParser can be reused via a new [.parse], but is not thread-safe for concurrent inputs.
 * New parsers should be used in each thread.
 *
 * If created via [Connection.Response.streamParser], or another Reader that is I/O backed, the iterator and
 * stream consumers will throw an [java.io.UncheckedIOException] if the underlying Reader errors during read.
 *
 * The StreamParser interface is currently in **beta** and may change in subsequent releases. Feedback on the
 * feature and how you're using it is very welcome
 */
class StreamParser(private val parser: Parser) {
    private val treeBuilder = parser.getTreeBuilder()
    private val elementIterator: ElementIterator = ElementIterator()

    private var document: Document? = null
    private var stopped = false

    /**
     * Construct a new StreamParser, using the supplied base Parser.
     * @param parser the configured base parser
     */
    init {
        treeBuilder.nodeListener(elementIterator)
    }

    /**
     * Provide the input for a Document parse. The input is not read until a consuming operation is called.
     * @param input the input to be read.
     * @param baseUri the URL of this input, for absolute link resolution
     * @return this parser, for chaining
     */
    fun parse(input: StreamCharReader, baseUri: String): StreamParser {
        close() // probably a no-op, but ensures any previous reader is closed
        elementIterator.reset()
        treeBuilder.initialiseParse(input, baseUri, parser) // reader is not read, so no chance of IO error
        document = treeBuilder.doc
        return this
    }

    /**
     * Provide the input for a Document parse. The input is not read until a consuming operation is called.
     * @param input the input to be read
     * @param baseUri the URL of this input, for absolute link resolution
     * @return this parser
     */
    fun parse(input: String, baseUri: String): StreamParser {
        return parse(input.openSync().toStreamCharReader(), baseUri)
    }

    /**
     * Provide the input for a fragment parse. The input is not read until a consuming operation is called.
     * @param input the input to be read
     * @param context the optional fragment context element
     * @param baseUri the URL of this input, for absolute link resolution
     * @return this parser
     * @see .completeFragment
     */
    fun parseFragment(input: StreamCharReader, context: Element?, baseUri: String): StreamParser {
        parse(input, baseUri)
        treeBuilder.initialiseParseFragment(context)
        return this
    }

    /**
     * Provide the input for a fragment parse. The input is not read until a consuming operation is called.
     * @param input the input to be read
     * @param context the optional fragment context element
     * @param baseUri the URL of this input, for absolute link resolution
     * @return this parser
     * @see .completeFragment
     */
    fun parseFragment(input: String, context: Element?, baseUri: String): StreamParser {
        return parseFragment(input.openSync().toStreamCharReader(), context, baseUri)
    }

    /**
     * Creates a [Stream] of [Element]s, with the input being parsed as each element is consumed. Each
     * Element returned will be complete (that is, all of its children will be included, and if it has a next sibling, that
     * (empty) sibling will exist at [Element.nextElementSibling]). The stream will be emitted in document order as
     * each element is closed. That means that child elements will be returned prior to their parents.
     *
     * The stream will start from the current position of the backing iterator and the parse.
     *
     * When consuming the stream, if the Reader that the Parser is reading throws an I/O exception (for example a
     * SocketTimeoutException), that will be emitted as an [UncheckedIOException]
     * @return a stream of Element objects
     * @throws UncheckedIOException if the underlying Reader excepts during a read (in stream consuming methods)
     */
    fun stream(): Sequence<Element> {
        return elementIterator.asSequence()
    }

    /**
     * Returns an [Iterator] of [Element]s, with the input being parsed as each element is consumed. Each
     * Element returned will be complete (that is, all of its children will be included, and if it has a next sibling, that
     * (empty) sibling will exist at [Element.nextElementSibling]). The elements will be emitted in document order as
     * each element is closed. That means that child elements will be returned prior to their parents.
     *
     * The iterator will start from the current position of the parse.
     *
     * The iterator is backed by this StreamParser, and the resources it holds.
     * @return a stream of Element objects
     */
    fun iterator(): Iterator<Element?> {
        return elementIterator
    }

    /**
     * Flags that the parse should be stopped; the backing iterator will not return any more Elements.
     * @return this parser
     */
    fun stop(): StreamParser {
        stopped = true
        return this
    }

    /**
     * Closes the input and releases resources including the underlying parser and reader.
     *
     * The parser will also be closed when the input is fully read.
     *
     * The parser can be reused with another call to [.parse].
     */
    fun close() {
        treeBuilder.completeParse() // closes the reader, frees resources
    }


    fun use(receiver: (StreamParser) -> Unit) {
        receiver(this)
        close()
    }

    /**
     * Get the current [Document] as it is being parsed. It will be only partially complete until the input is fully
     * read. Structural changes (e.g. insert, remove) may be made to the Document contents.
     * @return the (partial) Document
     */
    fun document(): Document {
        document = treeBuilder.doc

        requireNotNull(document) { "Must run parse() before calling." }
        return document!!
    }

    /**
     * Runs the parser until the input is fully read, and returns the completed Document.
     * @return the completed Document
     * @throws IOException if an I/O error occurs
     */
    fun complete(): Document {
        val doc: Document = document()
        treeBuilder.runParser()
        return doc
    }

    /**
     * When initialized as a fragment parse, runs the parser until the input is fully read, and returns the completed
     * fragment child nodes.
     * @return the completed child nodes
     * @throws IOException if an I/O error occurs
     * @see .parseFragment
     */
    fun completeFragment(): List<Node> {
        treeBuilder.runParser()
        return treeBuilder.completeParseFragment()
    }

    /**
     * Finds the first Element that matches the provided query. If the parsed Document does not already have a match, the
     * input will be parsed until the first match is found, or the input is completely read.
     * @param query the [Selector] query.
     * @return the first matching [Element], or `null` if there's no match
     * @throws IOException if an I/O error occurs
     */
    fun selectFirst(query: String): Element? {
        return selectFirst(QueryParser.parse(query))
    }

    /**
     * Just like [.selectFirst], but if there is no match, throws an [IllegalArgumentException]. This
     * is useful if you want to simply abort processing on a failed match.
     * @param query the [Selector] query.
     * @return the first matching element
     * @throws IllegalArgumentException if no match is found
     * @throws IOException if an I/O error occurs
     */
    fun expectFirst(query: String): Element {
        return Validate.ensureNotNull(
            selectFirst(query),
            "No elements matched the query '$query' in the document."
        ) as Element
    }

    /**
     * Finds the first Element that matches the provided query. If the parsed Document does not already have a match, the
     * input will be parsed until the first match is found, or the input is completely read.
     * @param eval the [Selector] evaluator.
     * @return the first matching [Element], or `null` if there's no match
     * @throws IOException if an I/O error occurs
     */
    fun selectFirst(eval: Evaluator): Element? {
        val doc: Document = document()

        // run the query on the existing (partial) doc first, as there may be a hit already parsed
        val first: Element? = doc.selectFirst(eval)
        if (first != null) return first

        return selectNext(eval)
    }

    /**
     * Finds the next Element that matches the provided query. The input will be parsed until the next match is found, or
     * the input is completely read.
     * @param query the [Selector] query.
     * @return the next matching [Element], or `null` if there's no match
     * @throws IOException if an I/O error occurs
     */
    fun selectNext(query: String): Element? {
        return selectNext(QueryParser.parse(query))
    }

    /**
     * Just like [.selectFirst], but if there is no match, throws an [IllegalArgumentException]. This
     * is useful if you want to simply abort processing on a failed match.
     * @param query the [Selector] query.
     * @return the first matching element
     * @throws IllegalArgumentException if no match is found
     * @throws IOException if an I/O error occurs
     */
    fun expectNext(query: String): Element {
        return Validate.ensureNotNull(
            selectNext(query),
            "No elements matched the query '$query' in the document."
        ) as Element
    }

    /**
     * Finds the next Element that matches the provided query. The input will be parsed until the next match is found, or
     * the input is completely read.
     * @param eval the [Selector] evaluator.
     * @return the next matching [Element], or `null` if there's no match
     * @throws IOException if an I/O error occurs
     */
    fun selectNext(eval: Evaluator): Element? {
        val doc: Document = document() // validates the parse was initialized, keeps stack trace out of stream
        return stream().firstOrNull(eval.asPredicate(doc))
    }

    internal inner class ElementIterator : MutableIterator<Element>, NodeVisitor {
        // listeners add to a next emit queue, as a single token read step may yield multiple elements
        private val emitQueue: Queue<Element> = Queue()

        private var current: Element? = null // most recently emitted
        private var next: Element? = null // element waiting to be picked up
        private var tail: Element? = null // The last tailed element (</html>), on hold for final pop

        fun reset() {
            emitQueue.clear()
            tail = null
            next = tail
            current = next
            stopped = false
        }

        // Iterator Interface:
        /**
         * {@inheritDoc}
         * @throws UncheckedIOException if the underlying Reader errors during a read
         */
        override fun hasNext(): Boolean {
            maybeFindNext()
            return next != null
        }

        /**
         * {@inheritDoc}
         * @throws UncheckedIOException if the underlying Reader errors during a read
         */
        override fun next(): Element {
            maybeFindNext()
            if (next == null) throw NoSuchElementException()
            current = next
            next = null
            return current!!
        }

        private fun maybeFindNext() {
            if (stopped || next != null) return

            // drain the current queue before stepping to get more
            if (!emitQueue.isEmpty()) {
                next = emitQueue.dequeue()
                return
            }

            // step the parser, which will hit the node listeners to add to the queue:
            while (treeBuilder.stepParser()) {
                if (!emitQueue.isEmpty()) {
                    next = emitQueue.dequeue()
                    return
                }
            }
            stop()
            close()

            // send the final element out:
            if (tail != null) {
                next = tail
                tail = null
            }
        }

        override fun remove() {
            if (current == null) throw NoSuchElementException()
            current?.remove()
        }

        // NodeVisitor Interface:
        override fun head(node: Node, depth: Int) {
            if (node is Element) {
                val prev: Element? = (node as Element).previousElementSibling()
                // We prefer to wait until an element has a next sibling before emitting it; otherwise, get it in tail
                if (prev != null) emitQueue.enqueue(prev)
            }
        }

        override fun tail(node: Node, depth: Int) {
            if (node is Element) {
                tail = node as Element? // kept for final hit
                val lastChild: Element? = tail?.lastElementChild() // won't get a nextsib, so emit that:
                if (lastChild != null) emitQueue.enqueue(lastChild)
            }
        }
    }
}
