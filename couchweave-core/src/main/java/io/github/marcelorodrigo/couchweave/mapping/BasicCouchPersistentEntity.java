package io.github.marcelorodrigo.couchweave.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.Alias;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.model.BasicPersistentEntity;

final class BasicCouchPersistentEntity<T> extends BasicPersistentEntity<T, CouchPersistentProperty>
        implements CouchPersistentEntity<T> {

    private static final Set<String> RESERVED_FIELDS =
            Set.of(CouchFieldNames.ID, CouchFieldNames.REVISION, CouchFieldNames.DISCRIMINATOR);

    private final String discriminator;
    private final String database;

    private CouchPersistentProperty idProperty;
    private CouchPersistentProperty revisionProperty;
    private boolean verified;

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

    boolean isVerified() {
        return verified;
    }

    private void verifyIdentifier() {
        if (idProperty == null) {
            throw mappingException("must declare exactly one @Id property");
        }
        verifyStringProperty(idProperty, "ID");
        verifyNotRenamed(idProperty, "ID");
    }

    private void verifyRevision() {
        if (revisionProperty == null) {
            return;
        }
        verifyStringProperty(revisionProperty, "revision");
        verifyNotRenamed(revisionProperty, "revision");
    }

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

    private void verifyStringProperty(CouchPersistentProperty property, String role) {
        if (!String.class.equals(property.getType())) {
            throw mappingException("%s property '%s' must be declared as String".formatted(role, property.getName()));
        }
    }

    private void verifyNotRenamed(CouchPersistentProperty property, String role) {
        if (property instanceof BasicCouchPersistentProperty basicProperty && basicProperty.hasCouchFieldAnnotation()) {
            throw mappingException("%s property '%s' cannot declare @CouchField".formatted(role, property.getName()));
        }
    }

    private String resolveSetting(String configured, String fallback, String settingName) {
        if (!configured.isEmpty() && configured.isBlank()) {
            throw mappingException("declares a blank %s".formatted(settingName));
        }
        return configured.isEmpty() ? fallback : configured;
    }

    private MappingException mappingException(String detail) {
        return new MappingException("Type %s %s".formatted(getType().getName(), detail));
    }
}
