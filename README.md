# 네 동의 장부 API

작은 비닐하우스 네 동에서 키우는 난의 수량과 생장 단계를 기록하는 API입니다. 화면은 [greenhouse-ledger-frontend](https://github.com/ljs3536/greenhouse-ledger-frontend)입니다.

동마다 품종이 하나씩 고정되어 있습니다.

| 동 | 품종 | 기준 환경 |
| --- | --- | --- |
| 1동 | 심비디움 | 낮 20–24°C, 밤 10–15°C, 밝게 |
| 2동 | 호접란 | 낮 26–29°C, 밤 18–20°C, 그늘 |
| 3동 | 덴파레 | 낮 25–30°C, 밤 18–20°C, 밝게 |
| 4동 | 온시디움 | 낮 23–27°C, 밤 15–18°C, 밝게, 통풍 |

기준 환경은 재배 참고값입니다. 센서 값은 아직 연결하지 않습니다.

## 기술

- Java 21
- Spring Boot 4.1
- MariaDB 11.4

## 실행

Java 21과 Docker가 필요합니다.

```bash
docker compose up -d
./gradlew bootRun
```

API는 http://127.0.0.1:8080 입니다. 데이터베이스는 호스트의 3307 포트로 열립니다. 로컬 개발 계정은 `farm` / `farm`, 데이터베이스 이름은 `greenhouse`입니다.

처음 기동하면 `schema.sql`로 테이블을 만들고, 네 동을 넣습니다. 이미 테이블이 있으면 그대로 둡니다.

## 기록 규칙

배치는 한 동에 심은 포트 묶음입니다. 단계는 육묘, 생장, 꽃대, 개화, 출하 가능입니다.

- 입고는 수량을 더합니다. 배치를 만들 때 최초 입고가 함께 기록됩니다.
- 폐기와 판매는 남은 수량 이하만 기록됩니다.
- 판매는 출하 가능 단계에서만 기록됩니다.

## API

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/health` | 기동 확인 |
| GET | `/api/summary` | 동별 수량과 출하 가능 수량 |
| GET | `/api/batches` | 배치 목록 |
| GET | `/api/batches/{id}` | 배치 상세와 기록 |
| POST | `/api/batches` | 배치 등록 |
| PATCH | `/api/batches/{id}/stage` | 단계 변경 |
| POST | `/api/batches/{id}/movements` | 입고, 폐기, 판매 |
| DELETE | `/api/batches/{id}` | 배치와 기록 삭제 |

단계 코드는 `SEEDLING`, `GROWTH`, `SPIKE`, `BLOOM`, `READY`입니다. 수량 기록 종류는 `INBOUND`, `DISPOSAL`, `SALE`입니다.
