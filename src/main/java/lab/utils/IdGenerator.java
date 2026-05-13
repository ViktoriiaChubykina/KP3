package lab.utils;

import java.util.concurrent.atomic.AtomicLong;

public final class IdGenerator {
    private static final AtomicLong COUNTER = new AtomicLong(1);

    private IdGenerator() {
    }

    public static String nextBlockId() {
        return "b" + COUNTER.getAndIncrement();
    }

    public static void seed(long v) {
        COUNTER.set(v);
    }
}
