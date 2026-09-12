import java.util.Random;

public class Main {
    private static final int WIDTH = 54;
    private static final int HEIGHT = 24;
    private static final int DELAY_MS = 110;

    public static void main(String[] args) throws Exception {
        boolean[][] world = new boolean[HEIGHT][WIDTH];
        Random random = new Random();

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                world[y][x] = random.nextDouble() < 0.26;
            }
        }

        while (true) {
            clearScreen();
            draw(world);
            world = nextGeneration(world);
            Thread.sleep(DELAY_MS);
        }
    }

    private static boolean[][] nextGeneration(boolean[][] current) {
        boolean[][] next = new boolean[HEIGHT][WIDTH];

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int neighbors = countNeighbors(current, x, y);

                if (current[y][x]) {
                    next[y][x] = neighbors == 2 || neighbors == 3;
                } else {
                    next[y][x] = neighbors == 3;
                }
            }
        }

        return next;
    }

    private static int countNeighbors(boolean[][] world, int x, int y) {
        int count = 0;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }

                int nx = (x + dx + WIDTH) % WIDTH;
                int ny = (y + dy + HEIGHT) % HEIGHT;

                if (world[ny][nx]) {
                    count++;
                }
            }
        }

        return count;
    }

    private static void draw(boolean[][] world) {
        StringBuilder frame = new StringBuilder();
        frame.append("Conway's Game of Life  |  Ctrl+C to stop\n\n");

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                frame.append(world[y][x] ? "██" : "  ");
            }
            frame.append('\n');
        }

        System.out.print(frame);
    }

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
