package com.predict.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ExchangeStatusConverter implements AttributeConverter<ExchangeStatus, String> {

    @Override
    public String convertToDatabaseColumn(ExchangeStatus attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public ExchangeStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ExchangeStatus.fromDbValue(dbData);
    }
}
