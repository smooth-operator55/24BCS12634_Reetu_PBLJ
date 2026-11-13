# StudyNest — Java Course Project Report

## Problem and objective

Students often track coursework across notebooks and messages, making it easy to
miss deadlines or lose track of completed work. StudyNest provides one desktop
screen for recording assignments, finding them by subject, identifying overdue
work, and tracking completion. The scope is deliberately small enough to explain
and maintain as a Java course project.

## Solution

The application supports adding, editing, completing, reopening, searching,
filtering, and deleting assignments. Each task has a title, subject, deadline,
priority, completion flag, and unique ID. A progress bar summarizes completion.
Changes are saved immediately to a local file and restored on the next launch.
No database or external Java library is required.

## Class structure

| Class | Responsibility | Main methods |
| --- | --- | --- |
| `Task` | Represents one assignment; encapsulates its fields | Constructors, getters, `toggleCompleted()` |
| `Task.Priority` | Restricts priority to HIGH, MEDIUM, or LOW | Enum constants |
| `TaskStore` | Converts tasks to file records and back | `save()`, `load()`, private encoding helpers |
| `StudyNest` | Builds the Swing interface and responds to user events | `main()`, `refresh()`, `taskCard()`, `showTaskDialog()`, `save()` |
| `SubmissionCheck` (test only) | Exercises storage and Swing actions; captures evidence | Feature mode and fresh-process restart mode |

`StudyNest` owns an in-memory `List<Task>` and delegates persistence to
`TaskStore`. `TaskStore` reconstructs `Task` objects when loading. This separates
the model, persistence, and interface responsibilities without adding a framework.

## Java concepts used

- **Objects and encapsulation:** `Task` has private fields and public methods.
  Its identity and descriptive fields are final; completion can be toggled.
- **Inheritance and interfaces:** `StudyNest` extends `JFrame`; the search listener
  implements `DocumentListener`. Swing component references also demonstrate
  working through common superclasses such as `Container`.
- **Collections and generics:** `List<Task>` and `ArrayList<Task>` hold typed data.
- **Enums:** priority is a fixed set rather than arbitrary strings.
- **Event handling:** action listeners handle buttons and checkboxes; document
  events update search results immediately.
- **Streams, lambdas, and comparators:** the interface calculates counts, filters
  tasks, and sorts unfinished tasks first, then by date and priority.
- **Dates and exceptions:** `LocalDate` represents deadlines; invalid input raises
  `DateTimeParseException`, while file failures are handled through exceptions.
- **File I/O:** `Path`, `Files`, UTF-8, and Base64 implement local persistence.

## Key implementation decisions

**Storage:** `data/tasks.tsv` is relative to the launch folder. Each line contains
six tab-separated values: ID, Base64 title, Base64 subject, ISO date, priority,
and completion flag. Base64 prevents delimiter characters in text from breaking
the record; it does not encrypt data. Saving writes the whole list to a temporary
file in the same directory before replacing the destination. This avoids writing
partial records directly into the live file, but the replacement is not explicitly
requested as an atomic move and is not a backup system. Invalid file records
stop startup with an error rather than silently overwriting the file.

**Events:** `main()` uses `SwingUtilities.invokeLater()` to create the interface on
Swing's event dispatch thread. Button listeners call application methods;
`refresh()` rebuilds task cards and counts from the list. Display filtering does
not remove tasks from storage.

**Validation:** adding and editing share one form. Input is stripped of surrounding
whitespace; title and subject must be nonempty and at most 100 and 50 Java string
characters respectively. `LocalDate.parse()` rejects impossible dates. Past dates
are allowed because overdue assignments are useful. Validation currently lives in
the UI, not in the `Task` constructor.

**Editing:** an existing task opens a prefilled form. After validation, a replacement
`Task` is created with the original ID and completion flag. `tasks.set(index,
updated)` replaces the original entry, then saves. If saving fails, the original
entry is restored and the form stays available. Cancel makes no changes. The same
save-and-rollback idea is used for add, delete, and completion changes.

## Testing and evidence

The source compiled and the checks ran on Homebrew OpenJDK 17.0.21 on macOS.
Thirty feature/storage assertions and three separate-process restart assertions
passed on 14 November 2026. Tests used disposable folders, not the user's data.
The checks exercise actual Swing listeners and modal forms programmatically;
they are not mouse/keyboard automation. The restart test starts a fresh JVM,
loads the saved file, and checks both data and the reopened interface.

See [TESTING.md](TESTING.md) for coverage and reproduction instructions,
[feature output](feature-test-output.txt), [restart output](restart-test-output.txt),
and the screenshots embedded in the [README](../README.md).

## Limitations and future work

- Single-user local storage; simultaneous app instances can overwrite each other.
- No accounts, cloud sync, reminders, recurring tasks, or database.
- Whole-file saving and UI rebuilding suit small task lists; file operations run
  on the event thread and may pause the interface with very large files.
- The launch directory determines which data file is used.
- No automatic backups or recovery interface for damaged files.
- Long titles can be clipped in the task cards, especially in smaller windows.
- Overdue status refreshes on interaction rather than automatically at midnight.
- Tests ran on macOS with JDK 17; Windows/Linux and other screen sizes still need
  platform-specific visual checks. Save-failure rollback was reviewed in source
  but not exercised by the automated suite.

Possible extensions include reminders, task categories, background persistence,
and a database once multi-user access is required.
