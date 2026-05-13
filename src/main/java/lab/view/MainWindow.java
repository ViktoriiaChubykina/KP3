package lab.view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import lab.controller.EditorController;
import lab.controller.FileController;
import lab.controller.RunController;
import lab.controller.TestController;
import lab.model.ProgramModel;
import lab.model.TestCase;
import lab.translator.JavaCodeGenerator;
import lab.utils.Constants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MainWindow extends Application {

    private final ProgramModel program = new ProgramModel();
    private final EditorController editor = new EditorController(program);
    private final FileController fileController = new FileController(editor);
    private final RunController runController = new RunController();

    private CanvasView canvas;
    private PropertiesPanel props;
    private ConsoleView consoleView;
    private TextField stdinField;
    private TextField declaredVarsField;
    private TextField testNameField;
    private TextArea testStdinField;
    private TextArea testExpectedField;
    private Spinner<Integer> testDepthSpinner;

    private ListView<String> threadNames;
    private boolean syncingThreads;

    @Override
    public void start(Stage stage) {
        editor.createDemoStarterProject();

        threadNames = new ListView<>();
        threadNames.setPrefWidth(160);

        props = new PropertiesPanel(editor, this::fullRefresh);
        canvas = new CanvasView(editor, () -> props.reloadFromSelection());
        editor.setOnModelChanged(this::fullRefresh);

        consoleView = new ConsoleView();

        ToolbarView toolbar = new ToolbarView(bt -> canvas.setAddMode(bt));
        stdinField = new TextField();
        stdinField.setPromptText("stdin (цілі через пробіл, для Reset/Step)");

        SplitPane vertical = new SplitPane(buildMainSplit(), buildBottomSplit());
        vertical.setOrientation(Orientation.VERTICAL);
        vertical.setDividerPositions(0.68);

        HBox stdinRow = new HBox(12, new Label("stdin"), stdinField);
        stdinField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(stdinField, Priority.ALWAYS);
        stdinRow.setPadding(new Insets(4, 8, 4, 8));

        VBox top = new VBox(buildMenuBar(stage), toolbar, stdinRow);
        BorderPane root = new BorderPane();
        root.setCenter(vertical);
        root.setTop(top);

        Scene scene = new Scene(root, 1260, 800);
        String css = """
                .root { -fx-base: #2d2d30; -fx-font-family: "Segoe UI", system-ui, sans-serif; }
                .menu-bar { -fx-background-color: derive(-fx-base,6%); }
                .label { -fx-text-fill: #e8e8e8; }
                .text-field, .text-area, .combo-box-base, .spinner { -fx-control-inner-background: #1e1e1e; }
                .list-view { -fx-background-color: #252526; }
                .floating-panel { -fx-background-radius:10; }
                .button { -fx-background-radius:12; }
                TextArea { -fx-text-fill: #e0e0e0; }
                """;
        try {
            scene.getStylesheets().add("data:text/css," + URLEncoder.encode(css, StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }

        stage.setTitle("Багатопотокові блок-схеми (KP3)");
        stage.setScene(scene);
        syncUiFromDemoModel();
        stage.show();
        fullRefresh();
        consoleView.append("Завантажено стартовий приклад: Thread-1 (INPUT w → IF w<10 → x=w → IF x==5 → PRINT), Thread-2 (START→END), тести внизу.");
        consoleView.append("Підказка: тулбар → тип блока → клік по полотну; з’єднання — next / true / false у властивостях. Симуляція: меню «Симуляція» — Reset з полем stdin, потім Step.");
    }

    /** Підставляє в поля інтерфейсу дані з поточної моделі (оголошення, перший тест, stdin для кроків). */
    private void syncUiFromDemoModel() {
        declaredVarsField.setText(program.declaredVariablesAsCsv());
        if (!program.getTestCases().isEmpty()) {
            TestCase tc = program.getTestCases().get(0);
            testNameField.setText(tc.getName());
            testStdinField.setText(tc.getStdin());
            testExpectedField.setText(tc.getExpectedStdout());
        }
        stdinField.setText("5");
    }

    private void syncThreadNames() {
        syncingThreads = true;
        try {
            threadNames.getItems().clear();
            editor.model().getThreads().forEach(t -> threadNames.getItems().add(t.getName()));
            int i = editor.getActiveThreadIndex();
            if (!threadNames.getItems().isEmpty()) {
                int idx = Math.min(i, threadNames.getItems().size() - 1);
                threadNames.getSelectionModel().clearAndSelect(idx);
            }
        } finally {
            syncingThreads = false;
        }
    }

    private void fullRefresh() {
        syncThreadNames();
        canvas.render();
        props.reloadFromSelection();
    }

    private MenuBar buildMenuBar(Stage stage) {
        FileChooser fcOpen = new FileChooser();
        fcOpen.setTitle("Відкрити проєкт");
        fcOpen.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        FileChooser fcSave = new FileChooser();
        fcSave.setTitle("Зберегти проєкт");
        fcSave.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));

        Menu file = new Menu("Файл");
        MenuItem fresh = new MenuItem("Новий");
        MenuItem open = new MenuItem("Відкрити…");
        MenuItem save = new MenuItem("Зберегти як…");
        MenuItem quit = new MenuItem("Вихід");
        fresh.setOnAction(e -> fileController.newBlankProject());
        open.setOnAction(e -> {
            File f = fcOpen.showOpenDialog(stage);
            if (f != null) {
                Path p = f.toPath();
                try {
                    fileController.load(p);
                    consoleView.append("Відкрито: " + p);
                } catch (Exception ex) {
                    consoleView.append("Помилка: " + ex.getMessage());
                }
            }
        });
        save.setOnAction(e -> {
            File f = fcSave.showSaveDialog(stage);
            if (f != null) {
                Path p = f.toPath();
                try {
                    fileController.save(p);
                    consoleView.append("Збережено: " + p);
                } catch (Exception ex) {
                    consoleView.append("Помилка: " + ex.getMessage());
                }
            }
        });
        quit.setOnAction(e -> Platform.exit());

        Menu code = new Menu("Трансляція");
        MenuItem gen = new MenuItem("Згенерувати Java…");
        gen.setOnAction(e -> exportJava(stage));
        Menu sim = new Menu("Симуляція");
        MenuItem reset = new MenuItem("Reset");
        MenuItem step = new MenuItem("Step");
        reset.setOnAction(e -> {
            try {
                runController.reset(editor.model(), stdinField.getText());
                highlightRun();
                consoleView.append("[reset] готово до кроків.");
            } catch (Exception ex) {
                consoleView.append("Reset помилка: " + ex.getMessage());
            }
        });
        step.setOnAction(e -> {
            RunController.StepResult r = runController.stepOnce(editor.model());
            highlightRun();
            if (r.issue() != null) {
                consoleView.append("Step зупинився: " + r.issue());
            } else if (r.state() != null && r.state().allTerminated()) {
                consoleView.append("[завершено] stdout:\n" + r.state().stdout());
            }
        });

        Menu edit = new Menu("Редагування");
        MenuItem del = new MenuItem("Видалити вибраний блок");
        del.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("DELETE"));
        del.setOnAction(e -> {
            editor.deleteSelected();
            fullRefresh();
        });

        file.getItems().addAll(fresh, open, save, new SeparatorMenuItem(), quit);
        code.getItems().add(gen);
        sim.getItems().addAll(reset, step);
        edit.getItems().add(del);
        return new MenuBar(file, edit, sim, code);
    }

    private void highlightRun() {
        var s = runController.getCurrentOrNull();
        canvas.setHighlightPc(s != null ? s.pcSnapshot() : null);
        fullRefresh();
    }

    /** Ліва колонка списків потоків, центр полотно, права форма блока. */
    private SplitPane buildMainSplit() {
        Button add = new Button("+ потік");

        threadNames.getSelectionModel().selectedIndexProperty().addListener((a, ov, nv) -> {
            if (syncingThreads || nv == null || nv.intValue() < 0) {
                return;
            }
            editor.setActiveThreadIndex(nv.intValue());
            fullRefresh();
        });

        declaredVarsField = new TextField();
        declaredVarsField.setPromptText("Оголошені спільні змінні через кому…");
        declaredVarsField.setOnAction(e -> editor.declareVariablesCsv(declaredVarsField.getText()));
        declaredVarsField.textProperty().addListener((ob, ox, vx) -> editor.declareVariablesCsv(vx));

        add.setOnAction(e -> editor.addThreadIfAllowed());

        VBox leftCol = new VBox(10, add, declaredVarsField, threadNames);
        VBox.setVgrow(threadNames, Priority.ALWAYS);

        SplitPane hz = new SplitPane(leftCol, canvas, props);
        hz.setDividerPositions(0.14, 0.78);
        return hz;
    }

    /** Тестові поля поруч із консоллю. */
    private SplitPane buildBottomSplit() {
        testDepthSpinner = new Spinner<>(1, Constants.MAX_TEST_DEPTH_K, Math.min(12, Constants.MAX_TEST_DEPTH_K));

        testNameField = new TextField("Тест 1");
        testStdinField = new TextArea();
        testStdinField.setPromptText("stdin: цифри або перенесення рядків як розділювачі");
        testExpectedField = new TextArea();
        testExpectedField.setPromptText("очікуваний stdout целиком (порівняння trim)");
        testDepthSpinner.setEditable(true);

        AtomicBoolean cancelled = new AtomicBoolean(false);
        Button go = new Button("Повний перебір (до K)");
        Button stopBtn = new Button("Зупинити");
        Button saveTest = new Button("У проєкт");

        Label stat = new Label();
        stat.setWrapText(true);
        stat.setStyle("-fx-text-fill:#aaa;-fx-padding:8;");

        go.setOnAction(e -> {
            TestCase tc = new TestCase();
            tc.setName(testNameField.getText().isBlank() ? "Тест" : testNameField.getText());
            tc.setStdin(testStdinField.getText());
            tc.setExpectedStdout(testExpectedField.getText());
            int kk = clampK(testDepthSpinner.getValue());
            cancelled.set(false);
            consoleView.append("———— Перебір «" + tc.getName() + "», глибина ≤ " + kk);
            stat.setText("Йде виконання…");
            TestController.exploreAsync(editor.model(), tc, kk, cancelled).whenComplete((tr, er) ->
                    Platform.runLater(() -> {
                        if (er != null) {
                            consoleView.append("Помилка: " + er.getMessage());
                            stat.setText("Помилка.");
                            return;
                        }
                        printResult(tr);
                        stat.setText(String.format(Locale.ROOT,
                                "Станів: %d | Завершень: %d | Коректних: %d — Оцінка %% перебору: %.2f",
                                tr.getUniqueStatesVisited(),
                                tr.getTerminatingTracesChecked(),
                                tr.getCorrectTerminatingTraces(),
                                tr.getProgressPercentEstimate()));
                        if (cancelled.get()) {
                            stat.setText(stat.getText() + " | (перервано користувачем)");
                        }
                    }));
        });
        stopBtn.setOnAction(e -> cancelled.set(true));
        saveTest.setOnAction(e -> {
            TestCase tc = new TestCase();
            tc.setName(testNameField.getText().isBlank() ? "Тест" : testNameField.getText());
            tc.setStdin(testStdinField.getText());
            tc.setExpectedStdout(testExpectedField.getText());
            program.getTestCases().removeIf(x -> tc.getName().equals(x.getName()));
            program.getTestCases().add(tc);
            consoleView.append("Тест «" + tc.getName() + "» збережено у проекті.");
        });

        GridPane gp = new GridPane();
        gp.setHgap(8);
        gp.setVgap(6);
        int r = 0;
        gp.addRow(r++, new Label("Назва тесту"), testNameField);
        gp.addRow(r++, new Label("K (1–20)"), testDepthSpinner);
        gp.addRow(r++, new Label("stdin"), testStdinField);
        gp.addRow(r++, new Label("Очікуваний stdout"), testExpectedField);

        VBox tst = new VBox(8, new Label("Перебір міжпотокових сценаріїв"), gp, new HBox(8, go, stopBtn, saveTest), stat);
        tst.setPadding(new Insets(8));

        SplitPane hz = new SplitPane(tst, consoleView);
        hz.setDividerPositions(0.38);
        return hz;
    }

    private int clampK(int v) {
        return Math.max(1, Math.min(Constants.MAX_TEST_DEPTH_K, v));
    }

    private void printResult(lab.model.TestResult tr) {
        consoleView.append("Унікальних станів: " + tr.getUniqueStatesVisited());
        consoleView.append("Знайдених завершень усіх потоків: " + tr.getTerminatingTracesChecked());
        consoleView.append("Коректних / некоректних: " + tr.getCorrectTerminatingTraces()
                + " / " + tr.getIncorrectTerminatingTraces());
        consoleView.append("Різних виводів: " + tr.getDistinctOutputs().size());
        if (!tr.getDistinctOutputs().isEmpty()) {
            consoleView.append(tr.getDistinctOutputs().toString());
        }
        consoleView.append(String.format(Locale.ROOT, "Оцінка %% перевірки простору (стани + черга): %.2f", tr.getProgressPercentEstimate()));
        if (!tr.getMessage().isBlank()) {
            consoleView.append(tr.getMessage());
        }
        consoleView.append(tr.isFailed() ? "Результат: НЕ ок" : "Результат: ок (для всіх знайдених завершень)");
    }

    private void exportJava(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Експорт GeneratedProgram.java");
        fc.setInitialFileName("GeneratedProgram.java");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java", "*.java"));
        File f = fc.showSaveDialog(stage);
        if (f == null) {
            return;
        }
        Path p = f.toPath();
        try {
            Files.writeString(p, JavaCodeGenerator.generate(program), StandardCharsets.UTF_8);
            consoleView.append("Java записано: " + p);
        } catch (IOException ex) {
            consoleView.append("Помилка Java: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
