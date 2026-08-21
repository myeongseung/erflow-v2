package com.erflow.post;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 게시글 첨부 업무.
 *
 * <h2>저장 위치가 레거시와 다르다</h2>
 *
 * <p>레거시는 {@code getServletContext().getRealPath("/upload")} 로 배포된 WAR 안의
 * 폴더에 넣었다. 실행 가능한 jar 에는 그런 경로가 없고, 있다 해도 재배포하면
 * 첨부가 사라진다. 바깥 폴더를 설정으로 받는다 — {@code erflow.upload.dir}.
 *
 * <p>화면에 보이는 것은 달라지지 않는다. 근거는
 * {@code migration/design/00-decisions.md} 의 D-027 에 남겼다.
 *
 * <h2>저장 이름은 UUID 다</h2>
 *
 * <p>레거시가 확장자 없는 UUID 로 저장하고 원본 이름·확장자를 DB 에 따로 넣는다.
 * 그대로 따른다 — 이름이 겹치지 않고, 올린 이름이 경로로 해석될 여지도 없다.
 */
@Service
public class PostFileService {

    private final PostFileMapper postFileMapper;
    private final Path uploadDir;

    /**
     * @param postFileMapper 첨부 매퍼
     * @param uploadDir 저장 폴더
     */
    public PostFileService(
            PostFileMapper postFileMapper,
            @Value("${erflow.upload.dir}") Path uploadDir) {
        this.postFileMapper = postFileMapper;
        this.uploadDir = uploadDir;
    }

    /**
     * 게시글의 첨부 목록.
     *
     * @param postId 글번호
     * @return 첨부 목록
     */
    @Transactional(readOnly = true)
    public List<PostAttachment> list(int postId) {
        return postFileMapper.findByPost(postId);
    }

    /**
     * 저장 이름으로 첨부를 찾는다.
     *
     * @param name 저장 파일명
     * @return 첨부. 등록되지 않은 이름이면 {@code null}
     */
    @Transactional(readOnly = true)
    public PostAttachment find(String name) {
        return name == null || name.isBlank() ? null : postFileMapper.findByStoredName(name);
    }

    /**
     * 첨부의 실제 경로.
     *
     * <p>이름은 DB 에서 온 UUID 다. 요청에 실린 문자열을 그대로 경로로 만들지
     * 않는다 — {@link #find} 를 거친 것만 여기 들어온다.
     *
     * @param file 첨부
     * @return 저장 경로
     */
    public Path locate(PostAttachment file) {
        return uploadDir.resolve(file.name());
    }

    /**
     * 올라온 파일을 저장하고 게시글에 붙인다.
     *
     * <p>빈 파일은 건너뛴다. 레거시도 파일명이 비면 {@code continue} 했다.
     *
     * @param postId 글번호
     * @param uploads 올라온 파일들
     * @return 저장된 첨부 목록
     */
    @Transactional
    public List<PostAttachment> attach(int postId, List<MultipartFile> uploads) {
        List<PostAttachment> saved = new ArrayList<>();
        if (uploads == null) {
            return saved;
        }
        try {
            Files.createDirectories(uploadDir);
            for (MultipartFile upload : uploads) {
                if (upload == null || upload.isEmpty()) {
                    continue;
                }
                String submitted = upload.getOriginalFilename();
                if (submitted == null || submitted.isBlank()) {
                    continue;
                }
                String storedName = UUID.randomUUID().toString();
                upload.transferTo(uploadDir.resolve(storedName));

                PostAttachment attachment = new PostAttachment(
                        0, postId, baseName(submitted), storedName,
                        extension(submitted), upload.getSize());
                postFileMapper.insert(attachment);
                saved.add(attachment);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("첨부 저장 실패", exception);
        }
        return saved;
    }

    /**
     * 게시글의 첨부를 전부 지운다.
     *
     * <p>레거시는 DB 행만 지우고 디스크 파일을 남긴다. 여기서도 남긴다 — 지우는
     * 쪽이 옳아 보이지만, 옮기면서 없던 삭제를 더하면 되돌릴 수 없는 차이가 된다.
     * 별도 안건으로 둔다(O-009).
     *
     * @param postId 글번호
     * @return 지워진 건수
     */
    @Transactional
    public int detachAll(int postId) {
        return postFileMapper.deleteByPost(postId);
    }

    /**
     * 확장자를 뺀 파일 이름.
     *
     * @param filename 올라온 이름
     * @return 레거시 {@code WebHelper.extractFileName} 과 같은 결과
     */
    static String baseName(String filename) {
        String bare = stripPath(filename);
        int dot = bare.lastIndexOf('.');
        return dot == -1 ? bare : bare.substring(0, dot);
    }

    /**
     * 확장자.
     *
     * @param filename 올라온 이름
     * @return 레거시 {@code WebHelper.extractFileExtension} 과 같은 결과
     */
    static String extension(String filename) {
        String bare = stripPath(filename);
        int dot = bare.lastIndexOf('.');
        return dot == -1 ? "" : bare.substring(dot + 1);
    }

    /**
     * 경로를 떼고 이름만 남긴다.
     *
     * <p>브라우저가 전체 경로를 보내는 경우가 있다. 레거시도
     * {@code new File(item.getName()).getName()} 으로 떼어냈다.
     *
     * @param filename 올라온 이름
     * @return 경로가 제거된 이름
     */
    private static String stripPath(String filename) {
        int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        return slash == -1 ? filename : filename.substring(slash + 1);
    }
}
