package goat.projectLinearity.structure;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class StructureListener implements Listener {
    private final StructureManager manager;

    public StructureListener(StructureManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player p = event.getPlayer();
        String worldKey = p.getWorld().getUID().toString();
        int x = event.getTo().getBlockX();
        int z = event.getTo().getBlockZ();

        StructureStore store = StructureStore.get(manager.getPlugin());
        var opt = store.findUndiscoveredNear(worldKey, x, z);
        if (opt.isEmpty()) return;

        StructureStore.StructEntry e = opt.get();
        store.markDiscovered(worldKey, e.id);

        int discovered = store.getDiscoveredCount(worldKey);
        int total = store.getPlacedCount(worldKey);
        p.sendMessage("Discovered Structure. " + discovered + "/" + Math.max(total, discovered));
    }
}
