package com.predict.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MissionTypeConverter implements AttributeConverter<MissionType, String> {

    @Override
    public String convertToDatabaseColumn(MissionType attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public MissionType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MissionType.fromDbValue(dbData);
    }
}
