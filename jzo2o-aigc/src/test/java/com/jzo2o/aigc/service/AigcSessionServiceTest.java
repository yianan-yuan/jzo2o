package com.jzo2o.aigc.service;

import com.jzo2o.aigc.domain.AigcSession;
import com.jzo2o.aigc.domain.ChatTurn;
import com.jzo2o.aigc.exception.AigcErrorCode;
import com.jzo2o.aigc.exception.AigcException;
import com.jzo2o.aigc.exception.AigcExceptionAdvice;
import com.jzo2o.aigc.properties.AigcProperties;
import com.jzo2o.aigc.repository.AigcSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AigcSessionServiceTest {

    private final AigcSessionRepository repository = mock(AigcSessionRepository.class);

    @Test
    void shouldRejectExpiredOwnedSession() {
        when(repository.findOwner("s1")).thenReturn(Optional.of(7L));
        when(repository.find(7L, "s1")).thenReturn(Optional.empty());

        AigcSessionService service = new AigcSessionService(repository, properties());

        assertThatThrownBy(() -> service.loadOwned(7L, "s1"))
                .isInstanceOfSatisfying(AigcException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(AigcErrorCode.SESSION_EXPIRED));
    }

    @Test
    void shouldClearRecommendationsWhenCityChanges() {
        AigcSession session = AigcSession.create("s1", 7L);
        session.setCityCode("010");
        session.setLastRecommendedServeIds(Arrays.asList(1L, 2L));
        session.getDemandProfile().setSummary("Need a cleaner");

        session.updateCity("021");

        assertThat(session.getLastRecommendedServeIds()).isEmpty();
        assertThat(session.getCityCode()).isEqualTo("021");
        assertThat(session.getDemandProfile().getSummary()).isEqualTo("Need a cleaner");
    }

    @Test
    void shouldHideSessionOwnedByAnotherUser() {
        when(repository.findOwner("s1")).thenReturn(Optional.of(8L));

        AigcSessionService service = new AigcSessionService(repository, properties());

        assertThatThrownBy(() -> service.loadOwned(7L, "s1"))
                .isInstanceOfSatisfying(AigcException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(AigcErrorCode.SESSION_NOT_FOUND));
    }

    @Test
    void shouldRefreshSessionTtlAfterSuccessfulOwnedLoad() {
        AigcSession session = AigcSession.create("s1", 7L);
        when(repository.findOwner("s1")).thenReturn(Optional.of(7L));
        when(repository.find(7L, "s1")).thenReturn(Optional.of(session));

        AigcSession loaded = new AigcSessionService(repository, properties()).loadOwned(7L, "s1");

        assertThat(loaded).isSameAs(session);
        verify(repository).save(session, Duration.ofMinutes(30));
    }

    @Test
    void shouldTrimChatTurnsToConfiguredMaximum() {
        AigcSession session = AigcSession.create("s1", 7L);
        for (int index = 0; index < 11; index++) {
            session.addChatTurn(new ChatTurn("user", "message-" + index), 10);
        }

        assertThat(session.getRecentChatTurns()).hasSize(10);
        assertThat(session.getRecentChatTurns().get(0).getContent()).isEqualTo("message-1");
    }

    @Test
    void shouldReturnStableUnwrappedErrorResponse() {
        AigcException exception = new AigcException(AigcErrorCode.RATE_LIMITED, "Try later");

        ResponseEntity<?> response = new AigcExceptionAdvice().handle(exception);

        assertThat(response.getStatusCodeValue()).isEqualTo(429);
        assertThat(response.getHeaders().getFirst("Processed-Mark")).isEqualTo("1");
        assertThat(response.getBody()).isEqualTo(java.util.Map.of(
                "code", "AIGC_RATE_LIMITED",
                "message", "Try later",
                "retryable", true));
    }

    private AigcProperties properties() {
        return new AigcProperties();
    }
}
