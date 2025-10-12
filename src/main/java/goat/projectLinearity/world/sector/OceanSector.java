package goat.projectLinearity.world.sector;

import org.bukkit.World;

/**
 * Sector wrapper for ocean regions so structure registrations can target them.
 * Uses the same floor shaping noise as the main chunk generator to provide
 * consistent seafloor estimates for placement heuristics.
 */
public class OceanSector extends SectorBase {
    private static final int MIN_FLOOR_Y = 80;
    private static final int MAX_FLOOR_Y = 147;

    @Override
    public int computeSurfaceY(World world, long seed, int wx, int wz) {
        double n1 = valueNoise2(seed ^ 0xA1B2C3D4L, (double) wx / 260.0, (double) wz / 260.0);
        double n2 = valueNoise2(seed ^ 0x5EEDBEEFL, (double) wx / 110.0, (double) wz / 110.0);
        double n3 = valueNoise2(seed ^ 0x13579BDFL, (double) wx / 40.0, (double) wz / 40.0);
        double h = n1 * 0.55 + n2 * 0.30 + n3 * 0.15;
        h = h * 2.0 - 1.0;
        int base = 116;
        int amplitude = 26;
        int floor = base + (int) Math.round(h * amplitude);
        double trench = valueNoise2(seed ^ 0xDEADC0DEL, (double) wx / 600.0, (double) wz / 600.0);
        if (trench > 0.75) {
            floor -= 8 + (int) Math.round((trench - 0.75) * 16.0);
        }
        if (floor < MIN_FLOOR_Y) {
            floor = MIN_FLOOR_Y;
        } else if (floor > MAX_FLOOR_Y) {
            floor = MAX_FLOOR_Y;
        }
        return floor;
    }
}
