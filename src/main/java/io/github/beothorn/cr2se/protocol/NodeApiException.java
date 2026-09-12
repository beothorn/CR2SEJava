package io.github.beothorn.cr2se.protocol;

import com.fasterxml.jackson.databind.JsonNode;

/** A structured error returned by the CR2SE Node API. */
public final class NodeApiException extends Exception {
    private final String code;
    private final JsonNode details;

    public NodeApiException(String code, String message, JsonNode details) {
        super(message);
        this.code = code;
        this.details = details;
    }
    public String code() { return code; }
    public JsonNode details() { return details; }
}
