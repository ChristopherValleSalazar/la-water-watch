package org.lawaterwatch.api.service;

public class PwsidNotFoundException extends RuntimeException {

    public PwsidNotFoundException(String pwsid) {
        super("pwsid not found: " + pwsid);
    }
}
