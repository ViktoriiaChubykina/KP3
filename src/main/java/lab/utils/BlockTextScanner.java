package lab.utils;

import lab.model.Block;
import java.util.Set;
import java.util.regex.Pattern;

public final class BlockTextScanner {
    private static final Pattern VAR = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    private BlockTextScanner() {
    }

    public static void collectFromBlock(Block b, Set<String> out) {
        if (b == null || out == null) {
            return;
        }
        String t = b.getCommandText() != null ? b.getCommandText().trim() : "";
        if (t.isEmpty()) {
            return;
        }
        switch (b.getType()) {
            case ASSIGN_VAR -> {
                int eq = t.indexOf('=');
                if (eq > 0) {
                    maybeAdd(t.substring(0, eq), out);
                    maybeAdd(t.substring(eq + 1), out);
                }
            }
            case ASSIGN_CONST -> {
                int eq = t.indexOf('=');
                if (eq > 0) {
                    maybeAdd(t.substring(0, eq), out);
                }
            }
            case INPUT -> maybeAddWord(t, out);
            case PRINT -> maybeAddWord(t, out);
            case IF -> extractIfVars(t, out);
            default -> {
            }
        }
    }

    private static void extractIfVars(String t, Set<String> out) {
        if (t.contains("==")) {
            String[] p = t.split("==");
            maybeAddWord(p.length > 0 ? p[0] : "", out);
            return;
        }
        if (t.contains("<")) {
            String[] p = t.split("<");
            maybeAddWord(p.length > 0 ? p[0] : "", out);
        }
    }

    private static void maybeAdd(String raw, Set<String> out) {
        if (raw == null) {
            return;
        }
        VAR.matcher(raw.replace(" ", "")).results().forEach(mr -> out.add(mr.group()));
    }

    private static void maybeAddWord(String raw, Set<String> out) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        out.add(raw.trim());
    }
}
