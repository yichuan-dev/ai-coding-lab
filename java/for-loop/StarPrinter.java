public class StarPrinter {
    public static void main(String[] args) {
        System.out.println("Experiment 01: growing triangle");

        for (int row = 1; row <= 5; row++) {
            for (int star = 1; star <= row; star++) {
                System.out.print("*");
            }
            System.out.println();
        }

        System.out.println();
        System.out.println("Experiment 02: shrinking triangle");

        for (int row = 5; row >= 1; row--) {
            for (int star = 1; star <= row; star++) {
                System.out.print("*");
            }
            System.out.println();
        }

        // Try changing 5 into 10.
        // Then try replacing "*" with "#" or "Java ".
    }
}
