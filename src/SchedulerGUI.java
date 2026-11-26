import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;


public class SchedulerGUI extends JFrame {


    private static final Color BG       = new Color(18,  18,  32);
    private static final Color PANEL_BG = new Color(28,  28,  48);
    private static final Color ACCENT   = new Color(99,  102, 241);
    private static final Color ACCENT2  = new Color(16,  185, 129);
    private static final Color ACCENT3  = new Color(245, 158,  11);
    private static final Color TEXT     = new Color(226, 226, 240);
    private static final Color TEXT_DIM = new Color(140, 140, 170);
    private static final Color ROW_ALT  = new Color(36,  36,  60);
    private static final Color BORDER_C = new Color(55,  55,  90);
    private static final Color C_RUN    = new Color(16,  185, 129);
    private static final Color C_CS     = new Color(245, 158,  11);
    private static final Color C_IDLE   = new Color(90,  90, 120);

    private static final Font FONT_BODY  = new Font("Segoe UI", Font.PLAIN,  14);
    private static final Font FONT_BOLD  = new Font("Segoe UI", Font.BOLD,   14);
    private static final Font FONT_HEAD  = new Font("Segoe UI", Font.BOLD,   22);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN,  12);
    private static final Font FONT_MONO  = new Font("Consolas",  Font.PLAIN,  13);

    private static final Color[] PROC_COLORS = {
        new Color(99,102,241), new Color(16,185,129), new Color(245,158,11),
        new Color(239,68,68),  new Color(14,165,233), new Color(217,70,239),
        new Color(251,191,36), new Color(52,211,153)
    };


    private final DefaultTableModel processModel;
    private final JTable processTable;
    private final JComboBox<String> algoCombo;
    private final JSpinner csSpinner, quantumSpinner, agingSpinner;
    private final JLabel quantumLabel, agingLabel;


    private final DefaultTableModel resultModel;
    private final JLabel avgResponseLabel, avgTurnaroundLabel, avgWaitingLabel, cpuUtilizationLabel;


    private final GanttPanel ganttPanel;
    private final JSlider    timeSlider;
    private final JSpinner   timeJumpSpinner;
    private final JLabel     timePosLabel;
    private final JLabel     kindLabel;
    private final JLabel     cpuLabel;
    private final JTextArea  tier1Area, tier2Area, eventArea;
    private final JButton    prevBtn, nextBtn;
    private final DefaultTableModel timelineModel;
    private final JTable            timelineTable;


    private TreeMap<Integer, List<TimeStep>> eventsByTime = new TreeMap<>();
    private final List<TimeRow> reconstructed = new ArrayList<>();
    private int maxTime = 0;
    private boolean syncingNav = false;

    private static class TimeRow {
        final int time;
        final String kind;
        final String cpu;
        final String tier1;
        final String tier2;
        final String event;

        TimeRow(int time, String kind, String cpu, String tier1, String tier2, String event) {
            this.time = time;
            this.kind = kind;
            this.cpu = cpu;
            this.tier1 = tier1;
            this.tier2 = tier2;
            this.event = event;
        }
    }


    public SchedulerGUI() {
        super("CPU Scheduling Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 840);
        setMinimumSize(new Dimension(940, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(10, 10));


        JPanel titleBar = darkPanel(new FlowLayout(FlowLayout.LEFT, 16, 10));
        JLabel title = label("CPU Scheduling Simulator", FONT_HEAD, ACCENT);
        JLabel sub   = label("  ·  SRTF   ACS (Aging + Priority)", FONT_SMALL, TEXT_DIM);
        titleBar.add(title); titleBar.add(sub);
        add(titleBar, BorderLayout.NORTH);


        JPanel left = new JPanel();
        left.setBackground(BG);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(new EmptyBorder(6, 8, 8, 4));
        left.setPreferredSize(new Dimension(340, 0));


        processModel = new DefaultTableModel(new String[]{"PID","Arrival","Burst","Priority"}, 0) {
            public boolean isCellEditable(int r, int c) { return true; }
        };
        processTable = styledTable(processModel);
        JScrollPane procScroll = styledScroll(processTable);
        procScroll.setPreferredSize(new Dimension(320, 180));

        JPanel procRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        procRow.setBackground(BG);
        procRow.add(label("Processes", FONT_BOLD, TEXT));
        procRow.add(Box.createHorizontalGlue());
        JButton addBtn = smallBtn("+ Add");
        JButton remBtn = smallBtn("− Remove");
        addBtn.addActionListener(e -> addDefaultProcess());
        remBtn.addActionListener(e -> { int r = processTable.getSelectedRow(); if (r>=0) processModel.removeRow(r); });
        procRow.add(addBtn); procRow.add(remBtn);
        left.add(procRow);
        left.add(vGap(4));
        left.add(procScroll);
        left.add(vGap(12));


        left.add(sectionLbl("Algorithm"));
        left.add(vGap(4));
        algoCombo = new JComboBox<>(new String[]{"SRTF","ACS"});
        styleCombo(algoCombo);
        left.add(algoCombo);
        left.add(vGap(12));


        left.add(sectionLbl("Parameters"));
        left.add(vGap(4));
        csSpinner      = styledSpinner(1, 0, 100, 1);
        quantumSpinner = styledSpinner(4, 1, 100, 1);
        agingSpinner   = styledSpinner(5, 1, 100, 1);
        quantumLabel   = label("Quantum",        FONT_BODY, TEXT_DIM);
        agingLabel     = label("Aging Interval", FONT_BODY, TEXT_DIM);
        left.add(paramRow(label("Context Switch", FONT_BODY, TEXT_DIM), csSpinner));
        left.add(vGap(3));
        left.add(paramRow(quantumLabel, quantumSpinner));
        left.add(vGap(3));
        left.add(paramRow(agingLabel,   agingSpinner));
        left.add(vGap(14));


        JButton runBtn = new JButton("▶  Run Simulation");
        runBtn.setFont(FONT_BOLD); runBtn.setBackground(ACCENT);
        runBtn.setForeground(Color.WHITE); runBtn.setFocusPainted(false);
        runBtn.setBorder(new EmptyBorder(10, 20, 10, 20));
        runBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        runBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        runBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        runBtn.addActionListener(e -> runSimulation());
        runBtn.addMouseListener(hover(runBtn, ACCENT, ACCENT.brighter()));
        left.add(runBtn);
        left.add(Box.createVerticalGlue());

        algoCombo.addActionListener(e -> updateParamVisibility());
        updateParamVisibility();
        add(left, BorderLayout.WEST);


        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(BG); tabs.setForeground(TEXT);
        tabs.setFont(FONT_BOLD);


        JPanel resultsTab = new JPanel(new BorderLayout(0, 10));
        resultsTab.setBackground(BG);
        resultsTab.setBorder(new EmptyBorder(8, 8, 8, 8));

        String[] resCols = {"PID","Arrival","Burst","Completion","Turnaround","Waiting","Response"};
        resultModel = new DefaultTableModel(resCols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable resultTable = styledTable(resultModel);
        resultTable.setDefaultRenderer(Object.class, new AlternatingRenderer());
        JScrollPane resScroll = styledScroll(resultTable);

        JPanel avgPanel = darkPanel(new GridLayout(1, 4, 10, 0));
        avgPanel.setBorder(new EmptyBorder(10, 14, 10, 14));
        avgResponseLabel   = avgLabel("Avg Response",   "—");
        avgTurnaroundLabel = avgLabel("Avg Turnaround", "—");
        avgWaitingLabel    = avgLabel("Avg Waiting",    "—");
        cpuUtilizationLabel = avgLabel("CPU Utilization", "—");
        avgPanel.add(avgResponseLabel);
        avgPanel.add(avgTurnaroundLabel);
        avgPanel.add(avgWaitingLabel);
        avgPanel.add(cpuUtilizationLabel);

        resultsTab.add(resScroll, BorderLayout.CENTER);
        resultsTab.add(avgPanel,  BorderLayout.SOUTH);
        tabs.addTab("Results", resultsTab);


        JPanel timelineTab = new JPanel(new BorderLayout(0, 8));
        timelineTab.setBackground(BG);
        timelineTab.setBorder(new EmptyBorder(8, 8, 8, 8));


        ganttPanel = new GanttPanel(t -> navigateToTime(t));
        JScrollPane ganttScroll = styledScroll(ganttPanel);
        ganttScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        ganttScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        ganttScroll.setPreferredSize(new Dimension(0, 96));

        timeSlider = new JSlider(0, 0, 0);
        timeSlider.setBackground(BG);
        timeSlider.setForeground(TEXT);
        timeSlider.setPaintTicks(true);
        timeSlider.setPaintLabels(true);
        timeSlider.setFont(FONT_SMALL);

        timeJumpSpinner = styledSpinner(0, 0, 0, 1);
        timePosLabel    = label("t = 0 / 0", FONT_BOLD, ACCENT);
        prevBtn         = smallBtn("◀ Prev");
        nextBtn         = smallBtn("Next ▶");
        prevBtn.addActionListener(e -> navigateToTime(timeSlider.getValue() - 1));
        nextBtn.addActionListener(e -> navigateToTime(timeSlider.getValue() + 1));

        JPanel navBar = new JPanel(new BorderLayout(10, 0));
        navBar.setBackground(BG);
        JPanel navControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        navControls.setBackground(BG);
        navControls.add(prevBtn);
        navControls.add(nextBtn);
        navControls.add(label("Go to t:", FONT_SMALL, TEXT_DIM));
        navControls.add(timeJumpSpinner);
        navControls.add(timePosLabel);
        navBar.add(navControls, BorderLayout.WEST);
        navBar.add(timeSlider,  BorderLayout.CENTER);

        JPanel timelineTop = new JPanel(new BorderLayout(0, 6));
        timelineTop.setBackground(BG);
        timelineTop.add(ganttScroll, BorderLayout.NORTH);
        timelineTop.add(navBar,      BorderLayout.CENTER);


        kindLabel = label("—", FONT_BOLD, TEXT);
        kindLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        cpuLabel  = label("—", FONT_BOLD, TEXT);

        tier1Area = monoArea();
        tier2Area = monoArea();
        eventArea = monoArea();
        eventArea.setForeground(ACCENT3);

        JPanel topCards = new JPanel(new GridLayout(1, 3, 8, 8));
        topCards.setBackground(BG);
        topCards.add(infoCard("CPU State", kindLabel));
        topCards.add(infoCard("CPU Activity / Process", cpuLabel));
        topCards.add(infoCard("Event at Time t", styledScroll(eventArea)));

        JPanel snapCards = new JPanel(new GridLayout(1, 2, 8, 8));
        snapCards.setBackground(BG);
        snapCards.add(infoCard("Tier-1 / Ready Queue Snapshot", styledScroll(tier1Area)));
        snapCards.add(infoCard("Tier-2 Snapshot",               styledScroll(tier2Area)));

        JPanel inspectorPanel = new JPanel(new GridLayout(2, 1, 0, 8));
        inspectorPanel.setBackground(BG);
        inspectorPanel.setPreferredSize(new Dimension(0, 175));
        inspectorPanel.add(topCards);
        inspectorPanel.add(snapCards);


        timelineModel = new DefaultTableModel(
                new String[]{"t", "Kind", "CPU", "Tier-1 / Queue", "Tier-2", "Event"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        timelineTable = styledTable(timelineModel);
        timelineTable.setDefaultRenderer(Object.class, new AlternatingRenderer());
        timelineTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        timelineTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        timelineTable.getColumnModel().getColumn(0).setMaxWidth(60);
        timelineTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        timelineTable.getColumnModel().getColumn(1).setMaxWidth(75);
        timelineTable.getColumnModel().getColumn(2).setPreferredWidth(65);
        timelineTable.getColumnModel().getColumn(2).setMaxWidth(85);
        timelineTable.getColumnModel().getColumn(3).setPreferredWidth(250);
        timelineTable.getColumnModel().getColumn(4).setPreferredWidth(180);
        timelineTable.getColumnModel().getColumn(5).setPreferredWidth(180);

        timelineTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !syncingNav) {
                int row = timelineTable.getSelectedRow();
                if (row >= 0 && row < reconstructed.size()) {
                    navigateToTime(reconstructed.get(row).time);
                }
            }
        });

        JSplitPane centerSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                inspectorPanel,
                styledScroll(timelineTable));
        centerSplit.setResizeWeight(0.35);
        centerSplit.setDividerSize(6);
        centerSplit.setBackground(BG);
        centerSplit.setBorder(null);

        timelineTab.add(timelineTop, BorderLayout.NORTH);
        timelineTab.add(centerSplit, BorderLayout.CENTER);

        timeSlider.addChangeListener(e -> {
            if (!syncingNav) navigateToTime(timeSlider.getValue());
        });
        timeJumpSpinner.addChangeListener(e -> {
            if (!syncingNav) navigateToTime((int) timeJumpSpinner.getValue());
        });

        tabs.addTab("Timeline", timelineTab);
        add(tabs, BorderLayout.CENTER);


        addProcess("P1", 0, 8, 2);
        addProcess("P2", 1, 4, 1);
        addProcess("P3", 2, 9, 3);
        addProcess("P4", 3, 5, 0);

        setVisible(true);
    }


    private void runSimulation() {
        List<SimProcess> procs = collectProcesses();
        if (procs == null) return;
        int cs   = (int) csSpinner.getValue();
        String a = (String) algoCombo.getSelectedItem();
        SimulationResult result;
        try {
            if ("SRTF".equals(a)) {
                result = SRTFScheduler.run(procs, cs);
            } else {
                result = ACSScheduler.run(procs, (int)quantumSpinner.getValue(), cs, (int)agingSpinner.getValue());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        displayResults(result);
    }

    private void displayResults(SimulationResult result) {

        resultModel.setRowCount(0);
        int lastCompletion = 0;
        for (SimProcess p : result.getProcesses()) {
            lastCompletion = Math.max(lastCompletion, p.getCompletionTime());
            resultModel.addRow(new Object[]{
                p.getId(), p.getArrivalTime(), p.getBurstTime(),
                p.getCompletionTime(), p.turnaroundTime(), p.waitingTime(), p.responseTime()
            });
        }
        setAvgLabel(avgResponseLabel,   "Avg Response",   result.averageResponse());
        setAvgLabel(avgTurnaroundLabel, "Avg Turnaround", result.averageTurnaround());
        setAvgLabel(avgWaitingLabel,    "Avg Waiting",    result.averageWaiting());
        setAvgLabel(cpuUtilizationLabel, "CPU Utilization (%)", result.cpuUtilization());


        eventsByTime.clear();
        int maxEventTime = 0;
        for (TimeStep s : result.getSteps()) {
            eventsByTime.computeIfAbsent(s.getTime(), k -> new ArrayList<>()).add(s);
            maxEventTime = Math.max(maxEventTime, s.getTime());
        }
        maxTime = result.getSteps().isEmpty() ? lastCompletion : Math.max(lastCompletion, maxEventTime);


        reconstructed.clear();
        timelineModel.setRowCount(0);
        TimeStep currentState = null;

        for (int t = 0; t <= maxTime; t++) {
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

            TimeRow row;
            if (currentState == null) {
                row = new TimeRow(t, "-", "-", "", "", eventDesc);
            } else {
                row = new TimeRow(
                        t,
                        currentState.getKind(),
                        currentState.getCpuActivity().isEmpty() ? "-" : currentState.getCpuActivity(),
                        currentState.getTier1Snapshot(),
                        currentState.getTier2Snapshot(),
                        eventDesc);
            }
            reconstructed.add(row);
            timelineModel.addRow(new Object[]{
                row.time, row.kind, row.cpu, row.tier1, row.tier2, row.event
            });
        }


        ganttPanel.setTimeline(eventsByTime, result.getProcesses(), maxTime);


        syncingNav = true;
        try {
            timeSlider.setMinimum(0);
            timeSlider.setMaximum(maxTime);
            int major = Math.max(1, maxTime / 10);
            timeSlider.setMajorTickSpacing(major);
            timeSlider.setMinorTickSpacing(1);
            timeSlider.setLabelTable(timeSlider.createStandardLabels(major));
            timeSlider.setValue(0);

            timeJumpSpinner.setModel(new SpinnerNumberModel(0, 0, Math.max(0, maxTime), 1));
            ((JSpinner.DefaultEditor) timeJumpSpinner.getEditor()).getTextField().setBackground(new Color(40,40,70));
            ((JSpinner.DefaultEditor) timeJumpSpinner.getEditor()).getTextField().setForeground(TEXT);
            ((JSpinner.DefaultEditor) timeJumpSpinner.getEditor()).getTextField().setFont(FONT_BODY);
        } finally {
            syncingNav = false;
        }

        navigateToTime(0);
    }


    private void navigateToTime(int t) {
        if (reconstructed.isEmpty()) return;
        int clamped = Math.max(0, Math.min(t, maxTime));

        syncingNav = true;
        try {
            if (timeSlider.getValue() != clamped) timeSlider.setValue(clamped);
            if (!Integer.valueOf(clamped).equals(timeJumpSpinner.getValue())) {
                timeJumpSpinner.setValue(clamped);
            }
            if (clamped < timelineTable.getRowCount() && timelineTable.getSelectedRow() != clamped) {
                timelineTable.setRowSelectionInterval(clamped, clamped);
                timelineTable.scrollRectToVisible(timelineTable.getCellRect(clamped, 0, true));
            }
        } finally {
            syncingNav = false;
        }

        prevBtn.setEnabled(clamped > 0);
        nextBtn.setEnabled(clamped < maxTime);
        timePosLabel.setText("t = " + clamped + " / " + maxTime);
        ganttPanel.setHighlight(clamped);

        TimeRow row = reconstructed.get(clamped);
        switch (row.kind) {
            case "RUN":
                kindLabel.setForeground(C_RUN);
                kindLabel.setText("▶ RUN");
                break;
            case "CS":
                kindLabel.setForeground(C_CS);
                kindLabel.setText("⇄ CS");
                break;
            case "IDLE":
                kindLabel.setForeground(C_IDLE);
                kindLabel.setText("— IDLE");
                break;
            default:
                kindLabel.setForeground(TEXT_DIM);
                kindLabel.setText("—");
        }

        cpuLabel.setText(row.cpu);
        tier1Area.setText(row.tier1.isEmpty() ? "(empty)" : row.tier1);
        tier2Area.setText(row.tier2.isEmpty() ? "(empty)" : row.tier2);
        eventArea.setText(row.event.isEmpty() ? "(no new event at t=" + clamped + ")" : row.event);
    }


    private List<SimProcess> collectProcesses() {
        List<SimProcess> list = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int r = 0; r < processModel.getRowCount(); r++) {
            try {
                String pid = processModel.getValueAt(r,0).toString().trim();
                int arr = Integer.parseInt(processModel.getValueAt(r,1).toString().trim());
                int bst = Integer.parseInt(processModel.getValueAt(r,2).toString().trim());
                int pri = Integer.parseInt(processModel.getValueAt(r,3).toString().trim());
                if (!ids.add(pid)) { err("Duplicate PID: " + pid); return null; }
                list.add(new SimProcess(pid, arr, bst, pri));
            } catch (Exception e) { err("Invalid data in row " + (r+1)); return null; }
        }
        if (list.isEmpty()) { err("Add at least one process."); return null; }
        return list;
    }

    private void addDefaultProcess() {
        int r = processModel.getRowCount();
        processModel.addRow(new Object[]{"P"+(r+1), r, 4, 0});
    }
    private void addProcess(String pid, int a, int b, int p) {
        processModel.addRow(new Object[]{pid, a, b, p});
    }
    private void updateParamVisibility() {
        boolean acs = "ACS".equals(algoCombo.getSelectedItem());
        quantumLabel.setVisible(acs); quantumSpinner.setVisible(acs);
        agingLabel.setVisible(acs);   agingSpinner.setVisible(acs);
    }
    private void err(String m) {
        JOptionPane.showMessageDialog(this, m, "Input Error", JOptionPane.WARNING_MESSAGE);
    }


    private static JPanel darkPanel(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(PANEL_BG);
        p.setBorder(BorderFactory.createLineBorder(BORDER_C));
        return p;
    }
    private static JLabel label(String t, Font f, Color c) {
        JLabel l = new JLabel(t); l.setFont(f); l.setForeground(c); return l;
    }
    private static JLabel sectionLbl(String t) {
        JLabel l = label(t, FONT_BOLD, ACCENT2);
        l.setAlignmentX(Component.LEFT_ALIGNMENT); return l;
    }
    private static JLabel avgLabel(String n, String v) {
        JLabel l = new JLabel("<html><center><span style='color:#8080aa;font-size:11px'>"
                + n + "</span><br><b style='font-size:16px;color:#e2e2f0'>" + v + "</b></center></html>",
                SwingConstants.CENTER);
        l.setFont(FONT_BODY); return l;
    }
    private static void setAvgLabel(JLabel lbl, String name, double val) {
        lbl.setText("<html><center><span style='color:#8080aa;font-size:11px'>"
                + name + "</span><br><b style='font-size:16px;color:#e2e2f0'>"
                + String.format("%.2f", val) + "</b></center></html>");
    }
    private static JPanel paramRow(JLabel lbl, JComponent ctrl) {
        JPanel p = new JPanel(new BorderLayout(8,0));
        p.setBackground(PANEL_BG); p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lbl, BorderLayout.WEST); p.add(ctrl, BorderLayout.EAST); return p;
    }
    private static JTable styledTable(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setBackground(PANEL_BG); t.setForeground(TEXT); t.setFont(FONT_BODY);
        t.setGridColor(BORDER_C); t.setRowHeight(26);
        t.setSelectionBackground(ACCENT.darker()); t.setSelectionForeground(Color.WHITE);
        t.setShowHorizontalLines(true); t.setShowVerticalLines(false);
        JTableHeader h = t.getTableHeader();
        h.setBackground(new Color(40,40,70)); h.setForeground(ACCENT); h.setFont(FONT_BOLD);
        return t;
    }
    private static JScrollPane styledScroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBackground(PANEL_BG); sp.getViewport().setBackground(PANEL_BG);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_C)); return sp;
    }
    private static JButton smallBtn(String t) {
        JButton b = new JButton(t);
        b.setFont(FONT_SMALL); b.setBackground(new Color(50,50,80)); b.setForeground(TEXT);
        b.setFocusPainted(false); b.setBorder(new EmptyBorder(4,10,4,10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b;
    }
    private static JSpinner styledSpinner(int v, int min, int max, int step) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(v, min, max, step));
        s.setFont(FONT_BODY); s.setPreferredSize(new Dimension(72, 28));
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setBackground(new Color(40,40,70));
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setForeground(TEXT);
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setFont(FONT_BODY); return s;
    }
    private static void styleCombo(JComboBox<String> c) {
        c.setFont(FONT_BODY); c.setBackground(new Color(40,40,70)); c.setForeground(TEXT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
    }
    private static JTextArea monoArea() {
        JTextArea a = new JTextArea(2, 20);
        a.setFont(FONT_MONO); a.setBackground(new Color(22,22,40)); a.setForeground(TEXT);
        a.setEditable(false); a.setLineWrap(true); a.setWrapStyleWord(true);
        a.setBorder(new EmptyBorder(4,6,4,6)); return a;
    }
    private static JPanel infoCard(String title, Component content) {
        JPanel p = new JPanel(new BorderLayout(0,4));
        p.setBackground(PANEL_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_C),
            new EmptyBorder(6,8,6,8)));
        JLabel lbl = new JLabel(title); lbl.setFont(FONT_SMALL); lbl.setForeground(TEXT_DIM);
        p.add(lbl, BorderLayout.NORTH); p.add(content, BorderLayout.CENTER); return p;
    }
    private static MouseAdapter hover(JButton b, Color normal, Color over) {
        return new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(over); }
            public void mouseExited (MouseEvent e) { b.setBackground(normal); }
        };
    }
    private static Component vGap(int h) { return Box.createRigidArea(new Dimension(0, h)); }


    private static class AlternatingRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setFont(FONT_BODY); setForeground(TEXT);
            setBackground(sel ? ACCENT.darker() : (row%2==0 ? PANEL_BG : ROW_ALT));
            setHorizontalAlignment(col==0 ? LEFT : CENTER); return this;
        }
    }


    interface TimeSelectListener { void onSelect(int t); }

    static class GanttPanel extends JPanel {
        private final List<int[]> blocks = new ArrayList<>();
        private final List<String> blockKinds = new ArrayList<>();
        private final List<String> blockLabels = new ArrayList<>();
        private final Map<String, Color> colorMap = new HashMap<>();
        private int endTime = 0;
        private int highlight = -1;
        private static final int X_PAD = 10, BW = 40, BH = 40, YO = 10, LH = 16;

        GanttPanel(TimeSelectListener listener) {
            setBackground(PANEL_BG);
            setPreferredSize(new Dimension(600, BH + LH + YO + 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (endTime <= 0) return;
                    int t = Math.max(0, Math.min(endTime, (e.getX() - X_PAD) / BW));
                    listener.onSelect(t);
                }
            });
        }

        void setTimeline(TreeMap<Integer, List<TimeStep>> eventsByTime,
                         List<SimProcess> procs, int endTime) {
            this.endTime = endTime;
            blocks.clear();
            blockKinds.clear();
            blockLabels.clear();
            colorMap.clear();
            int idx = 0;
            for (SimProcess p : procs) colorMap.put(p.getId(), PROC_COLORS[idx++ % PROC_COLORS.length]);

            if (endTime > 0 && !eventsByTime.isEmpty()) {
                TimeStep cur = null;
                String curKind = null, curLbl = null;
                int blockStart = 0;

                for (int t = 0; t < endTime; t++) {
                    List<TimeStep> atT = eventsByTime.get(t);
                    if (atT != null && !atT.isEmpty()) cur = atT.get(atT.size() - 1);
                    String k = (cur == null) ? "IDLE" : cur.getKind();
                    String l = (cur == null) ? "IDLE" : ganttLabel(cur);
                    if (curKind == null) {
                        curKind = k; curLbl = l; blockStart = t;
                    } else if (!curKind.equals(k) || !curLbl.equals(l)) {
                        blocks.add(new int[]{blockStart, t});
                        blockKinds.add(curKind);
                        blockLabels.add(curLbl);
                        curKind = k; curLbl = l; blockStart = t;
                    }
                }
                if (curKind != null) {
                    blocks.add(new int[]{blockStart, endTime});
                    blockKinds.add(curKind);
                    blockLabels.add(curLbl);
                }
            }

            setPreferredSize(new Dimension(Math.max(1, endTime) * BW + 60, BH + LH + YO + 18));
            revalidate(); repaint();
        }

        void setHighlight(int t) {
            this.highlight = t;
            if (t >= 0) {
                int cx = X_PAD + t * BW;
                scrollRectToVisible(new Rectangle(Math.max(0, cx - BW), 0, BW * 3, getHeight()));
            }
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (blocks.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (int i = 0; i < blocks.size(); i++) {
                int[] range = blocks.get(i);
                int tStart  = range[0], tEnd = range[1];
                String kind = blockKinds.get(i);
                String lbl  = blockLabels.get(i);

                int x = X_PAD + tStart * BW;
                int w = Math.max(4, (tEnd - tStart) * BW - 2);

                Color fill;
                if ("CS".equals(kind))        fill = new Color(80, 80, 110);
                else if ("IDLE".equals(kind)) fill = new Color(50, 50, 70);
                else                          fill = colorMap.getOrDefault(lbl, ACCENT);

                g2.setColor(fill);
                g2.fill(new RoundRectangle2D.Float(x, YO, w, BH, 8, 8));
                g2.setColor(fill.brighter());
                g2.draw(new RoundRectangle2D.Float(x, YO, w, BH, 8, 8));

                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int lx = x + (w - fm.stringWidth(lbl)) / 2;
                int ly = YO + BH / 2 + fm.getAscent() / 2 - 2;
                if (w > fm.stringWidth(lbl) + 4) g2.drawString(lbl, lx, ly);

                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(TEXT_DIM);
                g2.drawString(String.valueOf(tStart), x, YO + BH + LH);
            }

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(TEXT_DIM);
            g2.drawString(String.valueOf(endTime), X_PAD + endTime * BW, YO + BH + LH);


            if (highlight >= 0) {
                int cx = X_PAD + highlight * BW;
                if (highlight < endTime) {
                    g2.setColor(new Color(255, 255, 255, 55));
                    g2.fillRect(cx, YO, BW - 2, BH);
                }
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(cx, YO - 4, cx, YO + BH + 4);
            }
        }

        private static String ganttLabel(TimeStep step) {
            switch (step.getKind()) {
                case "RUN":  return step.getCpuActivity();
                case "CS":   return "CS";
                case "IDLE": return "IDLE";
                default:     return "?";
            }
        }
    }


    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(SchedulerGUI::new);
    }
}
