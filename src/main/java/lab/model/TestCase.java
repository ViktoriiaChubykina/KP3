package lab.model;

public class TestCase {
    private String name = "Тест";
    private String stdin = "";
    private String expectedStdout = "";

    public TestCase() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getStdin() {
        return stdin;
    }

    public void setStdin(String stdin) {
        this.stdin = stdin != null ? stdin : "";
    }

    public String getExpectedStdout() {
        return expectedStdout;
    }

    public void setExpectedStdout(String expectedStdout) {
        this.expectedStdout = expectedStdout != null ? expectedStdout : "";
    }
}
