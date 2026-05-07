package com.criticalflow.domain.ai.entity;

public enum QuestionType {
    TYPE_A,  // 개념 정의 확인
    TYPE_B,  // 설계 의도 탐침
    TYPE_C,  // 과거 학습 연계 (Spaced Recall)
    TYPE_D,  // 심화 탐구 (엣지케이스)
    TYPE_E,  // 코드 동작 탐침
    TYPE_F   // 실제 적용 맥락 탐침 (텍스트 전용)
}
