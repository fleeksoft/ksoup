package com.fleeksoft.ksoup.internal

import com.fleeksoft.io.exception.IOException
import com.fleeksoft.ksoup.exception.SerializationException


/**
 * A Ksoup internal class to wrap an Appendable and throw IOExceptions as SerializationExceptions.
 *
 * Only implements the appendable methods we actually use.
 */
abstract class QuietAppendable {
    abstract fun append(csq: CharSequence?): QuietAppendable

    abstract fun append(c: Char): QuietAppendable

    abstract fun append(chars: CharArray, offset: Int, len: Int): QuietAppendable // via StringBuilder, not Appendable

    class BaseAppendable(private val a: Appendable) : QuietAppendable() {

        private fun interface Action {
            fun append()
        }

        private fun quiet(action: Action): BaseAppendable {
            try {
                action.append()
            } catch (e: IOException) {
                throw SerializationException(e)
            }
            return this
        }

        override fun append(csq: CharSequence?): BaseAppendable {
            return quiet { a.append(csq) }
        }

        override fun append(c: Char): BaseAppendable {
            return quiet { a.append(c) }
        }

        override fun append(chars: CharArray, offset: Int, len: Int): QuietAppendable {
            return quiet { a.append(chars.concatToString(offset, offset + len)) }
        }
    }

    /** A version that wraps a StringBuilder, and so doesn't need the exception wrap.  */
    class StringBuilderAppendable(private val sb: StringBuilder) : QuietAppendable() {

        override fun append(csq: CharSequence?): StringBuilderAppendable {
            sb.append(csq)
            return this
        }

        override fun append(c: Char): StringBuilderAppendable {
            sb.append(c)
            return this
        }

        override fun append(chars: CharArray, offset: Int, len: Int): QuietAppendable {
            sb.appendRange(chars, offset, offset + len)
            return this
        }

        override fun toString(): String {
            return sb.toString()
        }
    }

    companion object {
        fun wrap(a: Appendable): QuietAppendable {
            return if (a is StringBuilder) StringBuilderAppendable(a)
            else BaseAppendable(a)
        }
    }
}
