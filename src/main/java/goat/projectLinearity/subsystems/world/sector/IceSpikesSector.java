package goat.projectLinearity.subsystems.world.sector;

import goat.projectLinearity.subsystems.world.ConsegrityRegions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.type.Snow;
import org.bukkit.generator.ChunkGenerator;

import java.util.SplittableRandom;

public class IceSpikesSector extends SectorBase {
    @Override
    public int computeSurfaceY(World world, long seed, int wx, int wz) {
        double h1 = valueNoise2(seed ^ 0x11CE5EEDL, (double) wx / 240.0, (double) wz / 240.0);
        double h2 = valueNoise2(seed ^ 0x22CE5EEDL, (double) wx / 84.0, (double) wz / 84.0);
        double h = (h1 * 0.65 + h2 * 0.35) * 2.0 - 1.0;
        int base = 171;
        int amp = 20;
        return base + (int) Math.round(h * amp);
    }

    @Override
    public void decorate(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid, double[][] centralMaskGrid) {
        int baseX = chunkX << 4, baseZ = chunkZ << 4;
        SplittableRandom rng = rngFor(seed, chunkX, chunkZ, 484811169L);
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (regionGrid[lx][lz] != ConsegrityRegions.Region.ICE_SPIKES) continue;
                int topY = topYGrid[lx][lz];
                if (topY < world.getMinHeight() + 1) continue;
                int ySnow = topY + 1;
                if (ySnow > world.getMaxHeight() - 1) continue;
                try {
                    Snow snow = (Snow) Bukkit.createBlockData(Material.SNOW);
                    snow.setLayers(1 + rng.nextInt(3));
                    if (data.getType(lx, ySnow, lz) == Material.AIR) data.setBlock(lx, ySnow, lz, snow);
                } catch (Throwable ignore) {
                    if (data.getType(lx, ySnow, lz) == Material.AIR) data.setBlock(lx, ySnow, lz, Material.SNOW);
                }
            }
        }

        int spikes = rng.nextDouble() < 0.65 ? 1 + rng.nextInt(3) : 0;
        for (int i = 0; i < spikes; ++i) {
            int lx = 2 + rng.nextInt(12);
            int lz = 2 + rng.nextInt(12);
            if (regionGrid[lx][lz] != ConsegrityRegions.Region.ICE_SPIKES) continue;
            int topY = topYGrid[lx][lz];
            if (topY < 151 || slopeGrid(topYGrid, lx, lz) > 3) continue;
            int h = 6 + rng.nextInt(11);
            int r = rng.nextDouble() < 0.2 ? 2 : 1;
            makeIceSpike(data, lx, topY + 1, lz, h, r, rng);
        }

        for (int t = 0; t < 60; ++t) {
            int lx = rng.nextInt(16), lz = rng.nextInt(16);
            if (lx < 1 || lx > 14 || lz < 1 || lz > 14) continue;
            if (regionGrid[lx][lz] != ConsegrityRegions.Region.ICE_SPIKES) continue;
            int topY = topYGrid[lx][lz];
            int wx = baseX + lx, wz = baseZ + lz;
            double r = Math.hypot(wx, wz);
            double inner = 60.0, outer = 260.0;
            double closeness = clamp01(1.0 - (r - inner) / Math.max(1.0, outer - inner));
            double chance = 0.02 + 0.1 * closeness;
            if (rng.nextDouble() > chance) continue;
            if (slopeGrid(topYGrid, lx, lz) > 3) continue;
            Material ground = data.getType(lx, topY, lz);
            if (ground != Material.GRASS_BLOCK && ground != Material.DIRT && ground != Material.SNOW_BLOCK && ground != Material.STONE) continue;
            int height = 5 + rng.nextInt(6);
            placeSpruceSimple(data, lx, topY + 1, lz, height);
        }
    }
}
