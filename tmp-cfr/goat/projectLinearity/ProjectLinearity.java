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

import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import com.fren_gor.ultimateAdvancementAPI.UltimateAdvancementAPI;
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

    public void onEnable() {
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
    }

    @EventHandler
    public void onPlayerJoin(PlayerLoadingCompletedEvent e) {
        Player p = e.getPlayer();
        this.tab0.showTab(p);
    }

    public void onDisable() {
    }
}

