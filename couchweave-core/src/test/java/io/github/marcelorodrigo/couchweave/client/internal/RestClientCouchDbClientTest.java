package io.github.marcelorodrigo.couchweave.client.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.ResourceAccessException;

class RestClientCouchDbClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("should send JSON requests with configured basic authentication")
    void shouldSendJsonRequestsWithConfiguredBasicAuthentication() throws IOException {
        // given
        var request = new AtomicReference<HttpExchange>();
        startServer(exchange -> {
            request.set(exchange);
            respond(exchange, 201, "{\"ok\":true}");
        });
        var client = client("/base", "admin", "secret", Duration.ofSeconds(1));

        // when
        var response = client.exchange(HttpMethod.POST, List.of("books", "book one"), "{\"title\":\"CouchWeave\"}");

        // then
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).isEqualTo("{\"ok\":true}");
        assertThat(request.get().getRequestMethod()).isEqualTo("POST");
        assertThat(request.get().getRequestURI().getRawPath()).isEqualTo("/base/books/book%20one");
        assertThat(request.get().getRequestHeaders().getFirst("Accept")).contains("application/json");
        assertThat(request.get().getRequestHeaders().getFirst("Content-Type")).contains("application/json");
        assertThat(request.get().getRequestHeaders().getFirst("Authorization")).isEqualTo("Basic YWRtaW46c2VjcmV0");
    }

    @Test
    @DisplayName("should omit authorization when credentials are not configured")
    void shouldOmitAuthorizationWhenCredentialsAreNotConfigured() throws IOException {
        // given
        var request = new AtomicReference<HttpExchange>();
        startServer(exchange -> {
            request.set(exchange);
            respond(exchange, 200, "{\"ok\":true}");
        });
        var client = client("", null, null, Duration.ofSeconds(1));

        // when
        client.exchange(HttpMethod.GET, List.of("books"), null);

        // then
        assertThat(request.get().getRequestHeaders().getFirst("Authorization")).isNull();
    }

    @Test
    @DisplayName("should configure the JDK client connect timeout")
    void shouldConfigureTheJdkClientConnectTimeout() {
        // given
        var settings = new CouchDbClientSettings(
                URI.create("https://couch.example.test"),
                "books",
                null,
                null,
                Duration.ofMillis(125),
                Duration.ofSeconds(1));

        // when
        var httpClient = RestClientCouchDbClient.createHttpClient(settings);

        // then
        assertThat(httpClient.connectTimeout()).contains(Duration.ofMillis(125));
    }

    @Test
    @DisplayName("should apply the configured read timeout to requests")
    void shouldApplyTheConfiguredReadTimeoutToRequests() throws IOException {
        // given
        startServer(exchange -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 200, "{\"ok\":true}");
        });
        var client = client("", "admin", "secret", Duration.ofMillis(50));

        // when / then
        assertThatThrownBy(() -> client.exchange(HttpMethod.GET, List.of("books"), null))
                .isInstanceOf(ResourceAccessException.class)
                .hasMessageNotContaining("secret");
    }

    @Test
    @DisplayName("should safely share one client instance across concurrent requests")
    void shouldSafelyShareOneClientInstanceAcrossConcurrentRequests() throws Exception {
        // given
        startServer(exchange -> respond(exchange, 200, "{\"ok\":true}"));
        var client = client("", null, null, Duration.ofSeconds(1));
        var requests = IntStream.range(0, 20)
                .<Callable<CouchDbResponse>>mapToObj(
                        ignored -> () -> client.exchange(HttpMethod.GET, List.of("books"), null))
                .toList();

        // when
        List<CouchDbResponse> responses;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            responses = executor.invokeAll(requests).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new AssertionError("concurrent request failed", exception);
                        }
                    })
                    .toList();
        }

        // then
        assertThat(responses)
                .hasSize(20)
                .allSatisfy(response -> assertThat(response.statusCode()).isEqualTo(200));
    }

    private RestClientCouchDbClient client(String basePath, String username, String password, Duration readTimeout) {
        var settings = new CouchDbClientSettings(
                URI.create("http://localhost:%d%s".formatted(server.getAddress().getPort(), basePath)),
                "books",
                username,
                password,
                Duration.ofSeconds(1),
                readTimeout);
        return new RestClientCouchDbClient(settings);
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> handler.handle(exchange));
        server.start();
    }

    private void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @FunctionalInterface
    private interface ExchangeHandler {

        void handle(HttpExchange exchange) throws IOException;
    }
}
