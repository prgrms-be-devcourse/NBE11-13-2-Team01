# Delivery Insight Web

Delivery Insight Spring Boot API와 연결되는 배송 기사용 React 애플리케이션입니다.

## 실행

백엔드를 `http://localhost:8080`에서 먼저 실행한 뒤:

```bash
npm install
npm run dev
```

브라우저에서 `http://localhost:5173`으로 접속합니다. 개발 서버는 `/api` 요청을 백엔드로 프록시하므로 별도 CORS 설정이 필요하지 않습니다.

## 명령어

- `npm run dev`: 개발 서버
- `npm run build`: 타입 검사 및 배포 빌드
- `npm run lint`: Oxlint 정적 검사
- `npm run preview`: 빌드 결과 미리보기

## 환경 변수

개발 프록시를 사용하지 않는 환경에서는 `.env`에 API 주소를 지정합니다.

```dotenv
VITE_API_BASE_URL=https://api.example.com
```

출발지와 배송지 선택에는 카카오 우편번호 서비스를 사용하므로 프론트엔드
API 키가 필요하지 않습니다. 선택된 주소의 좌표는 배송 계획 생성 시 백엔드의
기존 카카오 로컬 API 연동으로 변환됩니다.

## 역할별 화면

- 공통 로그인 및 Access Token 관리
- 만료된 Access Token 자동 재발급

### ADMIN

- 전체 배송 기사의 계획 및 담당 기사 조회
- 배송 기사 목록 조회
- 특정 배송 기사에게 배송 계획 생성 및 할당
- 카카오 우편번호 서비스를 통한 서울 지역 출발지·배송지 선택
- 배송 계획 상세와 위험 요인 조회

### DELIVERY_DRIVER

- 본인에게 할당된 배송 계획만 조회
- 지도에서 출발지와 배송지 순서 확인
- READY 상태에서 마우스·터치 드래그로 배송 순서 변경
- 배송 시작, 배송지 완료, 전체 배송 완료
- 배송 계획 생성 및 출발 시각 변경 불가
