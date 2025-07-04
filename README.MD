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



```소셜로그인 
        // 해당 URL호출시 콜백을 받는 메소드
        // http://localhost:8089/realms/polio/protocol/openid-connect/auth?client_id=polio-toy-client&redirect_uri=http://localhost:3000/auth/callback&response_type=code&scope=openid%20email%20profile&kc_idp_hint=github
        // 로그인 성공 처리 (세션 생성, 토큰 저장 등 원하는 처리 가능)
       
  
```


```keycloak docker

docker run -d \
-p 8089:8080 \
--name keycloak \
-e KEYCLOAK_ADMIN_PASSWORD=keycloakadmin \
-e KC_DB=mysql \
-e KC_DB_URL_HOST=host.docker.internal \
-e KC_DB_URL_PORT=3306 \
-e KC_DB_URL_DATABASE=keycloak \
-e KC_DB_USERNAME=keycloak \
-e KC_DB_PASSWORD=keycloak \
-e KEYCLOAK_ADMIN=keycloakadmin \
-e KC_RUN_IN_CONTAINER=true \
-e JAVA_OPTS_APPEND="-Dkeycloak.profile.feature.token_exchange=enabled -Dkeycloak.profile.feature.admin_fine_grained_authz=enabled" \
quay.io/keycloak/keycloak:24.0.3 start-dev
```


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
```
  
``` 비밀번호 리셋에 필요한 별도 클라이언트 설정
Keycloak token-exchange 권한 설정 요약
polio-toy-client-action 클라이언트 추가

Docker 실행 시 -Dkeycloak.profile.feature.token_exchange=enabled 옵션 추가

polio-toy-client-action의 Authorization 기능 활성화

Permissions > token-exchange 진입

Create Policy 클릭 → Policy Type: Client 선택

기존 클라이언트(polio-toy-client) 선택 후 저장

생성한 Policy를 token-exchange Permission에 연결
```


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
