package io.github.beothorn.cr2se.transport;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Persistent UTF-8, newline-delimited JSON connection to a loopback Node API. */
public final class TcpJsonLinesTransport implements NodeTransport {
    private static final Logger LOG = LoggerFactory.getLogger(TcpJsonLinesTransport.class);
    private final Socket socket;
    private final BufferedReader input;
    private final BufferedWriter output;

    public TcpJsonLinesTransport(String host, int port, int timeoutMillis) throws IOException {
        InetAddress address = InetAddress.getByName(host);
        if (!address.isLoopbackAddress()) {
            LOG.error("Refusing non-loopback Node API endpoint {}:{}", host, port);
            throw new IllegalArgumentException("Node API host must resolve to a loopback address (use --allow-remote to override is intentionally unsupported)");
        }
        LOG.info("Connecting to CR2SE Node API at {}:{}", host, port);
        socket = new Socket();
        socket.connect(new InetSocketAddress(address, port), timeoutMillis);
        socket.setSoTimeout(timeoutMillis);
        input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        LOG.debug("Node API TCP connection established");
    }
    @Override public synchronized String exchange(String jsonLine) throws IOException {
        if (jsonLine.indexOf('\n') >= 0 || jsonLine.indexOf('\r') >= 0) throw new IllegalArgumentException("JSON frame contains a literal newline");
        LOG.trace("Sending Node API frame: {}", jsonLine);
        output.write(jsonLine); output.write('\n'); output.flush();
        String response = input.readLine();
        if (response == null) { LOG.error("Node closed the connection before responding"); throw new EOFException("Node closed the connection before responding"); }
        LOG.trace("Received Node API frame: {}", response);
        return response;
    }
    @Override public void close() throws IOException { LOG.debug("Closing Node API connection"); socket.close(); }
}
