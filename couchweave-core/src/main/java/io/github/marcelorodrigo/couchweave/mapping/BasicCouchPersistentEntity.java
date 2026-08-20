package io.github.marcelorodrigo.couchweave.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.Alias;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.model.BasicPersistentEntity;

/** Represents persistent metadata for a CouchDB document. */
final class BasicCouchPersistentEntity<T> extends BasicPersistentEntity<T, CouchPersistentProperty>
        implements CouchPersistentEntity<T> {

    /** Contains field names reserved by CouchDB and Couchweave. */
    private static final Set<String> RESERVED_FIELDS =
            Set.of(CouchFieldNames.ID, CouchFieldNames.REVISION, CouchFieldNames.DISCRIMINATOR);

    /** Stores the document type discriminator. */
    private final String discriminator;
    /** Stores the target database name. */
    private final String database;

    /** Stores the ID property. */
    private CouchPersistentProperty idProperty;
    /** Stores the revision property. */
    private CouchPersistentProperty revisionProperty;
    /** Indicates whether the entity has been verified. */
    private boolean verified;

    /**
     * Creates persistent entity metadata from type information and a default database.
     *
     * @param information the entity type information
     * @param defaultDatabase the default database name
     */
    BasicCouchPersistentEntity(TypeInformation<T> information, String defaultDatabase) {
        super(information);
        var annotation = findAnnotation(CouchDocument.class);
        if (annotation == null) {
            throw mappingException("is missing @CouchDocument");
        }
        this.discriminator = resolveSetting(annotation.type(), getType().getSimpleName(), "type discriminator");
        this.database = resolveSetting(annotation.database(), defaultDatabase, "database");
    }

    @Override
    public String getDiscriminator() {
        return discriminator;
    }

    @Override
    public String getDatabase() {
        return database;
    }

    @Override
    public Alias getTypeAlias() {
        return Alias.of(discriminator);
    }

    @Override
    public void addPersistentProperty(CouchPersistentProperty property) {
        if (property.isIdProperty() && property.isRevisionProperty()) {
            throw mappingException("property '%s' cannot be both ID and revision".formatted(property.getName()));
        }
        if (property.isIdProperty()) {
            if (idProperty != null) {
                throw mappingException("declares multiple ID properties '%s' and '%s'"
                        .formatted(idProperty.getName(), property.getName()));
            }
            idProperty = property;
        }
        if (property.isRevisionProperty()) {
            if (revisionProperty != null) {
                throw mappingException("declares multiple revision properties '%s' and '%s'"
                        .formatted(revisionProperty.getName(), property.getName()));
            }
            revisionProperty = property;
        }
        super.addPersistentProperty(property);
    }

    @Override
    public void verify() {
        super.verify();
        verifyIdentifier();
        verifyRevision();
        verifyStoredNames();
        verified = true;
    }

    /**
     * @return whether the entity has been verified
     */
    boolean isVerified() {
        return verified;
    }

    /** Verifies that the entity declares a valid identifier. */
    private void verifyIdentifier() {
        if (idProperty == null) {
            throw mappingException("must declare exactly one @Id property");
        }
        verifyStringProperty(idProperty, "ID");
        verifyNotRenamed(idProperty, "ID");
    }

    /** Verifies that the entity declares a valid revision property. */
    private void verifyRevision() {
        if (revisionProperty == null) {
            return;
        }
        verifyStringProperty(revisionProperty, "revision");
        verifyNotRenamed(revisionProperty, "revision");
    }

    /** Verifies that stored field names are valid and unique. */
    private void verifyStoredNames() {
        Map<String, CouchPersistentProperty> storedProperties = new HashMap<>();
        for (var property : this) {
            var fieldName = property.getFieldName();
            if (!property.isIdProperty() && !property.isRevisionProperty() && RESERVED_FIELDS.contains(fieldName)) {
                throw mappingException(
                        "property '%s' resolves to reserved field '%s'".formatted(property.getName(), fieldName));
            }
            var existing = storedProperties.putIfAbsent(fieldName, property);
            if (existing != null) {
                throw mappingException("properties '%s' and '%s' resolve to duplicate stored field '%s'"
                        .formatted(existing.getName(), property.getName(), fieldName));
            }
        }
    }

    /**
     * Verifies that a special property uses the String type.
     *
     * @param property the property to verify
     * @param role the property role
     */
    private void verifyStringProperty(CouchPersistentProperty property, String role) {
        if (!String.class.equals(property.getType())) {
            throw mappingException("%s property '%s' must be declared as String".formatted(role, property.getName()));
        }
    }

    /**
     * Verifies that a special property has not been renamed.
     *
     * @param property the property to verify
     * @param role the property role
     */
    private void verifyNotRenamed(CouchPersistentProperty property, String role) {
        if (property instanceof BasicCouchPersistentProperty basicProperty && basicProperty.hasCouchFieldAnnotation()) {
            throw mappingException("%s property '%s' cannot declare @CouchField".formatted(role, property.getName()));
        }
    }

    /**
     * Resolves a configured setting or its fallback value.
     *
     * @param configured the configured value
     * @param fallback the fallback value
     * @param settingName the setting name
     * @return the resolved setting value
     */
    private String resolveSetting(String configured, String fallback, String settingName) {
        if (!configured.isEmpty() && configured.isBlank()) {
            throw mappingException("declares a blank %s".formatted(settingName));
        }
        return configured.isEmpty() ? fallback : configured;
    }

    /**
     * Creates a mapping exception for this entity.
     *
     * @param detail the failure detail
     * @return the mapping exception
     */
    private MappingException mappingException(String detail) {
        return new MappingException("Type %s %s".formatted(getType().getName(), detail));
    }
}
