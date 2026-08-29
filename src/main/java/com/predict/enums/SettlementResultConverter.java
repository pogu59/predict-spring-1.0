package com.predict.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SettlementResultConverter implements AttributeConverter<SettlementResult, String> {

    @Override
    public String convertToDatabaseColumn(SettlementResult attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public SettlementResult convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SettlementResult.fromDbValue(dbData);
    }
}
