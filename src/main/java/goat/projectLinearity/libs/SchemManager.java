package goat.projectLinearity.libs;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class SchemManager {

    private final JavaPlugin plugin;

    public SchemManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads a schematic from resources/schematics/<name>.schem and pastes it
     * at the given Bukkit Location.
     *
     * @param name The base name of the schematic (without “.schem”).
     * @param loc  The Bukkit Location to paste at.
     */
    public void placeStructure(String name, Location loc) {
        String resourcePath = "schematics/" + name + ".schem";

        try (InputStream is = plugin.getResource(resourcePath)) {
            if (is == null) {
                plugin.getLogger().severe("Schematic not found: " + resourcePath);
                return;
            }

            // detect format by file-extension alias ("schem")
            String ext = resourcePath.substring(resourcePath.lastIndexOf('.') + 1);
            ClipboardFormat format = ClipboardFormats.findByAlias(ext);
            if (format == null) {
                plugin.getLogger().severe("Unrecognized schematic format: " + ext);
                return;
            }

            // read the resource into memory to validate header and avoid classloader stream quirks
            byte[] bytes = readAll(is);
            if (bytes.length < 16) {
                plugin.getLogger().severe("Schematic '" + name + "' appears empty or truncated (" + bytes.length + " bytes)");
                return;
            }
            // For .schem (Sponge), expect GZIP header 0x1F 0x8B
            if ("schem".equalsIgnoreCase(ext)) {
                int b0 = bytes[0] & 0xFF;
                int b1 = bytes[1] & 0xFF;
                if (b0 != 0x1F || b1 != 0x8B) {
                    plugin.getLogger().severe("Schematic '" + name + ".schem' does not start with GZIP header. It may be corrupted by resource filtering. Ensure POM excludes filtering for .schem files.");
                }
            }

            // read the clipboard
            try (ClipboardReader reader = format.getReader(new ByteArrayInputStream(bytes))) {
                Clipboard clipboard = reader.read();

                // perform the paste
                var world = BukkitAdapter.adapt(loc.getWorld());
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
                    Operation pasteOp = new ClipboardHolder(clipboard)
                            .createPaste(editSession)
                            .to(BukkitAdapter.asBlockVector(loc))
                            .ignoreAirBlocks(false)
                            .build();
                    Operations.complete(pasteOp);
                    editSession.flushSession();
                }
            }

            plugin.getLogger().info("Pasted schematic “" + name + "” at " + loc);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to paste schematic “" + name + "”: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Overload allowing control of air treatment during paste.
     * If ignoreAir is true, air in the schematic will not overwrite existing blocks
     * (useful for underwater placements to avoid clearing water).
     */
    public void placeStructure(String name, Location loc, boolean ignoreAir) {
        String resourcePath = "schematics/" + name + ".schem";

        try (InputStream is = plugin.getResource(resourcePath)) {
            if (is == null) {
                plugin.getLogger().severe("Schematic not found: " + resourcePath);
                return;
            }

            String ext = resourcePath.substring(resourcePath.lastIndexOf('.') + 1);
            ClipboardFormat format = ClipboardFormats.findByAlias(ext);
            if (format == null) {
                plugin.getLogger().severe("Unrecognized schematic format: " + ext);
                return;
            }

            try (ClipboardReader reader = format.getReader(is)) {
                Clipboard clipboard = reader.read();
                var world = BukkitAdapter.adapt(loc.getWorld());
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
                    Operation pasteOp = new ClipboardHolder(clipboard)
                            .createPaste(editSession)
                            .to(BukkitAdapter.asBlockVector(loc))
                            .ignoreAirBlocks(ignoreAir)
                            .build();
                    Operations.complete(pasteOp);
                    editSession.flushSession();
                }
            }

            plugin.getLogger().info("Pasted schematic '" + name + "' at " + loc);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to paste schematic '" + name + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static byte[] readAll(InputStream is) throws java.io.IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(8192, is.available()));
        byte[] buf = new byte[8192];
        int r;
        while ((r = is.read(buf)) != -1) bos.write(buf, 0, r);
        return bos.toByteArray();
    }
}
