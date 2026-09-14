-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: vcampus
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `access_records`
--

DROP TABLE IF EXISTS `access_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `access_records` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `student_user_id` bigint unsigned NOT NULL,
  `record_type` varchar(8) NOT NULL,
  `occurred_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `door_name` varchar(120) DEFAULT NULL,
  `source` varchar(40) NOT NULL DEFAULT 'MANUAL',
  `note` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_access_records_student_time` (`student_user_id`,`occurred_at`),
  KEY `idx_access_records_type_time` (`record_type`,`occurred_at`),
  CONSTRAINT `fk_access_records_student` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_access_records_type` CHECK ((`record_type` in (_utf8mb4'ENTRY',_utf8mb4'EXIT')))
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `access_records`
--

LOCK TABLES `access_records` WRITE;
/*!40000 ALTER TABLE `access_records` DISABLE KEYS */;
INSERT INTO `access_records` VALUES (11,1,'EXIT','2026-09-07 07:52:00.000','D1 南门','CARD',NULL),(12,1,'ENTRY','2026-09-07 21:35:00.000','D1 南门','CARD',NULL),(13,1,'EXIT','2026-09-08 07:48:00.000','D1 南门','CARD',NULL),(14,1,'ENTRY','2026-09-08 22:05:00.000','D1 南门','CARD',NULL),(15,1,'EXIT','2026-09-09 08:10:00.000','D1 南门','CARD',NULL),(16,1,'ENTRY','2026-09-09 23:40:00.000','D1 南门','CARD','门禁后刷卡进入'),(17,1,'EXIT','2026-09-10 07:55:00.000','D1 南门','CARD',NULL),(18,1,'ENTRY','2026-09-10 21:20:00.000','D1 南门','CARD',NULL),(19,1,'EXIT','2026-09-11 07:45:00.000','D1 南门','CARD',NULL),(20,1,'ENTRY','2026-09-11 12:10:00.000','D1 南门','CARD',NULL),(21,11,'EXIT','2026-09-06 08:00:00.000','D1 南门','CARD',NULL),(22,11,'ENTRY','2026-09-06 23:20:00.000','D1 南门','CARD',NULL),(23,11,'EXIT','2026-09-10 08:00:00.000','D1 南门','CARD',NULL),(24,11,'ENTRY','2026-09-10 22:10:00.000','D1 南门','CARD',NULL),(25,12,'ENTRY','2026-09-02 21:00:00.000','D1 南门','CARD',NULL),(26,12,'EXIT','2026-09-07 09:30:00.000','D1 南门','CARD',NULL),(27,13,'ENTRY','2026-08-22 20:15:00.000','D1 南门','CARD',NULL),(28,13,'EXIT','2026-09-02 14:00:00.000','D1 南门','CARD',NULL),(29,14,'EXIT','2026-08-30 07:30:00.000','D1 南门','CARD',NULL),(30,15,'EXIT','2026-09-09 09:00:00.000','D1 南门','CARD',NULL),(31,15,'ENTRY','2026-09-09 23:35:00.000','D1 南门','CARD',NULL),(32,15,'EXIT','2026-09-10 08:20:00.000','D1 南门','CARD',NULL),(33,15,'ENTRY','2026-09-10 21:50:00.000','D1 南门','CARD',NULL),(34,16,'EXIT','2026-08-22 10:00:00.000','D2 东门','CARD',NULL),(35,19,'EXIT','2026-09-10 18:00:00.000','D2 东门','CARD',NULL),(36,19,'ENTRY','2026-09-10 21:05:00.000','D2 东门','CARD',NULL),(37,1,'EXIT','2026-09-13 13:55:04.133',NULL,'SELF',NULL),(38,1,'ENTRY','2026-09-13 13:55:08.299',NULL,'SELF',NULL),(39,1,'EXIT','2026-09-13 17:24:13.550',NULL,'SELF',NULL),(40,1,'ENTRY','2026-09-13 17:24:17.519',NULL,'SELF',NULL);
/*!40000 ALTER TABLE `access_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `accommodation_records`
--

DROP TABLE IF EXISTS `accommodation_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `accommodation_records` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `student_user_id` bigint unsigned NOT NULL,
  `bed_id` bigint unsigned NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `created_by` bigint unsigned NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `active_student_id` bigint unsigned GENERATED ALWAYS AS (if((`status` = _utf8mb4'ACTIVE'),`student_user_id`,NULL)) STORED,
  `active_bed_id` bigint unsigned GENERATED ALWAYS AS (if((`status` = _utf8mb4'ACTIVE'),`bed_id`,NULL)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_accommodation_active_student` (`active_student_id`),
  UNIQUE KEY `uk_accommodation_active_bed` (`active_bed_id`),
  KEY `idx_accommodation_student_history` (`student_user_id`,`start_date`,`status`),
  KEY `idx_accommodation_bed_history` (`bed_id`,`start_date`,`status`),
  KEY `fk_accommodation_records_creator` (`created_by`),
  CONSTRAINT `fk_accommodation_records_bed` FOREIGN KEY (`bed_id`) REFERENCES `dorm_beds` (`id`),
  CONSTRAINT `fk_accommodation_records_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_accommodation_records_student` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_accommodation_records_date` CHECK (((`end_date` is null) or (`end_date` >= `start_date`))),
  CONSTRAINT `ck_accommodation_records_end` CHECK (((`status` = _utf8mb4'ACTIVE') or (`end_date` is not null))),
  CONSTRAINT `ck_accommodation_records_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'ENDED',_utf8mb4'CANCELLED')))
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `accommodation_records`
--

LOCK TABLES `accommodation_records` WRITE;
/*!40000 ALTER TABLE `accommodation_records` DISABLE KEYS */;
INSERT INTO `accommodation_records` (`id`, `student_user_id`, `bed_id`, `start_date`, `end_date`, `status`, `created_by`, `created_at`, `updated_at`) VALUES (11,1,27,'2026-02-23','2026-08-31','ENDED',7,'2026-09-11 15:01:08.617','2026-09-11 15:01:08.617'),(12,1,1,'2026-09-01','2026-09-13','ENDED',7,'2026-09-11 15:01:08.627','2026-09-13 17:26:54.815'),(13,11,2,'2026-09-01',NULL,'ACTIVE',7,'2026-09-11 15:01:08.627','2026-09-11 15:01:08.627'),(14,12,3,'2026-09-01',NULL,'ACTIVE',7,'2026-09-11 15:01:08.627','2026-09-11 15:01:08.627'),(15,13,29,'2026-09-01',NULL,'ACTIVE',7,'2026-09-11 15:01:08.627','2026-09-11 15:01:08.627'),(16,15,28,'2026-09-01',NULL,'ACTIVE',7,'2026-09-11 15:01:08.627','2026-09-11 15:01:08.627'),(17,14,33,'2026-09-01','2026-09-13','ENDED',7,'2026-09-11 15:01:08.627','2026-09-13 17:27:25.466'),(18,16,41,'2026-09-01',NULL,'ACTIVE',7,'2026-09-11 15:01:08.627','2026-09-11 15:01:08.627'),(19,19,45,'2026-09-01',NULL,'ACTIVE',7,'2026-09-11 15:01:08.627','2026-09-11 15:01:08.627'),(20,1,4,'2026-09-13',NULL,'ACTIVE',7,'2026-09-13 17:26:54.820','2026-09-13 17:26:54.820'),(21,18,27,'2026-09-13',NULL,'ACTIVE',7,'2026-09-13 17:27:16.083','2026-09-13 17:27:16.083'),(22,14,26,'2026-09-13',NULL,'ACTIVE',7,'2026-09-13 17:27:25.468','2026-09-13 17:27:25.468');
/*!40000 ALTER TABLE `accommodation_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `accommodation_requests`
--

DROP TABLE IF EXISTS `accommodation_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `accommodation_requests` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `student_user_id` bigint unsigned NOT NULL,
  `request_type` varchar(20) NOT NULL,
  `current_record_id` bigint unsigned DEFAULT NULL,
  `requested_bed_id` bigint unsigned DEFAULT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `reviewed_by` bigint unsigned DEFAULT NULL,
  `reviewed_at` datetime(3) DEFAULT NULL,
  `review_remark` varchar(500) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_accommodation_requests_student_status` (`student_user_id`,`status`,`created_at`),
  KEY `idx_accommodation_requests_review` (`status`,`created_at`),
  KEY `fk_accommodation_requests_current` (`current_record_id`),
  KEY `fk_accommodation_requests_bed` (`requested_bed_id`),
  KEY `fk_accommodation_requests_reviewer` (`reviewed_by`),
  CONSTRAINT `fk_accommodation_requests_bed` FOREIGN KEY (`requested_bed_id`) REFERENCES `dorm_beds` (`id`),
  CONSTRAINT `fk_accommodation_requests_current` FOREIGN KEY (`current_record_id`) REFERENCES `accommodation_records` (`id`),
  CONSTRAINT `fk_accommodation_requests_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_accommodation_requests_student` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_accommodation_requests_bed` CHECK (((`request_type` <> _utf8mb4'CHECK_OUT') or (`requested_bed_id` is null))),
  CONSTRAINT `ck_accommodation_requests_current` CHECK (((`request_type` <> _utf8mb4'CHECK_OUT') or (`current_record_id` is not null))),
  CONSTRAINT `ck_accommodation_requests_reviewed_at` CHECK (((`status` in (_utf8mb4'PENDING',_utf8mb4'CANCELLED')) or (`reviewed_at` is not null))),
  CONSTRAINT `ck_accommodation_requests_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'APPROVED',_utf8mb4'REJECTED',_utf8mb4'CANCELLED'))),
  CONSTRAINT `ck_accommodation_requests_type` CHECK ((`request_type` in (_utf8mb4'CHECK_IN',_utf8mb4'TRANSFER',_utf8mb4'CHECK_OUT')))
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `accommodation_requests`
--

LOCK TABLES `accommodation_requests` WRITE;
/*!40000 ALTER TABLE `accommodation_requests` DISABLE KEYS */;
INSERT INTO `accommodation_requests` VALUES (5,14,'TRANSFER',17,27,'103 目前只有我一个人住，晚上回来楼道很暗，心里不太踏实。同班同学吴欣然住在 102，那边还有空床，希望能调过去和她同住，方便一起上下课。','APPROVED',7,'2026-09-13 17:27:25.471',NULL,'2026-09-09 15:01:08.000','2026-09-13 17:27:25.471'),(6,18,'CHECK_IN',NULL,NULL,'本学期从软件学院转入计算机学院，原宿舍在丁家桥校区，已办完转专业手续，目前在九龙湖没有床位，申请安排入住 D1。','APPROVED',7,'2026-09-13 17:27:16.086',NULL,'2026-09-10 15:01:08.000','2026-09-13 17:27:16.086'),(7,19,'CHECK_OUT',19,NULL,'下学期获批到上海一家企业实习六个月，实习期间在企业附近租房，申请自 9 月底起退宿，实习结束后再申请入住。','REJECTED',7,'2026-09-13 17:27:07.402','不同意','2026-09-11 12:01:08.000','2026-09-13 17:27:07.402'),(8,1,'TRANSFER',11,1,'原住 102 室，室友作息差异较大，经常凌晨才休息，长期影响我早上上课。听说 101 室有空床，申请调至 101。','APPROVED',7,'2026-08-31 15:01:08.000','已与双方室友核实情况，同意调至 101 室 1 号床，请在本周内完成搬迁并到值班室登记。','2026-08-28 15:01:08.000','2026-09-11 15:01:08.645'),(9,1,'CHECK_OUT',11,NULL,'暑假两个月都不在校，想申请短期退宿，节省一部分住宿费。','REJECTED',7,'2026-06-28 15:01:08.000','住宿费按学年收取，不支持假期短期退宿。暑假不留校请到值班室做离校登记即可。','2026-06-25 15:01:08.000','2026-09-11 15:01:08.645'),(10,1,'TRANSFER',12,NULL,'1','APPROVED',7,'2026-09-13 17:26:54.824',NULL,'2026-09-13 17:21:13.482','2026-09-13 17:26:54.824'),(11,1,'TRANSFER',20,NULL,'室友作息不和','PENDING',NULL,NULL,NULL,'2026-09-13 19:04:29.571','2026-09-13 19:04:29.571');
/*!40000 ALTER TABLE `accommodation_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `account_cancellation_requests`
--

DROP TABLE IF EXISTS `account_cancellation_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account_cancellation_requests` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `reason` varchar(500) NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'PENDING',
  `reviewed_by` bigint unsigned DEFAULT NULL,
  `reviewed_at` datetime(3) DEFAULT NULL,
  `review_remark` varchar(500) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `pending_user_id` bigint unsigned GENERATED ALWAYS AS (if((`status` = _utf8mb4'PENDING'),`user_id`,NULL)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_cancellation_pending_user` (`pending_user_id`),
  KEY `idx_account_cancellation_user_status` (`user_id`,`status`,`created_at`),
  KEY `idx_account_cancellation_review` (`status`,`created_at`),
  KEY `fk_account_cancellation_reviewer` (`reviewed_by`),
  CONSTRAINT `fk_account_cancellation_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_account_cancellation_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_account_cancellation_reason` CHECK ((char_length(trim(`reason`)) > 0)),
  CONSTRAINT `ck_account_cancellation_review` CHECK ((((`status` in (_utf8mb4'PENDING',_utf8mb4'CANCELLED')) and (`reviewed_by` is null) and (`reviewed_at` is null) and (`review_remark` is null)) or ((`status` in (_utf8mb4'APPROVED',_utf8mb4'REJECTED')) and (`reviewed_by` is not null) and (`reviewed_at` is not null) and (`review_remark` is not null) and (char_length(trim(`review_remark`)) > 0)))),
  CONSTRAINT `ck_account_cancellation_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'APPROVED',_utf8mb4'REJECTED',_utf8mb4'CANCELLED')))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `account_cancellation_requests`
--

LOCK TABLES `account_cancellation_requests` WRITE;
/*!40000 ALTER TABLE `account_cancellation_requests` DISABLE KEYS */;
INSERT INTO `account_cancellation_requests` (`id`, `user_id`, `reason`, `status`, `reviewed_by`, `reviewed_at`, `review_remark`, `created_at`, `updated_at`) VALUES (1,1,'我不活了','PENDING',NULL,NULL,NULL,'2026-09-04 15:10:30.117','2026-09-04 15:10:30.117');
/*!40000 ALTER TABLE `account_cancellation_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `account_transactions`
--

DROP TABLE IF EXISTS `account_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account_transactions` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `account_id` bigint unsigned NOT NULL,
  `transaction_type` varchar(24) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `balance_before` decimal(12,2) NOT NULL,
  `balance_after` decimal(12,2) NOT NULL,
  `reference_type` varchar(32) DEFAULT NULL,
  `reference_id` bigint unsigned DEFAULT NULL,
  `idempotency_key` varchar(128) NOT NULL,
  `operator_id` bigint unsigned DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_transactions_idempotency` (`idempotency_key`),
  KEY `idx_account_transactions_account_time` (`account_id`,`created_at`),
  KEY `idx_account_transactions_reference` (`reference_type`,`reference_id`),
  KEY `fk_account_transactions_operator` (`operator_id`),
  CONSTRAINT `fk_account_transactions_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`),
  CONSTRAINT `fk_account_transactions_operator` FOREIGN KEY (`operator_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ck_account_transactions_amount` CHECK ((`amount` <> 0)),
  CONSTRAINT `ck_account_transactions_balances` CHECK (((`balance_before` >= 0) and (`balance_after` >= 0) and (`balance_after` = (`balance_before` + `amount`)))),
  CONSTRAINT `ck_account_transactions_type` CHECK ((`transaction_type` in (_utf8mb4'RECHARGE',_utf8mb4'PURCHASE',_utf8mb4'REFUND',_utf8mb4'ADJUSTMENT',_utf8mb4'DORM_BILL_PAYMENT')))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `account_transactions`
--

LOCK TABLES `account_transactions` WRITE;
/*!40000 ALTER TABLE `account_transactions` DISABLE KEYS */;
INSERT INTO `account_transactions` VALUES (1,1,'RECHARGE',100.00,0.00,100.00,'DEMO_SEED',1,'DEMO-ACCOUNT-RECHARGE-0001',9,'演示账户初始化余额','2026-09-04 13:46:23.377'),(2,1,'PURCHASE',-12.50,100.00,87.50,'STORE_ORDER',1,'DEMO-STORE-PAYMENT-0001',1,'演示订单支付','2026-09-04 13:46:23.380'),(3,1,'RECHARGE',12.50,87.50,100.00,'ACCOUNT',1,'desktop-recharge-1788508673667',1,NULL,'2026-09-04 15:57:53.693'),(4,1,'RECHARGE',100000000.00,100.00,100000100.00,'ACCOUNT',1,'desktop-recharge-1788509187889',1,NULL,'2026-09-04 16:06:44.057'),(5,1,'DORM_BILL_PAYMENT',-60.00,100000100.00,100000040.00,'UTILITY_ALLOCATION',1,'desktop-bill-pay-1',1,'宿舍水电分摊','2026-09-05 18:55:20.547'),(6,1,'DORM_BILL_PAYMENT',-47.02,100000040.00,99999992.98,'UTILITY_ALLOCATION',2,'desktop-bill-pay-2',1,'宿舍水电分摊','2026-09-13 17:23:54.759');
/*!40000 ALTER TABLE `account_transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `accounts`
--

DROP TABLE IF EXISTS `accounts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `accounts` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `balance` decimal(12,2) NOT NULL DEFAULT '0.00',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `version` int unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_accounts_user` (`user_id`),
  CONSTRAINT `fk_accounts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_accounts_balance` CHECK ((`balance` >= 0)),
  CONSTRAINT `ck_accounts_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'FROZEN',_utf8mb4'CLOSED')))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `accounts`
--

LOCK TABLES `accounts` WRITE;
/*!40000 ALTER TABLE `accounts` DISABLE KEYS */;
INSERT INTO `accounts` VALUES (1,1,99999992.98,'ACTIVE',4,'2026-09-04 13:46:23.363','2026-09-13 17:23:54.757');
/*!40000 ALTER TABLE `accounts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_answer_feedback`
--

DROP TABLE IF EXISTS `ai_answer_feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_answer_feedback` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `session_id` bigint unsigned NOT NULL,
  `request_id` varchar(80) NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `rating` varchar(12) NOT NULL,
  `category` varchar(40) DEFAULT NULL,
  `comment` varchar(500) DEFAULT NULL,
  `question_preview` varchar(240) DEFAULT NULL,
  `process_status` varchar(16) NOT NULL DEFAULT 'PENDING',
  `related_chunk_id` bigint unsigned DEFAULT NULL,
  `handled_by` bigint unsigned DEFAULT NULL,
  `handled_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_feedback_user_request` (`user_id`,`request_id`),
  KEY `idx_ai_feedback_rating_time` (`rating`,`created_at`),
  KEY `fk_ai_feedback_session` (`session_id`),
  KEY `idx_ai_feedback_process_time` (`process_status`,`created_at`),
  KEY `fk_ai_feedback_related_chunk` (`related_chunk_id`),
  KEY `fk_ai_feedback_handler` (`handled_by`),
  CONSTRAINT `fk_ai_feedback_handler` FOREIGN KEY (`handled_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_feedback_related_chunk` FOREIGN KEY (`related_chunk_id`) REFERENCES `ai_knowledge_chunks` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_feedback_session` FOREIGN KEY (`session_id`) REFERENCES `ai_chat_sessions` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ai_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ck_ai_feedback_process_status` CHECK ((`process_status` in (_utf8mb4'PENDING',_utf8mb4'RESOLVED',_utf8mb4'IGNORED'))),
  CONSTRAINT `ck_ai_feedback_rating` CHECK ((`rating` in (_utf8mb4'HELPFUL',_utf8mb4'UNHELPFUL')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_answer_feedback`
--

LOCK TABLES `ai_answer_feedback` WRITE;
/*!40000 ALTER TABLE `ai_answer_feedback` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_answer_feedback` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_chat_messages`
--

DROP TABLE IF EXISTS `ai_chat_messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_messages` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `session_id` bigint unsigned NOT NULL,
  `request_id` varchar(80) DEFAULT NULL,
  `sequence_no` int unsigned NOT NULL,
  `sender_type` varchar(12) NOT NULL,
  `content` longtext NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'COMPLETED',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_chat_messages_session_sequence` (`session_id`,`sequence_no`),
  KEY `idx_ai_chat_messages_request` (`request_id`),
  CONSTRAINT `fk_ai_chat_messages_session` FOREIGN KEY (`session_id`) REFERENCES `ai_chat_sessions` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ck_ai_chat_messages_sender` CHECK ((`sender_type` in (_utf8mb4'USER',_utf8mb4'ASSISTANT',_utf8mb4'SYSTEM',_utf8mb4'TOOL'))),
  CONSTRAINT `ck_ai_chat_messages_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'STREAMING',_utf8mb4'COMPLETED',_utf8mb4'CANCELLED',_utf8mb4'FAILED')))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_chat_messages`
--

LOCK TABLES `ai_chat_messages` WRITE;
/*!40000 ALTER TABLE `ai_chat_messages` DISABLE KEYS */;
INSERT INTO `ai_chat_messages` VALUES (1,1,'demo-ai-request-0001',1,'USER','我如何查看本学期课程？','COMPLETED','2026-09-04 13:46:23.416'),(2,1,'demo-ai-request-0001',2,'ASSISTANT','已通过教务服务查询本人课表；AI 助手会沿用当前登录会话和业务权限返回结果。','COMPLETED','2026-09-04 13:46:23.416');
/*!40000 ALTER TABLE `ai_chat_messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_chat_sessions`
--

DROP TABLE IF EXISTS `ai_chat_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_sessions` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `title` varchar(200) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `model_name` varchar(120) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_ai_chat_sessions_user_status` (`user_id`,`status`,`updated_at`),
  CONSTRAINT `fk_ai_chat_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_ai_chat_sessions_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'ARCHIVED')))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_chat_sessions`
--

LOCK TABLES `ai_chat_sessions` WRITE;
/*!40000 ALTER TABLE `ai_chat_sessions` DISABLE KEYS */;
INSERT INTO `ai_chat_sessions` VALUES (1,1,'演示校园助手会话','ACTIVE',NULL,'2026-09-04 13:46:23.412','2026-09-04 13:46:23.412'),(2,1,NULL,'ACTIVE','gpt-4.1-mini（API 未配置）','2026-09-04 14:25:43.873','2026-09-04 14:25:43.873');
/*!40000 ALTER TABLE `ai_chat_sessions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_knowledge_chunks`
--

DROP TABLE IF EXISTS `ai_knowledge_chunks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_knowledge_chunks` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `source_type` varchar(40) NOT NULL,
  `source_ref_id` bigint unsigned DEFAULT NULL,
  `title` varchar(240) DEFAULT NULL,
  `content` longtext NOT NULL,
  `embedding_json` json DEFAULT NULL COMMENT 'Reserved for a later RAG implementation; nullable in V1',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `updated_by` bigint unsigned DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_ai_knowledge_chunks_source` (`source_type`,`source_ref_id`,`status`),
  KEY `idx_ai_knowledge_chunks_status_time` (`status`,`updated_at`),
  KEY `fk_ai_knowledge_chunks_updater` (`updated_by`),
  FULLTEXT KEY `ft_ai_knowledge_chunks_content` (`content`),
  CONSTRAINT `fk_ai_knowledge_chunks_updater` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ck_ai_knowledge_chunks_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'INACTIVE')))
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_knowledge_chunks`
--

LOCK TABLES `ai_knowledge_chunks` WRITE;
/*!40000 ALTER TABLE `ai_knowledge_chunks` DISABLE KEYS */;
INSERT INTO `ai_knowledge_chunks` VALUES (1,'SYSTEM_GUIDE',NULL,'课程查询说明','学生可以在虚拟教务模块查看已选课程、上课时间和教室。',NULL,'ACTIVE',8,'2026-09-04 13:46:23.420','2026-09-04 13:46:23.420'),(2,'SYSTEM_GUIDE',NULL,'学生选课并加入课表操作步骤','适用角色：学生。步骤：1. 登录后进入“教务管理”。2. 打开“选课中心”，在“课程与课表”区域按课程编号或名称搜索。3. 只选择状态为“已发布”的课程，并查看课程名称、学分、容量和已有上课时段。4. 选中目标课程后点击“选课”。5. 系统会校验课程状态、剩余容量、重复选课和课表时间冲突；校验失败时按页面提示更换课程或联系教务老师。6. 页面出现“选课成功”后，切换到“我的课表”查看课程、上课时间和教室。学生不能在课表中手工创建课程或时段；课表内容来自已成功选修的课程。',NULL,'ACTIVE',8,'2026-09-04 13:46:23.424','2026-09-04 13:46:23.424'),(3,'SYSTEM_GUIDE',NULL,'教务老师添加课程上课时段操作步骤','适用角色：教务老师。步骤：1. 登录后进入“教务管理”，打开“课程与排课”。2. 在课程列表中选择已有课程；没有课程时先点击“新建课程”，填写课程资料并保存。3. 在下方“排课维护”区域点击“新建时段”。4. 依次填写星期、开始节次、结束节次、可选的起止日期和教室编号。5. 点击“保存时段”。6. 系统会校验节次范围、日期范围、教师课表冲突和教室占用冲突；失败时根据提示调整。7. 页面显示“课程时段已保存”且时段出现在列表中即完成。修改时先选中已有时段再保存；删除时必须再次确认。',NULL,'ACTIVE',8,'2026-09-04 13:46:23.425','2026-09-04 13:46:23.425'),(4,'SYSTEM_GUIDE',NULL,'图书借阅与归还操作步骤','适用角色：具有图书借阅权限的用户。借书步骤：1. 进入“虚拟图书馆”的图书列表。2. 按书名、作者或关键词搜索。3. 选择有可借库存的图书并提交借阅。4. 确认借阅操作后等待系统校验库存和重复借阅。5. 成功后在“我的借阅”查看记录。归还步骤：1. 打开“我的借阅”。2. 选中状态仍为借阅中的记录。3. 点击归还并确认。4. 页面显示归还成功且记录状态更新即完成。AI 代办借书或归还同样必须由当前用户确认，并继续使用图书馆模块的库存和权限校验。',NULL,'ACTIVE',8,'2026-09-04 13:46:23.426','2026-09-04 13:46:23.426'),(5,'SYSTEM_GUIDE',NULL,'校园商店购物操作步骤','适用角色：具有商店购买权限的用户。步骤：1. 进入“校园商店”并搜索商品。2. 查看商品状态、单价和库存。3. 将商品加入购物车并调整数量。4. 检查购物车后创建订单。5. 在订单页面确认金额并支付。6. 系统在支付时重新校验商品状态、库存、账户余额和订单状态，成功后生成账户流水。AI 可以查询商品、购物车、本人订单和余额；加入购物车或创建订单属于写操作，必须确认。',NULL,'ACTIVE',8,'2026-09-04 13:46:23.427','2026-09-04 13:46:23.427'),(6,'SYSTEM_GUIDE',NULL,'宿舍报修与水电查询操作步骤','住宿信息与水电查询：进入“宿舍管理”，学生可以查看本人当前住宿信息和水电分摊，不能查看其他学生的数据。报修步骤：1. 打开本人报修页面。2. 新建报修并填写地点、问题类型和描述。3. 提交后在报修列表查看处理状态。4. 维修完成后可按页面提供的入口评价。水电缴费会修改账户和账单状态，执行前必须确认，并由宿舍模块再次校验账单是否可支付、是否重复支付。',NULL,'ACTIVE',8,'2026-09-04 13:46:23.428','2026-09-04 13:46:23.428'),(7,'SYSTEM_GUIDE',NULL,'账号、学籍与成绩查询操作步骤','查看账号资料：登录后进入个人中心或身份信息页面，系统只返回当前登录人的公开资料，不返回密码、密码哈希或会话令牌。查看学籍：学生进入“学籍信息”查看本人学号、院系、专业和状态。查看成绩：学生在学籍模块打开“我的成绩”，可按课程分页查看；教师或管理员的成绩登记权限不能与学生本人查询权限混用。AI 查询这些信息时使用当前会话身份，不接受用户在问题中伪造的用户编号。',NULL,'ACTIVE',8,'2026-09-04 13:46:23.430','2026-09-04 13:46:23.430'),(8,'SYSTEM_RULE',NULL,'选课业务规则','课程必须处于已发布且可选状态。系统拒绝重复选修同一课程、超过课程容量的选课以及与已选课程上课时段冲突的选课。退课会修改选课记录，必须由当前学生确认；已完成或不允许退选的记录不能强制修改。最终结果以教务模块返回的信息为准。',NULL,'ACTIVE',8,'2026-09-04 13:46:23.431','2026-09-04 13:46:23.431'),(9,'KNOWLEDGE_SCOPE',NULL,'校纪校规知识回答范围','AI 助手只能依据知识库中由知识管理员录入、标注来源并启用的校纪校规条款回答正式规定。若检索结果没有对应条款，助手必须明确说明“当前知识库未收录该规定”，建议咨询学校主管部门或由知识管理员补充正式文件，不得依据常识编造处分标准、申请期限或管理办法。管理员录入时应在标题中写明制度名称和条款，在正文中保留适用对象、具体要求、生效范围和官方来源。',NULL,'ACTIVE',8,'2026-09-04 13:46:23.432','2026-09-04 13:46:23.432'),(10,'SYSTEM_RULE',NULL,'AI业务代办安全规则','AI 只通过系统已有命令路由调用业务模块，不直接修改课程、图书、商品、宿舍、账号或学籍数据。查询按当前登录角色鉴权。选课、退课、借书、归还、报名、取消报名、购物车修改和创建订单等写操作先生成待确认卡片，只有原用户在有效期内确认后才执行；确认时原业务模块仍会再次校验权限、状态、容量、库存、余额和并发冲突。',NULL,'ACTIVE',8,'2026-09-04 13:46:23.433','2026-09-04 13:46:23.433'),(11,'SYSTEM_GUIDE',NULL,'校园助手三种模式说明','问答模式用于查询校园实时信息和回答系统指南，包括课表、成绩、学籍、馆藏、本人借阅、商品、订单、竞赛、住宿、报修和公告；只读，不修改数据。聊天模式连接通用大模型，可连续讨论校园内外的一般问题，但不执行校园业务。代办模式用于选课、退课、借书、还书、竞赛报名或取消报名、加入购物车和创建订单；所有写操作都会先展示确认信息，确认后仍由原业务模块校验权限、状态、容量、库存和冲突。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.191','2026-09-07 14:17:15.191'),(12,'SYSTEM_GUIDE',NULL,'校园实时数据查询范围','校园助手不会把课程、图书、商品、订单、竞赛或个人资料复制到知识库。问答模式会使用当前登录身份调用原业务模块，并读取数据库中的最新状态。可询问：我的学院、专业、班级和学号；我的课表和成绩；图书馆有什么书、某书是否可借、我借了什么书；商店有什么商品、我的购物车和订单；目前有哪些竞赛、我的 SRTP；我的宿舍、水电、报修、请假和教室申请。结果以原业务模块实时返回为准。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.220','2026-09-07 14:17:15.220'),(13,'SYSTEM_GUIDE',NULL,'学籍资料查询说明','学生可在问答模式直接询问“我的学院”“我的专业”“我的班级”“我的学号”“我的入学年份”等。助手通过学籍服务实时返回姓名、账号、学号、学院、专业、班级、培养层次、入学年份、预计毕业年份和学籍状态等本人可见字段。若资料缺失，应联系学籍管理人员维护，助手不会根据其他信息猜测。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.227','2026-09-07 14:17:15.227'),(14,'SYSTEM_GUIDE',NULL,'图书馆馆藏与借还书指南','问答模式可用“图书馆有什么书”“查一下 Java 相关书籍”“我借了哪些书”等自然表达查询实时馆藏和本人借阅。馆藏结果包含图书编号、书名、作者、分类、位置、总册数、可借册数和状态。代办模式可按编号或书名借书，也可先说“帮我还书”列出本人借阅记录，再按借阅记录编号或书名归还。借书和还书必须确认，最终由图书馆服务校验库存、借阅状态和权限。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.237','2026-09-07 14:17:15.237'),(15,'SYSTEM_GUIDE',NULL,'校园商店查询与代办指南','问答模式中“商品”“查看商品”“查找商品”“校园商店有什么”“我的订单”“购买记录”等说法都可查询实时商品或本人订单，不要求出现购物车字样。代办模式支持加入、修改和移除购物车商品，创建订单、余额支付订单和领取优惠券；所有写操作需要确认，并由商店服务重新校验库存、价格、余额、资格与订单状态。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.244','2026-09-07 14:17:15.482'),(16,'SYSTEM_GUIDE',NULL,'竞赛查询和报名指南','问答模式可查询系统当前显示的竞赛及其编号、名称、简介、开始与结束时间、报名截止时间、容量、已报名人数和状态。代办模式可直接说竞赛名称，例如“帮我报名数学建模竞赛”；助手会用名称检索实时竞赛并解析编号，再展示确认信息。若名称无法唯一匹配，应先查看竞赛列表并补充更完整名称。报名和取消报名最终由竞赛服务检查登录身份、报名窗口、容量和重复报名。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.251','2026-09-07 14:17:15.251'),(17,'SYSTEM_GUIDE',NULL,'课程查询与选退课指南','问答模式可查询本人课表、成绩和系统中可见课程。代办模式可按课程数据库编号、课程代码或课程名称发起选课和退课；名称会先通过实时课程列表解析。确认后，教务服务仍会校验课程是否发布、容量是否充足、是否重复选课、是否存在上课时间冲突以及当前用户是否具有选课权限。成功后可在本人课表或已选课程中核对。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.257','2026-09-07 14:17:15.257'),(18,'SYSTEM_GUIDE',NULL,'宿舍服务查询指南','问答模式可查询本人住宿信息、水电分摊、报修记录、请假记录和宿舍公告。代办模式可提交报修、提交或取消请假、支付本人水电分摊；助手会先澄清必要参数并请求确认，最终由宿舍模块校验身份、状态和余额。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.264','2026-09-07 14:17:15.467'),(19,'SYSTEM_GUIDE',NULL,'连续对话和指代说明','同一会话会保留最近对话上下文，可继续追问“它什么时候截止”“那我报名这个”“第三个呢”等。为避免误操作，涉及写入时助手会结合上下文解析对象，但仍展示对象名称和编号供用户确认。新建会话会开始独立上下文；清空会话会归档原历史。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.270','2026-09-07 14:17:15.270'),(20,'SYSTEM_RULE',NULL,'校园助手权限、确认与隐私边界','助手只能调用登记在白名单中的既有业务命令，不能直接修改其他模块数据库。当前登录会话是身份唯一来源，用户话语和模型输出都不能覆盖用户编号或角色。所有写操作先生成五分钟有效的确认请求，确认后由原业务模块再次鉴权并执行事务。助手不得输出密码、会话令牌或密钥，也不得把工具执行失败描述为成功。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.277','2026-09-07 14:17:15.277'),(21,'SYSTEM_GUIDE',NULL,'校园助手常见问题排查','按 Enter 发送消息，Shift+Enter 换行。若聊天模式提示模型未配置，需要在服务端配置兼容 Responses API 的密钥和地址；问答模式的实时校园查询以及确定性代办仍可使用。若知识问答提示未收录，应由知识管理员补充经核验的正式材料；若实时查询无结果，应检查当前账号权限、筛选词和业务数据状态。写操作未确认、确认超时或原模块校验失败时都不会修改数据。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.287','2026-09-07 14:17:15.287'),(22,'POLICY',NULL,'学生公寓管理：适用范围与管理原则','《东南大学学生公寓管理办法》（2025年6月修订版）适用于学校统一安排住宿的学生。学生公寓实行学校统筹、部门协同、学院参与、学生自我教育与自我管理相结合；住宿人应遵守国家法律法规、学校规章和公寓管理要求。用户提供的扫描件文件名标注“2026年”，知识条目按其中现行内容整理，遇版本差异应以学校最新正式发布文本为准。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.295','2026-09-07 14:17:15.295'),(23,'POLICY',NULL,'学生公寓管理：入住调宿与退宿','住宿由学校统一安排。学生应按分配的楼栋、房间和床位入住，不得私自调换、转让、出租床位或留宿他人；确需调宿应按学校流程申请并获批准。离校、休学、退学、毕业或其他需要退宿的情形，应在规定期限内办理退宿、交还钥匙、结清费用并带走个人物品。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.302','2026-09-07 14:17:15.302'),(24,'POLICY',NULL,'学生公寓管理：门禁访客与夜间秩序','住宿人应遵守门禁、会客和查验制度，维护公共秩序。访客应按要求登记并在规定时间、区域内活动，不得擅自留宿校外人员。晚归或夜不归宿应按要求说明或办理请假；不得以喧哗、起哄、酗酒等方式影响他人学习休息。具体开放和门禁时间以所在公寓最新通知为准。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.310','2026-09-07 14:17:15.310'),(25,'POLICY',NULL,'学生公寓管理：消防与用电安全','学生应爱护消防设施，保持消防通道和安全出口畅通；不得挪用、遮挡或损坏消防器材，不得在室内使用明火、焚烧物品、吸烟或存放易燃易爆及其他危险物品。发现火情或安全隐患应立即报告并服从应急处置，不得虚报火警。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.318','2026-09-07 14:17:15.318'),(26,'POLICY',NULL,'学生公寓管理：禁用电器与电池充电','公寓内不得私拉乱接电线或违规改变供电设施。扫描件列举的禁止或限制情形包括：电动自行车电池、平衡车等在室内充电；户外便携储能设备和大容量蓄电池；无国家强制性产品认证标志的电器；无自动断电保护装置的相关设备；额定功率达到或超过800瓦的其他大功率电器；以及其他存在用电安全隐患的设备。因疾病确需使用特殊医疗电器，应凭疾病证明履行申请、审批和登记。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.325','2026-09-07 14:17:15.325'),(27,'POLICY',NULL,'学生公寓管理：卫生设施与公共财物','住宿人应维护室内外清洁，按规定分类投放垃圾，配合卫生检查和传染病防控；不得向楼外抛物、倾倒污水或堆放妨碍通行的物品。家具、门窗、水电、网络和公共设施不得擅自拆改、搬移或损坏；发现故障可通过宿舍报修流程登记，因人为原因造成损失的按规定承担责任。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.334','2026-09-07 14:17:15.334'),(28,'POLICY',NULL,'学生公寓管理：禁止经营饲养与危险行为','未经批准不得在学生公寓从事经营、推销、广告张贴、收费服务等活动；不得饲养影响卫生、安全或他人生活的动物；不得赌博、酗酒滋事、打架斗殴、传播违法信息，或携带管制器具和危险物品。学校可依据情节采取教育整改、依规处理并移交相关部门。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.341','2026-09-07 14:17:15.341'),(29,'POLICY',NULL,'学生违纪处分：原则与处分种类','学生违纪处分坚持教育与惩戒相结合，做到事实清楚、证据充分、依据明确、定性准确、程序正当、处分适当。处分一般包括警告、严重警告、记过、留校察看和开除学籍；具体处分应结合行为性质、情节、后果、本人态度及从轻、从重情形依法依规确定。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.355','2026-09-07 14:17:15.355'),(30,'POLICY',NULL,'学生违纪处分：从轻减轻与从重情形','主动承认错误、配合调查、及时消除影响或赔偿损失等情形，可按条例综合认定是否从轻或减轻；拒不承认、妨碍调查、串供伪造证据、打击报复、屡次违纪或造成严重后果等，可能从重处理。是否适用及处分幅度由有权部门依事实、证据和正式条款决定，助手不能代替处分认定。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.365','2026-09-07 14:17:15.365'),(31,'POLICY',NULL,'学生违纪处分：公共秩序与人身财产','扰乱学校教育教学、生活秩序或公共场所秩序，打架斗殴、寻衅滋事、侮辱诽谤、侵害他人人身权利，盗窃、诈骗、侵占、故意损坏公私财物等行为，可依据情节和后果给予相应处分；涉嫌违法犯罪的还可能移送有关机关。具体认定应查阅正式条例对应条款。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.372','2026-09-07 14:17:15.372'),(32,'POLICY',NULL,'学生违纪处分：网络与信息行为','利用网络制作、复制、发布、传播违法有害信息，侵犯他人隐私、名誉或知识产权，冒用身份、攻击系统、窃取或篡改数据，以及其他危害网络与信息安全的行为，可按性质、影响和后果处理。正常批评建议与违法侵权行为应依据事实和正式规则区分。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.382','2026-09-07 14:17:15.382'),(33,'POLICY',NULL,'学生违纪处分：宿舍消防与危险物品','在宿舍违反消防安全规定、违规使用电器或明火、私拉电线、给电动车电池等危险设备充电、堵塞消防通道，或存放使用管制器具、易燃易爆和其他危险物品，可能依据情节、整改情况及是否造成事故给予处分。造成损害的还应承担相应责任。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.391','2026-09-07 14:17:15.391'),(34,'POLICY',NULL,'学生违纪处分：学习考试与学术诚信','无故旷课、扰乱课堂或考试秩序、考试作弊，以及抄袭、剽窃、伪造研究数据或其他学术不端行为，按照行为性质和情节处理。对考试与学术行为的判断应使用课程、考试和学术规范的正式证据，不应仅凭传闻；涉及学位和成果处理时还应适用相关专门规定。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.398','2026-09-07 14:17:15.398'),(35,'POLICY',NULL,'学生违纪处分：调查告知决定与申诉','处分前应进行调查取证，并告知学生拟处分的事实、理由和依据，听取其陈述和申辩；处分决定应按程序作出并送达。学生对处理或处分决定有异议，可在正式规定的期限内向指定机构提出书面申诉。具体期限、材料和受理机构以决定书及学校最新学生申诉办法为准。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.407','2026-09-07 14:17:15.407'),(36,'SYSTEM_GUIDE',NULL,'聊天模式附件使用指南','聊天模式输入框左侧“+”可一次选择最多3个附件。图片支持 PNG、JPEG、GIF、WebP，单张不超过2MB；文本和常见代码文件不超过256KB。图片会使用视觉模型，文本内容会连同文件名发给模型。问答和代办模式不接收附件；PDF、Word、压缩包等当前未直接解析，应转为图片或纯文本后上传。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.413','2026-09-07 14:17:15.413'),(37,'SYSTEM_GUIDE',NULL,'自习室预约操作指南','问答模式可查询可用自习室和本人预约。代办模式可说“预约自习室”，并提供自习室编号、开始和结束时间；时间格式示例为2026-09-06T14:00。参数不全时助手会逐项追问，确认后由图书馆模块检查开放状态和时段冲突。取消预约需要预约记录编号。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.421','2026-09-07 14:17:15.421'),(38,'SYSTEM_GUIDE',NULL,'校园商店完整代办指南','代办模式支持按商品名称加入购物车、修改数量、移除商品、从购物车创建订单、按订单编号余额支付，以及按代码领取优惠券。每个写操作均先确认，价格、库存、订单状态、余额、优惠券资格和幂等性仍由商店服务实时校验。问“我的订单”“买过什么”会直接查询订单，不要求出现“购物车”。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.427','2026-09-07 14:17:15.427'),(39,'SYSTEM_GUIDE',NULL,'宿舍报修请假与水电代办指南','代办模式支持提交报修、提交或取消请假、支付水电分摊。报修需房间编号、故障类别和描述，可选优先级；请假需类型、起止时间和原因；缴费需待缴分摊编号。助手缺少字段时会先澄清，确认后宿舍模块按本人身份、记录状态和余额执行。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.435','2026-09-07 14:17:15.435'),(40,'SYSTEM_GUIDE',NULL,'教室查询申请与取消指南','问答模式可查询可申请教室和本人申请记录。代办模式提交申请时需教室编号、用途、开始时间和结束时间；取消需本人申请编号。助手先澄清缺少参数并展示确认，最终由校园服务校验教室状态、时段冲突、申请权限和记录状态。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.441','2026-09-07 14:17:15.441'),(41,'SYSTEM_GUIDE',NULL,'快捷问题与自然语言理解','聊天框上方按当前模式显示可横向滚动的常用示例，点击会立即发送。固定规则覆盖高频表达，大模型意图路由可把“买过什么”“找点能买的东西”“参加数学建模”等近义说法映射到实时工具。写操作只在意图明确时选择工具；缺少名称、编号、时间、数量、原因等参数时进入多轮澄清，不猜测业务数据。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.448','2026-09-07 14:17:15.448'),(42,'SYSTEM_GUIDE',NULL,'校园助手结果阅读说明','实时工具结果以“实时数据”标题开头，并按记录分组展示关键字段、状态和编号；编号可在后续代办中引用。知识问答应区分正式规则摘要、系统操作指南与实时个人数据。查询无结果不等于系统故障，可能是当前账号没有对应记录、筛选条件过窄或业务记录尚未生成。',NULL,'ACTIVE',NULL,'2026-09-07 14:17:15.458','2026-09-07 14:17:15.458');
/*!40000 ALTER TABLE `ai_knowledge_chunks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_knowledge_versions`
--

DROP TABLE IF EXISTS `ai_knowledge_versions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_knowledge_versions` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `chunk_id` bigint unsigned NOT NULL,
  `version_no` int unsigned NOT NULL,
  `source_type` varchar(40) NOT NULL,
  `title` varchar(240) DEFAULT NULL,
  `content` longtext NOT NULL,
  `status` varchar(20) NOT NULL,
  `created_by` bigint unsigned DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_knowledge_version_no` (`chunk_id`,`version_no`),
  KEY `idx_ai_knowledge_versions_chunk_time` (`chunk_id`,`created_at`),
  KEY `fk_ai_knowledge_versions_user` (`created_by`),
  CONSTRAINT `fk_ai_knowledge_versions_chunk` FOREIGN KEY (`chunk_id`) REFERENCES `ai_knowledge_chunks` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ai_knowledge_versions_user` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_knowledge_versions`
--

LOCK TABLES `ai_knowledge_versions` WRITE;
/*!40000 ALTER TABLE `ai_knowledge_versions` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_knowledge_versions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_tool_call_logs`
--

DROP TABLE IF EXISTS `ai_tool_call_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_tool_call_logs` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `session_id` bigint unsigned DEFAULT NULL,
  `message_id` bigint unsigned DEFAULT NULL,
  `request_id` varchar(80) NOT NULL,
  `tool_name` varchar(120) NOT NULL,
  `action_type` varchar(12) NOT NULL DEFAULT 'READ',
  `arguments_json` json DEFAULT NULL,
  `result_summary` text,
  `status` varchar(24) NOT NULL DEFAULT 'REQUESTED',
  `requested_by` bigint unsigned NOT NULL,
  `confirmed_by` bigint unsigned DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `completed_at` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ai_tool_call_logs_request` (`request_id`),
  KEY `idx_ai_tool_call_logs_user_time` (`requested_by`,`created_at`),
  KEY `idx_ai_tool_call_logs_status` (`status`,`created_at`),
  KEY `fk_ai_tool_call_logs_session` (`session_id`),
  KEY `fk_ai_tool_call_logs_message` (`message_id`),
  KEY `fk_ai_tool_call_logs_confirmer` (`confirmed_by`),
  CONSTRAINT `fk_ai_tool_call_logs_confirmer` FOREIGN KEY (`confirmed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_tool_call_logs_message` FOREIGN KEY (`message_id`) REFERENCES `ai_chat_messages` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_tool_call_logs_requester` FOREIGN KEY (`requested_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_ai_tool_call_logs_session` FOREIGN KEY (`session_id`) REFERENCES `ai_chat_sessions` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ck_ai_tool_call_logs_action` CHECK ((`action_type` in (_utf8mb4'READ',_utf8mb4'WRITE'))),
  CONSTRAINT `ck_ai_tool_call_logs_completed_at` CHECK (((`status` in (_utf8mb4'REQUESTED',_utf8mb4'CONFIRM_REQUIRED',_utf8mb4'CONFIRMED')) or (`completed_at` is not null))),
  CONSTRAINT `ck_ai_tool_call_logs_status` CHECK ((`status` in (_utf8mb4'REQUESTED',_utf8mb4'CONFIRM_REQUIRED',_utf8mb4'CONFIRMED',_utf8mb4'CANCELLED',_utf8mb4'SUCCEEDED',_utf8mb4'FAILED')))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_tool_call_logs`
--

LOCK TABLES `ai_tool_call_logs` WRITE;
/*!40000 ALTER TABLE `ai_tool_call_logs` DISABLE KEYS */;
INSERT INTO `ai_tool_call_logs` VALUES (1,1,NULL,'demo-ai-request-0001','academic.schedule.read','READ','{}','已通过教务服务接口查询本人课表。','SUCCEEDED',1,NULL,'2026-09-04 13:46:23.434','2026-08-29 10:20:00.000');
/*!40000 ALTER TABLE `ai_tool_call_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `announcements`
--

DROP TABLE IF EXISTS `announcements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcements` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `module_code` varchar(20) NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` longtext NOT NULL,
  `visible_scope` varchar(16) NOT NULL DEFAULT 'ALL',
  `target_role_id` bigint unsigned DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `publish_at` datetime(3) DEFAULT NULL,
  `expire_at` datetime(3) DEFAULT NULL,
  `publisher_id` bigint unsigned NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_announcements_module_status_time` (`module_code`,`status`,`publish_at`,`expire_at`),
  KEY `idx_announcements_target_role` (`target_role_id`,`status`),
  KEY `fk_announcements_publisher` (`publisher_id`),
  CONSTRAINT `fk_announcements_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_announcements_target_role` FOREIGN KEY (`target_role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `ck_announcements_module` CHECK ((`module_code` in (_utf8mb4'SYSTEM',_utf8mb4'ACADEMIC',_utf8mb4'LIBRARY',_utf8mb4'DORM'))),
  CONSTRAINT `ck_announcements_scope` CHECK ((`visible_scope` in (_utf8mb4'ALL',_utf8mb4'ROLE'))),
  CONSTRAINT `ck_announcements_scope_role` CHECK (((`visible_scope` <> _utf8mb4'ROLE') or (`target_role_id` is not null))),
  CONSTRAINT `ck_announcements_status` CHECK ((`status` in (_utf8mb4'DRAFT',_utf8mb4'SCHEDULED',_utf8mb4'PUBLISHED',_utf8mb4'EXPIRED',_utf8mb4'REVOKED'))),
  CONSTRAINT `ck_announcements_time` CHECK (((`expire_at` is null) or (`publish_at` is null) or (`expire_at` > `publish_at`)))
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `announcements`
--

LOCK TABLES `announcements` WRITE;
/*!40000 ALTER TABLE `announcements` DISABLE KEYS */;
INSERT INTO `announcements` VALUES (1,'ACADEMIC','虚拟校园系统演示公告','本公告用于演示公告查询、发布状态和角色可见范围。','ALL',NULL,'PUBLISHED','2026-08-01 09:00:00.000',NULL,4,'2026-09-04 13:46:23.330','2026-09-04 13:46:23.330'),(6,'DORM','9 月 15 日（周二）下午全楼消防疏散演练','为提高同学们的消防安全意识，桃园片区将于 9 月 15 日 15:00 组织消防疏散演练。届时楼内将拉响警报，请全体在寝同学听从楼层安全员指挥，沿疏散指示标志有序撤离至楼下空地集合。演练期间请勿使用电梯，行动不便的同学请提前告知值班室。','ALL',NULL,'PUBLISHED','2026-09-10 15:01:08.000','2026-09-21 15:01:08.000',7,'2026-09-11 15:01:08.855','2026-09-11 15:01:08.855'),(7,'DORM','D1 热水系统检修通知：9 月 13 日 14:00–17:00 暂停供应热水','因 D1 楼顶热水机组例行检修，9 月 13 日（周日）14:00 至 17:00 全楼暂停供应热水，冷水不受影响。请同学们合理安排洗浴时间，给大家带来不便敬请谅解。','ALL',NULL,'PUBLISHED','2026-09-09 15:01:08.000','2026-09-14 15:01:08.000',7,'2026-09-11 15:01:08.863','2026-09-11 15:01:08.863'),(8,'DORM','101 室卫生整改通知','9 月 9 日周检中，101 室卫生评分 64 分，未达到 70 分整改线。主要问题：地面碎屑未清扫、书桌堆放杂物、卫生间水渍明显。请于 9 月 12 日复查前完成整改，复查通过后恢复本月评优资格。','ALL',NULL,'PUBLISHED','2026-09-09 15:01:08.000','2026-09-16 15:01:08.000',7,'2026-09-11 15:01:08.870','2026-09-11 15:01:08.870'),(9,'DORM','8 月水电费账单已出，请于 9 月 15 日前缴清','8 月份各房间水电费账单已生成并按在住人数分摊到个人，请同学们在「水电账单」页面查看本人应缴金额，并于 9 月 15 日前通过校园卡余额完成缴纳。逾期未缴的房间将暂停用电额度充值。','ALL',NULL,'PUBLISHED','2026-09-03 15:01:08.000','2026-09-16 15:01:08.000',7,'2026-09-11 15:01:08.877','2026-09-11 15:01:08.877'),(10,'DORM','暑期留校住宿登记','暑假期间需留校住宿的同学，请于 6 月 25 日前在本页面提交留校登记，留校期间统一调整至 D2 集中住宿。','ALL',NULL,'EXPIRED','2026-06-23 15:01:08.000','2026-09-10 15:01:08.000',7,'2026-09-11 15:01:08.886','2026-09-12 00:32:48.070'),(11,'DORM','台风预警','注意防范！','ALL',NULL,'PUBLISHED',NULL,NULL,7,'2026-09-13 17:31:42.163','2026-09-13 17:31:42.163');
/*!40000 ALTER TABLE `announcements` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `audit_logs`
--

DROP TABLE IF EXISTS `audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_logs` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `actor_user_id` bigint unsigned DEFAULT NULL,
  `actor_role_id` bigint unsigned DEFAULT NULL COMMENT 'Actual role used for this operation',
  `action` varchar(100) NOT NULL,
  `resource_type` varchar(80) DEFAULT NULL,
  `resource_id` bigint unsigned DEFAULT NULL,
  `outcome` varchar(20) NOT NULL DEFAULT 'SUCCESS',
  `detail_json` json DEFAULT NULL,
  `occurred_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_audit_logs_actor_time` (`actor_user_id`,`occurred_at`),
  KEY `idx_audit_logs_resource_time` (`resource_type`,`resource_id`,`occurred_at`),
  KEY `fk_audit_logs_actor_role` (`actor_role_id`),
  CONSTRAINT `fk_audit_logs_actor_role` FOREIGN KEY (`actor_role_id`) REFERENCES `roles` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_audit_logs_actor_user` FOREIGN KEY (`actor_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ck_audit_logs_outcome` CHECK ((`outcome` in (_utf8mb4'SUCCESS',_utf8mb4'FAILURE')))
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_logs`
--

LOCK TABLES `audit_logs` WRITE;
/*!40000 ALTER TABLE `audit_logs` DISABLE KEYS */;
INSERT INTO `audit_logs` VALUES (1,1,1,'PROFILE_UPDATE','USER',1,'SUCCESS',NULL,'2026-09-04 15:02:38.652'),(2,1,1,'ACCOUNT_CANCELLATION_SUBMIT','ACCOUNT_CANCELLATION',1,'SUCCESS',NULL,'2026-09-04 15:10:30.122'),(3,1,1,'PROFILE_UPDATE','USER',1,'SUCCESS',NULL,'2026-09-04 16:10:14.275'),(4,1,1,'PROFILE_UPDATE','USER',1,'SUCCESS',NULL,'2026-09-04 17:35:24.477');
/*!40000 ALTER TABLE `audit_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `books`
--

DROP TABLE IF EXISTS `books`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `books` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `isbn` varchar(32) DEFAULT NULL,
  `title` varchar(240) NOT NULL,
  `author` varchar(160) DEFAULT NULL,
  `publisher` varchar(160) DEFAULT NULL,
  `category` varchar(80) DEFAULT NULL,
  `total_copies` int unsigned NOT NULL DEFAULT '0',
  `available_copies` int unsigned NOT NULL DEFAULT '0',
  `location` varchar(160) DEFAULT NULL,
  `description` text,
  `status` varchar(20) NOT NULL DEFAULT 'ON_SHELF',
  `created_by` bigint unsigned DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_books_isbn` (`isbn`),
  KEY `idx_books_search` (`title`,`author`,`category`,`status`),
  KEY `fk_books_created_by` (`created_by`),
  CONSTRAINT `fk_books_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ck_books_copies` CHECK ((`available_copies` <= `total_copies`)),
  CONSTRAINT `ck_books_status` CHECK ((`status` in (_utf8mb4'ON_SHELF',_utf8mb4'UNAVAILABLE',_utf8mb4'ARCHIVED')))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `books`
--

LOCK TABLES `books` WRITE;
/*!40000 ALTER TABLE `books` DISABLE KEYS */;
INSERT INTO `books` VALUES (1,'DEMO-978000000001','软件工程实践导论','VCampus编写组','演示出版社','软件工程',3,3,'A区-01-01',NULL,'ON_SHELF',5,'2026-09-04 13:46:23.341','2026-09-04 15:37:50.580');
/*!40000 ALTER TABLE `books` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `borrow_records`
--

DROP TABLE IF EXISTS `borrow_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `borrow_records` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `book_id` bigint unsigned NOT NULL,
  `borrower_user_id` bigint unsigned NOT NULL,
  `issued_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `due_at` datetime(3) NOT NULL,
  `returned_at` datetime(3) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'BORROWED',
  `renew_count` tinyint unsigned NOT NULL DEFAULT '0',
  `handled_by` bigint unsigned DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_borrow_records_borrower_status` (`borrower_user_id`,`status`,`due_at`),
  KEY `idx_borrow_records_book_status` (`book_id`,`status`),
  KEY `fk_borrow_records_handler` (`handled_by`),
  CONSTRAINT `fk_borrow_records_book` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`),
  CONSTRAINT `fk_borrow_records_borrower` FOREIGN KEY (`borrower_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_borrow_records_handler` FOREIGN KEY (`handled_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ck_borrow_records_returned_at` CHECK (((`status` <> _utf8mb4'RETURNED') or (`returned_at` is not null))),
  CONSTRAINT `ck_borrow_records_status` CHECK ((`status` in (_utf8mb4'BORROWED',_utf8mb4'OVERDUE',_utf8mb4'RETURNED',_utf8mb4'LOST'))),
  CONSTRAINT `ck_borrow_records_time` CHECK ((`due_at` > `issued_at`))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `borrow_records`
--

LOCK TABLES `borrow_records` WRITE;
/*!40000 ALTER TABLE `borrow_records` DISABLE KEYS */;
INSERT INTO `borrow_records` VALUES (1,1,1,'2026-08-20 09:00:00.000','2026-09-20 23:59:59.000','2026-09-04 15:37:50.551','RETURNED',0,1,'演示借阅记录。');
/*!40000 ALTER TABLE `borrow_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cart_items`
--

DROP TABLE IF EXISTS `cart_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart_items` (
  `cart_id` bigint unsigned NOT NULL,
  `product_id` bigint unsigned NOT NULL,
  `quantity` int unsigned NOT NULL,
  `added_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`cart_id`,`product_id`),
  KEY `fk_cart_items_product` (`product_id`),
  CONSTRAINT `fk_cart_items_cart` FOREIGN KEY (`cart_id`) REFERENCES `shopping_carts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cart_items_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `ck_cart_items_quantity` CHECK ((`quantity` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cart_items`
--

LOCK TABLES `cart_items` WRITE;
/*!40000 ALTER TABLE `cart_items` DISABLE KEYS */;
INSERT INTO `cart_items` VALUES (1,1,6,'2026-09-04 15:46:37.713','2026-09-04 15:48:14.119');
/*!40000 ALTER TABLE `cart_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `classroom_reservations`
--

DROP TABLE IF EXISTS `classroom_reservations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `classroom_reservations` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `classroom_id` bigint unsigned NOT NULL,
  `applicant_id` bigint unsigned NOT NULL,
  `purpose` varchar(500) NOT NULL,
  `start_at` datetime(3) NOT NULL,
  `end_at` datetime(3) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `reviewed_by` bigint unsigned DEFAULT NULL,
  `reviewed_at` datetime(3) DEFAULT NULL,
  `review_remark` varchar(500) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_classroom_reservations_overlap` (`classroom_id`,`start_at`,`end_at`,`status`),
  KEY `idx_classroom_reservations_applicant` (`applicant_id`,`status`),
  KEY `fk_classroom_reservations_reviewer` (`reviewed_by`),
  CONSTRAINT `fk_classroom_reservations_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_classroom_reservations_classroom` FOREIGN KEY (`classroom_id`) REFERENCES `classrooms` (`id`),
  CONSTRAINT `fk_classroom_reservations_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ck_classroom_reservations_reviewed_at` CHECK (((`status` in (_utf8mb4'PENDING',_utf8mb4'CANCELLED')) or (`reviewed_at` is not null))),
  CONSTRAINT `ck_classroom_reservations_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'APPROVED',_utf8mb4'REJECTED',_utf8mb4'CANCELLED',_utf8mb4'COMPLETED'))),
  CONSTRAINT `ck_classroom_reservations_time` CHECK ((`end_at` > `start_at`))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `classroom_reservations`
--

LOCK TABLES `classroom_reservations` WRITE;
/*!40000 ALTER TABLE `classroom_reservations` DISABLE KEYS */;
INSERT INTO `classroom_reservations` VALUES (1,1,1,'项目组阶段汇报演示','2026-09-15 18:00:00.000','2026-09-15 20:00:00.000','APPROVED',4,'2026-09-10 10:00:00.000','演示申请已批准。','2026-09-04 13:46:23.336','2026-09-04 13:46:23.336');
/*!40000 ALTER TABLE `classroom_reservations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `classrooms`
--

DROP TABLE IF EXISTS `classrooms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `classrooms` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `building_name` varchar(120) NOT NULL,
  `room_no` varchar(40) NOT NULL,
  `classroom_type` varchar(20) NOT NULL DEFAULT 'TEACHING',
  `capacity` int unsigned NOT NULL,
  `equipment_description` varchar(500) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'AVAILABLE',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_classrooms_building_room` (`building_name`,`room_no`),
  KEY `idx_classrooms_search` (`classroom_type`,`capacity`,`status`),
  CONSTRAINT `ck_classrooms_capacity` CHECK ((`capacity` > 0)),
  CONSTRAINT `ck_classrooms_status` CHECK ((`status` in (_utf8mb4'AVAILABLE',_utf8mb4'MAINTENANCE',_utf8mb4'CLOSED'))),
  CONSTRAINT `ck_classrooms_type` CHECK ((`classroom_type` in (_utf8mb4'TEACHING',_utf8mb4'LAB',_utf8mb4'MEETING',_utf8mb4'OTHER')))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `classrooms`
--

LOCK TABLES `classrooms` WRITE;
/*!40000 ALTER TABLE `classrooms` DISABLE KEYS */;
INSERT INTO `classrooms` VALUES (1,'九龙湖计算机楼','B201','TEACHING',60,'投影、电子讲台、网络','AVAILABLE','2026-09-04 13:46:23.322','2026-09-04 13:46:23.322');
/*!40000 ALTER TABLE `classrooms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `competition_registrations`
--

DROP TABLE IF EXISTS `competition_registrations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competition_registrations` (
  `competition_id` bigint unsigned NOT NULL,
  `student_user_id` bigint unsigned NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'REGISTERED',
  `registered_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `cancelled_at` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`competition_id`,`student_user_id`),
  KEY `idx_competition_registrations_student` (`student_user_id`,`status`),
  CONSTRAINT `fk_competition_registrations_competition` FOREIGN KEY (`competition_id`) REFERENCES `competitions` (`id`),
  CONSTRAINT `fk_competition_registrations_student` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_competition_registrations_cancelled_at` CHECK (((`status` <> _utf8mb4'CANCELLED') or (`cancelled_at` is not null))),
  CONSTRAINT `ck_competition_registrations_status` CHECK ((`status` in (_utf8mb4'REGISTERED',_utf8mb4'CANCELLED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `competition_registrations`
--

LOCK TABLES `competition_registrations` WRITE;
/*!40000 ALTER TABLE `competition_registrations` DISABLE KEYS */;
INSERT INTO `competition_registrations` VALUES (1,1,'REGISTERED','2026-09-04 15:26:45.228',NULL);
/*!40000 ALTER TABLE `competition_registrations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `competitions`
--

DROP TABLE IF EXISTS `competitions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competitions` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `description` text,
  `organizer_id` bigint unsigned NOT NULL,
  `start_at` datetime(3) NOT NULL,
  `end_at` datetime(3) NOT NULL,
  `registration_deadline` datetime(3) NOT NULL,
  `capacity` int unsigned DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_competitions_status_deadline` (`status`,`registration_deadline`),
  KEY `fk_competitions_organizer` (`organizer_id`),
  CONSTRAINT `fk_competitions_organizer` FOREIGN KEY (`organizer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_competitions_capacity` CHECK (((`capacity` is null) or (`capacity` > 0))),
  CONSTRAINT `ck_competitions_status` CHECK ((`status` in (_utf8mb4'DRAFT',_utf8mb4'PUBLISHED',_utf8mb4'CLOSED',_utf8mb4'CANCELLED'))),
  CONSTRAINT `ck_competitions_time` CHECK (((`end_at` > `start_at`) and (`registration_deadline` <= `start_at`)))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `competitions`
--

LOCK TABLES `competitions` WRITE;
/*!40000 ALTER TABLE `competitions` DISABLE KEYS */;
INSERT INTO `competitions` VALUES (1,'校园创新实践演示赛','用于演示比赛发布、报名、取消与名单查看。',4,'2026-10-10 09:00:00.000','2026-10-10 17:00:00.000','2026-10-01 23:59:59.000',100,'PUBLISHED','2026-09-04 13:46:23.332','2026-09-04 13:46:23.332');
/*!40000 ALTER TABLE `competitions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `course_grades`
--

DROP TABLE IF EXISTS `course_grades`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_grades` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `enrollment_id` bigint unsigned NOT NULL,
  `score` decimal(5,2) NOT NULL,
  `grade_point` decimal(4,2) DEFAULT NULL,
  `gpa_included` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Server-controlled inclusion flag for GPA calculations',
  `recorded_by` bigint unsigned NOT NULL,
  `recorded_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `remark` varchar(500) DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_grades_enrollment` (`enrollment_id`),
  KEY `idx_course_grades_recorder` (`recorded_by`,`recorded_at`),
  CONSTRAINT `fk_course_grades_enrollment` FOREIGN KEY (`enrollment_id`) REFERENCES `enrollments` (`id`),
  CONSTRAINT `fk_course_grades_recorder` FOREIGN KEY (`recorded_by`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_course_grades_point` CHECK (((`grade_point` is null) or (`grade_point` between 0 and 5))),
  CONSTRAINT `ck_course_grades_score` CHECK ((`score` between 0 and 100))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `course_grades`
--

LOCK TABLES `course_grades` WRITE;
/*!40000 ALTER TABLE `course_grades` DISABLE KEYS */;
INSERT INTO `course_grades` VALUES (1,1,92.00,4.00,1,2,'2026-09-04 13:46:23.329','演示成绩','2026-09-04 13:46:23.329');
/*!40000 ALTER TABLE `course_grades` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `course_instructors`
--

DROP TABLE IF EXISTS `course_instructors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_instructors` (
  `course_id` bigint unsigned NOT NULL,
  `teacher_user_id` bigint unsigned NOT NULL,
  `instructor_role` varchar(20) NOT NULL DEFAULT 'PRIMARY',
  `assigned_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`course_id`,`teacher_user_id`),
  KEY `idx_course_instructors_teacher` (`teacher_user_id`,`course_id`),
  CONSTRAINT `fk_course_instructors_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`),
  CONSTRAINT `fk_course_instructors_teacher` FOREIGN KEY (`teacher_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_course_instructors_role` CHECK ((`instructor_role` in (_utf8mb4'PRIMARY',_utf8mb4'ASSISTANT')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `course_instructors`
--

LOCK TABLES `course_instructors` WRITE;
/*!40000 ALTER TABLE `course_instructors` DISABLE KEYS */;
INSERT INTO `course_instructors` VALUES (1,2,'PRIMARY','2026-09-04 13:46:23.325');
/*!40000 ALTER TABLE `course_instructors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `course_schedules`
--

DROP TABLE IF EXISTS `course_schedules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_schedules` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `course_id` bigint unsigned NOT NULL,
  `weekday` tinyint unsigned NOT NULL,
  `start_period` tinyint unsigned NOT NULL,
  `end_period` tinyint unsigned NOT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `classroom_id` bigint unsigned DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_course_schedules_course` (`course_id`),
  KEY `idx_course_schedules_classroom_time` (`classroom_id`,`weekday`,`start_period`,`end_period`),
  CONSTRAINT `fk_course_schedules_classroom` FOREIGN KEY (`classroom_id`) REFERENCES `classrooms` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_course_schedules_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`),
  CONSTRAINT `ck_course_schedules_date` CHECK (((`end_date` is null) or (`start_date` is null) or (`end_date` >= `start_date`))),
  CONSTRAINT `ck_course_schedules_period` CHECK (((`start_period` >= 1) and (`end_period` >= `start_period`))),
  CONSTRAINT `ck_course_schedules_weekday` CHECK ((`weekday` between 1 and 7))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `course_schedules`
--

LOCK TABLES `course_schedules` WRITE;
/*!40000 ALTER TABLE `course_schedules` DISABLE KEYS */;
INSERT INTO `course_schedules` VALUES (1,1,2,1,2,'2026-09-01','2026-12-31',1,'2026-09-04 13:46:23.326');
/*!40000 ALTER TABLE `course_schedules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `courses`
--

DROP TABLE IF EXISTS `courses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `courses` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `course_code` varchar(40) NOT NULL,
  `course_name` varchar(160) NOT NULL,
  `course_type` varchar(20) NOT NULL DEFAULT 'ELECTIVE',
  `semester_code` varchar(32) NOT NULL DEFAULT 'UNSPECIFIED',
  `credits` decimal(4,2) NOT NULL,
  `total_hours` smallint unsigned DEFAULT NULL,
  `capacity` int unsigned NOT NULL,
  `description` text,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `created_by` bigint unsigned DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_courses_course_code` (`course_code`),
  KEY `idx_courses_search` (`course_name`,`course_type`,`status`),
  KEY `fk_courses_created_by` (`created_by`),
  KEY `idx_courses_semester` (`semester_code`,`status`),
  CONSTRAINT `fk_courses_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ck_courses_capacity` CHECK ((`capacity` > 0)),
  CONSTRAINT `ck_courses_credits` CHECK ((`credits` > 0)),
  CONSTRAINT `ck_courses_status` CHECK ((`status` in (_utf8mb4'DRAFT',_utf8mb4'PUBLISHED',_utf8mb4'CLOSED',_utf8mb4'ARCHIVED'))),
  CONSTRAINT `ck_courses_type` CHECK ((`course_type` in (_utf8mb4'REQUIRED',_utf8mb4'ELECTIVE',_utf8mb4'PUBLIC',_utf8mb4'PRACTICE')))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `courses`
--

LOCK TABLES `courses` WRITE;
/*!40000 ALTER TABLE `courses` DISABLE KEYS */;
INSERT INTO `courses` VALUES (1,'DEMO-SE-001','软件工程实践','PRACTICE','2026-FALL',3.00,48,50,'用于演示课程查询、选课、课表与成绩链路。','PUBLISHED',2,'2026-09-04 13:46:23.323','2026-09-04 13:46:29.949');
/*!40000 ALTER TABLE `courses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dorm_absence_warnings`
--

DROP TABLE IF EXISTS `dorm_absence_warnings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dorm_absence_warnings` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `student_user_id` bigint unsigned NOT NULL,
  `room_id` bigint unsigned NOT NULL,
  `scan_date` date NOT NULL COMMENT '扫描日；与学生构成唯一键',
  `last_leave_at` datetime(3) DEFAULT NULL COMMENT '最后一次离宿时间',
  `absence_days` int unsigned NOT NULL COMMENT '连续未归天数',
  `warning_level` varchar(16) NOT NULL COMMENT 'NORMAL 一般 / SEVERE 严重 / EXEMPT 已豁免',
  `handle_status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING 待处理 / NOTIFIED 已通知 / VERIFIED 已核实',
  `notified_teacher_id` bigint unsigned DEFAULT NULL,
  `notified_at` datetime(3) DEFAULT NULL,
  `note` varchar(500) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dorm_absence_warnings_student_scan` (`student_user_id`,`scan_date`),
  KEY `idx_dorm_absence_warnings_triage` (`handle_status`,`warning_level`,`scan_date`),
  KEY `idx_dorm_absence_warnings_room` (`room_id`,`scan_date`),
  KEY `fk_dorm_absence_warnings_teacher` (`notified_teacher_id`),
  CONSTRAINT `fk_dorm_absence_warnings_room` FOREIGN KEY (`room_id`) REFERENCES `dorm_rooms` (`id`),
  CONSTRAINT `fk_dorm_absence_warnings_student` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_dorm_absence_warnings_teacher` FOREIGN KEY (`notified_teacher_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_dorm_absence_warnings_level` CHECK ((`warning_level` in (_utf8mb4'NORMAL',_utf8mb4'SEVERE',_utf8mb4'EXEMPT'))),
  CONSTRAINT `ck_dorm_absence_warnings_notified` CHECK (((`handle_status` <> _utf8mb4'NOTIFIED') or ((`notified_teacher_id` is not null) and (`notified_at` is not null)))),
  CONSTRAINT `ck_dorm_absence_warnings_status` CHECK ((`handle_status` in (_utf8mb4'PENDING',_utf8mb4'NOTIFIED',_utf8mb4'VERIFIED')))
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dorm_absence_warnings`
--

LOCK TABLES `dorm_absence_warnings` WRITE;
/*!40000 ALTER TABLE `dorm_absence_warnings` DISABLE KEYS */;
INSERT INTO `dorm_absence_warnings` VALUES (20,12,1,'2026-09-10','2026-09-07 09:30:00.000',3,'NORMAL','PENDING',NULL,NULL,NULL,'2026-09-11 15:01:08.694','2026-09-11 15:01:08.694'),(21,13,2,'2026-09-10','2026-09-02 14:00:00.000',8,'SEVERE','NOTIFIED',2,'2026-09-10 08:30:00.000','已电话通知辅导员，反馈学生在家备考研究生考试，家长知情。','2026-09-11 15:01:08.694','2026-09-11 15:01:08.694'),(22,16,5,'2026-09-10','2026-08-22 10:00:00.000',19,'SEVERE','PENDING',NULL,NULL,NULL,'2026-09-11 15:01:08.694','2026-09-11 15:01:08.694'),(23,14,3,'2026-09-10','2026-08-30 07:30:00.000',11,'EXEMPT','VERIFIED',NULL,NULL,'校外实习请假已批准，按规则豁免。','2026-09-11 15:01:08.694','2026-09-11 15:01:08.694'),(24,12,1,'2026-09-12','2026-09-07 09:30:00.000',5,'NORMAL','PENDING',NULL,NULL,NULL,'2026-09-12 07:59:58.019','2026-09-12 07:59:58.019'),(25,13,2,'2026-09-12','2026-09-02 14:00:00.000',10,'SEVERE','PENDING',NULL,NULL,NULL,'2026-09-12 07:59:58.031','2026-09-12 07:59:58.031'),(26,14,3,'2026-09-12','2026-08-30 07:30:00.000',13,'EXEMPT','PENDING',NULL,NULL,NULL,'2026-09-12 07:59:58.035','2026-09-12 07:59:58.035'),(27,16,5,'2026-09-12','2026-08-22 10:00:00.000',21,'SEVERE','PENDING',NULL,NULL,NULL,'2026-09-12 07:59:58.039','2026-09-12 07:59:58.039'),(32,12,1,'2026-09-13','2026-09-07 09:30:00.000',6,'NORMAL','PENDING',NULL,NULL,NULL,'2026-09-13 07:59:59.769','2026-09-13 07:59:59.769'),(33,13,2,'2026-09-13','2026-09-02 14:00:00.000',11,'SEVERE','PENDING',NULL,NULL,NULL,'2026-09-13 07:59:59.795','2026-09-13 07:59:59.795'),(34,14,2,'2026-09-13','2026-08-30 07:30:00.000',14,'EXEMPT','PENDING',NULL,NULL,NULL,'2026-09-13 07:59:59.797','2026-09-13 17:29:38.447'),(35,16,5,'2026-09-13','2026-08-22 10:00:00.000',22,'SEVERE','NOTIFIED',2,'2026-09-13 17:30:00.144','经常未归','2026-09-13 07:59:59.800','2026-09-13 17:30:00.146');
/*!40000 ALTER TABLE `dorm_absence_warnings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dorm_access_policies`
--

DROP TABLE IF EXISTS `dorm_access_policies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dorm_access_policies` (
  `id` tinyint unsigned NOT NULL DEFAULT '1',
  `curfew_time` time NOT NULL DEFAULT '23:00:00' COMMENT '门禁时间，晚于此点归宿记晚归',
  `dawn_time` time NOT NULL DEFAULT '05:00:00' COMMENT '早于此点归宿同样记晚归（通宵未归）',
  `updated_by` bigint unsigned DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `fk_dorm_access_policies_actor` (`updated_by`),
  CONSTRAINT `fk_dorm_access_policies_actor` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ck_dorm_access_policies_singleton` CHECK ((`id` = 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dorm_access_policies`
--

LOCK TABLES `dorm_access_policies` WRITE;
/*!40000 ALTER TABLE `dorm_access_policies` DISABLE KEYS */;
INSERT INTO `dorm_access_policies` VALUES (1,'23:30:00','05:00:00',7,'2026-09-13 17:32:16.978');
/*!40000 ALTER TABLE `dorm_access_policies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dorm_beds`
--

DROP TABLE IF EXISTS `dorm_beds`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dorm_beds` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `room_id` bigint unsigned NOT NULL,
  `bed_no` varchar(20) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'AVAILABLE',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dorm_beds_room_bed` (`room_id`,`bed_no`),
  KEY `idx_dorm_beds_room_status` (`room_id`,`status`),
  CONSTRAINT `fk_dorm_beds_room` FOREIGN KEY (`room_id`) REFERENCES `dorm_rooms` (`id`),
  CONSTRAINT `ck_dorm_beds_status` CHECK ((`status` in (_utf8mb4'AVAILABLE',_utf8mb4'OCCUPIED',_utf8mb4'MAINTENANCE')))
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dorm_beds`
--

LOCK TABLES `dorm_beds` WRITE;
/*!40000 ALTER TABLE `dorm_beds` DISABLE KEYS */;
INSERT INTO `dorm_beds` VALUES (1,1,'1','AVAILABLE','2026-09-04 13:46:23.383','2026-09-13 17:26:54.819'),(2,1,'2','OCCUPIED','2026-09-04 13:46:23.383','2026-09-11 15:01:08.636'),(3,1,'3','OCCUPIED','2026-09-04 13:46:23.383','2026-09-11 15:01:08.636'),(4,1,'4','OCCUPIED','2026-09-04 13:46:23.383','2026-09-13 17:26:54.822'),(26,2,'4','OCCUPIED','2026-09-11 15:01:08.446','2026-09-13 17:27:25.469'),(27,2,'3','OCCUPIED','2026-09-11 15:01:08.446','2026-09-13 17:27:16.084'),(28,2,'2','OCCUPIED','2026-09-11 15:01:08.446','2026-09-11 15:01:08.636'),(29,2,'1','OCCUPIED','2026-09-11 15:01:08.446','2026-09-11 15:01:08.636'),(30,3,'4','AVAILABLE','2026-09-11 15:01:08.446','2026-09-11 15:01:08.446'),(31,3,'3','AVAILABLE','2026-09-11 15:01:08.446','2026-09-11 15:01:08.446'),(32,3,'2','AVAILABLE','2026-09-11 15:01:08.446','2026-09-11 15:01:08.446'),(33,3,'1','AVAILABLE','2026-09-11 15:01:08.446','2026-09-13 17:27:25.467'),(34,4,'4','AVAILABLE','2026-09-11 15:01:08.446','2026-09-11 15:01:08.446'),(35,4,'3','AVAILABLE','2026-09-11 15:01:08.446','2026-09-11 15:01:08.446'),(36,4,'2','AVAILABLE','2026-09-11 15:01:08.446','2026-09-11 15:01:08.446'),(37,4,'1','AVAILABLE','2026-09-11 15:01:08.446','2026-09-11 15:01:08.446'),(38,5,'4','AVAILABLE','2026-09-11 15:01:08.446','2026-09-11 15:01:08.446'),(39,5,'3','AVAILABLE','2026-09-11 15:01:08.446','2026-09-11 15:01:08.446'),(40,5,'2','AVAILABLE','2026-09-11 15:01:08.446','2026-09-11 15:01:08.446'),(41,5,'1','OCCUPIED','2026-09-11 15:01:08.446','2026-09-11 15:01:08.636'),(42,6,'4','AVAILABLE','2026-09-11 15:01:08.446','2026-09-11 15:01:08.446'),(43,6,'3','AVAILABLE','2026-09-11 15:01:08.446','2026-09-11 15:01:08.446'),(44,6,'2','AVAILABLE','2026-09-11 15:01:08.446','2026-09-11 15:01:08.446'),(45,6,'1','OCCUPIED','2026-09-11 15:01:08.446','2026-09-11 15:01:08.636');
/*!40000 ALTER TABLE `dorm_beds` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dorm_buildings`
--

DROP TABLE IF EXISTS `dorm_buildings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dorm_buildings` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `building_code` varchar(40) NOT NULL,
  `building_name` varchar(120) NOT NULL,
  `address` varchar(255) DEFAULT NULL,
  `gender_policy` varchar(16) NOT NULL DEFAULT 'MIXED',
  `status` varchar(20) NOT NULL DEFAULT 'OPEN',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dorm_buildings_code` (`building_code`),
  KEY `idx_dorm_buildings_status` (`status`),
  CONSTRAINT `ck_dorm_buildings_gender` CHECK ((`gender_policy` in (_utf8mb4'MALE',_utf8mb4'FEMALE',_utf8mb4'MIXED'))),
  CONSTRAINT `ck_dorm_buildings_status` CHECK ((`status` in (_utf8mb4'OPEN',_utf8mb4'MAINTENANCE',_utf8mb4'CLOSED')))
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dorm_buildings`
--

LOCK TABLES `dorm_buildings` WRITE;
/*!40000 ALTER TABLE `dorm_buildings` DISABLE KEYS */;
INSERT INTO `dorm_buildings` VALUES (1,'DEMO-D1','九龙湖学生公寓D1','九龙湖校区桃园片区','FEMALE','OPEN','2026-09-04 13:46:23.381','2026-09-11 15:01:08.413'),(2,'DEMO-D2','九龙湖学生公寓D2','九龙湖校区桃园片区','FEMALE','OPEN','2026-09-05 18:52:59.051','2026-09-11 15:01:08.413');
/*!40000 ALTER TABLE `dorm_buildings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dorm_hygiene_item_scores`
--

DROP TABLE IF EXISTS `dorm_hygiene_item_scores`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dorm_hygiene_item_scores` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `inspection_id` bigint unsigned NOT NULL,
  `item_code` varchar(24) NOT NULL COMMENT 'FLOOR/DESK/BED/BATHROOM/BALCONY',
  `score` decimal(5,2) NOT NULL COMMENT '单项 0~20',
  `deduct_reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dorm_hygiene_item` (`inspection_id`,`item_code`),
  CONSTRAINT `fk_dorm_hygiene_item_inspection` FOREIGN KEY (`inspection_id`) REFERENCES `hygiene_inspections` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ck_dorm_hygiene_item_code` CHECK ((`item_code` in (_utf8mb4'FLOOR',_utf8mb4'DESK',_utf8mb4'BED',_utf8mb4'BATHROOM',_utf8mb4'BALCONY'))),
  CONSTRAINT `ck_dorm_hygiene_item_score` CHECK ((`score` between 0 and 20))
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dorm_hygiene_item_scores`
--

LOCK TABLES `dorm_hygiene_item_scores` WRITE;
/*!40000 ALTER TABLE `dorm_hygiene_item_scores` DISABLE KEYS */;
INSERT INTO `dorm_hygiene_item_scores` VALUES (1,3,'FLOOR',19.00,NULL),(2,3,'DESK',18.00,'一张书桌有少量杂物'),(3,3,'BED',19.00,NULL),(4,3,'BATHROOM',18.00,'镜面有水渍'),(5,3,'BALCONY',18.00,'晾晒区有未收的衣架'),(6,4,'FLOOR',12.00,'零食碎屑、头发未清扫'),(7,4,'DESK',11.00,'外卖盒与杂物堆放'),(8,4,'BED',15.00,'一张床被褥未整理'),(9,4,'BATHROOM',10.00,'镜面台面水渍、垃圾桶未清'),(10,4,'BALCONY',16.00,'地面有积水'),(11,5,'FLOOR',9.00,'积水未清'),(12,5,'DESK',14.00,NULL),(13,5,'BED',15.00,NULL),(14,5,'BATHROOM',12.00,'垃圾未倒'),(15,5,'BALCONY',8.00,'堆放大量纸箱'),(16,6,'FLOOR',17.00,NULL),(17,6,'DESK',16.00,'书桌略乱'),(18,6,'BED',17.00,NULL),(19,6,'BATHROOM',16.00,NULL),(20,6,'BALCONY',16.00,NULL),(21,7,'FLOOR',18.00,NULL),(22,7,'DESK',17.00,NULL),(23,7,'BED',18.00,NULL),(24,7,'BATHROOM',17.00,NULL),(25,7,'BALCONY',18.00,NULL),(26,8,'FLOOR',20.00,NULL),(27,8,'DESK',20.00,NULL),(28,8,'BED',20.00,NULL),(29,8,'BATHROOM',20.00,NULL),(30,8,'BALCONY',20.00,NULL),(31,9,'FLOOR',10.00,NULL),(32,9,'DESK',20.00,NULL),(33,9,'BED',20.00,NULL),(34,9,'BATHROOM',20.00,NULL),(35,9,'BALCONY',20.00,NULL);
/*!40000 ALTER TABLE `dorm_hygiene_item_scores` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dorm_hygiene_tasks`
--

DROP TABLE IF EXISTS `dorm_hygiene_tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dorm_hygiene_tasks` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `room_id` bigint unsigned NOT NULL,
  `task_type` varchar(12) NOT NULL COMMENT 'WEEKLY 周检查 / RECHECK 复查',
  `plan_date` date NOT NULL,
  `status` varchar(12) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DONE/SKIPPED',
  `inspection_id` bigint unsigned DEFAULT NULL COMMENT '完成时回填本次检查记录',
  `source_inspection_id` bigint unsigned DEFAULT NULL COMMENT '复查任务指向触发它的那次检查',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dorm_hygiene_task` (`room_id`,`plan_date`,`task_type`),
  KEY `idx_dorm_hygiene_task_queue` (`status`,`plan_date`),
  KEY `fk_dorm_hygiene_task_inspection` (`inspection_id`),
  KEY `fk_dorm_hygiene_task_source` (`source_inspection_id`),
  CONSTRAINT `fk_dorm_hygiene_task_inspection` FOREIGN KEY (`inspection_id`) REFERENCES `hygiene_inspections` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_dorm_hygiene_task_room` FOREIGN KEY (`room_id`) REFERENCES `dorm_rooms` (`id`),
  CONSTRAINT `fk_dorm_hygiene_task_source` FOREIGN KEY (`source_inspection_id`) REFERENCES `hygiene_inspections` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ck_dorm_hygiene_task_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'DONE',_utf8mb4'SKIPPED'))),
  CONSTRAINT `ck_dorm_hygiene_task_type` CHECK ((`task_type` in (_utf8mb4'WEEKLY',_utf8mb4'RECHECK')))
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dorm_hygiene_tasks`
--

LOCK TABLES `dorm_hygiene_tasks` WRITE;
/*!40000 ALTER TABLE `dorm_hygiene_tasks` DISABLE KEYS */;
INSERT INTO `dorm_hygiene_tasks` VALUES (9,1,'WEEKLY','2026-09-02','DONE',3,NULL,'2026-09-11 15:01:08.847','2026-09-11 15:01:08.847'),(10,2,'WEEKLY','2026-09-02','DONE',5,NULL,'2026-09-11 15:01:08.847','2026-09-11 15:01:08.847'),(11,3,'WEEKLY','2026-09-02','DONE',7,NULL,'2026-09-11 15:01:08.847','2026-09-11 15:01:08.847'),(12,1,'WEEKLY','2026-09-09','DONE',4,NULL,'2026-09-11 15:01:08.847','2026-09-11 15:01:08.847'),(13,2,'RECHECK','2026-09-09','DONE',6,5,'2026-09-11 15:01:08.847','2026-09-11 15:01:08.847'),(14,1,'RECHECK','2026-09-12','DONE',8,4,'2026-09-11 15:01:08.847','2026-09-13 17:30:40.933'),(15,2,'WEEKLY','2026-09-11','PENDING',NULL,NULL,'2026-09-11 15:01:08.847','2026-09-11 15:01:08.847'),(16,3,'WEEKLY','2026-09-11','PENDING',NULL,NULL,'2026-09-11 15:01:08.847','2026-09-11 15:01:08.847'),(17,5,'WEEKLY','2026-09-11','DONE',9,NULL,'2026-09-11 15:01:08.847','2026-09-13 17:30:50.833'),(18,6,'WEEKLY','2026-09-11','PENDING',NULL,NULL,'2026-09-11 15:01:08.847','2026-09-11 15:01:08.847'),(19,1,'WEEKLY','2026-09-13','DONE',8,NULL,'2026-09-13 14:02:09.816','2026-09-13 17:30:40.933'),(20,2,'WEEKLY','2026-09-13','PENDING',NULL,NULL,'2026-09-13 14:02:09.818','2026-09-13 14:02:09.818'),(21,3,'WEEKLY','2026-09-13','PENDING',NULL,NULL,'2026-09-13 14:02:09.819','2026-09-13 14:02:09.819'),(22,4,'WEEKLY','2026-09-13','PENDING',NULL,NULL,'2026-09-13 14:02:09.820','2026-09-13 14:02:09.820'),(23,5,'WEEKLY','2026-09-13','DONE',9,NULL,'2026-09-13 14:02:09.821','2026-09-13 17:30:50.833'),(24,6,'WEEKLY','2026-09-13','PENDING',NULL,NULL,'2026-09-13 14:02:09.823','2026-09-13 14:02:09.823');
/*!40000 ALTER TABLE `dorm_hygiene_tasks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dorm_meter_readings`
--

DROP TABLE IF EXISTS `dorm_meter_readings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dorm_meter_readings` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `room_id` bigint unsigned NOT NULL,
  `period_start` date NOT NULL,
  `period_end` date NOT NULL,
  `electricity_units` decimal(12,3) NOT NULL DEFAULT '0.000' COMMENT '本账期用电量',
  `water_units` decimal(12,3) NOT NULL DEFAULT '0.000' COMMENT '本账期用水量',
  `electricity_price` decimal(10,4) NOT NULL COMMENT '电费单价',
  `water_price` decimal(10,4) NOT NULL COMMENT '水费单价',
  `recorded_by` bigint unsigned NOT NULL COMMENT '录入的宿管员',
  `recorded_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `bill_id` bigint unsigned DEFAULT NULL COMMENT '已生成账单时回填；非空表示读数已锁定',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dorm_meter_readings_room_period` (`room_id`,`period_start`,`period_end`),
  KEY `idx_dorm_meter_readings_pending` (`bill_id`,`period_start`),
  KEY `fk_dorm_meter_readings_recorder` (`recorded_by`),
  CONSTRAINT `fk_dorm_meter_readings_bill` FOREIGN KEY (`bill_id`) REFERENCES `utility_bills` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_dorm_meter_readings_recorder` FOREIGN KEY (`recorded_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_dorm_meter_readings_room` FOREIGN KEY (`room_id`) REFERENCES `dorm_rooms` (`id`),
  CONSTRAINT `ck_dorm_meter_readings_period` CHECK ((`period_end` >= `period_start`)),
  CONSTRAINT `ck_dorm_meter_readings_price` CHECK (((`electricity_price` > 0) and (`water_price` > 0))),
  CONSTRAINT `ck_dorm_meter_readings_units` CHECK (((`electricity_units` >= 0) and (`water_units` >= 0)))
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dorm_meter_readings`
--

LOCK TABLES `dorm_meter_readings` WRITE;
/*!40000 ALTER TABLE `dorm_meter_readings` DISABLE KEYS */;
INSERT INTO `dorm_meter_readings` VALUES (5,1,'2026-08-01','2026-08-31',132.500,21.300,0.5500,3.2000,7,'2026-09-01 09:20:00.000','2026-09-11 15:01:08.743',2),(6,2,'2026-08-01','2026-08-31',98.200,15.600,0.5500,3.2000,7,'2026-09-01 09:24:00.000','2026-09-11 15:01:08.728',NULL),(7,3,'2026-08-01','2026-08-31',41.000,6.800,0.5500,3.2000,7,'2026-09-01 09:27:00.000','2026-09-11 15:01:08.728',NULL),(8,4,'2026-08-01','2026-08-31',12.300,0.500,0.5500,3.2000,7,'2026-09-01 09:30:00.000','2026-09-11 15:01:08.728',NULL),(9,5,'2026-08-01','2026-08-31',90.400,12.000,0.5500,3.2000,7,'2026-09-01 10:05:00.000','2026-09-13 17:29:20.815',3),(10,6,'2026-08-01','2026-08-31',60.900,9.400,0.5500,3.2000,7,'2026-09-01 10:08:00.000','2026-09-13 17:29:27.936',4);
/*!40000 ALTER TABLE `dorm_meter_readings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dorm_notice_extras`
--

DROP TABLE IF EXISTS `dorm_notice_extras`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dorm_notice_extras` (
  `announcement_id` bigint unsigned NOT NULL,
  `notice_type` varchar(20) NOT NULL DEFAULT 'GENERAL',
  `scope_type` varchar(16) NOT NULL DEFAULT 'ALL',
  `scope_building_id` bigint unsigned DEFAULT NULL,
  `scope_room_id` bigint unsigned DEFAULT NULL,
  `pinned` tinyint(1) NOT NULL DEFAULT '0',
  `pinned_at` datetime(3) DEFAULT NULL COMMENT '置顶时刻，多条置顶时按此倒序',
  `updated_by` bigint unsigned NOT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`announcement_id`),
  KEY `idx_dorm_notice_extras_pinned` (`pinned`,`pinned_at`),
  KEY `idx_dorm_notice_extras_scope` (`scope_type`,`scope_building_id`,`scope_room_id`),
  KEY `fk_dorm_notice_extras_building` (`scope_building_id`),
  KEY `fk_dorm_notice_extras_room` (`scope_room_id`),
  KEY `fk_dorm_notice_extras_actor` (`updated_by`),
  CONSTRAINT `fk_dorm_notice_extras_actor` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_dorm_notice_extras_announcement` FOREIGN KEY (`announcement_id`) REFERENCES `announcements` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_dorm_notice_extras_building` FOREIGN KEY (`scope_building_id`) REFERENCES `dorm_buildings` (`id`),
  CONSTRAINT `fk_dorm_notice_extras_room` FOREIGN KEY (`scope_room_id`) REFERENCES `dorm_rooms` (`id`),
  CONSTRAINT `ck_dorm_notice_extras_scope` CHECK ((`scope_type` in (_utf8mb4'ALL',_utf8mb4'BUILDING',_utf8mb4'ROOM'))),
  CONSTRAINT `ck_dorm_notice_extras_scope_target` CHECK ((((`scope_type` = _utf8mb4'ALL') and (`scope_building_id` is null) and (`scope_room_id` is null)) or ((`scope_type` = _utf8mb4'BUILDING') and (`scope_building_id` is not null) and (`scope_room_id` is null)) or ((`scope_type` = _utf8mb4'ROOM') and (`scope_room_id` is not null)))),
  CONSTRAINT `ck_dorm_notice_extras_type` CHECK ((`notice_type` in (_utf8mb4'GENERAL',_utf8mb4'MAINTENANCE',_utf8mb4'HYGIENE',_utf8mb4'SAFETY',_utf8mb4'URGENT')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dorm_notice_extras`
--

LOCK TABLES `dorm_notice_extras` WRITE;
/*!40000 ALTER TABLE `dorm_notice_extras` DISABLE KEYS */;
INSERT INTO `dorm_notice_extras` VALUES (6,'URGENT','ALL',NULL,NULL,1,'2026-09-10 15:01:08.000',7,'2026-09-11 15:01:08.893'),(7,'MAINTENANCE','BUILDING',1,NULL,0,NULL,7,'2026-09-11 15:01:08.893'),(8,'HYGIENE','ROOM',NULL,1,0,NULL,7,'2026-09-11 15:01:08.893'),(9,'GENERAL','ALL',NULL,NULL,1,'2026-09-03 15:01:08.000',7,'2026-09-11 15:01:08.893'),(10,'GENERAL','ALL',NULL,NULL,0,NULL,7,'2026-09-11 15:01:08.893'),(11,'URGENT','ALL',NULL,NULL,0,NULL,7,'2026-09-13 17:31:42.200');
/*!40000 ALTER TABLE `dorm_notice_extras` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dorm_repair_entry_permits`
--

DROP TABLE IF EXISTS `dorm_repair_entry_permits`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dorm_repair_entry_permits` (
  `repair_order_id` bigint unsigned NOT NULL,
  `allow_enter` tinyint(1) NOT NULL DEFAULT '0',
  `note` varchar(255) DEFAULT NULL,
  `updated_by` bigint unsigned NOT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`repair_order_id`),
  KEY `fk_dorm_repair_permit_actor` (`updated_by`),
  CONSTRAINT `fk_dorm_repair_permit_actor` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_dorm_repair_permit_order` FOREIGN KEY (`repair_order_id`) REFERENCES `repair_orders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dorm_repair_entry_permits`
--

LOCK TABLES `dorm_repair_entry_permits` WRITE;
/*!40000 ALTER TABLE `dorm_repair_entry_permits` DISABLE KEYS */;
INSERT INTO `dorm_repair_entry_permits` VALUES (8,0,'希望本人在场，周三下午没课。',1,'2026-09-11 15:01:08.715'),(10,1,'白天都在上课，可直接联系宿管开门维修，贵重物品已收好。',1,'2026-09-11 15:01:08.715'),(12,1,'维修时请提前十分钟打电话，我从教室赶回来开门。',1,'2026-09-11 15:01:08.715'),(16,1,NULL,1,'2026-09-13 17:23:16.332');
/*!40000 ALTER TABLE `dorm_repair_entry_permits` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dorm_rooms`
--

DROP TABLE IF EXISTS `dorm_rooms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dorm_rooms` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `building_id` bigint unsigned NOT NULL,
  `room_no` varchar(40) NOT NULL,
  `floor_no` smallint NOT NULL,
  `capacity` int unsigned NOT NULL,
  `room_type` varchar(20) NOT NULL DEFAULT 'STANDARD',
  `status` varchar(20) NOT NULL DEFAULT 'AVAILABLE',
  `description` varchar(500) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dorm_rooms_building_room` (`building_id`,`room_no`),
  KEY `idx_dorm_rooms_search` (`building_id`,`floor_no`,`status`),
  CONSTRAINT `fk_dorm_rooms_building` FOREIGN KEY (`building_id`) REFERENCES `dorm_buildings` (`id`),
  CONSTRAINT `ck_dorm_rooms_capacity` CHECK ((`capacity` > 0)),
  CONSTRAINT `ck_dorm_rooms_status` CHECK ((`status` in (_utf8mb4'AVAILABLE',_utf8mb4'FULL',_utf8mb4'MAINTENANCE',_utf8mb4'CLOSED'))),
  CONSTRAINT `ck_dorm_rooms_type` CHECK ((`room_type` in (_utf8mb4'STANDARD',_utf8mb4'SUITE',_utf8mb4'SPECIAL')))
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dorm_rooms`
--

LOCK TABLES `dorm_rooms` WRITE;
/*!40000 ALTER TABLE `dorm_rooms` DISABLE KEYS */;
INSERT INTO `dorm_rooms` VALUES (1,1,'101',1,4,'STANDARD','AVAILABLE','四人间，朝南。','2026-09-04 13:46:23.382','2026-09-11 15:01:08.424'),(2,1,'102',1,4,'STANDARD','AVAILABLE','四人间，朝南。','2026-09-05 18:52:59.060','2026-09-11 15:01:08.424'),(3,1,'103',1,4,'STANDARD','AVAILABLE','四人间，朝北，靠近楼梯口。','2026-09-05 18:52:59.060','2026-09-11 15:01:08.424'),(4,1,'201',2,4,'STANDARD','AVAILABLE','四人间，本学期暂未安排入住。','2026-09-05 18:52:59.060','2026-09-11 15:01:08.424'),(5,2,'101',1,4,'STANDARD','AVAILABLE','四人间，朝南。','2026-09-05 18:52:59.060','2026-09-11 15:01:08.424'),(6,2,'102',1,4,'STANDARD','AVAILABLE','四人间，朝南。','2026-09-05 18:52:59.060','2026-09-11 15:01:08.424');
/*!40000 ALTER TABLE `dorm_rooms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dorm_visitor_registrations`
--

DROP TABLE IF EXISTS `dorm_visitor_registrations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dorm_visitor_registrations` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `student_user_id` bigint unsigned NOT NULL COMMENT '接待的学生',
  `room_id` bigint unsigned NOT NULL COMMENT '服务端按在住记录解析',
  `visitor_name` varchar(60) NOT NULL,
  `visitor_id_card` varchar(40) NOT NULL COMMENT '仅登记与核验用；查询接口只返回掩码',
  `visitor_phone` varchar(32) DEFAULT NULL,
  `visit_reason` varchar(500) NOT NULL,
  `start_at` datetime(3) NOT NULL,
  `end_at` datetime(3) NOT NULL,
  `submitted_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `audit_status` varchar(16) NOT NULL DEFAULT 'PENDING',
  `auditor_id` bigint unsigned DEFAULT NULL,
  `audited_at` datetime(3) DEFAULT NULL,
  `audit_remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_dorm_visitor_student` (`student_user_id`,`submitted_at`),
  KEY `idx_dorm_visitor_review` (`audit_status`,`start_at`),
  KEY `idx_dorm_visitor_room` (`room_id`,`start_at`),
  KEY `fk_dorm_visitor_auditor` (`auditor_id`),
  CONSTRAINT `fk_dorm_visitor_auditor` FOREIGN KEY (`auditor_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_dorm_visitor_room` FOREIGN KEY (`room_id`) REFERENCES `dorm_rooms` (`id`),
  CONSTRAINT `fk_dorm_visitor_student` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_dorm_visitor_audited` CHECK (((`audit_status` in (_utf8mb4'PENDING',_utf8mb4'CANCELLED')) or ((`auditor_id` is not null) and (`audited_at` is not null)))),
  CONSTRAINT `ck_dorm_visitor_period` CHECK ((`end_at` > `start_at`)),
  CONSTRAINT `ck_dorm_visitor_status` CHECK ((`audit_status` in (_utf8mb4'PENDING',_utf8mb4'APPROVED',_utf8mb4'REJECTED',_utf8mb4'CANCELLED')))
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dorm_visitor_registrations`
--

LOCK TABLES `dorm_visitor_registrations` WRITE;
/*!40000 ALTER TABLE `dorm_visitor_registrations` DISABLE KEYS */;
INSERT INTO `dorm_visitor_registrations` VALUES (1,1,1,'王丽华','320102197505124521','13905167788','家长来校探望，顺便送换季衣物和被褥。','2026-09-12 10:00:00.000','2026-09-12 16:00:00.000','2026-09-11 09:01:08.000','PENDING',NULL,NULL,NULL),(2,1,1,'刘雨欣','320582200409185624','13851923366','高中同学来南京旅游，白天到宿舍坐一会儿，不留宿。','2026-09-05 13:00:00.000','2026-09-05 20:00:00.000','2026-09-03 15:01:08.000','APPROVED',7,'2026-09-04 15:01:08.000','已核对身份信息，来访人员请在 21:00 前离开宿舍区。'),(3,13,2,'李建国','320106197009087736','13705188899','父亲来校送生活费和药品。','2026-09-13 14:00:00.000','2026-09-13 17:00:00.000','2026-09-10 15:01:08.000','PENDING',NULL,NULL,NULL),(4,1,1,'人','111','0','玩','2026-09-22 14:00:00.000','2026-09-22 16:00:00.000','2026-09-13 17:22:34.360','CANCELLED',NULL,NULL,NULL);
/*!40000 ALTER TABLE `dorm_visitor_registrations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dorm_warning_configs`
--

DROP TABLE IF EXISTS `dorm_warning_configs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dorm_warning_configs` (
  `id` tinyint unsigned NOT NULL DEFAULT '1',
  `warn_days` int unsigned NOT NULL DEFAULT '3' COMMENT '连续未归几天开始预警',
  `notify_days` int unsigned NOT NULL DEFAULT '7' COMMENT '连续未归几天升为严重并通知辅导员',
  `exempt_on_leave` tinyint(1) NOT NULL DEFAULT '1' COMMENT '已批准的请假是否豁免预警',
  `updated_by` bigint unsigned DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `fk_dorm_warning_configs_actor` (`updated_by`),
  CONSTRAINT `fk_dorm_warning_configs_actor` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ck_dorm_warning_configs_days` CHECK (((`warn_days` > 0) and (`notify_days` >= `warn_days`))),
  CONSTRAINT `ck_dorm_warning_configs_singleton` CHECK ((`id` = 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dorm_warning_configs`
--

LOCK TABLES `dorm_warning_configs` WRITE;
/*!40000 ALTER TABLE `dorm_warning_configs` DISABLE KEYS */;
INSERT INTO `dorm_warning_configs` VALUES (1,3,7,1,NULL,'2026-09-04 13:46:45.169');
/*!40000 ALTER TABLE `dorm_warning_configs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `enrollments`
--

DROP TABLE IF EXISTS `enrollments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enrollments` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `student_user_id` bigint unsigned NOT NULL,
  `course_id` bigint unsigned NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ENROLLED',
  `enrolled_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `dropped_at` datetime(3) DEFAULT NULL,
  `version` int unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_enrollments_student_course` (`student_user_id`,`course_id`),
  KEY `idx_enrollments_course_status` (`course_id`,`status`),
  CONSTRAINT `fk_enrollments_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`),
  CONSTRAINT `fk_enrollments_student` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_enrollments_dropped_at` CHECK (((`status` <> _utf8mb4'DROPPED') or (`dropped_at` is not null))),
  CONSTRAINT `ck_enrollments_status` CHECK ((`status` in (_utf8mb4'ENROLLED',_utf8mb4'DROPPED',_utf8mb4'COMPLETED')))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `enrollments`
--

LOCK TABLES `enrollments` WRITE;
/*!40000 ALTER TABLE `enrollments` DISABLE KEYS */;
INSERT INTO `enrollments` VALUES (1,1,1,'COMPLETED','2026-09-04 13:46:23.327',NULL,0);
/*!40000 ALTER TABLE `enrollments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `hygiene_inspections`
--

DROP TABLE IF EXISTS `hygiene_inspections`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hygiene_inspections` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `room_id` bigint unsigned NOT NULL,
  `inspector_id` bigint unsigned NOT NULL,
  `inspected_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `score` decimal(5,2) NOT NULL,
  `result` varchar(20) NOT NULL,
  `issue_description` varchar(1000) DEFAULT NULL,
  `status` varchar(24) NOT NULL DEFAULT 'NORMAL',
  `rectified_at` datetime(3) DEFAULT NULL,
  `rectification_note` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_hygiene_inspections_room_time` (`room_id`,`inspected_at`),
  KEY `idx_hygiene_inspections_status` (`status`,`inspected_at`),
  KEY `fk_hygiene_inspections_inspector` (`inspector_id`),
  CONSTRAINT `fk_hygiene_inspections_inspector` FOREIGN KEY (`inspector_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_hygiene_inspections_room` FOREIGN KEY (`room_id`) REFERENCES `dorm_rooms` (`id`),
  CONSTRAINT `ck_hygiene_inspections_rectified_at` CHECK (((`status` <> _utf8mb4'RECTIFIED') or (`rectified_at` is not null))),
  CONSTRAINT `ck_hygiene_inspections_result` CHECK ((`result` in (_utf8mb4'PASS',_utf8mb4'FAIL'))),
  CONSTRAINT `ck_hygiene_inspections_score` CHECK ((`score` between 0 and 100)),
  CONSTRAINT `ck_hygiene_inspections_status` CHECK ((`status` in (_utf8mb4'NORMAL',_utf8mb4'RECTIFICATION_REQUIRED',_utf8mb4'RECTIFIED')))
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hygiene_inspections`
--

LOCK TABLES `hygiene_inspections` WRITE;
/*!40000 ALTER TABLE `hygiene_inspections` DISABLE KEYS */;
INSERT INTO `hygiene_inspections` VALUES (3,1,7,'2026-09-02 10:05:00.000',92.00,'PASS',NULL,'NORMAL',NULL,NULL),(4,1,7,'2026-09-09 10:20:00.000',64.00,'FAIL','地面有零食碎屑未清扫，两张书桌堆放外卖盒和杂物，卫生间镜面与台面水渍明显，垃圾桶未及时清理。','RECTIFICATION_REQUIRED',NULL,NULL),(5,2,7,'2026-09-02 10:30:00.000',58.00,'FAIL','地面积水未清理，垃圾未倒，阳台堆放大量纸箱。','RECTIFIED','2026-09-09 10:45:00.000','复查已通过，纸箱已清运。'),(6,2,7,'2026-09-09 10:45:00.000',82.00,'PASS',NULL,'NORMAL',NULL,NULL),(7,3,7,'2026-09-02 10:50:00.000',88.00,'PASS',NULL,'NORMAL',NULL,NULL),(8,1,7,'2026-09-13 17:30:40.919',100.00,'PASS','好','NORMAL',NULL,NULL),(9,5,7,'2026-09-13 17:30:50.823',90.00,'PASS','地面不干净','NORMAL',NULL,NULL);
/*!40000 ALTER TABLE `hygiene_inspections` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `late_return_alerts`
--

DROP TABLE IF EXISTS `late_return_alerts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `late_return_alerts` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `student_user_id` bigint unsigned NOT NULL,
  `alert_date` date NOT NULL,
  `detected_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `status` varchar(20) NOT NULL DEFAULT 'OPEN',
  `handled_by` bigint unsigned DEFAULT NULL,
  `handled_at` datetime(3) DEFAULT NULL,
  `note` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_late_return_alerts_student_date` (`student_user_id`,`alert_date`),
  KEY `idx_late_return_alerts_status` (`status`,`alert_date`),
  KEY `fk_late_return_alerts_handler` (`handled_by`),
  CONSTRAINT `fk_late_return_alerts_handler` FOREIGN KEY (`handled_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_late_return_alerts_student` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_late_return_alerts_handled_at` CHECK (((`status` = _utf8mb4'OPEN') or (`handled_at` is not null))),
  CONSTRAINT `ck_late_return_alerts_status` CHECK ((`status` in (_utf8mb4'OPEN',_utf8mb4'CONFIRMED',_utf8mb4'CLEARED',_utf8mb4'IGNORED')))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `late_return_alerts`
--

LOCK TABLES `late_return_alerts` WRITE;
/*!40000 ALTER TABLE `late_return_alerts` DISABLE KEYS */;
INSERT INTO `late_return_alerts` VALUES (4,1,'2026-09-09','2026-09-09 23:40:00.000','CLEARED',7,'2026-09-13 17:30:18.784',NULL),(5,15,'2026-09-09','2026-09-09 23:35:00.000','CONFIRMED',7,'2026-09-13 17:30:16.173','ok'),(6,11,'2026-09-06','2026-09-06 23:20:00.000','CLEARED',7,'2026-09-07 09:10:00.000','参加学院迎新晚会彩排晚归，辅导员已出具证明。');
/*!40000 ALTER TABLE `late_return_alerts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `leave_requests`
--

DROP TABLE IF EXISTS `leave_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `leave_requests` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `student_user_id` bigint unsigned NOT NULL,
  `leave_type` varchar(20) NOT NULL DEFAULT 'PERSONAL',
  `start_at` datetime(3) NOT NULL,
  `end_at` datetime(3) NOT NULL,
  `reason` varchar(500) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `reviewed_by` bigint unsigned DEFAULT NULL,
  `reviewed_at` datetime(3) DEFAULT NULL,
  `review_remark` varchar(500) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_leave_requests_student_status_time` (`student_user_id`,`status`,`start_at`),
  KEY `fk_leave_requests_reviewer` (`reviewed_by`),
  CONSTRAINT `fk_leave_requests_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_leave_requests_student` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_leave_requests_reviewed_at` CHECK (((`status` in (_utf8mb4'PENDING',_utf8mb4'CANCELLED')) or (`reviewed_at` is not null))),
  CONSTRAINT `ck_leave_requests_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'APPROVED',_utf8mb4'REJECTED',_utf8mb4'CANCELLED'))),
  CONSTRAINT `ck_leave_requests_time` CHECK ((`end_at` > `start_at`)),
  CONSTRAINT `ck_leave_requests_type` CHECK ((`leave_type` in (_utf8mb4'PERSONAL',_utf8mb4'ILLNESS',_utf8mb4'OFF_CAMPUS',_utf8mb4'OTHER')))
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `leave_requests`
--

LOCK TABLES `leave_requests` WRITE;
/*!40000 ALTER TABLE `leave_requests` DISABLE KEYS */;
INSERT INTO `leave_requests` VALUES (5,1,'PERSONAL','2026-09-18 18:00:00.000','2026-09-20 20:00:00.000','表姐下周六在镇江老家办婚礼，家里希望我回去帮忙，周五晚上回去，周日晚饭后返校。','REJECTED',7,'2026-09-13 17:27:46.031',NULL,'2026-09-11 10:01:08.000'),(6,1,'PERSONAL','2026-08-21 16:00:00.000','2026-08-23 21:00:00.000','回家参加外婆八十岁生日家宴。','APPROVED',7,'2026-08-19 15:01:08.000','已核实，注意往返路上安全，按时返校。','2026-08-18 15:01:08.000'),(7,1,'OTHER','2026-09-01 20:00:00.000','2026-09-02 08:00:00.000','室友生日，想和几个同学去新街口通宵唱歌庆祝。','REJECTED',7,'2026-08-31 15:01:08.000','非必要情况不批准通宵外出，请在门禁时间前返校。','2026-08-30 15:01:08.000'),(8,14,'OFF_CAMPUS','2026-08-28 08:00:00.000','2026-09-27 22:00:00.000','赴苏州一家软件企业参加为期一个月的校外实习，实习期间住企业安排的员工宿舍，实习证明已交辅导员。','APPROVED',7,'2026-08-27 15:01:08.000','已收到实习单位接收函，同意。实习期间保持联系方式畅通。','2026-08-26 15:01:08.000'),(9,11,'ILLNESS','2026-09-12 09:00:00.000','2026-09-12 18:00:00.000','明天上午去市区医院复查，下午返校。','APPROVED',7,'2026-09-13 17:27:42.804',NULL,'2026-09-11 13:01:08.000'),(10,1,'PERSONAL','2026-09-10 00:00:00.000','2026-09-17 23:59:00.000','开会','CANCELLED',NULL,NULL,NULL,'2026-09-13 17:21:47.232');
/*!40000 ALTER TABLE `leave_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `login_audits`
--

DROP TABLE IF EXISTS `login_audits`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `login_audits` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned DEFAULT NULL,
  `username_snapshot` varchar(64) NOT NULL,
  `role_id` bigint unsigned DEFAULT NULL COMMENT 'Role selected for a successful login, if any',
  `result_code` varchar(80) NOT NULL,
  `client_ip` varchar(64) DEFAULT NULL,
  `occurred_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_login_audits_user_time` (`user_id`,`occurred_at`),
  KEY `idx_login_audits_username_time` (`username_snapshot`,`occurred_at`),
  KEY `fk_login_audits_role` (`role_id`),
  CONSTRAINT `fk_login_audits_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_login_audits_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `login_audits`
--

LOCK TABLES `login_audits` WRITE;
/*!40000 ALTER TABLE `login_audits` DISABLE KEYS */;
INSERT INTO `login_audits` VALUES (1,1,'demo_student',1,'OK',NULL,'2026-09-04 14:03:47.770'),(2,1,'demo_student',1,'OK',NULL,'2026-09-04 14:09:42.744'),(3,1,'demo_student',1,'OK',NULL,'2026-09-04 14:25:15.638'),(4,1,'demo_student',1,'OK',NULL,'2026-09-04 14:26:28.015'),(5,1,'demo_student',1,'OK',NULL,'2026-09-04 14:28:59.291'),(6,1,'demo_student',1,'OK',NULL,'2026-09-04 14:34:30.189'),(7,1,'demo_student',1,'OK',NULL,'2026-09-04 14:40:00.771'),(8,1,'demo_student',1,'OK',NULL,'2026-09-04 14:49:21.964'),(9,1,'demo_student',1,'OK',NULL,'2026-09-04 16:02:19.071'),(10,1,'demo_student',1,'OK',NULL,'2026-09-04 16:54:18.182'),(11,7,'demo_dorm',7,'OK',NULL,'2026-09-04 16:55:13.773'),(12,7,'demo_dorm',7,'OK',NULL,'2026-09-04 17:00:50.156'),(13,1,'demo_student',1,'OK',NULL,'2026-09-04 17:35:05.547'),(14,1,'demo_student',1,'OK',NULL,'2026-09-04 19:21:37.710'),(15,7,'demo_dorm',7,'OK',NULL,'2026-09-04 19:25:51.298'),(16,1,'demo_student',1,'OK',NULL,'2026-09-05 00:01:26.490'),(17,7,'demo_dorm',7,'OK',NULL,'2026-09-05 00:02:39.055'),(18,10,'demo_repair',10,'OK',NULL,'2026-09-05 00:05:39.851'),(19,7,'demo_dorm',7,'OK',NULL,'2026-09-05 00:11:22.256'),(20,1,'demo_student',1,'OK',NULL,'2026-09-05 00:24:26.092'),(21,10,'demo_repair',10,'OK',NULL,'2026-09-05 14:48:26.620'),(22,1,'demo_student',1,'OK',NULL,'2026-09-05 14:48:57.604'),(23,1,'demo_student',1,'OK',NULL,'2026-09-05 14:51:17.470'),(24,10,'demo_repair',10,'OK',NULL,'2026-09-05 14:58:39.981'),(25,1,'demo_student',1,'OK',NULL,'2026-09-05 15:04:32.785'),(26,7,'demo_dorm',7,'OK',NULL,'2026-09-05 15:04:50.299'),(27,1,'demo_student',1,'OK',NULL,'2026-09-05 15:19:42.550'),(28,7,'demo_dorm',7,'OK',NULL,'2026-09-05 15:20:30.348'),(29,1,'demo_student',1,'OK',NULL,'2026-09-05 15:48:51.361'),(30,7,'demo_dorm',7,'OK',NULL,'2026-09-05 15:50:08.033'),(31,1,'demo_student',1,'OK',NULL,'2026-09-05 15:51:41.361'),(32,10,'demo_repair',10,'OK',NULL,'2026-09-05 15:52:00.065'),(33,7,'demo_dorm',7,'OK',NULL,'2026-09-05 15:52:51.396'),(34,1,'demo_student',1,'OK',NULL,'2026-09-05 16:10:33.895'),(35,7,'demo_dorm',7,'OK',NULL,'2026-09-05 16:24:22.492'),(36,10,'demo_repair',10,'OK',NULL,'2026-09-05 16:35:40.870'),(37,1,'demo_student',1,'OK',NULL,'2026-09-05 16:39:06.710'),(38,1,'demo_student',1,'OK',NULL,'2026-09-05 17:21:38.089'),(39,7,'demo_dorm',7,'OK',NULL,'2026-09-05 17:22:50.688'),(40,1,'demo_student',NULL,'AUTH.INVALID_CREDENTIALS',NULL,'2026-09-05 17:23:19.225'),(41,1,'demo_student',1,'OK',NULL,'2026-09-05 17:23:23.487'),(42,7,'demo_dorm',7,'OK',NULL,'2026-09-05 17:23:55.224'),(43,10,'demo_repair',10,'OK',NULL,'2026-09-05 17:26:52.254'),(44,1,'demo_student',1,'OK',NULL,'2026-09-05 18:28:29.867'),(45,7,'demo_dorm',NULL,'AUTH.INVALID_CREDENTIALS',NULL,'2026-09-05 18:29:43.823'),(46,7,'demo_dorm',7,'OK',NULL,'2026-09-05 18:29:48.982'),(47,10,'demo_repair',10,'OK',NULL,'2026-09-05 18:33:02.080'),(48,7,'demo_dorm',7,'OK',NULL,'2026-09-05 18:33:36.854'),(49,1,'demo_student',1,'OK',NULL,'2026-09-05 18:34:04.602'),(50,1,'demo_student',1,'OK',NULL,'2026-09-05 18:54:21.122'),(51,7,'demo_dorm',7,'OK',NULL,'2026-09-05 18:55:39.183'),(52,1,'demo_student',1,'OK',NULL,'2026-09-05 18:56:11.687'),(53,7,'demo_dorm',7,'OK',NULL,'2026-09-05 18:56:36.571'),(54,7,'demo_dorm',7,'OK',NULL,'2026-09-05 19:29:02.984'),(55,1,'demo_student',1,'OK',NULL,'2026-09-05 19:29:46.461'),(56,1,'demo_student',NULL,'AUTH.INVALID_CREDENTIALS',NULL,'2026-09-05 19:34:42.365'),(57,1,'demo_student',1,'OK',NULL,'2026-09-05 19:34:44.851'),(58,7,'demo_dorm',7,'OK',NULL,'2026-09-05 19:35:29.936'),(59,1,'demo_student',1,'OK',NULL,'2026-09-05 19:36:38.385'),(60,7,'demo_dorm',7,'OK',NULL,'2026-09-05 19:37:08.591'),(61,1,'demo_student',1,'OK',NULL,'2026-09-07 15:40:19.652'),(62,1,'demo_student',1,'OK',NULL,'2026-09-11 14:27:37.250'),(63,7,'demo_dorm',7,'OK',NULL,'2026-09-11 14:30:00.752'),(64,1,'demo_student',1,'OK',NULL,'2026-09-11 14:32:44.481'),(65,7,'demo_dorm',7,'OK',NULL,'2026-09-11 14:35:51.113'),(66,10,'demo_repair',10,'OK',NULL,'2026-09-11 14:51:55.723'),(67,7,'demo_dorm',7,'OK',NULL,'2026-09-11 14:52:57.579'),(68,7,'demo_dorm',7,'OK',NULL,'2026-09-11 15:01:31.973'),(69,7,'demo_dorm',7,'OK',NULL,'2026-09-11 15:20:38.038'),(70,10,'demo_repair',10,'OK',NULL,'2026-09-11 15:23:57.272'),(71,1,'demo_student',1,'OK',NULL,'2026-09-11 15:24:07.476'),(72,1,'demo_student',1,'OK',NULL,'2026-09-11 15:24:52.854'),(73,7,'demo_dorm',7,'OK',NULL,'2026-09-11 15:29:59.158'),(74,1,'demo_student',1,'OK',NULL,'2026-09-13 13:53:10.162'),(75,7,'demo_dorm',7,'OK',NULL,'2026-09-13 13:55:39.438'),(76,10,'demo_repair',10,'OK',NULL,'2026-09-13 14:02:27.890'),(77,1,'demo_student',1,'OK',NULL,'2026-09-13 14:03:57.656'),(78,1,'demo_student',1,'OK',NULL,'2026-09-13 14:25:05.528'),(79,10,'demo_repair',10,'OK',NULL,'2026-09-13 14:26:12.733'),(80,7,'demo_dorm',7,'OK',NULL,'2026-09-13 14:26:23.206'),(81,1,'demo_student',1,'OK',NULL,'2026-09-13 14:30:23.694'),(82,1,'demo_student',1,'OK',NULL,'2026-09-13 15:44:07.449'),(83,7,'demo_dorm',7,'OK',NULL,'2026-09-13 15:44:43.247'),(84,7,'demo_dorm',7,'OK',NULL,'2026-09-13 15:58:42.196'),(85,7,'demo_dorm',7,'OK',NULL,'2026-09-13 15:59:35.373'),(86,7,'demo_dorm',7,'OK',NULL,'2026-09-13 16:00:42.099'),(87,7,'demo_dorm',7,'OK',NULL,'2026-09-13 16:27:20.997'),(88,7,'demo_dorm',7,'OK',NULL,'2026-09-13 16:52:57.206'),(89,7,'demo_dorm',7,'OK',NULL,'2026-09-13 17:01:09.735'),(90,1,'demo_student',1,'OK',NULL,'2026-09-13 17:20:50.997'),(91,7,'demo_dorm',7,'OK',NULL,'2026-09-13 17:24:56.154'),(92,10,'demo_repair',10,'OK',NULL,'2026-09-13 17:32:32.923'),(93,1,'demo_student',1,'OK',NULL,'2026-09-13 17:33:26.555'),(94,1,'demo_student',NULL,'AUTH.INVALID_CREDENTIALS',NULL,'2026-09-13 18:48:51.416'),(95,1,'demo_student',1,'OK',NULL,'2026-09-13 18:48:54.238'),(96,10,'demo_repair',NULL,'AUTH.INVALID_CREDENTIALS',NULL,'2026-09-13 19:00:06.581'),(97,10,'demo_repair',10,'OK',NULL,'2026-09-13 19:00:09.084'),(98,7,'demo_dorm',7,'OK',NULL,'2026-09-13 19:01:19.513'),(99,1,'demo_student',1,'OK',NULL,'2026-09-13 19:04:07.900'),(100,7,'demo_dorm',7,'OK',NULL,'2026-09-13 19:04:40.116'),(101,1,'demo_student',1,'OK',NULL,'2026-09-14 14:12:13.602');
/*!40000 ALTER TABLE `login_audits` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `online_resource_access_logs`
--

DROP TABLE IF EXISTS `online_resource_access_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `online_resource_access_logs` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `resource_id` bigint unsigned NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `accessed_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `client_ip` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_resource_access_logs_resource_time` (`resource_id`,`accessed_at`),
  KEY `idx_resource_access_logs_user_time` (`user_id`,`accessed_at`),
  CONSTRAINT `fk_resource_access_logs_resource` FOREIGN KEY (`resource_id`) REFERENCES `online_resources` (`id`),
  CONSTRAINT `fk_resource_access_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `online_resource_access_logs`
--

LOCK TABLES `online_resource_access_logs` WRITE;
/*!40000 ALTER TABLE `online_resource_access_logs` DISABLE KEYS */;
INSERT INTO `online_resource_access_logs` VALUES (1,1,1,'2026-08-29 10:00:00.000','127.0.0.1'),(2,1,1,'2026-09-04 15:44:26.913',NULL);
/*!40000 ALTER TABLE `online_resource_access_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `online_resources`
--

DROP TABLE IF EXISTS `online_resources`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `online_resources` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `title` varchar(240) NOT NULL,
  `resource_type` varchar(40) NOT NULL,
  `url` varchar(1000) NOT NULL,
  `description` text,
  `publisher_id` bigint unsigned NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `published_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_online_resources_type_status` (`resource_type`,`status`,`published_at`),
  KEY `fk_online_resources_publisher` (`publisher_id`),
  CONSTRAINT `fk_online_resources_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_online_resources_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'INACTIVE')))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `online_resources`
--

LOCK TABLES `online_resources` WRITE;
/*!40000 ALTER TABLE `online_resources` DISABLE KEYS */;
INSERT INTO `online_resources` VALUES (1,'MySQL 8参考文档','DOCUMENTATION','https://dev.mysql.com/doc/','用于演示线上资源启停和访问记录。',5,'ACTIVE','2026-08-01 09:00:00.000','2026-09-04 13:46:23.355','2026-09-04 13:46:23.355');
/*!40000 ALTER TABLE `online_resources` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `permissions`
--

DROP TABLE IF EXISTS `permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permissions` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(80) NOT NULL,
  `display_name` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permissions_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `permissions`
--

LOCK TABLES `permissions` WRITE;
/*!40000 ALTER TABLE `permissions` DISABLE KEYS */;
INSERT INTO `permissions` VALUES (1,'PROFILE_READ','查看个人资料','读取当前用户公共资料','2026-09-04 13:46:23.298'),(2,'PROFILE_UPDATE','修改个人资料','修改当前用户可编辑资料','2026-09-04 13:46:23.298'),(3,'STUDENT_RECORD_SELF_READ','查看本人学籍','学生读取本人学籍档案','2026-09-04 13:46:23.298'),(4,'STUDENT_RECORD_MANAGE','管理学籍档案','学籍管理员维护学生档案','2026-09-04 13:46:23.298'),(5,'SCORE_SELF_READ','查看本人成绩','学生读取本人成绩','2026-09-04 13:46:23.298'),(6,'SCORE_RECORD','登记或核对成绩','教师登记本人课程成绩或管理员核对','2026-09-04 13:46:23.298'),(7,'COURSE_READ','查看课程','查询课程和课程时段','2026-09-04 13:46:23.298'),(8,'COURSE_MANAGE','管理课程','教务老师维护课程与排课','2026-09-04 13:46:23.298'),(9,'COURSE_ENROLL','选退课程','学生办理选课和退课','2026-09-04 13:46:23.298'),(10,'COURSE_TEACH','授课管理','教师查看本人授课课程','2026-09-04 13:46:23.298'),(11,'ANNOUNCEMENT_READ','查看公告','读取有权范围内的公告','2026-09-04 13:46:23.298'),(12,'ANNOUNCEMENT_MANAGE','管理公告','维护所属业务公告','2026-09-04 13:46:23.298'),(13,'COMPETITION_ENROLL','报名比赛','学生报名或取消比赛','2026-09-04 13:46:23.298'),(14,'COMPETITION_MANAGE','管理比赛','教务老师维护比赛与报名名单','2026-09-04 13:46:23.298'),(15,'SRTP_SELF_READ','查看本人SRTP','学生查看本人SRTP记录','2026-09-04 13:46:23.298'),(16,'SRTP_MANAGE','管理SRTP','教务老师登记与审核SRTP','2026-09-04 13:46:23.298'),(17,'CLASSROOM_RESERVE','申请教室','学生或教师提交教室申请','2026-09-04 13:46:23.298'),(18,'CLASSROOM_APPROVE','审批教室','教务老师审批教室申请','2026-09-04 13:46:23.298'),(19,'LIBRARY_READ','查看图书馆','查询图书、资源和自习室','2026-09-04 13:46:23.298'),(20,'LIBRARY_BORROW','借还图书','办理本人借书、还书和预约','2026-09-04 13:46:23.298'),(21,'LIBRARY_MANAGE','管理图书馆','图书管理员维护馆藏和资源','2026-09-04 13:46:23.298'),(22,'STUDY_ROOM_RESERVE','预约自习室','用户预约和取消自习室','2026-09-04 13:46:23.298'),(23,'STUDY_ROOM_MANAGE','管理自习室','图书管理员维护自习室','2026-09-04 13:46:23.298'),(24,'STORE_READ','查看商店','查询商品与订单状态','2026-09-04 13:46:23.298'),(25,'STORE_PURCHASE','购买商品','创建订单并完成支付','2026-09-04 13:46:23.298'),(26,'STORE_MANAGE','管理商店','维护商品、库存和订单','2026-09-04 13:46:23.298'),(27,'STORE_SALES_READ','查看销售统计','查询已支付订单统计','2026-09-04 13:46:23.298'),(28,'DORM_SELF_READ','查看本人住宿','查看本人住宿、门禁和账单','2026-09-04 13:46:23.298'),(29,'DORM_REQUEST','提交宿舍申请','提交入住、调宿、退宿、请假和报修','2026-09-04 13:46:23.298'),(30,'DORM_BILL_PAY','缴纳水电费','支付本人水电分摊','2026-09-04 13:46:23.298'),(31,'DORM_MANAGE','管理宿舍基础数据','维护楼栋、房间、床位等','2026-09-04 13:46:23.298'),(32,'DORM_APPROVE','审批住宿事务','审批入住、调宿、退宿、请假','2026-09-04 13:46:23.298'),(33,'DORM_GOVERN','处理宿舍治理','处理门禁、卫生、报修和水电','2026-09-04 13:46:23.298'),(34,'AI_QUERY','使用AI助手','创建会话和查询授权数据','2026-09-04 13:46:23.298'),(35,'AI_KNOWLEDGE_MANAGE','管理AI知识库','维护知识片段','2026-09-04 13:46:23.298'),(36,'USER_MANAGE','管理用户','维护账号状态','2026-09-04 13:46:23.298'),(37,'ROLE_MANAGE','管理角色权限','分配和撤销用户角色','2026-09-04 13:46:23.298'),(38,'SYSTEM_MONITOR','查看系统状态','查看运行和AI调用日志','2026-09-04 13:46:23.298'),(39,'DORM_REPAIR_WORK','宿舍报修处理','接单、推进和完成宿舍报修工单','2026-09-04 22:44:51.452'),(40,'SCORE_AUDIT','核对成绩','教务管理员核对课程成绩','2026-09-13 13:42:32.165');
/*!40000 ALTER TABLE `permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `sku` varchar(64) NOT NULL,
  `name` varchar(200) NOT NULL,
  `category` varchar(80) DEFAULT NULL,
  `category_code` varchar(40) DEFAULT NULL,
  `description` text,
  `price` decimal(12,2) NOT NULL,
  `stock_qty` int unsigned NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `image_url` varchar(1000) DEFAULT NULL,
  `rating_average` decimal(4,2) NOT NULL DEFAULT '0.00',
  `rating_count` int unsigned NOT NULL DEFAULT '0',
  `created_by` bigint unsigned DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `version` int unsigned NOT NULL DEFAULT '0',
  `image_data` mediumblob,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_products_sku` (`sku`),
  KEY `idx_products_search` (`category`,`status`,`name`),
  KEY `fk_products_created_by` (`created_by`),
  KEY `idx_products_category_code` (`category_code`),
  CONSTRAINT `fk_products_category_code` FOREIGN KEY (`category_code`) REFERENCES `store_categories` (`code`),
  CONSTRAINT `fk_products_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ck_products_price` CHECK ((`price` > 0)),
  CONSTRAINT `ck_products_status` CHECK ((`status` in (_utf8mb4'DRAFT',_utf8mb4'ON_SALE',_utf8mb4'OFF_SALE',_utf8mb4'ARCHIVED')))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `products`
--

LOCK TABLES `products` WRITE;
/*!40000 ALTER TABLE `products` DISABLE KEYS */;
INSERT INTO `products` VALUES (1,'DEMO-CUP-001','VCampus纪念马克杯','文创','CULTURE','用于演示商品、购物车、库存与订单。',12.50,19,'ON_SALE',NULL,0.00,0,6,'2026-09-04 13:46:23.367','2026-09-04 13:46:38.145',0,NULL);
/*!40000 ALTER TABLE `products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `repair_orders`
--

DROP TABLE IF EXISTS `repair_orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `repair_orders` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `room_id` bigint unsigned NOT NULL,
  `reporter_id` bigint unsigned NOT NULL,
  `category` varchar(40) NOT NULL,
  `description` varchar(1000) NOT NULL,
  `priority` varchar(12) NOT NULL DEFAULT 'NORMAL',
  `status` varchar(20) NOT NULL DEFAULT 'SUBMITTED',
  `handler_id` bigint unsigned DEFAULT NULL,
  `submitted_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `accepted_at` datetime(3) DEFAULT NULL,
  `completed_at` datetime(3) DEFAULT NULL,
  `evaluation_score` tinyint unsigned DEFAULT NULL,
  `evaluation_note` varchar(500) DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_repair_orders_room_status` (`room_id`,`status`,`submitted_at`),
  KEY `idx_repair_orders_queue` (`status`,`priority`,`submitted_at`),
  KEY `fk_repair_orders_reporter` (`reporter_id`),
  KEY `fk_repair_orders_handler` (`handler_id`),
  CONSTRAINT `fk_repair_orders_handler` FOREIGN KEY (`handler_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_repair_orders_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_repair_orders_room` FOREIGN KEY (`room_id`) REFERENCES `dorm_rooms` (`id`),
  CONSTRAINT `ck_repair_orders_completed_at` CHECK (((`status` <> _utf8mb4'COMPLETED') or (`completed_at` is not null))),
  CONSTRAINT `ck_repair_orders_priority` CHECK ((`priority` in (_utf8mb4'LOW',_utf8mb4'NORMAL',_utf8mb4'HIGH',_utf8mb4'URGENT'))),
  CONSTRAINT `ck_repair_orders_score` CHECK (((`evaluation_score` is null) or (`evaluation_score` between 1 and 5))),
  CONSTRAINT `ck_repair_orders_status` CHECK ((`status` in (_utf8mb4'SUBMITTED',_utf8mb4'ACCEPTED',_utf8mb4'IN_PROGRESS',_utf8mb4'PENDING_REVIEW',_utf8mb4'COMPLETED',_utf8mb4'CANCELLED')))
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `repair_orders`
--

LOCK TABLES `repair_orders` WRITE;
/*!40000 ALTER TABLE `repair_orders` DISABLE KEYS */;
INSERT INTO `repair_orders` VALUES (8,1,1,'ELECTRICAL','书桌上方的阅读灯接触不良，开关要按好几次才亮，有时用着用着会自己灭。','NORMAL','SUBMITTED',NULL,'2026-09-11 10:01:08.000',NULL,NULL,NULL,NULL,'2026-09-11 15:01:08.705'),(9,2,13,'PLUMBING','卫生间地漏堵塞，洗澡时积水会漫到门口，已经影响正常使用。','HIGH','SUBMITTED',NULL,'2026-09-11 13:01:08.000',NULL,NULL,NULL,NULL,'2026-09-11 15:01:08.705'),(10,1,1,'FURNITURE','衣柜左侧柜门铰链松动，柜门关不严，一碰就自己弹开。','NORMAL','PENDING_REVIEW',10,'2026-09-10 15:01:08.000','2026-09-10 19:01:08.000',NULL,NULL,NULL,'2026-09-13 17:33:02.446'),(11,1,11,'APPLIANCE','空调制冷效果很差，出风口有持续的异响，晚上开着睡不着。','HIGH','IN_PROGRESS',10,'2026-09-09 15:01:08.000','2026-09-09 23:01:08.000',NULL,NULL,NULL,'2026-09-11 15:01:08.705'),(12,1,1,'DOOR_WINDOW','靠窗一侧的窗户锁扣损坏，窗户关不严，下雨天会渗水到书桌上。','HIGH','COMPLETED',10,'2026-09-08 15:01:08.000','2026-09-09 03:01:08.000','2026-09-13 17:28:17.784',NULL,NULL,'2026-09-13 17:28:17.784'),(13,1,1,'PLUMBING','洗手池水龙头关不紧，一直滴水，夜里声音很明显。','NORMAL','COMPLETED',10,'2026-09-05 15:01:08.000','2026-09-06 15:01:08.000','2026-09-08 15:01:08.000',4,'还行','2026-09-13 17:23:32.606'),(14,1,1,'NETWORK','墙上的网口松动，插上网线没有信号，换了网线也不行。','NORMAL','COMPLETED',10,'2026-08-27 15:01:08.000','2026-08-28 15:01:08.000','2026-08-29 15:01:08.000',5,'师傅来得很快，修好后还顺手帮我把线理整齐了。','2026-09-11 15:01:08.705'),(15,2,15,'FURNITURE','床板有一块松动，翻身会响。','LOW','CANCELLED',NULL,'2026-09-03 15:01:08.000',NULL,NULL,NULL,NULL,'2026-09-11 15:01:08.705'),(16,1,1,'FURNITURE','椅子坏了','NORMAL','PENDING_REVIEW',10,'2026-09-13 17:23:16.281','2026-09-13 17:28:03.489',NULL,NULL,NULL,'2026-09-13 17:32:55.303');
/*!40000 ALTER TABLE `repair_orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role_permissions`
--

DROP TABLE IF EXISTS `role_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_permissions` (
  `role_id` bigint unsigned NOT NULL,
  `permission_id` bigint unsigned NOT NULL,
  `granted_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`role_id`,`permission_id`),
  KEY `idx_role_permissions_permission` (`permission_id`,`role_id`),
  CONSTRAINT `fk_role_permissions_permission` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`),
  CONSTRAINT `fk_role_permissions_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_permissions`
--

LOCK TABLES `role_permissions` WRITE;
/*!40000 ALTER TABLE `role_permissions` DISABLE KEYS */;
INSERT INTO `role_permissions` VALUES (1,1,'2026-09-04 13:46:23.301'),(1,2,'2026-09-04 13:46:23.301'),(1,3,'2026-09-04 13:46:23.301'),(1,5,'2026-09-04 13:46:23.301'),(1,7,'2026-09-04 13:46:23.301'),(1,9,'2026-09-04 13:46:23.301'),(1,11,'2026-09-04 13:46:23.301'),(1,13,'2026-09-04 13:46:23.301'),(1,15,'2026-09-04 13:46:23.301'),(1,17,'2026-09-04 13:46:23.301'),(1,19,'2026-09-04 13:46:23.301'),(1,20,'2026-09-04 13:46:23.301'),(1,22,'2026-09-04 13:46:23.301'),(1,24,'2026-09-04 13:46:23.301'),(1,25,'2026-09-04 13:46:23.301'),(1,28,'2026-09-04 13:46:23.301'),(1,29,'2026-09-04 13:46:23.301'),(1,30,'2026-09-04 13:46:23.301'),(1,34,'2026-09-04 13:46:23.301'),(2,1,'2026-09-04 13:46:23.303'),(2,2,'2026-09-04 13:46:23.303'),(2,6,'2026-09-04 13:46:23.303'),(2,7,'2026-09-04 13:46:23.303'),(2,10,'2026-09-04 13:46:23.303'),(2,11,'2026-09-04 13:46:23.303'),(2,17,'2026-09-04 13:46:23.303'),(2,19,'2026-09-04 13:46:23.303'),(3,1,'2026-09-04 13:46:23.304'),(3,2,'2026-09-04 13:46:23.304'),(3,4,'2026-09-04 13:46:23.304'),(3,11,'2026-09-04 13:46:23.304'),(4,1,'2026-09-04 13:46:23.305'),(4,2,'2026-09-04 13:46:23.305'),(4,7,'2026-09-04 13:46:23.305'),(4,8,'2026-09-04 13:46:23.305'),(4,12,'2026-09-04 13:46:23.305'),(4,14,'2026-09-04 13:46:23.305'),(4,16,'2026-09-04 13:46:23.305'),(4,18,'2026-09-04 13:46:23.305'),(4,40,'2026-09-13 13:42:32.279'),(5,1,'2026-09-04 13:46:23.305'),(5,2,'2026-09-04 13:46:23.305'),(5,12,'2026-09-04 13:46:23.305'),(5,19,'2026-09-04 13:46:23.305'),(5,21,'2026-09-04 13:46:23.305'),(5,23,'2026-09-04 13:46:23.305'),(6,1,'2026-09-04 13:46:23.306'),(6,2,'2026-09-04 13:46:23.306'),(6,24,'2026-09-04 13:46:23.306'),(6,26,'2026-09-04 13:46:23.306'),(6,27,'2026-09-04 13:46:23.306'),(7,1,'2026-09-04 13:46:23.307'),(7,2,'2026-09-04 13:46:23.307'),(7,12,'2026-09-04 13:46:23.307'),(7,31,'2026-09-04 13:46:23.307'),(7,32,'2026-09-04 13:46:23.307'),(7,33,'2026-09-04 13:46:23.307'),(8,1,'2026-09-04 13:46:23.307'),(8,2,'2026-09-04 13:46:23.307'),(8,35,'2026-09-04 13:46:23.307'),(8,38,'2026-09-04 13:46:23.307'),(9,1,'2026-09-04 13:46:23.308'),(9,2,'2026-09-04 13:46:23.308'),(9,36,'2026-09-04 13:46:23.308'),(9,37,'2026-09-04 13:46:23.308'),(9,38,'2026-09-04 13:46:23.308'),(10,1,'2026-09-04 22:44:51.461'),(10,2,'2026-09-04 22:44:51.461'),(10,39,'2026-09-04 22:44:51.461');
/*!40000 ALTER TABLE `role_permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(40) NOT NULL,
  `display_name` varchar(80) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_roles_code` (`code`),
  CONSTRAINT `ck_roles_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'DISABLED')))
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES (1,'STUDENT','学生','查看个人数据并办理校园业务','ACTIVE','2026-09-04 13:46:23.294','2026-09-04 13:46:23.294'),(2,'TEACHER','任课教师','查看授课课程并登记本人课程成绩','ACTIVE','2026-09-04 13:46:23.294','2026-09-04 13:46:23.294'),(3,'REGISTRAR','学籍管理员','维护学生学籍与成绩档案','ACTIVE','2026-09-04 13:46:23.294','2026-09-04 13:46:23.294'),(4,'ACADEMIC_ADMIN','教务老师','维护课程、教务公告、比赛、SRTP与教室审批','ACTIVE','2026-09-04 13:46:23.294','2026-09-04 13:46:23.294'),(5,'LIBRARIAN','图书管理员','维护图书、借还、自习室与线上资源','ACTIVE','2026-09-04 13:46:23.294','2026-09-04 13:46:23.294'),(6,'STORE_MANAGER','商店管理员','维护商品、库存、订单与销售统计','ACTIVE','2026-09-04 13:46:23.294','2026-09-04 13:46:23.294'),(7,'DORM_MANAGER','宿管员','处理住宿、门禁、卫生、报修与水电业务','ACTIVE','2026-09-04 13:46:23.294','2026-09-04 13:46:23.294'),(8,'AI_KNOWLEDGE_ADMIN','AI知识管理员','维护AI知识片段并监控调用日志','ACTIVE','2026-09-04 13:46:23.294','2026-09-04 13:46:23.294'),(9,'SYSTEM_ADMIN','系统管理员','维护账号、角色和系统运行状态','ACTIVE','2026-09-04 13:46:23.294','2026-09-04 13:46:23.294'),(10,'REPAIR_WORKER','维修员','接单、上报维修进度并查看入内授权','ACTIVE','2026-09-04 22:44:51.414','2026-09-04 22:44:51.414');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shopping_carts`
--

DROP TABLE IF EXISTS `shopping_carts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shopping_carts` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shopping_carts_user` (`user_id`),
  CONSTRAINT `fk_shopping_carts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_shopping_carts_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'CLOSED')))
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shopping_carts`
--

LOCK TABLES `shopping_carts` WRITE;
/*!40000 ALTER TABLE `shopping_carts` DISABLE KEYS */;
INSERT INTO `shopping_carts` VALUES (1,1,'ACTIVE','2026-09-04 13:46:23.370','2026-09-04 13:46:23.370');
/*!40000 ALTER TABLE `shopping_carts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `srtp_records`
--

DROP TABLE IF EXISTS `srtp_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `srtp_records` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `project_code` varchar(64) NOT NULL,
  `student_user_id` bigint unsigned NOT NULL,
  `title` varchar(200) NOT NULL,
  `description` text,
  `credits` decimal(4,2) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'SUBMITTED',
  `submitted_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `reviewed_by` bigint unsigned DEFAULT NULL,
  `reviewed_at` datetime(3) DEFAULT NULL,
  `review_remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srtp_records_project_code` (`project_code`),
  KEY `idx_srtp_records_student_status` (`student_user_id`,`status`),
  KEY `idx_srtp_records_review` (`status`,`reviewed_at`),
  KEY `fk_srtp_records_reviewer` (`reviewed_by`),
  CONSTRAINT `fk_srtp_records_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_srtp_records_student` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_srtp_records_credits` CHECK (((`credits` is null) or (`credits` > 0))),
  CONSTRAINT `ck_srtp_records_reviewed_at` CHECK (((`status` in (_utf8mb4'SUBMITTED',_utf8mb4'CANCELLED')) or (`reviewed_at` is not null))),
  CONSTRAINT `ck_srtp_records_status` CHECK ((`status` in (_utf8mb4'SUBMITTED',_utf8mb4'APPROVED',_utf8mb4'REJECTED',_utf8mb4'CANCELLED')))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `srtp_records`
--

LOCK TABLES `srtp_records` WRITE;
/*!40000 ALTER TABLE `srtp_records` DISABLE KEYS */;
INSERT INTO `srtp_records` VALUES (1,'DEMO-SRTP-001',1,'虚拟校园服务体验优化','用于演示SRTP提交、审核和学生查询。',2.00,'APPROVED','2026-09-04 13:46:23.335',4,'2026-08-20 10:00:00.000','演示记录：审核通过。'),(2,'007',1,'论牛来对屈原的影响','神秘的文字',2.00,'SUBMITTED','2026-09-04 15:28:17.670',NULL,NULL,NULL);
/*!40000 ALTER TABLE `srtp_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `store_categories`
--

DROP TABLE IF EXISTS `store_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_categories` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(40) NOT NULL,
  `name` varchar(80) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_categories_code` (`code`),
  CONSTRAINT `ck_store_categories_code` CHECK ((char_length(trim(`code`)) > 0)),
  CONSTRAINT `ck_store_categories_name` CHECK ((char_length(trim(`name`)) > 0))
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `store_categories`
--

LOCK TABLES `store_categories` WRITE;
/*!40000 ALTER TABLE `store_categories` DISABLE KEYS */;
INSERT INTO `store_categories` VALUES (1,'DAILY','日用百货',1,'2026-09-04 13:46:38.133','2026-09-04 13:46:38.133'),(2,'FOOD','食品饮料',1,'2026-09-04 13:46:38.133','2026-09-04 13:46:38.133'),(3,'STATIONERY','文具用品',1,'2026-09-04 13:46:38.133','2026-09-04 13:46:38.133'),(4,'CULTURE','校园文创',1,'2026-09-04 13:46:38.133','2026-09-04 13:46:38.133'),(5,'OTHER','其他',1,'2026-09-04 13:46:38.133','2026-09-04 13:46:38.133');
/*!40000 ALTER TABLE `store_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `store_coupons`
--

DROP TABLE IF EXISTS `store_coupons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_coupons` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL,
  `name` varchar(120) NOT NULL,
  `threshold_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `discount_amount` decimal(12,2) NOT NULL,
  `expires_at` datetime(3) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_coupons_code` (`code`),
  CONSTRAINT `ck_store_coupons_amount` CHECK (((`threshold_amount` >= 0) and (`discount_amount` > 0)))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `store_coupons`
--

LOCK TABLES `store_coupons` WRITE;
/*!40000 ALTER TABLE `store_coupons` DISABLE KEYS */;
INSERT INTO `store_coupons` VALUES (1,'WELCOME10','新生优惠券',50.00,10.00,'2026-12-03 13:46:38.326',1,'2026-09-04 13:46:38.326');
/*!40000 ALTER TABLE `store_coupons` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `store_friend_payments`
--

DROP TABLE IF EXISTS `store_friend_payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_friend_payments` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `order_id` bigint unsigned NOT NULL,
  `buyer_id` bigint unsigned NOT NULL,
  `payer_id` bigint unsigned NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'PENDING',
  `message` varchar(500) DEFAULT NULL,
  `expires_at` datetime(3) NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `decided_at` datetime(3) DEFAULT NULL,
  `pending_order_id` bigint GENERATED ALWAYS AS ((case when (`status` = _utf8mb4'PENDING') then `order_id` else NULL end)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_friend_payment_order_pending` (`pending_order_id`),
  KEY `idx_store_friend_payment_payer_status` (`payer_id`,`status`,`created_at`),
  KEY `idx_store_friend_payment_buyer_status` (`buyer_id`,`status`,`created_at`),
  KEY `fk_store_friend_payment_order` (`order_id`),
  CONSTRAINT `fk_store_friend_payment_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_store_friend_payment_order` FOREIGN KEY (`order_id`) REFERENCES `store_orders` (`id`),
  CONSTRAINT `fk_store_friend_payment_payer` FOREIGN KEY (`payer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_store_friend_payment_amount` CHECK ((`amount` > 0)),
  CONSTRAINT `ck_store_friend_payment_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'ACCEPTED',_utf8mb4'REJECTED',_utf8mb4'WITHDRAWN',_utf8mb4'EXPIRED'))),
  CONSTRAINT `ck_store_friend_payment_users` CHECK ((`buyer_id` <> `payer_id`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `store_friend_payments`
--

LOCK TABLES `store_friend_payments` WRITE;
/*!40000 ALTER TABLE `store_friend_payments` DISABLE KEYS */;
/*!40000 ALTER TABLE `store_friend_payments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `store_order_items`
--

DROP TABLE IF EXISTS `store_order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_order_items` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `order_id` bigint unsigned NOT NULL,
  `product_id` bigint unsigned NOT NULL,
  `product_name_snapshot` varchar(200) NOT NULL,
  `unit_price_snapshot` decimal(12,2) NOT NULL,
  `quantity` int unsigned NOT NULL,
  `line_amount` decimal(12,2) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_order_items_order_product` (`order_id`,`product_id`),
  KEY `idx_store_order_items_product` (`product_id`),
  CONSTRAINT `fk_store_order_items_order` FOREIGN KEY (`order_id`) REFERENCES `store_orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_store_order_items_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `ck_store_order_items_amount` CHECK ((`line_amount` = (`unit_price_snapshot` * `quantity`))),
  CONSTRAINT `ck_store_order_items_price` CHECK ((`unit_price_snapshot` > 0)),
  CONSTRAINT `ck_store_order_items_quantity` CHECK ((`quantity` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `store_order_items`
--

LOCK TABLES `store_order_items` WRITE;
/*!40000 ALTER TABLE `store_order_items` DISABLE KEYS */;
INSERT INTO `store_order_items` VALUES (1,1,1,'VCampus纪念马克杯',12.50,1,12.50);
/*!40000 ALTER TABLE `store_order_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `store_orders`
--

DROP TABLE IF EXISTS `store_orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_orders` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) NOT NULL,
  `buyer_id` bigint unsigned NOT NULL,
  `total_amount` decimal(12,2) NOT NULL,
  `original_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `discount_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `promotion_code` varchar(64) DEFAULT NULL,
  `coupon_code` varchar(64) DEFAULT NULL,
  `payment_mode` varchar(16) NOT NULL DEFAULT 'SELF',
  `status` varchar(20) NOT NULL DEFAULT 'CREATED',
  `shipping_status` varchar(32) DEFAULT NULL,
  `tracking_no` varchar(80) DEFAULT NULL,
  `shipping_remark` varchar(500) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `paid_at` datetime(3) DEFAULT NULL,
  `cancelled_at` datetime(3) DEFAULT NULL,
  `completed_at` datetime(3) DEFAULT NULL,
  `version` int unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_orders_order_no` (`order_no`),
  KEY `idx_store_orders_buyer_status` (`buyer_id`,`status`,`created_at`),
  KEY `idx_store_orders_sales` (`status`,`paid_at`),
  CONSTRAINT `fk_store_orders_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_store_orders_cancelled_at` CHECK (((`status` <> _utf8mb4'CANCELLED') or (`cancelled_at` is not null))),
  CONSTRAINT `ck_store_orders_paid_at` CHECK (((`status` not in (_utf8mb4'PAID',_utf8mb4'REFUNDED',_utf8mb4'COMPLETED')) or (`paid_at` is not null))),
  CONSTRAINT `ck_store_orders_payment_mode` CHECK ((`payment_mode` in (_utf8mb4'SELF',_utf8mb4'FRIEND'))),
  CONSTRAINT `ck_store_orders_price_snapshot` CHECK (((`original_amount` >= 0) and (`discount_amount` >= 0) and (`discount_amount` <= `original_amount`) and (`total_amount` = (`original_amount` - `discount_amount`)))),
  CONSTRAINT `ck_store_orders_shipping_status` CHECK (((`shipping_status` is null) or (`shipping_status` in (_utf8mb4'PREPARING',_utf8mb4'SHIPPED',_utf8mb4'IN_TRANSIT',_utf8mb4'READY_FOR_PICKUP',_utf8mb4'DELIVERED')))),
  CONSTRAINT `ck_store_orders_status` CHECK ((`status` in (_utf8mb4'CREATED',_utf8mb4'PAID',_utf8mb4'CANCELLED',_utf8mb4'REFUNDED',_utf8mb4'COMPLETED'))),
  CONSTRAINT `ck_store_orders_total` CHECK ((`total_amount` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `store_orders`
--

LOCK TABLES `store_orders` WRITE;
/*!40000 ALTER TABLE `store_orders` DISABLE KEYS */;
INSERT INTO `store_orders` VALUES (1,'DEMO-ORDER-0001',1,12.50,12.50,0.00,NULL,NULL,'SELF','PAID',NULL,NULL,NULL,'2026-09-04 13:46:23.374','2026-08-29 10:05:00.000',NULL,NULL,0);
/*!40000 ALTER TABLE `store_orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `store_product_reviews`
--

DROP TABLE IF EXISTS `store_product_reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_product_reviews` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `product_id` bigint unsigned NOT NULL,
  `order_id` bigint unsigned NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `score` tinyint unsigned NOT NULL,
  `content` varchar(1000) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_review_order_product` (`order_id`,`product_id`),
  KEY `idx_store_review_product_time` (`product_id`,`created_at`),
  KEY `fk_store_review_user` (`user_id`),
  CONSTRAINT `fk_store_review_order` FOREIGN KEY (`order_id`) REFERENCES `store_orders` (`id`),
  CONSTRAINT `fk_store_review_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `fk_store_review_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_store_review_score` CHECK ((`score` between 1 and 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `store_product_reviews`
--

LOCK TABLES `store_product_reviews` WRITE;
/*!40000 ALTER TABLE `store_product_reviews` DISABLE KEYS */;
/*!40000 ALTER TABLE `store_product_reviews` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `store_promotions`
--

DROP TABLE IF EXISTS `store_promotions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_promotions` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL,
  `name` varchar(120) NOT NULL,
  `promotion_type` varchar(16) NOT NULL,
  `threshold_amount` decimal(12,2) DEFAULT NULL,
  `discount_value` decimal(12,2) NOT NULL,
  `product_scope` varchar(16) NOT NULL DEFAULT 'ALL',
  `product_id` bigint unsigned DEFAULT NULL,
  `category_code` varchar(40) DEFAULT NULL,
  `starts_at` datetime(3) NOT NULL,
  `ends_at` datetime(3) DEFAULT NULL,
  `stackable` tinyint(1) NOT NULL DEFAULT '0',
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_promotions_code` (`code`),
  KEY `idx_store_promotions_active_time` (`active`,`starts_at`,`ends_at`),
  KEY `fk_store_promotions_product` (`product_id`),
  KEY `fk_store_promotions_category` (`category_code`),
  CONSTRAINT `fk_store_promotions_category` FOREIGN KEY (`category_code`) REFERENCES `store_categories` (`code`),
  CONSTRAINT `fk_store_promotions_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `ck_store_promotions_dates` CHECK (((`ends_at` is null) or (`ends_at` > `starts_at`))),
  CONSTRAINT `ck_store_promotions_percent` CHECK (((`promotion_type` <> _utf8mb4'PERCENT') or (`discount_value` <= 100))),
  CONSTRAINT `ck_store_promotions_scope` CHECK ((`product_scope` in (_utf8mb4'ALL',_utf8mb4'PRODUCT',_utf8mb4'CATEGORY'))),
  CONSTRAINT `ck_store_promotions_scope_target` CHECK ((((`product_scope` = _utf8mb4'ALL') and (`product_id` is null) and (`category_code` is null)) or ((`product_scope` = _utf8mb4'PRODUCT') and (`product_id` is not null) and (`category_code` is null)) or ((`product_scope` = _utf8mb4'CATEGORY') and (`product_id` is null) and (`category_code` is not null)))),
  CONSTRAINT `ck_store_promotions_threshold` CHECK (((`threshold_amount` is null) or (`threshold_amount` >= 0))),
  CONSTRAINT `ck_store_promotions_type` CHECK ((`promotion_type` in (_utf8mb4'THRESHOLD',_utf8mb4'PERCENT',_utf8mb4'FIXED'))),
  CONSTRAINT `ck_store_promotions_value` CHECK ((`discount_value` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `store_promotions`
--

LOCK TABLES `store_promotions` WRITE;
/*!40000 ALTER TABLE `store_promotions` DISABLE KEYS */;
/*!40000 ALTER TABLE `store_promotions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `store_user_coupons`
--

DROP TABLE IF EXISTS `store_user_coupons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_user_coupons` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `coupon_id` bigint unsigned NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `used_at` datetime(3) DEFAULT NULL,
  `order_id` bigint unsigned DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_user_coupon` (`coupon_id`,`user_id`),
  KEY `idx_store_user_coupon_user` (`user_id`,`used_at`),
  KEY `fk_store_user_coupon_order` (`order_id`),
  CONSTRAINT `fk_store_user_coupon_coupon` FOREIGN KEY (`coupon_id`) REFERENCES `store_coupons` (`id`),
  CONSTRAINT `fk_store_user_coupon_order` FOREIGN KEY (`order_id`) REFERENCES `store_orders` (`id`),
  CONSTRAINT `fk_store_user_coupon_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `store_user_coupons`
--

LOCK TABLES `store_user_coupons` WRITE;
/*!40000 ALTER TABLE `store_user_coupons` DISABLE KEYS */;
INSERT INTO `store_user_coupons` VALUES (1,1,1,NULL,NULL,'2026-09-04 16:02:40.582');
/*!40000 ALTER TABLE `store_user_coupons` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_profiles`
--

DROP TABLE IF EXISTS `student_profiles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_profiles` (
  `user_id` bigint unsigned NOT NULL,
  `student_no` varchar(32) NOT NULL,
  `college` varchar(120) DEFAULT NULL,
  `major` varchar(120) DEFAULT NULL,
  `class_name` varchar(120) DEFAULT NULL,
  `enrollment_year` year DEFAULT NULL,
  `expected_graduation_year` year DEFAULT NULL,
  `degree_level` varchar(20) DEFAULT NULL,
  `gender` varchar(16) DEFAULT NULL,
  `birth_date` date DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `emergency_contact` varchar(100) DEFAULT NULL,
  `emergency_phone` varchar(32) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ENROLLED',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_student_profiles_student_no` (`student_no`),
  KEY `idx_student_profiles_search` (`college`,`major`,`class_name`,`status`),
  CONSTRAINT `fk_student_profiles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_student_profiles_status` CHECK ((`status` in (_utf8mb4'ENROLLED',_utf8mb4'SUSPENDED',_utf8mb4'GRADUATED',_utf8mb4'WITHDRAWN'))),
  CONSTRAINT `ck_student_profiles_years` CHECK (((`expected_graduation_year` is null) or (`enrollment_year` is null) or (`expected_graduation_year` >= `enrollment_year`)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_profiles`
--

LOCK TABLES `student_profiles` WRITE;
/*!40000 ALTER TABLE `student_profiles` DISABLE KEYS */;
INSERT INTO `student_profiles` VALUES (1,'DEMO2026001','计算机科学与工程学院','软件工程','软件工程2601',2026,2030,'UNDERGRADUATE','UNKNOWN',NULL,NULL,NULL,NULL,'ENROLLED','2026-09-04 13:46:23.319','2026-09-04 13:46:23.319');
/*!40000 ALTER TABLE `student_profiles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `study_room_reservations`
--

DROP TABLE IF EXISTS `study_room_reservations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_room_reservations` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `room_id` bigint unsigned NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `start_at` datetime(3) NOT NULL,
  `end_at` datetime(3) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'RESERVED',
  `cancelled_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_study_room_reservations_overlap` (`room_id`,`start_at`,`end_at`,`status`),
  KEY `idx_study_room_reservations_user` (`user_id`,`status`,`start_at`),
  CONSTRAINT `fk_study_room_reservations_room` FOREIGN KEY (`room_id`) REFERENCES `study_rooms` (`id`),
  CONSTRAINT `fk_study_room_reservations_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_study_room_reservations_cancelled_at` CHECK (((`status` <> _utf8mb4'CANCELLED') or (`cancelled_at` is not null))),
  CONSTRAINT `ck_study_room_reservations_status` CHECK ((`status` in (_utf8mb4'RESERVED',_utf8mb4'CANCELLED',_utf8mb4'COMPLETED',_utf8mb4'NO_SHOW'))),
  CONSTRAINT `ck_study_room_reservations_time` CHECK ((`end_at` > `start_at`))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `study_room_reservations`
--

LOCK TABLES `study_room_reservations` WRITE;
/*!40000 ALTER TABLE `study_room_reservations` DISABLE KEYS */;
INSERT INTO `study_room_reservations` VALUES (1,1,1,'2026-09-05 14:00:00.000','2026-09-05 16:00:00.000','RESERVED',NULL,'2026-09-04 13:46:23.351');
/*!40000 ALTER TABLE `study_room_reservations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `study_rooms`
--

DROP TABLE IF EXISTS `study_rooms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_rooms` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `building_name` varchar(120) NOT NULL,
  `room_no` varchar(40) NOT NULL,
  `capacity` int unsigned NOT NULL,
  `open_time` time NOT NULL,
  `close_time` time NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'OPEN',
  `description` varchar(500) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_study_rooms_building_room` (`building_name`,`room_no`),
  KEY `idx_study_rooms_status_capacity` (`status`,`capacity`),
  CONSTRAINT `ck_study_rooms_capacity` CHECK ((`capacity` > 0)),
  CONSTRAINT `ck_study_rooms_status` CHECK ((`status` in (_utf8mb4'OPEN',_utf8mb4'MAINTENANCE',_utf8mb4'CLOSED'))),
  CONSTRAINT `ck_study_rooms_time` CHECK ((`close_time` > `open_time`))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `study_rooms`
--

LOCK TABLES `study_rooms` WRITE;
/*!40000 ALTER TABLE `study_rooms` DISABLE KEYS */;
INSERT INTO `study_rooms` VALUES (1,'图书馆','研习室A',8,'08:00:00','22:00:00','OPEN','用于演示自习室预约和冲突检查。','2026-09-04 13:46:23.347','2026-09-04 13:46:23.347');
/*!40000 ALTER TABLE `study_rooms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teacher_profiles`
--

DROP TABLE IF EXISTS `teacher_profiles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_profiles` (
  `user_id` bigint unsigned NOT NULL,
  `employee_no` varchar(32) NOT NULL,
  `department` varchar(120) DEFAULT NULL,
  `title` varchar(80) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_teacher_profiles_employee_no` (`employee_no`),
  KEY `idx_teacher_profiles_department` (`department`,`status`),
  CONSTRAINT `fk_teacher_profiles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_teacher_profiles_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'INACTIVE')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teacher_profiles`
--

LOCK TABLES `teacher_profiles` WRITE;
/*!40000 ALTER TABLE `teacher_profiles` DISABLE KEYS */;
INSERT INTO `teacher_profiles` VALUES (2,'DEMO-T-001','计算机科学与工程学院','讲师','ACTIVE','2026-09-04 13:46:23.321','2026-09-04 13:46:23.321');
/*!40000 ALTER TABLE `teacher_profiles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teacher_time_preferences`
--

DROP TABLE IF EXISTS `teacher_time_preferences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_time_preferences` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `teacher_user_id` bigint unsigned NOT NULL,
  `weekday` tinyint unsigned NOT NULL,
  `start_period` tinyint unsigned NOT NULL,
  `end_period` tinyint unsigned NOT NULL,
  `preference_type` varchar(20) NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_time_preference_slot` (`teacher_user_id`,`weekday`,`start_period`,`end_period`),
  KEY `idx_teacher_time_preference_teacher` (`teacher_user_id`,`preference_type`),
  CONSTRAINT `fk_teacher_time_preference_teacher` FOREIGN KEY (`teacher_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ck_teacher_time_preference_period` CHECK (((`start_period` >= 1) and (`end_period` >= `start_period`))),
  CONSTRAINT `ck_teacher_time_preference_type` CHECK ((`preference_type` in (_utf8mb4'UNAVAILABLE',_utf8mb4'AVOID',_utf8mb4'PREFERRED'))),
  CONSTRAINT `ck_teacher_time_preference_weekday` CHECK ((`weekday` between 1 and 7))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teacher_time_preferences`
--

LOCK TABLES `teacher_time_preferences` WRITE;
/*!40000 ALTER TABLE `teacher_time_preferences` DISABLE KEYS */;
/*!40000 ALTER TABLE `teacher_time_preferences` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `user_id` bigint unsigned NOT NULL,
  `role_id` bigint unsigned NOT NULL,
  `assigned_by` bigint unsigned DEFAULT NULL,
  `assigned_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `idx_user_roles_role` (`role_id`,`user_id`),
  KEY `fk_user_roles_assigned_by` (`assigned_by`),
  CONSTRAINT `fk_user_roles_assigned_by` FOREIGN KEY (`assigned_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_user_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_roles`
--

LOCK TABLES `user_roles` WRITE;
/*!40000 ALTER TABLE `user_roles` DISABLE KEYS */;
INSERT INTO `user_roles` VALUES (1,1,9,'2026-09-04 13:46:23.315'),(2,2,9,'2026-09-04 13:46:23.316'),(2,4,9,'2026-09-04 13:46:23.316'),(3,3,9,'2026-09-04 13:46:23.316'),(4,4,9,'2026-09-04 13:46:23.317'),(5,5,9,'2026-09-04 13:46:23.317'),(6,6,9,'2026-09-04 13:46:23.318'),(7,7,9,'2026-09-04 13:46:23.318'),(8,8,9,'2026-09-04 13:46:23.318'),(9,9,NULL,'2026-09-04 13:46:23.318'),(10,10,9,'2026-09-04 22:44:51.491'),(11,1,9,'2026-09-05 18:49:49.792'),(12,1,9,'2026-09-05 18:49:49.792'),(13,1,9,'2026-09-05 18:49:49.792'),(14,1,9,'2026-09-05 18:49:49.792'),(15,1,9,'2026-09-05 18:49:49.792'),(16,1,9,'2026-09-05 18:49:49.792'),(18,1,9,'2026-09-11 15:01:08.396'),(19,1,9,'2026-09-11 15:01:08.396');
/*!40000 ALTER TABLE `user_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_sessions`
--

DROP TABLE IF EXISTS `user_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_sessions` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `token_hash` char(64) NOT NULL COMMENT 'Hash of the session token; do not persist the raw token',
  `current_role_id` bigint unsigned DEFAULT NULL,
  `client_version` varchar(40) DEFAULT NULL,
  `client_ip` varchar(64) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `last_seen_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `expires_at` datetime(3) NOT NULL,
  `revoked_at` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_sessions_token_hash` (`token_hash`),
  KEY `idx_user_sessions_user_active` (`user_id`,`revoked_at`,`expires_at`),
  KEY `fk_user_sessions_current_role` (`current_role_id`),
  CONSTRAINT `fk_user_sessions_current_role` FOREIGN KEY (`current_role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `fk_user_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_user_sessions_expiry` CHECK ((`expires_at` > `created_at`)),
  CONSTRAINT `ck_user_sessions_revoked` CHECK (((`revoked_at` is null) or (`revoked_at` >= `created_at`)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_sessions`
--

LOCK TABLES `user_sessions` WRITE;
/*!40000 ALTER TABLE `user_sessions` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_sessions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `password_hash` varchar(100) NOT NULL COMMENT 'BCrypt/Argon2 hash only; never store a plaintext password',
  `display_name` varchar(100) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `phone` varchar(32) DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `avatar_url` mediumtext,
  `last_login_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `version` int unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`),
  UNIQUE KEY `uk_users_email` (`email`),
  UNIQUE KEY `uk_users_phone` (`phone`),
  KEY `idx_users_status_display_name` (`status`,`display_name`),
  CONSTRAINT `ck_users_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'DISABLED',_utf8mb4'PENDING',_utf8mb4'CLOSED')))
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'demo_student','$2a$10$XXapbEAQFSVxOLqHDkfWLuvSk3HlgskBuF9xxA3p4LFP5YpJ7gnAi','演示学生','123@vcampus.local','13812340001','ACTIVE','2',NULL,'2026-09-04 13:46:23.309','2026-09-13 14:23:09.672',3),(2,'demo_teacher','$2a$10$5WUEQdJ2eGj6V6mC1ky2mu0iPPB/qTqMk8bUpEdpOD.9kvyzjiEaG','演示教师兼教务员','demo.teacher@vcampus.local',NULL,'ACTIVE',NULL,NULL,'2026-09-04 13:46:23.309','2026-09-04 13:46:23.309',0),(3,'demo_registrar','$2a$10$5ekn2HRLljAFzcUnmZKaEOP6Hv3lmPwYClugS0LwmC3N3NgwZRvtC','演示学籍管理员','demo.registrar@vcampus.local',NULL,'ACTIVE',NULL,NULL,'2026-09-04 13:46:23.309','2026-09-04 13:46:23.309',0),(4,'demo_academic','$2a$10$yjcvppAQlhH5kKE2Zd8oeOPydQJ4fYH1UNWMa3qSTa5ojeG6U3RY2','演示教务老师','demo.academic@vcampus.local',NULL,'ACTIVE',NULL,NULL,'2026-09-04 13:46:23.309','2026-09-04 13:46:23.309',0),(5,'demo_librarian','$2a$10$1EGZqHksUfmAFpbNUUYjmO6fLznNlLMJeMPzcV7Z1tjTyCoO.pBuK','演示图书管理员','demo.librarian@vcampus.local',NULL,'ACTIVE',NULL,NULL,'2026-09-04 13:46:23.309','2026-09-04 13:46:23.309',0),(6,'demo_store','$2a$10$MUcAM/wOO/dLPxIn4tBQTuXnQv7/2Zk5sGcCQSzcGLbtisRIqUSSm','演示商店管理员','demo.store@vcampus.local',NULL,'ACTIVE',NULL,NULL,'2026-09-04 13:46:23.309','2026-09-04 13:46:23.309',0),(7,'demo_dorm','$2a$10$zXg9yG2WFWzADsSoTBNJJOpJAsUU2Yw.x7YhZzsh/8inwZIMSZEV6','演示宿管员','demo.dorm@vcampus.local',NULL,'ACTIVE',NULL,NULL,'2026-09-04 13:46:23.309','2026-09-04 13:46:23.309',0),(8,'demo_ai','$2a$10$5vJUUJ5qZusyhaLyDWE/MO4.cbpaoJF7Ccav66mjmDxmII0rZlmYu','演示AI知识管理员','demo.ai@vcampus.local',NULL,'ACTIVE',NULL,NULL,'2026-09-04 13:46:23.309','2026-09-04 13:46:23.309',0),(9,'demo_system','$2a$10$ffS7jMPz9A0Rnz9bYzraxeDWeksow9bBW426QJBBLs4EiJJvVBgLi','演示系统管理员','demo.system@vcampus.local',NULL,'ACTIVE',NULL,NULL,'2026-09-04 13:46:23.309','2026-09-04 13:46:23.309',0),(10,'demo_repair','$2a$10$nzRizN//GQ1wCYMq23.O4OdHV6kHrOCSKOa3t3QIpPyZXW5h.xhFm','演示维修员','demo.repair@vcampus.local','13900000010','ACTIVE',NULL,NULL,'2026-09-04 22:44:51.473','2026-09-04 22:44:51.473',0),(11,'demo_stu2','$2a$10$XXapbEAQFSVxOLqHDkfWLuvSk3HlgskBuF9xxA3p4LFP5YpJ7gnAi','钱雨桐','demo.stu2@vcampus.local','13812340002','ACTIVE',NULL,NULL,'2026-09-05 18:49:49.779','2026-09-11 15:01:08.344',0),(12,'demo_stu3','$2a$10$XXapbEAQFSVxOLqHDkfWLuvSk3HlgskBuF9xxA3p4LFP5YpJ7gnAi','孙嘉怡','demo.stu3@vcampus.local','13812340003','ACTIVE',NULL,NULL,'2026-09-05 18:49:49.779','2026-09-11 15:01:08.344',0),(13,'demo_stu4','$2a$10$XXapbEAQFSVxOLqHDkfWLuvSk3HlgskBuF9xxA3p4LFP5YpJ7gnAi','李思彤','demo.stu4@vcampus.local','13812340004','ACTIVE',NULL,NULL,'2026-09-05 18:49:49.779','2026-09-11 15:01:08.344',0),(14,'demo_stu5','$2a$10$XXapbEAQFSVxOLqHDkfWLuvSk3HlgskBuF9xxA3p4LFP5YpJ7gnAi','周若曦','demo.stu5@vcampus.local','13812340005','ACTIVE',NULL,NULL,'2026-09-05 18:49:49.779','2026-09-11 15:01:08.344',0),(15,'demo_stu6','$2a$10$XXapbEAQFSVxOLqHDkfWLuvSk3HlgskBuF9xxA3p4LFP5YpJ7gnAi','吴欣然','demo.stu6@vcampus.local','13812340006','ACTIVE',NULL,NULL,'2026-09-05 18:49:49.779','2026-09-11 15:01:08.344',0),(16,'demo_stu7','$2a$10$XXapbEAQFSVxOLqHDkfWLuvSk3HlgskBuF9xxA3p4LFP5YpJ7gnAi','郑一诺','demo.stu7@vcampus.local','13812340007','ACTIVE',NULL,NULL,'2026-09-05 18:49:49.779','2026-09-11 15:01:08.344',0),(18,'demo_stu8','$2a$10$XXapbEAQFSVxOLqHDkfWLuvSk3HlgskBuF9xxA3p4LFP5YpJ7gnAi','赵书瑶','demo.stu8@vcampus.local','13812340008','ACTIVE',NULL,NULL,'2026-09-11 15:01:08.344','2026-09-11 15:01:08.344',0),(19,'demo_stu9','$2a$10$XXapbEAQFSVxOLqHDkfWLuvSk3HlgskBuF9xxA3p4LFP5YpJ7gnAi','王梓萱','demo.stu9@vcampus.local','13812340009','ACTIVE',NULL,NULL,'2026-09-11 15:01:08.344','2026-09-11 15:01:08.344',0);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `utility_allocations`
--

DROP TABLE IF EXISTS `utility_allocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `utility_allocations` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `bill_id` bigint unsigned NOT NULL,
  `student_user_id` bigint unsigned NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'UNPAID',
  `paid_transaction_id` bigint unsigned DEFAULT NULL,
  `paid_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_utility_allocations_bill_student` (`bill_id`,`student_user_id`),
  KEY `idx_utility_allocations_student_status` (`student_user_id`,`status`),
  KEY `fk_utility_allocations_transaction` (`paid_transaction_id`),
  CONSTRAINT `fk_utility_allocations_bill` FOREIGN KEY (`bill_id`) REFERENCES `utility_bills` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_utility_allocations_student` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_utility_allocations_transaction` FOREIGN KEY (`paid_transaction_id`) REFERENCES `account_transactions` (`id`),
  CONSTRAINT `ck_utility_allocations_amount` CHECK ((`amount` > 0)),
  CONSTRAINT `ck_utility_allocations_paid` CHECK (((`status` <> _utf8mb4'PAID') or ((`paid_transaction_id` is not null) and (`paid_at` is not null)))),
  CONSTRAINT `ck_utility_allocations_status` CHECK ((`status` in (_utf8mb4'UNPAID',_utf8mb4'PAID',_utf8mb4'WAIVED')))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `utility_allocations`
--

LOCK TABLES `utility_allocations` WRITE;
/*!40000 ALTER TABLE `utility_allocations` DISABLE KEYS */;
INSERT INTO `utility_allocations` VALUES (2,2,1,47.02,'PAID',6,'2026-09-13 17:23:54.761','2026-09-11 15:01:08.752'),(3,2,11,47.01,'UNPAID',NULL,NULL,'2026-09-11 15:01:08.752'),(4,2,12,47.01,'UNPAID',NULL,NULL,'2026-09-11 15:01:08.752'),(5,3,16,88.12,'UNPAID',NULL,NULL,'2026-09-13 17:29:20.814'),(6,4,19,63.58,'UNPAID',NULL,NULL,'2026-09-13 17:29:27.935');
/*!40000 ALTER TABLE `utility_allocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `utility_bills`
--

DROP TABLE IF EXISTS `utility_bills`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `utility_bills` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `room_id` bigint unsigned NOT NULL,
  `period_start` date NOT NULL,
  `period_end` date NOT NULL,
  `electricity_units` decimal(12,3) NOT NULL DEFAULT '0.000',
  `water_units` decimal(12,3) NOT NULL DEFAULT '0.000',
  `total_amount` decimal(12,2) NOT NULL,
  `due_at` datetime(3) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'UNPAID',
  `created_by` bigint unsigned DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_utility_bills_room_period` (`room_id`,`period_start`,`period_end`),
  KEY `idx_utility_bills_status_due` (`status`,`due_at`),
  KEY `fk_utility_bills_creator` (`created_by`),
  CONSTRAINT `fk_utility_bills_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_utility_bills_room` FOREIGN KEY (`room_id`) REFERENCES `dorm_rooms` (`id`),
  CONSTRAINT `ck_utility_bills_amount` CHECK ((`total_amount` >= 0)),
  CONSTRAINT `ck_utility_bills_period` CHECK ((`period_end` >= `period_start`)),
  CONSTRAINT `ck_utility_bills_status` CHECK ((`status` in (_utf8mb4'UNPAID',_utf8mb4'PARTIAL',_utf8mb4'PAID',_utf8mb4'VOID'))),
  CONSTRAINT `ck_utility_bills_units` CHECK (((`electricity_units` >= 0) and (`water_units` >= 0)))
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `utility_bills`
--

LOCK TABLES `utility_bills` WRITE;
/*!40000 ALTER TABLE `utility_bills` DISABLE KEYS */;
INSERT INTO `utility_bills` VALUES (2,1,'2026-08-01','2026-08-31',132.500,21.300,141.04,'2026-09-15 23:59:00.000','PARTIAL',7,'2026-09-01 09:35:00.000','2026-09-13 17:23:54.762'),(3,5,'2026-08-01','2026-08-31',90.400,12.000,88.12,'2026-09-15 23:59:00.000','UNPAID',7,'2026-09-13 17:29:20.813','2026-09-13 17:29:20.813'),(4,6,'2026-08-01','2026-08-31',60.900,9.400,63.58,'2026-09-15 23:59:00.000','UNPAID',7,'2026-09-13 17:29:27.934','2026-09-13 17:29:27.934');
/*!40000 ALTER TABLE `utility_bills` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `vw_current_accommodation`
--

DROP TABLE IF EXISTS `vw_current_accommodation`;
/*!50001 DROP VIEW IF EXISTS `vw_current_accommodation`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `vw_current_accommodation` AS SELECT 
 1 AS `accommodation_id`,
 1 AS `student_user_id`,
 1 AS `bed_id`,
 1 AS `room_id`,
 1 AS `room_no`,
 1 AS `building_id`,
 1 AS `building_name`,
 1 AS `start_date`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `vw_store_sales`
--

DROP TABLE IF EXISTS `vw_store_sales`;
/*!50001 DROP VIEW IF EXISTS `vw_store_sales`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `vw_store_sales` AS SELECT 
 1 AS `product_id`,
 1 AS `product_name_snapshot`,
 1 AS `sold_quantity`,
 1 AS `sales_amount`*/;
