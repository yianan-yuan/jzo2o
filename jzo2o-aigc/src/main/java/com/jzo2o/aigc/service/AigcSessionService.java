package com.jzo2o.aigc.service;

import com.jzo2o.aigc.domain.AigcSession;
import com.jzo2o.aigc.exception.AigcErrorCode;
import com.jzo2o.aigc.exception.AigcException;
import com.jzo2o.aigc.properties.AigcProperties;
import com.jzo2o.aigc.repository.AigcSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AigcSessionService {

    private final AigcSessionRepository repository;
    private final AigcProperties properties;

    public AigcSession create(Long userId) {
        AigcSession session = AigcSession.create(UUID.randomUUID().toString(), userId);
        repository.create(session, sessionTtl());
        return session;
    }

    public AigcSession loadOwned(Long userId, String sessionId) {
        Long ownerId = repository.findOwner(sessionId)
                .orElseThrow(() -> new AigcException(AigcErrorCode.SESSION_NOT_FOUND));
        if (!ownerId.equals(userId)) {
            throw new AigcException(AigcErrorCode.SESSION_NOT_FOUND);
        }
        AigcSession session = repository.find(userId, sessionId)
                .orElseThrow(() -> new AigcException(AigcErrorCode.SESSION_EXPIRED));
        repository.save(session, sessionTtl());
        return session;
    }

    public void save(AigcSession session) {
        repository.save(session, sessionTtl());
    }

    private Duration sessionTtl() {
        return Duration.ofMinutes(properties.getSessionTtlMinutes());
    }
}
