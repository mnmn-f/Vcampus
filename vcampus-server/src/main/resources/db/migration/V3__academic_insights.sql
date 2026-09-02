-- VCampus academic insights extension; run after V1 and V2.
-- This migration is additive and keeps legacy grade rows readable.
USE `vcampus`;

SET @vcampus_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'courses'
      AND column_name = 'semester_code'
);
SET @vcampus_column_sql = IF(@vcampus_column_exists = 0,
    'ALTER TABLE courses ADD COLUMN semester_code VARCHAR(32) NOT NULL DEFAULT ''UNSPECIFIED'' AFTER course_type',
    'SELECT 1');
PREPARE vc_column_stmt FROM @vcampus_column_sql;
EXECUTE vc_column_stmt;
DEALLOCATE PREPARE vc_column_stmt;

SET @vcampus_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'course_grades'
      AND column_name = 'gpa_included'
);
SET @vcampus_column_sql = IF(@vcampus_column_exists = 0,
    'ALTER TABLE course_grades ADD COLUMN gpa_included TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''Server-controlled inclusion flag for GPA calculations'' AFTER grade_point',
    'SELECT 1');
PREPARE vc_column_stmt FROM @vcampus_column_sql;
EXECUTE vc_column_stmt;
DEALLOCATE PREPARE vc_column_stmt;

-- The V2 demonstration offering has a fixed academic term. Other legacy rows
-- remain UNSPECIFIED until an academic administrator assigns their term.
UPDATE `courses`
SET `semester_code` = '2026-FALL'
WHERE `course_code` = 'DEMO-SE-001' AND `semester_code` = 'UNSPECIFIED';

SET @vcampus_index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'courses'
      AND index_name = 'idx_courses_semester'
);
SET @vcampus_index_sql = IF(@vcampus_index_exists = 0,
    'CREATE INDEX idx_courses_semester ON courses (semester_code, status)',
    'SELECT 1');
PREPARE vc_idx_stmt FROM @vcampus_index_sql;
EXECUTE vc_idx_stmt;
DEALLOCATE PREPARE vc_idx_stmt;
