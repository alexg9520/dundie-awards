package com.ninjaone.dundie_awards.exceptions;

public abstract class AbstractDundieException extends Exception {

    public AbstractDundieException(String message) {
        super(message);
    }

    public AbstractDundieException(String message, Throwable cause) {
        super(message, cause);
    }

}
