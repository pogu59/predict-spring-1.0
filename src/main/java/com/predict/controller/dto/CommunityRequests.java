package com.predict.controller.dto;

import com.predict.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 이번 리디자인에서 새로 생긴 작은 요청/응답 DTO 모음. 한 파일에 몰아 두면 컨트롤러가 늘어나도
 * dto 패키지가 한두 필드짜리 파일로 가득 차지 않는다.
 */
public final class CommunityRequests {

    private CommunityRequests() {
    }

    public record ChangeVoteRequest(@NotNull Long optionId) {
    }

    public record ReportRequest(@NotBlank @Size(max = 30) String reason) {
    }

    public record LikeResponse(boolean liked, long likeCount) {
    }

    public record HiddenRequest(boolean hidden) {
    }

    public record RoleRequest(@NotNull Role role) {
    }

    public record SuspensionRequest(boolean suspended) {
    }

    public record UploadResponse(String url) {
    }

    public record Terms(boolean age, boolean service, boolean privacy, boolean marketing) {
        public boolean requiredAgreed() {
            return age && service && privacy;
        }
    }

    public record EmailSignupRequest(
            @NotBlank @Email @Size(max = 100) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank String nickname,
            @NotNull Terms terms) {
    }

    public record EmailLoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record SocialSignupRequest(@NotBlank String via, @NotBlank String nickname, @NotNull Terms terms) {
    }

    public record TokenResponse(String token) {
    }

    public record NicknameCheckResponse(boolean available) {
    }
}
