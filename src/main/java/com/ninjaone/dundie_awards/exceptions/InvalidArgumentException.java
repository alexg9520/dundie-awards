package com.ninjaone.dundie_awards.exceptions;

/**
 * Exception thrown when an invalid or null argument is provided to a method.
 */
public class InvalidArgumentException extends AbstractDundieException {

    public InvalidArgumentException(String message) {
        super(message);
    }

    public InvalidArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

}
