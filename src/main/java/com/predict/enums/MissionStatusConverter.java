package com.predict.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MissionStatusConverter implements AttributeConverter<MissionStatus, String> {

    @Override
    public String convertToDatabaseColumn(MissionStatus attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public MissionStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MissionStatus.fromDbValue(dbData);
    }
}
