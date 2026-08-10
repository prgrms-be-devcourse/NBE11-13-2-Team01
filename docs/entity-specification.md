# Delivery Insight 엔티티 명세

## 1. 개요

Delivery Insight는 배송 계획과 외부 환경 데이터를 바탕으로 배송 구간별 위험도를 분석하는 서비스이다.

엔티티는 다음 흐름을 기준으로 구성한다.

```text
배송 계획 → 배송지 → 이동 구간 → 위험도 분석
```

차량은 상온·냉장·냉동 적재 능력을 구분하지 않으며, 모든 차량이 모든 상품을 상차할 수 있다고 가정한다. 상품의 냉장·냉동 여부는 차량 배정이 아니라 위험도 계산에만 사용한다.

---

## 2. User

서비스 사용자와 권한을 관리한다.

| 필드 | 설명 |
| --- | --- |
| `id` | 사용자 식별자 |
| `loginId` | 로그인 아이디 |
| `password` | 암호화된 비밀번호 |
| `name` | 사용자 이름 |
| `role` | 사용자 권한 |

### Role

- `DELIVERY_DRIVER`: 배송 기사
- `ADMIN`: 관리자

기사와 관리자를 별도 엔티티로 분리하지 않고 `role`로 구분한다.

---

## 3. Vehicle

배송에 사용하는 차량을 관리한다.

| 필드 | 설명 |
| --- | --- |
| `id` | 차량 식별자 |
| `vehicleNumber` | 차량 번호 |

차량은 특정 기사에게 고정하지 않는다. 배송 계획을 만들 때 기사와 차량을 각각 배정한다.

차량의 상온·냉장·냉동 타입은 관리하지 않는다.

---

## 4. DeliveryPlan

기사의 하루 또는 특정 시간대 전체 배송 계획을 관리하는 핵심 엔티티이다.

| 필드 | 설명 |
| --- | --- |
| `id` | 배송 계획 식별자 |
| `driver` | 담당 기사 |
| `vehicle` | 배정 차량 |
| `departureLocation` | 출발지 주소 |
| `scheduledDepartureAt` | 예정 출발 시각 |
| `actualDepartureAt` | 실제 출발 시각 |
| `status` | 배송 진행 상태 |
| `overallRiskScore` | 전체 배송 위험 점수 |
| `createdAt` | 배송 계획 생성 시각 |

### DeliveryStatus

- `READY`: 배송 준비
- `LOADING`: 상차 진행
- `DELIVERING`: 배송 진행
- `COMPLETE`: 배송 완료

예를 들어 `서울 물류센터 → 역삼 → 방배 → 잠실`이라는 전체 일정이 하나의 `DeliveryPlan`이다.

---

## 5. DeliveryStop

배송 계획에 포함된 개별 배송지를 관리한다.

| 필드 | 설명 |
| --- | --- |
| `id` | 배송지 식별자 |
| `deliveryPlan` | 소속 배송 계획 |
| `sequence` | 배송 순서 |
| `address` | 배송지 주소 |
| `latitude` | 배송지 위도 |
| `longitude` | 배송지 경도 |
| `scheduledArrivalAt` | 예정 도착 시각 |
| `actualArrivalAt` | 실제 도착 시각 |
| `status` | 배송지 처리 상태 |

### DeliveryStopStatus

- `WAITING`: 방문 전
- `ARRIVED`: 배송지 도착
- `COMPLETED`: 해당 배송지의 배송 완료

`sequence`를 이용해 배송 순서를 표현한다.

```text
1 역삼
2 방배
3 잠실
```

동일한 배송 계획 안에서 `sequence`는 중복될 수 없다. 배송 순서 추천이 적용되면 이 값을 변경한다.

---

## 6. DeliveryItem

각 배송지에서 전달할 상품 정보를 관리한다.

| 필드 | 설명 |
| --- | --- |
| `id` | 배송 상품 식별자 |
| `deliveryStop` | 상품을 전달할 배송지 |
| `productName` | 상품명 |
| `productType` | 상품 종류 |
| `quantity` | 배송 수량 |

### ProductType

- `NORMAL`: 상온 상품
- `REFRIGERATED`: 냉장 상품
- `FROZEN`: 냉동 상품
- `FRAGILE`: 파손주의 상품

MVP에서는 별도의 `Product` 엔티티를 만들지 않는다. 상품명, 종류, 수량을 배송 당시의 스냅샷으로 저장한다.

