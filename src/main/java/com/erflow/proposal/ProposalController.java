package com.erflow.proposal;

import com.erflow.auth.ErflowUserDetails;
import com.erflow.document.DocumentFinder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 결재 화면.
 *
 * <pre>
 * proposalList.jsp      GET  /proposal/list
 * proposalRegister.jsp  GET  /proposal/register   (결재 리스트 + 열린 대화상자, D-161)
 * proposalRegisterProc  POST /proposal/register-proc
 * proposalDocument.jsp  GET  /proposal/document?proposalId=
 * proposalDocumentProc  POST /proposal/document-proc
 * </pre>
 *
 * <p>결재라인 관리({@link ProposalRouteController})와 같은 {@code /proposal} 아래지만
 * 프로그램(권한)이 다르다.
 */
@Controller
public class ProposalController {

    private final ProposalService proposalService;
    private final ProposalRouteService proposalRouteService;
    private final DocumentFinder documentFinder;

    /**
     * @param proposalService 결재 업무
     * @param proposalRouteService 결재라인(결재 올리기 대화상자가 고를 것)
     * @param documentFinder 결재에 올릴 수 있는 내 문서(결재 올리기 대화상자가 고를 것)
     */
    public ProposalController(
            ProposalService proposalService,
            ProposalRouteService proposalRouteService,
            DocumentFinder documentFinder) {
        this.proposalService = proposalService;
        this.proposalRouteService = proposalRouteService;
        this.documentFinder = documentFinder;
    }

