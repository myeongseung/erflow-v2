package com.erflow.bound;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 입·출고 관리 화면.
 *
 * <pre>
 * inbound.jsp          GET  /bound/inbound        (입고, type=0)
 * outbound.jsp         GET  /bound/outbound       (출고, type=1)
 * boundRegister.jsp    GET  /bound/register?flag=
 * boundRegisterProc    POST /bound/register-proc
 * boundUpdate.jsp      GET  /bound/update?flag=&amp;id=
 * boundUpdateProc      POST /bound/update-proc
 * boundDeleteProc      POST /bound/delete-proc
 * </pre>
 *
 * <p>입고와 출고는 거의 같은 화면인데 {@code type} 과 권한이 갈린다. 레거시가 파일과
 * 경로를 둘로 나눠 두었고({@code screen} 테이블도 {@code /bound/inbound}·
 * {@code /bound/outbound}) 화면 글자(입고/출고, 입고자/출고자)까지 달라 템플릿도 둘로 둔다.
 *
 * <p>등록·수정·삭제는 다음 단계에서 붙인다. 레거시는 그 액션들을 {@code -proc} 경로로
 * 따로 두었고({@code /bound/register-proc} 등) 권한도 flag 로 갈린다.
 */
@Controller
@RequestMapping("/bound")
public class BoundController {

    private final BoundService boundService;

    /**
     * @param boundService 입·출고 업무
     */
    public BoundController(BoundService boundService) {
        this.boundService = boundService;
    }

    /**
     * 입고 목록.
     *
     * @param keyfield 검색 대상
     * @param keyword 검색어
     * @param nowPage 현재 페이지
     * @param model 뷰 모델
     * @return 입고 목록 템플릿
     */
    @GetMapping("/inbound")
    public String inbound(
            @RequestParam(required = false) String keyfield,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int nowPage,
            Model model) {
        return listView(BoundService.INBOUND, "bound/inbound", keyfield, keyword, nowPage, model);
    }

    /**
     * 출고 목록.
     *
     * @param keyfield 검색 대상
     * @param keyword 검색어
     * @param nowPage 현재 페이지
     * @param model 뷰 모델
     * @return 출고 목록 템플릿
     */
    @GetMapping("/outbound")
    public String outbound(
            @RequestParam(required = false) String keyfield,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int nowPage,
            Model model) {
        return listView(BoundService.OUTBOUND, "bound/outbound", keyfield, keyword, nowPage, model);
    }

    private String listView(
            int type, String view, String keyfield, String keyword, int nowPage, Model model) {
        BoundSearch search = new BoundSearch(
                keyfield == null ? "" : keyfield, keyword == null ? "" : keyword);
        BoundService.BoundPage page = boundService.list(type, search, nowPage);

        model.addAttribute("bounds", page.rows());
        model.addAttribute("page", page.pagination());
        model.addAttribute("keyfield", keyfield == null ? "" : keyfield);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        return view;
    }

    /**
     * 입·출고 등록 화면.
     *
     * @param flag inbound 또는 outbound
     * @param model 뷰 모델
     * @return 등록 템플릿. flag 가 없으면 잘못된 접근 화면
     */
    @GetMapping("/register")
    public String registerForm(@RequestParam(required = false) String flag, Model model) {
        if (!"inbound".equals(flag) && !"outbound".equals(flag)) {
            return "redirect:/access-error";
        }
        model.addAttribute("flag", flag);
        model.addAttribute("isInbound", "inbound".equals(flag));
        return "bound/register";
    }

