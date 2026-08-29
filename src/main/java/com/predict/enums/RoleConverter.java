package com.predict.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * role 컬럼이 ddl-auto=update로 뒤늦게 추가되면서 기존 유저 로우는 NULL을 갖게 되는데,
 * DB 기본값('user')과 동일하게 USER로 취급해 널포인터 없이 안전하게 읽는다.
 */
@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, String> {

    @Override
    public String convertToDatabaseColumn(Role attribute) {
        return attribute == null ? Role.USER.getDbValue() : attribute.getDbValue();
    }

    @Override
    public Role convertToEntityAttribute(String dbData) {
        return dbData == null ? Role.USER : Role.fromDbValue(dbData);
    }
}