    /**
     * 결재 리스트.
     *
     * @param keyfield 상태 필터(3 결재진행중 / 1 승인 / 2 반려). 없으면 3
     * @param nowPage 현재 페이지
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 결재 리스트 템플릿
     */
    @GetMapping("/proposal/list")
    public String list(
            @RequestParam(defaultValue = "3") int keyfield,
            @RequestParam(defaultValue = "1") int nowPage,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {
        fillList(keyfield, nowPage, user, model);
        return "proposal/list";
    }

    /**
     * 결재 올리기 — 결재 리스트를 «결재 올리기» 대화상자가 열린 채로 그린다(D-161).
     *
     * <p>결재 올리기는 별도 화면이 아니라 결재 리스트 위의 대화상자다. 옛 주소로
     * 들어와도 같은 곳에 닿도록 주소는 남긴다(관리자 화면의 옛 주소와 같은 방식,
     * D-148). 문서 리스트의 «결재 올리기» 가 {@code ?documentId=} 를 달고 오면 그
     * 문서가 미리 골라져 있다.
     *
     * @param documentId 미리 고를 문서번호. 없으면 아무것도 골라져 있지 않다
     * @param user 로그인 사용자(기안자)
     * @param model 뷰 모델
     * @return 결재 리스트 템플릿
     */
    @GetMapping("/proposal/register")
    public String registerForm(
            @RequestParam(required = false) Long documentId,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {
        fillList(ProposalService.IN_PROGRESS, 1, user, model);
        model.addAttribute("openRegister", true);
        model.addAttribute("pickedDocumentId", documentId);
        return "proposal/list";
    }

    /**
     * 결재 리스트의 모델을 채운다 — 목록과, 그 위의 «결재 올리기» 대화상자가 고를 것.
     *
     * <p>대화상자가 고를 목록(결재에 올린 적 없는 내 문서 · 내 결재라인)은 리스트를
     * 열 때마다 읽는다. 그래서 문서는 한 질의, 결재라인은 사람 풀이까지 둘이다
     * (D-161, D-152).
     */
    private void fillList(int keyfield, int nowPage, ErflowUserDetails user, Model model) {
        ProposalService.ProposalPage page = proposalService.list(user.id(), keyfield, nowPage);
        model.addAttribute("proposals", page.rows());
        model.addAttribute("page", page.pagination());
        model.addAttribute("keyfield", keyfield);
        model.addAttribute("pickDocuments", documentFinder.search(user.id(), null));
        model.addAttribute("pickRoutes", proposalRouteService.all(user.id()));
    }

    /**
     * 결재 등록 처리.
     *
     * <p>레거시 {@code proposalRegisterProc.jsp} 를 옮겼다. 문서와 결재라인이 모두
     * 있어야 등록한다. 옛 화면이 함께 보내던 결재경로 문자열은 저장하지도 않던 값이라
     * 받지 않는다(D-161). 성공하면 그 결재의 문서 상세로, 실패하면 대화상자가 열린
     * 결재 리스트로 간다.
     *
     * @param documentId 문서번호
     * @param routeId 결재라인 번호
     * @param user 로그인 사용자(기안자)
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/proposal/register-proc")
    public String register(
            @RequestParam(required = false) Long documentId,
            @RequestParam(required = false) Integer routeId,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {

        long created = documentId != null && routeId != null
                ? proposalService.create(user.id(), documentId, routeId)
                : 0;

        model.addAttribute("message", created > 0 ? "등록에 성공했습니다." : "등록에 실패했습니다.");
        model.addAttribute("nextPage", created > 0
                ? "/proposal/document?proposalId=" + created
                : "/proposal/register");
        return "proposal/route-result";
    }

    /**
     * 문서 상세.
     *
     * <p>결재 스탬프 표와 문서 내용, 결재 의견을 함께 보여준다. 결재할 수 있는 상태면
     * 의견 입력과 승인·반려 버튼이 딸려 나온다.
     *
     * @param proposalId 결재번호
     * @param model 뷰 모델
     * @return 문서 상세 템플릿. 대상이 없으면 잘못된 접근 화면
     */
    @GetMapping("/proposal/document")
    public String document(@RequestParam(required = false) Long proposalId, Model model) {
        ProposalDocument document =
                proposalId == null ? null : proposalService.document(proposalId);
        if (document == null) {
            return "redirect:/access-error";
        }
        model.addAttribute("document", document);
        return "proposal/document";
    }

    /**
     * 승인/반려 처리.
     *
     * <p>레거시 {@code proposalDocumentProc.jsp} 를 옮겼다. 어느 쪽이든 결재 리스트로
     * 돌아간다. 안내 문구도 레거시 그대로다 — 마지막 차례가 아닌 승인이
     * «결재완료하였습니다», 마지막 차례의 승인이 «결재하였습니다» 로, 뜻이 뒤집혀
     * 보이지만 손대지 않는다.
     *
     * <p>레거시는 {@code result} 가 <b>없으면</b> 잘못된 접근 화면으로 보내고,
     * <b>비어 있으면</b> 500 으로 죽었다. 죽는 쪽은 옮기지 않고 둘 다 잘못된 접근으로
     * 본다(D-033 과 같은 판단).
     *
     * <p><b>내 차례가 아니면 권한 화면으로 보낸다(D-102).</b> 레거시는 결재번호만
     * 맞으면 누구든 승인·반려할 수 있었다(D-049) — 2단계에서 막았다.
     *
     * @param proposalId 결재번호
     * @param result 승인이면 {@code confirm}, 반려면 {@code reject}
     * @param comment 결재 의견
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 결과 템플릿. 대상이 없으면 잘못된 접근, 남의 차례면 권한 화면
     */
    @PostMapping("/proposal/document-proc")
    public String documentProc(
            @RequestParam(required = false) Long proposalId,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {

        if (proposalId == null || result == null || result.isBlank()) {
            return "redirect:/access-error";
        }
        ProposalService.Decision decision =
                proposalService.decide(proposalId, user.id(), result, comment);
        if (decision == ProposalService.Decision.NOT_YOUR_TURN) {
            return "redirect:/permission-error";
        }
        if (!decision.done()) {
            return "redirect:/access-error";
        }
        model.addAttribute("message", decision.message());
        model.addAttribute("nextPage", "/proposal/list");
        return "proposal/route-result";
    }
}
