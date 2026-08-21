package com.erflow.profile;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 프로필 화면이 쓰는 조회와 수정.
 *
 * <p>SQL 은 {@code resources/mapper/profile/ProfileMapper.xml} 에 있고, 각 구문에
 * 레거시 출처를 주석으로 달아 뒀다.
 */
@Mapper
public interface ProfileMapper {

    /**
     * 사번으로 프로필을 읽는다.
     *
     * @param id 사번
     * @return 사용자. 없으면 {@code null}
     */
    ProfileUser findProfile(@Param("id") String id);

    /**
     * 한 사람의 그 달 근무 기록.
     *
     * @param id 사번
     * @param date {@code yyyy-MM}
     * @return 근무 기록
     */
    List<ProfileWork> findWorks(@Param("id") String id, @Param("date") String date);

    /**
     * 프로필을 고친다.
     *
     * @param user 고칠 값. {@code id} 로 대상을 찾는다
     * @return 반영된 행 수
     */
    int updateProfile(@Param("user") ProfileUser user);
}
