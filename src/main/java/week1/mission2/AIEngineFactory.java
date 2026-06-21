package week1.mission2;

public class AIEngineFactory {

    /**
     * 엔진 타입에 맞는 AIEngine 구현체를 생성해 반환
     *
     * @param engineType 엔진 타입 ("gemini")
     * @return AIEngine 구현체
     * @throws IllegalArgumentException 알 수 없는 엔진 타입인 경우
     */
    public static AIEngine create(String engineType) {
        return switch (engineType.toLowerCase()) {
            case "gemini" -> new GeminiEngine();
            default -> throw new IllegalArgumentException("알 수 없는 엔진 타입: " + engineType);
        };
    }
}
