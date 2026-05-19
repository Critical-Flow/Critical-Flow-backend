package com.criticalflow.domain.focus;

import com.criticalflow.domain.focus.dto.FocusEventCreateRequest;
import com.criticalflow.domain.focus.dto.FocusEventResponse;
import com.criticalflow.domain.focus.entity.FocusEvent;
import com.criticalflow.domain.focus.repository.FocusEventRepository;
import com.criticalflow.domain.focus.service.FocusEventService;
import com.criticalflow.domain.session.entity.StudySession;
import com.criticalflow.domain.session.repository.StudySessionRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FocusEventServiceTest {

    @Mock private FocusEventRepository focusEventRepository;
    @Mock private StudySessionRepository studySessionRepository;

    @InjectMocks
    private FocusEventService focusEventService;

    private static final Long USER_ID = 1L;
    private static final Long SESSION_ID = 100L;

    private StudySession sampleSession() {
        return StudySession.builder()
                .sessionId(SESSION_ID)
                .userId(USER_ID)
                .startTime(LocalDateTime.now().minusMinutes(30))
                .build();
    }

    private FocusEvent sampleEvent() {
        return FocusEvent.builder()
                .eventId(1L)
                .sessionId(SESSION_ID)
                .eventType(FocusEvent.EventType.GAZE_OUT)
                .detectedAt(LocalDateTime.now().minusMinutes(10))
                .durationSec(15)
                .alerted(false)
                .build();
    }

    @Nested
    @DisplayName("집중도 이벤트 수신")
    class RecordEvent {

        @Test
        @DisplayName("정상적으로 이벤트를 저장하고 응답을 반환한다")
        void 이벤트_저장_성공() {
            FocusEventCreateRequest request = new FocusEventCreateRequest(
                    FocusEvent.EventType.GAZE_OUT,
                    LocalDateTime.now().minusMinutes(10),
                    15
            );
            when(studySessionRepository.findBySessionIdAndUserId(SESSION_ID, USER_ID))
                    .thenReturn(Optional.of(sampleSession()));
            when(focusEventRepository.save(any(FocusEvent.class))).thenReturn(sampleEvent());

            FocusEventResponse response = focusEventService.recordEvent(USER_ID, SESSION_ID, request);

            assertThat(response.eventId()).isEqualTo(1L);
            assertThat(response.sessionId()).isEqualTo(SESSION_ID);
            assertThat(response.eventType()).isEqualTo(FocusEvent.EventType.GAZE_OUT);
            assertThat(response.durationSec()).isEqualTo(15);
        }

        @Test
        @DisplayName("저장된 이벤트의 alerted 필드는 false로 초기화된다")
        void alerted_초기값_false() {
            FocusEventCreateRequest request = new FocusEventCreateRequest(
                    FocusEvent.EventType.DROWSY,
                    LocalDateTime.now(),
                    30
            );
            ArgumentCaptor<FocusEvent> captor = ArgumentCaptor.forClass(FocusEvent.class);
            when(studySessionRepository.findBySessionIdAndUserId(SESSION_ID, USER_ID))
                    .thenReturn(Optional.of(sampleSession()));
            when(focusEventRepository.save(captor.capture())).thenReturn(sampleEvent());

            focusEventService.recordEvent(USER_ID, SESSION_ID, request);

            assertThat(captor.getValue().getAlerted()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 세션에 이벤트 저장 시 SESSION_NOT_FOUND 예외가 발생한다")
        void 존재하지_않는_세션_이벤트_저장_예외() {
            when(studySessionRepository.findBySessionIdAndUserId(SESSION_ID, USER_ID))
                    .thenReturn(Optional.empty());

            FocusEventCreateRequest request = new FocusEventCreateRequest(
                    FocusEvent.EventType.GAZE_OUT, LocalDateTime.now(), 10
            );

            assertThatThrownBy(() -> focusEventService.recordEvent(USER_ID, SESSION_ID, request))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(ErrorCode.SESSION_NOT_FOUND);

            verify(focusEventRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("집중도 이벤트 목록 조회")
    class GetEventsBySession {

        @Test
        @DisplayName("세션의 이벤트 목록을 detectedAt 오름차순으로 반환한다")
        void 이벤트_목록_조회_성공() {
            when(studySessionRepository.findBySessionIdAndUserId(SESSION_ID, USER_ID))
                    .thenReturn(Optional.of(sampleSession()));
            when(focusEventRepository.findBySessionIdOrderByDetectedAtAsc(SESSION_ID))
                    .thenReturn(List.of(sampleEvent()));

            List<FocusEventResponse> result = focusEventService.getEventsBySession(USER_ID, SESSION_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).sessionId()).isEqualTo(SESSION_ID);
            assertThat(result.get(0).eventType()).isEqualTo(FocusEvent.EventType.GAZE_OUT);
        }

        @Test
        @DisplayName("존재하지 않는 세션 조회 시 SESSION_NOT_FOUND 예외가 발생한다")
        void 존재하지_않는_세션_이벤트_조회_예외() {
            when(studySessionRepository.findBySessionIdAndUserId(SESSION_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> focusEventService.getEventsBySession(USER_ID, SESSION_ID))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(ErrorCode.SESSION_NOT_FOUND);

            verify(focusEventRepository, never()).findBySessionIdOrderByDetectedAtAsc(any());
        }
    }
}
