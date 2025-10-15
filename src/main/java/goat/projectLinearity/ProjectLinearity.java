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
import goat.projectLinearity.commands.SetCultistPopulationCommand;
import goat.projectLinearity.commands.DebugOxygenCommand;
import goat.projectLinearity.commands.SetStatCommand;
import goat.projectLinearity.commands.SetStatRateCommand;
import goat.projectLinearity.commands.SpawnCustomMobCommand;
import goat.projectLinearity.commands.WarptoCommand;
import goat.projectLinearity.util.CustomEntityRegistry;
import goat.projectLinearity.util.cultist.CultistPopulationManager;
import goat.projectLinearity.util.cultist.MountainCultistAlertListener;
import goat.projectLinearity.util.cultist.MountainCultistBehaviour;
import goat.projectLinearity.util.cultist.MountainCultistDamageListener;
import goat.projectLinearity.util.cultist.MountainCultistSpawnListener;
import goat.projectLinearity.util.*;
import goat.projectLinearity.world.RegionTitleListener;
import goat.projectLinearity.world.MountainMobSpawnBlocker;
import goat.projectLinearity.world.KeystoneManager;
import goat.projectLinearity.world.KeystoneListener;
import goat.projectLinearity.world.NocturnalStructureManager;
import goat.projectLinearity.world.structure.StructureListener;
import goat.projectLinearity.world.structure.StructureManager;
import goat.projectLinearity.world.structure.GenCheckType;
import goat.projectLinearity.world.sector.*;
import goat.projectLinearity.world.ConsegrityRegions;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Optional;

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
    private VillagerTradeManager villagerTradeManager;
    private CultistPopulationManager cultistPopulationManager;
    private MountainCultistBehaviour mountainCultistBehaviour;
    private MountainCultistSpawnListener mountainCultistSpawnListener;
    private MountainCultistDamageListener mountainCultistDamageListener;
    private MountainCultistAlertListener mountainCultistAlertListener;
    private MountainMobSpawnBlocker mountainMobSpawnBlocker;
    private NocturnalStructureManager nocturnalStructureManager;
    private KeystoneManager keystoneManager;
    private CustomEntityRegistry customEntityRegistry;
    private Listener citizensEnableListener;
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
        Bukkit.getPluginManager().registerEvents(new TreeFellingListener(), this);

        miningOxygenManager = new MiningOxygenManager(this, spaceManager);
        sidebarManager = new SidebarManager(this, spaceManager, miningOxygenManager);
        Bukkit.getOnlinePlayers().forEach(sidebarManager::initialise);

        shelfManager = new ShelfManager(this);
        culinarySubsystem = CulinarySubsystem.getInstance(this);
        culinaryCauldron = new CulinaryCauldron(this);
        villagerTradeManager = new VillagerTradeManager(this, enchantedManager);
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Unable to create plugin data folder.");
        }
        cultistPopulationManager = new CultistPopulationManager(this);
        boolean cultistsReady = cultistPopulationManager.startup();
        mountainCultistBehaviour = new MountainCultistBehaviour(this, cultistPopulationManager);
        mountainCultistSpawnListener = new MountainCultistSpawnListener(this);
        Bukkit.getPluginManager().registerEvents(mountainCultistSpawnListener, this);
        mountainCultistDamageListener = new MountainCultistDamageListener(cultistPopulationManager);
        Bukkit.getPluginManager().registerEvents(mountainCultistDamageListener, this);
        mountainCultistAlertListener = new MountainCultistAlertListener(cultistPopulationManager, mountainCultistBehaviour);
        Bukkit.getPluginManager().registerEvents(mountainCultistAlertListener, this);
        mountainMobSpawnBlocker = new MountainMobSpawnBlocker(cultistPopulationManager);
        Bukkit.getPluginManager().registerEvents(mountainMobSpawnBlocker, this);
        nocturnalStructureManager = new NocturnalStructureManager(this);
        nocturnalStructureManager.registerStruct("haywagon", 5, 8, 200);
        nocturnalStructureManager.startup();
        customEntityRegistry = new CustomEntityRegistry(this);
        registerCustomEntities();
        if (!cultistsReady) {
            citizensEnableListener = new CitizensEnableWatcher();
            Bukkit.getPluginManager().registerEvents(citizensEnableListener, this);
        }
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
        try { SetCultistPopulationCommand cmd = new SetCultistPopulationCommand(this); getCommand("setcultistpopulation").setExecutor(cmd); getCommand("setcultistpopulation").setTabCompleter(cmd);} catch (Throwable ignored) {}
        try { SpawnCustomMobCommand cmd = new SpawnCustomMobCommand(this); getCommand("spawncustommob").setExecutor(cmd); getCommand("spawncustommob").setTabCompleter(cmd);} catch (Throwable ignored) {}

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
            structureManager.registerStruct("jungletemple", 24, 7, 100, new JungleSector(), GenCheckType.SURFACE, true, 300);
            structureManager.registerStruct("deserttemple", 30, 6, 200, new DesertBiome(), GenCheckType.SURFACE, true, 100);
            structureManager.registerStruct("witchhut", 9, 10, 150, new SwampSector(), GenCheckType.SURFACE, true, 250);
            structureManager.registerStruct("witchfestival", 60, 1, 200, new SwampSector(), GenCheckType.SURFACE, true, 300);
            structureManager.registerStruct("monastery", 30, 1, 100, new CherrySector(), GenCheckType.SURFACE, true, 500);
            structureManager.registerStruct("hotspring", 10, 20, 100, new CherrySector(), GenCheckType.SURFACE, true, 120);
            structureManager.registerStruct("monument", 70, 20, 500, new OceanSector(), GenCheckType.UNDERWATER, true, 80);
            structureManager.registerStruct("jadestatue1", 20, 1, 800, new JungleSector(), GenCheckType.SURFACE, true, 0);
            structureManager.registerStruct("beacon0", 26, 1, 800, new MountainSector(), GenCheckType.SURFACE, true, 600);
            structureManager.registerStruct("conduit1", 70, 1, 800, new OceanSector(), GenCheckType.UNDERWATER, true, 0);
            structureManager.registerStruct("pillager", 20, 12, 200, new MesaSector(), GenCheckType.SURFACE, true, 80);
            structureManager.registerStruct("prospect", 20, 8, 200, new MesaSector(), GenCheckType.SURFACE, true, 80);
        } catch (Throwable ignored) {}

        keystoneManager = new KeystoneManager(this);
        keystoneManager.registerDefinition(new KeystoneManager.KeystoneDefinition(
                "jadestatue1",
                ChatColor.GREEN + "Jade Statue",
                "jadestatue1",
                java.util.List.of("jadestatue1", "jadestatue2", "jadestatue3", "jadestatuefinal"),
                20,
                20,
                32,
                new KeystoneManager.RequiredItem("Jade"),
                20,
                32.0,
                24.0,
                player -> player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 200, 0, true, false, true))
        ));
        
        keystoneManager.registerDefinition(new KeystoneManager.KeystoneDefinition(
                "beacon0",
                ChatColor.AQUA + "Ancient Beacon",
                "beacon0",
                java.util.List.of("beacon0", "beacon1", "beacon2", "beacon3", "beacon4"),
                15,
                15,
                100,
                new KeystoneManager.RequiredItem(Material.IRON_BLOCK),
                30,
                30.0,
                20.0,
                player -> player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 400, 0, true, false, true))
        ));
        
        keystoneManager.registerDefinition(new KeystoneManager.KeystoneDefinition(
                "conduit1",
                ChatColor.BLUE + "Oceanic Conduit",
                "conduit1",
                java.util.List.of("conduit1", "conduit2", "conduit3", "conduit4"),
                30,
                30,
                512,
                new KeystoneManager.RequiredItem(Material.PRISMARINE_CRYSTALS),
                80,
                40.0,
                25.0,
                player -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 600, 0, true, false, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 600, 0, true, false, true));
                }
        ));
        
        keystoneManager.startup();
        Bukkit.getPluginManager().registerEvents(new KeystoneListener(keystoneManager), this);

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

    private void registerCustomEntities() {
        if (customEntityRegistry == null) {
            return;
        }
        customEntityRegistry.register(new CustomEntityRegistry.CustomEntityEntry(
                "cultist",
                "Mountain Cultist",
                "Spawns a Mountain Cultist NPC for testing.",
                List.of("cultists", "mountain_cultist", "mountaincultist"),
                (pl, location, sender) -> {
                    CultistPopulationManager manager = pl.getCultistPopulationManager();
                    if (manager == null) {
                        return CustomEntityRegistry.SpawnResult.failure("Cultist manager is unavailable.");
                    }
                    Optional<NPC> npcOptional = manager.spawnCultistEntity(location);
                    if (npcOptional.isEmpty()) {
                        return CustomEntityRegistry.SpawnResult.failure("Unable to spawn Mountain Cultist. See console for details.");
                    }
                    NPC npc = npcOptional.get();
                    Location spawnLocation = npc.getEntity() != null ? npc.getEntity().getLocation() : location;
                    String worldName = spawnLocation.getWorld() != null ? spawnLocation.getWorld().getName() : "unknown";
                    String message = String.format(
                            "Spawned Mountain Cultist (%s) at %s x=%.1f y=%.1f z=%.1f",
                            npc.getUniqueId(),
                            worldName,
                            spawnLocation.getX(),
                            spawnLocation.getY(),
                            spawnLocation.getZ());
                    return CustomEntityRegistry.SpawnResult.success(message);
                }
        ));
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
        if (cultistPopulationManager != null) {
            cultistPopulationManager.shutdown();
        }
        if (mountainCultistBehaviour != null) {
            mountainCultistBehaviour.shutdown();
        }
        if (mountainCultistSpawnListener != null) {
            HandlerList.unregisterAll(mountainCultistSpawnListener);
            mountainCultistSpawnListener = null;
        }
        if (mountainCultistDamageListener != null) {
            HandlerList.unregisterAll(mountainCultistDamageListener);
            mountainCultistDamageListener = null;
        }
        if (mountainCultistAlertListener != null) {
            HandlerList.unregisterAll(mountainCultistAlertListener);
            mountainCultistAlertListener = null;
        }
        if (mountainMobSpawnBlocker != null) {
            HandlerList.unregisterAll(mountainMobSpawnBlocker);
            mountainMobSpawnBlocker = null;
        }
        if (nocturnalStructureManager != null) {
            nocturnalStructureManager.shutdown();
            nocturnalStructureManager = null;
        }
        if (keystoneManager != null) {
            keystoneManager.shutdown();
            keystoneManager = null;
        }
        if (citizensEnableListener != null) {
            HandlerList.unregisterAll(citizensEnableListener);
            citizensEnableListener = null;
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
    public CultistPopulationManager getCultistPopulationManager() { return cultistPopulationManager; }
    public MountainCultistBehaviour getMountainCultistBehaviour() { return mountainCultistBehaviour; }
    public KeystoneManager getKeystoneManager() { return keystoneManager; }
    public CustomEntityRegistry getCustomEntityRegistry() { return customEntityRegistry; }
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
    private class CitizensEnableWatcher implements Listener {

        @EventHandler
        public void onPluginEnable(PluginEnableEvent event) {
            if (!"Citizens".equalsIgnoreCase(event.getPlugin().getName())) {
                return;
            }
            if (cultistPopulationManager == null) {
                return;
            }
            if (cultistPopulationManager.startup()) {
                HandlerList.unregisterAll(this);
                citizensEnableListener = null;
            }
        }
    }
}
