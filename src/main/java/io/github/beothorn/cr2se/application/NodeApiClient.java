package io.github.beothorn.cr2se.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.beothorn.cr2se.protocol.*;
import io.github.beothorn.cr2se.transport.NodeTransport;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** High-level Node API facade. Service-specific behavior remains data-driven through invoke. */
public final class NodeApiClient implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NodeApiClient.class);
    private final NodeTransport transport;
    private final NodeApiCodec codec;
    private final AtomicLong sequence = new AtomicLong();
    public NodeApiClient(NodeTransport transport, NodeApiCodec codec) { this.transport = transport; this.codec = codec; }

    public JsonNode call(String operation, ObjectNode fields) throws IOException, NodeApiException {
        String id = Long.toString(sequence.incrementAndGet());
        ObjectNode request = fields.deepCopy(); request.put("id", id); request.put("operation", operation);
        LOG.debug("Calling operation {} with request id {}", operation, id);
        ObjectNode response = codec.decodeObject(transport.exchange(codec.encode(request)));
        JsonNode result = codec.resultFor(response, id);
        LOG.info("Operation {} completed", operation);
        return result;
    }
    public JsonNode open(String address, int port, String expectedPeerId) throws IOException, NodeApiException {
        ObjectNode f = codec.mapper().createObjectNode().put("address", address).put("port", port);
        if (expectedPeerId != null) f.put("expected_peer_id", expectedPeerId);
        return call("connection.open", f);
    }
    public JsonNode list() throws IOException, NodeApiException { return call("connections.list", codec.mapper().createObjectNode()); }
    public JsonNode connectionCommand(String operation, String id) throws IOException, NodeApiException { return call(operation, codec.mapper().createObjectNode().put("connection_id", id)); }
    public JsonNode offeringCommand(String operation, String connection, String offering) throws IOException, NodeApiException { return call(operation, codec.mapper().createObjectNode().put("connection_id", connection).put("offering_id", offering)); }
    @Override public void close() throws IOException { transport.close(); }
}
