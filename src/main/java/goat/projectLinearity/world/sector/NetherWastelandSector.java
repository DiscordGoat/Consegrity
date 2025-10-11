package goat.projectLinearity.world.sector;

import goat.projectLinearity.world.ConsegrityRegions;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

/**
 * Marker sector for the custom Nether wasteland ring. Terrain is handled directly by the
 * ConsegrityNetherChunkGenerator; this sector exists so structure systems can target the region.
 */
public class NetherWastelandSector extends SectorBase {

    @Override
    public int computeSurfaceY(World world, long seed, int wx, int wz) {
        double h1 = valueNoise2(seed ^ 0x4F91CE1AL, wx / 180.0, wz / 180.0);
        double h2 = valueNoise2(seed ^ 0x1B9B5A73L, wx / 60.0, wz / 60.0);
        double h3 = valueNoise2(seed ^ 0x73B21F4DL, wx / 28.0, wz / 28.0);
        double blended = h1 * 0.55 + h2 * 0.3 + h3 * 0.15;
        return 60 + (int) Math.round((blended * 2.0 - 1.0) * 10.0);
    }

    @Override
    public void decorate(World world,
                         ChunkGenerator.ChunkData data,
                         long seed,
                         int chunkX,
                         int chunkZ,
                         int[][] topYGrid,
                         ConsegrityRegions.Region[][] regionGrid,
                         double[][] centralMaskGrid) {
        // Decorations provided within the custom nether generator.
    }
}
