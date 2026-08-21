package com.erflow.proposal;

import com.erflow.auth.ErflowUserDetails;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 결재라인 관리 화면.
 *
 * <pre>
 * proposalRouteList.jsp        GET  /proposal/route-list
 * proposalRouteRegister.jsp    GET  /proposal/route-register
 * proposalRouteRegisterProc    POST /proposal/route-register-proc
 * proposalRouteUpdate.jsp      GET  /proposal/route-update?id=
 * proposalRouteUpdateProc      POST /proposal/route-update-proc
 * proposalRouteDeleteProc      POST /proposal/route-delete-proc
 * </pre>
 *
 * <p>내가 만든 결재라인만 보인다 — 조회가 사번으로 좁혀져 있다.
 *
 * <p>등록 화면은 사용자 찾기로 결재자를 고르면 화면을 다시 그리는 self-reload 폼이다.
 * 고른 사번을 Base64(URL 인코딩)로 감싸 넘긴다.
 */
@Controller
@RequestMapping("/proposal")
public class ProposalRouteController {

    private final ProposalRouteService proposalRouteService;

    /**
     * @param proposalRouteService 결재라인 업무
     */
    public ProposalRouteController(ProposalRouteService proposalRouteService) {
        this.proposalRouteService = proposalRouteService;
    }

    /**
     * 결재라인 목록.
     *
     * @param keyfield 검색 대상
     * @param keyword 검색어
     * @param nowPage 현재 페이지
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 결재라인 목록 템플릿
     */
    @GetMapping("/route-list")
    public String routeList(
            @RequestParam(required = false) String keyfield,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int nowPage,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {

        ProposalRouteSearch search = new ProposalRouteSearch(
                keyfield == null ? "" : keyfield, keyword == null ? "" : keyword);
        ProposalRouteService.RoutePage page =
                proposalRouteService.list(user.id(), search, nowPage);

        model.addAttribute("routes", page.items());
        model.addAttribute("page", page.pagination());
        model.addAttribute("keyfield", keyfield == null ? "" : keyfield);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        return "proposal/route-list";
    }

    /**
     * 결재라인 등록 화면.
     *
     * <p>사용자 찾기가 고른 결재자를 {@code receiver} 로 넘기면 표를 다시 그린다.
     *
     * @param receiver Base64(URL 인코딩)로 감싼 결재자 사번. 처음엔 없음
     * @param nickname 결재명(다시 그릴 때 유지)
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 등록 템플릿
     */
    @GetMapping("/route-register")
    public String routeRegisterForm(
            @RequestParam(required = false) String receiver,
            @RequestParam(required = false) String nickname,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {
        ProposalRouteService.RouteForm form = proposalRouteService.registerForm(receiver);
        model.addAttribute("userId", user.id());
        model.addAttribute("nickname", nickname == null ? "" : nickname);
        model.addAttribute("routeUserNames", form.routeUserNames());
        model.addAttribute("routeUsers", form.users());
        return "proposal/route-register";
    }

    /**
     * 결재라인 등록 처리.
     *
     * @param nickname 결재명
     * @param routeId 고른 결재자 사번(순서대로)
     * @param user 로그인 사용자(만든 사람이자 결재 순서 맨 앞)
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/route-register-proc")
    public String routeRegister(
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) List<String> routeId,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {
        boolean created = nickname != null && !nickname.isBlank() && routeId != null
                && proposalRouteService.create(user.id(), nickname, routeId);
        return result(model, created ? "등록에 성공했습니다." : "등록에 실패했습니다.",
                created ? "/proposal/route-list" : "/proposal/route-register");
    }

    /**
     * 결재라인 수정 화면.
     *
     * @param id 결재관리번호
     * @param model 뷰 모델
     * @return 수정 템플릿. 대상이 없으면 잘못된 접근 화면
     */
    @GetMapping("/route-update")
    public String routeUpdateForm(@RequestParam(required = false) Integer id, Model model) {
        ProposalRouteService.RouteEditForm form =
                id == null ? null : proposalRouteService.updateForm(id);
        if (form == null) {
            return "redirect:/access-error";
        }
        model.addAttribute("proposalId", id);
        model.addAttribute("nickname", form.nickname());
        model.addAttribute("routeUsers", form.users());
        return "proposal/route-update";
    }

    /**
     * 결재라인 수정 처리.
     *
     * @param proposalId 결재관리번호
     * @param nickname 결재명
     * @param routeId 결재자 사번(순서대로)
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/route-update-proc")
    public String routeUpdate(
            @RequestParam(required = false) Integer proposalId,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) List<String> routeId,
            Model model) {
        boolean updated = proposalId != null && nickname != null && routeId != null
                && proposalRouteService.update(proposalId, nickname, routeId);
        return result(model, updated ? "수정에 성공했습니다." : "수정에 실패했습니다.",
                "/proposal/route-list");
    }

    /**
     * 선택된 결재라인 삭제.
     *
     * @param proposalId 지울 결재관리번호 목록
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/route-delete-proc")
    public String routeDelete(
            @RequestParam(required = false) List<Integer> proposalId, Model model) {
        boolean deleted = proposalId != null && proposalRouteService.delete(proposalId);
        return result(model, deleted ? "선택한 내역을 삭제하였습니다." : "선택한 내역을 삭제하지 못했습니다.",
                "/proposal/route-list");
    }

    private String result(Model model, String message, String nextPage) {
        model.addAttribute("message", message);
        model.addAttribute("nextPage", nextPage);
        return "proposal/route-result";
    }
}
