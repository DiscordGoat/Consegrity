package goat.projectLinearity.libs.gravedigging.corpses;

import goat.minecraft.minecraftnew.subsystems.fishing.Rarity;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;

/**
 * Trait giving a Corpse hostile behaviour and stats.
 */
public class CorpseTrait extends Trait {
    private final JavaPlugin plugin;
    private final Rarity rarity;
    private final boolean ranged;
    private final int teleportInterval;
    private int taskId = -1;
    private int tickCounter = 0;

    private static final double BASE_DAMAGE = 5.0;      // beefed up base damage
    private static final int ARCHER_COOLDOWN = 40;      // ticks between shots (2s)
    private int archerTimer = 0;
    private boolean buffsApplied = false;

    public CorpseTrait(JavaPlugin plugin, Rarity rarity, boolean ranged, int teleportInterval) {
        super("corpse_trait");
        this.plugin = plugin;
        this.rarity = rarity;
        this.ranged = ranged;
        this.teleportInterval = teleportInterval;
    }

    @Override
    public void onAttach() {
        npc.setProtected(false);
        startBehaviorLoop();
    }




    private void startBehaviorLoop() {
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!npc.isSpawned()) return;
            LivingEntity self = (LivingEntity) npc.getEntity();

            // ─── On first tick only: apply all buffs & 5s of regen ───
            if (!buffsApplied) {
                applyPermanentBuffs(self);
                // drop them to 1 HP so regen has something to heal
                self.setHealth(self.getMaxHealth());
                // 5s @ amp=4 is “insane”…should fill a few hundred HP in time
                self.addPotionEffect(new PotionEffect(
                        PotionEffectType.REGENERATION,
                        /*durationTicks=*/20*3,  /*5s * 20 tps*/
                        /*amp=*/255,
                        true, false
                ));
                buffsApplied = true;
            }

            // ─── your existing follow/teleport/attack logic ───
            Player target = getNearestPlayer(self.getLocation());
            if (target == null) return;

            npc.getNavigator().setTarget(target, true);
            // … teleport, ranged vs melee attacks, etc. …

        }, 10L, 10L);
    }

    private void applyPermanentBuffs(LivingEntity e) {
        // 1) clear old
        for (PotionEffectType t : Arrays.asList(
                PotionEffectType.HEALTH_BOOST,
                PotionEffectType.RESISTANCE,
                PotionEffectType.SLOWNESS,
                PotionEffectType.STRENGTH)) {
            e.removePotionEffect(t);
        }

        // 2) figure out the HEALTH_BOOST amp for your HP tiers
        int targetHP = switch (rarity) {
            case COMMON    -> 100;
            case UNCOMMON  -> 200;
            case RARE      -> 300;
            case EPIC      -> 400;
            case LEGENDARY -> 500;
            case MYTHIC    -> 600;
        };
        // each amp+1 adds +4 HP; base is 20 HP → amp = (targetHP−20)/4 −1
        int hpAmp = (targetHP - 20) / 4 - 1;

        int infinite = Integer.MAX_VALUE;
        e.addPotionEffect(new PotionEffect(
                PotionEffectType.HEALTH_BOOST, infinite, hpAmp, true, false
        ));
        e.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE, infinite, 1, true, false
        ));
        e.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, infinite, 1, true, false
        ));
        e.addPotionEffect(new PotionEffect(
                PotionEffectType.STRENGTH, infinite, 1, true, false
        ));
    }
    private Player getNearestPlayer(Location loc) {
        double best = Double.MAX_VALUE;
        Player closest = null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            double d = p.getLocation().distanceSquared(loc);
            if (d < best) {
                best = d;
                closest = p;
            }
        }
        return closest;
    }

    @Override
    public void onRemove() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    @Override public void load(DataKey key) {}
    @Override public void save(DataKey key) {}
}
