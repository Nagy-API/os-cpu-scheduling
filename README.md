# CS241 Assignment 2 — Cloud-Cluster CPU Scheduling

A Java CPU-scheduling simulator implementing the two **required** algorithms:

- **SRTF:** preemptive shortest remaining time first, including context-switch overhead.
- **ACS:** fixed-quantum Round Robin for new jobs (Tier 1), then **non-preemptive** priority for jobs demoted after a full quantum (Tier 2), with waiting-time aging (Tier 3 concept).

The existing team-written Swing GUI is preserved as an **optional bonus**, not a requirement for running the schedulers.

## Run

Requires a Java JDK (17 or newer). From the repository root:

```sh
javac -d out src/*.java tests/*.java
java -cp out SchedulerTests
java -cp out Main --cli
java -cp out Main
```

`Main --cli` runs both demonstration workloads and prints the time-by-time log and metrics. `Main` without arguments opens the Swing GUI to enter process data, choose SRTF/ACS and adjust Q, CS and aging.

## Core files

| Class | Role |
| --- | --- |
| `SimProcess` | Process identity, arrival, burst, priority, remaining work and timing metrics |
| `TimeStep` | One RUN, CS or IDLE entry per elapsed unit |
| `SimulationResult` | Time-step collection, per-process and average metrics, CPU utilization |
| `SRTFScheduler` | Shortest remaining time preemption and context switches |
| `PriorityTier` | Tier 2 waiting list, effective priority with aging and selection |
| `ACSScheduler` | Tier 1 Round Robin and Tier 2 non-preemptive scheduling |
| `ConsolePrinter` | Readable per-step tables, Gantt and averages |
| `Main` | Launches CLI or GUI |
| `SchedulerGUI` | Optional Swing interface |

## Tests

`tests/SchedulerTests.java` checks both official ACS examples and their documented completion times and averages, SRTF preemption, idle CPU, context-switch overhead, Tier 2 non-preemption, aging, original-data preservation, contiguous chronological steps, and randomized cases.

No JUnit dependency is needed.

## Report and submission

The separate report PDF contains three workloads (the two official cases and one generated case), graphs showing average waiting and turnaround versus Q and CS, and a CPU utilization / response-time trade-off discussion.

The experiment CSV covers Q = 2, 3, 4, 5 and CS = 0, 1, 2, with aging interval = 4; SRTF baselines vary CS only because SRTF has no quantum parameter.

Before submitting: add actual student IDs, TA/lab information, and use the exact ZIP filename requested in the assignment. The assignment requires **exactly 4 or 5 students** unless your TA explicitly approves otherwise. All participants must be able to explain the code.

## Design decisions

- Each process is copied before simulation, so SRTF and ACS can use the same original inputs.
- The priority value becomes **smaller** as a waiting Tier 2 job ages; the smaller number has higher precedence.
- A Tier 2 job, once running, **never gets preempted** by an arriving Tier 1 job.
- A context switch is charged when changing directly between different processes, not on the initial dispatch or after idle.
- Every elapsed unit produces exactly one `TimeStep` (RUN, CS or IDLE), with no synthetic final IDLE marker.
