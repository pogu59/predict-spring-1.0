package com.predict.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PostTopicConverter implements AttributeConverter<PostTopic, String> {

    @Override
    public String convertToDatabaseColumn(PostTopic attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public PostTopic convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PostTopic.fromDbValue(dbData);
    }
}
