package io.objectbox.gradle.transform

class TransformException : RuntimeException {
    constructor(msg: String) : super(msg)
    constructor(msg: String, th: Throwable) : super(msg, th)
}
