package com.erflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.ResultSet;
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
 * 권한 프로그램 이름이 메뉴 라벨과 맞는지 확인한다 (D-129, O-003·O-004 해소).
 *
 * <p>레거시는 메뉴 라벨과 권한 프로그램명이 17개 중 10개나 달랐다(D-004). 관리자가
 * 권한 화면에서 보는 이름과 사용자가 메뉴에서 보는 이름이 다르면 어느 화면의 권한을
 * 고치는지 헷갈린다 — 프로그램 이름은 그 화면의 메뉴 라벨을 따른다.
 *
 * <p>한 라벨이 두 화면을 가리키는 곳(«협력업체 관리» — 구매·영업)만 수식어를 남긴다.
 * 그래서 판정은 «같거나, 수식어 뒤에 라벨이 온다» 다.
 */
@SpringBootTest(properties = "server.port=0")
@ActiveProfiles("local")
class ProgramNameAlignmentTest {

    @Autowired
    private DataSource dataSource;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    @Test
    @DisplayName("메뉴에 걸린 프로그램의 이름은 그 메뉴 라벨을 따른다")
    void programNamesFollowMenuLabels() throws Exception {
        List<String[]> pairs = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("""
                        SELECT m.label, p.name
                          FROM menu m
                          JOIN screen s ON m.screen_id = s.screen_id
                          JOIN program p ON s.program_id = p.program_id""")) {
            while (rs.next()) {
                pairs.add(new String[] {rs.getString(1), rs.getString(2)});
            }
        }
        assertThat(pairs).as("메뉴에 걸린 프로그램").isNotEmpty();
        assertThat(pairs).allSatisfy(pair -> assertThat(pair[1])
                .as("메뉴 «%s» 의 프로그램 이름", pair[0])
                .satisfiesAnyOf(
                        name -> assertThat(name).isEqualTo(pair[0]),
                        name -> assertThat(name).endsWith(" " + pair[0])));
    }

    @Test
    @DisplayName("두 이름 테이블이 항상 같은 이름을 갖는다")
    void bothNameTablesAgree() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("""
                        SELECT COUNT(*)
                          FROM program p
                          JOIN permission_program_tbl l ON p.program_id = l.program_id
                         WHERE p.name != l.program_name""")) {
            rs.next();
            assertThat(rs.getInt(1))
                    .as("program.name 과 permission_program_tbl.program_name 이 다른 행")
                    .isZero();
        }
    }

    @Test
    @DisplayName("오타였던 «결재관리 관리» 는 더 이상 없다")
    void typoIsGone() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(
                        "SELECT COUNT(*) FROM program WHERE name = '결재관리 관리'")) {
            rs.next();
            assertThat(rs.getInt(1)).isZero();
        }
    }
}
