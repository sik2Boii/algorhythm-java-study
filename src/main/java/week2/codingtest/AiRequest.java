package week2.codingtest;

public class AiRequest implements Comparable<AiRequest> {

    private final int userId;
    private final String message;
    private final int priority;  // 숫자가 높을수록 먼저 처리
    private final long createdAt;
    private final int reinsertCount;  // 재삽입 횟수 (무한 루프 방지용)

    public AiRequest(int userId, String message, int priority) {
        this(userId, message, priority, 0);
    }

    private AiRequest(int userId, String message, int priority, int reinsertCount) {
        this.userId = userId;
        this.message = message;
        this.priority = priority;
        this.createdAt = System.currentTimeMillis();
        this.reinsertCount = reinsertCount;
    }

    /**
     * 현재 요청의 우선순위를 올린 새 요청을 반환
     *
     * 백오프 재시도 소진 후 큐에 재삽입할 때 사용한다.
     * reinsertCount를 증가시켜 무한 재삽입을 방지한다.
     *
     * @param bump 현재 priority에 더할 값
     * @return 우선순위가 올라간 새 AiRequest
     */
    public AiRequest withBumpedPriority(int bump) {
        return new AiRequest(userId, message, priority + bump, reinsertCount + 1);
    }

    /**
     * 우선순위 내림차순, 동일 우선순위면 생성 시각 오름차순(FIFO)으로 정렬
     *
     * PriorityBlockingQueue가 이 메서드를 기준으로 꺼낼 순서를 결정한다.
     *
     * @param other 비교할 요청
     * @return 양수면 other가 먼저, 음수면 this가 먼저
     */
    @Override
    public int compareTo(AiRequest other) {
        if (this.priority != other.priority) {
            return Integer.compare(other.priority, this.priority);
        }
        return Long.compare(this.createdAt, other.createdAt);
    }

    public int getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }

    public int getPriority() {
        return priority;
    }

    public int getReinsertCount() {
        return reinsertCount;
    }

    @Override
    public String toString() {
        return String.format("[userId=%d, priority=%d, reinsert=%d, message=%s]",
                userId, priority, reinsertCount, message);
    }
}
