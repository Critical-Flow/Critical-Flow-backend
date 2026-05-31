package com.criticalflow.domain.session;

import com.criticalflow.domain.session.dto.SessionResponse;
import com.criticalflow.domain.session.dto.VisionResultRequest;
import com.criticalflow.domain.session.entity.StudySession;
import com.criticalflow.domain.session.repository.StudySessionRepository;
import com.criticalflow.domain.session.service.StudySessionService;
import com.criticalflow.global.exception.DomainException;
import com.criticalflow.global.exception.ErrorCode;
import com.criticalflow.global.vision.PythonVisionClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudySessionServiceTest {

    @Mock private StudySessionRepository studySessionRepository;
    @Mock private PythonVisionClient pythonVisionClient;

    @InjectMocks
    private StudySessionService studySessionService;

    private static final Long USER_ID = 1L;
    private static final Long SESSION_ID = 100L;

    private StudySession activeSession() {
        return StudySession.builder()
                .sessionId(SESSION_ID)
                .userId(USER_ID)
                .startTime(LocalDateTime.now().minusMinutes(60))
                .build();
    }

    private StudySession endedSession() {
        return StudySession.builder()
                .sessionId(SESSION_ID)
                .userId(USER_ID)
                .startTime(LocalDateTime.now().minusMinutes(60))
                .endTime(LocalDateTime.now().minusMinutes(10))
                .totalStudyMinutes(50)
                .totalFocusMinutes(45)
                .build();
    }

    @Nested
    @DisplayName("세션 시작")
    class StartSession {

        @Test
        @DisplayName("세션을 생성하고 startTime이 설정된 응답을 반환한다")
        void 세션_시작_성공() {
            ArgumentCaptor<StudySession> captor = ArgumentCaptor.forClass(StudySession.class);
            StudySession saved = activeSession();
            when(studySessionRepository.save(any(StudySession.class))).thenReturn(saved);

            SessionResponse response = studySessionService.startSession(USER_ID);

            verify(studySessionRepository).save(captor.capture());
            StudySession captured = captor.getValue();
            assertThat(captured.getUserId()).isEqualTo(USER_ID);
            assertThat(captured.getStartTime()).isNotNull();
            assertThat(captured.getEndTime()).isNull();
            assertThat(response.sessionId()).isEqualTo(SESSION_ID);
            assertThat(response.endTime()).isNull();
        }
    }

    @Nested
    @DisplayName("세션 종료")
    class EndSession {

        @Test
        @DisplayName("세션 종료 시 endTime이 기록되고 Python 종료 신호가 전송된다")
        void 세션_종료_성공() {
            when(studySessionRepository.findBySessionIdAndUserId(SESSION_ID, USER_ID))
                    .thenReturn(Optional.of(activeSession()));

            SessionResponse response = studySessionService.endSession(USER_ID, SESSION_ID);

            assertThat(response.endTime()).isNotNull();
            verify(pythonVisionClient).stopWebcam(SESSION_ID);
        }

        @Test
        @DisplayName("존재하지 않는 세션 종료 시 SESSION_NOT_FOUND 예외가 발생한다")
        void 존재하지_않는_세션_종료_예외() {
            when(studySessionRepository.findBySessionIdAndUserId(SESSION_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> studySessionService.endSession(USER_ID, SESSION_ID))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
        }

        @Test
        @DisplayName("이미 종료된 세션 재종료 시 SESSION_ALREADY_ENDED 예외가 발생한다")
        void 이미_종료된_세션_재종료_예외() {
            when(studySessionRepository.findBySessionIdAndUserId(SESSION_ID, USER_ID))
                    .thenReturn(Optional.of(endedSession()));

            assertThatThrownBy(() -> studySessionService.endSession(USER_ID, SESSION_ID))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(ErrorCode.SESSION_ALREADY_ENDED);
        }
    }

    @Nested
    @DisplayName("비전 결과 적용")
    class ApplyVisionResult {

        @Test
        @DisplayName("Python 콜백 결과를 세션에 정상 반영한다")
        void 비전_결과_적용_성공() {
            when(studySessionRepository.findById(SESSION_ID))
                    .thenReturn(Optional.of(activeSession()));

            VisionResultRequest request = new VisionResultRequest(1L, 3600, 3000, 300, 60, 2, 1);
            SessionResponse response = studySessionService.applyVisionResult(SESSION_ID, request);

            assertThat(response.totalStudyMinutes()).isEqualTo(60);  // 3600 / 60
            assertThat(response.totalFocusMinutes()).isEqualTo(50);  // 3000 / 60
            assertThat(response.drowsySeconds()).isEqualTo(300);
            assertThat(response.absentSeconds()).isEqualTo(60);
            assertThat(response.drowsyCount()).isEqualTo(2);
            assertThat(response.absentCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("존재하지 않는 세션에 비전 결과 적용 시 SESSION_NOT_FOUND 예외가 발생한다")
        void 존재하지_않는_세션_비전_결과_예외() {
            when(studySessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

            VisionResultRequest request = new VisionResultRequest(1L, 3600, 3000, 300, 60, 2, 1);

            assertThatThrownBy(() -> studySessionService.applyVisionResult(SESSION_ID, request))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
        }
    }
}
