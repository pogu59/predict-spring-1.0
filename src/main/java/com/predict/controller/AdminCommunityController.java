package com.predict.controller;

import com.predict.controller.dto.CommunityRequests.HiddenRequest;
import com.predict.service.AdminCommunityService;
import com.predict.service.AdminCommunityService.CommunityItem;
import com.predict.service.AdminCommunityService.ReportItem;
import com.predict.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 관리자 페이지 — 커뮤니티 관리(게시글/댓글 숨김·삭제)와 신고 처리. */
@RestController
@RequestMapping("/api/admin")
public class AdminCommunityController {

    private final AdminCommunityService adminCommunityService;
    private final CurrentUserService currentUserService;

    public AdminCommunityController(AdminCommunityService adminCommunityService, CurrentUserService currentUserService) {
        this.adminCommunityService = adminCommunityService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/posts")
    public List<CommunityItem> posts(@RequestHeader("Authorization") String authorization) {
        currentUserService.requireAdmin(authorization);
        return adminCommunityService.listPosts();
    }

    @GetMapping("/comments")
    public List<CommunityItem> comments(@RequestHeader("Authorization") String authorization) {
        currentUserService.requireAdmin(authorization);
        return adminCommunityService.listComments();
    }

    @PatchMapping("/posts/{postId}/hidden")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hidePost(@RequestHeader("Authorization") String authorization, @PathVariable Long postId,
                         @RequestBody HiddenRequest request) {
        currentUserService.requireAdmin(authorization);
        adminCommunityService.setPostHidden(postId, request.hidden());
    }

    @PatchMapping("/comments/{replyId}/hidden")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hideComment(@RequestHeader("Authorization") String authorization, @PathVariable Long replyId,
                            @RequestBody HiddenRequest request) {
        currentUserService.requireAdmin(authorization);
        adminCommunityService.setReplyHidden(replyId, request.hidden());
    }

    @DeleteMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@RequestHeader("Authorization") String authorization, @PathVariable Long postId) {
        currentUserService.requireAdmin(authorization);
        adminCommunityService.deletePost(postId);
    }

    @DeleteMapping("/comments/{replyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@RequestHeader("Authorization") String authorization, @PathVariable Long replyId) {
        currentUserService.requireAdmin(authorization);
        adminCommunityService.deleteReply(replyId);
    }

    /** status: PENDING(미처리) | DONE(처리 완료). */
    @GetMapping("/reports")
    public List<ReportItem> reports(@RequestHeader("Authorization") String authorization,
                                    @RequestParam(defaultValue = "PENDING") String status) {
        currentUserService.requireAdmin(authorization);
        return adminCommunityService.listReports(!"DONE".equals(status));
    }

    @PostMapping("/reports/{reportId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@RequestHeader("Authorization") String authorization, @PathVariable Long reportId) {
        currentUserService.requireAdmin(authorization);
        adminCommunityService.rejectReport(reportId);
    }

    @PostMapping("/reports/{reportId}/remove-content")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeContent(@RequestHeader("Authorization") String authorization, @PathVariable Long reportId) {
        currentUserService.requireAdmin(authorization);
        adminCommunityService.removeReportedContent(reportId);
    }
}
