package com.erflow.profile;

import com.erflow.auth.AuthMapper;
import com.erflow.auth.AuthUser;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로필 업무.
 *
 * <p>레거시 {@code UserController} 와 {@code ActivityController.getWorkViews(id, date)} 가
 * 하던 일이다.
 */
@Service
public class ProfileService {

    private final ProfileMapper profileMapper;
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    /**
     * @param profileMapper 프로필 조회·수정
     * @param authMapper 비밀번호 확인에 쓰는 조회
     * @param passwordEncoder 비밀번호 인코더
     * @param clock 오늘을 정하는 시계
     */
    public ProfileService(ProfileMapper profileMapper, AuthMapper authMapper,
            PasswordEncoder passwordEncoder, Clock clock) {
        this.profileMapper = profileMapper;
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * 프로필을 읽는다.
     *
     * @param id 사번
     * @return 사용자. 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public ProfileUser get(String id) {
        return id == null ? null : profileMapper.findProfile(id);
    }

    /**
     * 근무 현황 표를 만든다.
     *
     * <p><b>남의 프로필에서는 빈 표가 나온다.</b> 레거시가 자기 프로필일 때만 기록을
     * 조회했다 — 근무 기록은 자기 것만 본다는 뜻이다.
     *
     * @param viewerId 화면을 보는 사람의 사번
     * @param ownerId 프로필 주인의 사번
     * @param month 그릴 달
     * @return 두 줄짜리 표
     */
    @Transactional(readOnly = true)
    public WorkCalendar calendar(String viewerId, String ownerId, YearMonth month) {
        List<ProfileWork> works = viewerId.equals(ownerId)
                ? profileMapper.findWorks(ownerId, month.toString())
                : List.of();
        return WorkCalendar.of(month, LocalDate.now(clock), works);
    }

    /**
     * 화면이 처음 여는 달과, 달 고르기의 하한.
     *
     * <p>하한은 <b>올해 1월</b>로 고정이다. 다른 달을 보고 있어도 하한은 움직이지
     * 않는다 — 레거시가 파라미터를 읽기 전에 계산해 둔다.
     *
     * @return 올해 1월
     */
    public YearMonth minMonth() {
        return YearMonth.now(clock).withMonth(1);
    }

    /**
     * @return 이번 달
     */
    public YearMonth thisMonth() {
        return YearMonth.now(clock);
    }

    /**
     * 비밀번호가 맞는지 확인한다.
     *
     * <p>레거시는 로그인과 <b>같은 함수</b>를 불렀다. 저장된 해시를 다시 읽어 맞춰
     * 본다 — 세션에 해시를 들고 있지 않기 위해서다.
     *
     * @param id 사번
     * @param password 입력한 비밀번호
     * @return 맞으면 {@code true}
     */
    @Transactional(readOnly = true)
    public boolean passwordMatches(String id, String password) {
        if (password == null) {
            return false;
        }
        AuthUser user = authMapper.findAuthUser(id);
        return user != null && passwordEncoder.matches(password, user.password());
    }

    /**
     * 프로필을 고친다.
     *
     * @param user 고칠 값
     * @return 한 행이 바뀌었으면 {@code true}
     */
    @Transactional
    public boolean update(ProfileUser user) {
        return profileMapper.updateProfile(user) == 1;
    }
}
