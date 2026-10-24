package com.netvanguard.ui;

import com.netvanguard.net.Peer;
import com.netvanguard.net.RelayListener;

/** Lets a screen ask the app to switch to another screen. Implemented by App. */
public interface Navigator {
    void showMenu();

    void startSolo();

    void showHostLobby();

    void showJoinLobby();

    /**
     * Called once the opponent is connected. The new screen must call relay.setTarget(...)
     * to start receiving messages (events are held until it does).
     */
    void onConnected(Peer peer, RelayListener relay, boolean isHost);
}
