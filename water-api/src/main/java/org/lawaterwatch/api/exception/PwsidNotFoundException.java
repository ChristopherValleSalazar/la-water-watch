package org.lawaterwatch.api.exception;

//Thrown by any lookup of a water system by PWSID that finds nothing, the GlobalExceptionHandler turns it into a 404
public class PwsidNotFoundException extends RuntimeException {

    private final String pwsid;

    public PwsidNotFoundException(String pwsid) {
        super("No water system with PWSID '" + pwsid + "'. GET /api/systems/leaderboard lists the valid ones");
        this.pwsid = pwsid;
    }

    public String getPwsid() {
        return pwsid;
    }
}
