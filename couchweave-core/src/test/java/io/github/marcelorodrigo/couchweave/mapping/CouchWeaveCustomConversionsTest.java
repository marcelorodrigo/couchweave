package io.github.marcelorodrigo.couchweave.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

class CouchWeaveCustomConversionsTest {

    @Test
    @DisplayName("should register a property write converter")
    void shouldRegisterAPropertyWriteConverter() {
        // given
        var conversions =
                new CouchWeaveCustomConversions(List.of(CodeWriteConverter.INSTANCE, CodeReadConverter.INSTANCE));
        var conversionService = conversions.getConversionService();

        // when
        var stored = conversionService.convert(new Code("ABC-123"), String.class);

        // then
        assertThat(stored).isEqualTo("ABC-123");
    }

    @Test
    @DisplayName("should register a property read converter")
    void shouldRegisterAPropertyReadConverter() {
        // given
        var conversions =
                new CouchWeaveCustomConversions(List.of(CodeWriteConverter.INSTANCE, CodeReadConverter.INSTANCE));
        var conversionService = conversions.getConversionService();

        // when
        var restored = conversionService.convert("ABC-123", Code.class);

        // then
        assertThat(restored).isEqualTo(new Code("ABC-123"));
    }

    @Test
    @DisplayName("should expose custom read and write targets")
    void shouldExposeCustomReadAndWriteTargets() {
        // given
        var conversions =
                new CouchWeaveCustomConversions(List.of(CodeWriteConverter.INSTANCE, CodeReadConverter.INSTANCE));

        // when
        var writeTarget = conversions.getCustomWriteTarget(Code.class);
        var hasReadTarget = conversions.hasCustomReadTarget(String.class, Code.class);

        // then
        assertThat(writeTarget).contains(String.class);
        assertThat(hasReadTarget).isTrue();
    }

    @Test
    @DisplayName("should reject a null converter collection")
    void shouldRejectANullConverterCollection() {
        // given / when / then
        assertThatThrownBy(() -> new CouchWeaveCustomConversions(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("converters");
    }

    record Code(String value) {}

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
}
