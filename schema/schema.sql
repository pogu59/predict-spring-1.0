-- =====================================================================
-- 예측 게임 프로젝트 — MySQL 스키마 (전체 9개 테이블)
-- 대상: MySQL 8.x (HeidiSQL로 작업)
-- 작성 기준: 카테고리 5개 / 신용도 점수 시스템 / 티어 시스템 / 이벤트 로깅 스펙
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. categories : 카테고리 5개 고정값 (유저가 추가/삭제하지 않음)
-- ---------------------------------------------------------------------
CREATE TABLE categories (
    category_id     TINYINT UNSIGNED PRIMARY KEY,   -- 카테고리 번호(1~5). 작은 정수라 TINYINT로 용량 절약
    name            VARCHAR(30) NOT NULL UNIQUE      -- 카테고리명. UNIQUE라 이름 중복 불가
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO categories (category_id, name) VALUES
    (1, '정치'), (2, '스포츠'), (3, 'E스포츠'), (4, '경제'), (5, '날씨');


-- ---------------------------------------------------------------------
-- 2. users : 유저 정보 + 누적 신용도 점수 + 현재 티어
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,  -- 유저 고유 번호(자동 증가)
    nickname            VARCHAR(50) NOT NULL,                        -- 닉네임
    signup_channel      VARCHAR(50) NULL,                            -- 유입경로(예: direct, kakao 등). 알 수 없으면 NULL
    referred_by_code    VARCHAR(20) NULL,                            -- 가입 시 사용한 추천 코드 (share_clicks.referral_code 참조, 없으면 NULL)
    credibility_score   INT NOT NULL DEFAULT 0,                      -- 누적 신용도 점수. 하한 0은 애플리케이션 레벨에서 강제
    tier                VARCHAR(20) NOT NULL DEFAULT '언랭크',        -- 현재 티어. 가입 직후 자동으로 '언랭크'
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP  -- 가입일시(자동 기록)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------------------------------
-- 3. topics : 투표 주제
-- ---------------------------------------------------------------------
CREATE TABLE topics (
    topic_id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    category_id         TINYINT UNSIGNED NOT NULL,                   -- 소속 카테고리 (categories 참조)
    title               VARCHAR(255) NOT NULL,                       -- 주제 제목
    description         TEXT NULL,                                   -- 부가 설명 (없어도 됨)
    status              ENUM('open','pending_result','confirmed','void')
                             NOT NULL DEFAULT 'open',
                             -- open=투표중 / pending_result=결과대기 / confirmed=확정 / void=무효
    vote_start_at       DATETIME NOT NULL,                           -- 투표 시작 시각
    vote_deadline_at    DATETIME NOT NULL,                           -- 투표 마감 시각
    confirmed_at        DATETIME NULL,                               -- 결과 확정 시각 (미확정이면 NULL)
    correct_answer      ENUM('yes','no') NULL,                       -- 정답 (void면 NULL 유지)
    yes_count           INT UNSIGNED NOT NULL DEFAULT 0,             -- 마감 시점 Yes 득표수 (점수 계산용 스냅샷)
    no_count            INT UNSIGNED NOT NULL DEFAULT 0,             -- 마감 시점 No 득표수
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 주제 생성 시각

    FOREIGN KEY (category_id) REFERENCES categories(category_id),
    INDEX idx_topics_category_status (category_id, status),          -- "카테고리별 특정 상태" 조회 최적화
    INDEX idx_topics_deadline (vote_deadline_at)                     -- 마감시각 기준 조회/배치 처리 최적화
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------------------------------
-- 4. votes : 투표 참여 기록
-- ---------------------------------------------------------------------
CREATE TABLE votes (
    vote_id     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT UNSIGNED NOT NULL,
    topic_id    BIGINT UNSIGNED NOT NULL,
    choice      ENUM('yes','no') NOT NULL,                           -- 유저의 선택
    voted_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,         -- 투표 시각

    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (topic_id) REFERENCES topics(topic_id),
    UNIQUE KEY uq_votes_user_topic (user_id, topic_id),              -- 유저당 주제 1표만 (중복 투표 원천 차단)
    INDEX idx_votes_topic (topic_id),                                -- 특정 주제의 참여자 조회(정산 시 사용)
    INDEX idx_votes_user_time (user_id, voted_at)                    -- 유저별 시간순 조회(주간 참여횟수 집계용)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------------------------------
-- 5. score_settlements : 점수 정산 결과 (votes와 1:1)
-- ---------------------------------------------------------------------
CREATE TABLE score_settlements (
    settlement_id   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    vote_id         BIGINT UNSIGNED NOT NULL UNIQUE,                 -- 투표 1건당 정산 1건 (중복 정산 방지)
    user_id         BIGINT UNSIGNED NOT NULL,
    topic_id        BIGINT UNSIGNED NOT NULL,
    choice          ENUM('yes','no') NOT NULL,                       -- votes에서 복사 (조회 편의용)
    result          ENUM('correct','incorrect','void') NOT NULL,     -- 정답/오답/무효
    p_value         DECIMAL(5,4) NOT NULL,                           -- 득표비율 p (예: 0.5384). 반올림 오차 없는 DECIMAL 사용
    score_delta     INT NOT NULL,                                    -- 이번 정산으로 발생한 점수 변동분(+/-)
    score_after     INT NOT NULL,                                    -- 정산 직후 누적 신용도 점수 스냅샷
    settled_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,     -- 정산 실행 시각

    FOREIGN KEY (vote_id) REFERENCES votes(vote_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (topic_id) REFERENCES topics(topic_id),
    INDEX idx_settlements_user (user_id),                            -- 유저별 정산 이력 조회(정답률 분석 등)
    INDEX idx_settlements_topic (topic_id)                           -- 주제별 정산 결과 조회
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------------------------------
-- 6. tier_changes : 티어 변경 이력
-- ---------------------------------------------------------------------
CREATE TABLE tier_changes (
    tier_change_id  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED NOT NULL,
    previous_tier   VARCHAR(20) NOT NULL,
    new_tier        VARCHAR(20) NOT NULL,
    reason          ENUM('natural_promotion','natural_demotion',
                         'activity_demotion','activity_restoration') NOT NULL,
                         -- natural_promotion/demotion = 점수 변동에 따른 자연 승급/강등
                         -- activity_demotion/restoration = 다이아·마스터 활동성 조건에 의한 강등/복귀
    snapshot_id     BIGINT UNSIGNED NULL,                            -- activity_* 사유일 때만 채움 (아래 9번 테이블 참조)
    changed_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id),
    -- snapshot_id의 FOREIGN KEY는 weekly_activity_snapshots 테이블이 아직 생성되기 전이라
    -- 여기서 바로 걸 수 없음. 파일 맨 아래에서 ALTER TABLE로 나중에 추가함
    INDEX idx_tier_changes_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------------------------------
-- 7. share_clicks : 공유 버튼 클릭 로그
-- ---------------------------------------------------------------------
CREATE TABLE share_clicks (
    share_click_id  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED NOT NULL,                        -- 공유한 유저
    channel         VARCHAR(30) NOT NULL,                            -- 공유 채널(카톡/X 등)
    referral_code   VARCHAR(20) NOT NULL UNIQUE,                     -- 이 공유 클릭 고유의 추천 코드 (링크에 심어서 배포)
    clicked_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------------------------------
-- 8. login_sessions : 로그인 기록
-- ---------------------------------------------------------------------
CREATE TABLE login_sessions (
    session_id  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT UNSIGNED NOT NULL,
    login_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------------------------------
-- 9. weekly_activity_snapshots : 다이아/마스터 주간 활동성 체크 스냅샷
--    (매주 일요일 자정 배치가 계산한 결과를 그대로 영구 저장)
-- ---------------------------------------------------------------------
CREATE TABLE weekly_activity_snapshots (
    snapshot_id     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED NOT NULL,
    week_start      DATE NOT NULL,                                   -- 해당 주 월요일 날짜
    week_end        DATE NOT NULL,                                   -- 해당 주 일요일 날짜
    vote_count      INT UNSIGNED NOT NULL,                           -- 그 주 실제 투표 횟수 (계산 당시 값 고정)
    met_requirement TINYINT(1) NOT NULL,                             -- 5회 조건 충족 여부 (1=충족, 0=미충족)
    tier_before     VARCHAR(20) NOT NULL,                            -- 체크 직전 티어
    tier_after      VARCHAR(20) NOT NULL,                            -- 체크 직후 티어
    checked_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id),
    UNIQUE KEY uq_snapshot_user_week (user_id, week_start)           -- 유저당 주 하나에 기록 하나만
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- weekly_activity_snapshots 생성이 끝난 뒤, tier_changes.snapshot_id에 FK를 추가
-- (6번 테이블 생성 시점엔 9번 테이블이 없어서 미리 걸 수 없었기 때문에 여기서 추가)
-- ---------------------------------------------------------------------
ALTER TABLE tier_changes
    ADD FOREIGN KEY (snapshot_id) REFERENCES weekly_activity_snapshots(snapshot_id);

-- ---------------------------------------------------------------------
-- share_clicks 생성이 끝난 뒤, users.referred_by_code에 FK를 추가
-- (2번 테이블 생성 시점엔 7번 테이블이 없어서 미리 걸 수 없었기 때문에 여기서 추가)
-- 신규가입이 항상 공유 링크를 통하는 건 아니므로 NULL 허용 유지
-- ---------------------------------------------------------------------
ALTER TABLE users
    ADD FOREIGN KEY (referred_by_code) REFERENCES share_clicks(referral_code),
    ADD INDEX idx_users_referred_by (referred_by_code);
