public class GuessTheOutput {
    public static void main(String[] args) {
        System.out.println("=== AI Coding Lab: Guess The Output ===");

        // Challenge 1: 先别运行，猜猜会打印什么。
        for (int i = 1; i <= 5; i++) {
            System.out.print(i + " ");
        }
        System.out.println();

        // Challenge 2: i 每次不是 +1，而是 +2。
        for (int i = 2; i <= 10; i += 2) {
            System.out.print(i + " ");
        }
        System.out.println();

        // Challenge 3: 倒着走。
        for (int i = 5; i >= 1; i--) {
            System.out.print(i + " ");
        }
        System.out.println();

        System.out.println("Done. Now change one loop and see what breaks.");
    }
}
