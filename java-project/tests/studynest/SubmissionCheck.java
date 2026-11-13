package studynest;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Dependency-free integration checks. Run in a disposable working directory. */
public class SubmissionCheck {
    private static int checks;
    private static Path screenshots;

    public static void main(String[] args) throws Exception {
        screenshots = Path.of(args[1]);
        Files.createDirectories(screenshots);
        if (args[0].equals("restart")) {
            List<Task> restored = new TaskStore(Path.of("data/tasks.tsv")).load();
            check(restored.size() == 2, "Fresh JVM restores two tasks");
            Task edited = restored.stream().filter(t -> t.getTitle().equals("Submit Java project")).findFirst().orElseThrow();
            check(edited.isCompleted() && edited.getPriority() == Task.Priority.HIGH
                    && edited.getSubject().equals("Java Programming")
                    && edited.getDeadline().equals(LocalDate.now().plusDays(3)), "Edited fields and completion survive process restart");
            SwingUtilities.invokeAndWait(() -> {
                try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
                catch (Exception error) { throw new RuntimeException(error); }
                StudyNest app = new StudyNest(new ArrayList<>(restored));
                app.setVisible(true);
                capture(app, "03-after-restart.png");
                check(text(app).contains("1 of 2 tasks complete"), "Restarted interface shows restored progress");
                app.dispose();
            });
        } else {
            storageChecks();
            SwingUtilities.invokeAndWait(SubmissionCheck::interfaceChecks);
        }
        System.out.println("PASS: " + checks + " checks (" + args[0] + ")");
        System.exit(0);
    }

    private static void storageChecks() throws Exception {
        TaskStore store = new TaskStore(Path.of("storage-check/tasks.tsv"));
        check(store.load().isEmpty(), "Missing file loads as empty");
        Task task = new Task("Unicode café\t作业", "Java\nLab", LocalDate.now(), Task.Priority.LOW);
        task.toggleCompleted();
        store.save(List.of(task));
        Task loaded = store.load().get(0);
        check(loaded.getId().equals(task.getId()) && loaded.getTitle().equals(task.getTitle())
                && loaded.getSubject().equals(task.getSubject()) && loaded.isCompleted(), "Unicode, delimiters, identity and completion round trip");
        store.save(List.of());
        check(store.load().isEmpty(), "Empty list persists");
        Path corrupt = Path.of("storage-check/corrupt.tsv");
        Files.writeString(corrupt, "bad data");
        try {
            new TaskStore(corrupt).load();
            throw new AssertionError("Corrupt input accepted");
        } catch (IOException expected) {
            check(Files.readString(corrupt).equals("bad data"), "Corrupt input is rejected without overwriting it");
        }
    }

    private static void interfaceChecks() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            StudyNest app = new StudyNest(new ArrayList<>());
            app.setVisible(true);
            check(text(app).contains("Your next small win starts here."), "Empty state displays");
            dialog(() -> button(app, "+ New task").doClick(), d -> answer(d, JOptionPane.CANCEL_OPTION));
            check(load().isEmpty(), "Cancel add leaves storage empty");
            add(app, "Finish Java lab", "Java Programming", LocalDate.now().plusDays(1), Task.Priority.MEDIUM);
            add(app, "Read database notes", "Databases", LocalDate.now().minusDays(1), Task.Priority.LOW);
            add(app, "Practice loops", "Java Programming", LocalDate.now(), Task.Priority.HIGH);
            check(load().size() == 3, "Add saves three tasks");
            check(text(app).contains("1 overdue"), "Yesterday is overdue; today is not");
            JTextField search = components(app, JTextField.class).get(0);
            search.setText("JAVA");
            check(text(app).contains("2 tasks shown"), "Search ignores case and matches subject");
            search.setText("nothing matches");
            check(text(app).contains("No matching tasks."), "Unmatched search displays empty state");
            search.setText("");
            JComboBox<?> filter = components(app, JComboBox.class).get(0);
            filter.setSelectedIndex(3);
            check(text(app).contains("1 task shown") && text(app).contains("Read database notes"), "Overdue filter selects unfinished past deadline");
            filter.setSelectedIndex(0);
            search.setText("Finish Java lab");
            components(app, JCheckBox.class).get(0).doClick();
            check(load().stream().anyMatch(t -> t.getTitle().equals("Finish Java lab") && t.isCompleted()), "Complete action saves status");
            components(app, JCheckBox.class).get(0).doClick();
            check(load().stream().noneMatch(Task::isCompleted), "Reopen action saves status");
            components(app, JCheckBox.class).get(0).doClick();
            String id = load().stream().filter(t -> t.getTitle().equals("Finish Java lab")).findFirst().orElseThrow().getId();
            dialog(() -> button(app, "Edit").doClick(), d -> {
                List<JTextField> fields = components(d, JTextField.class);
                check(fields.get(0).getText().equals("Finish Java lab")
                        && fields.get(1).getText().equals("Java Programming"), "Edit form is prefilled");
                capture(d, "02-edit-task.png");
                fields.get(0).setText("Submit Java project");
                fields.get(2).setText(LocalDate.now().plusDays(3).toString());
                components(d, JComboBox.class).get(0).setSelectedItem(Task.Priority.HIGH);
                answer(d, JOptionPane.OK_OPTION);
            });
            Task edited = load().stream().filter(t -> t.getId().equals(id)).findFirst().orElseThrow();
            check(edited.getTitle().equals("Submit Java project") && edited.isCompleted()
                    && edited.getPriority() == Task.Priority.HIGH, "Edit saves details and preserves ID and completion");
            search.setText("Submit Java project");
            dialog(() -> button(app, "Edit").doClick(), d -> {
                components(d, JTextField.class).get(0).setText("Discard this change");
                answer(d, JOptionPane.CANCEL_OPTION);
            });
            check(load().stream().noneMatch(t -> t.getTitle().equals("Discard this change")), "Cancel edit preserves saved details");
            // Invalid input causes a nested message dialog and then returns to the form.
            invalid(app, "", "Java", "2026-12-15", "Blank title rejected");
            invalid(app, "Valid title", " ", "2026-12-15", "Blank subject rejected");
            invalid(app, "x".repeat(101), "Java", "2026-12-15", "Oversized title rejected");
            invalid(app, "Valid title", "x".repeat(51), "2026-12-15", "Oversized subject rejected");
            invalid(app, "Valid title", "Java", "2026-02-30", "Impossible date rejected");
            search.setText("Practice loops");
            dialog(() -> button(app, "Delete").doClick(), d -> answer(d, JOptionPane.NO_OPTION));
            check(load().size() == 3, "Cancel delete preserves task");
            dialog(() -> button(app, "Delete").doClick(), d -> answer(d, JOptionPane.YES_OPTION));
            check(load().size() == 2, "Confirm delete persists removal");
            search.setText("");
            filter.setSelectedIndex(2);
            check(text(app).contains("1 task shown"), "Completed filter works");
            filter.setSelectedIndex(1);
            check(text(app).contains("1 task shown"), "To-do filter works");
            filter.setSelectedIndex(0);
            capture(app, "01-dashboard.png");
            app.dispose();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void add(StudyNest app, String title, String subject, LocalDate due, Task.Priority priority) {
        dialog(() -> button(app, "+ New task").doClick(), d -> {
            List<JTextField> fields = components(d, JTextField.class);
            fields.get(0).setText(title);
            fields.get(1).setText(subject);
            fields.get(2).setText(due.toString());
            components(d, JComboBox.class).get(0).setSelectedItem(priority);
            answer(d, JOptionPane.OK_OPTION);
        });
    }

