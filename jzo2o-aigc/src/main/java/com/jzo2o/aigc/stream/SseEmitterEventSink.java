package com.jzo2o.aigc.stream;

import com.jzo2o.aigc.controller.consumer.dto.RecommendationCardDTO;
import com.jzo2o.aigc.domain.ConversationStage;
import com.jzo2o.aigc.exception.AigcErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class SseEmitterEventSink implements SseEventSink {

    private static final int MAX_SUGGESTED_QUESTIONS = 3;

    private final SseEmitter emitter;
    private final Runnable onFirstDelta;
    private final Runnable onTerminal;
    private final Object sendMonitor = new Object();
    private final AtomicBoolean firstDeltaSent = new AtomicBoolean();
    private final AtomicBoolean terminated = new AtomicBoolean();

    public SseEmitterEventSink(SseEmitter emitter, Runnable onFirstDelta, Runnable onTerminal) {
        this.emitter = Objects.requireNonNull(emitter, "emitter");
        this.onFirstDelta = Objects.requireNonNull(onFirstDelta, "onFirstDelta");
        this.onTerminal = Objects.requireNonNull(onTerminal, "onTerminal");
    }

    @Override
    public void status(String stage) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stage", stage);
        send("status", data);
    }

    @Override
    public void delta(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("text", text);
        if (send("delta", data) && firstDeltaSent.compareAndSet(false, true)) {
            runSafely(onFirstDelta, "first delta callback");
        }
    }

    @Override
    public void recommendations(List<RecommendationCardDTO> cards) {
        List<RecommendationCardDTO> data = cards == null
                ? Collections.emptyList()
                : new ArrayList<>(cards);
        send("recommendations", data);
    }

    @Override
    public void done(ConversationStage stage, List<String> suggestedQuestions) {
        List<String> questions = suggestedQuestions == null
                ? Collections.emptyList()
                : new ArrayList<>(suggestedQuestions.subList(
                        0, Math.min(MAX_SUGGESTED_QUESTIONS, suggestedQuestions.size())));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stage", stage.name());
        data.put("suggestedQuestions", questions);
        terminate("done", data);
    }

    @Override
    public void error(AigcErrorCode errorCode, String message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", errorCode.getCode());
        data.put("message", message);
        data.put("retryable", errorCode.isRetryable());
        terminate("error", data);
    }

    public boolean isTerminated() {
        return terminated.get();
    }

    private boolean send(String eventName, Object data) {
        synchronized (sendMonitor) {
            if (terminated.get()) {
                return false;
            }
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
                return true;
            } catch (IOException | RuntimeException error) {
                terminateAfterSendFailure(error);
                return false;
            }
        }
    }

    private void terminate(String eventName, Object data) {
        synchronized (sendMonitor) {
            if (!terminated.compareAndSet(false, true)) {
                return;
            }
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
                emitter.complete();
            } catch (IOException | RuntimeException error) {
                completeWithErrorSafely(error);
            } finally {
                runSafely(onTerminal, "terminal callback");
            }
        }
    }

    private void terminateAfterSendFailure(Throwable error) {
        if (!terminated.compareAndSet(false, true)) {
            return;
        }
        completeWithErrorSafely(error);
        runSafely(onTerminal, "terminal callback");
    }

    private void completeWithErrorSafely(Throwable error) {
        try {
            emitter.completeWithError(error);
        } catch (RuntimeException completionFailure) {
            log.trace("SSE completion failed after transport error", completionFailure);
        }
    }

    private void runSafely(Runnable callback, String description) {
        try {
            callback.run();
        } catch (RuntimeException error) {
            log.trace("SSE {} failed", description, error);
        }
    }
}
