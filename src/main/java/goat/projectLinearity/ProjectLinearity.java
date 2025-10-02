package goat.projectLinearity;

import com.fren_gor.ultimateAdvancementAPI.AdvancementMain;
import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import com.fren_gor.ultimateAdvancementAPI.UltimateAdvancementAPI;
import com.fren_gor.ultimateAdvancementAPI.advancement.RootAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.FancyAdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import com.fren_gor.ultimateAdvancementAPI.util.CoordAdapter;
import goat.projectLinearity.advs.AdvancementTabNamespaces;
import goat.projectLinearity.advs.consegrity.*;
import goat.projectLinearity.advs.desert.*;
import goat.projectLinearity.advs.mesa.*;
import goat.projectLinearity.advs.swamp.*;
import goat.projectLinearity.advs.cherry.*;
import goat.projectLinearity.advs.mountain.*;
import goat.projectLinearity.advs.jungle.*;
import goat.projectLinearity.commands.RegenerateCommand;
import goat.projectLinearity.world.ConsegritySpawnListener;
import goat.projectLinearity.world.RegionTitleListener;
import goat.projectLinearity.world.DeferredSpawnManager;
import goat.projectLinearity.world.ConsegrityRegions;
import goat.projectLinearity.structure.StructureManager;
import goat.projectLinearity.structure.DeferredStructureSpawner;
import goat.projectLinearity.structure.GenCheckType;
import goat.projectLinearity.structure.StructureListener;
import goat.projectLinearity.world.sector.JungleSector;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ProjectLinearity extends JavaPlugin implements Listener {

    public static UltimateAdvancementAPI api;
    private AdvancementMain uaaMain;
    public AdvancementTab consegrity;
    private final java.util.List<com.fren_gor.ultimateAdvancementAPI.advancement.Advancement> consegrityAdvancements = new java.util.ArrayList<>();


    @Override
    public void onLoad() {
        // Prepare UAA as early as possible
        uaaMain = new AdvancementMain(this);
        uaaMain.load();
    }

    @Override
    public void onEnable() {
        // Initialize the shaded UltimateAdvancementAPI before using it
        if (uaaMain == null) {
            uaaMain = new AdvancementMain(this);
            uaaMain.load();
        }
        uaaMain.enableInMemory();
        api = UltimateAdvancementAPI.getInstance(this);

        initializeAdvancements();
        initializeTabs();

        Bukkit.getPluginManager().registerEvents(this, this);
        // Register dev command
        getCommand("regenerate").setExecutor(new RegenerateCommand());
        try {
            getCommand("getallconsegrityadvancements").setExecutor(new goat.projectLinearity.commands.GetAllConsegrityAdvancementsCommand(this));
        } catch (Throwable ignored) {}
        // Register world population listener (fish/squid near kelp)
        Bukkit.getPluginManager().registerEvents(new ConsegritySpawnListener(), this);
        // Register deferred spawn manager for sector-based entities
        DeferredSpawnManager dsm = new DeferredSpawnManager();
        Bukkit.getPluginManager().registerEvents(dsm, this);
        try { Bukkit.getScheduler().runTaskTimer(this, dsm, 1L, 5L); } catch (Throwable ignore) {}
        // Register structure manager and deferred structure spawner (post-generation)
        StructureManager structMgr = new StructureManager(this);
        this.structureManager = structMgr;
        DeferredStructureSpawner structSpawner = new DeferredStructureSpawner(structMgr);
        Bukkit.getPluginManager().registerEvents(structSpawner, this);
        Bukkit.getPluginManager().registerEvents(new StructureListener(structMgr), this);
        try { Bukkit.getScheduler().runTaskTimer(this, structSpawner, 1L, 10L); } catch (Throwable ignore) {}
        // Register region title listener
        Bukkit.getPluginManager().registerEvents(new RegionTitleListener(this), this);

        // Register initial structures
        try {
            // Spawn 10 jungle temples on jungle surface, modest footprint and spacing
            structMgr.registerStruct("jungletemple", 24, 10, 100, new JungleSector(), GenCheckType.SURFACE, true);
        } catch (Throwable t) {
            getLogger().warning("Failed to register default structures: " + t.getMessage());
        }
    }
    public AdvancementTab desert;
    public AdvancementTab mesa;
    public AdvancementTab swamp;
    public AdvancementTab cherry;
    public AdvancementTab mountain;
    public AdvancementTab jungle;
    private StructureManager structureManager;

    public void initializeTabs() {
        api = UltimateAdvancementAPI.getInstance(this);
        consegrity = api.createAdvancementTab(AdvancementTabNamespaces.consegrity_NAMESPACE);
        AdvancementKey combatrootKey = new AdvancementKey(consegrity.getNamespace(), "combatroot");
        CoordAdapter adapterconsegrity = CoordAdapter.builder().add(combatrootKey, 0f, 0f).add(Slay1.KEY, 1f, 0f).add(Slay10.KEY, 1f, -1f).add(Slay50.KEY, 1f, -2f).add(Slay100.KEY, 1f, -3f).add(Slay500.KEY, 1f, -4f).add(Slay1000.KEY, 1f, -5f).add(Freezing.KEY, 5f, 0f).add(Hypothermia.KEY, 5f, 1f).add(Breed.KEY, 3f, 0f).add(Plant.KEY, 2f, 0f).add(Fishing.KEY, 4f, 0f).add(Icefish.KEY, 5f, -1f).add(Completeconduit.KEY, 8f, -1f).add(Locateconduit.KEY, 6f, -1f).add(Repairconduit.KEY, 7f, -1f).build();
        desert = api.createAdvancementTab(AdvancementTabNamespaces.desert_NAMESPACE);
        AdvancementKey enterdesertKey = new AdvancementKey(desert.getNamespace(), "enterdesert");
        CoordAdapter adapterdesert = CoordAdapter.builder().add(combatrootKey, 0f, 0f).add(Slay1.KEY, 1f, 0f).add(Slay10.KEY, 1f, -1f).add(Slay50.KEY, 1f, -2f).add(Slay100.KEY, 1f, -3f).add(Slay500.KEY, 1f, -4f).add(Slay1000.KEY, 1f, -5f).add(Freezing.KEY, 5f, 0f).add(Hypothermia.KEY, 5f, 1f).add(Breed.KEY, 3f, 0f).add(Plant.KEY, 2f, 0f).add(Fishing.KEY, 4f, 0f).add(Icefish.KEY, 5f, -1f).add(Completeconduit.KEY, 8f, -1f).add(Locateconduit.KEY, 6f, -1f).add(Repairconduit.KEY, 7f, -1f).add(enterdesertKey, 0f, 0f).add(Slayhusk.KEY, 1f, 0f).add(Slaycursed.KEY, 2f, 0f).add(Getcursed.KEY, 2f, -1f).add(Allcurses.KEY, 2f, -2f).add(Dropcursedgold.KEY, 3f, 0f).add(Applydesertenchant.KEY, 4f, 0f).add(Fulldesertenchant.KEY, 4f, 1f).add(Obtaincactus.KEY, -1f, 0f).add(Findtemple.KEY, -2f, 0f).add(Finddesertlapis.KEY, 5f, 0f).build();
        mesa = api.createAdvancementTab(AdvancementTabNamespaces.mesa_NAMESPACE);
        AdvancementKey entermesaKey = new AdvancementKey(mesa.getNamespace(), "entermesa");
        CoordAdapter adaptermesa = CoordAdapter.builder().add(combatrootKey, 0f, 0f).add(Slay1.KEY, 1f, 0f).add(Slay10.KEY, 1f, -1f).add(Slay50.KEY, 1f, -2f).add(Slay100.KEY, 1f, -3f).add(Slay500.KEY, 1f, -4f).add(Slay1000.KEY, 1f, -5f).add(Freezing.KEY, 5f, 0f).add(Hypothermia.KEY, 5f, 1f).add(Breed.KEY, 3f, 0f).add(Plant.KEY, 2f, 0f).add(Fishing.KEY, 4f, 0f).add(Icefish.KEY, 5f, -1f).add(Completeconduit.KEY, 8f, -1f).add(Locateconduit.KEY, 6f, -1f).add(Repairconduit.KEY, 7f, -1f).add(enterdesertKey, 0f, 0f).add(Slayhusk.KEY, 1f, 0f).add(Slaycursed.KEY, 2f, 0f).add(Getcursed.KEY, 2f, -1f).add(Allcurses.KEY, 2f, -2f).add(Dropcursedgold.KEY, 3f, 0f).add(Applydesertenchant.KEY, 4f, 0f).add(Fulldesertenchant.KEY, 4f, 1f).add(Obtaincactus.KEY, -1f, 0f).add(Findtemple.KEY, -2f, 0f).add(Finddesertlapis.KEY, 5f, 0f).add(entermesaKey, 0f, 0f).add(Minemesagold.KEY, 1f, 0f).add(Findheirloom.KEY, 1f, -1f).add(Gildheirloom.KEY, 1f, -2f).add(Applyheirloom.KEY, 1f, -3f).add(Findkali.KEY, 2f, 0f).add(Barterkali.KEY, 3f, 0f).add(Kalicurse.KEY, 3f, 1f).add(Kaliblesses.KEY, 4f, 0f).add(Kalihalfway.KEY, 5f, 0f).add(Kalileaves.KEY, 6f, 0f).build();
        swamp = api.createAdvancementTab(AdvancementTabNamespaces.swamp_NAMESPACE);
        AdvancementKey enterswampKey = new AdvancementKey(swamp.getNamespace(), "enterswamp");
        CoordAdapter adapterswamp = CoordAdapter.builder().add(combatrootKey, 0f, 0f).add(Slay1.KEY, 1f, 0f).add(Slay10.KEY, 1f, -1f).add(Slay50.KEY, 1f, -2f).add(Slay100.KEY, 1f, -3f).add(Slay500.KEY, 1f, -4f).add(Slay1000.KEY, 1f, -5f).add(Freezing.KEY, 5f, 0f).add(Hypothermia.KEY, 5f, 1f).add(Breed.KEY, 3f, 0f).add(Plant.KEY, 2f, 0f).add(Fishing.KEY, 4f, 0f).add(Icefish.KEY, 5f, -1f).add(Completeconduit.KEY, 8f, -1f).add(Locateconduit.KEY, 6f, -1f).add(Repairconduit.KEY, 7f, -1f).add(enterdesertKey, 0f, 0f).add(Slayhusk.KEY, 1f, 0f).add(Slaycursed.KEY, 2f, 0f).add(Getcursed.KEY, 2f, -1f).add(Allcurses.KEY, 2f, -2f).add(Dropcursedgold.KEY, 3f, 0f).add(Applydesertenchant.KEY, 4f, 0f).add(Fulldesertenchant.KEY, 4f, 1f).add(Obtaincactus.KEY, -1f, 0f).add(Findtemple.KEY, -2f, 0f).add(Finddesertlapis.KEY, 5f, 0f).add(entermesaKey, 0f, 0f).add(Minemesagold.KEY, 1f, 0f).add(Findheirloom.KEY, 1f, -1f).add(Gildheirloom.KEY, 1f, -2f).add(Applyheirloom.KEY, 1f, -3f).add(Findkali.KEY, 2f, 0f).add(Barterkali.KEY, 3f, 0f).add(Kalicurse.KEY, 3f, 1f).add(Kaliblesses.KEY, 4f, 0f).add(Kalihalfway.KEY, 5f, 0f).add(Kalileaves.KEY, 6f, 0f).add(enterswampKey, 0f, 0f).add(Slayslime.KEY, 1f, 0f).add(Slaywitch.KEY, 2f, 0f).add(Slaywitches10.KEY, 2f, -1f).add(Slaywitches100.KEY, 2f, -2f).add(Tonic1.KEY, 3f, 0f).add(Tonic2.KEY, 3f, 1f).add(Tonic3.KEY, 3f, 2f).add(Dilutetonic.KEY, 4f, 0f).add(Potenttonic.KEY, 4f, -1f).add(Weaknesstonic.KEY, 5f, 0f).add(Heattonic.KEY, 6f, 0f).add(Coldtonic.KEY, 7f, 0f).build();
        cherry = api.createAdvancementTab(AdvancementTabNamespaces.cherry_NAMESPACE);
        mountain = api.createAdvancementTab(AdvancementTabNamespaces.mountain_NAMESPACE);
        AdvancementKey entermountainKey = new AdvancementKey(mountain.getNamespace(), "entermountain");
        CoordAdapter adaptermountain = CoordAdapter.builder().add(combatrootKey, 0f, 0f).add(Slay1.KEY, 1f, 0f).add(Slay10.KEY, 1f, -1f).add(Slay50.KEY, 1f, -2f).add(Slay100.KEY, 1f, -3f).add(Slay500.KEY, 1f, -4f).add(Slay1000.KEY, 1f, -5f).add(Freezing.KEY, 5f, 0f).add(Hypothermia.KEY, 5f, 1f).add(Breed.KEY, 3f, 0f).add(Plant.KEY, 2f, 0f).add(Fishing.KEY, 4f, 0f).add(Icefish.KEY, 5f, -1f).add(Completeconduit.KEY, 8f, -1f).add(Locateconduit.KEY, 6f, -1f).add(Repairconduit.KEY, 7f, -1f).add(enterdesertKey, 0f, 0f).add(Slayhusk.KEY, 1f, 0f).add(Slaycursed.KEY, 2f, 0f).add(Getcursed.KEY, 2f, -1f).add(Allcurses.KEY, 2f, -2f).add(Dropcursedgold.KEY, 3f, 0f).add(Applydesertenchant.KEY, 4f, 0f).add(Fulldesertenchant.KEY, 4f, 1f).add(Obtaincactus.KEY, -1f, 0f).add(Findtemple.KEY, -2f, 0f).add(Finddesertlapis.KEY, 5f, 0f).add(entermesaKey, 0f, 0f).add(Minemesagold.KEY, 1f, 0f).add(Findheirloom.KEY, 1f, -1f).add(Gildheirloom.KEY, 1f, -2f).add(Applyheirloom.KEY, 1f, -3f).add(Findkali.KEY, 2f, 0f).add(Barterkali.KEY, 3f, 0f).add(Kalicurse.KEY, 3f, 1f).add(Kaliblesses.KEY, 4f, 0f).add(Kalihalfway.KEY, 5f, 0f).add(Kalileaves.KEY, 6f, 0f).add(enterswampKey, 0f, 0f).add(Slayslime.KEY, 1f, 0f).add(Slaywitch.KEY, 2f, 0f).add(Slaywitches10.KEY, 2f, -1f).add(Slaywitches100.KEY, 2f, -2f).add(Tonic1.KEY, 3f, 0f).add(Tonic2.KEY, 3f, 1f).add(Tonic3.KEY, 3f, 2f).add(Dilutetonic.KEY, 4f, 0f).add(Potenttonic.KEY, 4f, -1f).add(Weaknesstonic.KEY, 5f, 0f).add(Heattonic.KEY, 6f, 0f).add(Coldtonic.KEY, 7f, 0f).add(entermountainKey, 0f, 0f).add(Getcoal.KEY, 1f, 0f).add(Getiron.KEY, 1f, -1f).add(Getgold.KEY, 1f, -2f).add(Getlapis.KEY, 2f, -2f).add(Getredstone.KEY, 0f, -2f).add(Getdiamonds.KEY, 1f, -3f).add(Startsuffocate.KEY, 2f, 0f).add(Getbulkredstone.KEY, 0f, -3f).add(Powertable.KEY, 2f, -3f).add(Endsuffocate.KEY, 3f, 0f).add(Drinkminingtonic.KEY, 4f, 0f).add(Drinkbane.KEY, 5f, 0f).add(Mineemerald.KEY, 3f, -1f).add(Findbeacon.KEY, 6f, 0f).add(Repairbeacon.KEY, 7f, 0f).add(Repairbeaconfully.KEY, 8f, 0f).add(Curevillager.KEY, 5f, -1f).add(Trade.KEY, 4f, -2f).add(Trademaster.KEY, 5f, -2f).build();
        jungle = api.createAdvancementTab(AdvancementTabNamespaces.jungle_NAMESPACE);
        AdvancementKey enterjungleKey = new AdvancementKey(jungle.getNamespace(), "enterjungle");
        CoordAdapter adapterjungle = CoordAdapter.builder().add(combatrootKey, 0f, 0f).add(Slay1.KEY, 1f, 0f).add(Slay10.KEY, 1f, -1f).add(Slay50.KEY, 1f, -2f).add(Slay100.KEY, 1f, -3f).add(Slay500.KEY, 1f, -4f).add(Slay1000.KEY, 1f, -5f).add(Freezing.KEY, 5f, 0f).add(Hypothermia.KEY, 5f, 1f).add(Breed.KEY, 3f, 0f).add(Plant.KEY, 2f, 0f).add(Fishing.KEY, 4f, 0f).add(Icefish.KEY, 5f, -1f).add(Completeconduit.KEY, 8f, -1f).add(Locateconduit.KEY, 6f, -1f).add(Repairconduit.KEY, 7f, -1f).add(enterdesertKey, 0f, 0f).add(Slayhusk.KEY, 1f, 0f).add(Slaycursed.KEY, 2f, 0f).add(Getcursed.KEY, 2f, -1f).add(Allcurses.KEY, 2f, -2f).add(Dropcursedgold.KEY, 3f, 0f).add(Applydesertenchant.KEY, 4f, 0f).add(Fulldesertenchant.KEY, 4f, 1f).add(Obtaincactus.KEY, -1f, 0f).add(Findtemple.KEY, -2f, 0f).add(Finddesertlapis.KEY, 5f, 0f).add(entermesaKey, 0f, 0f).add(Minemesagold.KEY, 1f, 0f).add(Findheirloom.KEY, 1f, -1f).add(Gildheirloom.KEY, 1f, -2f).add(Applyheirloom.KEY, 1f, -3f).add(Findkali.KEY, 2f, 0f).add(Barterkali.KEY, 3f, 0f).add(Kalicurse.KEY, 3f, 1f).add(Kaliblesses.KEY, 4f, 0f).add(Kalihalfway.KEY, 5f, 0f).add(Kalileaves.KEY, 6f, 0f).add(enterswampKey, 0f, 0f).add(Slayslime.KEY, 1f, 0f).add(Slaywitch.KEY, 2f, 0f).add(Slaywitches10.KEY, 2f, -1f).add(Slaywitches100.KEY, 2f, -2f).add(Tonic1.KEY, 3f, 0f).add(Tonic2.KEY, 3f, 1f).add(Tonic3.KEY, 3f, 2f).add(Dilutetonic.KEY, 4f, 0f).add(Potenttonic.KEY, 4f, -1f).add(Weaknesstonic.KEY, 5f, 0f).add(Heattonic.KEY, 6f, 0f).add(Coldtonic.KEY, 7f, 0f).add(entermountainKey, 0f, 0f).add(Getcoal.KEY, 1f, 0f).add(Getiron.KEY, 1f, -1f).add(Getgold.KEY, 1f, -2f).add(Getlapis.KEY, 2f, -2f).add(Getredstone.KEY, 0f, -2f).add(Getdiamonds.KEY, 1f, -3f).add(Startsuffocate.KEY, 2f, 0f).add(Getbulkredstone.KEY, 0f, -3f).add(Powertable.KEY, 2f, -3f).add(Endsuffocate.KEY, 3f, 0f).add(Drinkminingtonic.KEY, 4f, 0f).add(Drinkbane.KEY, 5f, 0f).add(Mineemerald.KEY, 3f, -1f).add(Findbeacon.KEY, 6f, 0f).add(Repairbeacon.KEY, 7f, 0f).add(Repairbeaconfully.KEY, 8f, 0f).add(Curevillager.KEY, 5f, -1f).add(Trade.KEY, 4f, -2f).add(Trademaster.KEY, 5f, -2f).add(enterjungleKey, 0f, 0f).add(Getinfected.KEY, 1f, 0f).add(Killdiseased.KEY, 1f, -1f).add(Cactustonic.KEY, 1f, 1f).add(Getjade.KEY, 2f, 0f).add(Locatestatue.KEY, 3f, 0f).add(Repairstatuefirst.KEY, 4f, 0f).add(Finishstatue.KEY, 5f, 0f).build();
        RootAdvancement combatroot = new RootAdvancement(consegrity, combatrootKey.getKey(), new FancyAdvancementDisplay(Material.ARMOR_STAND, "Consegrity", AdvancementFrameType.TASK, true, true, adapterconsegrity.getX(combatrootKey), adapterconsegrity.getY(combatrootKey),"", "Can you beat the game?"),"textures/block/iron_block.png",1);
        Slay1 slay1 = new Slay1(combatroot,adapterconsegrity.getX(Slay1.KEY), adapterconsegrity.getY(Slay1.KEY));
        Slay10 slay10 = new Slay10(slay1,adapterconsegrity.getX(Slay10.KEY), adapterconsegrity.getY(Slay10.KEY));
        Slay50 slay50 = new Slay50(slay10,adapterconsegrity.getX(Slay50.KEY), adapterconsegrity.getY(Slay50.KEY));
        Slay100 slay100 = new Slay100(slay50,adapterconsegrity.getX(Slay100.KEY), adapterconsegrity.getY(Slay100.KEY));
        Slay500 slay500 = new Slay500(slay100,adapterconsegrity.getX(Slay500.KEY), adapterconsegrity.getY(Slay500.KEY));
        Slay1000 slay1000 = new Slay1000(slay500,adapterconsegrity.getX(Slay1000.KEY), adapterconsegrity.getY(Slay1000.KEY));
        Freezing freezing = new Freezing(combatroot,adapterconsegrity.getX(Freezing.KEY), adapterconsegrity.getY(Freezing.KEY));
        Hypothermia hypothermia = new Hypothermia(freezing,adapterconsegrity.getX(Hypothermia.KEY), adapterconsegrity.getY(Hypothermia.KEY));
        Breed breed = new Breed(combatroot,adapterconsegrity.getX(Breed.KEY), adapterconsegrity.getY(Breed.KEY));
        Plant plant = new Plant(combatroot,adapterconsegrity.getX(Plant.KEY), adapterconsegrity.getY(Plant.KEY));
        Fishing fishing = new Fishing(combatroot,adapterconsegrity.getX(Fishing.KEY), adapterconsegrity.getY(Fishing.KEY));
        Icefish icefish = new Icefish(fishing,adapterconsegrity.getX(Icefish.KEY), adapterconsegrity.getY(Icefish.KEY));
        Locateconduit locateconduit = new Locateconduit(icefish,adapterconsegrity.getX(Locateconduit.KEY), adapterconsegrity.getY(Locateconduit.KEY));
        Repairconduit repairconduit = new Repairconduit(locateconduit,adapterconsegrity.getX(Repairconduit.KEY), adapterconsegrity.getY(Repairconduit.KEY));
        Completeconduit completeconduit = new Completeconduit(repairconduit,adapterconsegrity.getX(Completeconduit.KEY), adapterconsegrity.getY(Completeconduit.KEY));
        consegrity.registerAdvancements(combatroot ,slay1 ,slay10 ,slay50 ,slay100 ,slay500 ,slay1000 ,freezing ,hypothermia ,breed ,plant ,fishing ,icefish ,completeconduit ,locateconduit ,repairconduit );
        try {
            consegrityAdvancements.clear();
            consegrityAdvancements.addAll(java.util.Arrays.asList(
                    combatroot ,slay1 ,slay10 ,slay50 ,slay100 ,slay500 ,slay1000 ,freezing ,hypothermia ,breed ,plant ,fishing ,icefish ,completeconduit ,locateconduit ,repairconduit
            ));
        } catch (Throwable ignored) {}
        RootAdvancement enterdesert = new RootAdvancement(desert, enterdesertKey.getKey(), new AdvancementDisplay(Material.SAND, "Desert", AdvancementFrameType.TASK, true, true, adapterdesert.getX(enterdesertKey), adapterdesert.getY(enterdesertKey), "Enter The Desert"),"textures/block/sandstone.png",1);
        Slayhusk slayhusk = new Slayhusk(enterdesert,adapterdesert.getX(Slayhusk.KEY), adapterdesert.getY(Slayhusk.KEY));
        Slaycursed slaycursed = new Slaycursed(enterdesert,adapterdesert.getX(Slaycursed.KEY), adapterdesert.getY(Slaycursed.KEY));
        Getcursed getcursed = new Getcursed(slaycursed,adapterdesert.getX(Getcursed.KEY), adapterdesert.getY(Getcursed.KEY));
        Allcurses allcurses = new Allcurses(getcursed,adapterdesert.getX(Allcurses.KEY), adapterdesert.getY(Allcurses.KEY));
        Dropcursedgold dropcursedgold = new Dropcursedgold(enterdesert,adapterdesert.getX(Dropcursedgold.KEY), adapterdesert.getY(Dropcursedgold.KEY));
        Applydesertenchant applydesertenchant = new Applydesertenchant(enterdesert,adapterdesert.getX(Applydesertenchant.KEY), adapterdesert.getY(Applydesertenchant.KEY));
        Fulldesertenchant fulldesertenchant = new Fulldesertenchant(applydesertenchant,adapterdesert.getX(Fulldesertenchant.KEY), adapterdesert.getY(Fulldesertenchant.KEY));
        Obtaincactus obtaincactus = new Obtaincactus(enterdesert,adapterdesert.getX(Obtaincactus.KEY), adapterdesert.getY(Obtaincactus.KEY));
        Findtemple findtemple = new Findtemple(enterdesert,adapterdesert.getX(Findtemple.KEY), adapterdesert.getY(Findtemple.KEY));
        Finddesertlapis finddesertlapis = new Finddesertlapis(enterdesert,adapterdesert.getX(Finddesertlapis.KEY), adapterdesert.getY(Finddesertlapis.KEY));
        desert.registerAdvancements(enterdesert ,slayhusk ,slaycursed ,getcursed ,allcurses ,dropcursedgold ,applydesertenchant ,fulldesertenchant ,obtaincactus ,findtemple ,finddesertlapis );
        RootAdvancement entermesa = new RootAdvancement(mesa, entermesaKey.getKey(), new AdvancementDisplay(Material.RED_SAND, "Mesa", AdvancementFrameType.TASK, true, true, adaptermesa.getX(entermesaKey), adaptermesa.getY(entermesaKey), "Enter The Mesa"),"textures/block/red_sand.png",1);
        Minemesagold minemesagold = new Minemesagold(entermesa,adaptermesa.getX(Minemesagold.KEY), adaptermesa.getY(Minemesagold.KEY));
        Findheirloom findheirloom = new Findheirloom(minemesagold,adaptermesa.getX(Findheirloom.KEY), adaptermesa.getY(Findheirloom.KEY));
        Gildheirloom gildheirloom = new Gildheirloom(findheirloom,adaptermesa.getX(Gildheirloom.KEY), adaptermesa.getY(Gildheirloom.KEY));
        Applyheirloom applyheirloom = new Applyheirloom(findheirloom,adaptermesa.getX(Applyheirloom.KEY), adaptermesa.getY(Applyheirloom.KEY));
        Findkali findkali = new Findkali(entermesa,adaptermesa.getX(Findkali.KEY), adaptermesa.getY(Findkali.KEY));
        Barterkali barterkali = new Barterkali(findkali,adaptermesa.getX(Barterkali.KEY), adaptermesa.getY(Barterkali.KEY));
        Kalicurse kalicurse = new Kalicurse(barterkali,adaptermesa.getX(Kalicurse.KEY), adaptermesa.getY(Kalicurse.KEY));
        Kaliblesses kaliblesses = new Kaliblesses(findkali,adaptermesa.getX(Kaliblesses.KEY), adaptermesa.getY(Kaliblesses.KEY));
        Kalihalfway kalihalfway = new Kalihalfway(kaliblesses,adaptermesa.getX(Kalihalfway.KEY), adaptermesa.getY(Kalihalfway.KEY));
        Kalileaves kalileaves = new Kalileaves(kalihalfway,adaptermesa.getX(Kalileaves.KEY), adaptermesa.getY(Kalileaves.KEY));
        mesa.registerAdvancements(entermesa ,minemesagold ,findheirloom ,gildheirloom ,applyheirloom ,findkali ,barterkali ,kalicurse ,kaliblesses ,kalihalfway ,kalileaves );
        RootAdvancement enterswamp = new RootAdvancement(swamp, enterswampKey.getKey(), new AdvancementDisplay(Material.OAK_SAPLING, "Swamp", AdvancementFrameType.TASK, true, true, adapterswamp.getX(enterswampKey), adapterswamp.getY(enterswampKey), "Enter The Swamp"),"textures/block/cauldron_side.png",1);
        Slayslime slayslime = new Slayslime(enterswamp,adapterswamp.getX(Slayslime.KEY), adapterswamp.getY(Slayslime.KEY));
        Slaywitch slaywitch = new Slaywitch(enterswamp,adapterswamp.getX(Slaywitch.KEY), adapterswamp.getY(Slaywitch.KEY));
        Slaywitches10 slaywitches10 = new Slaywitches10(slaywitch,adapterswamp.getX(Slaywitches10.KEY), adapterswamp.getY(Slaywitches10.KEY));
        Slaywitches100 slaywitches100 = new Slaywitches100(slaywitches10,adapterswamp.getX(Slaywitches100.KEY), adapterswamp.getY(Slaywitches100.KEY));
        Tonic1 tonic1 = new Tonic1(enterswamp,adapterswamp.getX(Tonic1.KEY), adapterswamp.getY(Tonic1.KEY));
        Tonic2 tonic2 = new Tonic2(tonic1,adapterswamp.getX(Tonic2.KEY), adapterswamp.getY(Tonic2.KEY));
        Tonic3 tonic3 = new Tonic3(tonic2,adapterswamp.getX(Tonic3.KEY), adapterswamp.getY(Tonic3.KEY));
        Dilutetonic dilutetonic = new Dilutetonic(enterswamp,adapterswamp.getX(Dilutetonic.KEY), adapterswamp.getY(Dilutetonic.KEY));
        Potenttonic potenttonic = new Potenttonic(dilutetonic,adapterswamp.getX(Potenttonic.KEY), adapterswamp.getY(Potenttonic.KEY));
        Weaknesstonic weaknesstonic = new Weaknesstonic(enterswamp,adapterswamp.getX(Weaknesstonic.KEY), adapterswamp.getY(Weaknesstonic.KEY));
        Heattonic heattonic = new Heattonic(enterswamp,adapterswamp.getX(Heattonic.KEY), adapterswamp.getY(Heattonic.KEY));
        Coldtonic coldtonic = new Coldtonic(enterswamp,adapterswamp.getX(Coldtonic.KEY), adapterswamp.getY(Coldtonic.KEY));
        swamp.registerAdvancements(enterswamp ,slayslime ,slaywitch ,slaywitches10 ,slaywitches100 ,tonic1 ,tonic2 ,tonic3 ,dilutetonic ,potenttonic ,weaknesstonic ,heattonic ,coldtonic );
        RootAdvancement entercherry = new RootAdvancement(cherry, "entercherry", new AdvancementDisplay(Material.PINK_TULIP, "Cherry Grove", AdvancementFrameType.TASK, true, true, 0f, 0f , "Enter The Cherry Grove"),"textures/block/pink_stained_glass.png",1);
        Slaysamurai slaysamurai = new Slaysamurai(entercherry);
        Getrosegoldore getrosegoldore = new Getrosegoldore(slaysamurai);
        Makerosegoldequipment makerosegoldequipment = new Makerosegoldequipment(getrosegoldore);
        Enchantrosegoldequipment enchantrosegoldequipment = new Enchantrosegoldequipment(makerosegoldequipment);
        cherry.registerAdvancements(entercherry ,slaysamurai ,getrosegoldore ,makerosegoldequipment ,enchantrosegoldequipment );
        RootAdvancement entermountain = new RootAdvancement(mountain, entermountainKey.getKey(), new AdvancementDisplay(Material.DEEPSLATE_DIAMOND_ORE, "The Mountain", AdvancementFrameType.TASK, true, true, adaptermountain.getX(entermountainKey), adaptermountain.getY(entermountainKey), "Reach The Mountain"),"textures/block/deepslate.png",1);
        Getcoal getcoal = new Getcoal(entermountain,adaptermountain.getX(Getcoal.KEY), adaptermountain.getY(Getcoal.KEY));
        Getiron getiron = new Getiron(getcoal,adaptermountain.getX(Getiron.KEY), adaptermountain.getY(Getiron.KEY));
        Getgold getgold = new Getgold(getiron,adaptermountain.getX(Getgold.KEY), adaptermountain.getY(Getgold.KEY));
        Getlapis getlapis = new Getlapis(getgold,adaptermountain.getX(Getlapis.KEY), adaptermountain.getY(Getlapis.KEY));
        Getredstone getredstone = new Getredstone(getgold,adaptermountain.getX(Getredstone.KEY), adaptermountain.getY(Getredstone.KEY));
        Getdiamonds getdiamonds = new Getdiamonds(getgold,adaptermountain.getX(Getdiamonds.KEY), adaptermountain.getY(Getdiamonds.KEY));
        Startsuffocate startsuffocate = new Startsuffocate(getcoal,adaptermountain.getX(Startsuffocate.KEY), adaptermountain.getY(Startsuffocate.KEY));
        Getbulkredstone getbulkredstone = new Getbulkredstone(getredstone,adaptermountain.getX(Getbulkredstone.KEY), adaptermountain.getY(Getbulkredstone.KEY));
        Powertable powertable = new Powertable(getlapis,adaptermountain.getX(Powertable.KEY), adaptermountain.getY(Powertable.KEY));
        Endsuffocate endsuffocate = new Endsuffocate(startsuffocate,adaptermountain.getX(Endsuffocate.KEY), adaptermountain.getY(Endsuffocate.KEY));
        Drinkminingtonic drinkminingtonic = new Drinkminingtonic(endsuffocate,adaptermountain.getX(Drinkminingtonic.KEY), adaptermountain.getY(Drinkminingtonic.KEY));
        Drinkbane drinkbane = new Drinkbane(drinkminingtonic,adaptermountain.getX(Drinkbane.KEY), adaptermountain.getY(Drinkbane.KEY));
        Mineemerald mineemerald = new Mineemerald(drinkbane,adaptermountain.getX(Mineemerald.KEY), adaptermountain.getY(Mineemerald.KEY));
        Findbeacon findbeacon = new Findbeacon(drinkbane,adaptermountain.getX(Findbeacon.KEY), adaptermountain.getY(Findbeacon.KEY));
        Repairbeacon repairbeacon = new Repairbeacon(findbeacon,adaptermountain.getX(Repairbeacon.KEY), adaptermountain.getY(Repairbeacon.KEY));
        Repairbeaconfully repairbeaconfully = new Repairbeaconfully(repairbeacon,adaptermountain.getX(Repairbeaconfully.KEY), adaptermountain.getY(Repairbeaconfully.KEY));
        Curevillager curevillager = new Curevillager(entermountain,adaptermountain.getX(Curevillager.KEY), adaptermountain.getY(Curevillager.KEY));
        Trade trade = new Trade(curevillager,adaptermountain.getX(Trade.KEY), adaptermountain.getY(Trade.KEY));
        Trademaster trademaster = new Trademaster(trade,adaptermountain.getX(Trademaster.KEY), adaptermountain.getY(Trademaster.KEY));
        mountain.registerAdvancements(entermountain ,getcoal ,getiron ,getgold ,getlapis ,getredstone ,getdiamonds ,startsuffocate ,getbulkredstone ,powertable ,endsuffocate ,drinkminingtonic ,drinkbane ,mineemerald ,findbeacon ,repairbeacon ,repairbeaconfully ,curevillager ,trade ,trademaster );
        RootAdvancement enterjungle = new RootAdvancement(jungle, enterjungleKey.getKey(), new AdvancementDisplay(Material.JUNGLE_SAPLING, "The Jungle", AdvancementFrameType.TASK, true, true, adapterjungle.getX(enterjungleKey), adapterjungle.getY(enterjungleKey), "Enter The Jungle"),"textures/block/jungle_log.png",1);
        Getinfected getinfected = new Getinfected(enterjungle,adapterjungle.getX(Getinfected.KEY), adapterjungle.getY(Getinfected.KEY));
        Killdiseased killdiseased = new Killdiseased(getinfected,adapterjungle.getX(Killdiseased.KEY), adapterjungle.getY(Killdiseased.KEY));
        Cactustonic cactustonic = new Cactustonic(getinfected,adapterjungle.getX(Cactustonic.KEY), adapterjungle.getY(Cactustonic.KEY));
        Getjade getjade = new Getjade(enterjungle,adapterjungle.getX(Getjade.KEY), adapterjungle.getY(Getjade.KEY));
        Locatestatue locatestatue = new Locatestatue(enterjungle,adapterjungle.getX(Locatestatue.KEY), adapterjungle.getY(Locatestatue.KEY));
        Repairstatuefirst repairstatuefirst = new Repairstatuefirst(locatestatue,adapterjungle.getX(Repairstatuefirst.KEY), adapterjungle.getY(Repairstatuefirst.KEY));
        Finishstatue finishstatue = new Finishstatue(repairstatuefirst,adapterjungle.getX(Finishstatue.KEY), adapterjungle.getY(Finishstatue.KEY));
        jungle.registerAdvancements(enterjungle ,getinfected ,killdiseased ,cactustonic ,getjade ,locatestatue ,repairstatuefirst ,finishstatue );
    }

    private AdvancementTab getOrCreateTab(String namespace) {
        try {
            // Prefer retrieval if available to avoid duplicates on reloads
            try {
                AdvancementTab existing = api.getAdvancementTab(namespace);
                if (existing != null) return existing;
            } catch (Throwable ignored) { }
            return api.createAdvancementTab(namespace);
        } catch (IllegalArgumentException alreadyExists) {
            try {
                AdvancementTab existing = api.getAdvancementTab(namespace);
                if (existing != null) return existing;
            } catch (Throwable ignored) { }
            throw alreadyExists;
        }
    }


    public void initializeAdvancements() {
        // Disable vanilla advancements and recipe advancements via UAA (visual/UI suppression)
        try {
            api.disableVanillaAdvancements();
            api.disableVanillaRecipeAdvancements();
        } catch (RuntimeException ex) {
            getLogger().warning("Couldn't disable vanilla advancements: " + ex.getMessage());
        }

        // Also turn off announcement toasts/messages from vanilla via gamerule
        try {
            for (World w : Bukkit.getWorlds()) {
                w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            }
        } catch (Throwable ignored) {
        }
    }

    public java.util.List<com.fren_gor.ultimateAdvancementAPI.advancement.Advancement> getConsegrityAdvancements() {
        return consegrityAdvancements;
    }

    // Show the tab corresponding to the player's current region
    public void showRegionTab(org.bukkit.entity.Player player, ConsegrityRegions.Region region) {
        AdvancementTab tabToShow;
        switch (region) {
            case CENTRAL:
                tabToShow = consegrity; break;
            case DESERT:
                tabToShow = desert; break;
            case MESA:
                tabToShow = mesa; break;
            case SWAMP:
                tabToShow = swamp; break;
            case JUNGLE:
                tabToShow = jungle; break;
            case MOUNTAIN:
                tabToShow = mountain; break;
            case CHERRY:
                tabToShow = cherry; break;
            case ICE_SPIKES:
            case OCEAN:
            default:
                tabToShow = consegrity; break;
        }
        if (tabToShow != null) {
            try { tabToShow.showTab(player); } catch (Throwable ignored) {}
        }
    }

    // Best-effort revocation of vanilla advancements after they complete (event not cancellable)
    @EventHandler
    public void onVanillaAdvancement(PlayerAdvancementDoneEvent e) {
        try {
            Advancement adv = e.getAdvancement();
            NamespacedKey key = adv.getKey();
            if (key != null && "minecraft".equalsIgnoreCase(key.getNamespace())) {
                var player = e.getPlayer();
                var progress = player.getAdvancementProgress(adv);
                for (String awarded : new java.util.HashSet<>(progress.getAwardedCriteria())) {
                    progress.revokeCriteria(awarded);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onDisable() {
        if (uaaMain != null) {
            try {
                uaaMain.disable();
            } catch (Exception ignored) {
            }
        }
    }
}
