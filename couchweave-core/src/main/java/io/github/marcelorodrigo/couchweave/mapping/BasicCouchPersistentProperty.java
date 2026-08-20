package io.github.marcelorodrigo.couchweave.mapping;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Objects;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/** Represents persistent metadata for a CouchDB property. */
final class BasicCouchPersistentProperty extends AnnotationBasedPersistentProperty<CouchPersistentProperty>
        implements CouchPersistentProperty {

    /** Stores the CouchField annotation. */
    private final CouchField couchField;
    /** Indicates whether this is a revision property. */
    private final boolean revisionProperty;
    /** Stores the resolved field name. */
    private final String fieldName;

    /**
     * Creates persistent property metadata.
     *
     * @param property the persistent property
     * @param owner the owning persistent entity
     * @param simpleTypeHolder the simple type holder
     */
    BasicCouchPersistentProperty(
            Property property, PersistentEntity<?, CouchPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {
        super(property, owner, simpleTypeHolder);
        this.revisionProperty = findPropertyAnnotation(Revision.class) != null;
        this.couchField = findPropertyAnnotation(CouchField.class);
        this.fieldName = resolveFieldName();
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public boolean hasExplicitFieldName() {
        return couchField != null && !couchField.value().isEmpty();
    }

    @Override
    public boolean isRevisionProperty() {
        return revisionProperty;
    }

    @Override
    public boolean isVersionProperty() {
        return isRevisionProperty();
    }

    @Override
    public boolean isIdProperty() {
        return super.isIdProperty() || findPropertyAnnotation(org.springframework.data.annotation.Id.class) != null;
    }

    /**
     * Returns whether the property declares a CouchField annotation.
     *
     * @return whether the property declares a CouchField annotation
     */
    boolean hasCouchFieldAnnotation() {
        return couchField != null;
    }

    @Override
    protected Association<CouchPersistentProperty> createAssociation() {
        return new Association<>(this, null);
    }

    /**
     * Finds an annotation declared on the property or its record component.
     *
     * @param <A> the annotation type
     * @param annotationType the annotation type to find
     * @return the matching annotation, or null when none is present
     */
    private <A extends Annotation> A findPropertyAnnotation(Class<A> annotationType) {
        var annotation = findAnnotation(annotationType);
        if (annotation != null || !getOwner().getType().isRecord()) {
            return annotation;
        }
        return Arrays.stream(getOwner().getType().getRecordComponents())
                .filter(component -> component.getName().equals(getName()))
                .map(component -> AnnotatedElementUtils.findMergedAnnotation(component, annotationType))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Resolves the stored field name.
     *
     * @return the resolved field name
     */
    private String resolveFieldName() {
        if (isIdProperty()) {
            return CouchFieldNames.ID;
        }
        if (isRevisionProperty()) {
            return CouchFieldNames.REVISION;
        }
        if (couchField == null || couchField.value().isEmpty()) {
            return getName();
        }
        if (couchField.value().isBlank()) {
            throw new MappingException("Type %s property '%s' declares a blank @CouchField name"
                    .formatted(getOwner().getType().getName(), getName()));
        }
        return couchField.value();
    }
}
