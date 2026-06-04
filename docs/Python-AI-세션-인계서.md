# Critical-Flow-AI 배포 작업 인계서

> 새 세션에서 이 파일만 읽으면 현재 상황과 흐름을 바로 이어갈 수 있습니다.

---

## 프로젝트 개요

- **백엔드**: Spring Boot 3.2.4 — EC2 (15.165.244.126) Docker 배포 중
- **Python AI**: FastAPI 웹캠 집중도 분석 서버 — 아직 미배포 (로컬 상태)
- **프론트**: React — Vercel 배포 중 (`https://critical-flow-frontend-z1ch.vercel.app`)
- **도메인**: `https://api.aice-edu.site`

---

## 현재 Spring ↔ Python 연동 구조

### 구현된 흐름

```
프론트 → POST /api/v1/sessions
           → Spring DB 저장
           → Python POST /vision/start 호출 (PythonVisionClient)

프론트 → POST /api/v1/sessions/{id}/end
           → Spring endTime DB 저장
           → Python POST /vision/stop 호출

Python 분석 완료
           → Spring POST /api/v1/sessions/{id}/vision-result 콜백
           → Spring이 drowsyCount, absentSeconds 등 DB 저장
```

### Spring PythonVisionClient 코드 위치

`src/main/java/com/criticalflow/global/vision/PythonVisionClient.java`

```java
// Spring이 Python을 호출하는 경로
POST /vision/start  (body: sessionId, userId)
POST /vision/stop   (body: sessionId)
```

### Python vision-result 수신 엔드포인트

```
POST /api/v1/sessions/{sessionId}/vision-result
```

수신 payload:
```json
{
  "userId": 123,
  "totalStudySeconds": 43,
  "goodFocusSeconds": 42,
  "drowsySeconds": 1,
  "absentSeconds": 0,
  "drowsyCount": 1,
  "absentCount": 0
}
```

### Spring 환경변수

```
PYTHON_VISION_URL=http://python-ai:5000   ← EC2 .env에 추가 필요 (아직 미설정)
```

현재 기본값 `http://localhost:5000` 상태. Python 호출 실패해도 try-catch로 로그만 찍고 서버는 정상 동작.

---

## Python AI 레포 현황

**레포**: `https://github.com/Critical-Flow/Critical-Flow-AI`

### 현재 코드 문제점 (수정 필요)

**1. `src/core/config.py` URL이 localhost로 되어있음**
```python
# 현재 (틀림)
BACKEND_URL = "http://localhost:8080/api/v1/sessions/end"

# 수정 필요
BACKEND_URL = "http://app:8080/api/v1/sessions/{sessionId}/vision-result"
```

**2. `src/api/session_router.py` 경로 불일치**
```python
# 현재 Python 경로
POST /start
POST /stop

# Spring이 호출하는 경로 (이것에 맞춰야 함)
POST /vision/start
POST /vision/stop
```

**3. `main.py` 포트 충돌**
```python
# 현재 (ChromaDB도 8000이라 충돌)
uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=False)

# 수정 필요
uvicorn.run("main:app", host="0.0.0.0", port=5000, reload=False)
```

**4. Dockerfile 없음** → 새로 만들어야 함

**5. requirements.txt 없음** → 새로 만들어야 함

**6. GitHub Actions 워크플로우 없음** → 새로 만들어야 함

---

## Python AI 서버 구조 요약

```
main.py              FastAPI 진입점, uvicorn 실행
src/core/app.py      FastAPI 앱 팩토리, 의존성 조립
src/core/config.py   URL, 임계값 등 전역 설정
src/api/session_router.py   /start, /stop 엔드포인트
src/service/session_orchestrator.py   세션 생명주기 관리
src/service/session_report_service.py  집계 계산 + Spring 전송
src/repository/backend_client.py       Spring API 호출 HTTP 클라이언트
src/domain/models.py   SessionResult (Pydantic DTO)
```

- DB 직접 접속 없음 (Spring API로만 통신)
- MediaPipe로 웹캠 얼굴 분석
- 졸음/자리이탈 감지 후 세션 종료 시 집계값을 Spring에 POST

---

## 배포 목표

같은 EC2에 Docker로 배포. Spring의 `docker-compose.yml`에 서비스 추가.

```yaml
# docker-compose.yml에 추가할 서비스
python-ai:
  image: ghcr.io/critical-flow/critical-flow-ai:latest
  container_name: criticalflow-python
  restart: unless-stopped
  networks:
    - criticalflow-net
```

포트는 외부에 열지 않음. Spring과 내부 Docker 네트워크(`criticalflow-net`)로 통신.

---

## 해야 할 작업 목록

### Critical-Flow-AI 레포에서

- [ ] `src/core/config.py` BACKEND_URL 수정
- [ ] `src/api/session_router.py` 경로 `/vision/start`, `/vision/stop`으로 수정
- [ ] `main.py` 포트 8000 → 5000 변경
- [ ] `requirements.txt` 생성
- [ ] `Dockerfile` 생성
- [ ] `.github/workflows/deploy.yml` 생성
- [ ] GitHub Secrets 등록 (EC2_HOST, EC2_USER, EC2_SSH_KEY)
- [ ] GHCR 패키지 Public 설정

### Critical-Flow-backend 레포에서

- [ ] `docker-compose.yml`에 `python-ai` 서비스 추가

### EC2 서버에서

- [ ] `.env`에 `PYTHON_VISION_URL=http://python-ai:5000` 추가
- [ ] `git pull` 후 `docker compose up -d python-ai`
- [ ] `docker compose up -d --force-recreate app` (Spring 환경변수 반영)

---

## 상세 배포 가이드

더 자세한 내용은 같은 `docs/` 폴더의 `Python-AI-배포-가이드.md` 파일 참고.

---

## EC2 서버 정보

| 항목 | 값 |
|---|---|
| IP | 15.165.244.126 |
| 도메인 | api.aice-edu.site |
| 접속 유저 | ubuntu |
| 프로젝트 경로 | ~/Critical-Flow-backend |
| 배포 방식 | docker compose (5개 서비스: nginx, app, db, chroma, chroma-init) |

---

## Spring 백엔드 현재 상태

- 서버 정상 동작 중 (`{"status":"UP"}`)
- develop 브랜치 기준 최신 코드 배포됨
- Python 연동 코드는 이미 구현되어 있음 (PythonVisionClient)
- Python 서버가 없어도 서버 동작에 문제 없음 (호출 실패 시 로그만 출력)
