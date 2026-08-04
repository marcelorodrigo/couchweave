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

final class BasicCouchPersistentProperty extends AnnotationBasedPersistentProperty<CouchPersistentProperty>
        implements CouchPersistentProperty {

    private static final String ID_FIELD = "_id";
    private static final String REVISION_FIELD = "_rev";

    private final CouchField couchField;
    private final String fieldName;

    BasicCouchPersistentProperty(
            Property property, PersistentEntity<?, CouchPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {
        super(property, owner, simpleTypeHolder);
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
        return findPropertyAnnotation(Revision.class) != null;
    }

    @Override
    public boolean isVersionProperty() {
        return isRevisionProperty();
    }

    boolean hasCouchFieldAnnotation() {
        return couchField != null;
    }

    @Override
    protected Association<CouchPersistentProperty> createAssociation() {
        return new Association<>(this, null);
    }

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

    private String resolveFieldName() {
        if (isIdProperty()) {
            return ID_FIELD;
        }
        if (isRevisionProperty()) {
            return REVISION_FIELD;
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
