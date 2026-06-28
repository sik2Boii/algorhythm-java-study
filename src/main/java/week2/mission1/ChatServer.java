package week2.mission1;

import week1.mission2.AIEngine;
import week1.mission2.AIEngineFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer {

    private static final int PORT = 9999;
    private static AIEngine engine = AIEngineFactory.create("gemini"); // 공유 자원 - 스레드 경합 발생

    public static void main(String[] args) throws Exception {
        System.out.println("=== [Thread-Per-Connection] AI 채팅 서버 시작 (포트: " + PORT + ") ===");
        System.out.println("현재 엔진: " + engine.getEngineName());

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            out.println("연결 성공!");

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("quit")) {
                    break;
                }

                String response;
                synchronized (engine) {
                    out.println("[" + engine.getEngineName() + "] 응답 생성 중...");
                    response = engine.chat(message);
                }
                out.println(response);
            }
        } catch (Exception e) {
            System.out.println("클라이언트 처리 중 오류: " + e.getMessage());
        }
    }
}
