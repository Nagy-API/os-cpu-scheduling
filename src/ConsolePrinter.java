import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;


public class ConsolePrinter {

    private static final String RESET   = "\u001B[0m";
    private static final String GREEN   = "\u001B[32m";
    private static final String YELLOW  = "\u001B[33m";
    private static final String CYAN    = "\u001B[36m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String BOLD    = "\u001B[1m";
    private static final String DIM     = "\u001B[2m";

    public void print(SimulationResult result) {
        List<SimProcess> procs = result.getProcesses();
        List<TimeStep>   steps = result.getSteps();

        int lastCompletion = 0;
        for (SimProcess p : procs) {
            lastCompletion = Math.max(lastCompletion, p.getCompletionTime());
        }


        line('=', 110);
        System.out.println(BOLD + "  Algorithm : " + result.getAlgorithmName() + RESET);
        line('=', 110);

        if (steps.isEmpty()) {
            System.out.println("  No timeline data.");
        } else {
            printTimeline(steps, lastCompletion);
            line('-', 110);
            System.out.println(BOLD + "  Gantt Chart:" + RESET);
            printGantt(steps, lastCompletion);
        }


        line('-', 110);
        System.out.println(BOLD + "  Process Statistics" + RESET);
        line('-', 110);
        System.out.printf(CYAN + "%-6s %8s %8s %12s %12s %9s %10s%n" + RESET,
                "PID", "Arrival", "Burst", "Completion", "Turnaround", "Waiting", "Response");
        line('-', 110);
        for (SimProcess p : procs) {
            System.out.printf("%-6s %8d %8d %12d %12d %9d %10d%n",
                    p.getId(),
                    p.getArrivalTime(),
                    p.getBurstTime(),
                    p.getCompletionTime(),
                    p.turnaroundTime(),
                    p.waitingTime(),
                    p.responseTime());
        }
        line('-', 110);


        System.out.printf(YELLOW +
                "  Avg Response  : %.2f%n" +
                "  Avg Turnaround: %.2f%n" +
                "  Avg Waiting   : %.2f%n" +
                "  CPU Utilization: %.2f%%%n" + RESET,
                result.averageResponse(),
                result.averageTurnaround(),
                result.averageWaiting(),
                result.cpuUtilization());

        line('=', 110);
        System.out.println();
    }


    private static void printTimeline(List<TimeStep> steps, int lastCompletion) {
        TreeMap<Integer, List<TimeStep>> eventsByTime = new TreeMap<>();
        int maxEventTime = 0;
        for (TimeStep s : steps) {
            eventsByTime.computeIfAbsent(s.getTime(), k -> new ArrayList<>()).add(s);
            maxEventTime = Math.max(maxEventTime, s.getTime());
        }

        int endTime = Math.max(lastCompletion, maxEventTime);

        System.out.println(BOLD + "  Timeline (t = 0 .. " + endTime + ")" + RESET);
        line('-', 110);
        System.out.printf(BOLD + "%-5s %-6s %-7s  %-38s %-24s  %s%n" + RESET,
                "t", "Kind", "CPU", "Tier-1 / Queue", "Tier-2", "Event");
        line('-', 110);

        TimeStep currentState = null;

        for (int t = 0; t <= endTime; t++) {
            List<TimeStep> atT = eventsByTime.get(t);
            String eventDesc = "";
            if (atT != null && !atT.isEmpty()) {
                currentState = atT.get(atT.size() - 1);
                StringBuilder ev = new StringBuilder();
                for (TimeStep s : atT) {
                    if (!s.getEvent().isEmpty()) {
                        if (ev.length() > 0) ev.append(" | ");
                        ev.append(s.getEvent());
                    }
                }
                eventDesc = ev.toString();
            }

            if (currentState == null) {
                System.out.printf("%-5d %-6s %-7s  %-38s %-24s  %s%n",
                        t, "-", "-", "-", "-", "");
                continue;
            }


            String kindPlain = String.format("%-6s", currentState.getKind());
            String kindColored;
            switch (currentState.getKind()) {
                case "RUN":  kindColored = GREEN  + kindPlain + RESET; break;
                case "CS":   kindColored = YELLOW + kindPlain + RESET; break;
                default:     kindColored = DIM    + kindPlain + RESET; break;
            }

            String cpu = currentState.getCpuActivity().isEmpty() ? "-" : currentState.getCpuActivity();
            String t1  = trunc(currentState.getTier1Snapshot(), 38);
            String t2  = trunc(currentState.getTier2Snapshot(), 24);
            String evOut = eventDesc.isEmpty() ? "" : (MAGENTA + eventDesc + RESET);

            System.out.printf("%-5d %s %-7s  %-38s %-24s  %s%n",
                    t,
                    kindColored,
                    cpu,
                    t1.isEmpty() ? "-" : t1,
                    t2.isEmpty() ? "-" : t2,
                    evOut);
        }
    }


    private static void printGantt(List<TimeStep> steps, int lastCompletion) {
        TreeMap<Integer, TimeStep> eventAt = new TreeMap<>();
        for (TimeStep s : steps) {
            eventAt.put(s.getTime(), s);
        }
        int endTime = Math.max(lastCompletion, eventAt.lastKey());
        if (endTime <= 0) return;


        String[] labels = new String[endTime];
        TimeStep current = null;
        for (int t = 0; t < endTime; t++) {
            TimeStep ev = eventAt.get(t);
            if (ev != null) current = ev;
            labels[t] = (current == null) ? "---" : ganttLabel(current);
        }

        StringBuilder top = new StringBuilder("|");
        StringBuilder bot = new StringBuilder("0");
        int blockStart = 0;
        String blockLabel = labels[0];

        for (int t = 1; t <= endTime; t++) {
            String nextLabel = (t < endTime) ? labels[t] : null;
            if (!blockLabel.equals(nextLabel)) {
                String cell = " " + blockLabel + " ";
                int innerW = Math.max(cell.length(), 6);
                top.append(center(cell, innerW)).append("|");

                String timeStr = String.valueOf(t);
                int pad = (innerW + 1) - timeStr.length();
                bot.append(" ".repeat(Math.max(1, pad))).append(timeStr);

                if (t < endTime) {
                    blockStart = t;
                    blockLabel = nextLabel;
                }
            }
        }

        System.out.println(top);
        System.out.println(bot);
    }

    private static String ganttLabel(TimeStep step) {
        switch (step.getKind()) {
            case "RUN":  return step.getCpuActivity();
            case "CS":   return "CS";
            case "IDLE": return "---";
            default:     return "?";
        }
    }

    private static String center(String s, int width) {
        if (s.length() >= width) return s;
        int pad   = width - s.length();
        int left  = pad / 2;
        int right = pad - left;
        return " ".repeat(left) + s + " ".repeat(right);
    }

    private static String trunc(String s, int max) {
        if (s == null || s.isEmpty()) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static void line(char ch, int len) {
        System.out.println(String.valueOf(ch).repeat(len));
    }
}
