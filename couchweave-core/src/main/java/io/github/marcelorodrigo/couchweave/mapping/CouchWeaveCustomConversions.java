package io.github.marcelorodrigo.couchweave.mapping;

import java.util.Collection;
import java.util.Objects;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.convert.CustomConversions;

/**
 * CouchWeave-specific registry for property value conversions.
 */
public final class CouchWeaveCustomConversions extends CustomConversions {

    /** The conversion service configured with the registered converters. */
    private final ConversionService conversionService;

    /**
     * Creates a registry containing the supplied Spring converters.
     *
     * @param converters Spring converter registrations
     */
    public CouchWeaveCustomConversions(Collection<?> converters) {
        super(StoreConversions.NONE, Objects.requireNonNull(converters, "converters must not be null"));
        var service = new DefaultConversionService();
        registerConvertersIn(service);
        this.conversionService = service;
    }

    /**
     * Returns the conversion service configured with the registered converters.
     *
     * @return the configured conversion service
     */
    ConversionService getConversionService() {
        return conversionService;
    }
}
