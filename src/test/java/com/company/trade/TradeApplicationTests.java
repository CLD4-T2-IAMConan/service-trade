package com.company.trade;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "spring.main.allow-bean-definition-overriding=true"
})
@ActiveProfiles("test")
class TradeApplicationTests {

    @Test
    void contextLoads() {
        // Spring Boot 컨텍스트가 정상적으로 로드되는지 확인
        // 주의: 이 테스트는 H2 인메모리 DB를 사용합니다 (application-test.yml)
    }
}
