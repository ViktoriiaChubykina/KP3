package lab.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import lab.model.BlockType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class ToolbarView extends ScrollPane {

    private final Consumer<BlockType> onPickType;

    public ToolbarView(Consumer<BlockType> onPickType) {
        this.onPickType = onPickType;
        HBox row = new HBox(10);
        row.setPadding(new Insets(6, 10, 6, 10));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: derive(-fx-base,-8%);");

        Map<String, BlockType> m = new LinkedHashMap<>();
        m.put("START", BlockType.START);
        m.put("END", BlockType.END);
        m.put("V=V", BlockType.ASSIGN_VAR);
        m.put("V=C", BlockType.ASSIGN_CONST);
        m.put("INPUT", BlockType.INPUT);
        m.put("PRINT", BlockType.PRINT);
        m.put("IF", BlockType.IF);

        for (var e : m.entrySet()) {
            Button bt = pill(e.getKey());
            bt.setOnAction(ev -> this.onPickType.accept(e.getValue()));
            row.getChildren().add(bt);
        }

        Label hint = new Label("Клік по полотню після інструменту — додати блок");
        hint.setStyle("-fx-text-fill: gray;");
        row.getChildren().add(hint);
        setFitToHeight(true);
        setFitToWidth(true);
        setContent(row);
    }

    private static Button pill(String text) {
        Button b = new Button(text);
        b.setTooltip(new Tooltip("Додати «" + text + "» на схему"));
        b.setStyle("-fx-background-radius:16;-fx-padding:6 14;-fx-background-insets:0;");
        return b;
    }
}
