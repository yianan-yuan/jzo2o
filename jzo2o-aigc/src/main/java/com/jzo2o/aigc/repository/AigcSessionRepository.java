package com.jzo2o.aigc.repository;

import com.jzo2o.aigc.domain.AigcSession;

import java.time.Duration;
import java.util.Optional;

public interface AigcSessionRepository {

    void create(AigcSession session, Duration ttl);

    Optional<Long> findOwner(String sessionId);

    Optional<AigcSession> find(Long userId, String sessionId);

    void save(AigcSession session, Duration ttl);
}
