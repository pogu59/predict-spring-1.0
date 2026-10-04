package com.predict.controller.dto;

import com.predict.IssueOption;
import com.predict.scoring.VotePercentages;

import java.util.List;
import java.util.Map;

/**
 * 선택지 1개 + 비율(percent, 0~100). 참여 인원 비공개 원칙에 따라 voteCount는 항상 null이다
 * (필드는 기존 프론트와의 호환을 위해 남겨 둔다).
 */
public record IssueOptionResponse(Long id, String text, Integer voteCount, Integer percent) {

    public static List<IssueOptionResponse> percentsOf(List<IssueOption> options, Map<Long, Integer> countsByOptionId) {
        Map<Long, Integer> percents = VotePercentages.of(options, countsByOptionId);
        return options.stream()
                .map(option -> new IssueOptionResponse(option.getId(), option.getText(), null, percents.get(option.getId())))
                .toList();
    }
}
