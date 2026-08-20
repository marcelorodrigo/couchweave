package io.github.marcelorodrigo.couchweave.autoconfigure;

import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import io.github.marcelorodrigo.couchweave.mapping.CouchMappingContext;
import io.github.marcelorodrigo.couchweave.mapping.CouchWeaveConverter;
import io.github.marcelorodrigo.couchweave.mapping.CouchWeaveCustomConversions;
import io.github.marcelorodrigo.couchweave.mapping.MappingCouchWeaveConverter;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Provides the default CouchWeave mapping infrastructure when the application has not supplied it.
 *
 * <p>The mapping context is non-strict so repository domain types discovered during scanning are
 * resolved lazily. The default settings bean (or a user-supplied one) provides the fallback
 * database, so the mapping context is only created when a settings bean is available.
 */
@AutoConfiguration
@AutoConfigureAfter(CouchWeaveClientAutoConfiguration.class)
@ConditionalOnClass(
        name = {
            "io.github.marcelorodrigo.couchweave.mapping.CouchMappingContext",
            "io.github.marcelorodrigo.couchweave.mapping.CouchWeaveCustomConversions",
            "io.github.marcelorodrigo.couchweave.mapping.MappingCouchWeaveConverter"
        })
public class CouchWeaveMappingAutoConfiguration {

    /**
     * Creates the default non-strict mapping context when no custom mapping context exists.
     *
     * @param settings the client settings providing the fallback database
     * @return the non-strict mapping context
     */
    @Bean
    @ConditionalOnMissingBean(CouchMappingContext.class)
    CouchMappingContext couchMappingContext(CouchDbClientSettings settings) {
        return new CouchMappingContext(settings, false);
    }

    /**
     * Creates the default empty custom conversions when no custom conversions exist.
     *
     * @return the empty custom conversions
     */
    @Bean
    @ConditionalOnMissingBean(CouchWeaveCustomConversions.class)
    CouchWeaveCustomConversions couchWeaveCustomConversions() {
        return new CouchWeaveCustomConversions(List.of());
    }

    /**
     * Creates the default mapping converter when no custom converter exists.
     *
     * @param mappingContext the mapping context
     * @param customConversions the custom conversions
     * @return the mapping converter
     */
    @Bean
    @ConditionalOnMissingBean(CouchWeaveConverter.class)
    MappingCouchWeaveConverter couchWeaveConverter(
            CouchMappingContext mappingContext, CouchWeaveCustomConversions customConversions) {
        return new MappingCouchWeaveConverter(mappingContext, customConversions);
    }
}
