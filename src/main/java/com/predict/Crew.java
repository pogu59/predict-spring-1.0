package com.predict;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 크루(커뮤니티·학교·팬덤 단위). 크루 대항전은 매주 크루별 인원당 점수 증가량으로 순위를 매긴다.
 * 관리자만 만들고 고친다. 비활성 크루는 목록·가입에서 빠지지만 기존 소속은 유지된다.
 */
@Getter
@Entity
@Table(name = "crews")
public class Crew {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crew_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 30)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 40)
    private String slug;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "active", nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Crew() {
    }

    public Crew(String name, String slug, String description, boolean active) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.active = active;
    }

    public void update(String name, String slug, String description, boolean active) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.active = active;
    }
}
