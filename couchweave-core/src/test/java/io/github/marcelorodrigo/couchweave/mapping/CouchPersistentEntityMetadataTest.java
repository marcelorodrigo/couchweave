package io.github.marcelorodrigo.couchweave.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;

class CouchPersistentEntityMetadataTest {

    @Test
    @DisplayName("should expose explicit document metadata for a mutable class")
    void shouldExposeExplicitDocumentMetadataForAMutableClass() {
        // given
        var context = initializedContext(Book.class);

        // when
        var entity = context.getRequiredPersistentEntity(Book.class);

        // then
        assertThat(entity.getDiscriminator()).isEqualTo("book");
        assertThat(entity.getTypeAlias().hasValue("book")).isTrue();
        assertThat(entity.getDatabase()).isEqualTo("library");
        assertThat(entity.getIdProperty())
                .extracting(CouchPersistentProperty::getName)
                .isEqualTo("documentId");
        assertThat(entity.getVersionProperty())
                .extracting(CouchPersistentProperty::getName)
                .isEqualTo("revision");
    }

    @Test
    @DisplayName("should derive document metadata when annotation settings are omitted")
    void shouldDeriveDocumentMetadataWhenAnnotationSettingsAreOmitted() {
        // given
        var context = initializedContext(DefaultDocument.class);

        // when
        var entity = context.getRequiredPersistentEntity(DefaultDocument.class);

        // then
        assertThat(entity.getDiscriminator()).isEqualTo("DefaultDocument");
        assertThat(entity.getDatabase()).isEqualTo("default-db");
    }

    @Test
    @DisplayName("should expose equivalent document metadata for a record")
    void shouldExposeEquivalentDocumentMetadataForARecord() {
        // given
        var context = initializedContext(MappedRecord.class);

        // when
        var entity = context.getRequiredPersistentEntity(MappedRecord.class);

        // then
        assertThat(entity.getDiscriminator()).isEqualTo("record");
        assertThat(entity.getDatabase()).isEqualTo("default-db");
        assertThat(entity.getIdProperty())
                .extracting(CouchPersistentProperty::getName)
                .isEqualTo("id");
        assertThat(entity.getVersionProperty())
                .extracting(CouchPersistentProperty::getName)
                .isEqualTo("revision");
    }

    @Test
    @DisplayName("should discover inherited document and property metadata")
    void shouldDiscoverInheritedDocumentAndPropertyMetadata() {
        // given
        var context = initializedContext(InheritedDocument.class);

        // when
        var entity = context.getRequiredPersistentEntity(InheritedDocument.class);

        // then
        assertThat(entity.getDiscriminator()).isEqualTo("inherited");
        assertThat(entity.getDatabase()).isEqualTo("archive");
        assertThat(entity.getIdProperty())
                .extracting(CouchPersistentProperty::getName)
                .isEqualTo("id");
        assertThat(entity.getPersistentProperty("baseName"))
                .extracting(CouchPersistentProperty::getFieldName)
                .isEqualTo("base_name");
    }

    private static CouchMappingContext initializedContext(Class<?> type) {
        var context = new CouchMappingContext("default-db");
        context.setInitialEntitySet(Set.of(type));
        context.initialize();
        return context;
    }

    @CouchDocument(type = "book", database = "library")
    static class Book {
        @Id
        String documentId;

        @Revision
        String revision;

        String title;
    }

    @CouchDocument
    static class DefaultDocument {
        @Id
        String id;
    }

    @CouchDocument(type = "record")
    record MappedRecord(@Id String id, @Revision String revision, String title) {}

    @CouchDocument(type = "inherited", database = "archive")
    static class BaseDocument {
        @Id
        String id;

        @CouchField("base_name")
        String baseName;
    }

    static final class InheritedDocument extends BaseDocument {
        String childName;
    }
}
