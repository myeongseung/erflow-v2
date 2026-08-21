package com.erflow;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 스캐폴딩 스모크 테스트.
 *
 * <p>DB 연결을 요구하지 않는다. 커넥션은 지연 획득되므로 컨텍스트 로딩만으로는
 * MariaDB 가 필요 없다. 실제 DB 를 상대로 하는 검증은 도메인 이관과 함께 붙인다.
 */
@SpringBootTest(properties = "server.port=0")
class ErflowApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private org.apache.ibatis.session.SqlSessionFactory sqlSessionFactory;

    @Test
    @DisplayName("컨텍스트가 로드된다")
    void contextLoads() {
        assertThat(dataSource).isNotNull();
    }

    @Test
    @DisplayName("MyBatis 가 기동되고 underscore -> camelCase 매핑이 켜져 있다")
    void mybatisIsConfigured() {
        assertThat(sqlSessionFactory).isNotNull();
        assertThat(sqlSessionFactory.getConfiguration().isMapUnderscoreToCamelCase())
                .as("레거시 컬럼명이 snake_case 이므로 매핑이 켜져 있어야 한다")
                .isTrue();
        assertThat(SqlSessionFactoryBean.class).isNotNull();
    }
}
