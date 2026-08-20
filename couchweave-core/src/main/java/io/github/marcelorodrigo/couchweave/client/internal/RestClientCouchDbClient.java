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

/** HTTP implementation of the internal CouchDB CRUD client boundary. */
final class RestClientCouchDbClient implements CouchDbClient {

    /** Query parameter containing the target database for generic request helpers. */
    private static final String DATABASE_PARAMETER = "database";

    /** Query parameter containing the target document identifier for generic request helpers. */
    private static final String DOCUMENT_ID_PARAMETER = "documentId";

    /** Spring HTTP client used to execute CouchDB requests. */
    private final RestClient restClient;

    /** Builder that encodes CouchDB paths and query parameters. */
    private final CouchDbUriBuilder uriBuilder;

    /** Translator that maps HTTP and transport failures to data-access exceptions. */
    private final CouchDbFailureTranslator failureTranslator;

    /** Decoder that validates successful CouchDB JSON responses. */
    private final CouchDbResponseDecoder responseDecoder;

    /** Database used when a generic request path does not provide one. */
    private final String defaultDatabase;

    /**
     * Creates a client with a configured HTTP transport and default collaborators.
     *
     * @param settings validated CouchDB connection settings
     */
    RestClientCouchDbClient(CouchDbClientSettings settings) {
        this(settings, createRestClient(settings), new CouchDbFailureTranslator(settings));
    }

    /**
     * Creates a client with a supplied HTTP transport and default failure translator.
     *
     * @param settings validated CouchDB connection settings
     * @param restClient HTTP client, typically supplied by a test
     */
    RestClientCouchDbClient(CouchDbClientSettings settings, RestClient restClient) {
        this(settings, restClient, new CouchDbFailureTranslator(settings));
    }

    /**
     * Creates a client with a supplied HTTP transport and failure translator.
     *
     * @param settings validated CouchDB connection settings
     * @param restClient HTTP client
     * @param failureTranslator response and transport failure translator
     */
    RestClientCouchDbClient(
            CouchDbClientSettings settings, RestClient restClient, CouchDbFailureTranslator failureTranslator) {
        this(settings, restClient, failureTranslator, new ObjectMapper());
    }

    /**
     * Creates a fully configurable client for production and focused tests.
     *
     * @param settings validated CouchDB connection settings
     * @param restClient HTTP client
     * @param failureTranslator response and transport failure translator
     * @param objectMapper parser for successful response bodies
     */
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

    /**
     * Reads all document bodies from a database through CouchDB's {@code _all_docs} endpoint.
     *
     * @param database source database
     * @return document bodies in CouchDB response order
     */
    @Override
    public List<JsonNode> getAllDocuments(String database) {
        requireText(database, DATABASE_PARAMETER);
        var context = CouchDbRequestContext.forDocument(database, null, null);
        var response =
                exchange(HttpMethod.GET, List.of(database, "_all_docs"), Map.of("include_docs", "true"), null, context);
        return responseDecoder.decodeDocuments(response, context);
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

    /**
     * Executes a generic request using context inferred from its path.
     *
     * @param method HTTP method
     * @param pathSegments CouchDB path segments
     * @param body optional JSON request body
     * @return buffered CouchDB response
     */
    CouchDbResponse exchange(HttpMethod method, List<String> pathSegments, String body) {
        return exchange(method, pathSegments, body, CouchDbRequestContext.fromPath(defaultDatabase, pathSegments));
    }

    /**
     * Executes a generic request with explicit failure context.
     *
     * @param method HTTP method
     * @param pathSegments CouchDB path segments
     * @param body optional JSON request body
     * @param context request location used when translating failures
     * @return buffered CouchDB response
     */
    CouchDbResponse exchange(HttpMethod method, List<String> pathSegments, String body, CouchDbRequestContext context) {
        return exchange(method, pathSegments, Map.of(), body, context);
    }

    /**
     * Executes an encoded HTTP request and translates non-success responses.
     *
     * @param method HTTP method
     * @param pathSegments CouchDB path segments
     * @param queryParameters query parameters
     * @param body optional JSON request body
     * @param context request location used when translating failures
     * @return buffered CouchDB response
     */
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

    /**
     * Creates the Spring HTTP client configured for the CouchDB server and credentials.
     *
     * @param settings validated CouchDB connection settings
     * @return configured Spring REST client
     */
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

    /**
     * Creates the JDK request factory with the configured response timeout.
     *
     * @param settings validated CouchDB connection settings
     * @return configured request factory
     */
    static JdkClientHttpRequestFactory createRequestFactory(CouchDbClientSettings settings) {
        var requestFactory = new JdkClientHttpRequestFactory(createHttpClient(settings));
        requestFactory.setReadTimeout(settings.readTimeout());
        return requestFactory;
    }

    /**
     * Creates the JDK HTTP client with the configured connection timeout.
     *
     * @param settings validated CouchDB connection settings
     * @return configured JDK HTTP client
     */
    static HttpClient createHttpClient(CouchDbClientSettings settings) {
        return HttpClient.newBuilder().connectTimeout(settings.connectTimeout()).build();
    }

    /**
     * Returns a non-blank textual field from a JSON document, or null otherwise.
     *
     * @param document JSON document to inspect
     * @param fieldName field to read
     * @return non-blank textual value, or {@code null}
     */
    private static String textValue(JsonNode document, String fieldName) {
        var value = document.get(fieldName);
        return value != null && value.isString() && !value.stringValue().isBlank() ? value.stringValue() : null;
    }

    /**
     * Rejects missing or blank values required to build a CouchDB request.
     *
     * @param value value to validate
     * @param name request parameter name used in the validation error
     */
    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
