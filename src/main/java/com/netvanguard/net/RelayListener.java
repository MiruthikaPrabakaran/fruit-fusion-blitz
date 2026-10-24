package com.netvanguard.net;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * A NetListener whose receiver can be swapped while the connection stays open.
 *
 * Why it exists: the lobby screen creates the connection, but a different screen (the game)
 * must receive the messages afterwards. While no receiver is set (setTarget(null)), events are
 * kept in order and handed over as soon as the next receiver is set, so nothing is lost
 * during the screen switch.
 */
public class RelayListener implements NetListener {
    private NetListener target;
    private final Queue<Consumer<NetListener>> waiting = new ArrayDeque<>();

    /** Sets who receives events from now on. null = hold events until a new target is set. */
    public synchronized void setTarget(NetListener newTarget) {
        target = newTarget;
        while (target != null && !waiting.isEmpty()) {
            waiting.poll().accept(target);
        }
    }

    private synchronized void dispatch(Consumer<NetListener> event) {
        if (target == null) {
            waiting.add(event);
        } else {
            event.accept(target);
        }
    }

    @Override
    public void onConnected() {
        dispatch(NetListener::onConnected);
    }

    @Override
    public void onMessage(String line) {
        dispatch(t -> t.onMessage(line));
    }

    @Override
    public void onDisconnected() {
        dispatch(NetListener::onDisconnected);
    }

    @Override
    public void onError(String message) {
        dispatch(t -> t.onError(message));
    }
}
