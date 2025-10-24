package goat.minecraft.minecraftnew.subsystems.fishing;

import goat.minecraft.minecraftnew.MinecraftNew;
import goat.minecraft.minecraftnew.other.additionalfunctionality.BlessingUtils;
import goat.minecraft.minecraftnew.other.skilltree.Skill;
import goat.minecraft.minecraftnew.other.skilltree.SkillTreeManager;
import goat.minecraft.minecraftnew.other.skilltree.Talent;
import goat.minecraft.minecraftnew.subsystems.combat.HostilityManager;
import goat.minecraft.minecraftnew.subsystems.pets.PetManager;
import goat.minecraft.minecraftnew.subsystems.pets.PetRegistry;
import goat.minecraft.minecraftnew.utils.devtools.ItemRegistry;
import goat.minecraft.minecraftnew.utils.devtools.XPManager;
import goat.minecraft.minecraftnew.utils.devtools.PlayerMeritManager;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.MetadataValue;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class SeaCreatureDeathEvent implements Listener {
    private final MinecraftNew plugin = MinecraftNew.getInstance();
    private final XPManager xpManager = new XPManager(plugin);
    private final Random random = new Random();

    private static final java.util.Map<java.util.UUID, Double> mawBonus = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, Long> mawExpire = new java.util.HashMap<>();

    public static double getMawBonus(Player player) {
        Double bonus = mawBonus.get(player.getUniqueId());
        Long exp = mawExpire.get(player.getUniqueId());
        if (bonus == null || exp == null) return 0;
        if (System.currentTimeMillis() > exp) {
            mawBonus.remove(player.getUniqueId());
            mawExpire.remove(player.getUniqueId());
            return 0;
        }
        return bonus;
    }

    public static void activateMawBonus(Player player, int level) {
        if (level <= 0) return;
        mawBonus.put(player.getUniqueId(), level * 1.0);
        mawExpire.put(player.getUniqueId(), System.currentTimeMillis() + 10000);
    }

    ItemStack lapis = new ItemStack(Material.LAPIS_LAZULI, 4);

    @EventHandler
    public void onSeaCreatureHit(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();

        // Ensure metadata exists
        List<MetadataValue> metadata = entity.getMetadata("SEA_CREATURE");

        if (!metadata.isEmpty()) {
            Location loc = entity.getLocation();
            World world = entity.getWorld();

            // Play iron clang sound
            world.playSound(loc, Sound.ENTITY_ZOMBIE_CONVERTED_TO_DROWNED, 0.1f, 0.5f);

            // Spawn iron particles
            world.spawnParticle(Particle.FISHING, loc, 10, 0.5, 1, 0.5);
        }
    }

    @EventHandler
    public void onSeaCreatureDeath(EntityDeathEvent event) {
        PetRegistry petRegistry = new PetRegistry();

        Entity entity = event.getEntity();
        List<MetadataValue> metadata = entity.getMetadata("SEA_CREATURE");
        if (metadata == null || metadata.isEmpty()) {
            // Debug: Print all metadata keys
            for (MetadataValue value : entity.getMetadata("SEA_CREATURE")) {
                Bukkit.getLogger().info("Found metadata key: " + value.toString());
            }
            return;
        }
        if (entity instanceof LivingEntity) {
            ((LivingEntity) entity).getEquipment().setHelmet(null);
            ((LivingEntity) entity).getEquipment().setItemInOffHandDropChance(1);
        }



        String creatureName = metadata.get(0).asString();
        Optional<SeaCreature> optionalSeaCreature = SeaCreatureRegistry.getSeaCreatureByName(creatureName);
        if (!optionalSeaCreature.isPresent()) {
            Bukkit.getLogger().warning("Sea creature not found in registry: " + creatureName);
            return;
        }

        // Prevent vanilla mob drops from being added to the loot table
        event.getDrops().clear();

        SeaCreature seaCreature = optionalSeaCreature.get();
        Player killer = event.getEntity().getKiller();
        if (!(killer instanceof Player)) {
            Bukkit.getLogger().info("Sea creature killed by non-player entity: " + event.getEntity().getKiller());
            return;
        }
        Player player = (Player) killer;

        int boostedXP = getBoostedXP(seaCreature.getRarity());
        xpManager.addXP(killer, "Fishing", boostedXP);
        event.setDroppedExp(100);
        FishingPetManager fishingPetManager = FishingPetManager.getInstance();

        fishingPetManager.incrementSeaCreatureKills(player);

        SkillTreeManager mgr = SkillTreeManager.getInstance();
        if (mgr != null) {
            int rain = mgr.getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.WHEN_IT_RAINS_IT_POURS);
            if (rain > 0 && player.getWorld().hasStorm() && random.nextDouble() < rain * 0.05) {
                World w = player.getWorld();
                w.setWeatherDuration(w.getWeatherDuration() + 200);
                player.sendMessage(ChatColor.AQUA + "The rain continues!");
            }

            int abyssal = mgr.getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.ABYSSAL_STRIKE);
            if (abyssal > 0 && random.nextDouble() < abyssal * 0.20) {
                java.util.List<ItemStack> extra = new java.util.ArrayList<>();
                for (ItemStack drop : event.getDrops()) {
                    extra.add(drop.clone());
                }
                event.getDrops().addAll(extra);
                player.sendMessage(ChatColor.DARK_PURPLE + "Abyssal Strike doubles your loot!");
            }

            int biolum = mgr.getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.BIOLUMINESCENCE);
            if (biolum > 0 && random.nextDouble() < biolum * 0.01) {
                for (ItemStack armor : player.getInventory().getArmorContents()) {
                    if (armor == null) continue;
                    int max = armor.getType().getMaxDurability();
                    if (max <= 0) continue;
                    org.bukkit.inventory.meta.Damageable dmg = (org.bukkit.inventory.meta.Damageable) armor.getItemMeta();
                    int repair = (int) (max * 0.10);
                    dmg.setDamage(Math.max(0, dmg.getDamage() - repair));
                    armor.setItemMeta((ItemMeta) dmg);
                }
                player.sendMessage(ChatColor.AQUA + "Bioluminescence mends your armor!");
            }

            int ruins = mgr.getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.SUNKEN_RUINS);
            if (ruins > 0 && random.nextDouble() < ruins * 0.01) {
                ItemStack stone = ItemRegistry.getCompactStone();
                stone.setAmount(64);
                event.getDrops().add(stone);
            }

            int maw = mgr.getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.MAW_OF_THE_VOID);
            activateMawBonus(player, maw);
        }

        // 16% chance to drop a Forbidden Book
        if (random.nextInt(100) < 32) {
            for (int i = 0; i < 1; i++) {
                event.getDrops().add(ItemRegistry.getForbiddenBook());
            }
        }

        // 2% chance to drop the Ghost relic
        if (random.nextInt(100) < 2) {
            event.getDrops().add(ItemRegistry.getGhost());
        }

        Bukkit.getLogger().info("Player " + killer.getName() + " gained " + boostedXP + " Fishing XP.");

        if(seaCreature.getSkullName().equals("Pirate")){
            PetManager petManager = PetManager.getInstance(plugin);
            int pirateChance = random.nextInt(100) + 1;
            if (pirateChance <= 10) {
                petRegistry.addPetByName(killer, "Golden Steve");            }
        }
        if(seaCreature.getSkullName().equals("Yeti")){
            PetManager petManager = PetManager.getInstance(plugin);
            int pirateChance = random.nextInt(100) + 1;
            if (pirateChance <= 10) {
                petRegistry.addPetByName(killer, "Yeti");            }
        } 



        // Handle drops
        if(seaCreature.getDrops() != null) {
            List<ItemStack> drops = seaCreature.getDrops();
            if (drops != null && !drops.isEmpty()) {
                for (ItemStack drop : drops) {
                    if (drop != null && drop.getType() != Material.AIR) {
                        event.getDrops().add(drop);
                    }
                }
                // Drop bait based on creature rarity
                ItemStack rarityBait = switch (seaCreature.getRarity()) {
                    case UNCOMMON -> ItemRegistry.getShrimpBait();
                    case RARE -> ItemRegistry.getLeechBait();
                    case EPIC -> ItemRegistry.getFrogBait();
                    case LEGENDARY -> ItemRegistry.getCaviarBait();
                    default -> ItemRegistry.getCommonBait();
                };

                rarityBait.setAmount(1);
                if(random.nextBoolean()) {
                    if (random.nextBoolean()) {
                        event.getDrops().add(rarityBait);
                    }
                }
                killer.playSound(killer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 10.f);
            }
        }

        // Play effects    
        playDeathEffects(entity, seaCreature.getRarity());

        int kills = fishingPetManager.getSeaCreatureKills(player);

            if (kills >= 3) {
                petRegistry.addPetByName(killer, "Fish");
            }

            if (kills >= 9) {
                petRegistry.addPetByName(killer, "Glow Squid");
            }


            if (kills >= 27) {
                petRegistry.addPetByName(killer, "Dolphin");
            }


            if (kills >= 81) {
                petRegistry.addPetByName(killer, "Turtle");
            }


            if (kills >= 243) {
                petRegistry.addPetByName(killer, "Leviathan");
            }

    }


    /**
     * Plays death sounds and spawns particles based on the rarity of the sea creature.
     *
     * @param entity The sea creature entity.
     * @param rarity The rarity of the sea creature.
     */
    private void playDeathEffects(Entity entity, Rarity rarity) {
        Sound sound;
        Particle particle;
        float volume = 1.0f;
        float pitch = 1.0f;

        switch (rarity) {
            case COMMON:
                sound = Sound.ENTITY_FISH_SWIM;
                particle = Particle.BUBBLE_POP;
                break;
            case UNCOMMON:
                sound = Sound.ENTITY_DOLPHIN_HURT;
                particle = Particle.FISHING;
                break;
            case RARE:
                sound = Sound.ENTITY_GUARDIAN_DEATH;
                particle = Particle.CRIT;
                break;
            case EPIC:
                sound = Sound.ENTITY_WITHER_DEATH;
                particle = Particle.EXPLOSION;
                volume = 1.5f;
                pitch = 0.8f;
                break;
            case LEGENDARY:
                sound = Sound.ENTITY_ENDER_DRAGON_GROWL;
                particle = Particle.DRAGON_BREATH;
                volume = 2.0f;
                pitch = 0.6f;
                break;
            case MYTHIC:
                sound = Sound.MUSIC_CREDITS;
                particle = Particle.TOTEM_OF_UNDYING;
                volume = 2.0f;
                pitch = 0.5f;
                break;
            default:
                sound = Sound.ENTITY_GENERIC_EXPLODE;
                particle = Particle.SMOKE;
                break;
        }

        entity.getWorld().playSound(entity.getLocation(), sound, volume, pitch);
        entity.getWorld().spawnParticle(particle, entity.getLocation(), 50, 1.0, 1.0, 1.0, 0.1);
    }

    private int getBoostedXP(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> 20 * 5/2;
            case UNCOMMON -> 40 * 5/2;
            case RARE -> 80 * 5/2;
            case EPIC -> 160 * 5/2;
            case LEGENDARY -> 320 * 5/2;
            case MYTHIC -> 640 * 5/2;
            default -> 20;
        };
    }
}
