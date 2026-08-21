package com.erflow.company;

/**
 * 협력업체 한 건. {@code company_tbl} 에 대응한다.
 *
 * @param id 번호
 * @param name 이름
 * @param postalCode 우편번호
 * @param address1 도로명 주소
 * @param address2 상세주소
 * @param phone 전화 번호
 * @param businessCode 사업자 번호
 * @param field 업종코드. 이름은 {@link FieldCodes} 가 푼다
 * @param bankCode 은행 코드
 * @param bankAccount 은행 계좌번호
 * @param subcontract 구분. 0 이면 영업(하청), 1 이면 구매(상청)
 */
public record Company(
        int id,
        String name,
        String postalCode,
        String address1,
        String address2,
        String phone,
        String businessCode,
        String field,
        String bankCode,
        String bankAccount,
        int subcontract) {

    /** 영업 협력업체. 레거시 {@code flag == 0}. */
    public static final int OUTBOUND = 0;

    /** 구매 협력업체. 레거시 {@code flag == 1}. */
    public static final int INBOUND = 1;

    /**
     * 목록에 보이는 주소.
     *
     * <p>레거시는 {@code address1 + " " + address2} 로 이어 붙여 한 칸에 넣었다.
     *
     * @return 도로명과 상세주소를 이은 문자열
     */
    public String fullAddress() {
        return (address1 == null ? "" : address1) + " " + (address2 == null ? "" : address2);
    }
}
