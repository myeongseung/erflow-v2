package com.erflow.layout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

/**
 * 레이아웃 seed 가 실제 DB 에 제대로 들어갔는지 확인한다.
 *
 * <p>연결된 MariaDB 를 상대로 돈다. 접속 설정이 없는 환경(CI 등)에서는 건너뛴다.
 * 임베디드 DB 로 대체하지 않는 이유는, collation 처럼 DB 구현에 따라 달라지는 것이
 * 이 프로젝트의 검증 대상이기 때문이다.
 */
@SpringBootTest(properties = "server.port=0")
@ActiveProfiles("local")
class MenuSeedTest {

    @Autowired
    private DataSource dataSource;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    private long count(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private String one(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }

    @Test
    @DisplayName("program / screen / menu seed 가 적재되어 있다")
    void seedIsLoaded() throws Exception {
        assertThat(count("SELECT COUNT(*) FROM program")).isEqualTo(20);
        assertThat(count("SELECT COUNT(*) FROM screen")).isEqualTo(49);
        assertThat(count("SELECT COUNT(*) FROM menu")).isEqualTo(27);
    }

    @Test
    @DisplayName("메뉴가 화면을 거쳐 권한까지 이어진다")
    void menuResolvesToProgram() throws Exception {
        String program = one("""
                SELECT pr.name FROM menu m
                  JOIN screen s  ON m.screen_id = s.screen_id
                  JOIN program pr ON s.program_id = pr.program_id
                 WHERE m.url = '/unit/list'
                """);
        assertThat(program).isEqualTo("생산 설비 관리");
    }

    @Test
    @DisplayName("한 화면이 파라미터에 따라 다른 권한을 요구한다")
    void conditionalPermissionPerParameter() throws Exception {
        // 레거시 companyList.jsp 는 switch (paramFlag) 로 PROGRAM_CODE 를 골랐다.
        // 파일에서 첫 코드만 읽으면 절반이 엉뚱한 권한에 붙는다 (D-008 철회, D-016).
        assertThat(one("""
                SELECT p.name FROM screen s JOIN program p ON s.program_id = p.program_id
                 WHERE s.route = '/company/list' AND s.param_name = 'flag' AND s.param_value = '1'
                """)).isEqualTo("구매 협력업체 관리");
        assertThat(one("""
                SELECT p.name FROM screen s JOIN program p ON s.program_id = p.program_id
                 WHERE s.route = '/company/list' AND s.param_name = 'flag' AND s.param_value = '0'
                """)).isEqualTo("영업 협력업체 관리");
        assertThat(count("SELECT COUNT(*) FROM screen WHERE param_name IS NOT NULL"))
                .as("10개 화면 x 2가지 조건")
                .isEqualTo(20);
    }

    @Test
    @DisplayName("메뉴에 없는 하위 화면도 권한 대상이다")
    void hiddenScreensAreProtected() throws Exception {
        // 레거시는 unit 도메인 6개 JSP 전부에 같은 PROGRAM_CODE 를 박아뒀다.
        // 메뉴로 들어오든 URL 을 직접 치든 같은 권한이 걸려야 한다.
        assertThat(count("SELECT COUNT(*) FROM screen WHERE route LIKE '/unit/%'")).isEqualTo(6);
    }

    @Test
    @DisplayName("레거시와 같은 collation 이라 한글 정렬이 어긋나지 않는다")
    void collationMatchesLegacy() throws Exception {
        // D-006 / O-005. 레거시 라이브 DB 도 utf8mb4_general_ci 다.
        String collation = one("""
                SELECT DEFAULT_COLLATION_NAME FROM information_schema.SCHEMATA
                 WHERE SCHEMA_NAME = DATABASE()
                """);
        assertThat(collation).isEqualTo("utf8mb4_general_ci");
    }
}
