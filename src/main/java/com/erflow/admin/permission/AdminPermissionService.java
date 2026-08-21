package com.erflow.admin.permission;

import com.erflow.auth.Permissions;
import com.erflow.common.Pagination;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 권한 관리 업무 — 직급·부서.
 *
 * <p>레거시는 이 계산을 {@code *Proc.jsp} 스크립틀릿에서 했다. 체크된 번호를 받아 그
 * 번호의 비트를 찾아 {@code |} 로 합치는 일이다. 화면에서 걷어내 여기로 모은다.
 *
 * <p><b>비트 계산은 전부 여기(Java)에서 한다.</b> 매퍼에는 계산이 끝난 값 하나만 넘긴다 —
 * MariaDB 의 비트 연산은 부호가 없어 관리자 비트가 걸리면 뒤집힌다.
 */
@Service
public class AdminPermissionService {

    private final AdminPermissionMapper mapper;

    /**
     * @param mapper 권한 관리 매퍼
     */
    public AdminPermissionService(AdminPermissionMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 직급·부서 목록 화면 한 벌.
     *
     * @param jobKeyword 직급명 검색어
     * @param deptKeyword 부서명 검색어
     * @return 직급 목록과 부서 목록
     */
    @Transactional(readOnly = true)
    public JobDeptList list(String jobKeyword, String deptKeyword) {
        return new JobDeptList(
                mapper.findJobPermissions(jobKeyword), mapper.findDeptPermissions(deptKeyword));
    }

    /**
     * 부서 수정 화면 한 벌.
     *
     * <p>체크박스는 <b>자기 자신을 뺀</b> 나머지 부서다. 이미 가진 비트에 체크가 켜진다.
     *
     * @param deptId 부서 번호
     * @return 화면 한 벌. 부서가 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public DeptForm deptForm(int deptId) {
        DeptRow dept = mapper.findDept(deptId);
        if (dept == null) {
            return null;
        }
        return new DeptForm(dept, choices(mapper.findDeptPermissions(null), deptId));
    }

    /**
     * 직급 수정 화면 한 벌.
     *
     * @param jobId 직급 번호
     * @return 화면 한 벌. 직급이 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public JobForm jobForm(int jobId) {
        String name = mapper.findJobName(jobId);
        if (name == null) {
            return null;
        }
        return new JobForm(jobId, name, choices(mapper.findJobPermissions(null), jobId));
    }

    /**
     * 부서를 만든다.
     *
     * <p>부서를 넣고 <b>빈 비트 하나를 찾아</b> 권한 행을 만든다. 레거시는 이 둘을 따로
     * 커밋해서, 사이에서 끊기면 권한 행 없는 부서가 남았다. 한 트랜잭션으로 묶는다(D-061).
     *
     * @param name 부서명
     * @param postalCode 우편번호
     * @param address1 도로명 주소
     * @param address2 상세 주소
     * @return 만들었으면 {@code true}. 이름이 비었거나 이미 있으면 {@code false}
     */
    @Transactional
    public boolean createDept(String name, String postalCode, String address1, String address2) {
        if (name == null || name.trim().isEmpty() || hasDept(name)) {
            return false;
        }
        mapper.insertDept(new DeptRow(0, name, null, postalCode, address1, address2));
        int deptId = mapper.lastInsertId();
        return mapper.insertDeptPermission(deptId, Levels.next(mapper.findDeptLevels())) == 1;
    }

    /**
     * 직급을 만든다.
     *
     * @param name 직급명
     * @return 만들었으면 {@code true}. 이미 있으면 {@code false}
     */
    @Transactional
    public boolean createJob(String name) {
        if (name == null || hasJob(name)) {
            return false;
        }
        mapper.insertJob(name);
        int jobId = mapper.lastInsertId();
        return mapper.insertJobPermission(jobId, Levels.next(mapper.findJobLevels())) == 1;
    }

    /**
     * 부서 이름·주소와 권한을 고친다.
     *
     * <p>권한은 <b>자기 비트 + 체크된 부서들의 비트</b> 다. 자기 비트는 화면에 체크박스가
     * 없지만 언제나 켜진다.
     *
     * <p>이름이 이미 있으면 아무것도 하지 않는다 — 자기 이름을 그대로 둔 경우는 예외다.
     *
     * @param deptId 부서 번호
     * @param name 부서명
     * @param postalCode 우편번호
     * @param address1 도로명 주소
     * @param address2 상세 주소
     * @param checked 체크된 부서 번호들
     * @return 고쳤으면 {@code true}
     */
    @Transactional
    public boolean updateDept(
            int deptId, String name, String postalCode, String address1, String address2,
            List<Integer> checked) {

        DeptRow previous = mapper.findDept(deptId);
        if (previous == null || (hasDept(name) && !previous.name().equals(name))) {
            return false;
        }
        List<PermissionRow> all = mapper.findDeptPermissions(null);
        // 부서장은 화면에 없다. 읽은 값을 그대로 되돌려 놓지 않으면 지워진다.
        boolean updated = mapper.updateDept(new DeptRow(
                deptId, name, previous.managerId(), postalCode, address1, address2)) == 1;
        return updated
                && mapper.updateDeptPermission(deptId, permissionOf(all, deptId, checked)) == 1;
    }

    /**
     * 직급 이름과 권한을 고친다.
     *
     * @param jobId 직급 번호
     * @param name 직급명
     * @param checked 체크된 직급 번호들
     * @return 고쳤으면 {@code true}
     */
    @Transactional
    public boolean updateJob(int jobId, String name, List<Integer> checked) {
        String previous = mapper.findJobName(jobId);
        if (previous == null || (hasJob(name) && !previous.equals(name))) {
            return false;
        }
        List<PermissionRow> all = mapper.findJobPermissions(null);
        boolean updated = mapper.updateJob(jobId, name) == 1;
        return updated
                && mapper.updateJobPermission(jobId, permissionOf(all, jobId, checked)) == 1;
    }

    /**
     * 부서들을 지운다.
     *
     * <p>권한 행을 먼저 지우고 부서를 지운 뒤, <b>다른 부서의 {@code permission} 과
     * 프로그램의 {@code dept_level} 에 남은 그 부서의 비트를 걷어낸다.</b> 레거시는
     * 걷어내지 않아, 빈 자리를 찾는 규칙이 그 비트를 새 부서에 다시 주면 지운 부서의
     * 권한을 그대로 물려받았다(D-062). 2단계에서 걷어낸다(D-109) — 계산은 여기(Java)서
     * 한다. 레거시의 {@code revokePermission} 은 DB 비트 연산이라 쓰지 않는다.
     *
     * @param deptIds 지울 부서 번호들
     * @return 전부 지웠으면 {@code true}
     */
    @Transactional
    public boolean deleteDepts(List<Integer> deptIds) {
        boolean result = deptIds != null && !deptIds.isEmpty();
        for (int deptId : deptIds == null ? List.<Integer>of() : deptIds) {
            // 비트는 권한 행에 있으니 지우기 전에 읽어 둔다.
            long bit = levelOf(mapper.findDeptPermissions(null), deptId);
            mapper.deleteDeptPermission(deptId);
            result &= mapper.deleteDept(deptId) == 1;
            if (bit != 0L) {
                for (PermissionRow row : mapper.findDeptPermissions(null)) {
                    if ((row.permission() & bit) != 0L) {
                        mapper.updateDeptPermission(row.classId(), row.permission() & ~bit);
                    }
                }
                for (ProgramRow program : mapper.findPrograms()) {
                    if ((program.deptLevel() & bit) != 0L) {
                        mapper.updateProgramDeptLevel(
                                program.programId(), program.deptLevel() & ~bit);
                    }
                }
            }
        }
        return result;
    }

    /**
     * 직급들을 지운다.
     *
     * <p>부서와 같은 규칙으로(D-109) 다른 직급의 {@code permission} 과 프로그램의
     * {@code job_level} 에 남은 비트를 걷어낸다 — 결함의 모양이 D-062 와 같다.
     *
     * @param jobIds 지울 직급 번호들
     * @return 전부 지웠으면 {@code true}
     */
    @Transactional
    public boolean deleteJobs(List<Integer> jobIds) {
        boolean result = jobIds != null && !jobIds.isEmpty();
        for (int jobId : jobIds == null ? List.<Integer>of() : jobIds) {
            long bit = levelOf(mapper.findJobPermissions(null), jobId);
            mapper.deleteJobPermission(jobId);
            result &= mapper.deleteJob(jobId) == 1;
            if (bit != 0L) {
                for (PermissionRow row : mapper.findJobPermissions(null)) {
                    if ((row.permission() & bit) != 0L) {
                        mapper.updateJobPermission(row.classId(), row.permission() & ~bit);
                    }
                }
                for (ProgramRow program : mapper.findPrograms()) {
                    if ((program.jobLevel() & bit) != 0L) {
                        mapper.updateProgramJobLevel(
                                program.programId(), program.jobLevel() & ~bit);
                    }
                }
            }
        }
        return result;
    }

    /**
     * 프로그램 목록 한 페이지.
     *
     * <p>세는 쪽과 가져오는 쪽의 조건이 다르다 — 목록은 부분 일치, 건수는 완전 일치다.
     * 그래서 검색하면 줄은 나오는데 페이지가 그려지지 않는다. 레거시 그대로다(D-064).
     *
     * @param keyword 프로그램 이름 검색어
     * @param requestedPage 요청된 페이지
     * @return 목록과 페이징
     */
    @Transactional(readOnly = true)
    public ProgramPage programs(String keyword, int requestedPage) {
        Pagination pagination = Pagination.of(mapper.countPrograms(keyword), requestedPage);
        return new ProgramPage(
                mapper.findProgramPage(keyword, pagination.start(), pagination.numPerPage()),
                pagination);
    }

    /**
     * 프로그램 부서 권한 수정 화면 한 벌.
     *
     * <p>부서 수정 화면과 달리 <b>모든 부서</b>가 체크박스로 나온다. 프로그램은 부서가
     * 아니라 «빼야 할 자기 자신» 이 없기 때문이다.
     *
     * @param id 프로그램 행 번호
     * @return 화면 한 벌. 프로그램이 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public ProgramForm programDeptForm(int id) {
        ProgramRow program = mapper.findProgram(id);
        if (program == null) {
            return null;
        }
        return new ProgramForm(program,
                allChoices(mapper.findDeptPermissions(null), program.deptLevel()));
    }

    /**
     * 프로그램 직급 권한 수정 화면 한 벌.
     *
     * @param id 프로그램 행 번호
     * @return 화면 한 벌. 프로그램이 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public ProgramForm programJobForm(int id) {
        ProgramRow program = mapper.findProgram(id);
        if (program == null) {
            return null;
        }
        return new ProgramForm(program,
                allChoices(mapper.findJobPermissions(null), program.jobLevel()));
    }

    /**
     * 프로그램의 부서 권한을 바꾼다.
     *
     * <p>시작값이 <b>관리자 비트</b>다. 체크를 다 지워도 관리자는 남는다 — 관리자 부서는
     * 체크박스로 나오지도 않으므로 이 화면에서 관리자를 뺄 방법이 아예 없다.
     *
     * @param id 프로그램 행 번호
     * @param checked 체크된 부서 번호들
     * @return 바꿨으면 {@code true}
     */
    @Transactional
    public boolean updateProgramDeptLevel(int id, List<Integer> checked) {
        ProgramRow program = mapper.findProgram(id);
        if (program == null) {
            return false;
        }
        long level = levelsOf(mapper.findDeptPermissions(null), checked);
        return mapper.updateProgramDeptLevel(program.programId(), level) == 1;
    }

    /**
     * 프로그램의 직급 권한을 바꾼다.
     *
     * @param id 프로그램 행 번호
     * @param checked 체크된 직급 번호들
     * @return 바꿨으면 {@code true}
     */
    @Transactional
    public boolean updateProgramJobLevel(int id, List<Integer> checked) {
        ProgramRow program = mapper.findProgram(id);
        if (program == null) {
            return false;
        }
        long level = levelsOf(mapper.findJobPermissions(null), checked);
        return mapper.updateProgramJobLevel(program.programId(), level) == 1;
    }

    /** 그 번호의 자기 비트 하나. 없으면 0. 지울 때 걷어낼 비트를 찾는다(D-109). */
    private static long levelOf(List<PermissionRow> all, int classId) {
        for (PermissionRow row : all) {
            if (row.classId() == classId) {
                return row.level();
            }
        }
        return 0L;
    }

    /** 체크된 번호들의 비트를 관리자 비트 위에 얹는다. */
    private static long levelsOf(List<PermissionRow> all, List<Integer> checked) {
        List<Long> levels = new ArrayList<>();
        for (PermissionRow row : all) {
            if (checked != null && checked.contains(row.classId())) {
                levels.add(row.level());
            }
        }
        return Levels.combine(Permissions.ADMIN_BIT, levels);
    }

    /** 프로그램 화면은 자기 자신을 뺄 것이 없다. 전부 내놓는다. */
    private static List<PermissionChoice> allChoices(List<PermissionRow> all, long level) {
        List<PermissionChoice> choices = new ArrayList<>();
        for (PermissionRow row : all) {
            choices.add(new PermissionChoice(
                    row.classId(), row.name(), Levels.has(level, row.level())));
        }
        return choices;
    }

    /**
     * 체크된 번호들을 비트마스크로 만든다.
     *
     * <p>시작값은 <b>자기 비트</b>다. 그래서 체크를 다 지워도 자기 자신은 남는다.
     */
    private static long permissionOf(List<PermissionRow> all, int selfId, List<Integer> checked) {
        List<Long> levels = new ArrayList<>();
        long self = 0L;
        for (PermissionRow row : all) {
            if (row.classId() == selfId) {
                self = row.level();
            } else if (checked != null && checked.contains(row.classId())) {
                levels.add(row.level());
            }
        }
        return Levels.combine(self, levels);
    }

    /** 자기 자신을 뺀 나머지를 체크박스로 만든다. 이미 가진 비트는 켜 둔다. */
    private static List<PermissionChoice> choices(List<PermissionRow> all, int selfId) {
        long permission = 0L;
        for (PermissionRow row : all) {
            if (row.classId() == selfId) {
                permission = row.permission();
                break;
            }
        }
        List<PermissionChoice> choices = new ArrayList<>();
        for (PermissionRow row : all) {
            if (row.classId() != selfId) {
                choices.add(new PermissionChoice(
                        row.classId(), row.name(), Levels.has(permission, row.level())));
            }
        }
        return choices;
    }

    /**
     * 같은 이름이 있는가.
     *
     * <p>레거시는 {@code count == 1} 로 본다. 같은 이름이 둘이면 «없다» 가 되어 셋째를
     * 만들 수 있다. 그대로 옮긴다.
     */
    private boolean hasDept(String name) {
        return mapper.countDeptByName(name) == 1;
    }

    private boolean hasJob(String name) {
        return mapper.countJobByName(name) == 1;
    }

    /**
     * 직급·부서 목록 화면 한 벌.
     *
     * @param jobs 직급 목록
     * @param depts 부서 목록
     */
    public record JobDeptList(List<PermissionRow> jobs, List<PermissionRow> depts) {
    }

    /**
     * 부서 수정 화면 한 벌.
     *
     * @param dept 고칠 부서
     * @param choices 다른 부서 체크박스
     */
    public record DeptForm(DeptRow dept, List<PermissionChoice> choices) {
    }

    /**
     * 직급 수정 화면 한 벌.
     *
     * @param id 직급 번호
     * @param name 직급명
     * @param choices 다른 직급 체크박스
     */
    public record JobForm(int id, String name, List<PermissionChoice> choices) {
    }

    /**
     * 프로그램 목록 한 페이지.
     *
     * @param rows 이 페이지의 프로그램
     * @param pagination 페이징 정보
     */
    public record ProgramPage(List<ProgramRow> rows, Pagination pagination) {
    }

    /**
     * 프로그램 권한 수정 화면 한 벌.
     *
     * @param program 고칠 프로그램
     * @param choices 부서 또는 직급 체크박스
     */
    public record ProgramForm(ProgramRow program, List<PermissionChoice> choices) {
    }
}
