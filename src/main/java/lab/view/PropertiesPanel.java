package lab.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import lab.controller.EditorController;
import lab.model.Block;
import lab.model.BlockType;

public final class PropertiesPanel extends VBox {

    private final EditorController editor;
    private final Runnable onModelTouch;

    private final ComboBox<BlockType> types = new ComboBox<>();
    private final TextField command = new TextField();
    private final TextField nextTarget = new TextField();
    private final TextField branchTrue = new TextField();
    private final TextField branchFalse = new TextField();
    private final Label cmdHint = new Label();
    private final Button setStartBt = new Button("Старт із вибраного");
    private boolean suppress;

    private final HBox rowNext = new HBox(8);
    private final HBox rowT = new HBox(8);
    private final HBox rowF = new HBox(8);

    public PropertiesPanel(EditorController editor, Runnable onModelTouch) {
        this.editor = editor;
        this.onModelTouch = onModelTouch != null ? onModelTouch : () -> {};
        setSpacing(10);
        setPadding(new Insets(12));
        getStyleClass().add("floating-panel");

        Label title = new Label("Властивості");
        title.setStyle("-fx-font-size:13px;-fx-font-weight:bold;");

        types.getItems().setAll(BlockType.values());
        rowNext.getChildren().addAll(new Label("next →"), nextTarget);
        HBox.setHgrow(nextTarget, javafx.scene.layout.Priority.ALWAYS);

        rowT.getChildren().addAll(new Label("true →"), branchTrue);
        HBox.setHgrow(branchTrue, javafx.scene.layout.Priority.ALWAYS);
        rowF.getChildren().addAll(new Label("false →"), branchFalse);
        HBox.setHgrow(branchFalse, javafx.scene.layout.Priority.ALWAYS);

        nextTarget.setPromptText("id наступного блока");
        branchTrue.setPromptText("id при true");
        branchFalse.setPromptText("id при false");
        command.setPromptText("команда або умова");

        types.valueProperty().addListener((a, o, v) -> syncTypeToModel());
        command.textProperty().addListener((a, o, v) -> {
            if (!suppress) {
                editor.updateSelectionCommand(command.getText());
                onModelTouch.run();
            }
        });
        nextTarget.textProperty().addListener((a, o, v) -> syncEdgesToModel());
        branchTrue.textProperty().addListener((a, o, v) -> syncEdgesToModel());
        branchFalse.textProperty().addListener((a, o, v) -> syncEdgesToModel());

        setStartBt.setMaxWidth(Double.MAX_VALUE);
        setStartBt.setOnAction(e -> {
            if (editor.getSelectedBlockId() != null) {
                editor.setStartBlock(editor.getSelectedBlockId());
                onModelTouch.run();
            }
        });

        getChildren().addAll(title, types, cmdHint, command, rowNext, rowT, rowF, setStartBt);

        nextTarget.setTooltip(new Tooltip("Джерело — ребро next (для всіх блоків, окрім IF)."));
        branchTrue.setTooltip(new Tooltip("IF: вихід при істинній умові."));
        branchFalse.setTooltip(new Tooltip("IF: вихід при хибній умові."));
    }

    private void syncTypeToModel() {
        if (suppress) {
            return;
        }
        if (types.getValue() != null) {
            editor.updateSelectionType(types.getValue());
            reloadFromSelection();
            onModelTouch.run();
        }
    }

    private void syncEdgesToModel() {
        if (suppress) {
            return;
        }
        editor.updateSelectionEdges(trimOrNull(nextTarget.getText()),
                trimOrNull(branchTrue.getText()),
                trimOrNull(branchFalse.getText()));
        onModelTouch.run();
    }

    private static String trimOrNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public void reloadFromSelection() {
        suppress = true;
        try {
            String sid = editor.getSelectedBlockId();
            if (sid == null) {
                types.setValue(null);
                command.clear();
                nextTarget.clear();
                branchTrue.clear();
                branchFalse.clear();
                disableAll(true);
                cmdHint.setText("");
                return;
            }
            disableAll(false);
            Block b = editor.activeChart().find(sid).orElse(null);
            if (b == null) {
                return;
            }
            cmdHint.setText(hint(b.getType()));
            types.setValue(b.getType());
            command.setText(b.getCommandText());

            boolean isIf = b.getType() == BlockType.IF;
            rowNext.setManaged(!isIf);
            rowNext.setVisible(!isIf);
            rowT.setManaged(isIf);
            rowT.setVisible(isIf);
            rowF.setManaged(isIf);
            rowF.setVisible(isIf);

            nextTarget.setText(b.getOutgoing().getOrDefault("next", ""));
            branchTrue.setText(b.getOutgoing().getOrDefault("true", ""));
            branchFalse.setText(b.getOutgoing().getOrDefault("false", ""));
        } finally {
            suppress = false;
        }
    }

    private void disableAll(boolean dis) {
        types.setDisable(dis);
        command.setDisable(dis);
        nextTarget.setDisable(dis);
        branchTrue.setDisable(dis);
        branchFalse.setDisable(dis);
        setStartBt.setDisable(dis);
        rowNext.setDisable(dis);
        rowT.setDisable(dis);
        rowF.setDisable(dis);
    }

    private static String hint(BlockType t) {
        return switch (t) {
            case ASSIGN_VAR -> "Приклад: x=y";
            case ASSIGN_CONST -> "Приклад: x=0";
            case INPUT -> "Ім'я змінної (спільна пам'ять)";
            case PRINT -> "Ім'я змінної для друку";
            case IF -> "Умова V==C або V<C (літерал без пробілів)";
            default -> "";
        };
    }
}
