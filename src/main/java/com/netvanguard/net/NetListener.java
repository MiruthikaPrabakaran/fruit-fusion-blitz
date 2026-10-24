package com.netvanguard.net;

/**
 * Implemented by whoever wants to react to network events (the game / UI code).
 *
 * IMPORTANT: these methods are called from a BACKGROUND thread, not the JavaFX thread.
 * If you touch the UI from inside them, wrap it in Platform.runLater(() -> ...).
 */
public interface NetListener {

    /** The other player is connected and messages can now be sent. */
    void onConnected();

    /** One full line arrived from the opponent, e.g. "GARBAGE 3". Use Protocol.command/argument to split it. */
    void onMessage(String line);

    /** The connection closed (the other player quit, the network dropped, or we called close()). Called once. */
    void onDisconnected();

    /** Something went wrong, e.g. "Could not connect to 192.168.1.5:5050". Optional to override. */
    default void onError(String message) {
    }
}
