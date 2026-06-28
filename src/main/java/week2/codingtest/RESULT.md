# 코딩테스트 - 고동시성 AI 요청 비동기 스케줄링 큐

## 구현 환경
- Java 21
- 워커 스레드: 2개 (`ExecutorService` 고정 스레드 풀)
- Rate Limit: 5초 윈도우에 최대 3개 요청
- 최대 재시도: 5회 / 최대 재삽입: 2회

## 클래스 구성

| 클래스 | 역할 |
|---|---|
| `AiRequest` | 요청 데이터. priority 높은 순 → 같으면 FIFO 정렬 |
| `RateLimiter` | 슬라이딩 윈도우 기반 Rate Limit 시뮬레이터 |
| `AiRequestScheduler` | `PriorityBlockingQueue` + `ExecutorService` + 지수 백오프 핵심 스케줄러 |

## 실행 결과

```
[큐 등록] [userId=1, priority=1, reinsert=0, message=일반 질문]
[큐 등록] [userId=2, priority=10, reinsert=0, message=긴급 요청]
[큐 등록] [userId=3, priority=1, reinsert=0, message=일반 질문]
[큐 등록] [userId=4, priority=5, reinsert=0, message=VIP 요청]
[큐 등록] [userId=5, priority=1, reinsert=0, message=일반 질문]
[큐 등록] [userId=6, priority=10, reinsert=0, message=긴급 요청]
[큐 등록] [userId=7, priority=5, reinsert=0, message=VIP 요청]
[큐 등록] [userId=8, priority=1, reinsert=0, message=일반 질문]
[큐 등록] [userId=9, priority=10, reinsert=0, message=긴급 요청]
[큐 등록] [userId=10, priority=1, reinsert=0, message=일반 질문]
[처리 완료] [userId=2, priority=10, reinsert=0, message=긴급 요청] | worker: pool-1-thread-2
[처리 완료] [userId=6, priority=10, reinsert=0, message=긴급 요청] | worker: pool-1-thread-2
[처리 완료] [userId=9, priority=10, reinsert=0, message=긴급 요청] | worker: pool-1-thread-1
[백오프] [userId=4, priority=5, reinsert=0, message=VIP 요청] | 1번째 재시도 | 500ms 대기
[백오프] [userId=7, priority=5, reinsert=0, message=VIP 요청] | 1번째 재시도 | 500ms 대기
[백오프] [userId=4, priority=5, reinsert=0, message=VIP 요청] | 2번째 재시도 | 1000ms 대기
[백오프] [userId=7, priority=5, reinsert=0, message=VIP 요청] | 2번째 재시도 | 1000ms 대기
[백오프] [userId=4, priority=5, reinsert=0, message=VIP 요청] | 3번째 재시도 | 2000ms 대기
[백오프] [userId=7, priority=5, reinsert=0, message=VIP 요청] | 3번째 재시도 | 2000ms 대기
[백오프] [userId=7, priority=5, reinsert=0, message=VIP 요청] | 4번째 재시도 | 4000ms 대기
[백오프] [userId=4, priority=5, reinsert=0, message=VIP 요청] | 4번째 재시도 | 4000ms 대기
[처리 완료] [userId=7, priority=5, reinsert=0, message=VIP 요청] | worker: pool-1-thread-1
[처리 완료] [userId=1, priority=1, reinsert=0, message=일반 질문] | worker: pool-1-thread-1
[처리 완료] [userId=5, priority=1, reinsert=0, message=일반 질문] | worker: pool-1-thread-1
[백오프] [userId=8, priority=1, reinsert=0, message=일반 질문] | 1번째 재시도 | 500ms 대기
[백오프] [userId=4, priority=5, reinsert=0, message=VIP 요청] | 5번째 재시도 | 8000ms 대기
[백오프] [userId=8, priority=1, reinsert=0, message=일반 질문] | 2번째 재시도 | 1000ms 대기
[백오프] [userId=8, priority=1, reinsert=0, message=일반 질문] | 3번째 재시도 | 2000ms 대기
[백오프] [userId=8, priority=1, reinsert=0, message=일반 질문] | 4번째 재시도 | 4000ms 대기
[처리 완료] [userId=8, priority=1, reinsert=0, message=일반 질문] | worker: pool-1-thread-1
[처리 완료] [userId=10, priority=1, reinsert=0, message=일반 질문] | worker: pool-1-thread-1
[처리 완료] [userId=3, priority=1, reinsert=0, message=일반 질문] | worker: pool-1-thread-1
[재삽입] [userId=4, priority=5, reinsert=0, message=VIP 요청] → priority 10로 상향 후 재큐잉 (1/2회)
[백오프] [userId=4, priority=10, reinsert=1, message=VIP 요청] | 1번째 재시도 | 500ms 대기
[백오프] [userId=4, priority=10, reinsert=1, message=VIP 요청] | 2번째 재시도 | 1000ms 대기
[백오프] [userId=4, priority=10, reinsert=1, message=VIP 요청] | 3번째 재시도 | 2000ms 대기
[백오프] [userId=4, priority=10, reinsert=1, message=VIP 요청] | 4번째 재시도 | 4000ms 대기
[처리 완료] [userId=4, priority=10, reinsert=1, message=VIP 요청] | worker: pool-1-thread-2

BUILD SUCCESSFUL in 23s
```

## 실행 결과 분석

### 지수 백오프 동작

Rate Limit 초과 시 대기 시간이 2배씩 증가한다.

| 재시도 횟수 | 대기 시간 |
|---|---|
| 1번째 | 500ms |
| 2번째 | 1,000ms |
| 3번째 | 2,000ms |
| 4번째 | 4,000ms |
| 5번째 | 8,000ms |

공식: `BASE_DELAY_MS * (1L << attempt)` → O(2ⁿ) 증가

### 우선순위 기아(Starvation) 및 재삽입 보완

백오프 루프에 갇힌 스레드는 Rate Limit 슬롯을 계속 놓치는 우선순위 기아 현상이 발생한다.
최대 재시도 소진 시 우선순위를 상향(+5)해 큐에 재삽입함으로써 처리 기회를 보장한다.

```
priority=5 → 5번 재시도 소진 → priority=10으로 재삽입 → 처리 성공
```

### 데드락 없는 이유

- `PriorityBlockingQueue`는 내부적으로 스레드 안전하게 설계되어 별도 `synchronized` 불필요
- 워커 스레드들이 동일한 락을 경쟁하지 않고 각자 큐에서 독립적으로 요청을 꺼내 처리

## 결론

`PriorityBlockingQueue` + `ExecutorService` + 지수 백오프 조합으로 Rate Limit 환경에서
우선순위 기반 요청을 데드락 없이 안전하게 처리하는 스케줄러를 구현했다.
백오프로 인한 우선순위 기아는 재삽입 시 priority 상향으로 보완했다.
