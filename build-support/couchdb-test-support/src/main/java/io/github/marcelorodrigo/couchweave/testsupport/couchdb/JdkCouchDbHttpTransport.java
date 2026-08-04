package io.github.marcelorodrigo.couchweave.testsupport.couchdb;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

final class JdkCouchDbHttpTransport implements CouchDbHttpTransport {

    private final HttpClient httpClient;

    JdkCouchDbHttpTransport(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public CouchDbHttpResponse send(HttpRequest request) throws IOException, InterruptedException {
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new CouchDbHttpResponse(response.uri(), response.statusCode(), response.body());
    }
}
