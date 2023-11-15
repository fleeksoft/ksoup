package com.fleeksoft.ksoup.ported

public fun interface Consumer<T> {
    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     */
    public fun accept(t: T)

    /**
     * Returns a composed `Consumer` that performs, in sequence, this
     * operation followed by the `after` operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation.  If performing this operation throws an exception,
     * the `after` operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed `Consumer` that performs in sequence this
     * operation followed by the `after` operation
     * @throws NullPointerException if `after` is null
     */
    public fun andThen(after: Consumer<in T>): Consumer<T>? {
        return Consumer { t: T ->
            accept(t)
            after.accept(t)
        }
    }
}
