package com.erflow.auth;

/**
 * 저장 형식 승격 대상 행.
 *
 * @param id 사번
 * @param password 저장된 비밀번호 값
 */
public record StoredPassword(String id, String password) {
}
