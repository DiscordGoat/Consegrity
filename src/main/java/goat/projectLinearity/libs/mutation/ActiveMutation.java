package goat.projectLinearity.libs.mutation;

import org.bukkit.scheduler.BukkitTask;

/**
 * Runtime data for a mutated entity.
 */
public final class ActiveMutation {
    private final MutationDefinition definition;
    private final double damageMultiplier;
    private final double resistancePercentage;
    private BukkitTask behaviorTask;

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

    public void cancelTasks() {
        if (behaviorTask != null) {
            try {
                behaviorTask.cancel();
            } catch (Throwable ignored) {
            }
            behaviorTask = null;
        }
    }
}
