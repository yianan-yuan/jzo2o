package com.jzo2o.aigc.controller.consumer;

import com.jzo2o.aigc.controller.consumer.dto.AssistantMessageReqDTO;
import com.jzo2o.aigc.controller.consumer.dto.CreateSessionResDTO;
import com.jzo2o.aigc.domain.AigcSession;
import com.jzo2o.aigc.exception.AigcErrorCode;
import com.jzo2o.aigc.exception.AigcException;
import com.jzo2o.aigc.guard.AigcRequestGuard;
import com.jzo2o.aigc.guard.GenerationLease;
import com.jzo2o.aigc.model.CancellationToken;
import com.jzo2o.aigc.properties.AigcProperties;
import com.jzo2o.aigc.service.AigcSessionService;
import com.jzo2o.aigc.service.AssistantOrchestrator;
import com.jzo2o.aigc.stream.SseEmitterEventSink;
import com.jzo2o.common.handler.UserInfoHandler;
import com.jzo2o.common.model.CurrentUser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/consumer/assistant")
public class AiAssistantController {

    private static final int DEFAULT_SESSION_TTL_SECONDS = 1800;
    private static final long SSE_TIMEOUT_MILLIS = 95_000L;
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final Pattern CITY_CODE = Pattern.compile("[0-9]{3,6}");

    private final UserInfoHandler userInfoHandler;
    private final AigcSessionService sessionService;
    private final AigcRequestGuard guard;
    private final AssistantOrchestrator orchestrator;
    private final Executor executor;
    private final ScheduledExecutorService scheduler;
    private final AigcProperties properties;

    public AiAssistantController(UserInfoHandler userInfoHandler,
                                 AigcSessionService sessionService,
                                 AigcRequestGuard guard,
                                 AssistantOrchestrator orchestrator,
                                 @Qualifier("aigcExecutor") Executor executor,
                                 @Qualifier("aigcScheduler") ScheduledExecutorService scheduler,
                                 AigcProperties properties) {
        this.userInfoHandler = userInfoHandler;
        this.sessionService = sessionService;
        this.guard = guard;
        this.orchestrator = orchestrator;
        this.executor = executor;
        this.scheduler = scheduler;
        this.properties = properties;
    }

    @PostMapping("/sessions")
    public CreateSessionResDTO createSession() {
        Long userId = currentUserId();
        AigcSession session = sessionService.create(userId);
        return new CreateSessionResDTO(session.getSessionId(), DEFAULT_SESSION_TTL_SECONDS);
    }

