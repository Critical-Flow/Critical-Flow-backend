# Python AI 서버 EC2 인스턴스 생성 가이드

> AWS에서 Python AI 전용 EC2 서버를 새로 만드는 가이드입니다.

---

## STEP 1. EC2 인스턴스 생성

1. AWS 콘솔 접속 → **EC2** → **인스턴스 시작** 클릭

2. **이름**: `critical-flow-python-ai`

3. **AMI (운영체제)**: `Ubuntu Server 24.04 LTS` 선택
   - 검색창에 `ubuntu` 입력 후 선택

4. **인스턴스 유형**: `t2.medium` 선택
   - MediaPipe + OpenCV가 메모리를 많이 써서 t2.small은 부족할 수 있음
   - RAM 4GB

5. **키 페어**: 기존에 쓰던 키 페어 선택
   - 새로 만들면 `.pem` 파일 잘 보관

6. **네트워크 설정** → **보안 그룹 생성**:

   | 유형 | 프로토콜 | 포트 | 소스 | 설명 |
   |---|---|---|---|---|
   | SSH | TCP | 22 | 내 IP | 서버 접속용 |
   | 사용자 지정 TCP | TCP | 5000 | `Spring EC2 IP/32` | Spring 서버에서만 접근 허용 |

   > Spring EC2 IP: `15.165.244.126`
   > 5000번 포트를 Spring 서버 IP에서만 열어서 외부 접근 차단

7. **스토리지**: 기본 8GB → `20GB`로 변경 (Docker 이미지 용량)

8. **인스턴스 시작** 클릭

- [ ] EC2 인스턴스 생성 완료

---

## STEP 2. Elastic IP 할당

> 서버 재시작해도 IP가 바뀌지 않도록 고정 IP를 설정합니다.

1. EC2 콘솔 → 왼쪽 메뉴 **탄력적 IP** 클릭
2. **탄력적 IP 주소 할당** 클릭 → **할당**
3. 생성된 IP 선택 → **작업** → **탄력적 IP 주소 연결**
4. 방금 만든 인스턴스 선택 → **연결**
5. 할당된 IP 메모해두기 (예: `13.xxx.xxx.xxx`)

- [ ] Elastic IP 할당 및 연결 완료
- [ ] IP 주소 메모: `______________`

---

## STEP 3. 서버 초기 설정

EC2에 SSH 접속:
```bash
ssh -i your-key.pem ubuntu@<새 EC2 IP>
```

### Docker 설치

```bash
# 패키지 업데이트
sudo apt-get update

# Docker 설치
sudo apt-get install -y docker.io docker-compose-plugin

# ubuntu 유저에게 Docker 권한 부여
sudo usermod -aG docker ubuntu

# 적용을 위해 재접속
exit
```

다시 SSH 접속 후 확인:
```bash
docker --version
```

- [ ] Docker 설치 완료

---

## STEP 4. GitHub Actions Secrets 업데이트

> Critical-Flow-AI 레포의 GitHub Secrets를 새 서버 정보로 업데이트합니다.

GitHub → Critical-Flow-AI 레포 → Settings → Secrets and variables → Actions

| Secret 이름 | 값 |
|---|---|
| `EC2_HOST` | 새 EC2 Elastic IP |
| `EC2_USER` | `ubuntu` |
| `EC2_SSH_KEY` | 키 페어 `.pem` 파일 내용 전체 |

- [ ] Secrets 업데이트 완료

---

## STEP 5. Spring 서버 보안 그룹 수정

> Spring EC2가 새 Python EC2의 5000번 포트로 요청을 보낼 수 있어야 합니다.
> 이건 Python EC2 보안 그룹에서 이미 설정했으므로 별도 작업 불필요.

- [ ] 확인 완료

---

## STEP 6. Spring 서버 환경변수 업데이트

> Spring EC2의 `.env` 파일에서 Python URL을 새 서버 주소로 변경합니다.

```bash
# Spring EC2 접속
ssh -i your-key.pem ubuntu@15.165.244.126

# .env 수정
vi ~/Critical-Flow-backend/.env

# 아래 줄 수정
PYTHON_VISION_URL=http://<새 EC2 IP>:5000

# Spring 재시작
cd ~/Critical-Flow-backend
docker compose up -d --force-recreate app
```

- [ ] `.env` 수정 완료
- [ ] Spring 재시작 완료

---

## 완료 후 구조

```
[프론트 - Vercel]
      ↓
[Spring - EC2 15.165.244.126]
      ↓ HTTP 내부 통신
[Python AI - EC2 새 IP:5000]
```

---

## 다음 단계

EC2 생성 완료 후 `Python-AI-배포-가이드.md` 로 돌아가서 배포 작업 진행.
