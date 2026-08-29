package com.predict.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TierChangeReasonConverter implements AttributeConverter<TierChangeReason, String> {

    @Override
    public String convertToDatabaseColumn(TierChangeReason attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public TierChangeReason convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TierChangeReason.fromDbValue(dbData);
    }
}
