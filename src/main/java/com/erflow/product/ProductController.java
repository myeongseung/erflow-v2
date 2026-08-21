package com.erflow.product;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 제품 관리 화면.
 *
 * <pre>
 * ingredientProduct.jsp   GET  /product/ingredient-product
 * processedProduct.jsp    GET  /product/processed-product
 * productedProduct.jsp    GET  /product/producted-product
 * productRegister.jsp     GET  /product/register?flag=
 * productRegisterProc     POST /product/register-proc
 * productUpdate.jsp       GET  /product/update?flag=&amp;id=
 * productUpdateProc       POST /product/update-proc
 * productDeleteProc       POST /product/delete-proc
 * </pre>
 *
 * <p>목록 셋은 같은 표를 {@code type} 으로 나눠 본다. <b>권한은 셋이 다르다</b> —
 * {@code screen} 표에 세 경로가 각각 다른 프로그램으로 걸려 있다. 그래서 화면도 셋으로
 * 나뉘어 있고 여기서도 나눠 둔다.
 *
 * <p>등록·수정·삭제는 셋이 함께 쓴다. 레거시도 파일 하나였다.
 */
@Controller
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;

    /**
     * @param productService 제품 업무
     */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 원재료 관리.
     *
     * @param keyfield 검색할 칸
     * @param keyword 검색어
     * @param nowPage 현재 페이지
     * @param model 뷰 모델
     * @return 원재료 목록 템플릿
     */
    @GetMapping("/ingredient-product")
    public String ingredient(
            @RequestParam(required = false) String keyfield,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int nowPage,
            Model model) {
        return list(ProductType.INGREDIENT, keyfield, keyword, nowPage, model,
                "product/ingredient-product");
    }

    /**
     * 가공품 관리.
     *
     * @param keyfield 검색할 칸
     * @param keyword 검색어
     * @param nowPage 현재 페이지
     * @param model 뷰 모델
     * @return 가공품 목록 템플릿
     */
    @GetMapping("/processed-product")
    public String processed(
            @RequestParam(required = false) String keyfield,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int nowPage,
            Model model) {
        return list(ProductType.PROCESSED, keyfield, keyword, nowPage, model,
                "product/processed-product");
    }

    /**
     * 완제품(출고 제품) 관리.
     *
     * @param keyfield 검색할 칸
     * @param keyword 검색어
     * @param nowPage 현재 페이지
     * @param model 뷰 모델
     * @return 완제품 목록 템플릿
     */
    @GetMapping("/producted-product")
    public String producted(
            @RequestParam(required = false) String keyfield,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int nowPage,
            Model model) {
        return list(ProductType.PRODUCTED, keyfield, keyword, nowPage, model,
                "product/producted-product");
    }

    /**
     * 제품 등록 화면.
     *
     * @param flag 분류({@code ingredient} / {@code processed} / {@code producted})
     * @param model 뷰 모델
     * @return 등록 템플릿. 분류가 없으면 잘못된 접근 화면
     */
    @GetMapping("/register")
    public String registerForm(@RequestParam(required = false) String flag, Model model) {
        ProductType type = ProductType.of(flag);
        if (type == null) {
            return "redirect:/access-error";
        }
        model.addAttribute("type", type);
        return "product/register";
    }

    /**
     * 제품 등록 처리.
     *
     * <p><b>레거시에는 이 처리에 아무 검사도 없다.</b> 로그인 확인 코드가 주석으로 막혀
     * 있어 누구나 제품을 만들 수 있었다(D-070). 여기서는 로그인해야 들어온다.
     *
     * @param productId 제품 ID
     * @param productName 이름
     * @param count 수량
     * @param type 분류 코드
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/register-proc")
    public String register(
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String count,
            @RequestParam(required = false) Integer type,
            Model model) {

        ProductType kind = typeOf(type);
        if (kind == null) {
            return "redirect:/access-error";
        }
        boolean given = productId != null && productName != null && count != null;
        boolean created = given && productService.create(
                productId.trim(), productName.trim(), number(count), kind);
        return result(model, created ? "등록에 성공했습니다." : "등록에 실패했습니다.",
                "/product/" + kind.flag() + "-product");
    }

    /**
     * 제품 수정 화면.
     *
     * @param flag 분류
     * @param id 제품 ID
     * @param model 뷰 모델
     * @return 수정 템플릿. 분류나 제품이 없으면 잘못된 접근 화면
     */
    @GetMapping("/update")
    public String updateForm(
            @RequestParam(required = false) String flag,
            @RequestParam(required = false) String id,
            Model model) {

        ProductType type = ProductType.of(flag);
        ProductListRow product = id == null ? null : productService.get(id);
        if (type == null || product == null) {
            return "redirect:/access-error";
        }
        model.addAttribute("type", type);
        model.addAttribute("product", product);
        return "product/update";
    }

    /**
     * 제품 수정 처리.
     *
     * <p>레거시는 고친 뒤 {@code product.jsp} 로 보낸다. <b>그런 화면은 없다</b> — 수정에
     * 성공하든 실패하든 없는 주소로 간다(D-071). 분류에 맞는 목록으로 보낸다.
     *
     * @param productId 제품 ID
     * @param productName 이름
     * @param count 수량
     * @param type 분류 코드
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/update-proc")
    public String update(
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String count,
            @RequestParam(required = false) Integer type,
            Model model) {

        ProductType kind = typeOf(type);
        if (kind == null) {
            return "redirect:/access-error";
        }
        boolean given = productId != null && productName != null && count != null;
        boolean updated = given && productService.update(
                productId.trim(), productName.trim(), number(count), kind);
        return result(model, updated ? "수정에 성공했습니다." : "수정에 실패했습니다.",
                "/product/" + kind.flag() + "-product");
    }

    /**
     * 선택한 제품 삭제.
     *
     * @param productId 지울 제품 ID 들
     * @param type 돌아갈 목록의 분류
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/delete-proc")
    public String delete(
            @RequestParam(required = false) List<String> productId,
            @RequestParam(required = false) String type,
            Model model) {

        ProductType kind = ProductType.of(type);
        if (kind == null) {
            return "redirect:/access-error";
        }
        String next = "/product/" + kind.flag() + "-product";
        if (productId == null) {
            return result(model, "삭제에 실패했습니다.", next);
        }
        return result(model, productService.delete(productId)
                ? "삭제에 성공했습니다." : "삭제에 실패했습니다.", next);
    }

    private String list(
            ProductType type, String keyfield, String keyword, int nowPage,
            Model model, String view) {

        ProductSearch search = new ProductSearch(
                keyfield == null ? "" : keyfield, keyword == null ? "" : keyword);
        ProductService.ProductPage page = productService.list(type, search, nowPage);

        model.addAttribute("products", page.rows());
        model.addAttribute("page", page.pagination());
        model.addAttribute("keyfield", search.keyfield());
        model.addAttribute("keyword", search.keyword());
        model.addAttribute("type", type);
        return view;
    }

    private static ProductType typeOf(Integer code) {
        if (code == null) {
            return null;
        }
        for (ProductType type : ProductType.values()) {
            if (type.code() == code) {
                return type;
            }
        }
        return null;
    }

    /**
     * 수량을 숫자로 읽는다.
     *
     * <p>레거시는 {@code Integer.parseInt} 를 그대로 불러, 숫자가 아니면 그 자리에서
     * 죽는다. 그 죽음은 옮기지 않고 0 으로 본다 — 저장은 되고 값이 0 이 된다.
     */
    private static int number(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String result(Model model, String message, String nextPage) {
        model.addAttribute("message", message);
        model.addAttribute("nextPage", nextPage);
        return "product/result";
    }
}
