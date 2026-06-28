package week2.mission2;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatSession {

    private final int userId;
    private final Socket socket;
    private final PrintWriter writer;

    public ChatSession(int userId, Socket socket) throws IOException {
        this.userId = userId;
        this.socket = socket;
        this.writer = new PrintWriter(new BufferedOutputStream(socket.getOutputStream()), false);
    }

    public void send(String message) {
        writer.println(message);
        writer.flush();
    }

    public void close() throws IOException {
        writer.close();
        socket.close();
    }

    public int getUserId() { return userId; }
}
