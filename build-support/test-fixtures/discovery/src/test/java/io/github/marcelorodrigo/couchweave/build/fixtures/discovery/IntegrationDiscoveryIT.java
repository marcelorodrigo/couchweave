package io.github.marcelorodrigo.couchweave.build.fixtures.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationDiscoveryIT {

    @Test
    @DisplayName("should run fixture tests during the integration-test phase")
    void shouldRunFixtureTestsDuringTheIntegrationTestPhase() {
        // given
        var expectedPhase = "integration-test";

        // when
        var executedPhase = "integration-test";

        // then
        assertThat(executedPhase).isEqualTo(expectedPhase);
    }
}
