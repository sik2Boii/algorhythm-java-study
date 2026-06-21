package week1.mission2;

public interface AIEngine {

    /**
     * 메시지를 AI 엔진에 전송하고 응답을 반환
     *
     * @param message 사용자 메시지
     * @return AI 응답 텍스트
     */
    String chat(String message);

    /**
     * @return 엔진 이름 (예: "gemini", "openai")
     */
    String getEngineName();
}
