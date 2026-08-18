-- ============================================================
-- delivery_service DB 및 테이블 생성 스크립트
-- 엔티티 기준: User, DeliveryPlan, DeliveryStop, DeliveryItem,
--             RiskAssessment, RiskFactor, RefreshToken
-- ============================================================

CREATE DATABASE IF NOT EXISTS delivery_service
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE delivery_service;

-- 자식 -> 부모 순서로 DROP (재실행 시 FK 충돌 방지)
DROP TABLE IF EXISTS risk_factor;
DROP TABLE IF EXISTS risk_assessment;
DROP TABLE IF EXISTS delivery_item;
DROP TABLE IF EXISTS delivery_stop;
DROP TABLE IF EXISTS delivery_plan;
DROP TABLE IF EXISTS refresh_token;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS weather;

-- ------------------------------------------------------------
-- users
-- ------------------------------------------------------------
CREATE TABLE users (
                       id        BIGINT AUTO_INCREMENT PRIMARY KEY,
                       login_id  VARCHAR(255) NOT NULL,
                       password  VARCHAR(255) NOT NULL,
                       name      VARCHAR(255) NOT NULL,
                       role      VARCHAR(30)  NOT NULL,
                       CONSTRAINT uk_users_login_id UNIQUE (login_id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- refresh_token
-- ------------------------------------------------------------
CREATE TABLE refresh_token (
                               id       BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id  BIGINT       NOT NULL,
                               token    VARCHAR(1000) CHARACTER SET ascii NOT NULL,

                               CONSTRAINT uk_refresh_token_user UNIQUE (user_id),
                               CONSTRAINT uk_refresh_token_token UNIQUE (token),

                               CONSTRAINT fk_refresh_token_user
                                   FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- delivery_plan
-- ------------------------------------------------------------
CREATE TABLE delivery_plan (
                               id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
                               driver_id               BIGINT       NOT NULL,
                               departure_location      VARCHAR(255) NOT NULL,
                               departure_latitude      DOUBLE       NOT NULL,
                               departure_longitude     DOUBLE       NOT NULL,
                               scheduled_departure_at  DATETIME(6),
                               actual_departure_at     DATETIME(6),
                               status                  VARCHAR(30)  NOT NULL,
                               created_at              DATETIME(6)  NOT NULL,
                               completed_at            DATETIME(6),
                               CONSTRAINT fk_delivery_plan_driver
                                   FOREIGN KEY (driver_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE INDEX idx_delivery_plan_driver_created
    ON delivery_plan (driver_id, created_at);

-- ------------------------------------------------------------
-- delivery_stop
-- ------------------------------------------------------------
CREATE TABLE delivery_stop (
                               id                BIGINT AUTO_INCREMENT PRIMARY KEY,
                               delivery_plan_id  BIGINT       NOT NULL,
                               sequence          INT,
                               status            VARCHAR(30)  NOT NULL,
                               address           VARCHAR(255) NOT NULL,
                               latitude          DOUBLE       NOT NULL,
                               longitude         DOUBLE       NOT NULL,
                               completed_at      DATETIME(6),

                               CONSTRAINT fk_delivery_stop_plan
                                   FOREIGN KEY (delivery_plan_id)
                                       REFERENCES delivery_plan (id)
) ENGINE=InnoDB;

CREATE INDEX idx_delivery_stop_plan
    ON delivery_stop (delivery_plan_id);

-- ------------------------------------------------------------
-- delivery_item
-- ------------------------------------------------------------
CREATE TABLE delivery_item (
                               id                BIGINT AUTO_INCREMENT PRIMARY KEY,
                               delivery_stop_id  BIGINT       NOT NULL,
                               product_name      VARCHAR(255) NOT NULL,
                               product_type      VARCHAR(30)  NOT NULL,
                               quantity          INT          NOT NULL,
                               CONSTRAINT fk_delivery_item_stop
                                   FOREIGN KEY (delivery_stop_id) REFERENCES delivery_stop (id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- risk_assessment
-- ------------------------------------------------------------
CREATE TABLE risk_assessment (
                                 id                BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 delivery_stop_id  BIGINT      NOT NULL,
                                 level             VARCHAR(30) NOT NULL,
                                 analyzed_at       DATETIME(6) NOT NULL,
                                 CONSTRAINT uk_risk_assessment_stop UNIQUE (delivery_stop_id),
                                 CONSTRAINT fk_risk_assessment_stop
                                     FOREIGN KEY (delivery_stop_id) REFERENCES delivery_stop (id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- risk_factor
-- ------------------------------------------------------------
CREATE TABLE risk_factor (
                             id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                             risk_assessment_id  BIGINT       NOT NULL,
                             type                VARCHAR(30)  NOT NULL,
                             description         VARCHAR(255),
                             CONSTRAINT fk_risk_factor_assessment
                                 FOREIGN KEY (risk_assessment_id) REFERENCES risk_assessment (id)
) ENGINE=InnoDB;

CREATE INDEX idx_delivery_item_stop
    ON delivery_item (delivery_stop_id);

CREATE INDEX idx_risk_factor_assessment
    ON risk_factor (risk_assessment_id);

-- ------------------------------------------------------------
-- weather
-- ------------------------------------------------------------
CREATE TABLE weather (
                         id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                         nx          INT          NOT NULL,
                         ny          INT          NOT NULL,
                         fcst_date   DATE         NOT NULL,
                         fcst_time   TIME         NOT NULL,
                         base_date   DATE         NOT NULL,
                         base_time   TIME         NOT NULL,
                         category    VARCHAR(10)  NOT NULL,
                         fcst_value  VARCHAR(50)  NOT NULL,
                         fetched_at  DATETIME(6)  NOT NULL,
                         CONSTRAINT uk_weather_slot UNIQUE (nx, ny, fcst_date, fcst_time, category)
) ENGINE=InnoDB;
