package com.predict.support;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;

/**
 * 짧은 문자열 목록(설문 보기 등)을 줄바꿈으로 이어 한 컬럼에 저장한다. 보기 하나하나에는 줄바꿈이
 * 들어갈 수 없다는 전제(MissionQuestion 생성자에서 검증)로 JSON 라이브러리 없이 왕복한다.
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String SEPARATOR = "\n";

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        return attribute == null ? null : String.join(SEPARATOR, attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return List.of();
        }
        return List.copyOf(Arrays.asList(dbData.split(SEPARATOR, -1)));
    }
}
