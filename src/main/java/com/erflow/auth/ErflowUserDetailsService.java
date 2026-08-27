package com.erflow.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사번으로 사용자를 읽어 인증 주체를 만든다.
 *
 * <p>{@link UserDetailsPasswordService} 도 겸한다 — 저장 형식이 기본(bcrypt)이 아닌
 * 사용자가 로그인에 성공하면 Spring Security 가 {@link #updatePassword} 를 불러 그
 * 자리에서 bcrypt 로 다시 저장한다. 겹씌움 형식({@code {erflow-bcrypt}})이 이 길로
 * 점진 소멸한다(D-128, O-006 의 «로그인 시 재해시»).
 */
@Service
public class ErflowUserDetailsService implements UserDetailsService, UserDetailsPasswordService {

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * @param authMapper 인증 조회 매퍼
     * @param passwordEncoder 비밀번호 인코더
     */
    public ErflowUserDetailsService(AuthMapper authMapper, PasswordEncoder passwordEncoder) {
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * {@inheritDoc}
     *
     * <p>비밀번호가 사번과 같으면 최초 로그인으로 본다. 레거시
     * {@code UserController.isInitialLogin} 이 같은 판정을 했다.
     */
    @Override
    @Transactional(readOnly = true)
    public ErflowUserDetails loadUserByUsername(String username) {
        AuthUser user = authMapper.findAuthUser(username);
        if (user == null || user.password() == null) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없다");
        }
        boolean initial = passwordEncoder.matches(user.id(), user.password());
        return new ErflowUserDetails(user, initial);
    }

    /**
     * 로그인 성공 직후 저장 형식을 승격한다.
     *
     * <p>Spring Security 가 인증 성공 후 {@code PasswordEncoder.upgradeEncoding} 이 참일
     * 때만 부른다. {@code newPassword} 는 방금 입력된 평문을 기본 형식(bcrypt)으로 새로
     * 인코딩한 값이다.
     *
     * @param user 방금 인증된 사용자
     * @param newPassword 새 형식으로 인코딩된 비밀번호
     * @return 새 저장값을 반영한 인증 주체
     */
    @Override
    @Transactional
    public ErflowUserDetails updatePassword(UserDetails user, String newPassword) {
        ErflowUserDetails details = (ErflowUserDetails) user;
        authMapper.updatePassword(details.id(), newPassword);
        AuthUser updated = new AuthUser(details.id(), details.name(), newPassword,
                details.deptPermission(), details.jobPermission());
        return new ErflowUserDetails(updated, details.passwordChangeRequired());
    }
}
