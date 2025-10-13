package goat.projectLinearity.libs.gravedigging.corpses;

import goat.minecraft.minecraftnew.subsystems.fishing.Rarity;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.MemoryNPCDataStore;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Spawns a random Corpse from the registry with an emergence animation.
 */
public class CorpseEvent {
    private final JavaPlugin plugin;

    public CorpseEvent(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawns a random corpse at the given location rising from the ground.
     */
    public void trigger(Location location) {
        Optional<Corpse> optional = CorpseRegistry.getRandomCorpse();
        if (!optional.isPresent()) {
            return;
        }
        spawnCorpse(optional.get(), location);
    }

    /**
     * Spawns a random legendary corpse at the given location.
     */
    public void triggerLegendary(Location location) {
        List<Corpse> legendary = CorpseRegistry.getCorpses().stream()
                .filter(c -> c.getRarity() == Rarity.LEGENDARY)
                .collect(Collectors.toList());
        if (legendary.isEmpty()) {
            return;
        }
        Corpse corpse = legendary.get(new Random().nextInt(legendary.size()));
        spawnCorpse(corpse, location);
    }

    private void applyAttributes(NPC npc, Rarity rarity) {
        if (!(npc.getEntity() instanceof LivingEntity entity)) return;

        // 1) Clear any previous effects
        for (PotionEffectType type : Arrays.asList(
                PotionEffectType.HEALTH_BOOST,
                PotionEffectType.SLOWNESS,
                PotionEffectType.STRENGTH,
                PotionEffectType.RESISTANCE)) {
            entity.removePotionEffect(type);
        }

        // 2) Decide how much Health Boost you need to reach your target HP.
        //    Health Boost adds 4 hearts (8 HP) per amplifier level + base 20 HP.
        //    amp = ((targetHP − 20) / 4) − 1
        int hpBoostAmp = switch (rarity) {
            case COMMON    -> 19;   // (100-20)/4 -1 = 19
            case UNCOMMON  -> 44;   // (200-20)/4 -1 = 44
            case RARE      -> 69;   // (300-20)/4 -1 = 69
            case EPIC      -> 94;   // (400-20)/4 -1 = 94
            case LEGENDARY -> 119;  // (500-20)/4 -1 = 119
            case MYTHIC    -> 144;  // (600-20)/4 -1 = 144
        };

        // 3) Slowness II to cap movement speed near 0.1
        int slownessAmp = 1;

        // 4) Strength II to roughly double base melee damage
        int strengthAmp = 1;

        // Apply the effects “permanently”
        int duration = Integer.MAX_VALUE;
        entity.addPotionEffect(new PotionEffect(
                PotionEffectType.HEALTH_BOOST,
                duration, hpBoostAmp, true, false
        ));
        // reset health to the new max
        double newMax = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        entity.setHealth(newMax);

        entity.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                duration, 1, true, false
        ));
        entity.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                duration, slownessAmp, true, false
        ));
        entity.addPotionEffect(new PotionEffect(
                PotionEffectType.STRENGTH,
                duration, strengthAmp, true, false
        ));
    }


        /**
         * Spawns a Corpse NPC at the given location with the given corpse's
         * attributes and traits. The NPC is spawned at the location minus 1
         * block down to prevent it from spawning in the air.
         *
         * @param corpse the Corpse to spawn
         * @param loc    the location to spawn the Corpse at
         */

    private void spawnCorpse(Corpse corpse, Location loc) {
        NPCRegistry registry = CitizensAPI.createAnonymousNPCRegistry(new MemoryNPCDataStore());
        NPC npc = registry.createNPC(EntityType.PLAYER, corpse.getDisplayName());

        npc.setName(corpse.getDisplayName());
        if (npc.getEntity() instanceof LivingEntity le) {
            le.setCustomName(corpse.getDisplayName());
            le.setCustomNameVisible(true);
        }
        Location spawnLoc = loc.clone().subtract(0, 1, 0);

        npc.spawn(spawnLoc);
        applyAttributes(npc, corpse.getRarity());
        npc.getOrAddTrait(SkinTrait.class).setSkinPersistent("corpse", corpse.getSkinSignature(),
                corpse.getSkinTexture());
        if (npc.getEntity() instanceof LivingEntity le) {
            EntityEquipment eq = le.getEquipment();
            if (eq != null && corpse.getWeaponMaterial() != null && corpse.getWeaponMaterial() != Material.AIR) {
                eq.setItemInMainHand(new ItemStack(corpse.getWeaponMaterial()));
            }
        }
        npc.data().setPersistent(NPC.Metadata.DEFAULT_PROTECTED, false);
        npc.addTrait(new CorpseTrait(plugin, corpse.getRarity(), corpse.usesBow(),
                corpse.getDisplayName().equalsIgnoreCase("Duskblood") ? 100 : 0));
        npc.getEntity().setMetadata("CORPSE", new FixedMetadataValue(plugin, corpse.getDisplayName()));
        npc.getEntity().setMetadata("CORPSE_LEVEL", new FixedMetadataValue(plugin, corpse.getLevel()));
        playSpawnSound(loc, corpse.getRarity());
    }

    private void playSpawnSound(Location loc, Rarity rarity) {
        Sound sound;
        float volume = 1.0f;
        float pitch = 1.0f;
        switch (rarity) {
            case UNCOMMON:
                sound = Sound.ENTITY_ZOMBIE_AMBIENT;
                break;
            case RARE:
                sound = Sound.ENTITY_WITHER_SPAWN;
                volume = 1.2f;
                pitch = 0.9f;
                break;
            case EPIC:
                sound = Sound.ENTITY_WITHER_SPAWN;
                volume = 1.5f;
                pitch = 0.8f;
                break;
            case LEGENDARY:
                sound = Sound.ENTITY_ENDER_DRAGON_GROWL;
                volume = 2.0f;
                pitch = 0.7f;
                break;
            default:
                sound = Sound.BLOCK_GRAVEL_BREAK;
                break;
        }
        loc.getWorld().playSound(loc, sound, volume, pitch);
    }
}
