package goat.projectLinearity.world;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Deque;

/**
 * Pre-generates a square area centered at (0,0) efficiently with progress broadcasts.
 * Processes chunks in outward rings and adapts per-tick budget based on TPS.
 */
public final class ChunkPreGenerator extends BukkitRunnable {
    private final ProjectLinearity plugin;
    private final World world;
    private final int chunkMin;
    private final int chunkMax;
    private final Queue<int[]> queue = new ArrayDeque<>();
    private final Deque<long[]> recent = new ArrayDeque<>();
    private final int total;
    private int processed = 0;
    private long lastBroadcastMs = 0L; // retained for reference; no longer used to throttle
    private long lastStatsMs = 0L;
    private long lastAdaptMs = 0L;
    private int processedAtLastStats = 0;
    private double cpsMeasured = 0.0;   // chunks per second (measured)
    private double cpsTarget = 10.0;    // desired CPS, 1..1000, adaptive
    private double budgetAcc = 0.0;     // fractional chunks-per-tick accumulator
    private int keepWindow = 512;       // max recently kept chunks before unloading older

    public interface Completion {
        void onDone();
    }

    private final Completion onDone;

    public ChunkPreGenerator(ProjectLinearity plugin, World world, int radiusBlocks, Completion onDone) {
        this.plugin = plugin;
        this.world = world;
        this.onDone = onDone;
        // Convert radius in blocks to chunk bounds
        int r = Math.max(0, radiusBlocks);
        this.chunkMin = (int)Math.floor((-r) / 16.0);
        this.chunkMax = (int)Math.floor(( r) / 16.0);
        fillQueueSpiral();
        this.total = queue.size();
    }

    private void fillQueueSpiral() {
        // Quadrant + region-major order for better IO locality.
        // Process the square area in four quadrants centered at (0,0),
        // completing one quadrant before moving to the next.
        // Order: SE (x>=0,z>=0) -> SW (x<0,z>=0) -> NW (x<0,z<0) -> NE (x>=0,z<0)
        addQuadrantRegionMajor(0, chunkMax, 0, chunkMax);           // SE
        addQuadrantRegionMajor(chunkMin, -1, 0, chunkMax);          // SW
        addQuadrantRegionMajor(chunkMin, -1, chunkMin, -1);         // NW
        addQuadrantRegionMajor(0, chunkMax, chunkMin, -1);          // NE
    }

    private static int floorDiv32(int v) { return Math.floorDiv(v, 32); }

    private void addQuadrantRegionMajor(int cxMin, int cxMax, int czMin, int czMax) {
        if (cxMin > cxMax || czMin > czMax) return;
        int rminX = floorDiv32(cxMin);
        int rmaxX = floorDiv32(cxMax);
        int rminZ = floorDiv32(czMin);
        int rmaxZ = floorDiv32(czMax);
        for (int rz = rminZ; rz <= rmaxZ; rz++) {
            for (int rx = rminX; rx <= rmaxX; rx++) {
                int baseCx = rx * 32;
                int baseCz = rz * 32;
                int fromCx = Math.max(cxMin, baseCx);
                int toCx   = Math.min(cxMax, baseCx + 31);
                int fromCz = Math.max(czMin, baseCz);
                int toCz   = Math.min(czMax, baseCz + 31);
                for (int cz = fromCz; cz <= toCz; cz++) {
                    for (int cx = fromCx; cx <= toCx; cx++) {
                        queue.add(new int[]{cx, cz});
                    }
                }
            }
        }
    }

    private static int clamp(int min, int max, int v) { return Math.max(min, Math.min(max, v)); }

    @Override
    public void run() {
        // Derive per-tick chunk budget from target CPS and current TPS
        int toProcess = 1;
        try {
            double tps = (plugin.getTpsMonitor() != null) ? plugin.getTpsMonitor().getTps() : 20.0;
            if (tps < 1e-3) tps = 1.0;
            // accumulate fractional budget so CPS target is met across ticks
            double perTick = cpsTarget / tps;
            if (!Double.isFinite(perTick) || perTick < 0) perTick = 1.0;
            budgetAcc += perTick;
            toProcess = (int) Math.floor(budgetAcc);
            if (toProcess < 0) toProcess = 0;
            // Keep a reasonable per-tick upper bound to avoid long single-tick stalls
            if (toProcess > 1024) toProcess = 1024;
        } catch (Throwable ignored) {}

        int processedNow = 0;
        while (processedNow < toProcess && !queue.isEmpty()) {
            int[] c = queue.poll();
            if (c == null) break;
            try {
                world.getChunkAt(c[0], c[1]).load(true);
            } catch (Throwable ignored) {}
            processed++;
            processedNow++;
            // Track recent chunks for potential unloading
            recent.addLast(new long[]{c[0], c[1]});
            if (recent.size() > keepWindow) {
                long[] old = recent.pollFirst();
                if (old != null) {
                    try {
                        if (world.isChunkLoaded((int)old[0], (int)old[1])) {
                            world.unloadChunk((int)old[0], (int)old[1]);
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }
        // remove what we actually processed from the accumulator
        budgetAcc -= processedNow;

        // Measure CPS each tick over real time, with smoothing
        long now = System.currentTimeMillis();
        if (lastStatsMs == 0L) lastStatsMs = now;
        double secs = Math.max(0.001, (now - lastStatsMs) / 1000.0);
        double cpsInstant = (processed - processedAtLastStats) / secs;
        // Smooth the CPS to avoid jitter
        cpsMeasured = (cpsMeasured * 0.7) + (cpsInstant * 0.3);
        processedAtLastStats = processed;
        lastStatsMs = now;

        // Broadcast progress every tick
        int pct = total == 0 ? 100 : (int)Math.floor((processed * 100.0) / total);
        try {
            Bukkit.broadcastMessage("[Consegrity] Pre-generating: " + pct + "% (" + processed + "/" + total + ") Efficiency: " + (int)Math.round(cpsMeasured) + " CPS");
        } catch (Throwable ignored) {}

        // Adaptive control once per second to target ~18 TPS
        if (lastAdaptMs == 0L) lastAdaptMs = now;
        if (now - lastAdaptMs >= 1000L) {
            try {
                double tps = (plugin.getTpsMonitor() != null) ? plugin.getTpsMonitor().getTps() : 20.0;
                // target band: 17.5 .. 18.5 TPS
                if (tps > 18.5) {
                    double step = (tps > 19.5) ? 1.25 : 1.10;
                    cpsTarget = Math.min(1000.0, Math.max(1.0, cpsTarget * step));
                } else if (tps < 17.5) {
                    double step = (tps < 15.0) ? 0.75 : 0.9;
                    cpsTarget = Math.max(1.0, cpsTarget * step);
                } // else within band: hold steady
            } catch (Throwable ignored) {}
            lastAdaptMs = now;
        }

        if (queue.isEmpty()) {
            try { Bukkit.broadcastMessage("[Consegrity] Pre-generation complete."); } catch (Throwable ignored) {}
            cancel();
            try { if (onDone != null) onDone.onDone(); } catch (Throwable ignored) {}
        }
    }
}
