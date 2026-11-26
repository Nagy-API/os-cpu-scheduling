import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SchedulerTests {
    private static int checks = 0;

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }

    private static void equal(int expected, int actual, String message) {
        check(expected == actual, message + ": expected " + expected + ", got " + actual);
    }

    private static void close(double expected, double actual, String message) {
        check(Math.abs(expected - actual) < 0.00001,
                message + ": expected " + expected + ", got " + actual);
    }

    private static void verify(SimulationResult result, List<SimProcess> inputs) {
        int burstTotal = 0;
        int completion = 0;
        for (SimProcess p : result.getProcesses()) {
            check(p.getRemainingTime() == 0, "Process did not finish");
            check(p.getCompletionTime() > p.getArrivalTime(), "Invalid completion");
            check(p.waitingTime() >= 0, "Negative waiting");
            check(p.responseTime() >= 0, "Negative response");
            equal(p.turnaroundTime(), p.waitingTime() + p.getBurstTime(), "TAT");
            burstTotal += p.getBurstTime();
            completion = Math.max(completion, p.getCompletionTime());
        }
        equal(completion, result.getSteps().size(), "Timeline length");
        int runSteps = 0;
        for (int t = 0; t < result.getSteps().size(); t++) {
            TimeStep step = result.getSteps().get(t);
            equal(t, step.getTime(), "Timeline order");
            if ("RUN".equals(step.getKind())) runSteps++;
        }
        equal(burstTotal, runSteps, "CPU work");
        close(100.0 * burstTotal / completion, result.cpuUtilization(), "CPU utilization");
        for (SimProcess input : inputs) {
            equal(input.getBurstTime(), input.getRemainingTime(), "Original process changed");
            equal(-1, input.getCompletionTime(), "Original completion changed");
        }
    }

    private static void officialCases() {
        List<SimProcess> first = List.of(
                new SimProcess("P1", 0, 4, 2),
                new SimProcess("P2", 1, 7, 3),
                new SimProcess("P3", 3, 2, 1));
        SimulationResult a = ACSScheduler.run(first, 3, 0, 4);
        verify(a, first);
        equal(9, a.getProcesses().get(0).getCompletionTime(), "Case 1 P1");
        equal(13, a.getProcesses().get(1).getCompletionTime(), "Case 1 P2");
        equal(8, a.getProcesses().get(2).getCompletionTime(), "Case 1 P3");
        close(13.0 / 3, a.averageWaiting(), "Case 1 waiting");
        close(26.0 / 3, a.averageTurnaround(), "Case 1 turnaround");

        List<SimProcess> second = List.of(
                new SimProcess("P1", 0, 5, 4),
                new SimProcess("P2", 1, 6, 2),
                new SimProcess("P3", 4, 4, 3));
        SimulationResult b = ACSScheduler.run(second, 3, 1, 4);
        verify(b, second);
        equal(18, b.getProcesses().get(0).getCompletionTime(), "Case 2 P1");
        equal(15, b.getProcesses().get(1).getCompletionTime(), "Case 2 P2");
        equal(20, b.getProcesses().get(2).getCompletionTime(), "Case 2 P3");
        close(11.0, b.averageWaiting(), "Case 2 waiting");
        close(16.0, b.averageTurnaround(), "Case 2 turnaround");
        close(75.0, b.cpuUtilization(), "Case 2 CPU");
    }

    private static void edgeCases() {
        List<SimProcess> srtf = List.of(
                new SimProcess("P1", 0, 8, 1),
                new SimProcess("P2", 2, 2, 1));
        SimulationResult a = SRTFScheduler.run(srtf, 2);
        verify(a, srtf);
        equal(6, a.getProcesses().get(1).getCompletionTime(), "SRTF preemption");
        equal(14, a.getProcesses().get(0).getCompletionTime(), "SRTF restart");

        List<SimProcess> idle = List.of(new SimProcess("P1", 4, 1, 1));
        SimulationResult b = SRTFScheduler.run(idle, 3);
        verify(b, idle);
        equal(5, b.getProcesses().get(0).getCompletionTime(), "Initial idle");

        List<SimProcess> nonPreemptive = List.of(
                new SimProcess("P1", 0, 10, 2),
                new SimProcess("P2", 5, 1, 1));
        SimulationResult c = ACSScheduler.run(nonPreemptive, 2, 0, 4);
        verify(c, nonPreemptive);
        equal(10, c.getProcesses().get(0).getCompletionTime(), "Tier 2 non-preemption");
        equal(11, c.getProcesses().get(1).getCompletionTime(), "Later Tier 1 process");

        PriorityTier tier = new PriorityTier(4);
        SimProcess p = new SimProcess("P", 0, 8, 4);
        p.startIfFirst(0);
        p.executeOneUnit();
        tier.add(p, 1);
        equal(4, tier.effectivePriority(p, 1), "Initial priority");
        equal(2, tier.effectivePriority(p, 9), "Aging after 8 units");
        equal(-1, tier.effectivePriority(p, 21), "Aging below zero");
    }

    private static void timelineDisplay() {
        java.io.PrintStream original = System.out;
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        try {
            System.setOut(new java.io.PrintStream(output));
            new ConsolePrinter().print(SRTFScheduler.run(
                    List.of(new SimProcess("P1", 0, 1, 1)), 0));
        } finally {
            System.setOut(original);
        }
        String text = output.toString();
        check(text.contains("Timeline (t = 0 .. 0)"), "Incorrect timeline last step");
        check(!text.matches("(?s).*\\n1 +RUN +P1.*"), "Phantom RUN at completion boundary");
    }

    private static void randomCases() {
        Random random = new Random(241);
        for (int test = 0; test < 1500; test++) {
            List<SimProcess> processes = new ArrayList<>();
            int n = 1 + random.nextInt(8);
            for (int i = 0; i < n; i++) {
                processes.add(new SimProcess("P" + i,
                        random.nextInt(13), 1 + random.nextInt(12), random.nextInt(6)));
            }
            int cs = random.nextInt(4);
            verify(SRTFScheduler.run(processes, cs), processes);
            verify(ACSScheduler.run(processes, 1 + random.nextInt(6),
                    cs, 1 + random.nextInt(6)), processes);
        }
    }

    public static void main(String[] args) {
        officialCases();
        edgeCases();
        timelineDisplay();
        randomCases();
        System.out.println("PASS: " + checks + " assertions");
    }
}
