package com.jzo2o.aigc.repository;

import com.jzo2o.aigc.domain.AigcSession;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RedisAigcSessionRepository implements AigcSessionRepository {

    private static final Duration OWNER_RETENTION = Duration.ofHours(24);

    private final RedissonClient redissonClient;

    @Override
    public void create(AigcSession session, Duration ttl) {
        bucket(session.getUserId(), session.getSessionId()).set(session, ttl.toMillis(), TimeUnit.MILLISECONDS);
        ownerBucket(session.getSessionId()).set(session.getUserId(), OWNER_RETENTION.plus(ttl).toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public Optional<Long> findOwner(String sessionId) {
        return Optional.ofNullable(ownerBucket(sessionId).get());
    }

    @Override
    public Optional<AigcSession> find(Long userId, String sessionId) {
        return Optional.ofNullable(bucket(userId, sessionId).get());
    }

    @Override
    public void save(AigcSession session, Duration ttl) {
        bucket(session.getUserId(), session.getSessionId()).set(session, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    private RBucket<AigcSession> bucket(Long userId, String sessionId) {
        return redissonClient.getBucket("AIGC:SESSION:" + userId + ':' + sessionId);
    }

    private RBucket<Long> ownerBucket(String sessionId) {
        return redissonClient.getBucket("AIGC:SESSION:OWNER:" + sessionId);
    }
}
