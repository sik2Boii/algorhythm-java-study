package week1.mission1;

import java.util.Scanner;

public class WrapperOverheadBenchmark {

    private static final int SIZE = 1_000_000;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== 벤치마킹 시작 ===");
        System.out.println("VisualVM에서 이 프로세스를 연결한 후 엔터를 누르세요.");
        scanner.nextLine();

        // JVM 초기값 (배열 생성 전 기본 힙 사용량)
        double initialHeap = getHeapUsageMB();
        System.out.printf("[초기값] Used Heap: %.2f MB%n", initialHeap);

        // 1단계: double[] (primitive)
        System.out.println("[1단계] double[] 생성 중...");
        double[] primitiveArray = new double[SIZE];
        for (int i = 0; i < SIZE; i++) {
            primitiveArray[i] = Math.random();
        }
        double primitiveHeap = getHeapUsageMB();

        System.out.printf("[double[]] Used Heap: %.2f MB  |  순수 사용량: %.2f MB%n",
                primitiveHeap, primitiveHeap - initialHeap);
        System.out.println("double[] 생성 완료 - VisualVM에서 힙 수치를 확인한 후 엔터를 누르세요.");

        scanner.nextLine();

        // 2단계: Double[] (Wrapper 객체)
        System.out.println("[2단계] Double[] 생성 중...");
        Double[] wrapperArray = new Double[SIZE];
        for (int i = 0; i < SIZE; i++) {
            wrapperArray[i] = Math.random();
        }
        double wrapperHeap = getHeapUsageMB();

        System.out.printf("[Double[]] Used Heap: %.2f MB  |  순수 사용량: %.2f MB%n",
                wrapperHeap, wrapperHeap - primitiveHeap);
        System.out.println("Double[] 생성 완료 - VisualVM에서 힙 수치를 확인한 후 엔터를 누르세요.");

        scanner.nextLine();

        System.out.printf("%n=== 최종 비교 ===%n");
        System.out.printf("double[] 순수 사용량: %.2f MB%n", primitiveHeap - initialHeap);
        System.out.printf("Double[] 순수 사용량: %.2f MB%n", wrapperHeap - primitiveHeap);
        System.out.printf("메모리 배율: %.1f배%n", (wrapperHeap - primitiveHeap) / (primitiveHeap - initialHeap));

        // GC 방지용
        System.out.println("=== 측정 완료 ===");
        System.out.println("primitive 첫 번째 값: " + primitiveArray[0]);
        System.out.println("wrapper 첫 번째 값: " + wrapperArray[0]);

        scanner.close();
    }

    /**
     * 현재 JVM 힙 사용량을 MB 단위로 반환
     *
     * @return 현재 사용 중인 힙 메모리 (MB)
     */
    private static double getHeapUsageMB() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        return used / 1024.0 / 1024.0;
    }
}
