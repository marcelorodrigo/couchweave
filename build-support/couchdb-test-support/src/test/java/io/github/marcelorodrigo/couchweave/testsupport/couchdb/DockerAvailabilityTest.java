package io.github.marcelorodrigo.couchweave.testsupport.couchdb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

class DockerAvailabilityTest {

    @Test
    @DisplayName("should abort locally when Docker is unavailable")
    void shouldAbortLocallyWhenDockerIsUnavailable() {
        // given
        var cause = new IllegalStateException("Could not find a valid Docker environment");

        // when
        var failure = DockerAvailability.startupFailure(cause, false);

        // then
        assertThat(failure).isInstanceOf(TestAbortedException.class).hasMessageContaining("Docker is unavailable");
    }

    @Test
    @DisplayName("should fail continuous integration when Docker is unavailable")
    void shouldFailContinuousIntegrationWhenDockerIsUnavailable() {
        // given
        var cause = new IllegalStateException("Could not find a valid Docker environment");

        // when
        var failure = DockerAvailability.startupFailure(cause, true);

        // then
        assertThat(failure)
                .isInstanceOf(CouchDbTestHarnessException.class)
                .hasMessageContaining("Docker is unavailable");
    }

    @Test
    @DisplayName("should report other container startup failures as harness failures")
    void shouldReportOtherContainerStartupFailuresAsHarnessFailures() {
        // given
        var cause = new IllegalStateException("Image pull failed");

        // when
        var failure = DockerAvailability.startupFailure(cause, false);

        // then
        assertThat(failure)
                .isInstanceOf(CouchDbTestHarnessException.class)
                .hasMessage("Unable to start the CouchDB test container");
    }
}