SET character_set_client = @saved_cs_client;

--
-- Final view structure for view `vw_current_accommodation`
--

/*!50001 DROP VIEW IF EXISTS `vw_current_accommodation`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`vcampus`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `vw_current_accommodation` AS select `ar`.`id` AS `accommodation_id`,`ar`.`student_user_id` AS `student_user_id`,`ar`.`bed_id` AS `bed_id`,`dr`.`id` AS `room_id`,`dr`.`room_no` AS `room_no`,`db`.`id` AS `building_id`,`db`.`building_name` AS `building_name`,`ar`.`start_date` AS `start_date` from (((`accommodation_records` `ar` join `dorm_beds` `bed` on((`bed`.`id` = `ar`.`bed_id`))) join `dorm_rooms` `dr` on((`dr`.`id` = `bed`.`room_id`))) join `dorm_buildings` `db` on((`db`.`id` = `dr`.`building_id`))) where (`ar`.`status` = 'ACTIVE') */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `vw_store_sales`
--

/*!50001 DROP VIEW IF EXISTS `vw_store_sales`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`vcampus`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `vw_store_sales` AS select `soi`.`product_id` AS `product_id`,`soi`.`product_name_snapshot` AS `product_name_snapshot`,sum(`soi`.`quantity`) AS `sold_quantity`,sum(`soi`.`line_amount`) AS `sales_amount` from (`store_order_items` `soi` join `store_orders` `so` on((`so`.`id` = `soi`.`order_id`))) where (`so`.`status` in ('PAID','COMPLETED')) group by `soi`.`product_id`,`soi`.`product_name_snapshot` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-14 14:18:45
