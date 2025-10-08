package goat.projectLinearity;

import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import goat.projectLinearity.commands.GetAllConsegrityAdvancementsCommand;
import goat.projectLinearity.commands.ItemCommand;
import goat.projectLinearity.commands.RegenerateCommand;
import goat.projectLinearity.commands.SetCustomDurabilityCommand;
import goat.projectLinearity.commands.SetGildCommand;
import goat.projectLinearity.commands.SetGoldenDurabilityCommand;
import goat.projectLinearity.commands.SetMaxDurabilityCommand;
import goat.projectLinearity.commands.WarptoCommand;
import goat.projectLinearity.util.AnvilManager;
import goat.projectLinearity.util.CustomDurabilityManager;
import goat.projectLinearity.util.HeirloomManager;
import goat.projectLinearity.world.structure.StructureListener;
import goat.projectLinearity.world.structure.StructureManager;
import goat.projectLinearity.world.structure.GenCheckType;
import goat.projectLinearity.world.sector.*;
import goat.projectLinearity.world.ConsegrityRegions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class ProjectLinearity extends JavaPlugin implements Listener {

    private StructureManager structureManager;
    private volatile boolean regenInProgress = false;
    private AnvilManager anvilManager;

    // Advancement tabs (optional; may remain null). Only used by commands/listeners defensively.
    public AdvancementTab consegrity, desert, mesa, swamp, cherry, mountain, jungle;

    @Override
    public void onEnable() {
        CustomDurabilityManager.init(this);
        HeirloomManager.init(this);
        anvilManager = new AnvilManager(this);

        // Commands
        try { getCommand("regenerate").setExecutor(new RegenerateCommand(this)); } catch (Throwable ignored) {}
        try { WarptoCommand warpto = new WarptoCommand(); getCommand("warpto").setExecutor(warpto); getCommand("warpto").setTabCompleter(warpto);} catch (Throwable ignored) {}
        try { getCommand("getallconsegrityadvancements").setExecutor(new GetAllConsegrityAdvancementsCommand(this)); } catch (Throwable ignored) {}
        try { SetCustomDurabilityCommand cmd = new SetCustomDurabilityCommand(); getCommand("setcustomdurability").setExecutor(cmd); getCommand("setcustomdurability").setTabCompleter(cmd);} catch (Throwable ignored) {}
        try { SetGoldenDurabilityCommand cmd = new SetGoldenDurabilityCommand(); getCommand("setgoldendurability").setExecutor(cmd); getCommand("setgoldendurability").setTabCompleter(cmd);} catch (Throwable ignored) {}
        try { SetMaxDurabilityCommand cmd = new SetMaxDurabilityCommand(); getCommand("setmaxdurability").setExecutor(cmd); getCommand("setmaxdurability").setTabCompleter(cmd);} catch (Throwable ignored) {}
        try { SetGildCommand cmd = new SetGildCommand(); getCommand("setgild").setExecutor(cmd); getCommand("setgild").setTabCompleter(cmd);} catch (Throwable ignored) {}
        try { ItemCommand cmd = new ItemCommand(); getCommand("i").setExecutor(cmd); getCommand("i").setTabCompleter(cmd);} catch (Throwable ignored) {}

        // Managers
        structureManager = new StructureManager(this);
        // Enable deferred spawner (periodic + on chunk load) to place as you explore
        goat.projectLinearity.world.structure.DeferredStructureSpawner structSpawner = new goat.projectLinearity.world.structure.DeferredStructureSpawner(this, structureManager);
        Bukkit.getPluginManager().registerEvents(structSpawner, this);
        Bukkit.getPluginManager().registerEvents(new StructureListener(structureManager), this);
        // Natural spawning only; no sector-based enforcement
        try { Bukkit.getScheduler().runTaskTimer(this, structSpawner, 1L, 5L); } catch (Throwable ignore) {}

        try {
            structureManager.registerStruct("jungletemple", 24, 10, 200, new JungleSector(), GenCheckType.SURFACE, true, 300);
            structureManager.registerStruct("deserttemple", 30, 5, 200, new DesertBiome(), GenCheckType.SURFACE, true, 200);
            structureManager.registerStruct("witchhut", 10, 7, 150, new SwampSector(), GenCheckType.SURFACE, true, 150);
            structureManager.registerStruct("witchfestival", 60, 1, 200, new SwampSector(), GenCheckType.SURFACE, true, 300);
            structureManager.registerStruct("monastery", 30, 1, 100, new CherrySector(), GenCheckType.SURFACE, true, 400);
            structureManager.registerStruct("hotspring", 10, 10, 100, new CherrySector(), GenCheckType.SURFACE, true, 120);
        } catch (Throwable ignored) {}

        // Top-up scheduling is started after pre-generation completes in RegenerateCommand
    }

    // Default world generator remains the server default; Consegrity world is created via command

    public StructureManager getStructureManager() { return structureManager; }
    public boolean isRegenInProgress() { return regenInProgress; }
    public void setRegenInProgress(boolean inProgress) { this.regenInProgress = inProgress; }

    // Optional hook used by RegionTitleListener; safe no-op if tabs not initialized
    public void showRegionTab(Player p, ConsegrityRegions.Region r) {
        try {
            AdvancementTab tab = switch (r) {
                case CENTRAL -> consegrity;
                case DESERT -> desert;
                case SAVANNAH -> null; // no dedicated tab available here
                case SWAMP -> swamp;
                case JUNGLE -> jungle;
                case MESA -> mesa;
                case MOUNTAIN -> mountain;
                case ICE_SPIKES -> null;
                case CHERRY -> cherry;
                case OCEAN -> null;
                case NETHER -> null;
            };
            if (tab != null) tab.showTab(p);
        } catch (Throwable ignored) {}
    }
}
