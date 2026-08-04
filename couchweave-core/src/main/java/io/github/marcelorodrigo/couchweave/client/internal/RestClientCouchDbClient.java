package io.github.marcelorodrigo.couchweave.client.internal;

import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

final class RestClientCouchDbClient implements CouchDbClient {

    private final RestClient restClient;
    private final CouchDbUriBuilder uriBuilder;
    private final CouchDbFailureTranslator failureTranslator;
    private final String defaultDatabase;

    RestClientCouchDbClient(CouchDbClientSettings settings) {
        this(settings, createRestClient(settings), new CouchDbFailureTranslator(settings));
    }

    RestClientCouchDbClient(CouchDbClientSettings settings, RestClient restClient) {
        this(settings, restClient, new CouchDbFailureTranslator(settings));
    }

    RestClientCouchDbClient(
            CouchDbClientSettings settings, RestClient restClient, CouchDbFailureTranslator failureTranslator) {
        this.uriBuilder = new CouchDbUriBuilder(settings);
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
        this.failureTranslator = Objects.requireNonNull(failureTranslator, "failureTranslator must not be null");
        this.defaultDatabase = settings.database();
    }

    @Override
    public CouchDbResponse exchange(HttpMethod method, List<String> pathSegments, String body) {
        return exchange(method, pathSegments, body, CouchDbRequestContext.fromPath(defaultDatabase, pathSegments));
    }

    @Override
    public CouchDbResponse exchange(
            HttpMethod method, List<String> pathSegments, String body, CouchDbRequestContext context) {
        Objects.requireNonNull(method, "method must not be null");
        var request =
                restClient.method(method).uri(uriBuilder.build(pathSegments)).accept(MediaType.APPLICATION_JSON);
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
}
