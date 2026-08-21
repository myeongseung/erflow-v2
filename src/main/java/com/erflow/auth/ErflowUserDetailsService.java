package com.erflow.auth;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사번으로 사용자를 읽어 인증 주체를 만든다.
 */
@Service
public class ErflowUserDetailsService implements UserDetailsService {

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
}
