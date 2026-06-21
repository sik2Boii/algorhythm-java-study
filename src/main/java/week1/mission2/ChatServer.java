package week1.mission2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer {

    private static final int PORT = 9999;

    public static void main(String[] args) throws Exception {
        // 기본 엔진은 gemini
        AIEngine engine = AIEngineFactory.create("gemini");
        System.out.println("=== AI 채팅 서버 시작 (포트: " + PORT + ") ===");
        System.out.println("현재 엔진: " + engine.getEngineName());

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                System.out.println("\n클라이언트 연결 대기 중...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("클라이언트 연결됨: " + clientSocket.getInetAddress());

                // 단일 스레드 - 한 번에 한 클라이언트만 처리
                engine = handleClient(clientSocket, engine);
            }
        }
    }

    private static AIEngine handleClient(Socket clientSocket, AIEngine engine) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            out.println("연결 성공! 현재 엔진: " + engine.getEngineName());
            out.println("명령어: SWITCH:gemini | quit");

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("[수신] " + message);

                if (message.equalsIgnoreCase("quit")) {
                    out.println("연결을 종료합니다.");
                    break;
                }

                // 엔진 스위칭 명령어 처리
                if (message.startsWith("SWITCH:")) {
                    String engineType = message.substring(7).trim();
                    try {
                        engine = AIEngineFactory.create(engineType);
                        out.println("[서버] 엔진 전환 완료: " + engine.getEngineName());
                        System.out.println("엔진 전환: " + engine.getEngineName());
                    } catch (IllegalArgumentException e) {
                        out.println("[서버] " + e.getMessage());
                    }
                    continue;
                }

                // AI 응답 요청
                out.println("[" + engine.getEngineName() + "] 응답 생성 중...");
                String response = engine.chat(message);
                out.println(response);
            }

        } catch (Exception e) {
            System.out.println("클라이언트 처리 중 오류: " + e.getMessage());
        }

        return engine;
    }
}
