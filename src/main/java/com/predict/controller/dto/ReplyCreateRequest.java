package com.predict.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** parentId: 게시판 대댓글일 때 부모 댓글 id(선택). 이슈 댓글에서는 무시된다. */
public record ReplyCreateRequest(
        @NotBlank @Size(max = 1000) String content,
        Long parentId
) {
}
