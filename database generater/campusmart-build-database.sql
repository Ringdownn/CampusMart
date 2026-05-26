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

USE test;   -- 如果你想使用 test 数据库
-- 或者先创建并切换到新数据库
CREATE DATABASE IF NOT EXISTS campus_mart;
USE campus_mart;


SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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

-- ----------------------------
-- Table structure for goods_like
-- ----------------------------
DROP TABLE IF EXISTS `goods_like`;
CREATE TABLE `goods_like` (
  `likeID` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `userID` bigint NOT NULL COMMENT '用户ID',
  `goodID` bigint NOT NULL COMMENT '商品ID',
  `likeTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`likeID`) USING BTREE,
  UNIQUE KEY `uk_user_good_like` (`userID`, `goodID`),
  KEY `idx_goodID_like` (`goodID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='商品点赞表';

-- ----------------------------
-- Table structure for goods_collection
-- ----------------------------
DROP TABLE IF EXISTS `goods_collection`;
CREATE TABLE `goods_collection` (
  `collectionID` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `userID` bigint NOT NULL COMMENT '用户ID',
  `goodID` bigint NOT NULL COMMENT '商品ID',
  `collectionTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`collectionID`) USING BTREE,
  UNIQUE KEY `uk_user_good_collection` (`userID`, `goodID`),
  KEY `idx_goodID_collection` (`goodID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='商品收藏表';

SET FOREIGN_KEY_CHECKS = 1;
