package com.netvanguard.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One live socket between the two players. Reads lines on a background thread and
 * hands them to the listener. Used internally by Host and Client.
 */
class Connection {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final NetListener listener;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    Connection(Socket socket, NetListener listener) throws IOException {
        this.socket = socket;
        this.listener = listener;
        this.socket.setTcpNoDelay(true); // send small messages immediately, important for a real-time game
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    /** Starts the background thread that waits for incoming lines. */
    void startReading() {
        Thread reader = new Thread(this::readLoop, "net-reader");
        reader.setDaemon(true); // don't keep the program alive after the window closes
        reader.start();
    }

    private void readLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                listener.onMessage(line);
            }
        } catch (IOException e) {
            if (!closed.get()) {
                listener.onError("Connection lost: " + e.getMessage());
            }
        } finally {
            close();
        }
    }

    void send(String line) {
        if (closed.get()) {
            return;
        }
        // A message must stay on one line, otherwise the receiver would see two messages.
        String clean = line.replace('\r', ' ').replace('\n', ' ');
        boolean failed;
        synchronized (out) {
            out.print(clean);
            out.print('\n');
            out.flush();
            failed = out.checkError();
        }
        if (failed) {
            close();
        }
    }

    boolean isOpen() {
        return !closed.get();
    }

    /** Closes the socket and tells the listener, exactly once. */
    void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // nothing useful to do
        }
        listener.onDisconnected();
    }
}
