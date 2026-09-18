import java.util.Random;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random random = new Random();

        int secret = random.nextInt(100) + 1;
        int attempts = 0;

        System.out.println("I picked a number from 1 to 100.");
        System.out.println("Can you guess it?");

        while (true) {
            System.out.print("Your guess: ");
            int guess = scanner.nextInt();
            attempts++;

            if (guess < secret) {
                System.out.println("Too small.");
            } else if (guess > secret) {
                System.out.println("Too big.");
            } else {
                System.out.println("Correct! Attempts: " + attempts);
                break;
            }
        }

        scanner.close();
    }
}
