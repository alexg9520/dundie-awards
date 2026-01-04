package com.ninjaone.dundie_awards.exceptions;

public class InvalidArgumentException extends AbstractDundieException {

    public InvalidArgumentException(String message) {
        super(message);
    }

    public InvalidArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

}
