package io.github.beothorn.cr2se.transport;

import java.io.IOException;

/** Replaceable transport boundary, enabling deterministic application tests. */
public interface NodeTransport extends AutoCloseable {
    String exchange(String jsonLine) throws IOException;
    @Override void close() throws IOException;
}
