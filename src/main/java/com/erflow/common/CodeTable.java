package com.erflow.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;

/**
 * 코드와 이름의 짝을 담는 읽기 전용 표.
 *
 * <p>레거시는 이런 표를 자바 배열 두 개로 소스에 박아 뒀다(업종코드 156, 은행코드 73).
 * {@code code_tbl} 이 있는데도 비어 있어 코드가 유일한 출처였다.
 *
 * <p>담는 곳만 자원 파일로 옮겼다. 값도 순서도 조회 방식도 그대로이며, 자원 파일은
 * {@code migration/tools/extract_code_tables.py} 가 원본에서 기계로 뽑는다 —
 * 수백 건을 손으로 옮겨 적으면 오타를 검증할 방법이 없다.
 */
public final class CodeTable {

    private final Map<String, String> byCode;

    private CodeTable(Map<String, String> byCode) {
        this.byCode = byCode;
    }

    /**
     * 자원 파일에서 표를 읽는다.
     *
     * <p>{@code Properties} 를 쓰지 않고 줄을 직접 읽는 이유는 순서를 지키기 위해서다.
     * 원본 배열과 같은 순서라야 대조하기 쉽다.
     *
     * @param resource 클래스패스 자원 경로
     * @return 읽어들인 표
     * @throws IllegalStateException 자원을 읽을 수 없거나 비어 있을 때
     */
    public static CodeTable load(String resource) {
        Map<String, String> codes = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(resource).getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.strip();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator > 0) {
                    codes.put(trimmed.substring(0, separator), trimmed.substring(separator + 1));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(resource + " 을 읽을 수 없다", e);
        }
        if (codes.isEmpty()) {
            throw new IllegalStateException(resource + " 이 비어 있다");
        }
        return new CodeTable(Map.copyOf(codes));
    }

    /**
     * 코드의 이름.
     *
     * @param code 코드
     * @return 이름. 없으면 {@code null}
     */
    public String name(String code) {
        return code == null ? null : byCode.get(code);
    }

    /**
     * 이름에 검색어가 들어간 코드를 모은다.
     *
     * <p>레거시 {@code getFieldCodes(name)} 과 같다 — 부분 일치이며 대소문자를 가린다.
     *
     * @param keyword 검색어
     * @return 걸리는 코드 목록. 없으면 빈 목록
     */
    /**
     * 코드와 이름 중 하나라도 검색어를 품은 항목. 이름 순으로 돌려준다.
     *
     * <p>레거시 찾기 팝업이 그렇게 한다 — 목록을 이름으로 정렬해 두고
     * {@code key.contains(search) || value.contains(search)} 로 거른다.
     * 검색어가 빈 문자열이면 전부 보여준다. 그것도 레거시 그대로다.
     *
     * @param keyword 검색어. 빈 문자열이면 전체
     * @return (코드, 이름) 목록
     */
    public List<Entry> search(String keyword) {
        String needle = keyword == null ? "" : keyword;
        return byCode.entrySet().stream()
                .filter(entry -> needle.isEmpty()
                        || entry.getKey().contains(needle)
                        || entry.getValue().contains(needle))
                .map(entry -> new Entry(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(Entry::name))
                .toList();
    }

    /**
     * 코드표 한 줄.
     *
     * @param code 코드
     * @param name 이름
     */
    public record Entry(String code, String name) {
    }

    /**
     * 이름에 검색어가 든 항목의 코드.
     *
     * <p>목록 화면의 검색이 쓴다. 이름만 보고 코드만 돌려준다는 점에서
     * {@link #search(String)} 와 다르다 — 그쪽은 팝업이 쓰며 코드도 훑는다.
     *
     * @param keyword 검색어
     * @return 코드 목록. 검색어가 비면 빈 목록
     */
    public List<String> matching(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return byCode.entrySet().stream()
                .filter(entry -> entry.getValue().contains(keyword))
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * @return 담고 있는 코드 수
     */
    public int size() {
        return byCode.size();
    }
}
