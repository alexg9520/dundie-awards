package com.ninjaone.dundie_awards.exceptions;

public class LookupException extends AbstractDundieException {

    public LookupException(String message) {
        super(message);
    }

    public LookupException(String message, Throwable cause) {
        super(message, cause);
    }

}
