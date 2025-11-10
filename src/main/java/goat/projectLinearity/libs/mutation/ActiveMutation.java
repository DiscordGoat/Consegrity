package goat.projectLinearity.libs.mutation;

import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitTask;

/**
 * Runtime data for a mutated entity.
 */
public final class ActiveMutation {
    private final MutationDefinition definition;
    private final double damageMultiplier;
    private final double resistancePercentage;
    private BukkitTask behaviorTask;
    private BossBar healthBossBar;
    private BukkitTask bossBarTask;

    public ActiveMutation(MutationDefinition definition, double damageMultiplier, double resistancePercentage) {
        this.definition = definition;
        this.damageMultiplier = damageMultiplier;
        this.resistancePercentage = Math.max(0.0, Math.min(100.0, resistancePercentage));
    }

    public MutationDefinition definition() {
        return definition;
    }

    public double damageMultiplier() {
        return damageMultiplier;
    }

    public double resistancePercentage() {
        return resistancePercentage;
    }

    public BukkitTask behaviorTask() {
        return behaviorTask;
    }

    public void behaviorTask(BukkitTask behaviorTask) {
        this.behaviorTask = behaviorTask;
    }

    public BossBar healthBossBar() {
        return healthBossBar;
    }

    public void healthBossBar(BossBar healthBossBar) {
        this.healthBossBar = healthBossBar;
    }

    public BukkitTask bossBarTask() {
        return bossBarTask;
    }

    public void bossBarTask(BukkitTask bossBarTask) {
        this.bossBarTask = bossBarTask;
    }

    public void cancelTasks() {
        if (behaviorTask != null) {
            try {
                behaviorTask.cancel();
            } catch (Throwable ignored) {
            }
            behaviorTask = null;
        }
        cancelBossBar();
    }

    public void cancelBossBar() {
        if (bossBarTask != null) {
            try {
                bossBarTask.cancel();
            } catch (Throwable ignored) {
            }
            bossBarTask = null;
        }
        if (healthBossBar != null) {
            try {
                healthBossBar.removeAll();
            } catch (Throwable ignored) {
            }
            healthBossBar = null;
        }
    }
}
