package io.objectbox.generator.model;

/**
 * Thrown when there is a configuration issue with the model that can be resolved by the user.
 */
public class ModelException extends Exception {

    ModelException(String message) {
        super(message);
    }
}
