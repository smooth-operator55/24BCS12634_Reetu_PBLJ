package studynest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** UTF-8 file persistence. Text fields are encoded so tabs and Unicode are safe. */
public class TaskStore {
    private final Path file;

    public TaskStore(Path file) { this.file = file; }

    public List<Task> load() throws IOException {
        List<Task> tasks = new ArrayList<>();
        if (!Files.exists(file)) return tasks;
        int lineNumber = 0;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            lineNumber++;
            if (line.isBlank()) continue;
            try {
                String[] fields = line.split("\t", -1);
                if (fields.length != 6) throw new IllegalArgumentException("Expected six fields");
                if (!fields[5].equals("true") && !fields[5].equals("false")) {
                    throw new IllegalArgumentException("Invalid completion status");
                }
                tasks.add(new Task(fields[0], decode(fields[1]), decode(fields[2]),
                        LocalDate.parse(fields[3]), Task.Priority.valueOf(fields[4]),
                        Boolean.parseBoolean(fields[5])));
            } catch (RuntimeException error) {
                throw new IOException("Could not read task on line " + lineNumber
                        + ". The original file has been preserved.", error);
            }
        }
        return tasks;
    }

    public void save(List<Task> tasks) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Task task : tasks) {
            lines.add(String.join("\t", task.getId(), encode(task.getTitle()),
                    encode(task.getSubject()), task.getDeadline().toString(),
                    task.getPriority().name(), Boolean.toString(task.isCompleted())));
        }
        Path absolute = file.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        Path temporary = Files.createTempFile(absolute.getParent(), "studynest-", ".tmp");
        try {
            Files.write(temporary, lines, StandardCharsets.UTF_8);
            Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String text) {
        return new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
    }
}
