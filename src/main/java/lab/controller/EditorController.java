package lab.controller;

import lab.model.*;
import lab.utils.Constants;
import lab.utils.IdGenerator;
import lab.utils.Validator;

import java.util.Objects;

public final class EditorController {

    private final ProgramModel model;
    private int activeThreadIndex;
    private String selectedBlockId;
    private Runnable onModelChanged = () -> {};

    public EditorController(ProgramModel model) {
        this.model = Objects.requireNonNull(model);
    }

    public void setOnModelChanged(Runnable onModelChanged) {
        this.onModelChanged = onModelChanged != null ? onModelChanged : () -> {};
    }

    public ProgramModel model() {
        return model;
    }

    public int getActiveThreadIndex() {
        return activeThreadIndex;
    }

    public void setActiveThreadIndex(int activeThreadIndex) {
        this.activeThreadIndex = Math.max(0, Math.min(activeThreadIndex, Math.max(0, model.getThreads().size() - 1)));
        selectedBlockId = null;
    }

    public String getSelectedBlockId() {
        return selectedBlockId;
    }

    public void setSelectedBlockId(String selectedBlockId) {
        this.selectedBlockId = selectedBlockId;
    }

    public Flowchart activeChart() {
        if (model.getThreads().isEmpty()) {
            Flowchart fc = new Flowchart();
            fc.setName("Thread-1");
            model.getThreads().add(fc);
        }
        return model.getThreads().get(activeThreadIndex);
    }

    public Block addBlock(BlockType type, double x, double y) {
        Flowchart fc = activeChart();
        if (fc.getBlocks().size() >= Constants.MAX_BLOCKS_PER_CHART) {
            return null;
        }
        Block b = new Block();
        b.setId(IdGenerator.nextBlockId());
        b.setType(type);
        b.setLayoutX(x);
        b.setLayoutY(y);
        fc.getBlocks().add(b);
        touch();
        return b;
    }

    public void deleteSelected() {
        if (selectedBlockId == null) {
            return;
        }
        activeChart().removeBlock(selectedBlockId);
        selectedBlockId = null;
        touch();
    }

    public void moveBlock(String blockId, double x, double y) {
        activeChart().find(blockId).ifPresent(b -> {
            b.setLayoutX(x);
            b.setLayoutY(y);
            touch();
        });
    }

    public void updateSelectionType(BlockType type) {
        if (selectedBlockId == null) {
            return;
        }
        activeChart().find(selectedBlockId).ifPresent(b -> {
            b.setType(type);
            touch();
        });
    }

    public void updateSelectionCommand(String text) {
        if (selectedBlockId == null) {
            return;
        }
        activeChart().find(selectedBlockId).ifPresent(b -> {
            b.setCommandText(text != null ? text : "");
            touch();
        });
    }

    public void updateSelectionEdges(String nextId, String trueId, String falseId) {
        if (selectedBlockId == null) {
            return;
        }
        activeChart().find(selectedBlockId).ifPresent(b -> {
            if (b.getType() != BlockType.IF) {
                b.setNextTargetId(nextId);
            } else {
                b.setBranchTargets(trueId, falseId);
            }
            touch();
        });
    }

    public void setStartBlock(String id) {
        activeChart().setStartBlockId(id);
        touch();
    }

    public void addThreadIfAllowed() {
        if (model.getThreads().size() >= Constants.MAX_THREADS) {
            return;
        }
        Flowchart fc = new Flowchart();
        fc.setName("Thread-" + (model.getThreads().size() + 1));
        model.getThreads().add(fc);
        activeThreadIndex = model.getThreads().size() - 1;
        selectedBlockId = null;
        touch();
    }

    public void declareVariablesCsv(String csv) {
        model.setDeclaredVariableNamesFromCsv(csv);
        touch();
    }

    public Validator.ValidationReport validateAll() {
        return Validator.validate(model);
    }

