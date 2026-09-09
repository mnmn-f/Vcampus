-- VCampus MySQL 8 baseline schema
-- Requires MySQL 8.0.16 or later (CHECK constraints are enforced from 8.0.16).
-- The application must use the same utf8mb4 connection settings as this schema.
-- This script is intentionally non-destructive: it creates missing objects only.
-- It is intended for a new/empty database; it does not repair an incompatible schema.

CREATE DATABASE IF NOT EXISTS `vcampus`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE `vcampus`;

SET NAMES utf8mb4;
SET SESSION time_zone = '+08:00';

-- ============================================================================
-- Identity, roles, permissions and audit
-- ============================================================================

CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(64) NOT NULL,
    `password_hash` VARCHAR(100) NOT NULL COMMENT 'BCrypt/Argon2 hash only; never store a plaintext password',
    `display_name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(255) NULL,
    `phone` VARCHAR(32) NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    `avatar_url` MEDIUMTEXT NULL,
    `last_login_at` DATETIME(3) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `version` INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_username` (`username`),
    UNIQUE KEY `uk_users_email` (`email`),
    UNIQUE KEY `uk_users_phone` (`phone`),
    KEY `idx_users_status_display_name` (`status`, `display_name`),
    CONSTRAINT `ck_users_status` CHECK (`status` IN ('ACTIVE', 'DISABLED', 'PENDING', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `roles` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(40) NOT NULL,
    `display_name` VARCHAR(80) NOT NULL,
    `description` VARCHAR(255) NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_roles_code` (`code`),
    CONSTRAINT `ck_roles_status` CHECK (`status` IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `permissions` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(80) NOT NULL,
    `display_name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(255) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permissions_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- One user may have many roles. The active role in a session is a presentation/
-- context choice; it never replaces server-side authorization from this table.
CREATE TABLE IF NOT EXISTS `user_roles` (
    `user_id` BIGINT UNSIGNED NOT NULL,
    `role_id` BIGINT UNSIGNED NOT NULL,
    `assigned_by` BIGINT UNSIGNED NULL,
    `assigned_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`user_id`, `role_id`),
    KEY `idx_user_roles_role` (`role_id`, `user_id`),
    CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_user_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
    CONSTRAINT `fk_user_roles_assigned_by` FOREIGN KEY (`assigned_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `role_permissions` (
    `role_id` BIGINT UNSIGNED NOT NULL,
    `permission_id` BIGINT UNSIGNED NOT NULL,
    `granted_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`role_id`, `permission_id`),
    KEY `idx_role_permissions_permission` (`permission_id`, `role_id`),
    CONSTRAINT `fk_role_permissions_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
    CONSTRAINT `fk_role_permissions_permission` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_sessions` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `token_hash` CHAR(64) NOT NULL COMMENT 'Hash of the session token; do not persist the raw token',
    `current_role_id` BIGINT UNSIGNED NULL,
    `client_version` VARCHAR(40) NULL,
    `client_ip` VARCHAR(64) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `last_seen_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `expires_at` DATETIME(3) NOT NULL,
    `revoked_at` DATETIME(3) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_sessions_token_hash` (`token_hash`),
    KEY `idx_user_sessions_user_active` (`user_id`, `revoked_at`, `expires_at`),
    CONSTRAINT `fk_user_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_user_sessions_current_role` FOREIGN KEY (`current_role_id`) REFERENCES `roles` (`id`),
    CONSTRAINT `ck_user_sessions_expiry` CHECK (`expires_at` > `created_at`),
    CONSTRAINT `ck_user_sessions_revoked` CHECK (`revoked_at` IS NULL OR `revoked_at` >= `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `login_audits` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NULL,
    `username_snapshot` VARCHAR(64) NOT NULL,
    `role_id` BIGINT UNSIGNED NULL COMMENT 'Role selected for a successful login, if any',
    `result_code` VARCHAR(80) NOT NULL,
    `client_ip` VARCHAR(64) NULL,
    `occurred_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_login_audits_user_time` (`user_id`, `occurred_at`),
    KEY `idx_login_audits_username_time` (`username_snapshot`, `occurred_at`),
    CONSTRAINT `fk_login_audits_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_login_audits_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `audit_logs` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `actor_user_id` BIGINT UNSIGNED NULL,
    `actor_role_id` BIGINT UNSIGNED NULL COMMENT 'Actual role used for this operation',
    `action` VARCHAR(100) NOT NULL,
    `resource_type` VARCHAR(80) NULL,
    `resource_id` BIGINT UNSIGNED NULL,
    `outcome` VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    `detail_json` JSON NULL,
    `occurred_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_audit_logs_actor_time` (`actor_user_id`, `occurred_at`),
    KEY `idx_audit_logs_resource_time` (`resource_type`, `resource_id`, `occurred_at`),
    CONSTRAINT `fk_audit_logs_actor_user` FOREIGN KEY (`actor_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_audit_logs_actor_role` FOREIGN KEY (`actor_role_id`) REFERENCES `roles` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_audit_logs_outcome` CHECK (`outcome` IN ('SUCCESS', 'FAILURE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `account_cancellation_requests` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `reason` VARCHAR(500) NOT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    `reviewed_by` BIGINT UNSIGNED NULL,
    `reviewed_at` DATETIME(3) NULL,
    `review_remark` VARCHAR(500) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    -- Historical rows remain available while this generated key enforces at
    -- most one pending request for each user.
    `pending_user_id` BIGINT UNSIGNED GENERATED ALWAYS AS (
        IF(`status` = 'PENDING', `user_id`, NULL)
    ) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account_cancellation_pending_user` (`pending_user_id`),
    KEY `idx_account_cancellation_user_status` (`user_id`, `status`, `created_at`),
    KEY `idx_account_cancellation_review` (`status`, `created_at`),
    CONSTRAINT `fk_account_cancellation_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_account_cancellation_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`),
    CONSTRAINT `ck_account_cancellation_reason` CHECK (CHAR_LENGTH(TRIM(`reason`)) > 0),
    CONSTRAINT `ck_account_cancellation_status` CHECK (
        `status` IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')
    ),
    CONSTRAINT `ck_account_cancellation_review` CHECK (
        (`status` IN ('PENDING', 'CANCELLED') AND `reviewed_by` IS NULL
            AND `reviewed_at` IS NULL AND `review_remark` IS NULL)
        OR (`status` IN ('APPROVED', 'REJECTED') AND `reviewed_by` IS NOT NULL
            AND `reviewed_at` IS NOT NULL AND `review_remark` IS NOT NULL
            AND CHAR_LENGTH(TRIM(`review_remark`)) > 0)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================================
-- Profiles and academic/teaching domain
-- ============================================================================

CREATE TABLE IF NOT EXISTS `student_profiles` (
    `user_id` BIGINT UNSIGNED NOT NULL,
    `student_no` VARCHAR(32) NOT NULL,
    `college` VARCHAR(120) NULL,
    `major` VARCHAR(120) NULL,
    `class_name` VARCHAR(120) NULL,
    `enrollment_year` YEAR NULL,
    `expected_graduation_year` YEAR NULL,
    `degree_level` VARCHAR(20) NULL,
    `gender` VARCHAR(16) NULL,
    `birth_date` DATE NULL,
    `address` VARCHAR(255) NULL,
    `emergency_contact` VARCHAR(100) NULL,
    `emergency_phone` VARCHAR(32) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_student_profiles_student_no` (`student_no`),
    KEY `idx_student_profiles_search` (`college`, `major`, `class_name`, `status`),
    CONSTRAINT `fk_student_profiles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `ck_student_profiles_status` CHECK (`status` IN ('ENROLLED', 'SUSPENDED', 'GRADUATED', 'WITHDRAWN')),
    CONSTRAINT `ck_student_profiles_years` CHECK (
        `expected_graduation_year` IS NULL OR `enrollment_year` IS NULL
        OR `expected_graduation_year` >= `enrollment_year`
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `teacher_profiles` (
    `user_id` BIGINT UNSIGNED NOT NULL,
    `employee_no` VARCHAR(32) NOT NULL,
    `department` VARCHAR(120) NULL,
    `title` VARCHAR(80) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_teacher_profiles_employee_no` (`employee_no`),
    KEY `idx_teacher_profiles_department` (`department`, `status`),
    CONSTRAINT `fk_teacher_profiles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `ck_teacher_profiles_status` CHECK (`status` IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `classrooms` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `building_name` VARCHAR(120) NOT NULL,
    `room_no` VARCHAR(40) NOT NULL,
    `classroom_type` VARCHAR(20) NOT NULL DEFAULT 'TEACHING',
    `capacity` INT UNSIGNED NOT NULL,
    `equipment_description` VARCHAR(500) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_classrooms_building_room` (`building_name`, `room_no`),
    KEY `idx_classrooms_search` (`classroom_type`, `capacity`, `status`),
    CONSTRAINT `ck_classrooms_type` CHECK (`classroom_type` IN ('TEACHING', 'LAB', 'MEETING', 'OTHER')),
    CONSTRAINT `ck_classrooms_status` CHECK (`status` IN ('AVAILABLE', 'MAINTENANCE', 'CLOSED')),
    CONSTRAINT `ck_classrooms_capacity` CHECK (`capacity` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `courses` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `course_code` VARCHAR(40) NOT NULL,
    `course_name` VARCHAR(160) NOT NULL,
    `course_type` VARCHAR(20) NOT NULL DEFAULT 'ELECTIVE',
    `credits` DECIMAL(4,2) NOT NULL,
    `total_hours` SMALLINT UNSIGNED NULL,
    `capacity` INT UNSIGNED NOT NULL,
    `description` TEXT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    `created_by` BIGINT UNSIGNED NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_courses_course_code` (`course_code`),
    KEY `idx_courses_search` (`course_name`, `course_type`, `status`),
    CONSTRAINT `fk_courses_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_courses_type` CHECK (`course_type` IN ('REQUIRED', 'ELECTIVE', 'PUBLIC', 'PRACTICE')),
    CONSTRAINT `ck_courses_status` CHECK (`status` IN ('DRAFT', 'PUBLISHED', 'CLOSED', 'ARCHIVED')),
    CONSTRAINT `ck_courses_credits` CHECK (`credits` > 0),
    CONSTRAINT `ck_courses_capacity` CHECK (`capacity` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `course_instructors` (
    `course_id` BIGINT UNSIGNED NOT NULL,
    `teacher_user_id` BIGINT UNSIGNED NOT NULL,
    `instructor_role` VARCHAR(20) NOT NULL DEFAULT 'PRIMARY',
    `assigned_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`course_id`, `teacher_user_id`),
    KEY `idx_course_instructors_teacher` (`teacher_user_id`, `course_id`),
    CONSTRAINT `fk_course_instructors_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`),
    CONSTRAINT `fk_course_instructors_teacher` FOREIGN KEY (`teacher_user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `ck_course_instructors_role` CHECK (`instructor_role` IN ('PRIMARY', 'ASSISTANT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `course_schedules` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `course_id` BIGINT UNSIGNED NOT NULL,
    `weekday` TINYINT UNSIGNED NOT NULL,
    `start_period` TINYINT UNSIGNED NOT NULL,
    `end_period` TINYINT UNSIGNED NOT NULL,
    `start_date` DATE NULL,
    `end_date` DATE NULL,
    `classroom_id` BIGINT UNSIGNED NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_course_schedules_course` (`course_id`),
    KEY `idx_course_schedules_classroom_time` (`classroom_id`, `weekday`, `start_period`, `end_period`),
    CONSTRAINT `fk_course_schedules_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`),
    CONSTRAINT `fk_course_schedules_classroom` FOREIGN KEY (`classroom_id`) REFERENCES `classrooms` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_course_schedules_weekday` CHECK (`weekday` BETWEEN 1 AND 7),
    CONSTRAINT `ck_course_schedules_period` CHECK (`start_period` >= 1 AND `end_period` >= `start_period`),
    CONSTRAINT `ck_course_schedules_date` CHECK (`end_date` IS NULL OR `start_date` IS NULL OR `end_date` >= `start_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `enrollments` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `student_user_id` BIGINT UNSIGNED NOT NULL,
    `course_id` BIGINT UNSIGNED NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    `enrolled_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `dropped_at` DATETIME(3) NULL,
    `version` INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_enrollments_student_course` (`student_user_id`, `course_id`),
    KEY `idx_enrollments_course_status` (`course_id`, `status`),
    CONSTRAINT `fk_enrollments_student` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_enrollments_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`),
    CONSTRAINT `ck_enrollments_status` CHECK (`status` IN ('ENROLLED', 'DROPPED', 'COMPLETED')),
    CONSTRAINT `ck_enrollments_dropped_at` CHECK (`status` <> 'DROPPED' OR `dropped_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `course_grades` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `enrollment_id` BIGINT UNSIGNED NOT NULL,
    `score` DECIMAL(5,2) NOT NULL,
    `grade_point` DECIMAL(4,2) NULL,
    `recorded_by` BIGINT UNSIGNED NOT NULL,
    `recorded_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `remark` VARCHAR(500) NULL,
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_grades_enrollment` (`enrollment_id`),
    KEY `idx_course_grades_recorder` (`recorded_by`, `recorded_at`),
    CONSTRAINT `fk_course_grades_enrollment` FOREIGN KEY (`enrollment_id`) REFERENCES `enrollments` (`id`),
    CONSTRAINT `fk_course_grades_recorder` FOREIGN KEY (`recorded_by`) REFERENCES `users` (`id`),
    CONSTRAINT `ck_course_grades_score` CHECK (`score` BETWEEN 0 AND 100),
    CONSTRAINT `ck_course_grades_point` CHECK (`grade_point` IS NULL OR `grade_point` BETWEEN 0 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `announcements` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `module_code` VARCHAR(20) NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` LONGTEXT NOT NULL,
    `visible_scope` VARCHAR(16) NOT NULL DEFAULT 'ALL',
    `target_role_id` BIGINT UNSIGNED NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    `publish_at` DATETIME(3) NULL,
    `expire_at` DATETIME(3) NULL,
    `publisher_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_announcements_module_status_time` (`module_code`, `status`, `publish_at`, `expire_at`),
    KEY `idx_announcements_target_role` (`target_role_id`, `status`),
    -- Keep the target-role CHECK enforceable on MySQL 8: a column used by a
    -- CHECK cannot also participate in an ON DELETE action. RESTRICT is the
    -- safer fallback because deleting a referenced role must not invalidate a
    -- ROLE-scoped announcement.
    CONSTRAINT `fk_announcements_target_role` FOREIGN KEY (`target_role_id`) REFERENCES `roles` (`id`),
    CONSTRAINT `fk_announcements_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `users` (`id`),
    CONSTRAINT `ck_announcements_module` CHECK (`module_code` IN ('SYSTEM', 'ACADEMIC', 'LIBRARY', 'DORM')),
    CONSTRAINT `ck_announcements_scope` CHECK (`visible_scope` IN ('ALL', 'ROLE')),
    CONSTRAINT `ck_announcements_scope_role` CHECK (`visible_scope` <> 'ROLE' OR `target_role_id` IS NOT NULL),
    CONSTRAINT `ck_announcements_status` CHECK (`status` IN ('DRAFT', 'SCHEDULED', 'PUBLISHED', 'EXPIRED', 'REVOKED')),
    CONSTRAINT `ck_announcements_time` CHECK (`expire_at` IS NULL OR `publish_at` IS NULL OR `expire_at` > `publish_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `competitions` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL,
    `description` TEXT NULL,
    `organizer_id` BIGINT UNSIGNED NOT NULL,
    `start_at` DATETIME(3) NOT NULL,
    `end_at` DATETIME(3) NOT NULL,
    `registration_deadline` DATETIME(3) NOT NULL,
    `capacity` INT UNSIGNED NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_competitions_status_deadline` (`status`, `registration_deadline`),
    CONSTRAINT `fk_competitions_organizer` FOREIGN KEY (`organizer_id`) REFERENCES `users` (`id`),
    CONSTRAINT `ck_competitions_time` CHECK (`end_at` > `start_at` AND `registration_deadline` <= `start_at`),
    CONSTRAINT `ck_competitions_capacity` CHECK (`capacity` IS NULL OR `capacity` > 0),
    CONSTRAINT `ck_competitions_status` CHECK (`status` IN ('DRAFT', 'PUBLISHED', 'CLOSED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `competition_registrations` (
    `competition_id` BIGINT UNSIGNED NOT NULL,
    `student_user_id` BIGINT UNSIGNED NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    `registered_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `cancelled_at` DATETIME(3) NULL,
    PRIMARY KEY (`competition_id`, `student_user_id`),
    KEY `idx_competition_registrations_student` (`student_user_id`, `status`),
    CONSTRAINT `fk_competition_registrations_competition` FOREIGN KEY (`competition_id`) REFERENCES `competitions` (`id`),
    CONSTRAINT `fk_competition_registrations_student` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `ck_competition_registrations_status` CHECK (`status` IN ('REGISTERED', 'CANCELLED')),
    CONSTRAINT `ck_competition_registrations_cancelled_at` CHECK (`status` <> 'CANCELLED' OR `cancelled_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `srtp_records` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `project_code` VARCHAR(64) NOT NULL,
    `student_user_id` BIGINT UNSIGNED NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `description` TEXT NULL,
    `credits` DECIMAL(4,2) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    `submitted_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `reviewed_by` BIGINT UNSIGNED NULL,
    `reviewed_at` DATETIME(3) NULL,
    `review_remark` VARCHAR(500) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_srtp_records_project_code` (`project_code`),
    KEY `idx_srtp_records_student_status` (`student_user_id`, `status`),
    KEY `idx_srtp_records_review` (`status`, `reviewed_at`),
    CONSTRAINT `fk_srtp_records_student` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_srtp_records_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_srtp_records_credits` CHECK (`credits` IS NULL OR `credits` > 0),
    CONSTRAINT `ck_srtp_records_status` CHECK (`status` IN ('SUBMITTED', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT `ck_srtp_records_reviewed_at` CHECK (`status` IN ('SUBMITTED', 'CANCELLED') OR `reviewed_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `classroom_reservations` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `classroom_id` BIGINT UNSIGNED NOT NULL,
    `applicant_id` BIGINT UNSIGNED NOT NULL,
    `purpose` VARCHAR(500) NOT NULL,
    `start_at` DATETIME(3) NOT NULL,
    `end_at` DATETIME(3) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `reviewed_by` BIGINT UNSIGNED NULL,
    `reviewed_at` DATETIME(3) NULL,
    `review_remark` VARCHAR(500) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_classroom_reservations_overlap` (`classroom_id`, `start_at`, `end_at`, `status`),
    KEY `idx_classroom_reservations_applicant` (`applicant_id`, `status`),
    CONSTRAINT `fk_classroom_reservations_classroom` FOREIGN KEY (`classroom_id`) REFERENCES `classrooms` (`id`),
    CONSTRAINT `fk_classroom_reservations_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_classroom_reservations_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_classroom_reservations_time` CHECK (`end_at` > `start_at`),
    CONSTRAINT `ck_classroom_reservations_status` CHECK (`status` IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT `ck_classroom_reservations_reviewed_at` CHECK (`status` IN ('PENDING', 'CANCELLED') OR `reviewed_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================================
-- Library
-- ============================================================================

CREATE TABLE IF NOT EXISTS `books` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `isbn` VARCHAR(32) NULL,
    `title` VARCHAR(240) NOT NULL,
    `author` VARCHAR(160) NULL,
    `publisher` VARCHAR(160) NULL,
    `category` VARCHAR(80) NULL,
    `total_copies` INT UNSIGNED NOT NULL DEFAULT 0,
    `available_copies` INT UNSIGNED NOT NULL DEFAULT 0,
    `location` VARCHAR(160) NULL,
    `description` TEXT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ON_SHELF',
    `created_by` BIGINT UNSIGNED NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_books_isbn` (`isbn`),
    KEY `idx_books_search` (`title`, `author`, `category`, `status`),
    CONSTRAINT `fk_books_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_books_status` CHECK (`status` IN ('ON_SHELF', 'UNAVAILABLE', 'ARCHIVED')),
    CONSTRAINT `ck_books_copies` CHECK (`available_copies` <= `total_copies`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `borrow_records` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `book_id` BIGINT UNSIGNED NOT NULL,
    `borrower_user_id` BIGINT UNSIGNED NOT NULL,
    `issued_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `due_at` DATETIME(3) NOT NULL,
    `returned_at` DATETIME(3) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'BORROWED',
    `renew_count` TINYINT UNSIGNED NOT NULL DEFAULT 0,
    `handled_by` BIGINT UNSIGNED NULL,
    `remark` VARCHAR(500) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_borrow_records_borrower_status` (`borrower_user_id`, `status`, `due_at`),
    KEY `idx_borrow_records_book_status` (`book_id`, `status`),
    CONSTRAINT `fk_borrow_records_book` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`),
    CONSTRAINT `fk_borrow_records_borrower` FOREIGN KEY (`borrower_user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_borrow_records_handler` FOREIGN KEY (`handled_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_borrow_records_time` CHECK (`due_at` > `issued_at`),
    CONSTRAINT `ck_borrow_records_status` CHECK (`status` IN ('BORROWED', 'OVERDUE', 'RETURNED', 'LOST')),
    CONSTRAINT `ck_borrow_records_returned_at` CHECK (`status` <> 'RETURNED' OR `returned_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `study_rooms` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `building_name` VARCHAR(120) NOT NULL,
    `room_no` VARCHAR(40) NOT NULL,
    `capacity` INT UNSIGNED NOT NULL,
    `open_time` TIME NOT NULL,
    `close_time` TIME NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    `description` VARCHAR(500) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_study_rooms_building_room` (`building_name`, `room_no`),
    KEY `idx_study_rooms_status_capacity` (`status`, `capacity`),
    CONSTRAINT `ck_study_rooms_capacity` CHECK (`capacity` > 0),
    CONSTRAINT `ck_study_rooms_time` CHECK (`close_time` > `open_time`),
    CONSTRAINT `ck_study_rooms_status` CHECK (`status` IN ('OPEN', 'MAINTENANCE', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `study_room_reservations` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `room_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `start_at` DATETIME(3) NOT NULL,
    `end_at` DATETIME(3) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    `cancelled_at` DATETIME(3) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_study_room_reservations_overlap` (`room_id`, `start_at`, `end_at`, `status`),
    KEY `idx_study_room_reservations_user` (`user_id`, `status`, `start_at`),
    CONSTRAINT `fk_study_room_reservations_room` FOREIGN KEY (`room_id`) REFERENCES `study_rooms` (`id`),
    CONSTRAINT `fk_study_room_reservations_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `ck_study_room_reservations_time` CHECK (`end_at` > `start_at`),
    CONSTRAINT `ck_study_room_reservations_status` CHECK (`status` IN ('RESERVED', 'CANCELLED', 'COMPLETED', 'NO_SHOW')),
    CONSTRAINT `ck_study_room_reservations_cancelled_at` CHECK (`status` <> 'CANCELLED' OR `cancelled_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `online_resources` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(240) NOT NULL,
    `resource_type` VARCHAR(40) NOT NULL,
    `url` VARCHAR(1000) NOT NULL,
    `description` TEXT NULL,
    `publisher_id` BIGINT UNSIGNED NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `published_at` DATETIME(3) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_online_resources_type_status` (`resource_type`, `status`, `published_at`),
    CONSTRAINT `fk_online_resources_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `users` (`id`),
    CONSTRAINT `ck_online_resources_status` CHECK (`status` IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `online_resource_access_logs` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `resource_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `accessed_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `client_ip` VARCHAR(64) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_resource_access_logs_resource_time` (`resource_id`, `accessed_at`),
    KEY `idx_resource_access_logs_user_time` (`user_id`, `accessed_at`),
    CONSTRAINT `fk_resource_access_logs_resource` FOREIGN KEY (`resource_id`) REFERENCES `online_resources` (`id`),
    CONSTRAINT `fk_resource_access_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================================
-- Store, account and orders
-- ============================================================================

CREATE TABLE IF NOT EXISTS `accounts` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `balance` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `version` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_accounts_user` (`user_id`),
    CONSTRAINT `fk_accounts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `ck_accounts_balance` CHECK (`balance` >= 0),
    CONSTRAINT `ck_accounts_status` CHECK (`status` IN ('ACTIVE', 'FROZEN', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Amount is signed: recharge/refund are positive, purchase/bill payment are negative.
-- reference_id is deliberately polymorphic without a foreign key because it can point
-- to an order or a dorm bill; idempotency_key provides the exact-once application guard.
CREATE TABLE IF NOT EXISTS `account_transactions` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `account_id` BIGINT UNSIGNED NOT NULL,
    `transaction_type` VARCHAR(24) NOT NULL,
    `amount` DECIMAL(12,2) NOT NULL,
    `balance_before` DECIMAL(12,2) NOT NULL,
    `balance_after` DECIMAL(12,2) NOT NULL,
    `reference_type` VARCHAR(32) NULL,
    `reference_id` BIGINT UNSIGNED NULL,
    `idempotency_key` VARCHAR(128) NOT NULL,
    `operator_id` BIGINT UNSIGNED NULL,
    `remark` VARCHAR(500) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account_transactions_idempotency` (`idempotency_key`),
    KEY `idx_account_transactions_account_time` (`account_id`, `created_at`),
    KEY `idx_account_transactions_reference` (`reference_type`, `reference_id`),
    CONSTRAINT `fk_account_transactions_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`),
    CONSTRAINT `fk_account_transactions_operator` FOREIGN KEY (`operator_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_account_transactions_type` CHECK (`transaction_type` IN ('RECHARGE', 'PURCHASE', 'REFUND', 'ADJUSTMENT', 'DORM_BILL_PAYMENT')),
    CONSTRAINT `ck_account_transactions_amount` CHECK (`amount` <> 0),
    CONSTRAINT `ck_account_transactions_balances` CHECK (`balance_before` >= 0 AND `balance_after` >= 0 AND `balance_after` = `balance_before` + `amount`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `products` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `sku` VARCHAR(64) NOT NULL,
    `name` VARCHAR(200) NOT NULL,
    `category` VARCHAR(80) NULL,
    `description` TEXT NULL,
    `price` DECIMAL(12,2) NOT NULL,
    `stock_qty` INT UNSIGNED NOT NULL DEFAULT 0,
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    `created_by` BIGINT UNSIGNED NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `version` INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_products_sku` (`sku`),
    KEY `idx_products_search` (`category`, `status`, `name`),
    CONSTRAINT `fk_products_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_products_price` CHECK (`price` > 0),
    CONSTRAINT `ck_products_status` CHECK (`status` IN ('DRAFT', 'ON_SALE', 'OFF_SALE', 'ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `shopping_carts` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_shopping_carts_user` (`user_id`),
    CONSTRAINT `fk_shopping_carts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `ck_shopping_carts_status` CHECK (`status` IN ('ACTIVE', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `cart_items` (
    `cart_id` BIGINT UNSIGNED NOT NULL,
    `product_id` BIGINT UNSIGNED NOT NULL,
    `quantity` INT UNSIGNED NOT NULL,
    `added_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`cart_id`, `product_id`),
    CONSTRAINT `fk_cart_items_cart` FOREIGN KEY (`cart_id`) REFERENCES `shopping_carts` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cart_items_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
    CONSTRAINT `ck_cart_items_quantity` CHECK (`quantity` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `store_orders` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `order_no` VARCHAR(64) NOT NULL,
    `buyer_id` BIGINT UNSIGNED NOT NULL,
    `total_amount` DECIMAL(12,2) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `paid_at` DATETIME(3) NULL,
    `cancelled_at` DATETIME(3) NULL,
    `completed_at` DATETIME(3) NULL,
    `version` INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_store_orders_order_no` (`order_no`),
    KEY `idx_store_orders_buyer_status` (`buyer_id`, `status`, `created_at`),
    KEY `idx_store_orders_sales` (`status`, `paid_at`),
    CONSTRAINT `fk_store_orders_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `users` (`id`),
    CONSTRAINT `ck_store_orders_total` CHECK (`total_amount` >= 0),
    CONSTRAINT `ck_store_orders_status` CHECK (`status` IN ('CREATED', 'PAID', 'CANCELLED', 'REFUNDED', 'COMPLETED')),
    CONSTRAINT `ck_store_orders_paid_at` CHECK (`status` NOT IN ('PAID', 'REFUNDED', 'COMPLETED') OR `paid_at` IS NOT NULL),
    CONSTRAINT `ck_store_orders_cancelled_at` CHECK (`status` <> 'CANCELLED' OR `cancelled_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `store_order_items` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT UNSIGNED NOT NULL,
    `product_id` BIGINT UNSIGNED NOT NULL,
    `product_name_snapshot` VARCHAR(200) NOT NULL,
    `unit_price_snapshot` DECIMAL(12,2) NOT NULL,
    `quantity` INT UNSIGNED NOT NULL,
    `line_amount` DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_store_order_items_order_product` (`order_id`, `product_id`),
    KEY `idx_store_order_items_product` (`product_id`),
    CONSTRAINT `fk_store_order_items_order` FOREIGN KEY (`order_id`) REFERENCES `store_orders` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_store_order_items_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
    CONSTRAINT `ck_store_order_items_price` CHECK (`unit_price_snapshot` > 0),
    CONSTRAINT `ck_store_order_items_quantity` CHECK (`quantity` > 0),
    CONSTRAINT `ck_store_order_items_amount` CHECK (`line_amount` = `unit_price_snapshot` * `quantity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================================
-- Dormitory
-- ============================================================================

CREATE TABLE IF NOT EXISTS `dorm_buildings` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `building_code` VARCHAR(40) NOT NULL,
    `building_name` VARCHAR(120) NOT NULL,
    `address` VARCHAR(255) NULL,
    `gender_policy` VARCHAR(16) NOT NULL DEFAULT 'MIXED',
    `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dorm_buildings_code` (`building_code`),
    KEY `idx_dorm_buildings_status` (`status`),
    CONSTRAINT `ck_dorm_buildings_gender` CHECK (`gender_policy` IN ('MALE', 'FEMALE', 'MIXED')),
    CONSTRAINT `ck_dorm_buildings_status` CHECK (`status` IN ('OPEN', 'MAINTENANCE', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `dorm_rooms` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `building_id` BIGINT UNSIGNED NOT NULL,
    `room_no` VARCHAR(40) NOT NULL,
    `floor_no` SMALLINT NOT NULL,
    `capacity` INT UNSIGNED NOT NULL,
    `room_type` VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    `status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    `description` VARCHAR(500) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dorm_rooms_building_room` (`building_id`, `room_no`),
    KEY `idx_dorm_rooms_search` (`building_id`, `floor_no`, `status`),
    CONSTRAINT `fk_dorm_rooms_building` FOREIGN KEY (`building_id`) REFERENCES `dorm_buildings` (`id`),
    CONSTRAINT `ck_dorm_rooms_capacity` CHECK (`capacity` > 0),
    CONSTRAINT `ck_dorm_rooms_type` CHECK (`room_type` IN ('STANDARD', 'SUITE', 'SPECIAL')),
    CONSTRAINT `ck_dorm_rooms_status` CHECK (`status` IN ('AVAILABLE', 'FULL', 'MAINTENANCE', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `dorm_beds` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `room_id` BIGINT UNSIGNED NOT NULL,
    `bed_no` VARCHAR(20) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dorm_beds_room_bed` (`room_id`, `bed_no`),
    KEY `idx_dorm_beds_room_status` (`room_id`, `status`),
    CONSTRAINT `fk_dorm_beds_room` FOREIGN KEY (`room_id`) REFERENCES `dorm_rooms` (`id`),
    CONSTRAINT `ck_dorm_beds_status` CHECK (`status` IN ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `accommodation_records` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `student_user_id` BIGINT UNSIGNED NOT NULL,
    `bed_id` BIGINT UNSIGNED NOT NULL,
    `start_date` DATE NOT NULL,
    `end_date` DATE NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `created_by` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    -- These generated keys enforce at most one active bed per student and one
    -- active student per bed while allowing unlimited historical ENDED rows.
    `active_student_id` BIGINT UNSIGNED GENERATED ALWAYS AS (IF(`status` = 'ACTIVE', `student_user_id`, NULL)) STORED,
    `active_bed_id` BIGINT UNSIGNED GENERATED ALWAYS AS (IF(`status` = 'ACTIVE', `bed_id`, NULL)) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_accommodation_active_student` (`active_student_id`),
    UNIQUE KEY `uk_accommodation_active_bed` (`active_bed_id`),
    KEY `idx_accommodation_student_history` (`student_user_id`, `start_date`, `status`),
    KEY `idx_accommodation_bed_history` (`bed_id`, `start_date`, `status`),
    CONSTRAINT `fk_accommodation_records_student` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_accommodation_records_bed` FOREIGN KEY (`bed_id`) REFERENCES `dorm_beds` (`id`),
    CONSTRAINT `fk_accommodation_records_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
    CONSTRAINT `ck_accommodation_records_status` CHECK (`status` IN ('ACTIVE', 'ENDED', 'CANCELLED')),
    CONSTRAINT `ck_accommodation_records_date` CHECK (`end_date` IS NULL OR `end_date` >= `start_date`),
    CONSTRAINT `ck_accommodation_records_end` CHECK (`status` = 'ACTIVE' OR `end_date` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `accommodation_requests` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `student_user_id` BIGINT UNSIGNED NOT NULL,
    `request_type` VARCHAR(20) NOT NULL,
    `current_record_id` BIGINT UNSIGNED NULL,
    `requested_bed_id` BIGINT UNSIGNED NULL,
    `reason` VARCHAR(500) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `reviewed_by` BIGINT UNSIGNED NULL,
    `reviewed_at` DATETIME(3) NULL,
    `review_remark` VARCHAR(500) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_accommodation_requests_student_status` (`student_user_id`, `status`, `created_at`),
    KEY `idx_accommodation_requests_review` (`status`, `created_at`),
    CONSTRAINT `fk_accommodation_requests_student` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_accommodation_requests_current` FOREIGN KEY (`current_record_id`) REFERENCES `accommodation_records` (`id`),
    CONSTRAINT `fk_accommodation_requests_bed` FOREIGN KEY (`requested_bed_id`) REFERENCES `dorm_beds` (`id`),
    CONSTRAINT `fk_accommodation_requests_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_accommodation_requests_type` CHECK (`request_type` IN ('CHECK_IN', 'TRANSFER', 'CHECK_OUT')),
    CONSTRAINT `ck_accommodation_requests_status` CHECK (`status` IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT `ck_accommodation_requests_bed` CHECK (
        (`request_type` IN ('CHECK_IN', 'TRANSFER') AND `requested_bed_id` IS NOT NULL)
        OR (`request_type` = 'CHECK_OUT' AND `requested_bed_id` IS NULL)
    ),
    CONSTRAINT `ck_accommodation_requests_current` CHECK (`request_type` <> 'CHECK_OUT' OR `current_record_id` IS NOT NULL),
    CONSTRAINT `ck_accommodation_requests_reviewed_at` CHECK (`status` IN ('PENDING', 'CANCELLED') OR `reviewed_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `leave_requests` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `student_user_id` BIGINT UNSIGNED NOT NULL,
    `leave_type` VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    `start_at` DATETIME(3) NOT NULL,
    `end_at` DATETIME(3) NOT NULL,
    `reason` VARCHAR(500) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `reviewed_by` BIGINT UNSIGNED NULL,
    `reviewed_at` DATETIME(3) NULL,
    `review_remark` VARCHAR(500) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_leave_requests_student_status_time` (`student_user_id`, `status`, `start_at`),
    CONSTRAINT `fk_leave_requests_student` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_leave_requests_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_leave_requests_type` CHECK (`leave_type` IN ('PERSONAL', 'ILLNESS', 'OFF_CAMPUS', 'OTHER')),
    CONSTRAINT `ck_leave_requests_time` CHECK (`end_at` > `start_at`),
    CONSTRAINT `ck_leave_requests_status` CHECK (`status` IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT `ck_leave_requests_reviewed_at` CHECK (`status` IN ('PENDING', 'CANCELLED') OR `reviewed_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `access_records` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `student_user_id` BIGINT UNSIGNED NOT NULL,
    `record_type` VARCHAR(8) NOT NULL,
    `occurred_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `door_name` VARCHAR(120) NULL,
    `source` VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
    `note` VARCHAR(500) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_access_records_student_time` (`student_user_id`, `occurred_at`),
    KEY `idx_access_records_type_time` (`record_type`, `occurred_at`),
    CONSTRAINT `fk_access_records_student` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `ck_access_records_type` CHECK (`record_type` IN ('ENTRY', 'EXIT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `late_return_alerts` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `student_user_id` BIGINT UNSIGNED NOT NULL,
    `alert_date` DATE NOT NULL,
    `detected_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    `handled_by` BIGINT UNSIGNED NULL,
    `handled_at` DATETIME(3) NULL,
    `note` VARCHAR(500) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_late_return_alerts_student_date` (`student_user_id`, `alert_date`),
    KEY `idx_late_return_alerts_status` (`status`, `alert_date`),
    CONSTRAINT `fk_late_return_alerts_student` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_late_return_alerts_handler` FOREIGN KEY (`handled_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_late_return_alerts_status` CHECK (`status` IN ('OPEN', 'CONFIRMED', 'CLEARED', 'IGNORED')),
    CONSTRAINT `ck_late_return_alerts_handled_at` CHECK (`status` = 'OPEN' OR `handled_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `hygiene_inspections` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `room_id` BIGINT UNSIGNED NOT NULL,
    `inspector_id` BIGINT UNSIGNED NOT NULL,
    `inspected_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `score` DECIMAL(5,2) NOT NULL,
    `result` VARCHAR(20) NOT NULL,
    `issue_description` VARCHAR(1000) NULL,
    `status` VARCHAR(24) NOT NULL DEFAULT 'NORMAL',
    `rectified_at` DATETIME(3) NULL,
    `rectification_note` VARCHAR(1000) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_hygiene_inspections_room_time` (`room_id`, `inspected_at`),
    KEY `idx_hygiene_inspections_status` (`status`, `inspected_at`),
    CONSTRAINT `fk_hygiene_inspections_room` FOREIGN KEY (`room_id`) REFERENCES `dorm_rooms` (`id`),
    CONSTRAINT `fk_hygiene_inspections_inspector` FOREIGN KEY (`inspector_id`) REFERENCES `users` (`id`),
    CONSTRAINT `ck_hygiene_inspections_score` CHECK (`score` BETWEEN 0 AND 100),
    CONSTRAINT `ck_hygiene_inspections_result` CHECK (`result` IN ('PASS', 'FAIL')),
    CONSTRAINT `ck_hygiene_inspections_status` CHECK (`status` IN ('NORMAL', 'RECTIFICATION_REQUIRED', 'RECTIFIED')),
    CONSTRAINT `ck_hygiene_inspections_rectified_at` CHECK (`status` <> 'RECTIFIED' OR `rectified_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `repair_orders` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `room_id` BIGINT UNSIGNED NOT NULL,
    `reporter_id` BIGINT UNSIGNED NOT NULL,
    `category` VARCHAR(40) NOT NULL,
    `description` VARCHAR(1000) NOT NULL,
    `priority` VARCHAR(12) NOT NULL DEFAULT 'NORMAL',
    `status` VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    `handler_id` BIGINT UNSIGNED NULL,
    `submitted_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `accepted_at` DATETIME(3) NULL,
    `completed_at` DATETIME(3) NULL,
    `evaluation_score` TINYINT UNSIGNED NULL,
    `evaluation_note` VARCHAR(500) NULL,
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_repair_orders_room_status` (`room_id`, `status`, `submitted_at`),
    KEY `idx_repair_orders_queue` (`status`, `priority`, `submitted_at`),
    CONSTRAINT `fk_repair_orders_room` FOREIGN KEY (`room_id`) REFERENCES `dorm_rooms` (`id`),
    CONSTRAINT `fk_repair_orders_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_repair_orders_handler` FOREIGN KEY (`handler_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_repair_orders_priority` CHECK (`priority` IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT `ck_repair_orders_status` CHECK (`status` IN ('SUBMITTED', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT `ck_repair_orders_score` CHECK (`evaluation_score` IS NULL OR `evaluation_score` BETWEEN 1 AND 5),
    CONSTRAINT `ck_repair_orders_completed_at` CHECK (`status` <> 'COMPLETED' OR `completed_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `utility_bills` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `room_id` BIGINT UNSIGNED NOT NULL,
    `period_start` DATE NOT NULL,
    `period_end` DATE NOT NULL,
    `electricity_units` DECIMAL(12,3) NOT NULL DEFAULT 0,
    `water_units` DECIMAL(12,3) NOT NULL DEFAULT 0,
    `total_amount` DECIMAL(12,2) NOT NULL,
    `due_at` DATETIME(3) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    `created_by` BIGINT UNSIGNED NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_utility_bills_room_period` (`room_id`, `period_start`, `period_end`),
    KEY `idx_utility_bills_status_due` (`status`, `due_at`),
    CONSTRAINT `fk_utility_bills_room` FOREIGN KEY (`room_id`) REFERENCES `dorm_rooms` (`id`),
    CONSTRAINT `fk_utility_bills_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_utility_bills_period` CHECK (`period_end` >= `period_start`),
    CONSTRAINT `ck_utility_bills_units` CHECK (`electricity_units` >= 0 AND `water_units` >= 0),
    CONSTRAINT `ck_utility_bills_amount` CHECK (`total_amount` >= 0),
    CONSTRAINT `ck_utility_bills_status` CHECK (`status` IN ('UNPAID', 'PARTIAL', 'PAID', 'VOID'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `utility_allocations` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `bill_id` BIGINT UNSIGNED NOT NULL,
    `student_user_id` BIGINT UNSIGNED NOT NULL,
    `amount` DECIMAL(12,2) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    `paid_transaction_id` BIGINT UNSIGNED NULL,
    `paid_at` DATETIME(3) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_utility_allocations_bill_student` (`bill_id`, `student_user_id`),
    KEY `idx_utility_allocations_student_status` (`student_user_id`, `status`),
    CONSTRAINT `fk_utility_allocations_bill` FOREIGN KEY (`bill_id`) REFERENCES `utility_bills` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_utility_allocations_student` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
    -- A paid allocation requires this reference by CHECK, so use RESTRICT
    -- instead of an ON DELETE action that MySQL forbids on CHECK columns.
    CONSTRAINT `fk_utility_allocations_transaction` FOREIGN KEY (`paid_transaction_id`) REFERENCES `account_transactions` (`id`),
    CONSTRAINT `ck_utility_allocations_amount` CHECK (`amount` > 0),
    CONSTRAINT `ck_utility_allocations_status` CHECK (`status` IN ('UNPAID', 'PAID', 'WAIVED')),
    CONSTRAINT `ck_utility_allocations_paid` CHECK (`status` <> 'PAID' OR (`paid_transaction_id` IS NOT NULL AND `paid_at` IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================================
-- AI extension boundary (storage only; no model implementation in V1)
-- ============================================================================

CREATE TABLE IF NOT EXISTS `ai_chat_sessions` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `title` VARCHAR(200) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `model_name` VARCHAR(120) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_ai_chat_sessions_user_status` (`user_id`, `status`, `updated_at`),
    CONSTRAINT `fk_ai_chat_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `ck_ai_chat_sessions_status` CHECK (`status` IN ('ACTIVE', 'ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `ai_chat_messages` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `session_id` BIGINT UNSIGNED NOT NULL,
    `request_id` VARCHAR(80) NULL,
    `sequence_no` INT UNSIGNED NOT NULL,
    `sender_type` VARCHAR(12) NOT NULL,
    `content` LONGTEXT NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_chat_messages_session_sequence` (`session_id`, `sequence_no`),
    KEY `idx_ai_chat_messages_request` (`request_id`),
    CONSTRAINT `fk_ai_chat_messages_session` FOREIGN KEY (`session_id`) REFERENCES `ai_chat_sessions` (`id`) ON DELETE CASCADE,
    CONSTRAINT `ck_ai_chat_messages_sender` CHECK (`sender_type` IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
    CONSTRAINT `ck_ai_chat_messages_status` CHECK (`status` IN ('PENDING', 'STREAMING', 'COMPLETED', 'CANCELLED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `ai_knowledge_chunks` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `source_type` VARCHAR(40) NOT NULL,
    `source_ref_id` BIGINT UNSIGNED NULL,
    `title` VARCHAR(240) NULL,
    `content` LONGTEXT NOT NULL,
    `embedding_json` JSON NULL COMMENT 'Reserved for a later RAG implementation; nullable in V1',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `updated_by` BIGINT UNSIGNED NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_ai_knowledge_chunks_source` (`source_type`, `source_ref_id`, `status`),
    KEY `idx_ai_knowledge_chunks_status_time` (`status`, `updated_at`),
    FULLTEXT KEY `ft_ai_knowledge_chunks_content` (`content`),
    CONSTRAINT `fk_ai_knowledge_chunks_updater` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_ai_knowledge_chunks_status` CHECK (`status` IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `ai_tool_call_logs` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `session_id` BIGINT UNSIGNED NULL,
    `message_id` BIGINT UNSIGNED NULL,
    `request_id` VARCHAR(80) NOT NULL,
    `tool_name` VARCHAR(120) NOT NULL,
    `action_type` VARCHAR(12) NOT NULL DEFAULT 'READ',
    `arguments_json` JSON NULL,
    `result_summary` TEXT NULL,
    `status` VARCHAR(24) NOT NULL DEFAULT 'REQUESTED',
    `requested_by` BIGINT UNSIGNED NOT NULL,
    `confirmed_by` BIGINT UNSIGNED NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `completed_at` DATETIME(3) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_ai_tool_call_logs_request` (`request_id`),
    KEY `idx_ai_tool_call_logs_user_time` (`requested_by`, `created_at`),
    KEY `idx_ai_tool_call_logs_status` (`status`, `created_at`),
    CONSTRAINT `fk_ai_tool_call_logs_session` FOREIGN KEY (`session_id`) REFERENCES `ai_chat_sessions` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_ai_tool_call_logs_message` FOREIGN KEY (`message_id`) REFERENCES `ai_chat_messages` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_ai_tool_call_logs_requester` FOREIGN KEY (`requested_by`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_ai_tool_call_logs_confirmer` FOREIGN KEY (`confirmed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_ai_tool_call_logs_action` CHECK (`action_type` IN ('READ', 'WRITE')),
    CONSTRAINT `ck_ai_tool_call_logs_status` CHECK (`status` IN ('REQUESTED', 'CONFIRM_REQUIRED', 'CONFIRMED', 'CANCELLED', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT `ck_ai_tool_call_logs_completed_at` CHECK (`status` IN ('REQUESTED', 'CONFIRM_REQUIRED', 'CONFIRMED') OR `completed_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Useful read-only views. They do not replace service-layer permission checks.
CREATE OR REPLACE VIEW `vw_current_accommodation` AS
SELECT
    ar.`id` AS `accommodation_id`,
    ar.`student_user_id`,
    ar.`bed_id`,
    dr.`id` AS `room_id`,
    dr.`room_no`,
    db.`id` AS `building_id`,
    db.`building_name`,
    ar.`start_date`
FROM `accommodation_records` ar
JOIN `dorm_beds` bed ON bed.`id` = ar.`bed_id`
JOIN `dorm_rooms` dr ON dr.`id` = bed.`room_id`
JOIN `dorm_buildings` db ON db.`id` = dr.`building_id`
WHERE ar.`status` = 'ACTIVE';

CREATE OR REPLACE VIEW `vw_store_sales` AS
SELECT
    soi.`product_id`,
    soi.`product_name_snapshot`,
    SUM(soi.`quantity`) AS `sold_quantity`,
    SUM(soi.`line_amount`) AS `sales_amount`
FROM `store_order_items` soi
JOIN `store_orders` so ON so.`id` = soi.`order_id`
WHERE so.`status` IN ('PAID', 'COMPLETED')
GROUP BY soi.`product_id`, soi.`product_name_snapshot`;
