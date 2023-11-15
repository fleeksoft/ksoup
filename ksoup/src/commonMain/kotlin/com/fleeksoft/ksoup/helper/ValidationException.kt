package com.fleeksoft.ksoup.helper

/**
 * Validation exceptions, as thrown by the methods in [Validate].
 */
class ValidationException(msg: String?) : IllegalArgumentException(msg) {
    // TODO: incomplete
    /*@Synchronized
    overrie fun fillInStackTrace(): Throwable {
        // Filters out the Validate class from the stacktrace, to more clearly point at the root-cause.
        super.fillInStackTrace()
        val stackTrace: Array<StackTraceElement> = getStackTrace()
        val filteredTrace: MutableList<java.lang.StackTraceElement> =
            ArrayList<java.lang.StackTraceElement>()
        for (trace in stackTrace) {
            if (trace.getClassName() == Validator) continue
            filteredTrace.add(trace)
        }
        setStackTrace(filteredTrace.toTypedArray<java.lang.StackTraceElement>())
        return this
    }*/

    /*companion object {
        val Validator: String = Validate::class.java.getName()
    }*/
}
