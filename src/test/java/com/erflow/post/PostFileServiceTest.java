package com.erflow.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

/**
 * 첨부 삭제가 디스크 파일까지 지우는지 확인한다 (D-132, O-009 해소).
 *
 * <p>파일 삭제는 <b>커밋 뒤에</b> 일어나므로 {@code @Transactional} 시험으로는 볼 수
 * 없다 — 롤백되면 커밋이 없어 파일이 남는다. 그래서 실제로 커밋하고 뒷정리는 직접
 * 한다. 글번호는 실제 데이터와 겹치지 않게 크게 잡는다.
 */
@SpringBootTest(properties = "server.port=0")
@ActiveProfiles("local")
class PostFileServiceTest {

    /** 실제 게시글과 겹치지 않는 글번호. */
    private static final int TEST_POST_ID = 987_654_321;

    @Autowired
    private PostFileService postFileService;

    @Autowired
    private JdbcTemplate jdbc;

    @Value("${erflow.upload.dir}")
    private Path uploadDir;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    @AfterEach
    void cleanUp() throws Exception {
        // 시험이 중간에 죽어도 행과 파일이 남지 않게 한다.
        for (PostAttachment attachment : postFileService.list(TEST_POST_ID)) {
            Files.deleteIfExists(uploadDir.resolve(attachment.name()));
        }
        jdbc.update("DELETE FROM post_file_tbl WHERE post_tbl_id = ?", TEST_POST_ID);
    }

    @Test
    @DisplayName("첨부를 떼면 DB 행과 디스크 파일이 같이 사라진다")
    void detachAllRemovesFilesFromDisk() {
        List<PostAttachment> saved = postFileService.attach(TEST_POST_ID, List.of(
                new MockMultipartFile("files", "보고서.txt", "text/plain",
                        "내용".getBytes(StandardCharsets.UTF_8))));
        assertThat(saved).hasSize(1);
        Path stored = uploadDir.resolve(saved.get(0).name());
        assertThat(stored).as("붙인 직후에는 파일이 있어야 한다").exists();

        int deleted = postFileService.detachAll(TEST_POST_ID);

        assertThat(deleted).isEqualTo(1);
        assertThat(postFileService.list(TEST_POST_ID)).isEmpty();
        assertThat(stored).as("커밋 뒤에는 파일도 없어야 한다").doesNotExist();
    }

    @Test
    @DisplayName("첨부가 없는 글은 아무 일도 없이 0 을 돌려준다")
    void detachAllOnEmptyPostIsNoOp() {
        assertThat(postFileService.detachAll(TEST_POST_ID)).isZero();
    }
}
