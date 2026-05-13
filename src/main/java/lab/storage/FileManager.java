package lab.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lab.model.ProgramModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private FileManager() {
    }

    public static void save(Path path, ProgramModel model) throws IOException {
        String json = GSON.toJson(model);
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    public static ProgramModel load(Path path) throws IOException {
        String json = Files.readString(path, StandardCharsets.UTF_8);
        ProgramModel m = GSON.fromJson(json, ProgramModel.class);
        if (m == null) {
            return new ProgramModel();
        }
        if (m.getThreads().isEmpty()) {
            var fc = new lab.model.Flowchart();
            fc.setName("Thread-1");
            m.getThreads().add(fc);
        }
        return m;
    }
}
