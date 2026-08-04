package io.github.marcelorodrigo.couchweave.testsupport.couchdb;

import org.opentest4j.TestAbortedException;

final class DockerAvailability {

    private DockerAvailability() {}

    static RuntimeException startupFailure(RuntimeException cause, boolean continuousIntegration) {
        if (!isUnavailable(cause)) {
            return new CouchDbTestHarnessException("Unable to start the CouchDB test container", cause);
        }

        var message = "Docker is unavailable. Start Docker or Podman, or configure Testcontainers."
                + " CouchDB integration tests cannot run without a container runtime.";
        if (continuousIntegration) {
            return new CouchDbTestHarnessException(message, cause);
        }
        return new TestAbortedException(message, cause);
    }

    private static boolean isUnavailable(Throwable failure) {
        for (var current = failure; current != null; current = current.getCause()) {
            var message = current.getMessage();
            if (message != null && message.contains("Could not find a valid Docker environment")) {
                return true;
            }
        }
        return false;
    }
}
