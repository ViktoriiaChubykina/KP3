package lab.controller;

import lab.model.ProgramModel;
import lab.simulator.ExecutionState;
import lab.simulator.Interpreter;
import lab.simulator.Scheduler;

import java.util.Objects;

public final class RunController {

    private ExecutionState current;
    private final Scheduler scheduler = Scheduler.newDefault();

    public void reset(ProgramModel model, String stdinText) {
        Objects.requireNonNull(model);
        var stdin = Interpreter.parseStdin(stdinText);
        this.current = ExecutionState.initial(model, stdin);
    }

    public StepResult stepOnce(ProgramModel model) {
        Objects.requireNonNull(model);
        if (current == null) {
            return new StepResult(null, false, "Симуляцію ще не ініціалізовано (Reset).");
        }
        if (current.allTerminated()) {
            return new StepResult(current, false, null);
        }
        var ready = current.readyThreads();
        if (ready.isEmpty()) {
            return new StepResult(current, false, "Немає готових потоків (deadlock?).");
        }
        int tid = scheduler.roundRobin(ready);
        Interpreter.StepOutcome o = Interpreter.step(model, current, tid);
        if (o instanceof Interpreter.Stall stall) {
            return new StepResult(current, false, stall.reason());
        }
        current = ((Interpreter.Advance) o).next();
        return new StepResult(current, true, null);
    }

    public ExecutionState getCurrentOrNull() {
        return current;
    }

    public record StepResult(ExecutionState state, boolean continuedStep, String issue) {}
}
