public class SimProcess {
    private final String id;
    private final int arrivalTime, burstTime, initialPriority;
    private int remainingTime;
    private int firstStartTime = -1, completionTime = -1, tier2EntryTime = -1;

    public SimProcess(String id, int arrival, int burst, int priority) {
        if (id == null || id.isBlank() || arrival < 0 || burst <= 0) {
            throw new IllegalArgumentException("Invalid process data");
        }
        this.id = id;
        arrivalTime = arrival;
        burstTime = burst;
        initialPriority = priority;
        remainingTime = burst;
    }

    public SimProcess copy() {
        return new SimProcess(id, arrivalTime, burstTime, initialPriority);
    }

    public void startIfFirst(int time) {
        if (time < arrivalTime || remainingTime == 0) {
            throw new IllegalArgumentException("Invalid start time");
        }
        if (firstStartTime == -1) firstStartTime = time;
    }

    public void executeOneUnit() {
        if (firstStartTime == -1 || remainingTime == 0) {
            throw new IllegalStateException("Process cannot run now");
        }
        remainingTime--;
    }

    public void finish(int time) {
        if (firstStartTime == -1 || remainingTime != 0 || completionTime != -1) {
            throw new IllegalStateException("Process has not finished its CPU work");
        }
        if (time - firstStartTime < burstTime) {
            throw new IllegalArgumentException("Invalid finish time");
        }
        completionTime = time;
    }

    public void enterTier2(int time) {
        if (tier2EntryTime != -1 || firstStartTime == -1 ||
                remainingTime == 0 || remainingTime == burstTime ||
                time <= firstStartTime) {
            throw new IllegalStateException("Invalid Tier 2 entry");
        }
        tier2EntryTime = time;
    }

    private void requireCompleted() {
        if (completionTime == -1) {
            throw new IllegalStateException("Process is not finished yet");
        }
    }

    public int waitingTime() { return turnaroundTime() - burstTime; }

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