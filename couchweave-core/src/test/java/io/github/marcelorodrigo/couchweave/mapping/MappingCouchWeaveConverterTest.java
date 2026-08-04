package io.github.marcelorodrigo.couchweave.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.annotation.Id;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mapping.MappingException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class MappingCouchWeaveConverterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("should round trip a mutable entity")
    void shouldRoundTripAMutableEntity() {
        // given
        var converter = converterFor(MutableBook.class);
        var source = new MutableBook();
        source.id = "book-1";
        source.revision = "2-revision";
        source.title = "The Left Hand of Darkness";
        source.author = new Author("Ursula K. Le Guin");

        // when
        var result = converter.read(MutableBook.class, converter.write(source));

        // then
        assertThat(result.id).isEqualTo(source.id);
        assertThat(result.revision).isEqualTo(source.revision);
        assertThat(result.title).isEqualTo(source.title);
        assertThat(result.author).isEqualTo(source.author);
    }

    @Test
    @DisplayName("should round trip an immutable record with renamed and nested values")
    void shouldRoundTripAnImmutableRecordWithRenamedAndNestedValues() {
        // given
        var converter = converterFor(ImmutableBook.class);
        var source = new ImmutableBook("book-2", "3-revision", "Kindred", new Author("Octavia E. Butler"), "classic");

        // when
        var document = converter.write(source);
        var result = converter.read(ImmutableBook.class, document);

        // then
        assertThat(document.get("display_title").stringValue()).isEqualTo("Kindred");
        assertThat(document.has("title")).isFalse();
        assertThat(result).isEqualTo(source);
    }

    @Test
    @DisplayName("should preserve an assigned identifier and revision")
    void shouldPreserveAnAssignedIdentifierAndRevision() {
        // given
        var converter = converterFor(ImmutableBook.class);
        var source = new ImmutableBook("assigned-id", "4-revision", "Parable", null, null);

        // when
        var document = converter.write(source);

        // then
        assertThat(document.get("_id").stringValue()).isEqualTo("assigned-id");
        assertThat(document.get("_rev").stringValue()).isEqualTo("4-revision");
    }

    @Test
    @DisplayName("should generate an identifier without mutating the source")
    void shouldGenerateAnIdentifierWithoutMutatingTheSource() {
        // given
        var converter = converterFor(MutableBook.class);
        var source = new MutableBook();
        source.title = "A Memory Called Empire";

        // when
        var document = converter.write(source);

        // then
        assertThat(UUID.fromString(document.get("_id").stringValue())).isNotNull();
        assertThat(source.id).isNull();
    }

    @Test
    @DisplayName("should omit a missing revision from a new document")
    void shouldOmitAMissingRevisionFromANewDocument() {
        // given
        var converter = converterFor(ImmutableBook.class);
        var source = new ImmutableBook("book-3", null, "Ancillary Justice", null, null);

        // when
        var document = converter.write(source);

        // then
        assertThat(document.has("_rev")).isFalse();
    }

    @Test
    @DisplayName("should write ordinary null values as JSON null")
    void shouldWriteOrdinaryNullValuesAsJsonNull() {
        // given
        var converter = converterFor(ImmutableBook.class);
        var source = new ImmutableBook("book-4", null, "The Dispossessed", null, null);

        // when
        var document = converter.write(source);

        // then
        assertThat(document.get("author").isNull()).isTrue();
        assertThat(document.get("note").isNull()).isTrue();
    }

    @Test
    @DisplayName("should read missing optional fields and ignore unknown fields")
    void shouldReadMissingOptionalFieldsAndIgnoreUnknownFields() throws Exception {
        // given
        var converter = converterFor(ImmutableBook.class);
        var document = objectMapper.readTree("""
                {"_id":"book-5","couchweave_type":"book","display_title":"Dawn","unknown":true}
                """);

        // when
        var result = converter.read(ImmutableBook.class, document);

        // then
        assertThat(result).isEqualTo(new ImmutableBook("book-5", null, "Dawn", null, null));
    }

    @Test
    @DisplayName("should write a property with a custom conversion")
    void shouldWriteAPropertyWithACustomConversion() {
        // given
        var context = initializedContext(CustomDocument.class);
        var conversions =
                new CouchWeaveCustomConversions(List.of(CodeWriteConverter.INSTANCE, CodeReadConverter.INSTANCE));
        var converter = new MappingCouchWeaveConverter(context, objectMapper, conversions);
        var source = new CustomDocument("custom-1", new Code("code-value"));

        // when
        var document = converter.write(source);

        // then
        assertThat(document.get("code").stringValue()).isEqualTo("code-value");
    }

    @Test
    @DisplayName("should read a property with a custom conversion")
    void shouldReadAPropertyWithACustomConversion() throws Exception {
        // given
        var context = initializedContext(CustomDocument.class);
        var conversions =
                new CouchWeaveCustomConversions(List.of(CodeWriteConverter.INSTANCE, CodeReadConverter.INSTANCE));
        var converter = new MappingCouchWeaveConverter(context, objectMapper, conversions);
        var document =
                objectMapper.readTree("{\"_id\":\"custom-1\",\"couchweave_type\":\"custom\",\"code\":\"code-value\"}");

        // when
        var result = converter.read(CustomDocument.class, document);

        // then
        assertThat(result).isEqualTo(new CustomDocument("custom-1", new Code("code-value")));
    }

    @Test
    @DisplayName("should preserve nested collection element types")
    void shouldPreserveNestedCollectionElementTypes() {
        // given
        var converter = converterFor(CollectionDocument.class);
        var source = new CollectionDocument("collection-1", List.of(new Author("N. K. Jemisin")));

        // when
        var result = converter.read(CollectionDocument.class, converter.write(source));

        // then
        assertThat(result).isEqualTo(source);
    }

    @Test
    @DisplayName("should use the primitive default for a missing property")
    void shouldUseThePrimitiveDefaultForAMissingProperty() throws Exception {
        // given
        var converter = converterFor(PrimitiveDocument.class);
        var document = objectMapper.readTree("{\"_id\":\"primitive-1\",\"couchweave_type\":\"primitive\"}");

        // when
        var result = converter.read(PrimitiveDocument.class, document);

        // then
        assertThat(result.count()).isZero();
    }

    @Test
    @DisplayName("should reject a null source entity")
    void shouldRejectANullSourceEntity() {
        // given
        var converter = converterFor(ImmutableBook.class);

        // when / then
        assertThatThrownBy(() -> converter.write(null))
                .isInstanceOf(MappingException.class)
                .hasMessageContaining("null");
    }

    @Test
    @DisplayName("should reject a missing target type")
    void shouldRejectAMissingTargetType() {
        // given
        var converter = converterFor(ImmutableBook.class);
        var document = objectMapper.createObjectNode();

        // when / then
        assertThatThrownBy(() -> converter.read(null, document))
                .isInstanceOf(MappingException.class)
                .hasMessageContaining("target type");
    }

    @Test
    @DisplayName("should report property context when reading fails")
    void shouldReportPropertyContextWhenReadingFails() throws Exception {
        // given
        var converter = converterFor(ImmutableBook.class);
        var document = objectMapper.readTree("""
                {"_id":"book-6","couchweave_type":"book","display_title":"Fledgling","author":42}
                """);

        // when / then
        assertThatThrownBy(() -> converter.read(ImmutableBook.class, document))
                .isInstanceOf(MappingException.class)
                .hasMessageContaining(ImmutableBook.class.getName())
                .hasMessageContaining("author");
    }

    @Test
    @DisplayName("should report property context when writing fails")
    void shouldReportPropertyContextWhenWritingFails() {
        // given
        var context = initializedContext(FailingWriteDocument.class);
        var conversions = new CouchWeaveCustomConversions(List.of(FailingWriteConverter.INSTANCE));
        var converter = new MappingCouchWeaveConverter(context, objectMapper, conversions);
        var source = new FailingWriteDocument("failure-1", new FailingValue("value"));

        // when / then
        assertThatThrownBy(() -> converter.write(source))
                .isInstanceOf(MappingException.class)
                .hasMessageContaining(FailingWriteDocument.class.getName())
                .hasMessageContaining("failingValue");
    }

    @Test
    @DisplayName("should report entity context when construction fails")
    void shouldReportEntityContextWhenConstructionFails() throws Exception {
        // given
        var converter = converterFor(ExplodingDocument.class);
        var document = objectMapper.readTree(
                "{\"_id\":\"exploding-1\",\"couchweave_type\":\"exploding\",\"value\":\"explode\"}");

        // when / then
        assertThatThrownBy(() -> converter.read(ExplodingDocument.class, document))
                .isInstanceOf(MappingException.class)
                .hasMessageContaining(ExplodingDocument.class.getName())
                .hasMessageContaining("instantiate");
    }

    @Test
    @DisplayName("should expose configured mapping and conversion services")
    void shouldExposeConfiguredMappingAndConversionServices() {
        // given
        var context = initializedContext(ImmutableBook.class);
        var conversions = new CouchWeaveCustomConversions(List.of());
        var converter = new MappingCouchWeaveConverter(context, objectMapper, conversions);

        // when
        var mappingContext = converter.getMappingContext();
        var conversionService = converter.getConversionService();

        // then
        assertThat(mappingContext).isSameAs(context);
        assertThat(conversionService).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("invalidRootDocuments")
    @DisplayName("should reject a null or non-object root document")
    void shouldRejectANullOrNonObjectRootDocument(JsonNode source) {
        // given
        var converter = converterFor(ImmutableBook.class);

        // when / then
        assertThatThrownBy(() -> converter.read(ImmutableBook.class, source))
                .isInstanceOf(MappingException.class)
                .hasMessageContaining(ImmutableBook.class.getName())
                .hasMessageContaining("non-object");
    }

    @ParameterizedTest
    @MethodSource("invalidIdentityDocuments")
    @DisplayName("should reject an invalid identifier or revision")
    void shouldRejectAnInvalidIdentifierOrRevision(String json, String field) throws Exception {
        // given
        var converter = converterFor(ImmutableBook.class);
        var document = objectMapper.readTree(json);

        // when / then
        assertThatThrownBy(() -> converter.read(ImmutableBook.class, document))
                .isInstanceOf(MappingException.class)
                .hasMessageContaining(ImmutableBook.class.getName())
                .hasMessageContaining(field)
                .hasMessageContaining("nonblank text");
    }

    @ParameterizedTest
    @MethodSource("invalidDiscriminatorDocuments")
    @DisplayName("should reject a missing malformed or mismatched discriminator")
    void shouldRejectAMissingMalformedOrMismatchedDiscriminator(String json, String detail) throws Exception {
        // given
        var converter = converterFor(ImmutableBook.class);
        var document = objectMapper.readTree(json);

        // when / then
        assertThatThrownBy(() -> converter.read(ImmutableBook.class, document))
                .isInstanceOf(MappingException.class)
                .hasMessageContaining(ImmutableBook.class.getName())
                .hasMessageContaining("couchweave_type")
                .hasMessageContaining(detail);
    }

    @Test
    @DisplayName("should reject an unknown mapped type")
    void shouldRejectAnUnknownMappedType() {
        // given
        var converter = converterFor(ImmutableBook.class);
        var source = new UnknownDocument("unknown-1");

        // when / then
        assertThatThrownBy(() -> converter.write(source))
                .isInstanceOf(MappingException.class)
                .hasMessageContaining(UnknownDocument.class.getName());
    }

    @Test
    @DisplayName("should fail invalid metadata before converting properties")
    void shouldFailInvalidMetadataBeforeConvertingProperties() {
        // given
        var context = new CouchMappingContext("test-db");
        context.setInitialEntitySet(Set.of(ReservedFieldDocument.class));

        // when / then
        assertThatThrownBy(context::initialize)
                .isInstanceOf(MappingException.class)
                .hasMessageContaining(ReservedFieldDocument.class.getName())
                .hasMessageContaining("reserved field");
    }

    private MappingCouchWeaveConverter converterFor(Class<?>... types) {
        return new MappingCouchWeaveConverter(
                initializedContext(types), objectMapper, new CouchWeaveCustomConversions(List.of()));
    }

    private CouchMappingContext initializedContext(Class<?>... types) {
        var context = new CouchMappingContext("test-db");
        context.setInitialEntitySet(Set.of(types));
        context.initialize();
        return context;
    }

    private static Stream<JsonNode> invalidRootDocuments() {
        var mapper = new ObjectMapper();
        return Stream.of(null, mapper.createArrayNode(), mapper.stringNode("document"));
    }

    private static Stream<Arguments> invalidIdentityDocuments() {
        return Stream.of(
                Arguments.of("{\"couchweave_type\":\"book\"}", "_id"),
                Arguments.of("{\"_id\":42,\"couchweave_type\":\"book\"}", "_id"),
                Arguments.of("{\"_id\":\" \",\"couchweave_type\":\"book\"}", "_id"),
                Arguments.of("{\"_id\":\"book-1\",\"_rev\":null,\"couchweave_type\":\"book\"}", "_rev"),
                Arguments.of("{\"_id\":\"book-1\",\"_rev\":7,\"couchweave_type\":\"book\"}", "_rev"),
                Arguments.of("{\"_id\":\"book-1\",\"_rev\":\" \",\"couchweave_type\":\"book\"}", "_rev"));
    }

    private static Stream<Arguments> invalidDiscriminatorDocuments() {
        return Stream.of(
                Arguments.of("{\"_id\":\"book-1\"}", "textual"),
                Arguments.of("{\"_id\":\"book-1\",\"couchweave_type\":42}", "textual"),
                Arguments.of("{\"_id\":\"book-1\",\"couchweave_type\":\"magazine\"}", "does not match"));
    }

    @CouchDocument(type = "book")
    static class MutableBook {
        @Id
        String id;

        @Revision
        String revision;

        @CouchField("display_title")
        String title;

        Author author;
    }

    @CouchDocument(type = "book")
    record ImmutableBook(
            @Id String id,
            @Revision String revision,
            @CouchField("display_title") String title,
            Author author,
            String note) {}

    record Author(String name) {}

    record Code(String value) {}

    @CouchDocument(type = "custom")
    record CustomDocument(@Id String id, Code code) {}

    @CouchDocument(type = "collection")
    record CollectionDocument(@Id String id, List<Author> authors) {}

    @CouchDocument(type = "primitive")
    record PrimitiveDocument(@Id String id, int count) {}

    record FailingValue(String value) {}

    @CouchDocument(type = "failing-write")
    record FailingWriteDocument(@Id String id, FailingValue failingValue) {}

    @CouchDocument(type = "exploding")
    record ExplodingDocument(@Id String id, String value) {
        ExplodingDocument {
            if ("explode".equals(value)) {
                throw new IllegalArgumentException("Cannot construct exploding document");
            }
        }
    }

    @CouchDocument(type = "unknown")
    record UnknownDocument(@Id String id) {}

    @CouchDocument
    record ReservedFieldDocument(
            @Id String id, @CouchField("_id") String value) {}

    @WritingConverter
    enum CodeWriteConverter implements Converter<Code, String> {
        INSTANCE;

        @Override
        public String convert(Code source) {
            return source.value();
        }
    }

    @ReadingConverter
    enum CodeReadConverter implements Converter<String, Code> {
        INSTANCE;

        @Override
        public Code convert(String source) {
            return new Code(source);
        }
    }

    @WritingConverter
    enum FailingWriteConverter implements Converter<FailingValue, String> {
        INSTANCE;

        @Override
        public String convert(FailingValue source) {
            throw new IllegalArgumentException("Cannot convert failing value");
        }
    }
}
