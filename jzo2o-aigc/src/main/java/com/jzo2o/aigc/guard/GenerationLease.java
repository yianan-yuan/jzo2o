package com.jzo2o.aigc.guard;

import org.redisson.api.RMapCache;

public class GenerationLease implements AutoCloseable {

    private final RMapCache<String, String> activeGenerations;
    private final String sessionId;
    private final String token;
    private boolean closed;

    public GenerationLease(RMapCache<String, String> activeGenerations, String sessionId, String token) {
        this.activeGenerations = activeGenerations;
        this.sessionId = sessionId;
        this.token = token;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        activeGenerations.remove(sessionId, token);
        closed = true;
    }
}
