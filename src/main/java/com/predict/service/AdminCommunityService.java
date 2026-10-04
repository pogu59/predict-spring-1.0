package com.predict.service;

import com.predict.Post;
import com.predict.Reply;
import com.predict.Report;
import com.predict.repository.PostLikeRepository;
import com.predict.repository.PostRepository;
import com.predict.repository.ReplyLikeRepository;
import com.predict.repository.ReplyRepository;
import com.predict.repository.ReportRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 관리자 커뮤니티 관리(게시글/댓글 숨김·삭제)와 신고 처리. 신고는 대상별로 묶어서 보여주고,
 * 반려/콘텐츠 삭제도 그 대상에 쌓인 미처리 신고 전체에 한 번에 적용한다.
 */
@Service
public class AdminCommunityService {

    private static final int LIST_LIMIT = 200;

    private final PostRepository postRepository;
    private final ReplyRepository replyRepository;
    private final PostLikeRepository postLikeRepository;
    private final ReplyLikeRepository replyLikeRepository;
    private final ReportRepository reportRepository;

    public AdminCommunityService(PostRepository postRepository, ReplyRepository replyRepository,
                                 PostLikeRepository postLikeRepository, ReplyLikeRepository replyLikeRepository,
                                 ReportRepository reportRepository) {
        this.postRepository = postRepository;
        this.replyRepository = replyRepository;
        this.postLikeRepository = postLikeRepository;
        this.replyLikeRepository = replyLikeRepository;
        this.reportRepository = reportRepository;
    }

    public record CommunityItem(Long id, Long postId, String authorNickname, String title, String where,
                                long likeCount, Long replyCount, long reportCount, boolean hidden,
                                LocalDateTime createdAt) {
    }

    public record ReportItem(Long id, String kind, Long targetId, Parent parent, String authorNickname,
                             String excerpt, String reason, long count, String status, LocalDateTime createdAt) {
    }

    public record Parent(String type, Long id) {
    }

