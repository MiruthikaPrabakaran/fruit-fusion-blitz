package com.netvanguard.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * The player who joins a game by typing the host's IP address.
 *
 * Usage:
 *   Client client = new Client("192.168.1.5", Protocol.DEFAULT_PORT, listener);
 *   client.connect();           // returns immediately; the result arrives through the listener
 *   ... listener.onConnected() on success, listener.onError(...) on failure ...
 *   client.send(Protocol.garbage(3));
 *   client.close();
 */
public class Client implements Peer {
    private static final int CONNECT_TIMEOUT_MS = 5000;

    private final String hostAddress;
    private final int port;
    private final NetListener listener;
    private volatile Connection connection;
    private volatile Socket pending;
    private volatile boolean stopped;

    public Client(String hostAddress, int port, NetListener listener) {
        this.hostAddress = hostAddress.trim();
        this.port = port;
        this.listener = listener;
    }

    /** Tries to connect on a background thread so the window never freezes. Returns immediately. */
    public void connect() {
        Thread connector = new Thread(this::doConnect, "net-connect");
        connector.setDaemon(true);
        connector.start();
    }

    private void doConnect() {
        Socket socket = new Socket();
        pending = socket;
        try {
            socket.connect(new InetSocketAddress(hostAddress, port), CONNECT_TIMEOUT_MS);
            Connection c = new Connection(socket, listener);
            connection = c;
            if (stopped) {
                c.close();
                return;
            }
            listener.onConnected();
            c.startReading();
        } catch (IOException | IllegalArgumentException e) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // nothing useful to do
            }
            if (!stopped) {
                listener.onError("Could not connect to " + hostAddress + ":" + port + " (" + e.getMessage() + ")");
            }
        }
    }

    @Override
    public void send(String line) {
        Connection c = connection;
        if (c != null) {
            c.send(line);
        }
    }

    @Override
    public boolean isConnected() {
        Connection c = connection;
        return c != null && c.isOpen();
    }

    @Override
    public void close() {
        stopped = true;
        Connection c = connection;
        if (c != null) {
            c.close();
            return;
        }
        Socket s = pending; // still connecting: abort the attempt
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignored) {
                // nothing useful to do
            }
        }
    }
}
