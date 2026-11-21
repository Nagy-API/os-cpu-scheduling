public class TimeStep {
    private final int time;
    private final String kind, cpuActivity, tier1Snapshot, tier2Snapshot, event;

    // A TimeStep describes the CPU activity during [time, time + 1).
    public TimeStep(int time, String kind, String cpu, String tier1, String tier2, String event) {
        if (time < 0) {
            throw new IllegalArgumentException("Time must be non-negative");
        }
        if (!"RUN".equals(kind) && !"CS".equals(kind) && !"IDLE".equals(kind)) {
            throw new IllegalArgumentException("Kind must be RUN, CS or IDLE");
        }
        if ("RUN".equals(kind) && (cpu == null || cpu.isBlank())) {
            throw new IllegalArgumentException("RUN requires a process ID");
        }
        this.time = time;
        this.kind = kind;
        this.cpuActivity = cpu == null ? "" : cpu;
        this.tier1Snapshot = tier1 == null ? "" : tier1;
        this.tier2Snapshot = tier2 == null ? "" : tier2;
        this.event = event == null ? "" : event;
    }

    public int getTime() { return time; }
    public String getKind() { return kind; }
    public String getCpuActivity() { return cpuActivity; }
    public String getTier1Snapshot() { return tier1Snapshot; }
    public String getTier2Snapshot() { return tier2Snapshot; }
    public String getEvent() { return event; }
}