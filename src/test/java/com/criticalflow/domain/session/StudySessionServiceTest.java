package com.criticalflow.domain.session;

import com.criticalflow.domain.focus.repository.FocusEventRepository;
import com.criticalflow.domain.session.dto.SessionResponse;
import com.criticalflow.domain.session.entity.StudySession;
import com.criticalflow.domain.session.repository.StudySessionRepository;
import com.criticalflow.domain.session.service.StudySessionService;
import com.criticalflow.global.exception.DomainException;
import com.criticalflow.global.exception.ErrorCode;
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
    @Mock private FocusEventRepository focusEventRepository;

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
        @DisplayName("집중이탈 이벤트가 없을 때 totalFocusMinutes는 totalStudyMinutes와 같다")
        void 집중이탈_없을때_포커스시간_같음() {
            when(studySessionRepository.findBySessionIdAndUserId(SESSION_ID, USER_ID))
                    .thenReturn(Optional.of(activeSession()));
            when(focusEventRepository.sumDurationSecBySessionId(SESSION_ID)).thenReturn(0);

            SessionResponse response = studySessionService.endSession(USER_ID, SESSION_ID);

            assertThat(response.endTime()).isNotNull();
            assertThat(response.totalStudyMinutes()).isGreaterThanOrEqualTo(0);
            assertThat(response.totalFocusMinutes()).isEqualTo(response.totalStudyMinutes());
        }

        @Test
        @DisplayName("집중이탈 이벤트가 있을 때 totalFocusMinutes는 차감된다")
        void 집중이탈_있을때_포커스시간_차감() {
            when(studySessionRepository.findBySessionIdAndUserId(SESSION_ID, USER_ID))
                    .thenReturn(Optional.of(activeSession()));
            // 600초(10분) 집중이탈
            when(focusEventRepository.sumDurationSecBySessionId(SESSION_ID)).thenReturn(600);

            SessionResponse response = studySessionService.endSession(USER_ID, SESSION_ID);

            assertThat(response.totalFocusMinutes())
                    .isEqualTo(response.totalStudyMinutes() - 10);
        }

        @Test
        @DisplayName("집중이탈 시간이 학습 시간을 초과하면 totalFocusMinutes는 0이다")
        void 집중이탈_초과시_포커스시간_0() {
            when(studySessionRepository.findBySessionIdAndUserId(SESSION_ID, USER_ID))
                    .thenReturn(Optional.of(activeSession()));
            // 99999초 집중이탈 (학습 시간 초과)
            when(focusEventRepository.sumDurationSecBySessionId(SESSION_ID)).thenReturn(99999);

            SessionResponse response = studySessionService.endSession(USER_ID, SESSION_ID);

            assertThat(response.totalFocusMinutes()).isEqualTo(0);
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
}
