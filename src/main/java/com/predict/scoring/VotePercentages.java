package com.predict.scoring;

import com.predict.IssueOption;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 선택지별 득표 비율(0~100 정수). 참여 인원은 공개하지 않고 이 비율만 내려준다.
 * 각 선택지를 round(득표/전체x100)로 반올림하고, 합이 100이 안 되면 가장 큰 값에 차이를 더한다
 * (동률이면 앞 선택지). 전체 0표면 모두 0%. 프론트 lib/issues.ts optionPercents와 같은 규칙이다.
 */
public final class VotePercentages {

    private VotePercentages() {
    }

    public static Map<Long, Integer> of(List<IssueOption> options, Map<Long, Integer> countsByOptionId) {
        Map<Long, Integer> result = new HashMap<>();
        int total = 0;
        for (IssueOption option : options) {
            total += countsByOptionId.getOrDefault(option.getId(), 0);
        }
        if (total == 0) {
            for (IssueOption option : options) {
                result.put(option.getId(), 0);
            }
            return result;
        }
        int[] rounded = new int[options.size()];
        int sum = 0;
        int maxIndex = 0;
        for (int i = 0; i < options.size(); i++) {
            int count = countsByOptionId.getOrDefault(options.get(i).getId(), 0);
            rounded[i] = (int) Math.round(count * 100.0 / total);
            sum += rounded[i];
            if (rounded[i] > rounded[maxIndex]) {
                maxIndex = i;
            }
        }
        rounded[maxIndex] += 100 - sum;
        for (int i = 0; i < options.size(); i++) {
            result.put(options.get(i).getId(), rounded[i]);
        }
        return result;
    }

    /**
     * 보기 순서대로 센 응답 수 → 같은 규칙의 비율 배열. 미션 결과(MissionService.results)처럼
     * IssueOption이 없는 곳에서 쓴다. 전체 0이면 모두 0.
     */
    public static int[] ofCounts(int[] counts) {
        int[] rounded = new int[counts.length];
        int total = 0;
        for (int count : counts) {
            total += count;
        }
        if (total == 0) {
            return rounded;
        }
        int sum = 0;
        int maxIndex = 0;
        for (int i = 0; i < counts.length; i++) {
            rounded[i] = (int) Math.round(counts[i] * 100.0 / total);
            sum += rounded[i];
            if (rounded[i] > rounded[maxIndex]) {
                maxIndex = i;
            }
        }
        rounded[maxIndex] += 100 - sum;
        return rounded;
    }
}
