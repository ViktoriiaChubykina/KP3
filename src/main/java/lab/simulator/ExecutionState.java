package lab.simulator;

import lab.model.ProgramModel;
import lab.model.SharedMemory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ExecutionState {
    private final String[] programCounter;
    private final SharedMemory memory;
    private final List<Integer> stdinValues;
    private final int stdinCursor;
    private final String stdout;
    private final int depth;

    public ExecutionState(String[] pc, SharedMemory memory, List<Integer> stdinValues, int stdinCursor, String stdout, int depth) {
        this.programCounter = pc != null ? pc.clone() : new String[0];
        this.memory = SharedMemory.copyOf(memory);
        this.stdinValues = stdinValues != null ? new ArrayList<>(stdinValues) : new ArrayList<>();
        this.stdinCursor = stdinCursor;
        this.stdout = stdout != null ? stdout : "";
        this.depth = depth;
    }

    public int depth() {
        return depth;
    }

    public String fingerprint() {
        StringBuilder fb = new StringBuilder(96);
        fb.append(Arrays.toString(programCounter)).append('|');
        fb.append(stdinCursor).append('|').append(stdout).append('|');
        memory.getValues().entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .forEach(e -> fb.append(e.getKey()).append('=').append(e.getValue()).append(';'));
        return fb.toString();
    }

    public String[] pcSnapshot() {
        return programCounter.clone();
    }

    public SharedMemory memorySnapshot() {
        return SharedMemory.copyOf(memory);
    }

    public List<Integer> stdinValuesSnapshot() {
        return new ArrayList<>(stdinValues);
    }

    public int stdinCursor() {
        return stdinCursor;
    }

    public String stdout() {
        return stdout;
    }

    public boolean allTerminated() {
        for (String s : programCounter) {
            if (s != null && !s.isBlank()) {
                return false;
            }
        }
        return true;
    }

    public List<Integer> readyThreads() {
        List<Integer> r = new ArrayList<>();
        for (int i = 0; i < programCounter.length; i++) {
            if (programCounter[i] != null && !programCounter[i].isBlank()) {
                r.add(i);
            }
        }
        return r;
    }

    public static ExecutionState initial(ProgramModel prog, List<Integer> stdin) {
        int n = prog.getThreads().size();
        String[] pc = new String[n];
        for (int i = 0; i < n; i++) {
            pc[i] = prog.getThreads().get(i).getStartBlockId();
        }
        return new ExecutionState(pc, new SharedMemory(), stdin, 0, "", 0);
    }
}
