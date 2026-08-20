package io.github.marcelorodrigo.couchweave.mapping;

import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
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
    private final boolean strict;
    private final ConcurrentMap<String, Class<?>> discriminatorTypes = new ConcurrentHashMap<>();

    /**
     * Creates an empty mapping context using the supplied fallback database.
     *
     * <p>The context is strict: unknown types are rejected rather than created lazily.
     *
     * @param defaultDatabase the nonblank database used by documents without an override
     * @throws IllegalArgumentException when {@code defaultDatabase} is {@code null} or blank
     */
    public CouchMappingContext(String defaultDatabase) {
        this(defaultDatabase, true);
    }

    /**
     * Creates an empty mapping context using the database of the supplied settings as the fallback.
     *
     * @param settings validated CouchDB connection settings whose database becomes the fallback
     * @param strict when {@code true}, unknown types are rejected instead of being created lazily
     * @throws IllegalArgumentException when {@code settings} is {@code null}
     */
    public CouchMappingContext(CouchDbClientSettings settings, boolean strict) {
        this(databaseOf(settings), strict);
    }

    /**
     * Creates an empty mapping context using the supplied fallback database.
     *
     * <p>A non-strict context lazily creates persistent entities for annotated document types
     * on first access, which lets repository infrastructure resolve domain types discovered
     * during scanning. Strict contexts require every entity to be registered before
     * initialization and reject unknown types.
     *
     * @param defaultDatabase the nonblank database used by documents without an override
     * @param strict when {@code true}, unknown types are rejected instead of being created lazily
     * @throws IllegalArgumentException when {@code defaultDatabase} is {@code null} or blank
     */
    private static String databaseOf(CouchDbClientSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("settings must not be null");
        }
        return settings.database();
    }

    public CouchMappingContext(String defaultDatabase, boolean strict) {
        if (defaultDatabase == null || defaultDatabase.isBlank()) {
            throw new IllegalArgumentException("defaultDatabase must not be blank");
        }
        this.defaultDatabase = defaultDatabase;
        this.strict = strict;
        setStrict(strict);
    }

    /** Returns whether unknown types are rejected instead of being created lazily. */
    public boolean isStrict() {
        return strict;
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
