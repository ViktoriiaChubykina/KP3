package lab.simulator;

import lab.model.ProgramModel;
import lab.model.TestCase;
import lab.model.TestResult;
import lab.utils.Validator;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class StateExplorer {

    private StateExplorer() {
    }

    public static TestResult exploreFull(ProgramModel model, TestCase test, int maxStepsK, AtomicBoolean cancel) {
        TestResult tr = new TestResult(test.getName());
        tr.setDepthLimitK(maxStepsK);
        var report = Validator.validate(model);
        if (!report.ok()) {
            tr.setFailed(true);
            tr.setMessage(String.join("\n", report.issues()));
            return tr;
        }
        List<Integer> stdin;
        try {
            stdin = Interpreter.parseStdin(test.getStdin());
        } catch (Exception ex) {
            tr.setFailed(true);
            tr.setMessage("Помилка розбору stdin тесту: " + ex.getMessage());
            return tr;
        }

        ExecutionState start = ExecutionState.initial(model, stdin);
        ArrayDeque<ExecutionState> dq = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        String exp = normalizeOut(test.getExpectedStdout());

        dq.add(start);
        seen.add(start.fingerprint());
        tr.setUniqueStatesVisited(seen.size());

        int stallDeadlocks = 0;

        while (!dq.isEmpty() && !cancel.get()) {
            ExecutionState cur = dq.removeFirst();

            if (cur.allTerminated()) {
                validateTerminal(exp, cur.stdout(), tr);
                continue;
            }

            if (cur.depth() >= maxStepsK) {
                continue;
            }

            List<Integer> ready = cur.readyThreads();
            boolean progressed = false;
            for (int tid : ready) {
                Interpreter.StepOutcome outcome = Interpreter.step(model, cur, tid);
                if (outcome instanceof Interpreter.Advance adv) {
                    ExecutionState nx = adv.next();
                    String fp = nx.fingerprint();
                    if (!seen.contains(fp)) {
                        seen.add(fp);
                        dq.addLast(nx);
                        progressed = true;
                    }
                    tr.setUniqueStatesVisited(seen.size());
                }
            }

            if (!progressed && !ready.isEmpty()) {
                stallDeadlocks++;
            }
        }

        if (cancel.get()) {
            tr.setInterrupted(true);
        }

        long visited = seen.size();
        int frontier = dq.size();
        double pct = 100.0 * visited / Math.max(1L, visited + frontier);
        if (!cancel.get() && frontier == 0) {
            pct = 100.0;
        }
        tr.setProgressPercentEstimate(Math.min(100.0, pct));

        if (stallDeadlocks > 0) {
            tr.setMessage(tr.getMessage() + "Стійки стани (deadlock/stall) при розгортанні: " + stallDeadlocks + ".\n");
        }

        if (tr.getIncorrectTerminatingTraces() > 0) {
            tr.setFailed(true);
        } else if (!cancel.get()) {
            if (tr.getTerminatingTracesChecked() == 0) {
                tr.setFailed(true);
                tr.setMessage(tr.getMessage() + "Не знайдено завершень усіх потоків із ≤ " + maxStepsK + " кроками. Збільште K або перевірте схему.\n");
            } else if (tr.getCorrectTerminatingTraces() != tr.getTerminatingTracesChecked()) {
                tr.setFailed(true);
            }
        }

        return tr;
    }

    private static void validateTerminal(String expectedNorm, String actualRaw, TestResult tr) {
        String act = normalizeOut(actualRaw);
        tr.setTerminatingTracesChecked(tr.getTerminatingTracesChecked() + 1);
        tr.getDistinctOutputs().add(act);
        if (expectedNorm.equals(act)) {
            tr.setCorrectTerminatingTraces(tr.getCorrectTerminatingTraces() + 1);
        } else {
            tr.setIncorrectTerminatingTraces(tr.getIncorrectTerminatingTraces() + 1);
        }
    }

    private static String normalizeOut(String s) {
        return s == null ? "" : s.trim();
    }
}
