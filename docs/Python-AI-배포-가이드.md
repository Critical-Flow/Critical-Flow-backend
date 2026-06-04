# Critical-Flow-AI 서버 배포 가이드

> EC2 서버에 Python AI 웹캠 분석 서버를 Docker로 배포하고, GitHub Actions로 자동 배포까지 설정하는 가이드입니다.

---

## 전체 흐름 요약

```
코드 push → GitHub Actions → Docker 이미지 빌드 → GHCR 업로드 → EC2 자동 배포
```

---

## 📋 사전 준비 체크리스트

- [ ] EC2 서버 접속 가능한 상태
- [ ] Critical-Flow-AI 레포 clone 되어 있음
- [ ] Critical-Flow-backend 레포의 docker-compose.yml 수정 권한 있음

---

## STEP 1. Python AI 레포 코드 수정

> Critical-Flow-AI 레포에서 작업합니다.

### 1-1. `src/core/config.py` 수정

현재 `localhost`로 되어있는 URL을 Docker 내부 네트워크 주소로 변경합니다.

```python
# 수정 전
BACKEND_URL      = "http://localhost:8080/api/v1/sessions/end"
FOCUS_EVENTS_URL = "http://localhost:8080/api/v1/sessions/{userId}/focus-events"

# 수정 후
BACKEND_URL      = "http://app:8080/api/v1/sessions/{sessionId}/vision-result"
FOCUS_EVENTS_URL = "http://app:8080/api/v1/sessions/{userId}/focus-events"
```

> `app`은 Docker 네트워크 안에서 Spring 서버의 이름입니다. (docker-compose.yml의 서비스명)

- [ ] `config.py` 수정 완료

---

### 1-2. `src/api/session_router.py` 경로 수정

Spring에서 `/vision/start`, `/vision/stop`으로 호출하기 때문에 Python 라우터 경로를 맞춰줍니다.

```python
# 수정 전
@router.post("/start", ...)
@router.post("/stop", ...)

# 수정 후
@router.post("/vision/start", ...)
@router.post("/vision/stop", ...)
```

- [ ] `session_router.py` 경로 수정 완료

---

### 1-3. `main.py` 포트 변경

ChromaDB가 8000번 포트를 이미 쓰고 있어서 충돌납니다. 5000으로 변경합니다.

```python
# 수정 전
uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=False)

# 수정 후
uvicorn.run("main:app", host="0.0.0.0", port=5000, reload=False)
```

- [ ] `main.py` 포트 수정 완료

---

### 1-4. `requirements.txt` 생성

레포 루트에 `requirements.txt` 파일을 만듭니다.

```txt
fastapi
uvicorn
mediapipe
opencv-python-headless
httpx
requests
pydantic
```

> `opencv-python-headless` : 서버 환경에서는 GUI가 없어서 headless 버전을 써야 합니다.

- [ ] `requirements.txt` 생성 완료

---

### 1-5. `Dockerfile` 생성

레포 루트에 `Dockerfile` 파일을 만듭니다.

```dockerfile
FROM python:3.11-slim

WORKDIR /app

# 시스템 의존성 (OpenCV headless 실행에 필요)
RUN apt-get update && apt-get install -y \
    libglib2.0-0 \
    libgl1-mesa-glx \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 5000

CMD ["python", "main.py"]
```

- [ ] `Dockerfile` 생성 완료

---

### 1-6. GitHub Actions 워크플로우 생성

`.github/workflows/deploy.yml` 파일을 생성합니다. (폴더가 없으면 만들어야 합니다)

```yaml
name: Deploy Python AI

on:
  push:
    branches: [ main ]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and Push Docker Image
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ghcr.io/critical-flow/critical-flow-ai:latest

      - name: Deploy to EC2
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USER }}
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            cd ~/Critical-Flow-backend
            docker compose pull python-ai
            docker compose up -d python-ai
```

- [ ] `.github/workflows/deploy.yml` 생성 완료

---

### 1-7. 변경사항 push

```bash
git add .
git commit -m "feat: Docker 배포 환경 설정"
git push origin main
```

- [ ] push 완료

---

## STEP 2. GitHub Secrets 설정

> Critical-Flow-AI 레포의 Settings → Secrets and variables → Actions 에서 추가합니다.

