/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package goat.projectLinearity;

import com.fren_gor.ultimateAdvancementAPI.AdvancementMain;
import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import com.fren_gor.ultimateAdvancementAPI.UltimateAdvancementAPI;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.RootAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.FancyAdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.events.PlayerLoadingCompletedEvent;
import goat.projectLinearity.subsystems.advancements.advs.AdvancementTabNamespaces;
import goat.projectLinearity.subsystems.advancements.advs.tab0.Oak_sapling2;
import goat.projectLinearity.subsystems.advancements.advs.tab0.Oak_sapling3;
import goat.projectLinearity.commands.RegenerateCommand;
import goat.projectLinearity.world.ConsegritySpawnListener;
import goat.projectLinearity.world.RegionTitleListener;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class ProjectLinearity
extends JavaPlugin
implements Listener {
    public static UltimateAdvancementAPI api;
    public AdvancementTab tab0;
    private AdvancementMain uaaMain;
    private final List<AdvancementTab> allTabs = new ArrayList<AdvancementTab>();

    public void onEnable() {
        this.uaaMain = new AdvancementMain((Plugin)this);
        this.uaaMain.load();
        this.uaaMain.enableInMemory();
        this.initializeTabs();
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        this.getCommand("regenerate").setExecutor((CommandExecutor)new RegenerateCommand());
        Bukkit.getPluginManager().registerEvents((Listener)new ConsegritySpawnListener(), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new RegionTitleListener(), (Plugin)this);
    }

    public void initializeTabs() {
        api = UltimateAdvancementAPI.getInstance((Plugin)this);
        this.tab0 = api.createAdvancementTab(AdvancementTabNamespaces.tab0_NAMESPACE);
        RootAdvancement oak_sapling0 = new RootAdvancement(this.tab0, "oak_sapling0", new FancyAdvancementDisplay(Material.OAK_SAPLING, "Get 1 Wood", AdvancementFrameType.TASK, true, true, 0.0f, 0.0f, "", "Yup."), "textures/block/polished_andesite.png", 1);
        Oak_sapling2 oak_sapling2 = new Oak_sapling2(oak_sapling0);
        Oak_sapling3 oak_sapling3 = new Oak_sapling3(oak_sapling2);
        this.tab0.registerAdvancements(oak_sapling0, oak_sapling2, oak_sapling3);
        this.tab0.automaticallyGrantRootAdvancement();
        this.tab0.automaticallyShowToPlayers();
        this.allTabs.add(this.tab0);
        this.buildLinearTab("skills_combat", Material.IRON_SWORD, "Combat", "Progress your combat prowess.", new AdvItem("Apprentice Combat", Material.STONE_SWORD), new AdvItem("Journeyman Combat", Material.IRON_SWORD), new AdvItem("Master Combat", Material.DIAMOND_SWORD));
        this.buildLinearTab("skills_mining", Material.IRON_PICKAXE, "Mining", "Dig deeper and mine rarer ores.", new AdvItem("Stone Miner", Material.STONE_PICKAXE), new AdvItem("Iron Miner", Material.IRON_PICKAXE), new AdvItem("Diamond Miner", Material.DIAMOND_PICKAXE));
        this.buildLinearTab("skills_farming", Material.WHEAT, "Farming", "Cultivate and harvest bountiful crops.", new AdvItem("Farmhand", Material.WOODEN_HOE), new AdvItem("Planter", Material.IRON_HOE), new AdvItem("Harvester", Material.DIAMOND_HOE));
        this.buildLinearTab("skills_fishing", Material.FISHING_ROD, "Fishing", "Master the frozen waters.", new AdvItem("Angler", Material.COD), new AdvItem("Fisher", Material.SALMON), new AdvItem("Trawler", Material.TROPICAL_FISH));
        this.buildLinearTab("skills_woodcutting", Material.IRON_AXE, "Woodcutting", "Chop and process timber.", new AdvItem("Lumberjack", Material.OAK_LOG), new AdvItem("Timberwright", Material.SPRUCE_LOG), new AdvItem("Sawyer", Material.BIRCH_LOG));
        this.buildLinearTab("skills_exploration", Material.COMPASS, "Exploration", "Chart the Consegrity sectors.", new AdvItem("Scout", Material.MAP), new AdvItem("Pathfinder", Material.SPYGLASS), new AdvItem("Wayfarer", Material.ELYTRA));
        this.buildLinearTab("skills_building", Material.BRICKS, "Building", "Construct shelters and structures.", new AdvItem("Mason", Material.STONE_BRICKS), new AdvItem("Architect", Material.OAK_PLANKS), new AdvItem("Master Builder", Material.COPPER_BLOCK));
        this.buildLinearTab("skills_enchanting", Material.ENCHANTING_TABLE, "Enchanting", "Imbue gear with magic.", new AdvItem("Novice Enchanter", Material.LAPIS_LAZULI), new AdvItem("Adept Enchanter", Material.BOOKSHELF), new AdvItem("Master Enchanter", Material.ENCHANTED_BOOK));
        this.buildLinearTab("skills_brewing", Material.BREWING_STAND, "Brewing", "Brew helpful potions.", new AdvItem("Apprentice Brewer", Material.GLASS_BOTTLE), new AdvItem("Adept Brewer", Material.NETHER_WART), new AdvItem("Master Brewer", Material.POTION));
        this.buildLinearTab("skills_smithing", Material.SMITHING_TABLE, "Smithing", "Forge and upgrade tools.", new AdvItem("Blacksmith", Material.ANVIL), new AdvItem("Armorer", Material.IRON_CHESTPLATE), new AdvItem("Master Smith", Material.NETHERITE_INGOT));
        this.buildLinearTab("region_central", Material.GRASS_BLOCK, "Central Plains", "Thrive at the heart of Consegrity.", new AdvItem("Settle Central", Material.OAK_SAPLING), new AdvItem("Central Forager", Material.APPLE), new AdvItem("Central Steward", Material.HAY_BLOCK));
        this.buildLinearTab("region_desert", Material.SAND, "Desert", "Survive scorching sands.", new AdvItem("Desert Walker", Material.DEAD_BUSH), new AdvItem("Dune Roamer", Material.CACTUS), new AdvItem("Sand Warden", Material.SANDSTONE));
        this.buildLinearTab("region_savannah", Material.ACACIA_LOG, "Savannah", "Prosper among acacias.", new AdvItem("Savannah Settler", Material.ACACIA_SAPLING), new AdvItem("Savannah Forager", Material.MELON_SLICE), new AdvItem("Savannah Ranger", Material.CROSSBOW));
        this.buildLinearTab("region_swamp", Material.LILY_PAD, "Swamp", "Tread the murky marshes.", new AdvItem("Swamp Wader", Material.VINE), new AdvItem("Bog Forager", Material.SEA_PICKLE), new AdvItem("Marsh Keeper", Material.SLIME_BLOCK));
        this.buildLinearTab("region_jungle", Material.JUNGLE_LOG, "Jungle", "Endure in dense jungle.", new AdvItem("Jungle Trekker", Material.COCOA_BEANS), new AdvItem("Jungle Forager", Material.MELON), new AdvItem("Canopy Dweller", Material.BAMBOO));
        this.buildLinearTab("region_mesa", Material.RED_SANDSTONE, "Badlands", "Carve a life in mesas.", new AdvItem("Mesa Prospector", Material.RED_SAND), new AdvItem("Mesa Settler", Material.TERRACOTTA), new AdvItem("Canyon Pioneer", Material.CUT_RED_SANDSTONE));
        this.buildLinearTab("region_mountain", Material.STONE, "Mountains", "Survive the high peaks.", new AdvItem("Hill Climber", Material.SNOWBALL), new AdvItem("Peak Explorer", Material.GOAT_HORN), new AdvItem("Summit Conqueror", Material.SNOW_BLOCK));
        this.buildLinearTab("region_ice_spikes", Material.PACKED_ICE, "Ice Spikes", "Endure freezing spires.", new AdvItem("Frost Walker", Material.ICE), new AdvItem("Glacier Runner", Material.BLUE_ICE), new AdvItem("Icicle Warden", Material.PACKED_ICE));
        this.buildLinearTab("region_cherry", Material.CHERRY_LOG, "Cherry Grove", "Blossom among cherry trees.", new AdvItem("Petal Gatherer", Material.CHERRY_LEAVES), new AdvItem("Cherry Harvester", Material.CHERRY_SAPLING), new AdvItem("Grove Tender", Material.PINK_PETALS));
        this.buildLinearTab("region_ocean", Material.KELP, "Ocean", "Master the seas.", new AdvItem("Reef Diver", Material.PRISMARINE_SHARD), new AdvItem("Sea Forager", Material.KELP), new AdvItem("Ocean Navigator", Material.HEART_OF_THE_SEA));
    }

    private AdvancementTab buildLinearTab(String namespace, Material rootIcon, String title, String description, AdvItem stage1, AdvItem stage2, AdvItem stage3) {
        AdvancementTab tab = api.createAdvancementTab(namespace);
        RootAdvancement root = new RootAdvancement(tab, "root", new FancyAdvancementDisplay(rootIcon, title, AdvancementFrameType.TASK, true, true, 0.0f, 0.0f, "", description), "textures/block/polished_andesite.png", 1);
        BaseAdvancement a1 = new BaseAdvancement("stage1", new FancyAdvancementDisplay(stage1.icon, stage1.title, AdvancementFrameType.TASK, true, true, 1.0f, 0.0f, ""), root, 1);
        BaseAdvancement a2 = new BaseAdvancement("stage2", new FancyAdvancementDisplay(stage2.icon, stage2.title, AdvancementFrameType.TASK, true, true, 2.0f, 0.0f, ""), a1, 1);
        BaseAdvancement a3 = new BaseAdvancement("stage3", new FancyAdvancementDisplay(stage3.icon, stage3.title, AdvancementFrameType.GOAL, true, true, 3.0f, 0.0f, ""), a2, 1);
        tab.registerAdvancements(root, a1, a2, a3);
        tab.automaticallyGrantRootAdvancement();
        tab.automaticallyShowToPlayers();
        this.allTabs.add(tab);
        return tab;
    }

    @EventHandler
    public void onPlayerJoin(PlayerLoadingCompletedEvent e) {
        block3: {
            Player p = e.getPlayer();
            try {
                for (AdvancementTab t : this.allTabs) {
                    t.showTab(p);
                }
            }
            catch (Throwable ignored) {
                if (this.tab0 == null) break block3;
                this.tab0.showTab(p);
            }
        }
    }

    public void onDisable() {
        if (this.uaaMain != null) {
            try {
                this.uaaMain.disable();
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    private static final class AdvItem {
        final String title;
        final Material icon;

        AdvItem(String title, Material icon) {
            this.title = title;
            this.icon = icon;
        }
    }
}

