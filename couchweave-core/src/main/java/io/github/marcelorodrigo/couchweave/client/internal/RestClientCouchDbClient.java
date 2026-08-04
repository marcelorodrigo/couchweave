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
import org.springframework.web.client.RestClient;

final class RestClientCouchDbClient implements CouchDbClient {

    private final RestClient restClient;
    private final CouchDbUriBuilder uriBuilder;

    RestClientCouchDbClient(CouchDbClientSettings settings) {
        this(settings, createRestClient(settings));
    }

    RestClientCouchDbClient(CouchDbClientSettings settings, RestClient restClient) {
        this.uriBuilder = new CouchDbUriBuilder(settings);
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
    }

    @Override
    public CouchDbResponse exchange(HttpMethod method, List<String> pathSegments, String body) {
        Objects.requireNonNull(method, "method must not be null");
        var request =
                restClient.method(method).uri(uriBuilder.build(pathSegments)).accept(MediaType.APPLICATION_JSON);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        return request.exchange((ignored, response) -> new CouchDbResponse(
                response.getStatusCode().value(),
                response.getHeaders(),
                new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
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