    /**
     * 입·출고 등록 처리.
     *
     * <p>레거시 {@code boundRegisterProc.jsp} 를 옮겼다. 여덟 값(제품·사번·우편번호·
     * 도로명·상세주소·날짜·수량·flag)이 모두 있어야 등록한다. 하나라도 없으면 «실패».
     *
     * @param productId 제품 코드
     * @param userId 입고자(출고자) 사번
     * @param postalCode 우편번호
     * @param address1 도로명 주소
     * @param address2 상세주소
     * @param boundedAt 입고(출고) 시간
     * @param count 수량
     * @param flag inbound 또는 outbound
     * @param model 뷰 모델
     * @return 결과 템플릿. 값이 없으면 잘못된 접근 화면
     */
    @PostMapping("/register-proc")
    public String register(
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) String address1,
            @RequestParam(required = false) String address2,
            @RequestParam(required = false) String boundedAt,
            @RequestParam(required = false) Integer count,
            @RequestParam(required = false) String flag,
            Model model) {

        boolean present = productId != null && userId != null && postalCode != null
                && address1 != null && address2 != null && boundedAt != null
                && count != null && flag != null;
        if (!present) {
            // 레거시는 하나라도 없으면 accessError 로 보냈다.
            return "redirect:/access-error";
        }
        boolean inbound = "inbound".equals(flag);
        boolean created = boundService.create(new Bound(productId, userId, postalCode,
                address1, address2, boundedAt, count, inbound ? 0 : 1));

        String label = inbound ? "입고" : "출고";
        model.addAttribute("message",
                label + (created ? " 정보를 등록하였습니다." : " 정보를 등록하지 못했습니다."));
        model.addAttribute("nextPage", inbound ? "/bound/inbound" : "/bound/outbound");
        return "bound/result";
    }

    /**
     * 입·출고 수정 화면.
     *
     * @param flag inbound 또는 outbound
     * @param id 번호
     * @param model 뷰 모델
     * @return 수정 템플릿. 대상이 없으면 잘못된 접근 화면
     */
    @GetMapping("/update")
    public String updateForm(
            @RequestParam(required = false) String flag,
            @RequestParam(required = false) Integer id,
            Model model) {
        boolean inbound = "inbound".equals(flag);
        BoundDetail bound = id == null ? null : boundService.get(id, inbound ? 0 : 1);
        if (bound == null) {
            return "redirect:/access-error";
        }
        model.addAttribute("flag", flag);
        model.addAttribute("isInbound", inbound);
        model.addAttribute("boundId", id);
        model.addAttribute("bound", bound);
        return "bound/update";
    }

    /**
     * 입·출고 수정 처리.
     *
     * <p>레거시 {@code boundUpdateProc.jsp} 를 옮겼다. 여덟 값(번호·제품·사번·우편번호·
     * 도로명·상세주소·날짜·수량)이 모두 있어야 처리한다. 입고 시간은 바꾸지 않는다.
     *
     * @param boundId 번호
     * @param productId 제품 코드
     * @param userId 입고자(출고자) 사번
     * @param postalCode 우편번호
     * @param address1 도로명 주소
     * @param address2 상세주소
     * @param boundedAt 입고(출고) 시간(무시된다)
     * @param count 수량
     * @param flag inbound 또는 outbound
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/update-proc")
    public String update(
            @RequestParam(required = false) Integer boundId,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) String address1,
            @RequestParam(required = false) String address2,
            @RequestParam(required = false) String boundedAt,
            @RequestParam(required = false) Integer count,
            @RequestParam(required = false) String flag,
            Model model) {

        boolean present = boundId != null && productId != null && userId != null
                && postalCode != null && address1 != null && address2 != null
                && boundedAt != null && count != null;
        boolean inbound = "inbound".equals(flag);
        boolean updated = present && boundService.update(new BoundUpdate(
                boundId, productId, userId, postalCode, address1, address2,
                count, inbound ? 0 : 1));

        model.addAttribute("message", updated ? "수정에 성공했습니다." : "수정에 실패했습니다.");
        model.addAttribute("nextPage", inbound ? "/bound/inbound" : "/bound/outbound");
        return "bound/result";
    }

    /**
     * 선택된 입·출고 삭제.
     *
     * @param boundId 지울 번호 목록
     * @param flag inbound 또는 outbound
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/delete-proc")
    public String delete(
            @RequestParam(required = false) List<Integer> boundId,
            @RequestParam(required = false) String flag,
            Model model) {
        boolean inbound = "inbound".equals(flag);
        boolean valid = boundId != null && flag != null && !flag.isBlank()
                && boundService.delete(boundId, inbound ? 0 : 1);
        model.addAttribute("message",
                valid ? "선택한 내역을 삭제하였습니다." : "선택한 내역을 삭제하지 못했습니다.");
        model.addAttribute("nextPage", inbound ? "/bound/inbound" : "/bound/outbound");
        return "bound/result";
    }
}