상품 종류는 위험도 계산에 사용하지만 차량의 적재 가능 여부를 판단하는 데에는 사용하지 않는다.

---

## 7. RouteSegment

출발지와 배송지 또는 배송지와 배송지 사이의 이동 구간을 관리한다.

| 필드 | 설명 |
| --- | --- |
| `id` | 이동 구간 식별자 |
| `deliveryPlan` | 소속 배송 계획 |
| `fromStop` | 출발 배송지 |
| `toStop` | 도착 배송지 |
| `sequence` | 구간 순서 |
| `distanceMeters` | 이동 거리(m) |
| `estimatedDurationMinutes` | 예상 이동 시간(분) |

예시는 다음과 같다.

```text
1구간 서울 물류센터 → 역삼
2구간 역삼 → 방배
3구간 방배 → 잠실
```

첫 번째 구간은 물류센터에서 출발하므로 `fromStop`을 비워두고 `DeliveryPlan.departureLocation`을 사용한다.

동일한 배송 계획 안에서 `sequence`는 중복될 수 없다. 위험도 분석은 배송지가 아닌 이동 구간을 기준으로 수행한다.

---

## 8. RiskAssessment

각 이동 구간의 위험도 분석 결과를 관리한다.

| 필드 | 설명 |
| --- | --- |
| `id` | 위험도 분석 식별자 |
| `routeSegment` | 분석 대상 이동 구간 |
| `score` | 위험 점수 |
| `level` | 위험 단계 |
| `analyzedAt` | 분석 시각 |

### RiskLevel

- `SAFE`: 안전
- `CAUTION`: 주의
- `DANGER`: 위험

MVP에서는 하나의 이동 구간이 현재 위험도 분석 결과 하나를 가진다. 재분석하면 기존 결과를 갱신한다.

```text
구간: 방배 → 잠실
점수: 50점
단계: DANGER
```

---

## 9. RiskFactor

위험 점수가 계산된 구체적인 원인을 관리한다.

| 필드 | 설명 |
| --- | --- |
| `id` | 위험 요소 식별자 |
| `riskAssessment` | 소속 위험도 분석 결과 |
| `type` | 위험 요소 종류 |
| `penaltyScore` | 감점 점수 |
| `description` | 위험 요소 상세 설명 |

### RiskFactorType

- `HEAVY_RAIN`: 폭우
- `HEAT_WAVE`: 폭염
- `LONG_DISTANCE`: 장거리 이동
- `REFRIGERATED`: 냉장 상품 포함
- `FROZEN`: 냉동 상품 포함
- `TRAFFIC_CONGESTION`: 교통 혼잡
- `WEATHER_WARNING`: 기상 특보

`penaltyScore`는 음수가 아니라 감점할 점수의 크기를 양수로 저장한다.

```text
기본 점수 100
폭우 30점 감점
냉장 상품 20점 감점
최종 점수 50
```

`RiskFactor`를 통해 위험 점수뿐 아니라 해당 구간이 위험한 이유도 제공할 수 있다.

---

## 10. 엔티티 관계

```text
User ─────────────┐
                  │ 담당 기사
Vehicle ──────────┤ 배정 차량
                  ▼
            DeliveryPlan
             ├── DeliveryStop
             │     └── DeliveryItem
             │
             └── RouteSegment
                    └── RiskAssessment
                           └── RiskFactor
```

- 사용자 한 명은 여러 배송 계획을 담당할 수 있다.
- 차량 한 대는 여러 배송 계획에 배정될 수 있다.
- 배송 계획 하나는 여러 배송지를 가진다.
- 배송지 하나는 여러 배송 상품을 가진다.
- 배송 계획 하나는 여러 이동 구간을 가진다.
- 이동 구간 하나는 현재 위험도 결과 하나를 가진다.
- 위험도 결과 하나는 여러 위험 요소를 가진다.

---

## 11. 핵심 처리 흐름

```text
배송 계획 등록
    ↓
배송지와 배송 순서 등록
    ↓
배송 상품 등록
    ↓
배송지 사이 이동 구간 생성
    ↓
지도·날씨 데이터 조회
    ↓
구간별 위험도 계산
    ↓
전체 배송 위험도 제공
```

설계의 핵심은 다음 네 엔티티이다.

```text
DeliveryPlan
    ↓
DeliveryStop
    ↓
RouteSegment
    ↓
RiskAssessment
```

각각 전체 배송 계획, 개별 배송지, 배송지 사이의 이동 구간, 구간별 위험도 결과를 담당한다.
