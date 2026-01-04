package com.company.trade;

import com.company.sns.config.SnsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@ComponentScan(basePackages = {"com.company.trade", "com.company.sns"})
@Import(SnsConfig.class)
public class TradeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeApplication.class, args);
    }

    @Bean // 🚨 이 어노테이션이 필수
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
