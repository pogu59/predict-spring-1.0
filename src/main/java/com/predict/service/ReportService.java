package com.predict.service;

import com.predict.Report;
import com.predict.User;
import com.predict.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/** 게시글/댓글 신고 접수. 처리(반려·삭제)는 AdminCommunityService가 한다. */
@Service
public class ReportService {

    /** 프론트 신고 사유 시트와 같은 목록. */
    public static final Set<String> REASONS = Set.of("스팸·광고", "욕설·비하", "음란·선정성", "개인정보 노출", "기타");

    private final ReportRepository reportRepository;
    private final PostService postService;
    private final ReplyService replyService;

    public ReportService(ReportRepository reportRepository, PostService postService, ReplyService replyService) {
        this.reportRepository = reportRepository;
        this.postService = postService;
        this.replyService = replyService;
    }

    /** 같은 유저가 같은 대상을 다시 신고하면 조용히 무시한다(누적 신고 수 부풀리기 방지). */
    @Transactional
    public void report(Report.TargetType type, Long targetId, User reporter, String reason) {
        if (!REASONS.contains(reason)) {
            throw new IllegalArgumentException("신고 사유를 선택해 주세요");
        }
        if (type == Report.TargetType.POST) {
            postService.requirePost(targetId);
        } else {
            replyService.requireReply(targetId);
        }
        if (reportRepository.existsByTargetTypeAndTargetIdAndReporterId(type, targetId, reporter.getId())) {
            return;
        }
        reportRepository.save(new Report(type, targetId, reporter, reason));
    }
}
