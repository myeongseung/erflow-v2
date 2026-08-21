package com.erflow.admin.user;

import com.erflow.admin.AdminOption;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 사원 관리 매퍼.
 *
 * <p>SQL 은 {@code resources/mapper/admin/AdminUserMapper.xml} 에 있고, 각 구문에 레거시
 * 출처를 주석으로 달아 뒀다.
 *
 * <p>사용자 찾기 팝업({@code com.erflow.user.UserMapper})과 같은 {@code user_view} 를
 * 읽지만 보는 칸이 다르다. 팝업은 부서·직급·이름·사번만, 여기는 주민등록번호까지 본다.
 */
@Mapper
public interface AdminUserMapper {

    /**
     * 사원 리스트 한 페이지.
     *
     * @param search 검색 조건
     * @param start 조회 시작 위치
     * @param count 가져올 건수
     * @return 사원 목록. 부서·직급 이름순이며 {@code admin} 은 빠진다
     */
    List<AdminUserRow> findPage(
            @Param("search") AdminUserSearch search,
            @Param("start") int start,
            @Param("count") int count);

    /**
     * 조건에 걸리는 사원 수.
     *
     * @param search 검색 조건
     * @return 전체 건수
     */
    int countBy(@Param("search") AdminUserSearch search);

    /**
     * 한 사원의 주소.
     *
     * @param id 사번
     * @return 주소. 없으면 {@code null}
     */
    AdminUserAddress findAddress(@Param("id") String id);

    /**
     * 수정 화면이 채울 사원 한 명.
     *
     * @param id 사번
     * @return 사원. 없으면 {@code null}
     */
    AdminUserForm findForUpdate(@Param("id") String id);

    /**
     * 직급 콤보에 채울 항목.
     *
     * @return 직급. DB 가 주는 순서 그대로다
     */
    List<AdminOption> findJobs();

    /**
     * 부서 콤보에 채울 항목.
     *
     * @return 부서. DB 가 주는 순서 그대로다
     */
    List<AdminOption> findDepartments();

    /**
     * 사원을 넣는다.
     *
     * @param user 화면이 보낸 값
     * @param password 해시된 비밀번호. 평문은 사번이다
     * @return 반영된 행 수
     */
    int insertUser(@Param("user") AdminUserEdit user, @Param("password") String password);

    /**
     * 사원을 고친다.
     *
     * @param user 화면이 보낸 값
     * @return 반영된 행 수
     */
    int updateUser(@Param("user") AdminUserEdit user);
}
