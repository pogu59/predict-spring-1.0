package com.predict.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ChoiceConverter implements AttributeConverter<Choice, String> {

    @Override
    public String convertToDatabaseColumn(Choice attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public Choice convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Choice.fromDbValue(dbData);
    }
}
