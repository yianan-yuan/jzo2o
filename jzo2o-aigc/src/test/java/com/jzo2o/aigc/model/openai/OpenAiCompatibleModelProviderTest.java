package com.jzo2o.aigc.model.openai;

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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleModelProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Deque<StubResponse> responses = new ArrayDeque<>();
    private final List<CapturedRequest> requests = Collections.synchronizedList(new ArrayList<>());
    private HttpServer server;
    private ExecutorService serverExecutor;
    private CapturingHttpClient httpClient;
    private AigcProperties properties;
    private OpenAiCompatibleModelProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        serverExecutor = Executors.newCachedThreadPool();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(serverExecutor);
        server.createContext("/v1/chat/completions", this::respond);
        server.start();

        properties = new AigcProperties();
        properties.getModel().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/");
        properties.getModel().setApiKey("secret-key");
        httpClient = new CapturingHttpClient(HttpClient.newHttpClient());
        provider = new OpenAiCompatibleModelProvider(httpClient, objectMapper, properties);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        server.stop(0);
        serverExecutor.shutdownNow();
        assertThat(serverExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void completeReturnsFirstChoiceContentAndSendsOpenAiRequest() throws Exception {
        enqueue(200, "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"clean answer\"}}]}");

        String result = provider.complete(
                Collections.singletonList(new ModelMessage("user", "recommend")),
                0.65D,
                new CancellationToken());

        assertThat(result).isEqualTo("clean answer");
        CapturedRequest request = requests.get(0);
        assertThat(request.method).isEqualTo("POST");
        assertThat(request.path).isEqualTo("/v1/chat/completions");
        assertThat(request.contentType).isEqualTo("application/json");
        assertThat(request.authorization).isEqualTo("Bearer secret-key");
        JsonNode body = objectMapper.readTree(request.body);
        assertThat(body.path("model").asText()).isEqualTo("qwen3:0.6b");
        assertThat(body.path("messages").get(0).path("role").asText()).isEqualTo("user");
        assertThat(body.path("messages").get(0).path("content").asText()).isEqualTo("recommend");
        assertThat(body.path("stream").asBoolean()).isFalse();
        assertThat(body.path("temperature").asDouble()).isEqualTo(0.65D);
        assertThat(body.path("max_tokens").asInt()).isEqualTo(1024);
        assertThat(httpClient.requests.get(0).timeout()).contains(Duration.ofSeconds(90));
    }

    @Test
    void streamEmitsTwoSseDeltasStopsAtDoneAndUsesConfiguredTemperature() throws Exception {
        properties.getModel().setTemperature(0.35D);
        properties.getModel().setApiKey("  ");
        enqueue(200,
                "event: message\n"
                        + "\n"
                        + "  data: {\"choices\":[{\"delta\":{\"content\":\"recommend\"}}]}  \n"
                        + "data:{\"choices\":[{\"delta\":{\"content\":\"clean\"}}]}\n"
                        + "data: [DONE]\n"
                        + "data: {\"choices\":[{\"delta\":{\"content\":\"ignored\"}}]}\n");
        List<String> deltas = new ArrayList<>();

        provider.stream(
                Collections.singletonList(new ModelMessage("user", "go")),
                new CancellationToken(),
                deltas::add);

        assertThat(deltas).containsExactly("recommend", "clean");
        CapturedRequest request = requests.get(0);
        assertThat(request.authorization).isNull();
        JsonNode body = objectMapper.readTree(request.body);
        assertThat(body.path("stream").asBoolean()).isTrue();
        assertThat(body.path("temperature").asDouble()).isEqualTo(0.35D);
        assertThat(body.path("max_tokens").asInt()).isEqualTo(1024);
        assertThat(httpClient.requests.get(0).timeout()).contains(Duration.ofSeconds(90));
    }

    @Test
    void streamIgnoresEmptyContentDelta() {
        enqueue(200,
                "data: {\"choices\":[{\"delta\":{\"content\":\"\"}}]}\n"
                        + "data: {\"choices\":[{\"delta\":{\"content\":\"usable\"}}]}\n"
                        + "data: [DONE]\n");
        List<String> deltas = new ArrayList<>();

        provider.stream(Collections.emptyList(), new CancellationToken(), deltas::add);

        assertThat(deltas).containsExactly("usable");
    }

    @Test
    void streamStopsBeforeSecondDeltaWhenFirstCallbackCancels() {
        enqueue(200,
                "data: {\"choices\":[{\"delta\":{\"content\":\"first\"}}]}\n"
                        + "data: {\"choices\":[{\"delta\":{\"content\":\"second\"}}]}\n"
                        + "data: [DONE]\n");
        CancellationToken token = new CancellationToken();
        List<String> deltas = new ArrayList<>();

        provider.stream(Collections.emptyList(), token, delta -> {
            deltas.add(delta);
            token.cancel();
        });

        assertThat(deltas).containsExactly("first");
    }

    @Test
    void completeMapsNonSuccessAndMissingChoicesToModelUnavailable() {
        enqueue(503, "{\"error\":\"offline\"}");
        enqueue(200, "{}");

        assertModelUnavailable(() -> provider.complete(Collections.emptyList(), 0.2D, new CancellationToken()));
        assertModelUnavailable(() -> provider.complete(Collections.emptyList(), 0.2D, new CancellationToken()));
    }

    @Test
    void completeMapsNullNonObjectAndInvalidJsonToModelUnavailable() {
        enqueue(200, "null");
        enqueue(200, "[]");
        enqueue(200, "not-json");

        assertModelUnavailable(() -> provider.complete(Collections.emptyList(), 0.2D, new CancellationToken()));
        assertModelUnavailable(() -> provider.complete(Collections.emptyList(), 0.2D, new CancellationToken()));
        assertModelUnavailable(() -> provider.complete(Collections.emptyList(), 0.2D, new CancellationToken()));
    }

    @Test
    void streamMapsMalformedDataPayloadToModelUnavailable() {
        enqueue(200, "data: {\"choices\":[]}\n");

        assertModelUnavailable(() -> provider.stream(
                Collections.emptyList(), new CancellationToken(), ignored -> { }));
    }

    @Test
    void transportIoFailureMapsToModelUnavailable() {
        provider = new OpenAiCompatibleModelProvider(new FailingHttpClient(), objectMapper, properties);

        assertModelUnavailable(() -> provider.complete(Collections.emptyList(), 0.2D, new CancellationToken()));
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
            requests.add(new CapturedRequest(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    exchange.getRequestHeaders().getFirst("Authorization"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
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

    private static final class CapturedRequest {
        private final String method;
        private final String path;
        private final String contentType;
        private final String authorization;
        private final String body;

        private CapturedRequest(String method, String path, String contentType, String authorization, String body) {
            this.method = method;
            this.path = path;
            this.contentType = contentType;
            this.authorization = authorization;
            this.body = body;
        }
    }

    private static class CapturingHttpClient extends HttpClient {
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

    private static final class FailingHttpClient extends CapturingHttpClient {
        private FailingHttpClient() {
            super(HttpClient.newHttpClient());
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException {
            throw new IOException("transport unavailable");
        }
    }
}
