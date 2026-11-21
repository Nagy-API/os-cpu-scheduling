import java.util.ArrayList;
import java.util.List;

public class PriorityTier {
    private final List<SimProcess> queue = new ArrayList<>();
    private final int agingInterval;

    public PriorityTier(int agingInterval) {
        if (agingInterval <= 0) throw new IllegalArgumentException("Aging interval must be > 0");
        this.agingInterval = agingInterval;
    }

    public void add(SimProcess process, int time) {
        throw new UnsupportedOperationException("TODO: Member 4");
    }
    public SimProcess chooseNext(int time) {
        throw new UnsupportedOperationException("TODO: Member 4");
    }
    public int effectivePriority(SimProcess process, int time) {
        throw new UnsupportedOperationException("TODO: Member 4");
    }
    public String snapshot(int time) {
        throw new UnsupportedOperationException("TODO: Member 4");
    }
    public boolean isEmpty() { return queue.isEmpty(); }
}
