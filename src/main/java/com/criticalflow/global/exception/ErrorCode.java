package com.criticalflow.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Note
    NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTE_NOT_FOUND", "노트를 찾을 수 없습니다."),
    NOTE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "NOTE_ACCESS_DENIED", "해당 노트에 대한 접근 권한이 없습니다."),
    NOTE_TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "NOTE_TITLE_REQUIRED", "노트 제목은 필수입니다."),
    NOTE_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "NOTE_CONTENT_REQUIRED", "노트 내용은 필수입니다."),

    // Conversation
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", "대화를 찾을 수 없습니다."),
    CONVERSATION_NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "CONVERSATION_NOTE_NOT_FOUND", "대화에 연결된 노트를 찾을 수 없습니다."),
    MESSAGE_EMPTY(HttpStatus.BAD_REQUEST, "MESSAGE_EMPTY", "메시지 내용은 필수입니다."),

    // AI
    AI_RESPONSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI_RESPONSE_FAILED", "AI 응답 생성에 실패했습니다."),

    // Auth
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 RefreshToken입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_REFRESH_TOKEN", "만료된 RefreshToken입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
