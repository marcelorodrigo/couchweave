package io.github.marcelorodrigo.couchweave.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CouchWeavePropertiesTest {

    @Test
    @DisplayName("should default timeouts to the client settings defaults")
    void shouldDefaultTimeouts() {
        // given
        var properties = new CouchWeaveProperties();
        // when / then
        assertThat(properties.getConnectTimeout()).isEqualTo(CouchDbClientSettings.DEFAULT_CONNECT_TIMEOUT);
        assertThat(properties.getReadTimeout()).isEqualTo(CouchDbClientSettings.DEFAULT_READ_TIMEOUT);
    }

    @Test
    @DisplayName("should enable repositories by default")
    void shouldEnableRepositoriesByDefault() {
        // given
        var properties = new CouchWeaveProperties();
        // when / then
        assertThat(properties.getRepositories().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("should build settings from valid properties")
    void shouldBuildSettingsFromValidProperties() {
        // given
        var properties = new CouchWeaveProperties();
        properties.setServerUri(URI.create("http://localhost:5984"));
        properties.setDatabase("couchweave_test");
        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setReadTimeout(Duration.ofSeconds(10));
        // when
        var settings = properties.toSettings();
        // then
        assertThat(settings.serverUri()).isEqualTo(URI.create("http://localhost:5984"));
        assertThat(settings.database()).isEqualTo("couchweave_test");
        assertThat(settings.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(settings.readTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(settings.hasCredentials()).isFalse();
    }

    @Test
    @DisplayName("should build settings with credentials")
    void shouldBuildSettingsWithCredentials() {
        // given
        var properties = new CouchWeaveProperties();
        properties.setServerUri(URI.create("http://localhost:5984"));
        properties.setDatabase("couchweave_test");
        properties.setUsername("user");
        properties.setPassword("secret");
        // when
        var settings = properties.toSettings();
        // then
        assertThat(settings.hasCredentials()).isTrue();
    }

    @Test
    @DisplayName("should fail when server-uri is missing")
    void shouldFailWhenServerUriMissing() {
        // given
        var properties = new CouchWeaveProperties();
        properties.setDatabase("couchweave_test");
        // when / then
        assertThatThrownBy(properties::toSettings)
                .isInstanceOf(CouchWeaveConfigurationException.class)
                .hasMessage("Property 'spring.data.couchweave.server-uri' must be configured");
    }

    @Test
    @DisplayName("should fail when database is missing")
    void shouldFailWhenDatabaseMissing() {
        // given
        var properties = new CouchWeaveProperties();
        properties.setServerUri(URI.create("http://localhost:5984"));
        // when / then
        assertThatThrownBy(properties::toSettings)
                .isInstanceOf(CouchWeaveConfigurationException.class)
                .hasMessage("Property 'spring.data.couchweave.database' must be configured");
    }

    @Test
    @DisplayName("should fail with property-specific message when only username is configured")
    void shouldFailWhenOnlyUsernameConfigured() {
        // given
        var properties = new CouchWeaveProperties();
        properties.setServerUri(URI.create("http://localhost:5984"));
        properties.setDatabase("couchweave_test");
        properties.setUsername("user");
        // when / then
        assertThatThrownBy(properties::toSettings)
                .isInstanceOf(CouchWeaveConfigurationException.class)
                .hasMessageContaining("spring.data.couchweave.username and spring.data.couchweave.password");
    }

    @Test
    @DisplayName("should fail with property-specific message for illegal database name")
    void shouldFailWithPropertySpecificDatabaseMessage() {
        // given
        var properties = new CouchWeaveProperties();
        properties.setServerUri(URI.create("http://localhost:5984"));
        properties.setDatabase("Invalid Name");
        // when / then
        assertThatThrownBy(properties::toSettings)
                .isInstanceOf(CouchWeaveConfigurationException.class)
                .hasMessageContaining("spring.data.couchweave.database");
    }

    @Test
    @DisplayName("should never expose the password in the configuration exception message")
    void shouldNotExposePassword() {
        // given
        var properties = new CouchWeaveProperties();
        properties.setServerUri(URI.create("http://localhost:5984"));
        properties.setDatabase("Invalid Name");
        properties.setUsername("user");
        properties.setPassword("super-secret");
        // when / then
        assertThatThrownBy(properties::toSettings)
                .hasMessageContaining("spring.data.couchweave.database")
                .hasMessageNotContaining("super-secret");
    }
}
