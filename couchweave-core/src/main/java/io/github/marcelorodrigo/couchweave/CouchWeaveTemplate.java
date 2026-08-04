package io.github.marcelorodrigo.couchweave;

import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import io.github.marcelorodrigo.couchweave.client.internal.CouchDbClient;
import io.github.marcelorodrigo.couchweave.mapping.CouchPersistentEntity;
import io.github.marcelorodrigo.couchweave.mapping.CouchPersistentProperty;
import io.github.marcelorodrigo.couchweave.mapping.CouchWeaveConverter;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import tools.jackson.databind.JsonNode;

/**
 * Synchronous, stateless implementation of {@link CouchWeaveOperations}.
 *
 * <p>The template resolves the target database from CouchWeave mapping metadata for every
 * operation. It can be used directly in applications without a Spring Boot context.
 */
public final class CouchWeaveTemplate implements CouchWeaveOperations {

    private static final String ID_FIELD = "_id";
    private static final String REVISION_FIELD = "_rev";

    private final CouchDbClient client;
    private final CouchWeaveConverter converter;

    /**
     * Creates a template backed by the default CouchDB HTTP client.
     *
     * @param settings validated CouchDB connection settings
     * @param converter entity/document converter and mapping metadata provider
     */
    public CouchWeaveTemplate(CouchDbClientSettings settings, CouchWeaveConverter converter) {
        this(CouchDbClient.create(Objects.requireNonNull(settings, "settings must not be null")), converter);
    }

    /**
     * Creates a template with an explicitly supplied client.
     *
     * <p>This constructor is package-private so tests and same-package infrastructure can replace the
     * transport without exposing raw client wiring as the primary application API.
     *
     * @param client CouchDB CRUD boundary
     * @param converter entity/document converter and mapping metadata provider
     */
    CouchWeaveTemplate(CouchDbClient client, CouchWeaveConverter converter) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.converter = Objects.requireNonNull(converter, "converter must not be null");
    }

    @Override
    public <T> T save(T entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        var entityMetadata = getRequiredEntity(entity.getClass());
        var document = converter.write(entity);
        var documentId = requiredText(document, ID_FIELD);
        var result = client.putDocument(entityMetadata.getDatabase(), documentId, document);
        document.put(ID_FIELD, result.documentId());
        document.put(REVISION_FIELD, result.revision());
        return converter.read(entityClass(entity), document);
    }

    @Override
    public <T> Optional<T> findById(String id, Class<T> entityType) {
        requireId(id);
        Objects.requireNonNull(entityType, "entityType must not be null");
        var entityMetadata = getRequiredEntity(entityType);
        return client.getDocument(entityMetadata.getDatabase(), id)
                .map(document -> converter.read(entityType, document));
    }

    @Override
    public boolean existsById(String id, Class<?> entityType) {
        requireId(id);
        Objects.requireNonNull(entityType, "entityType must not be null");
        return client.documentExists(getRequiredEntity(entityType).getDatabase(), id);
    }

    @Override
    public void delete(Object entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        var entityMetadata = getRequiredEntity(entity.getClass());
        var accessor = entityMetadata.getPropertyAccessor(entity);
        var idProperty = entityMetadata.getRequiredIdProperty();
        var id = mappedText(accessor.getProperty(idProperty));
        var revisionProperty = findRevisionProperty(entityMetadata);
        var revision = revisionProperty == null ? null : mappedText(accessor.getProperty(revisionProperty));
        if (id == null || revision == null) {
            throw new InvalidDataAccessApiUsageException(
                    "Deleting a CouchWeave entity requires a mapped nonblank ID and revision");
        }
        client.deleteDocument(entityMetadata.getDatabase(), id, revision);
    }

    @Override
    public void deleteById(String id, Class<?> entityType) {
        requireId(id);
        Objects.requireNonNull(entityType, "entityType must not be null");
        var entityMetadata = getRequiredEntity(entityType);
        var document = client.getDocument(entityMetadata.getDatabase(), id);
        if (document.isEmpty()) {
            return;
        }
        var revision = requiredText(document.get(), REVISION_FIELD);
        client.deleteDocument(entityMetadata.getDatabase(), id, revision);
    }

    @SuppressWarnings("unchecked")
    private <T> CouchPersistentEntity<T> getRequiredEntity(Class<T> entityType) {
        return (CouchPersistentEntity<T>) converter.getMappingContext().getRequiredPersistentEntity(entityType);
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> entityClass(T entity) {
        return (Class<T>) entity.getClass();
    }

    private CouchPersistentProperty findRevisionProperty(CouchPersistentEntity<?> entityMetadata) {
        for (var property : entityMetadata) {
            if (property.isRevisionProperty()) {
                return property;
            }
        }
        return null;
    }

    private static String mappedText(Object value) {
        return value instanceof String text && !text.isBlank() ? text : null;
    }

    private static String requiredText(JsonNode document, String fieldName) {
        var value = document.get(fieldName);
        if (value == null || !value.isString() || value.stringValue().isBlank()) {
            throw new IllegalStateException("CouchDB document must contain a nonblank " + fieldName);
        }
        return value.stringValue();
    }

    private static void requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }
}
