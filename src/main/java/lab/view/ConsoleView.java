package lab.view;

import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class ConsoleView extends VBox {

    private final TextArea area = new TextArea();

    public ConsoleView() {
        area.setEditable(false);
        area.setWrapText(true);
        VBox.setVgrow(area, Priority.ALWAYS);
        getChildren().add(area);
        setSpacing(6);
        setPrefHeight(180);
        area.setPromptText("Вивід симуляції, генерації коду, повідомлень тестів…");
    }

    public void clear() {
        area.clear();
    }

    public void append(String text) {
        area.appendText(text.endsWith("\n") ? text : text + "\n");
    }

    public TextArea delegate() {
        return area;
    }
}
