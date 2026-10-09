package com.predict.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RewardTransactionTypeConverter implements AttributeConverter<RewardTransactionType, String> {

    @Override
    public String convertToDatabaseColumn(RewardTransactionType attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public RewardTransactionType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RewardTransactionType.fromDbValue(dbData);
    }
}
