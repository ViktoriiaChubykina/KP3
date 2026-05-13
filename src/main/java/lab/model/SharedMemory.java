package lab.model;

import java.util.HashMap;
import java.util.Map;

public final class SharedMemory {
    private final Map<String, Integer> values = new HashMap<>();

    public Map<String, Integer> getValues() {
        return values;
    }

    public static SharedMemory copyOf(SharedMemory src) {
        SharedMemory m = new SharedMemory();
        if (src != null) {
            m.values.putAll(src.values);
        }
        return m;
    }

    public int getOrZero(String var) {
        return values.getOrDefault(var, 0);
    }

    public void put(String var, int v) {
        values.put(var, v);
    }
}
