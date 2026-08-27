# erflow-v2 — ERFlow 기능·디자인 업그레이드

[parity-harness](https://github.com/myeongseung/parity-harness) 에서 레거시를 1:1
이관한 완성본(`phase2-complete`)을 넘겨받아 발전시키는 프로젝트. **전작과 달리
레거시는 더 이상 정답이 아니다** — 화면을 다시 그리고 기능을 더한다. 대신 규율은
남는다: 바꾸는 것마다 근거를 결정 로그에 적는다.

```
src/          Spring Boot 앱 (도메인별 패키지)
docs/         decisions.md (이 저장소의 결정 로그, D-116~)
              legacy-decisions.md (전작 D-001~D-115 사본, 읽기 전용)
```

## 지켜야 할 것

**결정 로그가 규율이다.** 화면·동작·스키마를 바꾸면 `docs/decisions.md` 에 «왜» 를
남긴다. 번호는 전작에 이어 D-116 부터 — 코드 주석의 D-번호가 두 로그에 걸쳐
유일해야 한다. `legacy-decisions.md` 는 수정하지 않는다.

**정답 스키마는 여전히 읽기 전용이다.** 앱 계정은 레거시 스키마 `erflow` 에
SELECT 만 갖는다(권한으로 강제). 스키마는 셋이다(D-116) — `erflow`(정답, 읽기
전용) · `erflow_mig`(전작 1단계 산출물, **동결**) · `erflow_v2`(이 저장소의 앱
스키마). **수정은 `erflow_v2` 에만.** 매퍼의 테이블명이 비수식이라 어느 스키마를
쓰는지는 `application-local.yml` 의 URL 하나가 정한다.

**커밋 전 `./gradlew build`.** Checkstyle 위반은 빌드 실패다. 테스트 386건 중
대부분은 DB 를 상대로 하므로 `application-local.yml` 이 있어야 돈다.

**전작의 함정은 그대로 유효하다:**

| | |
|---|---|
| **비트마스크 판정** | MariaDB 비트 연산은 부호가 없다. SQL 로 옮기면 관리자 판정이 뒤집힌다. Java 에서 계산한다 |
| **비밀번호 해시** | 저장 형식은 bcrypt 로 승격됐다(D-128). 레거시 재현 인코더는 겹씌움({erflow-bcrypt})의 안쪽 계산에 아직 쓰이므로 지우면 안 된다 |
| **MyBatis 세션 캐시** | 트랜잭션에 묶인 시험에서 서비스로 먼저 읽으면 뒤의 JDBC 삽입이 안 보인다. 개수는 JDBC 로 세고 서비스 읽기는 삽입 뒤에 |
| **비밀은 눈으로 못 막는다** | 문서·주석·테스트에 비밀번호나 호스트명을 쓰지 않는다. 접속 정보는 gitignore 된 application-local.yml 에만 |
| **실행 권한** | Windows 커밋 셸 스크립트는 실행 비트가 빠진다. `git update-index --chmod=+x <파일>` |

## 착수 순서 (전작에서 넘어온 목록)

`docs/legacy-decisions.md` 종결 절 참조. 요약:

1. ~~**보안 우선** — O-010(게시글 본문 `th:utext` XSS), O-012(주민번호 노출)~~
   ✅ 둘 다 해소 — D-118(허용목록 정화) · D-117(뒷자리 한 자리)
2. ~~**화면 재설계급** — D-077(근무 통계 한 줄), D-068(라벨 통일), D-069(그래프)~~
   ✅ 셋 다 해소 — D-127 · D-125 · D-126
3. ~~O-002·003·004(데드 레코드·오타·용어), O-006(해시)~~ ✅ 해소 — D-129 · D-128
4. 나머지 미결 — O-007(비밀번호=사번, 운영 판단), O-009(고아 첨부), O-011(메일 발송),
   O-013(DeleteProcess 프로시저)

## 자주 쓰는 명령

```bash
./gradlew build                                           # 컴파일 + Checkstyle + 테스트
./gradlew bootRun --args='--spring.profiles.active=local' # 기동 (기본 8080)
```
