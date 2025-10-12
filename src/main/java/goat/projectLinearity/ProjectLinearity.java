package goat.projectLinearity;

import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import goat.projectLinearity.commands.GetAllConsegrityAdvancementsCommand;
import goat.projectLinearity.commands.ItemCommand;
import goat.projectLinearity.commands.RegenerateCommand;
import goat.projectLinearity.commands.RegenerateNetherCommand;
import goat.projectLinearity.commands.SetCustomDurabilityCommand;
import goat.projectLinearity.commands.SetGildCommand;
import goat.projectLinearity.commands.SetGoldenDurabilityCommand;
import goat.projectLinearity.commands.SetMaxDurabilityCommand;
import goat.projectLinearity.commands.SetStatCommand;
import goat.projectLinearity.commands.DebugOxygenCommand;
import goat.projectLinearity.commands.SetStatRateCommand;
import goat.projectLinearity.commands.WarptoCommand;
import goat.projectLinearity.util.CulinaryCauldron;
import goat.projectLinearity.util.CulinarySubsystem;
import goat.projectLinearity.util.ShelfManager;
import goat.projectLinearity.util.AnvilManager;
import goat.projectLinearity.util.CustomDurabilityManager;
import goat.projectLinearity.util.EnchantedManager;
import goat.projectLinearity.util.EnchantingManager;
import goat.projectLinearity.util.HeirloomManager;
import goat.projectLinearity.util.ItemRegistry;
import goat.projectLinearity.util.MiningOxygenManager;
import goat.projectLinearity.util.SidebarManager;
import goat.projectLinearity.util.SpaceBlockListener;
import goat.projectLinearity.util.SpaceEventListener;
import goat.projectLinearity.util.SpaceManager;
import goat.projectLinearity.util.SpacePresenceListener;
import goat.projectLinearity.world.RegionTitleListener;
import goat.projectLinearity.world.structure.StructureListener;
import goat.projectLinearity.world.structure.StructureManager;
import goat.projectLinearity.world.structure.GenCheckType;
import goat.projectLinearity.world.sector.*;
import goat.projectLinearity.world.ConsegrityRegions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public final class ProjectLinearity extends JavaPlugin implements Listener {

    private StructureManager structureManager;
    private volatile boolean regenInProgress = false;
    private AnvilManager anvilManager;
    private SpaceManager spaceManager;
    private MiningOxygenManager miningOxygenManager;
    private SidebarManager sidebarManager;
    private EnchantedManager enchantedManager;
    private EnchantingManager enchantingManager;
    private ShelfManager shelfManager;
    private CulinarySubsystem culinarySubsystem;
    private CulinaryCauldron culinaryCauldron;
    private double statRate = 1.0;
    private boolean debugOxygen = false;

    // Advancement tabs (optional; may remain null). Only used by commands/listeners defensively.
    public AdvancementTab consegrity, desert, mesa, swamp, cherry, mountain, jungle;

    @Override
    public void onEnable() {
        CustomDurabilityManager.init(this);
        HeirloomManager.init(this);
        enchantedManager = new EnchantedManager(this);
        enchantedManager.start();
        anvilManager = new AnvilManager(this, enchantedManager);
        spaceManager = new SpaceManager(this);
        spaceManager.load();
        Bukkit.getPluginManager().registerEvents(new SpacePresenceListener(spaceManager), this);
        Bukkit.getPluginManager().registerEvents(new SpaceEventListener(spaceManager, this), this);
        Bukkit.getPluginManager().registerEvents(new SpaceBlockListener(spaceManager), this);
        Bukkit.getPluginManager().registerEvents(new RegionTitleListener(this), this);
        miningOxygenManager = new MiningOxygenManager(this, spaceManager);
        sidebarManager = new SidebarManager(this, spaceManager, miningOxygenManager);
        Bukkit.getOnlinePlayers().forEach(sidebarManager::initialise);

        shelfManager = new ShelfManager(this);
        culinarySubsystem = CulinarySubsystem.getInstance(this);
        culinaryCauldron = new CulinaryCauldron(this);
        registerShelfRecipe();

        // Commands
        try { getCommand("regenerate").setExecutor(new RegenerateCommand(this)); } catch (Throwable ignored) {}
        try { getCommand("regeneratenether").setExecutor(new RegenerateNetherCommand(this)); } catch (Throwable ignored) {}
        try { WarptoCommand warpto = new WarptoCommand(); getCommand("warpto").setExecutor(warpto); getCommand("warpto").setTabCompleter(warpto);} catch (Throwable ignored) {}
        try { getCommand("getallconsegrityadvancements").setExecutor(new GetAllConsegrityAdvancementsCommand(this)); } catch (Throwable ignored) {}
        try { SetCustomDurabilityCommand cmd = new SetCustomDurabilityCommand(); getCommand("setcustomdurability").setExecutor(cmd); getCommand("setcustomdurability").setTabCompleter(cmd);} catch (Throwable ignored) {}
        try { SetGoldenDurabilityCommand cmd = new SetGoldenDurabilityCommand(); getCommand("setgoldendurability").setExecutor(cmd); getCommand("setgoldendurability").setTabCompleter(cmd);} catch (Throwable ignored) {}
        try { SetMaxDurabilityCommand cmd = new SetMaxDurabilityCommand(); getCommand("setmaxdurability").setExecutor(cmd); getCommand("setmaxdurability").setTabCompleter(cmd);} catch (Throwable ignored) {}
        try { SetGildCommand cmd = new SetGildCommand(); getCommand("setgild").setExecutor(cmd); getCommand("setgild").setTabCompleter(cmd);} catch (Throwable ignored) {}
        try { ItemCommand cmd = new ItemCommand(); getCommand("i").setExecutor(cmd); getCommand("i").setTabCompleter(cmd);} catch (Throwable ignored) {}
        try { SetStatCommand cmd = new SetStatCommand(this); getCommand("setstat").setExecutor(cmd); getCommand("setstat").setTabCompleter(cmd);} catch (Throwable ignored) {}
        try { SetStatRateCommand cmd = new SetStatRateCommand(this); getCommand("setstatrate").setExecutor(cmd); getCommand("setstatrate").setTabCompleter(cmd);} catch (Throwable ignored) {}
        try { DebugOxygenCommand cmd = new DebugOxygenCommand(this); getCommand("debugoxygen").setExecutor(cmd); getCommand("debugoxygen").setTabCompleter(cmd);} catch (Throwable ignored) {}

        // Managers
        structureManager = new StructureManager(this);
        // Enable deferred spawner (periodic + on chunk load) to place as you explore
        goat.projectLinearity.world.structure.DeferredStructureSpawner structSpawner = new goat.projectLinearity.world.structure.DeferredStructureSpawner(this, structureManager);
        Bukkit.getPluginManager().registerEvents(structSpawner, this);
        Bukkit.getPluginManager().registerEvents(new StructureListener(structureManager), this);
        // Natural spawning only; no sector-based enforcement
        try { Bukkit.getScheduler().runTaskTimer(this, structSpawner, 1L, 5L); } catch (Throwable ignore) {}

        enchantingManager = new EnchantingManager(this, enchantedManager);
        enchantingManager.start();
        registerRecipes();

        try {
            //structureManager.registerStruct("jungletemple", 24, 10, 200, new JungleSector(), GenCheckType.SURFACE, true, 300);
            //structureManager.registerStruct("deserttemple", 30, 5, 200, new DesertBiome(), GenCheckType.SURFACE, true, 200);
            //structureManager.registerStruct("deserttemple", 30, 5, 200, new NetherWastelandSector(), GenCheckType.SURFACE, true, 200);
            //structureManager.registerStruct("witchhut", 10, 7, 150, new SwampSector(), GenCheckType.SURFACE, true, 150);
            //structureManager.registerStruct("witchfestival", 60, 1, 200, new SwampSector(), GenCheckType.SURFACE, true, 300);
            //structureManager.registerStruct("monastery", 30, 1, 100, new CherrySector(), GenCheckType.SURFACE, true, 400);
            //structureManager.registerStruct("hotspring", 10, 10, 100, new CherrySector(), GenCheckType.SURFACE, true, 120);
            structureManager.registerStruct("monument", 70, 100, 500, new OceanSector(), GenCheckType.UNDERWATER, true, 80);
        } catch (Throwable ignored) {}

        // Top-up scheduling is started after pre-generation completes in RegenerateCommand
    }

    private void registerRecipes() {
        NamespacedKey key = new NamespacedKey(this, "rosegold_ingot");
        try {
            Bukkit.removeRecipe(key);
        } catch (Throwable ignored) {}
        try {
            ShapelessRecipe recipe = new ShapelessRecipe(key, ItemRegistry.getRosegoldIngot());
            for (int i = 0; i < 4; i++) {
                recipe.addIngredient(Material.COPPER_INGOT);
            }
            RecipeChoice.ExactChoice chunkChoice = new RecipeChoice.ExactChoice(ItemRegistry.getRosegoldChunk());
            for (int i = 0; i < 4; i++) {
                recipe.addIngredient(chunkChoice);
            }
            Bukkit.addRecipe(recipe);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void registerShelfRecipe() {
        NamespacedKey key = new NamespacedKey(this, "culinary_shelf");
        try {
            Bukkit.removeRecipe(key);
        } catch (Throwable ignored) {}
        try {
            ShapedRecipe recipe = new ShapedRecipe(key, ItemRegistry.getShelfItem());
            recipe.shape("PPP", "PPP", "PPP");
            recipe.setIngredient('P', Material.OAK_PLANKS);
            Bukkit.addRecipe(recipe);
        } catch (IllegalArgumentException ignored) {
        }
    }

    // Default world generator remains the server default; Consegrity world is created via command

    @Override
    public void onDisable() {
        if (culinarySubsystem != null) {
            culinarySubsystem.finalizeAllSessionsOnShutdown();
            culinarySubsystem.onDisable();
        }
        if (shelfManager != null) {
            shelfManager.onDisable();
        }
        if (miningOxygenManager != null) {
            miningOxygenManager.shutdown();
        }
        if (sidebarManager != null) {
            sidebarManager.shutdown();
        }
        if (spaceManager != null) {
            spaceManager.save();
        }
    }

    public StructureManager getStructureManager() { return structureManager; }
    public boolean isRegenInProgress() { return regenInProgress; }
    public void setRegenInProgress(boolean inProgress) { this.regenInProgress = inProgress; }
    public SpaceManager getSpaceManager() { return spaceManager; }
    public SidebarManager getSidebarManager() { return sidebarManager; }
    public MiningOxygenManager getMiningOxygenManager() { return miningOxygenManager; }
    public double getStatRate() { return statRate; }
    public void setStatRate(double rate) { this.statRate = Math.max(0.01, rate); }
    public boolean isDebugOxygen() { return debugOxygen; }
    public void setDebugOxygen(boolean debug) { this.debugOxygen = debug; }

    // Optional hook used by RegionTitleListener; safe no-op if tabs not initialized
    public void showRegionTab(Player p, ConsegrityRegions.Region r) {
        try {
            AdvancementTab tab = switch (r) {
                case CENTRAL -> consegrity;
                case DESERT -> desert;
                case SAVANNAH -> null;
                case SWAMP -> swamp;
                case JUNGLE -> jungle;
                case MESA -> mesa;
                case MOUNTAIN -> mountain;
                case ICE_SPIKES -> null;
                case CHERRY -> cherry;
                case OCEAN -> null;
                case NETHER -> null;
                case NETHER_WASTELAND, NETHER_BASIN, NETHER_CLIFF, NETHER_OCEAN, NETHER_BOUNDARY -> null;
            };
            if (tab != null) tab.showTab(p);
        } catch (Throwable ignored) {}
    }
}
