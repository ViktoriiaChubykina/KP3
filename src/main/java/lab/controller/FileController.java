package lab.controller;

import lab.model.ProgramModel;
import lab.storage.FileManager;

import java.nio.file.Path;
import java.util.Objects;

public final class FileController {

    private final EditorController editor;

    public FileController(EditorController editor) {
        this.editor = Objects.requireNonNull(editor);
    }

    public void newBlankProject() {
        editor.createBlankProject();
    }

    public void save(Path path) throws Exception {
        FileManager.save(path, editor.model());
    }

    public void load(Path path) throws Exception {
        ProgramModel loaded = FileManager.load(path);
        ProgramModel cur = editor.model();
        cur.getThreads().clear();
        cur.getTestCases().clear();
        cur.getDeclaredVariableNames().clear();
        cur.getThreads().addAll(loaded.getThreads());
        cur.getTestCases().addAll(loaded.getTestCases());
        cur.getDeclaredVariableNames().addAll(loaded.getDeclaredVariableNames());
        editor.setActiveThreadIndex(0);
        editor.setSelectedBlockId(null);
        editor.bump();
    }
}
