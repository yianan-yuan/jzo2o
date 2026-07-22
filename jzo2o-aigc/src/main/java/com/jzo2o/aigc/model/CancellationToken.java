package com.jzo2o.aigc.model;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CancellationToken {

    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final ConcurrentLinkedQueue<Runnable> callbacks = new ConcurrentLinkedQueue<>();

    public void cancel() {
        if (!cancelled.compareAndSet(false, true)) {
            return;
        }
        RuntimeException firstFailure = null;
        Runnable callback;
        while ((callback = callbacks.poll()) != null) {
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
        return cancelled.get();
    }

    public void onCancel(Runnable callback) {
        Objects.requireNonNull(callback, "callback");
        if (cancelled.get()) {
            callback.run();
            return;
        }
        callbacks.add(callback);
        if (cancelled.get() && callbacks.remove(callback)) {
            callback.run();
        }
    }
}
