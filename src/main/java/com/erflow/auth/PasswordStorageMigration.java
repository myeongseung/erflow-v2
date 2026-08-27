package com.erflow.auth;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 기동 때 레거시 형식의 비밀번호를 전부 새 형식으로 승격한다 (D-128).
 *
 * <p>접두사({@code {bcrypt}} 등)가 없는 행만 골라 딱 두 갈래로 나눈다.
 *
 * <ul>
 *   <li><b>비밀번호가 사번과 같은 계정</b> — 평문(사번)을 아는 것이므로 바로 순수
 *       bcrypt 로 간다. 최초 로그인 판정(비밀번호=사번)도 그대로 성립한다.</li>
 *   <li><b>그 밖의 계정</b> — 평문을 모르므로 저장된 레거시 해시를 bcrypt 로 겹씌운다
 *       ({@link WrappedLegacyPasswordEncoder}). 로그인이 성공하는 순간
 *       {@link ErflowUserDetailsService#updatePassword} 가 순수 bcrypt 로 다시 쓴다.</li>
 * </ul>
 *
 * <p>승격이 끝난 행에는 손대지 않으므로 몇 번을 기동해도 무해하다. 레거시 형식으로
 * 읽히지 않는 값은 건너뛰고 경고만 남긴다 — 그 계정은 접두사 없는 값의 검증 경로
 * (레거시 인코더)로 계속 로그인할 수 있다.
 *
 * <p>DB 에 닿지 못하면 경고만 남기고 물러난다. 같은 안전망 덕에 승격이 미뤄져도
 * 로그인은 막히지 않고, 다음 기동이 다시 시도한다. 트랜잭션으로 묶지 않는 것도 같은
 * 이유다 — 행마다 독립이고 몇 번을 다시 돌아도 무해하므로, 중간에 끊겨도 이미 승격된
 * 행은 그대로 남는 편이 낫다.
 */
@Component
public class PasswordStorageMigration implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(PasswordStorageMigration.class);

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final ErflowPasswordEncoder legacyEncoder = new ErflowPasswordEncoder();

    /**
     * @param authMapper 인증 조회 매퍼
     * @param passwordEncoder 비밀번호 인코더 — 기본 형식(bcrypt)으로 인코딩한다
     */
    public PasswordStorageMigration(AuthMapper authMapper, PasswordEncoder passwordEncoder) {
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 접두사 없는 행을 모두 승격한다.
     *
     * @param args 기동 인자 (쓰지 않는다)
     */
    @Override
    public void run(ApplicationArguments args) {
        List<StoredPassword> rows;
        try {
            rows = authMapper.findLegacyPasswords();
        } catch (DataAccessException e) {
            LOG.warn("DB 에 닿지 못해 비밀번호 저장 형식 승격을 건너뛴다 — 다음 기동에서 다시 시도한다: {}",
                    e.getMessage());
            return;
        }
        if (rows.isEmpty()) {
            return;
        }
        int initial = 0;
        int wrapped = 0;
        int skipped = 0;
        for (StoredPassword row : rows) {
            String upgraded;
            if (legacyEncoder.matches(row.id(), row.password())) {
                upgraded = passwordEncoder.encode(row.id());
                initial++;
            } else {
                try {
                    upgraded = "{" + WrappedLegacyPasswordEncoder.ID + "}"
                            + WrappedLegacyPasswordEncoder.wrap(row.password());
                    wrapped++;
                } catch (IllegalArgumentException e) {
                    LOG.warn("사번 {} 의 저장값이 레거시 형식이 아니라 승격을 건너뛴다: {}",
                            row.id(), e.getMessage());
                    skipped++;
                    continue;
                }
            }
            authMapper.updatePassword(row.id(), upgraded);
        }
        LOG.info("비밀번호 저장 형식 승격: 대상 {}건 — 사번과 같아 bcrypt {} · 겹씌움 {} · 건너뜀 {}",
                rows.size(), initial, wrapped, skipped);
    }
}
