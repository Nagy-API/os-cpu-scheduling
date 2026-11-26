import javax.swing.SwingUtilities;
import java.util.Arrays;
import java.util.List;

/**
 * Entry point.
 * Run with no args → opens the GUI.
 * Run with --cli → runs a quick console demo for both SRTF and ACS.
 */
public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && "--cli".equalsIgnoreCase(args[0])) {
            runCli();
            return;
        }
        SwingUtilities.invokeLater(SchedulerGUI::new);

    }

    private static void runCli() {
        ConsolePrinter printer = new ConsolePrinter();

        // ── SRTF demo ──────────────────────────────────────────────────────
        List<SimProcess> srtfProcs = Arrays.asList(
                new SimProcess("P1", 0, 8, 0),
                new SimProcess("P2", 1, 4, 0),
                new SimProcess("P3", 2, 9, 0),
                new SimProcess("P4", 3, 5, 0));
        SimulationResult srtfResult = SRTFScheduler.run(srtfProcs, 1);
        printer.print(srtfResult);

        // ── ACS demo ───────────────────────────────────────────────────────
        List<SimProcess> acsProcs = Arrays.asList(
                new SimProcess("P1", 0, 8, 2),
                new SimProcess("P2", 1, 4, 1),
                new SimProcess("P3", 2, 9, 3),
                new SimProcess("P4", 3, 5, 0));
        // quantum=4, cs=1, agingInterval=5
        SimulationResult acsResult = ACSScheduler.run(acsProcs, 4, 1, 5);
        printer.print(acsResult);
    }
}