    @Transactional(readOnly = true)
    public List<CommunityItem> listPosts() {
        List<Post> posts = postRepository.findByDeletedFalseOrderByCreatedAtDesc(PageRequest.of(0, LIST_LIMIT));
        List<Long> ids = posts.stream().map(Post::getId).toList();
        if (ids.isEmpty()) return List.of();
        Map<Long, Long> likes = PostService.toCountMap(postLikeRepository.countByPostIds(ids));
        Map<Long, Long> replies = PostService.toCountMap(replyRepository.countByPostIds(ids));
        Map<Long, Long> reports = PostService.toCountMap(reportRepository.countByTargets(Report.TargetType.POST, ids));
        return posts.stream()
                .map(p -> new CommunityItem(p.getId(), null, p.getAuthor().getNickname(), p.getTitle(), null,
                        likes.getOrDefault(p.getId(), 0L), replies.getOrDefault(p.getId(), 0L),
                        reports.getOrDefault(p.getId(), 0L), p.isHidden(), p.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommunityItem> listComments() {
        List<Reply> replies = replyRepository.findByDeletedFalseOrderByCreatedAtDesc(PageRequest.of(0, LIST_LIMIT));
        List<Long> ids = replies.stream().map(Reply::getId).toList();
        if (ids.isEmpty()) return List.of();
        Map<Long, Long> likes = PostService.toCountMap(replyLikeRepository.countByReplyIds(ids));
        Map<Long, Long> reports = PostService.toCountMap(reportRepository.countByTargets(Report.TargetType.REPLY, ids));
        return replies.stream()
                .map(r -> new CommunityItem(r.getId(), r.getPost() != null ? r.getPost().getId() : null,
                        r.getAuthor().getNickname(), r.getContent(), whereOf(r), likes.getOrDefault(r.getId(), 0L),
                        null, reports.getOrDefault(r.getId(), 0L), r.isHidden(), r.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void setPostHidden(Long postId, boolean hidden) {
        requirePost(postId).setHidden(hidden);
    }

    @Transactional
    public void setReplyHidden(Long replyId, boolean hidden) {
        requireReply(replyId).setHidden(hidden);
    }

    @Transactional
    public void deletePost(Long postId) {
        requirePost(postId).delete(LocalDateTime.now());
        processPending(Report.TargetType.POST, postId, Report.Status.REMOVED);
    }

    @Transactional
    public void deleteReply(Long replyId) {
        requireReply(replyId).delete(LocalDateTime.now());
        processPending(Report.TargetType.REPLY, replyId, Report.Status.REMOVED);
    }

    /** 신고 목록 — 대상별로 묶는다. 대표 id는 그 대상의 가장 최근 신고 id. */
    @Transactional(readOnly = true)
    public List<ReportItem> listReports(boolean pending) {
        List<Report> reports = pending
                ? reportRepository.findByStatusOrderByCreatedAtDesc(Report.Status.PENDING)
                : reportRepository.findByStatusInOrderByCreatedAtDesc(List.of(Report.Status.REJECTED, Report.Status.REMOVED));
        Map<String, List<Report>> byTarget = new LinkedHashMap<>();
        for (Report r : reports) {
            byTarget.computeIfAbsent(r.getTargetType() + ":" + r.getTargetId() + ":" + r.getStatus(),
                    k -> new ArrayList<>()).add(r);
        }
        List<ReportItem> items = new ArrayList<>();
        for (List<Report> group : byTarget.values()) {
            Report latest = group.get(0);
            Optional<ReportItem> item = toReportItem(latest, group.size());
            item.ifPresent(items::add);
        }
        items.sort(Comparator.comparing(ReportItem::createdAt).reversed());
        return items;
    }

    @Transactional
    public void rejectReport(Long reportId) {
        Report report = requireReport(reportId);
        processPending(report.getTargetType(), report.getTargetId(), Report.Status.REJECTED);
    }

    @Transactional
    public void removeReportedContent(Long reportId) {
        Report report = requireReport(reportId);
        if (report.getTargetType() == Report.TargetType.POST) {
            postRepository.findById(report.getTargetId()).ifPresent(p -> p.delete(LocalDateTime.now()));
        } else {
            replyRepository.findById(report.getTargetId()).ifPresent(r -> r.delete(LocalDateTime.now()));
        }
        processPending(report.getTargetType(), report.getTargetId(), Report.Status.REMOVED);
    }

    private void processPending(Report.TargetType type, Long targetId, Report.Status status) {
        LocalDateTime now = LocalDateTime.now();
        for (Report r : reportRepository.findByTargetTypeAndTargetIdAndStatus(type, targetId, Report.Status.PENDING)) {
            r.process(status, now);
        }
    }

    private Optional<ReportItem> toReportItem(Report report, long count) {
        String status = report.getStatus().name();
        if (report.getTargetType() == Report.TargetType.POST) {
            return postRepository.findById(report.getTargetId())
                    .map(p -> new ReportItem(report.getId(), "post", p.getId(), null, p.getAuthor().getNickname(),
                            p.getTitle(), report.getReason(), count, status, report.getCreatedAt()));
        }
        return replyRepository.findById(report.getTargetId())
                .map(r -> new ReportItem(report.getId(), "comment", r.getId(),
                        r.getIssue() != null ? new Parent("issue", r.getIssue().getId()) : new Parent("post", r.getPost().getId()),
                        r.getAuthor().getNickname(), r.getContent(), report.getReason(), count, status,
                        report.getCreatedAt()));
    }

    private static String whereOf(Reply reply) {
        return reply.getIssue() != null
                ? "이슈 · " + reply.getIssue().getTitle()
                : "게시글 · " + reply.getPost().getTitle();
    }

    private Post requirePost(Long postId) {
        return postRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글: " + postId));
    }

    private Reply requireReply(Long replyId) {
        return replyRepository.findById(replyId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글: " + replyId));
    }

    private Report requireReport(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신고: " + reportId));
    }
}
