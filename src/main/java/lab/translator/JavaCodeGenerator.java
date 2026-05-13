package lab.translator;

import lab.model.*;

import java.util.LinkedHashSet;
import java.util.Set;

public final class JavaCodeGenerator {

    private JavaCodeGenerator() {
    }

    public static String generate(ProgramModel program) {
        Set<String> vars = new LinkedHashSet<>(program.getDeclaredVariableNames());
        vars.addAll(program.collectUsedIdentifiers());
        StringBuilder sb = new StringBuilder(8096);
        sb.append("// Автоматично згенеровано середовищем блок-схем\n");
        sb.append("import java.util.Scanner;\n\n");
        sb.append("public final class GeneratedProgram {\n\n");
        sb.append("    private static final Scanner IN = new Scanner(System.in);\n");
        if (hasPrintBlock(program)) {
            sb.append("    private static final Object PRINT_LOCK = new Object();\n");
        }
        sb.append("\n");
        for (String v : vars) {
            sb.append("    private static volatile int ").append(safe(v)).append(";\n");
        }
        sb.append("\n");
        for (int i = 0; i < program.getThreads().size(); i++) {
            Flowchart fc = program.getThreads().get(i);
            String className = "Worker" + (i + 1);
            sb.append("    private static final class ").append(className).append(" extends Thread {\n");
            sb.append("        @Override public void run() {\n");
            sb.append("            String cur = \"").append(escape(fc.getStartBlockId())).append("\";\n");
            sb.append("            while (cur != null) {\n");
            sb.append("                switch (cur) {\n");
            for (Block b : fc.getBlocks()) {
                appendBlockCase(sb, fc, b);
            }
            sb.append("                    default -> throw new IllegalStateException(\"Unknown block \" + cur);\n");
            sb.append("                }\n");
            sb.append("            }\n");
            sb.append("        }\n");
            sb.append("    }\n\n");
        }
        sb.append("    public static void main(String[] args) throws Exception {\n");
        for (int i = 0; i < program.getThreads().size(); i++) {
            sb.append("        Thread t").append(i + 1).append(" = new Worker").append(i + 1).append("();\n");
        }
        for (int i = 0; i < program.getThreads().size(); i++) {
            sb.append("        t").append(i + 1).append(".start();\n");
        }
        for (int i = 0; i < program.getThreads().size(); i++) {
            sb.append("        t").append(i + 1).append(".join();\n");
        }
        sb.append("    }\n}\n");
        return sb.toString();
    }

    private static void appendBlockCase(StringBuilder sb, Flowchart fc, Block b) {
        String id = escape(b.getId());
        sb.append("                    case \"").append(id).append("\" -> {\n");
        switch (b.getType()) {
            case START, ASSIGN_CONST, ASSIGN_VAR, INPUT, PRINT -> {
                sb.append(generateStmt(b));
                String nx = nextId(b, fc, "next");
                if (nx == null) {
                    sb.append("                        throw new IllegalStateException(\"no next edge from ").append(id).append("\");\n");
                } else {
                    sb.append("                        cur = \"").append(nx).append("\";\n");
                }
                sb.append("                    }\n");
            }
            case IF -> {
                String cond = buildJavaCondition(b.getCommandText());
                String tBranch = escape(b.getOutgoing().get("true"));
                String fBranch = escape(b.getOutgoing().get("false"));
                sb.append("                        if (").append(cond).append(") {\n");
                sb.append("                            cur = \"").append(nullToEmpty(tBranch)).append("\";\n");
                sb.append("                        } else {\n");
                sb.append("                            cur = \"").append(nullToEmpty(fBranch)).append("\";\n");
                sb.append("                        }\n");
                sb.append("                    }\n");
            }
            case END -> sb.append("                        cur = null;\n                    }\n");
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String buildJavaCondition(String commandText) {
        String t = commandText != null ? commandText.trim() : "";
        if (t.contains("==")) {
            String[] p = t.split("==", 2);
            String v = safe(p[0].trim());
            long c = Long.parseLong(p[1].trim());
            return v + " == " + c + "L";
        }
        if (t.contains("<")) {
            String[] p = t.split("<", 2);
            String v = safe(p[0].trim());
            long c = Long.parseLong(p[1].trim());
            return v + " < " + c + "L";
        }
        return "false";
    }

    private static String generateStmt(Block b) {
        return switch (b.getType()) {
            case START -> "                        /* START */\n";
            case ASSIGN_CONST -> {
                String[] p = splitAssign(b.getCommandText());
                yield "                        " + safe(p[0]) + " = " + p[1] + ";\n";
            }
            case ASSIGN_VAR -> {
                String[] p = splitAssign(b.getCommandText());
                yield "                        " + safe(p[0]) + " = " + safe(p[1]) + ";\n";
            }
            case INPUT ->
                    "                        " + safe(stripWs(b.getCommandText())) + " = IN.nextInt();\n";
            case PRINT -> {
                String v = safe(stripWs(b.getCommandText()));
                yield "                        synchronized (PRINT_LOCK) {\n"
                        + "                            System.out.println(" + v + ");\n"
                        + "                        }\n";
            }
            default -> "";
        };
    }

    private static String[] splitAssign(String t) {
        String[] x = (t != null ? t : "").split("=");
        if (x.length != 2) {
            throw new IllegalArgumentException("assignment");
        }
        return new String[]{x[0].trim(), x[1].trim()};
    }

    private static String stripWs(String s) {
        return s != null ? s.trim() : "";
    }

    private static String nextId(Block b, Flowchart fc, String key) {
        String nid = b.getOutgoing().get(key);
        if (nid == null || nid.isBlank() || fc.find(nid).isEmpty()) {
            return null;
        }
        return escape(nid);
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static boolean hasPrintBlock(ProgramModel program) {
        for (Flowchart fc : program.getThreads()) {
            for (Block b : fc.getBlocks()) {
                if (b.getType() == BlockType.PRINT) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String safe(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("empty identifier");
        }
        if (!name.matches("[a-zA-Z_]\\w*")) {
            throw new IllegalArgumentException("invalid identifier: " + name);
        }
        return name;
    }
}
