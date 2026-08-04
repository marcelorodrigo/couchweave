package io.github.marcelorodrigo.couchweave.sample;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReactorBuildTest {

    @Test
    @DisplayName("should run tests during the unit-test phase")
    void shouldRunTestsDuringTheUnitTestPhase() {
        // given
        var minimumJavaFeatureVersion = 21;

        // when
        var currentJavaFeatureVersion = Runtime.version().feature();

        // then
        assertThat(currentJavaFeatureVersion).isGreaterThanOrEqualTo(minimumJavaFeatureVersion);
    }
}
