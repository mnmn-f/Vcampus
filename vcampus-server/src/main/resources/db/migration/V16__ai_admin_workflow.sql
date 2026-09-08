-- AI 反馈处理闭环：只增加管理状态与知识关联，不扩大学生隐私数据范围。
ALTER TABLE `ai_answer_feedback`
    ADD COLUMN `process_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' AFTER `question_preview`,
    ADD COLUMN `related_chunk_id` BIGINT UNSIGNED NULL AFTER `process_status`,
    ADD COLUMN `handled_by` BIGINT UNSIGNED NULL AFTER `related_chunk_id`,
    ADD COLUMN `handled_at` DATETIME(3) NULL AFTER `handled_by`,
    ADD KEY `idx_ai_feedback_process_time` (`process_status`, `created_at`),
    ADD CONSTRAINT `fk_ai_feedback_related_chunk` FOREIGN KEY (`related_chunk_id`)
        REFERENCES `ai_knowledge_chunks` (`id`) ON DELETE SET NULL,
    ADD CONSTRAINT `fk_ai_feedback_handler` FOREIGN KEY (`handled_by`)
        REFERENCES `users` (`id`) ON DELETE SET NULL,
    ADD CONSTRAINT `ck_ai_feedback_process_status`
        CHECK (`process_status` IN ('PENDING', 'RESOLVED', 'IGNORED'));


