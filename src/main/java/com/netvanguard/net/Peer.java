package com.netvanguard.net;

/**
 * What the game code sees. A Host and a Client both behave like this once connected,
 * so the game never needs to care which role it has.
 */
public interface Peer extends AutoCloseable {

    /** Sends one message line to the opponent. Does nothing if not connected. Safe to call from any thread. */
    void send(String line);

    boolean isConnected();

    /** Closes the connection (and stops waiting for one). Safe to call more than once. */
    @Override
    void close();
}
