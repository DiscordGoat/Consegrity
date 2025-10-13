package goat.projectLinearity.libs.gravedigging;

import goat.minecraft.minecraftnew.MinecraftNew;
import org.bukkit.entity.Player;

import java.io.*;

/**
 * Simple persistence helper for tracking how many corpse mobs a player has killed.
 */
public class CorpseKillManager {

    private static final File DATA_FOLDER = new File(MinecraftNew.getInstance().getDataFolder(), "corpseKills");

    private static CorpseKillManager instance;

    static {
        if (!DATA_FOLDER.exists()) {
            DATA_FOLDER.mkdirs();
        }
    }

    private CorpseKillManager() {}

    public static CorpseKillManager getInstance() {
        if (instance == null) {
            instance = new CorpseKillManager();
        }
        return instance;
    }

    /**
     * Increment the corpse kill count for the given player.
     */
    public void incrementCorpseKills(Player player) {
        File file = new File(DATA_FOLDER, player.getUniqueId().toString() + ".txt");
        int count = 0;
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();
                if (line != null && !line.isEmpty()) {
                    count = Integer.parseInt(line.trim());
                }
            } catch (IOException | NumberFormatException ignored) {
            }
        }
        count++;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            writer.write(Integer.toString(count));
        } catch (IOException ignored) {
        }
    }

    /**
     * Retrieve the corpse kill count for the given player.
     */
    public int getCorpseKills(Player player) {
        File file = new File(DATA_FOLDER, player.getUniqueId().toString() + ".txt");
        if (!file.exists()) {
            return 0;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null && !line.isEmpty()) {
                return Integer.parseInt(line.trim());
            }
        } catch (IOException | NumberFormatException ignored) {
        }
        return 0;
    }
}
