package com.predict.controller;

import com.predict.controller.dto.CommunityRequests.UploadResponse;
import com.predict.service.CurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * 이미지 업로드(이슈 커버, 게시글 첨부). 파일은 app.upload-dir(도커에서는 볼륨)에 저장하고
 * /uploads/{파일명}으로 정적 서빙한다(WebConfig). 응답 URL은 이 서버 기준 절대 주소다.
 */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp");

    private final CurrentUserService currentUserService;
    private final Path uploadDir;

    public UploadController(CurrentUserService currentUserService,
                            @Value("${app.upload-dir:uploads}") String uploadDir) {
        this.currentUserService = currentUserService;
        this.uploadDir = Path.of(uploadDir).toAbsolutePath();
    }

    @PostMapping("/images")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse uploadImage(@RequestHeader("Authorization") String authorization,
                                      @RequestParam("file") MultipartFile file) throws IOException {
        currentUserService.requireActiveUser(authorization);
        String extension = file.getContentType() == null ? null : EXTENSIONS.get(file.getContentType());
        if (file.isEmpty() || extension == null) {
            throw new IllegalArgumentException("JPG, PNG, GIF, WEBP 이미지만 올릴 수 있어요");
        }
        Files.createDirectories(uploadDir);
        String filename = UUID.randomUUID() + extension;
        file.transferTo(uploadDir.resolve(filename));
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(filename)
                .toUriString();
        return new UploadResponse(url);
    }
}
