package com.netvanguard.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The player who creates the game. Opens a port and waits for ONE opponent to join.
 *
 * Usage:
 *   Host host = new Host(Protocol.DEFAULT_PORT, listener);
 *   host.start();               // throws IOException if the port is already in use
 *   ... listener.onConnected() fires when the opponent joins ...
 *   host.send(Protocol.score(100));
 *   host.close();
 */
public class Host implements Peer {
    private final int port;
    private final NetListener listener;
    private volatile ServerSocket server;
    private volatile Connection connection;
    private volatile boolean stopped;

    public Host(int port, NetListener listener) {
        this.port = port;
        this.listener = listener;
    }

    /**
     * Opens the port right away (so a "port in use" problem shows up here, where the UI can report it),
     * then waits for the opponent on a background thread. Returns immediately.
     */
    public void start() throws IOException {
        server = new ServerSocket(port);
        Thread acceptor = new Thread(this::acceptOne, "net-accept");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    /** The port actually in use. Handy when you pass port 0 (any free port), as the tests do. */
    public int getPort() {
        ServerSocket s = server;
        return s == null ? port : s.getLocalPort();
    }

    private void acceptOne() {
        try {
            Socket socket = server.accept();
            closeServer(); // only one opponent allowed
            Connection c = new Connection(socket, listener);
            connection = c;
            if (stopped) {
                c.close();
                return;
            }
            listener.onConnected();
            c.startReading();
        } catch (IOException e) {
            if (!stopped) {
                listener.onError("Host error: " + e.getMessage());
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
        closeServer();
        Connection c = connection;
        if (c != null) {
            c.close();
        }
    }

    private void closeServer() {
        ServerSocket s = server;
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignored) {
                // nothing useful to do
            }
        }
    }
}
