package com.erflow.company;

/**
 * 등록·수정 화면에서 넘어온 값.
 *
 * <p>이름은 레거시 파라미터를 그대로 쓴다({@code companyName}, {@code businessNumber},
 * {@code workCode}…). 화면 이름과 컬럼 이름이 어긋나 있지만 고치지 않는다 — 이름을
 * 손대면 레거시 화면과 대조할 수 없게 된다.
 *
 * <p>필드가 {@code String} 인 이유도 같다. 레거시는 모든 값을 문자열로 받아 비어 있으면
 * {@code null} 로 바꿔 넣었고, 하나라도 없으면(파라미터 자체가 안 온 경우) 저장하지
 * 않았다. "빈 문자열"과 "안 온 것"을 구분해야 그 동작을 재현할 수 있다.
 */
public class CompanyForm {

    private String id;
    private String companyName;
    private String status;
    private String postalCode;
    private String address1;
    private String address2;
    private String companyPhone;
    private String businessNumber;
    private String workCode;
    private String bankCode;
    private String bankAccount;

    /**
     * 저장에 필요한 값이 모두 왔는지 확인한다.
     *
     * <p>레거시는 열 개 키를 돌며 {@code request.getParameter(key) == null} 이면
     * 그 자리에서 중단했다. 빈 문자열은 통과시켰다.
     *
     * @return 모두 왔으면 {@code true}
     */
    public boolean complete() {
        return companyName != null && status != null && postalCode != null
                && address1 != null && address2 != null && companyPhone != null
                && businessNumber != null && workCode != null
                && bankCode != null && bankAccount != null;
    }

    /**
     * 저장할 형태로 바꾼다.
     *
     * @param companyId 번호. 등록이면 0
     * @return 협력업체
     */
    public Company toCompany(int companyId) {
        return new Company(
                companyId,
                blankToNull(companyName),
                blankToNull(postalCode),
                blankToNull(address1),
                blankToNull(address2),
                blankToNull(companyPhone),
                blankToNull(businessNumber),
                blankToNull(workCode),
                blankToNull(bankCode),
                blankToNull(bankAccount),
                Integer.parseInt(status.trim()));
    }

    /**
     * 수정 대상 번호.
     *
     * @return 번호. 등록이거나 숫자가 아니면 {@code null}
     */
    public Integer id() {
        try {
            return Integer.valueOf(id.trim());
        } catch (NumberFormatException | NullPointerException expected) {
            return null;
        }
    }

    /**
     * 구매/영업 구분.
     *
     * @return {@code status} 원본 값
     */
    public String status() {
        return status;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** @param value 번호 */
    public void setId(String value) {
        this.id = value;
    }

    /** @param value 회사명 */
    public void setCompanyName(String value) {
        this.companyName = value;
    }

    /** @param value 구매/영업 구분 */
    public void setStatus(String value) {
        this.status = value;
    }

    /** @param value 우편번호 */
    public void setPostalCode(String value) {
        this.postalCode = value;
    }

    /** @param value 도로명 주소 */
    public void setAddress1(String value) {
        this.address1 = value;
    }

    /** @param value 상세주소 */
    public void setAddress2(String value) {
        this.address2 = value;
    }

    /** @param value 업체 전화번호 */
    public void setCompanyPhone(String value) {
        this.companyPhone = value;
    }

    /** @param value 사업자 등록번호 */
    public void setBusinessNumber(String value) {
        this.businessNumber = value;
    }

    /** @param value 업종코드 */
    public void setWorkCode(String value) {
        this.workCode = value;
    }

    /** @param value 은행 코드 */
    public void setBankCode(String value) {
        this.bankCode = value;
    }

    /** @param value 은행 계좌번호 */
    public void setBankAccount(String value) {
        this.bankAccount = value;
    }
}
