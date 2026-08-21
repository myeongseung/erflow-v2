package com.erflow.unit;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 생산 설비 관리 화면.
 *
 * <p>레거시 라우팅은 {@code migration/design/00-decisions.md} 의 D-005 규칙으로 옮겼다.
 *
 * <pre>
 * unitList.jsp          GET  /unit/list
 * unitRegister.jsp      GET  /unit/register      (팝업)
 * unitRegisterProc.jsp  POST /unit/register
 * unitUpdate.jsp        GET  /unit/update        (팝업)
 * unitUpdateProc.jsp    POST /unit/update
 * unitDeleteProc.jsp    POST /unit/delete
 * </pre>
 *
 * <p>등록/수정은 목록에서 {@code window.open} 으로 여는 팝업이다. 처리 결과도 레거시
 * 그대로 {@code alert} 후 창을 닫고 부모를 새로 고친다. 화면 흐름을 바꾸는 것은 이관이
 * 아니라 개선이므로 별도 안건으로 둔다.
 */
@Controller
@RequestMapping("/unit")
public class UnitController {

    private static final String POPUP_RESULT = "unit/popup-result";

    private final UnitService unitService;

    /**
     * @param unitService 설비 업무
     */
    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    /**
     * 설비 목록.
     *
     * @param keyfield 검색 대상
     * @param keyword 검색어
     * @param nowPage 현재 페이지
     * @param model 뷰 모델
     * @return 목록 템플릿
     */
    @GetMapping("/list")
    public String list(
            @RequestParam(required = false) String keyfield,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int nowPage,
            Model model) {

        UnitSearch search = new UnitSearch(keyfield, keyword);
        UnitService.UnitPage page = unitService.list(search, nowPage);

        model.addAttribute("units", page.rows());
        model.addAttribute("page", page.pagination());
        model.addAttribute("keyfield", keyfield == null ? "" : keyfield);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        return "unit/list";
    }

    /**
     * 설비 등록 팝업.
     *
     * @return 등록 템플릿
     */
    @GetMapping("/register")
    public String registerForm() {
        return "unit/register";
    }

    /**
     * 설비 등록 처리.
     *
     * <p>레거시는 다섯 개 파라미터 중 하나라도 없으면 등록하지 않았고, 빈 문자열은
     * {@code null} 로 바꿔 넣었다. 문서번호는 숫자가 아니면 넣지 않았다.
     *
     * @param equipmentId 장비ID
     * @param userId 관리자ID
     * @param documentId 문서ID
     * @param equipmentName 장비명
     * @param manufactureDate 제조일자
     * @param model 뷰 모델
     * @return 팝업 결과 템플릿
     */
    @PostMapping("/register")
    public String register(
            @RequestParam(required = false) String equipmentId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String documentId,
            @RequestParam(required = false) String equipmentName,
            @RequestParam(required = false) String manufactureDate,
            Model model) {

        boolean present = equipmentId != null && userId != null && documentId != null
                && equipmentName != null && manufactureDate != null;

        boolean created = false;
        if (present) {
            Unit unit = new Unit(
                    blankToNull(equipmentId),
                    blankToNull(userId),
                    parseDocumentId(documentId),
                    blankToNull(equipmentName),
                    1,
                    blankToNull(manufactureDate));
            created = unitService.create(unit);
        }
        return popup(model, created ? "설비를 등록했습니다." : "설비를 등록하지 못했습니다.");
    }

    /**
     * 설비 수정 팝업.
     *
     * <p>레거시는 대상이 없으면 {@code accessError.jsp} 로 보냈다.
     *
     * @param id 장비ID
     * @param model 뷰 모델
     * @return 수정 템플릿. 대상이 없으면 오류 화면으로 보낸다
     */
    @GetMapping("/update")
    public String updateForm(@RequestParam(required = false) String id, Model model) {
        Unit unit = unitService.get(id);
        if (unit == null) {
            return "redirect:/access-error";
        }
        model.addAttribute("unit", unit);
        return "unit/update";
    }

    /**
     * 설비 수정 처리.
     *
     * @param id 장비ID
     * @param userID 관리자 사번
     * @param documentID 문서번호
     * @param equipmentName 장비명
     * @param status 장비 상태
     * @param model 뷰 모델
     * @return 팝업 결과 템플릿
     */
    @PostMapping("/update")
    public String update(
            @RequestParam String id,
            @RequestParam(required = false) String userID,
            @RequestParam(required = false) String documentID,
            @RequestParam(required = false) String equipmentName,
            @RequestParam(defaultValue = "0") int status,
            Model model) {

        Unit unit = new Unit(
                id,
                blankToNull(userID),
                parseDocumentId(documentID),
                blankToNull(equipmentName),
                status,
                null);
        boolean updated = unitService.update(unit);
        return popup(model, updated ? "설비를 수정했습니다." : "설비를 수정하지 못했습니다.");
    }

    /**
     * 선택된 설비 삭제.
     *
     * <p>목록 화면의 form 에서 넘어온다. 팝업이 아니라 목록으로 돌아간다.
     *
     * @param unitId 선택된 장비ID 목록
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/delete")
    public String delete(
            @RequestParam(required = false) List<String> unitId, Model model) {

        String message;
        if (unitId == null || unitId.isEmpty()) {
            message = "삭제할 설비를 선택해주세요.";
        } else {
            message = unitService.delete(unitId)
                    ? "설비를 삭제하였습니다."
                    : "설비를 삭제하지 못했습니다.";
        }
        model.addAttribute("message", message);
        model.addAttribute("nextPage", "/unit/list");
        model.addAttribute("closePopup", false);
        return POPUP_RESULT;
    }

    private String popup(Model model, String message) {
        model.addAttribute("message", message);
        model.addAttribute("nextPage", null);
        model.addAttribute("closePopup", true);
        return POPUP_RESULT;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 문서번호를 숫자로 바꾼다.
     *
     * <p>레거시는 숫자가 아니면 조용히 넘겼다. 화면에서 읽기 전용이라 사용자가 직접
     * 칠 일이 없고, 문서를 고르지 않은 채 등록하는 경우가 여기 걸린다.
     */
    private static Long parseDocumentId(String value) {
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException | NullPointerException expected) {
            return null;
        }
    }
}
