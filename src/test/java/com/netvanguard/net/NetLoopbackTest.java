package com.netvanguard.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Runs a Host and a Client inside the same program, over the loopback address 127.0.0.1. */
class NetLoopbackTest {

    /** Collects everything a listener is told, so the test can wait for it. */
    static class Recorder implements NetListener {
        final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        final BlockingQueue<String> errors = new LinkedBlockingQueue<>();
        final CountDownLatch connected = new CountDownLatch(1);
        final CountDownLatch disconnected = new CountDownLatch(1);

        @Override
        public void onConnected() {
            connected.countDown();
        }

        @Override
        public void onMessage(String line) {
            messages.add(line);
        }

        @Override
        public void onDisconnected() {
            disconnected.countDown();
        }

        @Override
        public void onError(String message) {
            errors.add(message);
        }
    }

    @Test
    void messagesFlowBothWaysAndDisconnectIsReported() throws Exception {
        Recorder hostSide = new Recorder();
        Recorder clientSide = new Recorder();

        Host host = new Host(0, hostSide); // port 0 = pick any free port
        host.start();
        Client client = new Client("127.0.0.1", host.getPort(), clientSide);
        client.connect();

        assertTrue(hostSide.connected.await(5, TimeUnit.SECONDS), "host should see the client join");
        assertTrue(clientSide.connected.await(5, TimeUnit.SECONDS), "client should connect");
        assertTrue(host.isConnected());
        assertTrue(client.isConnected());

        client.send(Protocol.garbage(3));
        assertEquals("GARBAGE 3", hostSide.messages.poll(5, TimeUnit.SECONDS));

        host.send(Protocol.score(1200));
        assertEquals("SCORE 1200", clientSide.messages.poll(5, TimeUnit.SECONDS));

        // many messages in a row must arrive complete and in order
        for (int i = 0; i < 50; i++) {
            host.send("SCORE " + i);
        }
        for (int i = 0; i < 50; i++) {
            assertEquals("SCORE " + i, clientSide.messages.poll(5, TimeUnit.SECONDS));
        }

        client.close();
        assertTrue(hostSide.disconnected.await(5, TimeUnit.SECONDS), "host should notice the client leaving");
        assertTrue(clientSide.disconnected.await(5, TimeUnit.SECONDS));
        assertFalse(client.isConnected());
        host.close();
    }

    @Test
    void connectingToNothingReportsAnError() throws Exception {
        Recorder side = new Recorder();
        Client client = new Client("127.0.0.1", 1, side); // nothing listens on port 1
        client.connect();
        assertNotNull(side.errors.poll(10, TimeUnit.SECONDS), "an error should be reported");
        assertFalse(client.isConnected());
    }

    @Test
    void newlinesInsideAMessageDoNotSplitIt() throws Exception {
        Recorder hostSide = new Recorder();
        Recorder clientSide = new Recorder();
        Host host = new Host(0, hostSide);
        host.start();
        Client client = new Client("127.0.0.1", host.getPort(), clientSide);
        client.connect();
        assertTrue(clientSide.connected.await(5, TimeUnit.SECONDS));

        client.send("HELLO\nWORLD");
        assertEquals("HELLO WORLD", hostSide.messages.poll(5, TimeUnit.SECONDS));

        client.close();
        host.close();
    }

    @Test
    void relayHoldsEventsUntilATargetIsSetAndKeepsOrder() {
        RelayListener relay = new RelayListener();
        relay.onMessage("A");
        relay.onMessage("B");

        Recorder receiver = new Recorder();
        relay.setTarget(receiver);
        assertEquals("A", receiver.messages.poll());
        assertEquals("B", receiver.messages.poll());

        relay.onMessage("C");
        assertEquals("C", receiver.messages.poll());

        relay.setTarget(null); // screen switch in progress
        relay.onMessage("D");
        assertTrue(receiver.messages.isEmpty());
        relay.setTarget(receiver);
        assertEquals("D", receiver.messages.poll());
    }

    @Test
    void protocolParsing() {
        assertEquals("GARBAGE", Protocol.command("GARBAGE 3"));
        assertEquals("3", Protocol.argument("GARBAGE 3"));
        assertEquals("GAMEOVER", Protocol.command("GAMEOVER"));
        assertEquals("", Protocol.argument("GAMEOVER"));
    }
}
