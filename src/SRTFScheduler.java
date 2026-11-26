import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.StringJoiner;

public class SRTFScheduler {
    public static SimulationResult run(List<SimProcess> processes, int cs) {
        if (processes == null || processes.isEmpty() || cs < 0) {
            throw new IllegalArgumentException("Invalid SRTF configuration");
        }
        List<SimProcess> copies = new ArrayList<>();
        for (SimProcess p : processes) {
            if (p == null) throw new IllegalArgumentException("Null process");
            copies.add(p.copy());
        }
        SimulationResult result = new SimulationResult("SRTFScheduler", copies);
        Map<SimProcess, Integer> order = new IdentityHashMap<>();
        for (int i = 0; i < copies.size(); i++) order.put(copies.get(i), i);
        PriorityQueue<SimProcess> arrivalTimeQueue = new PriorityQueue<>(
                Comparator.comparingInt(SimProcess::getArrivalTime).thenComparingInt(order::get));
        PriorityQueue<SimProcess> remainingTimeQueue = new PriorityQueue<>(
                Comparator.comparingInt(SimProcess::getRemainingTime)
                          .thenComparingInt(SimProcess::getArrivalTime)
                          .thenComparingInt(order::get));
        arrivalTimeQueue.addAll(copies);

        int time = 0;
        int completed = 0;
        int switchLeft = 0;
        boolean switchPaid = false;
        SimProcess current = null;
        SimProcess previous = null;
        while (completed < copies.size()) {
            while (!arrivalTimeQueue.isEmpty() && arrivalTimeQueue.peek().getArrivalTime() <= time) {
                remainingTimeQueue.offer(arrivalTimeQueue.poll());
            }
            if (switchLeft > 0) {
                result.addStep(new TimeStep(time, "CS", "", snapshot(remainingTimeQueue, null),
                        "", "Context switching"));
                switchLeft--;
                time++;
                continue;
            }
            SimProcess next = remainingTimeQueue.peek();
            SimProcess best = current != null && (next == null ||
                    current.getRemainingTime() <= next.getRemainingTime()) ? current : next;
            if (best == null) {
                result.addStep(new TimeStep(time, "IDLE", "", snapshot(remainingTimeQueue, null),
                        "", "CPU idle"));
                previous = null;
                switchPaid = false;
                time++;
                continue;
            }
            if (current != null && current != best) {
                remainingTimeQueue.offer(current);
                current = null;
            }
            if (!switchPaid && previous != null && previous != best && cs > 0) {
                result.addStep(new TimeStep(time, "CS", "", snapshot(remainingTimeQueue, null),
                        "", "Switching from " + previous.getId()));
                switchLeft = cs - 1;
                switchPaid = true;
                time++;
                continue;
            }
            if (current == null) {
                remainingTimeQueue.remove(best);
                current = best;
            }
            String readyState = snapshot(remainingTimeQueue, current);
            String event = previous != null && previous != current && previous.getRemainingTime() > 0
                    ? "Preempted " + previous.getId() : "SRTF";
            current.startIfFirst(time);
            current.executeOneUnit();
            if (current.getRemainingTime() == 0) {
                current.finish(time + 1);
                completed++;
                event = current.getId() + " completed at t=" + (time + 1);
            }
            result.addStep(new TimeStep(time, "RUN", current.getId(), readyState, "", event));
            time++;
            previous = current;
            switchPaid = false;
            if (current.getRemainingTime() == 0) current = null;
        }
        return result;
    }

    static String snapshot(PriorityQueue<SimProcess> active, SimProcess current) {
        List<SimProcess> sorted = new ArrayList<>(active);
        sorted.sort(Comparator.comparingInt(SimProcess::getRemainingTime)
                .thenComparingInt(SimProcess::getArrivalTime));
        StringJoiner text = new StringJoiner(", ", "processes queue:[", "]");
        for (SimProcess p : sorted) {
            text.add(p.getId() + "(remaining:" + p.getRemainingTime() + ")");
        }
        return (current == null ? "" : "active process:" + current.getId() + "; ") + text;
    }
}
