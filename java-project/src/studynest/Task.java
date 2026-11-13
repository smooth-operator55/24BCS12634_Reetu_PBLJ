package studynest;

import java.time.LocalDate;
import java.util.UUID;

/** A task's data, kept separate from the interface and storage. */
public class Task {
    public enum Priority { HIGH, MEDIUM, LOW }

    private final String id;
    private final String title;
    private final String subject;
    private final LocalDate deadline;
    private final Priority priority;
    private boolean completed;

    public Task(String title, String subject, LocalDate deadline, Priority priority) {
        this(UUID.randomUUID().toString(), title, subject, deadline, priority, false);
    }

    public Task(String id, String title, String subject, LocalDate deadline,
                Priority priority, boolean completed) {
        this.id = id;
        this.title = title;
        this.subject = subject;
        this.deadline = deadline;
        this.priority = priority;
        this.completed = completed;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSubject() { return subject; }
    public LocalDate getDeadline() { return deadline; }
    public Priority getPriority() { return priority; }
    public boolean isCompleted() { return completed; }
    public void toggleCompleted() { completed = !completed; }
}
