package io.objectbox.generator.model;

/**
 * Thrown when the model was configured incorrectly.
 */
public class ModelRuntimeException extends RuntimeException {

    public ModelRuntimeException(String message) {
        super(message);
    }
}
