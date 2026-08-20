package io.github.marcelorodrigo.couchweave.autoconfigure;

import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code spring.data.couchweave.*} properties into typed values consumed by the CouchWeave
 * Spring Boot auto-configuration.
 *
 * <p>Connection properties are optional only when the application supplies its own
 * {@link CouchDbClientSettings} bean. When bound, {@link #toSettings()} validates them and
 * translates failures into property-specific messages that never include the configured password.
 */
@ConfigurationProperties(prefix = "spring.data.couchweave")
public class CouchWeaveProperties {

    /** Absolute HTTP(S) URI of the CouchDB server. */
    private URI serverUri;

    /** Default CouchDB database name. */
    private String database;

    /** Optional basic-auth username. */
    private String username;

    /** Optional basic-auth password. */
    private String password;

    /** Timeout for establishing a connection to CouchDB. */
    private Duration connectTimeout = CouchDbClientSettings.DEFAULT_CONNECT_TIMEOUT;

    /** Timeout for receiving a response from CouchDB. */
    private Duration readTimeout = CouchDbClientSettings.DEFAULT_READ_TIMEOUT;

    /** Repository discovery options. */
    private final Repositories repositories = new Repositories();

    /**
     * Builds immutable client settings from the bound properties, applying CouchWeave validation and
     * translating failures into property-specific messages.
     *
     * @return the validated client settings
     * @throws CouchWeaveConfigurationException when a required property is missing or semantically invalid
     */
    CouchDbClientSettings toSettings() {
        if (serverUri == null) {
            throw new CouchWeaveConfigurationException(
                    "Property 'spring.data.couchweave.server-uri' must be configured");
        }
        if (database == null || database.isBlank()) {
            throw new CouchWeaveConfigurationException("Property 'spring.data.couchweave.database' must be configured");
        }
        try {
            return new CouchDbClientSettings(serverUri, database, username, password, connectTimeout, readTimeout);
        } catch (IllegalArgumentException exception) {
            throw new CouchWeaveConfigurationException(translate(exception.getMessage()), exception);
        }
    }

    /**
     * Rewrites a {@link CouchDbClientSettings} validation message to reference the external property
     * names instead of the Java component names.
     *
     * @param message the original validation message
     * @return the message with property-specific names
     */
    private static String translate(String message) {
        return message.replace("serverUri", "spring.data.couchweave.server-uri")
                .replace("database", "spring.data.couchweave.database")
                .replace("username and password", "spring.data.couchweave.username and spring.data.couchweave.password")
                .replace("connectTimeout", "spring.data.couchweave.connect-timeout")
                .replace("readTimeout", "spring.data.couchweave.read-timeout");
    }

    public URI getServerUri() {
        return serverUri;
    }

    public void setServerUri(URI serverUri) {
        this.serverUri = serverUri;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Repositories getRepositories() {
        return repositories;
    }

    /**
     * Repository discovery options bound to {@code spring.data.couchweave.repositories.*}.
     */
    public static class Repositories {

        /** Whether repository interfaces are discovered from the Boot auto-configuration base packages. */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
