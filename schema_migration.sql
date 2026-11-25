-- ==============================================================================
-- UniAttend Attendance Management System - Database Migration Script
-- Customized for: Institute of Technology, University of Moratuwa (ITUM), Sri Lanka
-- Target Database: MySQL 8.0+ / MariaDB 10.5+ / H2 (MySQL Mode)
-- Timezone: Asia/Colombo (+05:30)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. System Settings Table (GPS Coordinates, Server Address, Geofence Config)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `system_settings` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `setting_key` VARCHAR(60) NOT NULL UNIQUE,
    `setting_value` VARCHAR(500) NULL,
    `description` VARCHAR(255) NULL,
    `updated_at` DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed Default ITUM Geofencing & Network Settings
INSERT INTO `system_settings` (`setting_key`, `setting_value`, `description`, `updated_at`)
VALUES
    ('CAMPUS_NAME', 'Institute of Technology, University of Moratuwa (ITUM)', 'Official Institutional Campus Name', NOW()),
    ('GPS_CAMPUS_LATITUDE', '6.80894', 'ITUM Diyagama, Homagama Campus Latitude', NOW()),
    ('GPS_CAMPUS_LONGITUDE', '79.99282', 'ITUM Diyagama, Homagama Campus Longitude', NOW()),
    ('GPS_ALLOWED_RADIUS_METERS', '1000', 'Permitted Geofence Radius in Meters', NOW()),
    ('GPS_ENFORCEMENT_ENABLED', 'true', 'Enforce mobile GPS verification during attendance marking', NOW()),
    ('SERVER_BASE_URL', 'http://172.20.10.2:8080', 'Configurable LAN Server Base URL for QR codes', NOW()),
    ('ABSENCE_DOCUMENT_MANDATORY', 'false', 'Institutional policy requirement for absence proof documents', NOW())
ON DUPLICATE KEY UPDATE `updated_at` = NOW();

-- ------------------------------------------------------------------------------
-- 2. Audit Logs Table (Security Trail for Logins, Approvals, System Overrides)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `audit_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(60) NOT NULL,
    `action` VARCHAR(60) NOT NULL,
    `details` VARCHAR(500) NULL,
    `ip_address` VARCHAR(60) NULL,
    `created_at` DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 3. Extend Attendance Table with Geolocation Columns
-- ------------------------------------------------------------------------------
-- MySQL 8.0+ conditional column addition (run if columns do not already exist):
-- ALTER TABLE `attendance` ADD COLUMN `latitude` DOUBLE NULL AFTER `device_info`;
-- ALTER TABLE `attendance` ADD COLUMN `longitude` DOUBLE NULL AFTER `latitude`;

-- ------------------------------------------------------------------------------
-- 4. Absence Requests Table (Medical Certificates & Leave Workflow)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `absence_requests` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `subject_id` BIGINT NOT NULL,
    `session_id` BIGINT NULL,
    `reason_category` VARCHAR(40) NOT NULL,
    `start_date` DATE NOT NULL,
    `end_date` DATE NOT NULL,
    `explanation` VARCHAR(2000) NOT NULL,
    `attachment_path` VARCHAR(300) NULL,
    `attachment_original_name` VARCHAR(200) NULL,
    `attachment_type` VARCHAR(50) NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    `review_comments` VARCHAR(2000) NULL,
    `reviewed_by` VARCHAR(60) NULL,
    `reviewed_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL,
    CONSTRAINT `fk_absence_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_absence_subject` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_absence_session` FOREIGN KEY (`session_id`) REFERENCES `class_sessions` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- ROLLBACK GUIDANCE:
-- In case rollback is required:
--   DROP TABLE IF EXISTS `absence_requests`;
--   DROP TABLE IF EXISTS `audit_logs`;
--   DROP TABLE IF EXISTS `system_settings`;
--   ALTER TABLE `attendance` DROP COLUMN IF EXISTS `latitude`;
--   ALTER TABLE `attendance` DROP COLUMN IF EXISTS `longitude`;
-- ==============================================================================
