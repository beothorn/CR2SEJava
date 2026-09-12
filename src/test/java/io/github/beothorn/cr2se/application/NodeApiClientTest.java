package io.github.beothorn.cr2se.application;

import static org.junit.jupiter.api.Assertions.*;
import io.github.beothorn.cr2se.protocol.*;
import io.github.beothorn.cr2se.transport.NodeTransport;
import java.io.IOException;
import org.junit.jupiter.api.*;

class NodeApiClientTest {
    static final class RecordingTransport implements NodeTransport {
        String sent; boolean closed;
        public String exchange(String line) throws IOException { sent=line; return "{\"id\":\"1\",\"ok\":true,\"result\":{\"accepted\":true}}"; }
        public void close() { closed=true; }
    }
    @Test void openBuildsNormativeRequest() throws Exception {
        var t = new RecordingTransport(); var codec = new NodeApiCodec();
        try (var client = new NodeApiClient(t, codec)) { assertTrue(client.open("::1", 8042, "cr2se:peer").get("accepted").booleanValue()); }
        var json=codec.decodeObject(t.sent); assertEquals("connection.open", json.get("operation").textValue()); assertEquals("::1",json.get("address").textValue()); assertEquals(8042,json.get("port").intValue()); assertEquals("cr2se:peer",json.get("expected_peer_id").textValue()); assertTrue(t.closed);
    }
    @Test void IDsIncreaseAndFieldsCannotOverrideEnvelope() throws Exception {
        NodeApiCodec codec=new NodeApiCodec();
        NodeTransport t=new NodeTransport(){ int n; public String exchange(String line) throws IOException { n++; return "{\"id\":\""+n+"\",\"ok\":true,\"result\":{}}"; } public void close(){} };
        try(var c=new NodeApiClient(t,codec)){ var fields=codec.mapper().createObjectNode().put("id","evil").put("operation","evil"); c.call("board.get",fields); c.call("board.get",fields); }
    }
}
