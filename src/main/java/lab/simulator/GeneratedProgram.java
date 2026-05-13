// Автоматично згенеровано середовищем блок-схем
import java.util.Scanner;

public final class GeneratedProgram {

    private static final Scanner IN = new Scanner(System.in);

    private static volatile int w;
    private static volatile int x;

    private static final class Worker1 extends Thread {
        @Override public void run() {
            String cur = "b1";
            while (cur != null) {
                switch (cur) {
                    case "b1" -> {
                        /* START */
                        cur = "b2";
                    }
                    case "b2" -> {
                        w = IN.nextInt();
                        cur = "b3";
                    }
                    case "b3" -> {
                        if (w < 10L) {
                            cur = "b4";
                        } else {
                            cur = "b7";
                        }
                    }
                    case "b4" -> {
                        x = w;
                        cur = "b5";
                    }
                    case "b5" -> {
                        if (x == 5L) {
                            cur = "b6";
                        } else {
                            cur = "b7";
                        }
                    }
                    case "b6" -> {
                        System.out.println(x);
                        cur = "b9";
                    }
                    case "b7" -> {
                        x = 0;
                        cur = "b8";
                    }
                    case "b8" -> {
                        System.out.println(x);
                        cur = "b9";
                    }
                    case "b9" -> {
                        cur = null;
                    }
                    default -> throw new IllegalStateException("Unknown block " + cur);
                }
            }
        }
    }

    private static final class Worker2 extends Thread {
        @Override public void run() {
            String cur = "b10";
            while (cur != null) {
                switch (cur) {
                    case "b10" -> {
                        /* START */
                        cur = "b11";
                    }
                    case "b11" -> {
                        cur = null;
                    }
                    default -> throw new IllegalStateException("Unknown block " + cur);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Thread t1 = new Worker1();
        Thread t2 = new Worker2();
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }
}
