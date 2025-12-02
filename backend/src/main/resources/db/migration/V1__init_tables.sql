/*M!999999\- enable the sandbox mode */
-- MariaDB dump 10.19-11.7.2-MariaDB, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: chwimeet
-- ------------------------------------------------------
-- Server version	11.7.2-MariaDB-ubu2404

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*M!100616 SET @OLD_NOTE_VERBOSITY=@@NOTE_VERBOSITY, NOTE_VERBOSITY=0 */;

--
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
                            `id` bigint(20) NOT NULL AUTO_INCREMENT,
                            `created_at` datetime(6) DEFAULT NULL,
                            `modified_at` datetime(6) DEFAULT NULL,
                            `name` varchar(255) DEFAULT NULL,
                            `parent_id` bigint(20) DEFAULT NULL,
                            PRIMARY KEY (`id`),
                            KEY `FK2y94svpmqttx80mshyny85wqr` (`parent_id`),
                            CONSTRAINT `FK2y94svpmqttx80mshyny85wqr` FOREIGN KEY (`parent_id`) REFERENCES `category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_member`
--

DROP TABLE IF EXISTS `chat_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_member` (
                               `id` bigint(20) NOT NULL AUTO_INCREMENT,
                               `created_at` datetime(6) DEFAULT NULL,
                               `modified_at` datetime(6) DEFAULT NULL,
                               `last_read_message_id` bigint(20) DEFAULT NULL,
                               `chat_room_id` bigint(20) DEFAULT NULL,
                               `member_id` bigint(20) DEFAULT NULL,
                               PRIMARY KEY (`id`),
                               KEY `FKp3ov6ys5mw1i7e9va4nniwa5q` (`chat_room_id`),
                               KEY `FKnvohh3wx5hc6293ob3kfne72f` (`member_id`),
                               CONSTRAINT `FKnvohh3wx5hc6293ob3kfne72f` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`),
                               CONSTRAINT `FKp3ov6ys5mw1i7e9va4nniwa5q` FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_message`
--

DROP TABLE IF EXISTS `chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_message` (
                                `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                `created_at` datetime(6) DEFAULT NULL,
                                `modified_at` datetime(6) DEFAULT NULL,
                                `chat_member_id` bigint(20) DEFAULT NULL,
                                `chat_room_id` bigint(20) DEFAULT NULL,
                                `content` text DEFAULT NULL,
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_room`
--

DROP TABLE IF EXISTS `chat_room`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_room` (
                             `id` bigint(20) NOT NULL AUTO_INCREMENT,
                             `created_at` datetime(6) DEFAULT NULL,
                             `modified_at` datetime(6) DEFAULT NULL,
                             `last_message` varchar(255) DEFAULT NULL,
                             `last_message_time` datetime(6) DEFAULT NULL,
                             `post_id` bigint(20) DEFAULT NULL,
                             `post_title_snapshot` varchar(255) NOT NULL,
                             PRIMARY KEY (`id`),
                             KEY `FKdedif34f1oocp49p9lxh3tglc` (`post_id`),
                             CONSTRAINT `FKdedif34f1oocp49p9lxh3tglc` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

DROP TABLE IF EXISTS `member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `member` (
                          `id` bigint(20) NOT NULL AUTO_INCREMENT,
                          `created_at` datetime(6) DEFAULT NULL,
                          `modified_at` datetime(6) DEFAULT NULL,
                          `address1` varchar(255) DEFAULT NULL,
                          `address2` varchar(255) DEFAULT NULL,
                          `email` varchar(255) NOT NULL,
                          `is_banned` bit(1) NOT NULL,
                          `name` varchar(255) DEFAULT NULL,
                          `nickname` varchar(255) NOT NULL,
                          `password` varchar(255) NOT NULL,
                          `phone_number` varchar(255) DEFAULT NULL,
                          `profile_img_url` varchar(255) DEFAULT NULL,
                          `role` enum('ADMIN','USER') NOT NULL,
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `UKmbmcqelty0fbrvxp1q58dn57t` (`email`),
                          UNIQUE KEY `UKhh9kg6jti4n1eoiertn2k6qsc` (`nickname`),
                          UNIQUE KEY `UKn2qryhkfoqeel6njfhrcq6k7u` (`phone_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification`
--

DROP TABLE IF EXISTS `notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
                                `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                `created_at` datetime(6) DEFAULT NULL,
                                `modified_at` datetime(6) DEFAULT NULL,
                                `is_read` bit(1) DEFAULT NULL,
                                `target_id` bigint(20) DEFAULT NULL,
                                `type` enum('RESERVATION_CANCELLED','RESERVATION_CLAIMING','RESERVATION_CLAIM_COMPLETED','RESERVATION_INSPECTING_RENTAL','RESERVATION_INSPECTING_RETURN','RESERVATION_LOST_OR_UNRETURNED','RESERVATION_PENDING_APPROVAL','RESERVATION_PENDING_PAYMENT','RESERVATION_PENDING_PICKUP','RESERVATION_PENDING_REFUND','RESERVATION_PENDING_RETURN','RESERVATION_REFUND_COMPLETED','RESERVATION_REJECTED','RESERVATION_RENTING','RESERVATION_RETURNING','RESERVATION_RETURN_COMPLETED','RESERVATION_SHIPPING') DEFAULT NULL,
                                `member_id` bigint(20) DEFAULT NULL,
                                PRIMARY KEY (`id`),
                                KEY `FK1xep8o2ge7if6diclyyx53v4q` (`member_id`),
                                CONSTRAINT `FK1xep8o2ge7if6diclyyx53v4q` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `post`
--

DROP TABLE IF EXISTS `post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `post` (
                        `id` bigint(20) NOT NULL AUTO_INCREMENT,
                        `created_at` datetime(6) DEFAULT NULL,
                        `modified_at` datetime(6) DEFAULT NULL,
                        `content` text NOT NULL,
                        `deposit` int(11) NOT NULL,
                        `fee` int(11) NOT NULL,
                        `is_banned` bit(1) DEFAULT NULL,
                        `receive_method` enum('ANY','DELIVERY','DIRECT') NOT NULL,
                        `return_address1` varchar(255) DEFAULT NULL,
                        `return_address2` varchar(255) DEFAULT NULL,
                        `return_method` enum('ANY','DELIVERY','DIRECT') NOT NULL,
                        `title` varchar(255) NOT NULL,
                        `author_id` bigint(20) NOT NULL,
                        `category_id` bigint(20) NOT NULL,
                        `embedding_status` enum('DONE','PENDING','WAIT') NOT NULL,
                        `embedding_version` bigint(20) DEFAULT NULL,
                        PRIMARY KEY (`id`),
                        KEY `FKiq6lerqnw4jde34k91gugrx9` (`author_id`),
                        KEY `fk_post_category` (`category_id`),
                        CONSTRAINT `FKiq6lerqnw4jde34k91gugrx9` FOREIGN KEY (`author_id`) REFERENCES `member` (`id`),
                        CONSTRAINT `fk_post_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `post_favorite`
--

DROP TABLE IF EXISTS `post_favorite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_favorite` (
                                 `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                 `created_at` datetime(6) DEFAULT NULL,
                                 `modified_at` datetime(6) DEFAULT NULL,
                                 `member_id` bigint(20) NOT NULL,
                                 `post_id` bigint(20) NOT NULL,
                                 PRIMARY KEY (`id`),
                                 KEY `FKa8aly3gnyl9xue6txyf0ckojj` (`member_id`),
                                 KEY `FKtnr54tuktg3welr2u950p0mqr` (`post_id`),
                                 CONSTRAINT `FKa8aly3gnyl9xue6txyf0ckojj` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`),
                                 CONSTRAINT `FKtnr54tuktg3welr2u950p0mqr` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `post_image`
--

DROP TABLE IF EXISTS `post_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_image` (
                              `id` bigint(20) NOT NULL AUTO_INCREMENT,
                              `created_at` datetime(6) DEFAULT NULL,
                              `modified_at` datetime(6) DEFAULT NULL,
                              `image_url` varchar(255) DEFAULT NULL,
                              `is_primary` bit(1) DEFAULT NULL,
                              `post_id` bigint(20) NOT NULL,
                              PRIMARY KEY (`id`),
                              KEY `FKsip7qv57jw2fw50g97t16nrjr` (`post_id`),
                              CONSTRAINT `FKsip7qv57jw2fw50g97t16nrjr` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `post_option`
--

DROP TABLE IF EXISTS `post_option`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_option` (
                               `id` bigint(20) NOT NULL AUTO_INCREMENT,
                               `created_at` datetime(6) DEFAULT NULL,
                               `modified_at` datetime(6) DEFAULT NULL,
                               `deposit` int(11) DEFAULT NULL,
                               `fee` int(11) DEFAULT NULL,
                               `name` varchar(255) DEFAULT NULL,
                               `post_id` bigint(20) NOT NULL,
                               PRIMARY KEY (`id`),
                               KEY `FKmxsh5bfx9e0n7ncm5ksckdgmg` (`post_id`),
                               CONSTRAINT `FKmxsh5bfx9e0n7ncm5ksckdgmg` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `post_region`
--

DROP TABLE IF EXISTS `post_region`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_region` (
                               `id` bigint(20) NOT NULL AUTO_INCREMENT,
                               `created_at` datetime(6) DEFAULT NULL,
                               `modified_at` datetime(6) DEFAULT NULL,
                               `post_id` bigint(20) NOT NULL,
                               `region_id` bigint(20) NOT NULL,
                               PRIMARY KEY (`id`),
                               KEY `FK9l3v41p5r5usshroiywb6cccg` (`post_id`),
                               KEY `fk_postregion_region` (`region_id`),
                               CONSTRAINT `FK9l3v41p5r5usshroiywb6cccg` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`),
                               CONSTRAINT `fk_postregion_region` FOREIGN KEY (`region_id`) REFERENCES `region` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `region`
--

DROP TABLE IF EXISTS `region`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `region` (
                          `id` bigint(20) NOT NULL AUTO_INCREMENT,
                          `created_at` datetime(6) DEFAULT NULL,
                          `modified_at` datetime(6) DEFAULT NULL,
                          `name` varchar(255) DEFAULT NULL,
                          `parent_id` bigint(20) DEFAULT NULL,
                          PRIMARY KEY (`id`),
                          KEY `FK5cgfpq4u2digwkllynq14k7te` (`parent_id`),
                          CONSTRAINT `FK5cgfpq4u2digwkllynq14k7te` FOREIGN KEY (`parent_id`) REFERENCES `region` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report`
--

DROP TABLE IF EXISTS `report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `report` (
                          `id` bigint(20) NOT NULL AUTO_INCREMENT,
                          `created_at` datetime(6) DEFAULT NULL,
                          `modified_at` datetime(6) DEFAULT NULL,
                          `comment` varchar(255) NOT NULL,
                          `report_type` enum('MEMBER','POST','REVIEW') NOT NULL,
                          `target_id` bigint(20) NOT NULL,
                          `member_id` bigint(20) NOT NULL,
                          PRIMARY KEY (`id`),
                          KEY `FKel7y5wyx42a6njav1dbe2torl` (`member_id`),
                          CONSTRAINT `FKel7y5wyx42a6njav1dbe2torl` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reservation`
--

DROP TABLE IF EXISTS `reservation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservation` (
                               `id` bigint(20) NOT NULL AUTO_INCREMENT,
                               `created_at` datetime(6) DEFAULT NULL,
                               `modified_at` datetime(6) DEFAULT NULL,
                               `cancel_reason` varchar(255) DEFAULT NULL,
                               `claim_reason` varchar(255) DEFAULT NULL,
                               `receive_address1` varchar(255) DEFAULT NULL,
                               `receive_address2` varchar(255) DEFAULT NULL,
                               `receive_carrier` varchar(255) DEFAULT NULL,
                               `receive_method` enum('DELIVERY','DIRECT') DEFAULT NULL,
                               `receive_tracking_number` varchar(255) DEFAULT NULL,
                               `reject_reason` varchar(255) DEFAULT NULL,
                               `reservation_end_at` datetime(6) DEFAULT NULL,
                               `reservation_start_at` datetime(6) DEFAULT NULL,
                               `return_carrier` varchar(255) DEFAULT NULL,
                               `return_method` enum('DELIVERY','DIRECT') DEFAULT NULL,
                               `return_tracking_number` varchar(255) DEFAULT NULL,
                               `status` enum('CANCELLED','CLAIMING','CLAIM_COMPLETED','INSPECTING_RENTAL','INSPECTING_RETURN','LOST_OR_UNRETURNED','PENDING_APPROVAL','PENDING_PAYMENT','PENDING_PICKUP','PENDING_REFUND','PENDING_RETURN','REFUND_COMPLETED','REJECTED','RENTING','RETURNING','RETURN_COMPLETED','SHIPPING') NOT NULL,
                               `author_id` bigint(20) DEFAULT NULL,
                               `post_id` bigint(20) DEFAULT NULL,
                               PRIMARY KEY (`id`),
                               KEY `FKaflm6rogggja90detv2xybat2` (`author_id`),
                               KEY `FKqachmfh1uvgquvj3svmd0b84s` (`post_id`),
                               CONSTRAINT `FKaflm6rogggja90detv2xybat2` FOREIGN KEY (`author_id`) REFERENCES `member` (`id`),
                               CONSTRAINT `FKqachmfh1uvgquvj3svmd0b84s` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reservation_log`
--

DROP TABLE IF EXISTS `reservation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservation_log` (
                                   `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                   `created_at` datetime(6) DEFAULT NULL,
                                   `modified_at` datetime(6) DEFAULT NULL,
                                   `status` enum('CANCELLED','CLAIMING','CLAIM_COMPLETED','INSPECTING_RENTAL','INSPECTING_RETURN','LOST_OR_UNRETURNED','PENDING_APPROVAL','PENDING_PAYMENT','PENDING_PICKUP','PENDING_REFUND','PENDING_RETURN','REFUND_COMPLETED','REJECTED','RENTING','RETURNING','RETURN_COMPLETED','SHIPPING') NOT NULL,
                                   `reservation_id` bigint(20) NOT NULL,
                                   `author_id` bigint(20) DEFAULT NULL,
                                   PRIMARY KEY (`id`),
                                   KEY `FKehqrjsnudejd72fn627bbb26d` (`reservation_id`),
                                   CONSTRAINT `FKehqrjsnudejd72fn627bbb26d` FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reservation_option`
--

DROP TABLE IF EXISTS `reservation_option`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservation_option` (
                                      `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                      `created_at` datetime(6) DEFAULT NULL,
                                      `modified_at` datetime(6) DEFAULT NULL,
                                      `option_id` bigint(20) NOT NULL,
                                      `reservation_id` bigint(20) NOT NULL,
                                      PRIMARY KEY (`id`),
                                      KEY `FK16lgbybxwwotaj72bjbx986us` (`option_id`),
                                      KEY `FKh1ml21e3plitta9woi906n2a2` (`reservation_id`),
                                      CONSTRAINT `FK16lgbybxwwotaj72bjbx986us` FOREIGN KEY (`option_id`) REFERENCES `post_option` (`id`),
                                      CONSTRAINT `FKh1ml21e3plitta9woi906n2a2` FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `review`
--

DROP TABLE IF EXISTS `review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `review` (
                          `id` bigint(20) NOT NULL AUTO_INCREMENT,
                          `created_at` datetime(6) DEFAULT NULL,
                          `modified_at` datetime(6) DEFAULT NULL,
                          `comment` varchar(255) NOT NULL,
                          `equipment_score` int(11) NOT NULL,
                          `is_banned` bit(1) NOT NULL,
                          `kindness_score` int(11) NOT NULL,
                          `response_time_score` int(11) NOT NULL,
                          `reservation_id` bigint(20) DEFAULT NULL,
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `UKhyxvthxr4ats27c7vko0rb4xg` (`reservation_id`),
                          CONSTRAINT `FK7tyi0jd0eaphyr0gsvfjqww9i` FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `vector_store`
--

DROP TABLE IF EXISTS `vector_store`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `vector_store` (
                                `id` uuid NOT NULL DEFAULT uuid(),
                                `content` text DEFAULT NULL,
                                `metadata` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`metadata`)),
                                `embedding` vector(1536) NOT NULL,
                                PRIMARY KEY (`id`),
                                VECTOR KEY `vector_store_embedding_idx` (`embedding`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*M!100616 SET NOTE_VERBOSITY=@OLD_NOTE_VERBOSITY */;

-- Dump completed on 2025-12-02 11:42:46
