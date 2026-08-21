package com.erflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

/**
 * 레거시에서 복제한 스키마·데이터가 정답과 어긋나지 않는지 확인한다.
 *
 * <p>레거시({@code erflow})가 덤프가 아니라 동작하는 DB 로 존재하므로, 이관 대상
 * ({@code erflow_mig})과 실제 데이터로 대조할 수 있다. 스키마를 다시 옮기거나
 * 도메인 이관 중 데이터를 건드렸을 때 이 테스트가 먼저 깨진다.
 */
@SpringBootTest(properties = "server.port=0")
@ActiveProfiles("local")
class LegacyCloneTest {

    /** 레거시가 이관 대상 화면에 실제로 쓰는 뷰들. */
    private static final List<String> VIEWS =
            List.of("unit_view", "user_view", "work_view", "proposal_view", "post_view");

    @Autowired
    private DataSource dataSource;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    private <T> T query(String sql, java.util.function.Function<ResultSet, T> reader) throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            return reader.apply(rs);
        }
    }

    private long count(String sql) throws Exception {
        return query(sql, rs -> {
            try {
                rs.next();
                return rs.getLong(1);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Test
    @DisplayName("레거시 테이블 32개와 뷰 15개가 모두 복제되어 있다")
    void allObjectsCloned() throws Exception {
        assertThat(count("""
                SELECT COUNT(*) FROM information_schema.tables
                 WHERE table_schema = 'erflow_mig' AND table_type = 'BASE TABLE'
                """))
                .as("레거시 32개 + 레이아웃 3개(program/screen/menu)")
                .isEqualTo(35);
        assertThat(count("""
                SELECT COUNT(*) FROM information_schema.views WHERE table_schema = 'erflow_mig'
                """))
                .isEqualTo(15);
    }

    @Test
    @DisplayName("모든 테이블의 행수가 레거시와 일치한다")
    void rowCountsMatchLegacy() throws Exception {
        List<String> mismatched = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {

            List<String> tables = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery("""
                    SELECT table_name FROM information_schema.tables
                     WHERE table_schema = 'erflow' AND table_type = 'BASE TABLE'
                     ORDER BY table_name
                    """)) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }
            assertThat(tables).hasSize(32);

            for (String table : tables) {
                long legacy = scalar(statement, "SELECT COUNT(*) FROM `erflow`.`" + table + "`");
                long migrated = scalar(statement, "SELECT COUNT(*) FROM `erflow_mig`.`" + table + "`");
                if (legacy != migrated) {
                    mismatched.add("%s: %d != %d".formatted(table, legacy, migrated));
                }
            }
        }
        assertThat(mismatched).isEmpty();
    }

    @Test
    @DisplayName("뷰가 레거시와 같은 결과를 돌려준다")
    void viewsReturnSameRows() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            for (String view : VIEWS) {
                long legacy = scalar(statement, "SELECT COUNT(*) FROM `erflow`.`" + view + "`");
                long migrated = scalar(statement, "SELECT COUNT(*) FROM `erflow_mig`.`" + view + "`");
                assertThat(migrated).as(view).isEqualTo(legacy);
            }
        }
    }

    @Test
    @DisplayName("한글 정렬 결과가 레거시와 완전히 같다")
    void koreanSortMatchesLegacy() throws Exception {
        // D-006 / O-005. 양쪽 모두 utf8mb4_general_ci 라 순서가 어긋날 수 없다.
        // collation 을 건드리면 이 테스트가 먼저 깨진다.
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            String legacy = text(statement, """
                    SELECT GROUP_CONCAT(unit_name ORDER BY unit_name, id SEPARATOR '|')
                      FROM `erflow`.`unit_view`
                    """);
            String migrated = text(statement, """
                    SELECT GROUP_CONCAT(unit_name ORDER BY unit_name, id SEPARATOR '|')
                      FROM `erflow_mig`.`unit_view`
                    """);
            assertThat(migrated).isEqualTo(legacy);
        }
    }

    @Test
    @DisplayName("레거시는 쓰기가 막혀 있다")
    void legacyIsReadOnly() throws Exception {
        // 관례가 아니라 권한이다. 앱 계정은 erflow 에 SELECT 만 갖는다 (D-011).
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            assertThatThrownBy(
                    () -> statement.execute("CREATE TABLE `erflow`.`__harness_probe` (id INT)"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("denied");
        }
    }

    private static long scalar(Statement statement, String sql) throws Exception {
        try (ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static String text(Statement statement, String sql) throws Exception {
        try (ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }
}
