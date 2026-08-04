package io.github.marcelorodrigo.couchweave.build.fixtures.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestDiscoveryTest {

    @Test
    @DisplayName("should run fixture tests during the unit-test phase")
    void shouldRunFixtureTestsDuringTheUnitTestPhase() {
        // given
        var expectedPhase = "test";

        // when
        var executedPhase = "test";

        // then
        assertThat(executedPhase).isEqualTo(expectedPhase);
    }
}
