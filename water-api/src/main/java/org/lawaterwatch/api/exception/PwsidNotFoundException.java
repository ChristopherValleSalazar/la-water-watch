package org.lawaterwatch.api.exception;

public class PwsidNotFoundException extends RuntimeException {
    public PwsidNotFoundException(String message) {
        super(message);
    }
}
