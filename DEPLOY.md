# 클라우드 배포 가이드

로컬 `docker compose up -d`로 되던 걸 실제 VPS에 올려서 인터넷에 공개하는 절차. GitHub에는
이미 `predict`, `predict-next-1.0` 두 레포가 올라가 있다고 가정한다.

## 0. 준비물

- Vultr 계정 (https://www.vultr.com 개인 가입, 사업자등록 불필요, 카드 등록)
- 도메인 1개 (카카오 로그인 redirect URI 때문에 필수. 가비아/Namecheap 등에서 연 1만원 안팎)

## 1. Vultr에서 서버 생성

1. Vultr 대시보드 > **Deploy** > **Deploy New Server**
2. **Cloud Compute** 선택 (Shared CPU, 가장 저렴한 카테고리)
3. Location: **Seoul, South Korea (ICN)**
4. Image: **Ubuntu 24.04 LTS**
5. Server Size: **2 GB RAM / 1 vCPU / 55 GB SSD ($12/mo)** 선택
   (MySQL + Spring Boot(JVM) + Next.js를 같이 돌려서 1GB짜리는 빠듯함)
6. SSH Keys: 로컬에 SSH 키 없으면 먼저 만들기
   ```bash
   ssh-keygen -t ed25519 -C "predict-vultr"
   ```
   `~/.ssh/predict-vultr.pub` 내용을 Vultr의 "Add SSH Key"에 붙여넣고 체크
7. **Deploy Now**. 몇 분 뒤 인스턴스 상세 페이지에서 공인 IP 확인

## 2. 방화벽 설정 (Vultr Firewall)

Vultr 대시보드 > **Firewall** > **Add Firewall Group** 만들고 인바운드 규칙:

| Protocol | Port | Source |
|---|---|---|
| SSH | 22 | My IP (또는 Anywhere, 접속 불안정하면 Anywhere) |
| TCP | 80 | Anywhere |
| TCP | 443 | Anywhere |

8080, 3000은 **열지 않는다** (Caddy가 컨테이너 내부 네트워크로만 접속하므로 외부 노출 불필요).
만든 Firewall Group을 서버 인스턴스에 연결(Manage 메뉴에서 Firewall 탭).

## 2. 도메인 연결

도메인 관리 페이지에서 A레코드 2개를 서버 공인 IP로 연결:

```
predict.com       A   <서버 IP>      # 프론트엔드
api.predict.com   A   <서버 IP>      # 백엔드
```

전파에 몇 분~몇 시간 걸릴 수 있음. `nslookup predict.com`으로 확인 가능.

## 3. 서버에 Docker 설치

Vultr Ubuntu 이미지는 기본적으로 **root**로 SSH 접속한다 (배포 시 등록한 키 사용).

```bash
ssh -i ~/.ssh/predict-vultr root@<서버 IP>
curl -fsSL https://get.docker.com | sh
```

root라서 `usermod`/재접속 없이 바로 `docker` 명령을 쓸 수 있다.

## 4. 코드 내려받기

두 레포를 **형제 디렉토리**로 받는다 (docker-compose.yml이 `../predict-next-1.0`을 상대경로로 참조하므로).

```bash
mkdir -p ~/app && cd ~/app
git clone <predict 레포 주소> predict
git clone <predict-next-1.0 레포 주소> predict-next-1.0
cd predict
```

## 5. `.env` 채우기

로컬에서 쓰던 `.env`는 gitignore 대상이라 서버엔 없다. `predict/.env` 파일을 새로 만든다.

```bash
nano .env
```

```env
KAKAO_CLIENT_ID=95c76fcc8bccc3a632892d84f5880ff0
KAKAO_CLIENT_SECRET=DXWFjAsOwE7j7sw0tqvpD7k3Sipo1JzB

DOMAIN=predict.com
API_DOMAIN=api.predict.com
ACME_EMAIL=본인이메일@example.com

APP_FRONTEND_URL=https://predict.com
NEXT_PUBLIC_API_BASE_URL=https://api.predict.com

# 로컬 개발 기본값(root1234/predict1234) 그대로 쓰지 말고 반드시 새 값으로
MYSQL_ROOT_PASSWORD=<새 비밀번호>
MYSQL_PASSWORD=<새 비밀번호>
```

## 6. 카카오 디벨로퍼스 설정 갱신

카카오 디벨로퍼스 > 내 애플리케이션 > 카카오 로그인 > Redirect URI에 추가:

```
https://api.predict.com/login/oauth2/code/kakao
```

(플랫폼 설정에 웹 사이트 도메인 `https://predict.com`도 등록)

## 7. 빌드 및 실행

```bash
docker compose --profile prod up -d --build
```

`--profile prod`를 붙여야 Caddy(HTTPS 리버스 프록시)까지 같이 뜬다. 처음 뜰 때 Caddy가
Let's Encrypt에서 인증서를 자동 발급받는다 (도메인 DNS가 이미 서버를 가리키고 있어야 성공함).

## 8. 확인

```bash
docker compose ps
docker compose logs -f caddy     # 인증서 발급 로그 확인
```

- https://predict.com — 프론트엔드
- https://api.predict.com — 백엔드
- 카카오 로그인 버튼 눌러서 끝까지 로그인되는지 확인

## 이후 배포(코드 업데이트할 때)

```bash
cd ~/app/predict && git pull
cd ~/app/predict-next-1.0 && git pull
cd ~/app/predict
docker compose --profile prod up -d --build
```

## 참고

- `Caddyfile`, `.env`의 `DOMAIN`/`API_DOMAIN`/`ACME_EMAIL`은 로컬 개발용 `docker compose up -d`에는
  전혀 관여하지 않는다 (caddy 서비스는 `profiles: ["prod"]`라 기본 실행에는 안 뜸).
- MySQL 데이터는 `mysql-data` 볼륨에 저장된다. 서버 자체를 날리지 않는 한 재배포해도 데이터는
  유지됨. 백업이 필요하면 `docker exec predict-mysql mysqldump -upredict -p<비밀번호> predict > backup.sql`.
- 카테고리 5개 기본값(`schema/예측게임_schema_8.sql`) 같은 초기 데이터는 배포 후 한 번 직접
  INSERT 해줘야 한다. Hibernate `ddl-auto=update`는 테이블 구조만 만들고 데이터는 안 넣는다.
