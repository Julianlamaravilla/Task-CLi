# Task CLI

URL: https://roadmap.sh/projects/task-tracker?fl=0

A small command-line task tracker written in plain Java — no build tool, no external
dependencies. Tasks are stored in a `task.json` file next to wherever you run the
program, serialized and parsed by hand.

## Requirements

- JDK 11 or newer (developed against JDK 25)

## Build & run

```sh
cd src
javac TaskCli.java
java TaskCli <command> [arguments]
```

Optionally wrap it in a `task-cli` shell alias or `.bat` file so the examples below
work verbatim:

```sh
alias task-cli='java -cp /path/to/Task-CLi/src TaskCli'
```

## Usage

```
task-cli add "<description>"
task-cli update <id> "<description>"
task-cli delete <id>
task-cli mark-in-progress <id>
task-cli mark-done <id>
task-cli list [todo|in-progress|done]
```

Running with no arguments prints this usage text.

### Examples

```sh
# Add a task (gets the next free id and status "todo")
task-cli add "Buy groceries"

# Change its description
task-cli update 1 "Buy groceries and cook dinner"

# Move it through the workflow
task-cli mark-in-progress 1
task-cli mark-done 1

# List everything, or filter by status
task-cli list
task-cli list todo
task-cli list in-progress
task-cli list done

# Remove it
task-cli delete 1
```

`list` prints a fixed-width table:

```
ID   | Status       | Description                    | Last Updated
----------------------------------------------------------------------
1    | todo         | Buy groceries                  | 2026-09-30 12:19:50
```

## Task model

Each task has:

| Field         | Description                                      |
| ------------- | ------------------------------------------------ |
| `id`          | Auto-incrementing integer (max existing id + 1)  |
| `description` | Free text                                        |
| `status`      | `todo`, `in-progress`, or `done`                 |
| `createdAt`   | `yyyy-MM-dd HH:mm:ss`                            |
| `updatedAt`   | `yyyy-MM-dd HH:mm:ss`                            |

## Storage

Tasks live in `task.json` in the current working directory. The file is created on
the first `add`; a missing or empty file is treated as an empty task list. Reading
and writing are done with hand-rolled string manipulation rather than a JSON
library, so the format is only as flexible as `saveTasks` writes it:

```json
[
  {
    "id": 1,
    "description": "Buy groceries",
    "status": "todo",
    "createdAt": "2026-09-30 12:19:50",
    "updatedAt": "2026-09-30 12:19:50"
  }
]
```

If parsing fails, the program warns and starts from a clean in-memory list — note
that the next write will then overwrite the unparseable file.

## Project layout

```
Task-CLi/
├── src/
│   └── TaskCli.java   # everything: commands, persistence, helpers
├── task_tracker.iml   # IntelliJ module
└── README.md
```

## Known limitations

- `saveTasks` writes `createdAt`/`updatedAt`, but `loadTasks` looks for `createAt`
  and `updateAt`. Timestamps therefore come back empty after a reload, and the
  `Last Updated` column is blank for tasks written in an earlier run.
- `updateStatus` prints "not found" for an unknown id but then keeps going, which
  throws a `NullPointerException` (caught and reported as an unexpected error).
- `add`, `mark-in-progress`, and `mark-done` succeed silently — no confirmation is
  printed.
- Escaping only covers double quotes; descriptions containing `{`, `}`, or `,` can
  break the hand-written parser.
- Compiled `.class` files and `task.json` in `src/` are not currently ignored by
  `.gitignore`.
