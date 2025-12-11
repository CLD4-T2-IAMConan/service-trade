package com.company.trade.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        // 💡 기본적인 RestTemplate을 Bean으로 등록합니다.
        // 필요에 따라 타임아웃 설정 등을 추가할 수 있습니다.
        return new RestTemplate();
    }
}