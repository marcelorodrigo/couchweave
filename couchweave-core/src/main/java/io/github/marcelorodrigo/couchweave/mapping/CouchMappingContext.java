package io.github.marcelorodrigo.couchweave.mapping;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * Boot-independent Spring Data mapping context for CouchDB documents.
 */
public final class CouchMappingContext
        extends AbstractMappingContext<CouchPersistentEntity<?>, CouchPersistentProperty> {

    private final String defaultDatabase;
    private final ConcurrentMap<String, Class<?>> discriminatorTypes = new ConcurrentHashMap<>();

    /**
     * Creates an empty mapping context using the supplied fallback database.
     *
     * @param defaultDatabase the nonblank database used by documents without an override
     * @throws IllegalArgumentException when {@code defaultDatabase} is {@code null} or blank
     */
    public CouchMappingContext(String defaultDatabase) {
        if (defaultDatabase == null || defaultDatabase.isBlank()) {
            throw new IllegalArgumentException("defaultDatabase must not be blank");
        }
        this.defaultDatabase = defaultDatabase;
        setStrict(true);
    }

    @Override
    protected <T> CouchPersistentEntity<?> createPersistentEntity(TypeInformation<T> typeInformation) {
        return new BasicCouchPersistentEntity<>(typeInformation, defaultDatabase);
    }

    @Override
    protected CouchPersistentProperty createPersistentProperty(
            Property property, CouchPersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder) {
        return new BasicCouchPersistentProperty(property, owner, simpleTypeHolder);
    }

    @Override
    protected Optional<CouchPersistentEntity<?>> addPersistentEntity(TypeInformation<?> typeInformation) {
        Optional<CouchPersistentEntity<?>> entity;
        try {
            entity = super.addPersistentEntity(typeInformation);
        } catch (MappingException exception) {
            throw unwrapMappingException(exception);
        }
        entity.filter(this::isVerified).ifPresent(this::registerDiscriminator);
        return entity;
    }

    @Override
    protected boolean shouldCreatePersistentEntityFor(TypeInformation<?> typeInformation) {
        return super.shouldCreatePersistentEntityFor(typeInformation)
                && AnnotatedElementUtils.hasAnnotation(typeInformation.getType(), CouchDocument.class);
    }

    private boolean isVerified(CouchPersistentEntity<?> entity) {
        return entity instanceof BasicCouchPersistentEntity<?> basicEntity && basicEntity.isVerified();
    }

    private void registerDiscriminator(CouchPersistentEntity<?> entity) {
        var conflictingType = discriminatorTypes.putIfAbsent(entity.getDiscriminator(), entity.getType());
        if (conflictingType != null && !conflictingType.equals(entity.getType())) {
            throw new MappingException("Type %s discriminator '%s' conflicts with type %s"
                    .formatted(entity.getType().getName(), entity.getDiscriminator(), conflictingType.getName()));
        }
    }

    private MappingException unwrapMappingException(MappingException exception) {
        var detailedException = exception;
        while (detailedException.getCause() instanceof MappingException cause) {
            detailedException = cause;
        }
        return detailedException;
    }
}
