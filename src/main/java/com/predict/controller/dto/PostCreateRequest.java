package com.predict.controller.dto;

import com.predict.enums.PostTopic;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 글 작성·수정 공용. images는 업로드 API가 돌려준 URL, 최대 4장. topic(말머리)은 비워도 된다. */
public record PostCreateRequest(
        @NotBlank @Size(max = 60, message = "제목은 60자 이하로 입력해 주세요.") String title,
        @NotBlank String content,
        @Size(max = 4, message = "이미지는 최대 4장까지 올릴 수 있어요.") List<@NotBlank @Size(max = 500) String> images,
        PostTopic topic
) {
    public List<String> imagesOrEmpty() {
        return images == null ? List.of() : images;
    }
}
