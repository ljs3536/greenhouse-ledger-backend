# Greenhouse ledger API

온실과 식물을 직접 등록하고, 배치의 수량과 생장 단계를 기록하는 API입니다. 화면은 [greenhouse-ledger-frontend](https://github.com/ljs3536/greenhouse-ledger-frontend)입니다.

온실 수와 식물 종류는 고정되어 있지 않습니다. 새 동을 들이거나 다른 식물을 키우기 시작하면 목록에 추가합니다. 배치가 남아 있는 온실과 식물은 삭제되지 않습니다.

식물에는 낮 온도, 밤 온도, 광 메모를 참고값으로 남길 수 있습니다. 센서 값은 아직 연결하지 않습니다.

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

처음 기동하면 `schema.sql`로 테이블을 만듭니다. 예전에 네 동으로 고정해 둔 데이터가 있으면 온실과 식물 목록으로 옮깁니다.

## 기록 규칙

배치는 한 온실에 심은 식물 묶음입니다. 단계는 육묘, 생장, 꽃대, 개화, 출하 가능입니다.

- 입고는 수량을 더합니다. 배치를 만들 때 최초 입고가 함께 기록됩니다.
- 폐기와 판매는 남은 수량 이하만 기록됩니다.
- 판매는 출하 가능 단계에서만 기록됩니다.

## API

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/health` | 기동 확인 |
| GET | `/api/summary` | 온실별, 식물별 수량과 단계별 포트 수 |
| GET | `/api/greenhouses` | 온실 목록 |
| POST | `/api/greenhouses` | 온실 추가 |
| PATCH | `/api/greenhouses/{id}` | 온실 이름 변경 |
| DELETE | `/api/greenhouses/{id}` | 온실 삭제 |
| GET | `/api/plants` | 식물 목록 |
| POST | `/api/plants` | 식물 추가 |
| PATCH | `/api/plants/{id}` | 식물 수정 |
| DELETE | `/api/plants/{id}` | 식물 삭제 |
| GET | `/api/batches` | 배치 목록 |
| GET | `/api/batches/{id}` | 배치 상세와 기록 |
| POST | `/api/batches` | 배치 등록 |
| PATCH | `/api/batches/{id}/stage` | 단계 변경 |
| POST | `/api/batches/{id}/movements` | 입고, 폐기, 판매 |
| DELETE | `/api/batches/{id}` | 배치와 기록 삭제 |

단계 코드는 `SEEDLING`, `GROWTH`, `SPIKE`, `BLOOM`, `READY`입니다. 수량 기록 종류는 `INBOUND`, `DISPOSAL`, `SALE`입니다.
