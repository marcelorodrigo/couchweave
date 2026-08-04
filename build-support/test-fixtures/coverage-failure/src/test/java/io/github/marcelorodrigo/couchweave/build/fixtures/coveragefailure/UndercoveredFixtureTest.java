package io.github.marcelorodrigo.couchweave.build.fixtures.coveragefailure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UndercoveredFixtureTest {

    @Test
    @DisplayName("should cover the configured fixture value")
    void shouldCoverTheConfiguredFixtureValue() {
        // given
        var fixture = new UndercoveredFixture();

        // when
        var value = fixture.coveredValue();

        // then
        assertThat(value).isEqualTo(1);
    }
}
