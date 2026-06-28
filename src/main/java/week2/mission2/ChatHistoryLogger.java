package week2.mission2;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatHistoryLogger implements Closeable {

    private static final int BUFFER_SIZE = 8192;  // 8KB 청크 — 시스템 콜 최소화
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BufferedOutputStream bos;

    public ChatHistoryLogger(String logFilePath) throws IOException {
        this.bos = new BufferedOutputStream(new FileOutputStream(logFilePath, true), BUFFER_SIZE);
    }

    /**
     * 유저 메시지와 AI 응답을 청크 단위로 로그 파일에 플러시
     *
     * synchronized로 멀티스레드 환경에서 로그가 섞이지 않도록 보장한다.
     *
     * @param userId   메시지를 보낸 유저 ID
     * @param message  유저가 보낸 메시지
     * @param response AI 응답
     */
    public synchronized void log(int userId, String message, String response) throws IOException {
        String entry = String.format("[%s] userId=%d | Q: %s | A: %s%n",
                LocalDateTime.now().format(FORMATTER), userId, message, response);
        bos.write(entry.getBytes(StandardCharsets.UTF_8));
        bos.flush();
    }

    @Override
    public void close() throws IOException {
        bos.close();
    }
}