| Secret 이름 | 값 | 설명 |
|---|---|---|
| `EC2_HOST` | `15.165.244.126` | EC2 서버 IP |
| `EC2_USER` | `ubuntu` | EC2 접속 유저 |
| `EC2_SSH_KEY` | EC2 .pem 파일 내용 전체 | SSH 접속 키 |

### EC2_SSH_KEY 넣는 방법

```bash
# 로컬에서 .pem 파일 내용 출력
cat ~/.ssh/your-key.pem
```

출력된 내용 전체를 복사해서 `EC2_SSH_KEY` 값에 붙여넣기

- [ ] `EC2_HOST` 추가 완료
- [ ] `EC2_USER` 추가 완료
- [ ] `EC2_SSH_KEY` 추가 완료

---

## STEP 3. GHCR 패키지 공개 설정

> GitHub Container Registry에서 이미지를 EC2가 pull 할 수 있도록 공개 설정합니다.

1. `https://github.com/orgs/Critical-Flow/packages` 접속
2. `critical-flow-ai` 패키지 클릭
3. Package settings → Change visibility → **Public** 선택

- [ ] GHCR 패키지 공개 설정 완료

---

## STEP 4. docker-compose.yml 수정

> Critical-Flow-backend 레포에서 작업합니다.

`docker-compose.yml`에 `python-ai` 서비스를 추가합니다.

```yaml
  python-ai:
    image: ghcr.io/critical-flow/critical-flow-ai:latest
    container_name: criticalflow-python
    restart: unless-stopped
    networks:
      - criticalflow-net
```

> 포트를 외부에 열지 않습니다. Spring이 내부 네트워크(`criticalflow-net`)로 직접 통신하기 때문입니다.

- [ ] `docker-compose.yml` 수정 완료
- [ ] PR 생성 및 병합 완료

---

## STEP 5. Spring 코드 수정

> Critical-Flow-backend 레포에서 작업합니다.

`src/main/java/com/criticalflow/global/vision/PythonVisionClient.java`의 URL 수정이 필요없는지 확인합니다.

현재 `PYTHON_VISION_URL` 환경변수로 URL을 관리하고 있으므로 EC2 서버의 `.env` 파일만 수정하면 됩니다.

- [ ] 확인 완료

---

## STEP 6. EC2 서버 설정

> EC2에 SSH로 접속해서 작업합니다.

### 6-1. `.env` 파일에 Python URL 추가

```bash
cd ~/Critical-Flow-backend
vi .env
```

아래 줄 추가:
```
PYTHON_VISION_URL=http://python-ai:5000
```

- [ ] `.env` 수정 완료

### 6-2. 최신 docker-compose.yml pull

```bash
cd ~/Critical-Flow-backend
git pull origin develop
```

- [ ] `git pull` 완료

### 6-3. 서비스 시작

```bash
docker compose up -d python-ai
```

- [ ] `python-ai` 컨테이너 시작 완료

### 6-4. Spring 앱 재시작 (환경변수 반영)

```bash
docker compose up -d --force-recreate app
```

- [ ] Spring 앱 재시작 완료

---

## STEP 7. 배포 확인

```bash
# 컨테이너 상태 확인
docker compose ps

# Python AI 로그 확인
docker compose logs -f python-ai
```

정상이면 아래처럼 보입니다:
```
criticalflow-python  | INFO:     Started server process
criticalflow-python  | INFO:     Uvicorn running on http://0.0.0.0:5000
```

- [ ] `criticalflow-python` 컨테이너 `Up` 상태 확인
- [ ] 로그에 오류 없음 확인

---

## 이후 자동 배포

설정 완료 후에는 `main` 브랜치에 push만 하면:

```
push → GitHub Actions 자동 실행 → 이미지 빌드 → EC2 자동 배포
```

---

## 문제 해결

### Python 컨테이너가 계속 재시작될 때
```bash
docker compose logs python-ai
```
로그에서 오류 원인 확인

### Spring → Python 통신이 안될 때
```bash
# python-ai 컨테이너 내부에서 확인
docker exec -it criticalflow-python sh
curl http://localhost:5000/vision/start
```

### 이미지 pull이 안될 때
GHCR 패키지가 Private인지 확인 → STEP 3으로 돌아가서 Public으로 변경
