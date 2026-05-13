package lab.utils;

import lab.model.*;

import java.util.*;

public final class Validator {

    private Validator() {
    }

    public record ValidationReport(boolean ok, List<String> issues) {}

    public static ValidationReport validate(ProgramModel prog) {
        List<String> issues = new ArrayList<>();
        if (prog == null) {
            issues.add("Модель програми не задана.");
            return new ValidationReport(false, issues);
        }
        int n = prog.getThreads().size();
        if (n < 1 || n > Constants.MAX_THREADS) {
            issues.add("Кількість потоків повинна бути в межах 1…" + Constants.MAX_THREADS + " (зараз: " + n + ").");
        }
        int varGuess = prog.collectUsedIdentifiers().size();
        if (varGuess > Constants.MAX_VARIABLES) {
            issues.add("Виявлено більше " + Constants.MAX_VARIABLES + " різних ідентифікаторів.");
        }

        Set<String> allVarNames = new TreeSet<>(prog.getDeclaredVariableNames());
        allVarNames.addAll(prog.collectUsedIdentifiers());
        if (allVarNames.size() > Constants.MAX_VARIABLES) {
            issues.add("Загальна кількість унікальних імен змінних перевищує " + Constants.MAX_VARIABLES + ".");
        }

        for (int i = 0; i < n; i++) {
            Flowchart fc = prog.getThreads().get(i);
            validateFlowchart(fc, i, issues);
        }
        return new ValidationReport(issues.isEmpty(), issues);
    }

    private static void validateFlowchart(Flowchart fc, int index, List<String> issues) {
        if (fc.getBlocks().size() > Constants.MAX_BLOCKS_PER_CHART) {
            issues.add("Потік " + (index + 1) + ": більше " + Constants.MAX_BLOCKS_PER_CHART + " блоків.");
        }
        if (fc.getStartBlockId() == null || fc.getStartBlockId().isBlank()) {
            issues.add("Потік " + (index + 1) + ": не вказано початковий блок.");
            return;
        }
        Map<String, Block> byId = new HashMap<>();
        for (Block b : fc.getBlocks()) {
            if (b.getId() == null || b.getId().isBlank()) {
                issues.add("Потік " + (index + 1) + ": є блок без id.");
                continue;
            }
            byId.put(b.getId(), b);
        }
        if (!byId.containsKey(fc.getStartBlockId())) {
            issues.add("Потік " + (index + 1) + ": початковий id не існує серед блоків.");
        }
        for (Block b : fc.getBlocks()) {
            for (Map.Entry<String, String> e : b.getOutgoing().entrySet()) {
                String tgt = e.getValue();
                if (tgt != null && !tgt.isBlank() && !byId.containsKey(tgt)) {
                    issues.add("Потік " + (index + 1) + ": з блока " + b.getId()
                            + " дуга \"" + e.getKey() + "\" веде в неіснуючий блок " + tgt + ".");
                }
            }
            if (b.getType() == BlockType.IF) {
                if (b.getOutgoing().get("true") == null || b.getOutgoing().get("true").isBlank()
                        || b.getOutgoing().get("false") == null || b.getOutgoing().get("false").isBlank()) {
                    issues.add("Потік " + (index + 1) + ": блок IF " + b.getId() + " має задати виходи true та false.");
                }
            }
        }
    }
}
