package studynest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** A small, dependency-free student planner. Requires JDK 17 or newer. */
public class StudyNest extends JFrame {
    private static final Color BACKGROUND = new Color(245, 247, 251);
    private static final Color INK = new Color(33, 43, 65);
    private static final Color MUTED = new Color(106, 116, 137);
    private static final Color ACCENT = new Color(91, 79, 214);
    private final TaskStore store = new TaskStore(Path.of("data", "tasks.tsv"));
    private final List<Task> tasks;
    private final JPanel taskList = new JPanel();
    private final JTextField search = new JTextField();
    private final JComboBox<String> filter = new JComboBox<>(new String[]{"All tasks", "To do", "Completed", "Overdue"});
    private final JLabel summary = new JLabel();
    private final JLabel count = new JLabel();
    private final JProgressBar progress = new JProgressBar(0, 100);

    public StudyNest(List<Task> tasks) {
        super("StudyNest · Student planner");
        this.tasks = tasks;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 570));
        setSize(940, 720);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 22));
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(30, 36, 26, 36));
        setContentPane(root);

        JPanel header = transparent(new BorderLayout(16, 12));
        JPanel heading = transparent(new GridLayout(0, 1, 0, 6));
        heading.add(label("STUDYNEST  /  YOUR STUDY SPACE", 12, ACCENT, true));
        heading.add(label("A little focus. A lot of progress.", 28, INK, true));
        heading.add(label("Keep your coursework in order, one task at a time.", 14, MUTED, false));
        header.add(heading, BorderLayout.CENTER);
        JButton add = button("+ New task", true);
        add.addActionListener(event -> showTaskDialog(null));
        JPanel action = transparent(new FlowLayout(FlowLayout.RIGHT, 0, 20));
        action.add(add);
        header.add(action, BorderLayout.EAST);

        JPanel stats = new JPanel(new BorderLayout(12, 12));
        stats.setBackground(Color.WHITE);
        stats.setBorder(new EmptyBorder(18, 20, 18, 20));
        summary.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        summary.setForeground(INK);
        stats.add(summary, BorderLayout.NORTH);
        progress.setForeground(ACCENT);
        progress.setBackground(BACKGROUND);
        progress.setStringPainted(true);
        stats.add(progress, BorderLayout.CENTER);
        JPanel top = transparent(new BorderLayout(0, 24));
        top.add(header, BorderLayout.NORTH);
        top.add(stats, BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);

        JPanel body = transparent(new BorderLayout(0, 14));
        JPanel toolbar = transparent(new BorderLayout(12, 0));
        search.putClientProperty("JTextField.placeholderText", "Search tasks or subjects");
        search.setToolTipText("Search tasks or subjects");
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(218, 222, 234)), new EmptyBorder(10, 12, 10, 12)));
        JPanel searchGroup = transparent(new BorderLayout(8, 0));
        searchGroup.add(label("Search", 13, MUTED, false), BorderLayout.WEST);
        searchGroup.add(search, BorderLayout.CENTER);
        toolbar.add(searchGroup, BorderLayout.CENTER);
        toolbar.add(filter, BorderLayout.EAST);
        body.add(toolbar, BorderLayout.NORTH);
        taskList.setLayout(new BoxLayout(taskList, BoxLayout.Y_AXIS));
        taskList.setBackground(BACKGROUND);
        JScrollPane scroll = new JScrollPane(taskList);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        body.add(scroll, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);
        JPanel footer = transparent(new BorderLayout());
        count.setForeground(MUTED);
        footer.add(count, BorderLayout.WEST);
        footer.add(label("Saved locally · Made for your next small win", 12, MUTED, false), BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);

        filter.addActionListener(event -> refresh());
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { refresh(); }
            public void removeUpdate(DocumentEvent event) { refresh(); }
            public void changedUpdate(DocumentEvent event) { refresh(); }
        });
        refresh();
    }

    private void refresh() {
        long done = tasks.stream().filter(Task::isCompleted).count();
        long overdue = tasks.stream().filter(task -> !task.isCompleted()
                && task.getDeadline().isBefore(LocalDate.now())).count();
        summary.setText(done + " of " + tasks.size() + " tasks complete   ·   " + overdue + " overdue");
        progress.setValue(tasks.isEmpty() ? 0 : (int) (done * 100 / tasks.size()));
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        List<Task> visible = tasks.stream()
                .filter(task -> (task.getTitle() + " " + task.getSubject()).toLowerCase(Locale.ROOT).contains(query))
                .filter(task -> switch (filter.getSelectedIndex()) {
                    case 1 -> !task.isCompleted();
                    case 2 -> task.isCompleted();
                    case 3 -> !task.isCompleted() && task.getDeadline().isBefore(LocalDate.now());
                    default -> true;
                })
                .sorted(Comparator.comparing(Task::isCompleted).thenComparing(Task::getDeadline)
                        .thenComparing(Task::getPriority)).toList();
        taskList.removeAll();
        if (visible.isEmpty()) {
            JPanel empty = transparent(new GridLayout(0, 1, 0, 12));
            empty.setBorder(new EmptyBorder(55, 20, 55, 20));
            empty.add(label(tasks.isEmpty() ? "Your next small win starts here." : "No matching tasks.", 22, INK, true));
            empty.add(label(tasks.isEmpty() ? "Click + New task to plan your first assignment." : "Try another search or filter.", 14, MUTED, false));
            taskList.add(empty);
        }
        for (Task task : visible) {
            taskList.add(taskCard(task));
            taskList.add(Box.createVerticalStrut(10));
        }
        count.setText(visible.size() + " task" + (visible.size() == 1 ? "" : "s") + " shown");
        taskList.revalidate();
        taskList.repaint();
    }

    private JPanel taskCard(Task task) {
        JPanel card = new JPanel(new BorderLayout(14, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(17, 16, 17, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 94));
        JCheckBox complete = new JCheckBox();
        complete.setBackground(Color.WHITE);
        complete.setSelected(task.isCompleted());
        complete.setToolTipText("Mark complete or reopen task");
        complete.addActionListener(event -> {
            task.toggleCompleted();
            if (!save()) task.toggleCompleted();
            refresh();
        });
        card.add(complete, BorderLayout.WEST);
        JPanel text = transparent(new GridLayout(0, 1, 0, 7));
        text.add(label(task.getTitle(), 16, task.isCompleted() ? MUTED : INK, true));
        boolean late = !task.isCompleted() && task.getDeadline().isBefore(LocalDate.now());
        text.add(label(task.getSubject() + "   ·   " + (late ? "Overdue " : "Due ")
                + task.getDeadline() + "   ·   " + task.getPriority().name().toLowerCase(Locale.ROOT)
                + " priority", 12, late ? new Color(180, 66, 66) : MUTED, false));
        card.add(text, BorderLayout.CENTER);
        JButton edit = button("Edit", false);
        edit.addActionListener(event -> showTaskDialog(task));
        JButton delete = button("Delete", false);
        delete.addActionListener(event -> {
            if (JOptionPane.showConfirmDialog(this, "Delete \"" + task.getTitle() + "\"?",
                    "Delete task", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                int index = tasks.indexOf(task);
                tasks.remove(index);
                if (!save()) tasks.add(index, task);
                refresh();
            }
        });
        JPanel actions = transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(edit);
        actions.add(delete);
        card.add(actions, BorderLayout.EAST);
        return card;
    }

    private void showTaskDialog(Task existing) {
        JTextField title = new JTextField(existing == null ? "" : existing.getTitle());
        JTextField subject = new JTextField(existing == null ? "" : existing.getSubject());
        JTextField deadline = new JTextField((existing == null
                ? LocalDate.now().plusDays(1) : existing.getDeadline()).toString());
        JComboBox<Task.Priority> priority = new JComboBox<>(Task.Priority.values());
        priority.setSelectedItem(existing == null ? Task.Priority.MEDIUM : existing.getPriority());
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 7));
        form.add(new JLabel("Task title (required, max 100 characters)")); form.add(title);
        form.add(new JLabel("Subject (required, max 50 characters)")); form.add(subject);
        form.add(new JLabel("Deadline (YYYY-MM-DD)")); form.add(deadline);
        form.add(new JLabel("Priority")); form.add(priority);
        while (JOptionPane.showConfirmDialog(this, form, existing == null ? "Plan a new task" : "Edit task",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            String name = title.getText().strip();
            String course = subject.getText().strip();
            if (name.isEmpty() || course.isEmpty() || name.length() > 100 || course.length() > 50) {
                JOptionPane.showMessageDialog(this, "Enter a title (1–100 characters) and subject (1–50 characters).");
                continue;
            }
            try {
                LocalDate due = LocalDate.parse(deadline.getText().strip());
                Task.Priority selectedPriority = (Task.Priority) priority.getSelectedItem();
                if (existing == null) {
                    Task task = new Task(name, course, due, selectedPriority);
                    tasks.add(task);
                    if (!save()) {
                        tasks.remove(task);
                        continue;
                    }
                } else {
                    int index = tasks.indexOf(existing);
                    Task updated = new Task(existing.getId(), name, course, due,
                            selectedPriority, existing.isCompleted());
                    tasks.set(index, updated);
                    if (!save()) {
                        tasks.set(index, existing);
                        continue;
                    }
                }
                refresh();
                return;
            } catch (DateTimeParseException error) {
                JOptionPane.showMessageDialog(this, "Enter a real date in YYYY-MM-DD format, such as 2026-12-15.");
            }
        }
    }

    private boolean save() {
        try {
            store.save(tasks);
            return true;
        } catch (Exception error) {
            JOptionPane.showMessageDialog(this, "The change could not be saved.\n" + error.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private static JPanel transparent(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    private static JLabel label(String text, int size, Color color, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    private static JButton button(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        button.setBackground(primary ? ACCENT : BACKGROUND);
        button.setForeground(primary ? Color.WHITE : MUTED);
        button.setOpaque(true);
        button.setBorder(new EmptyBorder(12, 16, 12, 16));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                TaskStore store = new TaskStore(Path.of("data", "tasks.tsv"));
                new StudyNest(new ArrayList<>(store.load())).setVisible(true);
            } catch (Exception error) {
                JOptionPane.showMessageDialog(null, "StudyNest could not start.\n" + error.getMessage(),
                        "Startup error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