    private static void invalid(StudyNest app, String title, String subject, String date, String description) {
        int before = load().size();
        dialog(() -> button(app, "+ New task").doClick(), form -> {
            List<JTextField> fields = components(form, JTextField.class);
            fields.get(0).setText(title); fields.get(1).setText(subject); fields.get(2).setText(date);
            later(message -> {
                check(components(message, JTextField.class).isEmpty(), description + " (validation message)");
                later(reopened -> answer(reopened, JOptionPane.CANCEL_OPTION));
                answer(message, JOptionPane.OK_OPTION);
            });
            answer(form, JOptionPane.OK_OPTION);
        });
        check(load().size() == before, description + " (no task saved)");
    }

    private static void dialog(Runnable open, Consumer<JDialog> action) {
        AtomicReference<Throwable> failure = later(action);
        open.run();
        if (failure.get() != null) throw new AssertionError("Dialog action failed", failure.get());
    }

    private static AtomicReference<Throwable> later(Consumer<JDialog> action) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Timer timer = new Timer(180, event -> {
            try {
                JDialog active = null;
                for (Window w : Window.getWindows()) if (w instanceof JDialog d && d.isShowing()) active = d;
                if (active == null) throw new AssertionError("Expected an open dialog");
                action.accept(active);
            } catch (Throwable error) {
                failure.set(error);
                for (Window w : Window.getWindows()) if (w instanceof JDialog) w.dispose();
            }
        });
        timer.setRepeats(false);
        timer.start();
        return failure;
    }

    private static void answer(JDialog dialog, int value) {
        components(dialog, JOptionPane.class).get(0).setValue(value);
    }

    private static JButton button(Container root, String title) {
        return components(root, JButton.class).stream().filter(b -> b.getText().equals(title)).findFirst().orElseThrow();
    }

    private static <T> List<T> components(Container root, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) result.add(type.cast(child));
            if (child instanceof Container container) result.addAll(components(container, type));
        }
        return result;
    }

    private static String text(Container root) {
        return components(root, JLabel.class).stream().map(JLabel::getText).reduce("", (a, b) -> a + " " + b);
    }

    private static List<Task> load() {
        try { return new TaskStore(Path.of("data/tasks.tsv")).load(); }
        catch (IOException error) { throw new RuntimeException(error); }
    }

    private static void capture(Window window, String name) {
        try {
            window.validate();
            // Capture real Swing content; native OS title bars cannot be painted this way.
            Container content = ((RootPaneContainer) window).getContentPane();
            BufferedImage image = new BufferedImage(content.getWidth(), content.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            content.printAll(graphics);
            graphics.dispose();
            ImageIO.write(image, "png", screenshots.resolve(name).toFile());
        } catch (IOException error) { throw new RuntimeException(error); }
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
        checks++;
        System.out.println("PASS " + description);
    }
}
