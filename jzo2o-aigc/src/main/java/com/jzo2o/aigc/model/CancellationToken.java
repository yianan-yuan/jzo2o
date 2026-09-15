package com.jzo2o.aigc.model;

import java.util.Objects;
import java.util.ArrayList;
import java.util.List;

public final class CancellationToken {

    private final Object lock = new Object();
    private final List<Runnable> callbacks = new ArrayList<>();
    private volatile boolean cancelled;

    public void cancel() {
        List<Runnable> callbacksToRun;
        synchronized (lock) {
            if (cancelled) {
                return;
            }
            cancelled = true;
            callbacksToRun = new ArrayList<>(callbacks);
            callbacks.clear();
        }
        RuntimeException firstFailure = null;
        for (Runnable callback : callbacksToRun) {
            try {
                callback.run();
            } catch (RuntimeException error) {
                if (firstFailure == null) {
                    firstFailure = error;
                } else if (error != firstFailure) {
                    firstFailure.addSuppressed(error);
                }
            }
        }
        if (firstFailure != null) {
            throw firstFailure;
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void onCancel(Runnable callback) {
        Objects.requireNonNull(callback, "callback");
        synchronized (lock) {
            if (!cancelled) {
                callbacks.add(callback);
                return;
            }
        }
        callback.run();
    }
}
