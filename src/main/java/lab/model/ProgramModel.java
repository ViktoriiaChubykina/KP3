package lab.model;

import lab.utils.BlockTextScanner;

import java.util.*;


public class ProgramModel {
    private final List<Flowchart> threads = new ArrayList<>();
    private final List<TestCase> testCases = new ArrayList<>();
    private final LinkedHashSet<String> declaredVariableNames = new LinkedHashSet<>();

    public List<Flowchart> getThreads() {
        return threads;
    }

    public LinkedHashSet<String> getDeclaredVariableNames() {
        return declaredVariableNames;
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }

    public void setDeclaredVariableNamesFromCsv(String csv) {
        declaredVariableNames.clear();
        if (csv == null) {
            return;
        }
        for (String p : csv.split("[,;\s]+")) {
            String t = p.trim();
            if (!t.isEmpty()) {
                declaredVariableNames.add(t);
            }
        }
    }

    public String declaredVariablesAsCsv() {
        return String.join(", ", declaredVariableNames);
    }

    public Flowchart chartForIndex(int idx) {
        return threads.get(idx);
    }

    public ProgramModel copyShallowCharts() {
        ProgramModel pm = new ProgramModel();
        pm.declaredVariableNames.addAll(this.declaredVariableNames);
        for (Flowchart fc : threads) {
            Flowchart nfc = new Flowchart();
            nfc.setName(fc.getName());
            nfc.setStartBlockId(fc.getStartBlockId());
            for (Block b : fc.getBlocks()) {
                nfc.getBlocks().add(b.copy());
            }
            pm.getThreads().add(nfc);
        }
        return pm;
    }

    public Set<String> collectUsedIdentifiers() {
        Set<String> s = new TreeSet<>(declaredVariableNames);
        for (Flowchart fc : threads) {
            for (Block b : fc.getBlocks()) {
                BlockTextScanner.collectFromBlock(b, s);
            }
        }
        return s;
    }
}
