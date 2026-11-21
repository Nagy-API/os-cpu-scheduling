public class SimProcess {
    private final String id;
    private final int arrivalTime, burstTime, initialPriority;
    private int remainingTime;
    private int firstStartTime = -1, completionTime = -1, tier2EntryTime = -1;

    public SimProcess(String id, int arrival, int burst, int priority) {
        if (id == null || id.isBlank() || arrival < 0 || burst <= 0) {
            throw new IllegalArgumentException("Process needs an ID, arrival >= 0 and burst > 0");
        }
        this.id = id;
        this.arrivalTime = arrival;
        this.burstTime = burst;
        this.initialPriority = priority;
        this.remainingTime = burst;
    }

    // Each scheduling algorithm needs its own fresh execution state.
    public SimProcess copy() {
        return new SimProcess(id, arrivalTime, burstTime, initialPriority);
    }

    public void startIfFirst(int time) {
        if (time < arrivalTime || remainingTime == 0 || completionTime != -1) {
            throw new IllegalArgumentException("Process cannot start at time " + time);
        }
        if (firstStartTime == -1) {
            firstStartTime = time;
        } else if (time < firstStartTime) {
            throw new IllegalArgumentException("Execution time cannot go backwards");
        }
    }

    public void executeOneUnit() {
        if (firstStartTime == -1 || remainingTime == 0 || completionTime != -1) {
            throw new IllegalStateException("Process is not ready to execute");
        }
        remainingTime--;
    }

    // Called at the END of the last executed unit: e.g., RUN [2,3) => finish(3).
    public void finish(int time) {
        if (completionTime != -1 || remainingTime != 0 || firstStartTime == -1) {
            throw new IllegalStateException("Process must finish execution exactly once");
        }
        if ((long) time < (long) firstStartTime + burstTime) {
            throw new IllegalArgumentException("Completion time is earlier than required CPU work");
        }
        completionTime = time;
    }

    // Called by PriorityTier.add() when an unfinished job leaves Tier 1.
    public void enterTier2(int time) {
        if (tier2EntryTime != -1 || firstStartTime == -1 || remainingTime == 0 ||
                remainingTime == burstTime || completionTime != -1 ||
                (long) time < (long) firstStartTime + burstTime - remainingTime) {
            throw new IllegalStateException("Invalid or repeated Tier 2 demotion");
        }
        tier2EntryTime = time;
    }

    private void requireCompleted() {
        if (completionTime == -1) {
            throw new IllegalStateException("Metrics are available only after completion");
        }
    }

    public int waitingTime() {
        return turnaroundTime() - burstTime;
    }

    public int turnaroundTime() {
        requireCompleted();
        return completionTime - arrivalTime;
    }

    public int responseTime() {
        requireCompleted();
        return firstStartTime - arrivalTime;
    }

    public String getId() { return id; }
    public int getArrivalTime() { return arrivalTime; }
    public int getBurstTime() { return burstTime; }
    public int getInitialPriority() { return initialPriority; }
    public int getRemainingTime() { return remainingTime; }
    public int getTier2EntryTime() { return tier2EntryTime; }
    public int getCompletionTime() { return completionTime; }
}