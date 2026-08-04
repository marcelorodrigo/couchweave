package io.github.marcelorodrigo.couchweave.testsupport.couchdb;

import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

final class CouchDbContainerResource implements ExtensionContext.Store.CloseableResource, AutoCloseable {

    private static final String IMAGE_NAME = "couchdb:3.5";
    private static final int COUCHDB_PORT = 5984;
    private static final String USERNAME = "admin";

    private final GenericContainer<?> container;
    private final URI serverUri;
    private final String username;
    private final String password;

    CouchDbContainerResource(GenericContainer<?> container, URI serverUri, String username, String password) {
        this.container = container;
        this.serverUri = serverUri;
        this.username = username;
        this.password = password;
    }

    static CouchDbContainerResource start() {
        var password = UUID.randomUUID().toString();
        var container = new GenericContainer<>(DockerImageName.parse(IMAGE_NAME))
                .withEnv("COUCHDB_USER", USERNAME)
                .withEnv("COUCHDB_PASSWORD", password)
                .withExposedPorts(COUCHDB_PORT)
                .waitingFor(Wait.forHttp("/_up").forPort(COUCHDB_PORT).forStatusCode(200));

        try {
            container.start();
        } catch (RuntimeException exception) {
            throw DockerAvailability.startupFailure(exception, "true".equalsIgnoreCase(System.getenv("CI")));
        }

        var serverUri =
                URI.create("http://%s:%d".formatted(container.getHost(), container.getMappedPort(COUCHDB_PORT)));
        return new CouchDbContainerResource(container, serverUri, USERNAME, password);
    }

    URI serverUri() {
        return serverUri;
    }

    String username() {
        return username;
    }

    String password() {
        return password;
    }

    @Override
    public void close() {
        try {
            container.stop();
        } catch (RuntimeException exception) {
            throw new CouchDbTestHarnessException("Unable to stop the CouchDB test container", exception);
        }
    }
}
