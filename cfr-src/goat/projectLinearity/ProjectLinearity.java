/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package goat.projectLinearity;

import goat.projectLinearity.commands.RegenerateCommand;
import goat.projectLinearity.world.ConsegritySpawnListener;
import goat.projectLinearity.world.RegionTitleListener;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class ProjectLinearity
extends JavaPlugin {
    public void onEnable() {
        this.getCommand("regenerate").setExecutor((CommandExecutor)new RegenerateCommand());
        Bukkit.getPluginManager().registerEvents((Listener)new ConsegritySpawnListener(), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new RegionTitleListener(), (Plugin)this);
    }

    public void onDisable() {
    }
}

