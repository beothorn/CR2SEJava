package io.github.beothorn.cr2se.transport;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*; import java.net.*; import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

class TcpJsonLinesTransportTest {
    @Test void exchangesOneUtf8LineAndReusesSocket() throws Exception {
        try(ServerSocket server=new ServerSocket(0,1,InetAddress.getLoopbackAddress())) {
            var received=new CopyOnWriteArrayList<String>();
            var task=CompletableFuture.runAsync(()->{ try(Socket s=server.accept(); var r=new BufferedReader(new InputStreamReader(s.getInputStream(),StandardCharsets.UTF_8)); var w=new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),StandardCharsets.UTF_8))){ for(int i=0;i<2;i++){ received.add(r.readLine()); w.write("{\"reply\":"+i+"}\n"); w.flush(); }} catch(IOException e){throw new CompletionException(e);} });
            try(var transport=new TcpJsonLinesTransport("localhost",server.getLocalPort(),2000)){ assertEquals("{\"reply\":0}",transport.exchange("{\"text\":\"héllo\"}")); assertEquals("{\"reply\":1}",transport.exchange("{}")); }
            task.get(2,TimeUnit.SECONDS); assertEquals(java.util.List.of("{\"text\":\"héllo\"}","{}"),received);
        }
    }
    @Test void refusesRemoteEndpoints() { assertThrows(IllegalArgumentException.class,()->new TcpJsonLinesTransport("192.0.2.1",1234,1)); }
    @Test void refusesFramesWithLiteralNewline() throws Exception {
        try(ServerSocket server=new ServerSocket(0,1,InetAddress.getLoopbackAddress())) { var accepting=CompletableFuture.runAsync(()->{try(var ignored=server.accept()){}catch(IOException ignored){}}); try(var t=new TcpJsonLinesTransport("localhost",server.getLocalPort(),1000)){assertThrows(IllegalArgumentException.class,()->t.exchange("{}\n{}"));} accepting.get(); }
    }
}
