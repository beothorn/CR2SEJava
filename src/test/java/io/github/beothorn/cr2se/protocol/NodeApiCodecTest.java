package io.github.beothorn.cr2se.protocol;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

class NodeApiCodecTest {
    private final NodeApiCodec codec = new NodeApiCodec();
    @Test void extractsSuccessfulResult() throws Exception { assertEquals(42, codec.resultFor(codec.decodeObject("{\"id\":\"7\",\"ok\":true,\"result\":42}"), "7").intValue()); }
    @Test void preservesStructuredNodeErrors() throws Exception {
        var e = assertThrows(NodeApiException.class, () -> codec.resultFor(codec.decodeObject("{\"id\":\"7\",\"ok\":false,\"error\":{\"code\":\"no_credit\",\"message\":\"Insufficient credit\",\"details\":{\"needed\":2}}}"), "7"));
        assertEquals("no_credit", e.code()); assertEquals("Insufficient credit", e.getMessage()); assertEquals(2, e.details().get("needed").intValue());
    }
    @Test void rejectsMismatchedId() throws Exception { assertEquals("response_id_mismatch", assertThrows(NodeApiException.class, () -> codec.resultFor(codec.decodeObject("{\"id\":\"8\",\"ok\":true,\"result\":{}}"), "7")).code()); }
    @Test void rejectsNonObject() { assertThrows(Exception.class, () -> codec.decodeObject("[]")); }
    @Test void rejectsDuplicateKeys() { assertThrows(Exception.class, () -> codec.decodeObject("{\"ok\":true,\"ok\":false}")); }
    @Test void rejectsSuccessWithoutResult() throws Exception { assertEquals("invalid_response", assertThrows(NodeApiException.class, () -> codec.resultFor(codec.decodeObject("{\"id\":\"1\",\"ok\":true}"), "1")).code()); }
    @Test void acceptsUnknownResponseFields() throws Exception { assertTrue(codec.resultFor(codec.decodeObject("{\"id\":\"1\",\"ok\":true,\"result\":{},\"future\":99}"), "1").isObject()); }
}
