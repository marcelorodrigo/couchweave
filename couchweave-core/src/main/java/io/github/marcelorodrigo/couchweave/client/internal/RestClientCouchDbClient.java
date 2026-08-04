package io.github.marcelorodrigo.couchweave.client.internal;

import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import io.github.marcelorodrigo.couchweave.client.CouchDbNotFoundException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

final class RestClientCouchDbClient implements CouchDbClient {

    private static final String DATABASE_PARAMETER = "database";
    private static final String DOCUMENT_ID_PARAMETER = "documentId";

    private final RestClient restClient;
    private final CouchDbUriBuilder uriBuilder;
    private final CouchDbFailureTranslator failureTranslator;
    private final CouchDbResponseDecoder responseDecoder;
    private final String defaultDatabase;

    RestClientCouchDbClient(CouchDbClientSettings settings) {
        this(settings, createRestClient(settings), new CouchDbFailureTranslator(settings));
    }

    RestClientCouchDbClient(CouchDbClientSettings settings, RestClient restClient) {
        this(settings, restClient, new CouchDbFailureTranslator(settings));
    }

    RestClientCouchDbClient(
            CouchDbClientSettings settings, RestClient restClient, CouchDbFailureTranslator failureTranslator) {
        this(settings, restClient, failureTranslator, new ObjectMapper());
    }

    RestClientCouchDbClient(
            CouchDbClientSettings settings,
            RestClient restClient,
            CouchDbFailureTranslator failureTranslator,
            ObjectMapper objectMapper) {
        this.uriBuilder = new CouchDbUriBuilder(settings);
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
        this.failureTranslator = Objects.requireNonNull(failureTranslator, "failureTranslator must not be null");
        this.responseDecoder = new CouchDbResponseDecoder(objectMapper);
        this.defaultDatabase = settings.database();
    }

    @Override
    public CouchDbWriteResult putDocument(String database, String documentId, JsonNode document) {
        requireText(database, DATABASE_PARAMETER);
        requireText(documentId, DOCUMENT_ID_PARAMETER);
        Objects.requireNonNull(document, "document must not be null");
        if (!document.isObject()) {
            throw new IllegalArgumentException("document must be a JSON object");
        }
        var revision = textValue(document, "_rev");
        var context = CouchDbRequestContext.forDocument(database, documentId, revision);
        var response = exchange(HttpMethod.PUT, List.of(database, documentId), Map.of(), document.toString(), context);
        return responseDecoder.decodeWriteResult(response, context);
    }

    @Override
    public Optional<JsonNode> getDocument(String database, String documentId) {
        requireText(database, DATABASE_PARAMETER);
        requireText(documentId, DOCUMENT_ID_PARAMETER);
        var context = CouchDbRequestContext.forDocument(database, documentId, null);
        try {
            var response = exchange(HttpMethod.GET, List.of(database, documentId), Map.of(), null, context);
            return Optional.of(responseDecoder.decodeDocument(response, context));
        } catch (CouchDbNotFoundException exception) {
            return Optional.empty();
        }
    }

    @Override
    public boolean documentExists(String database, String documentId) {
        requireText(database, DATABASE_PARAMETER);
        requireText(documentId, DOCUMENT_ID_PARAMETER);
        var context = CouchDbRequestContext.forDocument(database, documentId, null);
        try {
            exchange(HttpMethod.HEAD, List.of(database, documentId), Map.of(), null, context);
            return true;
        } catch (CouchDbNotFoundException exception) {
            return false;
        }
    }

    @Override
    public CouchDbWriteResult deleteDocument(String database, String documentId, String revision) {
        requireText(database, DATABASE_PARAMETER);
        requireText(documentId, DOCUMENT_ID_PARAMETER);
        requireText(revision, "revision");
        var context = CouchDbRequestContext.forDocument(database, documentId, revision);
        var response =
                exchange(HttpMethod.DELETE, List.of(database, documentId), Map.of("rev", revision), null, context);
        return responseDecoder.decodeWriteResult(response, context);
    }

    CouchDbResponse exchange(HttpMethod method, List<String> pathSegments, String body) {
        return exchange(method, pathSegments, body, CouchDbRequestContext.fromPath(defaultDatabase, pathSegments));
    }

    CouchDbResponse exchange(HttpMethod method, List<String> pathSegments, String body, CouchDbRequestContext context) {
        return exchange(method, pathSegments, Map.of(), body, context);
    }

    private CouchDbResponse exchange(
            HttpMethod method,
            List<String> pathSegments,
            Map<String, String> queryParameters,
            String body,
            CouchDbRequestContext context) {
        Objects.requireNonNull(method, "method must not be null");
        var request = restClient
                .method(method)
                .uri(uriBuilder.build(pathSegments, queryParameters))
                .accept(MediaType.APPLICATION_JSON);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        try {
            var response = request.exchange((ignored, clientResponse) -> new CouchDbResponse(
                    clientResponse.getStatusCode().value(),
                    clientResponse.getHeaders(),
                    new String(clientResponse.getBody().readAllBytes(), StandardCharsets.UTF_8)));
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                throw failureTranslator.translate(response, context);
            }
            return response;
        } catch (ResourceAccessException exception) {
            throw failureTranslator.translate(exception, context);
        }
    }

    static RestClient createRestClient(CouchDbClientSettings settings) {
        Objects.requireNonNull(settings, "settings must not be null");
        var builder = RestClient.builder()
                .baseUrl(settings.serverUri())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(createRequestFactory(settings));
        if (settings.hasCredentials()) {
            builder.defaultHeaders(
                    headers -> headers.setBasicAuth(settings.username(), settings.password(), StandardCharsets.UTF_8));
        }
        return builder.build();
    }

    static JdkClientHttpRequestFactory createRequestFactory(CouchDbClientSettings settings) {
        var requestFactory = new JdkClientHttpRequestFactory(createHttpClient(settings));
        requestFactory.setReadTimeout(settings.readTimeout());
        return requestFactory;
    }

    static HttpClient createHttpClient(CouchDbClientSettings settings) {
        return HttpClient.newBuilder().connectTimeout(settings.connectTimeout()).build();
    }

    private static String textValue(JsonNode document, String fieldName) {
        var value = document.get(fieldName);
        return value != null && value.isString() && !value.stringValue().isBlank() ? value.stringValue() : null;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
