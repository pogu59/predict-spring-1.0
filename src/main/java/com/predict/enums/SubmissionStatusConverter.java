package com.predict.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SubmissionStatusConverter implements AttributeConverter<SubmissionStatus, String> {

    @Override
    public String convertToDatabaseColumn(SubmissionStatus attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public SubmissionStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SubmissionStatus.fromDbValue(dbData);
    }
}
