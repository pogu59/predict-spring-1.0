package com.predict.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TopicStatusConverter implements AttributeConverter<TopicStatus, String> {

    @Override
    public String convertToDatabaseColumn(TopicStatus attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public TopicStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TopicStatus.fromDbValue(dbData);
    }
}
