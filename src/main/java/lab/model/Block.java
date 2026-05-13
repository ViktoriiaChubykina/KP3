package lab.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class Block {
    private String id;
    private BlockType type = BlockType.START;
    private String commandText = "";
    private double layoutX = 60;
    private double layoutY = 80;

    private Map<String, String> outgoing = new LinkedHashMap<>();

    public Block() {
    }

    public Block copy() {
        Block b = new Block();
        b.id = this.id;
        b.type = this.type;
        b.commandText = this.commandText;
        b.layoutX = this.layoutX;
        b.layoutY = this.layoutY;
        b.outgoing = new LinkedHashMap<>(this.outgoing);
        return b;
    }

    public String getNextTargetIdOrNull() {
        return outgoing.get("next");
    }

    public void setNextTargetId(String idOrNull) {
        if (idOrNull == null || idOrNull.isBlank()) {
            outgoing.remove("next");
        } else {
            outgoing.put("next", idOrNull.trim());
        }
    }

    public String getTrueTargetIdOrNull() {
        return outgoing.get("true");
    }

    public String getFalseTargetIdOrNull() {
        return outgoing.get("false");
    }

    public void setBranchTargets(String trueTarget, String falseTarget) {
        if (trueTarget != null && !trueTarget.isBlank()) {
            outgoing.put("true", trueTarget.trim());
        } else {
            outgoing.remove("true");
        }
        if (falseTarget != null && !falseTarget.isBlank()) {
            outgoing.put("false", falseTarget.trim());
        } else {
            outgoing.remove("false");
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BlockType getType() {
        return type;
    }

    public void setType(BlockType type) {
        this.type = type;
    }

    public String getCommandText() {
        return commandText;
    }

    public void setCommandText(String commandText) {
        this.commandText = commandText != null ? commandText : "";
    }

    public double getLayoutX() {
        return layoutX;
    }

    public void setLayoutX(double layoutX) {
        this.layoutX = layoutX;
    }

    public double getLayoutY() {
        return layoutY;
    }

    public void setLayoutY(double layoutY) {
        this.layoutY = layoutY;
    }

    public Map<String, String> getOutgoing() {
        return outgoing;
    }

    public void setOutgoing(Map<String, String> outgoing) {
        this.outgoing = outgoing != null ? new LinkedHashMap<>(outgoing) : new LinkedHashMap<>();
    }

    public String displayLabel() {
        return switch (type) {
            case START -> "START";
            case END -> "END";
            case ASSIGN_CONST, ASSIGN_VAR -> commandText.isBlank() ? "ASSIGN" : commandText;
            case INPUT -> commandText.isBlank() ? "INPUT" : "INPUT " + commandText.trim();
            case PRINT -> commandText.isBlank() ? "PRINT" : "PRINT " + commandText.trim();
            case IF -> commandText.isBlank() ? "IF" : "IF " + commandText.trim();
        };
    }
}
