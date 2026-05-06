package com.criticalflow.global.ai.rag;

import com.criticalflow.domain.focus.entity.FocusEvent;
import com.criticalflow.domain.focus.repository.FocusEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FocusEventFormatter {

    private final FocusEventRepository focusEventRepository;

    @Value("${rag.focus-lookback-minutes:15}")
    private int lookbackMinutes;

    /**
     * 세션의 최근 N분 집중 이탈 이벤트를 System Prompt의 {focus_events} 변수 형식으로 반환한다.
     * 형식: [EVENT_TYPE | duration_sec | alerted=true/false]
     */
    public String format(Long sessionId) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(lookbackMinutes);
        List<FocusEvent> events = focusEventRepository
                .findBySessionIdAndDetectedAtAfterOrderByDetectedAtAsc(sessionId, since);

        if (events.isEmpty()) {
            return "(No focus events in the last " + lookbackMinutes + " minutes.)";
        }

        return events.stream()
                .map(e -> String.format("[%s | %ds | alerted=%s]",
                        e.getEventType(), e.getDurationSec(), e.getAlerted()))
                .collect(Collectors.joining("\n"));
    }
}
