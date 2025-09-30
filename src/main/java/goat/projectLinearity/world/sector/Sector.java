package goat.projectLinearity.world.sector;

import goat.projectLinearity.world.ConsegrityRegions;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

public interface Sector {
    // Compute the target surface Y for this sector at world coords (wx,wz)
    int computeSurfaceY(World world, long seed, int wx, int wz);

    // Place decorations/features for this sector inside the current chunk.
    // regionGrid is provided for per-cell region classification.
    // centralMaskGrid may be null for non-central sectors.
    default void decorate(World world,
                          ChunkGenerator.ChunkData data,
                          long seed,
                          int chunkX,
                          int chunkZ,
                          int[][] topYGrid,
                          ConsegrityRegions.Region[][] regionGrid,
                          double[][] centralMaskGrid) {
        // no-op by default
    }
}

