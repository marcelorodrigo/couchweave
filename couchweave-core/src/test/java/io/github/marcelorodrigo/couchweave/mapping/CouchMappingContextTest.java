package io.github.marcelorodrigo.couchweave.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.MappingException;

class CouchMappingContextTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("should reject a missing or blank default database")
    void shouldRejectAMissingOrBlankDefaultDatabase(String defaultDatabase) {
        // given / when / then
        assertThatThrownBy(() -> new CouchMappingContext(defaultDatabase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("defaultDatabase");
    }

    @Test
    @DisplayName("should register only CouchDB document types")
    void shouldRegisterOnlyCouchDbDocumentTypes() {
        // given
        var context = contextFor(DocumentWithValue.class);

        // when
        context.initialize();

        // then
        assertThat(context.hasPersistentEntityFor(DocumentWithValue.class)).isTrue();
        assertThat(context.hasPersistentEntityFor(UnrelatedValue.class)).isFalse();
        assertThat(context.hasPersistentEntityFor(String.class)).isFalse();
        assertThat(context.getPersistentEntities())
                .extracting(entity -> entity.getType().getName())
                .containsExactly(DocumentWithValue.class.getName());
    }

    @Test
    @DisplayName("should reject an initial type without CouchDocument")
    void shouldRejectAnInitialTypeWithoutCouchDocument() {
        // given
        var context = contextFor(UnannotatedDocument.class);

        // when / then
        assertThatThrownBy(context::initialize)
                .isInstanceOf(MappingException.class)
                .hasMessageContaining(UnannotatedDocument.class.getName())
                .hasMessageContaining("@CouchDocument");
    }

    @ParameterizedTest
    @MethodSource("blankDocumentSettings")
    @DisplayName("should reject a whitespace-only document setting")
    void shouldRejectAWhitespaceOnlyDocumentSetting(Class<?> documentType, String setting) {
        // given
        var context = contextFor(documentType);

        // when / then
        assertThatThrownBy(context::initialize)
                .isInstanceOf(MappingException.class)
                .hasMessageContaining(documentType.getName())
                .hasMessageContaining(setting);
    }

    @ParameterizedTest
    @MethodSource("invalidIdentifierMappings")
    @DisplayName("should reject an invalid identifier mapping")
    void shouldRejectAnInvalidIdentifierMapping(Class<?> documentType, String detail) {
        // given
        var context = contextFor(documentType);

        // when / then
        assertThatThrownBy(context::initialize)
                .isInstanceOf(MappingException.class)
                .hasMessageContaining(documentType.getName())
                .hasMessageContaining(detail);
    }

    @ParameterizedTest
    @MethodSource("invalidRevisionMappings")
    @DisplayName("should reject an invalid revision mapping")
    void shouldRejectAnInvalidRevisionMapping(Class<?> documentType, String detail) {
        // given
        var context = contextFor(documentType);

        // when / then
        assertThatThrownBy(context::initialize)
                .isInstanceOf(MappingException.class)
                .hasMessageContaining(documentType.getName())
                .hasMessageContaining(detail);
    }

    @Test
    @DisplayName("should reject a property that is both identifier and revision")
    void shouldRejectAPropertyThatIsBothIdentifierAndRevision() {
        // given
        var context = contextFor(OverlappingSpecialPropertyDocument.class);

        // when / then
        assertThatThrownBy(context::initialize)
                .isInstanceOf(MappingException.class)
                .hasMessageContaining(OverlappingSpecialPropertyDocument.class.getName())
                .hasMessageContaining("both ID and revision");
    }

    @ParameterizedTest
    @MethodSource("renamedSpecialProperties")
    @DisplayName("should reject CouchField on an identifier or revision property")
    void shouldRejectCouchFieldOnAnIdentifierOrRevisionProperty(Class<?> documentType, String role) {
        // given
        var context = contextFor(documentType);

        // when / then
        assertThatThrownBy(context::initialize)
                .isInstanceOf(MappingException.class)
                .hasMessageContaining(documentType.getName())
                .hasMessageContaining(role)
                .hasMessageContaining("@CouchField");
    }

    @Test
    @DisplayName("should reject duplicate explicit discriminators")
    void shouldRejectDuplicateExplicitDiscriminators() {
        // given
        var context = contextFor(FirstAliasedDocument.class, SecondAliasedDocument.class);

        // when / then
        assertThatThrownBy(context::initialize)
                .isInstanceOf(MappingException.class)
                .hasMessageContaining("shared")
                .hasMessageContaining(FirstAliasedDocument.class.getName())
                .hasMessageContaining(SecondAliasedDocument.class.getName());
    }

    @Test
    @DisplayName("should reject duplicate derived discriminators")
    void shouldRejectDuplicateDerivedDiscriminators() {
        // given
        var context = contextFor(FirstGroup.Duplicate.class, SecondGroup.Duplicate.class);

        // when / then
        assertThatThrownBy(context::initialize)
                .isInstanceOf(MappingException.class)
                .hasMessageContaining("Duplicate")
                .hasMessageContaining(FirstGroup.Duplicate.class.getName())
                .hasMessageContaining(SecondGroup.Duplicate.class.getName());
    }

    @Test
    @DisplayName("should fail required lookup for an unknown document type")
    void shouldFailRequiredLookupForAnUnknownDocumentType() {
        // given
        var context = contextFor(DocumentWithValue.class);
        context.initialize();

        // when / then
        assertThatThrownBy(() -> context.getRequiredPersistentEntity(UnknownDocument.class))
                .isInstanceOf(MappingException.class)
                .hasMessageContaining(UnknownDocument.class.getName());
    }

    private static Stream<Arguments> blankDocumentSettings() {
        return Stream.of(
                Arguments.of(BlankTypeDocument.class, "type discriminator"),
                Arguments.of(BlankDatabaseDocument.class, "database"));
    }

    private static Stream<Arguments> invalidIdentifierMappings() {
        return Stream.of(
                Arguments.of(MissingIdDocument.class, "exactly one @Id"),
                Arguments.of(DuplicateIdDocument.class, "multiple ID"),
                Arguments.of(NonStringIdDocument.class, "must be declared as String"));
    }

    private static Stream<Arguments> invalidRevisionMappings() {
        return Stream.of(
                Arguments.of(DuplicateRevisionDocument.class, "multiple revision"),
                Arguments.of(NonStringRevisionDocument.class, "must be declared as String"));
    }

    private static Stream<Arguments> renamedSpecialProperties() {
        return Stream.of(
                Arguments.of(RenamedIdDocument.class, "ID"),
                Arguments.of(EmptyRenameIdDocument.class, "ID"),
                Arguments.of(RenamedRevisionDocument.class, "revision"));
    }

    private static CouchMappingContext contextFor(Class<?>... types) {
        var context = new CouchMappingContext("default-db");
        context.setInitialEntitySet(Set.of(types));
        return context;
    }

    @CouchDocument
    static class DocumentWithValue {
        @Id
        String id;

        UnrelatedValue value;
    }

    static class UnrelatedValue {
        String name;
    }

    static class UnannotatedDocument {
        @Id
        String id;
    }

    @CouchDocument(type = " ")
    static class BlankTypeDocument {
        @Id
        String id;
    }

    @CouchDocument(database = " ")
    static class BlankDatabaseDocument {
        @Id
        String id;
    }

    @CouchDocument
    static class MissingIdDocument {
        String value;
    }

    @CouchDocument
    static class DuplicateIdDocument {
        @Id
        String firstId;

        @Id
        String secondId;
    }

    @CouchDocument
    static class NonStringIdDocument {
        @Id
        long id;
    }

    @CouchDocument
    static class DuplicateRevisionDocument {
        @Id
        String id;

        @Revision
        String firstRevision;

        @Revision
        String secondRevision;
    }

    @CouchDocument
    static class NonStringRevisionDocument {
        @Id
        String id;

        @Revision
        long revision;
    }

    @CouchDocument
    static class OverlappingSpecialPropertyDocument {
        @Id
        @Revision
        String id;
    }

    @CouchDocument
    static class RenamedIdDocument {
        @Id
        @CouchField("identifier")
        String id;
    }

    @CouchDocument
    static class EmptyRenameIdDocument {
        @Id
        @CouchField
        String id;
    }

    @CouchDocument
    static class RenamedRevisionDocument {
        @Id
        String id;

        @Revision
        @CouchField("revision")
        String revision;
    }

    @CouchDocument(type = "shared")
    static class FirstAliasedDocument {
        @Id
        String id;
    }

    @CouchDocument(type = "shared")
    static class SecondAliasedDocument {
        @Id
        String id;
    }

    static class FirstGroup {
        @CouchDocument
        static class Duplicate {
            @Id
            String id;
        }
    }

    static class SecondGroup {
        @CouchDocument
        static class Duplicate {
            @Id
            String id;
        }
    }

    @CouchDocument
    static class UnknownDocument {
        @Id
        String id;
    }
}