    @PostMapping(value = "/sessions/{sessionId}/messages",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(@PathVariable("sessionId") String sessionId,
                                  @Valid @RequestBody AssistantMessageReqDTO request) {
        Long userId = currentUserId();
        validateRequest(request);
        AigcSession session = sessionService.loadOwned(userId, sessionId);
        GenerationLease lease = guard.acquire(userId, sessionId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        CancellationToken cancellationToken = new CancellationToken();
        StreamLifecycle lifecycle = new StreamLifecycle(lease);
        SseEmitterEventSink sink = new SseEmitterEventSink(
                emitter, lifecycle::onFirstDelta, lifecycle::cleanup);

        emitter.onTimeout(() -> requestTimeout(cancellationToken, sink, lifecycle));
        emitter.onError(error -> {
            cancelQuietly(cancellationToken);
            lifecycle.cleanup();
        });
        emitter.onCompletion(() -> {
            cancelQuietly(cancellationToken);
            lifecycle.cleanup();
        });

        try {
            ScheduledFuture<?> firstTokenFuture = scheduler.schedule(
                    () -> requestTimeout(cancellationToken, sink, lifecycle),
                    properties.getFirstTokenTimeoutSeconds(), TimeUnit.SECONDS);
            lifecycle.setFirstTokenFuture(firstTokenFuture);
            ScheduledFuture<?> totalFuture = scheduler.schedule(
                    () -> requestTimeout(cancellationToken, sink, lifecycle),
                    properties.getTotalTimeoutSeconds(), TimeUnit.SECONDS);
            lifecycle.setTotalFuture(totalFuture);
        } catch (RuntimeException schedulingFailure) {
            cancelQuietly(cancellationToken);
            lifecycle.cleanup();
            throw schedulingFailure;
        }

        try {
            executor.execute(() -> {
                try {
                    orchestrator.run(session, request.getCityCode(), request.getMessage(), sink, cancellationToken);
                } catch (AigcException error) {
                    sink.error(error.getErrorCode(), error.getMessage());
                } catch (RuntimeException error) {
                    log.error("Assistant stream failed unexpectedly", error);
                    sink.error(AigcErrorCode.MODEL_UNAVAILABLE, AigcErrorCode.MODEL_UNAVAILABLE.getCode());
                } finally {
                    lifecycle.cleanup();
                }
            });
        } catch (RuntimeException executionFailure) {
            sink.error(AigcErrorCode.MODEL_UNAVAILABLE, AigcErrorCode.MODEL_UNAVAILABLE.getCode());
            lifecycle.cleanup();
        }
        return emitter;
    }

    private void requestTimeout(CancellationToken cancellationToken,
                                SseEmitterEventSink sink,
                                StreamLifecycle lifecycle) {
        cancelQuietly(cancellationToken);
        sink.error(AigcErrorCode.REQUEST_TIMEOUT, AigcErrorCode.REQUEST_TIMEOUT.getCode());
        lifecycle.cleanup();
    }

    private void cancelQuietly(CancellationToken cancellationToken) {
        try {
            cancellationToken.cancel();
        } catch (RuntimeException error) {
            log.trace("Cancellation callback failed", error);
        }
    }

    private void validateRequest(AssistantMessageReqDTO request) {
        String message = request == null ? null : request.getMessage();
        String cityCode = request == null ? null : request.getCityCode();
        boolean invalidMessage = message == null || message.trim().isEmpty()
                || message.length() > MAX_MESSAGE_LENGTH;
        boolean invalidCity = cityCode == null || cityCode.trim().isEmpty()
                || !CITY_CODE.matcher(cityCode).matches();
        if (invalidMessage || invalidCity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid assistant message request");
        }
    }

    private Long currentUserId() {
        CurrentUser currentUser = userInfoHandler.currentUserInfo();
        if (currentUser == null || currentUser.getId() == null) {
            throw new AigcException(AigcErrorCode.UNAUTHORIZED);
        }
        return currentUser.getId();
    }

    private static final class StreamLifecycle {
        private final GenerationLease lease;
        private final AtomicReference<ScheduledFuture<?>> firstTokenFuture = new AtomicReference<>();
        private final AtomicReference<ScheduledFuture<?>> totalFuture = new AtomicReference<>();
        private final AtomicBoolean firstDelta = new AtomicBoolean();
        private final AtomicBoolean terminated = new AtomicBoolean();
        private final AtomicBoolean leaseClosed = new AtomicBoolean();

        private StreamLifecycle(GenerationLease lease) {
            this.lease = lease;
        }

        private void setFirstTokenFuture(ScheduledFuture<?> future) {
            assignFuture(firstTokenFuture, future);
            if (firstDelta.get() || terminated.get()) {
                cancelFuture(firstTokenFuture);
            }
        }

        private void setTotalFuture(ScheduledFuture<?> future) {
            assignFuture(totalFuture, future);
            if (terminated.get()) {
                cancelFuture(totalFuture);
            }
        }

        private void onFirstDelta() {
            if (firstDelta.compareAndSet(false, true)) {
                cancelFuture(firstTokenFuture);
            }
        }

        private void cleanup() {
            terminated.set(true);
            cancelFuture(firstTokenFuture);
            cancelFuture(totalFuture);
            closeLease();
        }

        private void assignFuture(AtomicReference<ScheduledFuture<?>> target, ScheduledFuture<?> future) {
            if (!target.compareAndSet(null, future)) {
                cancel(future);
            }
        }

        private void cancelFuture(AtomicReference<ScheduledFuture<?>> target) {
            ScheduledFuture<?> future = target.getAndSet(null);
            if (future != null) {
                cancel(future);
            }
        }

        private void cancel(ScheduledFuture<?> future) {
            try {
                future.cancel(false);
            } catch (RuntimeException error) {
                log.trace("Timer cancellation failed", error);
            }
        }

        private synchronized void closeLease() {
            if (leaseClosed.get()) {
                return;
            }
            try {
                lease.close();
                leaseClosed.set(true);
            } catch (RuntimeException error) {
                log.trace("Generation lease cleanup failed and remains retryable", error);
            }
        }
    }
}
