package com.criticalflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CriticalFlowApplication {

    // CI/CD 파이프라인 동작 확인용 주석
    public static void main(String[] args) {
        SpringApplication.run(CriticalFlowApplication.class, args);
    }
}