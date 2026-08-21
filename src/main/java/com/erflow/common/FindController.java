package com.erflow.common;

import com.erflow.auth.ErflowUserDetails;
import com.erflow.company.CompanyCodes;
import com.erflow.company.CompanyFinder;
import com.erflow.document.DocumentFinder;
import com.erflow.product.ProductFinder;
import com.erflow.proposal.ProposalRouteFinder;
import com.erflow.user.UserFinder;
import com.erflow.user.UserSearch;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 코드 찾기 팝업.
 *
 * <pre>
 * findBank.jsp          GET /find/bank
 * findWork.jsp          GET /find/work
 * findDocument.jsp      GET /find/document
 * findCompany.jsp       GET /find/company
 * findProduct.jsp       GET /find/product
 * findMultiProduct.jsp  GET /find/multi-product
 * findUser.jsp          GET /find/user
 * findEachUser.jsp      GET /find/each-user
 * findProposalRoute.jsp GET /find/proposal-route
 * </pre>
 *
 * <p>등록 화면에서 {@code window.open} 으로 여는 작은 창이다. 고른 값을
 * {@code window.opener.receiveBankInfo(코드, 이름)} 로 부모에게 돌려주고 닫힌다.
 * 그 흐름은 레거시 그대로다 — 요즘 방식으로 바꾸면 부모 화면까지 손대야 한다.
 *
 * <h2>검색 전과 검색어가 빈 것은 다르다</h2>
 *
 * <p>레거시는 {@code request.getParameter("search")} 가 {@code null} 이면 도움말을
 * 보여주고, 빈 문자열이면 <b>전체 목록</b>을 보여준다. 창을 처음 열었을 때와
 * 검색창을 비운 채 누른 것을 구별하는 것이다. 그대로 옮긴다.
 *
 * <p>다만 코드 찾기 셋만 그렇다. 사용자 찾기는 도움말이 아예 없고 열자마자
 * 전체 목록이 나온다({@link #user}).
 */
@Controller
@RequestMapping("/find")
public class FindController {

    private final CompanyCodes codes;
    private final DocumentFinder documents;
    private final UserFinder users;
    private final CompanyFinder companies;
    private final ProductFinder products;
    private final ProposalRouteFinder routes;

    /**
     * @param codes 은행·업종 코드표
     * @param documents 문서 조회
     * @param users 사용자 조회
     * @param companies 협력업체 조회
     * @param products 제품 조회
     * @param routes 결재라인 조회
     */
    public FindController(
            CompanyCodes codes,
            DocumentFinder documents,
            UserFinder users,
            CompanyFinder companies,
            ProductFinder products,
            ProposalRouteFinder routes) {
        this.codes = codes;
        this.documents = documents;
        this.users = users;
        this.companies = companies;
        this.products = products;
        this.routes = routes;
    }

    /**
     * 은행 찾기.
     *
     * @param search 검색어. 파라미터 자체가 없으면 {@code null}
     * @param model 뷰 모델
     * @return 팝업 템플릿
     */
    @GetMapping("/bank")
    public String bank(@RequestParam(required = false) String search, Model model) {
        model.addAttribute("search", search);
        model.addAttribute("entries", search == null ? null : codes.bankTable().search(search));
        return "find/bank";
    }

    /**
     * 업종 찾기.
     *
     * @param search 검색어. 파라미터 자체가 없으면 {@code null}
     * @param model 뷰 모델
     * @return 팝업 템플릿
     */
    @GetMapping("/work")
    public String work(@RequestParam(required = false) String search, Model model) {
        model.addAttribute("search", search);
        model.addAttribute("entries", search == null ? null : codes.fieldTable().search(search));
        return "find/work";
    }

    /**
     * 문서 찾기.
     *
     * <p>은행·업종과 달리 <b>도움말이 없다.</b> 레거시가 검색어를 빈 문자열로
     * 채워 놓고 {@code null} 인지 검사해서, 도움말 갈래가 한 번도 실행되지 않는다.
     * 그대로 옮긴다 — D-036 참조.
     *
     * @param search 검색어
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 팝업 템플릿
     */
    @GetMapping("/document")
    public String document(
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {
        // 레거시가 Optional.orElse("") 로 채운다. 그래서 도움말 조건이 절대 참이
        // 되지 않는다. 여기서 null 을 그대로 넘기면 레거시에 없던 도움말이 뜬다.
        model.addAttribute("search", search == null ? "" : search);
        model.addAttribute("entries", documents.search(user.id(), search));
        return "find/document";
    }

    /**
     * 협력업체 찾기.
     *
     * <p>도움말이 번호로도 찾을 수 있다고 안내하지만 실제로는 이름만 훑는다 —
     * {@link CompanyFinder#search} 설명 참조(D-039).
     *
     * @param search 검색어. 파라미터 자체가 없으면 {@code null}
     * @param model 뷰 모델
     * @return 팝업 템플릿
     */
    @GetMapping("/company")
    public String company(@RequestParam(required = false) String search, Model model) {
        model.addAttribute("search", search);
        model.addAttribute("entries", search == null ? null : companies.search(search));
        return "find/company";
    }

    /**
     * 제품 찾기 — 한 개.
     *
     * @param search 검색어. 파라미터 자체가 없으면 {@code null}
     * @param model 뷰 모델
     * @return 팝업 템플릿
     */
    @GetMapping("/product")
    public String product(@RequestParam(required = false) String search, Model model) {
        model.addAttribute("search", search);
        model.addAttribute("entries", search == null ? null : products.search(search));
        return "find/product";
    }

    /**
     * 제품 찾기 — 여러 개.
     *
     * <p>{@code findProduct.jsp} 와 조회가 같다. 목록을 표로 보여주고 체크박스로
     * 여럿 고른다는 점만 다르다.
     *
     * @param search 검색어. 파라미터 자체가 없으면 {@code null}
     * @param model 뷰 모델
     * @return 팝업 템플릿
     */
    @GetMapping("/multi-product")
    public String multiProduct(@RequestParam(required = false) String search, Model model) {
        model.addAttribute("search", search);
        model.addAttribute("entries", search == null ? null : products.search(search));
        return "find/multi-product";
    }

    /**
     * 결재라인 찾기.
     *
     * <p>다른 팝업과 달리 <b>내 것만</b> 보여준다. 조회가 사번으로 좁혀져 있다.
     *
     * <p>돌려주는 값이 셋이다 — 번호, 결재 순서대로 이은 사번, 그리고 이름.
     * 부모 화면이 결재선을 그 문자열로 채운다.
     *
     * @param search 검색어. 파라미터 자체가 없으면 {@code null}
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 팝업 템플릿
     */
    @GetMapping("/proposal-route")
    public String proposalRoute(
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {
        model.addAttribute("search", search);
        model.addAttribute("entries",
                search == null ? null : routes.search(user.id(), search));
        return "find/proposal-route";
    }

    /**
     * 사용자 찾기 — 여러 명.
     *
     * <p>고른 사번을 {@code ;} 로 이어 {@code window.opener.updateSelect} 에 넘긴다 —
     * 설비 등록은 그중 하나만 쓰고, 쪽지 쓰기는 전부 받는다. 팝업은 어느 쪽인지 모른다.
     *
     * <p>파라미터 이름이 {@code search} 가 아니라 셋이다. 레거시 화면이 부서·직급
     * 콤보와 검색어를 각각 {@code deptKeyfield}·{@code jobKeyfield}·{@code keyword}
     * 로 보낸다. 이름을 바꾸면 부모 화면과 즐겨찾기한 주소가 어긋난다.
     *
     * @param deptKeyfield 부서명. 콤보에서 «전체부서» 면 빈 문자열
     * @param jobKeyfield 직급명. 콤보에서 «전체직급» 이면 빈 문자열
     * @param keyword 이름 검색어
     * @param model 뷰 모델
     * @return 팝업 템플릿
     */
    @GetMapping("/user")
    public String user(
            @RequestParam(defaultValue = "") String deptKeyfield,
            @RequestParam(defaultValue = "") String jobKeyfield,
            @RequestParam(defaultValue = "") String keyword,
            Model model) {
        return listUsers(deptKeyfield, jobKeyfield, keyword, model, "find/user");
    }

    /**
     * 사용자 찾기 — 한 명.
     *
     * <p>{@code findUser.jsp} 와 조회가 같고 화면도 같다. 다른 것은 고르는 방식뿐이다 —
     * 라디오라서 한 명이고, 사번과 <b>이름</b>을 {@code receiveEachUserInfo} 로 넘긴다.
     * 이름을 표의 셀에서 읽어 가므로 표 구조가 곧 계약이다.
     *
     * <p>레거시가 이 둘을 파일 두 개로 나눠 두었다. 한 화면에 파라미터를 붙여 합칠 수
     * 있지만 그러면 부모 화면들이 여는 주소가 바뀐다 — 화면 수를 줄이는 것이 이관의
     * 목표가 아니다.
     *
     * @param deptKeyfield 부서명
     * @param jobKeyfield 직급명
     * @param keyword 이름 검색어
     * @param model 뷰 모델
     * @return 팝업 템플릿
     */
    @GetMapping("/each-user")
    public String eachUser(
            @RequestParam(defaultValue = "") String deptKeyfield,
            @RequestParam(defaultValue = "") String jobKeyfield,
            @RequestParam(defaultValue = "") String keyword,
            Model model) {
        return listUsers(deptKeyfield, jobKeyfield, keyword, model, "find/each-user");
    }

    /**
     * 두 사용자 팝업이 함께 쓰는 모델 채우기.
     *
     * @param dept 부서명
     * @param job 직급명
     * @param keyword 이름 검색어
     * @param model 뷰 모델
     * @param view 팝업 템플릿 이름
     * @return 받은 템플릿 이름
     */
    private String listUsers(String dept, String job, String keyword, Model model, String view) {
        // 레거시는 파라미터가 없으면 빈 문자열로 두고 그 값으로 조회한다. 검색
        // 전과 후를 구별하지 않으므로 창을 열자마자 전체 목록이 나온다.
        model.addAttribute("deptKeyfield", dept);
        model.addAttribute("jobKeyfield", job);
        model.addAttribute("keyword", keyword);
        model.addAttribute("departments", users.departments());
        model.addAttribute("jobs", users.jobs());
        model.addAttribute("users", users.search(new UserSearch(dept, job, keyword)));
        return view;
    }
}
