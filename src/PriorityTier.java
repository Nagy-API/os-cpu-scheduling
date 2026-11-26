import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class PriorityTier {
    private final List<SimProcess> queue = new ArrayList<>();
    private final Map<SimProcess, Integer> entryTimes = new HashMap<>();
    private final int agingInterval;

    public PriorityTier(int agingInterval) {
        if (agingInterval <= 0) throw new IllegalArgumentException("Aging interval must be > 0");
        this.agingInterval = agingInterval;
    }

    public void add(SimProcess process, int time) {
        if (process == null || time < 0 || process.getRemainingTime() == 0) {
            throw new IllegalArgumentException("Invalid Tier 2 process or time");
        }
        if (queue.contains(process)) throw new IllegalStateException("Process already waiting in Tier 2");
        if (process.getTier2EntryTime() == -1) process.enterTier2(time);
        if (time < process.getTier2EntryTime()) throw new IllegalArgumentException("Invalid Tier 2 time");
        entryTimes.putIfAbsent(process, process.getTier2EntryTime());
        queue.add(process);
    }

    public SimProcess chooseNext(int time) {
        SimProcess best = null;
        for (SimProcess p : queue) {
            if (best == null || effectivePriority(p, time) < effectivePriority(best, time) ||
                    (effectivePriority(p, time) == effectivePriority(best, time) &&
                     entryTimes.get(p) < entryTimes.get(best))) {
                best = p;
            }
        }
        return best;
    }

    public void remove(SimProcess process) {
        if (!queue.remove(process)) throw new IllegalArgumentException("Process not found in Tier 2");
    }

    public int effectivePriority(SimProcess process, int time) {
        Integer entry = entryTimes.get(process);
        if (entry == null || time < entry) throw new IllegalArgumentException("Invalid aging time or process");
        long priority = (long) process.getInitialPriority() - (time - entry) / agingInterval;
        return (int) Math.max(Integer.MIN_VALUE, priority);
    }

    public String snapshot(int time) {
        StringJoiner parts = new StringJoiner(", ", "tier2:[", "]");
        for (SimProcess p : queue) {
            parts.add(p.getId() + "(remaining:" + p.getRemainingTime() +
                      ", effectivePriority:" + effectivePriority(p, time) + ")");
        }
        return parts.toString();
    }

    public boolean isEmpty() { return queue.isEmpty(); }
    public List<SimProcess> getQueue() { return Collections.unmodifiableList(queue); }
    public int getAgingInterval() { return agingInterval; }
}
