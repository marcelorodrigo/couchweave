package io.github.marcelorodrigo.couchweave.testsupport.couchdb;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;

class CouchDbAdminClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final Set<Integer> SUCCESSFUL_DELETION_STATUSES = Set.of(200, 202, 404);

    private final CouchDbHttpTransport httpTransport;
    private final URI serverUri;
    private final String username;
    private final String password;

    CouchDbAdminClient(CouchDbHttpTransport httpTransport, URI serverUri, String username, String password) {
        this.httpTransport = httpTransport;
        this.serverUri = serverUri;
        this.username = username;
        this.password = password;
    }

    void assertHealthy() {
        var response = send(
                "check CouchDB readiness",
                HttpRequest.newBuilder(endpoint("/_up")).GET().build());

        if (response.statusCode() != 200 || !response.body().contains("\"status\":\"ok\"")) {
            throw unexpectedStatus("check CouchDB readiness", response);
        }
    }

    CouchDbTestDatabase createDatabase(String databaseName) {
        var databaseUri = endpoint("/" + databaseName);
        var response = send(
                "create CouchDB database",
                HttpRequest.newBuilder(databaseUri)
                        .PUT(HttpRequest.BodyPublishers.noBody())
                        .build());

        if (response.statusCode() != 201) {
            throw unexpectedStatus("create CouchDB database", response);
        }

        return new CouchDbTestDatabase(serverUri, databaseUri, databaseName, username, password);
    }

    boolean databaseExists(String databaseName) {
        var response = send(
                "check CouchDB database",
                HttpRequest.newBuilder(endpoint("/" + databaseName)).GET().build());

        if (response.statusCode() == 200) {
            return true;
        }
        if (response.statusCode() == 404) {
            return false;
        }
        throw unexpectedStatus("check CouchDB database", response);
    }

    void deleteDatabase(String databaseName) {
        var response = send(
                "delete CouchDB database",
                HttpRequest.newBuilder(endpoint("/" + databaseName)).DELETE().build());

        if (!SUCCESSFUL_DELETION_STATUSES.contains(response.statusCode())) {
            throw unexpectedStatus("delete CouchDB database", response);
        }
    }

    private CouchDbHttpResponse send(String operation, HttpRequest request) {
        var authenticatedRequest = HttpRequest.newBuilder(request.uri())
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", authorizationHeader())
                .method(request.method(), request.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()))
                .build();
        try {
            return httpTransport.send(authenticatedRequest);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CouchDbTestHarnessException("Interrupted while attempting to " + operation, exception);
        } catch (IOException exception) {
            throw new CouchDbTestHarnessException("Unable to " + operation + " at " + request.uri(), exception);
        }
    }

    private URI endpoint(String path) {
        return serverUri.resolve(path);
    }

    private String authorizationHeader() {
        var credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private CouchDbTestHarnessException unexpectedStatus(String operation, CouchDbHttpResponse response) {
        return new CouchDbTestHarnessException("Unable to %s at %s: received HTTP %d with response %s"
                .formatted(operation, response.uri(), response.statusCode(), response.body()));
    }
}
