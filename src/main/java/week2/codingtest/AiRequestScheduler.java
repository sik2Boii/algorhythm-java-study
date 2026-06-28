package week2.codingtest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class AiRequestScheduler {

    private static final int MAX_RETRIES = 5;
    private static final int MAX_REINSERTS = 2;
    private static final long BASE_DELAY_MS = 500L;

    private final PriorityBlockingQueue<AiRequest> queue = new PriorityBlockingQueue<>();
    private final ExecutorService executor;
    private final RateLimiter rateLimiter;
    private final AtomicInteger activeWorkers = new AtomicInteger(0);  // 현재 처리 중인 워커 수

    public AiRequestScheduler(int workerCount, RateLimiter rateLimiter) {
        this.executor = Executors.newFixedThreadPool(workerCount);
        this.rateLimiter = rateLimiter;
    }

    public void submit(AiRequest request) {
        queue.offer(request);
        System.out.println("[큐 등록] " + request);
    }

    public void start() {
        int workerCount = ((java.util.concurrent.ThreadPoolExecutor) executor).getCorePoolSize();
        for (int i = 0; i < workerCount; i++) {
            executor.submit(this::consume);
        }
    }

    // 큐가 비어있고 처리 중인 워커도 없으면 완료
    public boolean isDone() {
        return queue.isEmpty() && activeWorkers.get() == 0;
    }

    // 블로킹 중인 take()를 인터럽트로 깨워서 종료
    public void shutdown() {
        executor.shutdownNow();
    }

    private void consume() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                AiRequest request = queue.take();
                activeWorkers.incrementAndGet();
                try {
                    processWithBackoff(request, 0);
                } finally {
                    activeWorkers.decrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void processWithBackoff(AiRequest request, int attempt) throws InterruptedException {
        if (attempt >= MAX_RETRIES) {
            if (request.getReinsertCount() < MAX_REINSERTS) {
                AiRequest bumped = request.withBumpedPriority(5);
                System.out.printf("[재삽입] %s → priority %d로 상향 후 재큐잉 (%d/%d회)%n",
                        request, bumped.getPriority(), bumped.getReinsertCount(), MAX_REINSERTS);
                queue.offer(bumped);
            } else {
                System.out.println("[포기] 최대 재삽입 초과 " + request);
            }
            return;
        }

        if (!rateLimiter.tryAcquire()) {
            long delay = BASE_DELAY_MS * (1L << attempt);  // 500 → 1000 → 2000 → 4000ms
            System.out.printf("[백오프] %s | %d번째 재시도 | %dms 대기%n", request, attempt + 1, delay);
            Thread.sleep(delay);
            processWithBackoff(request, attempt + 1);
            return;
        }

        System.out.printf("[처리 완료] %s | worker: %s%n", request, Thread.currentThread().getName());
    }
}
