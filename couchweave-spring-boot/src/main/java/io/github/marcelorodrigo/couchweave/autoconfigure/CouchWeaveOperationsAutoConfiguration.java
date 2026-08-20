package io.github.marcelorodrigo.couchweave.autoconfigure;

import io.github.marcelorodrigo.couchweave.CouchWeaveOperations;
import io.github.marcelorodrigo.couchweave.CouchWeaveTemplate;
import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import io.github.marcelorodrigo.couchweave.mapping.CouchWeaveConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * Provides the default {@link CouchWeaveOperations} implementation from the CouchWeave connection,
 * transport, and mapping infrastructure when the application has not supplied its own operations
 * bean.
 */
@AutoConfiguration
@AutoConfigureAfter(CouchWeaveMappingAutoConfiguration.class)
@ConditionalOnClass(
        name = {
            "io.github.marcelorodrigo.couchweave.CouchWeaveOperations",
            "io.github.marcelorodrigo.couchweave.CouchWeaveTemplate"
        })
public class CouchWeaveOperationsAutoConfiguration {

    /**
     * Creates the default CouchWeave template when no custom operations bean exists.
     *
     * @param settings the client settings
     * @param builder the REST client builder
     * @param converter the mapping converter
     * @return the CouchWeave operations
     */
    @Bean
    @ConditionalOnMissingBean(CouchWeaveOperations.class)
    CouchWeaveOperations couchWeaveOperations(
            CouchDbClientSettings settings, RestClient.Builder builder, CouchWeaveConverter converter) {
        return new CouchWeaveTemplate(settings, builder, converter);
    }
}
