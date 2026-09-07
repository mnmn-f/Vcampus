-- 图书馆扩展。备份后在原 Vcampus 数据库执行；不删除原有数据。
-- 服务端启动时会自动执行同等升级，本文件保留给需要手工升级的部署。
-- 本脚本可重复执行，适用于已经执行过旧版 V6 的数据库。
USE `vcampus`;
SET NAMES utf8mb4;

SET @vcampus_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'books'
      AND column_name = 'publication_year'
);
SET @vcampus_column_sql = IF(@vcampus_column_exists = 0,
    'ALTER TABLE books ADD COLUMN publication_year INT NULL', 'SELECT 1');
PREPARE vc_library_column_stmt FROM @vcampus_column_sql;
EXECUTE vc_library_column_stmt;
DEALLOCATE PREPARE vc_library_column_stmt;

SET @vcampus_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'books'
      AND column_name = 'cover_image'
);
SET @vcampus_column_sql = IF(@vcampus_column_exists = 0,
    'ALTER TABLE books ADD COLUMN cover_image MEDIUMBLOB NULL', 'SELECT 1');
PREPARE vc_library_column_stmt FROM @vcampus_column_sql;
EXECUTE vc_library_column_stmt;
DEALLOCATE PREPARE vc_library_column_stmt;

CREATE TABLE IF NOT EXISTS library_pdf_resources (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(240) NOT NULL,
    description TEXT NULL,
    file_name VARCHAR(200) NOT NULL,
    file_size BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    uploader_id BIGINT UNSIGNED NOT NULL,
    uploader_name VARCHAR(160) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    uploaded_at BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL DEFAULT 0,
    reviewer_name VARCHAR(160) NULL,
    reviewed_at BIGINT NOT NULL DEFAULT 0,
    rejection_reason VARCHAR(1000) NULL,
    KEY idx_pdf_public(status, id),
    KEY idx_pdf_owner(uploader_id, id),
    CONSTRAINT fk_pdf_uploader FOREIGN KEY(uploader_id) REFERENCES users(id),
    CONSTRAINT ck_pdf_status CHECK(status IN ('PENDING','APPROVED','REJECTED','INACTIVE')),
    CONSTRAINT ck_pdf_size CHECK(file_size BETWEEN 8 AND 52428800)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS library_pdf_downloads (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    resource_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(240) NOT NULL,
    file_name VARCHAR(200) NOT NULL,
    file_size BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    transferred BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    started_at BIGINT NOT NULL,
    finished_at BIGINT NOT NULL DEFAULT 0,
    failure_reason VARCHAR(1000) NULL,
    KEY idx_pdf_download_user(user_id, id),
    CONSTRAINT fk_pdf_download_user FOREIGN KEY(user_id) REFERENCES users(id),
    CONSTRAINT fk_pdf_download_resource FOREIGN KEY(resource_id) REFERENCES library_pdf_resources(id),
    CONSTRAINT ck_pdf_download_status CHECK(status IN ('DOWNLOADING','COMPLETED','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
