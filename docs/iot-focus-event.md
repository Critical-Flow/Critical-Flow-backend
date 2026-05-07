# IoT 집중 이탈 이벤트 연동

## 전체 데이터 흐름

```
IoT 카메라 (비전 처리)
    │
    │  POST /api/v1/focus-events
    ▼
FocusEventController (미구현)
    │
    ▼
FocusEventService (미구현)
    │
    ▼
FocusEventRepository → MySQL (focus_event 테이블)
    │
    ▼
FocusEventFormatter.format(sessionId)
    │  최근 15분 이벤트 조회 및 포맷
    ▼
AiTutorService → {focus_events} 변수로 시스템 프롬프트에 주입
    │
    ▼
GPT-4o — 집중 상태를 참고해 질문 전략 조정
```

---

## IoT 요청 형식

### 엔드포인트

```
POST /api/v1/focus-events
Content-Type: application/json
```

### 요청 Body

```json
{
  "sessionId":   1,
  "eventType":   "GAZE_OUT",
  "detectedAt":  "2025-05-06T14:32:10",
  "durationSec": 45,
  "alerted":     false
}
```

### 필드 명세

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `sessionId` | Long | 필수 | 현재 학습 세션 ID |
| `eventType` | String (Enum) | 필수 | `GAZE_OUT` / `DROWSY` / `ABSENT` |
| `detectedAt` | LocalDateTime | 필수 | 이벤트 감지 시각 (ISO 8601) |
| `durationSec` | Integer | 필수 | 이벤트 지속 시간 (초 단위) |
| `alerted` | Boolean | 필수 | 사용자에게 알림 발송 여부 |

---

## 이벤트 타입별 감지 기준

| 타입 | 의미 | IoT 감지 조건 |
|------|------|--------------|
| `GAZE_OUT` | 시선 이탈 | 시선 방향 벡터가 모니터 영역 밖을 향함 |
| `DROWSY` | 졸음 감지 | 눈 깜빡임 빈도 증가, 눈꺼풀 처짐 (EAR 임계값 이하) |
| `ABSENT` | 자리 비움 | 카메라 프레임에서 얼굴이 감지되지 않음 |

> **GAZE_OUT 해석 주의:** 듀얼 모니터 사용, 교재 참고 등 정상적인 학습 행동일 수 있음.
> AI 튜터는 LAW 5에 따라 단순 시선 이탈을 집중 문제로 과대 해석하지 않도록 설계됨.

---

## AI 튜터에서 사용되는 방식

### 프롬프트 주입 형식

```
[GAZE_OUT | 45s | alerted=false]
[DROWSY | 130s | alerted=true]
[GAZE_OUT | 12s | alerted=false]
```

### AI 튜터 개입 조건 (LAW 5)

IoT 이벤트가 아래 조건을 만족할 때만 AI가 집중 관련 코멘트를 한다.

| 조건 | 기준 |
|------|------|
| 단일 ABSENT / DROWSY | 지속 시간 > 120초 |
| GAZE_OUT 반복 | 10분 내 3회 이상 |

조건 미달 시 AI는 이벤트를 조용히 무시하고 학습 질문에만 집중한다.

---

## sessionId 발급 구조

`FocusEvent`와 `StudyNote` 모두 `sessionId`로 연결된다. IoT가 이벤트를 보내려면 **현재 학습 세션의 sessionId를 알아야 한다.**

```
학습 시작
    │
    ▼
StudySession 생성 → sessionId 발급  (미구현)
    │
    ├── IoT 카메라에 sessionId 전달
    │       └── 이후 FocusEvent 전송 시 sessionId 포함
    │
    └── 사용자 노트 작성 시 sessionId 포함 (StudyNote.sessionId)
```

**현재 StudySession 엔티티가 미구현 상태.** sessionId가 어떻게 생성되고 IoT로 전달되는지 정의 필요.

---

## 현재 누락된 작업

### 1. FocusEventController + FocusEventService 구현 (필수)

IoT에서 데이터를 수신할 엔드포인트가 없음.

```java
// 구현 예시
@RestController
@RequestMapping("/api/v1/focus-events")
public class FocusEventController {

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody FocusEventRequest request) {
        focusEventService.save(request);
        return ResponseEntity.ok().build();
    }
}
```

**FocusEventRequest DTO 필요:**

```java
public record FocusEventRequest(
    Long sessionId,
    FocusEvent.EventType eventType,
    LocalDateTime detectedAt,
    Integer durationSec,
    Boolean alerted
) {}
```

### 2. StudySession 도메인 구현 (필수)

sessionId 발급 주체가 없음. IoT와 노트가 같은 sessionId로 연결되려면 세션 시작 API가 필요.

**최소 필요 필드:**

```java
@Entity
public class StudySession {
    private Long sessionId;
    private Long userId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer totalStudyMinutes;
    private Integer totalFocusMinutes;
}
```

**세션 시작 API:**
```
POST /api/v1/sessions
→ sessionId 반환 → 프론트엔드와 IoT 디바이스에 공유
```

### 3. IoT 인증 처리 고려

현재 인증 없이 누구나 focus-events를 전송할 수 있는 구조. 디바이스 토큰 또는 세션 토큰 기반 인증 검토 필요.

---

## 작업 우선순위

| 작업 | 중요도 | 이유 |
|------|--------|------|
| StudySession 도메인 + 세션 시작 API | 필수 | sessionId 없이 IoT 연동 불가 |
| FocusEventController / Service 구현 | 필수 | IoT 수신 엔드포인트 없음 |
| IoT 인증 처리 | 중간 | 검증되지 않은 이벤트 유입 방지 |
