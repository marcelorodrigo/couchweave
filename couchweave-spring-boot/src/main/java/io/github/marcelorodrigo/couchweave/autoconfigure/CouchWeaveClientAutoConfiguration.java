package io.github.marcelorodrigo.couchweave.autoconfigure;

import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * Provides the CouchWeave connection foundation from {@code spring.data.couchweave.*} properties.
 *
 * <p>A {@link CouchDbClientSettings} bean is created only when the application has not supplied one,
 * so a custom settings bean always wins. A {@link RestClient.Builder} is created unless the
 * application provides its own, preserving it as a transport customization boundary.
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings")
@EnableConfigurationProperties(CouchWeaveProperties.class)
public class CouchWeaveClientAutoConfiguration {

    /**
     * Creates the default client settings from bound properties when no custom settings bean exists.
     *
     * @param properties the bound CouchWeave properties
     * @return the validated client settings
     */
    @Bean
    @ConditionalOnMissingBean(CouchDbClientSettings.class)
    CouchDbClientSettings couchDbClientSettings(CouchWeaveProperties properties) {
        return properties.toSettings();
    }

    /**
     * Creates the default REST client builder unless the application provides its own.
     *
     * @return the REST client builder
     */
    @Bean
    @ConditionalOnMissingBean(RestClient.Builder.class)
    RestClient.Builder couchWeaveRestClientBuilder() {
        return RestClient.builder();
    }
}
