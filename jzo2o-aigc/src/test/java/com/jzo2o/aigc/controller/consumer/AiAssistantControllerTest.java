package com.jzo2o.aigc.controller.consumer;

import com.jzo2o.aigc.controller.consumer.dto.AssistantMessageReqDTO;
import com.jzo2o.aigc.controller.consumer.dto.CreateSessionResDTO;
import com.jzo2o.aigc.controller.consumer.dto.RecommendationCardDTO;
import com.jzo2o.aigc.domain.AigcSession;
import com.jzo2o.aigc.domain.ConversationStage;
import com.jzo2o.aigc.domain.DemandDecision;
import com.jzo2o.aigc.domain.DemandProfile;
import com.jzo2o.aigc.exception.AigcErrorCode;
import com.jzo2o.aigc.exception.AigcException;
import com.jzo2o.aigc.exception.AigcExceptionAdvice;
import com.jzo2o.aigc.guard.AigcRequestGuard;
import com.jzo2o.aigc.guard.GenerationLease;
import com.jzo2o.aigc.model.CancellationToken;
import com.jzo2o.aigc.observability.AigcObservation;
import com.jzo2o.aigc.observability.AigcObservationLogger;
import com.jzo2o.aigc.properties.AigcProperties;
import com.jzo2o.aigc.service.AigcSessionService;
import com.jzo2o.aigc.service.AssistantOrchestrator;
import com.jzo2o.aigc.service.CandidateSelectionService;
import com.jzo2o.aigc.service.DemandUnderstandingService;
import com.jzo2o.aigc.service.ReplyGenerationService;
import com.jzo2o.aigc.service.ServiceCatalogService;
import com.jzo2o.aigc.stream.SseEmitterEventSink;
import com.jzo2o.aigc.stream.SseEventSink;
import com.jzo2o.common.handler.UserInfoHandler;
import com.jzo2o.common.model.CurrentUserInfo;
import com.jzo2o.mvc.advice.CommonExceptionAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiAssistantControllerTest {

    private final UserInfoHandler userInfoHandler = mock(UserInfoHandler.class);
    private final AigcSessionService sessionService = mock(AigcSessionService.class);
    private final AigcRequestGuard guard = mock(AigcRequestGuard.class);
    private final AssistantOrchestrator orchestrator = mock(AssistantOrchestrator.class);
    private final AigcProperties properties = new AigcProperties();
    private final GenerationLease lease = mock(GenerationLease.class);
    private AigcSession session;

    @BeforeEach
    void setUp() {
        session = AigcSession.create("s1", 7L);
        when(userInfoHandler.currentUserInfo()).thenReturn(new CurrentUserInfo(7L, "user", null, 1));
        when(sessionService.loadOwned(7L, "s1")).thenReturn(session);
        when(guard.acquire(7L, "s1")).thenReturn(lease);
    }

    @Test
    void shouldRejectUnauthenticatedRequestBeforeSessionOrGuardAccess() {
        when(userInfoHandler.currentUserInfo()).thenReturn(null);
        AiAssistantController controller = controller(Runnable::run, new ManualScheduler());

        assertThatThrownBy(controller::createSession)
                .isInstanceOfSatisfying(AigcException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(AigcErrorCode.UNAUTHORIZED));
        verifyNoInteractions(sessionService, guard, orchestrator);
    }

    @Test
    void shouldCreateSessionForAuthenticatedUserWithoutStartingModelChain() {
        when(sessionService.create(7L)).thenReturn(session);

        CreateSessionResDTO response = controller(Runnable::run, new ManualScheduler()).createSession();

        assertThat(response.getSessionId()).isEqualTo("s1");
        assertThat(response.getExpiresInSeconds()).isEqualTo(1800);
        verify(sessionService).create(7L);
        verifyNoInteractions(guard, orchestrator);
    }

    @Test
    void shouldMeasurePreloadedSessionAndRecordItThroughTheProductionOrchestratorPath() {
        QueuedExecutor executor = new QueuedExecutor();
        ManualScheduler scheduler = new ManualScheduler();
        DemandUnderstandingService understanding = mock(DemandUnderstandingService.class);
        ServiceCatalogService catalog = mock(ServiceCatalogService.class);
        CandidateSelectionService selection = mock(CandidateSelectionService.class);
        ReplyGenerationService replyGenerator = mock(ReplyGenerationService.class);
        AigcObservationLogger observationLogger = mock(AigcObservationLogger.class);
        DemandProfile profile = new DemandProfile();
        profile.setSummary("空调问题");
        when(understanding.understand(same(session), eq("保洁"), any()))
                .thenReturn(new DemandDecision(profile, true, "需要维修还是清洗？", null, null));
        AssistantOrchestrator realOrchestrator = new AssistantOrchestrator(
                sessionService, understanding, catalog, selection, replyGenerator, properties,
                observationLogger);
        long[] nanoTimes = {1_000_000_000L, 1_017_000_000L};
        AtomicInteger clockCalls = new AtomicInteger();
        LongSupplier nanoTime = () -> nanoTimes[clockCalls.getAndIncrement()];
        AiAssistantController controller = new AiAssistantController(
                userInfoHandler, sessionService, guard, realOrchestrator,
                executor, scheduler, properties, nanoTime);

        controller.sendMessage("s1", request());
        executor.runQueued();

        ArgumentCaptor<AigcObservation> observation = ArgumentCaptor.forClass(AigcObservation.class);
        verify(observationLogger, times(1)).log(observation.capture());
        assertThat(observation.getValue().toLogFields())
                .containsEntry("sessionLoadMillis", 17L)
                .containsEntry("terminalStage", "CLARIFYING");
        assertThat(clockCalls).hasValue(2);
        verify(sessionService, times(1)).loadOwned(7L, "s1");
        verify(guard, times(1)).acquire(7L, "s1");
        verifyNoInteractions(catalog, selection, replyGenerator);
    }

    @Test
    void shouldValidateMessageAndCityWithoutClientUserId() throws Exception {
        Field message = AssistantMessageReqDTO.class.getDeclaredField("message");
        Field cityCode = AssistantMessageReqDTO.class.getDeclaredField("cityCode");
        Method endpoint = AiAssistantController.class.getDeclaredMethod(
                "sendMessage", String.class, AssistantMessageReqDTO.class);

        assertThat(message.getAnnotation(NotBlank.class)).isNotNull();
        assertThat(message.getAnnotation(Size.class).max()).isEqualTo(1000);
        assertThat(cityCode.getAnnotation(NotBlank.class)).isNotNull();
        assertThat(cityCode.getAnnotation(Pattern.class).regexp()).isEqualTo("[0-9]{3,6}");
        assertThat(endpoint.getParameterAnnotations()[1])
                .anyMatch(annotation -> annotation.annotationType() == Valid.class);
        assertThat(endpoint.getAnnotation(PostMapping.class).produces())
                .containsExactly("text/event-stream");
        assertThat(AssistantMessageReqDTO.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .containsExactlyInAnyOrder("message", "cityCode");
    }

    @Test
    void shouldRejectMessageWithoutUserBeforeOwnedLoadOrGuard() {
        when(userInfoHandler.currentUserInfo()).thenReturn(null);

        assertThatThrownBy(() -> controller(Runnable::run, new ManualScheduler())
                .sendMessage("s1", request()))
                .isInstanceOfSatisfying(AigcException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(AigcErrorCode.UNAUTHORIZED));
        verifyNoInteractions(sessionService, guard, orchestrator);
    }

    @Test
    void shouldRejectInvalidDtoBeforeOwnedLoadEvenWithoutValidationProvider() {
        AssistantMessageReqDTO blankMessage = request();
        blankMessage.setMessage("   ");
        AssistantMessageReqDTO longMessage = request();
        longMessage.setMessage(String.join("", Collections.nCopies(1001, "字")));
        AssistantMessageReqDTO invalidCity = request();
        invalidCity.setCityCode("01A");

        for (AssistantMessageReqDTO invalid : java.util.Arrays.asList(blankMessage, longMessage, invalidCity)) {
            assertThatThrownBy(() -> controller(Runnable::run, new ManualScheduler())
                    .sendMessage("s1", invalid))
                    .isInstanceOfSatisfying(AigcException.class,
                            error -> assertThat(error.getErrorCode()).isEqualTo(AigcErrorCode.INVALID_REQUEST));
        }
        verifyNoInteractions(sessionService, guard, orchestrator);
    }

    @Test
    void shouldReturnRealBadRequestContractBeforeSessionOrGuardAccess() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller(Runnable::run, new ManualScheduler()))
                .setControllerAdvice(new AigcExceptionAdvice(), new CommonExceptionAdvice())
                .build();

        mockMvc.perform(post("/consumer/assistant/sessions/s1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \",\"cityCode\":\"010\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Processed-Mark", "1"))
                .andExpect(jsonPath("$.code").value("AIGC_INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("AIGC_INVALID_REQUEST"))
                .andExpect(jsonPath("$.retryable").value(false));

        verifyNoInteractions(sessionService, guard, orchestrator);
    }

    @Test
    void shouldLoadOwnedSessionBeforeGuardAndNotScheduleWhenGuardRejects() {
        ManualScheduler scheduler = new ManualScheduler();
        AigcException conflict = new AigcException(AigcErrorCode.GENERATION_CONFLICT);
        when(guard.acquire(7L, "s1")).thenThrow(conflict);

        assertThatThrownBy(() -> controller(Runnable::run, scheduler).sendMessage("s1", request()))
                .isSameAs(conflict);
        inOrder(sessionService, guard).verify(sessionService).loadOwned(7L, "s1");
        inOrder(sessionService, guard).verify(guard).acquire(7L, "s1");
        assertThat(scheduler.tasks).isEmpty();
        verifyNoInteractions(orchestrator);
    }

    @Test
    void shouldReturnNinetyFiveSecondEmitterAndUsePreloadedSession() {
        ManualScheduler scheduler = new ManualScheduler();
        doAnswer(invocation -> {
            invocation.<SseEventSink>getArgument(3)
                    .done(ConversationStage.CLARIFYING, Collections.emptyList());
            return null;
        }).when(orchestrator).run(same(session), eq("010"), eq("保洁"), any(), any(), anyLong());

        SseEmitter emitter = controller(Runnable::run, scheduler).sendMessage("s1", request());

        assertThat(emitter.getTimeout()).isEqualTo(95_000L);
        assertThat(scheduler.delays()).containsExactly(30L, 90L);
        verify(orchestrator).run(same(session), eq("010"), eq("保洁"), any(), any(), anyLong());
        verify(lease).close();
    }

    @Test
    void shouldCancelTokenDirectlyWhenSinkTerminatesWithoutEmitterCallback() {
        ManualScheduler scheduler = new ManualScheduler();
        doAnswer(invocation -> {
            CancellationToken token = invocation.getArgument(4);
            invocation.<SseEventSink>getArgument(3)
                    .done(ConversationStage.CLARIFYING, Collections.emptyList());
            assertThat(token.isCancelled()).isTrue();
            return null;
        }).when(orchestrator).run(same(session), anyString(), anyString(), any(), any(), anyLong());

        controller(Runnable::run, scheduler).sendMessage("s1", request());

        verify(lease).close();
    }

    @Test
    void shouldCancelOnlyFirstTokenTimerOnFirstNonEmptyDeltaThenCleanTotalOnDone() {
        ManualScheduler scheduler = new ManualScheduler();
        doAnswer(invocation -> {
            SseEventSink sink = invocation.getArgument(3);
            ManualFuture<?> first = scheduler.future(30L);
            ManualFuture<?> total = scheduler.future(90L);
            assertThat(first.cancelled).isFalse();
            assertThat(total.cancelled).isFalse();
            sink.delta("");
            assertThat(first.cancelled).isFalse();
            sink.delta("找到服务");
            assertThat(first.cancelled).isTrue();
            assertThat(total.cancelled).isFalse();
            sink.delta("继续说明");
            sink.done(ConversationStage.RECOMMENDING, Collections.emptyList());
            assertThat(total.cancelled).isTrue();
            return null;
        }).when(orchestrator).run(same(session), anyString(), anyString(), any(), any(), anyLong());

        controller(Runnable::run, scheduler).sendMessage("s1", request());

        verify(lease, times(1)).close();
    }

    @Test
    void shouldCancelTokenAndEndStreamWhenFirstTokenTimerFires() {
        ManualScheduler scheduler = new ManualScheduler();
        QueuedExecutor executor = new QueuedExecutor();
        doAnswer(invocation -> {
            assertThat(invocation.<CancellationToken>getArgument(4).isCancelled()).isTrue();
            invocation.<SseEventSink>getArgument(3).delta("ignored");
            return null;
        }).when(orchestrator).run(same(session), anyString(), anyString(), any(), any(), anyLong());

        controller(executor, scheduler).sendMessage("s1", request());
        scheduler.runDelay(30L);

        assertThat(scheduler.future(30L).cancelled).isTrue();
        assertThat(scheduler.future(90L).cancelled).isTrue();
        verify(lease).close();
        executor.runQueued();
    }

    @Test
    void shouldKeepTotalTimerAfterFirstDeltaAndCancelTokenAtNinetySeconds() {
        ManualScheduler scheduler = new ManualScheduler();
        doAnswer(invocation -> {
            SseEventSink sink = invocation.getArgument(3);
            CancellationToken token = invocation.getArgument(4);
            sink.delta("首个输出");
            assertThat(scheduler.future(30L).cancelled).isTrue();
            assertThat(scheduler.future(90L).cancelled).isFalse();
            scheduler.runDelay(90L);
            assertThat(token.isCancelled()).isTrue();
            return null;
        }).when(orchestrator).run(same(session), anyString(), anyString(), any(), any(), anyLong());

        controller(Runnable::run, scheduler).sendMessage("s1", request());

        assertThat(scheduler.future(90L).cancelled).isTrue();
        verify(lease).close();
    }

    @Test
    void shouldCancelFutureAssignedAfterImmediateTerminalRace() {
        ManualScheduler scheduler = new ManualScheduler();
        scheduler.runDuringScheduleDelay = 30L;
        QueuedExecutor executor = new QueuedExecutor();

        controller(executor, scheduler).sendMessage("s1", request());

        assertThat(scheduler.future(30L).cancelled).isTrue();
        assertThat(scheduler.future(90L).cancelled).isTrue();
        verify(lease).close();
    }

    @Test
    void shouldRetryLeaseCleanupAfterCloseFailureWithoutBreakingDone() {
        ManualScheduler scheduler = new ManualScheduler();
        doThrow(new RuntimeException("Redis unavailable")).doNothing().when(lease).close();
        doAnswer(invocation -> {
            invocation.<SseEventSink>getArgument(3)
                    .done(ConversationStage.RECOMMENDING, Collections.emptyList());
            return null;
        }).when(orchestrator).run(same(session), anyString(), anyString(), any(), any(), anyLong());

        assertThatCode(() -> controller(Runnable::run, scheduler).sendMessage("s1", request()))
                .doesNotThrowAnyException();

        verify(lease, times(2)).close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCleanupTimersAndLeaseOnClientCompletionAndDisconnect() {
        assertTransportCallbackCleans("completionCallback", callback -> ((Runnable) callback).run());
        assertTransportCallbackCleans("errorCallback",
                callback -> ((Consumer<Throwable>) callback).accept(new IOException("disconnected")));
    }

    @Test
    void shouldTimeoutAndCleanupFromEmitterTimeoutCallback() {
        ManualScheduler scheduler = new ManualScheduler();
        QueuedExecutor executor = new QueuedExecutor();
        doAnswer(invocation -> {
            assertThat(invocation.<CancellationToken>getArgument(4).isCancelled()).isTrue();
            return null;
        }).when(orchestrator).run(same(session), anyString(), anyString(), any(), any(), anyLong());
        SseEmitter emitter = controller(executor, scheduler).sendMessage("s1", request());

        ((Runnable) callback(emitter, "timeoutCallback")).run();

        assertThat(scheduler.future(30L).cancelled).isTrue();
        assertThat(scheduler.future(90L).cancelled).isTrue();
        verify(lease).close();
        executor.runQueued();
    }

    @Test
    void shouldNotStartOrSaveAfterFirstTokenTimeout() {
        ManualScheduler scheduler = new ManualScheduler();
        QueuedExecutor executor = new QueuedExecutor();
        DemandUnderstandingService understanding = mock(DemandUnderstandingService.class);
        ServiceCatalogService catalog = mock(ServiceCatalogService.class);
        CandidateSelectionService selection = mock(CandidateSelectionService.class);
        ReplyGenerationService replyGenerator = mock(ReplyGenerationService.class);
        AssistantOrchestrator realOrchestrator = new AssistantOrchestrator(
                sessionService, understanding, catalog, selection, replyGenerator, properties,
                mock(com.jzo2o.aigc.observability.AigcObservationLogger.class));

        controller(executor, scheduler, realOrchestrator).sendMessage("s1", request());
        scheduler.runDelay(30L);
        executor.runQueued();

        verifyNoInteractions(understanding, catalog, selection, replyGenerator);
        verify(sessionService, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldNotStartOrSaveAfterClientDisconnect() {
        ManualScheduler scheduler = new ManualScheduler();
        QueuedExecutor executor = new QueuedExecutor();
        DemandUnderstandingService understanding = mock(DemandUnderstandingService.class);
        ServiceCatalogService catalog = mock(ServiceCatalogService.class);
        CandidateSelectionService selection = mock(CandidateSelectionService.class);
        ReplyGenerationService replyGenerator = mock(ReplyGenerationService.class);
        AssistantOrchestrator realOrchestrator = new AssistantOrchestrator(
                sessionService, understanding, catalog, selection, replyGenerator, properties,
                mock(com.jzo2o.aigc.observability.AigcObservationLogger.class));
        SseEmitter emitter = controller(executor, scheduler, realOrchestrator).sendMessage("s1", request());

        ((Consumer<Throwable>) callback(emitter, "errorCallback"))
                .accept(new IOException("disconnected"));
        executor.runQueued();

        verifyNoInteractions(understanding, catalog, selection, replyGenerator);
        verify(sessionService, never()).save(any());
    }

    @Test
    void shouldEmitExactEventsIgnoreEmptyDeltaAndAllowOnlyOneTerminalEvent() {
        RecordingEmitter emitter = new RecordingEmitter();
        AtomicInteger firstDelta = new AtomicInteger();
        AtomicInteger terminal = new AtomicInteger();
        SseEmitterEventSink sink = new SseEmitterEventSink(
                emitter, firstDelta::incrementAndGet, terminal::incrementAndGet);
        RecommendationCardDTO card = RecommendationCardDTO.builder()
                .serveId(1L).serveItemName("日常保洁").price(new BigDecimal("99.00"))
                .priceUnit("次").actionType("SERVICE_DETAIL").build();

        sink.status("UNDERSTANDING");
        sink.delta("");
        sink.delta("第一段");
        sink.delta("第二段");
        sink.recommendations(Collections.singletonList(card));
        sink.done(ConversationStage.RECOMMENDING,
                java.util.Arrays.asList("一", "二", "三", "四"));
        sink.error(AigcErrorCode.MODEL_UNAVAILABLE, "ignored");

        assertThat(emitter.names()).containsExactly("status", "delta", "delta", "recommendations", "done");
        assertThat(emitter.event("status").payload).isEqualTo(Map.of("stage", "UNDERSTANDING"));
        assertThat(emitter.event("delta").payload).isEqualTo(Map.of("text", "第一段"));
        assertThat(emitter.event("recommendations").payload)
                .isEqualTo(Collections.singletonList(card));
        Map<?, ?> done = (Map<?, ?>) emitter.event("done").payload;
        assertThat(done.get("stage")).isEqualTo("RECOMMENDING");
        assertThat(done.get("suggestedQuestions")).isEqualTo(java.util.Arrays.asList("一", "二", "三"));
        assertThat(firstDelta).hasValue(1);
        assertThat(terminal).hasValue(1);
        assertThat(emitter.completed).isTrue();
    }

    @Test
    void shouldSendStableErrorShapeOnce() {
        RecordingEmitter emitter = new RecordingEmitter();
        AtomicInteger terminal = new AtomicInteger();
        SseEmitterEventSink sink = new SseEmitterEventSink(emitter, () -> { }, terminal::incrementAndGet);

        sink.error(AigcErrorCode.MODEL_UNAVAILABLE, "模型暂时不可用");
        sink.done(ConversationStage.NO_MATCH, Collections.emptyList());

        assertThat(emitter.names()).containsExactly("error");
        Map<?, ?> error = (Map<?, ?>) emitter.event("error").payload;
        assertThat(error.get("code")).isEqualTo("AIGC_MODEL_UNAVAILABLE");
        assertThat(error.get("message")).isEqualTo("模型暂时不可用");
        assertThat(error.get("retryable")).isEqualTo(true);
        assertThat(terminal).hasValue(1);
    }

    @Test
    void shouldCompleteWithErrorAndCleanupWhenSseSendFails() {
        RecordingEmitter emitter = new RecordingEmitter();
        emitter.failSend = true;
        AtomicInteger terminal = new AtomicInteger();
        SseEmitterEventSink sink = new SseEmitterEventSink(emitter, () -> { }, terminal::incrementAndGet);

        sink.status("UNDERSTANDING");
        sink.delta("ignored");

        assertThat(emitter.failure).isInstanceOf(IOException.class);
        assertThat(terminal).hasValue(1);
        assertThat(emitter.events).isEmpty();
    }

    private void assertTransportCallbackCleans(String callbackField, Consumer<Object> fire) {
        ManualScheduler scheduler = new ManualScheduler();
        QueuedExecutor executor = new QueuedExecutor();
        GenerationLease callbackLease = mock(GenerationLease.class);
        when(guard.acquire(7L, "s1")).thenReturn(callbackLease);
        SseEmitter emitter = controller(executor, scheduler).sendMessage("s1", request());

        fire.accept(callback(emitter, callbackField));

        assertThat(scheduler.future(30L).cancelled).isTrue();
        assertThat(scheduler.future(90L).cancelled).isTrue();
        verify(callbackLease).close();
    }

    private AssistantMessageReqDTO request() {
        AssistantMessageReqDTO request = new AssistantMessageReqDTO();
        request.setMessage("保洁");
        request.setCityCode("010");
        return request;
    }

    private Object callback(SseEmitter emitter, String fieldName) {
        Class<?> type = emitter.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(emitter);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException error) {
                throw new AssertionError(error);
            }
        }
        throw new AssertionError("No emitter callback field " + fieldName);
    }

    private AiAssistantController controller(Executor executor, ScheduledExecutorService scheduler) {
        return controller(executor, scheduler, orchestrator);
    }

    private AiAssistantController controller(Executor executor,
                                             ScheduledExecutorService scheduler,
                                             AssistantOrchestrator targetOrchestrator) {
        return new AiAssistantController(userInfoHandler, sessionService, guard, targetOrchestrator,
                executor, scheduler, properties);
    }

    private static final class QueuedExecutor implements Executor {
        private Runnable queued;

        @Override
        public void execute(Runnable command) {
            this.queued = command;
        }

        void runQueued() {
            queued.run();
        }
    }

    private static final class ManualScheduler extends AbstractExecutorService
            implements ScheduledExecutorService {
        private final List<ManualTask<?>> tasks = new ArrayList<>();
        private Long runDuringScheduleDelay;
        private boolean shutdown;

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            ManualTask<Void> task = new ManualTask<>(unit.toSeconds(delay), command, null);
            tasks.add(task);
            if (Long.valueOf(task.delaySeconds).equals(runDuringScheduleDelay)) {
                task.run();
            }
            return task.future;
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            ManualTask<V> task = new ManualTask<>(unit.toSeconds(delay), null, callable);
            tasks.add(task);
            return task.future;
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(
                Runnable command, long initialDelay, long period, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(
                Runnable command, long initialDelay, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return shutdown;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }

        List<Long> delays() {
            List<Long> delays = new ArrayList<>();
            for (ManualTask<?> task : tasks) {
                delays.add(task.delaySeconds);
            }
            return delays;
        }

        ManualFuture<?> future(long delaySeconds) {
            return task(delaySeconds).future;
        }

        void runDelay(long delaySeconds) {
            task(delaySeconds).run();
        }

        private ManualTask<?> task(long delaySeconds) {
            return tasks.stream()
                    .filter(task -> task.delaySeconds == delaySeconds)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No task at " + delaySeconds + " seconds"));
        }
    }

    private static final class ManualTask<V> {
        private final long delaySeconds;
        private final Runnable runnable;
        private final Callable<V> callable;
        private final ManualFuture<V> future;

        private ManualTask(long delaySeconds, Runnable runnable, Callable<V> callable) {
            this.delaySeconds = delaySeconds;
            this.runnable = runnable;
            this.callable = callable;
            this.future = new ManualFuture<>(delaySeconds);
        }

        private void run() {
            if (future.cancelled) {
                return;
            }
            try {
                if (runnable != null) {
                    runnable.run();
                } else {
                    future.value = callable.call();
                }
            } catch (Exception error) {
                future.failure = error;
            } finally {
                future.done = true;
            }
        }
    }

    private static final class ManualFuture<V> implements ScheduledFuture<V> {
        private final long delaySeconds;
        private boolean cancelled;
        private boolean done;
        private V value;
        private Exception failure;

        private ManualFuture(long delaySeconds) {
            this.delaySeconds = delaySeconds;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(delaySeconds, TimeUnit.SECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            return Long.compare(getDelay(TimeUnit.NANOSECONDS), other.getDelay(TimeUnit.NANOSECONDS));
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelled = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return done || cancelled;
        }

        @Override
        public V get() throws ExecutionException {
            if (failure != null) {
                throw new ExecutionException(failure);
            }
            return value;
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws ExecutionException, TimeoutException {
            return get();
        }
    }

    private static final class RecordingEmitter extends SseEmitter {
        private final List<RecordedEvent> events = new ArrayList<>();
        private boolean completed;
        private boolean failSend;
        private Throwable failure;

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (failSend) {
                throw new IOException("client disconnected");
            }
            String name = null;
            Object payload = null;
            Set<ResponseBodyEmitter.DataWithMediaType> parts = builder.build();
            for (ResponseBodyEmitter.DataWithMediaType part : parts) {
                Object data = part.getData();
                if (data instanceof String) {
                    String text = (String) data;
                    int start = text.indexOf("event:");
                    if (start >= 0) {
                        int end = text.indexOf('\n', start);
                        name = text.substring(start + "event:".length(), end);
                    }
                } else {
                    payload = data;
                }
            }
            events.add(new RecordedEvent(name, payload));
        }

        @Override
        public synchronized void complete() {
            completed = true;
        }

        @Override
        public synchronized void completeWithError(Throwable error) {
            failure = error;
        }

        List<String> names() {
            List<String> names = new ArrayList<>();
            for (RecordedEvent event : events) {
                names.add(event.name);
            }
            return names;
        }

        RecordedEvent event(String name) {
            return events.stream().filter(event -> name.equals(event.name)).findFirst()
                    .orElseThrow(() -> new AssertionError("No event " + name));
        }
    }

    private static final class RecordedEvent {
        private final String name;
        private final Object payload;

        private RecordedEvent(String name, Object payload) {
            this.name = name;
            this.payload = payload;
        }
    }
}
