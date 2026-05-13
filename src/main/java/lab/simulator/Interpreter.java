package lab.simulator;

import lab.model.*;

import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Pattern;

public final class Interpreter {

    private static final Pattern ASSIGN_VAR = Pattern.compile("^\\s*([a-zA-Z_]\\w*)\\s*=\\s*([a-zA-Z_]\\w*)\\s*$");
    private static final Pattern ASSIGN_CONST = Pattern.compile("^\\s*([a-zA-Z_]\\w*)\\s*=\\s*(\\d{1,10})\\s*$");
    private static final Pattern INPUT_P = Pattern.compile("^\\s*([a-zA-Z_]\\w*)\\s*$");
    private static final Pattern PRINT_P = Pattern.compile("^\\s*([a-zA-Z_]\\w*)\\s*$");
    private static final Pattern EQ = Pattern.compile("^\\s*([a-zA-Z_]\\w*)\\s*==\\s*(\\d{1,10})\\s*$");
    private static final Pattern PAT_LT = Pattern.compile("^\\s*([a-zA-Z_]\\w*)\\s*<\\s*(\\d{1,10})\\s*$");

    private Interpreter() {
    }

    public sealed interface StepOutcome permits Advance, Stall {
    }

    public record Advance(ExecutionState next) implements StepOutcome {
    }

    public record Stall(String reason) implements StepOutcome {
    }

    public static StepOutcome step(ProgramModel model, ExecutionState cur, int threadIdx) {
        if (threadIdx < 0 || threadIdx >= model.getThreads().size()) {
            return new Stall("невірний індекс потоку");
        }
        Flowchart fc = model.getThreads().get(threadIdx);
        String[] pcs = cur.pcSnapshot();
        if (pcs[threadIdx] == null || pcs[threadIdx].isBlank()) {
            return new Stall("потік уже завершений");
        }
        Optional<Block> ob = fc.find(pcs[threadIdx]);
        if (ob.isEmpty()) {
            return new Stall("блок не знайдено: " + pcs[threadIdx]);
        }
        Block b = ob.get();

        SharedMemory mem = SharedMemory.copyOf(cur.memorySnapshot());
        String stdout = cur.stdout();
        int stdinC = cur.stdinCursor();
        ArrayList<Integer> stdinBuf = new ArrayList<>(cur.stdinValuesSnapshot());

        BlockType t = b.getType();
        return switch (t) {
            case START -> advanceNext(fc, b, pcs, threadIdx, mem, stdout, stdinC, stdinBuf, cur.depth());
            case END -> {
                pcs[threadIdx] = null;
                yield safeAdvance(mem, pcs, stdout, stdinC, stdinBuf, cur.depth() + 1);
            }
            case ASSIGN_CONST -> execAssignConst(b, fc, pcs, threadIdx, mem, stdout, stdinC, stdinBuf, cur.depth());
            case ASSIGN_VAR -> execAssignVar(b, fc, pcs, threadIdx, mem, stdout, stdinC, stdinBuf, cur.depth());
            case INPUT -> execInput(b, fc, pcs, threadIdx, mem, stdout, stdinC, stdinBuf, cur.depth());
            case PRINT -> execPrint(b, fc, pcs, threadIdx, mem, stdout, stdinC, stdinBuf, cur.depth());
            case IF -> execIf(fc, b, pcs, threadIdx, mem, stdout, stdinC, stdinBuf, cur.depth());
        };
    }

    private static StepOutcome execAssignConst(Block b, Flowchart fc, String[] pcs, int tid,
                                               SharedMemory mem, String stdout, int stdinC, ArrayList<Integer> stdinBuf, int depth) {
        var m = ASSIGN_CONST.matcher(b.getCommandText());
        if (!m.matches()) {
            return new Stall("ASSIGN_CONST: очікується V=C, отримано: «" + b.getCommandText() + "»");
        }
        String var = m.group(1);
        int val = parseLit(m.group(2));
        mem.put(var, val);
        return advanceNext(fc, b, pcs, tid, mem, stdout, stdinC, stdinBuf, depth);
    }

    private static StepOutcome execAssignVar(Block b, Flowchart fc, String[] pcs, int tid,
                                             SharedMemory mem, String stdout, int stdinC, ArrayList<Integer> stdinBuf, int depth) {
        var m = ASSIGN_VAR.matcher(b.getCommandText());
        if (!m.matches()) {
            return new Stall("ASSIGN_VAR: очікується V1=V2, отримано: «" + b.getCommandText() + "»");
        }
        String v1 = m.group(1);
        String v2 = m.group(2);
        mem.put(v1, mem.getOrZero(v2));
        return advanceNext(fc, b, pcs, tid, mem, stdout, stdinC, stdinBuf, depth);
    }

