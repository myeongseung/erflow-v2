package com.erflow.admin.permission;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 권한 관리 매퍼.
 *
 * <p>SQL 은 {@code resources/mapper/admin/AdminPermissionMapper.xml} 에 있다.
 * <b>비트 연산은 하나도 없다</b> — 값만 읽고 쓰며 계산은 {@link Levels} 에서 한다.
 */
@Mapper
public interface AdminPermissionMapper {

    /**
     * 부서와 그 권한.
     *
     * @param keyword 부서명 검색어. 비면 전체
     * @return 부서 목록. 관리자 행은 빠진다
     */
    List<PermissionRow> findDeptPermissions(@Param("keyword") String keyword);

    /**
     * 직급과 그 권한.
     *
     * @param keyword 직급명 검색어. 비면 전체
     * @return 직급 목록. 관리자 행은 빠진다
     */
    List<PermissionRow> findJobPermissions(@Param("keyword") String keyword);

    /**
     * 이미 쓰이는 부서 비트.
     *
     * @return 모든 부서의 자기 비트. 관리자 비트도 들어 있다
     */
    List<Long> findDeptLevels();

    /**
     * 이미 쓰이는 직급 비트.
     *
     * @return 모든 직급의 자기 비트. 관리자 비트도 들어 있다
     */
    List<Long> findJobLevels();

    /**
     * 부서 한 행.
     *
     * @param id 부서 번호
     * @return 부서. 없으면 {@code null}
     */
    DeptRow findDept(@Param("id") int id);

    /**
     * 직급 이름.
     *
     * @param id 직급 번호
     * @return 이름. 없으면 {@code null}
     */
    String findJobName(@Param("id") int id);

    /**
     * 같은 이름의 부서 수.
     *
     * @param name 부서명
     * @return 건수
     */
    int countDeptByName(@Param("name") String name);

    /**
     * 같은 이름의 직급 수.
     *
     * @param name 직급명
     * @return 건수
     */
    int countJobByName(@Param("name") String name);

    /**
     * 부서를 넣는다.
     *
     * @param dept 새 부서. {@code id} 는 보지 않는다
     * @return 반영된 행 수
     */
    int insertDept(@Param("dept") DeptRow dept);

    /**
     * 직급을 넣는다.
     *
     * @param name 직급명
     * @return 반영된 행 수
     */
    int insertJob(@Param("name") String name);

    /**
     * 방금 넣은 행의 자동 생성 번호. 같은 트랜잭션에서 부른다.
     *
     * @return {@code LAST_INSERT_ID()}
     */
    int lastInsertId();

    /**
     * 새 부서에 비트를 하나 준다.
     *
     * @param deptId 부서 번호
     * @param level 줄 비트
     * @return 반영된 행 수
     */
    int insertDeptPermission(@Param("deptId") int deptId, @Param("level") long level);

    /**
     * 새 직급에 비트를 하나 준다.
     *
     * @param jobId 직급 번호
     * @param level 줄 비트
     * @return 반영된 행 수
     */
    int insertJobPermission(@Param("jobId") int jobId, @Param("level") long level);

    /**
     * 부서 정보를 고친다.
     *
     * @param dept 고칠 값
     * @return 반영된 행 수
     */
    int updateDept(@Param("dept") DeptRow dept);

    /**
     * 직급 이름을 고친다.
     *
     * @param id 직급 번호
     * @param name 새 이름
     * @return 반영된 행 수
     */
    int updateJob(@Param("id") int id, @Param("name") String name);

    /**
     * 부서가 가진 비트들을 바꾼다.
     *
     * @param deptId 부서 번호
     * @param permission 새 비트마스크
     * @return 반영된 행 수
     */
    int updateDeptPermission(@Param("deptId") int deptId, @Param("permission") long permission);

    /**
     * 직급이 가진 비트들을 바꾼다.
     *
     * @param jobId 직급 번호
     * @param permission 새 비트마스크
     * @return 반영된 행 수
     */
    int updateJobPermission(@Param("jobId") int jobId, @Param("permission") long permission);

    /**
     * 부서의 권한 행을 지운다.
     *
     * @param deptId 부서 번호
     * @return 반영된 행 수
     */
    int deleteDeptPermission(@Param("deptId") int deptId);

    /**
     * 부서를 지운다.
     *
     * @param deptId 부서 번호
     * @return 반영된 행 수
     */
    int deleteDept(@Param("deptId") int deptId);

    /**
     * 직급의 권한 행을 지운다.
     *
     * @param jobId 직급 번호
     * @return 반영된 행 수
     */
    int deleteJobPermission(@Param("jobId") int jobId);

    /**
     * 직급을 지운다.
     *
     * @param jobId 직급 번호
     * @return 반영된 행 수
     */
    int deleteJob(@Param("jobId") int jobId);

    /**
     * 프로그램 목록 한 페이지.
     *
     * @param keyword 프로그램 이름 검색어. 부분 일치
     * @param start 조회 시작 위치
     * @param count 가져올 건수
     * @return 프로그램 목록
     */
    List<ProgramRow> findProgramPage(
            @Param("keyword") String keyword,
            @Param("start") int start,
            @Param("count") int count);

    /**
     * 조건에 걸리는 프로그램 수.
     *
     * <p>목록과 달리 <b>완전 일치</b>로 센다. 레거시가 그렇다(D-064).
     *
     * @param keyword 프로그램 이름
     * @return 건수
     */
    int countPrograms(@Param("keyword") String keyword);

    /**
     * 프로그램 한 행.
     *
     * @param id 행 번호
     * @return 프로그램. 없으면 {@code null}
     */
    ProgramRow findProgram(@Param("id") int id);

    /**
     * 프로그램 권한 행 전부. 지운 부서·직급의 비트를 걷어낼 때 쓴다(D-109).
     *
     * @return 프로그램 목록
     */
    List<ProgramRow> findPrograms();

    /**
     * 프로그램의 부서 권한을 바꾼다.
     *
     * @param programId 프로그램 코드
     * @param level 새 비트마스크
     * @return 반영된 행 수
     */
    int updateProgramDeptLevel(
            @Param("programId") String programId, @Param("level") long level);

    /**
     * 프로그램의 직급 권한을 바꾼다.
     *
     * @param programId 프로그램 코드
     * @param level 새 비트마스크
     * @return 반영된 행 수
     */
    int updateProgramJobLevel(
            @Param("programId") String programId, @Param("level") long level);
}
