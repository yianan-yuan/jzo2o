package com.jzo2o.aigc.model.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jzo2o.aigc.exception.AigcErrorCode;
import com.jzo2o.aigc.exception.AigcException;
import com.jzo2o.aigc.model.CancellationToken;
import com.jzo2o.aigc.model.ModelMessage;
import com.jzo2o.aigc.properties.AigcProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class OllamaModelProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Deque<StubResponse> responses = new ArrayDeque<>();
    private final List<String> requestBodies = Collections.synchronizedList(new ArrayList<>());
    private HttpServer server;
    private ExecutorService serverExecutor;
    private CapturingHttpClient httpClient;
    private AigcProperties properties;
    private OllamaModelProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        serverExecutor = Executors.newCachedThreadPool();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(serverExecutor);
        server.createContext("/api/chat", this::respond);
        server.start();

        properties = new AigcProperties();
        properties.getModel().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        httpClient = new CapturingHttpClient(HttpClient.newHttpClient());
        provider = new OllamaModelProvider(httpClient, objectMapper, properties);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        server.stop(0);
        serverExecutor.shutdownNow();
        assertThat(serverExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void completeReturnsAssistantContentAndSendsConfiguredRequest() throws Exception {
        enqueue(200, "{\"message\":{\"role\":\"assistant\",\"content\":\"{\\\"summary\\\":\\\"保洁\\\"}\"},\"done\":true}");

        String result = provider.complete(
                Collections.singletonList(new ModelMessage("user", "需要日常保洁")),
                0.65D,
                new CancellationToken());

        assertThat(result).isEqualTo("{\"summary\":\"保洁\"}");
        JsonNode body = objectMapper.readTree(requestBodies.get(0));
        assertThat(body.path("model").asText()).isEqualTo("qwen3:0.6b");
        assertThat(body.path("messages").get(0).path("role").asText()).isEqualTo("user");
        assertThat(body.path("messages").get(0).path("content").asText()).isEqualTo("需要日常保洁");
        assertThat(body.path("stream").asBoolean()).isFalse();
        assertThat(body.path("options").path("temperature").asDouble()).isEqualTo(0.65D);
        assertThat(body.path("options").path("num_predict").asInt()).isEqualTo(1024);
        assertThat(httpClient.requests.get(0).timeout()).contains(Duration.ofSeconds(90));
    }

    @Test
    void streamEmitsEachNdjsonDeltaAndUsesConfiguredDefaults() throws Exception {
        enqueue(200,
                "{\"message\":{\"content\":\"适合\"},\"done\":false}\n"
                        + "{\"message\":{\"content\":\"日常清洁\"},\"done\":true}\n");
        List<String> deltas = new ArrayList<>();

        provider.stream(
                Collections.singletonList(new ModelMessage("user", "推荐")),
                new CancellationToken(),
                deltas::add);

        assertThat(deltas).containsExactly("适合", "日常清洁");
        JsonNode body = objectMapper.readTree(requestBodies.get(0));
        assertThat(body.path("stream").asBoolean()).isTrue();
        assertThat(body.path("options").path("temperature").asDouble()).isEqualTo(0.2D);
        assertThat(body.path("options").path("num_predict").asInt()).isEqualTo(1024);
        assertThat(httpClient.requests.get(0).timeout()).contains(Duration.ofSeconds(90));
    }

    @Test
    void streamStopsBeforeSecondDeltaWhenFirstCallbackCancels() {
        enqueue(200,
                "{\"message\":{\"content\":\"first\"},\"done\":false}\n"
                        + "{\"message\":{\"content\":\"second\"},\"done\":true}\n");
        CancellationToken token = new CancellationToken();
        List<String> deltas = new ArrayList<>();

        provider.stream(Collections.singletonList(new ModelMessage("user", "go")), token, delta -> {
            deltas.add(delta);
            token.cancel();
        });

        assertThat(deltas).containsExactly("first");
    }

    @Test
    void mapsNonSuccessAndMissingMessageToModelUnavailable() {
        enqueue(503, "{\"error\":\"offline\"}");
        enqueue(200, "{\"done\":true}");

        assertModelUnavailable(() -> provider.complete(Collections.emptyList(), 0.2D, new CancellationToken()));
        assertModelUnavailable(() -> provider.complete(Collections.emptyList(), 0.2D, new CancellationToken()));
    }

    @Test
    void completeMapsBlankJsonToModelUnavailable() {
        enqueue(200, "   ");

        assertModelUnavailable(() -> provider.complete(Collections.emptyList(), 0.2D, new CancellationToken()));
    }

    @Test
    void completeMapsNullParsedRootToModelUnavailable() {
        enqueue(200, "{}");
        provider = new OllamaModelProvider(httpClient, new NullRootObjectMapper(), properties);

        assertModelUnavailable(() -> provider.complete(Collections.emptyList(), 0.2D, new CancellationToken()));
    }

    @Test
    void streamMapsJsonNullToModelUnavailable() {
        enqueue(200, "null\n");

        assertModelUnavailable(() -> provider.stream(
                Collections.emptyList(), new CancellationToken(), ignored -> { }));
    }

    @Test
    void streamMapsMissingContentToModelUnavailable() {
        enqueue(200, "{\"message\":{},\"done\":true}\n");

        assertModelUnavailable(() -> provider.stream(
                Collections.emptyList(), new CancellationToken(), ignored -> { }));
    }

    @Test
    void streamIgnoresWhitespaceOnlyLines() {
        enqueue(200,
                "  \t  \n"
                        + "{\"message\":{\"content\":\"usable\"},\"done\":true}\n");
        List<String> deltas = new ArrayList<>();

        provider.stream(Collections.emptyList(), new CancellationToken(), deltas::add);

        assertThat(deltas).containsExactly("usable");
    }

    @Test
    void cancellationCallbacksRunOnceAndLateRegistrationRunsImmediately() {
        CancellationToken token = new CancellationToken();
        AtomicInteger first = new AtomicInteger();
        AtomicInteger late = new AtomicInteger();
        token.onCancel(first::incrementAndGet);

        token.cancel();
        token.cancel();
        token.onCancel(late::incrementAndGet);

        assertThat(token.isCancelled()).isTrue();
        assertThat(first).hasValue(1);
        assertThat(late).hasValue(1);
    }

    @Test
    void cancellationDrainsCallbacksBeforePropagatingFirstFailure() {
        CancellationToken token = new CancellationToken();
        RuntimeException firstFailure = new RuntimeException("first callback failed");
        AtomicInteger cleanupCalls = new AtomicInteger();
        token.onCancel(() -> {
            throw firstFailure;
        });
        token.onCancel(cleanupCalls::incrementAndGet);

        RuntimeException thrown = catchThrowableOfType(token::cancel, RuntimeException.class);

        assertThat(thrown).isSameAs(firstFailure);
        assertThat(cleanupCalls).hasValue(1);
        token.cancel();
        assertThat(cleanupCalls).hasValue(1);
    }

    private void assertModelUnavailable(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(AigcException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(AigcErrorCode.MODEL_UNAVAILABLE));
    }

    private void enqueue(int status, String body) {
        synchronized (responses) {
            responses.addLast(new StubResponse(status, body));
        }
    }

    private void respond(HttpExchange exchange) throws IOException {
        try {
            requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            StubResponse response;
            synchronized (responses) {
                response = responses.removeFirst();
            }
            byte[] bytes = response.body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(response.status, bytes.length);
            exchange.getResponseBody().write(bytes);
        } finally {
            exchange.close();
        }
    }

    private static final class StubResponse {
        private final int status;
        private final String body;

        private StubResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    private static final class NullRootObjectMapper extends ObjectMapper {
        @Override
        public JsonNode readTree(java.io.InputStream input) {
            return null;
        }
    }

    private static final class CapturingHttpClient extends HttpClient {
        private final HttpClient delegate;
        private final List<HttpRequest> requests = Collections.synchronizedList(new ArrayList<>());

        private CapturingHttpClient(HttpClient delegate) {
            this.delegate = delegate;
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return delegate.cookieHandler();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return delegate.connectTimeout();
        }

        @Override
        public Redirect followRedirects() {
            return delegate.followRedirects();
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return delegate.proxy();
        }

        @Override
        public SSLContext sslContext() {
            return delegate.sslContext();
        }

        @Override
        public SSLParameters sslParameters() {
            return delegate.sslParameters();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return delegate.authenticator();
        }

        @Override
        public Version version() {
            return delegate.version();
        }

        @Override
        public Optional<Executor> executor() {
            return delegate.executor();
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException, InterruptedException {
            requests.add(request);
            return delegate.send(request, responseBodyHandler);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            requests.add(request);
            return delegate.sendAsync(request, responseBodyHandler);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            requests.add(request);
            return delegate.sendAsync(request, responseBodyHandler, pushPromiseHandler);
        }
    }
}
