package io.github.beothorn.cr2se.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;

/** Encodes and validates the language-independent JSON-lines Node API envelope. */
public final class NodeApiCodec {
    private final ObjectMapper mapper;
    public NodeApiCodec() {
        mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    }
    public ObjectMapper mapper() { return mapper; }
    public String encode(ObjectNode request) throws JsonProcessingException { return mapper.writeValueAsString(request); }
    public ObjectNode decodeObject(String line) throws JsonProcessingException {
        JsonNode value = mapper.readTree(line);
        if (!(value instanceof ObjectNode object)) throw new JsonProcessingException("Node API message must be a JSON object") {};
        return object;
    }
    public JsonNode resultFor(ObjectNode response, String expectedId) throws NodeApiException {
        JsonNode id = response.get("id");
        if (id == null || !id.isTextual() || !Objects.equals(id.textValue(), expectedId))
            throw new NodeApiException("response_id_mismatch", "Response ID does not match request ID", response);
        JsonNode ok = response.get("ok");
        if (ok == null || !ok.isBoolean()) throw new NodeApiException("invalid_response", "Response has no boolean 'ok'", response);
        if (ok.booleanValue()) {
            if (!response.has("result")) throw new NodeApiException("invalid_response", "Successful response has no 'result'", response);
            return response.get("result");
        }
        JsonNode error = response.get("error");
        if (error == null || !error.isObject()) throw new NodeApiException("invalid_response", "Error response has no 'error' object", response);
        String code = error.path("code").asText("unknown_error");
        String message = error.path("message").asText("The node returned an error");
        throw new NodeApiException(code, message, error.get("details"));
    }
}
