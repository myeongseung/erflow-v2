package com.erflow.admin.user;

import com.erflow.admin.AdminOption;
import com.erflow.common.Pagination;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사원 관리 업무.
 *
 * <p>레거시는 페이징 계산을 {@code admin/user/userList.jsp} 스크립틀릿에, DB 접근을
 * {@code UserServiceImpl} 에 두었다. 화면에서 로직을 걷어내 여기로 모은다.
 */
@Service
public class AdminUserService {

    /** 임시 비밀번호에 쓰는 글자 — 눈으로 헷갈리는 0·O·1·l·I 는 뺐다. */
    private static final String TEMP_PASSWORD_ALPHABET =
            "23456789ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz";

    /** 임시 비밀번호 글자 수 (붙임표 제외). */
    private static final int TEMP_PASSWORD_LENGTH = 8;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AdminUserMapper adminUserMapper;

    private final PasswordEncoder passwordEncoder;

    /**
     * @param adminUserMapper 사원 관리 매퍼
     * @param passwordEncoder 비밀번호 인코더. 기본 형식(bcrypt)으로 저장한다(D-128)
     */
    public AdminUserService(AdminUserMapper adminUserMapper, PasswordEncoder passwordEncoder) {
        this.adminUserMapper = adminUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 사원 리스트 한 페이지.
     *
     * @param search 검색 조건
     * @param requestedPage 요청된 페이지
     * @return 목록과 페이징
     */
    @Transactional(readOnly = true)
    public UserPage list(AdminUserSearch search, int requestedPage) {
        int total = adminUserMapper.countBy(search);
        Pagination pagination = Pagination.of(total, requestedPage);
        List<AdminUserRow> rows = adminUserMapper.findPage(
                search, pagination.start(), pagination.numPerPage());
        return new UserPage(rows, pagination);
    }

    /**
     * 한 사원의 주소.
     *
     * @param id 사번
     * @return 주소. 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public AdminUserAddress address(String id) {
        return adminUserMapper.findAddress(id);
    }

    /**
     * 사원 추가 화면의 콤보.
     *
     * <p>직급·부서를 <b>DB 가 주는 순서</b> 그대로 그린다. 레거시가 정렬하지 않는다.
     *
     * @return 직급·부서 목록
     */
    @Transactional(readOnly = true)
    public Options registerForm() {
        return new Options(adminUserMapper.findJobs(), adminUserMapper.findDepartments());
    }

    /**
     * 사원 수정 화면 한 벌.
     *
     * <p>같은 콤보를 <b>이름순으로</b> 그린다. 추가 화면과 순서가 다르다 — 레거시가
     * 이 화면에서만 {@code Collections.sort} 를 한다(D-058).
     *
     * @param id 사번
     * @return 화면 한 벌. 사원이 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public UserForm updateForm(String id) {
        AdminUserForm user = adminUserMapper.findForUpdate(id);
        if (user == null) {
            return null;
        }
        return new UserForm(user, new Options(
                byName(adminUserMapper.findJobs()), byName(adminUserMapper.findDepartments())));
    }

    /**
     * 사원을 등록한다.
     *
     * <p>비밀번호는 사번을 해시한 값이다. 그래야 새 사원이 첫 로그인에서 비밀번호 변경
     * 화면으로 간다 — 레거시가 «비밀번호 == 사번» 으로 최초 로그인을 판정한다.
     *
     * <p>레거시는 여기서 나는 예외를 <b>통째로</b> 삼키고 «등록에 실패했습니다» 를
     * 띄웠다. 이미 있는 사번, 이미 쓰는 이메일(고유 키), 길이를 넘긴 내선 번호
     * (컬럼이 세 글자다) 가 전부 그 길로 간다. 여기서는 «데이터 제약 위반» 만 삼킨다 —
     * 연결 실패 같은 것까지 삼키면 실패를 조용히 숨기게 된다.
     *
     * @param user 화면이 보낸 값
     * @return 넣었으면 {@code true}. 제약에 걸리면 {@code false}
     */
    @Transactional
    public boolean register(AdminUserEdit user) {
        try {
            return adminUserMapper.insertUser(user, passwordEncoder.encode(user.id())) == 1;
        } catch (DataIntegrityViolationException expected) {
            return false;
        }
    }

    /**
     * 사원을 고친다.
     *
     * @param user 화면이 보낸 값
     * @return 고쳤으면 {@code true}
     */
    @Transactional
    public boolean update(AdminUserEdit user) {
        try {
            return adminUserMapper.updateUser(user) == 1;
        } catch (DataIntegrityViolationException expected) {
            return false;
        }
    }

    /**
     * 비밀번호를 임시 값으로 초기화한다 (D-134).
     *
     * <p>임시 비밀번호 평문은 이 반환값에만 있다 — DB 에는 해시만 남고, 다시 조회할
     * 길이 없다. 화면이 한 번 보여 주고 끝이다. 관리자가 본인에게 직접 전달한다.
     *
     * <p>초기화된 계정은 «변경 필요» 상태가 되어, 임시 비밀번호로 로그인하면 곧장
     * 비밀번호 변경 화면으로 간다.
     *
     * @param id 사번
     * @return 임시 비밀번호와 대상 사원. 사원이 없거나 {@code admin} 이면 {@code null}
     */
    @Transactional
    public TempPassword resetPassword(String id) {
        AdminUserForm user = adminUserMapper.findForUpdate(id);
        if (user == null) {
            return null;
        }
        String plain = newTempPassword();
        if (adminUserMapper.resetPassword(id, passwordEncoder.encode(plain)) != 1) {
            return null;
        }
        return new TempPassword(user.id(), user.name(), plain);
    }

    /**
     * 임시 비밀번호를 만든다.
     *
     * <p>여덟 글자를 넷씩 나눠 사이에 붙임표를 둔다 — 전화나 종이로 전달할 값이라
     * 읽어 주기 쉬워야 한다. 같은 이유로 헷갈리는 글자(0·O·1·l·I)는 뺐다.
     */
    private String newTempPassword() {
        StringBuilder plain = new StringBuilder(TEMP_PASSWORD_LENGTH + 1);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            if (i == TEMP_PASSWORD_LENGTH / 2) {
                plain.append('-');
            }
            plain.append(TEMP_PASSWORD_ALPHABET.charAt(
                    RANDOM.nextInt(TEMP_PASSWORD_ALPHABET.length())));
        }
        return plain.toString();
    }

