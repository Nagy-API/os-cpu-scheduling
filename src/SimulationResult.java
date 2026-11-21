import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SimulationResult {
    private final String algorithmName;
    private final List<SimProcess> processes;
    private final List<TimeStep> steps = new ArrayList<>();

    public SimulationResult(String name, List<SimProcess> processes) {
        if (name == null || name.isBlank() || processes == null || processes.isEmpty()) {
            throw new IllegalArgumentException("Algorithm name and processes are required");
        }
        Set<String> ids = new HashSet<>();
        for (SimProcess p : processes) {
            if (p == null || !ids.add(p.getId())) {
                throw new IllegalArgumentException("Processes must be non-null with unique IDs");
            }
        }
        this.algorithmName = name;
        // Keep the SAME process objects that a scheduler updates; copy only the list.
        this.processes = new ArrayList<>(processes);
    }

    public void addStep(TimeStep step) {
        if (step == null || step.getTime() != steps.size()) {
            throw new IllegalArgumentException("Exactly one chronological step per time unit, starting at 0");
        }
        steps.add(step);
    }

    private void requireCompleted() {
        for (SimProcess p : processes) {
            if (p.getCompletionTime() == -1) {
                throw new IllegalStateException("Finish all processes before reading results");
            }
        }
    }

    public double averageWaiting() {
        requireCompleted();
        long total = 0;
        for (SimProcess p : processes) total += p.waitingTime();
        return (double) total / processes.size();
    }

    public double averageTurnaround() {
        requireCompleted();
        long total = 0;
        for (SimProcess p : processes) total += p.turnaroundTime();
        return (double) total / processes.size();
    }

    public double averageResponse() {
        requireCompleted();
        long total = 0;
        for (SimProcess p : processes) total += p.responseTime();
        return (double) total / processes.size();
    }

    public double cpuUtilization() {
        requireCompleted();
        if (steps.isEmpty()) {
            throw new IllegalStateException("CPU utilization requires a completed time-step log");
        }
        int lastCompletion = 0;
        long totalBurst = 0;
        for (SimProcess p : processes) {
            lastCompletion = Math.max(lastCompletion, p.getCompletionTime());
            totalBurst += p.getBurstTime();
        }
        // Count actual CPU work; CS and IDLE are not useful execution time.
        int runSteps = 0;
        for (TimeStep step : steps) {
            if ("RUN".equals(step.getKind())) runSteps++;
        }
        if (steps.size() != lastCompletion || runSteps != totalBurst) {
            throw new IllegalStateException("Time-step log does not match completed CPU work");
        }
        // CS and IDLE count toward elapsed time, not useful CPU execution.
        return 100.0 * runSteps / steps.size();
    }

    public String getAlgorithmName() { return algorithmName; }
    public List<SimProcess> getProcesses() { return Collections.unmodifiableList(processes); }
    public List<TimeStep> getSteps() { return Collections.unmodifiableList(steps); }
}