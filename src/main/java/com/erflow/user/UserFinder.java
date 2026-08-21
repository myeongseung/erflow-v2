package com.erflow.user;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 찾기 팝업이 쓰는 조회.
 *
 * <p>사용자 도메인 자체는 아직 이관 전이다. 팝업이 부르는 세 조회만 만든다 —
 * 문서 찾기 팝업에서와 같은 판단이다.
 *
 * <p>거를 일이 없어 매퍼를 그대로 부른다. 레거시도 조건을 SQL 에 맡겼다.
 */
@Service
public class UserFinder {

    private final UserMapper userMapper;

    /**
     * @param userMapper 사용자 매퍼
     */
    public UserFinder(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 부서 콤보 항목.
     *
     * @return 부서명 목록
     */
    @Transactional(readOnly = true)
    public List<String> departments() {
        return userMapper.findDepartmentNames();
    }

    /**
     * 직급 콤보 항목.
     *
     * @return 직급명 목록
     */
    @Transactional(readOnly = true)
    public List<String> jobs() {
        return userMapper.findJobNames();
    }

    /**
     * 조건에 걸리는 사용자.
     *
     * <p>조건이 전부 비어도 목록은 나온다. 팝업을 처음 열었을 때가 그 경우다 —
     * 은행·업종 팝업이 검색하기 전에 도움말을 보여주는 것과 다르다. 이 화면에는
     * 도움말이 아예 없다.
     *
     * @param search 검색 조건
     * @return 사용자 목록
     */
    @Transactional(readOnly = true)
    public List<UserRow> search(UserSearch search) {
        return userMapper.findUserViews(search);
    }

    /**
     * 한 사용자의 이름.
     *
     * @param id 사번
     * @return 이름. 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public String name(String id) {
        return userMapper.findUserName(id);
    }
}
