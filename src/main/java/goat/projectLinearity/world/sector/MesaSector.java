package goat.projectLinearity.world.sector;

import goat.projectLinearity.world.ConsegrityRegions;
import org.bukkit.World;

public class MesaSector extends SectorBase {
    @Override
    public int computeSurfaceY(World world, long seed, int wx, int wz) {
        double h1 = valueNoise2(seed ^ 0xBADA11D5L, (double) wx / 300.0, (double) wz / 300.0);
        double h2 = valueNoise2(seed ^ 0xBADA11D6L, (double) wx / 96.0, (double) wz / 96.0);
        double h3 = valueNoise2(seed ^ 0xBADA11D7L, (double) wx / 36.0, (double) wz / 36.0);
        double h = (h1 * 0.5 + h2 * 0.3 + h3 * 0.2) * 2.0 - 1.0;
        int base = 175;
        int amp = 28;
        return base + (int) Math.round(h * amp);
    }
}

