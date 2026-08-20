package io.github.marcelorodrigo.couchweave;

import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import io.github.marcelorodrigo.couchweave.client.CouchOptimisticLockingFailureException;
import io.github.marcelorodrigo.couchweave.client.internal.CouchDbClient;
import io.github.marcelorodrigo.couchweave.mapping.CouchPersistentEntity;
import io.github.marcelorodrigo.couchweave.mapping.CouchPersistentProperty;
import io.github.marcelorodrigo.couchweave.mapping.CouchWeaveConverter;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * Synchronous, stateless implementation of {@link CouchWeaveOperations}.
 *
 * <p>The template resolves the target database from CouchWeave mapping metadata for every
 * operation. It can be used directly in applications without a Spring Boot context.
 */
public final class CouchWeaveTemplate implements CouchWeaveOperations {

    /** CouchDB document identifier field. */
    private static final String ID_FIELD = "_id";
    /** CouchDB document revision field. */
    private static final String REVISION_FIELD = "_rev";
    /** CouchWeave entity discriminator field. */
    private static final String DISCRIMINATOR_FIELD = "couchweave_type";

    /** CouchDB client used for document operations. */
    private final CouchDbClient client;
    /** Converter used for entity and document mapping. */
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
     * Creates a template backed by a caller-configured REST client builder.
     *
     * <p>The settings still determine the server URI, JSON accept header, and basic authentication,
     * but the transport, timeouts, and other builder configuration are preserved so applications can
     * customize the HTTP layer.
     *
     * @param settings validated CouchDB connection settings
     * @param restClientBuilder caller-configured REST client builder
     * @param converter entity/document converter and mapping metadata provider
     */
    public CouchWeaveTemplate(
            CouchDbClientSettings settings, RestClient.Builder restClientBuilder, CouchWeaveConverter converter) {
        this(
                CouchDbClient.create(
                        Objects.requireNonNull(settings, "settings must not be null"),
                        Objects.requireNonNull(restClientBuilder, "restClientBuilder must not be null")),
                converter);
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
        var accessor = entityMetadata.getPropertyAccessor(entity);
        var idProperty = entityMetadata.getRequiredIdProperty();
        var id = mappedText(accessor.getProperty(idProperty));
        var revisionProperty = findRevisionProperty(entityMetadata);
        var revision = revisionProperty == null ? null : mappedText(accessor.getProperty(revisionProperty));
        verifySaveLifecycle(entityMetadata, id, revision);
        var document = converter.write(entity);
        var documentId = requiredText(document, ID_FIELD);
        try {
            var result = client.putDocument(entityMetadata.getDatabase(), documentId, document);
            document.put(ID_FIELD, result.documentId());
            document.put(REVISION_FIELD, result.revision());
            return converter.read(entityClass(entity), document);
        } catch (CouchOptimisticLockingFailureException exception) {
            throw exception.withEntityType(entity.getClass());
        }
    }

    @Override
    public <T> Optional<T> findById(String id, Class<T> entityType) {
        requireId(id);
        Objects.requireNonNull(entityType, "entityType must not be null");
        var entityMetadata = getRequiredEntity(entityType);
        return client.getDocument(entityMetadata.getDatabase(), id)
                .map(document -> converter.read(entityType, document));
    }

    /**
     * Finds all documents whose discriminator matches the requested mapped entity type.
     *
     * @param entityType mapped entity type
     * @param <T> entity type
     * @return matching entities in CouchDB {@code _all_docs} response order
     */
    @Override
    public <T> Iterable<T> findAll(Class<T> entityType) {
        var entityMetadata = getRequiredEntity(entityType);
        var documents = client.getAllDocuments(entityMetadata.getDatabase());
        var result = new ArrayList<T>();
        for (var document : documents) {
            if (matchesEntity(document, entityMetadata)) {
                result.add(converter.read(entityType, document));
            }
        }
        return result;
    }

    /**
     * Counts all documents whose discriminator matches the requested mapped entity type.
     *
     * @param entityType mapped entity type
     * @return number of matching documents
     */
    @Override
    public long count(Class<?> entityType) {
        var entityMetadata = getRequiredEntity(entityType);
        var documents = client.getAllDocuments(entityMetadata.getDatabase());
        long count = 0;
        for (var document : documents) {
            if (matchesEntity(document, entityMetadata)) {
                converter.read(entityType, document);
                count++;
            }
        }
        return count;
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
        try {
            client.deleteDocument(entityMetadata.getDatabase(), id, revision);
        } catch (CouchOptimisticLockingFailureException exception) {
            throw exception.withEntityType(entity.getClass());
        }
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
        try {
            client.deleteDocument(entityMetadata.getDatabase(), id, revision);
        } catch (CouchOptimisticLockingFailureException exception) {
            throw exception.withEntityType(entityType);
        }
    }

