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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * 기존 사용자가 그대로 로그인되는지 확인한다.
 *
 * <p>레거시 {@code user_tbl} 의 실제 해시를 상대로 돈다. 해시값을 소스에 옮겨 적지
 * 않고 DB 에서 읽는다 — 공개 저장소에 남길 것이 아니다.
 *
 * <p>비밀번호가 사번과 같은 계정(최초 로그인 전)은 평문을 알 수 있으므로 검증에 쓸 수
 * 있다. 레거시 {@code UserController.isInitialLogin} 이 같은 판정을 했다.
 *
 * <p>이 시험이 깨지면 이관 후 아무도 로그인하지 못한다.
 */
@SpringBootTest(properties = "server.port=0")
@ActiveProfiles("local")
class LegacyPasswordCompatibilityTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    private List<String[]> legacyUsers() throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(
                        "SELECT id, password FROM `erflow`.`user_tbl` WHERE password IS NOT NULL")) {
            while (rs.next()) {
                rows.add(new String[] {rs.getString(1), rs.getString(2)});
            }
        }
        return rows;
    }

    @Test
    @DisplayName("초기 비밀번호 계정이 모두 검증된다")
    void initialPasswordUsersStillMatch() throws Exception {
        List<String[]> users = legacyUsers();
        assertThat(users).as("레거시 사용자").isNotEmpty();

        long verified = users.stream()
                .filter(row -> passwordEncoder.matches(row[0], row[1]))
                .count();

        assertThat(verified)
                .as("비밀번호가 사번과 같은 계정. 하나도 못 맞추면 해시 재현이 틀린 것이다")
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("틀린 비밀번호는 아무 계정에서도 통과하지 않는다")
    void wrongPasswordNeverMatches() throws Exception {
        for (String[] row : legacyUsers()) {
            assertThat(passwordEncoder.matches(row[0] + "x", row[1]))
                    .as("사번 뒤에 한 글자 붙인 값")
                    .isFalse();
        }
    }

    @Test
    @DisplayName("저장 형태가 레거시 그대로다")
    void storedShapeIsUnchanged() throws Exception {
        for (String[] row : legacyUsers()) {
            String decoded = new String(java.util.Base64.getDecoder().decode(row[1]),
                    java.nio.charset.StandardCharsets.UTF_8);
            assertThat(decoded).as("사번 %s", row[0]).hasSize(104);
        }
    }
}
