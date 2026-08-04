package io.github.marcelorodrigo.couchweave.sample;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReactorBuildIT {

    @Test
    @DisplayName("should run tests during the integration-test phase")
    void shouldRunTestsDuringTheIntegrationTestPhase() {
        // given
        var minimumJavaFeatureVersion = 21;

        // when
        var currentJavaFeatureVersion = Runtime.version().feature();

        // then
        assertThat(currentJavaFeatureVersion).isGreaterThanOrEqualTo(minimumJavaFeatureVersion);
    }
}
