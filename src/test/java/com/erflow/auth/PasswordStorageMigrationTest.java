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
import org.springframework.transaction.annotation.Transactional;

/**
 * 비밀번호 저장 형식 승격이 실제 DB 에 반영됐는지 확인한다 (D-128).
 *
 * <p>{@code @SpringBootTest} 가 컨텍스트를 띄우면서 {@link PasswordStorageMigration}
 * 이 이미 돌았다 — 이 시험은 그 결과를 본다.
 */
@SpringBootTest(properties = "server.port=0")
@ActiveProfiles("local")
class PasswordStorageMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ErflowUserDetailsService userDetailsService;

    @Autowired
    private AuthMapper authMapper;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    private List<String[]> storedPasswords() throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(
                        "SELECT id, password FROM user_tbl WHERE password IS NOT NULL")) {
            while (rs.next()) {
                rows.add(new String[] {rs.getString(1), rs.getString(2)});
            }
        }
        return rows;
    }

    @Test
    @DisplayName("접두사 없는(레거시 형식) 비밀번호가 하나도 남지 않았다")
    void noLegacyFormatRemains() throws Exception {
        List<String[]> rows = storedPasswords();
        assertThat(rows).as("사용자").isNotEmpty();
        assertThat(rows)
                .as("모든 저장값이 {bcrypt} 또는 {erflow-bcrypt} 로 시작해야 한다")
                .allSatisfy(row -> assertThat(row[1])
                        .matches("^\\{(bcrypt|erflow-bcrypt)}.+"));
    }

    @Test
    @DisplayName("겹씌운 값은 salt 40글자 뒤에 bcrypt 가 온다")
    void wrappedRowsKeepSaltAndBcrypt() throws Exception {
        for (String[] row : storedPasswords()) {
            if (!row[1].startsWith("{erflow-bcrypt}")) {
                continue;
            }
            String value = row[1].substring("{erflow-bcrypt}".length());
            assertThat(value.substring(0, 40)).as("사번 %s 의 salt", row[0])
                    .matches("[0-9a-f]{40}");
            assertThat(value.substring(40)).as("사번 %s 의 bcrypt", row[0])
                    .startsWith("$2");
        }
    }

    @Test
    @DisplayName("겹씌움 형식만 로그인 성공 때 재해시 대상이 된다")
    void onlyWrappedFormatIsUpgraded() {
        String legacyStored = ErflowPasswordEncoder.generate(
                "pw", "0123456789abcdef0123456789abcdef01234567");

        assertThat(passwordEncoder.upgradeEncoding(
                        "{erflow-bcrypt}" + WrappedLegacyPasswordEncoder.wrap(legacyStored)))
                .as("겹씌움 형식은 승격 대상")
                .isTrue();
        assertThat(passwordEncoder.upgradeEncoding(passwordEncoder.encode("pw")))
                .as("이미 bcrypt 면 그대로")
                .isFalse();
        assertThat(passwordEncoder.upgradeEncoding(legacyStored))
                .as("접두사 없는 레거시 형식도 승격 대상")
                .isTrue();
    }

    @Test
    @DisplayName("재해시 훅이 저장값을 실제로 바꾼다")
    @Transactional
    void updatePasswordWritesNewValue() throws Exception {
        String anyId = storedPasswords().get(0)[0];
        ErflowUserDetails details = userDetailsService.loadUserByUsername(anyId);
        String newValue = passwordEncoder.encode("새비밀번호123");

        ErflowUserDetails updated = userDetailsService.updatePassword(details, newValue);

        assertThat(updated.getPassword()).isEqualTo(newValue);
        assertThat(authMapper.findAuthUser(anyId).password()).isEqualTo(newValue);
    }
}
