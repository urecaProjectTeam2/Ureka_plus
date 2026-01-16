DROP DATABASE IF EXISTS billing_batch;
DROP DATABASE IF EXISTS billing_message;

CREATE DATABASE IF NOT EXISTS billing_batch;
CREATE DATABASE IF NOT EXISTS billing_message;

/*========================================================
billing_batch
=========================================================
*/

use billing_batch;

/* =========================================================
 * 1. 마스터 테이블 (부모)
 * ========================================================= */

CREATE TABLE IF NOT EXISTS `billing_user` (
  `user_id` BIGINT NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `billing_product` (
  `product_id` BIGINT NOT NULL AUTO_INCREMENT,
  `product_name` VARCHAR(50) NOT NULL,
  `product_type` ENUM('mobile','internet','iptv','dps','addon') NOT NULL,
  `price` INT NOT NULL,
  PRIMARY KEY (`product_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `billing_discount` (
  `discount_id` BIGINT NOT NULL AUTO_INCREMENT,
  `discount_name` VARCHAR(50) NOT NULL,
  `is_cash` ENUM('CASH','RATE') NOT NULL,
  `cash` INT DEFAULT NULL,
  `percent` DOUBLE DEFAULT NULL,
  PRIMARY KEY (`discount_id`)
) ENGINE=InnoDB;


/* =========================================================
 * 2. 관계 및 이력 테이블 (자식)
 * ========================================================= */

CREATE TABLE IF NOT EXISTS `user_subscribe_product` (
  `user_subscribe_product_id` BIGINT NOT NULL AUTO_INCREMENT,
  `created_month` DATE NOT NULL,
  `deleted_at` TIMESTAMP NULL,
  `user_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_subscribe_product_id`),
  KEY `idx_usp_user_active` (`user_id`, `deleted_at`),
  CONSTRAINT `fk_usp_user`
    FOREIGN KEY (`user_id`) REFERENCES `billing_user` (`user_id`),
  CONSTRAINT `fk_usp_product`
    FOREIGN KEY (`product_id`) REFERENCES `billing_product` (`product_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `user_subscribe_discount` (
  `user_discount_subscribe_id` BIGINT NOT NULL AUTO_INCREMENT,
  `discount_subscribe_month` DATE NOT NULL,
  `user_id` BIGINT NOT NULL,
  `discount_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_discount_subscribe_id`),
  KEY `idx_usd_user_month` (`user_id`, `discount_subscribe_month`),
  CONSTRAINT `fk_usd_user`
    FOREIGN KEY (`user_id`) REFERENCES `billing_user` (`user_id`),
  CONSTRAINT `fk_usd_discount`
    FOREIGN KEY (`discount_id`) REFERENCES `billing_discount` (`discount_id`),
  CONSTRAINT `fk_usd_product`
    FOREIGN KEY (`product_id`) REFERENCES `billing_product` (`product_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `unpaid` (
  `unpaid_id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `unpaid_price` INT NOT NULL,
  `unpaid_month` DATE NOT NULL,
  `is_paid` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`unpaid_id`),
  KEY `idx_unpaid_month_paid` (`unpaid_month`, `is_paid`),
  KEY `idx_unpaid_user` (`user_id`),
  CONSTRAINT `fk_unpaid_user`
    FOREIGN KEY (`user_id`) REFERENCES `billing_user` (`user_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `additional_charge` (
  `additional_charge_id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_name` VARCHAR(50) DEFAULT NULL,
  `price` INT NOT NULL,
  `additional_charge_month` DATE NOT NULL,
  `user_id` BIGINT NOT NULL,
  PRIMARY KEY (`additional_charge_id`),
  KEY `idx_additional_user_month` (`user_id`, `additional_charge_month`),
  CONSTRAINT `fk_additional_user`
    FOREIGN KEY (`user_id`) REFERENCES `billing_user` (`user_id`)
) ENGINE=InnoDB;


/* =========================================================
 * 3. 결과 및 로그 테이블 (물리적 FK 제거)
 * ========================================================= */

CREATE TABLE IF NOT EXISTS `billing_result` (
  `billing_result_id` BIGINT NOT NULL AUTO_INCREMENT,
  `settlement_month` DATE NOT NULL,
  `user_id` BIGINT NOT NULL,
  `total_price` INT NOT NULL,
  `settlement_details` JSON NOT NULL,
  `send_status` ENUM('READY', 'SENDING', 'SUCCESS', 'FAIL') DEFAULT 'READY',
  `batch_execution_id` BIGINT NOT NULL,
  `processed_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`billing_result_id`),
  UNIQUE KEY `uk_billing_month_user` (`settlement_month`, `user_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `batch_billing_error_log` (
  `error_log_id` BIGINT NOT NULL AUTO_INCREMENT,
  `job_execution_id` BIGINT NOT NULL,
  `step_execution_id` BIGINT DEFAULT NULL,
  `job_name` VARCHAR(100) NOT NULL,
  `step_name` VARCHAR(100) DEFAULT NULL,
  `settlement_month` DATE NOT NULL,
  `user_id` BIGINT NOT NULL,
  `error_type` ENUM('DATA', 'CALCULATION', 'POLICY', 'SYSTEM') NOT NULL,
  `error_code` VARCHAR(50) DEFAULT NULL,
  `error_message` VARCHAR(500) NOT NULL,
  `is_recoverable` TINYINT NOT NULL DEFAULT 1,
  `processed` TINYINT NOT NULL DEFAULT 0,
  `occurred_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`error_log_id`),
  KEY `idx_error_job_execution` (`job_execution_id`),
  KEY `idx_error_user_month` (`user_id`, `settlement_month`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `batch_billing_action_log` (
  `action_log_id` BIGINT NOT NULL AUTO_INCREMENT,
  `error_log_id` BIGINT NOT NULL,
  `actor_type` ENUM('ADMIN','SYSTEM') NOT NULL,
  `actor_id` VARCHAR(50) DEFAULT NULL,
  `action_type` ENUM(
    'RETRY_JOB','RETRY_STEP','RETRY_USER','DATA_FIX','IGNORE','MARK_FAILED'
  ) NOT NULL,
  `action_message` VARCHAR(500) DEFAULT NULL,
  `action_result` ENUM('SUCCESS','FAILED') NOT NULL,
  `action_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`action_log_id`),
  KEY `idx_action_error` (`error_log_id`),
  KEY `idx_action_actor` (`actor_type`, `actor_id`)
) ENGINE=InnoDB;


/* =========================================================
 * 4. Spring Batch 메타데이터 테이블
 * ========================================================= */

-- BATCH_JOB_INSTANCE
CREATE TABLE IF NOT EXISTS `BATCH_JOB_INSTANCE` (
  `JOB_INSTANCE_ID` BIGINT NOT NULL AUTO_INCREMENT,
  `VERSION` BIGINT NOT NULL,
  `JOB_NAME` VARCHAR(100) NOT NULL,
  `JOB_KEY` VARCHAR(32) NOT NULL,
  PRIMARY KEY (`JOB_INSTANCE_ID`),
  UNIQUE KEY `UK_JOB_NAME_KEY` (`JOB_NAME`, `JOB_KEY`)
) ENGINE=InnoDB;

-- BATCH_JOB_EXECUTION
CREATE TABLE IF NOT EXISTS `BATCH_JOB_EXECUTION` (
  `JOB_EXECUTION_ID` BIGINT NOT NULL AUTO_INCREMENT,
  `VERSION` BIGINT NOT NULL,
  `JOB_INSTANCE_ID` BIGINT NOT NULL,
  `CREATE_TIME` DATETIME NOT NULL,
  `START_TIME` DATETIME DEFAULT NULL,
  `END_TIME` DATETIME DEFAULT NULL,
  `STATUS` VARCHAR(10) DEFAULT NULL,
  `EXIT_CODE` VARCHAR(250) DEFAULT NULL,
  `EXIT_MESSAGE` VARCHAR(2500) DEFAULT NULL,
  `LAST_UPDATED` DATETIME DEFAULT NULL,
  PRIMARY KEY (`JOB_EXECUTION_ID`),
  KEY `JOB_INST_IDX` (`JOB_INSTANCE_ID`),
  CONSTRAINT `FK_JOB_EXEC_INST` FOREIGN KEY (`JOB_INSTANCE_ID`) REFERENCES `BATCH_JOB_INSTANCE`(`JOB_INSTANCE_ID`)
) ENGINE=InnoDB;

-- BATCH_STEP_EXECUTION
CREATE TABLE IF NOT EXISTS `BATCH_STEP_EXECUTION` (
  `STEP_EXECUTION_ID` BIGINT NOT NULL AUTO_INCREMENT,
  `VERSION` BIGINT NOT NULL,
  `STEP_NAME` VARCHAR(100) NOT NULL,
  `JOB_EXECUTION_ID` BIGINT NOT NULL,
  `START_TIME` DATETIME NOT NULL,
  `END_TIME` DATETIME DEFAULT NULL,
  `STATUS` VARCHAR(10) DEFAULT NULL,
  `COMMIT_COUNT` BIGINT DEFAULT NULL,
  `READ_COUNT` BIGINT DEFAULT NULL,
  `FILTER_COUNT` BIGINT DEFAULT NULL,
  `WRITE_COUNT` BIGINT DEFAULT NULL,
  `EXIT_CODE` VARCHAR(250) DEFAULT NULL,
  `EXIT_MESSAGE` VARCHAR(2500) DEFAULT NULL,
  `LAST_UPDATED` DATETIME DEFAULT NULL,
  PRIMARY KEY (`STEP_EXECUTION_ID`),
  KEY `JOB_EXEC_IDX` (`JOB_EXECUTION_ID`),
  CONSTRAINT `FK_STEP_JOB_EXEC` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `BATCH_JOB_EXECUTION`(`JOB_EXECUTION_ID`)
) ENGINE=InnoDB;

-- BATCH_JOB_EXECUTION_PARAMS
CREATE TABLE IF NOT EXISTS `BATCH_JOB_EXECUTION_PARAMS` (
  `JOB_EXECUTION_ID` BIGINT NOT NULL,
  `TYPE_CD` VARCHAR(6) NOT NULL,
  `KEY_NAME` VARCHAR(100) NOT NULL,
  `STRING_VAL` VARCHAR(250) DEFAULT NULL,
  `DATE_VAL` DATETIME DEFAULT NULL,
  `LONG_VAL` BIGINT DEFAULT NULL,
  `DOUBLE_VAL` DOUBLE DEFAULT NULL,
  `IDENTIFYING` VARCHAR(1) NOT NULL,
  PRIMARY KEY (`JOB_EXECUTION_ID`, `KEY_NAME`),
  CONSTRAINT `FK_JOB_EXEC_PARAMS` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `BATCH_JOB_EXECUTION`(`JOB_EXECUTION_ID`)
) ENGINE=InnoDB;

-- BATCH_STEP_EXECUTION_CONTEXT
CREATE TABLE IF NOT EXISTS `BATCH_STEP_EXECUTION_CONTEXT` (
  `STEP_EXECUTION_ID` BIGINT NOT NULL,
  `SHORT_CONTEXT` VARCHAR(2500) DEFAULT NULL,
  `SERIALIZED_CONTEXT` TEXT DEFAULT NULL,
  PRIMARY KEY (`STEP_EXECUTION_ID`),
  CONSTRAINT `FK_STEP_EXEC_CTX` FOREIGN KEY (`STEP_EXECUTION_ID`) REFERENCES `BATCH_STEP_EXECUTION`(`STEP_EXECUTION_ID`)
) ENGINE=InnoDB;



/*========================================================
billing_message
=========================================================
*/

use billing_message;

CREATE TABLE `billing_snapshot` (
  `billing_id` bigint NOT NULL,
  `settlement_month` date NOT NULL,
  `user_id` bigint NOT NULL,
  `total_price` int NOT NULL,
  `settlement_details` json NOT NULL,
  PRIMARY KEY (`billing_id`),
  UNIQUE KEY `uk_user_month` (`user_id`,`settlement_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `message` (
  `message_id` bigint NOT NULL AUTO_INCREMENT,
  `billing_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `status` enum('WAITED','CREATED','SENT','FAILED','BLOCKED') NOT NULL,
  `scheduled_at` datetime DEFAULT NULL,
  `retry_count` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`message_id`),
  KEY `idx_message_status_schedule` (`status`,`scheduled_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `message_send_log` (
  `message_send_log_id` bigint NOT NULL AUTO_INCREMENT,
  `message_id` bigint NOT NULL,
  `retry_no` int NOT NULL,
  `message_type` enum('SMS','EMAIL') NOT NULL,
  `provider_response_code` varchar(50) NOT NULL,
  `provider_response_message` varchar(500) DEFAULT NULL,
  `sent_at` datetime NOT NULL,
  PRIMARY KEY (`message_send_log_id`),
  UNIQUE KEY `uk_message_retry` (`message_id`,`retry_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `message_snapshot` (
  `message_id` bigint NOT NULL,
  `billing_id` bigint NOT NULL,
  `settlement_month` date NOT NULL,
  `user_id` bigint NOT NULL,
  `user_name` varchar(30) NOT NULL,
  `user_email` varchar(255) NOT NULL,
  `user_phone` varchar(255) NOT NULL,
  `total_price` int NOT NULL,
  `settlement_details` json NOT NULL,
  `message_content` mediumtext NOT NULL,
  PRIMARY KEY (`message_id`),
  UNIQUE KEY `uk_snapshot_user_month` (`user_id`,`settlement_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `message_template` (
  `template_id` bigint NOT NULL AUTO_INCREMENT,
  `template_name` varchar(30) NOT NULL,
  `message_type` enum('SMS','EMAIL') NOT NULL,
  `template_content` mediumtext NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`template_id`),
  UNIQUE KEY `uk_template_name_type` (`template_name`,`message_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `users` (
  `user_id` bigint NOT NULL,
  `name` varchar(30) NOT NULL,
  `email` varchar(255) NOT NULL,
  `phone` varchar(255) NOT NULL,
  `sending_day` int NOT NULL,
  `ban_start_time` time DEFAULT NULL,
  `ban_end_time` time DEFAULT NULL,
  `message_type` enum('SMS','EMAIL') NOT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;