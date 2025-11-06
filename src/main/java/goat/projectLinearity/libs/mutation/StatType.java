package goat.projectLinearity.libs.mutation;

/**
 * Types of stats that can be modified by a mutation.
 */
public enum StatType {
    HEALTH,
    SPEED,
    DAMAGE,
    RESISTANCE;

    public static Stat Health(double value) {
        return new Stat(HEALTH, value);
    }

    public static Stat Speed(double value) {
        return new Stat(SPEED, value);
    }

    public static Stat Damage(double value) {
        return new Stat(DAMAGE, value);
    }

    public static Stat Resistance(double value) {
        return new Stat(RESISTANCE, value);
    }
}
