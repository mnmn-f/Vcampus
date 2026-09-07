-- AI 管理质量工作台：知识版本和脱敏用户反馈。
CREATE TABLE IF NOT EXISTS `ai_knowledge_versions` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `chunk_id` BIGINT UNSIGNED NOT NULL,
    `version_no` INT UNSIGNED NOT NULL,
    `source_type` VARCHAR(40) NOT NULL,
    `title` VARCHAR(240) NULL,
    `content` LONGTEXT NOT NULL,
    `status` VARCHAR(20) NOT NULL,
    `created_by` BIGINT UNSIGNED NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_knowledge_version_no` (`chunk_id`, `version_no`),
    KEY `idx_ai_knowledge_versions_chunk_time` (`chunk_id`, `created_at`),
    CONSTRAINT `fk_ai_knowledge_versions_chunk` FOREIGN KEY (`chunk_id`)
        REFERENCES `ai_knowledge_chunks` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ai_knowledge_versions_user` FOREIGN KEY (`created_by`)
        REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `ai_answer_feedback` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `session_id` BIGINT UNSIGNED NOT NULL,
    `request_id` VARCHAR(80) NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `rating` VARCHAR(12) NOT NULL,
    `category` VARCHAR(40) NULL,
    `comment` VARCHAR(500) NULL,
    `question_preview` VARCHAR(240) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_feedback_user_request` (`user_id`, `request_id`),
    KEY `idx_ai_feedback_rating_time` (`rating`, `created_at`),
    CONSTRAINT `fk_ai_feedback_session` FOREIGN KEY (`session_id`)
        REFERENCES `ai_chat_sessions` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ai_feedback_user` FOREIGN KEY (`user_id`)
        REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `ck_ai_feedback_rating` CHECK (`rating` IN ('HELPFUL', 'UNHELPFUL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
