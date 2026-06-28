# Mission 1 - Thread-Per-Connection 스레드 폭발 및 락 병목 분석

## 실험 환경
- 서버: `week2.mission1.ChatServer` (Thread-Per-Connection 방식)
- 부하 도구: `week2.mission1.LoadGenerator` (가상 스레드 기반, 동시 연결 3,000개)
- JVM: Java 21 (메모리 제한 없음)
- 덤프 도구: `jstack`

## 스레드 덤프 추출

```
jstack <PID> > thread_dump.txt
```

## 결과

### 스레드 수

```
Threads class SMR info:
_java_thread_list=0x00006000010a5820, length=3005
```

클라이언트 3,000개 연결 → 플랫폼 스레드 **3,005개** 생성 (JVM 내부 스레드 5개 포함)

### 스레드 상태 분포

| 상태 | 수 | 의미 |
|---|---|---|
| `BLOCKED` | **2,990개** | `synchronized (engine)` 락 대기 중 |
| `RUNNABLE` | 10개 | 실행 중 |
| `WAITING` | 5개 | JVM 내부 스레드 |

### BLOCKED 스레드 스택 트레이스

```
java.lang.Thread.State: BLOCKED (on object monitor)
    at week2.mission1.ChatServer.handleClient(ChatServer.java:44)
    - waiting to lock <0x00000006800028c8> (a week1.mission2.GeminiEngine)
    at week2.mission1.ChatServer.lambda$main$0(ChatServer.java:24)
    at java.lang.Thread.run(java.base@21.0.6/Thread.java:1583)
```

## 분석

### 문제 1 — 스레드 폭발 (Thread Explosion)

Thread-Per-Connection 방식은 연결 1개당 플랫폼 스레드 1개를 생성한다.
플랫폼 스레드는 개당 약 512KB의 스택 메모리를 점유한다.

```
3,000개 × 512KB = 약 1.5GB (네이티브 메모리)
```

스레드 생성 자체가 무제한이므로 연결이 많아질수록 메모리가 선형으로 증가하며, 결국 OOM으로 이어진다.

### 문제 2 — 락 병목 (Lock Contention)

`synchronized (engine)` 블록으로 인해 AI 엔진이 단일 임계 영역(Critical Section)이 됐다.
락을 획득한 스레드 1개가 AI API 응답을 기다리는 동안 나머지 2,989개 스레드는 `BLOCKED` 상태로 대기한다.

```
전체 스레드 3,000개 중 99.6%가 BLOCKED 상태
```

AI API 응답 시간이 길수록 대기 시간이 누적되어 사실상 단일 스레드 서버와 다를 바 없는 처리량이 된다.

## 결론

Thread-Per-Connection 방식은 동시 연결이 증가할수록 두 가지 문제가 동시에 발생한다.

1. **스레드 폭발** — 무제한 스레드 생성으로 메모리 고갈 (OOM)
2. **락 병목** — `synchronized` 공유 자원으로 인해 대부분의 스레드가 BLOCKED

해결 방향: 스레드 풀(`ExecutorService`)로 스레드 수를 제한하고, AI 엔진을 스레드마다 독립적으로 사용하거나 `CompletableFuture` 기반 비동기 처리로 전환해야 한다.
