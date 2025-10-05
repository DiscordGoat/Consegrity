package goat.projectLinearity.util;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Simple TPS monitor using a 20-tick sampling window and EMA smoothing.
 */
public final class TpsMonitor extends BukkitRunnable {
    private static final double ALPHA = 0.2; // smoothing factor
    private volatile double tps = 20.0;
    private long lastNs = System.nanoTime();

    public double getTps() {
        return tps;
    }

    public boolean isHealthy() {
        return tps >= 18.0;
    }

    @Override
    public void run() {
        long now = System.nanoTime();
        long dt = now - lastNs;
        if (dt <= 0) dt = 1;
        lastNs = now;
        double seconds = dt / 1_000_000_000.0;
        double sample = 20.0 / Math.max(1e-6, seconds); // 20 ticks elapsed
        if (sample > 20.0) sample = 20.0;
        // exponential moving average
        tps = (ALPHA * sample) + ((1.0 - ALPHA) * tps);
    }
}

