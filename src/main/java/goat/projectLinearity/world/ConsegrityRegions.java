package goat.projectLinearity.world;

import org.bukkit.World;
import org.bukkit.block.Biome;

public final class ConsegrityRegions {
    private ConsegrityRegions() {}

    public enum Region { CENTRAL, DESERT, SAVANNAH, SWAMP, JUNGLE, MESA, MOUNTAIN, ICE_SPIKES, CHERRY, OCEAN, NETHER }

    public static final int SEA_LEVEL = 154;

    public static Region regionAt(World world, int x, int z) {
        try {
            Biome b = world.getBiome(x, SEA_LEVEL, z);
            return fromBiome(b);
        } catch (Throwable t) {
            try {
                Biome b2 = world.getBiome(x, z);
                return fromBiome(b2);
            } catch (Throwable ignore) {
                return Region.OCEAN;
            }
        }
    }

    // Overload aware of Y to support the underworld Nether layer as a single region
    public static Region regionAt(World world, int x, int y, int z) {
        if (y <= -70) {
            return Region.NETHER;
        }
        return regionAt(world, x, z);
    }

    public static Region fromBiome(Biome biome) {
        switch (biome) {
            case PLAINS: return Region.CENTRAL;
            case DESERT: return Region.DESERT;
            case SAVANNA: case WINDSWEPT_SAVANNA: return Region.SAVANNAH;
            case SWAMP: case MANGROVE_SWAMP: return Region.SWAMP;
            case JUNGLE: case SPARSE_JUNGLE: case BAMBOO_JUNGLE: return Region.JUNGLE;
            case BADLANDS: case ERODED_BADLANDS: case WOODED_BADLANDS: return Region.MESA;
            case ICE_SPIKES: return Region.ICE_SPIKES;
            case CHERRY_GROVE: return Region.CHERRY;
            case STONY_PEAKS: case JAGGED_PEAKS: case FROZEN_PEAKS: case WINDSWEPT_HILLS: case WINDSWEPT_FOREST: case WINDSWEPT_GRAVELLY_HILLS:
                return Region.MOUNTAIN;
            case OCEAN: case DEEP_OCEAN: case FROZEN_OCEAN: case COLD_OCEAN: case LUKEWARM_OCEAN: case WARM_OCEAN:
            case DEEP_COLD_OCEAN: case DEEP_FROZEN_OCEAN: case DEEP_LUKEWARM_OCEAN:
                return Region.OCEAN;
            default:
                return Region.OCEAN;
        }
    }
}

