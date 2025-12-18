package com.ninjaone.dundie_awards.exceptions;

public abstract class AbstractDundieRuntimeException extends RuntimeException {

    public AbstractDundieRuntimeException(String message) {
        super(message);
    }

    public AbstractDundieRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

}
