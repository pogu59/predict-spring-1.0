# Docker 사용법 (predict 백엔드)

## 전체 스택 한번에 띄우기 (권장)

이 폴더의 `docker-compose.yml`이 MySQL + 백엔드(이 폴더) + 프론트엔드(`../predict-next-1.0`)를 한번에 관리한다.
compose 명령어는 항상 이 폴더(`predict/`)에서 실행한다.

```bash
# 처음 실행하거나, 코드를 수정한 뒤 이미지를 다시 빌드해서 띄울 때
docker compose up -d --build

# 코드 변경 없이 그냥 다시 띄울 때 (이미 빌드된 이미지 재사용, 빠름)
docker compose up -d

# 상태 확인
docker compose ps

# 로그 보기 (Ctrl+C로 빠져나옴, 컨테이너는 안 죽음)
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql

# 전체 종료 (컨테이너만 삭제, DB 데이터는 볼륨에 남음)
docker compose down

# DB 데이터까지 완전히 초기화하고 싶을 때만 (주의: MySQL 데이터 삭제됨)
docker compose down -v
```

띄우고 나면:
- 백엔드: http://localhost:8080
- 프론트엔드: http://localhost:3000
- MySQL: localhost:3306 (계정 predict / predict1234, DB명 predict)

카카오 로그인 키는 이 폴더의 `.env` 파일(gitignore 대상)에서 읽어온다.

## 백엔드만 따로 빌드/실행하고 싶을 때

`docker-compose.yml` 없이 이 폴더의 `Dockerfile`만 써서 이미지를 만들고 실행할 수도 있다.
단, 이 경우 MySQL 컨테이너와 같은 네트워크에 있지 않으므로 `SPRING_DATASOURCE_URL` 등을
직접 넘겨줘야 한다.

```bash
# 이미지 빌드
docker build -t predict-backend .

# 실행 (예: docker-compose로 띄운 mysql 컨테이너를 그대로 쓰는 경우)
docker run --rm -p 8080:8080 \
  --network predict_default \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://mysql:3306/predict?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul" \
  -e SPRING_DATASOURCE_USERNAME=predict \
  -e SPRING_DATASOURCE_PASSWORD=predict1234 \
  -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \
  predict-backend
```

## 파일 설명

| 파일 | 역할 |
|---|---|
| `Dockerfile` | 백엔드 이미지 빌드 정의 (gradle:9-jdk25로 빌드 → eclipse-temurin:25-jre-alpine으로 실행) |
| `.dockerignore` | 이미지에 넣지 않을 파일 (build/, .git, application-local.properties 등) |
| `docker-compose.yml` | mysql / backend / frontend 3개 서비스 정의 |
| `.env` | compose가 자동으로 읽는 환경변수 (카카오 client-id/secret). git에는 안 올라감 |

## 참고

- `application-local.properties`(로컬 개발용 H2/카카오 키)는 이미지에 안 들어간다. 대신
  docker-compose가 환경변수로 MySQL 접속정보를 직접 주입한다.
- 컨테이너 안 백엔드는 MySQL을 `localhost`가 아니라 서비스명 `mysql`로 접속한다
  (docker 내부 네트워크 DNS). `./gradlew bootRun`으로 로컬에서 직접 띄울 때는 반대로
  `application-local.properties`의 `localhost:3306`을 그대로 쓰면 된다 (mysql 컨테이너가
  호스트 3306 포트로 노출돼 있으므로).
