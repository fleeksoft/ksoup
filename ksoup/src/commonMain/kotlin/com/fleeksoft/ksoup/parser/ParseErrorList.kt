package com.fleeksoft.ksoup.parser

/**
 * A container for ParseErrors.
 *
 * @author Sabeeh
 */
public class ParseErrorList private constructor(
    private val initialCapacity: Int,
    public val maxSize: Int,
) : MutableList<ParseError> by mutableListOf() {
    // FIXME: initialCapacity ignored
    /**
     * Create a new ParseErrorList with the same settings, but no errors in the list
     * @param copy initial and max size details to copy
     */
    internal constructor(copy: ParseErrorList) : this(copy.initialCapacity, copy.maxSize)

    public fun canAddError(): Boolean {
        return size < maxSize
    }

    internal fun clone(): ParseErrorList {
        // As there's no direct `clone()` in Kotlin's MutableList,
        // we need to manually create a new instance and add all items.
        return ParseErrorList(initialCapacity, maxSize).also { it.addAll(this) }
    }

    public companion object {
        private const val INITIAL_CAPACITY = 16

        public fun noTracking(): ParseErrorList {
            return ParseErrorList(0, 0)
        }

        public fun tracking(maxSize: Int): ParseErrorList {
            return ParseErrorList(INITIAL_CAPACITY, maxSize)
        }
    }
}
