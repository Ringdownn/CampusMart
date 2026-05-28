/*
 Navicat Premium Dump SQL

 Source Server         : Mysql
 Source Server Type    : MySQL
 Source Server Version : 80407 (8.4.7)
 Source Host           : localhost:3306
 Source Schema         : campusmart

 Target Server Type    : MySQL
 Target Server Version : 80407 (8.4.7)
 File Encoding         : 65001

 Date: 31/12/2025 07:25:14
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for alipay_account_binds
-- ----------------------------
DROP TABLE IF EXISTS `alipay_account_binds`;
CREATE TABLE `alipay_account_binds` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `alipay_user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `alipay_login_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `access_token` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refresh_token` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bind_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for payments
-- ----------------------------
DROP TABLE IF EXISTS `payments`;
CREATE TABLE `payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `pay_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `buyer_id` bigint NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `channel` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ALIPAY_SANDBOX',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `alipay_trade_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notify_body` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `paid_at` datetime DEFAULT NULL,
  `closed_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_order_id` (`order_id`) USING BTREE,
  UNIQUE KEY `uk_pay_no` (`pay_no`) USING BTREE,
  KEY `idx_order_no` (`order_no`) USING BTREE,
  KEY `idx_buyer_id` (`buyer_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for payment_outbox
-- ----------------------------
DROP TABLE IF EXISTS `payment_outbox`;
CREATE TABLE `payment_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `routing_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `payload` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NEW',
  `retry_count` int NOT NULL DEFAULT 0,
  `next_retry_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_event_id` (`event_id`) USING BTREE,
  KEY `idx_status_retry` (`status`, `next_retry_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for goods
-- ----------------------------
DROP TABLE IF EXISTS `goods`;
CREATE TABLE `goods` (
  `goodID` bigint NOT NULL AUTO_INCREMENT,
  `publishUserID` bigint DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `appearance` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `itemDescription` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci,
  `price` bigint DEFAULT NULL,
  `publishTime` datetime DEFAULT NULL,
  PRIMARY KEY (`goodID`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2006064601594097667 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `goods_id` bigint NOT NULL,
  `buyer_id` bigint NOT NULL,
  `seller_id` bigint NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `pay_expire_at` datetime NOT NULL,
  `paid_at` datetime DEFAULT NULL,
  `settled_at` datetime DEFAULT NULL,
  `cancelled_at` datetime DEFAULT NULL,
  `cancel_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_order_no` (`order_no`) USING BTREE,
  KEY `idx_buyer_id` (`buyer_id`) USING BTREE,
  KEY `idx_seller_id` (`seller_id`) USING BTREE,
  KEY `idx_goods_id` (`goods_id`) USING BTREE,
  KEY `idx_status_expire` (`status`, `pay_expire_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for order_events
-- ----------------------------
DROP TABLE IF EXISTS `order_events`;
CREATE TABLE `order_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `from_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `to_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_body` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_order_id` (`order_id`) USING BTREE,
  KEY `idx_order_no` (`order_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for trade_outbox
-- ----------------------------
DROP TABLE IF EXISTS `trade_outbox`;
CREATE TABLE `trade_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `routing_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `payload` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NEW',
  `retry_count` int NOT NULL DEFAULT 0,
  `next_retry_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_event_id` (`event_id`) USING BTREE,
  KEY `idx_status_retry` (`status`, `next_retry_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for message
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `messageID` bigint NOT NULL AUTO_INCREMENT,
  `senderID` bigint DEFAULT NULL,
  `receiverID` bigint DEFAULT NULL,
  `message_content` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `sendTime` datetime DEFAULT NULL,
  PRIMARY KEY (`messageID`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2006088217010401282 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for user_wallets
-- ----------------------------
DROP TABLE IF EXISTS `user_wallets`;
CREATE TABLE `user_wallets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `available_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `frozen_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for wallet_flows
-- ----------------------------
DROP TABLE IF EXISTS `wallet_flows`;
CREATE TABLE `wallet_flows` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `flow_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `flow_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `balance_after` decimal(10,2) NOT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_flow_no` (`flow_no`) USING BTREE,
  UNIQUE KEY `uk_order_flow_type` (`order_id`, `flow_type`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_order_id` (`order_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `notificationID` bigint NOT NULL AUTO_INCREMENT,
  `senderName` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `notification_content` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `sendTime` datetime DEFAULT NULL,
  PRIMARY KEY (`notificationID`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for picture
-- ----------------------------
DROP TABLE IF EXISTS `picture`;
CREATE TABLE `picture` (
  `pictureID` bigint NOT NULL AUTO_INCREMENT,
  `pictureURL` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `goodID` bigint DEFAULT NULL COMMENT 'foreign key',
  `userID` bigint DEFAULT NULL,
  PRIMARY KEY (`pictureID`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `userID` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `Nickname` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `phone` bigint DEFAULT NULL,
  `profileSignature` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `schoolName` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `studentID` bigint DEFAULT NULL,
  `avatarURL` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`userID`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2006064223557283843 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