    private static StepOutcome execInput(Block b, Flowchart fc, String[] pcs, int tid,
                                         SharedMemory mem, String stdout, int stdinC, ArrayList<Integer> stdinBuf, int depth) {
        var m = INPUT_P.matcher(b.getCommandText());
        if (!m.matches()) {
            return new Stall("INPUT: очікується ім’я змінної, отримано: «" + b.getCommandText() + "»");
        }
        String var = m.group(1);
        if (stdinC >= stdinBuf.size()) {
            return new Stall("INPUT: бракує числа на stdin");
        }
        stdinC++;
        mem.put(var, stdinBuf.get(stdinC - 1));
        return advanceNext(fc, b, pcs, tid, mem, stdout, stdinC, stdinBuf, depth);
    }

    private static StepOutcome execPrint(Block b, Flowchart fc, String[] pcs, int tid,
                                         SharedMemory mem, String stdout, int stdinC, ArrayList<Integer> stdinBuf, int depth) {
        var m = PRINT_P.matcher(b.getCommandText());
        if (!m.matches()) {
            return new Stall("PRINT: очікується ім’я змінної, отримано: «" + b.getCommandText() + "»");
        }
        String var = m.group(1);
        String line = stdout + mem.getOrZero(var) + "\n";
        return advanceNext(fc, b, pcs, tid, mem, line, stdinC, stdinBuf, depth);
    }

    private static StepOutcome execIf(Flowchart fc, Block b, String[] pcs, int tid, SharedMemory mem,
                                      String stdout, int stdinC, ArrayList<Integer> stdinBuf, int depth) {
        Optional<Boolean> cmp = evaluateIf(b.getCommandText(), mem);
        if (cmp.isEmpty()) {
            return new Stall("IF: не розпізнано умову («" + b.getCommandText() + "»). Допустимо: V==C або V<C");
        }
        String edgeKey = cmp.get() ? "true" : "false";
        String edge = b.getOutgoing().get(edgeKey);
        if (edge == null || edge.isBlank()) {
            return new Stall("IF: не задано вихід " + edgeKey);
        }
        if (fc.find(edge).isEmpty()) {
            return new Stall("IF: блок " + edge + " не існує");
        }
        pcs[tid] = edge;
        return safeAdvance(mem, pcs, stdout, stdinC, stdinBuf, depth + 1);
    }

    private static Optional<Boolean> evaluateIf(String commandText, SharedMemory mem) {
        String t = commandText != null ? commandText.trim() : "";
        var eq = EQ.matcher(t);
        if (eq.matches()) {
            String v = eq.group(1);
            int c = parseLit(eq.group(2));
            return Optional.of(mem.getOrZero(v) == c);
        }
        var lt = PAT_LT.matcher(t);
        if (lt.matches()) {
            String v = lt.group(1);
            int c = parseLit(lt.group(2));
            return Optional.of(mem.getOrZero(v) < c);
        }
        return Optional.empty();
    }

    /** Перехід уздовж орієнтовного ребра {@code next} після успішної обробки поточного блока. */
    private static StepOutcome advanceNext(Flowchart fc, Block cur, String[] pcs, int tid,
                                           SharedMemory mem, String stdout, int stdinC, ArrayList<Integer> stdinBuf, int depthDoneSoFar) {
        String next = cur.getOutgoing().get("next");
        if (next == null || next.isBlank()) {
            return new Stall("нема переходу 'next' з блока " + cur.getId() + " типу " + cur.getType());
        }
        if (fc.find(next).isEmpty()) {
            return new Stall("нема блока за id " + next);
        }
        pcs[tid] = next;
        return safeAdvance(mem, pcs, stdout, stdinC, stdinBuf, depthDoneSoFar + 1);
    }

    private static Advance safeAdvance(SharedMemory mem, String[] pcs, String stdout,
                                       int stdinC, ArrayList<Integer> stdinBuf, int nextDepth) {
        ExecutionState nx = new ExecutionState(pcs, mem, stdinBuf, stdinC, stdout, nextDepth);
        return new Advance(nx);
    }

    private static int parseLit(String s) {
        long v = Long.parseLong(s.trim());
        if (v < 0 || v > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("константа поза межами");
        }
        return (int) v;
    }

    /** Перетворює текст stdin у список int (пробільні символи — розділювачі). */
    public static ArrayList<Integer> parseStdin(String raw) {
        ArrayList<Integer> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        String normalized = raw.replace('\r', ' ').replace('\t', '\n').replace(',', ' ');
        for (String line : normalized.split("\\s+|\\v+")) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            long v = Long.parseLong(t, 10);
            if (v < 0 || v > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("вхідне число поза 0.." + Integer.MAX_VALUE + ": " + t);
            }
            out.add((int) v);
        }
        return out;
    }
}
