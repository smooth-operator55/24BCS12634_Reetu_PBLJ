# StudyNest viva preparation

Use these notes to explain the implementation in your own words. Follow the
methods in the source while rehearsing; reading notes alone is not practice.

## One-minute introduction

“StudyNest is a Java Swing desktop planner for student assignments. It lets a
student add tasks with subjects, priorities, and deadlines, edit them, mark them
complete, and search or filter the list. It saves tasks in a local text file so
they survive restarting. I separated task data, file storage, and the interface
into three classes.”

## Questions you should be able to answer

**Where are tasks stored while the application runs?**

In the `tasks` list in `StudyNest`, backed by an `ArrayList`. Filtering produces a
separate visible list; it does not delete anything from the main list.

**How do tasks survive a restart?**

After each accepted change, `save()` calls `TaskStore.save(tasks)`. On startup,
`TaskStore.load()` reads `data/tasks.tsv` and rebuilds the objects. The path is
relative to the directory from which Java was launched.

**What does one file record contain?**

`ID<TAB>encoded title<TAB>encoded subject<TAB>date<TAB>priority<TAB>completed`.
The final field is `true` or `false`. `LocalDate.parse()` rebuilds the date and
`Priority.valueOf()` rebuilds the enum.

**Why Base64? Is it encryption?**

Base64 keeps tabs and line breaks inside text fields from being interpreted as
record separators, and UTF-8 supports Unicode. It is reversible encoding, not
encryption. The file is not protected against someone who can read it.

**Why use a temporary file?**

The code writes complete new contents before replacing the old destination. It
reduces exposure to partially written records. It does not guarantee recovery
from every crash, and no historical backup is retained.

**How do buttons work?**

`addActionListener()` registers a callback. Swing calls it when the user activates
the control. The callback changes the list, saves it, then calls `refresh()`.
For example, the checkbox toggles completion and rolls it back if saving fails.

**Why use `SwingUtilities.invokeLater()`?**

Swing UI work should run on its event dispatch thread. `invokeLater()` schedules
startup there. File saving is currently also on that thread, which is simple for
small files but could make the interface pause for large files.

**How is live search different from a button action?**

Search uses `DocumentListener` to observe changes in the text field's document.
Each insertion or removal calls `refresh()`. Both the query and searchable text
are lowercased with `Locale.ROOT` for case-insensitive matching.

**How is input validated?**

The shared add/edit form strips whitespace, checks required fields and length,
then parses a real ISO date. It reports errors without committing changes. Past
dates are accepted intentionally. `Task` itself does not enforce these UI rules.

**Explain the editing logic step by step.**

1. Clicking Edit passes the selected `Task` to `showTaskDialog(existing)`.
2. Fields are initialized from its getters.
3. OK validates the proposed values.
4. A new object retains the existing ID and completion flag.
5. `tasks.set()` replaces the entry at the original index.
6. Successful saving commits the change and refreshes the screen; failed saving
   restores the original object. Cancel exits without replacing anything.

**Why retain the ID? Why replace the object?**

The ID represents the same assignment even when its title changes. The descriptive
fields are final, so editing creates a replacement object. The constructor that
accepts an ID is also used to restore tasks from disk.

**What happens if editing changes which filter a task matches?**

The refreshed visible list is recalculated. A task can disappear from the current
search or overdue filter while still being present in the underlying list.

**What counts as overdue? How is progress calculated?**

An unfinished task is overdue when its deadline is before today. Today is not
overdue. Progress is the integer percentage `done * 100 / total`; an empty list
uses zero to avoid division by zero.

**Did you test restart persistence?**

Yes. The feature process saved edited and completed tasks, then exited. A fresh
JVM loaded the file and checked the restored values and interface counts. The
logs show 30 feature/storage checks and 3 restart checks. This is process restart
testing, not an operating-system reboot or power-loss test.

**What would you improve next?**

For larger usage, move file I/O off the UI thread, add backups and file locking,
and provide reminders. A database would be appropriate if multiple users needed
shared access.

## Rehearse with the code

- Trace Add from its listener through `showTaskDialog()`, `save()`, and `refresh()`.
- Trace an edited task from its card to `tasks.set()` and the rollback branch.
- Open the TSV file in a text editor and identify its six columns.
- Demonstrate an invalid date, Cancel edit, and a successful edit.
- Close the app, reopen from the same directory, and explain why the data remains.
- Explain one limitation honestly rather than claiming the app handles every case.