    public void createBlankProject() {
        model.getThreads().clear();
        model.getTestCases().clear();
        model.getDeclaredVariableNames().clear();

        Flowchart fc = new Flowchart();
        fc.setName("Thread-1");
        Block s = new Block();
        s.setId(IdGenerator.nextBlockId());
        s.setType(BlockType.START);
        s.setLayoutX(80);
        s.setLayoutY(120);

        Block e = new Block();
        e.setId(IdGenerator.nextBlockId());
        e.setType(BlockType.END);
        e.setLayoutX(80);
        e.setLayoutY(320);

        s.setNextTargetId(e.getId());
        fc.getBlocks().add(s);
        fc.getBlocks().add(e);
        fc.setStartBlockId(s.getId());
        model.getThreads().add(fc);
        activeThreadIndex = 0;
        selectedBlockId = null;
        touch();
    }

    public void createDemoStarterProject() {
        model.getThreads().clear();
        model.getTestCases().clear();
        model.getDeclaredVariableNames().clear();
        model.setDeclaredVariableNamesFromCsv("w, x");

        Flowchart main = new Flowchart();
        main.setName("Thread-1 — приклад (INPUT, IF)");

        Block s = block(BlockType.START, 100, 50);
        Block inW = block(BlockType.INPUT, 100, 130, "w");
        Block ifW = block(BlockType.IF, 100, 210, "w<10");
        Block copy = block(BlockType.ASSIGN_VAR, 40, 290, "x=w");
        Block ifX = block(BlockType.IF, 40, 370, "x==5");
        Block printOk = block(BlockType.PRINT, 40, 450, "x");
        Block zero = block(BlockType.ASSIGN_CONST, 200, 290, "x=0");
        Block printOut = block(BlockType.PRINT, 200, 370, "x");
        Block end = block(BlockType.END, 100, 530);

        s.setNextTargetId(inW.getId());
        inW.setNextTargetId(ifW.getId());
        ifW.setBranchTargets(copy.getId(), zero.getId());
        copy.setNextTargetId(ifX.getId());
        ifX.setBranchTargets(printOk.getId(), zero.getId());
        printOk.setNextTargetId(end.getId());
        zero.setNextTargetId(printOut.getId());
        printOut.setNextTargetId(end.getId());
        main.setStartBlockId(s.getId());
        main.getBlocks().addAll(java.util.List.of(s, inW, ifW, copy, ifX, printOk, zero, printOut, end));

        Flowchart idle = new Flowchart();
        idle.setName("Thread-2 — порожній");
        Block s2 = block(BlockType.START, 480, 80);
        Block e2 = block(BlockType.END, 480, 200);
        s2.setNextTargetId(e2.getId());
        idle.setStartBlockId(s2.getId());
        idle.getBlocks().addAll(java.util.List.of(s2, e2));

        model.getThreads().add(main);
        model.getThreads().add(idle);

        TestCase tTrue = new TestCase();
        tTrue.setName("Гілка true (w<10, x=w, x==5)");
        tTrue.setStdin("5");
        tTrue.setExpectedStdout("5\n");

        TestCase tFalse = new TestCase();
        tFalse.setName("Гілка false (x=0)");
        tFalse.setStdin("12");
        tFalse.setExpectedStdout("0\n");

        model.getTestCases().add(tTrue);
        model.getTestCases().add(tFalse);

        activeThreadIndex = 0;
        selectedBlockId = null;
        touch();
    }

    private static Block block(BlockType type, double x, double y) {
        return block(type, x, y, "");
    }

    private static Block block(BlockType type, double x, double y, String cmd) {
        Block b = new Block();
        b.setId(IdGenerator.nextBlockId());
        b.setType(type);
        b.setLayoutX(x);
        b.setLayoutY(y);
        b.setCommandText(cmd != null ? cmd : "");
        return b;
    }

    public void bump() {
        touch();
    }

    private void touch() {
        onModelChanged.run();
    }
}
