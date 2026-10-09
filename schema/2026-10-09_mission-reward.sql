-- =====================================================================
-- 2026-10-09 · 미션 · 리워드 포인트 스키마 (신규 테이블 6개)
-- 대상: MySQL 8.x
-- 선행: 기존 스키마(users 테이블, user_id BIGINT — Hibernate가 만든 운영 테이블은 부호 있는 BIGINT)가 먼저 있어야 한다.
--
-- 신용도(users.credibility_score)와 리워드 포인트는 완전히 분리한다.
--   - users 테이블은 바꾸지 않는다. 포인트 잔액은 reward_wallets에 따로 둔다.
--   - 두 값 사이의 전환 경로(테이블·프로시저)는 만들지 않는다.
-- spring.jpa.hibernate.ddl-auto=none 이라 배포 전에 이 파일을 직접 실행해야 한다.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. missions : 참여자가 하는 미션 (출석 · 밸런스 게임 · 설문)
-- ---------------------------------------------------------------------
CREATE TABLE missions (
    mission_id      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    type            VARCHAR(20)  NOT NULL,                  -- attendance / balance / survey
    title           VARCHAR(100) NOT NULL,
    description     VARCHAR(500) NULL,
    reward_points   INT UNSIGNED NOT NULL DEFAULT 0,        -- 검수 통과 시 주는 리워드 포인트(신용도와 무관)
    is_daily        BOOLEAN      NOT NULL DEFAULT FALSE,    -- 오늘의 미션 여부. 그날 열린 오늘의 미션(출석 제외)을 모두 끝내면 보너스
    status          VARCHAR(20)  NOT NULL DEFAULT 'draft',  -- draft(작성 중) / open(공개) / closed(관리자가 닫음)
    starts_at       DATETIME     NOT NULL,                  -- 공개 기간 시작(포함)
    ends_at         DATETIME     NOT NULL,                  -- 공개 기간 끝(미포함)
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_missions_status_period (status, starts_at, ends_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------------------------------
-- 2. mission_questions : 미션 문항(객관식, 보기 2~6개, 매트릭스 문항 없음)
-- ---------------------------------------------------------------------
CREATE TABLE mission_questions (
    question_id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    mission_id              BIGINT UNSIGNED NOT NULL,
    sort_order              INT           NOT NULL,         -- 0부터
    question_text           VARCHAR(200)  NOT NULL,
    options_text            VARCHAR(1000) NOT NULL,         -- 보기 목록, 줄바꿈(\n)으로 구분
    attention_answer_index  INT           NULL,             -- 확인(주의) 문항이면 정답 보기 인덱스. 참여자 API에는 노출하지 않음

    FOREIGN KEY (mission_id) REFERENCES missions(mission_id) ON DELETE CASCADE,
    UNIQUE KEY uq_mission_questions_order (mission_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------------------------------
-- 3. mission_submissions : 미션 제출 + 규칙 검수 결과
-- ---------------------------------------------------------------------
CREATE TABLE mission_submissions (
    submission_id   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    mission_id      BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT          NOT NULL,
    submitted_on    DATE            NOT NULL,               -- 제출한 날짜(출석 하루 1회 판정)
    answers         VARCHAR(500)    NOT NULL DEFAULT '',     -- 문항 순서대로 고른 보기 인덱스, 쉼표 구분(출석은 빈 문자열)
    duration_ms     BIGINT          NOT NULL,               -- 화면을 연 뒤 제출까지 걸린 시간
    quality_score   INT             NOT NULL,               -- 0~100(분석용)
    status          VARCHAR(20)     NOT NULL,               -- pending / approved / rejected
    reject_reason   VARCHAR(200)    NULL,                   -- 참여자에게 그대로 보여 주는 반려 이유
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (mission_id) REFERENCES missions(mission_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    UNIQUE KEY uq_submissions_mission_user_day (mission_id, user_id, submitted_on),
    INDEX idx_submissions_user_day (user_id, submitted_on, status),
    INDEX idx_submissions_mission_status (mission_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------------------------------
-- 4. reward_wallets : 리워드 포인트 잔액(유저당 1행, 첫 적립 때 생성)
-- ---------------------------------------------------------------------
CREATE TABLE reward_wallets (
    user_id     BIGINT          PRIMARY KEY,
    balance     INT UNSIGNED NOT NULL DEFAULT 0,            -- 0 미만 불가(애플리케이션에서도 강제)
    version     BIGINT       NOT NULL DEFAULT 0,            -- 낙관적 락(@Version)
    updated_at  DATETIME     NULL,

    FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------------------------------
-- 5. reward_exchange_requests : 기프티콘 교환 신청(운영자 수동 승인)
-- ---------------------------------------------------------------------
CREATE TABLE reward_exchange_requests (
    exchange_id     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    product_code    VARCHAR(50)     NOT NULL,
    product_name    VARCHAR(100)    NOT NULL,               -- 신청 시점 상품명 복사본
    points          INT UNSIGNED    NOT NULL,               -- 신청 시점 포인트 복사본(신청 즉시 차감)
    status          VARCHAR(20)     NOT NULL DEFAULT 'requested', -- requested / sent / rejected / canceled
    reject_reason   VARCHAR(200)    NULL,
    handled_by      BIGINT          NULL,                   -- 처리한 관리자
    handled_at      DATETIME        NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (handled_by) REFERENCES users(user_id),
    INDEX idx_exchanges_status (status, exchange_id),
    INDEX idx_exchanges_user (user_id, exchange_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------------------------------
-- 6. reward_transactions : 포인트 원장(추가만, 수정·삭제 없음)
-- ---------------------------------------------------------------------
CREATE TABLE reward_transactions (
    transaction_id  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    type            VARCHAR(20)     NOT NULL,               -- earn / bonus / exchange / refund / adjust
    amount          INT             NOT NULL,               -- 부호 있음(+적립, -교환 신청)
    balance_after   INT             NOT NULL,               -- 반영 후 잔액
    idempotency_key VARCHAR(100)    NOT NULL,               -- 같은 사건 중복 기록 방지. 예) earn:submission:42, bonus:daily:7:2026-10-09, exchange:15, refund:15
    submission_id   BIGINT UNSIGNED NULL,
    exchange_id     BIGINT UNSIGNED NULL,
    memo            VARCHAR(200)    NULL,                   -- 내역 화면에 보여 줄 한 줄
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uq_reward_tx_idempotency (idempotency_key),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (submission_id) REFERENCES mission_submissions(submission_id),
    FOREIGN KEY (exchange_id) REFERENCES reward_exchange_requests(exchange_id),
    INDEX idx_reward_tx_user (user_id, transaction_id),
    INDEX idx_reward_tx_user_type_time (user_id, type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------------------------------
-- (선택) 확인용 예시 미션 — 필요하면 주석을 풀어 실행한다.
-- ---------------------------------------------------------------------
-- INSERT INTO missions (type, title, reward_points, is_daily, status, starts_at, ends_at)
-- VALUES ('attendance', '출석 체크', 10, FALSE, 'open', NOW(), NOW() + INTERVAL 30 DAY);
--
-- INSERT INTO missions (type, title, reward_points, is_daily, status, starts_at, ends_at)
-- VALUES ('balance', '점심 메뉴, 한식 vs 양식?', 10, TRUE, 'open', NOW(), NOW() + INTERVAL 1 DAY);
-- INSERT INTO mission_questions (mission_id, sort_order, question_text, options_text)
-- VALUES (LAST_INSERT_ID(), 0, '오늘 점심, 뭐 먹을래요?', '한식\n양식');
