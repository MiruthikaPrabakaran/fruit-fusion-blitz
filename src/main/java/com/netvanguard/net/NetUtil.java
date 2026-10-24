package com.netvanguard.net;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/** Small helpers for the menu screen, e.g. showing "Tell your friend to join: 192.168.1.5". */
public final class NetUtil {
    private NetUtil() {
    }

    /**
     * This computer's local-network IPv4 addresses, most likely first.
     * Addresses on virtual adapters (VirtualBox, VMware, WSL, Hyper-V) are listed last.
     */
    public static List<String> localIPv4Addresses() {
        List<String> real = new ArrayList<>();
        List<String> virtual = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return real;
            }
            for (NetworkInterface nic : Collections.list(interfaces)) {
                if (!nic.isUp() || nic.isLoopback()) {
                    continue;
                }
                String name = (nic.getDisplayName() + " " + nic.getName()).toLowerCase();
                boolean isVirtual = name.contains("virtual") || name.contains("vmware")
                        || name.contains("vethernet") || name.contains("wsl") || name.contains("docker");
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                        (isVirtual ? virtual : real).add(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            // fall through with whatever we found
        }
        real.addAll(virtual);
        return real;
    }

    /** The single address to show the user. Falls back to 127.0.0.1 if no network is found. */
    public static String bestLocalAddress() {
        List<String> all = localIPv4Addresses();
        return all.isEmpty() ? "127.0.0.1" : all.get(0);
    }
}
