package goat.projectLinearity.util;

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
    // Cache raw schematic bytes and parsed clipboards by name to avoid repeated IO and parsing
    private final java.util.Map<String, byte[]> schemCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, Clipboard> clipCache = new java.util.concurrent.ConcurrentHashMap<>();

    public SchemManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads a schematic from resources/schematics/<name>.schem and pastes it
     * at the given Bukkit Location.
     *
     * @param name The base name of the schematic (without .schem).
     * @param loc  The Bukkit Location to paste at.
     */
    public void placeStructure(String name, Location loc) {
        String resourcePath = "schematics/" + name + ".schem";

        try {
            // Load from cache or read once from jar
            byte[] bytes = schemCache.computeIfAbsent(name, n -> {
                try (InputStream is = plugin.getResource(resourcePath)) {
                    if (is == null) return null;
                    return readAll(is);
                } catch (Exception e) {
                    return null;
                }
            });
            if (bytes == null) {
                plugin.getLogger().severe("Schematic not found: " + resourcePath);
                return;
            }

            Clipboard clipboard = clipCache.computeIfAbsent(name, n -> loadClipboard(resourcePath, bytes));
            if (clipboard == null) return;

            // perform the paste
            var world = BukkitAdapter.adapt(loc.getWorld());
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
                try { editSession.setFastMode(true); } catch (Throwable ignored) {}
                Operation pasteOp = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BukkitAdapter.asBlockVector(loc))
                        .ignoreAirBlocks(false)
                        .build();
                Operations.complete(pasteOp);
                editSession.flushSession();
            }

            plugin.getLogger().info("Pasted schematic '" + name + "' at " + loc);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to paste schematic '" + name + "': " + e.getMessage());
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

        try {
            // Load from cache or read once from jar
            byte[] bytes = schemCache.computeIfAbsent(name, n -> {
                try (InputStream is = plugin.getResource(resourcePath)) {
                    if (is == null) return null;
                    return readAll(is);
                } catch (Exception e) {
                    return null;
                }
            });
            if (bytes == null) {
                plugin.getLogger().severe("Schematic not found: " + resourcePath);
                return;
            }

            Clipboard clipboard = clipCache.computeIfAbsent(name, n -> loadClipboard(resourcePath, bytes));
            if (clipboard == null) return;
            var world = BukkitAdapter.adapt(loc.getWorld());
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
                try { editSession.setFastMode(true); } catch (Throwable ignored) {}
                Operation pasteOp = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BukkitAdapter.asBlockVector(loc))
                        .ignoreAirBlocks(ignoreAir)
                        .build();
                Operations.complete(pasteOp);
                editSession.flushSession();
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

    private Clipboard loadClipboard(String resourcePath, byte[] bytes) {
        try {
            String ext = resourcePath.substring(resourcePath.lastIndexOf('.') + 1);
            ClipboardFormat format = ClipboardFormats.findByAlias(ext);
            if (format == null) {
                plugin.getLogger().severe("Unrecognized schematic format: " + ext);
                return null;
            }
            if (bytes == null || bytes.length < 16) {
                plugin.getLogger().severe("Schematic appears empty or missing: " + resourcePath);
                return null;
            }
            try (ClipboardReader reader = format.getReader(new ByteArrayInputStream(bytes))) {
                return reader.read();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load clipboard for '" + resourcePath + "': " + e.getMessage());
            return null;
        }
    }

    // No ground normalization here; StructureManager handles SURFACE-only grassification
}
