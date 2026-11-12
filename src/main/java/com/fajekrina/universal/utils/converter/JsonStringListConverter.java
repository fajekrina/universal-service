package com.fajekrina.universal.utils.converter;

import com.fajekrina.universal.utils.CanonicalJson;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/** List&lt;String&gt; ⇄ JSON array text (H2 has no text[] / jsonb). */
@Converter
public class JsonStringListConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        return CanonicalJson.write(attribute == null ? List.of() : attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        return CanonicalJson.readStringList(dbData);
    }
}
