package io.objectbox.processor;

import com.google.auto.service.AutoService;

import javax.annotation.processing.Processor;

/** Use shim as AutoService does not pick up a processor written in Kotlin */
@AutoService(Processor.class)
public final class ObjectBoxProcessorShim extends ObjectBoxProcessor {
}
