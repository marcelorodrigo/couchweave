package io.github.marcelorodrigo.couchweave.mapping;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.UUID;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.Parameter;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.mapping.model.ParameterValueProvider;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Metadata-driven {@link CouchWeaveConverter} implementation.
 */
public final class MappingCouchWeaveConverter implements CouchWeaveConverter {

    /** The mapping context used to resolve persistent entities. */
    private final CouchMappingContext mappingContext;
    /** The Jackson mapper used to convert JSON values. */
    private final ObjectMapper objectMapper;
    /** The property conversion registry. */
    private final CouchWeaveCustomConversions customConversions;
    /** The service used to convert property values. */
    private final ConversionService conversionService;
    /** The instantiators used to create persistent entities. */
    private final EntityInstantiators entityInstantiators = new EntityInstantiators();

    /**
     * Creates a converter using the supplied mapping and value conversion infrastructure.
     *
     * @param mappingContext the initialized CouchWeave mapping context
     * @param objectMapper the Jackson mapper used for JSON values
     * @param customConversions the property conversion registry
     */
    public MappingCouchWeaveConverter(
            CouchMappingContext mappingContext,
            ObjectMapper objectMapper,
            CouchWeaveCustomConversions customConversions) {
        this.mappingContext = Objects.requireNonNull(mappingContext, "mappingContext must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.customConversions = Objects.requireNonNull(customConversions, "customConversions must not be null");
        this.conversionService = customConversions.getConversionService();
    }

    /**
     * Creates a converter with a default Jackson mapper.
     *
     * @param mappingContext the initialized CouchWeave mapping context
     * @param customConversions the property conversion registry
     */
    public MappingCouchWeaveConverter(
            CouchMappingContext mappingContext, CouchWeaveCustomConversions customConversions) {
        this(mappingContext, new ObjectMapper(), customConversions);
    }

    @Override
    public ObjectNode write(Object source) {
        if (source == null) {
            throw new MappingException("Cannot write a null CouchWeave entity");
        }

        var entity = getRequiredEntity(source.getClass());
        var accessor = entity.getPropertyAccessor(source);
        var document = objectMapper.createObjectNode();

        var idProperty = entity.getRequiredIdProperty();
        var id = (String) accessor.getProperty(idProperty);
        document.put(CouchFieldNames.ID, id == null ? UUID.randomUUID().toString() : id);

        var revisionProperty = findRevisionProperty(entity);
        if (revisionProperty != null) {
            var revision = (String) accessor.getProperty(revisionProperty);
            if (revision != null) {
                document.put(CouchFieldNames.REVISION, revision);
            }
        }
        document.put(CouchFieldNames.DISCRIMINATOR, entity.getDiscriminator());

        for (var property : entity) {
            if (property.isIdProperty() || property.isRevisionProperty()) {
                continue;
            }
            writeProperty(document, property, accessor.getProperty(property), entity.getType());
        }
        return document;
    }

    @Override
    public <T> T read(Class<T> targetType, JsonNode source) {
        if (targetType == null) {
            throw new MappingException("Cannot read a CouchWeave document without a target type");
        }
        if (source == null || !source.isObject()) {
            throw new MappingException(
                    "Cannot read type %s from a non-object JSON value".formatted(targetType.getName()));
        }

        var entity = getRequiredEntity(targetType);
        validateDocument(entity, source);

        T instance;
        try {
            var instantiator = entityInstantiators.getInstantiatorFor(entity);
            instance = instantiator.createInstance(entity, new DocumentParameterValueProvider(entity, source));
        } catch (PropertyMappingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new MappingException(
                    "Could not instantiate mapped type %s".formatted(targetType.getName()), exception);
        }

        if (!entity.requiresPropertyPopulation()) {
            return instance;
        }

        var accessor = entity.getPropertyAccessor(instance);
        for (var property : entity) {
            if (entity.isCreatorArgument(property)) {
                continue;
            }
            try {
                accessor.setProperty(property, readProperty(property, source));
            } catch (PropertyMappingException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                throw propertyException(entity.getType(), property, "read", exception);
            }
        }
        return accessor.getBean();
    }

    @Override
    public CouchMappingContext getMappingContext() {
        return mappingContext;
    }

    @Override
    public ConversionService getConversionService() {
        return conversionService;
    }

    /**
     * Gets the persistent entity for the supplied type.
     *
     * @param type the mapped type
     * @return the persistent entity
     */
    @SuppressWarnings("unchecked")
    private <T> CouchPersistentEntity<T> getRequiredEntity(Class<T> type) {
        try {
            return (CouchPersistentEntity<T>) mappingContext.getRequiredPersistentEntity(type);
        } catch (MappingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new MappingException("Unknown mapped CouchWeave type %s".formatted(type.getName()), exception);
        }
    }

    /**
     * Writes a property value to a document.
     *
     * @param document the document being written
     * @param property the property to write
     * @param value the property value
     * @param entityType the mapped entity type
     */
    private void writeProperty(
            ObjectNode document, CouchPersistentProperty property, Object value, Class<?> entityType) {
        try {
            if (value == null) {
                document.putNull(property.getFieldName());
                return;
            }
            Object converted = value;
            var customTarget = customConversions.getCustomWriteTarget(value.getClass());
            if (customTarget.isPresent()) {
                converted = conversionService.convert(value, customTarget.get());
            }
            document.set(property.getFieldName(), objectMapper.valueToTree(converted));
        } catch (RuntimeException exception) {
            throw propertyException(entityType, property, "write", exception);
        }
    }

    /**
     * Reads a property value from a document.
     *
     * @param property the property to read
     * @param source the source document
     * @return the property value
     */
    private Object readProperty(CouchPersistentProperty property, JsonNode source) {
        var value = source.get(property.getFieldName());
        if (value == null || value.isNull()) {
            return ParameterValueProvider.getDefaultValue(property.getType());
        }

        try {
            var nativeValue = objectMapper.treeToValue(value, Object.class);
            if (nativeValue != null
                    && customConversions.hasCustomReadTarget(nativeValue.getClass(), property.getType())) {
                return conversionService.convert(nativeValue, property.getType());
            }
            return objectMapper.treeToValue(
                    value, objectMapper.getTypeFactory().constructType(getGenericType(property)));
        } catch (RuntimeException exception) {
            throw propertyException(property.getOwner().getType(), property, "read", exception);
        }
    }

    /**
     * Finds the revision property of an entity.
     *
     * @param entity the persistent entity
     * @return the revision property, or {@code null} when none exists
     */
    private CouchPersistentProperty findRevisionProperty(CouchPersistentEntity<?> entity) {
        for (var property : entity) {
            if (property.isRevisionProperty()) {
                return property;
            }
        }
        return null;
    }

    /**
     * Gets the generic type represented by a property.
     *
     * @param property the persistent property
     * @return the property's generic type
     */
    private Type getGenericType(CouchPersistentProperty property) {
        var field = property.getField();
        if (field != null) {
            return field.getGenericType();
        }
        var getter = property.getGetter();
        return getter == null ? property.getType() : getter.getGenericReturnType();
    }

    /**
     * Validates the required metadata in a document.
     *
     * @param entity the persistent entity
     * @param source the source document
     */
    private void validateDocument(CouchPersistentEntity<?> entity, JsonNode source) {
        requireNonblankText(source, CouchFieldNames.ID, entity.getType(), true);
        requireNonblankText(source, CouchFieldNames.REVISION, entity.getType(), false);

        var discriminator = source.get(CouchFieldNames.DISCRIMINATOR);
        if (discriminator == null || !discriminator.isString()) {
            throw new MappingException("Type %s document requires textual field '%s'"
                    .formatted(entity.getType().getName(), CouchFieldNames.DISCRIMINATOR));
        }
        if (!entity.getDiscriminator().equals(discriminator.stringValue())) {
            throw new MappingException("Type %s document field '%s' value '%s' does not match '%s'"
                    .formatted(
                            entity.getType().getName(),
                            CouchFieldNames.DISCRIMINATOR,
                            discriminator.stringValue(),
                            entity.getDiscriminator()));
        }
    }

    /**
     * Requires a document field to contain nonblank text.
     *
     * @param source the source document
     * @param field the field name
     * @param entityType the mapped entity type
     * @param required whether the field is required
     */
    private void requireNonblankText(JsonNode source, String field, Class<?> entityType, boolean required) {
        var value = source.get(field);
        if (value == null && !required) {
            return;
        }
        if (value == null || !value.isString() || value.stringValue().isBlank()) {
            throw new MappingException(
                    "Type %s document field '%s' must be nonblank text".formatted(entityType.getName(), field));
        }
    }

    /**
     * Creates an exception describing a property mapping failure.
     *
     * @param entityType the mapped entity type
     * @param property the affected property
     * @param operation the mapping operation
     * @param cause the underlying cause
     * @return the property mapping exception
     */
    private PropertyMappingException propertyException(
            Class<?> entityType, CouchPersistentProperty property, String operation, Throwable cause) {
        return new PropertyMappingException(
                "Could not %s type %s property '%s' from field '%s'"
                        .formatted(operation, entityType.getName(), property.getName(), property.getFieldName()),
                cause);
    }

    /** Exception raised when a property cannot be mapped. */
    private static final class PropertyMappingException extends MappingException {

        /**
         * Creates a property mapping exception.
         *
         * @param message the exception message
         * @param cause the underlying cause
         */
        private PropertyMappingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Provides constructor parameter values from a source document. */
    private final class DocumentParameterValueProvider implements ParameterValueProvider<CouchPersistentProperty> {

        /** The persistent entity being instantiated. */
        private final CouchPersistentEntity<?> entity;
        /** The source document. */
        private final JsonNode source;

        /**
         * Creates a parameter value provider for a document.
         *
         * @param entity the persistent entity
         * @param source the source document
         */
        private DocumentParameterValueProvider(CouchPersistentEntity<?> entity, JsonNode source) {
            this.entity = entity;
            this.source = source;
        }

        /**
         * Gets a constructor parameter value from the source document.
         *
         * @param parameter the constructor parameter
         * @return the parameter value
         */
        @Override
        @SuppressWarnings("unchecked")
        public <T> T getParameterValue(Parameter<T, CouchPersistentProperty> parameter) {
            try {
                var property = entity.getRequiredPersistentProperty(parameter.getRequiredName());
                return (T) readProperty(property, source);
            } catch (MappingException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                throw new MappingException(
                        "Could not resolve constructor parameter '%s' for type %s"
                                .formatted(parameter.getName(), entity.getType().getName()),
                        exception);
            }
        }
    }
}
