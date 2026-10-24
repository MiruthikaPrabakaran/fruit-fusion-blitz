package com.netvanguard.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Console tool to try the networking without any GUI. Open TWO terminals / run configurations.
 *
 *   Terminal 1:  host [port]
 *   Terminal 2:  client <host-ip> [port]
 *
 * Then type any line and press Enter: it appears on the other side. Type "quit" to exit.
 * Try the real messages too, e.g.  GARBAGE 3   or   SCORE 1200
 *
 * In IntelliJ: right-click this file -> Modify Run Configuration, set "Program arguments"
 * to  host  (or  client 127.0.0.1 ), and allow multiple instances to run both at once.
 */
public class NetTest {

    public static void main(String[] args) throws IOException {
        if (args.length == 0 || !(args[0].equals("host") || args[0].equals("client"))) {
            System.out.println("Usage:  host [port]   |   client <host-ip> [port]");
            return;
        }

        NetListener listener = new NetListener() {
            @Override
            public void onConnected() {
                System.out.println("[connected] type a message and press Enter, or 'quit'");
            }

            @Override
            public void onMessage(String line) {
                System.out.println("[received] " + line + "   (command=" + Protocol.command(line)
                        + ", argument=" + Protocol.argument(line) + ")");
            }

            @Override
            public void onDisconnected() {
                System.out.println("[disconnected]");
            }

            @Override
            public void onError(String message) {
                System.out.println("[error] " + message);
            }
        };

        Peer peer;
        if (args[0].equals("host")) {
            int port = args.length > 1 ? Integer.parseInt(args[1]) : Protocol.DEFAULT_PORT;
            Host host = new Host(port, listener);
            host.start();
            System.out.println("[hosting] port " + port + ". Other player should join one of: " + NetUtil.localIPv4Addresses());
            peer = host;
        } else {
            if (args.length < 2) {
                System.out.println("Usage:  client <host-ip> [port]");
                return;
            }
            int port = args.length > 2 ? Integer.parseInt(args[2]) : Protocol.DEFAULT_PORT;
            Client client = new Client(args[1], port, listener);
            client.connect();
            System.out.println("[connecting] " + args[1] + ":" + port + " ...");
            peer = client;
        }

        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = console.readLine()) != null) {
            if (line.equalsIgnoreCase("quit")) {
                break;
            }
            if (!peer.isConnected()) {
                System.out.println("[not connected yet]");
                continue;
            }
            peer.send(line);
        }
        peer.close();
    }
}
