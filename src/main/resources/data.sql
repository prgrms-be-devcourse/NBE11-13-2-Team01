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

-- ============================================================
-- 배송 기사 테스트 계정
-- loginId: user1, user2, user3
-- password: 1234
-- ============================================================

INSERT INTO users (
    login_id,
    password,
    name,
    role
)
VALUES
    (
        'user1',
        '$2y$10$Z3CLfcNpZ2VZag4YoSHUj.Ku3NmM6ZFhMywRmazbw1nmBi4KTO4hi',
        '기사1',
        'ROLE_DELIVERY_DRIVER'
    ),
    (
        'user2',
        '$2y$10$Z3CLfcNpZ2VZag4YoSHUj.Ku3NmM6ZFhMywRmazbw1nmBi4KTO4hi',
        '기사2',
        'ROLE_DELIVERY_DRIVER'
    ),
    (
        'user3',
        '$2y$10$Z3CLfcNpZ2VZag4YoSHUj.Ku3NmM6ZFhMywRmazbw1nmBi4KTO4hi',
        '기사3',
        'ROLE_DELIVERY_DRIVER'
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

-- ============================================================
-- 배송 계획 테스트 데이터
-- 기사별 배송지 10곳 구성
-- 1001: 배송 준비 10곳
-- 1002: 배송 준비 10곳 (전체 27박스 / 삽입 시점 2시간 뒤 출발)
-- 1003: 배송 완료 10곳
-- 1004: 기사3 배송 준비 10곳 (1002와 동일 시각 출발)
-- ============================================================

INSERT INTO delivery_plan (
    id,
    driver_id,
    departure_location,
    departure_latitude,
    departure_longitude,
    scheduled_departure_at,
    actual_departure_at,
    status,
    created_at,
    completed_at
)
VALUES
    (
        1001,
        (SELECT id FROM users WHERE login_id = 'user1'),
        '서울특별시 종로구 세종대로 175',
        37.5716,
        126.9769,
        TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00'),
        NULL,
        'READY',
        DATE_SUB(NOW(), INTERVAL 2 HOUR),
        NULL
    ),
    (
        1002,
        (SELECT id FROM users WHERE login_id = 'user2'),
        '서울특별시 관악구 관악로 1',
        37.4599,
        126.9519,
        DATE_ADD(NOW(), INTERVAL 2 HOUR),
        NULL,
        'READY',
        DATE_SUB(NOW(), INTERVAL 1 DAY),
        NULL
    ),
    (
        1003,
        (SELECT id FROM users WHERE login_id = 'user3'),
        '서울특별시 강남구 테헤란로 152',
        37.5000,
        127.0365,
        DATE_SUB(NOW(), INTERVAL 26 HOUR),
        DATE_SUB(NOW(), INTERVAL 25 HOUR),
        'COMPLETED',
        DATE_SUB(NOW(), INTERVAL 2 DAY),
        DATE_SUB(NOW(), INTERVAL 20 HOUR)
    ),
    (
        1004,
        (SELECT id FROM users WHERE login_id = 'user3'),
        '서울특별시 강남구 테헤란로 152',
        37.5000,
        127.0365,
        DATE_ADD(NOW(), INTERVAL 2 HOUR),
        NULL,
        'READY',
        NOW(),
        NULL
    );

INSERT INTO delivery_stop (
    id,
    delivery_plan_id,
    sequence,
    status,
    address,
    latitude,
    longitude,
    completed_at
)
VALUES
    (1101,1001,0,'READY','서울특별시 종로구 세종대로 175',37.5716,126.9769,NULL),
    (1102,1001,1,'READY','서울특별시 종로구 율곡로 75',37.5759,126.9891,NULL),
    (1103,1001,2,'READY','서울특별시 종로구 대학로 101',37.5837,127.0007,NULL),
    (1104,1001,3,'READY','서울특별시 종로구 창경궁로 185',37.5788,126.9950,NULL),
    (1105,1001,4,'READY','서울특별시 종로구 종로 51',37.5704,126.9831,NULL),
    (1106,1001,5,'READY','서울특별시 종로구 삼일대로 428',37.5689,126.9875,NULL),
    (1107,1001,6,'READY','서울특별시 종로구 북촌로 37',37.5826,126.9849,NULL),
    (1108,1001,7,'READY','서울특별시 종로구 사직로 161',37.5755,126.9710,NULL),
    (1109,1001,8,'READY','서울특별시 종로구 인사동길 44',37.5740,126.9850,NULL),
    (1110,1001,9,'READY','서울특별시 종로구 혜화로 12',37.5862,127.0016,NULL);


INSERT INTO delivery_stop (
    id,
    delivery_plan_id,
    sequence,
    status,
    address,
    latitude,
    longitude,
    completed_at
)
VALUES
    (1201,1002,0,'READY','서울특별시 관악구 관악로 1',37.4599,126.9519,NULL),
    (1202,1002,1,'READY','서울특별시 관악구 남부순환로 1820',37.4820,126.9298,NULL),
    (1203,1002,2,'READY','서울특별시 관악구 신림로 120',37.4802,126.9368,NULL),
    (1204,1002,3,'READY','서울특별시 관악구 봉천로 450',37.4895,126.9575,NULL),
    (1205,1002,4,'READY','서울특별시 관악구 대학길 10',37.4692,126.9354,NULL),
    (1206,1002,5,'READY','서울특별시 관악구 은천로 93',37.4868,126.9427,NULL),
    (1207,1002,6,'READY','서울특별시 관악구 쑥고개로 80',37.4765,126.9478,NULL),
    (1208,1002,7,'READY','서울특별시 관악구 난곡로 250',37.4645,126.9155,NULL),
    (1209,1002,8,'READY','서울특별시 관악구 호암로 399',37.4552,126.9421,NULL),
    (1210,1002,9,'READY','서울특별시 관악구 신림동길 15',37.4872,126.9279,NULL);


INSERT INTO delivery_stop (
    id,
    delivery_plan_id,
    sequence,
    status,
    address,
    latitude,
    longitude,
    completed_at
)
VALUES
    (1301,1003,0,'COMPLETED','서울특별시 강남구 테헤란로 152',
     37.5000,127.0365,DATE_SUB(NOW(),INTERVAL 24 HOUR)),

    (1302,1003,1,'COMPLETED','서울특별시 강남구 역삼로 180',
     37.5009,127.0351,DATE_SUB(NOW(),INTERVAL 23 HOUR)),

    (1303,1003,2,'COMPLETED','서울특별시 강남구 선릉로 420',
     37.5045,127.0489,DATE_SUB(NOW(),INTERVAL 22 HOUR)),

    (1304,1003,3,'COMPLETED','서울특별시 강남구 삼성로 212',
     37.5070,127.0630,DATE_SUB(NOW(),INTERVAL 21 HOUR)),

    (1305,1003,4,'COMPLETED','서울특별시 강남구 봉은사로 524',
     37.5147,127.0580,DATE_SUB(NOW(),INTERVAL 20 HOUR)),

    (1306,1003,5,'COMPLETED','서울특별시 강남구 학동로 342',
     37.5150,127.0400,DATE_SUB(NOW(),INTERVAL 19 HOUR)),

    (1307,1003,6,'COMPLETED','서울특별시 강남구 논현로 508',
     37.5140,127.0310,DATE_SUB(NOW(),INTERVAL 18 HOUR)),

    (1308,1003,7,'COMPLETED','서울특별시 강남구 도산대로 45',
     37.5222,127.0365,DATE_SUB(NOW(),INTERVAL 17 HOUR)),

    (1309,1003,8,'COMPLETED','서울특별시 강남구 압구정로 343',
     37.5285,127.0404,DATE_SUB(NOW(),INTERVAL 16 HOUR)),

    (1310,1003,9,'COMPLETED','서울특별시 강남구 청담동 123',
     37.5235,127.0470,DATE_SUB(NOW(),INTERVAL 15 HOUR));


INSERT INTO delivery_stop (
    id,
    delivery_plan_id,
    sequence,
    status,
    address,
    latitude,
    longitude,
    completed_at
)
VALUES
    (1401,1004,0,'READY','서울특별시 강남구 테헤란로 152',
     37.5000,127.0365,NULL),

    (1402,1004,1,'READY','서울특별시 강남구 역삼로 180',
     37.5009,127.0351,NULL),

    (1403,1004,2,'READY','서울특별시 강남구 압구정로 343',
     37.5285,127.0404,NULL),

    (1404,1004,3,'READY','서울특별시 강남구 도산대로 45',
     37.5222,127.0365,NULL),

    (1405,1004,4,'READY','서울특별시 강남구 삼성로 212',
     37.5070,127.0630,NULL),

    (1406,1004,5,'READY','서울특별시 강남구 논현로 508',
     37.5140,127.0310,NULL),

    (1407,1004,6,'READY','서울특별시 강남구 선릉로 420',
     37.5045,127.0489,NULL),

    (1408,1004,7,'READY','서울특별시 강남구 학동로 342',
     37.5150,127.0400,NULL),

    (1409,1004,8,'READY','서울특별시 강남구 봉은사로 524',
     37.5147,127.0580,NULL),

    (1410,1004,9,'READY','서울특별시 강남구 청담동 123',
     37.5235,127.0470,NULL);


-- 배송지별 상품
INSERT INTO delivery_item (
    delivery_stop_id,
    product_name,
    product_type,
    quantity
)
VALUES
    (1101, '생수', 'NORMAL', 3),
    (1102, '샐러드 세트', 'REFRIGERATED', 2),
    (1103, '냉동만두', 'FROZEN', 4),
    (1104, '와인잔 세트', 'FRAGILE', 1),
    (1105, '생활용품 박스', 'NORMAL', 2),
    (1106, '주방용품', 'NORMAL', 3),
    (1107, '수제 디저트', 'REFRIGERATED', 1),
    (1108, '아이스크림 세트', 'FROZEN', 2),
    (1109, '도자기 세트', 'FRAGILE', 5),
    (1110, '반려동물 용품', 'NORMAL', 2),
    (1201, '신선 우유', 'REFRIGERATED', 2),
    (1202, '사무용품', 'NORMAL', 5),
    (1203, '냉동 도시락', 'FROZEN', 3),
    (1204, '유리 화병', 'FRAGILE', 1),
    (1205, '과일 선물세트', 'REFRIGERATED', 2),
    (1206, '세제 묶음', 'NORMAL', 4),
    (1207, '캠핑용품', 'NORMAL', 2),
    (1208, '유아 식품', 'REFRIGERATED', 3),
    (1209, '냉동 육류', 'FROZEN', 1),
    (1210, '조명 기구', 'FRAGILE', 4),
    (1301, '도서', 'NORMAL', 2),
    (1302, '케이크', 'REFRIGERATED', 1),
    (1303, '냉동식품', 'FROZEN', 3),
    (1304, '의류', 'NORMAL', 2),
    (1305, '요거트 세트', 'REFRIGERATED', 4),
    (1306, '냉동 해산물', 'FROZEN', 1),
    (1307, '유리 식기', 'FRAGILE', 3),
    (1308, '휴지 묶음', 'NORMAL', 2),
    (1309, '치즈 세트', 'REFRIGERATED', 1),
    (1310, '전자기기', 'FRAGILE', 5),
    (1401, '생필품 세트', 'NORMAL', 2),
    (1402, '신선 채소', 'REFRIGERATED', 3),
    (1403, '디저트 세트', 'REFRIGERATED', 1),
    (1404, '냉동 간편식', 'FROZEN', 4),
    (1405, '유리 보관용기', 'FRAGILE', 2),
    (1406, '사무용품 박스', 'NORMAL', 5),
    (1407, '아이스크림', 'FROZEN', 1),
    (1408, '화장품 세트', 'FRAGILE', 2),
    (1409, '유제품', 'REFRIGERATED', 3),
    (1410, '반려동물 사료', 'NORMAL', 2);

-- 모든 배송지에 1:1 위험도 평가 생성
INSERT INTO risk_assessment (
    id,
    delivery_stop_id,
    level,
    analyzed_at
)
VALUES
    (2101, 1101, 'SAFE', NOW()),
    (2102, 1102, 'SAFE', NOW()),
    (2103, 1103, 'CAUTION', NOW()),
    (2104, 1104, 'DANGER', NOW()),
    (2105, 1105, 'UNKNOWN', NOW()),
    (2106, 1106, 'SAFE', NOW()),
    (2107, 1107, 'CAUTION', NOW()),
    (2108, 1108, 'DANGER', NOW()),
    (2109, 1109, 'SAFE', NOW()),
    (2110, 1110, 'UNKNOWN', NOW()),
    (2201, 1201, 'SAFE', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (2202, 1202, 'CAUTION', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (2203, 1203, 'DANGER', NOW()),
    (2204, 1204, 'SAFE', NOW()),
    (2205, 1205, 'CAUTION', NOW()),
    (2206, 1206, 'UNKNOWN', NOW()),
    (2207, 1207, 'SAFE', NOW()),
    (2208, 1208, 'CAUTION', NOW()),
    (2209, 1209, 'DANGER', NOW()),
    (2210, 1210, 'SAFE', NOW()),
    (2301, 1301, 'SAFE', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (2302, 1302, 'CAUTION', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (2303, 1303, 'DANGER', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (2304, 1304, 'SAFE', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (2305, 1305, 'CAUTION', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (2306, 1306, 'DANGER', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (2307, 1307, 'SAFE', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (2308, 1308, 'CAUTION', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (2309, 1309, 'SAFE', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (2310, 1310, 'UNKNOWN', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (2401, 1401, 'SAFE', NOW()),
    (2402, 1402, 'CAUTION', NOW()),
    (2403, 1403, 'SAFE', NOW()),
    (2404, 1404, 'DANGER', NOW()),
    (2405, 1405, 'UNKNOWN', NOW()),
    (2406, 1406, 'SAFE', NOW()),
    (2407, 1407, 'CAUTION', NOW()),
    (2408, 1408, 'DANGER', NOW()),
    (2409, 1409, 'SAFE', NOW()),
    (2410, 1410, 'UNKNOWN', NOW());

-- 점수는 RiskFactorType 기준으로 엔티티에서 자동 합산된다.
INSERT INTO risk_factor (
    risk_assessment_id,
    type,
    description
)
VALUES
    (2102, 'HEAT_WAVE', '폭염'),
    (2103, 'WEATHER_WARNING', '기상 특보'),
    (2104, 'HEAVY_RAIN', '폭우'),
    (2104, 'WEATHER_WARNING', '기상 특보'),
    (2106, 'HEAT_WAVE', '폭염'),
    (2107, 'WEATHER_WARNING', '기상 특보'),
    (2108, 'HEAVY_RAIN', '폭우'),
    (2108, 'WEATHER_WARNING', '기상 특보'),
    (2109, 'HEAT_WAVE', '폭염'),
    (2202, 'WEATHER_WARNING', '기상 특보'),
    (2203, 'HEAVY_RAIN', '폭우'),
    (2203, 'WEATHER_WARNING', '기상 특보'),
    (2204, 'HEAT_WAVE', '폭염'),
    (2205, 'HEAVY_RAIN', '폭우'),
    (2205, 'HEAT_WAVE', '폭염'),
    (2207, 'HEAT_WAVE', '폭염'),
    (2208, 'WEATHER_WARNING', '기상 특보'),
    (2209, 'HEAVY_RAIN', '폭우'),
    (2209, 'WEATHER_WARNING', '기상 특보'),
    (2210, 'HEAT_WAVE', '폭염'),
    (2302, 'WEATHER_WARNING', '기상 특보'),
    (2303, 'HEAVY_RAIN', '폭우'),
    (2303, 'WEATHER_WARNING', '기상 특보'),
    (2304, 'HEAT_WAVE', '폭염'),
    (2305, 'WEATHER_WARNING', '기상 특보'),
    (2306, 'HEAVY_RAIN', '폭우'),
    (2306, 'WEATHER_WARNING', '기상 특보'),
    (2307, 'HEAT_WAVE', '폭염'),
    (2308, 'HEAVY_RAIN', '폭우'),
    (2308, 'HEAT_WAVE', '폭염'),
    (2401, 'HEAT_WAVE', '폭염'),
    (2402, 'WEATHER_WARNING', '기상 특보'),
    (2403, 'HEAT_WAVE', '폭염'),
    (2404, 'HEAVY_RAIN', '폭우'),
    (2404, 'WEATHER_WARNING', '기상 특보'),
    (2406, 'HEAT_WAVE', '폭염'),
    (2407, 'WEATHER_WARNING', '기상 특보'),
    (2408, 'HEAVY_RAIN', '폭우'),
    (2408, 'WEATHER_WARNING', '기상 특보'),
    (2409, 'HEAT_WAVE', '폭염');

select * from risk_factor;
select * from risk_assessment;
select * from delivery_stop;
select * from delivery_plan;
select * from weather;

