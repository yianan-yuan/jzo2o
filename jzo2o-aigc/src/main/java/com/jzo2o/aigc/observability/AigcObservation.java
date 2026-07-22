package com.jzo2o.aigc.observability;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AigcObservation {

    private static final int ANONYMOUS_USER_LENGTH = 12;

    private final String requestId;
    private final String anonymousUser;
    private final String provider;
    private final String model;
    private final long startedNanos;
    private final AtomicBoolean finished = new AtomicBoolean();

    private long sessionLoadMillis;
    private long demandExtractionMillis;
    private long serviceQueryMillis;
    private long firstDeltaMillis;
    private long totalMillis;
    private int modelCalls;
    private int retries;
    private long tokenUsage;
    private int candidateCount;
    private int recommendationCount;
    private String terminalStage;
    private String errorCode;
    private boolean degraded;
    private boolean firstDeltaRecorded;

    private AigcObservation(String requestId, Long userId, String provider, String model) {
        this.requestId = Objects.requireNonNull(requestId, "requestId");
        this.anonymousUser = anonymize(Objects.requireNonNull(userId, "userId"));
        this.provider = Objects.requireNonNull(provider, "provider");
        this.model = Objects.requireNonNull(model, "model");
        this.startedNanos = System.nanoTime();
    }

    public static AigcObservation start(String requestId, Long userId, String provider, String model) {
        return new AigcObservation(requestId, userId, provider, model);
    }

    public synchronized void recordSessionLoadMillis(long millis) {
        sessionLoadMillis = nonNegative(millis);
    }

    public synchronized void recordDemandExtractionMillis(long millis) {
        demandExtractionMillis = nonNegative(millis);
    }

    public synchronized void recordServiceQueryMillis(long millis) {
        serviceQueryMillis = nonNegative(millis);
    }

    public synchronized void addServiceQueryMillis(long millis) {
        serviceQueryMillis = saturatedAdd(serviceQueryMillis, nonNegative(millis));
    }

    public synchronized void recordFirstDeltaMillis(long millis) {
        if (!firstDeltaRecorded) {
            firstDeltaMillis = nonNegative(millis);
            firstDeltaRecorded = true;
        }
    }

    public void markFirstDelta() {
        recordFirstDeltaMillis(elapsedMillis(startedNanos));
    }

    public synchronized boolean finish(String terminalStage,
                                       String errorCode,
                                       boolean degraded,
                                       int candidateCount,
                                       int recommendationCount,
                                       int modelCalls,
                                       int retries,
                                       long tokenUsage) {
        String stableTerminalStage = Objects.requireNonNull(terminalStage, "terminalStage");
        if (!finished.compareAndSet(false, true)) {
            return false;
        }
        this.terminalStage = stableTerminalStage;
        this.errorCode = normalizeErrorCode(errorCode);
        this.degraded = degraded;
        this.candidateCount = nonNegative(candidateCount);
        this.recommendationCount = nonNegative(recommendationCount);
        this.modelCalls = nonNegative(modelCalls);
        this.retries = nonNegative(retries);
        this.tokenUsage = nonNegative(tokenUsage);
        this.totalMillis = elapsedMillis(startedNanos);
        return true;
    }

    public synchronized Map<String, Object> toLogFields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("requestId", requestId);
        fields.put("anonymousUser", anonymousUser);
        fields.put("provider", provider);
        fields.put("model", model);
        fields.put("sessionLoadMillis", sessionLoadMillis);
        fields.put("demandExtractionMillis", demandExtractionMillis);
        fields.put("serviceQueryMillis", serviceQueryMillis);
        fields.put("firstDeltaMillis", firstDeltaMillis);
        fields.put("totalMillis", totalMillis);
        fields.put("modelCalls", modelCalls);
        fields.put("retries", retries);
        fields.put("tokenUsage", tokenUsage);
        fields.put("candidateCount", candidateCount);
        fields.put("recommendationCount", recommendationCount);
        fields.put("terminalStage", terminalStage);
        fields.put("errorCode", errorCode);
        fields.put("degraded", degraded);
        return Collections.unmodifiableMap(fields);
    }

    private static String anonymize(Long userId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(("aigc:" + userId).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                hex.append(String.format("%02x", value & 0xff));
            }
            return hex.substring(0, ANONYMOUS_USER_LENGTH);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static long elapsedMillis(long startNanos) {
        return nonNegative((System.nanoTime() - startNanos) / 1_000_000L);
    }

    private static String normalizeErrorCode(String value) {
        return value == null || value.trim().isEmpty() ? null : value;
    }

    private static int nonNegative(int value) {
        return Math.max(0, value);
    }

    private static long nonNegative(long value) {
        return Math.max(0L, value);
    }

    private static long saturatedAdd(long left, long right) {
        if (Long.MAX_VALUE - left < right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
