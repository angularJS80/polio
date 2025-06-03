# 🔐 Polio: Keycloak Dynamic Authorization Adapter for Spring Boot 3.5+

**Polio**는 Spring Boot 3.5 기반 REST API 프로젝트에서 Keycloak의 권한(Resource, Scope, Role, Policy, Permission)을 동적으로 조회하고,
Spring Security Filter Chain에 자동 반영하는 경량 어댑터 라이브러리입니다.

---

## 🚀 현재 진행 중인 작업

- Keycloak Admin REST API를 이용해 `Permission`, `Resource`, `Role`, `Scope` 정보를 애플리케이션 실행 시점에 동기화
- Keycloak의 정책 정의를 기반으로 URI 및 HTTP 메서드에 따른 동적 접근 제어 구성
- 기존 Keycloak Adapter의 `enforce-mode` 기능을 Spring Security로 재구현
- 오픈소스 라이브러리로 분리 및 배포 준비 중 (`Gradle` 의존성 형태로 사용할 수 있도록 구조화)

---

## 🧩 주요 기능

- ✅ Keycloak Permission → Spring Security Filter 자동 매핑
- ✅ Role + Scope 기반 접근 제어
- ✅ JWT Claim에서 Role 및 Scope 추출 (`realm_access`, `resource_access`, `scope`)
- ✅ 코드 기반 설정으로 유연한 보안 구성
- ⏳ 향후: Caching, WebFlux 지원, 테스트 커버리지 강화

---

## 📁 모듈 구조

## 📁 모듈 구조

polio
├── application
│ └── keycloak
│ ├── service/
│ └── dto/
├── infrastructure
│ └── keycloak
│ ├── client/
│ ├── dto/
│ └── prop/
└── presentation
└── security
└── config/


---

## 📜 예제 설정

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/myrealm

keycloak:
  server-url: http://localhost:8080
  realm: myrealm
  client-id: myclient
  client-secret: ${KEYCLOAK_CLIENT_SECRET}

🧪 개발 환경
Java 17+

Spring Boot 3.5.x

Keycloak 24+

Gradle 8+

IntelliJ + Codium

📌 참고 예정 기능
 Resource 및 Permission 변경사항 실시간 동기화

 Keycloak 정책 시각화 대시보드

 다양한 Scope 매핑 방식 지원

🤝 기여
현재 구조 개선 및 초기화 단계입니다. 누구나 오픈소스 구조 개선에 기여하실 수 있도록 작업 내역은 TODO.md를 참고해주세요.

Pull Request 또는 Discussion은 언제든 환영입니다!

이 프로젝트는 MIT License를 따릅니다.
