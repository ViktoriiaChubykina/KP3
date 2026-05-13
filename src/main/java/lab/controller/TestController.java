package lab.controller;

import javafx.application.Platform;
import lab.model.ProgramModel;
import lab.model.TestCase;
import lab.model.TestResult;
import lab.simulator.StateExplorer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;


public final class TestController {

    private TestController() {
    }

    public static CompletableFuture<TestResult> exploreAsync(
            ProgramModel model,
            TestCase tc,
            int maxStepsK,
            AtomicBoolean cancelled) {
        CompletableFuture<TestResult> f = new CompletableFuture<>();
        Thread t = new Thread(() -> {
            try {
                TestResult r = StateExplorer.exploreFull(model, tc, maxStepsK, cancelled);
                Platform.runLater(() -> f.complete(r));
            } catch (Exception ex) {
                Platform.runLater(() -> f.completeExceptionally(ex));
            }
        }, "flowchart-state-explorer");
        t.setDaemon(true);
        t.start();
        return f;
    }
}
