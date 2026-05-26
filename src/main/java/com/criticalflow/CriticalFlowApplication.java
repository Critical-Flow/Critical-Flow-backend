package com.criticalflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CriticalFlowApplication {

    // CI/CD 배포 테스트
    public static void main(String[] args) {
        SpringApplication.run(CriticalFlowApplication.class, args);
    }
}