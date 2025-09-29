package goat.projectLinearity.commands;

import goat.projectLinearity.ProjectLinearity;
import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class GetAllConsegrityAdvancementsCommand implements CommandExecutor {

    private final ProjectLinearity plugin;

    public GetAllConsegrityAdvancementsCommand(ProjectLinearity plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("consegrity.dev")) {
            player.sendMessage("You lack permission: consegrity.dev");
            return true;
        }

        // Parse which tabs to use: default to Consegrity for backward compatibility,
        // but support specifying multiple tabs or "all".
        Set<AdvancementTab> tabs = new LinkedHashSet<>();

        if (args.length == 0) {
            if (plugin.consegrity != null) tabs.add(plugin.consegrity);
        } else {
            for (String raw : args) {
                String arg = raw.toLowerCase(Locale.ROOT);
                if (arg.equals("all") || arg.equals("*")) {
                    maybeAdd(tabs, plugin.consegrity, plugin.desert, plugin.mesa, plugin.swamp, plugin.cherry, plugin.mountain, plugin.jungle);
                    break; // "all" supersedes specific ones
                }
                switch (arg) {
                    case "consegrity":
                    case "central":
                        maybeAdd(tabs, plugin.consegrity); break;
                    case "desert":
                        maybeAdd(tabs, plugin.desert); break;
                    case "mesa":
                        maybeAdd(tabs, plugin.mesa); break;
                    case "swamp":
                        maybeAdd(tabs, plugin.swamp); break;
                    case "cherry":
                        maybeAdd(tabs, plugin.cherry); break;
                    case "mountain":
                    case "mountains":
                        maybeAdd(tabs, plugin.mountain); break;
                    case "jungle":
                        maybeAdd(tabs, plugin.jungle); break;
                    default:
                        player.sendMessage("Unknown tab '" + raw + "'. Valid: all, consegrity, desert, mesa, swamp, cherry, mountain, jungle");
                        return true;
                }
            }
        }

        if (tabs.isEmpty()) {
            player.sendMessage("No valid tabs selected or tabs not initialized.");
            return true;
        }

        // Build the advancement list from selected tabs.
        List<Advancement> toGrant = new ArrayList<>();
        for (AdvancementTab tab : tabs) {
            try { tab.showTab(player); } catch (Throwable ignored) {}
            try {
                // Prefer including roots so the tree appears complete.
                Collection<Advancement> advs = tab.getAdvancements();
                if (advs != null) toGrant.addAll(advs);
            } catch (Throwable ignored) {
                try {
                    // Fallback: without root if method differs across versions
                    Collection<BaseAdvancement> advs = tab.getAdvancementsWithoutRoot();
                    if (advs != null) {
                        for (BaseAdvancement ba : advs) {
                            toGrant.add(ba);
                        }
                    }
                } catch (Throwable ignored2) {}
            }
        }

        if (toGrant.isEmpty()) {
            player.sendMessage("No advancements found for selected tab(s).");
            return true;
        }

        // De-duplicate while preserving order
        List<Advancement> list = new ArrayList<>(new LinkedHashSet<>(toGrant));

        final int[] idx = {0};
        final int total = list.size();
        final String tabSummary = tabs.size() == 1 ? tabs.iterator().next().getNamespace() : (tabs.size() + " tabs");

        // Grant one advancement every 10 ticks (~0.5s) to speed up multi-tab runs
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (idx[0] >= total) {
                player.sendMessage("Granted " + total + " advancements from " + tabSummary + ".");
                task.cancel();
                return;
            }
            Advancement adv = list.get(idx[0]++);
            try { adv.grant(player); } catch (Throwable ignored) {}
            if (idx[0] >= total) {
                player.sendMessage("Granted " + total + " advancements from " + tabSummary + ".");
                task.cancel();
            }
        }, 0L, 10L);
        return true;
    }

    private static void maybeAdd(Set<AdvancementTab> tabs, AdvancementTab... toAdd) {
        if (toAdd == null) return;
        for (AdvancementTab t : toAdd) if (t != null) tabs.add(t);
    }
}
