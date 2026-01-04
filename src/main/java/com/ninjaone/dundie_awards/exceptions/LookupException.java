package com.ninjaone.dundie_awards.exceptions;

/**
 * Exception thrown when a lookup operation fails, such as when an entity is not found.
 */
public class LookupException extends AbstractDundieException {

    public LookupException(String message) {
        super(message);
    }

    public LookupException(String message, Throwable cause) {
        super(message, cause);
    }

}
