-- ============================================================
-- delivery_service DB 및 테이블 생성 스크립트
-- 엔티티 기준: User, DeliveryPlan, DeliveryStop, DeliveryItem,
--             RiskAssessment, RiskFactor, Weather
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
-- 기존 RDB Refresh Token 테이블 제거용
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

-- ============================================================
-- 관리자 테스트 계정
-- loginId: admin
-- password: 1234
-- ============================================================

INSERT INTO users (
    login_id,
    password,
    name,
    role
)
VALUES (
           'admin',
           '$2y$10$Z3CLfcNpZ2VZag4YoSHUj.Ku3NmM6ZFhMywRmazbw1nmBi4KTO4hi',
           '관리자',
           'ROLE_ADMIN'
       );

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

select * from risk_factor;
select * from risk_assessment;
select * from delivery_stop;
select * from delivery_plan;
select * from weather;

-- ------------------------------------------------------------
-- 더미 데이터 (기능 테스트용)
-- ------------------------------------------------------------

-- delivery_plan.driver_id FK를 만족시키기 위한 최소 기사 계정 1명
INSERT INTO users (login_id, password, name, role) VALUES
    ('driver1', '1234', '김기사', 'ROLE_DELIVERY_DRIVER');

-- delivery_plan 2건
INSERT INTO delivery_plan
    (id, driver_id, departure_location, departure_latitude, departure_longitude, status, created_at) VALUES
    (1, 1, '서울 강남구 테헤란로 152', 37.5006, 127.0364, 'READY', NOW()),
    (2, 1, '서울 마포구 월드컵북로 396', 37.5665, 126.8977, 'READY', NOW());

-- delivery_stop 5건 (plan1: 3개, plan2: 2개)
INSERT INTO delivery_stop
    (id, delivery_plan_id, sequence, status, address, latitude, longitude) VALUES
    (1, 1, 0, 'READY', '서울 강남구 역삼동 123', 37.5006, 127.0365),
    (2, 1, 1, 'READY', '서울 강남구 삼성동 456', 37.5140, 127.0560),
    (3, 1, 2, 'READY', '서울 서초구 서초동 789', 37.4919, 127.0148),
    (4, 2, 0, 'READY', '서울 마포구 상암동 111', 37.5794, 126.8896),
    (5, 2, 1, 'READY', '서울 은평구 불광동 222', 37.6104, 126.9295);

-- risk_assessment 5건 (delivery_stop 1:1) - SAFE/CAUTION/DANGER 골고루 섞음
INSERT INTO risk_assessment (id, delivery_stop_id, level, analyzed_at) VALUES
    (1, 1, 'SAFE', NOW()),
    (2, 2, 'CAUTION', NOW()),
    (3, 3, 'DANGER', NOW()),
    (4, 4, 'SAFE', NOW()),
    (5, 5, 'CAUTION', NOW());

-- risk_factor : CAUTION(40~69점)/DANGER(70점 이상) assessment에만 부여
-- HEAVY_RAIN=30, HEAT_WAVE=20, WEATHER_WARNING=40
INSERT INTO risk_factor (risk_assessment_id, type, description) VALUES
    (2, 'HEAT_WAVE', '폭염'),
    (2, 'HEAVY_RAIN', '폭우'),
    (3, 'WEATHER_WARNING', '기상 특보'),
    (3, 'HEAVY_RAIN', '폭우'),
    (5, 'HEAT_WAVE', '폭염'),
    (5, 'HEAVY_RAIN', '폭우');
