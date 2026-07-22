package com.jzo2o.aigc.guard;

import org.redisson.api.RMapCache;

import java.util.concurrent.atomic.AtomicBoolean;

public class GenerationLease implements AutoCloseable {

    private final RMapCache<String, String> activeGenerations;
    private final String sessionId;
    private final String token;
    private final AtomicBoolean closed = new AtomicBoolean();

    public GenerationLease(RMapCache<String, String> activeGenerations, String sessionId, String token) {
        this.activeGenerations = activeGenerations;
        this.sessionId = sessionId;
        this.token = token;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            activeGenerations.remove(sessionId, token);
        }
    }
}
