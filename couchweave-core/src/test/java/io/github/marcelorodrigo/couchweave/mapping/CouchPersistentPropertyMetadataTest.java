package io.github.marcelorodrigo.couchweave.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.MappingException;

class CouchPersistentPropertyMetadataTest {

    @Test
    @DisplayName("should resolve special default and renamed field metadata")
    void shouldResolveSpecialDefaultAndRenamedFieldMetadata() {
        // given
        var context = initializedContext(PropertyDocument.class);

        // when
        var entity = context.getRequiredPersistentEntity(PropertyDocument.class);

        // then
        assertThat(entity.getPersistentProperty("id").getFieldName()).isEqualTo("_id");
        assertThat(entity.getPersistentProperty("id").hasExplicitFieldName()).isFalse();
        assertThat(entity.getPersistentProperty("revision").getFieldName()).isEqualTo("_rev");
        assertThat(entity.getPersistentProperty("revision").isRevisionProperty())
                .isTrue();
        assertThat(entity.getPersistentProperty("title").getFieldName()).isEqualTo("display_title");
        assertThat(entity.getPersistentProperty("title").hasExplicitFieldName()).isTrue();
        assertThat(entity.getPersistentProperty("author").getFieldName()).isEqualTo("author");
        assertThat(entity.getPersistentProperty("nickname").hasExplicitFieldName())
                .isFalse();
    }

    @Test
    @DisplayName("should resolve field annotations on accessors")
    void shouldResolveFieldAnnotationsOnAccessors() {
        // given
        var context = initializedContext(AccessorDocument.class);

        // when
        var property =
                context.getRequiredPersistentEntity(AccessorDocument.class).getPersistentProperty("description");

        // then
        assertThat(property.getFieldName()).isEqualTo("summary");
        assertThat(property.hasExplicitFieldName()).isTrue();
    }

    @Test
    @DisplayName("should resolve composed annotations on record components")
    void shouldResolveComposedAnnotationsOnRecordComponents() {
        // given
        var context = initializedContext(ComposedRecord.class);

        // when
        var entity = context.getRequiredPersistentEntity(ComposedRecord.class);

        // then
        assertThat(entity.getPersistentProperty("revision").isRevisionProperty())
                .isTrue();
        assertThat(entity.getPersistentProperty("name").getFieldName()).isEqualTo("display_name");
    }

    @Test
    @DisplayName("should resolve a composed ID annotation on a record component")
    void shouldResolveAComposedIdAnnotationOnARecordComponent() {
        // given
        var context = initializedContext(ComposedIdRecord.class);

        // when
        var entity = context.getRequiredPersistentEntity(ComposedIdRecord.class);

        // then
        assertThat(entity.getIdProperty())
                .extracting(CouchPersistentProperty::getName)
                .isEqualTo("id");
        assertThat(entity.getRequiredIdProperty().getFieldName()).isEqualTo("_id");
    }

    @ParameterizedTest
    @MethodSource("reservedFieldMappings")
    @DisplayName("should reject an ordinary property mapped to a reserved field")
    void shouldRejectAnOrdinaryPropertyMappedToAReservedField(Class<?> documentType, String fieldName) {
        // given
        var context = contextFor(documentType);

        // when / then
        assertThatThrownBy(context::initialize)
                .isInstanceOf(MappingException.class)
                .hasMessageContaining(documentType.getName())
                .hasMessageContaining(fieldName);
    }

    @Test
    @DisplayName("should reject duplicate stored field names")
    void shouldRejectDuplicateStoredFieldNames() {
        // given
        var context = contextFor(DuplicateStoredFieldDocument.class);

        // when / then
        assertThatThrownBy(context::initialize)
                .isInstanceOf(MappingException.class)
                .hasMessageContaining(DuplicateStoredFieldDocument.class.getName())
                .hasMessageContaining("same_name");
    }

    @Test
    @DisplayName("should reject a whitespace-only field override")
    void shouldRejectAWhitespaceOnlyFieldOverride() {
        // given
        var context = contextFor(BlankFieldDocument.class);

        // when / then
        assertThatThrownBy(context::initialize)
                .isInstanceOf(MappingException.class)
                .hasMessageContaining(BlankFieldDocument.class.getName())
                .hasMessageContaining("value");
    }

    private static Stream<Arguments> reservedFieldMappings() {
        return Stream.of(
                Arguments.of(NaturalIdFieldDocument.class, "_id"),
                Arguments.of(NaturalRevisionFieldDocument.class, "_rev"),
                Arguments.of(NaturalTypeFieldDocument.class, "couchweave_type"),
                Arguments.of(ExplicitIdFieldDocument.class, "_id"),
                Arguments.of(ExplicitRevisionFieldDocument.class, "_rev"),
                Arguments.of(ExplicitTypeFieldDocument.class, "couchweave_type"));
    }

    private static CouchMappingContext initializedContext(Class<?> type) {
        var context = contextFor(type);
        context.initialize();
        return context;
    }

    private static CouchMappingContext contextFor(Class<?> type) {
        var context = new CouchMappingContext("default-db");
        context.setInitialEntitySet(Set.of(type));
        return context;
    }

    @CouchDocument
    static class PropertyDocument {
        @Id
        String id;

        @Revision
        String revision;

        @DisplayTitle
        String title;

        String author;

        @CouchField
        String nickname;
    }

    @CouchDocument
    static class AccessorDocument {
        @Id
        String id;

        private String description;

        @CouchField("summary")
        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    @CouchDocument
    record ComposedRecord(
            @Id String id,
            @DocumentRevision String revision,
            @StoredDisplayName String name) {}

    @CouchDocument
    record ComposedIdRecord(@DocumentId String id) {}

    @CouchDocument
    static class NaturalIdFieldDocument {
        @Id
        String id;

        String _id;
    }

    @CouchDocument
    static class NaturalRevisionFieldDocument {
        @Id
        String id;

        String _rev;
    }

    @CouchDocument
    static class NaturalTypeFieldDocument {
        @Id
        String id;

        String couchweave_type;
    }

    @CouchDocument
    static class ExplicitIdFieldDocument {
        @Id
        String id;

        @CouchField("_id")
        String value;
    }

    @CouchDocument
    static class ExplicitRevisionFieldDocument {
        @Id
        String id;

        @CouchField("_rev")
        String value;
    }

    @CouchDocument
    static class ExplicitTypeFieldDocument {
        @Id
        String id;

        @CouchField("couchweave_type")
        String value;
    }

    @CouchDocument
    static class DuplicateStoredFieldDocument {
        @Id
        String id;

        @CouchField("same_name")
        String first;

        @CouchField("same_name")
        String second;
    }

    @CouchDocument
    static class BlankFieldDocument {
        @Id
        String id;

        @CouchField(" ")
        String value;
    }

    @CouchField("display_title")
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
    @interface DisplayTitle {}

    @CouchField("display_name")
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface StoredDisplayName {}

    @Revision
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface DocumentRevision {}

    @Id
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface DocumentId {}
}
