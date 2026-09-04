package com.predict.service;

import com.predict.Post;
import com.predict.User;
import com.predict.enums.Role;
import com.predict.repository.PostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional
    public Post create(User author, String title, String content) {
        return postRepository.save(new Post(author, title, content));
    }

    /** 상세 조회 시 조회수 +1. */
    @Transactional
    public Post viewDetail(Long postId) {
        Post post = requirePost(postId);
        post.increaseViewCount();
        return post;
    }

    @Transactional
    public void delete(Long postId, User requester) {
        Post post = requirePost(postId);
        if (!post.isAuthor(requester) && requester.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 글이거나 관리자만 삭제할 수 있습니다");
        }
        post.delete(LocalDateTime.now());
    }

    public Post requirePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글: " + postId));
        if (post.isDeleted()) {
            throw new IllegalArgumentException("삭제된 게시글: " + postId);
        }
        return post;
    }
}
