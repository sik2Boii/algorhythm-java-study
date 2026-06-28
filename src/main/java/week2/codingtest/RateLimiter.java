package week2.codingtest;

import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter {

    private final int maxRequestsPerWindow;
    private final long windowMs;
    private final AtomicInteger count = new AtomicInteger(0);
    private volatile long windowStart = System.currentTimeMillis();

    public RateLimiter(int maxRequestsPerWindow, long windowMs) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowMs = windowMs;
    }

    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();

        // 윈도우가 지났으면 카운트 초기화
        if (now - windowStart >= windowMs) {
            count.set(0);
            windowStart = now;
        }

        if (count.get() >= maxRequestsPerWindow) {
            return false;  // Rate Limit 초과
        }

        count.incrementAndGet();
        return true;
    }
}
