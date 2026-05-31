package com.criticalflow.global.vision;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PythonVisionClient {

    private final RestTemplate restTemplate;

    @Value("${python.vision.url}")
    private String pythonVisionUrl;

    public void startWebcam(Long sessionId, Long userId) {
        try {
            String url = pythonVisionUrl + "/vision/start";
            Map<String, Object> body = Map.of(
                    "sessionId", sessionId,
                    "userId", userId
            );
            restTemplate.postForEntity(url, body, Void.class);
            log.info("Python 웹캠 시작 요청 완료 - sessionId: {}", sessionId);
        } catch (Exception e) {
            log.warn("Python 웹캠 시작 요청 실패 - sessionId: {}, error: {}", sessionId, e.getMessage());
        }
    }

    public void stopWebcam(Long sessionId) {
        try {
            String url = pythonVisionUrl + "/vision/stop";
            Map<String, Object> body = Map.of("sessionId", sessionId);
            restTemplate.postForEntity(url, body, Void.class);
            log.info("Python 웹캠 종료 요청 완료 - sessionId: {}", sessionId);
        } catch (Exception e) {
            log.warn("Python 웹캠 종료 요청 실패 - sessionId: {}, error: {}", sessionId, e.getMessage());
        }
    }
}
