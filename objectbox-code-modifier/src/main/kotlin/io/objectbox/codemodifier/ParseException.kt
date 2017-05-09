package io.objectbox.codemodifier

/** Thrown during parsing when an invalid input is found that should be fixed by the user. */
class ParseException : RuntimeException {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}