    private static List<AdminOption> byName(List<AdminOption> options) {
        // 레거시 Collections.sort + Comparator 가 String.compareTo 로 견준다.
        // DB 에 ORDER BY 를 붙이지 않는 이유가 여기 있다 — 정렬 기준이 DB collation 이
        // 아니라 자바 문자열 비교다. 지금 데이터(한글 이름)에서는 결과가 같지만
        // 영문·숫자가 섞이면 갈린다.
        return options.stream().sorted(Comparator.comparing(AdminOption::name)).toList();
    }

    /**
     * 직급·부서 콤보.
     *
     * @param jobs 직급
     * @param departments 부서
     */
    public record Options(List<AdminOption> jobs, List<AdminOption> departments) {
    }

    /**
     * 사원 수정 화면 한 벌.
     *
     * @param user 채워 넣을 값
     * @param options 콤보 항목
     */
    public record UserForm(AdminUserForm user, Options options) {
    }

    /**
     * 사원 리스트 한 페이지.
     *
     * @param rows 이 페이지의 사원 목록
     * @param pagination 페이징 정보
     */
    public record UserPage(List<AdminUserRow> rows, Pagination pagination) {
    }

    /**
     * 초기화 결과 — 임시 비밀번호 평문이 사는 유일한 곳 (D-134).
     *
     * @param id 사번
     * @param name 이름
     * @param password 임시 비밀번호 평문
     */
    public record TempPassword(String id, String name, String password) {
    }
}
