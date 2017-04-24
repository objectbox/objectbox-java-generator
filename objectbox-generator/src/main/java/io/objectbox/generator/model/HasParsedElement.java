package io.objectbox.generator.model;

/**
 * A model parser can set any object for future reference;
 * e.g. a JDT parser can put AST nodes here to make source modifications later.
 */
public interface HasParsedElement {
    Object getParsedElement();

    void setParsedElement(Object parsedElement);
}
