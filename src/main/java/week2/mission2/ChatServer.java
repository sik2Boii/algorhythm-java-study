package week2.mission2;

import week1.mission2.AIEngine;
import week1.mission2.AIEngineFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatServer {

    private static final int PORT = 9999;
    private static final int THREAD_POOL_SIZE = 10;

    private final ConcurrentHashMap<Integer, ChatSession> sessions = new ConcurrentHashMap<>();
    private final AIEngine engine = AIEngineFactory.create("gemini");
    private final ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private final AtomicInteger userIdCounter = new AtomicInteger(0);
    private final ChatHistoryLogger logger;

    public ChatServer() throws IOException {
        new java.io.File("src/main/java/week2/mission2/logs").mkdirs();
        this.logger = new ChatHistoryLogger("src/main/java/week2/mission2/logs/chat_history.log");
    }

    public void start() throws IOException {
        System.out.println("=== [Pipeline] AI 채팅 서버 시작 (포트: " + PORT + ") ===");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                int userId = userIdCounter.incrementAndGet();
                executor.submit(() -> handleClient(userId, socket));
            }
        }
    }

    private void handleClient(int userId, Socket socket) {
        try {
            ChatSession session = new ChatSession(userId, socket);
            sessions.put(userId, session);
            System.out.println("[연결] userId=" + userId + " | 총 세션: " + sessions.size());

            session.send("연결 성공! userId=" + userId);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("quit")) break;

                String response = engine.chat(message);

                // 브로드캐스트 + 로그 동시 처리
                broadcast(userId, message, response);
                logger.log(userId, message, response);
            }
        } catch (Exception e) {
            System.out.println("[오류] userId=" + userId + ": " + e.getMessage());
        } finally {
            sessions.computeIfPresent(userId, (id, session) -> {
                try { session.close(); } catch (IOException ignored) {}
                return null;
            });
            System.out.println("[연결 종료] userId=" + userId + " | 남은 세션: " + sessions.size());
        }
    }

    private void broadcast(int senderId, String message, String response) {
        String broadcastMsg = String.format("[userId=%d] Q: %s | A: %s", senderId, message, response);
        sessions.forEach((userId, session) -> session.send(broadcastMsg));
    }

    public static void main(String[] args) throws IOException {
        new ChatServer().start();
    }
}
