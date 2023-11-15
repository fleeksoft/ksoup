package com.fleeksoft.ksoup.parser

/**
 * A container for ParseErrors.
 *
 * @author Sabeeh
 */
class ParseErrorList private constructor(
    private val initialCapacity: Int,
    val maxSize: Int,
) : MutableList<ParseError> by mutableListOf() {
    // FIXME: initialCapacity ignored
    /**
     * Create a new ParseErrorList with the same settings, but no errors in the list
     * @param copy initial and max size details to copy
     */
    internal constructor(copy: ParseErrorList) : this(copy.initialCapacity, copy.maxSize)

    fun canAddError(): Boolean {
        return size < maxSize
    }

    fun clone(): ParseErrorList {
        // As there's no direct `clone()` in Kotlin's MutableList,
        // we need to manually create a new instance and add all items.
        return ParseErrorList(initialCapacity, maxSize).also { it.addAll(this) }
    }

    companion object {
        private const val INITIAL_CAPACITY = 16

        fun noTracking(): ParseErrorList {
            return ParseErrorList(0, 0)
        }

        fun tracking(maxSize: Int): ParseErrorList {
            return ParseErrorList(INITIAL_CAPACITY, maxSize)
        }
    }
}
