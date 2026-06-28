package week2.codingtest;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        // 5초 윈도우에 최대 3개 요청만 허용 (Rate Limit 시뮬레이션)
        RateLimiter rateLimiter = new RateLimiter(3, 5000);

        // 워커 스레드 2개짜리 스케줄러
        AiRequestScheduler scheduler = new AiRequestScheduler(2, rateLimiter);

        // 요청을 전부 등록한 뒤 워커 시작 → 우선순위 정렬 보장
        scheduler.submit(new AiRequest(1, "일반 질문", 1));
        scheduler.submit(new AiRequest(2, "긴급 요청", 10));
        scheduler.submit(new AiRequest(3, "일반 질문", 1));
        scheduler.submit(new AiRequest(4, "VIP 요청", 5));
        scheduler.submit(new AiRequest(5, "일반 질문", 1));
        scheduler.submit(new AiRequest(6, "긴급 요청", 10));
        scheduler.submit(new AiRequest(7, "VIP 요청", 5));
        scheduler.submit(new AiRequest(8, "일반 질문", 1));
        scheduler.submit(new AiRequest(9, "긴급 요청", 10));
        scheduler.submit(new AiRequest(10, "일반 질문", 1));

        // 전부 등록 완료 후 워커 시작
        scheduler.start();

        // 모든 요청 처리 완료될 때까지 대기
        while (!scheduler.isDone()) {
            Thread.sleep(200);
        }
        scheduler.shutdown();
    }
}
