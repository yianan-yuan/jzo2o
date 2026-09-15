package com.jzo2o.aigc.guard;

import com.jzo2o.aigc.exception.AigcErrorCode;
import com.jzo2o.aigc.exception.AigcException;
import com.jzo2o.aigc.properties.AigcProperties;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RMapCache;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class AigcRequestGuard {

    private static final long LEASE_GRACE_SECONDS = 10L;

    private final RedissonClient redissonClient;
    private final AigcProperties properties;

    public GenerationLease acquire(Long userId, String sessionId) {
        RRateLimiter limiter = redissonClient.getRateLimiter("AIGC:RATE:" + userId);
        limiter.trySetRate(RateType.OVERALL, properties.getRequestsPerMinute(), 1, RateIntervalUnit.MINUTES);
        if (!limiter.tryAcquire()) {
            throw new AigcException(AigcErrorCode.RATE_LIMITED);
        }

        RMapCache<String, String> activeGenerations = redissonClient.getMapCache("AIGC:GENERATION");
        String token = UUID.randomUUID().toString();
        String existingToken = activeGenerations.putIfAbsent(sessionId, token,
                properties.getTotalTimeoutSeconds() + LEASE_GRACE_SECONDS, TimeUnit.SECONDS);
        if (existingToken != null) {
            throw new AigcException(AigcErrorCode.GENERATION_CONFLICT);
        }
        return new GenerationLease(activeGenerations, sessionId, token);
    }
}
