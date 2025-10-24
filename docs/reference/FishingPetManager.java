package goat.minecraft.minecraftnew.subsystems.fishing;

import goat.minecraft.minecraftnew.MinecraftNew;
import org.bukkit.entity.Player;

import java.io.*;

public class FishingPetManager {

    // Directory to store each player's tally file.
    private static final File DATA_FOLDER = new File(MinecraftNew.getInstance().getDataFolder(), "fishermanTally");

    // Singleton instance.
    private static FishingPetManager instance;

    // Ensure the directory exists.
    static {
        if (!DATA_FOLDER.exists()) {
            DATA_FOLDER.mkdirs();
        }
    }

    // Private constructor to prevent instantiation.
    private FishingPetManager() { }

    /**
     * Returns the singleton instance of FishermansTally.
     *
     * @return the instance.
     */
    public static FishingPetManager getInstance() {
        if (instance == null) {
            instance = new FishingPetManager();
        }
        return instance;
    }

    /**
     * Increments the sea creature kill count for the given player.
     *
     * @param player the player whose kill count to increment.
     */
    public void incrementSeaCreatureKills(Player player) {
        File file = new File(DATA_FOLDER, player.getUniqueId().toString() + ".txt");
        int count = 0;

        // Read existing count, if present.
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();
                if (line != null && !line.isEmpty()) {
                    count = Integer.parseInt(line.trim());
                }
            } catch (IOException | NumberFormatException ex) {
                ex.printStackTrace();
            }
        }
        count++;

        // Write the new count back to the file.
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            writer.write(Integer.toString(count));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns the current sea creature kill count for the given player.
     *
     * @param player the player whose kill count to retrieve.
     * @return the kill count, or 0 if no data exists.
     */
    public int getSeaCreatureKills(Player player) {
        File file = new File(DATA_FOLDER, player.getUniqueId().toString() + ".txt");
        if (!file.exists()) {
            return 0;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null && !line.isEmpty()) {
                return Integer.parseInt(line.trim());
            }
        } catch (IOException | NumberFormatException ex) {
            ex.printStackTrace();
        }
        return 0;
    }
}
