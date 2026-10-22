package org.lawaterwatch.api.client;

//Small helpers shared by the two ArcGIS clients
final class ArcGis {

    private static final int SNIPPET_LENGTH = 300;

    private ArcGis() {}

    //Start of a response body for error messages, enough to show ArcGIS's "error" object
    static String snippet(String body) {
        if (body == null) {
            return "<empty body>";
        }
        return body.length() <= SNIPPET_LENGTH ? body : body.substring(0, SNIPPET_LENGTH) + "...";
    }
}
