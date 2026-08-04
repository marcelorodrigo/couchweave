package io.github.marcelorodrigo.couchweave.client.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.marcelorodrigo.couchweave.client.CouchDbAuthenticationException;
import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import io.github.marcelorodrigo.couchweave.client.CouchDbNotFoundException;
import io.github.marcelorodrigo.couchweave.client.CouchDbResponseException;
import io.github.marcelorodrigo.couchweave.client.CouchOptimisticLockingFailureException;
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
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

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
        var pathSegments = List.of("books");

        // when / then
        assertThatThrownBy(() -> client.exchange(HttpMethod.GET, pathSegments, null))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessageNotContaining("secret");
    }

    @Test
    @DisplayName("should translate a connection failure and preserve its transport cause")
    void shouldTranslateAConnectionFailureAndPreserveItsTransportCause() throws IOException {
        // given
        startServer(exchange -> respond(exchange, 200, "{\"ok\":true}"));
        var unavailablePort = server.getAddress().getPort();
        server.stop(0);
        server = null;
        var settings = new CouchDbClientSettings(
                URI.create("http://localhost:" + unavailablePort),
                "books",
                "admin",
                "secret",
                Duration.ofSeconds(1),
                Duration.ofSeconds(1));
        var client = new RestClientCouchDbClient(settings);
        var pathSegments = List.of("books");

        // when / then
        assertThatThrownBy(() -> client.exchange(HttpMethod.GET, pathSegments, null))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasCauseInstanceOf(ResourceAccessException.class)
                .hasMessageNotContaining("secret");
    }

    @ParameterizedTest
    @MethodSource("statusTranslations")
    @DisplayName("should translate HTTP failures through the transport boundary")
    void shouldTranslateHttpFailuresThroughTheTransportBoundary(
            int statusCode, Class<? extends DataAccessException> expectedType) throws IOException {
        // given
        startServer(exchange -> respond(
                exchange, statusCode, "{\"error\":\"request_failed\",\"reason\":\"CouchDB rejected the request\"}"));
        var client = client("", null, null, Duration.ofSeconds(1));
        var context = CouchDbRequestContext.forDocument("books", "book-42", "3-stale");
        var pathSegments = List.of("books", "book-42");

        // when / then
        assertThatThrownBy(() -> client.exchange(HttpMethod.PUT, pathSegments, "{}", context))
                .isInstanceOf(expectedType)
                .hasMessageContaining("book-42");
    }

    @Test
    @DisplayName("should translate malformed CouchDB error responses through the transport boundary")
    void shouldTranslateMalformedCouchDbErrorResponsesThroughTheTransportBoundary() throws IOException {
        // given
        startServer(exchange -> respond(exchange, 502, "not-json"));
        var client = client("", null, null, Duration.ofSeconds(1));
        var pathSegments = List.of("books");

        // when / then
        assertThatThrownBy(() -> client.exchange(HttpMethod.GET, pathSegments, null))
                .isInstanceOf(CouchDbResponseException.class)
                .hasMessageContaining("unreadable error response")
                .hasMessageNotContaining("not-json");
    }

    @Test
    @DisplayName("should put a raw document and return the server revision")
    void shouldPutARawDocumentAndReturnTheServerRevision() throws IOException, JacksonException {
        // given
        var request = new AtomicReference<HttpExchange>();
        var requestBody = new AtomicReference<String>();
        startServer(exchange -> {
            request.set(exchange);
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 201, "{\"ok\":true,\"id\":\"book/a ü\",\"rev\":\"1-created\"}");
        });
        var client = client("", null, null, Duration.ofSeconds(1));
        var document = new ObjectMapper().readTree("{\"_id\":\"book/a ü\",\"title\":\"CouchWeave\"}");

        // when
        var result = client.putDocument("books", "book/a ü", document);

        // then
        assertThat(result).isEqualTo(new CouchDbWriteResult("book/a ü", "1-created"));
        assertThat(request.get().getRequestMethod()).isEqualTo("PUT");
        assertThat(request.get().getRequestURI().getRawPath()).isEqualTo("/books/book%2Fa%20%C3%BC");
        assertThat(requestBody.get()).isEqualTo("{\"_id\":\"book/a ü\",\"title\":\"CouchWeave\"}");
    }

    @Test
    @DisplayName("should fetch a raw document by ID")
    void shouldFetchARawDocumentById() throws IOException {
        // given
        startServer(exchange ->
                respond(exchange, 200, "{\"_id\":\"book-42\",\"_rev\":\"1-created\",\"title\":\"CouchWeave\"}"));
        var client = client("", null, null, Duration.ofSeconds(1));

        // when
        var document = client.getDocument("books", "book-42");

        // then
        assertThat(document)
                .isPresent()
                .get()
                .extracting(value -> value.get("title").stringValue())
                .isEqualTo("CouchWeave");
    }

    @Test
    @DisplayName("should return an empty result when a document is not found")
    void shouldReturnAnEmptyResultWhenADocumentIsNotFound() throws IOException {
        // given
        startServer(exchange -> respond(exchange, 404, "{\"error\":\"not_found\",\"reason\":\"missing\"}"));
        var client = client("", null, null, Duration.ofSeconds(1));

        // when
        var document = client.getDocument("books", "missing");

        // then
        assertThat(document).isEmpty();
    }

    @Test
    @DisplayName("should check document existence without reading its body")
    void shouldCheckDocumentExistenceWithoutReadingItsBody() throws IOException {
        // given
        var method = new AtomicReference<String>();
        startServer(exchange -> {
            method.set(exchange.getRequestMethod());
            respondWithoutBody(exchange, 200);
        });
        var client = client("", null, null, Duration.ofSeconds(1));

        // when
        var exists = client.documentExists("books", "book-42");

        // then
        assertThat(exists).isTrue();
        assertThat(method.get()).isEqualTo("HEAD");
    }

    @Test
    @DisplayName("should report that a missing document does not exist")
    void shouldReportThatAMissingDocumentDoesNotExist() throws IOException {
        // given
        startServer(exchange -> respondWithoutBody(exchange, 404));
        var client = client("", null, null, Duration.ofSeconds(1));

        // when
        var exists = client.documentExists("books", "missing");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("should delete a document with its encoded revision")
    void shouldDeleteADocumentWithItsEncodedRevision() throws IOException {
        // given
        var request = new AtomicReference<HttpExchange>();
        startServer(exchange -> {
            request.set(exchange);
            respond(exchange, 200, "{\"ok\":true,\"id\":\"book/a\",\"rev\":\"3-deleted\"}");
        });
        var client = client("", null, null, Duration.ofSeconds(1));

        // when
        var result = client.deleteDocument("books", "book/a", "2-current/value");

        // then
        assertThat(result).isEqualTo(new CouchDbWriteResult("book/a", "3-deleted"));
        assertThat(request.get().getRequestMethod()).isEqualTo("DELETE");
        assertThat(request.get().getRequestURI().getRawPath()).isEqualTo("/books/book%2Fa");
        assertThat(request.get().getRequestURI().getRawQuery()).isEqualTo("rev=2-current/value");
    }

    @Test
    @DisplayName("should include the attempted revision when a document write conflicts")
    void shouldIncludeTheAttemptedRevisionWhenADocumentWriteConflicts() throws IOException, JacksonException {
        // given
        startServer(exchange ->
                respond(exchange, 409, "{\"error\":\"conflict\",\"reason\":\"Document update conflict.\"}"));
        var client = client("", null, null, Duration.ofSeconds(1));
        var document = new ObjectMapper().readTree("{\"_id\":\"book-42\",\"_rev\":\"1-stale\"}");

        // when / then
        assertThatThrownBy(() -> client.putDocument("books", "book-42", document))
                .isInstanceOf(CouchOptimisticLockingFailureException.class)
                .hasMessageContaining("1-stale");
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

    private void respondWithoutBody(HttpExchange exchange, int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, -1);
        exchange.close();
    }

    private static Stream<Arguments> statusTranslations() {
        return Stream.of(
                Arguments.of(400, InvalidDataAccessResourceUsageException.class),
                Arguments.of(401, CouchDbAuthenticationException.class),
                Arguments.of(403, PermissionDeniedDataAccessException.class),
                Arguments.of(404, CouchDbNotFoundException.class),
                Arguments.of(409, CouchOptimisticLockingFailureException.class),
                Arguments.of(500, CouchDbResponseException.class));
    }

    @FunctionalInterface
    private interface ExchangeHandler {

        void handle(HttpExchange exchange) throws IOException;
    }
}
