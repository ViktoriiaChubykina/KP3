package lab.simulator;

import java.util.*;

public final class Scheduler {
    private int rr;
    private final Random rnd = new Random();

    private Scheduler() {
    }

    public static Scheduler newDefault() {
        return new Scheduler();
    }

    public int roundRobin(Collection<Integer> readyThreadIndices) {
        List<Integer> list = new ArrayList<>(readyThreadIndices);
        Collections.sort(list);
        if (list.isEmpty()) {
            return -1;
        }
        rr = rr % Math.max(list.size(), 1);
        int picked = list.get(rr % list.size());
        rr = (rr + 1) % list.size();
        return picked;
    }

    public int randomPick(Collection<Integer> ready) {
        if (ready.isEmpty()) {
            return -1;
        }
        ArrayList<Integer> list = new ArrayList<>(ready);
        return list.get(rnd.nextInt(list.size()));
    }
}
