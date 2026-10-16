# AGENTS.md

University concurrent-programming coursework (UFCG — Programação Concorrente).  
Labs are in Java, C (pthreads), Python (threading), and Go (goroutines/channels).  
No build system, no CI, no test framework, no linter — everything is driven by per-lab shell scripts.

---

## Repo layout

```
2025.2/Lab1 … Lab10/   # completed semester
2026.2/Lab1/           # current semester (in progress)
```

Each lab has independent `build.sh` / `run.sh` scripts.  
Labs are entirely self-contained — no shared libraries across labs.

---

## Running labs

Scripts use `readlink -f` internally, so they work from any CWD.

### Java labs (1, 3, 4, 5, 6, 9, 10, 2026.2/Lab1)

```bash
bash build.sh          # compiles *.java → bin/
bash run.sh [args]     # runs via 'java -cp bin/'
```

Lab 4 is an exception: `run.sh` compiles inline — no separate `build.sh`.

### C — Lab 2

```bash
bash build.sh S        # → .build/serial   (no -pthread)
bash build.sh C        # → .build/concurrent (-pthread)
bash run.sh <num_classes> <num_students_per_class>
```

### Python — Lab 2

```bash
bash run.sh S <num_classes> <num_students_per_class>
bash run.sh C <num_classes> <num_students_per_class>
```

### Go — Lab 8

Path is `2025.2/Lab8/Lab8/src/` (double-nested — artifact from how the project was added).  
Requires Go 1.23+; run `go mod download` first.

```bash
go run server/main.go
go run client/main.go <client_port> <peer_port> <data_dir>
```

---

## Dataset prerequisites (must run before `run.sh`)

- **Lab 3**: `bash make_dataset.sh <num_files>` — generates `dataset/dna_*.txt`. The `run.sh` aborts with an explicit error if `dataset/` is missing.
- **Lab 9**: `bash make_logs.sh <num_files> <lines_per_file>` — generates `dataset/log*.txt`.

---

## Known stubs / incomplete labs

- **Lab 7** (`2025.2/Lab7/src/lab7-e1.go`): file contains only `TODO to be implemented` — never implemented.
- **Lab 3B**: spec exists (`PC-lab-3b-25.2.md`) but no source code was written.

---

## Race conditions are intentional starting points

Labs 5, 6, 9, and 10 contain deliberately unsynchronized code (no `synchronized`, no locks, shared mutable statics). The exercise is to identify and fix them. Do not assume they are bugs to be silently corrected without context.

- Lab 5 `Buffer.java`: no synchronization on shared buffer.
- Lab 6 `WebStatsMain.java`: shared counters with no synchronization.
- Lab 9 `LogAnalyzer.java`: `static` counters (`total200`, `total500`) unsynchronized across threads.
- Lab 10: goal is to refactor using `java.util.concurrent` (BlockingQueue, ExecutorService, etc.).

---

## Lab 9 — `run-all.sh` only runs serial

`run-all.sh` has the concurrent invocation commented out. To run the concurrent version:

```bash
bash src/java/concurrent/build.sh
bash src/java/concurrent/run.sh dataset/log1.txt dataset/log2.txt ...
```

---

## Compiled artifacts in git

`2026.2/Lab1/src/java/concurrent/bin/` has committed `.class` files — this is the only lab where build output is tracked. Do not delete this directory.

---

## Submission

```bash
# 2025.2 labs (port 22):
bash submit-answer.sh lab<N> <path_to_file>

# 2026.2 labs (port 25000):
bash submit-answer.sh lab<N> <path_to_file>
```

Target server: `aluno@150.165.85.31`. The submit scripts have a cosmetic bug: some echo messages say the wrong lab number, but the actual `scp` destination uses the correct variable.

---

## Correctness verification

No automated tests exist. Verify by:
1. Running the program and observing console output.
2. Comparing `time` output between serial and concurrent versions (most `run.sh` scripts prepend `time`).