    /**
     * Resolves the mapped persistent entity metadata for a Java type.
     *
     * @param entityType mapped entity type
     * @param <T> entity type
     * @return the resolved persistent entity metadata
     */
    @SuppressWarnings("unchecked")
    private <T> CouchPersistentEntity<T> getRequiredEntity(Class<T> entityType) {
        return (CouchPersistentEntity<T>) converter.getMappingContext().getRequiredPersistentEntity(entityType);
    }

    /**
     * Returns the concrete class of a saved or deleted entity instance.
     *
     * @param entity entity instance
     * @param <T> entity type
     * @return the entity class
     */
    @SuppressWarnings("unchecked")
    private <T> Class<T> entityClass(T entity) {
        return (Class<T>) entity.getClass();
    }

    /**
     * Returns the revision property of the mapped entity, or {@code null} when the type is revisionless.
     *
     * @param entityMetadata mapped entity metadata
     * @return the revision property, or {@code null} when none is declared
     */
    private CouchPersistentProperty findRevisionProperty(CouchPersistentEntity<?> entityMetadata) {
        for (var property : entityMetadata) {
            if (property.isRevisionProperty()) {
                return property;
            }
        }
        return null;
    }

    /**
     * Validates whether a save may proceed, enforcing revision semantics before any document write.
     *
     * <p>A {@code null} identifier means a generated-identifier create. A nonblank identifier with a
     * nonblank revision means a conditional update. A nonblank identifier without a revision requires a
     * prior existence probe: the save proceeds only when the document does not yet exist. A revision
     * without a usable identifier is rejected.
     *
     * @param entityMetadata mapped entity metadata
     * @param id nonblank mapped identifier, or {@code null} for a generated identifier
     * @param revision nonblank mapped revision, or {@code null} when the caller has no snapshot
     */
    private void verifySaveLifecycle(CouchPersistentEntity<?> entityMetadata, String id, String revision) {
        if (id == null) {
            if (revision != null) {
                throw new InvalidDataAccessApiUsageException(
                        "Saving a CouchWeave entity with a revision requires a mapped nonblank ID");
            }
            return;
        }
        if (revision != null) {
            return;
        }
        if (client.getDocument(entityMetadata.getDatabase(), id).isPresent()) {
            throw new InvalidDataAccessApiUsageException(
                    "Saving an existing CouchWeave entity requires a mapped nonblank revision");
        }
    }

    /**
     * Returns a nonblank textual property value, or {@code null} when the value is missing, blank, or not text.
     *
     * @param value raw property value read from the entity accessor
     * @return the nonblank string value, or {@code null}
     */
    private static String mappedText(Object value) {
        return value instanceof String text && !text.isBlank() ? text : null;
    }

    /**
     * Returns a nonblank textual field from a CouchDB document, failing when the field is absent or invalid.
     *
     * @param document CouchDB document tree
     * @param fieldName required textual field name
     * @return the nonblank string value
     */
    private static String requiredText(JsonNode document, String fieldName) {
        var value = document.get(fieldName);
        if (value == null || !value.isString() || value.stringValue().isBlank()) {
            throw new IllegalStateException("CouchDB document must contain a nonblank " + fieldName);
        }
        return value.stringValue();
    }

    /**
     * Checks whether a document belongs to the requested persistent entity.
     *
     * @param document CouchDB document tree
     * @param entityMetadata persistent entity metadata
     * @return whether the document matches the entity
     */
    private static boolean matchesEntity(JsonNode document, CouchPersistentEntity<?> entityMetadata) {
        var id = document.get(ID_FIELD);
        var discriminator = document.get(DISCRIMINATOR_FIELD);
        return (id == null || !id.isString() || !id.stringValue().startsWith("_design/"))
                && discriminator != null
                && discriminator.isString()
                && entityMetadata.getDiscriminator().equals(discriminator.stringValue());
    }

    /**
     * Rejects blank document identifiers used as operation inputs.
     *
     * @param id supplied identifier
     */
    private static void requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }
}
