package com.erflow.admin.permission;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 권한 관리 화면 — 직급·부서.
 *
 * <pre>
 * jobDeptList.jsp      GET  /admin/permission/job-dept-list
 * jobRegister.jsp      GET  /admin/permission/job-register        (팝업)
 * jobRegisterProc      POST /admin/permission/job-register-proc
 * deptRegister.jsp     GET  /admin/permission/dept-register       (팝업)
 * deptRegisterProc     POST /admin/permission/dept-register-proc
 * jobUpdate.jsp        GET  /admin/permission/job-update?jobId=
 * jobUpdateProc        POST /admin/permission/job-update-proc
 * deptUpdate.jsp       GET  /admin/permission/dept-update?deptId=
 * deptUpdateProc       POST /admin/permission/dept-update-proc
 * jobDeleteProc        POST /admin/permission/job-delete-proc
 * deptDeleteProc       POST /admin/permission/dept-delete-proc
 * </pre>
 *
 * <p>권한 검사는 여기서 하지 않는다. {@code /admin/**} 을 통째로 막는다(D-053).
 *
 * <p>등록은 목록에서 {@code window.open} 으로 여는 팝업이라, 처리 결과도 레거시 그대로
 * 부모를 새로 고친 뒤 창을 닫는다. 수정·삭제는 목록으로 돌아간다.
 */
@Controller
@RequestMapping("/admin/permission")
public class AdminPermissionController {

    private static final String LIST = "/admin/permission/job-dept-list";

    private static final String RESULT = "admin/permission/result";

    private final AdminPermissionService permissionService;

    /**
     * @param permissionService 권한 관리 업무
     */
    public AdminPermissionController(AdminPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * 직급·부서 리스트.
     *
     * @param jobKeyword 직급명 검색어
     * @param deptKeyword 부서명 검색어
     * @param model 뷰 모델
     * @return 목록 템플릿
     */
    @GetMapping("/job-dept-list")
    public String jobDeptList(
            @RequestParam(required = false) String jobKeyword,
            @RequestParam(required = false) String deptKeyword,
            Model model) {

        AdminPermissionService.JobDeptList page =
                permissionService.list(jobKeyword, deptKeyword);
        model.addAttribute("jobs", page.jobs());
        model.addAttribute("depts", page.depts());
        model.addAttribute("jobKeyword", jobKeyword == null ? "" : jobKeyword);
        model.addAttribute("deptKeyword", deptKeyword == null ? "" : deptKeyword);
        return "admin/permission/job-dept-list";
    }

    /**
     * 직급 생성 팝업.
     *
     * @return 등록 템플릿
     */
    @GetMapping("/job-register")
    public String jobRegisterForm() {
        return "admin/permission/job-register";
    }

    /**
     * 직급 생성 처리.
     *
     * @param jobName 직급명
     * @param model 뷰 모델
     * @return 팝업 결과 템플릿
     */
    @PostMapping("/job-register-proc")
    public String jobRegister(@RequestParam(required = false) String jobName, Model model) {
        if (jobName == null) {
            return popup(model, "잘못된 접근입니다.");
        }
        return popup(model, permissionService.createJob(jobName)
                ? "등록에 성공했습니다." : "등록에 실패하였습니다.");
    }

    /**
     * 부서 생성 팝업.
     *
     * @return 등록 템플릿
     */
    @GetMapping("/dept-register")
    public String deptRegisterForm() {
        return "admin/permission/dept-register";
    }

    /**
     * 부서 생성 처리.
     *
     * <p>네 값이 모두 요청에 있어야 한다. 비어 있어도 되고, 빈 값은 {@code null} 로
     * 저장된다.
     *
     * @param deptName 부서명
     * @param postalCode 우편번호
     * @param address1 도로명 주소
     * @param address2 상세 주소
     * @param model 뷰 모델
     * @return 팝업 결과 템플릿
     */
    @PostMapping("/dept-register-proc")
    public String deptRegister(
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) String address1,
            @RequestParam(required = false) String address2,
            Model model) {

        if (deptName == null || postalCode == null || address1 == null || address2 == null) {
            return popup(model, "올바르지 않은 접근입니다.");
        }
        boolean created = permissionService.createDept(
                blankToNull(deptName), blankToNull(postalCode),
                blankToNull(address1), blankToNull(address2));
        return popup(model, created ? "등록에 성공하였습니다." : "등록에 실패했습니다.");
    }

    /**
     * 직급 수정 화면.
     *
     * @param jobId 직급 번호
     * @param model 뷰 모델
     * @return 수정 템플릿. 대상이 없으면 잘못된 접근 화면
     */
    @GetMapping("/job-update")
    public String jobUpdateForm(@RequestParam(required = false) Integer jobId, Model model) {
        AdminPermissionService.JobForm form =
                jobId == null ? null : permissionService.jobForm(jobId);
        if (form == null) {
            return "redirect:/access-error";
        }
        model.addAttribute("form", form);
        return "admin/permission/job-update";
    }

    /**
     * 직급 수정 처리.
     *
     * @param jobId 직급 번호
     * @param jobName 직급명
     * @param permissions 체크된 직급 번호들
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/job-update-proc")
    public String jobUpdate(
            @RequestParam(required = false) Integer jobId,
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) List<Integer> permissions,
            Model model) {

        if (jobId == null || jobName == null) {
            return "redirect:/access-error";
        }
        return result(model, permissionService.updateJob(jobId, jobName, permissions)
                ? "직급 정보를 수정했습니다." : "직급 정보를 수정하지 못했습니다.");
    }

    /**
     * 부서 수정 화면.
     *
     * @param deptId 부서 번호
     * @param model 뷰 모델
     * @return 수정 템플릿. 대상이 없으면 잘못된 접근 화면
     */
    @GetMapping("/dept-update")
    public String deptUpdateForm(@RequestParam(required = false) Integer deptId, Model model) {
        AdminPermissionService.DeptForm form =
                deptId == null ? null : permissionService.deptForm(deptId);
        if (form == null) {
            return "redirect:/access-error";
        }
        model.addAttribute("form", form);
        return "admin/permission/dept-update";
    }

    /**
     * 부서 수정 처리.
     *
     * <p><b>주소가 지워진다.</b> 화면이 주소칸을 채워 주지 않는데 처리는 그 빈 값을
     * 그대로 저장한다. 레거시 그대로다(D-059).
     *
     * @param deptId 부서 번호
     * @param deptName 부서명
     * @param postalCode 우편번호
     * @param address1 도로명 주소
     * @param address2 상세 주소
     * @param permissions 체크된 부서 번호들
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/dept-update-proc")
    public String deptUpdate(
            @RequestParam(required = false) Integer deptId,
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) String address1,
            @RequestParam(required = false) String address2,
            @RequestParam(required = false) List<Integer> permissions,
            Model model) {

        if (deptId == null || deptName == null) {
            return "redirect:/access-error";
        }
        boolean updated = permissionService.updateDept(
                deptId, deptName, postalCode, address1, address2, permissions);
        return result(model, updated
                ? "부서 정보를 수정했습니다." : "부서 정보를 수정하지 못했습니다.");
    }

    /**
     * 선택한 직급 삭제.
     *
     * @param jobId 지울 직급 번호들
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/job-delete-proc")
    public String jobDelete(
            @RequestParam(required = false) List<Integer> jobId, Model model) {
        if (jobId == null) {
            return result(model, "선택한 직급이 없습니다.");
        }
        return result(model, permissionService.deleteJobs(jobId)
                ? "직급을 삭제했습니다." : "직급을 삭제하지 못했습니다.");
    }

    /**
     * 선택한 부서 삭제.
     *
     * @param deptId 지울 부서 번호들
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/dept-delete-proc")
    public String deptDelete(
            @RequestParam(required = false) List<Integer> deptId, Model model) {
        if (deptId == null) {
            return result(model, "선택한 부서가 없습니다.");
        }
        return result(model, permissionService.deleteDepts(deptId)
                ? "부서를 삭제했습니다." : "부서를 삭제하지 못했습니다.");
    }

    /**
     * 프로그램 리스트.
     *
     * @param keyword 프로그램 이름 검색어
     * @param nowPage 현재 페이지
     * @param model 뷰 모델
     * @return 목록 템플릿
     */
    @GetMapping("/program-list")
    public String programList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int nowPage,
            Model model) {

        AdminPermissionService.ProgramPage page = permissionService.programs(keyword, nowPage);
        model.addAttribute("programs", page.rows());
        model.addAttribute("page", page.pagination());
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        return "admin/permission/program-list";
    }

    /**
     * 프로그램 부서 권한 수정 화면.
     *
     * @param id 프로그램 행 번호
     * @param model 뷰 모델
     * @return 수정 템플릿. 대상이 없으면 잘못된 접근 화면
     */
    @GetMapping("/program-dept-update")
    public String programDeptForm(@RequestParam(required = false) Integer id, Model model) {
        AdminPermissionService.ProgramForm form =
                id == null ? null : permissionService.programDeptForm(id);
        if (form == null) {
            return "redirect:/access-error";
        }
        model.addAttribute("form", form);
        return "admin/permission/program-dept-update";
    }

    /**
     * 프로그램 부서 권한 수정 처리.
     *
     * <p>레거시 폼에는 {@code method} 가 없어 <b>GET</b> 이었다. 주소만 알면 링크 한 번으로
     * 권한이 바뀐다. CSRF 방어를 켠 이관에서는 POST 로 받는다(D-031 과 같은 판단, D-065).
     *
     * @param programId 프로그램 행 번호
     * @param permissions 체크된 부서 번호들
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/program-dept-update-proc")
    public String programDeptUpdate(
            @RequestParam(required = false) Integer programId,
            @RequestParam(required = false) List<Integer> permissions,
            Model model) {

        if (programId == null) {
            return "redirect:/access-error";
        }
        boolean updated = permissionService.updateProgramDeptLevel(programId, permissions);
        return programResult(model, updated
                ? "부서 권한 정보를 수정했습니다." : "부서 권한을 수정하지 못했습니다.");
    }

    /**
     * 프로그램 직급 권한 수정 화면.
     *
     * @param id 프로그램 행 번호
     * @param model 뷰 모델
     * @return 수정 템플릿. 대상이 없으면 잘못된 접근 화면
     */
    @GetMapping("/program-job-update")
    public String programJobForm(@RequestParam(required = false) Integer id, Model model) {
        AdminPermissionService.ProgramForm form =
                id == null ? null : permissionService.programJobForm(id);
        if (form == null) {
            return "redirect:/access-error";
        }
        model.addAttribute("form", form);
        return "admin/permission/program-job-update";
    }

    /**
     * 프로그램 직급 권한 수정 처리.
     *
     * @param programId 프로그램 행 번호
     * @param permissions 체크된 직급 번호들
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/program-job-update-proc")
    public String programJobUpdate(
            @RequestParam(required = false) Integer programId,
            @RequestParam(required = false) List<Integer> permissions,
            Model model) {

        if (programId == null) {
            return "redirect:/access-error";
        }
        boolean updated = permissionService.updateProgramJobLevel(programId, permissions);
        return programResult(model, updated
                ? "직급 권한 정보를 수정했습니다." : "직급 권한을 수정하지 못했습니다.");
    }

    /** 레거시가 값을 다듬는 방식. 빈 값은 {@code null} 로 저장된다. */
    private static String blankToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String result(Model model, String message) {
        model.addAttribute("message", message);
        model.addAttribute("nextPage", LIST);
        model.addAttribute("closePopup", false);
        return RESULT;
    }

    private String popup(Model model, String message) {
        model.addAttribute("message", message);
        model.addAttribute("nextPage", LIST);
        model.addAttribute("closePopup", true);
        return RESULT;
    }

    private String programResult(Model model, String message) {
        model.addAttribute("message", message);
        model.addAttribute("nextPage", "/admin/permission/program-list");
        model.addAttribute("closePopup", false);
        return RESULT;
    }
}
