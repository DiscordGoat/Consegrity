package goat.projectLinearity.libs.gravedigging.corpses;

import goat.minecraft.minecraftnew.MinecraftNew;
import goat.minecraft.minecraftnew.subsystems.fishing.Rarity;
import goat.minecraft.minecraftnew.subsystems.gravedigging.CorpseKillManager;
import goat.minecraft.minecraftnew.subsystems.pets.PetRegistry;
import goat.minecraft.minecraftnew.utils.devtools.ItemRegistry;
import goat.minecraft.minecraftnew.utils.devtools.XPManager;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Handles drops and cleanup when a Corpse NPC dies.
 */
public class CorpseDeathEvent implements Listener {
    private final XPManager xpManager = new XPManager(MinecraftNew.getInstance());
    @EventHandler
    public void onCorpseDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        // 1) Check metadata so we only handle our corpses
        List<MetadataValue> meta = entity.getMetadata("CORPSE");
        if (meta.isEmpty()) return;

        // 2) Get the Citizens NPC wrapper for this entity
        NPC npc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(entity);
        if (npc == null) return;  // not a Citizens NPC

        // 3) Clear any item drops (we handle loot ourselves)
        event.getDrops().clear();

        // 4) Fetch your Corpse data by name and handle XP/visuals
        String corpseName = meta.get(0).asString();
        Optional<Corpse> corpseOpt = CorpseRegistry.getCorpseByName(corpseName);
        corpseOpt.ifPresent(corpse -> {
            playDeathEffects(entity, corpse.getRarity());

            // Award Terraforming XP to the killer based on corpse rarity
            var killer = event.getEntity().getKiller();
            if (killer != null) {
                int tier = switch (corpse.getRarity()) {
                    case UNCOMMON -> 2;
                    case RARE -> 3;
                    case EPIC -> 4;
                    case LEGENDARY -> 5;
                    default -> 1; // COMMON or unknown
                };
                int terraXP = 120 * tier;
                xpManager.addXP(killer, "Terraforming", terraXP);
            }

            // Legendary corpses (mass murderers) have a small chance to drop a random
            // ultimate smithing item. When this happens, notify the killer with a
            // message and play a celebratory sound.
            if (corpse.getRarity() == Rarity.LEGENDARY) {
                entity.getWorld().dropItemNaturally(
                        entity.getLocation(),
                        ItemRegistry.getRandomUltimateSmithingItem()
                );
                if (killer != null) {
                    killer.sendMessage(ChatColor.GOLD + "The mass murderer dropped an Ultimate Enchantment!");
                    killer.playSound(killer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                }
            }

            // Epic corpses have a 50% chance to drop Dinosaur Bones
            if (corpse.getRarity() == Rarity.EPIC && new Random().nextDouble() < 0.5) {
                entity.getWorld().dropItemNaturally(
                        entity.getLocation(),
                        ItemRegistry.getDinosaurBones()
                );
            }
        });

        // 4% chance to drop a random heirloom
        if (new Random().nextDouble() < 0.04) {
            ItemStack heirloom;
            int r = new Random().nextInt(3);
            if (r == 0) {
                heirloom = ItemRegistry.getGoldenRing();
            } else if (r == 1) {
                heirloom = ItemRegistry.getGoldenChalice();
            } else {
                heirloom = ItemRegistry.getGoldenCrown();
            }
            entity.getWorld().dropItemNaturally(entity.getLocation(), heirloom);
        }

        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            CorpseKillManager killManager = CorpseKillManager.getInstance();
            killManager.incrementCorpseKills(killer);
            int kills = killManager.getCorpseKills(killer);
            PetRegistry petRegistry = new PetRegistry();

            if (kills >= 3) {
                petRegistry.addPetByName(killer, "Imprint");
            }
            if (kills >= 9) {
                petRegistry.addPetByName(killer, "Spirit");
            }
            if (kills >= 27) {
                petRegistry.addPetByName(killer, "Banshee");
            }
            if (kills >= 94) {
                petRegistry.addPetByName(killer, "Wraith");
            }
            if (kills >= 282) {
                petRegistry.addPetByName(killer, "Revenant");
            }
        }

        // 5) Destroy the NPC so it won’t re-spawn on reload
        npc.destroy();

        Bukkit.getLogger().info("Destroyed corpse NPC #" + npc.getId() + " on death.");
    }


    private void playDeathEffects(Entity entity, Rarity rarity) {
        if (entity.getWorld() == null) return;
        Sound sound;
        Particle particle = Particle.SMOKE;
        float volume = 1.0f;
        float pitch = 1.0f;
        switch (rarity) {
            case UNCOMMON -> sound = Sound.ENTITY_SKELETON_DEATH;
            case RARE -> {
                sound = Sound.ENTITY_ZOMBIE_VILLAGER_DEATH;
                particle = Particle.CRIT;
            }
            case EPIC -> {
                sound = Sound.ENTITY_WITHER_DEATH;
                particle = Particle.EXPLOSION;
                volume = 1.5f;
                pitch = 0.8f;
            }
            case LEGENDARY -> {
                sound = Sound.ENTITY_ENDER_DRAGON_DEATH;
                particle = Particle.EXPLOSION;
                volume = 2.0f;
                pitch = 0.6f;
            }
            default -> sound = Sound.ENTITY_ZOMBIE_DEATH;
        }
        entity.getWorld().playSound(entity.getLocation(), sound, volume, pitch);
        entity.getWorld().spawnParticle(particle, entity.getLocation(), 25, 0.5, 0.5, 0.5, 0.1);
    }
}
