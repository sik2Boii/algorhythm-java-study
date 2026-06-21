package week1.mission2;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Google Gemini API를 호출하는 AI 엔진 구현체
 *
 * GEMINI_API_KEY 환경변수가 설정되어 있어야 한다.
 */
public class GeminiEngine implements AIEngine {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";
    private static final Pattern RESPONSE_PATTERN = Pattern.compile("\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final HttpClient httpClient;
    private final String apiKey;

    public GeminiEngine() {
        this.httpClient = HttpClient.newHttpClient();
        this.apiKey = System.getenv("GEMINI_API_KEY");
    }

    @Override
    public String chat(String message) {
        if (apiKey == null || apiKey.isBlank()) {
            return "[Gemini 오류] GEMINI_API_KEY 환경변수가 설정되어 있지 않습니다.";
        }

        String requestBody = String.format("""
                {
                  "contents": [{"parts": [{"text": "%s"}]}]
                }
                """, message.replace("\"", "\\\""));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_URL + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parseResponse(response.body());

        } catch (Exception e) {
            return "[Gemini 오류] " + e.getMessage();
        }
    }

    private String parseResponse(String json) {
        Matcher matcher = RESPONSE_PATTERN.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).replace("\\n", "\n");
        }
        return "[응답 파싱 실패]";
    }

    @Override
    public String getEngineName() {
        return "gemini";
    }
}
