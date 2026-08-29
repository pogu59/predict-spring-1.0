package com.predict.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TierConverter implements AttributeConverter<Tier, String> {

    @Override
    public String convertToDatabaseColumn(Tier attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public Tier convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Tier.fromDbValue(dbData);
    }
}
