package com.predict;

import com.predict.support.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.List;

/**
 * 미션 문항(객관식). 보기는 2~6개, 매트릭스(표) 문항은 두지 않는다 — 모바일에서 짧고 단순한
 * 설문일수록 이탈이 적기 때문(기획서 "디자인 원칙과 근거").
 */
@Getter
@Entity
@Table(name = "mission_questions")
public class MissionQuestion {

    public static final int MIN_OPTIONS = 2;
    public static final int MAX_OPTIONS = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "question_text", nullable = false, length = 200)
    private String text;

    /** 보기 목록. 줄바꿈으로 이어 한 컬럼에 저장한다(StringListConverter). */
    @Convert(converter = StringListConverter.class)
    @Column(name = "options_text", nullable = false, length = 1000)
    private List<String> options;

    /** 확인(주의) 문항이면 정답 보기 인덱스, 아니면 null. 참여자 API에는 절대 내려주지 않는다. */
    @Column(name = "attention_answer_index")
    private Integer attentionAnswerIndex;

    protected MissionQuestion() {
    }

    MissionQuestion(Mission mission, int sortOrder, String text, List<String> options, Integer attentionAnswerIndex) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("문항 내용을 입력해 주세요");
        }
        if (options == null || options.size() < MIN_OPTIONS || options.size() > MAX_OPTIONS) {
            throw new IllegalArgumentException("보기는 " + MIN_OPTIONS + "~" + MAX_OPTIONS + "개여야 해요");
        }
        for (String option : options) {
            if (option == null || option.isBlank()) {
                throw new IllegalArgumentException("빈 보기가 있어요");
            }
            if (option.contains("\n")) {
                throw new IllegalArgumentException("보기에는 줄바꿈을 넣을 수 없어요");
            }
        }
        if (attentionAnswerIndex != null && (attentionAnswerIndex < 0 || attentionAnswerIndex >= options.size())) {
            throw new IllegalArgumentException("확인 문항의 정답 보기 번호가 범위를 벗어났어요");
        }
        this.mission = mission;
        this.sortOrder = sortOrder;
        this.text = text.strip();
        this.options = options.stream().map(String::strip).toList();
        this.attentionAnswerIndex = attentionAnswerIndex;
    }

    public boolean isAttentionCheck() {
        return attentionAnswerIndex != null;
    }
}
