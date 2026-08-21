package com.erflow.company;

/**
 * 찾기 팝업이 뿌리는 협력업체 한 줄.
 *
 * <p>목록 화면이 쓰는 {@link Company} 와 달리 번호와 이름만 있다. 팝업이 그 둘만
 * 보여주고 그 둘만 부모 창에 돌려준다.
 *
 * @param id 번호
 * @param name 이름
 */
public record CompanyRow(int id, String name) {
}
