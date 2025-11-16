package goat.projectLinearity;

import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import goat.projectLinearity.commands.*;
import goat.projectLinearity.listeners.BlindnessListener;
import goat.projectLinearity.listeners.HungerListener;
import goat.projectLinearity.listeners.InvisibilityListener;
import goat.projectLinearity.listeners.NightVisionListener;
import goat.projectLinearity.subsystems.culinary.CulinaryCauldron;
import goat.projectLinearity.subsystems.culinary.CulinaryCatalogueManager;
import goat.projectLinearity.subsystems.culinary.CulinarySubsystem;
import goat.projectLinearity.subsystems.enchanting.EnchantedManager;
import goat.projectLinearity.subsystems.enchanting.EnchantingManager;
import goat.projectLinearity.subsystems.farming.CropHarvestListener;
import goat.projectLinearity.subsystems.farming.CropPlantingListener;
import goat.projectLinearity.subsystems.mechanics.*;
import goat.projectLinearity.subsystems.mechanics.listeners.CurseListener;
import goat.projectLinearity.subsystems.mechanics.listeners.ZombieAttackRangeListener;
import goat.projectLinearity.subsystems.mechanics.spaces.SpaceBlockListener;
import goat.projectLinearity.subsystems.mechanics.spaces.SpaceEventListener;
import goat.projectLinearity.subsystems.mechanics.spaces.SpaceManager;
import goat.projectLinearity.subsystems.mechanics.spaces.SpacePresenceListener;
import goat.projectLinearity.subsystems.mining.MiningOxygenManager;
import goat.projectLinearity.subsystems.mining.QuartzOreDropListener;
import goat.projectLinearity.subsystems.trading.VillagerTradeManager;
import goat.projectLinearity.commands.SaveInventoryCommand;
import goat.projectLinearity.commands.StructureDebugCommand;
import goat.projectLinearity.libs.effects.ParticleEngine;
import goat.projectLinearity.libs.mutation.MutationBehavior;
import goat.projectLinearity.libs.mutation.MutationManager;
import goat.projectLinearity.libs.mutation.StatType;
import goat.projectLinearity.libs.mutation.Stats;
import goat.projectLinearity.libs.mutation.ThreeHeadedFireballListener;
import goat.projectLinearity.subsystems.world.PortalReturnManager;
import goat.projectLinearity.subsystems.world.desert.CurseManager;
import goat.projectLinearity.subsystems.world.loot.HeirloomManager;
import goat.projectLinearity.subsystems.world.loot.LootChestOpenListener;
import goat.projectLinearity.subsystems.world.loot.LootPopulatorManager;
import goat.projectLinearity.subsystems.world.loot.LootRegistry;
import goat.projectLinearity.util.CustomEntityRegistry;
import goat.projectLinearity.subsystems.world.desert.CurseEffectController;
import goat.projectLinearity.subsystems.world.cultist.CultistPopulationManager;
import goat.projectLinearity.subsystems.world.cultist.MountainCultistAlertListener;
import goat.projectLinearity.subsystems.world.cultist.MountainCultistBehaviour;
import goat.projectLinearity.subsystems.world.cultist.MountainCultistDamageListener;
import goat.projectLinearity.subsystems.world.cultist.MountainCultistSpawnListener;
import goat.projectLinearity.util.*;
import goat.projectLinearity.subsystems.brewing.BonusJumpManager;
import goat.projectLinearity.subsystems.brewing.CustomPotionCombatListener;
import goat.projectLinearity.subsystems.brewing.NauseaProjectileListener;
import goat.projectLinearity.subsystems.brewing.CustomPotionEffectManager;
import goat.projectLinearity.subsystems.brewing.PotionGuiManager;
import goat.projectLinearity.subsystems.brewing.PotionUsageListener;
import goat.projectLinearity.subsystems.brewing.LingeringHazardManager;
import goat.projectLinearity.subsystems.world.ConsegritySpawnListener;
import goat.projectLinearity.subsystems.world.ConsegrityChunkGenerator;
import goat.projectLinearity.subsystems.world.samurai.CherrySamuraiBehaviour;
import goat.projectLinearity.subsystems.world.samurai.CherrySamuraiDamageListener;
import goat.projectLinearity.subsystems.world.samurai.CherrySamuraiDeathListener;
import goat.projectLinearity.subsystems.world.samurai.CherrySamuraiSpawnListener;
import goat.projectLinearity.subsystems.world.samurai.SamuraiPopulationManager;
import goat.projectLinearity.subsystems.mechanics.RegionTitleListener;
import goat.projectLinearity.subsystems.mechanics.MountainMobSpawnBlocker;
import goat.projectLinearity.subsystems.mechanics.combat.QuickfireFinisherManager;
import goat.projectLinearity.subsystems.world.structure.KeystoneManager;
import goat.projectLinearity.subsystems.world.structure.KeystoneListener;
import goat.projectLinearity.subsystems.world.NocturnalStructureManager;
import goat.projectLinearity.subsystems.world.structure.StructureListener;
import goat.projectLinearity.subsystems.world.structure.DeferredStructureSpawner;
import goat.projectLinearity.subsystems.world.structure.StructureManager;
import goat.projectLinearity.subsystems.world.structure.StructureEntityManager;
import goat.projectLinearity.subsystems.world.structure.StructureType;
import goat.projectLinearity.subsystems.fishing.FishingManager;
import goat.projectLinearity.subsystems.fishing.SeaCreatureRegistry;
import goat.projectLinearity.subsystems.world.structure.GenCheckType;
import goat.projectLinearity.subsystems.world.sector.*;
import goat.projectLinearity.subsystems.world.ConsegrityRegions;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.*;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public final class ProjectLinearity extends JavaPlugin implements Listener {

    private static ProjectLinearity instance;

    private static final String DEEP_SEA_DIVER_SKIN_VALUE = "ewogICJ0aW1lc3RhbXAiIDogMTc1MjI5NTMxODk1OSwKICAicHJvZmlsZUlkIiA6ICJhNzdkNmQ2YmFjOWE0NzY3YTFhNzU1NjYxOTllYmY5MiIsCiAgInByb2ZpbGVOYW1lIiA6ICIwOEJFRDUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzkxNTVmYzZjNTM3NTBhNjVlZDc0NTk4NjJiN2E4MDNlODdiM2FkYzczNzg2NmU0ZjU1NzNhZmFiOWRiM2M1ZiIKICAgIH0KICB9Cn0=";
    private static final String DEEP_SEA_DIVER_SKIN_SIGNATURE = "WSGoyR4hZtLze2rI+7DZTsgqI4+DiEjt2BeYtfHBaNJ/EhwbDrO2J/CjcrJO/Xv+ETK234/Sf0YNanMMfAObs6OyhYerpDMlneKl3jzAErApI46mswIrhz2G7z4VWHhjJLQycTvUe1aYQ2gO0a9j4aLNncHkBB9qw1s24lxBezzF0YkuU1gCihJav/QiOBKt+hFy+cdp1JuBpRNWU/RhLCUUSBCvKlp8TtVxZ839j2JXgPXEkyziX9gMJ2rsWcUxuUsfxXyBO0EpauoFmO7cuvJ4hLahT83/Vt8jqr0m1cmpiE0jc6xzOECdNjQKeiLAFnAv6KVyXBpY79FzIbXQ/czeDLodk6EfR8a9Tpkn0THh/rfTR53fryEyzdHuKPk9udLmDruzW41+WPmbzRacEMpkbVwd/P33oP9BPRqmtZUll4SW09bHt3n1o44VB12tYfrkl6gp6eRIM6JkjK/mPBa2Utkm35wKanHvlJtXpYsm2fbei/nX0CYINr3EpmU5mDfBCguK2GqRjYu3b3hHNHw4vBB7csfV1yOu6UN7cj2n+IEtt+CJ/z2UpnU1a/OSK2KEI3gI+yi8ItVPvLRNVISCe9HRIRF+UGXaHW78UuV+osZ/sr7xsxUJ9Wl+ecar2QVSvX2cyIHkYVAQcFvpbZ3Q8Npn3F87sjKteteZ64U=";

    private StructureManager structureManager;
    private DeferredStructureSpawner deferredStructureSpawner;
    private int deferredStructureTaskId = -1;
    private volatile boolean regenInProgress = false;
    private AnvilManager anvilManager;
    private SpaceManager spaceManager;
    private MiningOxygenManager miningOxygenManager;
    private SidebarManager sidebarManager;
    private EnchantedManager enchantedManager;
    private EnchantingManager enchantingManager;
    private ShelfManager shelfManager;
    private CulinarySubsystem culinarySubsystem;
    private CulinaryCatalogueManager culinaryCatalogueManager;
    private CulinaryCauldron culinaryCauldron;
    private VillagerTradeManager villagerTradeManager;
    private CultistPopulationManager cultistPopulationManager;
    private SamuraiPopulationManager samuraiPopulationManager;
    private MountainCultistBehaviour mountainCultistBehaviour;
    private MountainCultistSpawnListener mountainCultistSpawnListener;
    private MountainCultistDamageListener mountainCultistDamageListener;
    private MountainCultistAlertListener mountainCultistAlertListener;
    private CherrySamuraiBehaviour cherrySamuraiBehaviour;
    private CherrySamuraiSpawnListener cherrySamuraiSpawnListener;
    private CherrySamuraiDamageListener cherrySamuraiDamageListener;
    private MountainMobSpawnBlocker mountainMobSpawnBlocker;
    private NocturnalStructureManager nocturnalStructureManager;
    private LootRegistry lootRegistry;
    private LootPopulatorManager lootPopulatorManager;
    private StructureEntityManager structureEntityManager;
    private KeystoneManager keystoneManager;
    private CustomEntityRegistry customEntityRegistry;
    private Listener citizensEnableListener;
    private double statRate = 1.0;
    private boolean debugOxygen = false;
    private CurseManager curseManager;
    private TablistManager tablistManager;
    private DamageDisplayManager damageDisplayManager;
    private QuickfireFinisherManager quickfireFinisherManager;
    private CurseEffectController curseEffectController;
    private PotionGuiManager potionGuiManager;
    private LingeringHazardManager lingeringHazardManager;
    private CustomPotionEffectManager customPotionEffectManager;
    private CustomPotionCombatListener customPotionCombatListener;
    private BonusJumpManager bonusJumpManager;
    private NauseaProjectileListener nauseaProjectileListener;
    private SeaCreatureRegistry seaCreatureRegistry;
    private FishingManager fishingManager;
    private MutationManager mutationManager;
    private ParticleEngine particleEngine;
    private PortalReturnManager portalReturnManager;
    // Advancement tabs (optional; may remain null). Only used by commands/listeners defensively.
    public AdvancementTab consegrity, desert, mesa, swamp, cherry, mountain, jungle;

    public ProjectLinearity() {
        instance = this;
    }

    @Override
    public void onEnable() {
        instance = this;
        CustomDurabilityManager.init(this);
        HeirloomManager.init(this);
        enchantedManager = new EnchantedManager(this);
        enchantedManager.start();
        anvilManager = new AnvilManager(this, enchantedManager);
        potionGuiManager = new PotionGuiManager(this);
        spaceManager = new SpaceManager(this);
        spaceManager.load();
        Bukkit.getPluginManager().registerEvents(new SpacePresenceListener(spaceManager), this);
        Bukkit.getPluginManager().registerEvents(new SpaceEventListener(spaceManager, this), this);
        Bukkit.getPluginManager().registerEvents(new SpaceBlockListener(spaceManager), this);
        Bukkit.getPluginManager().registerEvents(new RegionTitleListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ConsegritySpawnListener(), this);
        Bukkit.getPluginManager().registerEvents(new TreeFellingListener(), this);
        Bukkit.getPluginManager().registerEvents(new CropPlantingListener(), this);
        Bukkit.getPluginManager().registerEvents(new CropHarvestListener(), this);
        Bukkit.getPluginManager().registerEvents(new QuartzOreDropListener(), this);

        miningOxygenManager = new MiningOxygenManager(this, spaceManager);
        sidebarManager = new SidebarManager(this, spaceManager, miningOxygenManager);
        Bukkit.getOnlinePlayers().forEach(sidebarManager::initialise);


        shelfManager = new ShelfManager(this);
        culinarySubsystem = CulinarySubsystem.getInstance(this);
        culinaryCatalogueManager = new CulinaryCatalogueManager(this, culinarySubsystem);
        culinarySubsystem.setCatalogueManager(culinaryCatalogueManager);
        culinaryCauldron = new CulinaryCauldron(this);
        villagerTradeManager = new VillagerTradeManager(this, enchantedManager);
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Unable to create plugin data folder.");
        }
        // Kick world creation to the next tick so Bukkit finishes bootstrapping its default worlds first.
        Bukkit.getScheduler().runTask(this, this::ensureConsegrityWorldLoaded);
        lootRegistry = new LootRegistry(this);
        lootPopulatorManager = new LootPopulatorManager(this, lootRegistry);
        Bukkit.getPluginManager().registerEvents(new LootChestOpenListener(this), this);

        particleEngine = new ParticleEngine(this);
        mutationManager = new MutationManager(this, particleEngine);
        registerMutations();
        mutationManager.rehydrateExistingMutations();
        portalReturnManager = new PortalReturnManager(this);
        Bukkit.getPluginManager().registerEvents(portalReturnManager, this);
        portalReturnManager.startup();
        structureEntityManager = new StructureEntityManager(this);
        cultistPopulationManager = new CultistPopulationManager(this);
        boolean cultistsReady = cultistPopulationManager.startup();
        mountainCultistBehaviour = new MountainCultistBehaviour(this, cultistPopulationManager);
        mountainCultistSpawnListener = new MountainCultistSpawnListener(this);
        Bukkit.getPluginManager().registerEvents(mountainCultistSpawnListener, this);
        mountainCultistDamageListener = new MountainCultistDamageListener(cultistPopulationManager);
        Bukkit.getPluginManager().registerEvents(mountainCultistDamageListener, this);
        mountainCultistAlertListener = new MountainCultistAlertListener(cultistPopulationManager, mountainCultistBehaviour);
        Bukkit.getPluginManager().registerEvents(mountainCultistAlertListener, this);
        samuraiPopulationManager = new SamuraiPopulationManager(this);
        boolean samuraiReady = samuraiPopulationManager.startup();
        cherrySamuraiBehaviour = new CherrySamuraiBehaviour(this, samuraiPopulationManager);
        cherrySamuraiSpawnListener = new CherrySamuraiSpawnListener(this);
        Bukkit.getPluginManager().registerEvents(cherrySamuraiSpawnListener, this);
        cherrySamuraiDamageListener = new CherrySamuraiDamageListener(this, samuraiPopulationManager, cherrySamuraiBehaviour);
        Bukkit.getPluginManager().registerEvents(cherrySamuraiDamageListener, this);
        mountainMobSpawnBlocker = new MountainMobSpawnBlocker(cultistPopulationManager);
        Bukkit.getPluginManager().registerEvents(mountainMobSpawnBlocker, this);
        nocturnalStructureManager = new NocturnalStructureManager(this, lootPopulatorManager);
        nocturnalStructureManager.registerStruct("haywagon", 5, 8, 200);
        nocturnalStructureManager.startup();
        customEntityRegistry = new CustomEntityRegistry(this);
        registerCustomEntities();
        seaCreatureRegistry = new SeaCreatureRegistry(this, customEntityRegistry);
        Bukkit.getPluginManager().registerEvents(seaCreatureRegistry, this);
        registerSeaCreatures();
        fishingManager = new FishingManager(this, seaCreatureRegistry);
        curseManager = new CurseManager(this);

        damageDisplayManager = new DamageDisplayManager(this);
        quickfireFinisherManager = new QuickfireFinisherManager(this);
        Bukkit.getPluginManager().registerEvents(quickfireFinisherManager, this);
        // Initialize tablist manager for curse display
        tablistManager = new TablistManager(this, curseManager);
        curseManager.setTablistManager(tablistManager);
        customPotionEffectManager = new CustomPotionEffectManager(this, tablistManager, damageDisplayManager);
        tablistManager.setPotionEffectManager(customPotionEffectManager);
        lingeringHazardManager = new LingeringHazardManager(this, customPotionEffectManager);
        new PotionUsageListener(this, customPotionEffectManager, lingeringHazardManager);
        customPotionCombatListener = new CustomPotionCombatListener(this, customPotionEffectManager);
        bonusJumpManager = new BonusJumpManager(this);
        nauseaProjectileListener = new NauseaProjectileListener(this, customPotionEffectManager);
        curseEffectController = new CurseEffectController(this, curseManager, miningOxygenManager, sidebarManager, customPotionEffectManager);
        Bukkit.getPluginManager().registerEvents(curseEffectController, this);
        curseEffectController.startup();

        // Register zombie attack range listener
        ZombieAttackRangeListener zombieAttackRangeListener = new ZombieAttackRangeListener(this);
        Bukkit.getPluginManager().registerEvents(zombieAttackRangeListener, this);

        // Register curse listener
        CurseListener curseListener = new CurseListener(this, curseManager);
        Bukkit.getPluginManager().registerEvents(curseListener, this);
        
        // Register invisibility listener
        InvisibilityListener invisibilityListener = new InvisibilityListener(this);
        Bukkit.getPluginManager().registerEvents(invisibilityListener, this);
        
        // Register blindness listener
        BlindnessListener blindnessListener = new BlindnessListener(customPotionEffectManager);
        Bukkit.getPluginManager().registerEvents(blindnessListener, this);
        
        // Register night vision listener
        NightVisionListener nightVisionListener = new NightVisionListener(this);
        Bukkit.getPluginManager().registerEvents(nightVisionListener, this);
        
        // Register hunger listener
        HungerListener hungerListener = new HungerListener(this);
        Bukkit.getPluginManager().registerEvents(hungerListener, this);

        new FireballItemListener(this);
        new ThreeHeadedFireballListener(this);
        
        if (!cultistsReady || !samuraiReady) {
            if (citizensEnableListener == null) {
                citizensEnableListener = new CitizensEnableWatcher();
                Bukkit.getPluginManager().registerEvents(citizensEnableListener, this);
            }
        }

        Bukkit.getPluginManager().registerEvents(new CherrySamuraiDeathListener(samuraiPopulationManager), this);


        registerShelfRecipe();

        // Commands
        try { getCommand("regenerate").setExecutor(new RegenerateCommand(this)); } catch (Throwable ignored) {}
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
        try { getCommand("firetickdebug").setExecutor(new FireTickDebugCommand(this)); } catch (Throwable ignored) {}
        try { getCommand("oxygendebug").setExecutor(new OxygenDebugCommand(this)); } catch (Throwable ignored) {}
        try { SetCultistPopulationCommand cmd = new SetCultistPopulationCommand(this); getCommand("setcultistpopulation").setExecutor(cmd); getCommand("setcultistpopulation").setTabCompleter(cmd);} catch (Throwable ignored) {}
        try { SpawnCustomMobCommand cmd = new SpawnCustomMobCommand(this); getCommand("spawncustommob").setExecutor(cmd); getCommand("spawncustommob").setTabCompleter(cmd);} catch (Throwable ignored) {}
        try { CurseCommand cmd = new CurseCommand(curseManager); getCommand("curse").setExecutor(cmd); getCommand("curse").setTabCompleter(cmd);} catch (Throwable ignored) {}
        try { PotionCatalogueCommand cmd = new PotionCatalogueCommand(this, potionGuiManager); getCommand("potions").setExecutor(cmd);} catch (Throwable ignored) {}
        try { CleanseCommand cmd = new CleanseCommand(this); getCommand("cleanse").setExecutor(cmd); getCommand("cleanse").setTabCompleter(cmd);} catch (Throwable ignored) {}
        try { EffectCustomCommand cmd = new EffectCustomCommand(this); getCommand("effectcustom").setExecutor(cmd); getCommand("effectcustom").setTabCompleter(cmd);} catch (Throwable ignored) {}
        try { SynthesizeCommand cmd = new SynthesizeCommand(); getCommand("synthesize").setExecutor(cmd); getCommand("synthesize").setTabCompleter(cmd);} catch (Throwable ignored) {}
        try { CulinaryCatalogueCommand cmd = new CulinaryCatalogueCommand(this, culinaryCatalogueManager, culinarySubsystem); getCommand("culinary").setExecutor(cmd); getCommand("culinary").setTabCompleter(cmd);} catch (Throwable ignored) {}
        try {
            SaveInventoryCommand cmd = new SaveInventoryCommand(this, lootRegistry);
            getCommand("saveinventory").setExecutor(cmd);
            getCommand("saveinventory").setTabCompleter(cmd);
            Bukkit.getPluginManager().registerEvents(cmd, this);
        } catch (Throwable ignored) {}
        try {
            StructureDebugCommand cmd = new StructureDebugCommand(this);
            getCommand("structuredebug").setExecutor(cmd);
            getCommand("structuredebug").setTabCompleter(cmd);
        } catch (Throwable ignored) {}

        // Managers
        structureManager = new StructureManager(this);
        structureManager.setLootPopulatorManager(lootPopulatorManager);
        structureManager.setStructureEntityManager(structureEntityManager);
        // Enable deferred spawner (periodic + on chunk load) to place as you explore
        deferredStructureSpawner = new DeferredStructureSpawner(this, structureManager);
        Bukkit.getPluginManager().registerEvents(deferredStructureSpawner, this);
        Bukkit.getPluginManager().registerEvents(new StructureListener(structureManager), this);
        // Natural spawning only; no sector-based enforcement
        try {
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(this, deferredStructureSpawner, 1L, 5L);
            deferredStructureTaskId = task.getTaskId();
        } catch (Throwable ignore) {
            deferredStructureTaskId = -1;
        }

        enchantingManager = new EnchantingManager(this, enchantedManager);
        enchantingManager.start();
        registerRecipes();

        try {
            structureManager.registerStruct("jungletemple", 24, 7, 100, new JungleSector(), GenCheckType.SURFACE, true, 300);
            structureManager.registerStruct("deserttemple", 30, 4, 200, new DesertBiome(), GenCheckType.SURFACE, true, 100);
            structureManager.registerStruct("witchhut", 9, 8, 150, new SwampSector(), GenCheckType.SURFACE, true, 250);
            structureManager.registerStruct("witchfestival", 60, 1, 200, new SwampSector(), GenCheckType.SURFACE, true, 300);
            structureManager.registerStruct("monastery", 30, 1, 100, new CherrySector(), GenCheckType.SURFACE, true, 500);
            structureManager.registerStruct("hotspring", 10, 20, 100, new CherrySector(), GenCheckType.SURFACE, true, 120);
            structureManager.registerStruct("sugarcane", 10, 3, 100, new DesertBiome(), GenCheckType.SURFACE, true, 120);
            structureManager.registerStruct("beehive", 5, 20, 30, new MountainSector(), GenCheckType.SURFACE, true, 0);
            structureManager.registerStruct("monument", 70, 20, 500, new OceanSector(), GenCheckType.UNDERWATER, true, 80);
            structureManager.registerStruct("jadestatue1", 20, 1, 800, new JungleSector(), GenCheckType.SURFACE, true, 0);
            structureManager.registerStruct("beacon0", 26, 1, 2, new MountainSector(), GenCheckType.SURFACE, true, 400);
            structureManager.registerStruct("conduit1", 70, 1, 800, new OceanSector(), GenCheckType.UNDERWATER, true, 0);
            structureManager.registerStruct("sarcophagus0", 35, 1, 800, new MesaSector(), GenCheckType.SURFACE, true, 0);
            structureManager.registerStruct("pillager", 20, 12, 200, new MesaSector(), GenCheckType.SURFACE, true, 80);
            structureManager.registerStruct("prospect", 20, 8, 200, new MesaSector(), GenCheckType.SURFACE, true, 80);
            structureManager.registerStruct("seamine", 4, 100, 200, new OceanSector(), GenCheckType.UNDERWATER, true, 10, Biome.FROZEN_OCEAN);
            structureManager.registerStruct("seamine", 4, 200, 50, new OceanSector(), GenCheckType.UNDERWATER, true, 80, null);

            structureManager.registerStruct("beets", 6, 5, 50, new SavannaSector(), GenCheckType.SURFACE, true, 80, null);
            structureManager.registerStruct("potatos", 6, 5, 50, new SwampSector(), GenCheckType.SURFACE, true, 80, null);
            structureManager.registerStruct("carritz", 6, 5, 50, new CherrySector(), GenCheckType.SURFACE, true, 80, null);


        } catch (Throwable ignored) {}
        registerStructureEntities();
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
                "sarcophagus0",
                ChatColor.GOLD + "Golden Sarcophagus",
                "sarcophagus0",
                java.util.List.of("sarcophagus0", "sarcophagus1", "sarcophagus2",
                        "sarcophagus3", "sarcophagus4", "sarcophagus5",
                        "sarcophagus6", "sarcophagus7", "sarcophagus8"),
                25,  // bounds
                20,  // height
                512, // totalProgress (gold ingots)
                new KeystoneManager.RequiredItem(Material.GOLD_INGOT),
                25,  // protectionRadius
                30.0, // notifyRadius
                20.0, // rewardRadius
                player -> player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 1200, 1, true, false, true))
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
                256,
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
        particleEngine = new ParticleEngine(this);
    }

    private void ensureConsegrityWorldLoaded() {
        String worldName = RegenerateCommand.WORLD_NAME;
        try {
            World existing = Bukkit.getWorld(worldName);
            if (existing != null) {
                return;
            }
            WorldCreator creator = new WorldCreator(worldName);
            creator.generateStructures(false);
            creator.generator(new ConsegrityChunkGenerator());
            World created = Bukkit.createWorld(creator);
            if (created == null) {
                getLogger().warning("[Consegrity] Unable to load world '" + worldName + "' (Bukkit returned null).");
                return;
            }
            try {
                created.setKeepSpawnInMemory(false);
            } catch (Throwable ignored) {}
            getLogger().info("[Consegrity] Loaded world '" + worldName + "'.");
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Failed to load Consegrity world '" + worldName + "'", t);
        }
    }

    private void registerStructureEntities() {
        if (structureEntityManager == null) {
            return;
        }
        structureEntityManager.registerStructureEntities(StructureType.DESERT_TEMPLE, EntityType.HUSK, 15, 15, 22, 1);
        structureEntityManager.registerStructureEntities(StructureType.MONUMENT, EntityType.GUARDIAN, 14, 26, 50, 2);
        structureEntityManager.registerStructureEntities(StructureType.WITCH_HUT, EntityType.WITCH, 4, 4, 10, 1);
        structureEntityManager.registerStructureEntities(StructureType.WITCH_FESTIVAL, EntityType.WITCH, 12, 16, 20, 1);
        structureEntityManager.registerStructureEntities(StructureType.BEEHHIVE, EntityType.BEE, 4, 4, 10, 1);
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

        NamespacedKey beetrootJuiceKey = new NamespacedKey(this, "beetroot_juice");
        try {
            Bukkit.removeRecipe(beetrootJuiceKey);
        } catch (Throwable ignored) {}
        try {
            ShapedRecipe beetrootJuiceRecipe = new ShapedRecipe(beetrootJuiceKey, ItemRegistry.getBeetrootJuice());
            beetrootJuiceRecipe.shape("B", "B", "G");
            beetrootJuiceRecipe.setIngredient('B', Material.BEETROOT);
            beetrootJuiceRecipe.setIngredient('G', Material.GLASS_BOTTLE);
            Bukkit.addRecipe(beetrootJuiceRecipe);
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

    public void registerMutation(String mutationId,
                                 EntityType entityType,
                                 int percentage,
                                 Color armorColor,
                                 String headTexture,
                                 Stats stats,
                                 String mutationName,
                                 MutationBehavior behavior,
                                 ItemStack drop,
                                 int min,
                                 int max,
                                 int dropChancePercentage,
                                 Biome... allowedBiomes) {
        if (mutationManager == null) {
            getLogger().warning("Mutation manager is not initialised; skipping mutation registration for " + entityType);
            return;
        }
        Biome[] biomes = allowedBiomes == null ? new Biome[0] : allowedBiomes;
        mutationManager.registerMutation(
                mutationId,
                entityType,
                percentage,
                armorColor,
                headTexture,
                stats,
                mutationName,
                behavior,
                drop,
                min,
                max,
                dropChancePercentage,
                false,
                null,
                null,
                null,
                0.0,
                0,
                0f,
                0f,
                biomes
        );
    }

    public void registerMutationWithAmbient(String mutationId,
                                            EntityType entityType,
                                            int percentage,
                                            Color armorColor,
                                            String headTexture,
                                            Stats stats,
                                            String mutationName,
                                            MutationBehavior behavior,
                                            ItemStack drop,
                                            int min,
                                            int max,
                                            int dropChancePercentage,
                                            Color ambientDustColor,
                                            Particle ambientParticle,
                                            Sound ambientSound,
                                            double ambientIntervalSeconds,
                                            int ambientSoundCooldownSeconds,
                                            float ambientSoundVolume,
                                            float ambientSoundPitch,
                                            Biome... allowedBiomes) {
        if (mutationManager == null) {
            getLogger().warning("Mutation manager is not initialised; skipping mutation registration for " + entityType);
            return;
        }
        Biome[] biomes = allowedBiomes == null ? new Biome[0] : allowedBiomes;
        mutationManager.registerMutation(
                mutationId,
                entityType,
                percentage,
                armorColor,
                headTexture,
                stats,
                mutationName,
                behavior,
                drop,
                min,
                max,
                dropChancePercentage,
                true,
                ambientDustColor,
                ambientParticle,
                ambientSound,
                ambientIntervalSeconds,
                ambientSoundCooldownSeconds,
                ambientSoundVolume,
                ambientSoundPitch,
                biomes
        );
    }

    private void registerMutations() {
        registerMutationWithAmbient(
                "gilded_wailer",
                EntityType.GHAST,
                25,
                Color.WHITE,
                null,
                Stats.of(StatType.Health(50)),
                ChatColor.GOLD + "Wailer",
                MutationBehavior.NONE,
                ItemRegistry.getGoldenTear(),
                1,
                1,
                100,
                Color.fromRGB(255, 255, 255),
                Particle.DUST,
                Sound.ENTITY_GHAST_AMBIENT,
                0.1,
                25,
                200f,
                0.5f
        );
        registerMutation(
                "three_headed_ghast",
                EntityType.GHAST,
                1,
                Color.fromRGB(255, 64, 64),
                null,
                Stats.of(StatType.Health(500)),
                ChatColor.DARK_RED + "Three Headed Ghast",
                MutationBehavior.THREE_HEADED_GHAST,
                ItemRegistry.getFireball(),
                4,
                16,
                100,
                Biome.NETHER_WASTES,
                Biome.SOUL_SAND_VALLEY,
                Biome.BASALT_DELTAS,
                Biome.CRIMSON_FOREST,
                Biome.WARPED_FOREST
        );
        registerMutationWithAmbient(
                "seer",
                EntityType.ZOMBIFIED_PIGLIN,
                1,
                Color.ORANGE,
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2M1MTY5N2RlZTg5NDQ3ZmM1MTc5MGQ4ZGEzNWY4NDdhNTQ1OGYzZDJjM2FhY2Q5NmRiZjJhMzU2NTFiOTMxMCJ9fX0=",
                Stats.of(StatType.Damage(3), StatType.Speed(2)),
                ChatColor.GOLD + "Seer",
                MutationBehavior.NONE,
                ItemRegistry.getGoldenEye(),
                1,
                1,
                100,
                Color.fromRGB(255, 220, 60),
                Particle.DUST,
                Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                3.0,
                6,
                50f,
                0.5f
        );
        registerMutationWithAmbient(
                "crimson_cultivator",
                EntityType.PIGLIN,
                6,
                Color.RED,
                null,
                Stats.of(StatType.Health(90), StatType.Damage(4), StatType.Resistance(20)),
                ChatColor.DARK_RED + "Crimson Cultivator",
                MutationBehavior.NONE,
                ItemRegistry.getRedSugarCane(),
                1,
                1,
                100,
                Color.fromRGB(200, 40, 40),
                Particle.DUST,
                Sound.ENTITY_PIGLIN_AMBIENT,
                3.0,
                8,
                40f,
                0.6f,
                Biome.CRIMSON_FOREST
        );
        registerMutationWithAmbient(
                "sand_scavenger",
                EntityType.HUSK,
                10,
                null,
                null,
                Stats.of(),
                ChatColor.DARK_GREEN + "Sand Scavenger",
                MutationBehavior.SCAVENGER,
                ItemRegistry.getMoldingFlesh(),
                1,
                1,
                100,
                Color.fromRGB(194, 178, 128),  // Sand color
                Particle.DUST,
                Sound.ENTITY_CAMEL_STEP_SAND,
                1.0,
                3,
                20f,
                0.5f
        );
        registerMutationWithAmbient(
                "withered_zombie",
                EntityType.WITHER_SKELETON,
                20,
                null,
                null,
                Stats.of(StatType.Health(40)),
                ChatColor.DARK_GRAY + "Withered Zombie",
                MutationBehavior.WITHERED,
                ItemRegistry.getCharredFlesh(),
                1,
                1,
                4,
                Color.fromRGB(24,24,24),
                Particle.DUST,
                Sound.BLOCK_NOTE_BLOCK_IMITATE_WITHER_SKELETON,
                0.1,
                2,
                5f,
                0.5f
        );
        registerMutationWithAmbient(
                "the_charred",
                EntityType.SKELETON,
                100,
                null,
                null,
                Stats.of(StatType.Health(1)),
                ChatColor.DARK_GRAY + "The Charred",
                MutationBehavior.CHARRED,
                ItemRegistry.getWeakenedMarrow(),
                1,
                1,
                10,
                Color.fromRGB(16,16,16),
                Particle.DUST,
                Sound.BLOCK_NOTE_BLOCK_IMITATE_WITHER_SKELETON,
                0.1,
                10,
                5f,
                0.5f,
                Biome.SOUL_SAND_VALLEY
        );
        registerMutationWithAmbient(
                "necromancer",
                EntityType.WITCH,
                10,
                null,
                null,
                Stats.of(StatType.Health(50)),
                ChatColor.BLUE + "Necromancer",
                MutationBehavior.NONE,
                new ItemStack(Material.WITHER_ROSE),
                1,
                1,
                100,
                Color.fromRGB(0,0,0),
                Particle.DUST,
                Sound.BLOCK_NOTE_BLOCK_IMITATE_WITHER_SKELETON,
                0.1,
                10,
                20f,
                0.5f
        );
        registerMutationWithAmbient(
                "warped_cultivator",
                EntityType.ENDERMAN,
                10,
                null,
                null,
                Stats.of(StatType.Health(100)),
                ChatColor.DARK_PURPLE + "Warped Cultivator",
                MutationBehavior.NONE,
                ItemRegistry.getWarpedApple(),
                1,
                1,
                100,
                Color.fromRGB(78, 168, 255),
                Particle.DUST,
                Sound.ENTITY_ENDERMAN_AMBIENT,
                0.1,
                10,
                50f,
                0.4f,
                Biome.WARPED_FOREST
        );
        registerMutationWithAmbient(
                "veil_warden",
                EntityType.ENDERMAN,
                8,
                Color.fromRGB(30, 120, 160),
                null,
                Stats.of(StatType.Health(120), StatType.Damage(6)),
                ChatColor.DARK_AQUA + "Veil Warden",
                MutationBehavior.VEIL_WARDEN,
                null,
                0,
                0,
                0,
                Color.fromRGB(60, 200, 255),
                Particle.DUST,
                Sound.ENTITY_ENDERMAN_AMBIENT,
                0.2,
                8,
                25f,
                0.8f,
                Biome.WARPED_FOREST
        );
        registerMutationWithAmbient(
                "sneak_mite",
                EntityType.ENDERMITE,
                100,
                Color.fromRGB(60, 140, 255),
                null,
                Stats.of(StatType.Health(1)),
                ChatColor.DARK_AQUA + "SneakMite",
                MutationBehavior.SNEAK_MITE,
                ItemRegistry.getWarpedVeil(),
                1,
                1,
                100,
                Color.fromRGB(90, 200, 255),
                Particle.PORTAL,
                Sound.ENTITY_ENDERMITE_AMBIENT,
                0.2,
                6,
                10f,
                1.2f,
                Biome.WARPED_FOREST
        );
        registerMutationWithAmbient(
                "headless_horseman",
                EntityType.SPIDER,
                1,
                null,
                null,
                Stats.of(StatType.Health(100), StatType.Speed(4), StatType.Damage(4)),
                ChatColor.DARK_PURPLE + "Knightmare",
                MutationBehavior.NONE,
                new ItemStack(Material.JACK_O_LANTERN),
                1,
                1,
                100,
                Color.fromRGB(255,215,0),
                Particle.DUST,
                Sound.AMBIENT_CAVE,
                0.1,
                10,
                50f,
                0.4f,
                Biome.SAVANNA
        );
        registerMutationWithAmbient(
                "tempest_creeper",
                EntityType.CREEPER,
                1,
                Color.fromRGB(80, 170, 255),
                null,
                Stats.of(StatType.Health(40)),
                ChatColor.AQUA + "Cloudbreaker",
                MutationBehavior.VOLTAIC_CREEPER,
                ItemRegistry.getCloudbreaker(),
                1,
                1,
                75,
                Color.fromRGB(80, 170, 255),
                Particle.ELECTRIC_SPARK,
                Sound.ENTITY_CREEPER_HURT,
                0.3,
                8,
                10f,
                0.3f
        );
        registerMutationWithAmbient(
                "stormcaller_blaze",
                EntityType.BLAZE,
                8,
                Color.fromRGB(255, 180, 64),
                null,
                Stats.of(StatType.Health(50), StatType.Damage(5)),
                ChatColor.LIGHT_PURPLE + "Stormcaller Blaze",
                MutationBehavior.STORMCALLER,
                ItemRegistry.getVoltaicChainmail(),
                1,
                1,
                60,
                Color.fromRGB(130, 70, 255),
                Particle.ELECTRIC_SPARK,
                Sound.BLOCK_FIRE_AMBIENT,
                0.6,
                6,
                35f,
                0.4f,
                Biome.NETHER_WASTES,
                Biome.BASALT_DELTAS
        );
        registerMutationWithAmbient(
                "dust_demon",
                EntityType.MAGMA_CUBE,
                12,
                Color.fromRGB(96, 50, 32),
                null,
                Stats.of(StatType.Health(60), StatType.Damage(5)),
                ChatColor.DARK_GRAY + "Dust Demon",
                MutationBehavior.DUST_DEMON,
                ItemRegistry.getAsh(),
                1,
                2,
                80,
                Color.fromRGB(120, 80, 60),
                Particle.ASH,
                Sound.ITEM_BRUSH_BRUSHING_GRAVEL,
                0.4,
                8,
                20f,
                1.0f,
                Biome.NETHER_WASTES,
                Biome.BASALT_DELTAS,
                Biome.SOUL_SAND_VALLEY
        );
        registerMutationWithAmbient(
                "legendary_hog",
                EntityType.HOGLIN,
                10,
                null,
                null,
                Stats.of(StatType.Health(100), StatType.Damage(4), StatType.Resistance(20)),
                ChatColor.GOLD + "Legendary Hog",
                MutationBehavior.NONE,
                ItemRegistry.getHoglinRoast(),
                1,
                1,
                100,
                Color.fromRGB(255,215,0),
                Particle.DUST,
                Sound.ENTITY_PIG_AMBIENT,
                0.1,
                10,
                50f,
                0.4f,
                Biome.CRIMSON_FOREST
        );
        registerMutationWithAmbient(
                "rabid_cube",
                EntityType.MAGMA_CUBE,
                12,
                Color.fromRGB(96, 50, 32),
                null,
                Stats.of(StatType.Health(60), StatType.Damage(5)),
                ChatColor.DARK_GRAY + "Rabid Cube",
                MutationBehavior.RABID_CUBE,
                ItemRegistry.getMagmaCubeFoot(),
                1,
                2,
                80,
                Color.fromRGB(120, 80, 60),
                Particle.ASH,
                Sound.BLOCK_SAND_BREAK,
                0.4,
                8,
                10f,
                1.0f,
                Biome.NETHER_WASTES,
                Biome.BASALT_DELTAS,
                Biome.SOUL_SAND_VALLEY
        );

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
        customEntityRegistry.register(new CustomEntityRegistry.CustomEntityEntry(
                "samurai",
                "Cherry Samurai",
                "Spawns a Cherry Samurai NPC for testing.",
                List.of("samurai", "cherry_samurai", "cherrysamurai"),
                (pl, location, sender) -> {
                    SamuraiPopulationManager manager = pl.getSamuraiPopulationManager();
                    if (manager == null) {
                        return CustomEntityRegistry.SpawnResult.failure("Samurai manager is unavailable.");
                    }
                    Optional<NPC> npcOptional = manager.spawnSamuraiEntity(location);
                    if (npcOptional.isEmpty()) {
                        return CustomEntityRegistry.SpawnResult.failure("Unable to spawn Cherry Samurai. See console for details.");
                    }
                    NPC npc = npcOptional.get();
                    Location spawnLocation = npc.getEntity() != null ? npc.getEntity().getLocation() : location;
                    String worldName = spawnLocation.getWorld() != null ? spawnLocation.getWorld().getName() : "unknown";
                    String message = String.format(
                            "Spawned Cherry Samurai (%s) at %s x=%.1f y=%.1f z=%.1f",
                            npc.getUniqueId(),
                            worldName,
                            spawnLocation.getX(),
                            spawnLocation.getY(),
                            spawnLocation.getZ());
                    return CustomEntityRegistry.SpawnResult.success(message);
                }
        ));
        customEntityRegistry.register(new CustomEntityRegistry.CustomEntityEntry(
                "cursed_zombie",
                "Cursed Zombie",
                "Spawns a cursed zombie for testing curse mechanics.",
                List.of("cursedzombie", "czombie"),
                (pl, location, sender) -> {
                    try {
                        org.bukkit.entity.Zombie zombie = (org.bukkit.entity.Zombie) location.getWorld().spawnEntity(location, org.bukkit.entity.EntityType.ZOMBIE);
                        goat.projectLinearity.subsystems.mechanics.listeners.CurseListener.markMonsterAsCursed(zombie);
                        return CustomEntityRegistry.SpawnResult.success("Spawned cursed zombie at " + location.getX() + ", " + location.getY() + ", " + location.getZ());
                    } catch (Exception e) {
                        return CustomEntityRegistry.SpawnResult.failure("Failed to spawn cursed zombie: " + e.getMessage());
                    }
                }
        ));
    }

    private void registerSeaCreatures() {
        if (seaCreatureRegistry == null) {
            return;
        }
        seaCreatureRegistry.register(new SeaCreatureRegistry.SeaCreatureDefinition(
                "deep_sea_diver",
                "Deep Sea Diver",
                DEEP_SEA_DIVER_SKIN_VALUE,
                DEEP_SEA_DIVER_SKIN_SIGNATURE,
                SeaCreatureRegistry.BehaviorTrait.DIVER,
                200.0,
                6.0,
                List.of()
        ));
        seaCreatureRegistry.registerLoot("deep_sea_diver", "Sponge", 1, 1, 1, 2);
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
        if (cherrySamuraiBehaviour != null) {
            cherrySamuraiBehaviour.shutdown();
            cherrySamuraiBehaviour = null;
        }
        if (samuraiPopulationManager != null) {
            samuraiPopulationManager.shutdown();
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
        if (cherrySamuraiSpawnListener != null) {
            HandlerList.unregisterAll(cherrySamuraiSpawnListener);
            cherrySamuraiSpawnListener = null;
        }
        if (cherrySamuraiDamageListener != null) {
            HandlerList.unregisterAll(cherrySamuraiDamageListener);
            cherrySamuraiDamageListener = null;
        }
        if (mountainMobSpawnBlocker != null) {
            HandlerList.unregisterAll(mountainMobSpawnBlocker);
            mountainMobSpawnBlocker = null;
        }
        if (mutationManager != null) {
            mutationManager.shutdown();
            mutationManager = null;
        }
        if (portalReturnManager != null) {
            HandlerList.unregisterAll(portalReturnManager);
            portalReturnManager.shutdown();
            portalReturnManager = null;
        }
        if (nocturnalStructureManager != null) {
            nocturnalStructureManager.shutdown();
            nocturnalStructureManager = null;
        }
        if (keystoneManager != null) {
            keystoneManager.shutdown();
            keystoneManager = null;
        }
        if (deferredStructureSpawner != null) {
            if (deferredStructureTaskId != -1) {
                try { Bukkit.getScheduler().cancelTask(deferredStructureTaskId); } catch (Throwable ignored) {}
                deferredStructureTaskId = -1;
            }
            deferredStructureSpawner.shutdown();
            deferredStructureSpawner = null;
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
        if (curseManager != null) {
            curseManager.shutdown();
        }
        if (curseEffectController != null) {
            curseEffectController.shutdown();
            curseEffectController = null;
        }
        if (tablistManager != null) {
            tablistManager.shutdown();
        }
        if (customPotionCombatListener != null) {
            customPotionCombatListener.shutdown();
            customPotionCombatListener = null;
        }
        if (bonusJumpManager != null) {
            bonusJumpManager.shutdown();
            bonusJumpManager = null;
        }
        if (nauseaProjectileListener != null) {
            HandlerList.unregisterAll(nauseaProjectileListener);
            nauseaProjectileListener = null;
        }
        if (lingeringHazardManager != null) {
            lingeringHazardManager.shutdown();
            lingeringHazardManager = null;
        }
        if (customPotionEffectManager != null) {
            customPotionEffectManager.shutdown();
        }
        if (damageDisplayManager != null) {
            damageDisplayManager.shutdown();
            damageDisplayManager = null;
        }
        if (fishingManager != null) {
            fishingManager.shutdown();
            fishingManager = null;
        }
        if (seaCreatureRegistry != null) {
            seaCreatureRegistry.shutdown();
            seaCreatureRegistry = null;
        }
        if (particleEngine != null) {
            particleEngine.shutdown();
            particleEngine = null;
        }
        instance = null;
    }

    public StructureManager getStructureManager() { return structureManager; }
    public LootPopulatorManager getLootPopulatorManager() { return lootPopulatorManager; }
    public boolean isRegenInProgress() { return regenInProgress; }
    public void setRegenInProgress(boolean inProgress) { this.regenInProgress = inProgress; }
    public SpaceManager getSpaceManager() { return spaceManager; }
    public SidebarManager getSidebarManager() { return sidebarManager; }
    public MiningOxygenManager getMiningOxygenManager() { return miningOxygenManager; }
    public EnchantedManager getEnchantedManager() { return enchantedManager; }
    public PotionGuiManager getPotionGuiManager() { return potionGuiManager; }
    public LingeringHazardManager getLingeringHazardManager() { return lingeringHazardManager; }
    public CultistPopulationManager getCultistPopulationManager() { return cultistPopulationManager; }
    public SamuraiPopulationManager getSamuraiPopulationManager() { return samuraiPopulationManager; }
    public CherrySamuraiBehaviour getCherrySamuraiBehaviour() { return cherrySamuraiBehaviour; }
    public MountainCultistBehaviour getMountainCultistBehaviour() { return mountainCultistBehaviour; }
    public KeystoneManager getKeystoneManager() { return keystoneManager; }
    public CustomEntityRegistry getCustomEntityRegistry() { return customEntityRegistry; }
    public MutationManager getMutationManager() { return mutationManager; }
    public CustomPotionEffectManager getCustomPotionEffectManager() { return customPotionEffectManager; }
    public DamageDisplayManager getDamageDisplayManager() { return damageDisplayManager; }
    public BonusJumpManager getBonusJumpManager() { return bonusJumpManager; }
    public StructureEntityManager getStructureEntityManager() { return structureEntityManager; }
    public ParticleEngine getParticleEngine() { return particleEngine; }
    public double getStatRate() { return statRate; }
    public void setStatRate(double rate) { this.statRate = Math.max(0.01, rate); }
    public boolean isDebugOxygen() { return debugOxygen; }
    public void setDebugOxygen(boolean debug) { this.debugOxygen = debug; }
    public CurseManager getCurseManager() { return curseManager; }
    public TablistManager getTablistManager() { return tablistManager; }

    public static ProjectLinearity getInstance() { return instance; }

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
            boolean cultistsReady = cultistPopulationManager == null || cultistPopulationManager.startup();
            boolean samuraiReady = samuraiPopulationManager == null || samuraiPopulationManager.startup();
            if (cultistsReady && samuraiReady) {
                HandlerList.unregisterAll(this);
                citizensEnableListener = null;
            }
        }
    }
}
