package io.github.beothorn.cr2se.cli;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import org.junit.jupiter.api.*;
import picocli.CommandLine;

class Cr2seCommandTest {
    @Test void helpListsEveryStandardOperationAndEscapeHatch() {
        var output=new StringWriter(); var cmd=new CommandLine(new Cr2seCommand()); cmd.setOut(new PrintWriter(output));
        assertEquals(0,cmd.execute("--help")); String h=output.toString();
        for(String name:new String[]{"connection-open","connections-list","connection-close","connection-ping","board-get","service-get","service-invoke","raw","batch"}) assertTrue(h.contains(name),name);
    }
    @Test void invokeRequiresServiceMetadata() { assertNotEquals(0,new CommandLine(new Cr2seCommand()).execute("service-invoke","abc","offer","--arguments","{}")); }
    @Test void versionIsAvailableOffline() { assertEquals(0,new CommandLine(new Cr2seCommand()).execute("--version")); }
}
