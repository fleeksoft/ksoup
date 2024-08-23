package com.fleeksoft.ksoup.ported.exception

/**
 * A SerializationException is raised whenever serialization of a DOM element fails. This exception usually wraps an
 * [IOException] that may be thrown due to an inaccessible output stream.
 */
public class SerializationException : RuntimeException {
    /**
     * Creates and initializes a new serialization exception with no error message and cause.
     */
    public constructor() : super()

    /**
     * Creates and initializes a new serialization exception with the given error message and no cause.
     *
     * @param message
     * the error message of the new serialization exception (may be `null`).
     */
    public constructor(message: String?) : super(message)

    /**
     * Creates and initializes a new serialization exception with the specified cause and an error message of
     * `(cause==null ? null : cause.toString())` (which typically contains the class and error message of
     * `cause`).
     *
     * @param cause
     * the cause of the new serialization exception (may be `null`).
     */
    public constructor(cause: Throwable?) : super(cause)

    /**
     * Creates and initializes a new serialization exception with the given error message and cause.
     *
     * @param message
     * the error message of the new serialization exception.
     * @param cause
     * the cause of the new serialization exception.
     */
    public constructor(message: String?, cause: Throwable?) : super(message, cause)
}
