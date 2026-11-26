import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.StringJoiner;

public class ACSScheduler {
    public static SimulationResult run(List<SimProcess> processes, int quantum,
                                       int cs, int agingInterval) {
        if (processes == null || processes.isEmpty() || quantum <= 0 || cs < 0 || agingInterval <= 0) {
            throw new IllegalArgumentException("Invalid ACS configuration");
        }
        List<SimProcess> copies = new ArrayList<>();
        for (SimProcess p : processes) {
            if (p == null) throw new IllegalArgumentException("Null process");
            copies.add(p.copy());
        }
        SimulationResult result = new SimulationResult("ACSScheduler", copies);
        Map<SimProcess, Integer> inputOrder = new IdentityHashMap<>();
        for (int i = 0; i < copies.size(); i++) inputOrder.put(copies.get(i), i);
        PriorityQueue<SimProcess> arrivalQueue = new PriorityQueue<>(
                Comparator.comparingInt(SimProcess::getArrivalTime).thenComparingInt(inputOrder::get));
        arrivalQueue.addAll(copies);
        LinkedList<SimProcess> tier1 = new LinkedList<>();
        PriorityTier tier2 = new PriorityTier(agingInterval);

        int time = 0;
        int completed = 0;
        int switchLeft = 0;
        int used = 0;
        boolean switchPaid = false;
        boolean runningFromTier2 = false;
        SimProcess current = null;
        SimProcess previous = null;

        while (completed < copies.size()) {
            while (!arrivalQueue.isEmpty() && arrivalQueue.peek().getArrivalTime() <= time) {
                tier1.addLast(arrivalQueue.poll());
            }
            if (switchLeft > 0) {
                result.addStep(new TimeStep(time, "CS", "", snapshotTier1(tier1),
                        tier2.snapshot(time), "Context switching"));
                switchLeft--;
                time++;
                continue;
            }
            if (current == null) {
                SimProcess next = !tier1.isEmpty() ? tier1.peekFirst() : tier2.chooseNext(time);
                if (next == null) {
                    result.addStep(new TimeStep(time, "IDLE", "", snapshotTier1(tier1),
                            tier2.snapshot(time), "CPU idle"));
                    previous = null;
                    switchPaid = false;
                    time++;
                    continue;
                }
                if (!switchPaid && previous != null && previous != next && cs > 0) {
                    result.addStep(new TimeStep(time, "CS", "", snapshotTier1(tier1),
                            tier2.snapshot(time), "Switching from " + previous.getId()));
                    switchLeft = cs - 1;
                    switchPaid = true;
                    time++;
                    continue;
                }
                runningFromTier2 = tier1.isEmpty();
                current = runningFromTier2 ? next : tier1.removeFirst();
                if (runningFromTier2) tier2.remove(current);
                used = 0;
                switchPaid = false;
            }

            String tier1State = snapshotTier1(tier1);
            String tier2State = tier2.snapshot(time);
            current.startIfFirst(time);
            current.executeOneUnit();
            used++;
            String event = runningFromTier2 ? "Tier 2 non-preemptive" : "Tier 1 round robin";
            if (current.getRemainingTime() == 0) {
                current.finish(time + 1);
                completed++;
                event = current.getId() + " completed at t=" + (time + 1);
            } else if (!runningFromTier2 && used == quantum) {
                tier2.add(current, time + 1);
                event = current.getId() + " demoted to Tier 2 at t=" + (time + 1);
            }
            result.addStep(new TimeStep(time, "RUN", current.getId(),
                    tier1State, tier2State, event));
            time++;
            if (current.getRemainingTime() == 0 || (!runningFromTier2 && used == quantum)) {
                previous = current;
                current = null;
            }
        }
        return result;
    }

    private static String snapshotTier1(LinkedList<SimProcess> tier1) {
        StringJoiner text = new StringJoiner(", ", "tier1:[", "]");
        for (SimProcess p : tier1) {
            text.add(p.getId() + "(remaining:" + p.getRemainingTime() +
                    ", priority:" + p.getInitialPriority() + ")");
        }
        return text.toString();
    }
}
