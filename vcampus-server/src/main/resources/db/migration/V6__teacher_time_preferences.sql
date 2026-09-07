-- Minimal persistence for automatic-scheduling hard/soft teacher time constraints.
CREATE TABLE IF NOT EXISTS `teacher_time_preferences` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `teacher_user_id` BIGINT UNSIGNED NOT NULL,
    `weekday` TINYINT UNSIGNED NOT NULL,
    `start_period` TINYINT UNSIGNED NOT NULL,
    `end_period` TINYINT UNSIGNED NOT NULL,
    `preference_type` VARCHAR(20) NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_teacher_time_preference_slot`
        (`teacher_user_id`, `weekday`, `start_period`, `end_period`),
    KEY `idx_teacher_time_preference_teacher` (`teacher_user_id`, `preference_type`),
    CONSTRAINT `fk_teacher_time_preference_teacher`
        FOREIGN KEY (`teacher_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `ck_teacher_time_preference_weekday` CHECK (`weekday` BETWEEN 1 AND 7),
    CONSTRAINT `ck_teacher_time_preference_period`
        CHECK (`start_period` >= 1 AND `end_period` >= `start_period`),
    CONSTRAINT `ck_teacher_time_preference_type`
        CHECK (`preference_type` IN ('UNAVAILABLE', 'AVOID', 'PREFERRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
