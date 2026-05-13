package lab.model;

import java.util.*;

public final class TestResult {
    private final String testName;
    private boolean failed;
    private String message = "";
    private final LinkedHashSet<String> distinctOutputs = new LinkedHashSet<>();
    private int terminatingTracesChecked;
    private int correctTerminatingTraces;
    private int incorrectTerminatingTraces;
    private long uniqueStatesVisited;
    private double progressPercentEstimate;
    private boolean interrupted;
    private int depthLimitK;

    public TestResult(String testName) {
        this.testName = testName;
    }

    public String getTestName() {
        return testName;
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message != null ? message : "";
    }

    public LinkedHashSet<String> getDistinctOutputs() {
        return distinctOutputs;
    }

    public int getTerminatingTracesChecked() {
        return terminatingTracesChecked;
    }

    public void setTerminatingTracesChecked(int terminatingTracesChecked) {
        this.terminatingTracesChecked = terminatingTracesChecked;
    }

    public int getCorrectTerminatingTraces() {
        return correctTerminatingTraces;
    }

    public void setCorrectTerminatingTraces(int correctTerminatingTraces) {
        this.correctTerminatingTraces = correctTerminatingTraces;
    }

    public int getIncorrectTerminatingTraces() {
        return incorrectTerminatingTraces;
    }

    public void setIncorrectTerminatingTraces(int incorrectTerminatingTraces) {
        this.incorrectTerminatingTraces = incorrectTerminatingTraces;
    }

    public long getUniqueStatesVisited() {
        return uniqueStatesVisited;
    }

    public void setUniqueStatesVisited(long uniqueStatesVisited) {
        this.uniqueStatesVisited = uniqueStatesVisited;
    }

    public double getProgressPercentEstimate() {
        return progressPercentEstimate;
    }

    public void setProgressPercentEstimate(double progressPercentEstimate) {
        this.progressPercentEstimate = progressPercentEstimate;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }

    public int getDepthLimitK() {
        return depthLimitK;
    }

    public void setDepthLimitK(int depthLimitK) {
        this.depthLimitK = depthLimitK;
    }
}
