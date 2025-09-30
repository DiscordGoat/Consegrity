package goat.projectLinearity.world.sector;

import goat.projectLinearity.world.ConsegrityRegions;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

public class JungleSector extends SectorBase {
    @Override
    public int computeSurfaceY(World world, long seed, int wx, int wz) {
        double h1 = valueNoise2(seed ^ 0x1B6C1E11L, (double) wx / 220.0, (double) wz / 220.0);
        double h2 = valueNoise2(seed ^ 0x1B6C1E12L, (double) wx / 70.0, (double) wz / 70.0);
        double h = (h1 * 0.55 + h2 * 0.45) * 2.0 - 1.0;
        int base = 169;
        int amp = 16;
        return base + (int) Math.round(h * amp);
    }
}

