package week2.mission1;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadGenerator {

    private static final String HOST = "localhost";
    private static final int PORT = 9999;
    private static final int CONNECTIONS = 3000;
    private static final int HOLD_SECONDS = 30;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 부하 생성기 시작: " + CONNECTIONS + "개 동시 연결 ===");

        AtomicInteger connected = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(CONNECTIONS);

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < CONNECTIONS; i++) {
                final int id = i;
                pool.submit(() -> {
                    try (
                        Socket socket = new Socket(HOST, PORT);
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
                    ) {
                        connected.incrementAndGet();
                        in.readLine(); // 서버 연결 확인 메시지 수신
                        out.println("안녕하세요 " + id + "번 유저");
                        Thread.sleep(HOLD_SECONDS * 1000L); // 연결 유지
                    } catch (Exception e) {
                        failed.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            System.out.println("연결 요청 " + CONNECTIONS + "개 전송 완료.");
            System.out.println("jstack <PID> 명령어로 스레드 덤프를 추출하세요.");
            latch.await();
        }

        System.out.println("=== 완료 — 성공: " + connected.get() + " / 실패: " + failed.get() + " ===");
    }
}
