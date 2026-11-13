# Submission verification

Executed on **14 November 2026**, macOS, Homebrew OpenJDK **17.0.21**.
Production and test source compiled successfully with UTF-8 encoding.

**Result: 33 passing assertions** — 30 in feature/storage mode and 3 in a second,
fresh JVM for restart verification. These are assertions, not 33 independent
user journeys. Existing project data was not modified.

| Area | Verified behavior |
| --- | --- |
| Storage | Missing file, Unicode and delimiter round trip, ID and completion preservation, empty save |
| Bad file | Malformed record rejected and original contents preserved |
| Add | Valid tasks saved; cancellation does not add a task |
| Edit | Prefilled values; changed title, date and priority saved; ID and completion preserved; cancel discards edits |
| Completion | Complete and reopen both update disk state |
| Search | Case-insensitive subject match and unmatched empty state |
| Filters | To do, completed, overdue; today not overdue |
| Validation | Blank title/subject, excessive title/subject lengths, impossible date rejected without saving |
| Delete | Cancel preserves task; confirm removes it from disk |
| Restart | Fresh JVM restores task count, edited fields, completion, and interface progress |

The harness drives Swing controls using `doClick()`, changes field values, and
responds to modal `JOptionPane` forms on the event dispatch thread. Screenshots
are captures of actual Swing content using `printAll()`; native window title bars
are omitted. They show test data, not personal task data.

Actual output: [feature checks](feature-test-output.txt) and
[restart checks](restart-test-output.txt).

## Repeat on macOS/Linux

Run these commands from the project root with JDK 17+ on PATH. A graphical desktop
is required. A unique temporary working directory isolates task storage.

```sh
javac -encoding UTF-8 -d out src/studynest/*.java tests/studynest/*.java
project_dir="$PWD"
check_dir=$(mktemp -d)
cd "$check_dir"
java -cp "$project_dir/out" studynest.SubmissionCheck features "$project_dir/docs/screenshots"
java -cp "$project_dir/out" studynest.SubmissionCheck restart "$project_dir/docs/screenshots"
cd "$project_dir"
```

The second command must run after the first, in the same temporary folder, on the
same calendar day (fixtures use relative dates). The feature mode expects an empty
folder. The screenshots will be refreshed by rerunning. Temporary test files can
be removed afterwards.

On this Mac, use `/opt/homebrew/opt/openjdk@17/bin/javac` and
`/opt/homebrew/opt/openjdk@17/bin/java` if the default Java commands cannot locate
the Homebrew installation. No machine-wide configuration change is needed.

## Remaining manual checks

- Try the interface with mouse and keyboard, including window resizing and long titles.
- Verify appearance on the platform used for the course demonstration.
- Save-failure rollback, concurrent application instances, and crash/power-loss
  behavior are not covered by the automated checks.

The app is intended for a single instance and small local lists. Passing these
checks does not imply concurrent access or crash recovery support.
