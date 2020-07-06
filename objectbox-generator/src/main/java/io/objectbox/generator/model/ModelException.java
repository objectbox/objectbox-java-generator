package io.objectbox.generator.model;

/**
 * Thrown if there was an issue configuring the model.
 */
public class ModelException extends Exception {

    ModelException(String message) {
        super(message);
    }
}
