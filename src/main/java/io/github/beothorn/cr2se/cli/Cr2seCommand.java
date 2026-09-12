package io.github.beothorn.cr2se.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.beothorn.cr2se.application.NodeApiClient;
import io.github.beothorn.cr2se.protocol.*;
import io.github.beothorn.cr2se.transport.TcpJsonLinesTransport;
import java.io.*;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.*;

/** Command-line adapter only: protocol, application, and transport live in independent packages. */
@Command(name="cr2se", mixinStandardHelpOptions=true, version="cr2se-client 1.0.0",
    description="Portable command-line client for the CR2SE Node API.",
    subcommands={Cr2seCommand.Open.class, Cr2seCommand.ListConnections.class, Cr2seCommand.Close.class,
        Cr2seCommand.Ping.class, Cr2seCommand.Board.class, Cr2seCommand.Service.class,
        Cr2seCommand.Invoke.class, Cr2seCommand.Raw.class, Cr2seCommand.Batch.class})
public final class Cr2seCommand implements Runnable {
    @Option(names="--host", defaultValue="127.0.0.1", description="Loopback Node API host (default: ${DEFAULT-VALUE})") String host;
    @Option(names="--port", defaultValue="39471", description="Node API TCP port (default: ${DEFAULT-VALUE})") int port;
    @Option(names="--timeout", defaultValue="10000", description="Connect/read timeout in milliseconds") int timeout;
    @Option(names="--compact", description="Print compact rather than pretty JSON") boolean compact;

    @Override public void run() { CommandLine.usage(this, System.out); }
    NodeApiCodec codec() { return new NodeApiCodec(); }
    NodeApiClient connect(NodeApiCodec codec) throws IOException { return new NodeApiClient(new TcpJsonLinesTransport(host, port, timeout), codec); }
    void print(NodeApiCodec codec, JsonNode value) throws IOException {
        System.out.println(compact ? codec.mapper().writeValueAsString(value) : codec.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(value));
    }

    abstract static class ClientCommand implements Callable<Integer> {
        @ParentCommand Cr2seCommand parent;
        abstract JsonNode execute(NodeApiClient client, NodeApiCodec codec) throws Exception;
        @Override public Integer call() {
            NodeApiCodec codec = parent.codec();
            try (NodeApiClient client = parent.connect(codec)) { parent.print(codec, execute(client, codec)); return 0; }
            catch (NodeApiException e) {
                System.err.printf("CR2SE error [%s]: %s%n", e.code(), e.getMessage());
                if (e.details() != null) try { System.err.println(codec.mapper().writeValueAsString(e.details())); } catch (IOException ignored) { /* best-effort diagnostics */ }
                return 2;
            } catch (Exception e) { System.err.println("Client error: " + e.getMessage()); return 1; }
        }
    }

    @Command(name="connection-open", description="Connect the node to a remote CR2SE peer.") static final class Open extends ClientCommand {
        @Parameters(index="0", description="IPv4 or IPv6 address") String address;
        @Parameters(index="1", description="Peer port") int peerPort;
        @Option(names="--expected-peer-id", description="Require this authenticated CR2SE identity") String peerId;
        JsonNode execute(NodeApiClient c, NodeApiCodec ignored) throws Exception { return c.open(address, peerPort, peerId); }
    }
    @Command(name="connections-list", description="List connections maintained by the node.") static final class ListConnections extends ClientCommand {
        JsonNode execute(NodeApiClient c, NodeApiCodec ignored) throws Exception { return c.list(); }
    }
    abstract static class ConnectionCommand extends ClientCommand {
        @Parameters(index="0", description="Opaque connection ID") String connectionId;
    }
    @Command(name="connection-close", description="Close a CR2SE peer connection.") static final class Close extends ConnectionCommand {
        JsonNode execute(NodeApiClient c, NodeApiCodec ignored) throws Exception { return c.connectionCommand("connection.close", connectionId); }
    }
    @Command(name="connection-ping", description="Check whether a connected peer is responsive.") static final class Ping extends ConnectionCommand {
        JsonNode execute(NodeApiClient c, NodeApiCodec ignored) throws Exception { return c.connectionCommand("connection.ping", connectionId); }
    }
    @Command(name="board-get", description="Retrieve a connected peer's Board.") static final class Board extends ConnectionCommand {
        JsonNode execute(NodeApiClient c, NodeApiCodec ignored) throws Exception { return c.connectionCommand("board.get", connectionId); }
    }
    abstract static class OfferingCommand extends ConnectionCommand {
        @Parameters(index="1", description="Board offering ID") String offeringId;
    }
    @Command(name="service-get", description="Retrieve the full definition of a Board offering.") static final class Service extends OfferingCommand {
        JsonNode execute(NodeApiClient c, NodeApiCodec ignored) throws Exception { return c.offeringCommand("service.get", connectionId, offeringId); }
    }
    @Command(name="service-invoke", description="Invoke any advertised service; arguments are a JSON value.") static final class Invoke extends OfferingCommand {
        @Option(names="--service", required=true, description="Service name from the Board") String service;
        @Option(names="--service-version", required=true, description="Service version from the Board") int version;
        @Option(names="--arguments", required=true, description="JSON service arguments") String arguments;
        @Option(names="--maximum-price", description="Optional unsigned maximum authorized price") String maximumPrice;
        JsonNode execute(NodeApiClient c, NodeApiCodec codec) throws Exception {
            JsonNode args = codec.mapper().readTree(arguments);
            ObjectNode f = codec.mapper().createObjectNode().put("connection_id", connectionId).put("offering_id", offeringId)
                .put("service", service).put("service_version", version).set("arguments", args);
            if (maximumPrice != null) {
                if (!maximumPrice.matches("0|[1-9][0-9]*")) throw new IllegalArgumentException("--maximum-price must be an unsigned decimal integer");
                /* JSON integers are arbitrary precision, avoiding IEEE-754 loss for uint64 prices. */
                f.put("maximum_price", new java.math.BigInteger(maximumPrice));
            }
            return c.call("service.invoke", f);
        }
    }
    @Command(name="raw", description="Call an extension/future operation using a JSON object of fields.") static final class Raw extends ClientCommand {
        @Parameters(index="0", description="Node API operation") String operation;
        @Parameters(index="1", description="JSON object containing operation-specific fields") String fields;
        JsonNode execute(NodeApiClient c, NodeApiCodec codec) throws Exception { return c.call(operation, (ObjectNode) codec.decodeObject(fields)); }
    }
    @Command(name="batch", description="Read JSON requests from stdin, one per line, and print results; keeps one TCP session open.") static final class Batch extends ClientCommand {
        JsonNode execute(NodeApiClient c, NodeApiCodec codec) throws Exception {
            var results = codec.mapper().createArrayNode();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                for (String line; (line = reader.readLine()) != null;) {
                    if (line.isBlank()) continue;
                    ObjectNode input = codec.decodeObject(line);
                    JsonNode op = input.remove("operation");
                    input.remove("id"); // Client owns correlation IDs; caller IDs cannot collide.
                    if (op == null || !op.isTextual()) throw new IllegalArgumentException("Each batch object needs a string 'operation'");
                    results.add(c.call(op.textValue(), input));
                }
            }
            return results;
        }
    }
    public static void main(String[] args) { System.exit(new CommandLine(new Cr2seCommand()).execute(args)); }
}
