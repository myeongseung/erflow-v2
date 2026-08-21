package com.erflow.index;

import com.erflow.auth.ErflowUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 메인 화면.
 *
 * <pre>
 * index.jsp   GET /index
 *             GET /          (로그인 뒤 여기로 온다)
 * </pre>
 *
 * <p>위젯 넷과 달력 하나가 한 화면에 있다. 위젯은 각 도메인의 목록을 15건씩 미리
 * 보여 주고, 오른쪽 위 아이콘이 그 도메인의 목록 화면으로 보낸다.
 *
 * <p>레거시는 <b>프로그램 권한을 묻지 않는다.</b> 로그인만 확인한다 — 그래서 공지사항
 * 위젯은 게시판을 볼 권한이 없는 사람에게도 보인다(D-084).
 */
@Controller
public class IndexController {

    private final IndexService indexService;

    /**
     * @param indexService 메인 화면 업무
     */
    public IndexController(IndexService indexService) {
        this.indexService = indexService;
    }

    /**
     * 메인 화면.
     *
     * <p>레거시에는 {@code /} 매핑이 없었다. 톰캣이 {@code index.jsp} 를 환영 파일로
     * 열어 줬을 뿐이다. 같은 자리를 매핑으로 만든다.
     *
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 메인 템플릿
     */
    @GetMapping({"/", "/index"})
    public String index(@AuthenticationPrincipal ErflowUserDetails user, Model model) {
        model.addAttribute("notices",
                indexService.posts(IndexService.NOTICE_BOARD, false));
        // 자유게시판만 작성자 이름까지 자른다.
        model.addAttribute("freePosts",
                indexService.posts(IndexService.FREE_BOARD, true));
        model.addAttribute("proposals", indexService.proposals(user.id()));
        model.addAttribute("messages", indexService.messages(user.id()));
        model.addAttribute("userId", user.id());
        return "index";
    }
}
