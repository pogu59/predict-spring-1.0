package com.predict.mission;

import java.util.List;

/** 관리자가 미션을 만들 때 넘기는 문항 초안. attentionAnswerIndex가 있으면 확인 문항. */
public record QuestionDraft(String text, List<String> options, Integer attentionAnswerIndex) {
}
