import javax.swing.SwingUtilities;
import java.util.Arrays;
import java.util.List;


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


        List<SimProcess> srtfProcs = Arrays.asList(
                new SimProcess("P1", 0, 8, 0),
                new SimProcess("P2", 1, 4, 0),
                new SimProcess("P3", 2, 9, 0),
                new SimProcess("P4", 3, 5, 0));
        SimulationResult srtfResult = SRTFScheduler.run(srtfProcs, 1);
        printer.print(srtfResult);


        List<SimProcess> acsProcs = Arrays.asList(
                new SimProcess("P1", 0, 8, 2),
                new SimProcess("P2", 1, 4, 1),
                new SimProcess("P3", 2, 9, 3),
                new SimProcess("P4", 3, 5, 0));

        SimulationResult acsResult = ACSScheduler.run(acsProcs, 4, 1, 5);
        printer.print(acsResult);
    }
}
