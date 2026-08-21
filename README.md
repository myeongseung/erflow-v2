# erflow-v2

**레거시 그룹웨어 ERFlow 를 현대 스택으로 이관한 것을 넘겨받아, 기능과 디자인을
업그레이드하는 프로젝트.**

전작 [parity-harness](https://github.com/myeongseung/parity-harness) 에서
JSP/Servlet 89화면을 Spring Boot 로 **1:1 정합 이관**하고(발명·누락 게이트,
실화면 글자·픽셀 대조로 증명), 이관 중 발견한 레거시 결함 16건을 해소했다.
이 저장소는 그 완성본(`parity-harness@8dbb182`, 태그 `phase2-complete`)의
앱 소스에서 출발한다.

전작이 «레거시가 정답이다» 를 규율로 삼았다면, **여기서부터는 정답을 우리가
만든다** — 화면을 다시 그리고, 없던 기능을 더한다.

## 스택

| | 버전 |
|---|---|
| Spring Boot | 4.0.6 |
| Java | 21 (Temurin) |
| MyBatis Starter | 4.0.1 |
| MariaDB Driver | 3.5.x |
| Gradle | 9.7.0 (wrapper) |
| Checkstyle | 13.10.0 — `check` 에 물려 있음, 위반은 빌드 실패 |

## 실행

```bash
./gradlew build          # 컴파일 + Checkstyle + 테스트
./gradlew bootRun --args='--spring.profiles.active=local'
```

접속 정보는 `src/main/resources/application-local.yml` 에 둔다 — gitignore 되어
있고, 옆의 `.example` 을 복사해 값을 채운다. `.example` 에는 값을 적지 않는다.

```bash
cd src/main/resources
cp application-local.yml.example application-local.yml
```

DB 는 MariaDB. 앱 스키마는 `erflow_mig`, 레거시 원본 스키마 `erflow` 는
**읽기 전용**(앱 계정에 SELECT 만)이다. 테스트 348건 중 273건이 실제 DB 를
상대로 돌며, `application-local.yml` 이 없으면 스스로 건너뛴다.

## 구성 규칙

- **도메인별 패키지** (`com.erflow.unit`, `com.erflow.company`, …). 계층별이 아니다
- 계층 의존은 한 방향 — `Controller -> Service -> Mapper`
  (`src/main/java/com/erflow/package-info.java`)
- **SQL 은 XML 매퍼에만** — 쿼리의 출처·변경을 한곳에서 대조한다
- **권한 비트마스크 판정은 반드시 Java 에서** — MariaDB 비트 연산은 부호가 없어
  관리자 비트가 걸리면 뒤집힌다

## 결정 로그

바꾸는 것마다 «왜» 를 남긴다 — 없으면 나중에 원복된다.

| | |
|---|---|
| [docs/decisions.md](docs/decisions.md) | **이 저장소의 결정 로그.** D-116 부터 이어 매긴다 |
| [docs/legacy-decisions.md](docs/legacy-decisions.md) | 전작의 결정 로그 사본 (D-001~D-115, 읽기 전용). 코드 곳곳의 D-번호 주석이 여기를 가리킨다 |

전작에서 넘어온 착수 목록은 [legacy-decisions.md 의 종결 절](docs/legacy-decisions.md)에
있다 — 화면 재설계급 후보(D-077·068·069)와 미결 안건(특히 O-010 본문 XSS,
O-012 주민번호 노출은 이른 순서에 둘 것).

## 출처

ERFlow 원본은 [jUqItEr](https://github.com/jUqItEr) 님의 MIT 라이선스
프로젝트다. 이관 과정과 정합성 증명은
[parity-harness](https://github.com/myeongseung/parity-harness) 에 있다.

## 라이선스

MIT — [LICENSE](LICENSE)
